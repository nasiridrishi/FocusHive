# Notification Service - Required Fixes for Email Delivery

**Status**: CRITICAL - Users cannot receive password reset emails
**Date**: 2025-09-22
**Priority**: IMMEDIATE ACTION REQUIRED

## Executive Summary

The Notification Service is completely non-functional due to multiple critical startup issues. Email delivery from Identity Service is blocked. This document provides a prioritized list of fixes with detailed implementation instructions.

### Critical Issues Found:
1. **YAML Configuration Error** - Service crashes on startup due to duplicate keys in configuration
2. **Missing RabbitMQ Queues** - Service cannot connect to required message queues
3. **Port Not Exposed** - Service only accessible via Cloudflare tunnel (which returns 502 when service is down)
4. **API Key Auth Works** - Good news: API key authentication is already implemented and configured correctly

---

## 🚨 Priority 0: Fix YAML Configuration Duplicate Keys (CRITICAL - SERVICE CRASHING)

### What
Fix duplicate key errors in application.yml that are preventing the service from starting.

### Why
- Service is crashing with: `DuplicateKeyException: while constructing a mapping`
- This is happening BEFORE the service even starts Spring Boot
- Blocks ALL functionality - service never starts
- Higher priority than RabbitMQ issues since the app crashes before reaching queue declarations

### How
1. **Check application.yml and application-docker.yml for duplicate keys**:
```bash
# Look for duplicate properties in config files
grep -n ":" src/main/resources/application.yml | sort | uniq -d
grep -n ":" src/main/resources/application-docker.yml | sort | uniq -d
```

2. **Common duplicate key issues**:
- Same property defined multiple times at same level
- Property defined in both application.yml and application-docker.yml
- Incorrect YAML indentation making keys appear as duplicates

3. **Fix the duplicates and rebuild**:
```bash
./gradlew clean build
docker-compose build focushive-notification-service-app
docker-compose up -d focushive-notification-service-app
```

## 🚨 Priority 1: Fix RabbitMQ Queue Declaration (AFTER YAML FIX)

### What
Create the missing RabbitMQ queue `notifications.priority` that is preventing service startup.

### Why
- Service is failing to start completely due to missing RabbitMQ queue
- Logs show repeated failures: "NOT_FOUND - no queue 'notifications.priority' in vhost '/'"
- This blocks ALL functionality including API endpoints
- Service remains unhealthy in Docker health checks

### How
1. **Connect to RabbitMQ container and create the queue**:
```bash
# Access RabbitMQ management
docker exec -it focushive-notification-rabbitmq rabbitmqctl add_queue notifications.priority

# Or use RabbitMQ admin API
curl -u guest:guest -X PUT http://localhost:15672/api/queues/%2F/notifications.priority \
  -H "content-type: application/json" \
  -d '{"durable":true,"auto_delete":false}'
```

2. **Update RabbitMQ configuration to auto-create queues**:
```java
@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue priorityNotificationQueue() {
        return QueueBuilder.durable("notifications.priority")
            .maxPriority(10)  // Enable priority queue
            .build();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
```

3. **Restart the service after queue creation**

## 🚨 Priority 1: Enable API Key Authentication (ALREADY IMPLEMENTED BUT NEEDS TESTING)

### What
API key authentication is already implemented in NotificationService but needs verification.

### Why
- Service logs show: "API key authentication filter initialized with 4 service keys"
- Identity Service is configured with matching API key
- Need to verify it works once service is running

### How
1. **Update your `.env` file**:
```bash
# Add this to notification service .env
# This MUST match NOTIFICATION_SERVICE_API_KEY in identity service
SERVICE_API_KEY_IDENTITY=YwGmBPp44RQrt4ZeZvXcYFCASKtuT/RF88Dt7FjfXVQ=
```

2. **Create API Key Authentication Filter**:

Create file: `src/main/java/com/focushive/notification/security/ApiKeyAuthenticationFilter.java`

```java
package com.focushive.notification.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${service.api-key.identity:}")
    private String identityServiceApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        String sourceService = request.getHeader("X-Source-Service");

        // Check if this is a valid service API key
        if (apiKey != null && !apiKey.isEmpty()) {
            if (apiKey.equals(identityServiceApiKey) && "identity-service".equals(sourceService)) {
                // Create authentication for identity service
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "identity-service",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"),
                           new SimpleGrantedAuthority("SERVICE_ACCOUNT"))
                );
                auth.setDetails("Service: identity-service via API Key");
                SecurityContextHolder.getContext().setAuthentication(auth);

                logger.info("Authenticated service request from identity-service via API key");
            } else {
                logger.warn("Invalid API key attempt from source: " + sourceService);
            }
        }

        chain.doFilter(request, response);
    }
}
```

3. **Update Security Configuration**:

Modify your `SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Add API key filter before JWT filter
            .addFilterBefore(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class)

            .authorizeHttpRequests(authz -> authz
                // Allow service accounts to send notifications
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications")
                    .hasAnyAuthority("SERVICE_ACCOUNT", "ROLE_SERVICE", "ROLE_USER")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            // ... rest of your config
    }
}
```

