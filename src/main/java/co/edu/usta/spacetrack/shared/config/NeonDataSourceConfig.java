package co.edu.usta.spacetrack.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Configures PostgreSQL from Neon's standard DATABASE_URL without storing credentials in source. */
@Configuration
@Profile("prod")
public class NeonDataSourceConfig {

    @Bean
    DataSource dataSource(Environment environment) {
        // Hikari ya es un pool: se prefiere la conexion directa de Neon y, si no existe, la del pooler.
        String databaseUrl = environment.getProperty("DATABASE_URL_UNPOOLED");
        if (databaseUrl == null || databaseUrl.isBlank()) databaseUrl = environment.getRequiredProperty("DATABASE_URL");
        URI uri = URI.create(databaseUrl.trim());
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalStateException("DATABASE_URL debe incluir usuario y contraseña de Neon");
        }

        String[] credentials = userInfo.split(":", 2);
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalStateException("DATABASE_URL no contiene un host válido");
        }

        String jdbcUrl = "jdbc:postgresql://" + host
                + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                + uri.getRawPath()
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(decode(credentials[0]));
        config.setPassword(decode(credentials[1]));
        config.setMaximumPoolSize(environment.getProperty("DB_POOL_MAX_SIZE", Integer.class, 5));
        config.setMinimumIdle(environment.getProperty("DB_POOL_MIN_IDLE", Integer.class, 1));
        config.setConnectionTimeout(30_000);
        return new HikariDataSource(config);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
