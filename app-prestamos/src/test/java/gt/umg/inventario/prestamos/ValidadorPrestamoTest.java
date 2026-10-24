package gt.umg.inventario.prestamos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias del ValidadorPrestamo.
 * "Hoy" se fija en el 22 de octubre de 2026 para que las pruebas
 * den siempre el mismo resultado, sin importar el día en que se ejecuten.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@DisplayName("ValidadorPrestamo")
class ValidadorPrestamoTest {

    private static final String TELEFONO = "55551234";

    private ValidadorPrestamo validador;

    @BeforeEach
    void prepararValidador() {
        ZonedDateTime hoy = ZonedDateTime.of(2026, 10, 22, 12, 0, 0, 0, ValidadorPrestamo.ZONA_GUATEMALA);
        Clock relojFijo = Clock.fixed(hoy.toInstant(), ValidadorPrestamo.ZONA_GUATEMALA);
        validador = new ValidadorPrestamo(relojFijo);
    }

    /** Solicitud con todos los datos correctos; cada prueba cambia solo un campo. */
    private SolicitudPrestamo solicitudValida() {
        return new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", 1, "2026-10-29");
    }

    // ---------- Casos generales ----------

    @Test
    @DisplayName("Acepta una solicitud con todos los datos correctos")
    void aceptaSolicitudValida() {
        ResultadoValidacion resultado = validador.validar(solicitudValida());

        assertTrue(resultado.esValido());
        assertTrue(resultado.errores().isEmpty());
    }

    @Test
    @DisplayName("Rechaza una solicitud nula")
    void rechazaSolicitudNula() {
        ResultadoValidacion resultado = validador.validar(null);

        assertFalse(resultado.esValido());
        assertEquals(List.of("La solicitud no puede estar vacía"), resultado.errores());
    }

    @Test
    @DisplayName("Limpia espacios y convierte el código a mayúsculas antes de validar")
    void normalizaDatosAntesDeValidar() {
        SolicitudPrestamo conEspacios = new SolicitudPrestamo(
                "  4090-23-1234 ", " Ana Lucia Pop ", " 5555-1234 ", " lap-001 ", 1, " 2026-10-29 ");

        assertTrue(validador.validar(conEspacios).esValido());
    }

    @Test
    @DisplayName("Detecta los 6 errores a la vez")
    void detectaTodosLosErrores() {
        SolicitudPrestamo todoMal = new SolicitudPrestamo("12345", "Juan123", "123", "laptop", 0, "2020-01-01");

        ResultadoValidacion resultado = validador.validar(todoMal);

        assertFalse(resultado.esValido());
        assertEquals(6, resultado.errores().size());
    }

    @Test
    @DisplayName("La lista de errores no se puede modificar desde afuera")
    void erroresSonInmutables() {
        ResultadoValidacion resultado = validador.validar(null);

        assertThrows(UnsupportedOperationException.class, () -> resultado.errores().add("otro"));
    }

    @Test
    @DisplayName("Un resultado creado con lista nula se considera válido y sin errores")
    void resultadoConListaNula() {
        ResultadoValidacion resultado = new ResultadoValidacion(null);

        assertTrue(resultado.esValido());
        assertTrue(resultado.errores().isEmpty());
    }

    // ---------- Regla 1: carné ----------

