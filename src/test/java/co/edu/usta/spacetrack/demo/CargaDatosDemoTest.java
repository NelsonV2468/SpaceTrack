package co.edu.usta.spacetrack.demo;

import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.clientes.service.ClienteService;
import co.edu.usta.spacetrack.conductores.service.ConductorService;
import co.edu.usta.spacetrack.entregas.service.EntregaService;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
import co.edu.usta.spacetrack.operaciones.service.OperacionService;
import co.edu.usta.spacetrack.rutas.service.RutaService;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.service.VehiculoService;
import co.edu.usta.spacetrack.zonas.service.ZonaService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Franja horaria de la necesidad demo y comportamiento de la carga cuando algo falla. */
class CargaDatosDemoTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final ClienteService clienteService = mock(ClienteService.class);
    private final PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);
    private final TransactionStatus estado = new SimpleTransactionStatus();
    private final CargaDatosDemo carga = new CargaDatosDemo(clienteRepository, clienteService, mock(OperacionService.class),
            mock(NecesidadClienteService.class), mock(VehiculoService.class), mock(ConductorService.class),
            mock(ZonaService.class), mock(RutaService.class), mock(EntregaService.class),
            Clock.system(ZoneId.of("America/Bogota")), transacciones, 10);

    CargaDatosDemoTest() {
        when(transacciones.getTransaction(any())).thenReturn(estado);
    }

    @Test
    void enHorarioDeOficinaUsaLaFranjaTipica() {
        LocalTime ahora = LocalTime.of(10, 15);

        assertThat(CargaDatosDemo.horaInicioFranja(ahora)).isEqualTo(LocalTime.of(6, 0));
        assertThat(CargaDatosDemo.horaFinFranja(ahora)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void deMadrugadaAdelantaElInicio() {
        assertThat(CargaDatosDemo.horaInicioFranja(LocalTime.of(3, 40))).isEqualTo(LocalTime.of(2, 0));
        assertThat(CargaDatosDemo.horaInicioFranja(LocalTime.of(0, 20))).isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void enLaNocheDejaAlMenosDosHorasOTerminaAlFinalDelDia() {
        assertThat(CargaDatosDemo.horaFinFranja(LocalTime.of(19, 10))).isEqualTo(LocalTime.of(21, 10));
        assertThat(CargaDatosDemo.horaFinFranja(LocalTime.of(22, 30))).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    void laFranjaSiempreContieneLaHoraDeArranqueIncluidosLosSegundos() {
        for (int minuto = 0; minuto < 24 * 60; minuto++) {
            LocalTime ahora = LocalTime.MIDNIGHT.plusMinutes(minuto);
            LocalTime inicio = CargaDatosDemo.horaInicioFranja(ahora);
            LocalTime fin = CargaDatosDemo.horaFinFranja(ahora);
            assertThat(fin).as("fin a las %s", ahora).isAfter(inicio);
            // La carga calcula la franja con el minuto, pero la ruta se inicia con la hora exacta (hasta :59 s).
            assertThat(ahora).as("arranque a las %s", ahora).isBetween(inicio, fin);
            assertThat(ahora.plusSeconds(59)).as("arranque a las %s:59", ahora).isBetween(inicio, fin);
        }
    }

    @Test
    void siUnaReglaRechazaLaCargaReintentaYNoImpideElArranque() {
        when(clienteService.crear(any(ClienteRequest.class))).thenThrow(new ReglaDeNegocioException("NIT repetido"));

        assertThatCode(() -> carga.run(null)).doesNotThrowAnyException();

        verify(clienteService, times(CargaDatosDemo.INTENTOS)).crear(any());
        // Cada intento se revierte completo: no queda nada a medias.
        verify(transacciones, times(CargaDatosDemo.INTENTOS)).rollback(estado);
        verify(transacciones, never()).commit(any());
    }

    @Test
    void siFallaUnaVezElReintentoSeHaceEnUnaTransaccionNueva() {
        // Primer intento: falla (por ejemplo, el dia cambio a medianoche). Segundo: ya hay clientes y no repite.
        when(clienteRepository.count()).thenReturn(0L, 1L);
        when(clienteService.crear(any(ClienteRequest.class))).thenThrow(new ReglaDeNegocioException("El dia cambio"));

        assertThatCode(() -> carga.run(null)).doesNotThrowAnyException();

        verify(clienteService, times(1)).crear(any());
        verify(transacciones, times(2)).getTransaction(any());
        verify(transacciones).rollback(estado);
        verify(transacciones).commit(estado);
    }

    @Test
    void siYaHayClientesNoCreaNada() {
        when(clienteRepository.count()).thenReturn(3L);

        carga.run(null);

        verify(clienteService, never()).crear(any());
        verify(transacciones).commit(estado);
    }
}
