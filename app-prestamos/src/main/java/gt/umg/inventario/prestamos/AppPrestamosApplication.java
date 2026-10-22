package gt.umg.inventario.prestamos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación web de préstamos.
 * Al ejecutarla, Spring Boot levanta un servidor web en http://localhost:8080
 *
 * Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
 */
@SpringBootApplication
public class AppPrestamosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppPrestamosApplication.class, args);
    }
}