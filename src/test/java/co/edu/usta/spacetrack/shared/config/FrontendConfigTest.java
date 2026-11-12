package co.edu.usta.spacetrack.shared.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendConfigTest {

    @Test
    void traduceLosFragmentosDePrefetchDeNextASuCarpetaExportada() {
        assertThat(FrontendConfig.IndiceDeCarpetaResolver.rutaSegmentoNext(
                "panel/monitoreo/__next.panel.monitoreo.__PAGE__.txt", "__next.panel.monitoreo.__PAGE__.txt"))
                .isEqualTo("panel/monitoreo/__next.panel/monitoreo/__PAGE__.txt");
        assertThat(FrontendConfig.IndiceDeCarpetaResolver.rutaSegmentoNext(
                "panel/clientes/__next.panel.$d$modulo.__PAGE__.txt", "__next.panel.$d$modulo.__PAGE__.txt"))
                .isEqualTo("panel/clientes/__next.panel/$d$modulo/__PAGE__.txt");
    }

    @Test
    void laTraduccionNuncaProduceSaltosDeCarpeta() {
        String ruta = FrontendConfig.IndiceDeCarpetaResolver.rutaSegmentoNext("x/__next.....txt", "__next.....txt");
        assertThat(ruta).doesNotContain("..");
    }
}
