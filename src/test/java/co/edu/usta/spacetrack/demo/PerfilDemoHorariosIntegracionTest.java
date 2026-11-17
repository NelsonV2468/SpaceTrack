package co.edu.usta.spacetrack.demo;

import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.conductores.repository.ConductorRepository;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.necesidades.repository.NecesidadClienteRepository;
import co.edu.usta.spacetrack.operaciones.repository.OperacionRepository;
import co.edu.usta.spacetrack.posiciones.repository.PosicionRutaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import co.edu.usta.spacetrack.zonas.repository.ZonaRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La carga demo debe funcionar a cualquier hora en que se arranque la aplicacion, incluidos los limites de la
 * franja y los ultimos segundos del dia. Cada caso borra la base y vuelve a cargar con el reloj en esa hora.
 */
@SpringBootTest(properties = "spacetrack.simulacion.intervalo-segundos=3600")
@ActiveProfiles("demo")
class PerfilDemoHorariosIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final RelojAjustable RELOJ = new RelojAjustable();

    @TestConfiguration
    static class Configuracion {
        @Bean
        @Primary
        Clock relojAjustable() {
            return RELOJ;
        }
    }

    @Autowired CargaDatosDemo carga;
    @Autowired PosicionRutaRepository posicionRepository;
    @Autowired EntregaRepository entregaRepository;
    @Autowired RutaRepository rutaRepository;
    @Autowired NecesidadClienteRepository necesidadRepository;
    @Autowired OperacionRepository operacionRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired VehiculoRepository vehiculoRepository;
    @Autowired ConductorRepository conductorRepository;
    @Autowired ZonaRepository zonaRepository;

    @ParameterizedTest
    @ValueSource(strings = {"00:00:05", "00:59:59", "03:30:00", "05:59:59", "06:00:00", "12:00:00",
            "17:59:59", "19:30:00", "21:59:30", "22:45:00", "23:59:00", "23:59:58"})
    void cargaCompletaACualquierHora(String hora) {
        vaciarBase();
        RELOJ.ajustar(LocalDate.now(BOGOTA).atTime(LocalTime.parse(hora)).atZone(BOGOTA).toInstant());

        carga.run(null);

        assertThat(clienteRepository.count()).as("clientes a las %s", hora).isEqualTo(1);
        assertThat(rutaRepository.count()).isEqualTo(5);
        assertThat(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.EN_CURSO)).hasSize(3);
        assertThat(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.PENDIENTE)).hasSize(1);
        assertThat(rutaRepository.findByEstadoOrderByCodigoAsc(EstadoRuta.FINALIZADA)).hasSize(1);
        assertThat(entregaRepository.count()).isEqualTo(8 + 6 + 3 + 9);
    }

    private void vaciarBase() {
        posicionRepository.deleteAll();
        entregaRepository.deleteAll();
        rutaRepository.deleteAll();
        necesidadRepository.deleteAll();
        operacionRepository.deleteAll();
        clienteRepository.deleteAll();
        vehiculoRepository.deleteAll();
        conductorRepository.deleteAll();
        zonaRepository.deleteAll();
    }

    /** Reloj de prueba que se puede mover a la hora de cada caso. */
    static class RelojAjustable extends Clock {
        private volatile Instant instante = Instant.now();

        void ajustar(Instant nuevo) {
            this.instante = nuevo;
        }

        @Override
        public ZoneId getZone() {
            return BOGOTA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(instante, zona);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
