package co.edu.usta.spacetrack.conductores.service;

import co.edu.usta.spacetrack.conductores.dto.ConductorRequest;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConductorServiceTest {

    private final ConductorRepository conductorRepository = mock(ConductorRepository.class);
    private final ConductorService conductorService = new ConductorService(conductorRepository);

    @Test
    void rechazaNumeroDeLicenciaDuplicado() {
        when(conductorRepository.existsByNumeroLicenciaIgnoreCase("C12345678")).thenReturn(true);

        assertThatThrownBy(() -> conductorService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe un conductor registrado con esa licencia");
    }

    private ConductorRequest solicitud() {
        return new ConductorRequest("CC", "123456789", "Ana", "Lopez", "3001234567", "ana@correo.co", "C12345678", LocalDate.now().plusYears(2), true);
    }
}
