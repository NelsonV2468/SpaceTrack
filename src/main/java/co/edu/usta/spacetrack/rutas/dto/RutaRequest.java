package co.edu.usta.spacetrack.rutas.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RutaRequest(
        @NotBlank(message = "El codigo de la ruta es obligatorio") @Size(max = 30) String codigo,
        @NotBlank(message = "El nombre de la ruta es obligatorio") @Size(max = 120) String nombre,
        @NotNull(message = "Debe seleccionar una zona") Long zonaId,
        @NotNull(message = "Debe seleccionar una operacion") Long operacionId,
        @NotBlank(message = "El punto de origen es obligatorio") @Size(max = 150) String puntoOrigen,
        @NotBlank(message = "El punto de destino es obligatorio") @Size(max = 150) String puntoDestino,
        @NotNull(message = "Indique la distancia estimada") @Positive(message = "La distancia debe ser mayor a cero") Double distanciaEstimadaKm,
        @NotNull(message = "Indique las entregas programadas") @Positive(message = "Debe programar al menos una entrega") Integer numeroEntregasProgramadas
) {
    // Una ruta que sale y llega al mismo punto no tiene recorrido de distribucion.
    @AssertTrue(message = "El punto de origen y el de destino deben ser diferentes")
    public boolean isRecorridoValido() {
        return puntoOrigen == null || puntoDestino == null || !puntoOrigen.trim().equalsIgnoreCase(puntoDestino.trim());
    }
}
