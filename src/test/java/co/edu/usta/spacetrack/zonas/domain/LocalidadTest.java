package co.edu.usta.spacetrack.zonas.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalidadTest {

    @Test
    void reconoceLaLocalidadSinTildesNiMayusculas() {
        assertThat(Localidad.buscar("ENGATIVA")).contains(Localidad.ENGATIVA);
        assertThat(Localidad.buscar("Localidad de Los Martires")).contains(Localidad.LOS_MARTIRES);
    }

    @Test
    void noReconoceTextosFueraDelCatalogo() {
        assertThat(Localidad.buscar("Chia")).isEmpty();
        assertThat(Localidad.buscar(null)).isEmpty();
        assertThat(Localidad.buscar("  ")).isEmpty();
    }

    @Test
    void todasLasLocalidadesCaenDentroDelPlanoDelMapa() {
        for (Localidad localidad : Localidad.values()) {
            assertThat(localidad.getLatitud()).isBetween(4.48, 4.78);
            assertThat(localidad.getLongitud()).isBetween(-74.25, -74.00);
        }
    }
}
