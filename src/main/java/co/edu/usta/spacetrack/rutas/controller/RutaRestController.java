package co.edu.usta.spacetrack.rutas.controller;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.dto.RutaRequest;
import co.edu.usta.spacetrack.rutas.dto.RutaResponse;
import co.edu.usta.spacetrack.rutas.service.RutaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rutas")
public class RutaRestController {
    private final RutaService rutaService;
    public RutaRestController(RutaService rutaService) { this.rutaService = rutaService; }
    @GetMapping public List<RutaResponse> listar(@RequestParam(required = false) EstadoRuta estado) { return rutaService.listar(estado); }
    @GetMapping("/{id}") public RutaResponse obtener(@PathVariable Long id) { return rutaService.obtenerPorId(id); }
    @PostMapping public ResponseEntity<RutaResponse> crear(@Valid @RequestBody RutaRequest solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(rutaService.crear(solicitud)); }
    @PutMapping("/{id}") public RutaResponse actualizar(@PathVariable Long id, @Valid @RequestBody RutaRequest solicitud) { return rutaService.actualizar(id, solicitud); }
    @PatchMapping("/{id}/iniciar") public RutaResponse iniciar(@PathVariable Long id) { return rutaService.iniciar(id); }
    @PatchMapping("/{id}/finalizar") public RutaResponse finalizar(@PathVariable Long id) { return rutaService.finalizar(id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { rutaService.eliminar(id); return ResponseEntity.noContent().build(); }
}
