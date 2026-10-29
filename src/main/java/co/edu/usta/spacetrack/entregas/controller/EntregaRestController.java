package co.edu.usta.spacetrack.entregas.controller;

import co.edu.usta.spacetrack.entregas.dto.EntregaRequest;
import co.edu.usta.spacetrack.entregas.dto.EntregaResponse;
import co.edu.usta.spacetrack.entregas.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
public class EntregaRestController {
    private final EntregaService entregaService;
    public EntregaRestController(EntregaService entregaService) { this.entregaService = entregaService; }
    @GetMapping public List<EntregaResponse> listar(@RequestParam(required = false) Long rutaId) { return entregaService.listar(rutaId); }
    @PostMapping public ResponseEntity<EntregaResponse> registrar(@Valid @RequestBody EntregaRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(entregaService.registrar(solicitud)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { entregaService.eliminar(id); return ResponseEntity.noContent().build(); }
}
