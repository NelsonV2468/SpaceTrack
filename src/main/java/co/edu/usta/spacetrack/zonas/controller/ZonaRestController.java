package co.edu.usta.spacetrack.zonas.controller;

import co.edu.usta.spacetrack.zonas.dto.LocalidadResponse;
import co.edu.usta.spacetrack.zonas.dto.ZonaResponse;
import co.edu.usta.spacetrack.zonas.service.ZonaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo lectura: las zonas son el catalogo fijo de localidades de Bogota. */
@RestController
@RequestMapping("/api/zonas")
public class ZonaRestController {
    private final ZonaService zonaService;
    public ZonaRestController(ZonaService zonaService) { this.zonaService = zonaService; }
    @GetMapping public List<ZonaResponse> listar() { return zonaService.listar(); }
    @GetMapping("/localidades") public List<LocalidadResponse> localidades() { return zonaService.localidades(); }
    @GetMapping("/{id}") public ZonaResponse obtener(@PathVariable Long id) { return zonaService.obtenerPorId(id); }
}
