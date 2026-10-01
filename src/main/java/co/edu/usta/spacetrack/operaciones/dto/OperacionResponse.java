package co.edu.usta.spacetrack.operaciones.dto;

import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OperacionResponse(Long id, String codigoContrato, Long clienteId, String razonSocialCliente,
                                String nombreOperacion, LocalDate fechaInicio, LocalDate fechaFin, String diasOperacion,
                                Integer numeroVehiculosRequeridos, Integer numeroEntregasEsperadas,
                                boolean requiereCadenaFrio, EstadoOperacion estado, LocalDateTime fechaCreacion,
                                LocalDateTime fechaActualizacion) { }
