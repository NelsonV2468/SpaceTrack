package co.edu.usta.spacetrack.entregas.repository;

import co.edu.usta.spacetrack.entregas.domain.Entrega;
import co.edu.usta.spacetrack.entregas.domain.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    List<Entrega> findByRutaIdOrderByIdAsc(Long rutaId);
    List<Entrega> findAllByOrderByIdDesc();
    long countByRutaId(Long rutaId);
    long countByRutaIdAndEstado(Long rutaId, EstadoEntrega estado);
}
