package gt.umg.inventario.prestamos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Pruebas del PrestamoController con MockMvc.
 * Se simula el navegador y el ServicioPrestamos: no hace falta servidor ni n8n.
 * El generador de PDF es el real, para comprobar el archivo que se descarga.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@WebMvcTest(PrestamoController.class)
@Import(GeneradorComprobantePdf.class)
@TestPropertySource(properties = "n8n.api-key=clave-de-prueba")
@DisplayName("PrestamoController")
class PrestamoControllerTest {

    @Autowired
    private MockMvc navegador;

    @MockitoBean
    private ServicioPrestamos servicio;

    /** Simula enviar el formulario con los datos de la prueba aprobada del navegador. */
    private MockHttpServletRequestBuilder enviarFormulario(String codigoEquipo, String cantidad) {
        return post("/prestamos")
                .param("carne", "4090-23-5555")
                .param("nombreEstudiante", "Pedro Tzul")
                .param("codigoEquipo", codigoEquipo)
                .param("cantidad", cantidad)
                .param("fechaDevolucion", "2026-10-27");
    }

    /** Respuesta aprobada como la devuelve el gateway real: con el detalle del préstamo. */
    private RespuestaPrestamo aprobadaConDetalle() {
        return new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO,
                "Prestamo registrado correctamente. Número de préstamo: 6, equipo: Kit Arduino UNO",
                new DetallePrestamo(6L, "Kit Arduino UNO", "2026-10-27"));
    }

    // ---------- Navegación ----------

    @Test
    @DisplayName("La dirección principal redirige al formulario")
    void inicioRedirigeAlFormulario() throws Exception {
        navegador.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestamos"));
    }

    @Test
    @DisplayName("GET /prestamos muestra el formulario vacío y sin resultado")
    void muestraFormularioVacio() throws Exception {
        navegador.perform(get("/prestamos"))
                .andExpect(status().isOk())
                .andExpect(view().name("prestamo"))
                .andExpect(model().attribute("formulario", hasProperty("carne", nullValue())))
                .andExpect(model().attributeDoesNotExist("respuesta"))
                .andExpect(content().string(containsString("Préstamo de equipo de laboratorio")))
                .andExpect(content().string(not(containsString("Préstamo aprobado"))));

        verifyNoInteractions(servicio);
    }

    // ---------- Envío del formulario ----------

    @Test
    @DisplayName("Aprobado: muestra el cuadro verde y limpia el formulario")
    void prestamoAprobado() throws Exception {
        RespuestaPrestamo aprobada = new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO,
                "Prestamo registrado correctamente. Número de préstamo: 6, equipo: Kit Arduino UNO");
        when(servicio.solicitar(any())).thenReturn(aprobada);

        navegador.perform(enviarFormulario("ARD-001", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("prestamo"))
                .andExpect(model().attribute("respuesta", aprobada))
                .andExpect(model().attribute("formulario", hasProperty("carne", nullValue())))
                .andExpect(content().string(containsString("Préstamo aprobado")))
                .andExpect(content().string(containsString("Número de préstamo: 6")));
    }

    @Test
    @DisplayName("Envía al servicio los datos del formulario convertidos en SolicitudPrestamo")
    void convierteFormularioEnSolicitud() throws Exception {
        when(servicio.solicitar(any())).thenReturn(new RespuestaPrestamo(201, RespuestaPrestamo.APROBADO, "ok"));

        navegador.perform(enviarFormulario("ARD-001", "2"));

        ArgumentCaptor<SolicitudPrestamo> captor = ArgumentCaptor.forClass(SolicitudPrestamo.class);
        verify(servicio).solicitar(captor.capture());
        SolicitudPrestamo enviada = captor.getValue();
        assertEquals("4090-23-5555", enviada.carne());
        assertEquals("Pedro Tzul", enviada.nombreEstudiante());
        assertEquals("ARD-001", enviada.codigoEquipo());
        assertEquals(2, enviada.cantidad());
        assertEquals("2026-10-27", enviada.fechaDevolucion());
    }

    @Test
    @DisplayName("Rechazado: muestra el cuadro naranja, conserva los datos y no ofrece comprobante")
    void prestamoRechazadoConservaDatos() throws Exception {
        when(servicio.solicitar(any())).thenReturn(
                RespuestaPrestamo.rechazada(409, "Stock insuficiente para la cantidad solicitada"));

        navegador.perform(enviarFormulario("PRO-001", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("formulario", hasProperty("carne", is("4090-23-5555"))))
                .andExpect(model().attribute("formulario", hasProperty("codigoEquipo", is("PRO-001"))))
                .andExpect(model().attributeDoesNotExist("comprobanteDisponible"))
                .andExpect(request().sessionAttribute(PrestamoController.SESION_COMPROBANTE, nullValue()))
                .andExpect(content().string(containsString("Solicitud rechazada")))
                .andExpect(content().string(containsString("Stock insuficiente")))
                .andExpect(content().string(not(containsString("Descargar comprobante"))));
    }

    @Test
    @DisplayName("Varios errores separados por ';' se muestran cada uno en su línea")
    void variosErroresEnLista() throws Exception {
        when(servicio.solicitar(any())).thenReturn(
                RespuestaPrestamo.rechazada(400, "Carné inválido; Nombre inválido"));

        navegador.perform(enviarFormulario("ARD-001", "1"))
                .andExpect(content().string(containsString("<li>Carné inválido</li>")))
                .andExpect(content().string(containsString("<li>Nombre inválido</li>")));
    }

    @Test
    @DisplayName("Error de comunicación: muestra el cuadro rojo")
    void errorDeComunicacion() throws Exception {
        when(servicio.solicitar(any())).thenReturn(
                RespuestaPrestamo.error("No se pudo comunicar con el sistema de préstamos"));

        navegador.perform(enviarFormulario("ARD-001", "1"))
                .andExpect(content().string(containsString("No se pudo procesar")))
                .andExpect(content().string(containsString("Código de respuesta: 503")));
    }

    @Test
    @DisplayName("Una cantidad que no es número llega al servicio como null, sin error técnico")
    void cantidadNoNumerica() throws Exception {
        when(servicio.solicitar(any())).thenReturn(
                RespuestaPrestamo.rechazada(400, "Cantidad inválida. Debe ser un número entero entre 1 y 10"));

        navegador.perform(enviarFormulario("ARD-001", "abc"))
                .andExpect(status().isOk());

        ArgumentCaptor<SolicitudPrestamo> captor = ArgumentCaptor.forClass(SolicitudPrestamo.class);
        verify(servicio).solicitar(captor.capture());
        assertNull(captor.getValue().cantidad());
    }

    @Test
    @DisplayName("Una cantidad vacía llega al servicio como null")
    void cantidadVacia() throws Exception {
        when(servicio.solicitar(any())).thenReturn(RespuestaPrestamo.rechazada(400, "Cantidad inválida"));

        navegador.perform(enviarFormulario("ARD-001", "  "));

        ArgumentCaptor<SolicitudPrestamo> captor = ArgumentCaptor.forClass(SolicitudPrestamo.class);
        verify(servicio).solicitar(captor.capture());
        assertNull(captor.getValue().cantidad());
    }

    // ---------- Comprobante en PDF ----------

    @Test
    @DisplayName("Aprobado con detalle: muestra el botón y guarda el comprobante en la sesión")
    void aprobadoGuardaComprobante() throws Exception {
        when(servicio.solicitar(any())).thenReturn(aprobadaConDetalle());
        MockHttpSession sesion = new MockHttpSession();

        navegador.perform(enviarFormulario("ard-001", "1").session(sesion))
                .andExpect(model().attribute("comprobanteDisponible", true))
                .andExpect(request().sessionAttribute(PrestamoController.SESION_COMPROBANTE,
                        instanceOf(ComprobantePrestamo.class)))
                .andExpect(content().string(containsString("Descargar comprobante (PDF)")))
                .andExpect(content().string(containsString("/prestamos/comprobante")));

        ComprobantePrestamo guardado =
                (ComprobantePrestamo) sesion.getAttribute(PrestamoController.SESION_COMPROBANTE);
        assertEquals(6L, guardado.numeroPrestamo());
        assertEquals("ARD-001", guardado.codigoEquipo());
        assertEquals("Pedro Tzul", guardado.nombreEstudiante());
        assertEquals("Kit Arduino UNO", guardado.equipo());
        assertEquals("2026-10-27", guardado.fechaDevolucion());
    }

    @Test
    @DisplayName("Descarga el comprobante como archivo PDF adjunto")
    void descargaComprobantePdf() throws Exception {
        when(servicio.solicitar(any())).thenReturn(aprobadaConDetalle());
        MockHttpSession sesion = new MockHttpSession();
        navegador.perform(enviarFormulario("ARD-001", "1").session(sesion));

        byte[] pdf = navegador.perform(get("/prestamos/comprobante").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("comprobante_prestamo_6.pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    @DisplayName("Sin préstamo aprobado en la sesión, la descarga regresa al formulario")
    void descargaSinComprobanteRedirige() throws Exception {
        navegador.perform(get("/prestamos/comprobante"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/prestamos"));
    }

    @Test
    @DisplayName("Una solicitud nueva rechazada borra el comprobante anterior")
    void nuevaSolicitudBorraComprobanteAnterior() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        when(servicio.solicitar(any())).thenReturn(aprobadaConDetalle());
        navegador.perform(enviarFormulario("ARD-001", "1").session(sesion));

        when(servicio.solicitar(any())).thenReturn(RespuestaPrestamo.rechazada(409, "Stock insuficiente"));
        navegador.perform(enviarFormulario("PRO-001", "1").session(sesion));

        navegador.perform(get("/prestamos/comprobante").session(sesion))
                .andExpect(status().isSeeOther());
    }
}