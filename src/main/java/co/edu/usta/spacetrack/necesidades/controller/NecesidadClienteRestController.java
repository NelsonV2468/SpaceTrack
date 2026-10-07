package co.edu.usta.spacetrack.necesidades.controller;

import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteRequest;
import co.edu.usta.spacetrack.necesidades.dto.NecesidadClienteResponse;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/necesidades")
public class NecesidadClienteRestController {
    private final NecesidadClienteService necesidadService;
    public NecesidadClienteRestController(NecesidadClienteService necesidadService) { this.necesidadService = necesidadService; }
    @GetMapping public List<NecesidadClienteResponse> listar(@RequestParam(required = false) Long operacionId) { return necesidadService.listar(operacionId); }
    @GetMapping("/{id}") public NecesidadClienteResponse obtener(@PathVariable Long id) { return necesidadService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<NecesidadClienteResponse> crear(@Valid @RequestBody NecesidadClienteRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(necesidadService.crear(solicitud)); }
    @PutMapping("/{id}") public NecesidadClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody NecesidadClienteRequest solicitud) { return necesidadService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { necesidadService.eliminar(id); return ResponseEntity.noContent().build(); }
}
