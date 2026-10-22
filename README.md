# Control de inventario y préstamos de laboratorio

Proyecto Final — Análisis de Sistemas II (código 037)
Universidad Mariano Gálvez de Guatemala
Docente: Ing. Edgar Raúl Molina Rey

**Autora:** Irma Yaritza Katiuxca Jagan Xol — Carné 4090-23-9748
**Modalidad:** Individual

---

## Descripción

Sistema que automatiza, en un entorno 100 % local, el control de préstamos de equipo de un laboratorio. Usa **n8n** para los workflows, **PostgreSQL** para la persistencia, **Docker** para el despliegue y una **aplicación web en Spring Boot** como interfaz para los usuarios.

El sistema recibe solicitudes de préstamo, valida los datos, verifica el stock disponible, registra el préstamo, descuenta el inventario y genera un reporte diario de préstamos vencidos. Todas las operaciones quedan registradas en una bitácora de auditoría, y los errores se capturan en una tabla aparte.

## Arquitectura

| Componente | Tecnología | Puerto |
|---|---|---|
| Aplicación web | Spring Boot 4.1 + Thymeleaf (`app-prestamos`) | 8080 |
| Motor de workflows | n8n (contenedor `inventario_n8n`) | 5678 |
| Base de datos | PostgreSQL 16 (contenedor `inventario_db`) | 5432 |
| Reportes | Carpeta `reportes/` compartida con n8n | — |

Flujo principal: **navegador → aplicación web → webhook de n8n (WF1 → WF2) → PostgreSQL → respuesta**.

### Workflows

| Workflow | Tipo | Función |
|---|---|---|
| **WF1 – Ingesta de préstamos** | Ingesta | Webhook protegido que recibe y valida la solicitud. Rechaza datos inválidos (400) o llama al WF2. |
| **WF2 – Procesamiento de préstamos** | Procesamiento | Verifica stock, registra el préstamo y descuenta el inventario en una sola operación atómica. Responde 201 (aprobado) o 409 (sin stock). |
| **WF3 – Salida de reportes** | Salida | Todos los días a las 7:00 a. m. marca los préstamos vencidos y genera un reporte en la auditoría y en un archivo CSV. |
| **WF4 – Manejo de errores** | Errores | Error Trigger conectado a WF1, WF2 y WF3. Registra cada fallo en la tabla `errores`. |

### Modelo de datos

| Tabla | Contenido |
|---|---|
| `equipos` | Inventario del laboratorio (stock total y disponible) |
| `prestamos` | Préstamos registrados y su estado (ACTIVO, DEVUELTO, VENCIDO) |
| `auditoria` | Bitácora de todas las operaciones y su resultado |
| `errores` | Fallos capturados por el WF4 |

## Requisitos

