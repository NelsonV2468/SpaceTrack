package co.edu.usta.spacetrack.rutas.dto;

// Punto geografico simulado; estado solo aplica a las paradas de entrega.
public record PuntoMapa(double latitud, double longitud, String etiqueta, String estado) { }
