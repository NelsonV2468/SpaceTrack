package co.edu.usta.spacetrack.necesidades.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteRequest;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteResponse;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NecesidadClienteServiceTest {

    private final NecesidadClienteRepository necesidadRepository = mock(NecesidadClienteRepository.class);
    private final OperacionRepository operacionRepository = mock(OperacionRepository.class);
    private final VehiculoRepository vehiculoRepository = mock(VehiculoRepository.class);
    private final NecesidadClienteService necesidadService =
            new NecesidadClienteService(necesidadRepository, operacionRepository, vehiculoRepository);

    @Test
    void rechazaOperacionFinalizada() {
        when(operacionRepository.findById(1L)).thenReturn(Optional.of(operacion(EstadoOperacion.FINALIZADA, 3)));

        assertThatThrownBy(() -> necesidadService.crear(solicitud(2)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se pueden registrar necesidades en una operacion finalizada");
    }

    @Test
    void rechazaCuandoSuperaLosVehiculosDelContrato() {
        when(operacionRepository.findById(1L)).thenReturn(Optional.of(operacion(EstadoOperacion.ACTIVA, 3)));
        when(necesidadRepository.sumarVehiculosPorOperacion(any())).thenReturn(2L);

        assertThatThrownBy(() -> necesidadService.crear(solicitud(2)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La operacion solo admite 3 vehiculos y ya tiene 2 registrados en sus necesidades");
    }

    @Test
    void informaSiLaFlotaNoAlcanza() {
        when(operacionRepository.findById(1L)).thenReturn(Optional.of(operacion(EstadoOperacion.ACTIVA, 3)));
        when(necesidadRepository.save(any(NecesidadCliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(vehiculoRepository.contarDisponibles("Camion", 3000.0, true)).thenReturn(1L);

        NecesidadClienteResponse respuesta = necesidadService.crear(solicitud(2));

        assertThat(respuesta.vehiculosDisponibles()).isEqualTo(1L);
        assertThat(respuesta.flotaSuficiente()).isFalse();
        assertThat(respuesta.diasOperacion()).containsExactly(DiaSemana.LUNES, DiaSemana.MIERCOLES, DiaSemana.VIERNES);
    }

    private Operacion operacion(EstadoOperacion estado, int vehiculosRequeridos) {
        Operacion operacion = new Operacion();
        operacion.setCliente(new Cliente());
        operacion.setEstado(estado);
        operacion.setNumeroVehiculosRequeridos(vehiculosRequeridos);
        return operacion;
    }

    private NecesidadClienteRequest solicitud(int cantidad) {
        return new NecesidadClienteRequest(1L, "Camion", cantidad, 3000.0, true,
                Set.of(DiaSemana.VIERNES, DiaSemana.LUNES, DiaSemana.MIERCOLES),
                LocalTime.of(6, 0), LocalTime.of(14, 0), "Entregas en supermercados");
    }
}
