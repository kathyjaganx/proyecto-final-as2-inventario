package gt.umg.inventario.prestamos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Pruebas unitarias del N8nPrestamoGateway.
 * MockRestServiceServer simula el webhook de n8n: no hace falta Docker ni red.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@DisplayName("N8nPrestamoGateway")
class N8nPrestamoGatewayTest {

    private static final String URL = "http://localhost:5678/webhook/prestamos";
    private static final String CLAVE = "clave-de-prueba";

    private MockRestServiceServer n8nSimulado;
    private N8nPrestamoGateway gateway;

    @BeforeEach
    void prepararGateway() {
        RestClient.Builder builder = RestClient.builder();
        n8nSimulado = MockRestServiceServer.bindTo(builder).build();
        gateway = new N8nPrestamoGateway(builder, URL, CLAVE);
    }

    @AfterEach
    void verificarQueSeRecibieronLasPeticionesEsperadas() {
        n8nSimulado.verify();
    }

    private SolicitudPrestamo solicitud() {
        return new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", "55551234", "LAP-001", 1, "2026-10-29");
    }

    // ---------- La petición que se envía ----------

    @Test
    @DisplayName("Envía POST al webhook con la clave X-API-Key y el JSON que espera el WF1")
    void enviaPeticionCorrecta() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(N8nPrestamoGateway.CABECERA_CLAVE, CLAVE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.carne").value("4090-23-1234"))
                .andExpect(jsonPath("$.nombre_estudiante").value("Ana Lucia Pop"))
                .andExpect(jsonPath("$.telefono").value("55551234"))
                .andExpect(jsonPath("$.codigo_equipo").value("LAP-001"))
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.fecha_devolucion").value("2026-10-29"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"estado":"APROBADO","mensaje":"Prestamo registrado correctamente",
                               "id_prestamo":5,"equipo":"Laptop Dell Latitude","stock_restante":3,
                               "fecha_devolucion":"2026-10-29"}
                              """));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(201, respuesta.codigoHttp());
        assertTrue(respuesta.fueAprobado());
        assertTrue(respuesta.mensaje().contains("Número de préstamo: 5"));
        assertTrue(respuesta.mensaje().contains("Laptop Dell Latitude"));
        assertTrue(respuesta.mensaje().contains("2026-10-29"));
        assertEquals(new DetallePrestamo(5L, "Laptop Dell Latitude", "2026-10-29"), respuesta.detalle());
    }

    // ---------- Respuestas de n8n ----------

    @Test
    @DisplayName("400: junta la lista de errores de validación en el mensaje")
    void datosInvalidos400() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"estado":"RECHAZADO","mensaje":"La solicitud tiene datos invalidos",
                               "errores":["Carné inválido","Teléfono inválido. Debe tener 8 dígitos"]}
                              """));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(400, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.RECHAZADO, respuesta.estado());
        assertEquals("Carné inválido; Teléfono inválido. Debe tener 8 dígitos", respuesta.mensaje());
        assertNull(respuesta.detalle());
    }

    @Test
    @DisplayName("409 con stock parcial: informa cuántos hay disponibles")
    void sinStockSuficiente409() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"estado":"RECHAZADO","mensaje":"Stock insuficiente para la cantidad solicitada",
                               "stock_disponible":3,"codigo_equipo":"LAP-001"}
                              """));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(409, respuesta.codigoHttp());
        assertEquals("Stock insuficiente para la cantidad solicitada (disponibles: 3)", respuesta.mensaje());
        assertNull(respuesta.detalle());
    }

    @Test
    @DisplayName("409 con stock 0 o equipo inexistente: no agrega '(disponibles: 0)'")
    void sinStockCero409() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"estado":"RECHAZADO","mensaje":"El equipo no existe o esta inactivo",
                               "stock_disponible":0,"codigo_equipo":"XYZ-999"}
                              """));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals("El equipo no existe o esta inactivo", respuesta.mensaje());
    }

    @Test
    @DisplayName("409 sin detalles: deja el mensaje de n8n tal cual")
    void rechazoSinDetalles() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"estado\":\"RECHAZADO\",\"mensaje\":\"Equipo no disponible\",\"errores\":[]}"));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals("Equipo no disponible", respuesta.mensaje());
    }

    @Test
    @DisplayName("201 sin mensaje: arma el mensaje con el número de préstamo")
    void aprobadoSinMensaje() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"estado":"APROBADO","id_prestamo":7,"equipo":"Kit Arduino UNO",
                               "fecha_devolucion":"2026-10-30"}
                              """));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertTrue(respuesta.fueAprobado());
        assertTrue(respuesta.mensaje().contains("Número de préstamo: 7"));
        assertEquals(7L, respuesta.detalle().numeroPrestamo());
    }

    @Test
    @DisplayName("403: clave incorrecta se informa como acceso denegado")
    void claveIncorrecta403() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Authorization data is wrong!"));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(403, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.RECHAZADO, respuesta.estado());
        assertTrue(respuesta.mensaje().startsWith("Acceso denegado"));
    }

    @Test
    @DisplayName("500: un código inesperado se convierte en error 503")
    void codigoInesperado() throws Exception {
        n8nSimulado.expect(requestTo(URL)).andRespond(withServerError());

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(503, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.ERROR, respuesta.estado());
        assertTrue(respuesta.mensaje().contains("código 500"));
    }

    @Test
    @DisplayName("Respuesta sin campo estado: se trata como error")
    void respuestaSinEstado() throws Exception {
        n8nSimulado.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"mensaje\":\"incompleta\"}"));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(503, respuesta.codigoHttp());
        assertTrue(respuesta.mensaje().contains("sin datos"));
    }

    @Test
    @DisplayName("Respuesta vacía: se trata como error")
    void respuestaVacia() throws Exception {
        n8nSimulado.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CREATED));

        RespuestaPrestamo respuesta = gateway.enviar(solicitud());

        assertEquals(503, respuesta.codigoHttp());
    }

    // ---------- Fallos de conexión ----------

    @Test
    @DisplayName("Si n8n está apagado, lanza PrestamoGatewayException con la causa")
    void n8nApagado() {
        n8nSimulado.expect(requestTo(URL)).andRespond(withException(new IOException("Connection refused")));

        PrestamoGatewayException e =
                assertThrows(PrestamoGatewayException.class, () -> gateway.enviar(solicitud()));

        assertTrue(e.getMessage().contains(URL));
        assertInstanceOf(RestClientException.class, e.getCause());
    }

    // ---------- Configuración obligatoria ----------

    @Test
    @DisplayName("Exige el constructor de RestClient")
    void requiereBuilder() {
        assertThrows(NullPointerException.class, () -> new N8nPrestamoGateway(null, URL, CLAVE));
    }

    @Test
    @DisplayName("Exige la URL del webhook")
    void requiereUrl() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new N8nPrestamoGateway(RestClient.builder(), "  ", CLAVE));
        assertTrue(e.getMessage().contains("n8n.webhook-url"));
    }

    @Test
    @DisplayName("Exige la clave N8N_API_KEY")
    void requiereClave() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new N8nPrestamoGateway(RestClient.builder(), URL, null));
        assertTrue(e.getMessage().contains("N8N_API_KEY"));
    }
}