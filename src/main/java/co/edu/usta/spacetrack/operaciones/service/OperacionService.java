package co.edu.usta.spacetrack.operaciones.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.DiasOperacion;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.OperacionResponse;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OperacionService {

    private final OperacionRepository operacionRepository;
    private final ClienteRepository clienteRepository;
    private final RutaRepository rutaRepository;
    private final NecesidadClienteRepository necesidadRepository;
    private final Clock reloj;

    public OperacionService(OperacionRepository operacionRepository, ClienteRepository clienteRepository,
                            RutaRepository rutaRepository, NecesidadClienteRepository necesidadRepository, Clock reloj) {
        this.operacionRepository = operacionRepository;
        this.clienteRepository = clienteRepository;
        this.rutaRepository = rutaRepository;
        this.necesidadRepository = necesidadRepository;
        this.reloj = reloj;
    }

    public List<OperacionResponse> listar() {
        return operacionRepository.findAllByOrderByFechaInicioDesc().stream().map(this::aResponse).toList();
    }

    public OperacionResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public OperacionResponse crear(OperacionRequest solicitud) {
        validarCodigoUnico(solicitud.codigoContrato(), null);
        Cliente cliente = buscarCliente(solicitud.clienteId());
        validarDatos(solicitud, cliente);
        Operacion operacion = new Operacion();
        copiarDatos(solicitud, cliente, operacion);
        return aResponse(operacionRepository.save(operacion));
    }

    @Transactional
    public OperacionResponse actualizar(Long id, OperacionRequest solicitud) {
        Operacion operacion = buscarEntidad(id);
        validarCodigoUnico(solicitud.codigoContrato(), id);
        Cliente cliente = buscarCliente(solicitud.clienteId());
        validarCambioDeEstado(operacion, solicitud.estado());
        validarDatos(solicitud, cliente);
        validarCuposRegistrados(operacion, solicitud);
        copiarDatos(solicitud, cliente, operacion);
        return aResponse(operacionRepository.save(operacion));
    }

    @Transactional
    public void eliminar(Long id) {
        operacionRepository.delete(buscarEntidad(id));
    }

    private Operacion buscarEntidad(Long id) {
        return operacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una operacion con id " + id));
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + clienteId));
    }

    private void validarCodigoUnico(String codigoContrato, Long id) {
        String codigo = codigoContrato.trim();
        boolean codigoExiste = id == null ? operacionRepository.existsByCodigoContratoIgnoreCase(codigo)
                : operacionRepository.existsByCodigoContratoIgnoreCaseAndIdNot(codigo, id);
        if (codigoExiste) {
            throw new ReglaDeNegocioException("Ya existe una operacion registrada con ese codigo de contrato");
        }
    }

    // ACTIVA y PAUSADA se pueden alternar (pausar y reactivar); FINALIZADA cierra el contrato.
    private void validarCambioDeEstado(Operacion operacion, EstadoOperacion nuevo) {
        if (operacion.getEstado() == EstadoOperacion.FINALIZADA && nuevo != EstadoOperacion.FINALIZADA) {
            throw new ReglaDeNegocioException("Una operacion finalizada no se puede reabrir");
        }
        if (nuevo == EstadoOperacion.FINALIZADA && operacion.getEstado() != EstadoOperacion.FINALIZADA
                && rutaRepository.existsByOperacionIdAndEstado(operacion.getId(), EstadoRuta.EN_CURSO)) {
            throw new ReglaDeNegocioException("No se puede finalizar la operacion mientras tenga rutas en curso");
        }
    }

    private void validarDatos(OperacionRequest solicitud, Cliente cliente) {
        boolean abierta = solicitud.estado() != EstadoOperacion.FINALIZADA;
        // Un cliente inactivo no puede tener contratos vigentes; solo se le pueden cerrar.
        if (abierta && !cliente.isActivo()) {
            throw new ReglaDeNegocioException("El cliente " + cliente.getRazonSocial()
                    + " esta inactivo; solo se le pueden registrar operaciones finalizadas");
        }
        // Una operacion vencida debe ampliar su vigencia o marcarse como finalizada.
        if (abierta && solicitud.fechaFin().isBefore(LocalDate.now(reloj))) {
            throw new ReglaDeNegocioException("La fecha final ya paso; amplie la vigencia o marque la operacion como FINALIZADA");
        }
        if (DiasOperacion.interpretar(solicitud.diasOperacion()).isEmpty()) {
            throw new ReglaDeNegocioException("No se reconocen los dias de operacion '" + solicitud.diasOperacion().trim()
                    + "'. Escriba por ejemplo " + DiasOperacion.EJEMPLOS);
        }
    }

    // El contrato no puede quedar por debajo de lo que ya tienen planeado sus rutas y necesidades.
    private void validarCuposRegistrados(Operacion operacion, OperacionRequest solicitud) {
        long entregasProgramadas = rutaRepository.sumarEntregasProgramadasPorOperacion(operacion.getId());
        if (solicitud.numeroEntregasEsperadas() < entregasProgramadas) {
            throw new ReglaDeNegocioException("Las rutas del contrato ya programan " + entregasProgramadas
                    + " entregas; las entregas esperadas no pueden ser menos");
        }
        long vehiculosEnNecesidades = necesidadRepository.sumarVehiculosPorOperacion(operacion.getId());
        if (solicitud.numeroVehiculosRequeridos() < vehiculosEnNecesidades) {
            throw new ReglaDeNegocioException("Las necesidades del contrato ya suman " + vehiculosEnNecesidades
                    + " vehiculos; los vehiculos requeridos no pueden ser menos");
        }
    }

    private void copiarDatos(OperacionRequest solicitud, Cliente cliente, Operacion operacion) {
        operacion.setCodigoContrato(solicitud.codigoContrato().trim().toUpperCase());
        operacion.setCliente(cliente);
        operacion.setNombreOperacion(solicitud.nombreOperacion().trim());
        operacion.setFechaInicio(solicitud.fechaInicio());
        operacion.setFechaFin(solicitud.fechaFin());
        operacion.setDiasOperacion(solicitud.diasOperacion().trim());
        operacion.setNumeroVehiculosRequeridos(solicitud.numeroVehiculosRequeridos());
        operacion.setNumeroEntregasEsperadas(solicitud.numeroEntregasEsperadas());
        operacion.setRequiereCadenaFrio(solicitud.requiereCadenaFrio());
        operacion.setEstado(solicitud.estado());
    }

    private OperacionResponse aResponse(Operacion operacion) {
        Cliente cliente = operacion.getCliente();
        return new OperacionResponse(operacion.getId(), operacion.getCodigoContrato(), cliente.getId(),
                cliente.getRazonSocial(), operacion.getNombreOperacion(), operacion.getFechaInicio(), operacion.getFechaFin(),
                operacion.getDiasOperacion(), operacion.getNumeroVehiculosRequeridos(), operacion.getNumeroEntregasEsperadas(),
                operacion.isRequiereCadenaFrio(), operacion.getEstado(), operacion.getFechaCreacion(), operacion.getFechaActualizacion());
    }
}
