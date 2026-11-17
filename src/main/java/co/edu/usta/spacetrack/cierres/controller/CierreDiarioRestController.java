package co.edu.usta.spacetrack.cierres.controller;

import co.edu.usta.spacetrack.cierres.dto.CierreDiarioRequest;
import co.edu.usta.spacetrack.cierres.dto.CierreDiarioResponse;
import co.edu.usta.spacetrack.cierres.service.CierreDiarioService;
import co.edu.usta.spacetrack.shared.security.UsuarioSesion;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cierres")
public class CierreDiarioRestController {

    private final CierreDiarioService cierreService;

    public CierreDiarioRestController(CierreDiarioService cierreService) {
        this.cierreService = cierreService;
    }

    @GetMapping
    public List<CierreDiarioResponse> listar() {
        return cierreService.listar();
    }

    // Cierre guardado de la fecha, o la vista previa si el dia sigue abierto. Formato de fecha: 2026-11-16.
    @GetMapping("/{fecha}")
    public CierreDiarioResponse resumen(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return cierreService.resumen(fecha);
    }

    @PostMapping
    public ResponseEntity<CierreDiarioResponse> cerrar(@Valid @RequestBody CierreDiarioRequest solicitud,
                                                       Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cierreService.cerrar(solicitud, quien(autenticacion)));
    }

    // Con el login activo se guarda el correo del usuario; en el perfil dev (sin login) queda "desarrollo".
    private String quien(Authentication autenticacion) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion usuario) {
            return usuario.correo();
        }
        return "desarrollo";
    }
}
