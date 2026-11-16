package co.edu.usta.spacetrack.usuarios.controller;

import co.edu.usta.spacetrack.shared.security.UsuarioSesion;
import co.edu.usta.spacetrack.usuarios.dto.CambioRolRequest;
import co.edu.usta.spacetrack.usuarios.dto.UsuarioResponse;
import co.edu.usta.spacetrack.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestion de roles. La regla de acceso (solo ADMIN) vive en SeguridadConfig. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioRestController {

    private final UsuarioService servicio;

    public UsuarioRestController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar();
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambioRolRequest solicitud,
                                      @AuthenticationPrincipal UsuarioSesion actual) {
        return servicio.cambiarRol(id, solicitud.rol(), actual == null ? null : actual.id());
    }
}
