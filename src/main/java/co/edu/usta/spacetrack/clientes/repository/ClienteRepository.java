package co.edu.usta.spacetrack.clientes.repository;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByNit(String nit);
    boolean existsByNitAndIdNot(String nit, Long id);
    boolean existsByCorreoContactoIgnoreCase(String correoContacto);
    boolean existsByCorreoContactoIgnoreCaseAndIdNot(String correoContacto, Long id);
    Optional<Cliente> findByNit(String nit);
    List<Cliente> findAllByOrderByRazonSocialAsc();
}
