package co.edu.usta.spacetrack.vehiculos.repository;

import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByPlacaIgnoreCase(String placa);
    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);
    List<Vehiculo> findAllByOrderByPlacaAsc();
}
