package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RutaServiceTest {

    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final OperacionRepository operacionRepository = mock(OperacionRepository.class);
    private final RutaService rutaService = new RutaService(rutaRepository, zonaRepository, operacionRepository);

    @Test
    void rechazaCodigoDuplicado() {
        when(rutaRepository.existsByCodigoIgnoreCase("R-001")).thenReturn(true);

        assertThatThrownBy(() -> rutaService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una ruta registrada con ese codigo");
    }

    @Test
    void rechazaZonaInactiva() {
        Zona zona = new Zona();
        zona.setActiva(false);
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));

        assertThatThrownBy(() -> rutaService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se pueden asignar rutas a una zona inactiva");
    }

    @Test
    void rechazaOperacionFinalizada() {
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(new Zona()));
        Operacion operacion = new Operacion();
        operacion.setEstado(EstadoOperacion.FINALIZADA);
        when(operacionRepository.findById(1L)).thenReturn(Optional.of(operacion));

        assertThatThrownBy(() -> rutaService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se pueden crear rutas para una operacion finalizada");
    }

    private RutaRequest solicitud() {
        return new RutaRequest("R-001", "Ruta Norte manana", 1L, 1L, "Bodega Calle 13", "Centro Comercial Santafe", 18.5, 12);
    }
}
