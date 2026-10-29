package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.conductores.domain.Conductor;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.AsignacionRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.dto.UbicacionRutaResponse;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RutaService {

    private final RutaRepository rutaRepository;
    private final ZonaRepository zonaRepository;
    private final OperacionRepository operacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final EntregaRepository entregaRepository;
    private final SimuladorUbicacion simuladorUbicacion;

    public RutaService(RutaRepository rutaRepository, ZonaRepository zonaRepository, OperacionRepository operacionRepository,
                       VehiculoRepository vehiculoRepository, ConductorRepository conductorRepository,
                       EntregaRepository entregaRepository, SimuladorUbicacion simuladorUbicacion) {
        this.rutaRepository = rutaRepository;
        this.zonaRepository = zonaRepository;
        this.operacionRepository = operacionRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.entregaRepository = entregaRepository;
        this.simuladorUbicacion = simuladorUbicacion;
    }

    public List<RutaResponse> listar(EstadoRuta estado) {
        List<Ruta> rutas = estado == null ? rutaRepository.findAllByOrderByCodigoAsc()
                : rutaRepository.findByEstadoOrderByCodigoAsc(estado);
        return rutas.stream().map(this::aResponse).toList();
    }

    public RutaResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public RutaResponse crear(RutaRequest solicitud) {
        validarCodigoUnico(solicitud.codigo(), null);
        Ruta ruta = new Ruta();
        copiarDatos(solicitud, ruta);
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse actualizar(Long id, RutaRequest solicitud) {
        Ruta ruta = buscarEntidad(id);
        if (ruta.getEstado() == EstadoRuta.FINALIZADA) {
            throw new ReglaDeNegocioException("No se puede editar una ruta finalizada");
        }
        validarCodigoUnico(solicitud.codigo(), id);
        long registradas = entregaRepository.countByRutaId(id);
        if (solicitud.numeroEntregasProgramadas() < registradas) {
            throw new ReglaDeNegocioException("La ruta ya tiene " + registradas
                    + " entregas registradas; no puede programar menos");
        }
        copiarDatos(solicitud, ruta);
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public void eliminar(Long id) {
        Ruta ruta = buscarEntidad(id);
        // Las rutas en curso o finalizadas se conservan como historial de la operacion.
        if (ruta.getEstado() != EstadoRuta.PENDIENTE) {
            throw new ReglaDeNegocioException("Solo se pueden eliminar rutas en estado PENDIENTE");
        }
        rutaRepository.delete(ruta);
    }

    @Transactional
    public RutaResponse asignar(Long id, AsignacionRequest solicitud) {
        Ruta ruta = buscarEntidad(id);
        if (ruta.getEstado() == EstadoRuta.FINALIZADA) {
            throw new ReglaDeNegocioException("No se pueden cambiar los recursos de una ruta finalizada");
        }
        Vehiculo vehiculo = vehiculoRepository.findById(solicitud.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehiculo con id " + solicitud.vehiculoId()));
        Conductor conductor = conductorRepository.findById(solicitud.conductorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un conductor con id " + solicitud.conductorId()));
        validarVehiculo(ruta, vehiculo);
        validarConductor(ruta, conductor);
        ruta.setVehiculo(vehiculo);
        ruta.setConductor(conductor);
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse iniciar(Long id) {
        Ruta ruta = buscarEntidad(id);
        validarEstadoActual(ruta, EstadoRuta.PENDIENTE, "iniciar");
        // Una ruta no sale sin vehiculo y conductor; se revalidan por si cambiaron desde la asignacion.
        if (ruta.getVehiculo() == null || ruta.getConductor() == null) {
            throw new ReglaDeNegocioException("Asigne un vehiculo y un conductor antes de iniciar la ruta");
        }
        validarVehiculo(ruta, ruta.getVehiculo());
        validarConductor(ruta, ruta.getConductor());
        ruta.setEstado(EstadoRuta.EN_CURSO);
        ruta.setFechaInicioReal(LocalDateTime.now());
        return aResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse finalizar(Long id) {
        Ruta ruta = buscarEntidad(id);
        validarEstadoActual(ruta, EstadoRuta.EN_CURSO, "finalizar");
        ruta.setEstado(EstadoRuta.FINALIZADA);
        ruta.setFechaFinReal(LocalDateTime.now());
        return aResponse(rutaRepository.save(ruta));
    }

    public UbicacionRutaResponse ubicacion(Long id) {
        return calcularUbicacion(buscarEntidad(id));
    }

    public List<UbicacionRutaResponse> ubicaciones() {
        return rutaRepository.findAllByOrderByCodigoAsc().stream().map(this::calcularUbicacion).toList();
    }

    private UbicacionRutaResponse calcularUbicacion(Ruta ruta) {
        return simuladorUbicacion.calcular(ruta, entregaRepository.findByRutaIdOrderByIdAsc(ruta.getId()), LocalDateTime.now());
    }

    private void validarVehiculo(Ruta ruta, Vehiculo vehiculo) {
        if (!vehiculo.isActivo()) {
            throw new ReglaDeNegocioException("El vehiculo " + vehiculo.getPlaca() + " esta inactivo");
        }
        if (ruta.getOperacion().isRequiereCadenaFrio() && !vehiculo.isRequiereCadenaFrio()) {
            throw new ReglaDeNegocioException("La operacion requiere cadena de frio y el vehiculo "
                    + vehiculo.getPlaca() + " no la tiene");
        }
        if (rutaRepository.existsByVehiculoIdAndEstadoAndIdNot(vehiculo.getId(), EstadoRuta.EN_CURSO, idOCero(ruta))) {
            throw new ReglaDeNegocioException("El vehiculo " + vehiculo.getPlaca() + " ya esta en otra ruta en curso");
        }
    }

    private void validarConductor(Ruta ruta, Conductor conductor) {
        String nombre = conductor.getNombres() + " " + conductor.getApellidos();
        if (!conductor.isActivo()) {
            throw new ReglaDeNegocioException("El conductor " + nombre + " esta inactivo");
        }
        if (conductor.getFechaVencimientoLicencia().isBefore(LocalDate.now())) {
            throw new ReglaDeNegocioException("La licencia del conductor " + nombre + " esta vencida");
        }
        if (rutaRepository.existsByConductorIdAndEstadoAndIdNot(conductor.getId(), EstadoRuta.EN_CURSO, idOCero(ruta))) {
            throw new ReglaDeNegocioException("El conductor " + nombre + " ya esta en otra ruta en curso");
        }
    }

    // Las consultas "AndIdNot" necesitan un id; una ruta sin guardar no coincide con ninguno.
    private Long idOCero(Ruta ruta) {
        return ruta.getId() == null ? 0L : ruta.getId();
    }

    private void validarEstadoActual(Ruta ruta, EstadoRuta esperado, String accion) {
        if (ruta.getEstado() != esperado) {
            throw new ReglaDeNegocioException("Solo se puede " + accion + " una ruta en estado " + esperado
                    + "; la ruta esta " + ruta.getEstado());
        }
    }

    private Ruta buscarEntidad(Long id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una ruta con id " + id));
    }

    private Zona buscarZonaActiva(Long zonaId) {
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una zona con id " + zonaId));
        if (!zona.isActiva()) {
            throw new ReglaDeNegocioException("No se pueden asignar rutas a una zona inactiva");
        }
        return zona;
    }

    private Operacion buscarOperacionVigente(Long operacionId) {
        Operacion operacion = operacionRepository.findById(operacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una operacion con id " + operacionId));
        if (operacion.getEstado() == EstadoOperacion.FINALIZADA) {
            throw new ReglaDeNegocioException("No se pueden crear rutas para una operacion finalizada");
        }
        return operacion;
    }

    private void validarCodigoUnico(String codigo, Long id) {
        String codigoLimpio = codigo.trim();
        boolean codigoExiste = id == null ? rutaRepository.existsByCodigoIgnoreCase(codigoLimpio)
                : rutaRepository.existsByCodigoIgnoreCaseAndIdNot(codigoLimpio, id);
        if (codigoExiste) {
            throw new ReglaDeNegocioException("Ya existe una ruta registrada con ese codigo");
        }
    }

    private void copiarDatos(RutaRequest solicitud, Ruta ruta) {
        ruta.setCodigo(solicitud.codigo().trim().toUpperCase());
        ruta.setNombre(solicitud.nombre().trim());
        ruta.setZona(buscarZonaActiva(solicitud.zonaId()));
        ruta.setOperacion(buscarOperacionVigente(solicitud.operacionId()));
        ruta.setPuntoOrigen(solicitud.puntoOrigen().trim());
        ruta.setPuntoDestino(solicitud.puntoDestino().trim());
        ruta.setDistanciaEstimadaKm(solicitud.distanciaEstimadaKm());
        ruta.setNumeroEntregasProgramadas(solicitud.numeroEntregasProgramadas());
    }

    private RutaResponse aResponse(Ruta ruta) {
        Zona zona = ruta.getZona();
        Operacion operacion = ruta.getOperacion();
        Vehiculo vehiculo = ruta.getVehiculo();
        Conductor conductor = ruta.getConductor();
        long realizadas = entregaRepository.countByRutaIdAndEstado(ruta.getId(), EstadoEntrega.ENTREGADA);
        long noRealizadas = entregaRepository.countByRutaIdAndEstado(ruta.getId(), EstadoEntrega.NO_ENTREGADA);
        return new RutaResponse(ruta.getId(), ruta.getCodigo(), ruta.getNombre(), zona.getId(), zona.getNombre(),
                operacion.getId(), operacion.getCodigoContrato(),
                operacion.getCliente() == null ? null : operacion.getCliente().getRazonSocial(),
                ruta.getPuntoOrigen(), ruta.getPuntoDestino(), ruta.getDistanciaEstimadaKm(),
                ruta.getNumeroEntregasProgramadas(), realizadas, noRealizadas,
                operacion.getNumeroEntregasEsperadas(), rutaRepository.sumarEntregasProgramadasPorOperacion(operacion.getId()),
                vehiculo == null ? null : vehiculo.getId(), vehiculo == null ? null : vehiculo.getPlaca(),
                vehiculo == null ? null : vehiculo.getTipoVehiculo() + " " + vehiculo.getMarca() + " " + vehiculo.getModelo(),
                conductor == null ? null : conductor.getId(),
                conductor == null ? null : conductor.getNombres() + " " + conductor.getApellidos(),
                ruta.getEstado(), ruta.getFechaInicioReal(), ruta.getFechaFinReal(),
                ruta.getFechaCreacion(), ruta.getFechaActualizacion());
    }
}
