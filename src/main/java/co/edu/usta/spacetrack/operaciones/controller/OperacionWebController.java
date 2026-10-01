package co.edu.usta.spacetrack.operaciones.controller;

import co.edu.usta.spacetrack.clientes.service.ClienteService;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.dto.OperacionRequest;
import co.edu.usta.spacetrack.operaciones.service.OperacionService;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/operaciones")
public class OperacionWebController {
    private final OperacionService operacionService;
    private final ClienteService clienteService;

    public OperacionWebController(OperacionService operacionService, ClienteService clienteService) {
        this.operacionService = operacionService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model modelo) { modelo.addAttribute("operaciones", operacionService.listar()); return "operaciones/lista"; }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) {
        prepararFormulario(modelo, new OperacionRequest("", null, "", LocalDate.now(), LocalDate.now().plusMonths(1), "Lunes a sabado", 1, 1, false, EstadoOperacion.ACTIVA), false, null);
        return "operaciones/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        var operacion = operacionService.obtenerPorId(id);
        prepararFormulario(modelo, new OperacionRequest(operacion.codigoContrato(), operacion.clienteId(), operacion.nombreOperacion(), operacion.fechaInicio(), operacion.fechaFin(), operacion.diasOperacion(), operacion.numeroVehiculosRequeridos(), operacion.numeroEntregasEsperadas(), operacion.requiereCadenaFrio(), operacion.estado()), true, id);
        return "operaciones/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("operacion") OperacionRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, false, null); return "operaciones/formulario"; }
        try { operacionService.crear(solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("operacion", ex.getMessage()); prepararFormulario(modelo, solicitud, false, null); return "operaciones/formulario"; }
        atributos.addFlashAttribute("exito", "Operacion registrada correctamente.");
        return "redirect:/operaciones";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("operacion") OperacionRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, true, id); return "operaciones/formulario"; }
        try { operacionService.actualizar(id, solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("operacion", ex.getMessage()); prepararFormulario(modelo, solicitud, true, id); return "operaciones/formulario"; }
        atributos.addFlashAttribute("exito", "Operacion actualizada correctamente.");
        return "redirect:/operaciones";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) { operacionService.eliminar(id); atributos.addFlashAttribute("exito", "Operacion eliminada correctamente."); return "redirect:/operaciones"; }

    private void prepararFormulario(Model modelo, OperacionRequest operacion, boolean modoEdicion, Long operacionId) {
        modelo.addAttribute("operacion", operacion);
        modelo.addAttribute("clientes", clienteService.listar());
        modelo.addAttribute("estados", EstadoOperacion.values());
        modelo.addAttribute("modoEdicion", modoEdicion);
        modelo.addAttribute("operacionId", operacionId);
    }
}
