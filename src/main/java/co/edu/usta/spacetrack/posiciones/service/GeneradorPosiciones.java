package co.edu.usta.spacetrack.posiciones.service;

import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;
import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.UbicacionRutaResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.rutas.service.SimuladorUbicacion;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Guarda periodicamente la posicion simulada de cada ruta EN_CURSO. No hay GPS real.
 */
@Component
public class GeneradorPosiciones {

    private final PosicionRutaRepository posicionRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final SimuladorUbicacion simuladorUbicacion;
    private final Clock reloj;

    public GeneradorPosiciones(PosicionRutaRepository posicionRepository, RutaRepository rutaRepository,
                               EntregaRepository entregaRepository, SimuladorUbicacion simuladorUbicacion,
                               Clock reloj) {
        this.posicionRepository = posicionRepository;
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.simuladorUbicacion = simuladorUbicacion;
        this.reloj = reloj;
    }

    // La transaccion mantiene abierta la sesion para las relaciones lazy que lee el simulador.
    @Scheduled(fixedDelayString = "${spacetrack.simulacion.intervalo-segundos}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void generar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (Ruta ruta : rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO)) {
            UbicacionRutaResponse ubicacion = simuladorUbicacion.calcular(ruta,
                    entregaRepository.findByRutaIdOrderByIdAsc(ruta.getId()), ahora);
            PosicionRuta posicion = new PosicionRuta();
            posicion.setRuta(ruta);
            posicion.setLatitud(ubicacion.vehiculo().latitud());
            posicion.setLongitud(ubicacion.vehiculo().longitud());
            posicion.setVelocidadKmh(ubicacion.velocidadKmh());
            posicion.setOrigen(OrigenPosicion.SIMULADA);
            posicion.setFechaRegistro(ahora);
            posicionRepository.save(posicion);
        }
    }
}
