package co.edu.usta.spacetrack.vehiculos.dto;

import java.time.LocalDateTime;

public record VehiculoResponse(Long id, String placa, String tipoVehiculo, String marca, String modelo,
                               Integer anioModelo, Double capacidadCargaKg, boolean requiereCadenaFrio,
                               boolean activo, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
