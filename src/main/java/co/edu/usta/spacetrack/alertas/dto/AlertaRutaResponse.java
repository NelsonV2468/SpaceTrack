package co.edu.usta.spacetrack.alertas.dto;

import co.edu.usta.spacetrack.alertas.domain.NivelAlerta;
import co.edu.usta.spacetrack.alertas.domain.TipoAlerta;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Alerta de una ruta EN_CURSO: por franja horaria (entregas pendientes y poco tiempo) o por desvio del recorrido.
 * {@code minutosRestantes} es negativo cuando la franja ya termino; en las alertas de desvio es 0.
 * {@code distanciaDesvioKm} solo aplica a las alertas de desvio.
 */
public record AlertaRutaResponse(Long rutaId, String codigoRuta, String nombreRuta, String nombreZona, String localidad,
                                 String codigoContrato, String placaVehiculo, String nombreConductor,
                                 NivelAlerta nivel, int entregasProgramadas, int entregasPendientes,
                                 LocalTime horaFinFranja, long minutosRestantes, long minutosEstimadosPendientes,
                                 String mensaje, LocalDateTime calculadoEn, TipoAlerta tipo, Double distanciaDesvioKm) { }
