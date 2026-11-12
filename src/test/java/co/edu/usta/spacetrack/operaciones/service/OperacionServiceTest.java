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
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
