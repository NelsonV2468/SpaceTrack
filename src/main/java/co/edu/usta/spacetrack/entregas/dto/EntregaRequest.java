package co.edu.usta.spacetrack.entregas.dto;

import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntregaRequest(
        @NotNull(message = "Debe seleccionar una ruta") Long rutaId,
        @Size(max = 40, message = "El numero de guia no puede superar 40 caracteres") String numeroGuia,
        @NotBlank(message = "El destinatario es obligatorio") @Size(max = 120) String destinatario,
        @NotBlank(message = "La direccion es obligatoria") @Size(max = 150) String direccion,
        @NotNull(message = "Indique el resultado de la entrega") EstadoEntrega estado,
        @Size(max = 255, message = "La novedad no puede superar 255 caracteres") String novedad
) {
    // Una entrega fallida siempre debe explicar por que no se completo.
    @AssertTrue(message = "Describa la novedad cuando la entrega no se realiza")
    public boolean isNovedadValida() {
        return estado != EstadoEntrega.NO_ENTREGADA || (novedad != null && !novedad.isBlank());
    }
}
