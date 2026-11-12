package co.edu.usta.spacetrack.usuarios.service;

import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.shared.security.IdentidadVerificada;
import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import co.edu.usta.spacetrack.usuarios.dto.UsuarioResponse;
import co.edu.usta.spacetrack.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final Set<String> correosAdministradores;

    public UsuarioService(UsuarioRepository repositorio,
                          @Value("${spacetrack.seguridad.correos-admin:}") String correosAdministradores) {
        this.repositorio = repositorio;
        this.correosAdministradores = Arrays.stream(correosAdministradores.split(","))
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .map(c -> c.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Registra el ingreso de una identidad ya verificada. Las cuentas nuevas quedan PENDIENTES,
     * salvo los correos configurados como administradores, que siempre reciben ADMIN.
     */
    @Transactional
    public Usuario registrarIngreso(IdentidadVerificada identidad) {
        String correo = identidad.correo().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = repositorio.findByAuthId(identidad.authId())
                .or(() -> repositorio.findByCorreoIgnoreCase(correo))
                .orElseGet(() -> new Usuario(identidad.authId(), correo, nombreVisible(identidad, correo), Rol.PENDIENTE));

        usuario.setAuthId(identidad.authId());
        usuario.setCorreo(correo);
        usuario.setNombre(nombreVisible(identidad, correo));
        if (correosAdministradores.contains(correo)) {
            usuario.setRol(Rol.ADMIN);
        }
        usuario.setUltimoIngreso(LocalDateTime.now());
        return repositorio.save(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscar(Long id) {
        return repositorio.findById(id);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repositorio.findAllByOrderByFechaCreacionDesc().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Rol nuevoRol, Long idSolicitante) {
        if (id.equals(idSolicitante)) {
            throw new ReglaDeNegocioException("No puede cambiar su propio rol");
        }
        Usuario usuario = repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
        if (usuario.getRol() == Rol.ADMIN && nuevoRol != Rol.ADMIN && repositorio.countByRol(Rol.ADMIN) <= 1) {
            throw new ReglaDeNegocioException("Debe quedar al menos un administrador");
        }
        if (correosAdministradores.contains(usuario.getCorreo()) && nuevoRol != Rol.ADMIN) {
            throw new ReglaDeNegocioException("Este correo esta configurado como administrador en el servidor");
        }
        usuario.setRol(nuevoRol);
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    private String nombreVisible(IdentidadVerificada identidad, String correo) {
        String nombre = identidad.nombre() == null ? "" : identidad.nombre().trim();
        if (nombre.isEmpty()) nombre = correo.substring(0, Math.max(correo.indexOf('@'), 1));
        return nombre.length() > 120 ? nombre.substring(0, 120) : nombre;
    }
}
