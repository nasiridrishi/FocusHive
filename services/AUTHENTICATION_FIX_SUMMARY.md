# Service-to-Service Authentication Fix Summary

## Problem Identified

The services were experiencing 401 Unauthorized errors when trying to communicate with each other. Root causes:

1. **Incorrect JWT Issuer Configuration**: Notification service was configured to validate JWT tokens from port 8082 (old music service) instead of 8081 (identity service)
2. **Missing JWKS Implementation**: The identity service JWKS endpoint was returning empty key set
3. **No Service Authentication**: Services weren't properly handling service-to-service JWT tokens
4. **Inconsistent Configuration**: Different services had different JWT validation configurations

## Solutions Implemented

### 1. Fixed JWT Issuer Configuration (All Services)

**Changed From**: `http://localhost:8082`
**Changed To**: `http://localhost:8081`

Updated files:
- `/services/notification-service/src/main/resources/application.yml`
- `/services/notification-service/src/main/resources/application-docker.yml`
- `/services/buddy-service/src/main/resources/application-prod.yml`
- `/services/focushive-backend/src/main/resources/application.yml`
- `/services/focushive-backend/src/main/resources/application-docker.yml`

### 2. Implemented JWKS Endpoint Support

Updated `/services/identity-service/src/main/java/com/focushive/identity/controller/OpenIdConnectDiscoveryController.java`:
- Injected `RSAJwtTokenProvider` to get actual public keys
- Modified JWKS endpoint to return real RSA public keys instead of empty array
- Added proper logging for debugging

### 3. Created Service Authentication Components

#### Notification Service:
- **Created**: `/services/notification-service/src/main/java/com/focushive/notification/security/ServiceAuthenticationFilter.java`
  - Handles service account JWT tokens
  - Validates service-to-service requests
  - Sets proper authentication context

- **Created**: `/services/notification-service/src/main/java/com/focushive/notification/config/JwtConfig.java`
  - Configures JWT decoder with proper issuer and JWKS URI
  - Handles fallback scenarios

- **Updated**: `/services/notification-service/src/main/java/com/focushive/notification/config/SecurityConfig.java`
  - Added service authentication filter to security chain
  - Configured proper filter ordering

### 4. Documentation Created

- **`SERVICE_AUTHENTICATION.md`**: Comprehensive documentation covering:
  - Authentication architecture
  - JWT token types (User vs Service Account)
  - Service endpoints and flows
  - Docker configuration
  - Common issues and solutions
  - Production considerations

### 5. Test Infrastructure

- **Created**: `test-service-auth.sh` - Comprehensive test script that:
  - Checks service health
  - Tests JWKS endpoint
  - Tests OpenID Discovery
  - Validates service-to-service authentication
  - Provides detailed diagnostics

## Configuration Requirements

### Environment Variables

Each service needs these environment variables for Docker deployment:

```yaml
# For client services (notification, buddy, backend):
JWT_ISSUER_URI: http://identity-service:8081
JWT_JWK_SET_URI: http://identity-service:8081/.well-known/jwks.json

# For identity service:
JWT_USE_RSA: true
JWT_SECRET: <your-secret-key>
```

### Identity Service RSA Keys

RSA keys are already present at:
- Private: `/services/identity-service/src/main/resources/keys/jwt-private.pem`
- Public: `/services/identity-service/src/main/resources/keys/jwt-public.pem`

## Testing Authentication

### Quick Test
```bash
# Run the test script
./test-service-auth.sh
```

### Manual Test
```bash
# 1. Get JWKS from identity service
curl http://localhost:8081/.well-known/jwks.json

# 2. Get OpenID configuration
curl http://localhost:8081/.well-known/openid_configuration

# 3. Test service-to-service call
curl -X POST http://localhost:8083/api/v1/notifications/send \
  -H "Authorization: Bearer <service-jwt-token>" \
  -H "X-Service-Name: identity-service" \
  -H "Content-Type: application/json" \
  -d '{"userId": "123", "message": "Test"}'
```

## Deployment Checklist

✅ **Identity Service**:
- RSA keys present in resources/keys/
- jwt.use-rsa=true in application.properties
- JWKS endpoint returns public keys

✅ **Notification Service**:
- JWT issuer URI points to identity service (8081)
- ServiceAuthenticationFilter configured
- JwtDecoder bean configured

✅ **Buddy Service**:
- JWT issuer URI points to identity service (8081)
- JWKS URI corrected to /.well-known/jwks.json

✅ **Backend Service**:
- OAuth2 resource server configuration added
- JWT issuer URI points to identity service (8081)

## Known Issues & Next Steps

### Current Limitations:
1. **HMAC Fallback**: If RSA is disabled, services can't validate tokens (symmetric key can't be shared)
2. **Service Token Generation**: Services need to implement ServiceJwtTokenProvider for outbound requests
3. **Token Refresh**: Service tokens expire in 5 minutes, need refresh mechanism

### Recommended Next Steps:
1. **Implement Service Token Generation** in each service for outbound requests
2. **Add Token Caching** to reduce token generation overhead
3. **Implement Token Refresh** logic for long-running operations
4. **Add Monitoring** for authentication failures
5. **Setup Distributed Tracing** using correlation IDs

## Production Ready Status

### ✅ Completed:
- JWT validation configuration fixed
- JWKS endpoint implemented
- Service authentication filters created
- Documentation complete
- Test infrastructure ready

### ⚠️ Pending for Production:
- Service token generation in all services
- Token refresh mechanism
- Rate limiting on auth endpoints
- Monitoring and alerting
- Load testing authentication flow

## Summary

The authentication infrastructure is now properly configured for service-to-service communication using JWT tokens with RSA signatures. The identity service acts as the central authorization server, and all other services validate tokens against it using the JWKS endpoint.

The 401 errors should now be resolved when services communicate with each other, provided they include proper JWT tokens in their requests.