# =====================================================
# App Préstamos - Imagen de Docker
# Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
# =====================================================

# ---------- Etapa 1: compilación ----------
FROM maven:3.9.9-eclipse-temurin-21 AS compilacion
WORKDIR /compilacion

# Primero solo el pom.xml: así Docker reutiliza las librerías descargadas
# mientras el pom.xml no cambie, y las siguientes compilaciones son más rápidas
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Luego el código fuente, y se empaqueta el .jar.
# Las pruebas ya se ejecutaron con "mvn verify" antes de construir la imagen.
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios: si alguien vulnerara la aplicación, no tendría acceso de administrador
RUN useradd --system --uid 1001 --no-create-home appuser

COPY --from=compilacion /compilacion/target/app-prestamos-0.2.0.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]