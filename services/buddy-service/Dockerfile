# =============================================================================
# Ultra-Optimized Multi-stage Dockerfile for Buddy Service - Production Grade
# Target: <200MB final image with <30s startup time
# =============================================================================

# Build stage with Alpine for minimal footprint
FROM gradle:8.8-jdk21-alpine AS build

# Set build arguments for maximum optimization
ARG GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.configureondemand=true"
ARG BUILD_PROFILE=production

WORKDIR /app

# Install only essential build dependencies
RUN apk add --no-cache git && \
    rm -rf /var/cache/apk/*

# Copy dependency management files first for better caching
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon --parallel && \
    ./gradlew clean --no-daemon

# Copy source code and build with maximum optimizations
COPY src ./src

# Build application with ultra-optimized settings
RUN ./gradlew build -x test --no-daemon --parallel \
    -Pprofile=${BUILD_PROFILE} \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" && \
    # Verify JAR was created
    ls -la build/libs/ && \
    # Clean up all unnecessary build artifacts to minimize transfer
    rm -rf ~/.gradle && \
    rm -rf /tmp/* && \
    find . -name "*.tmp" -delete

# =============================================================================
# Jar extraction stage - Optimize Spring Boot jar layering
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS extract

WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/buddy-service.jar buddy-service.jar

# Extract jar layers for better Docker layer caching
RUN java -Djarmode=layertools -jar buddy-service.jar extract

# =============================================================================
# Ultra-minimal production runtime stage using distroless
# =============================================================================
FROM gcr.io/distroless/java21-debian12:nonroot AS production

# Metadata labels for production tracking
LABEL maintainer="FocusHive Team" \
      version="2.0.0" \
      description="Buddy Service - Ultra-optimized Production Build" \
      org.opencontainers.image.source="https://github.com/focushive/buddy-service" \
      org.opencontainers.image.documentation="https://docs.focushive.com/buddy-service" \
      org.opencontainers.image.vendor="FocusHive" \
      org.opencontainers.image.title="Buddy Service Production"

WORKDIR /app

# Copy Spring Boot layers in optimal order for caching
COPY --from=extract --chown=nonroot:nonroot /app/dependencies/ ./
COPY --from=extract --chown=nonroot:nonroot /app/spring-boot-loader/ ./
COPY --from=extract --chown=nonroot:nonroot /app/snapshot-dependencies/ ./
COPY --from=extract --chown=nonroot:nonroot /app/application/ ./

# Copy optimized application configuration
COPY --chown=nonroot:nonroot src/main/resources/application-docker.properties ./config/

# Already using nonroot user from distroless (uid=65532)
USER nonroot:nonroot

# Expose application port
EXPOSE 8087

# Ultra-optimized JVM configuration for fast startup and low memory
ENV JVM_OPTS="-XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=70.0 \
              -XX:+UseSerialGC \
              -XX:+UseStringDeduplication \
              -XX:+UseCompressedOops \
              -XX:+UseCompressedClassPointers \
              -XX:+TieredCompilation \
              -XX:TieredStopAtLevel=1 \
              -XX:+ExitOnOutOfMemoryError \
              -Xss256k \
              -XX:MetaspaceSize=128m \
              -XX:MaxMetaspaceSize=256m \
              -Djava.security.egd=file:/dev/./urandom \
              -Djava.awt.headless=true \
              -Dspring.backgroundpreinitializer.ignore=true \
              -Dspring.jmx.enabled=false \
              -Dfile.encoding=UTF-8 \
              -Duser.timezone=UTC \
              -Djava.net.preferIPv4Stack=true \
              -Dspring.config.location=classpath:/application.properties,classpath:/application-docker.properties"

# Application configuration for ultra-fast startup
ENV SPRING_PROFILES_ACTIVE=docker \
    SPRING_MAIN_LAZY_INITIALIZATION=true \
    LOGGING_LEVEL_ROOT=WARN \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info \
    MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never

# Use optimized Spring Boot entry point with layered jar approach
ENTRYPOINT ["java", "-cp", "/app", "org.springframework.boot.loader.launch.JarLauncher"]

# =============================================================================
# Health check stage - separate for curl capability
# =============================================================================
FROM alpine:3.19 AS healthcheck

RUN apk add --no-cache curl

# =============================================================================
# Final distroless stage with health check
# =============================================================================
FROM production AS final

# Copy health check script from Alpine stage
COPY --from=healthcheck /usr/bin/curl /usr/bin/curl
# Copy architecture-specific musl loader
COPY --from=healthcheck /lib/ld-musl-*.so.1 /lib/
COPY --from=healthcheck /usr/lib/libcurl.so.4 /usr/lib/libcurl.so.4
COPY --from=healthcheck /usr/lib/libssl.so.3 /usr/lib/libssl.so.3
COPY --from=healthcheck /usr/lib/libcrypto.so.3 /usr/lib/libcrypto.so.3
COPY --from=healthcheck /lib/libz.so.1 /lib/libz.so.1

# Ultra-fast health check optimized for production
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD ["/usr/bin/curl", "-f", "http://localhost:8087/actuator/health"] || exit 1
