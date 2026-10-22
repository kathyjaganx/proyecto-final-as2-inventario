package gt.umg.inventario.prestamos;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de arranque: la aplicación completa se levanta en memoria
 * y Spring conecta todas las piezas (inyección de dependencias).
 * Usa una clave de prueba: no se conecta a n8n.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@SpringBootTest(properties = "n8n.api-key=clave-de-prueba")
@DisplayName("Aplicación completa")
class AppPrestamosApplicationTest {

    @Autowired
    private PrestamoController controlador;

    @Autowired
    private ServicioPrestamos servicio;

    @Autowired
    private PrestamoGateway gateway;

    @Test
    @DisplayName("Arranca y Spring conecta controlador, servicio y gateway real de n8n")
    void arrancaYConectaLasPiezas() {
        assertNotNull(controlador);
        assertNotNull(servicio);
        assertInstanceOf(N8nPrestamoGateway.class, gateway);
    }
}