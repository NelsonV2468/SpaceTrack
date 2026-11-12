package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.conductores.domain.Conductor;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.AsignacionRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RutaServiceTest {

    // Miercoles 11 de noviembre de 2026, 9:00 a. m. en Bogota.
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 11, 9, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    private final RutaRepository rutaRepository = mock(RutaRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final OperacionRepository operacionRepository = mock(OperacionRepository.class);
    private final VehiculoRepository vehiculoRepository = mock(VehiculoRepository.class);
    private final ConductorRepository conductorRepository = mock(ConductorRepository.class);
    private final EntregaRepository entregaRepository = mock(EntregaRepository.class);
    private final NecesidadClienteRepository necesidadRepository = mock(NecesidadClienteRepository.class);
    private final RutaService rutaService = new RutaService(rutaRepository, zonaRepository, operacionRepository,
            vehiculoRepository, conductorRepository, entregaRepository, necesidadRepository, new SimuladorUbicacion(),
            Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));

    @Test
    void rechazaCodigoDuplicado() {
        when(rutaRepository.existsByCodigoIgnoreCase("R-001")).thenReturn(true);

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una ruta registrada con ese codigo");
    }

    @Test
    void rechazaZonaInactiva() {
        Zona zona = new Zona();
        zona.setActiva(false);
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se pueden asignar rutas a una zona inactiva");
    }

    @Test
    void rechazaOperacionFinalizada() {
        prepararCreacion(operacion(EstadoOperacion.FINALIZADA));

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("No se pueden crear rutas para una operacion finalizada");
    }

    @Test
    void rechazaCrearRutasEnOperacionPausada() {
        prepararCreacion(operacion(EstadoOperacion.PAUSADA));

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La operacion CT-001 esta pausada; reactivela para crear rutas");
    }

    @Test
    void rechazaCrearRutasDeUnClienteInactivo() {
        Operacion operacion = operacion(EstadoOperacion.ACTIVA);
        operacion.getCliente().setActivo(false);
        prepararCreacion(operacion);

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El cliente Alimentos Urbanos SAS esta inactivo; no se pueden crear rutas");
    }

    @Test
    void rechazaCrearRutasFueraDeLaVigenciaDelContrato() {
        Operacion operacion = operacion(EstadoOperacion.ACTIVA);
        operacion.setFechaInicio(HOY.plusDays(1));
        operacion.setFechaFin(HOY.plusMonths(2));
        prepararCreacion(operacion);

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("esta vigente del 2026-11-12 al 2027-01-11; hoy (2026-11-11) no se pueden crear rutas");
    }

    @Test
    void rechazaProgramarMasEntregasDeLasEsperadasPorElContrato() {
        prepararCreacion(operacion(EstadoOperacion.ACTIVA));
        when(rutaRepository.sumarEntregasProgramadasPorOperacion(10L)).thenReturn(95L);

        assertThatThrownBy(() -> rutaService.crear(solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El contrato CT-001 espera 100 entregas y sus otras rutas ya programan 95; "
                        + "esta ruta puede programar como maximo 5");
    }

    @Test
    void creaRutaDentroDelCupoDelContrato() {
        prepararCreacion(operacion(EstadoOperacion.ACTIVA));
        when(rutaRepository.sumarEntregasProgramadasPorOperacion(10L)).thenReturn(88L);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.crear(solicitud(12));

        assertThat(respuesta.codigo()).isEqualTo("R-001");
        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.PENDIENTE);
    }

    @Test
    void alEditarNoCuentaLasEntregasDeLaPropiaRuta() {
        Ruta ruta = ruta(EstadoRuta.PENDIENTE);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(ruta.getZona()));
        when(rutaRepository.sumarEntregasProgramadasPorOperacionExcluyendo(10L, 1L)).thenReturn(90L);

        assertThatThrownBy(() -> rutaService.actualizar(1L, solicitud(11)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageEndingWith("esta ruta puede programar como maximo 10");
    }

    @Test
    void iniciaRutaPendienteYGuardaHoraDeInicio() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaLista()));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.iniciar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(respuesta.fechaInicioReal()).isEqualTo(AHORA);
        assertThat(respuesta.fechaFinReal()).isNull();
    }

    @Test
    void noIniciaRutaDeOperacionPausada() {
        Ruta ruta = rutaLista();
        ruta.getOperacion().setEstado(EstadoOperacion.PAUSADA);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La operacion CT-001 esta pausada; reactivela para iniciar rutas");
    }

    @Test
    void noIniciaRutaFueraDeLaVigenciaDelContrato() {
        Ruta ruta = rutaLista();
        ruta.getOperacion().setFechaFin(HOY.minusDays(1));
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no se pueden iniciar rutas");
    }

    @Test
    void noIniciaRutaUnDiaQueElContratoNoOpera() {
        Ruta ruta = rutaLista();
        ruta.getOperacion().setDiasOperacion("Lunes y martes");
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El contrato CT-001 opera Lunes y martes; hoy es MIERCOLES y no se puede iniciar la ruta");
    }

    @Test
    void noIniciaRutaFueraDelHorarioDeLasNecesidades() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaLista()));
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(10L))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(13, 0), LocalTime.of(18, 0))));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La ruta solo se puede iniciar en el horario del contrato para hoy (13:00-18:00); son las 09:00");
    }

    @Test
    void noIniciaRutaSiNingunaNecesidadOperaHoy() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaLista()));
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(10L))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.LUNES), LocalTime.of(6, 0), LocalTime.of(14, 0))));

        assertThatThrownBy(() -> rutaService.iniciar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ninguna necesidad del contrato CT-001 opera los dias MIERCOLES; no se puede iniciar la ruta");
    }

    @Test
    void iniciaRutaDentroDelHorarioDeLasNecesidades() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaLista()));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(necesidadRepository.findByOperacionIdOrderByIdAsc(10L))
                .thenReturn(List.of(necesidad(Set.of(DiaSemana.MIERCOLES), LocalTime.of(6, 0), LocalTime.of(14, 0))));

        assertThat(rutaService.iniciar(1L).estado()).isEqualTo(EstadoRuta.EN_CURSO);
    }

    @Test
    void finalizaRutaEnCursoYGuardaHoraDeFin() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(entregaRepository.countByRutaId(any())).thenReturn(12L);

        RutaResponse respuesta = rutaService.finalizar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.FINALIZADA);
        assertThat(respuesta.fechaFinReal()).isEqualTo(AHORA);
        assertThat(respuesta.advertencia()).isNull();
    }

    @Test
    void finalizarConEntregasFaltantesAlertaLaZona() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(entregaRepository.countByRutaId(any())).thenReturn(9L);
        when(entregaRepository.countByRutaIdAndEstado(any(), eq(EstadoEntrega.NO_ENTREGADA))).thenReturn(2L);

        RutaResponse respuesta = rutaService.finalizar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.FINALIZADA);
        assertThat(respuesta.advertencia()).isEqualTo("La ruta R-001 se finalizo con entregas faltantes en la zona "
                + "Zona Norte (Usaquén): 3 sin registrar y 2 no entregadas de 12 programadas");
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

        assertThatThrownBy(() -> rutaService.actualizar(1L, solicitud(12)))
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
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(HOY.plusMonths(6))));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.asignar(1L, new AsignacionRequest(5L, 7L));

        assertThat(respuesta.placaVehiculo()).isEqualTo("ABC123");
        assertThat(respuesta.nombreConductor()).isEqualTo("Carlos Rojas");
    }

    @Test
    void permiteReasignarRecursosEnUnaRutaEnCurso() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(HOY.plusMonths(6))));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RutaResponse respuesta = rutaService.asignar(1L, new AsignacionRequest(5L, 7L));

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(respuesta.placaVehiculo()).isEqualTo("ABC123");
    }

    @Test
    void rechazaVehiculoSinCadenaDeFrioCuandoLaOperacionLaRequiere() {
        Ruta ruta = ruta(EstadoRuta.PENDIENTE);
        ruta.getOperacion().setRequiereCadenaFrio(true);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(HOY.plusMonths(6))));

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La operacion requiere cadena de frio y el vehiculo ABC123 no la tiene");
    }

    @Test
    void rechazaConductorConLicenciaVencida() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(HOY.minusDays(1))));

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La licencia del conductor Carlos Rojas esta vencida");
    }

    @Test
    void rechazaVehiculoQueYaEstaEnOtraRutaEnCurso() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.PENDIENTE)));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo(true, false)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(HOY.plusMonths(6))));
        when(rutaRepository.existsByVehiculoIdAndEstadoAndIdNot(any(), eq(EstadoRuta.EN_CURSO), any())).thenReturn(true);

        assertThatThrownBy(() -> rutaService.asignar(1L, new AsignacionRequest(5L, 7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El vehiculo ABC123 ya esta en otra ruta en curso");
    }

    @Test
    void noProgramaMenosEntregasQueLasYaRegistradas() {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta(EstadoRuta.EN_CURSO)));
        when(entregaRepository.countByRutaId(1L)).thenReturn(15L);

        assertThatThrownBy(() -> rutaService.actualizar(1L, solicitud(12)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La ruta ya tiene 15 entregas registradas; no puede programar menos");
    }

    private void prepararCreacion(Operacion operacion) {
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona()));
        when(operacionRepository.findById(10L)).thenReturn(Optional.of(operacion));
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

    private NecesidadCliente necesidad(Set<DiaSemana> dias, LocalTime inicio, LocalTime fin) {
        NecesidadCliente necesidad = new NecesidadCliente();
        necesidad.setDiasOperacion(dias);
        necesidad.setHoraInicio(inicio);
        necesidad.setHoraFin(fin);
        return necesidad;
    }

    private Zona zona() {
        Zona zona = new Zona();
        zona.setNombre("Zona Norte");
        zona.setLocalidad("Usaquén");
        return zona;
    }

    // Operacion vigente, de lunes a viernes y con cupo para 100 entregas. Su id es 10.
    private Operacion operacion(EstadoOperacion estado) {
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Alimentos Urbanos SAS");
        cliente.setActivo(true);
        Operacion operacion = new Operacion() {
            @Override
            public Long getId() {
                return 10L;
            }
        };
        operacion.setCodigoContrato("CT-001");
        operacion.setCliente(cliente);
        operacion.setEstado(estado);
        operacion.setFechaInicio(HOY.minusMonths(1));
        operacion.setFechaFin(HOY.plusMonths(2));
        operacion.setDiasOperacion("Lunes a viernes");
        operacion.setNumeroEntregasEsperadas(100);
        return operacion;
    }

    private Ruta ruta(EstadoRuta estado) {
        Ruta ruta = new Ruta() {
            @Override
            public Long getId() {
                return 1L;
            }
        };
        ruta.setCodigo("R-001");
        ruta.setNumeroEntregasProgramadas(12);
        ruta.setZona(zona());
        ruta.setOperacion(operacion(EstadoOperacion.ACTIVA));
        ruta.setEstado(estado);
        return ruta;
    }

    private Ruta rutaLista() {
        Ruta ruta = ruta(EstadoRuta.PENDIENTE);
        ruta.setVehiculo(vehiculo(true, false));
        ruta.setConductor(conductor(HOY.plusYears(1)));
        return ruta;
    }

    private RutaRequest solicitud(int entregasProgramadas) {
        return new RutaRequest("R-001", "Ruta Norte manana", 1L, 10L, "Bodega Calle 13", "Centro Comercial Santafe",
                18.5, entregasProgramadas);
    }
}
