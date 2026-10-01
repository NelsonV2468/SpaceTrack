package co.edu.usta.spacetrack.conductores.controller;

import co.edu.usta.spacetrack.conductores.dto.ConductorRequest;
import co.edu.usta.spacetrack.conductores.dto.ConductorResponse;
import co.edu.usta.spacetrack.conductores.service.ConductorService;
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
@RequestMapping("/api/conductores")
public class ConductorRestController {
    private final ConductorService conductorService;
    public ConductorRestController(ConductorService conductorService) { this.conductorService = conductorService; }
    @GetMapping public List<ConductorResponse> listar() { return conductorService.listar(); }
    @GetMapping("/{id}") public ConductorResponse obtener(@PathVariable Long id) { return conductorService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<ConductorResponse> crear(@Valid @RequestBody ConductorRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(conductorService.crear(solicitud)); }
    @PutMapping("/{id}") public ConductorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConductorRequest solicitud) { return conductorService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { conductorService.eliminar(id); return ResponseEntity.noContent().build(); }
}
