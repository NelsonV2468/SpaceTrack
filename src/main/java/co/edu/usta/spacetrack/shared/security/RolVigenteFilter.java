package co.edu.usta.spacetrack.shared.security;

import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import co.edu.usta.spacetrack.usuarios.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Recarga el rol del usuario en cada peticion: si un administrador lo cambia o lo deja PENDIENTE,
 * el efecto es inmediato sin esperar a que la sesion expire.
 */
class RolVigenteFilter extends OncePerRequestFilter {

    private final UsuarioService usuarios;
    private final SecurityContextRepository repositorioContexto;

    RolVigenteFilter(UsuarioService usuarios, SecurityContextRepository repositorioContexto) {
        this.usuarios = usuarios;
        this.repositorioContexto = repositorioContexto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        if (actual != null && actual.getPrincipal() instanceof UsuarioSesion sesion) {
            Optional<Usuario> usuario = usuarios.buscar(sesion.id());
            if (usuario.isEmpty() || !usuario.get().getRol().tieneAcceso()) {
                SecurityContextHolder.clearContext();
                HttpSession httpSession = request.getSession(false);
                if (httpSession != null) httpSession.invalidate();
            } else if (usuario.get().getRol() != sesion.rol()) {
                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(UsuarioSesion.desde(usuario.get()).comoAutenticacion());
                SecurityContextHolder.setContext(contexto);
                repositorioContexto.saveContext(contexto, request, response);
            }
        }
        chain.doFilter(request, response);
    }
}
