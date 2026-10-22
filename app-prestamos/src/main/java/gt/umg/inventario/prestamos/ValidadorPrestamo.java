package gt.umg.inventario.prestamos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Valida una solicitud de préstamo con las mismas reglas
 * del nodo "Validar datos" del WF1 - Ingesta de prestamos.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class ValidadorPrestamo {

    public static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");
    public static final int CANTIDAD_MINIMA = 1;
    public static final int CANTIDAD_MAXIMA = 10;
    public static final int DIAS_MAXIMOS_PRESTAMO = 30;

    private static final Pattern PATRON_CARNE = Pattern.compile("^\\d{4}-\\d{2}-\\d{3,6}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]{3,100}$");
    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Z]{3}-\\d{3}$");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final Clock reloj;

    /** Constructor normal: usa la fecha real de Guatemala. */
    public ValidadorPrestamo() {
        this(Clock.system(ZONA_GUATEMALA));
    }

    /** Constructor para pruebas: permite fijar "hoy" en una fecha conocida. */
    public ValidadorPrestamo(Clock reloj) {
        this.reloj = reloj;
    }

    public ResultadoValidacion validar(SolicitudPrestamo solicitud) {
        if (solicitud == null) {
            return new ResultadoValidacion(List.of("La solicitud no puede estar vacía"));
        }

        SolicitudPrestamo datos = solicitud.normalizada();
        List<String> errores = new ArrayList<>();

        // 1. Carné: formato 0000-00-0000
        if (!PATRON_CARNE.matcher(datos.carne()).matches()) {
            errores.add("Carné inválido. Formato esperado: 0000-00-0000");
        }

        // 2. Nombre: solo letras y espacios, de 3 a 100 caracteres
        if (!PATRON_NOMBRE.matcher(datos.nombreEstudiante()).matches()) {
            errores.add("Nombre inválido. Solo letras y espacios (3 a 100 caracteres)");
        }

        // 3. Código de equipo: formato AAA-000
        if (!PATRON_CODIGO.matcher(datos.codigoEquipo()).matches()) {
            errores.add("Código de equipo inválido. Formato esperado: AAA-000");
        }

        // 4. Cantidad: número entero entre 1 y 10
        Integer cantidad = datos.cantidad();
        if (cantidad == null || cantidad < CANTIDAD_MINIMA || cantidad > CANTIDAD_MAXIMA) {
            errores.add("Cantidad inválida. Debe ser un número entero entre 1 y 10");
        }

        // 5. Fecha de devolución: fecha real, desde hoy hasta 30 días
        validarFecha(datos.fechaDevolucion(), errores);

        return new ResultadoValidacion(errores);
    }

    private void validarFecha(String texto, List<String> errores) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            errores.add("Fecha de devolución inválida. Formato esperado: AAAA-MM-DD");
            return;
        }

        LocalDate hoy = LocalDate.now(reloj);
        LocalDate limite = hoy.plusDays(DIAS_MAXIMOS_PRESTAMO);

        if (fecha.isBefore(hoy) || fecha.isAfter(limite)) {
            errores.add("La fecha de devolución debe estar entre " + hoy + " y " + limite);
        }
    }
}