package co.edu.usta.spacetrack.clientes.dto;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String razonSocial,
        String nit,
        String nombreContacto,
        String correoContacto,
        String telefonoContacto,
        String direccion,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) { }
