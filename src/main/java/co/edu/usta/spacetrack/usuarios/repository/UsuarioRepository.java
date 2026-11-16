package co.edu.usta.spacetrack.usuarios.repository;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByAuthId(String authId);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    long countByRol(Rol rol);
    List<Usuario> findAllByOrderByFechaCreacionDesc();
}
