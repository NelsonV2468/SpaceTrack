package co.edu.usta.spacetrack.cierres.dto;

import java.time.LocalDateTime;

/** Una ruta dentro del resumen o del cierre de un dia. */
public record RutaCierreResponse(Long rutaId, String codigoRuta, String nombreRuta, String nombreZona,
                                 String placaVehiculo, String nombreConductor,
                                 LocalDateTime fechaInicioReal, LocalDateTime fechaFinReal,
                                 int entregasProgramadas, int entregasRealizadas, int entregasNoRealizadas,
                                 int entregasSinRegistrar) { }
