package co.edu.usta.spacetrack.vehiculos.service;

import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.domain.Vehiculo;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoRequest;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoResponse;
import co.edu.usta.spacetrack.vehiculos.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<VehiculoResponse> listar() {
        return vehiculoRepository.findAllByOrderByPlacaAsc().stream().map(this::aResponse).toList();
    }

    public VehiculoResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest solicitud) {
        validarPlacaUnica(solicitud.placa(), null);
        Vehiculo vehiculo = new Vehiculo();
        copiarDatos(solicitud, vehiculo);
        return aResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest solicitud) {
        Vehiculo vehiculo = buscarEntidad(id);
        validarPlacaUnica(solicitud.placa(), id);
        copiarDatos(solicitud, vehiculo);
        return aResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public void eliminar(Long id) {
        vehiculoRepository.delete(buscarEntidad(id));
    }

    private Vehiculo buscarEntidad(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehiculo con id " + id));
    }

    private void validarPlacaUnica(String placa, Long id) {
        boolean placaExiste = id == null ? vehiculoRepository.existsByPlacaIgnoreCase(placa)
                : vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, id);
        if (placaExiste) {
            throw new ReglaDeNegocioException("Ya existe un vehiculo registrado con esa placa");
        }
    }

    private void copiarDatos(VehiculoRequest solicitud, Vehiculo vehiculo) {
        vehiculo.setPlaca(solicitud.placa().trim().toUpperCase());
        vehiculo.setTipoVehiculo(solicitud.tipoVehiculo().trim());
        vehiculo.setMarca(solicitud.marca().trim());
        vehiculo.setModelo(solicitud.modelo().trim());
        vehiculo.setAnioModelo(solicitud.anioModelo());
        vehiculo.setCapacidadCargaKg(solicitud.capacidadCargaKg());
        vehiculo.setRequiereCadenaFrio(solicitud.requiereCadenaFrio());
        vehiculo.setActivo(solicitud.activo());
    }

    private VehiculoResponse aResponse(Vehiculo vehiculo) {
        return new VehiculoResponse(vehiculo.getId(), vehiculo.getPlaca(), vehiculo.getTipoVehiculo(),
                vehiculo.getMarca(), vehiculo.getModelo(), vehiculo.getAnioModelo(), vehiculo.getCapacidadCargaKg(),
                vehiculo.isRequiereCadenaFrio(), vehiculo.isActivo(), vehiculo.getFechaCreacion(), vehiculo.getFechaActualizacion());
    }
}
