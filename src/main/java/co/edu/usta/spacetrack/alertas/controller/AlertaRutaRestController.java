package co.edu.usta.spacetrack.alertas.controller;

import co.edu.usta.spacetrack.alertas.dto.AlertaRutaResponse;
import co.edu.usta.spacetrack.alertas.service.AlertaRutaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas/rutas")
public class AlertaRutaRestController {

    private final AlertaRutaService alertaService;

    public AlertaRutaRestController(AlertaRutaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    public List<AlertaRutaResponse> listar() {
        return alertaService.alertasActivas();
    }

    // 204 cuando la ruta existe pero no tiene alerta.
    @GetMapping("/{rutaId}")
    public ResponseEntity<AlertaRutaResponse> deRuta(@PathVariable Long rutaId) {
        return alertaService.alertaDeRuta(rutaId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
}
