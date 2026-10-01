package co.edu.usta.spacetrack.operaciones.controller;

import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.OperacionResponse;
import co.edu.usta.spacetrack.operaciones.service.OperacionService;
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
@RequestMapping("/api/operaciones")
public class OperacionRestController {
    private final OperacionService operacionService;
    public OperacionRestController(OperacionService operacionService) { this.operacionService = operacionService; }
    @GetMapping public List<OperacionResponse> listar() { return operacionService.listar(); }
    @GetMapping("/{id}") public OperacionResponse obtener(@PathVariable Long id) { return operacionService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<OperacionResponse> crear(@Valid @RequestBody OperacionRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(operacionService.crear(solicitud)); }
    @PutMapping("/{id}") public OperacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody OperacionRequest solicitud) { return operacionService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { operacionService.eliminar(id); return ResponseEntity.noContent().build(); }
}
