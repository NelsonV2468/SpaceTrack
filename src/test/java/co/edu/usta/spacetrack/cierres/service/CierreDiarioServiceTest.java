package co.edu.usta.spacetrack.cierres.service;

import co.edu.usta.spacetrack.cierres.domain.CierreDiario;
import co.edu.usta.spacetrack.cierres.domain.CierreDiarioRuta;
import co.edu.usta.spacetrack.cierres.dto.CierreDiarioRequest;
import co.edu.usta.spacetrack.cierres.dto.CierreDiarioResponse;
import co.edu.usta.spacetrack.cierres.repository.CierreDiarioRepository;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CierreDiarioServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);
    private static final LocalDateTime AHORA = HOY.atTime(19, 0);

    private final CierreDiarioRepository cierreRepository = mock(CierreDiarioRepository.class);
    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final CierreDiarioService servicio = new CierreDiarioService(cierreRepository, rutaRepository, entregaRepository,
            Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));

    @Test
    void laVistaPreviaSumaLasRutasFinalizadasEseDia() {
        finalizadasHoy(ruta(1L, "R-1", 10, 8, 1), ruta(2L, "R-2", 5, 5, 0));

        CierreDiarioResponse resumen = servicio.resumen(HOY);

        assertThat(resumen.cerrado()).isFalse();
        assertThat(resumen.puedeCerrar()).isTrue();
        assertThat(resumen.rutasCerradas()).isEqualTo(2);
        assertThat(resumen.entregasProgramadas()).isEqualTo(15);
        assertThat(resumen.entregasRealizadas()).isEqualTo(13);
        assertThat(resumen.entregasNoRealizadas()).isEqualTo(1);
        assertThat(resumen.entregasSinRegistrar()).isEqualTo(1);
        assertThat(resumen.cumplimiento()).isEqualTo(86.7);
        assertThat(resumen.rutas()).extracting("codigoRuta").containsExactly("R-1", "R-2");
    }

    @Test
    void consultaLasRutasDelDiaCompletoDeBogota() {
        servicio.resumen(HOY);

        verify(rutaRepository).findByEstadoAndFechaFinRealGreaterThanEqualAndFechaFinRealLessThanOrderByCodigoAsc(
                EstadoRuta.FINALIZADA, HOY.atStartOfDay(), HOY.plusDays(1).atStartOfDay());
        verify(rutaRepository).findByEstadoAndFechaInicioRealLessThanOrderByCodigoAsc(EstadoRuta.EN_CURSO, HOY.plusDays(1).atStartOfDay());
    }

    @Test
    void laVistaPreviaAvisaQueRutasImpidenCerrar() {
        Ruta enCurso = ruta(3L, "R-3", 4, 0, 0);
        when(rutaRepository.findByEstadoAndFechaInicioRealLessThanOrderByCodigoAsc(eq(EstadoRuta.EN_CURSO), any()))
                .thenReturn(List.of(enCurso));

        CierreDiarioResponse resumen = servicio.resumen(HOY);

        assertThat(resumen.puedeCerrar()).isFalse();
        assertThat(resumen.rutasEnCurso()).containsExactly("R-3");
        assertThat(resumen.motivoNoCierre()).isEqualTo("Finalice primero las rutas en curso: R-3");
    }

    @Test
    void cierraElDiaYGuardaUnaCopiaDeLasRutas() {
        finalizadasHoy(ruta(1L, "R-1", 10, 8, 1));
        when(cierreRepository.save(any(CierreDiario.class))).thenAnswer(i -> i.getArgument(0));

        CierreDiarioResponse cierre = servicio.cerrar(new CierreDiarioRequest(HOY, "  Lluvia en la tarde  "), "jefe@spacetrack.co");

        assertThat(cierre.cerrado()).isTrue();
        assertThat(cierre.registradoPor()).isEqualTo("jefe@spacetrack.co");
        assertThat(cierre.fechaRegistro()).isEqualTo(AHORA);
        assertThat(cierre.observaciones()).isEqualTo("Lluvia en la tarde");
        assertThat(cierre.entregasSinRegistrar()).isEqualTo(1);

        ArgumentCaptor<CierreDiario> guardado = ArgumentCaptor.forClass(CierreDiario.class);
        verify(cierreRepository).save(guardado.capture());
        CierreDiarioRuta detalle = guardado.getValue().getRutas().get(0);
        assertThat(detalle.getCodigoRuta()).isEqualTo("R-1");
        assertThat(detalle.getNombreZona()).isEqualTo("Norte");
        assertThat(detalle.getEntregasRealizadas()).isEqualTo(8);
    }

    @Test
    void noCierraConRutasEnCurso() {
        List<Ruta> enCurso = List.of(ruta(3L, "R-3", 4, 0, 0), ruta(4L, "R-4", 2, 1, 0));
        when(rutaRepository.findByEstadoAndFechaInicioRealLessThanOrderByCodigoAsc(eq(EstadoRuta.EN_CURSO), any()))
                .thenReturn(enCurso);

        assertThatThrownBy(() -> servicio.cerrar(new CierreDiarioRequest(HOY, null), "jefe"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se puede cerrar el dia 2026-11-18: finalice primero las rutas en curso R-3, R-4");
        verify(cierreRepository, never()).save(any());
    }

    @Test
    void noCierraDosVecesElMismoDia() {
        when(cierreRepository.existsByFecha(HOY)).thenReturn(true);

        assertThatThrownBy(() -> servicio.cerrar(new CierreDiarioRequest(HOY, null), "jefe"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El dia 2026-11-18 ya fue cerrado");
        verify(cierreRepository, never()).save(any());
    }

    @Test
    void noCierraNiConsultaDiasFuturos() {
        assertThatThrownBy(() -> servicio.cerrar(new CierreDiarioRequest(HOY.plusDays(1), null), "jefe"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se puede consultar ni cerrar un dia futuro (2026-11-19)");
        assertThatThrownBy(() -> servicio.resumen(HOY.plusDays(1))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void permiteCerrarUnDiaSinRutas() {
        when(cierreRepository.save(any(CierreDiario.class))).thenAnswer(i -> i.getArgument(0));

        CierreDiarioResponse cierre = servicio.cerrar(new CierreDiarioRequest(HOY.minusDays(3), ""), "jefe");

        assertThat(cierre.rutasCerradas()).isZero();
        assertThat(cierre.cumplimiento()).isZero();
        assertThat(cierre.observaciones()).isNull();
    }

    @Test
    void unDiaCerradoDevuelveLosDatosGuardadosYNoLosActuales() {
        CierreDiario guardado = new CierreDiario();
        guardado.setFecha(HOY);
        guardado.setFechaRegistro(AHORA);
        guardado.setRegistradoPor("jefe");
        guardado.agregarRuta(new CierreDiarioRuta(1L, "R-1", "Ruta", "Norte", "ABC123", "Ana Ruiz", null, null, 10, 10, 0));
        when(cierreRepository.findByFecha(HOY)).thenReturn(Optional.of(guardado));

        CierreDiarioResponse resumen = servicio.resumen(HOY);

        assertThat(resumen.cerrado()).isTrue();
        assertThat(resumen.puedeCerrar()).isFalse();
        assertThat(resumen.cumplimiento()).isEqualTo(100.0);
        verify(rutaRepository, never()).findByEstadoAndFechaFinRealGreaterThanEqualAndFechaFinRealLessThanOrderByCodigoAsc(any(), any(), any());
    }

    private void finalizadasHoy(Ruta... rutas) {
        // Las rutas se construyen antes (ruta() tambien prepara los mocks de entregas).
        when(rutaRepository.findByEstadoAndFechaFinRealGreaterThanEqualAndFechaFinRealLessThanOrderByCodigoAsc(
                eq(EstadoRuta.FINALIZADA), any(), any())).thenReturn(List.of(rutas));
    }

    private Ruta ruta(Long id, String codigo, int programadas, long entregadas, long noEntregadas) {
        Zona zona = new Zona();
        zona.setNombre("Norte");
        Ruta ruta = new Ruta() {
            @Override
            public Long getId() {
                return id;
            }
        };
        ruta.setCodigo(codigo);
        ruta.setNombre("Ruta " + codigo);
        ruta.setZona(zona);
        ruta.setNumeroEntregasProgramadas(programadas);
        ruta.setFechaInicioReal(AHORA.minusHours(8));
        ruta.setFechaFinReal(AHORA.minusHours(1));
        when(entregaRepository.countByRutaIdAndEstado(id, EstadoEntrega.ENTREGADA)).thenReturn(entregadas);
        when(entregaRepository.countByRutaIdAndEstado(id, EstadoEntrega.NO_ENTREGADA)).thenReturn(noEntregadas);
        return ruta;
    }
}
