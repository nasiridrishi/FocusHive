# Build stage
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /app

# Copy dependency management files first for better caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# Copy JAR for Spring Boot application (find the non-plain JAR)
RUN find build/libs -name "*SNAPSHOT.jar" -not -name "*plain.jar" -exec cp {} app.jar \;

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# Install curl for health checks and dumb-init for proper signal handling
RUN apk add --no-cache curl dumb-init

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring && \
    mkdir -p /app/logs && \
    chown -R spring:spring /app

# Copy the application JAR
COPY --from=build --chown=spring:spring /app/app.jar ./

# Switch to non-root user
USER spring:spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Use dumb-init for proper signal handling and optimized JVM flags
ENTRYPOINT ["dumb-init", "--", "java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.backgroundpreinitializer.ignore=true", \
    "-jar", "app.jar"]