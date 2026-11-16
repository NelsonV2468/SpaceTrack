package co.edu.usta.spacetrack.alertas.domain;

/**
 * Gravedad de una alerta de ruta.
 * <ul>
 *     <li>ADVERTENCIA: la franja horaria esta por terminar y quedan entregas pendientes.</li>
 *     <li>CRITICA: el tiempo restante no alcanza para las entregas pendientes, o la franja ya termino.</li>
 * </ul>
 */
public enum NivelAlerta {
    ADVERTENCIA,
    CRITICA
}
