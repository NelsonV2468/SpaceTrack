package co.edu.usta.spacetrack.shared.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Redirecciones hacia el frontend Next.js (las paginas viven en carpetas con "/" final). */
@Controller
public class InicioController {

    @GetMapping({"/", "/panel.html"})
    public String inicio() {
        return "redirect:/panel/";
    }

    @GetMapping("/login.html")
    public String loginAnterior() {
        return "redirect:/login/";
    }

    /** /login -> /login/, /panel/rutas/detalle?id=1 -> /panel/rutas/detalle/?id=1, etc. */
    @GetMapping({"/login", "/registro", "/panel", "/panel/{seccion:[a-z]+}", "/panel/rutas/detalle"})
    public String agregarBarraFinal(HttpServletRequest request) {
        String consulta = request.getQueryString();
        return "redirect:" + request.getRequestURI() + "/" + (consulta == null ? "" : "?" + consulta);
    }
}
