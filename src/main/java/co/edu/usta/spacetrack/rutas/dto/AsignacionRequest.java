package co.edu.usta.spacetrack.rutas.dto;

import jakarta.validation.constraints.NotNull;

public record AsignacionRequest(
        @NotNull(message = "Debe seleccionar un vehiculo") Long vehiculoId,
        @NotNull(message = "Debe seleccionar un conductor") Long conductorId
) { }
