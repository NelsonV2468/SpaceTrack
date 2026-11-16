package co.edu.usta.spacetrack.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Desarrollo local (perfil dev): sin login, igual que antes del despliegue en la nube. */
@Configuration
@ConditionalOnProperty(name = "spacetrack.seguridad.habilitada", havingValue = "false", matchIfMissing = true)
public class SeguridadDesactivadaConfig {

    private static final Logger log = LoggerFactory.getLogger(SeguridadDesactivadaConfig.class);

    @Bean
    SecurityFilterChain cadenaSinSeguridad(HttpSecurity http, Environment entorno) throws Exception {
        if (entorno.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("El perfil prod exige spacetrack.seguridad.habilitada=true");
        }
        log.warn("Seguridad DESACTIVADA: solo para desarrollo local. No exponga esta instancia a internet.");
        return http
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .build();
    }
}
