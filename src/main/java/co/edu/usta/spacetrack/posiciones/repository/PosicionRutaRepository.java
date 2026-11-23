package co.edu.usta.spacetrack.posiciones.repository;

import co.edu.usta.spacetrack.posiciones.domain.PosicionRuta;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PosicionRutaRepository extends JpaRepository<PosicionRuta, Long> {
    List<PosicionRuta> findByRutaIdOrderByIdDesc(Long rutaId, Limit limite);
    List<PosicionRuta> findAllByOrderByIdDesc(Limit limite);

    @Modifying
    @Query("delete from PosicionRuta p where p.fechaRegistro < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
