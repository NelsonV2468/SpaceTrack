package co.edu.usta.spacetrack.alertas.service;

import co.edu.usta.spacetrack.alertas.domain.NivelAlerta;
import co.edu.usta.spacetrack.alertas.dto.AlertaRutaResponse;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ON-4. Detecta rutas EN_CURSO que estan por terminar su franja horaria sin haber completado sus entregas.
 * <p>
 * El fin de la ruta es el cierre de la franja horaria de hoy definida por las necesidades del contrato
 * (la mas tardia entre las que ya empezaron). Sin necesidades para hoy no hay franja y no se genera alerta.
 */
@Service
@Transactional(readOnly = true)
public class AlertaRutaService {

    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final NecesidadClienteRepository necesidadRepository;
    private final Clock reloj;
    private final long minutosAviso;
    private final long minutosPorEntrega;

    public AlertaRutaService(RutaRepository rutaRepository, EntregaRepository entregaRepository,
                             NecesidadClienteRepository necesidadRepository, Clock reloj,
                             @Value("${spacetrack.alertas.minutos-aviso:60}") long minutosAviso,
                             @Value("${spacetrack.alertas.minutos-por-entrega:10}") long minutosPorEntrega) {
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.necesidadRepository = necesidadRepository;
        this.reloj = reloj;
        this.minutosAviso = minutosAviso;
        this.minutosPorEntrega = minutosPorEntrega;
    }

    /** Alertas vigentes, primero las criticas y luego las que tienen menos tiempo. */
    public List<AlertaRutaResponse> alertasActivas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO).stream()
                .map(ruta -> evaluar(ruta, ahora))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(AlertaRutaResponse::nivel).reversed()
                        .thenComparingLong(AlertaRutaResponse::minutosRestantes))
                .toList();
    }

    /** Alerta de una ruta concreta, si la tiene. */
    public Optional<AlertaRutaResponse> alertaDeRuta(Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una ruta con id " + rutaId));
        return ruta.getEstado() == EstadoRuta.EN_CURSO ? evaluar(ruta, LocalDateTime.now(reloj)) : Optional.empty();
    }

    private Optional<AlertaRutaResponse> evaluar(Ruta ruta, LocalDateTime ahora) {
        int programadas = ruta.getNumeroEntregasProgramadas();
        int pendientes = (int) Math.max(0, programadas - entregaRepository.countByRutaId(ruta.getId()));
        if (pendientes == 0) {
            return Optional.empty();
        }
        long estimado = pendientes * minutosPorEntrega;

        // Una ruta que sigue abierta desde un dia anterior ya supero la franja de ese dia.
        LocalDateTime inicio = ruta.getFechaInicioReal();
        if (inicio != null && inicio.toLocalDate().isBefore(ahora.toLocalDate())) {
            return Optional.of(alerta(ruta, NivelAlerta.CRITICA, programadas, pendientes, null,
                    -Duration.between(inicio, ahora).toMinutes(), estimado, ahora,
                    "La ruta " + ruta.getCodigo() + " sigue en curso desde el " + inicio.toLocalDate()
                            + " con " + pendientes + " entregas pendientes en la zona " + zona(ruta)));
        }

        Optional<LocalTime> finFranja = finDeFranja(ruta, ahora);
        if (finFranja.isEmpty()) {
            return Optional.empty();
        }
        LocalTime horaFin = finFranja.get();
        long restantes = Duration.between(ahora.toLocalTime(), horaFin).toMinutes();
        String base = "La ruta " + ruta.getCodigo() + " (zona " + zona(ruta) + ") tiene " + pendientes + " de "
                + programadas + " entregas pendientes";

        if (restantes <= 0) {
            return Optional.of(alerta(ruta, NivelAlerta.CRITICA, programadas, pendientes, horaFin, restantes, estimado, ahora,
                    base + " y su franja horaria termino a las " + horaFin));
        }
        if (estimado > restantes) {
            return Optional.of(alerta(ruta, NivelAlerta.CRITICA, programadas, pendientes, horaFin, restantes, estimado, ahora,
                    base + "; faltan " + restantes + " min para las " + horaFin + " y se estiman " + estimado
                            + " min para completarlas"));
        }
        if (restantes <= minutosAviso) {
            return Optional.of(alerta(ruta, NivelAlerta.ADVERTENCIA, programadas, pendientes, horaFin, restantes, estimado, ahora,
                    base + "; su franja horaria termina en " + restantes + " min (" + horaFin + ")"));
        }
        return Optional.empty();
    }

    // La franja de hoy es la mas tardia entre las necesidades del contrato que operan hoy y ya empezaron.
    private Optional<LocalTime> finDeFranja(Ruta ruta, LocalDateTime ahora) {
        if (ruta.getOperacion() == null) {
            return Optional.empty();
        }
        DiaSemana hoy = DiaSemana.desde(ahora.getDayOfWeek());
        List<NecesidadCliente> deHoy = necesidadRepository.findByOperacionIdOrderByIdAsc(ruta.getOperacion().getId())
                .stream().filter(n -> n.getDiasOperacion().contains(hoy)).toList();
        LocalTime hora = ahora.toLocalTime();
        return deHoy.stream().filter(n -> !hora.isBefore(n.getHoraInicio()))
                .map(NecesidadCliente::getHoraFin).max(Comparator.naturalOrder());
    }

    private String zona(Ruta ruta) {
        return ruta.getZona() == null ? "sin zona" : ruta.getZona().getNombre() + " - " + ruta.getZona().getLocalidad();
    }

    private AlertaRutaResponse alerta(Ruta ruta, NivelAlerta nivel, int programadas, int pendientes, LocalTime horaFin,
                                      long restantes, long estimado, LocalDateTime ahora, String mensaje) {
        return new AlertaRutaResponse(ruta.getId(), ruta.getCodigo(), ruta.getNombre(),
                ruta.getZona() == null ? null : ruta.getZona().getNombre(),
                ruta.getZona() == null ? null : ruta.getZona().getLocalidad(),
                ruta.getOperacion() == null ? null : ruta.getOperacion().getCodigoContrato(),
                ruta.getVehiculo() == null ? null : ruta.getVehiculo().getPlaca(),
                ruta.getConductor() == null ? null : ruta.getConductor().getNombres() + " " + ruta.getConductor().getApellidos(),
                nivel, programadas, pendientes, horaFin, restantes, estimado, mensaje, ahora);
    }
}
