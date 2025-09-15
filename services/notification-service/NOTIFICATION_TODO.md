# Notification Service Developer TODO - Complete Implementation Guide

## Executive Summary

This document provides a **complete implementation guide** for the Notification Service developer. Each task includes **what** needs to be done, **why** it's important, and **how** to implement it step-by-step.

**Current Status**: The service is BLOCKING password reset functionality for users because authentication between Identity Service and Notification Service is broken.

**Primary Goal**: Enable email notifications (especially password reset emails) by fixing service-to-service authentication.

## 🚨 CRITICAL PRIORITY - Service Authentication Issues

### Issue 1: JWT Authentication Failure (CRITICAL - FIX FIRST)

#### WHAT needs to be done:
Fix JWT authentication so Identity Service can successfully call Notification Service APIs.

#### WHY this is critical:
- Users cannot receive password reset emails
- All service-to-service communication is blocked
- Error: `[401 Unauthorized] during [POST] to [https://notification.focushive.app/api/v1/notifications]`

#### HOW to implement:

**Root Problem**: Identity Service sends RSA-signed (RS256) JWT tokens, but Notification Service expects HMAC-signed tokens.

**Step 1**: Configure Notification Service to accept both RSA and HMAC tokens

```java
// In your SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.jwk-set-uri:https://identity.focushive.app/.well-known/jwks.json}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .decoder(jwtDecoder()) // Use custom decoder for both RSA and HMAC
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications")
                    .hasAnyAuthority("SERVICE", "ROLE_SERVICE", "SCOPE_notification.send")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Support both RSA (from JWKS) and HMAC validation
        return new DualModeJwtDecoder(jwkSetUri);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // Check if this is a service account token
            String tokenType = jwt.getClaimAsString("type");
            if ("service-account".equals(tokenType)) {
                authorities.add(new SimpleGrantedAuthority("SERVICE"));
                
                // Add specific permissions
                List<String> permissions = jwt.getClaimAsStringList("permissions");
                if (permissions != null) {
                    permissions.forEach(permission -> 
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + permission)));
                }
            }
            
            // Handle regular user roles
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            }
            
            return authorities;
        });
        return converter;
    }
}
```

**Step 2**: Create the DualModeJwtDecoder class

```java
// Create new file: DualModeJwtDecoder.java
@Component
@Slf4j
public class DualModeJwtDecoder implements JwtDecoder {
    
    private final NimbusJwtDecoder rsaDecoder;
    private final NimbusJwtDecoder hmacDecoder;
    
    @Value("${jwt.secret:}")
    private String hmacSecret;
    
    public DualModeJwtDecoder(String jwkSetUri) {
        // RSA decoder for JWKS-based validation
        this.rsaDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        
        // HMAC decoder for legacy tokens (if secret is provided)
        if (hmacSecret != null && !hmacSecret.isEmpty() && hmacSecret.length() >= 32) {
            SecretKeySpec secretKey = new SecretKeySpec(
                hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            this.hmacDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512).build();
        } else {
            this.hmacDecoder = null;
            log.warn("No HMAC secret configured - only RSA tokens will be supported");
        }
    }
    
    @Override
    public Jwt decode(String token) throws JwtException {
        // Try RSA first (for service tokens from Identity Service)
        try {
            Jwt jwt = rsaDecoder.decode(token);
            log.debug("Successfully decoded RSA JWT token");
            return jwt;
        } catch (JwtException e) {
            log.debug("RSA validation failed, trying HMAC: {}", e.getMessage());
        }
        
        // Fall back to HMAC if available
        if (hmacDecoder != null) {
            try {
                Jwt jwt = hmacDecoder.decode(token);
                log.debug("Successfully decoded HMAC JWT token");
                return jwt;
            } catch (JwtException e) {
                log.error("Both RSA and HMAC JWT validation failed", e);
                throw e;
            }
        }
        
        throw new JwtValidationException("JWT token validation failed for both RSA and HMAC algorithms", 
            Collections.emptyList());
    }
}
```

**Step 3**: Update your application.yml configuration

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://identity.focushive.app/.well-known/jwks.json

# Optional: HMAC secret for legacy token support
jwt:
  secret: ${JWT_SECRET:}  # Set this in environment if you have HMAC tokens
