# Identity Service Integration Guide

## 1. JWT Token Configuration

### Signing Algorithm
- **Primary Algorithm**: RS256 (RSA with SHA-256)
- **Fallback Algorithm**: HS512 (HMAC with SHA-512) - still used by some components
- **Current Issue**: Mixed usage - RSAJwtTokenProvider exists but JwtTokenProvider (HMAC) is still being used in some places

### JWT Public Key/Certificate Location
- **JWKS Endpoint**: `https://identity.focushive.app/.well-known/jwks.json`
  ```json
  {
    "keys": [{
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "focushive-2025-01",
      "n": "ALmEyOJC96bDRCxeP4dEbRkE...",
      "e": "AQAB"
    }]
  }
  ```
- **No separate public key endpoint** - Use JWKS for RSA validation
- **OpenID Configuration**: `https://identity.focushive.app/.well-known/openid-configuration`

### JWT Secret (for HMAC fallback)
```
JWT_SECRET=51859c6bc2ed4c5c9e74afe96b5e37b47d41d0471a2928c8135c910d8d169f82
```
**Note**: This should only be used for backward compatibility. Migrate to RSA validation using JWKS.

### Key Rotation
- **Current Key ID**: `focushive-2025-01`
- **Rotation Strategy**: Not yet implemented
- **Recommendation**: Always fetch current keys from JWKS endpoint, cache for 1 hour

## 2. Token Validation Endpoint

### Current Status
**WARNING**: `/api/v1/auth/validate` endpoint is NOT implemented in Identity Service

### Recommended Approach
**Backend should validate tokens locally using:**
1. Fetch public keys from JWKS endpoint
2. Validate JWT signature using RS256
3. Check token expiration and claims

### Spring Boot Configuration for Local Validation
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.focushive.app
          jwk-set-uri: https://identity.focushive.app/.well-known/jwks.json
```

### If Validation Endpoint Gets Implemented
Expected format would be:
```json
POST /api/v1/auth/validate
Content-Type: application/json
{
  "token": "eyJhbGciOiJSUzI1NiIs..."
}
```

## 3. Authentication Flow

### Complete Flow
1. **User Registration/Login**
   - User authenticates at Identity Service
   - Receives access token (1 hour) and refresh token (30 days)

2. **Request to Backend**
   ```
   Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
   ```

3. **Backend Token Validation**
   - **Recommended**: Local validation using JWKS
   - **Current**: No validation endpoint available
   - **Do NOT** make API calls to Identity Service for each request

4. **Session Management**
   - Backend should NOT store sessions
   - Rely entirely on JWT (stateless)
   - Cache user info from token claims for request duration only

5. **Token Refresh**
   - Backend should return 401 when token expired
   - Frontend handles refresh with Identity Service
   - Backend doesn't manage refresh tokens

## 4. Health Check Integration

### Correct Endpoint
```
GET https://identity.focushive.app/actuator/health
```

### Expected Response Format
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 274726526976,
        "free": 19939214848,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

### Timeout Issues
- **Problem**: Health checks taking 10+ seconds
- **Solution**: Use shorter timeout (3 seconds) and implement circuit breaker
- **Alternative**: Use simpler endpoint if available

## 5. API Communication Issues

### Required Headers
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token> (if calling protected endpoints)
```

### Optional but Recommended Headers
```http
X-Correlation-ID: <uuid>
X-Source-Service: focushive-backend
X-Request-ID: <uuid>
```

### 415 Unsupported Media Type Fix
- **Always include**: `Content-Type: application/json`
- **For Feign clients**: Configure in @FeignClient annotation
```java
@FeignClient(
  name = "identity-service",
  url = "${identity.service.url}",
  configuration = FeignConfig.class
)
```

### No API Keys Required
- Public endpoints: health, JWKS, OpenID config
- Protected endpoints: Use Bearer token

## 6. User & Persona Management

