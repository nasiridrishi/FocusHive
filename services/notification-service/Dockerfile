# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy dependency management files first for better caching
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
COPY gradlew* ./

# Download dependencies (cached layer)
RUN chmod +x ./gradlew 2>/dev/null || true && \
    gradle dependencies --no-daemon || ./gradlew dependencies --no-daemon

# Copy source code and resources
COPY src ./src
COPY jvm.options* ./
COPY start-optimized.sh* ./

# Build application with optimizations (skip tests and license checks)
RUN gradle clean build -x test -x license --no-daemon && \
    echo "Build completed successfully"

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# Install required packages for production
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    dumb-init && \
    rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

# Create non-root user with specific UID/GID
RUN addgroup -g 1001 -S focushive && \
    adduser -S focushive -u 1001 -G focushive && \
    mkdir -p /app/logs /app/config /app/tmp && \
    chown -R focushive:focushive /app

# Copy the JAR file from the build stage
COPY --from=build --chown=focushive:focushive /app/build/libs/notification-service.jar ./app.jar

# Copy JVM options if exists
COPY --from=build --chown=focushive:focushive /app/jvm.options* ./

# Copy startup script if exists
COPY --from=build --chown=focushive:focushive /app/start-optimized.sh* ./

# Switch to non-root user
USER focushive:focushive

# Expose application and JMX ports
EXPOSE 8083 9090

# Environment variables for production
ENV SPRING_PROFILES_ACTIVE=prod
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
    CMD curl -f http://localhost:8083/actuator/health || exit 1

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
