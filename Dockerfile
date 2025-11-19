FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Make mvnw executable
RUN chmod +x ./mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the jar file
COPY --from=builder /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p uploads && chown -R appuser:appgroup uploads

# Change to app user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ✅ CHANGED: Use JAVA_OPTS from environment variable
# If JAVA_OPTS is not set, use safe defaults for 2GB instance
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx768m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/techstore-logs/heap_dump.hprof} -jar app.jar"]