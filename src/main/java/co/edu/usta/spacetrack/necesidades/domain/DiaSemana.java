package co.edu.usta.spacetrack.necesidades.domain;

public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    // El orden del enum coincide con DayOfWeek (lunes = 1 ... domingo = 7).
    public static DiaSemana desde(java.time.DayOfWeek dia) {
        return values()[dia.getValue() - 1];
    }
}