    @ParameterizedTest(name = "carné inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "4090-23-12", "abcd-23-1234", "4090/23/1234", "4090-2-12345"})
    void rechazaCarneInvalido(String carne) {
        SolicitudPrestamo s = new SolicitudPrestamo(carne, "Ana Lucia Pop", TELEFONO, "LAP-001", 1, "2026-10-29");

        assertEquals(List.of("Carné inválido. Formato esperado: 0000-00-0000"), validador.validar(s).errores());
    }

    // ---------- Regla 2: nombre ----------

    @ParameterizedTest(name = "nombre inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"Juan123", "Al", "Ana@Pop", "Ana_Pop"})
    void rechazaNombreInvalido(String nombre) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", nombre, TELEFONO, "LAP-001", 1, "2026-10-29");

        assertEquals(List.of("Nombre inválido. Solo letras y espacios (3 a 100 caracteres)"),
                validador.validar(s).errores());
    }

    @Test
    @DisplayName("Rechaza un nombre de más de 100 caracteres")
    void rechazaNombreDemasiadoLargo() {
        String nombreLargo = "A".repeat(101);
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", nombreLargo, TELEFONO, "LAP-001", 1, "2026-10-29");

        assertFalse(validador.validar(s).esValido());
    }

    @Test
    @DisplayName("Acepta nombres con tildes y ñ")
    void aceptaNombreConTildesYEnie() {
        SolicitudPrestamo s =
                new SolicitudPrestamo("4090-23-1234", "Begoña Pérez Güiza", TELEFONO, "LAP-001", 1, "2026-10-29");

        assertTrue(validador.validar(s).esValido());
    }

    // ---------- Regla 3: teléfono ----------

    @ParameterizedTest(name = "teléfono válido: \"{0}\"")
    @ValueSource(strings = {"55551234", "5555-1234", "5555 1234", " 55551234 "})
    void aceptaTelefonoConEspaciosOGuiones(String telefono) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", telefono, "LAP-001", 1, "2026-10-29");

        assertTrue(validador.validar(s).esValido());
    }

    @ParameterizedTest(name = "teléfono inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "123456789", "5555-12a4", "abcdefgh", "+50255551234"})
    void rechazaTelefonoInvalido(String telefono) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", telefono, "LAP-001", 1, "2026-10-29");

        assertEquals(List.of("Teléfono inválido. Debe tener 8 dígitos"), validador.validar(s).errores());
    }

    // ---------- Regla 4: código de equipo ----------

    @ParameterizedTest(name = "código inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"laptop", "LA-001", "LAP001", "LAP-01", "1AP-001"})
    void rechazaCodigoInvalido(String codigo) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, codigo, 1, "2026-10-29");

        assertEquals(List.of("Código de equipo inválido. Formato esperado: AAA-000"), validador.validar(s).errores());
    }

    // ---------- Regla 5: cantidad ----------

    @ParameterizedTest(name = "cantidad válida (límite): {0}")
    @ValueSource(ints = {1, 5, 10})
    void aceptaCantidadEnRango(int cantidad) {
        SolicitudPrestamo s =
                new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", cantidad, "2026-10-29");

        assertTrue(validador.validar(s).esValido());
    }

    @ParameterizedTest(name = "cantidad inválida: {0}")
    @NullSource
    @ValueSource(ints = {0, -1, 11, 100})
    void rechazaCantidadFueraDeRango(Integer cantidad) {
        SolicitudPrestamo s =
                new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", cantidad, "2026-10-29");

        assertEquals(List.of("Cantidad inválida. Debe ser un número entero entre 1 y 10"),
                validador.validar(s).errores());
    }

    // ---------- Regla 6: fecha de devolución ----------

    @ParameterizedTest(name = "fecha válida (límite): {0}")
    @ValueSource(strings = {"2026-10-22", "2026-11-21"})
    void aceptaFechaEnLosLimites(String fecha) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", 1, fecha);

        assertTrue(validador.validar(s).esValido());
    }

    @ParameterizedTest(name = "fecha con formato inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"22/10/2026", "2026-02-30", "2026-13-01", "mañana"})
    void rechazaFechaConFormatoInvalido(String fecha) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", 1, fecha);

        assertEquals(List.of("Fecha de devolución inválida. Formato esperado: AAAA-MM-DD"),
                validador.validar(s).errores());
    }

    @ParameterizedTest(name = "fecha fuera de rango: {0}")
    @ValueSource(strings = {"2026-10-21", "2026-11-22", "2020-01-01"})
    void rechazaFechaFueraDeRango(String fecha) {
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", 1, fecha);

        assertEquals(List.of("La fecha de devolución debe estar entre 2026-10-22 y 2026-11-21"),
                validador.validar(s).errores());
    }

    @Test
    @DisplayName("El constructor sin parámetros usa la fecha real y funciona")
    void constructorPorDefecto() {
        ValidadorPrestamo validadorReal = new ValidadorPrestamo();
        String manana = java.time.LocalDate.now(ValidadorPrestamo.ZONA_GUATEMALA).plusDays(1).toString();
        SolicitudPrestamo s = new SolicitudPrestamo("4090-23-1234", "Ana Lucia Pop", TELEFONO, "LAP-001", 1, manana);

        assertTrue(validadorReal.validar(s).esValido());
    }
}