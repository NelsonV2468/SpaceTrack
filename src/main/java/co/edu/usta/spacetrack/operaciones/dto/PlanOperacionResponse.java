package co.edu.usta.spacetrack.operaciones.dto;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Operacion con su necesidad principal, con los mismos nombres de campo que {@link PlanOperacionRequest}.
 * Los datos de la necesidad son null si el contrato aun no la tiene. {@code necesidadesAdicionales} cuenta las
 * necesidades registradas antes de unificar el formulario (se conservan y se suman al contrato).
 */
public record PlanOperacionResponse(Long id, String codigoContrato, Long clienteId, String razonSocialCliente,
                                    String nombreOperacion, LocalDate fechaInicio, LocalDate fechaFin,
                                    EstadoOperacion estado, Integer numeroEntregasEsperadas, boolean requiereCadenaFrio,
                                    String diasOperacionTexto, List<DiaSemana> diasOperacion,
                                    Integer numeroVehiculosRequeridos, Long necesidadId, String tipoVehiculo,
                                    Integer cantidadVehiculos, Double capacidadMinimaKg, LocalTime horaInicio,
                                    LocalTime horaFin, String observaciones, Long vehiculosDisponibles,
                                    Boolean flotaSuficiente, int necesidadesAdicionales) { }
