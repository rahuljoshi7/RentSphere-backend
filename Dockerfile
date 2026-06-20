# ── Stage 1: Build with Maven ─────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first — Docker caches this layer separately
# so dependencies are not re-downloaded on every code change
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build the jar
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Stage 2: Minimal runtime image ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built jar from stage 1
COPY --from=builder /app/target/rentsphere-backend-1.0.0.jar app.jar

# Render assigns PORT dynamically — default 9090 matches your application.yml
EXPOSE 9090

ENTRYPOINT ["java", \
  "-Xmx400m", \
  "-Xms200m", \
  "-Dspring.profiles.active=prod", \
  "-Dserver.port=${PORT:-9090}", \
  "-jar", \
  "app.jar"]
