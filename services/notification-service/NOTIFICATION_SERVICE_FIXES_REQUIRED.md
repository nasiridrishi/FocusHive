# Notification Service - Critical Fixes Required for Email Delivery

## Executive Summary
The Notification Service is currently unable to accept notification requests from the Identity Service, preventing password reset emails and other critical notifications from being sent to users. This document outlines the specific issues discovered and the required fixes.

## Current Problem
**User Impact**: Users cannot receive password reset emails (specifically tested with nasiridrishi@outlook.com)
**Technical Issue**: All requests from Identity Service to Notification Service return `401 Unauthorized`
**Service Communication**: Identity Service → Notification Service API calls are failing

## Test Case
```bash
# Password reset request that should trigger email
POST https://identity.focushive.app/api/v1/auth/password/reset-request
{
  "email": "nasiridrishi@outlook.com"
}
```

## Issues Discovered

### 1. JWT Authentication Failure
**Current Behavior**:
- Notification Service rejects all JWT tokens from Identity Service with 401 Unauthorized
- Error occurs at: `POST https://notification.focushive.app/api/v1/notifications`

**Root Causes Identified**:

#### A. JWT Signing Algorithm Mismatch
- **Identity Service**: Currently uses HMAC-SHA512 (HS512) for signing service tokens
- **Notification Service**: Expects RSA-SHA256 (RS256) signatures based on JWKS configuration
- **Evidence**:
  ```java
  // Identity Service - JwtTokenProvider.java line 134
  .signWith(secretKey, Jwts.SIG.HS512)

  // Notification Service expects RS256 from JWKS endpoint
  JWT_JWK_SET_URI=https://identity.focushive.app/.well-known/jwks.json
  ```

#### B. Service Account Token Structure
- **Issue**: Service tokens from Identity Service may not include required claims
- **Missing Claims**:
  - `aud` (audience) - Should include "notification-service"
  - `scope` or `permissions` - Should include "notification.send"
  - `sub` (subject) - Should be "service-identity-service" or similar service identifier

### 2. No Alternative Authentication Methods
**Current State**:
- Notification Service only supports JWT Bearer token authentication
- No API key authentication implemented
- No service account bypass for internal services

**Impact**:
- No fallback when JWT authentication fails
- Cannot test notifications independently
- Service-to-service communication is blocked

### 3. Missing Debug Information
**Problem**:
- No detailed logging when authentication fails
- Cannot determine which specific JWT validation step fails
- No visibility into expected vs actual token claims

## Required Fixes for Notification Service

### Priority 1: Fix JWT Validation for Service Accounts (CRITICAL)

#### Task 1.1: Add Service Account Recognition
**What to implement**:
```java
// In your Spring Security configuration
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Allow requests from known service accounts
                .requestMatchers("/api/v1/notifications")
                    .access("hasAuthority('SERVICE') or hasAuthority('ROLE_SERVICE')")
                // ... rest of config
            );
    }

    // Add custom JWT converter to recognize service tokens
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Check if this is a service account token
            String type = jwt.getClaimAsString("type");
            if ("service-account".equals(type)) {
                return List.of(new SimpleGrantedAuthority("SERVICE"));
            }
            // ... handle regular user tokens
        });
        return converter;
    }
}
```

#### Task 1.2: Support Multiple JWT Signing Algorithms
**What to implement**:
- Configure JWT decoder to accept both RS256 (RSA) and HS256/HS512 (HMAC) algorithms
- This is temporary until Identity Service fully migrates to RSA

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          algorithms:
            - RS256  # For user tokens
            - HS256  # For service tokens (temporary)
            - HS512  # For service tokens (temporary)
