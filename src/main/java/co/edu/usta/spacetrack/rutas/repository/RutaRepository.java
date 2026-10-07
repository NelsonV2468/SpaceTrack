package co.edu.usta.spacetrack.rutas.repository;

import co.edu.usta.spacetrack.rutas.domain.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
    boolean existsByZonaId(Long zonaId);
    List<Ruta> findAllByOrderByCodigoAsc();
}
