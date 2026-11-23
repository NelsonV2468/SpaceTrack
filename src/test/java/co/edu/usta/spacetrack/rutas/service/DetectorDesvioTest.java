package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;
import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DetectorDesvioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final PosicionRutaRepository posiciones = mock(PosicionRutaRepository.class);
    private final SimuladorUbicacion simulador = new SimuladorUbicacion();
    private final DetectorDesvio detector = new DetectorDesvio(posiciones, simulador, 0.5, 2.0, 30);

    @Test
    void laDistanciaAUnTramoEsPerpendicular() {
        // Tramo norte-sur; un punto 0.01 grados al oriente esta a ~1.1 km.
        List<double[]> recorrido = List.of(new double[]{4.60, -74.10}, new double[]{4.70, -74.10});
        assertThat(SimuladorUbicacion.distanciaAlRecorridoKm(recorrido, 4.65, -74.09)).isCloseTo(1.11, within(0.02));
        assertThat(SimuladorUbicacion.distanciaAlRecorridoKm(recorrido, 4.65, -74.10)).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void unaPosicionSobreElRecorridoNoEstaDesviada() {
        Ruta ruta = ruta();
        double[] parada = simulador.recorridoPlaneado(ruta).get(2);
        reportar(parada[0], parada[1], AHORA.minusMinutes(5));

        assertThat(detector.evaluar(ruta, AHORA)).hasValueSatisfying(p -> {
            assertThat(p.fueraDeRuta()).isFalse();
            assertThat(p.distanciaKm()).isLessThan(0.01);
        });
    }

    @Test
    void unaPosicionLejanaEstaDesviadaYEsCritica() {
        Ruta ruta = ruta();
        reportar(4.7650, -74.1300, AHORA.minusMinutes(5));

        assertThat(detector.evaluar(ruta, AHORA)).hasValueSatisfying(p -> {
            assertThat(p.fueraDeRuta()).isTrue();
            assertThat(p.critico()).isTrue();
            assertThat(p.distanciaKm()).isGreaterThan(2.0);
        });
        assertThat(detector.tieneReporteVigente(ruta, AHORA)).isTrue();
    }

    @Test
    void ignoraReportesViejosOAnterioresAlInicio() {
        Ruta ruta = ruta();
        reportar(4.7650, -74.1300, AHORA.minusMinutes(45));
        assertThat(detector.evaluar(ruta, AHORA)).isEmpty();

        reportar(4.7650, -74.1300, ruta.getFechaInicioReal().minusMinutes(1));
        assertThat(detector.evaluar(ruta, ruta.getFechaInicioReal().plusMinutes(5))).isEmpty();
    }

    @Test
    void soloEvaluaRutasEnCurso() {
        Ruta ruta = ruta();
        ruta.setEstado(EstadoRuta.FINALIZADA);
        reportar(4.7650, -74.1300, AHORA.minusMinutes(5));

        assertThat(detector.evaluar(ruta, AHORA)).isEmpty();
    }

    private void reportar(double lat, double lon, LocalDateTime fecha) {
        PosicionRuta posicion = new PosicionRuta();
        posicion.setLatitud(lat);
        posicion.setLongitud(lon);
        posicion.setVelocidadKmh(20);
        posicion.setOrigen(OrigenPosicion.MANUAL);
        posicion.setFechaRegistro(fecha);
        when(posiciones.findFirstByRutaIdAndOrigenOrderByIdDesc(7L, OrigenPosicion.MANUAL)).thenReturn(Optional.of(posicion));
    }

    private Ruta ruta() {
        Zona zona = new Zona();
        zona.setNombre("Suba");
        zona.setLocalidad("Suba");
        Ruta ruta = new Ruta() {
            @Override
            public Long getId() {
                return 7L;
            }
        };
        ruta.setCodigo("R-DEMO-03");
        ruta.setZona(zona);
        ruta.setNumeroEntregasProgramadas(10);
        ruta.setEstado(EstadoRuta.EN_CURSO);
        ruta.setFechaInicioReal(AHORA.minusHours(2));
        return ruta;
    }
}
