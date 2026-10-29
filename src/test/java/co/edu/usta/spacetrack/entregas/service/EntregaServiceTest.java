package co.edu.usta.spacetrack.entregas.service;

import co.edu.usta.spacetrack.entregas.domain.Entrega;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.dto.EntregaRequest;
import co.edu.usta.spacetrack.entregas.dto.EntregaResponse;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntregaServiceTest {

    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final EntregaService entregaService = new EntregaService(entregaRepository, rutaRepository);

    @Test
    void registraEntregaEnRutaEnCurso() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.registrar(solicitud(EstadoEntrega.ENTREGADA, null));

        assertThat(respuesta.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(respuesta.destinatario()).isEqualTo("Tienda La 80");
        assertThat(respuesta.numeroGuia()).isNull();
    }

    @Test
    void noRegistraEntregasEnRutaPendiente() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));

        assertThatThrownBy(() -> entregaService.registrar(solicitud(EstadoEntrega.ENTREGADA, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se registran entregas en rutas EN_CURSO; la ruta esta PENDIENTE");
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void noSuperaLasEntregasProgramadas() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(entregaRepository.countByRutaId(any())).thenReturn(3L);

        assertThatThrownBy(() -> entregaService.registrar(solicitud(EstadoEntrega.NO_ENTREGADA, "Local cerrado")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La ruta ya tiene registradas sus 3 entregas programadas");
    }

    @Test
    void exigeNovedadCuandoNoSeEntrega() {
        assertThat(solicitud(EstadoEntrega.NO_ENTREGADA, " ").isNovedadValida()).isFalse();
        assertThat(solicitud(EstadoEntrega.NO_ENTREGADA, "Cliente ausente").isNovedadValida()).isTrue();
        assertThat(solicitud(EstadoEntrega.ENTREGADA, null).isNovedadValida()).isTrue();
    }

    @Test
    void noCorrigeEntregasDeRutaFinalizada() {
        Entrega entrega = new Entrega();
        entrega.setRuta(ruta(EstadoRuta.FINALIZADA));
        when(entregaRepository.findById(9L)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.eliminar(9L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se pueden corregir entregas de rutas EN_CURSO");
        verify(entregaRepository, never()).delete(any());
    }

    private Ruta ruta(EstadoRuta estado) {
        Ruta ruta = new Ruta();
        ruta.setCodigo("R-001");
        ruta.setNumeroEntregasProgramadas(3);
        ruta.setEstado(estado);
        return ruta;
    }

    private EntregaRequest solicitud(EstadoEntrega estado, String novedad) {
        return new EntregaRequest(1L, " ", " Tienda La 80 ", "Calle 80 # 70-15", estado, novedad);
    }
}
