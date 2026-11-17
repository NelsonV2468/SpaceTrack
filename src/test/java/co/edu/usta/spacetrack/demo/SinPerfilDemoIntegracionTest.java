package co.edu.usta.spacetrack.demo;

import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Sin el perfil demo el cargador no existe y la base arranca vacia. */
@SpringBootTest(properties = "spacetrack.simulacion.intervalo-segundos=3600")
@ActiveProfiles("sin-demo")
class SinPerfilDemoIntegracionTest {

    @Autowired ApplicationContext contexto;
    @Autowired ClienteRepository clienteRepository;
    @Autowired RutaRepository rutaRepository;

    @Test
    void noCargaDatosDeEjemplo() {
        assertThat(contexto.getBeansOfType(CargaDatosDemo.class)).isEmpty();
        assertThat(clienteRepository.count()).isZero();
        assertThat(rutaRepository.count()).isZero();
    }
}
