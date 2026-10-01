package co.edu.usta.spacetrack.vehiculos.controller;

import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import co.edu.usta.spacetrack.vehiculos.dto.VehiculoRequest;
import co.edu.usta.spacetrack.vehiculos.service.VehiculoService;
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

@Controller
@RequestMapping("/vehiculos")
public class VehiculoWebController {
    private final VehiculoService vehiculoService;
    public VehiculoWebController(VehiculoService vehiculoService) { this.vehiculoService = vehiculoService; }

    @GetMapping
    public String listar(Model modelo) { modelo.addAttribute("vehiculos", vehiculoService.listar()); return "vehiculos/lista"; }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) { prepararFormulario(modelo, new VehiculoRequest("", "", "", "", null, null, false, true), false, null); return "vehiculos/formulario"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        var vehiculo = vehiculoService.obtenerPorId(id);
        prepararFormulario(modelo, new VehiculoRequest(vehiculo.placa(), vehiculo.tipoVehiculo(), vehiculo.marca(), vehiculo.modelo(), vehiculo.anioModelo(), vehiculo.capacidadCargaKg(), vehiculo.requiereCadenaFrio(), vehiculo.activo()), true, id);
        return "vehiculos/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("vehiculo") VehiculoRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, false, null); return "vehiculos/formulario"; }
        try { vehiculoService.crear(solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("vehiculo", ex.getMessage()); prepararFormulario(modelo, solicitud, false, null); return "vehiculos/formulario"; }
        atributos.addFlashAttribute("exito", "Vehiculo registrado correctamente.");
        return "redirect:/vehiculos";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("vehiculo") VehiculoRequest solicitud, BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) { prepararFormulario(modelo, solicitud, true, id); return "vehiculos/formulario"; }
        try { vehiculoService.actualizar(id, solicitud); } catch (ReglaDeNegocioException ex) { resultado.reject("vehiculo", ex.getMessage()); prepararFormulario(modelo, solicitud, true, id); return "vehiculos/formulario"; }
        atributos.addFlashAttribute("exito", "Vehiculo actualizado correctamente.");
        return "redirect:/vehiculos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) { vehiculoService.eliminar(id); atributos.addFlashAttribute("exito", "Vehiculo eliminado correctamente."); return "redirect:/vehiculos"; }

    private void prepararFormulario(Model modelo, VehiculoRequest vehiculo, boolean modoEdicion, Long vehiculoId) {
        modelo.addAttribute("vehiculo", vehiculo);
        modelo.addAttribute("modoEdicion", modoEdicion);
        modelo.addAttribute("vehiculoId", vehiculoId);
    }
}
