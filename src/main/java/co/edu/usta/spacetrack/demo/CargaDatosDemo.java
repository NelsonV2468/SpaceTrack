package co.edu.usta.spacetrack.demo;

import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.clientes.service.ClienteService;
import co.edu.usta.spacetrack.conductores.dto.ConductorRequest;
import co.edu.usta.spacetrack.conductores.service.ConductorService;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import co.edu.usta.spacetrack.entregas.dto.EntregaRequest;
import co.edu.usta.spacetrack.entregas.service.EntregaService;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteRequest;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.service.OperacionService;
import co.edu.usta.spacetrack.rutas.dto.AsignacionRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.service.RutaService;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoRequest;
import co.edu.usta.spacetrack.vehiculos.service.VehiculoService;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaRequest;
import co.edu.usta.spacetrack.posiciones.service.PosicionRutaService;
import co.edu.usta.spacetrack.zonas.domain.Localidad;
import co.edu.usta.spacetrack.zonas.service.ZonaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/**
 * Perfil demo: carga el caso de ejemplo del acta (distribucion con cadena de frio, flota de varios vehiculos
 * y entregas zonificadas) para presentar el sistema sin registrar todo a mano.
 *
 * <p>Todo se crea con los Service de cada modulo, asi que se aplican las mismas reglas de negocio que en el panel.
 * Si ya hay clientes registrados no se carga nada, para no duplicar datos en una base persistente.
 * La carga va en una sola transaccion: si una regla la rechaza, no queda nada a medias.</p>
 *
 * <p>La carga nunca impide que la aplicacion arranque: si falla se reintenta una vez (por ejemplo, si el dia
 * cambia a medianoche mientras se cargan los datos) y, si vuelve a fallar, se registra el motivo y la
 * aplicacion sigue funcionando sin datos de ejemplo.</p>
 */
