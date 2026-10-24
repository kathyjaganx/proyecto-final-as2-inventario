package gt.umg.inventario.prestamos;

/**
 * Datos de una solicitud de préstamo de equipo de laboratorio.
 * Corresponde al JSON que recibe el webhook del WF1.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public record SolicitudPrestamo(
        String carne,
        String nombreEstudiante,
        String telefono,
        String codigoEquipo,
        Integer cantidad,
        String fechaDevolucion
) {

    /**
     * Devuelve una copia con los textos limpios, igual que el nodo "Validar datos":
     * sin espacios sobrantes, el código de equipo en mayúsculas
     * y el teléfono sin espacios ni guiones (5555-1234 queda como 55551234).
     */
    public SolicitudPrestamo normalizada() {
        return new SolicitudPrestamo(
                limpiar(carne),
                limpiar(nombreEstudiante),
                limpiar(telefono).replaceAll("[\\s-]", ""),
                limpiar(codigoEquipo).toUpperCase(),
                cantidad,
                limpiar(fechaDevolucion)
        );
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}