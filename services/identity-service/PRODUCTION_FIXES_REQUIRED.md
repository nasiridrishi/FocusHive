# Identity Service - Production Fixes Required

## Critical Priority (P0) - Authentication Core Functionality 🚨

### 1. Authentication Endpoints Returning 400 Bad Request
**Impact**: Users cannot register or login - COMPLETE SERVICE FAILURE
**Endpoints Affected**:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`

**Root Cause Analysis Required**:
- [ ] Check request body validation annotations in DTOs
- [ ] Verify JSON deserialization configuration
- [ ] Investigate Spring Security filter chain interference
- [ ] Check CORS preflight handling for POST requests
- [ ] Validate Content-Type header processing

**Fixes Required**:
```java
// 1. Add comprehensive error handling in AuthController
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex)

// 2. Add request/response logging
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter

// 3. Validate DTO field mappings
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8)
    private String password;
    // Ensure all fields match frontend payload
}
```

**Tests Required**:
- [ ] Unit tests for each DTO validation scenario
- [ ] Integration tests with valid/invalid payloads
- [ ] CORS preflight tests
- [ ] Content-Type variation tests

---

## High Priority (P1) - OAuth2 Authorization Server 🔐

### 2. OAuth2 Authorization Endpoint Not Redirecting
**Impact**: Third-party integrations cannot authenticate
**Endpoint**: `GET /oauth2/authorize`

**Expected Behavior**:
- Should return 302 redirect to login page or consent screen
- Currently returns 400 Bad Request

**Fixes Required**:
```java
// 1. Configure OAuth2 authorization server properly
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig {
    // Add registered clients
    // Configure redirect URIs
    // Set up consent handling
}

// 2. Add test OAuth2 client
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret,
    redirect_uris, scopes
) VALUES (
    'test-client', 'focushive-web', '{bcrypt}secret',
    'https://focushive.app/callback', 'openid profile email'
);
```

**Tests Required**:
- [ ] OAuth2 authorization code flow test
- [ ] Client credentials flow test
- [ ] Token introspection test
- [ ] PKCE support test

---

## Medium Priority (P2) - Monitoring & Observability 📊

### 3. Actuator Endpoints Require Authentication
**Impact**: Cannot monitor service health externally
**Endpoints Affected**:
- `/actuator/prometheus` - Returns 401
- `/actuator/info` - Returns 401
- `/actuator/metrics` - Returns 401

**Fixes Required**:
```java
@Configuration
public class ActuatorSecurityConfig {
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) {
        return http
            .requestMatchers(EndpointRequest.to(
                HealthEndpoint.class,
                InfoEndpoint.class
            )).permitAll()
            .requestMatchers(EndpointRequest.to(
                PrometheusEndpoint.class
            )).hasRole("MONITORING")
            .build();
    }
}
```

**Configuration Updates**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
    prometheus:
      enabled: true
  security:
    enabled: true
```

---

## Implementation Plan (Following TDD)

### Phase 1: Diagnostic & Logging (Day 1)
1. **Add Comprehensive Request Logging**
   ```java
   @Slf4j
   @Component
   public class RequestResponseLoggingFilter extends OncePerRequestFilter {
       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) {
           // Log request details
           log.info("Request: {} {} - Headers: {}, Body: {}",
                   request.getMethod(),
                   request.getRequestURI(),
                   getHeaders(request),
                   getBody(request));

           // Wrap response to capture output
           ContentCachingResponseWrapper responseWrapper =
               new ContentCachingResponseWrapper(response);

           filterChain.doFilter(request, responseWrapper);

           // Log response
           log.info("Response: {} - Body: {}",
                   responseWrapper.getStatus(),
                   getResponseBody(responseWrapper));
       }
   }
   ```

2. **Add Global Exception Handler**
   ```java
   @RestControllerAdvice
   @Slf4j
   public class GlobalExceptionHandler {
       @ExceptionHandler(MethodArgumentNotValidException.class)
       public ResponseEntity<ErrorResponse> handleValidation(
               MethodArgumentNotValidException ex) {
           Map<String, String> errors = new HashMap<>();
           ex.getBindingResult().getFieldErrors()
             .forEach(error -> errors.put(error.getField(),
                                        error.getDefaultMessage()));

           log.error("Validation failed: {}", errors);
           return ResponseEntity.badRequest()
                   .body(new ErrorResponse("Validation failed", errors));
       }

       @ExceptionHandler(HttpMessageNotReadableException.class)
       public ResponseEntity<ErrorResponse> handleJsonParse(
               HttpMessageNotReadableException ex) {
           log.error("JSON parsing failed: {}", ex.getMessage());
           return ResponseEntity.badRequest()
                   .body(new ErrorResponse("Invalid JSON format", null));
       }
   }
   ```

