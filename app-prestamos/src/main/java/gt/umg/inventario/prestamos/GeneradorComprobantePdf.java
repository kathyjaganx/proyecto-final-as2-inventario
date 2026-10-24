package gt.umg.inventario.prestamos;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Genera el comprobante de un préstamo aprobado como documento PDF (tamaño carta).
 * Usa Apache PDFBox. Las coordenadas están en puntos: la hoja mide 612 x 792
 * y el punto (0, 0) es la esquina inferior izquierda.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class GeneradorComprobantePdf {

    private static final Color AZUL = new Color(0x1F, 0x38, 0x64);
    private static final Color GRIS = new Color(0x6B, 0x72, 0x80);
    private static final Color LINEA = new Color(0xD1, 0xD5, 0xDB);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEN = 60;
    private static final float COLUMNA_VALOR = 230;

    private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** Devuelve el contenido del PDF listo para descargar. */
    public byte[] generar(ComprobantePrestamo c) {
        Objects.requireNonNull(c, "El comprobante es obligatorio");

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            PDPage pagina = new PDPage(PDRectangle.LETTER);
            documento.addPage(pagina);
            float ancho = pagina.getMediaBox().getWidth();

            try (PDPageContentStream hoja = new PDPageContentStream(documento, pagina)) {
                // ---------- Encabezado ----------
                hoja.setNonStrokingColor(AZUL);
                hoja.addRect(0, 700, ancho, 92);
                hoja.fill();
                texto(hoja, negrita, 20, Color.WHITE, MARGEN, 755, "Comprobante de préstamo de equipo");
                texto(hoja, normal, 10, Color.WHITE, MARGEN, 735,
                        "Laboratorio - Universidad Mariano Gálvez de Guatemala");
                String numero = "No. " + c.numeroPrestamo();
                texto(hoja, negrita, 20, Color.WHITE, ancho - MARGEN - ancho(negrita, 20, numero), 755, numero);

                // ---------- Datos del préstamo ----------
                float y = 650;
                texto(hoja, negrita, 12, AZUL, MARGEN, y, "Datos del préstamo");
                y -= 28;
                y = fila(hoja, y, "Número de préstamo", String.valueOf(c.numeroPrestamo()));
                y = fila(hoja, y, "Fecha de registro", c.fechaRegistro() == null ? null : c.fechaRegistro().format(FECHA_HORA));
                y = fila(hoja, y, "Carné", c.carne());
                y = fila(hoja, y, "Estudiante", c.nombreEstudiante());
                y = fila(hoja, y, "Código de equipo", c.codigoEquipo());
                y = fila(hoja, y, "Equipo", c.equipo());
                y = fila(hoja, y, "Cantidad", c.cantidad() == null ? null : String.valueOf(c.cantidad()));
                y = fila(hoja, y, "Devolver a más tardar el", formatearFecha(c.fechaDevolucion()));

                // ---------- Compromiso ----------
                y -= 20;
                texto(hoja, negrita, 12, AZUL, MARGEN, y, "Compromiso de devolución");
                y -= 22;
                texto(hoja, normal, 10, Color.BLACK, MARGEN, y,
                        "El estudiante se compromete a devolver el equipo en buen estado a más tardar en la fecha");
                y -= 15;
                texto(hoja, normal, 10, Color.BLACK, MARGEN, y,
                        "indicada. Los préstamos no devueltos a tiempo se reportan automáticamente como vencidos.");

                // ---------- Firmas ----------
                float yFirma = 190;
                linea(hoja, MARGEN, yFirma, MARGEN + 200);
                linea(hoja, ancho - MARGEN - 200, yFirma, ancho - MARGEN);
                texto(hoja, normal, 10, GRIS, MARGEN, yFirma - 15, "Entregado por (encargado del laboratorio)");
                texto(hoja, normal, 10, GRIS, ancho - MARGEN - 200, yFirma - 15, "Recibido por (estudiante)");

                // ---------- Pie ----------
                texto(hoja, normal, 8, GRIS, MARGEN, 50,
                        "Documento generado por el Sistema de control de inventario y préstamos de laboratorio.");
            }

            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el comprobante PDF", e);
        }
    }

    /** Escribe una fila "Etiqueta: valor" con una línea divisoria debajo y devuelve la siguiente posición. */
    private float fila(PDPageContentStream hoja, float y, String etiqueta, String valor) throws IOException {
        texto(hoja, negrita, 11, Color.BLACK, MARGEN, y, etiqueta);
        texto(hoja, normal, 11, Color.BLACK, COLUMNA_VALOR, y, valor);
        hoja.setStrokingColor(LINEA);
        hoja.setLineWidth(0.5f);
        hoja.moveTo(MARGEN, y - 8);
        hoja.lineTo(612 - MARGEN, y - 8);
        hoja.stroke();
        return y - 26;
    }

    private void texto(PDPageContentStream hoja, PDType1Font fuente, float tamano, Color color,
                       float x, float y, String contenido) throws IOException {
        hoja.beginText();
        hoja.setFont(fuente, tamano);
        hoja.setNonStrokingColor(color);
        hoja.newLineAtOffset(x, y);
        hoja.showText(contenido == null || contenido.isBlank() ? "-" : contenido);
        hoja.endText();
    }

    private void linea(PDPageContentStream hoja, float x1, float y, float x2) throws IOException {
        hoja.setStrokingColor(Color.BLACK);
        hoja.setLineWidth(0.8f);
        hoja.moveTo(x1, y);
        hoja.lineTo(x2, y);
        hoja.stroke();
    }

    private static float ancho(PDType1Font fuente, float tamano, String texto) throws IOException {
        return fuente.getStringWidth(texto) / 1000 * tamano;
    }

    /** Convierte 2026-10-27 en 27/10/2026; si no es una fecha válida, la deja como está. */
    static String formatearFecha(String fecha) {
        if (fecha == null) {
            return null;
        }
        try {
            return LocalDate.parse(fecha).format(FECHA);
        } catch (DateTimeParseException e) {
            return fecha;
        }
    }
}