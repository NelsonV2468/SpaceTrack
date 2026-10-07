package co.edu.usta.spacetrack.zonas.service;

import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.dto.ZonaRequest;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZonaServiceTest {

    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final ZonaService zonaService = new ZonaService(zonaRepository, rutaRepository);

    @Test
    void rechazaCodigoDuplicadoSinDistinguirMayusculas() {
        when(zonaRepository.existsByCodigoIgnoreCase("Z-NORTE")).thenReturn(true);

        assertThatThrownBy(() -> zonaService.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una zona registrada con ese codigo");
    }

    @Test
    void noEliminaZonaConRutasAsociadas() {
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(new Zona()));
        when(rutaRepository.existsByZonaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> zonaService.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se puede eliminar la zona porque tiene rutas asociadas");
        verify(zonaRepository, never()).delete(any());
    }

    private ZonaRequest solicitud() {
        return new ZonaRequest("Z-NORTE", "Zona Norte", "Usaquen", "Cobertura calle 100 a calle 170", true);
    }
}
