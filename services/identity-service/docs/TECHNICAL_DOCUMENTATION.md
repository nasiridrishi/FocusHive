# Technical Documentation

> Comprehensive technical reference for Identity Service implementation, OAuth2 architecture, and integration patterns

## Table of Contents

1. [OAuth2 Authorization Server](#oauth2-authorization-server)
2. [JWT Token Architecture](#jwt-token-architecture)
3. [Security Implementation](#security-implementation)
4. [Microservice Integration](#microservice-integration)
5. [Database Design](#database-design)
6. [Performance & Scalability](#performance--scalability)
7. [Error Handling](#error-handling)
8. [API Reference](#api-reference)

## OAuth2 Authorization Server

### Implementation Details

The Identity Service implements a complete OAuth2 2.1 authorization server using Spring Authorization Server 1.3.1.

#### Supported Grant Types

1. **Authorization Code with PKCE** (RFC 7636)
   - Primary flow for web and mobile applications
   - Mandatory PKCE for all public clients
   - Code challenge methods: S256 (SHA-256)
   - Authorization code lifetime: 60 seconds
   - Implementation: `OAuth2AuthorizationController.authorize()`

2. **Client Credentials**
   - Service-to-service authentication
   - Used by microservices for inter-service communication
   - Token lifetime: 1 hour
   - Implementation: `OAuth2TokenService.clientCredentials()`

3. **Refresh Token**
   - Token rotation enabled for enhanced security
   - Refresh token lifetime: 30 days
   - One-time use with automatic rotation
   - Implementation: `OAuth2TokenService.refreshToken()`

#### OAuth2 Endpoints

```java
// Authorization Endpoint
GET /oauth2/authorize
  ?response_type=code
  &client_id={client_id}
  &redirect_uri={redirect_uri}
  &code_challenge={challenge}
  &code_challenge_method=S256
  &state={state}
  &scope={scope}

// Token Endpoint
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code={code}
&redirect_uri={redirect_uri}
&code_verifier={verifier}
&client_id={client_id}
&client_secret={secret}

// Token Introspection
POST /oauth2/introspect
Authorization: Bearer {token}

token={token_to_introspect}

// Token Revocation
POST /oauth2/revoke
Authorization: Bearer {token}

token={token_to_revoke}
&token_type_hint={access_token|refresh_token}

// JWKS Endpoint
GET /oauth2/jwks

// OpenID Discovery
GET /.well-known/openid-configuration
```

### Client Registration

OAuth2 clients are registered in the database with the following properties:

```java
@Entity
@Table(name = "oauth2_registered_client")
public class RegisteredClientEntity {
    @Id
    private String id;
    private String clientId;
    private String clientSecret; // Argon2 hashed
    private String clientName;
    private Set<ClientAuthenticationMethod> authenticationMethods;
    private Set<AuthorizationGrantType> grantTypes;
    private Set<String> redirectUris;
    private Set<String> scopes;
    private ClientSettings clientSettings;
    private TokenSettings tokenSettings;
}
```

### PKCE Implementation

```java
// Code challenge validation in AuthorizationCodeTokenGranter
private void validatePkceParameters(OAuth2AuthorizationCodeRequestAuthenticationToken authentication) {
    String codeVerifier = authentication.getCodeVerifier();
    String codeChallenge = authorization.getAttribute(OAuth2ParameterNames.CODE_CHALLENGE);
    String challengeMethod = authorization.getAttribute(OAuth2ParameterNames.CODE_CHALLENGE_METHOD);

    if (!StringUtils.hasText(codeVerifier)) {
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
    }

    if ("S256".equals(challengeMethod)) {
        String computedChallenge = createS256Hash(codeVerifier);
        if (!computedChallenge.equals(codeChallenge)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
    }
}
```

## JWT Token Architecture

### Token Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id-1"
  },
  "payload": {
    "iss": "http://localhost:8081/identity",
    "sub": "550e8400-e29b-41d4-a716-446655440000",
    "aud": ["focushive-api"],
    "exp": 1709654400,
    "iat": 1709650800,
    "nbf": 1709650800,
    "jti": "2e3f4g5h-6i7j-8k9l-0m1n-2o3p4q5r6s7t",
    "scope": "openid profile email hive.create",
    "persona": "work",
    "roles": ["USER", "PREMIUM"],
    "permissions": [
      "hive.create",
      "hive.join",
      "hive.moderate"
    ],
    "client_id": "focushive-web",
    "token_type": "Bearer"
  }
}
```

### Token Generation

```java
@Service
public class JwtTokenProvider {
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public String generateAccessToken(User user, String persona) {
        Instant now = Instant.now();

        return JWT.create()
            .withIssuer(issuerUri)
            .withSubject(user.getId().toString())
            .withAudience("focushive-api")
            .withExpiresAt(now.plus(1, ChronoUnit.HOURS))
            .withIssuedAt(now)
            .withNotBefore(now)
            .withClaim("persona", persona)
            .withClaim("roles", user.getRoles())
            .withClaim("permissions", user.getPermissions())
            .sign(Algorithm.RSA256(publicKey, privateKey));
    }
}
```

### Token Validation

```java
@Component
public class JwtTokenValidator {
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuerUri)
                .withAudience("focushive-api")
                .build();

            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Invalid JWT token", e);
        }
    }
}
```

## Security Implementation

### Password Security

```java
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
            16,     // Salt length
            32,     // Hash length
            1,      // Parallelism
            4096,   // Memory cost (4 MB)
            3       // Iterations
        );
    }
}
```

### Two-Factor Authentication (TOTP)

```java
@Service
public class TwoFactorAuthService {
    private static final String ISSUER = "FocusHive Identity";
    private static final int SECRET_SIZE = 32;

    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    public boolean verifyCode(String secret, String code) {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        return gAuth.authorize(secret, Integer.parseInt(code));
    }

