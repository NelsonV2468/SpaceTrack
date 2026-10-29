package co.edu.usta.spacetrack.rutas.dto;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;

import java.time.LocalDateTime;
import java.util.List;

public record UbicacionRutaResponse(Long rutaId, String codigo, String nombre, EstadoRuta estado, String nombreZona,
                                    String placaVehiculo, String nombreConductor, PuntoMapa origen, PuntoMapa destino,
                                    List<PuntoMapa> paradas, PuntoMapa vehiculo, int entregasRegistradas,
                                    int entregasProgramadas, double progreso, int velocidadKmh,
                                    String descripcionPosicion, LocalDateTime calculadoEn) { }
