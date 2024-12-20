# Identity Service - Fix Execution Plan

## Immediate Actions (Next 2 Hours)

### Step 1: Diagnose Authentication Endpoint Failures

```bash
# 1. Check current logs for specific errors
docker logs focushive-identity-app 2>&1 | grep -E "ERROR|WARN|400|Bad Request" | tail -50

# 2. Enable debug logging temporarily
docker exec focushive-identity-app sh -c 'echo "logging.level.org.springframework.web=DEBUG" >> application.properties'
docker restart focushive-identity-app

# 3. Test with curl to get detailed error
curl -X POST https://identity.focushive.app/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","username":"test","firstName":"Test","lastName":"User"}' \
  -v

# 4. Check if it's a CORS issue
curl -X OPTIONS https://identity.focushive.app/api/auth/register \
  -H "Origin: https://focushive.app" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

### Step 2: Quick Fixes Without Code Changes

```bash
# 1. Verify environment variables are set correctly
docker exec focushive-identity-app env | grep -E "CORS|JWT|AUTH"

# 2. Check if database migrations ran successfully
docker exec focushive-identity-postgres psql -U focushive_user -d focushive_identity -c "\dt"

# 3. Verify Redis is accessible
docker exec focushive-identity-app redis-cli -h focushive-identity-redis -a redis_pass ping

# 4. Test internal endpoint directly (bypass Cloudflare)
docker exec focushive-identity-app curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"internal@test.com","password":"Test123!","username":"internal"}'
```

## Priority Fix Order

### 1️⃣ Fix Authentication Endpoints (Critical)

#### A. Add Request Logging Filter
**File**: `src/main/java/com/focushive/identity/filter/RequestLoggingFilter.java`
```java
package com.focushive.identity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - startTime;

        // Log request
        String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("REQUEST: {} {} - Duration: {}ms - Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                duration,
                requestBody);

        // Log response
        String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("RESPONSE: Status: {} - Body: {}",
                responseWrapper.getStatus(),
                responseBody);

        // Copy response body to actual response
        responseWrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't log actuator endpoints to reduce noise
        return request.getRequestURI().startsWith("/actuator");
    }
}
```

#### B. Fix Global Exception Handler
**File**: `src/main/java/com/focushive/identity/exception/GlobalExceptionHandler.java`
```java
package com.focushive.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.error("JSON parsing error: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Request")
                .message("Malformed JSON request")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

### 2️⃣ Fix Security Configuration (High)

**File**: `src/main/java/com/focushive/identity/config/SecurityConfig.java`
```java
// Add to existing SecurityConfig class

@Bean
@Order(1)
public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher(
            "/api/auth/**",
            "/.well-known/**",
            "/oauth2/jwks",
            "/actuator/health",
            "/actuator/info"
        )
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        );

    return http.build();
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "https://focushive.app",
        "https://identity.focushive.app",
        "https://backend.focushive.app",
        "https://notification.focushive.app",
        "https://buddy.focushive.app",
        "http://localhost:3000",  // Development
        "http://localhost:5173"   // Development
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 3️⃣ Fix OAuth2 Configuration (Medium)

**File**: `src/main/java/com/focushive/identity/config/OAuth2AuthorizationServerConfig.java`
```java
@Configuration
@EnableAuthorizationServer
public class OAuth2AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        // Create default client if not exists
        RegisteredClient defaultClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("focushive-web")
            .clientSecret("{bcrypt}" + passwordEncoder.encode("focushive-secret-2025"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri("https://focushive.app/callback")
            .redirectUri("http://localhost:3000/callback")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(60))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .build())
            .build();

        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Check if client exists, if not save it
        try {
            repository.findByClientId("focushive-web");
        } catch (Exception e) {
            repository.save(defaultClient);
        }

        return repository;
    }
}
```

## Testing Strategy

### Unit Tests to Add
```java
// AuthControllerTest.java
@Test
void testRegisterSuccess() { }
@Test
void testRegisterDuplicateEmail() { }
@Test
void testRegisterInvalidEmail() { }
@Test
void testRegisterWeakPassword() { }
@Test
void testLoginSuccess() { }
@Test
void testLoginWrongPassword() { }
@Test
void testLoginNonExistentUser() { }
@Test
void testRefreshTokenSuccess() { }
@Test
void testLogoutSuccess() { }
```

### Integration Tests
```bash
# Run after each fix
./gradlew test
./gradlew integrationTest

# Test endpoints manually
./test-all-endpoints.sh
```

## Rollback Procedures

### If Issues Persist After Fixes
```bash
# 1. Rollback to previous version
docker pull focushive/identity-service:previous-stable
docker-compose down
docker-compose up -d

# 2. Clear corrupted data
docker exec focushive-identity-redis redis-cli FLUSHALL
docker exec focushive-identity-postgres psql -U focushive_user -d focushive_identity \
  -c "DELETE FROM oauth2_registered_client WHERE client_id='test-client';"

# 3. Restore from backup
docker exec focushive-identity-postgres pg_restore -U focushive_user -d focushive_identity /backup/latest.dump
```

## Monitoring Commands

```bash
# Watch logs in real-time
docker logs -f focushive-identity-app | grep -E "ERROR|WARN|Exception"

# Monitor endpoint health
watch -n 5 'curl -s https://identity.focushive.app/actuator/health | jq .'

# Check error rates
curl -s https://identity.focushive.app/actuator/metrics/http.server.requests | \
  jq '.measurements[] | select(.statistic == "COUNT")'

# Test authentication flow
while true; do
  curl -X POST https://identity.focushive.app/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test@example.com","password":"Test123!"}' \
    -w "\nStatus: %{http_code} - Time: %{time_total}s\n"
  sleep 5
done
```

## Success Criteria
- [ ] Registration endpoint returns 201 for valid requests
- [ ] Login endpoint returns 200 with JWT token
- [ ] Password reset returns 200 and sends email
- [ ] OAuth2 authorize redirects properly
- [ ] Health endpoint accessible without auth
- [ ] No 400 errors for valid requests
- [ ] Response times < 500ms
- [ ] All tests pass