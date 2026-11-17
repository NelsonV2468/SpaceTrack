package co.edu.usta.spacetrack.cierres.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de la operacion de un dia. Si {@code cerrado} es false es una vista previa calculada en vivo;
 * si es true son los datos fijos guardados al cerrar el dia.
 * {@code rutasEnCurso} lista los codigos de las rutas que impiden cerrar el dia.
 */
public record CierreDiarioResponse(Long id, LocalDate fecha, boolean cerrado, LocalDateTime fechaRegistro,
                                   String registradoPor, String observaciones,
                                   int rutasCerradas, int entregasProgramadas, int entregasRealizadas,
                                   int entregasNoRealizadas, int entregasSinRegistrar, double cumplimiento,
                                   List<RutaCierreResponse> rutas, List<String> rutasEnCurso,
                                   boolean puedeCerrar, String motivoNoCierre) { }