- Windows 10/11, macOS o Linux
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Git](https://git-scm.com/)
- Para la aplicación web: **Java 21 o superior** y **Maven 3.9 o superior**

## Instalación del entorno n8n

1. Clonar el repositorio:

```bash
   git clone https://github.com/kathyjaganx/proyecto-final-as2-inventario.git
   cd proyecto-final-as2-inventario
```

2. Crear el archivo de credenciales a partir de la plantilla:

```bash
   cp .env.example .env
```

   Editar `.env` y poner un usuario y una contraseña para PostgreSQL, y una clave aleatoria de 32 caracteres en `N8N_ENCRYPTION_KEY`.

3. Levantar el entorno:

```bash
   docker compose up -d
```

   La primera vez, PostgreSQL ejecuta `db/init.sql`, que crea las tablas y carga 5 equipos de prueba.

4. Entrar a **http://localhost:5678** y crear la cuenta de administrador de n8n.

## Configuración de n8n

1. **Crear las credenciales** en n8n (+ → Credential):
   - **Postgres Inventario** (tipo Postgres): host `postgres`, base de datos, usuario y contraseña del `.env`, puerto `5432`, SSL `Disable`.
   - **API Key Prestamos** (tipo Header Auth): nombre `X-API-Key` y como valor una clave aleatoria.

2. **Importar los workflows** desde la carpeta `workflows/`, en este orden: `WF04_Errores.json`, `WF02_Procesamiento.json`, `WF01_Ingesta.json`, `WF03_Salida.json`. Para cada uno: + → Workflow → (...) → Import from File.

3. En cada nodo de Postgres y en el Webhook, seleccionar la credencial correspondiente.

4. En WF1, WF2 y WF3: (...) → Settings → Error Workflow → **WF4 - Manejo de errores**.

5. En el WF1, revisar que el nodo **Procesar en WF2** apunte al WF2.

6. Publicar los cuatro workflows.

## Aplicación web (`app-prestamos`)

Interfaz web para registrar préstamos sin usar la terminal. Envía cada solicitud al webhook del WF1 y muestra el resultado: **verde** (aprobado), **naranja** (rechazado) o **rojo** (error de comunicación).

### Componentes

| Clase | Función |
|---|---|
| `PrestamoController` | Atiende el formulario en `/prestamos` |
| `ServicioPrestamos` | Valida la solicitud y solo si es válida la envía a n8n |
| `ValidadorPrestamo` | Las mismas 5 reglas de validación del WF1 |
| `N8nPrestamoGateway` | Cliente HTTP que llama al webhook con la cabecera `X-API-Key` |
| `ConfiguracionPrestamos` | Conecta las piezas (inyección de dependencias) |
| `prestamo.html` | Página Thymeleaf con el formulario y el resultado |

### Configuración

La aplicación lee su configuración de variables de entorno. **La clave nunca se escribe en el código**: si no se configura `N8N_API_KEY`, la aplicación no arranca e indica qué falta.

| Variable | Obligatoria | Valor |
|---|---|---|
| `N8N_API_KEY` | Sí | La misma clave de la credencial **API Key Prestamos** de n8n |
| `N8N_WEBHOOK_URL` | No | Por defecto `http://localhost:5678/webhook/prestamos` |

### Ejecución

Con el entorno de n8n encendido y los workflows publicados (PowerShell):

```powershell
cd app-prestamos
$env:N8N_API_KEY = "CLAVE_DEL_WEBHOOK"
mvn spring-boot:run
```

Abrir **http://localhost:8080**. Para detenerla: **Ctrl + C**.

También se puede empaquetar y ejecutar como un solo archivo:

```powershell
mvn verify
java -jar target\app-prestamos-0.2.0.jar
```

## Uso del webhook (sin la aplicación web)

Enviar una solicitud de préstamo directamente (PowerShell):

```powershell
$env:API_KEY = "CLAVE_DEL_WEBHOOK"
curl.exe -s -X POST "http://localhost:5678/webhook/prestamos" -H "Content-Type: application/json" -H "X-API-Key: $env:API_KEY" --data-binary "@pruebas/prestamo_valido.json"
```

Formato de la solicitud:

```json
{
  "carne": "4090-23-1234",
  "nombre_estudiante": "Ana Lucia Pop",
  "codigo_equipo": "PRO-001",
  "cantidad": 1,
  "fecha_devolucion": "2026-10-28"
}
```

Respuestas posibles:

| Código | Estado | Cuándo |
|---|---|---|
| 201 | APROBADO | El préstamo se registró y el stock se descontó |
| 400 | RECHAZADO | Los datos no cumplen las validaciones |
| 409 | RECHAZADO | El equipo no existe o no hay stock suficiente |
| 403 | — | Falta la clave `X-API-Key` o es incorrecta |

## Pruebas

### Pruebas del sistema (n8n)

| Caso | Archivo | Resultado esperado |
|---|---|---|
| Datos inválidos | `pruebas/prestamo_invalido.json` | 400 con la lista de 5 errores |
| Préstamo válido | `pruebas/prestamo_valido.json` o `prestamo_valido_laptop.json` | 201 con el número de préstamo |
| Sin stock | Repetir el préstamo válido hasta agotar el stock | 409 con el stock disponible |
| Acceso sin clave | Enviar sin la cabecera `X-API-Key` | 403 |
| Préstamo vencido | `pruebas/crear_vencido.sql` y ejecutar el WF3 | El préstamo pasa a VENCIDO y se genera el CSV |
| Error simulado | Renombrar temporalmente la tabla `prestamos` | El WF4 registra el error en WF2 y en WF1 |

Para crear el préstamo vencido de prueba:

```powershell
Get-Content pruebas\crear_vencido.sql | docker exec -i inventario_db psql -U USUARIO -d inventario_lab
```

### Pruebas unitarias de la aplicación web (JUnit, Mockito, JaCoCo)

Las pruebas **no necesitan Docker ni n8n**: las dependencias externas se simulan.

```powershell
cd app-prestamos
mvn verify
```

`mvn verify` ejecuta las pruebas, genera el reporte de cobertura y **falla si la cobertura de líneas baja del 80 %**.

| Clase de prueba | Pruebas | Herramientas | Qué verifica |
|---|---|---|---|
| `ValidadorPrestamoTest` | 48 | JUnit (parametrizadas) | Las 5 reglas, con valores válidos, inválidos y límite |
| `ServicioPrestamosTest` | 10 | JUnit + Mockito | Que los datos inválidos nunca lleguen a n8n y el manejo de fallos |
| `N8nPrestamoGatewayTest` | 14 | MockRestServiceServer | La petición HTTP exacta y la interpretación de cada respuesta de n8n |
| `PrestamoControllerTest` | 9 | MockMvc + @MockitoBean | Navegación, formulario, colores del resultado y conservación de datos |
| `AppPrestamosApplicationTest` | 1 | @SpringBootTest | Que la aplicación completa arranca y conecta sus piezas |
| **Total** | **82** | | |

**Cobertura (JaCoCo):** 99 % de instrucciones y 98 % de ramas.

Para ver el reporte de cobertura:

```powershell
start target\site\jacoco\index.html
```

## Seguridad

- **Credenciales en variables de entorno:** ninguna contraseña está escrita en el código. El archivo `.env` está excluido del repositorio con `.gitignore`, y la aplicación web lee la clave de `N8N_API_KEY`.
- **Control de acceso:** el webhook exige la cabecera `X-API-Key` (Header Auth).
- **Validación de entradas en dos capas:** la aplicación web y el WF1 aplican las mismas reglas. Los datos inválidos se rechazan antes de llegar a la base de datos.
- **Protección contra inyección SQL:** todas las consultas con datos del usuario usan parámetros o el modo Insert de n8n.
- **Consistencia de datos:** restricciones `CHECK` en la base de datos y operación atómica para registrar el préstamo y descontar el stock.
- **Acceso a archivos restringido:** n8n solo puede escribir en la carpeta `reportes/` (`N8N_RESTRICT_FILE_ACCESS_TO`).
- **Credenciales cifradas en n8n** con `N8N_ENCRYPTION_KEY`.
- **Tiempo máximo de espera:** la aplicación web corta la conexión con n8n a los 10 segundos y muestra un mensaje claro.

## Estructura del repositorio

```
├── docker-compose.yml   Definición de los contenedores
├── .env.example         Plantilla de variables de entorno
├── db/init.sql          Creación de tablas y datos iniciales
├── workflows/           Workflows exportados de n8n (WF01 a WF04)
├── pruebas/             Casos de prueba del sistema
├── reportes/            Reportes CSV generados por el WF3
├── docs/                Documento técnico y diagramas
└── app-prestamos/       Aplicación web Spring Boot
    ├── pom.xml
    └── src/
        ├── main/java/       Código de la aplicación
        ├── main/resources/  Configuración y plantilla HTML
        └── test/java/       Pruebas unitarias
```

## Comandos útiles

| Acción | Comando |
|---|---|
| Encender el entorno | `docker compose start` |
| Apagar sin borrar datos | `docker compose stop` |
| Ver estado | `docker ps` |
| Reiniciar la base desde cero (borra datos) | `docker compose down -v` y luego `docker compose up -d` |
| Arrancar la aplicación web | `cd app-prestamos` y `mvn spring-boot:run` |
| Ejecutar pruebas y cobertura | `cd app-prestamos` y `mvn verify` |