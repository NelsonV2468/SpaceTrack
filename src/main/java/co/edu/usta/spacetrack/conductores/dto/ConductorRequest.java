package co.edu.usta.spacetrack.conductores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ConductorRequest(
        @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
        @NotBlank(message = "El numero de documento es obligatorio") @Pattern(regexp = "^[0-9]{6,15}$", message = "Ingrese un documento valido") String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 70) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 70) String apellidos,
        @NotBlank(message = "El telefono es obligatorio") @Pattern(regexp = "^[0-9+() -]{7,25}$", message = "Ingrese un telefono valido") String telefono,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingrese un correo valido") @Size(max = 120) String correo,
        @NotBlank(message = "El numero de licencia es obligatorio") @Size(max = 30) String numeroLicencia,
        @NotNull(message = "La fecha de vencimiento es obligatoria") @Future(message = "La licencia debe estar vigente") LocalDate fechaVencimientoLicencia,
        boolean activo
) { }
