package co.edu.usta.spacetrack.alertas.domain;

/**
 * Motivo de una alerta de ruta.
 * <ul>
 *     <li>FRANJA_HORARIA (ON-4): la franja del contrato termina y quedan entregas pendientes.</li>
 *     <li>DESVIO_RUTA: la posicion reportada por el vehiculo se aleja del recorrido planeado.</li>
 * </ul>
 */
public enum TipoAlerta {
    FRANJA_HORARIA,
    DESVIO_RUTA
}
