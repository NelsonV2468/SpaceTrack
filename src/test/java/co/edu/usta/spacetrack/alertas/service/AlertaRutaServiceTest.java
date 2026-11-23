package co.edu.usta.spacetrack.alertas.service;

import co.edu.usta.spacetrack.alertas.domain.NivelAlerta;
import co.edu.usta.spacetrack.alertas.dto.AlertaRutaResponse;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.rutas.service.DetectorDesvio;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertaRutaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalTime FIN_FRANJA = LocalTime.of(14, 0);

    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final NecesidadClienteRepository necesidadRepository = mock(NecesidadClienteRepository.class);
    private final DetectorDesvio detector = mock(DetectorDesvio.class);

    // Miercoles 18 de noviembre de 2026; la franja del contrato es 06:00-14:00 de lunes a viernes.
    private AlertaRutaService servicioALas(int hora, int minuto) {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 18, hora, minuto);
        return new AlertaRutaService(rutaRepository, entregaRepository, necesidadRepository, detector,
                Clock.fixed(ahora.atZone(BOGOTA).toInstant(), BOGOTA), 60, 10);
    }

    @Test
    void sinAlertaCuandoFaltaMuchoParaElCierre() {
        prepararRuta(1L, 10, 7, LocalDateTime.of(2026, 11, 18, 7, 0));

        assertThat(servicioALas(10, 0).alertasActivas()).isEmpty();
    }

    @Test
    void advierteCuandoLaFranjaTerminaEnMenosDeUnaHora() {
        prepararRuta(1L, 10, 7, LocalDateTime.of(2026, 11, 18, 7, 0));

        List<AlertaRutaResponse> alertas = servicioALas(13, 15).alertasActivas();

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.nivel()).isEqualTo(NivelAlerta.ADVERTENCIA);
            assertThat(a.entregasPendientes()).isEqualTo(3);
            assertThat(a.minutosRestantes()).isEqualTo(45);
            assertThat(a.horaFinFranja()).isEqualTo(FIN_FRANJA);
            assertThat(a.mensaje()).isEqualTo("La ruta R-1 (zona Norte - Suba) tiene 3 de 10 entregas pendientes; "
                    + "su franja horaria termina en 45 min (14:00)");
        });
    }

    @Test
    void esCriticaCuandoElTiempoNoAlcanzaParaLasEntregasPendientes() {
        // 8 pendientes x 10 min = 80 min estimados, pero solo faltan 70.
        prepararRuta(1L, 10, 2, LocalDateTime.of(2026, 11, 18, 7, 0));

        AlertaRutaResponse alerta = servicioALas(12, 50).alertasActivas().get(0);

        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.CRITICA);
        assertThat(alerta.minutosEstimadosPendientes()).isEqualTo(80);
        assertThat(alerta.mensaje()).endsWith("faltan 70 min para las 14:00 y se estiman 80 min para completarlas");
    }

    @Test
    void esCriticaCuandoLaFranjaYaTermino() {
        prepararRuta(1L, 10, 9, LocalDateTime.of(2026, 11, 18, 7, 0));

        AlertaRutaResponse alerta = servicioALas(14, 20).alertasActivas().get(0);

        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.CRITICA);
        assertThat(alerta.minutosRestantes()).isEqualTo(-20);
        assertThat(alerta.mensaje()).endsWith("y su franja horaria termino a las 14:00");
    }

    @Test
    void esCriticaCuandoLaRutaSigueAbiertaDesdeUnDiaAnterior() {
        prepararRuta(1L, 10, 4, LocalDateTime.of(2026, 11, 17, 8, 0));

        AlertaRutaResponse alerta = servicioALas(7, 0).alertasActivas().get(0);

        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.CRITICA);
        assertThat(alerta.mensaje()).isEqualTo("La ruta R-1 sigue en curso desde el 2026-11-17 con 6 entregas pendientes "
                + "en la zona Norte - Suba");
    }

    @Test
    void sinAlertaCuandoTodasLasEntregasEstanRegistradas() {
        prepararRuta(1L, 10, 10, LocalDateTime.of(2026, 11, 18, 7, 0));

        assertThat(servicioALas(13, 50).alertasActivas()).isEmpty();
    }

    @Test
    void sinAlertaCuandoElContratoNoTieneFranjaParaHoy() {
        prepararRuta(1L, 10, 2, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.SABADO), LocalTime.of(6, 0), FIN_FRANJA)));

        assertThat(servicioALas(13, 50).alertasActivas()).isEmpty();
    }

    @Test
    void usaLaFranjaMasTardiaQueYaEmpezo() {
        prepararRuta(1L, 10, 5, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(anyLong())).thenReturn(List.of(
                necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(6, 0), FIN_FRANJA),
                necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(12, 0), LocalTime.of(18, 0)),
                necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(20, 0), LocalTime.of(23, 0))));

        // 13:30: la franja 06-14 y la 12-18 ya empezaron; la de las 20:00 no cuenta.
        assertThat(servicioALas(13, 30).alertasActivas()).isEmpty();
        assertThat(servicioALas(17, 15).alertasActivas()).singleElement()
                .satisfies(a -> assertThat(a.horaFinFranja()).isEqualTo(LocalTime.of(18, 0)));
    }

    @Test
    void ordenaPrimeroLasCriticas() {
        Ruta advertencia = ruta(1L, "R-1", 10, LocalDateTime.of(2026, 11, 18, 7, 0));
        Ruta critica = ruta(2L, "R-2", 10, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO)).thenReturn(List.of(advertencia, critica));
        when(entregaRepository.countByRutaId(1L)).thenReturn(8L);
        when(entregaRepository.countByRutaId(2L)).thenReturn(1L);
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(6, 0), FIN_FRANJA)));

        List<AlertaRutaResponse> alertas = servicioALas(13, 15).alertasActivas();

        assertThat(alertas).extracting(AlertaRutaResponse::codigoRuta).containsExactly("R-2", "R-1");
        assertThat(alertas).extracting(AlertaRutaResponse::nivel).containsExactly(NivelAlerta.CRITICA, NivelAlerta.ADVERTENCIA);
    }

    @Test
    void consultaLaAlertaDeUnaRutaConcreta() {
        Ruta ruta = prepararRuta(1L, 10, 7, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThat(servicioALas(13, 15).alertasDeRuta(1L)).hasSize(1);
        ruta.setEstado(EstadoRuta.FINALIZADA);
        assertThat(servicioALas(13, 15).alertasDeRuta(1L)).isEmpty();
    }

    @Test
    void alertaCuandoElVehiculoSeDesviaDeLaRuta() {
        Ruta ruta = prepararRuta(1L, 10, 9, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(detector.evaluar(org.mockito.ArgumentMatchers.eq(ruta), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new DetectorDesvio.PosicionReportada(4.76, -74.13, 20,
                        LocalDateTime.of(2026, 11, 18, 9, 55), 3.4, true, true)));

        List<AlertaRutaResponse> alertas = servicioALas(10, 0).alertasActivas();

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(co.edu.usta.spacetrack.alertas.domain.TipoAlerta.DESVIO_RUTA);
            assertThat(a.nivel()).isEqualTo(NivelAlerta.CRITICA);
            assertThat(a.distanciaDesvioKm()).isEqualTo(3.4);
            assertThat(a.mensaje()).contains("se desvio de la ruta").contains("3.4 km").contains("Norte - Suba");
        });
    }

    @Test
    void sinAlertaDeDesvioSiLaPosicionEstaDentroDelRecorrido() {
        Ruta ruta = prepararRuta(1L, 10, 9, LocalDateTime.of(2026, 11, 18, 7, 0));
        when(detector.evaluar(org.mockito.ArgumentMatchers.eq(ruta), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new DetectorDesvio.PosicionReportada(4.74, -74.08, 20,
                        LocalDateTime.of(2026, 11, 18, 9, 55), 0.2, false, false)));

        assertThat(servicioALas(10, 0).alertasActivas()).isEmpty();
    }

    private Ruta prepararRuta(Long id, int programadas, long registradas, LocalDateTime inicio) {
        Ruta ruta = ruta(id, "R-" + id, programadas, inicio);
        when(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO)).thenReturn(List.of(ruta));
        when(entregaRepository.countByRutaId(id)).thenReturn(registradas);
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MIERCOLES,
                        DiaSemana.JUEVES, DiaSemana.VIERNES), LocalTime.of(6, 0), FIN_FRANJA)));
        return ruta;
    }

    private Ruta ruta(Long id, String codigo, int programadas, LocalDateTime inicio) {
        Zona zona = new Zona();
        zona.setNombre("Norte");
        zona.setLocalidad("Suba");
        Operacion operacion = new Operacion() {
            @Override
            public Long getId() {
                return 10L;
            }
        };
        operacion.setCodigoContrato("CT-001");
        Ruta ruta = new Ruta() {
            @Override
            public Long getId() {
                return id;
            }
        };
        ruta.setCodigo(codigo);
        ruta.setNombre("Ruta " + codigo);
        ruta.setZona(zona);
        ruta.setOperacion(operacion);
        ruta.setNumeroEntregasProgramadas(programadas);
        ruta.setEstado(EstadoRuta.EN_CURSO);
        ruta.setFechaInicioReal(inicio);
        return ruta;
    }

    private NecesidadCliente necesidad(Set<DiaSemana> dias, LocalTime inicio, LocalTime fin) {
        NecesidadCliente necesidad = new NecesidadCliente();
        necesidad.setDiasOperacion(dias);
        necesidad.setHoraInicio(inicio);
        necesidad.setHoraFin(fin);
        return necesidad;
    }
}
