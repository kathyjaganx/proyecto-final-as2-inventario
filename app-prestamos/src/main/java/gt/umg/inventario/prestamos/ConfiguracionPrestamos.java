package gt.umg.inventario.prestamos;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Le indica a Spring cómo crear y conectar las piezas de la aplicación.
 * Cada método @Bean crea un objeto que Spring administra y entrega
 * automáticamente a quien lo necesite (inyección de dependencias).
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@Configuration
public class ConfiguracionPrestamos {

    @Bean
    public ValidadorPrestamo validadorPrestamo() {
        return new ValidadorPrestamo();
    }

    @Bean
    public PrestamoGateway prestamoGateway(
            @Value("${n8n.webhook-url}") String url,
            @Value("${n8n.api-key}") String apiKey,
            @Value("${n8n.timeout-segundos:10}") long timeoutSegundos) {

        // Si n8n no responde en este tiempo, se corta la espera en lugar de colgar la página
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        return new N8nPrestamoGateway(RestClient.builder().requestFactory(fabrica), url, apiKey);
    }

    @Bean
    public ServicioPrestamos servicioPrestamos(ValidadorPrestamo validador, PrestamoGateway gateway) {
        return new ServicioPrestamos(validador, gateway);
    }

    @Bean
    public GeneradorComprobantePdf generadorComprobantePdf() {
        return new GeneradorComprobantePdf();
    }
}