4. **Test the fix**:
```bash
# Test with curl (replace with actual notification payload)
curl -X POST http://localhost:8083/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YwGmBPp44RQrt4ZeZvXcYFCASKtuT/RF88Dt7FjfXVQ=" \
  -H "X-Source-Service: identity-service" \
  -d '{
    "userId": "test-user-id",
    "type": "PASSWORD_RESET",
    "title": "Password Reset Request",
    "content": "Test email content",
    "priority": "HIGH"
  }'
```

---

## 🔧 Priority 2: Fix JWT Validation for Service Tokens

### What
Update JWT validation to accept service account tokens from Identity Service.

### Why
- Proper long-term solution for service-to-service authentication
- More secure than API keys
- Allows token rotation and expiration

### How

1. **Support Multiple JWT Signing Algorithms**:

Update `application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Identity service currently uses HS512, migrating to RS256
          algorithms:
            - RS256  # For future RSA-signed tokens
            - HS256  # For backward compatibility
            - HS512  # Current identity service tokens

          # For HMAC validation (temporary until RSA migration)
          secret: "51859c6bc2ed4c5c9e74afe96b5e37b47d41d0471a2928c8135c910d8d169f82"

          # For RSA validation (future)
          jwk-set-uri: https://identity.focushive.app/.well-known/jwks.json
          issuer-uri: https://identity.focushive.app
```

2. **Recognize Service Account Tokens**:

Create `JwtServiceAccountConverter.java`:
```java
@Component
public class JwtServiceAccountConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Check if this is a service account token
        String tokenType = jwt.getClaimAsString("type");
        String service = jwt.getClaimAsString("service");

        if ("service-account".equals(tokenType) || "identity-service".equals(service)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));
            authorities.add(new SimpleGrantedAuthority("SERVICE_ACCOUNT"));

            // Add specific permissions from token
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            if (permissions != null && permissions.contains("notification.send")) {
                authorities.add(new SimpleGrantedAuthority("NOTIFICATION_SEND"));
            }
        }

        // Handle regular user tokens
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
```

---

## 📊 Priority 3: Add Debug Logging

### What
Add comprehensive logging to understand authentication failures.

### Why
- Currently impossible to debug why tokens are rejected
- Need visibility into token claims and validation steps
- Essential for troubleshooting production issues

### How

1. **Create Authentication Event Listener**:

```java
@Component
@Slf4j
public class AuthenticationEventListener {

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String principal = event.getAuthentication().getPrincipal().toString();
        String reason = event.getException().getMessage();

        log.error("=== AUTHENTICATION FAILURE ===");
        log.error("Principal: {}", principal);
        log.error("Reason: {}", reason);
        log.error("Exception: ", event.getException());

        // Try to decode JWT for debugging (without validation)
        if (event.getAuthentication().getCredentials() instanceof String) {
            String token = (String) event.getAuthentication().getCredentials();
            try {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    log.error("JWT Payload: {}", payload);
                }
            } catch (Exception e) {
                log.error("Could not decode JWT for debugging", e);
            }
        }
        log.error("==============================");
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        Collection<? extends GrantedAuthority> authorities = event.getAuthentication().getAuthorities();

        log.info("Authentication SUCCESS - Principal: {}, Authorities: {}",
                 principal, authorities);
    }
}
```

2. **Enable Debug Logging in `application.yml`**:
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.focushive.notification.security: DEBUG
    org.springframework.web.filter: DEBUG
```

---

## ✉️ Priority 4: Verify Email Configuration

### What
Ensure SMTP is properly configured and test email sending.

### Why
- Even with authentication fixed, emails won't send without proper SMTP config
- Need to verify email templates exist
- Must test actual email delivery

### How

1. **Verify SMTP Environment Variables**:
```bash
# Check these are set in your .env or environment
SMTP_HOST=smtp.gmail.com  # or your SMTP server
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-specific-password
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
```

2. **Create Test Endpoint** (for debugging only):
```java
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")  // Admin only for security
    public ResponseEntity<?> testEmail(@RequestParam String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Test Email from Notification Service");
            message.setText("If you receive this, SMTP is working correctly!");
            message.setFrom("noreply@focushive.com");

            mailSender.send(message);
            return ResponseEntity.ok("Email sent to " + to);
        } catch (Exception e) {
            log.error("Email test failed", e);
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }
}
```

3. **Verify Email Templates Exist**:
Check for these template files:
- `resources/templates/password-reset.html`
- `resources/templates/email-verification.html`
- `resources/templates/welcome.html`

If missing, create basic templates.

---

## 🧪 Testing Instructions

### Step 1: Test API Key Authentication (After Priority 1)
```bash
# Should return 200 OK
curl -X POST https://notification.focushive.app/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YwGmBPp44RQrt4ZeZvXcYFCASKtuT/RF88Dt7FjfXVQ=" \
  -H "X-Source-Service: identity-service" \
  -d '{
    "userId": "383aa501-e062-4e01-860b-1bc4ae0cb776",
    "type": "PASSWORD_RESET",
    "title": "Password Reset Request",
    "content": "Click link to reset password",
    "priority": "HIGH",
    "variables": {
      "userName": "nasiridrishi",
      "userEmail": "nasiridrishi@outlook.com",
      "resetUrl": "https://focushive.app/reset-password?token=test",
      "expirationMinutes": "30"
    }
  }'
