package co.edu.usta.spacetrack.zonas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Al arrancar deja guardadas en la base las 19 localidades de Bogota como zonas. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogoZonasInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoZonasInicial.class);

    private final ZonaService zonaService;

    public CatalogoZonasInicial(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        try {
            zonaService.asegurarCatalogo();
        } catch (RuntimeException error) {
            // Nunca impide el arranque; las zonas que falten se crean cuando se necesiten.
            log.error("No se pudo registrar el catalogo de zonas: {}", error.getMessage(), error);
        }
    }
}
