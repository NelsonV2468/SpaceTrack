package co.edu.usta.spacetrack.posiciones.controller;

import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaRequest;
import co.edu.usta.spacetrack.posiciones.dto.PosicionRutaResponse;
import co.edu.usta.spacetrack.posiciones.service.PosicionRutaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posiciones")
public class PosicionRutaRestController {

    private final PosicionRutaService posicionService;

    public PosicionRutaRestController(PosicionRutaService posicionService) {
        this.posicionService = posicionService;
    }

    @GetMapping
    public List<PosicionRutaResponse> listar(@RequestParam(required = false) Long rutaId,
                                             @RequestParam(defaultValue = "100") int limite) {
        return posicionService.listar(rutaId, limite);
    }

    @PostMapping
    public ResponseEntity<PosicionRutaResponse> registrar(@Valid @RequestBody PosicionRutaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(posicionService.registrar(solicitud));
    }
}
