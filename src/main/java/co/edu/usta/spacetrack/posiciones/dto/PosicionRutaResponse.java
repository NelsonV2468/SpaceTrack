package co.edu.usta.spacetrack.posiciones.dto;

import co.edu.usta.spacetrack.posiciones.domain.OrigenPosicion;

import java.time.LocalDateTime;

public record PosicionRutaResponse(Long id, Long rutaId, String codigoRuta, Double latitud, Double longitud,
                                   Integer velocidadKmh, OrigenPosicion origen, LocalDateTime fechaRegistro) { }