```

---

### Issue 2: API Key Authentication (HIGH PRIORITY - IMPLEMENT SECOND)

#### WHAT needs to be done:
Add API key authentication as a fallback when JWT fails.

#### WHY this is important:
- Provides immediate unblocking solution for service communication
- Simpler to debug than JWT issues
- Allows testing while JWT issues are being resolved

#### HOW to implement:

**Step 1**: Create API Key Authentication Filter

```java
// Create new file: ApiKeyAuthenticationFilter.java
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${service.api-keys.identity-service:}")
    private String identityServiceApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        String sourceService = request.getHeader("X-Source-Service");

        if (apiKey != null && !apiKey.isEmpty()) {
            if (validateApiKey(apiKey, sourceService)) {
                // Create service authentication
                Authentication auth = new PreAuthenticatedAuthenticationToken(
                    sourceService != null ? sourceService : "unknown-service",
                    null,
                    Arrays.asList(
                        new SimpleGrantedAuthority("SERVICE"),
                        new SimpleGrantedAuthority("SCOPE_notification.send")
                    )
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated service {} via API key", sourceService);
            } else {
                log.warn("Invalid API key provided by service: {}", sourceService);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean validateApiKey(String apiKey, String sourceService) {
        // For identity-service
        if ("identity-service".equals(sourceService) && 
            identityServiceApiKey != null && 
            identityServiceApiKey.equals(apiKey)) {
            return true;
        }
        
        // Add other services as needed
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip for health checks and public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || 
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs");
    }
}
```

**Step 2**: Register the filter in SecurityConfig

```java
// Add to SecurityConfig.java
@Autowired
private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Add API key filter before JWT processing
        .addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter.class)
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                .decoder(jwtDecoder())
            )
        )
        // ... rest of configuration
}
```

**Step 3**: Set environment variables

```bash
# In your environment (.env, docker-compose, or K8s config)
SERVICE_API_KEYS_IDENTITY_SERVICE=your-secure-api-key-here-minimum-32-characters
```

**Step 4**: Update Identity Service to use API Key

The Identity Service is already configured to use API keys. Ensure it has:
```bash
NOTIFICATION_SERVICE_API_KEY=your-secure-api-key-here-minimum-32-characters
```

---

## 🔍 TESTING PRIORITY - Verify Each Fix

### Test 1: API Key Authentication

```bash
# Test the notification endpoint directly
curl -X POST https://notification.focushive.app/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-secure-api-key-here-minimum-32-characters" \
  -H "X-Source-Service: identity-service" \
  -d '{
    "userId": "383aa501-e062-4e01-860b-1bc4ae0cb776",
    "type": "PASSWORD_RESET",
    "title": "Test Password Reset",
    "content": "This is a test notification",
    "priority": "HIGH",
    "variables": {
      "userName": "testuser",
      "resetUrl": "https://identity.focushive.app/reset-password?token=test123",
      "expirationMinutes": "30"
    }
  }'
```

**Expected Result**: `200 OK` with notification created response.

### Test 2: JWT Authentication

```bash
# This test requires a valid service token from Identity Service
# You can get one by calling the Identity Service's token endpoint
curl -X POST https://notification.focushive.app/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${SERVICE_JWT_TOKEN}" \
  -d '{...same payload as above...}'
```

### Test 3: End-to-End Password Reset

```bash
# Test the full password reset flow
curl -X POST https://identity.focushive.app/api/v1/auth/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

**Expected Result**: Email should be sent to the user.

---

## 🔧 OPTIONAL IMPROVEMENTS (Medium Priority)

### Improvement 1: Enhanced Logging for Debugging

#### WHAT: Add detailed authentication logs
#### WHY: Makes debugging auth issues much easier
#### HOW:

```java
// Add to your existing classes
@EventListener
public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    log.error("Authentication failed for principal: {}", 
              event.getAuthentication().getPrincipal());
    log.error("Failure reason: {}", event.getException().getMessage());
    
    // Log JWT details for debugging (remove in production)
    if (event.getAuthentication().getCredentials() instanceof String) {
        String token = (String) event.getAuthentication().getCredentials();
        logJwtDetails(token);
    }
}

private void logJwtDetails(String token) {
    try {
        String[] chunks = token.split("\\.");
        if (chunks.length == 3) {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String header = new String(decoder.decode(chunks[0]));
            String payload = new String(decoder.decode(chunks[1]));
            log.error("JWT Header: {}", header);
            log.error("JWT Payload: {}", payload);
        }
    } catch (Exception e) {
        log.error("Could not decode JWT for debugging", e);
    }
}
```

