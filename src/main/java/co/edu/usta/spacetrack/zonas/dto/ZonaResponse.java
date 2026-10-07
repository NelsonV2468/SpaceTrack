package co.edu.usta.spacetrack.zonas.dto;

import java.time.LocalDateTime;

public record ZonaResponse(Long id, String codigo, String nombre, String localidad, String descripcion,
                           boolean activa, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
