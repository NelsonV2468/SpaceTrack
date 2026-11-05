package co.edu.usta.spacetrack.posiciones.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PosicionRutaRequestTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceptaPosicionSinVelocidad() {
        assertThat(mensajes(new PosicionRutaRequest(1L, 4.65, -74.08, null))).isEmpty();
    }

    @Test
    void aceptaCoordenadasEnLosLimites() {
        assertThat(mensajes(new PosicionRutaRequest(1L, 90.0, -180.0, 0))).isEmpty();
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        assertThat(mensajes(new PosicionRutaRequest(1L, 90.5, -180.5, 30))).containsExactlyInAnyOrder(
                "La latitud debe estar entre -90 y 90", "La longitud debe estar entre -180 y 180");
    }

    @Test
    void rechazaVelocidadNegativa() {
        assertThat(mensajes(new PosicionRutaRequest(1L, 4.65, -74.08, -1)))
                .containsExactly("La velocidad no puede ser negativa");
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        assertThat(mensajes(new PosicionRutaRequest(null, null, null, null))).containsExactlyInAnyOrder(
                "Debe indicar la ruta", "La latitud es obligatoria", "La longitud es obligatoria");
    }

    private List<String> mensajes(PosicionRutaRequest solicitud) {
        return validador.validate(solicitud).stream().map(ConstraintViolation::getMessage).toList();
    }
}
