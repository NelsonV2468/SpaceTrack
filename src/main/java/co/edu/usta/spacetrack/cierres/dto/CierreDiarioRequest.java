package co.edu.usta.spacetrack.cierres.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CierreDiarioRequest(
        @NotNull(message = "Indique la fecha que desea cerrar") LocalDate fecha,
        @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres") String observaciones
) { }