### Phase 2: Fix Authentication Endpoints (Day 2)
1. **Write Failing Tests First**
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   class AuthControllerTest {
       @Test
       void testRegisterWithValidData() {
           // Given
           RegisterRequest request = RegisterRequest.builder()
               .email("test@example.com")
               .password("SecurePass123!")
               .username("testuser")
               .firstName("Test")
               .lastName("User")
               .build();

           // When/Then
           mockMvc.perform(post("/api/auth/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.email").value("test@example.com"));
       }

       @Test
       void testRegisterWithInvalidEmail() {
           // Test 400 response for invalid email
       }

       @Test
       void testLoginWithValidCredentials() {
           // Test successful login returns JWT
       }
   }
   ```

2. **Fix Request DTOs**
   ```java
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class RegisterRequest {
       @NotBlank(message = "Email is required")
       @Email(message = "Invalid email format")
       private String email;

       @NotBlank(message = "Password is required")
       @Size(min = 8, message = "Password must be at least 8 characters")
       @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
               message = "Password must contain uppercase, lowercase, and number")
       private String password;

       @NotBlank(message = "Username is required")
       @Size(min = 3, max = 50)
       private String username;

       private String firstName;
       private String lastName;
   }
   ```

### Phase 3: Fix OAuth2 Configuration (Day 3)
1. **Configure Authorization Server**
   ```java
   @Configuration
   @EnableAuthorizationServer
   public class AuthorizationServerConfig {
       @Bean
       public RegisteredClientRepository registeredClientRepository() {
           RegisteredClient webClient = RegisteredClient
               .withId(UUID.randomUUID().toString())
               .clientId("focushive-web")
               .clientSecret("{noop}secret") // Use BCrypt in production
               .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
               .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
               .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
               .redirectUri("https://focushive.app/callback")
               .redirectUri("http://localhost:3000/callback") // Dev
               .scope(OidcScopes.OPENID)
               .scope(OidcScopes.PROFILE)
               .scope(OidcScopes.EMAIL)
               .build();

           return new InMemoryRegisteredClientRepository(webClient);
       }
   }
   ```

### Phase 4: Configure Security & Monitoring (Day 4)
1. **Update Security Configuration**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       @Order(1)
       public SecurityFilterChain actuatorSecurityChain(HttpSecurity http) {
           http
               .securityMatcher("/actuator/**")
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                   .requestMatchers("/actuator/prometheus").permitAll() // Or use API key
                   .anyRequest().hasRole("ADMIN")
               )
               .httpBasic(Customizer.withDefaults());
           return http.build();
       }

       @Bean
       @Order(2)
       public SecurityFilterChain apiSecurityChain(HttpSecurity http) {
           http
               .securityMatcher("/api/**")
               .cors(cors -> cors.configurationSource(corsConfigurationSource()))
               .csrf(csrf -> csrf.disable())
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/api/auth/**").permitAll()
                   .requestMatchers("/.well-known/**").permitAll()
                   .anyRequest().authenticated()
               )
               .oauth2ResourceServer(oauth2 -> oauth2.jwt());
           return http.build();
       }
   }
   ```

### Phase 5: Integration Testing (Day 5)
1. **Create Comprehensive Test Suite**
   ```java
   @TestConfiguration
   public class IntegrationTestConfig {
       @Bean
       @Primary
       public TestRestTemplate testRestTemplate() {
           return new TestRestTemplate();
       }
   }

   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   class FullIntegrationTest {
       @Test
       void testCompleteUserJourney() {
           // 1. Register user
           // 2. Login
           // 3. Get profile
           // 4. Update profile
           // 5. Refresh token
           // 6. Logout
       }

       @Test
       void testOAuth2Flow() {
           // 1. Get authorization code
           // 2. Exchange for token
           // 3. Validate token
           // 4. Refresh token
       }
   }
   ```

## Validation Criteria

### Success Metrics
- [ ] All authentication endpoints return appropriate status codes
- [ ] JWT tokens are properly generated and validated
- [ ] OAuth2 authorization flow works end-to-end
- [ ] Health endpoint accessible without authentication
- [ ] Prometheus metrics exposed for monitoring
- [ ] Zero 500 errors in production logs
- [ ] 95% test coverage on critical paths

### Performance Criteria
- [ ] Login response time < 500ms
- [ ] Registration response time < 1s
- [ ] Token validation < 100ms
- [ ] Health check < 50ms

### Security Criteria
- [ ] All passwords properly hashed with Argon2
- [ ] JWT tokens expire appropriately
- [ ] Rate limiting enforced on auth endpoints
- [ ] CORS properly configured for Cloudflare domains
- [ ] No sensitive data in logs

## Rollback Plan
If any fix causes production issues:
1. Revert to previous Docker image version
2. Clear Redis cache to prevent token issues
3. Run database migration rollback if schema changed
4. Update Cloudflare tunnel configuration if needed

## Monitoring Post-Deployment
- Set up alerts for 4xx/5xx error rates > 1%
- Monitor JWT validation failures
- Track authentication success/failure ratios
- Monitor response times via Prometheus
- Set up Grafana dashboard for visualization