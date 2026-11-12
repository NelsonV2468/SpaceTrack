package co.edu.usta.spacetrack.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

    // La operacion es local: horarios, vigencias y dias de los contratos se evaluan en hora de Bogota,
    // sin importar la zona horaria del servidor.
    public static final ZoneId ZONA_OPERACION = ZoneId.of("America/Bogota");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_OPERACION);
    }
}
