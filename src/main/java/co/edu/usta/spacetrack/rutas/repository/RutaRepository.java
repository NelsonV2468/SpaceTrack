package co.edu.usta.spacetrack.rutas.repository;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
    boolean existsByZonaId(Long zonaId);
    List<Ruta> findAllByOrderByCodigoAsc();
    List<Ruta> findByEstadoOrderByCodigoAsc(EstadoRuta estado);

    // Un vehiculo o conductor no puede estar en dos rutas en curso al mismo tiempo.
    boolean existsByVehiculoIdAndEstadoAndIdNot(Long vehiculoId, EstadoRuta estado, Long id);
    boolean existsByConductorIdAndEstadoAndIdNot(Long conductorId, EstadoRuta estado, Long id);

    @Query("select coalesce(sum(r.numeroEntregasProgramadas), 0) from Ruta r where r.operacion.id = :operacionId")
    long sumarEntregasProgramadasPorOperacion(@Param("operacionId") Long operacionId);
}
