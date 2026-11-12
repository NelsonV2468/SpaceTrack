package co.edu.usta.spacetrack.posiciones.service;

import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Aplica la politica de retencion: las posiciones mas antiguas que
 * {@code spacetrack.posiciones.retencion-dias} se eliminan cada madrugada.
 */
@Component
public class PurgaPosiciones {

    private static final Logger log = LoggerFactory.getLogger(PurgaPosiciones.class);

    private final PosicionRutaRepository posicionRepository;
    private final Clock reloj;
    private final int diasRetencion;

    public PurgaPosiciones(PosicionRutaRepository posicionRepository, Clock reloj,
                           @Value("${spacetrack.posiciones.retencion-dias:30}") int diasRetencion) {
        this.posicionRepository = posicionRepository;
        this.reloj = reloj;
        this.diasRetencion = diasRetencion;
    }

    @Scheduled(cron = "${spacetrack.posiciones.purga-cron:0 30 2 * * *}", zone = "America/Bogota")
    @Transactional
    public int purgar() {
        LocalDateTime limite = LocalDateTime.now(reloj).minusDays(diasRetencion);
        int eliminadas = posicionRepository.eliminarAnterioresA(limite);
        if (eliminadas > 0) {
            log.info("Se eliminaron {} posiciones anteriores a {}", eliminadas, limite);
        }
        return eliminadas;
    }
}
