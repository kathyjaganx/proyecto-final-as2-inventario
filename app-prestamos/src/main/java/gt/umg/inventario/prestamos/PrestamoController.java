package gt.umg.inventario.prestamos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controlador web de préstamos.
 * No contiene reglas de negocio: recibe el formulario, se lo pasa al
 * ServicioPrestamos y muestra el resultado en la página.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@Controller
public class PrestamoController {

    static final String VISTA = "prestamo";

    private final ServicioPrestamos servicio;

    public PrestamoController(ServicioPrestamos servicio) {
        this.servicio = servicio;
    }

    /** La dirección principal lleva directo al formulario. */
    @GetMapping("/")
    public String inicio() {
        return "redirect:/prestamos";
    }

    /** Muestra el formulario vacío. */
    @GetMapping("/prestamos")
    public String mostrarFormulario(Model modelo) {
        modelo.addAttribute("formulario", new FormularioPrestamo());
        return VISTA;
    }

    /** Recibe el formulario, solicita el préstamo y muestra el resultado. */
    @PostMapping("/prestamos")
    public String solicitarPrestamo(@ModelAttribute("formulario") FormularioPrestamo formulario, Model modelo) {
        RespuestaPrestamo respuesta = servicio.solicitar(formulario.aSolicitud());
        modelo.addAttribute("respuesta", respuesta);

        // Si se aprobó, se limpia el formulario para registrar otro préstamo
        if (respuesta.fueAprobado()) {
            modelo.addAttribute("formulario", new FormularioPrestamo());
        }
        return VISTA;
    }
}