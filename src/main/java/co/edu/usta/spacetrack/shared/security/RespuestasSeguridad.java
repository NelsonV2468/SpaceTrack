package co.edu.usta.spacetrack.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas en espanol, con el mismo formato {"mensaje": ...} que usa el panel. */
final class RespuestasSeguridad {

    private RespuestasSeguridad() {
    }

    /** Las paginas redirigen al login; la API responde 401 en JSON. */
    static AuthenticationEntryPoint sinSesion() {
        return (request, response, ex) -> {
            if (esPeticionDeDatos(request)) {
                escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "Su sesion no es valida o expiro. Inicie sesion de nuevo");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html");
            }
        };
    }

    static AccessDeniedHandler accesoDenegado() {
        return (request, response, ex) -> escribir(response, HttpServletResponse.SC_FORBIDDEN,
                ex instanceof CsrfException
                        ? "La solicitud no incluye un token de seguridad valido. Recargue la pagina"
                        : "Su rol no tiene permiso para realizar esta accion");
    }

    static boolean esPeticionDeDatos(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        String acepta = request.getHeader("Accept");
        return ruta.startsWith("/api/") || ruta.startsWith("/auth/")
                || (acepta != null && acepta.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    static void escribir(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}");
    }
}
