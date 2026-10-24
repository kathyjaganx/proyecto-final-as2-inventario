package gt.umg.inventario.prestamos;

import java.time.LocalDateTime;

/**
 * Datos que se imprimen en el comprobante PDF de un préstamo aprobado.
 * Combina lo que escribió el usuario en el formulario con lo que respondió n8n.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public record ComprobantePrestamo(
        long numeroPrestamo,
        String carne,
        String nombreEstudiante,
        String telefono,
        String codigoEquipo,
        String equipo,
        Integer cantidad,
        String fechaDevolucion,
        LocalDateTime fechaRegistro
) {

    /** Nombre con el que se descarga el archivo, por ejemplo: comprobante_prestamo_6.pdf */
    public String nombreArchivo() {
        return "comprobante_prestamo_" + numeroPrestamo + ".pdf";
    }
}