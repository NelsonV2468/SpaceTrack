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

    private static final String[] NOMBRES_VISIBLES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    /**
     * Texto legible de un conjunto de dias que {@link #interpretar} vuelve a reconocer:
     * "Todos los dias", "Lunes a sábado" o "Lunes, miércoles y viernes".
     */
    public static String describir(Set<DiaSemana> dias) {
        if (dias == null || dias.isEmpty()) {
            return "";
        }
        if (dias.size() == DiaSemana.values().length) {
            return "Todos los dias";
        }
        java.util.List<DiaSemana> orden = dias.stream().sorted().toList();
        DiaSemana primero = orden.get(0), ultimo = orden.get(orden.size() - 1);
        boolean seguidos = ultimo.ordinal() - primero.ordinal() + 1 == orden.size();
        if (seguidos && orden.size() >= 3) {
            return NOMBRES_VISIBLES[primero.ordinal()] + " a " + NOMBRES_VISIBLES[ultimo.ordinal()].toLowerCase();
        }
        java.util.List<String> nombres = new java.util.ArrayList<>();
        for (int i = 0; i < orden.size(); i++) {
            String nombre = NOMBRES_VISIBLES[orden.get(i).ordinal()];
            nombres.add(i == 0 ? nombre : nombre.toLowerCase());
        }
        if (nombres.size() == 1) {
            return nombres.get(0);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
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
