package co.edu.usta.spacetrack.operaciones.dto;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Operacion (contrato) y lo que necesita el cliente, en un solo formulario: cada dato se pide una vez.
 * Los dias, la cantidad de vehiculos y la cadena de frio sirven a la vez para el contrato y para su necesidad.
 */
public record PlanOperacionRequest(
        @NotBlank(message = "El codigo del contrato es obligatorio") @Size(max = 30) String codigoContrato,
        @NotNull(message = "Debe seleccionar un cliente") Long clienteId,
        @NotBlank(message = "El nombre de la operacion es obligatorio") @Size(max = 120) String nombreOperacion,
        @NotNull(message = "La fecha inicial es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha final es obligatoria") LocalDate fechaFin,
        @NotNull(message = "El estado es obligatorio") EstadoOperacion estado,
        @NotNull(message = "Indique las entregas esperadas") @Positive(message = "Debe indicar al menos una entrega") Integer numeroEntregasEsperadas,
        boolean requiereCadenaFrio,
        @NotEmpty(message = "Seleccione al menos un dia de operacion") Set<DiaSemana> diasOperacion,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
        @NotBlank(message = "El tipo de vehiculo es obligatorio") @Size(max = 50) String tipoVehiculo,
        @NotNull(message = "Indique la cantidad de vehiculos") @Positive(message = "Debe requerir al menos un vehiculo") Integer cantidadVehiculos,
        @NotNull(message = "Indique la capacidad minima") @Positive(message = "La capacidad debe ser mayor a cero") Double capacidadMinimaKg,
        @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres") String observaciones
) {
    @AssertTrue(message = "La fecha final debe ser posterior o igual a la fecha inicial")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
