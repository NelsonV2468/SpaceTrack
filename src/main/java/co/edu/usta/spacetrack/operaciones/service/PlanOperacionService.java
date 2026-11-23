package co.edu.usta.spacetrack.operaciones.service;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteRequest;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteResponse;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
import co.edu.usta.spacetrack.operaciones.domain.DiasOperacion;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.OperacionResponse;
import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Registra y edita en un solo paso la operacion (contrato) y su necesidad principal (vehiculos, dias y horario).
 *
 * <p>Usa los servicios de cada modulo, asi que se aplican las mismas reglas de negocio. Los dias, la cantidad de
 * vehiculos y la cadena de frio se piden una sola vez: el contrato toma los dias y vehiculos de sus necesidades.
 * Las necesidades adicionales registradas antes de unificar el formulario se conservan y se suman al contrato.
 * Todo va en una transaccion: si una regla rechaza un paso, no queda nada a medias.</p>
 */
@Service
@Transactional(readOnly = true)
public class PlanOperacionService {

    private final OperacionService operacionService;
    private final NecesidadClienteService necesidadService;
    private final RutaRepository rutaRepository;

    public PlanOperacionService(OperacionService operacionService, NecesidadClienteService necesidadService,
                                RutaRepository rutaRepository) {
        this.operacionService = operacionService;
        this.necesidadService = necesidadService;
        this.rutaRepository = rutaRepository;
    }

    public List<PlanOperacionResponse> listar() {
        return operacionService.listar().stream()
                .map(o -> aResponse(o, necesidadService.listar(o.id())))
                .toList();
    }

    public PlanOperacionResponse obtenerPorId(Long id) {
        return aResponse(operacionService.obtenerPorId(id), necesidadService.listar(id));
    }

    @Transactional
    public PlanOperacionResponse crear(PlanOperacionRequest solicitud) {
        OperacionResponse operacion = operacionService.crear(operacion(solicitud, solicitud.estado(),
                solicitud.cantidadVehiculos(), solicitud.diasOperacion()));
        if (solicitud.estado() != EstadoOperacion.FINALIZADA) {
            necesidadService.crear(necesidad(operacion.id(), solicitud));
        }
        return obtenerPorId(operacion.id());
    }

    @Transactional
    public PlanOperacionResponse actualizar(Long id, PlanOperacionRequest solicitud) {
        OperacionResponse actual = operacionService.obtenerPorId(id);
        List<NecesidadClienteResponse> necesidades = necesidadService.listar(id);
        List<NecesidadClienteResponse> otras = necesidades.isEmpty() ? List.of() : necesidades.subList(1, necesidades.size());
        int vehiculosOtras = otras.stream().mapToInt(NecesidadClienteResponse::cantidadVehiculos).sum();
        Set<DiaSemana> diasFinales = EnumSet.copyOf(solicitud.diasOperacion());
        otras.forEach(n -> diasFinales.addAll(n.diasOperacion()));
        int vehiculosFinales = solicitud.cantidadVehiculos() + vehiculosOtras;

        // Un contrato finalizado ya no cambia su necesidad: solo se actualizan los datos del contrato.
        if (solicitud.estado() == EstadoOperacion.FINALIZADA || actual.estado() == EstadoOperacion.FINALIZADA) {
            int vehiculosRegistrados = necesidades.stream().mapToInt(NecesidadClienteResponse::cantidadVehiculos).sum();
            Set<DiaSemana> dias = EnumSet.copyOf(diasFinales);
            necesidades.forEach(n -> dias.addAll(n.diasOperacion()));
            operacionService.actualizar(id, operacion(solicitud, solicitud.estado(),
                    Math.max(vehiculosFinales, vehiculosRegistrados), dias));
            return obtenerPorId(id);
        }

        // 1) Se amplia el contrato para que la necesidad nueva quepa (vehiculos y dias), sin cambiar aun su estado.
        Set<DiaSemana> diasAmplios = EnumSet.copyOf(diasFinales);
        DiasOperacion.interpretar(actual.diasOperacion()).ifPresent(diasAmplios::addAll);
        necesidades.forEach(n -> diasAmplios.addAll(n.diasOperacion()));
        int vehiculosAmplios = Math.max(actual.numeroVehiculosRequeridos(), Math.max(vehiculosFinales,
                necesidades.stream().mapToInt(NecesidadClienteResponse::cantidadVehiculos).sum()));
        operacionService.actualizar(id, operacion(solicitud, actual.estado(), vehiculosAmplios, diasAmplios));

        // 2) Se guarda la necesidad principal (o se crea si el contrato no la tenia).
        if (necesidades.isEmpty()) {
            necesidadService.crear(necesidad(id, solicitud));
        } else {
            necesidadService.actualizar(necesidades.get(0).id(), necesidad(id, solicitud));
        }

        // 3) El contrato queda con los valores finales.
        operacionService.actualizar(id, operacion(solicitud, solicitud.estado(), vehiculosFinales, diasFinales));
        return obtenerPorId(id);
    }

    @Transactional
    public void eliminar(Long id) {
        operacionService.obtenerPorId(id);
        if (rutaRepository.existsByOperacionId(id)) {
            throw new ReglaDeNegocioException("No se puede eliminar la operacion porque tiene rutas registradas");
        }
        necesidadService.listar(id).forEach(n -> necesidadService.eliminar(n.id()));
        operacionService.eliminar(id);
    }

    private OperacionRequest operacion(PlanOperacionRequest s, EstadoOperacion estado, int vehiculos, Set<DiaSemana> dias) {
        return new OperacionRequest(s.codigoContrato(), s.clienteId(), s.nombreOperacion(), s.fechaInicio(), s.fechaFin(),
                DiasOperacion.describir(dias), vehiculos, s.numeroEntregasEsperadas(), s.requiereCadenaFrio(), estado);
    }

    private NecesidadClienteRequest necesidad(Long operacionId, PlanOperacionRequest s) {
        return new NecesidadClienteRequest(operacionId, s.tipoVehiculo(), s.cantidadVehiculos(), s.capacidadMinimaKg(),
                s.requiereCadenaFrio(), s.diasOperacion(), s.horaInicio(), s.horaFin(), s.observaciones());
    }

    private PlanOperacionResponse aResponse(OperacionResponse o, List<NecesidadClienteResponse> necesidades) {
        NecesidadClienteResponse n = necesidades.isEmpty() ? null : necesidades.get(0);
        List<DiaSemana> dias = n != null ? n.diasOperacion()
                : DiasOperacion.interpretar(o.diasOperacion()).map(d -> d.stream().sorted().toList()).orElse(List.of());
        return new PlanOperacionResponse(o.id(), o.codigoContrato(), o.clienteId(), o.razonSocialCliente(),
                o.nombreOperacion(), o.fechaInicio(), o.fechaFin(), o.estado(), o.numeroEntregasEsperadas(),
                o.requiereCadenaFrio(), o.diasOperacion(), dias, o.numeroVehiculosRequeridos(),
                n == null ? null : n.id(), n == null ? null : n.tipoVehiculo(),
                n == null ? o.numeroVehiculosRequeridos() : n.cantidadVehiculos(),
                n == null ? null : n.capacidadMinimaKg(), n == null ? null : n.horaInicio(),
                n == null ? null : n.horaFin(), n == null ? null : n.observaciones(),
                n == null ? null : n.vehiculosDisponibles(), n == null ? null : n.flotaSuficiente(),
                Math.max(0, necesidades.size() - 1));
    }
}
