package co.edu.usta.spacetrack.rutas.dto;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;

import java.time.LocalDateTime;

public record RutaResponse(Long id, String codigo, String nombre, Long zonaId, String nombreZona,
                           Long operacionId, String codigoContrato, String puntoOrigen, String puntoDestino,
                           Double distanciaEstimadaKm, Integer numeroEntregasProgramadas,
                           EstadoRuta estado, LocalDateTime fechaInicioReal, LocalDateTime fechaFinReal,
                           LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) { }
