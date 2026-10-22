package gt.umg.inventario.prestamos;

import java.util.Objects;

/**
 * Coordina una solicitud de préstamo:
 * 1. La valida con las reglas del WF1.
 * 2. Si es inválida, la rechaza sin contactar a n8n (ahorra una llamada de red).
 * 3. Si es válida, la envía a n8n y devuelve su respuesta.
 * 4. Si n8n no responde, devuelve un error controlado en lugar de fallar.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class ServicioPrestamos {

    private final ValidadorPrestamo validador;
    private final PrestamoGateway gateway;

    public ServicioPrestamos(ValidadorPrestamo validador, PrestamoGateway gateway) {
        this.validador = Objects.requireNonNull(validador, "El validador es obligatorio");
        this.gateway = Objects.requireNonNull(gateway, "El gateway es obligatorio");
    }

    public RespuestaPrestamo solicitar(SolicitudPrestamo solicitud) {
        ResultadoValidacion validacion = validador.validar(solicitud);
        if (!validacion.esValido()) {
            return RespuestaPrestamo.rechazada(400, String.join("; ", validacion.errores()));
        }

        try {
            RespuestaPrestamo respuesta = gateway.enviar(solicitud.normalizada());
            if (respuesta == null) {
                return RespuestaPrestamo.error("El sistema de préstamos no devolvió respuesta");
            }
            return respuesta;
        } catch (PrestamoGatewayException e) {
            return RespuestaPrestamo.error(
                    "No se pudo comunicar con el sistema de préstamos: " + e.getMessage());
        }
    }
}