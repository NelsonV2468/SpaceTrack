package co.edu.usta.spacetrack.conductores.service;

import co.edu.usta.spacetrack.conductores.domain.Conductor;
import co.edu.usta.spacetrack.conductores.dto.ConductorRequest;
import co.edu.usta.spacetrack.conductores.dto.ConductorResponse;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConductorService {

    private final ConductorRepository conductorRepository;

    public ConductorService(ConductorRepository conductorRepository) {
        this.conductorRepository = conductorRepository;
    }

    public List<ConductorResponse> listar() {
        return conductorRepository.findAllByOrderByApellidosAscNombresAsc().stream().map(this::aResponse).toList();
    }

    public ConductorResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public ConductorResponse crear(ConductorRequest solicitud) {
        validarDatosUnicos(solicitud, null);
        Conductor conductor = new Conductor();
        copiarDatos(solicitud, conductor);
        return aResponse(conductorRepository.save(conductor));
    }

    @Transactional
    public ConductorResponse actualizar(Long id, ConductorRequest solicitud) {
        Conductor conductor = buscarEntidad(id);
        validarDatosUnicos(solicitud, id);
        copiarDatos(solicitud, conductor);
        return aResponse(conductorRepository.save(conductor));
    }

    @Transactional
    public void eliminar(Long id) {
        conductorRepository.delete(buscarEntidad(id));
    }

    private Conductor buscarEntidad(Long id) {
        return conductorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un conductor con id " + id));
    }

    private void validarDatosUnicos(ConductorRequest solicitud, Long id) {
        boolean documentoExiste = id == null ? conductorRepository.existsByNumeroDocumento(solicitud.numeroDocumento())
                : conductorRepository.existsByNumeroDocumentoAndIdNot(solicitud.numeroDocumento(), id);
        boolean correoExiste = id == null ? conductorRepository.existsByCorreoIgnoreCase(solicitud.correo())
                : conductorRepository.existsByCorreoIgnoreCaseAndIdNot(solicitud.correo(), id);
        boolean licenciaExiste = id == null ? conductorRepository.existsByNumeroLicenciaIgnoreCase(solicitud.numeroLicencia())
                : conductorRepository.existsByNumeroLicenciaIgnoreCaseAndIdNot(solicitud.numeroLicencia(), id);
        if (documentoExiste) throw new ReglaDeNegocioException("Ya existe un conductor registrado con ese documento");
        if (correoExiste) throw new ReglaDeNegocioException("Ya existe un conductor registrado con ese correo");
        if (licenciaExiste) throw new ReglaDeNegocioException("Ya existe un conductor registrado con esa licencia");
    }

    private void copiarDatos(ConductorRequest solicitud, Conductor conductor) {
        conductor.setTipoDocumento(solicitud.tipoDocumento().trim());
        conductor.setNumeroDocumento(solicitud.numeroDocumento().trim());
        conductor.setNombres(solicitud.nombres().trim());
        conductor.setApellidos(solicitud.apellidos().trim());
        conductor.setTelefono(solicitud.telefono().trim());
        conductor.setCorreo(solicitud.correo().trim().toLowerCase());
        conductor.setNumeroLicencia(solicitud.numeroLicencia().trim().toUpperCase());
        conductor.setFechaVencimientoLicencia(solicitud.fechaVencimientoLicencia());
        conductor.setActivo(solicitud.activo());
    }

    private ConductorResponse aResponse(Conductor conductor) {
        return new ConductorResponse(conductor.getId(), conductor.getTipoDocumento(), conductor.getNumeroDocumento(),
                conductor.getNombres(), conductor.getApellidos(), conductor.getTelefono(), conductor.getCorreo(),
                conductor.getNumeroLicencia(), conductor.getFechaVencimientoLicencia(), conductor.isActivo(),
                conductor.getFechaCreacion(), conductor.getFechaActualizacion());
    }
}
