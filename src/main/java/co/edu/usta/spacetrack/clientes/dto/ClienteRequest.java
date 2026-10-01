package co.edu.usta.spacetrack.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "La razon social es obligatoria")
        @Size(max = 120, message = "La razon social no puede superar 120 caracteres")
        String razonSocial,

        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "^[0-9]{6,10}(-[0-9])?$", message = "Ingrese un NIT valido, por ejemplo 900123456-7")
        String nit,

        @NotBlank(message = "El nombre de contacto es obligatorio")
        @Size(max = 100, message = "El nombre de contacto no puede superar 100 caracteres")
        String nombreContacto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingrese un correo valido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correoContacto,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9+() -]{7,25}$", message = "Ingrese un telefono valido")
        String telefonoContacto,

        @NotBlank(message = "La direccion es obligatoria")
        @Size(max = 180, message = "La direccion no puede superar 180 caracteres")
        String direccion,
        boolean activo
) { }
