# FocusHive API Gateway

A Spring Cloud Gateway implementation providing centralized routing, authentication, and cross-cutting concerns for the FocusHive microservices architecture.

## Overview

The FocusHive API Gateway serves as the single entry point for all client requests, providing:

- **Centralized Authentication**: JWT token validation via Identity Service integration
- **Request Routing**: Intelligent routing to 8 downstream microservices
- **Rate Limiting**: Redis-based rate limiting with user and IP-based policies
- **Circuit Breaking**: Resilience patterns with automatic failover
- **Security Headers**: CORS, CSP, and security headers management
- **Request/Response Logging**: Comprehensive request tracking with correlation IDs
- **Health Monitoring**: Health checks for the gateway and downstream services

## Architecture

### Gateway Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Client App    │───▶│  API Gateway     │───▶│  Microservices  │
│                 │    │  (Port 8090)     │    │  (8080-8087)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  Gateway Redis   │
                       │  (Port 6381)     │
                       └──────────────────┘
```

### Service Routing

| Service | Port | Gateway Path | Features |
|---------|------|--------------|----------|
| Identity Service | 8081 | `/auth/**`, `/oauth2/**` | Public + JWT validation |
| FocusHive Backend | 8080 | `/hives/**`, `/presence/**` | JWT required |
| Music Service | 8082 | `/music/**`, `/playlists/**` | JWT required |
| Notification Service | 8083 | `/notifications/**` | JWT required |
| Chat Service | 8084 | `/chat/**`, `/messages/**` | JWT required |
| Analytics Service | 8085 | `/analytics/**`, `/reports/**` | JWT required |
| Forum Service | 8086 | `/forum/**`, `/posts/**` | JWT required |
| Buddy Service | 8087 | `/buddies/**`, `/partnerships/**` | JWT required |

## Configuration

### Environment Variables

```yaml
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=gateway_redis_pass

# JWT Configuration
FOCUSHIVE_JWT_SECRET=your-256-bit-secret-key-here-make-it-secure
FOCUSHIVE_JWT_EXPIRATION=86400000

# OAuth2 Resource Server
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8081
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://localhost:8081/oauth2/jwks

# CORS Origins
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8090
```

### Rate Limiting Configuration

The gateway uses Redis-based rate limiting with different policies for different services:

- **Identity Service**: 100 requests/second, burst capacity 200
- **FocusHive Backend**: 200 requests/second, burst capacity 400
- **Chat Service**: 500 requests/second, burst capacity 1000 (high volume)
- **Other Services**: 50-100 requests/second based on expected load

### Circuit Breaker Configuration

Each downstream service has its own circuit breaker with customized settings:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      chat-service-cb:
        sliding-window-size: 20
        failure-rate-threshold: 40%
        wait-duration-in-open-state: 5s
      identity-service-cb:
        sliding-window-size: 10
        failure-rate-threshold: 50%
        wait-duration-in-open-state: 10s
```

## Security Features

### JWT Authentication

The gateway validates JWT tokens for protected endpoints:

1. **Token Extraction**: Extracts Bearer tokens from Authorization header
2. **Token Validation**: Validates signature, expiration, and claims
3. **User Context**: Adds user information to request headers for downstream services
4. **Public Endpoints**: Bypasses authentication for login, registration, health checks

### Security Headers

Automatically adds comprehensive security headers to all responses:

- Content Security Policy (CSP)
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security
- Referrer-Policy

### CORS Management

Centralized CORS configuration supporting:

- Dynamic origin patterns for development and production
- Credential support for authenticated requests
- Exposed headers for rate limiting and correlation tracking

## Monitoring & Observability

### Health Checks

- **Gateway Health**: `/api/health` - Basic gateway status
- **Detailed Health**: `/api/health/detailed` - Gateway + dependencies status
- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

### Request Logging

Every request is logged with:

- Correlation ID for tracing across services
- Request/response timing
- Client IP address and User-Agent
- Response status and duration
- Performance warnings for slow requests (>5s)

### Metrics

Prometheus metrics available at `/actuator/prometheus`:

- Request count and timing per service
- Circuit breaker status
- Rate limiting metrics
- Error rates by service and endpoint

## Development

### Building

```bash
# Build the service
./gradlew build

# Run tests
./gradlew test

# Build Docker image
docker build -t focushive/api-gateway .
```

### Running Locally

```bash
# Start dependencies (Redis, Identity Service)
docker-compose up redis identity-service

# Run the gateway
./gradlew bootRun

# Or with specific profile
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### Testing

The gateway includes comprehensive tests:

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Run specific test class
./gradlew test --tests "JwtAuthenticationFilterTest"
```

## Docker Deployment

### Using Docker Compose

Add the gateway to your existing `docker-compose.yml`:

```yaml
# Add gateway-redis and api-gateway services from docker-compose-gateway.yml
# Update frontend environment to use gateway:
web:
  environment:
    VITE_API_URL: http://localhost:8090/api
```

### Standalone Docker

```bash
# Build image
docker build -t focushive/api-gateway .

# Run with environment file
docker run --env-file .env -p 8090:8090 focushive/api-gateway
```

## Production Considerations

### Performance

- **Connection Pooling**: Redis connection pool configured for high concurrency
- **Memory Management**: JVM tuned for gateway workloads
- **Circuit Breaker**: Prevents cascade failures
- **Rate Limiting**: Protects downstream services from overload

### Security

- **Token Validation**: Cryptographic signature validation
- **Request Validation**: Input sanitization and validation
- **Security Headers**: Comprehensive security headers
- **Audit Logging**: Security events logged for monitoring

### Scalability

- **Stateless Design**: Fully stateless for horizontal scaling
- **Redis Clustering**: Supports Redis cluster for high availability
- **Load Balancing**: Works with multiple gateway instances behind load balancer

## Troubleshooting

### Common Issues

1. **JWT Validation Failing**
   - Check JWT secret matches Identity Service
   - Verify token format and expiration
   - Check issuer URI configuration

2. **Rate Limiting Issues**
   - Verify Redis connectivity
   - Check rate limit configuration
   - Monitor Redis memory usage

3. **Circuit Breaker Activation**
   - Check downstream service health
   - Review circuit breaker thresholds
   - Monitor service response times

4. **CORS Issues**
   - Verify allowed origins configuration
   - Check request headers
   - Review browser developer tools

### Logging Configuration

```yaml
logging:
  level:
    com.focushive.gateway: DEBUG  # Gateway-specific logs
    org.springframework.cloud.gateway: DEBUG  # Gateway framework logs
    org.springframework.security: WARN  # Security logs
```

### Monitoring Commands

```bash
# Check gateway health
curl http://localhost:8090/api/health

# Check detailed health
curl http://localhost:8090/api/health/detailed

# Check metrics
curl http://localhost:8090/actuator/metrics

# Check Prometheus metrics
curl http://localhost:8090/actuator/prometheus
```

## Integration with Identity Service

The gateway integrates tightly with the FocusHive Identity Service:

1. **Token Validation**: Validates JWT tokens issued by Identity Service
2. **User Context**: Extracts user ID, roles, and persona information
3. **Public Endpoints**: Allows unauthenticated access to auth endpoints
4. **Token Refresh**: Supports token refresh flows

### Headers Added to Downstream Requests

- `X-User-Id`: Authenticated user ID
- `X-Username`: Username from token
- `X-User-Roles`: Comma-separated user roles
- `X-Persona-Id`: Active persona ID
- `X-Correlation-ID`: Request correlation ID
- `X-Request-ID`: Unique request identifier

## API Gateway Best Practices

This implementation follows Spring Cloud Gateway best practices:

1. **Reactive Programming**: Non-blocking I/O for high performance
2. **Circuit Breaker Pattern**: Resilience4j integration
3. **Rate Limiting**: Redis-based distributed rate limiting
4. **Observability**: Comprehensive metrics and logging
5. **Security**: Defense in depth with multiple security layers
6. **Configuration Management**: Externalized configuration
7. **Health Checks**: Proper health check implementation
8. **Graceful Degradation**: Fallback responses for service failures