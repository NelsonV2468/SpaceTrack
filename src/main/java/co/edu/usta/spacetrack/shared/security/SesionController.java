package co.edu.usta.spacetrack.shared.security;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import co.edu.usta.spacetrack.usuarios.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class SesionController {

    private static final Logger log = LoggerFactory.getLogger(SesionController.class);
    private static final UsuarioSesion USUARIO_DESARROLLO = new UsuarioSesion(null, "desarrollo@localhost", "Modo desarrollo", Rol.ADMIN);

    private final boolean seguridadHabilitada;
    private final Environment entorno;
    private final UsuarioService usuarios;
    private final ObjectProvider<VerificadorIdentidad> verificador;
    private final ObjectProvider<SecurityContextRepository> repositorioContexto;

    public SesionController(@Value("${spacetrack.seguridad.habilitada:false}") boolean seguridadHabilitada,
                            Environment entorno, UsuarioService usuarios,
                            ObjectProvider<VerificadorIdentidad> verificador,
                            ObjectProvider<SecurityContextRepository> repositorioContexto) {
        this.seguridadHabilitada = seguridadHabilitada;
        this.entorno = entorno;
        this.usuarios = usuarios;
        this.verificador = verificador;
        this.repositorioContexto = repositorioContexto;
    }

    /** Configuracion publica que necesita la pagina de login (no contiene secretos). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("seguridad", seguridadHabilitada);
        if (seguridadHabilitada) cuerpo.put("baseUrl", entorno.getRequiredProperty("NEON_AUTH_BASE_URL"));
        return cuerpo;
    }

    /** Cambia un JWT de Neon Auth (cabecera Authorization) por una sesion HttpOnly de SpaceTrack. */
    @PostMapping("/session")
    public ResponseEntity<?> iniciarSesion(HttpServletRequest request, HttpServletResponse response) {
        if (!seguridadHabilitada) return ResponseEntity.ok(USUARIO_DESARROLLO);

        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return error(HttpStatus.UNAUTHORIZED, "Falta el token de Neon Auth");
        }
        IdentidadVerificada identidad;
        try {
            identidad = verificador.getObject().verificar(cabecera.substring(7).trim());
        } catch (VerificadorIdentidad.TokenInvalidoException e) {
            log.info("Token de Neon Auth rechazado: {}", e.getMessage());
            return error(HttpStatus.UNAUTHORIZED, "No fue posible validar su identidad. Inicie sesion de nuevo");
        }

        Usuario usuario = usuarios.registrarIngreso(identidad);
        if (!usuario.getRol().tieneAcceso()) {
            return error(HttpStatus.FORBIDDEN, "Su cuenta fue creada y espera la aprobacion de un administrador");
        }

        HttpSession anterior = request.getSession(false);
        if (anterior != null) anterior.invalidate();

        UsuarioSesion sesion = UsuarioSesion.desde(usuario);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(sesion.comoAutenticacion());
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.getObject().saveContext(contexto, request, response);
        return ResponseEntity.ok(sesion);
    }

    @GetMapping("/me")
    public ResponseEntity<?> usuarioActual(Authentication autenticacion) {
        if (!seguridadHabilitada) return ResponseEntity.ok(USUARIO_DESARROLLO);
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion sesion) {
            return ResponseEntity.ok(sesion);
        }
        return error(HttpStatus.UNAUTHORIZED, "No hay una sesion activa");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("estado", estado.value(), "mensaje", mensaje));
    }
}
