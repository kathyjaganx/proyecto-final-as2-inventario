package gt.umg.inventario.prestamos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del GeneradorComprobantePdf.
 * Se genera un PDF real, se vuelve a abrir con PDFBox y se lee su texto.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@DisplayName("GeneradorComprobantePdf")
class GeneradorComprobantePdfTest {

    private final GeneradorComprobantePdf generador = new GeneradorComprobantePdf();

    private ComprobantePrestamo comprobante() {
        return new ComprobantePrestamo(6, "4090-23-5555", "Begoña Pérez Tzul", "ARD-001", "Kit Arduino UNO",
                1, "2026-10-27", LocalDateTime.of(2026, 10, 22, 15, 30));
    }

    private String leerTexto(byte[] pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertEquals(1, documento.getNumberOfPages());
            return new PDFTextStripper().getText(documento);
        }
    }

    @Test
    @DisplayName("Genera un archivo PDF válido de una página")
    void generaPdfValido() throws Exception {
        byte[] pdf = generador.generar(comprobante());

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
        leerTexto(pdf);
    }

    @Test
    @DisplayName("El PDF contiene todos los datos del préstamo, con tildes y ñ")
    void contieneLosDatos() throws Exception {
        String texto = leerTexto(generador.generar(comprobante()));

        assertTrue(texto.contains("Comprobante de préstamo de equipo"));
        assertTrue(texto.contains("No. 6"));
        assertTrue(texto.contains("4090-23-5555"));
        assertTrue(texto.contains("Begoña Pérez Tzul"));
        assertTrue(texto.contains("ARD-001"));
        assertTrue(texto.contains("Kit Arduino UNO"));
        assertTrue(texto.contains("27/10/2026"));
        assertTrue(texto.contains("22/10/2026 15:30"));
        assertTrue(texto.contains("Recibido por (estudiante)"));
    }

    @Test
    @DisplayName("Los datos vacíos se imprimen como '-' sin fallar")
    void datosVacios() throws Exception {
        ComprobantePrestamo incompleto =
                new ComprobantePrestamo(7, null, " ", "LAP-001", null, null, null, null);

        String texto = leerTexto(generador.generar(incompleto));

        assertTrue(texto.contains("No. 7"));
        assertTrue(texto.contains("-"));
    }

    @Test
    @DisplayName("Exige un comprobante")
    void requiereComprobante() {
        assertThrows(NullPointerException.class, () -> generador.generar(null));
    }

    @Test
    @DisplayName("Formatea la fecha de AAAA-MM-DD a DD/MM/AAAA, y deja igual lo que no es fecha")
    void formateaFecha() {
        assertEquals("27/10/2026", GeneradorComprobantePdf.formatearFecha("2026-10-27"));
        assertEquals("pronto", GeneradorComprobantePdf.formatearFecha("pronto"));
        assertNull(GeneradorComprobantePdf.formatearFecha(null));
    }

    @Test
    @DisplayName("El nombre del archivo incluye el número de préstamo")
    void nombreArchivo() {
        assertEquals("comprobante_prestamo_6.pdf", comprobante().nombreArchivo());
    }
}