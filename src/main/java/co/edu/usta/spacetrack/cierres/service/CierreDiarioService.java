package co.edu.usta.spacetrack.cierres.service;

import co.edu.usta.spacetrack.cierres.domain.CierreDiario;
import co.edu.usta.spacetrack.cierres.domain.CierreDiarioRuta;
import co.edu.usta.spacetrack.cierres.dto.CierreDiarioRequest;
import co.edu.usta.spacetrack.cierres.dto.CierreDiarioResponse;
import co.edu.usta.spacetrack.cierres.dto.RutaCierreResponse;
import co.edu.usta.spacetrack.cierres.repository.CierreDiarioRepository;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ON-20 (epica ON-3 Seguimiento de rutas). Cierre diario de rutas.
 * <ul>
 *     <li>El resumen de un dia incluye las rutas FINALIZADAS ese dia (segun su hora real de fin).</li>
 *     <li>Un dia se cierra una sola vez, nunca en el futuro y solo si no quedan rutas EN_CURSO iniciadas
 *     ese dia o antes.</li>
 *     <li>El cierre guarda una copia fija del resumen; despues no se inician ni finalizan rutas en ese dia
 *     (lo valida RutaService).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CierreDiarioService {

    private final CierreDiarioRepository cierreRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final Clock reloj;

    public CierreDiarioService(CierreDiarioRepository cierreRepository, RutaRepository rutaRepository,
                               EntregaRepository entregaRepository, Clock reloj) {
        this.cierreRepository = cierreRepository;
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.reloj = reloj;
    }

    /** Historial de cierres, del mas reciente al mas antiguo. */
    public List<CierreDiarioResponse> listar() {
        return cierreRepository.findAllByOrderByFechaDesc().stream().map(this::desdeCierre).toList();
    }

    /** Cierre guardado de la fecha, o la vista previa calculada en vivo si el dia sigue abierto. */
    public CierreDiarioResponse resumen(LocalDate fecha) {
        validarNoFutura(fecha);
        return cierreRepository.findByFecha(fecha).map(this::desdeCierre).orElseGet(() -> vistaPrevia(fecha));
    }

    @Transactional
    public CierreDiarioResponse cerrar(CierreDiarioRequest solicitud, String registradoPor) {
        LocalDate fecha = solicitud.fecha();
        validarNoFutura(fecha);
        if (cierreRepository.existsByFecha(fecha)) {
            throw new ReglaDeNegocioException("El dia " + fecha + " ya fue cerrado");
        }
        List<String> enCurso = rutasEnCurso(fecha);
        if (!enCurso.isEmpty()) {
            throw new ReglaDeNegocioException("No se puede cerrar el dia " + fecha + ": finalice primero las rutas en curso "
                    + String.join(", ", enCurso));
        }
        CierreDiario cierre = new CierreDiario();
        cierre.setFecha(fecha);
        cierre.setFechaRegistro(LocalDateTime.now(reloj));
        cierre.setRegistradoPor(registradoPor);
        cierre.setObservaciones(solicitud.observaciones() == null || solicitud.observaciones().isBlank()
                ? null : solicitud.observaciones().trim());
        rutasFinalizadas(fecha).forEach(ruta -> cierre.agregarRuta(copiar(ruta)));
        return desdeCierre(cierreRepository.save(cierre));
    }

    private CierreDiarioResponse vistaPrevia(LocalDate fecha) {
        List<RutaCierreResponse> rutas = rutasFinalizadas(fecha).stream().map(ruta -> aResponse(copiar(ruta))).toList();
        List<String> enCurso = rutasEnCurso(fecha);
        int programadas = rutas.stream().mapToInt(RutaCierreResponse::entregasProgramadas).sum();
        int realizadas = rutas.stream().mapToInt(RutaCierreResponse::entregasRealizadas).sum();
        int noRealizadas = rutas.stream().mapToInt(RutaCierreResponse::entregasNoRealizadas).sum();
        int sinRegistrar = rutas.stream().mapToInt(RutaCierreResponse::entregasSinRegistrar).sum();
        String motivo = enCurso.isEmpty() ? null
                : "Finalice primero las rutas en curso: " + String.join(", ", enCurso);
        return new CierreDiarioResponse(null, fecha, false, null, null, null, rutas.size(), programadas, realizadas,
                noRealizadas, sinRegistrar, cumplimiento(realizadas, programadas), rutas, enCurso, enCurso.isEmpty(), motivo);
    }

    private CierreDiarioResponse desdeCierre(CierreDiario cierre) {
        return new CierreDiarioResponse(cierre.getId(), cierre.getFecha(), true, cierre.getFechaRegistro(),
                cierre.getRegistradoPor(), cierre.getObservaciones(), cierre.getRutasCerradas(),
                cierre.getEntregasProgramadas(), cierre.getEntregasRealizadas(), cierre.getEntregasNoRealizadas(),
                cierre.getEntregasSinRegistrar(), cumplimiento(cierre.getEntregasRealizadas(), cierre.getEntregasProgramadas()),
                cierre.getRutas().stream().map(this::aResponse).toList(), List.of(), false, "El dia ya fue cerrado");
    }

    private List<Ruta> rutasFinalizadas(LocalDate fecha) {
        return rutaRepository.findByEstadoAndFechaFinRealGreaterThanEqualAndFechaFinRealLessThanOrderByCodigoAsc(
                EstadoRuta.FINALIZADA, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    private List<String> rutasEnCurso(LocalDate fecha) {
        return rutaRepository.findByEstadoAndFechaInicioRealLessThanOrderByCodigoAsc(EstadoRuta.EN_CURSO,
                fecha.plusDays(1).atStartOfDay()).stream().map(Ruta::getCodigo).toList();
    }

    private CierreDiarioRuta copiar(Ruta ruta) {
        return new CierreDiarioRuta(ruta.getId(), ruta.getCodigo(), ruta.getNombre(),
                ruta.getZona() == null ? null : ruta.getZona().getNombre(),
                ruta.getVehiculo() == null ? null : ruta.getVehiculo().getPlaca(),
                ruta.getConductor() == null ? null : ruta.getConductor().getNombres() + " " + ruta.getConductor().getApellidos(),
                ruta.getFechaInicioReal(), ruta.getFechaFinReal(), ruta.getNumeroEntregasProgramadas(),
                (int) entregaRepository.countByRutaIdAndEstado(ruta.getId(), EstadoEntrega.ENTREGADA),
                (int) entregaRepository.countByRutaIdAndEstado(ruta.getId(), EstadoEntrega.NO_ENTREGADA));
    }

    private RutaCierreResponse aResponse(CierreDiarioRuta r) {
        return new RutaCierreResponse(r.getRutaId(), r.getCodigoRuta(), r.getNombreRuta(), r.getNombreZona(),
                r.getPlacaVehiculo(), r.getNombreConductor(), r.getFechaInicioReal(), r.getFechaFinReal(),
                r.getEntregasProgramadas(), r.getEntregasRealizadas(), r.getEntregasNoRealizadas(), r.getEntregasSinRegistrar());
    }

    private void validarNoFutura(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now(reloj))) {
            throw new ReglaDeNegocioException("No se puede consultar ni cerrar un dia futuro (" + fecha + ")");
        }
    }

    // Porcentaje de entregas realizadas sobre las programadas, con un decimal.
    private static double cumplimiento(int realizadas, int programadas) {
        return programadas == 0 ? 0 : Math.round(realizadas * 1000.0 / programadas) / 10.0;
    }
}
