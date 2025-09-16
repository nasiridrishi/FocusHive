# Inter-Service Communication Solution

## Problem
Identity service cannot send notifications to notification service due to authentication requirements.

## Current Status
- ✅ Network connectivity works (services can reach each other)
- ✅ Service discovery works (Docker DNS resolution successful)
- ✅ Feign client configured and making calls
- ❌ Authentication failing (401 Unauthorized)

## Solution Steps

### 1. Configure Service-to-Service Authentication

#### Option A: Shared Secret (Simplest for Development)
```properties
# In identity-service application.properties
notification.service.api-key=shared-secret-key-12345

# In notification-service application.properties
api.security.api-key=shared-secret-key-12345
api.security.bypass-auth-for-api-key=true
```

#### Option B: JWT Service Tokens (Production-Ready)
```java
// Already implemented in ServiceJwtTokenProvider
// Fix the JWT configuration:
1. Ensure JWT_SECRET is the same in both services
2. Configure notification service to accept service tokens
3. Add role-based authorization for SERVICE role
```

### 2. Update Notification Service Security

In notification service, add configuration to accept service requests:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/v1/notifications")
            .hasAnyRole("USER", "SERVICE", "API_KEY")
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

### 3. Environment Variables (.env)

Add these to the main .env file:
```env
# Service-to-Service Authentication
SERVICE_API_KEY=focushive-service-api-key-2025
SERVICE_JWT_SECRET=shared-jwt-secret-for-services
```

### 4. Docker Compose Update

Ensure both services share the authentication configuration:
```yaml
services:
  identity-service:
    environment:
      SERVICE_API_KEY: ${SERVICE_API_KEY}
      NOTIFICATION_SERVICE_API_KEY: ${SERVICE_API_KEY}

  notification-service:
    environment:
      API_SECURITY_API_KEY: ${SERVICE_API_KEY}
      API_SECURITY_ENABLED: true
```

## Testing

### Test Connection (Working)
```bash
# From identity service container
curl -X GET http://focushive-notification-service-app:8083/api/v1/health
# Returns: {"status":"UP"}
```

### Test Feign Client (Currently 401)
```bash
curl -X POST http://localhost:8081/api/test/notification
# Currently returns: 401 Unauthorized
# Will work after notification service accepts API key
```

## Files Modified

1. `/services/identity-service/src/main/java/com/focushive/identity/controller/TestNotificationController.java` - Test endpoint
2. `/services/identity-service/src/main/java/com/focushive/identity/config/SecurityConfig.java` - Allow test endpoints
3. `/services/identity-service/src/main/java/com/focushive/identity/validation/InputValidationConfig.java` - Skip validation for tests
4. `/services/identity-service/src/main/resources/application-docker.properties` - API key configuration

## Next Steps

1. **Update Notification Service** to accept API key authentication
2. **Add SERVICE_API_KEY** to .env file as single source of truth
3. **Test full flow** after notification service update
4. **Remove test endpoints** after verification
5. **Implement proper JWT service tokens** for production

## Key Learning

The Feign client and Docker networking work perfectly. The only issue is missing authentication configuration between services. This is a common microservices pattern that requires either:
- Shared API keys (simple but less secure)
- Service account JWT tokens (more secure)
- OAuth2 client credentials flow (most secure)

For FocusHive development environment, shared API keys are sufficient.