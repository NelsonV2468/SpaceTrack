package co.edu.usta.spacetrack.zonas.repository;

import co.edu.usta.spacetrack.zonas.domain.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZonaRepository extends JpaRepository<Zona, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<Zona> findByCodigoIgnoreCase(String codigo);
    List<Zona> findAllByOrderByCodigoAsc();
}
