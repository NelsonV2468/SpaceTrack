package co.edu.usta.spacetrack.rutas.dto;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ubicacion del vehiculo de una ruta. Si el vehiculo reporto su posicion (GPS o registro manual) se usa esa;
 * si no, la simulada. {@code fueraDeRuta} indica que la posicion reportada se aleja del recorrido planeado
 * mas de lo permitido, y {@code distanciaDesvioKm} cuanto se aleja.
 */
public record UbicacionRutaResponse(Long rutaId, String codigo, String nombre, EstadoRuta estado, String nombreZona,
                                    String placaVehiculo, String nombreConductor, PuntoMapa origen, PuntoMapa destino,
                                    List<PuntoMapa> paradas, PuntoMapa vehiculo, int entregasRegistradas,
                                    int entregasProgramadas, double progreso, int velocidadKmh,
                                    String descripcionPosicion, LocalDateTime calculadoEn,
                                    boolean posicionReportada, boolean fueraDeRuta, Double distanciaDesvioKm) {

    public UbicacionRutaResponse(Long rutaId, String codigo, String nombre, EstadoRuta estado, String nombreZona,
                                 String placaVehiculo, String nombreConductor, PuntoMapa origen, PuntoMapa destino,
                                 List<PuntoMapa> paradas, PuntoMapa vehiculo, int entregasRegistradas,
                                 int entregasProgramadas, double progreso, int velocidadKmh,
                                 String descripcionPosicion, LocalDateTime calculadoEn) {
        this(rutaId, codigo, nombre, estado, nombreZona, placaVehiculo, nombreConductor, origen, destino, paradas, vehiculo,
                entregasRegistradas, entregasProgramadas, progreso, velocidadKmh, descripcionPosicion, calculadoEn,
                false, false, null);
    }

    /** Reemplaza la posicion simulada por la que reporto el vehiculo. */
    public UbicacionRutaResponse conPosicionReportada(double latitud, double longitud, Integer velocidad,
                                                      boolean fuera, double distanciaKm, String descripcion) {
        PuntoMapa punto = new PuntoMapa(latitud, longitud, vehiculo == null ? codigo : vehiculo.etiqueta(), null);
        return new UbicacionRutaResponse(rutaId, codigo, nombre, estado, nombreZona, placaVehiculo, nombreConductor,
                origen, destino, paradas, punto, entregasRegistradas, entregasProgramadas, progreso,
                velocidad == null ? 0 : velocidad, descripcion, calculadoEn, true, fuera,
                Math.round(distanciaKm * 100) / 100.0);
    }
}
