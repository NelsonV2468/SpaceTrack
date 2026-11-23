package co.edu.usta.spacetrack.demo;

import co.edu.usta.spacetrack.alertas.domain.NivelAlerta;
import co.edu.usta.spacetrack.alertas.dto.AlertaRutaResponse;
import co.edu.usta.spacetrack.alertas.service.AlertaRutaService;
import co.edu.usta.spacetrack.clientes.service.ClienteService;
import co.edu.usta.spacetrack.conductores.dto.ConductorResponse;
import co.edu.usta.spacetrack.conductores.service.ConductorService;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
import co.edu.usta.spacetrack.operaciones.service.OperacionService;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.service.RutaService;
import co.edu.usta.spacetrack.vehiculos.service.VehiculoService;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.service.ZonaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Arranca la aplicacion con el perfil demo (H2 en memoria) a las 10:00 de Bogota. */
@SpringBootTest(properties = "spacetrack.simulacion.intervalo-segundos=3600")
@ActiveProfiles("demo")
class PerfilDemoIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.now(BOGOTA);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDemo() {
            return Clock.fixed(HOY.atTime(10, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        }
    }

    @Autowired CargaDatosDemo carga;
    @Autowired ClienteService clienteService;
    @Autowired OperacionService operacionService;
    @Autowired NecesidadClienteService necesidadService;
    @Autowired VehiculoService vehiculoService;
    @Autowired ConductorService conductorService;
    @Autowired ZonaService zonaService;
    @Autowired RutaService rutaService;
    @Autowired AlertaRutaService alertaService;

    @Test
    void creaElCasoDeEjemploConCadenaDeFrio() {
        assertThat(clienteService.listar()).singleElement()
                .satisfies(c -> assertThat(c.razonSocial()).contains("(demo)"));
        assertThat(operacionService.listar()).singleElement()
                .satisfies(o -> assertThat(o.requiereCadenaFrio()).isTrue());
        assertThat(necesidadService.listar(null)).singleElement().satisfies(n -> {
            assertThat(n.requiereCadenaFrio()).isTrue();
            assertThat(n.flotaSuficiente()).isTrue();
        });
        assertThat(vehiculoService.listar()).hasSize(5).allSatisfy(v -> assertThat(v.requiereCadenaFrio()).isTrue());
        assertThat(conductorService.listar()).hasSize(5).extracting(ConductorResponse::fechaVencimientoLicencia)
                .allSatisfy(vence -> assertThat(vence).isAfter(HOY));
        assertThat(zonaService.listar()).hasSize(5).extracting(ZonaResponse::localidad).doesNotHaveDuplicates();
    }

    @Test
    void dejaTresRutasEnCursoUnaPendienteYUnaFinalizada() {
        List<RutaResponse> rutas = rutaService.listar(null);

        assertThat(rutas).hasSize(5).allSatisfy(r -> {
            assertThat(r.vehiculoId()).isNotNull();
            assertThat(r.conductorId()).isNotNull();
        });
        assertThat(rutas.stream().collect(Collectors.groupingBy(RutaResponse::estado, Collectors.counting())))
                .containsEntry(EstadoRuta.EN_CURSO, 3L).containsEntry(EstadoRuta.PENDIENTE, 1L)
                .containsEntry(EstadoRuta.FINALIZADA, 1L);

        Map<String, RutaResponse> porCodigo = rutas.stream().collect(Collectors.toMap(RutaResponse::codigo, Function.identity()));
        assertThat(porCodigo.get("R-DEMO-01").entregasRealizadas()).isEqualTo(5);
        assertThat(porCodigo.get("R-DEMO-03").entregasRealizadas()).isEqualTo(9);
        assertThat(porCodigo.get("R-DEMO-03").numeroEntregasProgramadas()).isEqualTo(10);
        assertThat(porCodigo.get("R-DEMO-04").entregasRealizadas()).isZero();
        RutaResponse finalizada = porCodigo.get("R-DEMO-05");
        assertThat(finalizada.entregasRealizadas() + finalizada.entregasNoRealizadas())
                .isEqualTo(finalizada.numeroEntregasProgramadas().longValue());
    }

    @Test
    void soloLaRutaAtrasadaGeneraAlertaCritica() {
        List<AlertaRutaResponse> alertas = alertaService.alertasActivas();

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.codigoRuta()).isEqualTo("R-DEMO-02");
            assertThat(a.nivel()).isEqualTo(NivelAlerta.CRITICA);
        });
    }

    @Test
    void noRepiteLaCargaSiYaExistenClientes() throws Exception {
        carga.run(null);

        assertThat(clienteService.listar()).hasSize(1);
        assertThat(rutaService.listar(null)).hasSize(5);
    }
}
