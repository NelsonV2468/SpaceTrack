package co.edu.usta.spacetrack.necesidades.service;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteRequest;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteResponse;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.DiasOperacion;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NecesidadClienteService {

    private final NecesidadClienteRepository necesidadRepository;
    private final OperacionRepository operacionRepository;
    private final VehiculoRepository vehiculoRepository;

    public NecesidadClienteService(NecesidadClienteRepository necesidadRepository, OperacionRepository operacionRepository,
                                   VehiculoRepository vehiculoRepository) {
        this.necesidadRepository = necesidadRepository;
        this.operacionRepository = operacionRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<NecesidadClienteResponse> listar(Long operacionId) {
        List<NecesidadCliente> necesidades = operacionId == null ? necesidadRepository.findAllByOrderByIdAsc()
                : necesidadRepository.findByOperacionIdOrderByIdAsc(operacionId);
        return necesidades.stream().map(this::aResponse).toList();
    }

    public NecesidadClienteResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public NecesidadClienteResponse crear(NecesidadClienteRequest solicitud) {
        Operacion operacion = buscarOperacionVigente(solicitud.operacionId());
        validarCupoDeVehiculos(operacion, solicitud.cantidadVehiculos(), null);
        validarDiasDentroDelContrato(operacion, solicitud.diasOperacion());
        NecesidadCliente necesidad = new NecesidadCliente();
        copiarDatos(solicitud, operacion, necesidad);
        return aResponse(necesidadRepository.save(necesidad));
    }

    @Transactional
    public NecesidadClienteResponse actualizar(Long id, NecesidadClienteRequest solicitud) {
        NecesidadCliente necesidad = buscarEntidad(id);
        Operacion operacion = buscarOperacionVigente(solicitud.operacionId());
        validarCupoDeVehiculos(operacion, solicitud.cantidadVehiculos(), id);
        validarDiasDentroDelContrato(operacion, solicitud.diasOperacion());
        copiarDatos(solicitud, operacion, necesidad);
        return aResponse(necesidadRepository.save(necesidad));
    }

    @Transactional
    public void eliminar(Long id) {
        necesidadRepository.delete(buscarEntidad(id));
    }

    private NecesidadCliente buscarEntidad(Long id) {
        return necesidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una necesidad con id " + id));
    }

    private Operacion buscarOperacionVigente(Long operacionId) {
        Operacion operacion = operacionRepository.findById(operacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una operacion con id " + operacionId));
        if (operacion.getEstado() == EstadoOperacion.FINALIZADA) {
            throw new ReglaDeNegocioException("No se pueden registrar necesidades en una operacion finalizada");
        }
        return operacion;
    }

    // Lo que pide el cliente no puede superar los vehiculos pactados en el contrato.
    private void validarCupoDeVehiculos(Operacion operacion, Integer cantidadSolicitada, Long necesidadId) {
        long yaRegistrados = necesidadId == null ? necesidadRepository.sumarVehiculosPorOperacion(operacion.getId())
                : necesidadRepository.sumarVehiculosPorOperacionExcluyendo(operacion.getId(), necesidadId);
        if (yaRegistrados + cantidadSolicitada > operacion.getNumeroVehiculosRequeridos()) {
            throw new ReglaDeNegocioException("La operacion solo admite " + operacion.getNumeroVehiculosRequeridos()
                    + " vehiculos y ya tiene " + yaRegistrados + " registrados en sus necesidades");
        }
    }

    // La necesidad detalla el horario del contrato; no puede operar en dias que el contrato no cubre.
    private void validarDiasDentroDelContrato(Operacion operacion, Set<DiaSemana> dias) {
        Set<DiaSemana> diasContrato = DiasOperacion.interpretar(operacion.getDiasOperacion())
                .orElseThrow(() -> new ReglaDeNegocioException("Corrija primero los dias de operacion del contrato "
                        + operacion.getCodigoContrato() + "; escriba por ejemplo " + DiasOperacion.EJEMPLOS));
        List<DiaSemana> fuera = dias.stream().filter(d -> !diasContrato.contains(d)).sorted().toList();
        if (!fuera.isEmpty()) {
            throw new ReglaDeNegocioException("El contrato opera " + operacion.getDiasOperacion()
                    + "; no incluye " + String.join(", ", fuera.stream().map(Enum::name).toList()));
        }
    }

    private void copiarDatos(NecesidadClienteRequest solicitud, Operacion operacion, NecesidadCliente necesidad) {
        necesidad.setOperacion(operacion);
        necesidad.setTipoVehiculo(solicitud.tipoVehiculo().trim());
        necesidad.setCantidadVehiculos(solicitud.cantidadVehiculos());
        necesidad.setCapacidadMinimaKg(solicitud.capacidadMinimaKg());
        necesidad.setRequiereCadenaFrio(solicitud.requiereCadenaFrio());
        necesidad.setDiasOperacion(solicitud.diasOperacion());
        necesidad.setHoraInicio(solicitud.horaInicio());
        necesidad.setHoraFin(solicitud.horaFin());
        necesidad.setObservaciones(solicitud.observaciones() == null || solicitud.observaciones().isBlank() ? null : solicitud.observaciones().trim());
    }

    private NecesidadClienteResponse aResponse(NecesidadCliente necesidad) {
        Operacion operacion = necesidad.getOperacion();
        long disponibles = vehiculoRepository.contarDisponibles(necesidad.getTipoVehiculo(),
                necesidad.getCapacidadMinimaKg(), necesidad.isRequiereCadenaFrio());
        return new NecesidadClienteResponse(necesidad.getId(), operacion.getId(), operacion.getCodigoContrato(),
                operacion.getCliente().getRazonSocial(), necesidad.getTipoVehiculo(), necesidad.getCantidadVehiculos(),
                necesidad.getCapacidadMinimaKg(), necesidad.isRequiereCadenaFrio(),
                necesidad.getDiasOperacion().stream().sorted().toList(), necesidad.getHoraInicio(), necesidad.getHoraFin(),
                necesidad.getObservaciones(), disponibles, disponibles >= necesidad.getCantidadVehiculos(),
                necesidad.getFechaCreacion(), necesidad.getFechaActualizacion());
    }
}
