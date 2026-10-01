package co.edu.usta.spacetrack.vehiculos.controller;

import co.edu.usta.spacetrack.vehiculos.dto.VehiculoRequest;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoResponse;
import co.edu.usta.spacetrack.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoRestController {
    private final VehiculoService vehiculoService;
    public VehiculoRestController(VehiculoService vehiculoService) { this.vehiculoService = vehiculoService; }
    @GetMapping public List<VehiculoResponse> listar() { return vehiculoService.listar(); }
    @GetMapping("/{id}") public VehiculoResponse obtener(@PathVariable Long id) { return vehiculoService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.crear(solicitud)); }
    @PutMapping("/{id}") public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest solicitud) { return vehiculoService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { vehiculoService.eliminar(id); return ResponseEntity.noContent().build(); }
}
