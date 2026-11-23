package co.edu.usta.spacetrack.zonas.service;

import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.zonas.domain.Localidad;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.dto.LocalidadResponse;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Las zonas ya no se registran a mano: son las 19 localidades de Bogota y quedan guardadas en la base
 * al arrancar la aplicacion. Las rutas solo eligen una de ellas en una lista desplegable.
 * Las zonas registradas antes de este cambio se conservan (sus rutas siguen apuntando a ellas),
 * pero se marcan como fuera del catalogo para no ofrecerlas en rutas nuevas.
 */
@Service
@Transactional(readOnly = true)
public class ZonaService {

    private static final Logger log = LoggerFactory.getLogger(ZonaService.class);
    static final String PREFIJO_CATALOGO = "LOC-";

    private final ZonaRepository zonaRepository;

    public ZonaService(ZonaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
    }

    /** Primero las localidades del catalogo (en orden oficial) y luego las zonas antiguas. */
    public List<ZonaResponse> listar() {
        return zonaRepository.findAllByOrderByCodigoAsc().stream()
                .sorted((a, b) -> Boolean.compare(!esDelCatalogo(a), !esDelCatalogo(b)))
                .map(this::aResponse).toList();
    }

    public List<LocalidadResponse> localidades() {
        return Arrays.stream(Localidad.values())
                .map(l -> new LocalidadResponse(l.name(), l.getNombre(), l.getLatitud(), l.getLongitud()))
                .toList();
    }

    public ZonaResponse obtenerPorId(Long id) {
        return zonaRepository.findById(id).map(this::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una zona con id " + id));
    }

    /** Crea las zonas de las localidades de Bogota que falten. Es idempotente. */
    @Transactional
    public int asegurarCatalogo() {
        int creadas = 0;
        for (Localidad localidad : Localidad.deBogota()) {
            if (zonaRepository.findByCodigoIgnoreCase(localidad.codigoZona()).isEmpty()) {
                crearZonaDeCatalogo(localidad);
                creadas++;
            }
        }
        if (creadas > 0) {
            log.info("Catalogo de zonas: se registraron {} localidades de Bogota", creadas);
        }
        return creadas;
    }

    /** Zona predefinida de una localidad de Bogota; la crea si aun no existe. */
    @Transactional
    public Zona zonaDeLocalidad(Localidad localidad) {
        if (!localidad.isDeBogota()) {
            throw new IllegalArgumentException(localidad.getNombre() + " no es una localidad de Bogota");
        }
        return zonaRepository.findByCodigoIgnoreCase(localidad.codigoZona())
                .orElseGet(() -> crearZonaDeCatalogo(localidad));
    }

    private Zona crearZonaDeCatalogo(Localidad localidad) {
        Zona zona = new Zona();
        zona.setCodigo(localidad.codigoZona());
        // Si una zona antigua ya usa el nombre de la localidad, se evita el choque con el indice unico.
        String nombre = zonaRepository.existsByNombreIgnoreCase(localidad.getNombre())
                ? "Localidad " + localidad.getNumero() + " - " + localidad.getNombre() : localidad.getNombre();
        zona.setNombre(nombre);
        zona.setLocalidad(localidad.getNombre());
        zona.setDescripcion("Localidad " + localidad.getNumero() + " de Bogotá D.C.");
        zona.setActiva(true);
        return zonaRepository.save(zona);
    }

    static boolean esDelCatalogo(Zona zona) {
        return zona.getCodigo() != null && zona.getCodigo().toUpperCase().startsWith(PREFIJO_CATALOGO);
    }

    private ZonaResponse aResponse(Zona zona) {
        return new ZonaResponse(zona.getId(), zona.getCodigo(), zona.getNombre(), zona.getLocalidad(),
                zona.getDescripcion(), zona.isActiva(), esDelCatalogo(zona), zona.getFechaCreacion(), zona.getFechaActualizacion());
    }
}
