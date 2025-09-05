# Multi-stage build for Spring Boot backend

# Stage 1: Build Stage
FROM gradle:8.5-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY services/focushive-backend/build.gradle.kts ./
COPY services/focushive-backend/settings.gradle.kts ./
COPY services/focushive-backend/gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon

# Copy source code
COPY services/focushive-backend/src ./src

# Build application
RUN gradle bootJar --no-daemon

# Stage 2: Runtime Stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/temp && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# JVM options for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]