@Component
@Profile("demo")
public class CargaDatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosDemo.class);

    /** Franja tipica del contrato; se amplia si la demo arranca de madrugada o de noche. */
    static final LocalTime INICIO_TIPICO = LocalTime.of(6, 0);
    static final LocalTime FIN_TIPICO = LocalTime.of(20, 0);
    /** Margen minimo entre el arranque y el fin de la franja, para que la ruta "al dia" no genere alerta. */
    static final Duration MARGEN_MINIMO = Duration.ofHours(2);
    /** Ultimo instante del dia, con segundos, para que una ruta iniciada a las 23:59:30 siga dentro de la franja. */
    static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59, 59);
    static final int INTENTOS = 2;
    /** Posicion reportada por el vehiculo desviado (occidente de Suba, lejos de su recorrido). */
    static final double LATITUD_DESVIO = 4.7650;
    static final double LONGITUD_DESVIO = -74.1300;

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final OperacionService operacionService;
    private final NecesidadClienteService necesidadService;
    private final VehiculoService vehiculoService;
    private final ConductorService conductorService;
    private final ZonaService zonaService;
    private final RutaService rutaService;
    private final EntregaService entregaService;
    private final PosicionRutaService posicionService;
    private final Clock reloj;
    private final long minutosPorEntrega;
    private final TransactionTemplate transaccion;

    public CargaDatosDemo(ClienteRepository clienteRepository, ClienteService clienteService, OperacionService operacionService,
                          NecesidadClienteService necesidadService, VehiculoService vehiculoService,
                          ConductorService conductorService, ZonaService zonaService, RutaService rutaService,
                          EntregaService entregaService, PosicionRutaService posicionService, Clock reloj, PlatformTransactionManager transacciones,
                          @Value("${spacetrack.alertas.minutos-por-entrega:10}") long minutosPorEntrega) {
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.operacionService = operacionService;
        this.necesidadService = necesidadService;
        this.vehiculoService = vehiculoService;
        this.conductorService = conductorService;
        this.zonaService = zonaService;
        this.rutaService = rutaService;
        this.entregaService = entregaService;
        this.posicionService = posicionService;
        this.reloj = reloj;
        this.minutosPorEntrega = minutosPorEntrega;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                // Cada intento es una transaccion completa: si algo falla se revierte todo lo creado en ese intento.
                Boolean cargado = transaccion.execute(estado -> {
                    if (clienteRepository.count() > 0) {
                        return false;
                    }
                    cargar();
                    return true;
                });
                if (Boolean.TRUE.equals(cargado)) {
                    log.info("Perfil demo: datos de ejemplo cargados (1 cliente, 1 operacion, 5 vehiculos, 5 conductores, 5 rutas en 5 localidades)");
                } else {
                    log.info("Perfil demo: ya existen clientes registrados; no se cargan datos de ejemplo");
                }
                return;
            } catch (RuntimeException error) {
                if (intento < INTENTOS) {
                    log.warn("Perfil demo: la carga de datos de ejemplo fallo ({}); se reintenta", error.getMessage());
                } else {
                    log.error("Perfil demo: no se cargaron los datos de ejemplo ({}). La aplicacion sigue funcionando;"
                            + " revise que la base no tenga datos con los mismos codigos (DMO101, CT-DEMO-001, R-DEMO-01...)",
                            error.getMessage(), error);
                }
            }
        }
    }

    void cargar() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalTime ahora = LocalTime.now(reloj).truncatedTo(ChronoUnit.MINUTES);
        LocalTime horaInicio = horaInicioFranja(ahora);
        LocalTime horaFin = horaFinFranja(ahora);

        Long clienteId = clienteService.crear(new ClienteRequest("Lácteos La Pradera SAS (demo)", "900555123-4",
                "Marcela Rojas", "logistica@lacteos-demo.test", "6015550101", "Calle 13 # 68-20, Bogotá", true)).id();

        Long operacionId = operacionService.crear(new OperacionRequest("CT-DEMO-001", clienteId,
                "Distribución refrigerada a tiendas y supermercados", hoy.minusDays(30), hoy.plusMonths(6),
                "Todos los dias", 5, 1200, true, EstadoOperacion.ACTIVA)).id();

        necesidadService.crear(new NecesidadClienteRequest(operacionId, "Furgón refrigerado", 5, 1500.0, true,
                EnumSet.allOf(DiaSemana.class), horaInicio, horaFin,
                "Caso de ejemplo del acta: cadena de frío de 2 a 6 °C y entregas por zonas"));

        String[][] flota = {
                {"DMO101", "Chevrolet", "NHR"}, {"DMO102", "Chevrolet", "NHR"}, {"DMO103", "JAC", "X200"},
                {"DMO104", "Foton", "Aumark"}, {"DMO105", "Hino", "Dutro"}};
        Long[] vehiculos = new Long[flota.length];
        for (int i = 0; i < flota.length; i++) {
            vehiculos[i] = vehiculoService.crear(new VehiculoRequest(flota[i][0], "Furgón refrigerado", flota[i][1],
                    flota[i][2], 2022 + i % 3, 1800.0, true, true)).id();
        }

        String[][] personas = {
                {"80123451", "Andrés", "Moreno"}, {"52123452", "Paola", "Castillo"}, {"79123453", "Julián", "Herrera"},
                {"1012345674", "Diana", "Suárez"}, {"1023456785", "Camilo", "Ortiz"}};
        Long[] conductores = new Long[personas.length];
        for (int i = 0; i < personas.length; i++) {
            String correo = personas[i][1].toLowerCase().replace("é", "e").replace("á", "a") + "."
                    + personas[i][2].toLowerCase().replace("á", "a") + "@conductores-demo.test";
            conductores[i] = conductorService.crear(new ConductorRequest("CC", personas[i][0], personas[i][1],
                    personas[i][2], "30055501" + (10 + i), correo, "LIC-DEMO-" + (101 + i),
                    hoy.plusYears(1).plusMonths(i * 4L), true)).id();
        }

        // Las zonas son las localidades de Bogota que ya estan guardadas en la base.
        Long chapinero = zona(Localidad.CHAPINERO);
        Long kennedy = zona(Localidad.KENNEDY);
        Long suba = zona(Localidad.SUBA);
        Long engativa = zona(Localidad.ENGATIVA);
        Long usaquen = zona(Localidad.USAQUEN);

        // Atrasada: le faltan mas entregas de las que caben en lo que queda de la franja (alerta critica de ON-4).
        long minutosRestantes = Duration.between(ahora, horaFin).toMinutes();
        int programadasAtrasada = (int) (3 + minutosRestantes / minutosPorEntrega + 6);

        Long alDia = ruta("R-DEMO-01", "Tiendas de Chapinero (al día)", chapinero, operacionId, "Cra 7 # 60-15", 14.5, 12, vehiculos[0], conductores[0]);
        Long atrasada = ruta("R-DEMO-02", "Supermercados de Kennedy (atrasada)", kennedy, operacionId, "Av. 1 de Mayo # 72-30", 18.0, programadasAtrasada, vehiculos[1], conductores[1]);
        Long casiCompleta = ruta("R-DEMO-03", "Minimercados de Suba (desviada)", suba, operacionId, "Cl 145 # 91-19", 21.3, 10, vehiculos[2], conductores[2]);
        ruta("R-DEMO-04", "Panaderías de Engativá (pendiente)", engativa, operacionId, "Cl 72 # 110-40", 16.8, 8, vehiculos[3], conductores[3]);
        Long finalizada = ruta("R-DEMO-05", "Droguerías de Usaquén (finalizada)", usaquen, operacionId, "Cra 15 # 124-30", 12.2, 8, vehiculos[4], conductores[4]);

        rutaService.iniciar(finalizada);
        entregas(finalizada, "USA", 7, 1, "Establecimiento cerrado a la hora de llegada");
        rutaService.finalizar(finalizada);

        rutaService.iniciar(alDia);
        entregas(alDia, "CHA", 5, 1, "Cliente ausente; se reprograma");
        rutaService.iniciar(atrasada);
        entregas(atrasada, "KEN", 2, 1, "Dirección incompleta; no se ubicó el local");
        rutaService.iniciar(casiCompleta);
        entregas(casiCompleta, "SUB", 9, 0, null);
        // El vehiculo de Suba reporta una posicion lejos de su recorrido: genera la alerta de desvio de ruta.
        posicionService.registrar(new PosicionRutaRequest(casiCompleta, LATITUD_DESVIO, LONGITUD_DESVIO, 18));
    }

    /** La franja empieza a las 06:00, o antes si la demo arranca de madrugada (la ruta debe poder iniciar ya). */
    static LocalTime horaInicioFranja(LocalTime ahora) {
        if (ahora.getHour() == 0) {
            return LocalTime.MIDNIGHT;
        }
        LocalTime unaHoraAntes = ahora.minusHours(1).truncatedTo(ChronoUnit.HOURS);
        return unaHoraAntes.isBefore(INICIO_TIPICO) ? unaHoraAntes : INICIO_TIPICO;
    }

    /** La franja termina a las 20:00, o al menos dos horas despues del arranque (maximo 23:59:59). */
    static LocalTime horaFinFranja(LocalTime ahora) {
        if (!ahora.isBefore(FIN_DEL_DIA.minus(MARGEN_MINIMO))) {
            return FIN_DEL_DIA;
        }
        LocalTime conMargen = ahora.plus(MARGEN_MINIMO);
        return conMargen.isAfter(FIN_TIPICO) ? conMargen : FIN_TIPICO;
    }

    private Long zona(Localidad localidad) {
        return zonaService.zonaDeLocalidad(localidad).getId();
    }

    private Long ruta(String codigo, String nombre, Long zonaId, Long operacionId, String destino, double km,
                      int entregas, Long vehiculoId, Long conductorId) {
        Long id = rutaService.crear(new RutaRequest(codigo, nombre, zonaId, operacionId,
                "CEDI Montevideo, Cl 19 # 68-50", destino, km, entregas)).id();
        rutaService.asignar(id, new AsignacionRequest(vehiculoId, conductorId));
        return id;
    }

    private void entregas(Long rutaId, String prefijo, int entregadas, int noEntregadas, String novedad) {
        for (int i = 1; i <= entregadas + noEntregadas; i++) {
            boolean entregada = i <= entregadas;
            entregaService.registrar(new EntregaRequest(rutaId, "GU-" + prefijo + "-" + String.format("%03d", i),
                    "Cliente " + prefijo + " " + i, "Dirección de entrega " + i + ", " + prefijo,
                    entregada ? EstadoEntrega.ENTREGADA : EstadoEntrega.NO_ENTREGADA, entregada ? null : novedad));
        }
    }
}
