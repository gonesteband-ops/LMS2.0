# ============================================================
# Dockerfile para LMS LegalTech (Java 17 + Spring Boot 3.3)
# ============================================================

# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar dependencias primero (caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root
RUN addgroup -S lms && adduser -S lms -G lms

# Copiar JAR desde build stage
COPY --from=build /app/target/*.jar app.jar

# Copiar configuración y scripts
COPY --chown=lms:lms src/main/resources/application.yml ./config/

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseZGC"
ENV TZ=America/Santiago

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Puerto de la aplicación
EXPOSE 8080

# Usuario no-root
USER lms

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.additional-location=file:./config/application.yml"]
