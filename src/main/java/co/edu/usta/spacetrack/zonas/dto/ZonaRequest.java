package co.edu.usta.spacetrack.zonas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZonaRequest(
        @NotBlank(message = "El codigo de la zona es obligatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre de la zona es obligatorio") @Size(max = 80) String nombre,
        @NotBlank(message = "La localidad es obligatoria") @Size(max = 80) String localidad,
        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres") String descripcion,
        boolean activa
) { }
