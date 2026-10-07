package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RutaService {

    private final RutaRepository rutaRepository;
    private final ZonaRepository zonaRepository;
    private final OperacionRepository operacionRepository;

    public RutaService(RutaRepository rutaRepository, ZonaRepository zonaRepository, OperacionRepository operacionRepository) {
        this.rutaRepository = rutaRepository;
        this.zonaRepository = zonaRepository;
        this.operacionRepository = operacionRepository;
    }

    public List<RutaResponse> listar() {
        return rutaRepository.findAllByOrderByCodigoAsc().stream().map(this::aResponse).toList();
    }

    public RutaResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public RutaResponse crear(RutaRequest solicitud) {
        validarCodigoUnico(solicitud.codigo(), null);
        Ruta ruta = new Ruta();
        copiarDatos(solicitud, ruta);
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse actualizar(Long id, RutaRequest solicitud) {
        Ruta ruta = buscarEntidad(id);
        validarCodigoUnico(solicitud.codigo(), id);
        copiarDatos(solicitud, ruta);
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public void eliminar(Long id) {
        rutaRepository.delete(buscarEntidad(id));
    }

    private Ruta buscarEntidad(Long id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una ruta con id " + id));
    }

    private Zona buscarZonaActiva(Long zonaId) {
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una zona con id " + zonaId));
        if (!zona.isActiva()) {
            throw new ReglaDeNegocioException("No se pueden asignar rutas a una zona inactiva");
        }
        return zona;
    }

    private Operacion buscarOperacionVigente(Long operacionId) {
        Operacion operacion = operacionRepository.findById(operacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una operacion con id " + operacionId));
        if (operacion.getEstado() == EstadoOperacion.FINALIZADA) {
            throw new ReglaDeNegocioException("No se pueden crear rutas para una operacion finalizada");
        }
        return operacion;
    }

    private void validarCodigoUnico(String codigo, Long id) {
        String codigoLimpio = codigo.trim();
        boolean codigoExiste = id == null ? rutaRepository.existsByCodigoIgnoreCase(codigoLimpio)
                : rutaRepository.existsByCodigoIgnoreCaseAndIdNot(codigoLimpio, id);
        if (codigoExiste) {
            throw new ReglaDeNegocioException("Ya existe una ruta registrada con ese codigo");
        }
    }

    private void copiarDatos(RutaRequest solicitud, Ruta ruta) {
        ruta.setCodigo(solicitud.codigo().trim().toUpperCase());
        ruta.setNombre(solicitud.nombre().trim());
        ruta.setZona(buscarZonaActiva(solicitud.zonaId()));
        ruta.setOperacion(buscarOperacionVigente(solicitud.operacionId()));
        ruta.setPuntoOrigen(solicitud.puntoOrigen().trim());
        ruta.setPuntoDestino(solicitud.puntoDestino().trim());
        ruta.setDistanciaEstimadaKm(solicitud.distanciaEstimadaKm());
        ruta.setNumeroEntregasProgramadas(solicitud.numeroEntregasProgramadas());
    }

    private RutaResponse aResponse(Ruta ruta) {
        Zona zona = ruta.getZona();
        Operacion operacion = ruta.getOperacion();
        return new RutaResponse(ruta.getId(), ruta.getCodigo(), ruta.getNombre(), zona.getId(), zona.getNombre(),
                operacion.getId(), operacion.getCodigoContrato(), ruta.getPuntoOrigen(), ruta.getPuntoDestino(),
                ruta.getDistanciaEstimadaKm(), ruta.getNumeroEntregasProgramadas(), ruta.getFechaCreacion(),
                ruta.getFechaActualizacion());
    }
}
