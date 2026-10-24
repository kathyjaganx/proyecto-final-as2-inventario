package gt.umg.inventario.prestamos;

/**
 * Respuesta del sistema a una solicitud de préstamo.
 * Refleja lo que devuelve el webhook del WF1 (201, 400, 403 o 409).
 * Cuando el préstamo se aprueba, incluye además el detalle del préstamo.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public record RespuestaPrestamo(int codigoHttp, String estado, String mensaje, DetallePrestamo detalle) {

    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String ERROR = "ERROR";

    /** Respuesta sin detalle: rechazos, errores o aprobaciones sin datos adicionales. */
    public RespuestaPrestamo(int codigoHttp, String estado, String mensaje) {
        this(codigoHttp, estado, mensaje, null);
    }

    public boolean fueAprobado() {
        return codigoHttp == 201 && APROBADO.equals(estado);
    }

    public static RespuestaPrestamo rechazada(int codigoHttp, String mensaje) {
        return new RespuestaPrestamo(codigoHttp, RECHAZADO, mensaje);
    }

    public static RespuestaPrestamo error(String mensaje) {
        return new RespuestaPrestamo(503, ERROR, mensaje);
    }
}