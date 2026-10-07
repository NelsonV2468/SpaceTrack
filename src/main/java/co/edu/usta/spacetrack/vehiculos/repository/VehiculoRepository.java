package co.edu.usta.spacetrack.vehiculos.repository;

import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByPlacaIgnoreCase(String placa);
    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);
    List<Vehiculo> findAllByOrderByPlacaAsc();

    // Cuenta los vehiculos activos que pueden cubrir una necesidad del cliente.
    @Query("""
            select count(v) from Vehiculo v
            where v.activo = true
              and lower(v.tipoVehiculo) = lower(:tipoVehiculo)
              and v.capacidadCargaKg >= :capacidadMinimaKg
              and (:requiereCadenaFrio = false or v.requiereCadenaFrio = true)
            """)
    long contarDisponibles(@Param("tipoVehiculo") String tipoVehiculo,
                           @Param("capacidadMinimaKg") Double capacidadMinimaKg,
                           @Param("requiereCadenaFrio") boolean requiereCadenaFrio);
}