```

### Step 2: Verify Email Delivery
1. Check application logs for "Email sent successfully"
2. Check target email inbox (nasiridrishi@outlook.com)
3. Verify email content and formatting

### Step 3: Test End-to-End Password Reset
```bash
# Trigger from Identity Service
curl -X POST https://identity.focushive.app/api/v1/auth/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{"email": "nasiridrishi@outlook.com"}'
```

Should result in:
1. Identity Service accepts request
2. Identity Service calls Notification Service with API key
3. Notification Service sends email
4. Email arrives at nasiridrishi@outlook.com

---

## 🔧 Priority 5: Fix Docker Port Exposure (Optional for Local Testing)

### What
The Notification Service ports are commented out in docker-compose.yml, making local testing difficult.

### Why
- Ports 8083 and 9091 are commented out "for security"
- Service is only accessible through Cloudflare tunnel
- Makes local debugging and testing challenging
- Cloudflare tunnel returns 502 Bad Gateway when service has issues

### How
For local development/testing, uncomment the ports in `docker-compose.yml`:
```yaml
focushive-notification-service-app:
  ports:
    - "8083:8083"   # Application port
    - "9091:9090"   # JMX port
```

Then restart the service:
```bash
docker-compose down focushive-notification-service-app
docker-compose up -d focushive-notification-service-app
```

---

## ⚠️ Common Issues & Solutions

### Issue: "401 Unauthorized" still occurring
**Solution**: Verify API key matches exactly between services, check for trailing spaces

### Issue: "Connection refused" or timeouts
**Solution**: Ensure notification service is running and accessible at correct URL

### Issue: Emails not arriving despite 200 OK
**Solution**: Check SMTP credentials, verify spam folder, check email logs

### Issue: "Template not found" errors
**Solution**: Create missing email templates in resources/templates/

---

## ✅ Current Status (2025-09-22)

### Identity Service Side (WORKING ✅)
- ✅ Password reset endpoint working correctly
- ✅ User lookup successful for nasiridrishi@outlook.com
- ✅ Reset token generated: `e54267e2-5b0c-4bcf-aa34-d6a11ea8e9e5`
- ✅ API key being sent correctly in headers
- ✅ Feign client configured properly

### Notification Service Side (BROKEN ❌)
- ❌ Service crashes on startup with YAML configuration error
- ❌ Missing RabbitMQ queues (notifications.priority, notifications)
- ❌ Cloudflare tunnel returns 502 Bad Gateway
- ❌ Ports not exposed for local testing
- ✅ API key authentication IS implemented (just needs service to start)

## 📋 Implementation Checklist

- [ ] **Priority 0**: Fix YAML Configuration Duplicate Keys
- [ ] **Priority 1**: Create Missing RabbitMQ Queues
  - [ ] Add SERVICE_API_KEY_IDENTITY to .env
  - [ ] Create ApiKeyAuthenticationFilter
  - [ ] Update SecurityConfig
  - [ ] Test with curl
  - [ ] Verify 200 OK response

- [ ] **Priority 2**: JWT Service Token Support
  - [ ] Update application.yml with multiple algorithms
  - [ ] Add HMAC secret for backward compatibility
  - [ ] Create JwtServiceAccountConverter
  - [ ] Test with service token

- [ ] **Priority 3**: Debug Logging
  - [ ] Create AuthenticationEventListener
  - [ ] Enable debug logging
  - [ ] Test and verify logs show token details

- [ ] **Priority 4**: Email Configuration
  - [ ] Verify SMTP environment variables
  - [ ] Test SMTP connection
  - [ ] Verify email templates exist
  - [ ] Send test email

---

## 🎯 Success Criteria

After implementing these fixes:

1. ✅ API key authentication allows Identity Service to send notifications
2. ✅ Password reset emails arrive at nasiridrishi@outlook.com
3. ✅ Logs clearly show authentication success/failure reasons
4. ✅ No more 401 Unauthorized errors from Identity Service
5. ✅ Email delivery confirmed within 30 seconds of request

---

## 📞 Contact Information

**Identity Service API Key**: `YwGmBPp44RQrt4ZeZvXcYFCASKtuT/RF88Dt7FjfXVQ=`
**Test Email**: nasiridrishi@outlook.com
**Identity Service URL**: https://identity.focushive.app
**Test User ID**: 383aa501-e062-4e01-860b-1bc4ae0cb776

For questions or issues, check Identity Service logs:
```bash
docker logs focushive-identity-app --tail 100
```

---

**Document Created**: 2025-09-22
**Priority**: CRITICAL - Production email delivery blocked
**Estimated Time**: 1-2 hours for all fixes