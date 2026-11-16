package co.edu.usta.spacetrack.usuarios.dto;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String correo, String nombre, Rol rol,
                              LocalDateTime fechaCreacion, LocalDateTime ultimoIngreso) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getRol(),
                usuario.getFechaCreacion(), usuario.getUltimoIngreso());
    }
}
