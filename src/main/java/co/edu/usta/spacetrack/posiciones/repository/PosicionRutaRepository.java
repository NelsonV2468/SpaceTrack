package co.edu.usta.spacetrack.posiciones.repository;

import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosicionRutaRepository extends JpaRepository<PosicionRuta, Long> {
    List<PosicionRuta> findByRutaIdOrderByIdDesc(Long rutaId, Limit limite);
    List<PosicionRuta> findAllByOrderByIdDesc(Limit limite);
}
