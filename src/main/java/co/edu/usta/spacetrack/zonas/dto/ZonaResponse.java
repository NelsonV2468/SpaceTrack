package co.edu.usta.spacetrack.zonas.dto;

import java.time.LocalDateTime;

/** {@code catalogo} es true para las localidades predefinidas de Bogota y false para zonas antiguas. */
public record ZonaResponse(Long id, String codigo, String nombre, String localidad, String descripcion,
                           boolean activa, boolean catalogo, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
