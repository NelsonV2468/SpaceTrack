package co.edu.usta.spacetrack.zonas.service;

import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.dto.ZonaRequest;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void guardaElNombreOficialDeLaLocalidadDelCatalogo() {
        when(zonaRepository.save(any(Zona.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ZonaResponse respuesta = zonaService.crear(new ZonaRequest("Z-SUR", "Zona Sur", "localidad de ciudad bolivar", null, true));

        assertThat(respuesta.localidad()).isEqualTo("Ciudad Bolívar");
    }

    @Test
    void rechazaUnaLocalidadQueNoSePuedeUbicarEnElMapa() {
        assertThatThrownBy(() -> zonaService.crear(new ZonaRequest("Z-X", "Zona X", "Medellin", null, true)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageStartingWith("La localidad 'Medellin' no esta en el catalogo. Use una de: Usaquén, Chapinero");
        verify(zonaRepository, never()).save(any());
    }

    @Test
    void ofreceElCatalogoDeLocalidadesConCoordenadas() {
        assertThat(zonaService.localidades()).hasSize(22)
                .anySatisfy(l -> {
                    assertThat(l.nombre()).isEqualTo("Kennedy");
                    assertThat(l.latitud()).isEqualTo(4.63);
                });
    }

    private ZonaRequest solicitud() {
        return new ZonaRequest("Z-NORTE", "Zona Norte", "Usaquen", "Cobertura calle 100 a calle 170", true);
    }
}
