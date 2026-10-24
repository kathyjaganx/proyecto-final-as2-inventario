package gt.umg.inventario.prestamos;

/**
 * Datos de un préstamo aprobado, tal como los devuelve n8n.
 * Se usan para generar el comprobante en PDF.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public record DetallePrestamo(Long numeroPrestamo, String equipo, String fechaDevolucion) {
}