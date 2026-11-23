package co.edu.usta.spacetrack.alertas.dto;

import co.edu.usta.spacetrack.alertas.domain.NivelAlerta;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Ruta EN_CURSO proxima a terminar su franja horaria sin haber completado sus entregas.
 * {@code minutosRestantes} es negativo cuando la franja ya termino.
 */
public record AlertaRutaResponse(Long rutaId, String codigoRuta, String nombreRuta, String nombreZona, String localidad,
                                 String codigoContrato, String placaVehiculo, String nombreConductor,
                                 NivelAlerta nivel, int entregasProgramadas, int entregasPendientes,
                                 LocalTime horaFinFranja, long minutosRestantes, long minutosEstimadosPendientes,
                                 String mensaje, LocalDateTime calculadoEn) { }
