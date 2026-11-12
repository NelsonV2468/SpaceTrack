package co.edu.usta.spacetrack.usuarios.service;

import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.shared.security.IdentidadVerificada;
import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import co.edu.usta.spacetrack.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repositorio;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(repositorio, " Jefe@SpaceTrack.co , otro@spacetrack.co");
    }

    @Test
    void unaCuentaNuevaQuedaPendiente() {
        when(repositorio.findByAuthId("u1")).thenReturn(Optional.empty());
        when(repositorio.findByCorreoIgnoreCase("nuevo@correo.com")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario usuario = servicio.registrarIngreso(new IdentidadVerificada("u1", "Nuevo@Correo.com", null));

        assertThat(usuario.getRol()).isEqualTo(Rol.PENDIENTE);
        assertThat(usuario.getCorreo()).isEqualTo("nuevo@correo.com");
        assertThat(usuario.getNombre()).isEqualTo("nuevo");
        assertThat(usuario.getUltimoIngreso()).isNotNull();
    }

    @Test
    void unCorreoConfiguradoRecibeAdminSinImportarMayusculas() {
        when(repositorio.findByAuthId("u2")).thenReturn(Optional.empty());
        when(repositorio.findByCorreoIgnoreCase("jefe@spacetrack.co")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario usuario = servicio.registrarIngreso(new IdentidadVerificada("u2", "JEFE@spacetrack.co", "Jefe"));

        assertThat(usuario.getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void unUsuarioExistenteConservaSuRol() {
        Usuario existente = new Usuario("u3", "op@correo.com", "Op", Rol.OPERADOR);
        when(repositorio.findByAuthId("u3")).thenReturn(Optional.of(existente));
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(servicio.registrarIngreso(new IdentidadVerificada("u3", "op@correo.com", "Op")).getRol()).isEqualTo(Rol.OPERADOR);
    }

    @Test
    void nadiePuedeCambiarSuPropioRol() {
        assertThatThrownBy(() -> servicio.cambiarRol(5L, Rol.CONSULTA, 5L)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void noSePuedeQuitarElUltimoAdministrador() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(new Usuario("u7", "a@b.co", "A", Rol.ADMIN)));
        when(repositorio.countByRol(Rol.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarRol(7L, Rol.OPERADOR, 1L))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("administrador");
    }

    @Test
    void unAdminApruebaUnaCuentaPendiente() {
        Usuario pendiente = new Usuario("u8", "p@b.co", "P", Rol.PENDIENTE);
        when(repositorio.findById(8L)).thenReturn(Optional.of(pendiente));
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(servicio.cambiarRol(8L, Rol.CONSULTA, 1L).rol()).isEqualTo(Rol.CONSULTA);
    }
}
