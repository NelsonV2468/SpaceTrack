package co.edu.usta.spacetrack.zonas.service;

import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.dto.ZonaRequest;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final RutaRepository rutaRepository;

    public ZonaService(ZonaRepository zonaRepository, RutaRepository rutaRepository) {
        this.zonaRepository = zonaRepository;
        this.rutaRepository = rutaRepository;
    }

    public List<ZonaResponse> listar() {
        return zonaRepository.findAllByOrderByNombreAsc().stream().map(this::aResponse).toList();
    }

    public ZonaResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public ZonaResponse crear(ZonaRequest solicitud) {
        validarDatosUnicos(solicitud, null);
        Zona zona = new Zona();
        copiarDatos(solicitud, zona);
        return aResponse(zonaRepository.save(zona));
    }

    @Transactional
    public ZonaResponse actualizar(Long id, ZonaRequest solicitud) {
        Zona zona = buscarEntidad(id);
        validarDatosUnicos(solicitud, id);
        copiarDatos(solicitud, zona);
        return aResponse(zonaRepository.save(zona));
    }

    @Transactional
    public void eliminar(Long id) {
        Zona zona = buscarEntidad(id);
        // Una zona con rutas asociadas no se elimina para no dejar rutas huerfanas.
        if (rutaRepository.existsByZonaId(id)) {
            throw new ReglaDeNegocioException("No se puede eliminar la zona porque tiene rutas asociadas");
        }
        zonaRepository.delete(zona);
    }

    private Zona buscarEntidad(Long id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una zona con id " + id));
    }

    private void validarDatosUnicos(ZonaRequest solicitud, Long id) {
        String codigo = solicitud.codigo().trim();
        String nombre = solicitud.nombre().trim();
        boolean codigoExiste = id == null ? zonaRepository.existsByCodigoIgnoreCase(codigo)
                : zonaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id);
        if (codigoExiste) {
            throw new ReglaDeNegocioException("Ya existe una zona registrada con ese codigo");
        }
        boolean nombreExiste = id == null ? zonaRepository.existsByNombreIgnoreCase(nombre)
                : zonaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (nombreExiste) {
            throw new ReglaDeNegocioException("Ya existe una zona registrada con ese nombre");
        }
    }

    private void copiarDatos(ZonaRequest solicitud, Zona zona) {
        zona.setCodigo(solicitud.codigo().trim().toUpperCase());
        zona.setNombre(solicitud.nombre().trim());
        zona.setLocalidad(solicitud.localidad().trim());
        zona.setDescripcion(solicitud.descripcion() == null || solicitud.descripcion().isBlank() ? null : solicitud.descripcion().trim());
        zona.setActiva(solicitud.activa());
    }

    private ZonaResponse aResponse(Zona zona) {
        return new ZonaResponse(zona.getId(), zona.getCodigo(), zona.getNombre(), zona.getLocalidad(),
                zona.getDescripcion(), zona.isActiva(), zona.getFechaCreacion(), zona.getFechaActualizacion());
    }
}
