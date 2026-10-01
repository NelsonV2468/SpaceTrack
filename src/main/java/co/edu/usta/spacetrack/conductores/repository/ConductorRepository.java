package co.edu.usta.spacetrack.conductores.repository;

import co.edu.usta.spacetrack.conductores.domain.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);
    boolean existsByNumeroLicenciaIgnoreCase(String numeroLicencia);
    boolean existsByNumeroLicenciaIgnoreCaseAndIdNot(String numeroLicencia, Long id);
    List<Conductor> findAllByOrderByApellidosAscNombresAsc();
}
