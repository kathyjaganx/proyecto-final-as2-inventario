package gt.umg.inventario.prestamos;

import java.util.List;

/**
 * Resultado de validar una solicitud de préstamo.
 * Equivale a la salida { valido, errores } del nodo "Validar datos" del WF1.
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
public record ResultadoValidacion(List<String> errores) {

    public ResultadoValidacion {
        // Copia inmutable: nadie puede modificar la lista desde afuera
        errores = errores == null ? List.of() : List.copyOf(errores);
    }

    public boolean esValido() {
        return errores.isEmpty();
    }
}