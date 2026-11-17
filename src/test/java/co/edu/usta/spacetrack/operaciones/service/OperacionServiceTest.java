package co.edu.usta.spacetrack.operaciones.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.OperacionResponse;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
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

class OperacionServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

    private final OperacionRepository operacionRepository = mock(OperacionRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final NecesidadClienteRepository necesidadRepository = mock(NecesidadClienteRepository.class);
    private final OperacionService operacionService = new OperacionService(operacionRepository, clienteRepository,
            rutaRepository, necesidadRepository, Clock.fixed(HOY.atTime(9, 0).atZone(BOGOTA).toInstant(), BOGOTA));

    private final Cliente cliente = new Cliente();

    OperacionServiceTest() {
        cliente.setRazonSocial("Alimentos Urbanos SAS");
        cliente.setActivo(true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(operacionRepository.save(any(Operacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registraOperacionActiva() {
        OperacionResponse respuesta = operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a sábado"));

        assertThat(respuesta.codigoContrato()).isEqualTo("CT-001");
        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.ACTIVA);
    }

    @Test
    void rechazaOperacionVigenteParaUnClienteInactivo() {
        cliente.setActivo(false);

        assertThatThrownBy(() -> operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El cliente Alimentos Urbanos SAS esta inactivo; solo se le pueden registrar operaciones finalizadas");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void permiteFinalizarLaOperacionDeUnClienteInactivo() {
        cliente.setActivo(false);
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.FINALIZADA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.FINALIZADA);
    }

    @Test
    void permiteReactivarUnaOperacionPausada() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.PAUSADA)));

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.ACTIVA);
    }

    @Test
    void noReabreUnaOperacionFinalizada() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.FINALIZADA)));

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Una operacion finalizada no se puede reabrir");
    }

    @Test
    void noFinalizaUnaOperacionConRutasEnCurso() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(rutaRepository.existsByOperacionIdAndEstado(5L, EstadoRuta.EN_CURSO)).thenReturn(true);

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.FINALIZADA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se puede finalizar la operacion mientras tenga rutas en curso");
    }

    @Test
    void permiteFinalizarUnaOperacionVencida() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.FINALIZADA, HOY.minusDays(10), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.FINALIZADA);
    }

    @Test
    void exigeAmpliarVigenciaOFinalizarUnaOperacionVencida() {
        assertThatThrownBy(() -> operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY.minusDays(1), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La fecha final ya paso; amplie la vigencia o marque la operacion como FINALIZADA");
    }

    @Test
    void rechazaDiasDeOperacionQueNoSePuedenInterpretar() {
        assertThatThrownBy(() -> operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Entre semana")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageStartingWith("No se reconocen los dias de operacion 'Entre semana'");
    }

    @Test
    void noReduceLasEntregasEsperadasPorDebajoDeLasProgramadasEnRutas() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(rutaRepository.sumarEntregasProgramadasPorOperacion(5L)).thenReturn(150L);

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Las rutas del contrato ya programan 150 entregas; las entregas esperadas no pueden ser menos");
    }

    // ---- ON-6: cobertura de exito y fallo por cada regla del servicio ----

    @Test
    void normalizaElCodigoYLosTextosAlRegistrar() {
        OperacionRequest conEspacios = new OperacionRequest("  ct-009 ", 1L, "  Distribucion urbana  ", HOY.minusMonths(1),
                HOY.plusMonths(3), "  Lunes a viernes ", 2, 100, true, EstadoOperacion.ACTIVA);

        OperacionResponse respuesta = operacionService.crear(conEspacios);

        assertThat(respuesta.codigoContrato()).isEqualTo("CT-009");
        assertThat(respuesta.nombreOperacion()).isEqualTo("Distribucion urbana");
        assertThat(respuesta.diasOperacion()).isEqualTo("Lunes a viernes");
        assertThat(respuesta.razonSocialCliente()).isEqualTo("Alimentos Urbanos SAS");
        assertThat(respuesta.requiereCadenaFrio()).isTrue();
        verify(operacionRepository).existsByCodigoContratoIgnoreCase("ct-009");
    }

    @Test
    void rechazaUnCodigoDeContratoRepetidoAlRegistrar() {
        when(operacionRepository.existsByCodigoContratoIgnoreCase("ct-001")).thenReturn(true);

        assertThatThrownBy(() -> operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una operacion registrada con ese codigo de contrato");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void permiteConservarSuPropioCodigoAlActualizar() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.codigoContrato()).isEqualTo("CT-001");
        verify(operacionRepository).existsByCodigoContratoIgnoreCaseAndIdNot("ct-001", 5L);
    }

    @Test
    void rechazaElCodigoDeOtraOperacionAlActualizar() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(operacionRepository.existsByCodigoContratoIgnoreCaseAndIdNot("ct-001", 5L)).thenReturn(true);

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una operacion registrada con ese codigo de contrato");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void rechazaUnClienteQueNoExiste() {
        OperacionRequest sinCliente = new OperacionRequest("ct-001", 99L, "Distribucion urbana", HOY.minusMonths(1),
                HOY.plusMonths(3), "Lunes a viernes", 2, 100, false, EstadoOperacion.ACTIVA);

        assertThatThrownBy(() -> operacionService.crear(sinCliente))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe un cliente con id 99");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void consultaUnaOperacionExistente() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.PAUSADA)));

        OperacionResponse respuesta = operacionService.obtenerPorId(5L);

        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.PAUSADA);
    }

    @Test
    void informaCuandoLaOperacionNoExiste() {
        when(operacionRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operacionService.obtenerPorId(77L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe una operacion con id 77");
        assertThatThrownBy(() -> operacionService.actualizar(77L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> operacionService.eliminar(77L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(operacionRepository, never()).delete(any());
    }

    @Test
    void eliminaUnaOperacionExistente() {
        Operacion operacion = existente(EstadoOperacion.ACTIVA);
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(operacion));

        operacionService.eliminar(5L);

        verify(operacionRepository).delete(operacion);
    }

    @Test
    void listaLasOperacionesDelRepositorio() {
        when(operacionRepository.findAllByOrderByFechaInicioDesc())
                .thenReturn(List.of(existente(EstadoOperacion.ACTIVA), existente(EstadoOperacion.FINALIZADA)));

        assertThat(operacionService.listar()).extracting(OperacionResponse::estado)
                .containsExactly(EstadoOperacion.ACTIVA, EstadoOperacion.FINALIZADA);
    }

    @Test
    void permitePausarUnaOperacionActiva() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.PAUSADA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.PAUSADA);
    }

    @Test
    void permiteEditarUnaOperacionYaFinalizadaSinReabrirla() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.FINALIZADA)));
        // Aunque tenga rutas en curso, no se vuelve a validar porque ya estaba finalizada.
        when(rutaRepository.existsByOperacionIdAndEstado(5L, EstadoRuta.EN_CURSO)).thenReturn(true);

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.FINALIZADA, HOY.minusDays(30), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.FINALIZADA);
    }

    @Test
    void permiteFinalizarCuandoNoHayRutasEnCurso() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.PAUSADA)));
        when(rutaRepository.existsByOperacionIdAndEstado(5L, EstadoRuta.EN_CURSO)).thenReturn(false);

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.FINALIZADA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.estado()).isEqualTo(EstadoOperacion.FINALIZADA);
    }

    @Test
    void aceptaUnaOperacionQueTerminaHoy() {
        OperacionResponse respuesta = operacionService.crear(solicitud(EstadoOperacion.ACTIVA, HOY, "Todos los dias"));

        assertThat(respuesta.fechaFin()).isEqualTo(HOY);
    }

    @Test
    void rechazaDiasDeOperacionTambienAlActualizar() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Cuando haya pedidos")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageStartingWith("No se reconocen los dias de operacion 'Cuando haya pedidos'");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void permiteEntregasEsperadasIgualesALasProgramadasEnRutas() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(rutaRepository.sumarEntregasProgramadasPorOperacion(5L)).thenReturn(100L);

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.numeroEntregasEsperadas()).isEqualTo(100);
    }

    @Test
    void noReduceLosVehiculosRequeridosPorDebajoDeLasNecesidades() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(necesidadRepository.sumarVehiculosPorOperacion(5L)).thenReturn(3L);

        assertThatThrownBy(() -> operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Las necesidades del contrato ya suman 3 vehiculos; los vehiculos requeridos no pueden ser menos");
        verify(operacionRepository, never()).save(any());
    }

    @Test
    void permiteVehiculosRequeridosIgualesALasNecesidades() {
        when(operacionRepository.findById(5L)).thenReturn(Optional.of(existente(EstadoOperacion.ACTIVA)));
        when(necesidadRepository.sumarVehiculosPorOperacion(5L)).thenReturn(2L);

        OperacionResponse respuesta = operacionService.actualizar(5L, solicitud(EstadoOperacion.ACTIVA, HOY.plusMonths(3), "Lunes a viernes"));

        assertThat(respuesta.numeroVehiculosRequeridos()).isEqualTo(2);
    }

    private Operacion existente(EstadoOperacion estado) {
        Operacion operacion = new Operacion() {
            @Override
            public Long getId() {
                return 5L;
            }
        };
        operacion.setEstado(estado);
        operacion.setCliente(cliente);
        return operacion;
    }

    private OperacionRequest solicitud(EstadoOperacion estado, LocalDate fechaFin, String dias) {
        return new OperacionRequest("ct-001", 1L, "Distribucion urbana", HOY.minusMonths(1), fechaFin, dias,
                2, 100, false, estado);
    }
}