    public String generateQRCodeUri(User user, String secret) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            ISSUER,
            user.getEmail(),
            secret,
            ISSUER
        );
    }
}
```

### Brute Force Protection

```java
@Component
public class BruteForceProtectionService {
    private final LoadingCache<String, Integer> attemptsCache;
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 30;

    public BruteForceProtectionService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }

    public void registerFailedAttempt(String username) {
        int attempts = getFailedAttempts(username);
        attemptsCache.put(username, attempts + 1);

        if (attempts + 1 >= MAX_ATTEMPTS) {
            blockUser(username);
        }
    }

    private void blockUser(String username) {
        User user = userRepository.findByUsername(username);
        user.setAccountLocked(true);
        user.setLockoutTime(LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES));
        userRepository.save(user);
    }
}
```

### Rate Limiting with Bucket4j

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = getKey(request);
        Bucket bucket = resolveBucket(key);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            response.setStatus(429);
            response.addHeader("X-Rate-Limit-Retry-After", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            return false;
        }
    }

    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }
}
```

## Microservice Integration

### Notification Service Integration (OpenFeign)

```java
@FeignClient(
    name = "notification-service",
    url = "${notification.service.url}",
    fallback = NotificationServiceFallback.class,
    configuration = FeignConfig.class
)
public interface NotificationServiceClient {
    @PostMapping("/api/v1/notifications")
    NotificationResponse sendNotification(@RequestBody NotificationRequest request);

    @GetMapping("/actuator/health")
    String healthCheck();
}

@Component
@Slf4j
public class NotificationServiceFallback implements NotificationServiceClient {
    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.error("Notification service unavailable, falling back for notification type: {}", request.getType());
        // Graceful degradation - queue for retry or log
        return null;
    }
}
```

### Service Authentication

```java
@Configuration
public class FeignConfig {
    @Value("${notification.service.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-API-Key", apiKey);
            requestTemplate.header("X-Service-Name", "identity-service");
            requestTemplate.header("X-Correlation-Id", MDC.get("correlationId"));
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}
```

### Circuit Breaker Pattern

```java
@Component
public class NotificationServiceIntegration {
    private final NotificationServiceClient client;
    private final CircuitBreaker circuitBreaker;

    public NotificationServiceIntegration(NotificationServiceClient client) {
        this.client = client;
        this.circuitBreaker = CircuitBreaker.ofDefaults("notification-service");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit breaker state transition: {}", event));
    }

    public void sendNotification(NotificationRequest request) {
        Supplier<NotificationResponse> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> client.sendNotification(request));

        Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                log.error("Failed to send notification after circuit breaker", throwable);
                return handleFallback(request);
            });
    }
}
```

