package gt.umg.inventario.prestamos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación real del PrestamoGateway: envía la solicitud al webhook
 * del WF1 de n8n por HTTP, con la clave X-API-Key, y traduce la respuesta.
 *
 * Códigos que puede devolver n8n:
 *   201 préstamo aprobado · 400 datos inválidos · 409 sin stock · 403 clave incorrecta
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public class N8nPrestamoGateway implements PrestamoGateway {

    public static final String CABECERA_CLAVE = "X-API-Key";

    private final RestClient restClient;
    private final String url;
    private final String apiKey;

    public N8nPrestamoGateway(RestClient.Builder builder, String url, String apiKey) {
        Objects.requireNonNull(builder, "El constructor de RestClient es obligatorio");
        this.url = requerido(url, "Falta configurar la URL del webhook de n8n (n8n.webhook-url)");
        this.apiKey = requerido(apiKey, "Falta configurar la clave de acceso (variable de entorno N8N_API_KEY)");
        this.restClient = builder.build();
    }

    @Override
    public RespuestaPrestamo enviar(SolicitudPrestamo solicitud) throws PrestamoGatewayException {
        // Mismos nombres de campo que espera el webhook del WF1
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("carne", solicitud.carne());
        cuerpo.put("nombre_estudiante", solicitud.nombreEstudiante());
        cuerpo.put("telefono", solicitud.telefono());
        cuerpo.put("codigo_equipo", solicitud.codigoEquipo());
        cuerpo.put("cantidad", solicitud.cantidad());
        cuerpo.put("fecha_devolucion", solicitud.fechaDevolucion());

        try {
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(CABECERA_CLAVE, apiKey)
                    .body(cuerpo)
                    .exchange((peticion, respuesta) -> {
                        int codigo = respuesta.getStatusCode().value();

                        if (codigo == 403) {
                            return RespuestaPrestamo.rechazada(403,
                                    "Acceso denegado: la clave de acceso al sistema no es válida");
                        }
                        if (codigo != 201 && codigo != 400 && codigo != 409) {
                            return RespuestaPrestamo.error(
                                    "Respuesta inesperada del sistema de préstamos (código " + codigo + ")");
                        }
                        return convertir(codigo, respuesta.bodyTo(RespuestaN8n.class));
                    });
        } catch (RestClientException e) {
            throw new PrestamoGatewayException("no se pudo conectar con n8n en " + url, e);
        }
    }

    /** Traduce el JSON de n8n a una RespuestaPrestamo con un mensaje claro para el usuario. */
    private static RespuestaPrestamo convertir(int codigo, RespuestaN8n datos) {
        if (datos == null || datos.estado() == null) {
            return RespuestaPrestamo.error(
                    "El sistema de préstamos respondió sin datos (código " + codigo + ")");
        }

        String mensaje = datos.mensaje() == null ? "" : datos.mensaje();
        DetallePrestamo detalle = null;

        if (datos.errores() != null && !datos.errores().isEmpty()) {
            mensaje = String.join("; ", datos.errores());
        } else if (datos.idPrestamo() != null) {
            mensaje = mensaje + ". Número de préstamo: " + datos.idPrestamo()
                    + ", equipo: " + datos.equipo()
                    + ", devolver a más tardar el " + datos.fechaDevolucion();
            detalle = new DetallePrestamo(datos.idPrestamo(), datos.equipo(), datos.fechaDevolucion());
        } else if (datos.stockDisponible() != null && datos.stockDisponible() > 0) {
            mensaje = mensaje + " (disponibles: " + datos.stockDisponible() + ")";
        }

        return new RespuestaPrestamo(codigo, datos.estado(), mensaje, detalle);
    }

    private static String requerido(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }
        return valor.trim();
    }

    /** Forma del JSON que responde n8n (Responder resultado / Responder rechazo del WF1). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaN8n(
            String estado,
            String mensaje,
            List<String> errores,
            @JsonProperty("id_prestamo") Long idPrestamo,
            String equipo,
            @JsonProperty("fecha_devolucion") String fechaDevolucion,
            @JsonProperty("stock_disponible") Integer stockDisponible
    ) {
    }
}