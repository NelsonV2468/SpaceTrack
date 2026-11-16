package co.edu.usta.spacetrack.zonas.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Catalogo de localidades de Bogota y municipios vecinos que se pueden ubicar en el mapa.
 * Una zona solo se registra con una localidad de este catalogo, asi siempre tiene coordenadas conocidas.
 */
public enum Localidad {
    USAQUEN("Usaquén", "usaquen", 4.7110, -74.0300),
    CHAPINERO("Chapinero", "chapinero", 4.6480, -74.0620),
    SANTA_FE("Santa Fe", "santa fe", 4.6100, -74.0700),
    SAN_CRISTOBAL("San Cristóbal", "san cristobal", 4.5650, -74.0880),
    USME("Usme", "usme", 4.5000, -74.1100),
    TUNJUELITO("Tunjuelito", "tunjuelito", 4.5760, -74.1330),
    BOSA("Bosa", "bosa", 4.6180, -74.1880),
    KENNEDY("Kennedy", "kennedy", 4.6300, -74.1550),
    FONTIBON("Fontibón", "fontibon", 4.6740, -74.1450),
    ENGATIVA("Engativá", "engativa", 4.7070, -74.1100),
    SUBA("Suba", "suba", 4.7420, -74.0840),
    BARRIOS_UNIDOS("Barrios Unidos", "barrios unidos", 4.6680, -74.0760),
    TEUSAQUILLO("Teusaquillo", "teusaquillo", 4.6380, -74.0850),
    LOS_MARTIRES("Los Mártires", "martires", 4.6050, -74.0920),
    ANTONIO_NARINO("Antonio Nariño", "antonio narino", 4.5880, -74.1020),
    PUENTE_ARANDA("Puente Aranda", "puente aranda", 4.6170, -74.1170),
    LA_CANDELARIA("La Candelaria", "candelaria", 4.5960, -74.0740),
    RAFAEL_URIBE_URIBE("Rafael Uribe Uribe", "rafael uribe", 4.5700, -74.1160),
    CIUDAD_BOLIVAR("Ciudad Bolívar", "ciudad bolivar", 4.5450, -74.1500),
    SOACHA("Soacha", "soacha", 4.5790, -74.2170),
    MOSQUERA("Mosquera", "mosquera", 4.7060, -74.2300),
    FUNZA("Funza", "funza", 4.7160, -74.2110);

    // Las claves mas largas se prueban primero para que un nombre no quede opacado por otro mas corto.
    private static final List<Localidad> POR_CLAVE = Arrays.stream(values())
            .sorted(Comparator.comparingInt((Localidad l) -> l.clave.length()).reversed())
            .toList();

    private final String nombre;
    private final String clave;
    private final double latitud;
    private final double longitud;

    Localidad(String nombre, String clave, double latitud, double longitud) {
        this.nombre = nombre;
        this.clave = clave;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }

    /**
     * Reconoce la localidad sin importar tildes, mayusculas ni texto adicional
     * ("Localidad de Usaquen" o "USAQUÉN" equivalen a Usaquén).
     */
    public static Optional<Localidad> buscar(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isBlank()) {
            return Optional.empty();
        }
        return POR_CLAVE.stream().filter(l -> normalizado.contains(l.clave)).findFirst();
    }

    public static String nombresDisponibles() {
        return String.join(", ", Arrays.stream(values()).map(Localidad::getNombre).toList());
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
