# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom first so Maven dependency layer is cached separately
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S rentsphere && adduser -S rentsphere -G rentsphere

COPY --from=builder /app/target/rentsphere-backend-1.0.0.jar app.jar

RUN chown rentsphere:rentsphere app.jar
USER rentsphere

# Render injects PORT env var; Spring reads it via server.port=${PORT:9090}
EXPOSE 9090

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
