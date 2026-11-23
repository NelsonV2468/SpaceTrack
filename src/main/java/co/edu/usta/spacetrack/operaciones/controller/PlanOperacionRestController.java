package co.edu.usta.spacetrack.operaciones.controller;

import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionResponse;
import co.edu.usta.spacetrack.operaciones.service.PlanOperacionService;
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

/** Operacion y necesidad del cliente en un solo formulario (pantalla Operaciones del panel). */
@RestController
@RequestMapping("/api/operaciones/planes")
public class PlanOperacionRestController {
    private final PlanOperacionService planService;
    public PlanOperacionRestController(PlanOperacionService planService) { this.planService = planService; }
    @GetMapping public List<PlanOperacionResponse> listar() { return planService.listar(); }
    @GetMapping("/{id}") public PlanOperacionResponse obtener(@PathVariable Long id) { return planService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<PlanOperacionResponse> crear(@Valid @RequestBody PlanOperacionRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(planService.crear(solicitud)); }
    @PutMapping("/{id}") public PlanOperacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody PlanOperacionRequest solicitud) { return planService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { planService.eliminar(id); return ResponseEntity.noContent().build(); }
}
