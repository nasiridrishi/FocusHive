# FocusHive Identity Service - Production-Ready Dockerfile
# Multi-stage build for optimal size and security

# Stage 1: Build stage
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /app

# Copy Gradle configuration files for dependency caching
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
COPY gradlew* ./

# Download dependencies (this layer will be cached)
RUN chmod +x ./gradlew 2>/dev/null || true && \
    ./gradlew dependencies --no-daemon --parallel

# Copy source code
COPY src ./src

# Build the application JAR (skip tests for faster builds in Docker)
RUN ./gradlew clean bootJar -x test --no-daemon --parallel

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install necessary packages for production
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    tini && \
    rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

# Create non-root user for security
RUN addgroup -g 1001 -S focushive && \
    adduser -S focushive -u 1001 -G focushive && \
    mkdir -p /app/logs /app/tmp && \
    chown -R focushive:focushive /app

# Copy the JAR file from build stage
COPY --from=build --chown=focushive:focushive /app/build/libs/identity-service.jar app.jar

# Switch to non-root user
USER focushive:focushive

# Expose application port
EXPOSE 8081

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Dspring.backgroundpreinitializer.ignore=true"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/v1/health || exit 1

# Use tini for proper signal handling
ENTRYPOINT ["/sbin/tini", "--"]

# Start the application with explicit JVM options
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=50.0", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+UseStringDeduplication", \
     "-XX:+OptimizeStringConcat", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-Djava.awt.headless=true", \
     "-Dfile.encoding=UTF-8", \
     "-Duser.timezone=UTC", \
     "-Dspring.backgroundpreinitializer.ignore=true", \
     "-jar", \
     "app.jar"]