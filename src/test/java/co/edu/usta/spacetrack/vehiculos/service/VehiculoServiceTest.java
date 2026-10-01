package co.edu.usta.spacetrack.vehiculos.service;

import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoRequest;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehiculoServiceTest {

    private final VehiculoRepository vehiculoRepository = mock(VehiculoRepository.class);
    private final VehiculoService vehiculoService = new VehiculoService(vehiculoRepository);

    @Test
    void rechazaPlacaDuplicadaSinDistinguirMayusculas() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("ABC123")).thenReturn(true);

        assertThatThrownBy(() -> vehiculoService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe un vehiculo registrado con esa placa");
    }

    private VehiculoRequest solicitud() {
        return new VehiculoRequest("ABC123", "Camion", "Chevrolet", "NPR", 2024, 5000.0, false, true);
    }
}
