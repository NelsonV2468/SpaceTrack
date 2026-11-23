package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;
import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Detecta si el vehiculo de una ruta EN_CURSO se salio del recorrido planeado.
 *
 * <p>Se compara la ultima posicion reportada por el vehiculo (origen MANUAL: GPS del conductor o registro del
 * operador) con el recorrido planeado (centro de distribucion, paradas y destino). Solo cuenta un reporte
 * reciente: si el vehiculo deja de reportar, vuelve a mostrarse la posicion simulada.</p>
 */
@Component
public class DetectorDesvio {

    private final PosicionRutaRepository posicionRepository;
    private final SimuladorUbicacion simulador;
    private final double umbralKm;
    private final double umbralCriticoKm;
    private final long vigenciaMinutos;

    public DetectorDesvio(PosicionRutaRepository posicionRepository, SimuladorUbicacion simulador,
                          @Value("${spacetrack.alertas.desvio-km:0.5}") double umbralKm,
                          @Value("${spacetrack.alertas.desvio-critico-km:2.0}") double umbralCriticoKm,
                          @Value("${spacetrack.posiciones.vigencia-reporte-minutos:30}") long vigenciaMinutos) {
        this.posicionRepository = posicionRepository;
        this.simulador = simulador;
        this.umbralKm = umbralKm;
        this.umbralCriticoKm = umbralCriticoKm;
        this.vigenciaMinutos = vigenciaMinutos;
    }

    /** Posicion reportada por el vehiculo y cuanto se aleja del recorrido planeado. */
    public record PosicionReportada(double latitud, double longitud, Integer velocidadKmh, LocalDateTime reportadaEn,
                                    double distanciaKm, boolean fueraDeRuta, boolean critico) { }

    /** Ultimo reporte vigente de una ruta EN_CURSO, si lo hay. */
    public Optional<PosicionReportada> evaluar(Ruta ruta, LocalDateTime ahora) {
        if (ruta.getEstado() != EstadoRuta.EN_CURSO || ruta.getId() == null) {
            return Optional.empty();
        }
        return posicionRepository.findFirstByRutaIdAndOrigenOrderByIdDesc(ruta.getId(), OrigenPosicion.MANUAL)
                .filter(p -> vigente(p, ruta, ahora))
                .map(p -> {
                    double distancia = SimuladorUbicacion.distanciaAlRecorridoKm(simulador.recorridoPlaneado(ruta),
                            p.getLatitud(), p.getLongitud());
                    return new PosicionReportada(p.getLatitud(), p.getLongitud(), p.getVelocidadKmh(),
                            p.getFechaRegistro(), distancia, distancia > umbralKm, distancia > umbralCriticoKm);
                });
    }

    /** Hay un reporte reciente: el vehiculo esta enviando su posicion y no hace falta simularla. */
    public boolean tieneReporteVigente(Ruta ruta, LocalDateTime ahora) {
        return ruta.getId() != null && posicionRepository
                .findFirstByRutaIdAndOrigenOrderByIdDesc(ruta.getId(), OrigenPosicion.MANUAL)
                .filter(p -> vigente(p, ruta, ahora)).isPresent();
    }

    public double getUmbralKm() {
        return umbralKm;
    }

    // Un reporte anterior al inicio de la ruta (de otra jornada) no cuenta.
    private boolean vigente(PosicionRuta posicion, Ruta ruta, LocalDateTime ahora) {
        LocalDateTime fecha = posicion.getFechaRegistro();
        if (fecha == null || fecha.isBefore(ahora.minusMinutes(vigenciaMinutos))) {
            return false;
        }
        return ruta.getFechaInicioReal() == null || !fecha.isBefore(ruta.getFechaInicioReal());
    }
}
