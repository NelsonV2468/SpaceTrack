package co.edu.usta.spacetrack.rutas.service;

import co.edu.usta.spacetrack.entregas.domain.Entrega;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.dto.PuntoMapa;
import co.edu.usta.spacetrack.rutas.dto.UbicacionRutaResponse;
import co.edu.usta.spacetrack.zonas.domain.Localidad;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Calcula una ubicacion simulada del vehiculo de una ruta. No hay GPS real: las paradas se generan
 * alrededor de la localidad de la zona y el vehiculo avanza entre ellas segun las entregas registradas
 * y el tiempo transcurrido desde la ultima.
 */
@Component
public class SimuladorUbicacion {

    // Centro de distribucion de City Urban Bogota (zona industrial de Puente Aranda).
    static final double CEDI_LATITUD = 4.6330;
    static final double CEDI_LONGITUD = -74.1180;

    // Segundos que tarda el vehiculo simulado en recorrer el tramo entre dos paradas.
    static final long SEGUNDOS_POR_TRAMO = 120;

    // El vehiculo se detiene antes de la parada hasta que se registre la entrega.
    private static final double AVANCE_MAXIMO_SIN_REGISTRO = 0.92;

    public UbicacionRutaResponse calcular(Ruta ruta, List<Entrega> entregasEnOrden, LocalDateTime ahora) {
        Random aleatorio = new Random(ruta.getCodigo() == null ? 0 : ruta.getCodigo().hashCode());
        double[] centro = centroDeZona(ruta.getZona() == null ? null : ruta.getZona().getLocalidad());
        int programadas = ruta.getNumeroEntregasProgramadas() == null ? 0 : ruta.getNumeroEntregasProgramadas();
        int registradas = Math.min(entregasEnOrden.size(), programadas);

        PuntoMapa origen = new PuntoMapa(CEDI_LATITUD, CEDI_LONGITUD, ruta.getPuntoOrigen(), null);
        List<double[]> coordenadas = generarParadas(aleatorio, centro, programadas);
        double[] fin = puntoDestino(centro);
        PuntoMapa destino = new PuntoMapa(fin[0], fin[1], ruta.getPuntoDestino(), null);

        List<PuntoMapa> paradas = new ArrayList<>();
        for (int i = 0; i < coordenadas.size(); i++) {
            String estado = i < registradas ? entregasEnOrden.get(i).getEstado().name() : "PENDIENTE";
            paradas.add(new PuntoMapa(coordenadas.get(i)[0], coordenadas.get(i)[1], "Parada " + (i + 1), estado));
        }

        // Recorrido completo: centro de distribucion -> paradas -> destino.
        List<double[]> recorrido = new ArrayList<>();
        recorrido.add(new double[]{origen.latitud(), origen.longitud()});
        recorrido.addAll(coordenadas);
        recorrido.add(fin);

        double[] posicion;
        double avanceTramo = 0;
        String descripcion;
        int velocidad = 0;
        EstadoRuta estado = ruta.getEstado();
        if (estado == EstadoRuta.PENDIENTE) {
            posicion = recorrido.get(0);
            descripcion = "En el centro de distribucion, pendiente de salida";
        } else if (estado == EstadoRuta.FINALIZADA) {
            posicion = fin;
            descripcion = "Ruta finalizada en el punto de destino";
        } else {
            LocalDateTime referencia = registradas > 0 ? entregasEnOrden.get(registradas - 1).getFechaRegistro()
                    : ruta.getFechaInicioReal();
            long segundos = referencia == null ? 0 : Math.max(0, Duration.between(referencia, ahora).getSeconds());
            double tope = registradas < programadas ? AVANCE_MAXIMO_SIN_REGISTRO : 1.0;
            avanceTramo = Math.min(tope, (double) segundos / SEGUNDOS_POR_TRAMO);
            posicion = interpolar(recorrido.get(registradas), recorrido.get(registradas + 1), avanceTramo);
            boolean detenido = avanceTramo >= tope;
            velocidad = detenido ? 0 : 22 + aleatorio.nextInt(14);
            if (registradas < programadas) {
                descripcion = detenido ? "En la parada " + (registradas + 1) + ", esperando registro de la entrega"
                        : "En camino a la parada " + (registradas + 1) + " de " + programadas;
            } else {
                descripcion = detenido ? "En destino, pendiente de cerrar la ruta" : "Entregas completas, rumbo al destino";
            }
        }

        double progreso = estado == EstadoRuta.FINALIZADA ? 1.0
                : (registradas + avanceTramo) / (programadas + 1);
        String placa = ruta.getVehiculo() == null ? null : ruta.getVehiculo().getPlaca();
        String conductor = ruta.getConductor() == null ? null
                : ruta.getConductor().getNombres() + " " + ruta.getConductor().getApellidos();
        return new UbicacionRutaResponse(ruta.getId(), ruta.getCodigo(), ruta.getNombre(), estado,
                ruta.getZona() == null ? null : ruta.getZona().getNombre(), placa, conductor, origen, destino, paradas,
                new PuntoMapa(posicion[0], posicion[1], placa == null ? ruta.getCodigo() : placa, null),
                registradas, programadas, Math.round(progreso * 1000) / 1000.0, velocidad, descripcion, ahora);
    }

    static double[] centroDeZona(String localidad) {
        Optional<Localidad> conocida = Localidad.buscar(localidad);
        if (conocida.isPresent()) {
            return new double[]{conocida.get().getLatitud(), conocida.get().getLongitud()};
        }
        // Solo zonas antiguas, registradas antes del catalogo, pueden tener una localidad desconocida:
        // se ubican en un punto estable dentro del area urbana de Bogota.
        int hash = Math.abs(normalizar(localidad).hashCode());
        return new double[]{4.56 + (hash % 1000) / 1000.0 * 0.16, -74.17 + (hash / 1000 % 1000) / 1000.0 * 0.12};
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    // Paradas alrededor del centro de la zona, ordenadas por vecino mas cercano desde el CEDI.
    private List<double[]> generarParadas(Random aleatorio, double[] centro, int cantidad) {
        List<double[]> pendientes = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            double angulo = aleatorio.nextDouble() * Math.PI * 2;
            double radio = 0.003 + aleatorio.nextDouble() * 0.013;
            pendientes.add(new double[]{centro[0] + radio * Math.sin(angulo), centro[1] + radio * Math.cos(angulo)});
        }
        List<double[]> ordenadas = new ArrayList<>();
        double[] actual = {CEDI_LATITUD, CEDI_LONGITUD};
        while (!pendientes.isEmpty()) {
            double[] cercana = pendientes.get(0);
            for (double[] candidata : pendientes) {
                if (distancia(actual, candidata) < distancia(actual, cercana)) {
                    cercana = candidata;
                }
            }
            pendientes.remove(cercana);
            ordenadas.add(cercana);
            actual = cercana;
        }
        return ordenadas;
    }

    // El destino queda en el extremo de la zona opuesto al centro de distribucion.
    private double[] puntoDestino(double[] centro) {
        double dLat = centro[0] - CEDI_LATITUD;
        double dLon = centro[1] - CEDI_LONGITUD;
        double largo = Math.hypot(dLat, dLon);
        if (largo < 1e-6) {
            return new double[]{centro[0] + 0.018, centro[1]};
        }
        return new double[]{centro[0] + dLat / largo * 0.018, centro[1] + dLon / largo * 0.018};
    }

    private static double distancia(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    private static double[] interpolar(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }
}
