package co.edu.usta.spacetrack.shared.security;

import co.edu.usta.spacetrack.usuarios.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Seguridad con Neon Auth. El navegador inicia sesion en Neon Auth, envia el JWT una sola vez a
 * POST /auth/session y desde ahi trabaja con una cookie de sesion HttpOnly + token CSRF.
 * Asi funcionan tanto la API como las paginas Thymeleaf, sin guardar tokens en el navegador.
 */
@Configuration
@ConditionalOnProperty(name = "spacetrack.seguridad.habilitada", havingValue = "true")
public class SeguridadConfig {

    private static final String[] TODOS_LOS_ROLES = {"ADMIN", "OPERADOR", "CONSULTA"};

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    VerificadorIdentidad verificadorIdentidad(@Value("${NEON_AUTH_JWKS_URL}") String jwksUrl,
                                              @Value("${NEON_AUTH_BASE_URL}") String baseUrl,
                                              @Value("${NEON_AUTH_ISSUER:}") String emisor) {
        Set<String> emisores = new LinkedHashSet<>();
        if (!emisor.isBlank()) {
            emisores.add(emisor.trim());
        } else {
            emisores.add(sinBarraFinal(baseUrl));
            emisores.add(origen(baseUrl));
        }
        return NeonJwtVerificador.remoto(jwksUrl, emisores);
    }

    @Bean
    CookieSameSiteSupplier cookieSesionLax() {
        return CookieSameSiteSupplier.ofLax().whenHasName("JSESSIONID");
    }

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, UsuarioService usuarios,
                                        SecurityContextRepository repositorioContexto,
                                        @Value("${NEON_AUTH_BASE_URL}") String baseUrl,
                                        @Value("${server.servlet.session.cookie.secure:false}") boolean cookiesSeguras) throws Exception {
        CookieCsrfTokenRepository repositorioCsrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorioCsrf.setCookieCustomizer(c -> c.sameSite("Strict").secure(cookiesSeguras));
        CsrfTokenRequestAttributeHandler manejadorCsrf = new CsrfTokenRequestAttributeHandler();
        String authOrigen = origen(baseUrl);

        return http
                .securityContext(c -> c.securityContextRepository(repositorioContexto))
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
                .csrf(c -> c.csrfTokenRepository(repositorioCsrf).csrfTokenRequestHandler(manejadorCsrf))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(new RolVigenteFilter(usuarios, repositorioContexto), AuthorizationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login.html", "/js/login.bundle.js", "/css/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/session").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/*/eliminar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").hasAnyRole(TODOS_LOS_ROLES)
                        .anyRequest().hasAnyRole("ADMIN", "OPERADOR"))
                .logout(l -> l.logoutUrl("/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(RespuestasSeguridad.sinSesion())
                        .accessDeniedHandler(RespuestasSeguridad.accesoDenegado()))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                                        + "connect-src 'self' " + authOrigen + "; frame-src " + authOrigen + "; "
                                        + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'")))
                .build();
    }

    static String origen(String url) {
        URI uri = URI.create(url.trim());
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalStateException("NEON_AUTH_BASE_URL no es una URL valida");
        }
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
    }

    private static String sinBarraFinal(String url) {
        String limpia = url.trim();
        return limpia.endsWith("/") ? limpia.substring(0, limpia.length() - 1) : limpia;
    }
}
