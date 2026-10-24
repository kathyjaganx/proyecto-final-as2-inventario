package gt.umg.inventario.prestamos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.time.Clock;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias del ServicioPrestamos.
 * Mockito simula a n8n (PrestamoGateway): no hace falta tener Docker encendido.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioPrestamos")
class ServicioPrestamosTest {

    @Mock
    private PrestamoGateway gateway;

    @Captor
    private ArgumentCaptor<SolicitudPrestamo> solicitudEnviada;

    private ServicioPrestamos servicio;

    @BeforeEach
    void prepararServicio() {
        ZonedDateTime hoy = ZonedDateTime.of(2026, 10, 22, 12, 0, 0, 0, ValidadorPrestamo.ZONA_GUATEMALA);
        ValidadorPrestamo validador =
                new ValidadorPrestamo(Clock.fixed(hoy.toInstant(), ValidadorPrestamo.ZONA_GUATEMALA));
        servicio = new ServicioPrestamos(validador, gateway);
    }

    private SolicitudPrestamo solicitudValida() {
        return new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", "55551234", "LAP-001", 1, "2026-10-29");
    }

    // ---------- Validación antes de contactar a n8n ----------

    @Test
    @DisplayName("Con datos inválidos responde 400 y NUNCA contacta a n8n")
    void datosInvalidosNoLlamanAN8n() {
        SolicitudPrestamo todoMal = new SolicitudPrestamo("12345", "Juan123", "123", "laptop", 0, "2020-01-01");

        RespuestaPrestamo respuesta = servicio.solicitar(todoMal);

        assertEquals(400, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.RECHAZADO, respuesta.estado());
        assertTrue(respuesta.mensaje().contains("Carné inválido"));
        assertTrue(respuesta.mensaje().contains("Teléfono inválido"));
        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Con solicitud nula responde 400 y no contacta a n8n")
    void solicitudNulaNoLlamaAN8n() {
        RespuestaPrestamo respuesta = servicio.solicitar(null);

        assertEquals(400, respuesta.codigoHttp());
        verifyNoInteractions(gateway);
    }

    // ---------- Respuestas de n8n ----------

    @Test
    @DisplayName("Con datos válidos envía la solicitud a n8n una sola vez y devuelve su respuesta 201")
    void datosValidosSeEnvianAN8n() throws Exception {
        RespuestaPrestamo aprobada =
                new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO, "Prestamo registrado correctamente");
        when(gateway.enviar(any())).thenReturn(aprobada);

        RespuestaPrestamo respuesta = servicio.solicitar(solicitudValida());

        assertSame(aprobada, respuesta);
        assertTrue(respuesta.fueAprobado());
        verify(gateway, times(1)).enviar(any());
    }

    @Test
    @DisplayName("Envía a n8n los datos ya limpios (sin espacios, código en mayúsculas y teléfono sin guion)")
    void enviaDatosNormalizados() throws Exception {
        when(gateway.enviar(any()))
                .thenReturn(new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO, "ok"));
        SolicitudPrestamo conEspacios = new SolicitudPrestamo(
                " 4090-23-1234 ", " Ana Lucia Pop ", " 5555-1234 ", " lap-001 ", 1, "2026-10-29");

        servicio.solicitar(conEspacios);

        verify(gateway).enviar(solicitudEnviada.capture());
        assertEquals("LAP-001", solicitudEnviada.getValue().codigoEquipo());
        assertEquals("4090-23-1234", solicitudEnviada.getValue().carne());
        assertEquals("Ana Lucia Pop", solicitudEnviada.getValue().nombreEstudiante());
        assertEquals("55551234", solicitudEnviada.getValue().telefono());
    }

    @Test
    @DisplayName("Si n8n responde 409 (sin stock), el servicio devuelve ese rechazo tal cual")
    void sinStockDevuelve409() throws Exception {
        when(gateway.enviar(any()))
                .thenReturn(RespuestaPrestamo.rechazada(409, "Stock insuficiente para la cantidad solicitada"));

        RespuestaPrestamo respuesta = servicio.solicitar(solicitudValida());

        assertEquals(409, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.RECHAZADO, respuesta.estado());
        assertFalse(respuesta.fueAprobado());
    }

    // ---------- Fallos de comunicación ----------

    @Test
    @DisplayName("Si n8n está apagado, devuelve 503 con un mensaje claro en lugar de fallar")
    void n8nApagadoDevuelve503() throws Exception {
        when(gateway.enviar(any())).thenThrow(
                new PrestamoGatewayException("conexión rechazada", new ConnectException("Connection refused")));

        RespuestaPrestamo respuesta = servicio.solicitar(solicitudValida());

        assertEquals(503, respuesta.codigoHttp());
        assertEquals(RespuestaPrestamo.ERROR, respuesta.estado());
        assertTrue(respuesta.mensaje().contains("conexión rechazada"));
    }

    @Test
    @DisplayName("Si n8n no devuelve nada, responde 503")
    void respuestaNulaDevuelve503() throws Exception {
        when(gateway.enviar(any())).thenReturn(null);

        RespuestaPrestamo respuesta = servicio.solicitar(solicitudValida());

        assertEquals(503, respuesta.codigoHttp());
        assertEquals("El sistema de préstamos no devolvió respuesta", respuesta.mensaje());
    }

    // ---------- Construcción del servicio ----------

    @Test
    @DisplayName("No permite crear el servicio sin validador")
    void requiereValidador() {
        NullPointerException e =
                assertThrows(NullPointerException.class, () -> new ServicioPrestamos(null, gateway));
        assertEquals("El validador es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("No permite crear el servicio sin gateway")
    void requiereGateway() {
        NullPointerException e =
                assertThrows(NullPointerException.class, () -> new ServicioPrestamos(new ValidadorPrestamo(), null));
        assertEquals("El gateway es obligatorio", e.getMessage());
    }

    // ---------- RespuestaPrestamo ----------

    @Test
    @DisplayName("fueAprobado exige código 201 y estado APROBADO a la vez")
    void fueAprobadoExigeAmbasCondiciones() {
        assertTrue(new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO, "ok").fueAprobado());
        assertFalse(new RespuestaPrestamo(201, RespuestaPrestamo.RECHAZADO, "x").fueAprobado());
        assertFalse(new RespuestaPrestamo(409, RespuestaPrestamo.APROBADO, "x").fueAprobado());
    }
}