### Improvement 2: Email Service Health Check

#### WHAT: Verify SMTP configuration is working
#### WHY: Ensures emails can actually be sent after auth is fixed
#### HOW:

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
                .withDetail("timestamp", Instant.now().toString())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("smtp", "Connection failed")
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
```

---

## 📋 IMPLEMENTATION CHECKLIST

Use this checklist to track your progress:

### Phase 1: Critical Authentication Fixes
- [ ] **Step 1.1**: Create `DualModeJwtDecoder` class
- [ ] **Step 1.2**: Update `SecurityConfig` to use dual-mode decoder
- [ ] **Step 1.3**: Test JWT authentication with Identity Service tokens
- [ ] **Step 2.1**: Create `ApiKeyAuthenticationFilter` class  
- [ ] **Step 2.2**: Register API key filter in SecurityConfig
- [ ] **Step 2.3**: Configure API key environment variables
- [ ] **Step 2.4**: Test API key authentication

### Phase 2: Integration Testing
- [ ] **Test 2.1**: Verify API key endpoint access
- [ ] **Test 2.2**: Verify JWT token validation 
- [ ] **Test 2.3**: Test password reset email flow end-to-end
- [ ] **Test 2.4**: Check email delivery to actual email address

### Phase 3: Optional Improvements
- [ ] **Option 3.1**: Add authentication failure logging
- [ ] **Option 3.2**: Add email service health check
- [ ] **Option 3.3**: Add monitoring metrics for auth success/failure

---

## 🚀 DEPLOYMENT CONSIDERATIONS

### Environment Variables Required
```bash
# Critical - API Key Authentication
SERVICE_API_KEYS_IDENTITY_SERVICE=your-secure-api-key-here-minimum-32-characters

# Optional - HMAC support for legacy tokens
JWT_SECRET=your-hmac-secret-if-you-have-legacy-tokens

# SMTP Configuration (verify these are set)
SMTP_HOST=your-smtp-server
SMTP_PORT=587
SMTP_USERNAME=your-smtp-username  
SMTP_PASSWORD=your-smtp-password
```

### Docker/K8s Updates
If you're using Docker or Kubernetes, ensure these environment variables are properly configured in your deployment manifests.

---

## ⚠️ SECURITY NOTES

1. **API Keys**: Use cryptographically secure random strings, minimum 32 characters
2. **JWT Secrets**: If using HMAC fallback, ensure secret is minimum 256 bits (32 chars)
3. **Logging**: Remove JWT payload logging from production code
4. **Headers**: Validate all incoming headers to prevent injection attacks

---

## 🆘 TROUBLESHOOTING

### If API Key Auth Still Fails:
1. Check if `X-API-Key` header is being sent by Identity Service
2. Verify environment variable is loaded: `echo $SERVICE_API_KEYS_IDENTITY_SERVICE`
3. Check logs for "Invalid API key provided" messages
4. Ensure filter is registered correctly in Spring Security chain

### If JWT Auth Still Fails:
1. Check JWKS endpoint is accessible: `curl https://identity.focushive.app/.well-known/jwks.json`
2. Verify token signature algorithm in JWT header
3. Check token expiration time
4. Ensure proper authorities/scopes are present in token claims

### If Emails Still Don't Send:
1. Test SMTP connection: check health endpoint `/actuator/health`
2. Verify email template exists for `PASSWORD_RESET` type
3. Check spam folder for test emails
4. Review application logs for email sending errors

---

## 📞 GETTING HELP

If you encounter issues:

1. **Check logs first**: Look for specific error messages in application logs
2. **Test incrementally**: Verify each component (API key → JWT → Email) separately  
3. **Share specifics**: Include exact error messages, request/response headers, and relevant log snippets
4. **Environment details**: Specify your deployment environment (local, Docker, K8s, etc.)

---

**Document Version**: 1.0  
**Created**: 2025-09-21  
**Status**: CRITICAL - Blocking password reset emails  
**Primary Contact**: Identity Service Team  
**Estimated Implementation Time**: 4-8 hours for critical fixes
