package co.edu.usta.spacetrack.usuarios.dto;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import jakarta.validation.constraints.NotNull;

public record CambioRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}
