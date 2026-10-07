package co.edu.usta.spacetrack.necesidades.dto;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.Set;

public record NecesidadClienteRequest(
        @NotNull(message = "Debe seleccionar una operacion") Long operacionId,
        @NotBlank(message = "El tipo de vehiculo es obligatorio") @Size(max = 50) String tipoVehiculo,
        @NotNull(message = "Indique la cantidad de vehiculos") @Positive(message = "Debe requerir al menos un vehiculo") Integer cantidadVehiculos,
        @NotNull(message = "Indique la capacidad minima") @Positive(message = "La capacidad debe ser mayor a cero") Double capacidadMinimaKg,
        boolean requiereCadenaFrio,
        @NotEmpty(message = "Seleccione al menos un dia de operacion") Set<DiaSemana> diasOperacion,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
        @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres") String observaciones
) {
    // La jornada debe terminar despues de empezar dentro del mismo dia.
    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
