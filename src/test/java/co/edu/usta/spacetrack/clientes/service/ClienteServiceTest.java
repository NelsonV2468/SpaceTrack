package co.edu.usta.spacetrack.clientes.service;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.repository.ClienteRepository;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private final ClienteRepository repositorio = mock(ClienteRepository.class);
    private final ClienteService servicio = new ClienteService(repositorio);

    @Test
    void creaClienteCuandoNitYCorreoNoExisten() {
        ClienteRequest solicitud = solicitud();
        when(repositorio.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.crear(solicitud);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getRazonSocial()).isEqualTo("Alimentos Urbanos SAS");
        assertThat(captor.getValue().getCorreoContacto()).isEqualTo("operaciones@urbanos.co");
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    void rechazaNitDuplicado() {
        when(repositorio.existsByNit("900123456-7")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(solicitud()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe un cliente registrado con ese NIT");
    }

    private ClienteRequest solicitud() {
        return new ClienteRequest(" Alimentos Urbanos SAS ", "900123456-7", "Laura Gomez", "OPERACIONES@URBANOS.CO", "+57 300 123 4567", "Calle 10 # 20-30", true);
    }
}
