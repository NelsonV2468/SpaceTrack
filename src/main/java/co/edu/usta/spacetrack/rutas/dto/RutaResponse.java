package co.edu.usta.spacetrack.rutas.dto;

import java.time.LocalDateTime;

public record RutaResponse(Long id, String codigo, String nombre, Long zonaId, String nombreZona,
                           Long operacionId, String codigoContrato, String puntoOrigen, String puntoDestino,
                           Double distanciaEstimadaKm, Integer numeroEntregasProgramadas,
                           LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
