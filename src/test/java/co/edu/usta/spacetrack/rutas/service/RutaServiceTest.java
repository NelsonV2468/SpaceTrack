package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.conductores.domain.Conductor;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.AsignacionRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RutaServiceTest {

    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final OperacionRepository operacionRepository = mock(OperacionRepository.class);
    private final VehiculoRepository vehiculoRepository = mock(VehiculoRepository.class);
    private final ConductorRepository conductorRepository = mock(ConductorRepository.class);
    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final RutaService rutaService = new RutaService(rutaRepository, zonaRepository, operacionRepository,
            vehiculoRepository, conductorRepository, entregaRepository, new SimuladorUbicacion());

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
        Ruta ruta = ruta(EstadoRuta.PENDIENTE);
        ruta.setVehiculo(vehiculo(true, false));
        ruta.setConductor(conductor(LocalDate.now().plusYears(1)));
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));
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

    @Test
    void noIniciaRutaSinVehiculoNiConductor() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Asigne un vehiculo y un conductor antes de iniciar la ruta");
    }

    @Test
    void asignaVehiculoYConductorALaRuta() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(LocalDate.now().plusMonths(6))));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.asignar(1L, new AsignacionRequest(5L, 7L));

        assertThat(respuesta.placaVehiculo()).isEqualTo("ABC123");
        assertThat(respuesta.nombreConductor()).isEqualTo("Carlos Rojas");
    }

    @Test
    void rechazaVehiculoSinCadenaDeFrioCuandoLaOperacionLaRequiere() {
        Ruta ruta = ruta(EstadoRuta.PENDIENTE);
        ruta.getOperacion().setRequiereCadenaFrio(true);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(LocalDate.now().plusMonths(6))));

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La operacion requiere cadena de frio y el vehiculo ABC123 no la tiene");
    }

    @Test
    void rechazaConductorConLicenciaVencida() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(LocalDate.now().minusDays(1))));

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La licencia del conductor Carlos Rojas esta vencida");
    }

    @Test
    void rechazaVehiculoQueYaEstaEnOtraRutaEnCurso() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(LocalDate.now().plusMonths(6))));
        when(rutaRepository.existsByVehiculoIdAndEstadoAndIdNot(any(), eq(EstadoRuta.EN_CURSO), any())).thenReturn(true);

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El vehiculo ABC123 ya esta en otra ruta en curso");
    }

    @Test
    void noProgramaMenosEntregasQueLasYaRegistradas() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(entregaRepository.countByRutaId(1L)).thenReturn(15L);

        assertThatThrownBy(() -> rutaService.actualizar(1L, solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La ruta ya tiene 15 entregas registradas; no puede programar menos");
    }

    private Vehiculo vehiculo(boolean activo, boolean cadenaFrio) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca("ABC123");
        vehiculo.setTipoVehiculo("Furgon");
        vehiculo.setActivo(activo);
        vehiculo.setRequiereCadenaFrio(cadenaFrio);
        return vehiculo;
    }

    private Conductor conductor(LocalDate vencimientoLicencia) {
        Conductor conductor = new Conductor();
        conductor.setNombres("Carlos");
        conductor.setApellidos("Rojas");
        conductor.setActivo(true);
        conductor.setFechaVencimientoLicencia(vencimientoLicencia);
        return conductor;
    }

    private Ruta ruta(EstadoRuta estado) {
        Ruta ruta = new Ruta();
        ruta.setCodigo("R-001");
        ruta.setNumeroEntregasProgramadas(12);
        ruta.setZona(new Zona());
        ruta.setOperacion(new Operacion());
        ruta.setEstado(estado);
        return ruta;
    }

    private RutaRequest solicitud() {
        return new RutaRequest("R-001", "Ruta Norte manana", 1L, 1L, "Bodega Calle 13", "Centro Comercial Santafe", 18.5, 12);
    }
}
