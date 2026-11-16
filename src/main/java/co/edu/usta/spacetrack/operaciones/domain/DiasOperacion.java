package co.edu.usta.spacetrack.operaciones.domain;

import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta el texto de dias de operacion de un contrato, por ejemplo "Lunes a sabado",
 * "Lunes, miercoles y viernes" o "Todos los dias". Permite validar contra la fecha real
 * en que se inicia una ruta sin cambiar el formato que ya usan los usuarios.
 */
public final class DiasOperacion {

    public static final String EJEMPLOS = "'Lunes a viernes', 'Lunes, miercoles y viernes' o 'Todos los dias'";

    private static final Map<String, DiaSemana> NOMBRES = Map.ofEntries(
            Map.entry("lunes", DiaSemana.LUNES),
            Map.entry("martes", DiaSemana.MARTES),
            Map.entry("miercoles", DiaSemana.MIERCOLES),
            Map.entry("jueves", DiaSemana.JUEVES),
            Map.entry("viernes", DiaSemana.VIERNES),
            Map.entry("sabado", DiaSemana.SABADO),
            Map.entry("sabados", DiaSemana.SABADO),
            Map.entry("domingo", DiaSemana.DOMINGO),
            Map.entry("domingos", DiaSemana.DOMINGO));

    private static final Pattern RANGO = Pattern.compile("^(?:de )?(\\p{L}+) (?:a|al|hasta) (\\p{L}+)$");
    private static final Pattern SEPARADORES = Pattern.compile("\\s*(?:,|;|/| y | e )\\s*");

    private DiasOperacion() {
    }

    /** Devuelve los dias del texto, o vacio si el texto no se puede interpretar. */
    public static Optional<Set<DiaSemana>> interpretar(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return Optional.empty();
        }
        if (normalizado.equals("todos los dias") || normalizado.equals("toda la semana")) {
            return Optional.of(EnumSet.allOf(DiaSemana.class));
        }
        Set<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        for (String parte : SEPARADORES.split(normalizado)) {
            Matcher rango = RANGO.matcher(parte);
            if (rango.matches()) {
                DiaSemana desde = NOMBRES.get(rango.group(1));
                DiaSemana hasta = NOMBRES.get(rango.group(2));
                if (desde == null || hasta == null) {
                    return Optional.empty();
                }
                agregarRango(dias, desde, hasta);
            } else {
                DiaSemana dia = NOMBRES.get(parte.startsWith("de ") ? parte.substring(3) : parte);
                if (dia == null) {
                    return Optional.empty();
                }
                dias.add(dia);
            }
        }
        return Optional.of(dias);
    }

    // Admite rangos que cruzan el fin de semana, por ejemplo "viernes a lunes".
    private static void agregarRango(Set<DiaSemana> dias, DiaSemana desde, DiaSemana hasta) {
        DiaSemana[] semana = DiaSemana.values();
        int i = desde.ordinal();
        while (true) {
            dias.add(semana[i]);
            if (i == hasta.ordinal()) {
                return;
            }
            i = (i + 1) % semana.length;
        }
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replaceAll("[.]", "").replaceAll("\\s+", " ").trim();
    }
}
