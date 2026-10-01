package co.edu.usta.spacetrack.conductores.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConductorResponse(Long id, String tipoDocumento, String numeroDocumento, String nombres,
                                String apellidos, String telefono, String correo, String numeroLicencia,
                                LocalDate fechaVencimientoLicencia, boolean activo, LocalDateTime fechaCreacion,
                                LocalDateTime fechaActualizacion) { }
