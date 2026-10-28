package co.edu.usta.spacetrack.rutas.domain;

// Ciclo de vida de una ruta: solo avanza PENDIENTE -> EN_CURSO -> FINALIZADA.
public enum EstadoRuta {
    PENDIENTE,
    EN_CURSO,
    FINALIZADA
}
