package co.edu.usta.spacetrack.operaciones.domain;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.DOMINGO;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.JUEVES;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.LUNES;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.MARTES;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.MIERCOLES;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.SABADO;
import static co.edu.usta.spacetrack.necesidades.domain.DiaSemana.VIERNES;
import static org.assertj.core.api.Assertions.assertThat;

class DiasOperacionTest {

    @Test
    void interpretaUnRangoConTildes() {
        assertThat(DiasOperacion.interpretar("Lunes a sábado")).contains(EnumSet.of(LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO));
    }

    @Test
    void interpretaUnaListaDeDias() {
        assertThat(DiasOperacion.interpretar("Lunes, miércoles y viernes")).contains(EnumSet.of(LUNES, MIERCOLES, VIERNES));
    }

    @Test
    void interpretaRangosQueCruzanElFinDeSemana() {
        assertThat(DiasOperacion.interpretar("de viernes a lunes")).contains(EnumSet.of(VIERNES, SABADO, DOMINGO, LUNES));
    }

    @Test
    void interpretaTodosLosDias() {
        assertThat(DiasOperacion.interpretar("Todos los días")).contains(EnumSet.allOf(DiaSemana.class));
    }

    @Test
    void combinaRangosYDiasSueltos() {
        assertThat(DiasOperacion.interpretar("Lunes a miercoles y sabados")).contains(EnumSet.of(LUNES, MARTES, MIERCOLES, SABADO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Fines de semana", "Lunes a festivos", "L-V", "Diario"})
    void rechazaTextosQueNoSonDias(String texto) {
        assertThat(DiasOperacion.interpretar(texto)).isEmpty();
    }
}
