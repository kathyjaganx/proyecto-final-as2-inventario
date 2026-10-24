package gt.umg.inventario.prestamos;

import java.net.URI;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controlador web de préstamos.
 * No contiene reglas de negocio: recibe el formulario, se lo pasa al
 * ServicioPrestamos y muestra el resultado en la página. Si el préstamo
 * se aprueba, guarda el comprobante en la sesión para poder descargarlo en PDF.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@Controller
public class PrestamoController {

    static final String VISTA = "prestamo";
    static final String SESION_COMPROBANTE = "comprobantePrestamo";

    private final ServicioPrestamos servicio;
    private final GeneradorComprobantePdf generadorPdf;

    public PrestamoController(ServicioPrestamos servicio, GeneradorComprobantePdf generadorPdf) {
        this.servicio = servicio;
        this.generadorPdf = generadorPdf;
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
    public String solicitarPrestamo(@ModelAttribute("formulario") FormularioPrestamo formulario,
                                    Model modelo, HttpSession sesion) {
        // Cada solicitud nueva descarta el comprobante anterior
        sesion.removeAttribute(SESION_COMPROBANTE);

        SolicitudPrestamo solicitud = formulario.aSolicitud();
        RespuestaPrestamo respuesta = servicio.solicitar(solicitud);
        modelo.addAttribute("respuesta", respuesta);

        if (respuesta.fueAprobado()) {
            if (respuesta.detalle() != null) {
                sesion.setAttribute(SESION_COMPROBANTE, crearComprobante(solicitud.normalizada(), respuesta.detalle()));
                modelo.addAttribute("comprobanteDisponible", true);
            }
            // Se limpia el formulario para registrar otro préstamo
            modelo.addAttribute("formulario", new FormularioPrestamo());
        }
        return VISTA;
    }

    /** Descarga en PDF el comprobante del último préstamo aprobado en esta sesión. */
    @GetMapping("/prestamos/comprobante")
    public ResponseEntity<byte[]> descargarComprobante(HttpSession sesion) {
        if (!(sesion.getAttribute(SESION_COMPROBANTE) instanceof ComprobantePrestamo comprobante)) {
            // No hay un préstamo aprobado en esta sesión: se regresa al formulario
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/prestamos")).build();
        }

        byte[] pdf = generadorPdf.generar(comprobante);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(comprobante.nombreArchivo()).build().toString())
                .body(pdf);
    }

    private static ComprobantePrestamo crearComprobante(SolicitudPrestamo solicitud, DetallePrestamo detalle) {
        return new ComprobantePrestamo(
                detalle.numeroPrestamo(),
                solicitud.carne(),
                solicitud.nombreEstudiante(),
                solicitud.telefono(),
                solicitud.codigoEquipo(),
                detalle.equipo(),
                solicitud.cantidad(),
                detalle.fechaDevolucion(),
                LocalDateTime.now(ValidadorPrestamo.ZONA_GUATEMALA));
    }
}