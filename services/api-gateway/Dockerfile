# Multi-stage build for FocusHive API Gateway
FROM gradle:8.5-jdk21 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY build.gradle.kts ./
COPY gradle.properties* ./
COPY settings.gradle.kts* ./

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

# Install necessary packages and create user
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r focushive && useradd -r -g focushive focushive

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/api-gateway.jar app.jar

# Change ownership to non-root user
RUN chown -R focushive:focushive /app

# Switch to non-root user
USER focushive

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/api/health || exit 1

# Expose port
EXPOSE 8090

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100" \
    SPRING_PROFILES_ACTIVE="docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]