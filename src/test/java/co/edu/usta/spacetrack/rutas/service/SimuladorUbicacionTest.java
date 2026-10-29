package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.entregas.domain.Entrega;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.UbicacionRutaResponse;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SimuladorUbicacionTest {

    private final SimuladorUbicacion simulador = new SimuladorUbicacion();
    private final LocalDateTime ahora = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Test
    void rutaPendienteEstaEnElCentroDeDistribucion() {
        UbicacionRutaResponse ubicacion = simulador.calcular(ruta(EstadoRuta.PENDIENTE), List.of(), ahora);

        assertThat(ubicacion.vehiculo().latitud()).isEqualTo(SimuladorUbicacion.CEDI_LATITUD);
        assertThat(ubicacion.vehiculo().longitud()).isEqualTo(SimuladorUbicacion.CEDI_LONGITUD);
        assertThat(ubicacion.progreso()).isZero();
        assertThat(ubicacion.paradas()).hasSize(4);
    }

    @Test
    void rutaEnCursoAvanzaHaciaLaSiguienteParadaYMarcaLasVisitadas() {
        Ruta ruta = ruta(EstadoRuta.EN_CURSO);
        ruta.setFechaInicioReal(ahora.minusMinutes(30));

        UbicacionRutaResponse ubicacion = simulador.calcular(ruta, List.of(entrega(EstadoEntrega.ENTREGADA),
                entrega(EstadoEntrega.NO_ENTREGADA)), ahora);

        assertThat(ubicacion.entregasRegistradas()).isEqualTo(2);
        assertThat(ubicacion.paradas()).extracting(p -> p.estado())
                .containsExactly("ENTREGADA", "NO_ENTREGADA", "PENDIENTE", "PENDIENTE");
        assertThat(ubicacion.progreso()).isBetween(2.0 / 5, 3.0 / 5);
        assertThat(ubicacion.descripcionPosicion()).contains("parada 3");
    }

    @Test
    void rutaFinalizadaQuedaEnElDestino() {
        UbicacionRutaResponse ubicacion = simulador.calcular(ruta(EstadoRuta.FINALIZADA), List.of(), ahora);

        assertThat(ubicacion.vehiculo().latitud()).isCloseTo(ubicacion.destino().latitud(), within(1e-9));
        assertThat(ubicacion.vehiculo().longitud()).isCloseTo(ubicacion.destino().longitud(), within(1e-9));
        assertThat(ubicacion.progreso()).isEqualTo(1.0);
    }

    @Test
    void ubicaLasParadasCercaDeLaLocalidadDeLaZona() {
        double[] centro = SimuladorUbicacion.centroDeZona("Usaquén");
        UbicacionRutaResponse ubicacion = simulador.calcular(ruta(EstadoRuta.PENDIENTE), List.of(), ahora);

        assertThat(ubicacion.paradas()).allSatisfy(parada -> {
            assertThat(parada.latitud()).isCloseTo(centro[0], within(0.02));
            assertThat(parada.longitud()).isCloseTo(centro[1], within(0.02));
        });
    }

    private Ruta ruta(EstadoRuta estado) {
        Zona zona = new Zona();
        zona.setNombre("Norte");
        zona.setLocalidad("Usaquen");
        Ruta ruta = new Ruta();
        ruta.setCodigo("R-001");
        ruta.setZona(zona);
        ruta.setPuntoOrigen("CEDI Puente Aranda");
        ruta.setPuntoDestino("Centro Comercial Santafe");
        ruta.setNumeroEntregasProgramadas(4);
        ruta.setEstado(estado);
        return ruta;
    }

    private Entrega entrega(EstadoEntrega estado) {
        Entrega entrega = new Entrega();
        entrega.setEstado(estado);
        return entrega;
    }
}
