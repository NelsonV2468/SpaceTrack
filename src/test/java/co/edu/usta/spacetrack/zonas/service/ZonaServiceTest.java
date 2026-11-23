package co.edu.usta.spacetrack.zonas.service;

import co.edu.usta.spacetrack.zonas.domain.Localidad;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZonaServiceTest {

    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final ZonaService zonaService = new ZonaService(zonaRepository);

    ZonaServiceTest() {
        when(zonaRepository.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(zonaRepository.save(any(Zona.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registraLas19LocalidadesDeBogotaSinMunicipiosVecinos() {
        assertThat(zonaService.asegurarCatalogo()).isEqualTo(19);

        ArgumentCaptor<Zona> guardadas = ArgumentCaptor.forClass(Zona.class);
        verify(zonaRepository, times(19)).save(guardadas.capture());
        assertThat(guardadas.getAllValues()).extracting(Zona::getLocalidad)
                .contains("Usaquén", "Ciudad Bolívar", "La Candelaria").doesNotContain("Soacha", "Funza", "Mosquera");
        assertThat(guardadas.getAllValues().get(1).getCodigo()).isEqualTo("LOC-02");
        assertThat(guardadas.getAllValues()).allSatisfy(z -> assertThat(z.isActiva()).isTrue());
    }

    @Test
    void elCatalogoEsIdempotente() {
        when(zonaRepository.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(new Zona()));

        assertThat(zonaService.asegurarCatalogo()).isZero();
        verify(zonaRepository, never()).save(any());
    }

    @Test
    void noChocaConUnaZonaAntiguaDelMismoNombre() {
        when(zonaRepository.existsByNombreIgnoreCase("Chapinero")).thenReturn(true);

        Zona zona = zonaService.zonaDeLocalidad(Localidad.CHAPINERO);

        assertThat(zona.getNombre()).isEqualTo("Localidad 2 - Chapinero");
        assertThat(zona.getLocalidad()).isEqualTo("Chapinero");
    }

    @Test
    void noOfreceMunicipiosFueraDeBogota() {
        assertThatThrownBy(() -> zonaService.zonaDeLocalidad(Localidad.SOACHA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listaPrimeroElCatalogoYMarcaLasZonasAntiguas() {
        Zona antigua = zona("Z-NORTE", "Zona norte");
        Zona catalogo = zona("LOC-11", "Suba");
        when(zonaRepository.findAllByOrderByCodigoAsc()).thenReturn(List.of(antigua, catalogo));

        assertThat(zonaService.listar()).extracting(ZonaResponse::codigo, ZonaResponse::catalogo)
                .containsExactly(tuple("LOC-11", true), tuple("Z-NORTE", false));
    }

    @Test
    void ofreceElCatalogoDeLocalidadesConCoordenadas() {
        assertThat(zonaService.localidades()).hasSize(22)
                .anySatisfy(l -> {
                    assertThat(l.nombre()).isEqualTo("Kennedy");
                    assertThat(l.latitud()).isEqualTo(4.63);
                });
    }

    private Zona zona(String codigo, String nombre) {
        Zona zona = new Zona();
        zona.setCodigo(codigo);
        zona.setNombre(nombre);
        zona.setLocalidad(nombre);
        zona.setActiva(true);
        return zona;
    }
}
