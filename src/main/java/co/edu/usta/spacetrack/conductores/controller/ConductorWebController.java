package co.edu.usta.spacetrack.conductores.controller;

import co.edu.usta.spacetrack.conductores.dto.ConductorRequest;
import co.edu.usta.spacetrack.conductores.service.ConductorService;
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
@RequestMapping("/conductores")
public class ConductorWebController {
    private final ConductorService conductorService;
    public ConductorWebController(ConductorService conductorService) { this.conductorService = conductorService; }

    @GetMapping
    public String listar(Model modelo) { modelo.addAttribute("conductores", conductorService.listar()); return "conductores/lista"; }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) { prepararFormulario(modelo, new ConductorRequest("CC", "", "", "", "", "", "", LocalDate.now().plusYears(1), true), false, null); return "conductores/formulario"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        var conductor = conductorService.obtenerPorId(id);
        prepararFormulario(modelo, new ConductorRequest(conductor.tipoDocumento(), conductor.numeroDocumento(), conductor.nombres(), conductor.apellidos(), conductor.telefono(), conductor.correo(), conductor.numeroLicencia(), conductor.fechaVencimientoLicencia(), conductor.activo()), true, id);
        return "conductores/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("conductor") ConductorRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, false, null); return "conductores/formulario"; }
        try { conductorService.crear(solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("conductor", ex.getMessage()); prepararFormulario(modelo, solicitud, false, null); return "conductores/formulario"; }
        atributos.addFlashAttribute("exito", "Conductor registrado correctamente.");
        return "redirect:/conductores";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("conductor") ConductorRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, true, id); return "conductores/formulario"; }
        try { conductorService.actualizar(id, solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("conductor", ex.getMessage()); prepararFormulario(modelo, solicitud, true, id); return "conductores/formulario"; }
        atributos.addFlashAttribute("exito", "Conductor actualizado correctamente.");
        return "redirect:/conductores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) { conductorService.eliminar(id); atributos.addFlashAttribute("exito", "Conductor eliminado correctamente."); return "redirect:/conductores"; }

    private void prepararFormulario(Model modelo, ConductorRequest conductor, boolean modoEdicion, Long conductorId) {
        modelo.addAttribute("conductor", conductor);
        modelo.addAttribute("modoEdicion", modoEdicion);
        modelo.addAttribute("conductorId", conductorId);
    }
}
