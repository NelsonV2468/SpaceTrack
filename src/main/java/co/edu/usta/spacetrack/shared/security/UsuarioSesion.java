package co.edu.usta.spacetrack.shared.security;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.List;

/** Principal guardado en la sesion HTTP tras validar el token de Neon Auth. */
public record UsuarioSesion(Long id, String correo, String nombre, Rol rol) implements Serializable {

    public static UsuarioSesion desde(Usuario usuario) {
        return new UsuarioSesion(usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getRol());
    }

    public Authentication comoAutenticacion() {
        return UsernamePasswordAuthenticationToken.authenticated(this, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }
}