### Token Claims Structure
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "userId": "uuid",
  "personaId": "uuid",
  "displayName": "John Doe",
  "persona": "work",
  "roles": ["USER"],
  "permissions": ["hive.create", "hive.join"],
  "iss": "https://identity.focushive.app",
  "aud": ["focushive-api"],
  "exp": 1234567890,
  "iat": 1234567890
}
```

### Backend Data Handling
- **Store locally**: Only userId as foreign key
- **Cache per request**: Display name, email, current persona
- **Never store**: Passwords, tokens, sensitive data
- **Refresh on each request**: Pull fresh data from token claims

### Persona Switching
- User gets new token when switching personas
- Backend sees different personaId in token
- No special handling needed - treat as new context

## 7. Service Discovery & URLs

### Correct URLs
```
Identity Service: https://identity.focushive.app
JWKS Endpoint: https://identity.focushive.app/.well-known/jwks.json
Health: https://identity.focushive.app/actuator/health
```

### Service Discovery
- **Current**: Hard-coded URLs only
- **No service registry** implemented
- **Configuration**: Use environment variables

## 8. Error Handling

### Expected Error Responses
- **400 Bad Request**: Malformed request, invalid data
- **401 Unauthorized**: Missing, expired, or invalid token
- **403 Forbidden**: Valid token but insufficient permissions
- **404 Not Found**: Resource doesn't exist
- **500 Internal Server Error**: Service failure

### Identity Service Downtime Strategy
1. **Circuit Breaker**: Fail fast after threshold
2. **Cache JWKS**: Keep last known keys for 24 hours
3. **Degraded Mode**:
   - Accept valid tokens (validate locally)
   - Reject new registrations/logins
   - Show maintenance message for auth features

## 9. Performance & Caching

### Caching Strategy
- **JWKS**: Cache for 1 hour
- **Token Validation**: Cache validation result for token lifetime
- **User Info**: Extract from token, don't cache beyond request

### Local JWT Validation
**Strongly Recommended** - Validate signatures locally:
```java
@Component
public class JwtValidator {
    @Cacheable(value = "jwks", cacheManager = "jwksCacheManager")
    public RSAPublicKey getPublicKey(String kid) {
        // Fetch from JWKS endpoint
    }

    public boolean validateToken(String token) {
        // Validate using cached public key
    }
}
```

### No Introspection Endpoint
- Not available - use local validation

## 10. Configuration Requirements

### Spring Security Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .decoder(jwtDecoder())
                )
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
            .withJwkSetUri("https://identity.focushive.app/.well-known/jwks.json")
            .build();
    }
}
```

### Critical Environment Variables
```bash
# Required
IDENTITY_SERVICE_URL=https://identity.focushive.app
JWT_ISSUER_URI=https://identity.focushive.app
JWT_JWK_SET_URI=https://identity.focushive.app/.well-known/jwks.json

# Optional (for HMAC fallback - not recommended)
JWT_SECRET=51859c6bc2ed4c5c9e74afe96b5e37b47d41d0471a2928c8135c910d8d169f82
```

## 11. Testing & Development

### Test Users
```
Admin User:
Username: focushive-admin
Password: FocusHiveAdmin2024!
Email: admin@focushive.local
```

### Generating Test Tokens
Currently, you must:
1. Login via Identity Service API
2. Use the returned access token

```bash
curl -X POST https://identity.focushive.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "focushive-admin",
    "password": "FocusHiveAdmin2024!"
  }'
```

### API Documentation
- **OpenAPI/Swagger**: Currently not exposed publicly
- **Endpoints documented in**: API_REFERENCE.md

## 12. Specific Technical Issues & Solutions

### "Value must not be null" from Health Checks
**Cause**: Backend expecting different health response structure
**Solution**: Parse response correctly, handle missing fields gracefully

### Circuit Breaker 41.67% Failure Rate
**Causes**:
1. 415 errors from missing Content-Type
2. Timeouts from long health checks
3. 401 errors from invalid authentication

**Solutions**:
1. Always include Content-Type header
2. Set timeout to 3 seconds
3. Implement proper JWT validation

### 10-Second Timeouts
**Solution**: Configure Feign client timeouts
```yaml
feign:
  client:
    config:
      identity-service:
        connectTimeout: 5000
        readTimeout: 3000
```

### 415 Unsupported Media Type
**Solution**: Add to every request
```java
@Bean
public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
        requestTemplate.header("Content-Type", "application/json");
        requestTemplate.header("Accept", "application/json");
    };
}
```

## Summary of Key Points

1. **Use JWKS endpoint** for RSA public keys
2. **Validate tokens locally** - no validation endpoint exists
3. **Stateless authentication** - don't store sessions
4. **Include proper headers** - Content-Type is mandatory
5. **Configure timeouts** - 3 seconds max for health checks
6. **Cache JWKS** - refresh hourly
7. **Handle errors gracefully** - implement circuit breaker

## Known Issues Being Fixed

1. **Mixed JWT signing** - Migration from HMAC to RSA in progress
2. **No validation endpoint** - Must validate locally
3. **Service-to-service auth** - Currently broken, being fixed in Notification Service

## Contact

For Identity Service issues, check:
- Logs at: `docker logs focushive-identity-app`
- Health at: `https://identity.focushive.app/actuator/health`

---
*Document Generated: 2025-09-21*
*Based on Identity Service version deployed to production*