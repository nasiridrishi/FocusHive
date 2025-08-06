# Inter-Service Communication Setup

## Overview

This document describes the inter-service communication architecture between the FocusHive backend and the Identity Service microservice, implementing UOL-55.

## Architecture

```
┌─────────────────┐         ┌──────────────────┐
│   FocusHive     │ ───────> │  Identity        │
│   Backend       │ <─────── │  Service         │
│   (Port 8080)   │          │  (Port 8081)     │
└─────────────────┘          └──────────────────┘
        │                            │
        └──────────┬─────────────────┘
                   │
            ┌──────▼──────┐
            │    Redis    │
            │   Cache     │
            └─────────────┘
```

## Technologies Used

- **Spring Cloud OpenFeign**: Declarative REST client for service-to-service communication
- **Resilience4j**: Circuit breaker, retry, rate limiting, and bulkhead patterns
- **Spring Cache with Redis**: Distributed caching for reducing inter-service calls
- **Micrometer + Zipkin**: Distributed tracing for monitoring
- **Spring Boot Actuator**: Health checks and metrics

## Key Components

### 1. Feign Client (`IdentityServiceClient.java`)
- Declarative interface for Identity Service API
- Automatic retry and circuit breaker annotations
- Fallback implementation for graceful degradation

### 2. Integration Service (`IdentityIntegrationService.java`)
- Business logic layer for Identity Service integration
- Cache management for frequently accessed data
- Error handling and logging

### 3. Authentication Filter (`IdentityServiceAuthenticationFilter.java`)
- JWT validation delegation to Identity Service
- Security context population with user details
- Support for active persona context

### 4. Health Indicator (`IdentityServiceHealthIndicator.java`)
- Monitors Identity Service availability
- Integrates with Spring Boot Actuator
- Provides detailed health status

## Configuration

### Application Properties

```yaml
# Identity Service URL
identity:
  service:
    url: ${IDENTITY_SERVICE_URL:http://localhost:8081}
    token: ${IDENTITY_SERVICE_TOKEN:}  # Service-to-service auth token

# Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    instances:
      identity-service:
        failureRateThreshold: 60
        waitDurationInOpenState: 10s
        slidingWindowSize: 20
        minimumNumberOfCalls: 10

  retry:
    instances:
      identity-service:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true

# Cache Configuration (Redis)
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

## Usage Examples

### 1. Validate User Token

```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final IdentityIntegrationService identityService;
    
    @GetMapping("/api/v1/user/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        // Token is automatically validated by IdentityServiceAuthenticationFilter
        // Get additional user details if needed
        IdentityDto identity = identityService.getCurrentUser(token);
        return ResponseEntity.ok(identity);
    }
}
```

### 2. Get Active Persona

```java
@Service
@RequiredArgsConstructor
public class HiveService {
    private final IdentityIntegrationService identityService;
    
    public void joinHive(UUID userId, UUID hiveId, String serviceToken) {
        // Get user's active persona for hive membership
        PersonaDto activePersona = identityService.getActivePersona(userId, serviceToken);
        
        // Use persona information for hive context
        String displayName = activePersona.getDisplayName();
        // ... rest of the logic
    }
}
```

### 3. Switch Persona

```java
@PostMapping("/api/v1/persona/switch")
public ResponseEntity<?> switchPersona(@RequestParam UUID personaId, 
                                       @AuthenticationPrincipal IdentityServicePrincipal principal) {
    PersonaDto activated = identityService.activatePersona(
        personaId, 
        principal.getUserId(), 
        getServiceToken()
    );
    return ResponseEntity.ok(activated);
}
```

## Resilience Patterns

### Circuit Breaker
- Opens after 60% failure rate
- Waits 10 seconds before attempting recovery
- Prevents cascading failures

### Retry Logic
- 3 attempts with exponential backoff
- Only retries on transient failures (5xx errors, timeouts)
- Skips retry on client errors (4xx)

### Caching Strategy
- Current user: 5 minutes
- Identity data: 10 minutes
- Active persona: 2 minutes (frequently changes)
- Cache invalidation on updates

### Fallback Mechanisms
- Returns cached data when available
- Provides degraded service for non-critical operations
- Clear error messages for critical failures

## Monitoring

### Health Endpoints
- `/actuator/health` - Overall system health
- `/actuator/health/identityService` - Identity Service specific health
- `/actuator/health/circuitbreakers` - Circuit breaker status

### Metrics
- `/actuator/metrics/resilience4j.circuitbreaker.calls` - Circuit breaker metrics
- `/actuator/metrics/cache.gets` - Cache hit/miss rates
- `/actuator/metrics/http.client.requests` - Feign client metrics

### Distributed Tracing
- Trace IDs propagated across services
- Zipkin integration for visualization
- Correlation IDs in logs

## Testing

### Unit Tests
```bash
./gradlew test --tests "*IdentityIntegrationServiceTest"
```

### Integration Tests
```bash
./gradlew test --tests "*IdentityServiceIntegrationTest"
```

### Load Testing
Use Artillery or K6 to test circuit breaker behavior:
```yaml
# artillery.yml
config:
  target: "http://localhost:8080"
  phases:
    - duration: 60
      arrivalRate: 100
scenarios:
  - name: "Test Identity Service Integration"
    flow:
      - get:
          url: "/api/v1/user/profile"
          headers:
            Authorization: "Bearer ${token}"
```

## Troubleshooting

### Common Issues

1. **Circuit Breaker Open**
   - Check Identity Service health: `curl http://localhost:8081/actuator/health`
   - Review logs for connection errors
   - Verify network connectivity

2. **High Latency**
   - Check cache hit rates in metrics
   - Verify Redis connection
   - Review distributed tracing for bottlenecks

3. **Authentication Failures**
   - Verify service-to-service token configuration
   - Check Identity Service logs
   - Ensure token format is correct

### Debug Logging

Enable debug logging for troubleshooting:
```yaml
logging:
  level:
    com.focushive.backend.client: DEBUG
    feign: DEBUG
    io.github.resilience4j: DEBUG
```

## Security Considerations

1. **Service-to-Service Authentication**
   - Use mTLS or service tokens
   - Rotate tokens regularly
   - Never expose service tokens in logs

2. **Data Privacy**
   - Cache only non-sensitive data
   - Implement cache encryption if needed
   - Clear caches on security events

3. **Rate Limiting**
   - Implement rate limits per client
   - Monitor for unusual patterns
   - Use bulkhead pattern for isolation

## Future Enhancements

- [ ] Implement mTLS for service-to-service communication
- [ ] Add service mesh (Istio/Linkerd) for advanced traffic management
- [ ] Implement async communication with message queues
- [ ] Add GraphQL federation for unified API
- [ ] Implement CQRS pattern for read/write separation