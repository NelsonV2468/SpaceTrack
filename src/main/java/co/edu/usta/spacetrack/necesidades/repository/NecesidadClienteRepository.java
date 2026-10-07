package co.edu.usta.spacetrack.necesidades.repository;

import co.edu.usta.spacetrack.necesidades.domain.NecesidadCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NecesidadClienteRepository extends JpaRepository<NecesidadCliente, Long> {
    List<NecesidadCliente> findAllByOrderByIdAsc();
    List<NecesidadCliente> findByOperacionIdOrderByIdAsc(Long operacionId);

    @Query("select coalesce(sum(n.cantidadVehiculos), 0) from NecesidadCliente n where n.operacion.id = :operacionId")
    long sumarVehiculosPorOperacion(@Param("operacionId") Long operacionId);

    @Query("select coalesce(sum(n.cantidadVehiculos), 0) from NecesidadCliente n where n.operacion.id = :operacionId and n.id <> :id")
    long sumarVehiculosPorOperacionExcluyendo(@Param("operacionId") Long operacionId, @Param("id") Long id);
}
