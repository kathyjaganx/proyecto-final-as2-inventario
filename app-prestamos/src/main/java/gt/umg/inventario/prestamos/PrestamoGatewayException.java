package gt.umg.inventario.prestamos;

/**
 * Error de comunicación con el sistema de préstamos (n8n):
 * servicio apagado, tiempo de espera agotado o respuesta ilegible.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class PrestamoGatewayException extends Exception {

    public PrestamoGatewayException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}