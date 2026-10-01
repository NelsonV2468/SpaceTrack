package co.edu.usta.spacetrack.operaciones.repository;

import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperacionRepository extends JpaRepository<Operacion, Long> {
    boolean existsByCodigoContratoIgnoreCase(String codigoContrato);
    boolean existsByCodigoContratoIgnoreCaseAndIdNot(String codigoContrato, Long id);
    List<Operacion> findAllByOrderByFechaInicioDesc();
}
