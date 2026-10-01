package co.edu.usta.spacetrack.clientes.controller;

import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.service.ClienteService;
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

@Controller
@RequestMapping("/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;

    public ClienteWebController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("clientes", clienteService.listar());
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) {
        modelo.addAttribute("cliente", new ClienteRequest("", "", "", "", "", "", true));
        modelo.addAttribute("modoEdicion", false);
        return "clientes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        var cliente = clienteService.obtenerPorId(id);
        modelo.addAttribute("cliente", new ClienteRequest(cliente.razonSocial(), cliente.nit(), cliente.nombreContacto(),
                cliente.correoContacto(), cliente.telefonoContacto(), cliente.direccion(), cliente.activo()));
        modelo.addAttribute("clienteId", id);
        modelo.addAttribute("modoEdicion", true);
        return "clientes/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("cliente") ClienteRequest cliente, BindingResult resultado,
                        Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            modelo.addAttribute("modoEdicion", false);
            return "clientes/formulario";
        }
        try {
            clienteService.crear(cliente);
        } catch (ReglaDeNegocioException ex) {
            resultado.reject("cliente", ex.getMessage());
            modelo.addAttribute("modoEdicion", false);
            return "clientes/formulario";
        }
        atributos.addFlashAttribute("exito", "Cliente registrado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("cliente") ClienteRequest cliente,
                             BindingResult resultado, Model modelo, RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            modelo.addAttribute("clienteId", id);
            modelo.addAttribute("modoEdicion", true);
            return "clientes/formulario";
        }
        try {
            clienteService.actualizar(id, cliente);
        } catch (ReglaDeNegocioException ex) {
            resultado.reject("cliente", ex.getMessage());
            modelo.addAttribute("clienteId", id);
            modelo.addAttribute("modoEdicion", true);
            return "clientes/formulario";
        }
        atributos.addFlashAttribute("exito", "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) {
        clienteService.eliminar(id);
        atributos.addFlashAttribute("exito", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }
}
