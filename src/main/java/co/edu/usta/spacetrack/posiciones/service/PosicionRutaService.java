package co.edu.usta.spacetrack.posiciones.service;

import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;
import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaRequest;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaResponse;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PosicionRutaService {

    // Evita respuestas desproporcionadas al consultar el historial.
    static final int LIMITE_MAXIMO = 1000;

    private final PosicionRutaRepository posicionRepository;
    private final RutaRepository rutaRepository;
    private final Clock reloj;

    public PosicionRutaService(PosicionRutaRepository posicionRepository, RutaRepository rutaRepository, Clock reloj) {
        this.posicionRepository = posicionRepository;
        this.rutaRepository = rutaRepository;
        this.reloj = reloj;
    }

    public List<PosicionRutaResponse> listar(Long rutaId, int limite) {
        if (limite <= 0 || limite > LIMITE_MAXIMO) {
            throw new ReglaDeNegocioException("El limite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        if (rutaId != null && !rutaRepository.existsById(rutaId)) {
            throw rutaInexistente(rutaId);
        }
        List<PosicionRuta> posiciones = rutaId == null
                ? posicionRepository.findAllByOrderByIdDesc(Limit.of(limite))
                : posicionRepository.findByRutaIdOrderByIdDesc(rutaId, Limit.of(limite));
        return posiciones.stream().map(this::aResponse).toList();
    }

    @Transactional
    public PosicionRutaResponse registrar(PosicionRutaRequest solicitud) {
        Ruta ruta = rutaRepository.findById(solicitud.rutaId())
                .orElseThrow(() -> rutaInexistente(solicitud.rutaId()));
        if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaDeNegocioException("Solo se registran posiciones en rutas EN_CURSO; la ruta esta "
                    + ruta.getEstado());
        }
        PosicionRuta posicion = new PosicionRuta();
        posicion.setRuta(ruta);
        posicion.setLatitud(solicitud.latitud());
        posicion.setLongitud(solicitud.longitud());
        posicion.setVelocidadKmh(solicitud.velocidadKmh());
        posicion.setOrigen(OrigenPosicion.MANUAL);
        posicion.setFechaRegistro(LocalDateTime.now(reloj));
        return aResponse(posicionRepository.save(posicion));
    }

    private RecursoNoEncontradoException rutaInexistente(Long rutaId) {
        return new RecursoNoEncontradoException("No existe una ruta con id " + rutaId);
    }

    private PosicionRutaResponse aResponse(PosicionRuta posicion) {
        Ruta ruta = posicion.getRuta();
        return new PosicionRutaResponse(posicion.getId(), ruta.getId(), ruta.getCodigo(), posicion.getLatitud(),
                posicion.getLongitud(), posicion.getVelocidadKmh(), posicion.getOrigen(), posicion.getFechaRegistro());
    }
}
