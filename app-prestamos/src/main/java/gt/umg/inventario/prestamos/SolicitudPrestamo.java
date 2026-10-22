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
        String codigoEquipo,
        Integer cantidad,
        String fechaDevolucion
) {

    /**
     * Devuelve una copia con los textos limpios: sin espacios sobrantes
     * y el código de equipo en mayúsculas, igual que el nodo "Validar datos".
     */
    public SolicitudPrestamo normalizada() {
        return new SolicitudPrestamo(
                limpiar(carne),
                limpiar(nombreEstudiante),
                limpiar(codigoEquipo).toUpperCase(),
                cantidad,
                limpiar(fechaDevolucion)
        );
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}