package co.edu.usta.spacetrack.zonas.controller;

import co.edu.usta.spacetrack.zonas.dto.ZonaRequest;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.service.ZonaService;
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
@RequestMapping("/api/zonas")
public class ZonaRestController {
    private final ZonaService zonaService;
    public ZonaRestController(ZonaService zonaService) { this.zonaService = zonaService; }
    @GetMapping public List<ZonaResponse> listar() { return zonaService.listar(); }
    @GetMapping("/{id}") public ZonaResponse obtener(@PathVariable Long id) { return zonaService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody ZonaRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.crear(solicitud)); }
    @PutMapping("/{id}") public ZonaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ZonaRequest solicitud) { return zonaService.actualizar(id, solicitud); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { zonaService.eliminar(id); return ResponseEntity.noContent().build(); }
}
