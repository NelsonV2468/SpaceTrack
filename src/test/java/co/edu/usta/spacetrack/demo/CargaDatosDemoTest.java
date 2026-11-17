package co.edu.usta.spacetrack.demo;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Franja horaria de la necesidad demo: siempre contiene la hora de arranque para poder iniciar las rutas. */
class CargaDatosDemoTest {

    @Test
    void enHorarioDeOficinaUsaLaFranjaTipica() {
        LocalTime ahora = LocalTime.of(10, 15);

        assertThat(CargaDatosDemo.horaInicioFranja(ahora)).isEqualTo(LocalTime.of(6, 0));
        assertThat(CargaDatosDemo.horaFinFranja(ahora)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void deMadrugadaAdelantaElInicio() {
        assertThat(CargaDatosDemo.horaInicioFranja(LocalTime.of(3, 40))).isEqualTo(LocalTime.of(2, 0));
        assertThat(CargaDatosDemo.horaInicioFranja(LocalTime.of(0, 20))).isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void enLaNocheDejaAlMenosDosHorasOTerminaAntesDeMedianoche() {
        assertThat(CargaDatosDemo.horaFinFranja(LocalTime.of(19, 10))).isEqualTo(LocalTime.of(21, 10));
        assertThat(CargaDatosDemo.horaFinFranja(LocalTime.of(22, 30))).isEqualTo(LocalTime.of(23, 59));
    }

    @Test
    void laFranjaSiempreContieneLaHoraDeArranque() {
        for (int minuto = 0; minuto < 24 * 60; minuto += 7) {
            LocalTime ahora = LocalTime.MIDNIGHT.plusMinutes(minuto);
            LocalTime inicio = CargaDatosDemo.horaInicioFranja(ahora);
            LocalTime fin = CargaDatosDemo.horaFinFranja(ahora);
            assertThat(fin).as("fin a las %s", ahora).isAfter(inicio);
            assertThat(ahora).as("arranque a las %s", ahora).isBetween(inicio, fin);
        }
    }
}
