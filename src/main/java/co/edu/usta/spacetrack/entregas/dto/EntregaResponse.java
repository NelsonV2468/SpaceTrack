package co.edu.usta.spacetrack.entregas.dto;

import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;

import java.time.LocalDateTime;

public record EntregaResponse(Long id, Long rutaId, String codigoRuta, String numeroGuia, String destinatario,
                              String direccion, EstadoEntrega estado, String novedad, LocalDateTime fechaRegistro) { }
