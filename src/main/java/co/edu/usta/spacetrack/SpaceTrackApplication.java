package co.edu.usta.spacetrack;

import co.edu.usta.spacetrack.shared.config.RelojConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SpaceTrackApplication {

    public static void main(String[] args) {
        // Las fechas de auditoria (LocalDateTime.now()) quedan en la misma zona que el Clock de la aplicacion.
        TimeZone.setDefault(TimeZone.getTimeZone(RelojConfig.ZONA_OPERACION));
        SpringApplication.run(SpaceTrackApplication.class, args);
    }
}