## Database Design

### Entity Relationships

```sql
-- Core user table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verified BOOLEAN DEFAULT FALSE,
    account_locked BOOLEAN DEFAULT FALSE,
    lockout_time TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC'
);

-- User personas for context switching
CREATE TABLE user_personas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'work', 'study', 'personal', 'custom'
    is_active BOOLEAN DEFAULT FALSE,
    settings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, name)
);

-- OAuth2 clients
CREATE TABLE oauth2_registered_clients (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) UNIQUE NOT NULL,
    client_id_issued_at TIMESTAMP NOT NULL,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000) NOT NULL,
    client_settings TEXT NOT NULL,
    token_settings TEXT NOT NULL
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    replaced_by_token VARCHAR(500)
);

-- Audit log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
```

### Flyway Migrations

```sql
-- V1__Create_users_table.sql
-- V2__Create_oauth2_clients.sql
-- V3__Create_refresh_tokens.sql
-- V4__Create_user_personas.sql
-- V5__Add_two_factor_auth.sql
-- V6__Create_audit_log.sql
-- V7__Add_privacy_settings.sql
```

## Performance & Scalability

### Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("users",
                config.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("tokens",
                config.entryTtl(Duration.ofHours(1)))
            .build();
    }
}
```

### Connection Pooling

```yaml
# HikariCP configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000

# Redis connection pooling
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: -1ms
```

### Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class AsyncNotificationService {
    @Async
    public CompletableFuture<NotificationResponse> sendAsyncNotification(NotificationRequest request) {
        NotificationResponse response = notificationClient.sendNotification(request);
        return CompletableFuture.completedFuture(response);
    }
}
```

## Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2AuthenticationException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error(e.getError().getErrorCode())
            .error_description(e.getError().getDescription())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error("invalid_token")
            .error_description(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error("rate_limit_exceeded")
            .error_description("Too many requests")
            .retry_after(e.getRetryAfter())
            .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }
}
```

### Error Response Format

```json
{
  "error": "invalid_grant",
  "error_description": "The provided authorization grant is invalid",
  "error_uri": "https://tools.ietf.org/html/rfc6749#section-5.2",
  "timestamp": "2024-03-10T10:15:30",
  "path": "/oauth2/token",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## API Reference

### Authentication Endpoints

#### POST /api/auth/register
```json
// Request
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "persona": "work"
}

// Response (201 Created)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "emailVerified": false,
  "createdAt": "2024-03-10T10:15:30Z"
}
```

#### POST /api/auth/login
```json
// Request
{
  "username": "johndoe",
  "password": "SecurePassword123!",
  "persona": "work"
}

// Response (200 OK)
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def50200f7e6d3f3f4...",
  "scope": "openid profile email",
  "persona": "work"
}
```

#### POST /api/auth/refresh
```json
// Request
{
  "refresh_token": "def50200f7e6d3f3f4..."
}

// Response (200 OK)
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "xyz98765dcba321...",
  "scope": "openid profile email"
}
```

### User Management Endpoints

#### GET /api/users/profile
```json
// Headers
Authorization: Bearer {access_token}

// Response (200 OK)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "emailVerified": true,
  "personas": [
    {
      "id": "persona-123",
      "name": "Work",
      "type": "work",
      "isActive": true
    },
    {
      "id": "persona-456",
      "name": "Personal",
      "type": "personal",
      "isActive": false
    }
  ],
  "createdAt": "2024-03-10T10:15:30Z",
  "lastLoginAt": "2024-03-11T09:30:00Z"
}
```

#### POST /api/users/personas/{id}/switch
```json
// Headers
Authorization: Bearer {access_token}

// Response (200 OK)
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR...",
  "persona": "personal",
  "message": "Switched to Personal persona"
}
```

### Privacy Endpoints

#### POST /api/privacy/export
```json
// Headers
Authorization: Bearer {access_token}

// Response (202 Accepted)
{
  "requestId": "export-789",
  "status": "processing",
  "message": "Your data export request has been received",
  "estimatedCompletionTime": "2024-03-10T11:00:00Z"
}
```

---

*For complete API documentation with all endpoints, see the OpenAPI specification at `/swagger-ui.html` when the service is running.*