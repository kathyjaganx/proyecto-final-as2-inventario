package gt.umg.inventario.prestamos;

/**
 * Puerta de comunicación con el sistema que registra los préstamos.
 * La implementación real envía la solicitud al webhook del WF1 de n8n;
 * en las pruebas unitarias Mockito la reemplaza por una simulación.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public interface PrestamoGateway {

    RespuestaPrestamo enviar(SolicitudPrestamo solicitud) throws PrestamoGatewayException;
}