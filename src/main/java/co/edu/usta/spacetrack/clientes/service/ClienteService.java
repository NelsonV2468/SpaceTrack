package co.edu.usta.spacetrack.clientes.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.dto.ClienteResponse;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByRazonSocialAsc().stream().map(this::aResponse).toList();
    }

    public ClienteResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest solicitud) {
        validarUnicidad(solicitud, null);
        Cliente cliente = new Cliente();
        copiarDatos(solicitud, cliente);
        return aResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest solicitud) {
        Cliente cliente = buscarEntidad(id);
        validarUnicidad(solicitud, id);
        copiarDatos(solicitud, cliente);
        return aResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        clienteRepository.delete(buscarEntidad(id));
    }

    private Cliente buscarEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id));
    }

    private void validarUnicidad(ClienteRequest solicitud, Long id) {
        boolean nitExiste = id == null ? clienteRepository.existsByNit(solicitud.nit())
                : clienteRepository.existsByNitAndIdNot(solicitud.nit(), id);
        if (nitExiste) {
            throw new ReglaDeNegocioException("Ya existe un cliente registrado con ese NIT");
        }
        boolean correoExiste = id == null ? clienteRepository.existsByCorreoContactoIgnoreCase(solicitud.correoContacto())
                : clienteRepository.existsByCorreoContactoIgnoreCaseAndIdNot(solicitud.correoContacto(), id);
        if (correoExiste) {
            throw new ReglaDeNegocioException("Ya existe un cliente registrado con ese correo de contacto");
        }
    }

    private void copiarDatos(ClienteRequest solicitud, Cliente cliente) {
        cliente.setRazonSocial(solicitud.razonSocial().trim());
        cliente.setNit(solicitud.nit().trim());
        cliente.setNombreContacto(solicitud.nombreContacto().trim());
        cliente.setCorreoContacto(solicitud.correoContacto().trim().toLowerCase());
        cliente.setTelefonoContacto(solicitud.telefonoContacto().trim());
        cliente.setDireccion(solicitud.direccion().trim());
        cliente.setActivo(solicitud.activo());
    }

    private ClienteResponse aResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getRazonSocial(), cliente.getNit(),
                cliente.getNombreContacto(), cliente.getCorreoContacto(), cliente.getTelefonoContacto(),
                cliente.getDireccion(), cliente.isActivo(), cliente.getFechaCreacion(), cliente.getFechaActualizacion());
    }
}
