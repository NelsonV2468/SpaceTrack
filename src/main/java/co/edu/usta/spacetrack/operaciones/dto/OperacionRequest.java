package co.edu.usta.spacetrack.operaciones.dto;

import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OperacionRequest(
        @NotBlank(message = "El codigo del contrato es obligatorio") @Size(max = 30) String codigoContrato,
        @NotNull(message = "Debe seleccionar un cliente") Long clienteId,
        @NotBlank(message = "El nombre de la operacion es obligatorio") @Size(max = 120) String nombreOperacion,
        @NotNull(message = "La fecha inicial es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha final es obligatoria") @FutureOrPresent(message = "La fecha final debe ser actual o futura") LocalDate fechaFin,
        @NotBlank(message = "Los dias de operacion son obligatorios") @Size(max = 80) String diasOperacion,
        @NotNull(message = "Indique los vehiculos requeridos") @Positive(message = "Debe requerir al menos un vehiculo") Integer numeroVehiculosRequeridos,
        @NotNull(message = "Indique las entregas esperadas") @Positive(message = "Debe indicar al menos una entrega") Integer numeroEntregasEsperadas,
        boolean requiereCadenaFrio,
        @NotNull(message = "El estado es obligatorio") EstadoOperacion estado
) {
    // Evita registrar contratos con un periodo de vigencia inverso.
    @AssertTrue(message = "La fecha final debe ser posterior o igual a la fecha inicial")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