```

### Priority 2: Add API Key Authentication (HIGH)

#### Task 2.1: Implement API Key Authentication Filter
**What to implement**:
```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${service.api-keys.identity-service}")
    private String identityServiceApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && apiKey.equals(identityServiceApiKey)) {
            // Create authentication for service account
            Authentication auth = new PreAuthenticatedAuthenticationToken(
                "identity-service",
                null,
                List.of(new SimpleGrantedAuthority("SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
```

**Configuration needed**:
```yaml
# application.yml or environment variables
service:
  api-keys:
    identity-service: "production-api-key"  # Must match Identity Service config
```

### Priority 3: Add Debug Logging (MEDIUM)

#### Task 3.1: Add Authentication Debug Logging
**What to implement**:
```java
@Component
@Slf4j
public class AuthenticationDebugListener {

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.error("Authentication failed for principal: {}",
                  event.getAuthentication().getPrincipal());
        log.error("Failure reason: {}",
                  event.getException().getMessage());

        if (event.getAuthentication().getCredentials() instanceof String) {
            String token = (String) event.getAuthentication().getCredentials();
            try {
                // Decode JWT without validation to see claims
                String[] chunks = token.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String payload = new String(decoder.decode(chunks[1]));
                log.error("JWT payload: {}", payload);
            } catch (Exception e) {
                log.error("Could not decode JWT for debugging", e);
            }
        }
    }
}
```

### Priority 4: Verify Email Sending Configuration (MEDIUM)

#### Task 4.1: Check SMTP Configuration
**Verify these settings**:
```yaml
spring:
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### Task 4.2: Verify Email Template for PASSWORD_RESET
**Check if template exists**:
- Location: `resources/templates/email/password-reset.html` or similar
- Required variables: `userName`, `resetUrl`, `expirationMinutes`

### Priority 5: Add Health Check for Email Service (LOW)

#### Task 5.1: Add Email Service Health Indicator
**What to implement**:
```java
@Component
public class EmailServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public Health health() {
        try {
            // Test SMTP connection
            if (mailSender instanceof JavaMailSenderImpl) {
                ((JavaMailSenderImpl) mailSender).testConnection();
            }
            return Health.up()
                .withDetail("smtp", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("smtp", "Connection failed")
                .withException(e)
                .build();
        }
    }
}
```

## Testing Instructions

### Step 1: Test with API Key (After implementing Priority 2)
```bash
curl -X POST https://notification.focushive.app/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: production-api-key" \
  -H "X-Source-Service: identity-service" \
  -d '{
    "userId": "383aa501-e062-4e01-860b-1bc4ae0cb776",
    "type": "PASSWORD_RESET",
    "title": "Password Reset Request",
    "content": "Click here to reset your password",
    "priority": "HIGH",
    "variables": {
      "userName": "nasiridrishi",
      "resetUrl": "https://identity.focushive.app/reset-password?token=test123",
      "expirationMinutes": "30"
    }
  }'
```

### Step 2: Test JWT Service Token (After implementing Priority 1)
```bash
# First get a service token from Identity Service (for testing)
# Then test with that token
curl -X POST https://notification.focushive.app/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${SERVICE_TOKEN}" \
  -d '{...same payload as above...}'
```

### Step 3: Verify Email Delivery
1. Check application logs for email sending attempts
2. Check SMTP server logs if available
3. Verify email arrives at nasiridrishi@outlook.com

## Expected Outcomes After Fixes

1. **Immediate**: API key authentication should allow Identity Service to send notifications
2. **Short-term**: JWT service account authentication should work
3. **User Experience**: Password reset emails delivered within 30 seconds
4. **Monitoring**: Clear logs showing successful email delivery

## Configuration Summary for Identity Service

Once Notification Service is fixed, Identity Service is already configured with:
```
NOTIFICATION_SERVICE_URL=https://notification.focushive.app
NOTIFICATION_SERVICE_API_KEY=production-api-key
```

## Contact for Questions

If you need any clarification on:
- The JWT token structure Identity Service is sending
- The exact error messages from Identity Service logs
- Test coordination between services

Please reach out to the Identity Service team.

## Appendix: Current Error Logs

### Identity Service Error
```
2025-09-21T18:33:41.308Z ERROR [identity-service] NotificationServiceIntegration :
Error sending notification to user 383aa501-e062-4e01-860b-1bc4ae0cb776:
[401 Unauthorized] during [POST] to [https://notification.focushive.app/api/v1/notifications]
```

### Notification Service Response
```json
{
  "path": "/api/v1/notifications",
  "error": "Unauthorized",
  "message": "Authentication required to access this resource",
  "timestamp": "2025-09-21T18:35:43.858980585",
  "status": 401
}
```

## Priority Order for Implementation

1. **CRITICAL - Do First**: Implement API Key authentication (Priority 2) - This will immediately unblock email delivery
2. **HIGH - Do Second**: Fix JWT validation for service accounts (Priority 1) - This is the proper long-term solution
3. **MEDIUM - Do Third**: Add debug logging (Priority 3) - This will help troubleshoot any remaining issues
4. **MEDIUM - Do Fourth**: Verify email configuration (Priority 4) - Ensure emails actually send once auth works
5. **LOW - Do Last**: Add health checks (Priority 5) - For monitoring and operations

---

**Document Created**: 2025-09-21
**Urgency**: CRITICAL - Users cannot reset passwords
**Primary Test User**: nasiridrishi@outlook.com