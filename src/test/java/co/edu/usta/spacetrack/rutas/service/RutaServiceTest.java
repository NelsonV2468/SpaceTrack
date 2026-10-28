package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.zonas.domain.Zona;
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

    @Test
    void iniciaRutaPendienteYGuardaHoraDeInicio() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.iniciar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(respuesta.fechaInicioReal()).isNotNull();
        assertThat(respuesta.fechaFinReal()).isNull();
    }

    @Test
    void finalizaRutaEnCursoYGuardaHoraDeFin() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.finalizar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.FINALIZADA);
        assertThat(respuesta.fechaFinReal()).isNotNull();
    }

    @Test
    void noFinalizaRutaQueNoHaIniciado() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));

        assertThatThrownBy(() -> rutaService.finalizar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se puede finalizar una ruta en estado EN_CURSO; la ruta esta PENDIENTE");
    }

    @Test
    void noIniciaRutaFinalizada() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.FINALIZADA)));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se puede iniciar una ruta en estado PENDIENTE; la ruta esta FINALIZADA");
    }

    @Test
    void noEliminaRutaEnCurso() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));

        assertThatThrownBy(() -> rutaService.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo se pueden eliminar rutas en estado PENDIENTE");
        verify(rutaRepository, never()).delete(any());
    }

    @Test
    void noEditaRutaFinalizada() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.FINALIZADA)));

        assertThatThrownBy(() -> rutaService.actualizar(1L, solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se puede editar una ruta finalizada");
    }

    private Ruta ruta(EstadoRuta estado) {
        Ruta ruta = new Ruta();
        ruta.setZona(new Zona());
        ruta.setOperacion(new Operacion());
        ruta.setEstado(estado);
        return ruta;
    }

    private RutaRequest solicitud() {
        return new RutaRequest("R-001", "Ruta Norte manana", 1L, 1L, "Bodega Calle 13", "Centro Comercial Santafe", 18.5, 12);
    }
}
