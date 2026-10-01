package co.edu.usta.spacetrack.operaciones.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.OperacionResponse;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OperacionService {

    private final OperacionRepository operacionRepository;
    private final ClienteRepository clienteRepository;

    public OperacionService(OperacionRepository operacionRepository, ClienteRepository clienteRepository) {
        this.operacionRepository = operacionRepository;
        this.clienteRepository = clienteRepository;
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
        Operacion operacion = new Operacion();
        copiarDatos(solicitud, operacion);
        return aResponse(operacionRepository.save(operacion));
    }

    @Transactional
    public OperacionResponse actualizar(Long id, OperacionRequest solicitud) {
        Operacion operacion = buscarEntidad(id);
        validarCodigoUnico(solicitud.codigoContrato(), id);
        copiarDatos(solicitud, operacion);
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
        boolean codigoExiste = id == null ? operacionRepository.existsByCodigoContratoIgnoreCase(codigoContrato)
                : operacionRepository.existsByCodigoContratoIgnoreCaseAndIdNot(codigoContrato, id);
        if (codigoExiste) {
            throw new ReglaDeNegocioException("Ya existe una operacion registrada con ese codigo de contrato");
        }
    }

    private void copiarDatos(OperacionRequest solicitud, Operacion operacion) {
        operacion.setCodigoContrato(solicitud.codigoContrato().trim().toUpperCase());
        operacion.setCliente(buscarCliente(solicitud.clienteId()));
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
