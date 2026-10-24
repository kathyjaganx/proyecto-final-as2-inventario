package gt.umg.inventario.prestamos;

/**
 * Campos del formulario web de préstamo, tal como los escribe el usuario.
 * Todo llega como texto: la conversión y las reglas las aplica el ValidadorPrestamo,
 * así cualquier dato mal escrito recibe un mensaje claro en lugar de un error técnico.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class FormularioPrestamo {

    private String carne;
    private String nombreEstudiante;
    private String telefono;
    private String codigoEquipo;
    private String cantidad;
    private String fechaDevolucion;

    /** Convierte el formulario en una solicitud para el ServicioPrestamos. */
    public SolicitudPrestamo aSolicitud() {
        return new SolicitudPrestamo(carne, nombreEstudiante, telefono, codigoEquipo,
                convertirCantidad(), fechaDevolucion);
    }

    /** Devuelve la cantidad como número, o null si no es un número entero válido. */
    private Integer convertirCantidad() {
        if (cantidad == null || cantidad.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(cantidad.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Getters y setters: Spring los usa para llenar el formulario con lo que envía el navegador

    public String getCarne() { return carne; }
    public void setCarne(String carne) { this.carne = carne; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCodigoEquipo() { return codigoEquipo; }
    public void setCodigoEquipo(String codigoEquipo) { this.codigoEquipo = codigoEquipo; }

    public String getCantidad() { return cantidad; }
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }

    public String getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(String fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
}