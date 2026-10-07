package co.edu.usta.spacetrack.necesidades.dto;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record NecesidadClienteResponse(Long id, Long operacionId, String codigoContrato, String razonSocialCliente,
                                       String tipoVehiculo, Integer cantidadVehiculos, Double capacidadMinimaKg,
                                       boolean requiereCadenaFrio, List<DiaSemana> diasOperacion,
                                       LocalTime horaInicio, LocalTime horaFin, String observaciones,
                                       long vehiculosDisponibles, boolean flotaSuficiente,
                                       LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
