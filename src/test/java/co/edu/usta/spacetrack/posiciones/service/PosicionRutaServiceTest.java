package co.edu.usta.spacetrack.posiciones.service;

import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;
import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaRequest;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaResponse;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.UbicacionRutaResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.rutas.service.SimuladorUbicacion;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PosicionRutaServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    private final PosicionRutaRepository posicionRepository = mock(PosicionRutaRepository.class);
    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final SimuladorUbicacion simulador = new SimuladorUbicacion();
    private final PosicionRutaService posicionService = new PosicionRutaService(posicionRepository, rutaRepository,
            RELOJ);
    private final GeneradorPosiciones generador = new GeneradorPosiciones(posicionRepository, rutaRepository,
            entregaRepository, simulador, RELOJ);

    @Test
    void registraPosicionManualEnRutaEnCurso() {
        Ruta ruta = ruta("R-001", EstadoRuta.EN_CURSO);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));
        when(posicionRepository.save(any(PosicionRuta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PosicionRutaResponse respuesta = posicionService.registrar(new PosicionRutaRequest(1L, 4.65, -74.08, 30));

        assertThat(posicionGuardada().getRuta()).isSameAs(ruta);
        assertThat(respuesta.codigoRuta()).isEqualTo("R-001");
        assertThat(respuesta.latitud()).isEqualTo(4.65);
        assertThat(respuesta.longitud()).isEqualTo(-74.08);
        assertThat(respuesta.velocidadKmh()).isEqualTo(30);
        assertThat(respuesta.origen()).isEqualTo(OrigenPosicion.MANUAL);
        assertThat(respuesta.fechaRegistro()).isEqualTo(AHORA);
    }

    @Test
    void registraPosicionManualSinVelocidad() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta("R-001", EstadoRuta.EN_CURSO)));
        when(posicionRepository.save(any(PosicionRuta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PosicionRutaResponse respuesta = posicionService.registrar(new PosicionRutaRequest(1L, 4.65, -74.08, null));

        assertThat(respuesta.velocidadKmh()).isNull();
    }

    @Test
    void rechazaRutaInexistente() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> posicionService.registrar(new PosicionRutaRequest(1L, 4.65, -74.08, 30)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe una ruta con id 1");
        verify(posicionRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoRuta.class, names = {"PENDIENTE", "FINALIZADA"})
    void rechazaRutaQueNoEstaEnCurso(EstadoRuta estado) {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta("R-001", estado)));

        assertThatThrownBy(() -> posicionService.registrar(new PosicionRutaRequest(1L, 4.65, -74.08, 30)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se registran posiciones en rutas EN_CURSO; la ruta esta " + estado);
        verify(posicionRepository, never()).save(any());
    }

    @Test
    void generadorGuardaPosicionSimuladaSoloDeRutasEnCurso() {
        Ruta enCurso = ruta("R-002", EstadoRuta.EN_CURSO);
        enCurso.setFechaInicioReal(AHORA.minusSeconds(60));
        List<Ruta> rutas = List.of(ruta("R-001", EstadoRuta.PENDIENTE), enCurso, ruta("R-003", EstadoRuta.FINALIZADA));
        when(rutaRepository.findByEstadoOrderByCodigoAsc(any())).thenAnswer(invocacion ->
                rutas.stream().filter(ruta -> ruta.getEstado() == invocacion.getArgument(0)).toList());

        generador.generar();

        // La hora fijada hace reproducible el punto que calcula el simulador.
        UbicacionRutaResponse esperada = simulador.calcular(enCurso, List.of(), AHORA);
        PosicionRuta posicion = posicionGuardada();
        assertThat(posicion.getRuta()).isSameAs(enCurso);
        assertThat(posicion.getOrigen()).isEqualTo(OrigenPosicion.SIMULADA);
        assertThat(posicion.getLatitud()).isEqualTo(esperada.vehiculo().latitud());
        assertThat(posicion.getLongitud()).isEqualTo(esperada.vehiculo().longitud());
        assertThat(posicion.getVelocidadKmh()).isEqualTo(esperada.velocidadKmh());
        assertThat(posicion.getFechaRegistro()).isEqualTo(AHORA);
    }

    @Test
    void generadorNoGuardaSinRutasEnCurso() {
        when(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO)).thenReturn(List.of());

        generador.generar();

        verify(posicionRepository, never()).save(any());
    }

    @Test
    void listaPosicionesDeUnaRutaConLimite() {
        PosicionRuta posicion = new PosicionRuta();
        posicion.setRuta(ruta("R-001", EstadoRuta.EN_CURSO));
        posicion.setLatitud(4.65);
        when(rutaRepository.existsById(1L)).thenReturn(true);
        when(posicionRepository.findByRutaIdOrderByIdDesc(1L, Limit.of(5))).thenReturn(List.of(posicion));

        List<PosicionRutaResponse> respuesta = posicionService.listar(1L, 5);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).latitud()).isEqualTo(4.65);
    }

    @Test
    void listaPosicionesSinFiltroDeRuta() {
        PosicionRuta posicion = new PosicionRuta();
        posicion.setRuta(ruta("R-001", EstadoRuta.EN_CURSO));
        when(posicionRepository.findAllByOrderByIdDesc(Limit.of(100))).thenReturn(List.of(posicion));

        assertThat(posicionService.listar(null, 100)).hasSize(1);
    }

    @Test
    void rechazaConsultaDeRutaInexistente() {
        when(rutaRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> posicionService.listar(1L, 100))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe una ruta con id 1");
    }

    @Test
    void rechazaLimiteNoPositivo() {
        assertThatThrownBy(() -> posicionService.listar(null, 0))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El limite debe estar entre 1 y 1000");
    }

    private PosicionRuta posicionGuardada() {
        ArgumentCaptor<PosicionRuta> captor = ArgumentCaptor.forClass(PosicionRuta.class);
        verify(posicionRepository).save(captor.capture());
        return captor.getValue();
    }

    private Ruta ruta(String codigo, EstadoRuta estado) {
        Ruta ruta = new Ruta();
        ruta.setCodigo(codigo);
        ruta.setNumeroEntregasProgramadas(3);
        ruta.setEstado(estado);
        return ruta;
    }
}
