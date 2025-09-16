# Test Coverage Report

> Comprehensive test documentation for Identity Service including unit, integration, and performance tests

## Table of Contents

1. [Test Overview](#test-overview)
2. [Test Infrastructure](#test-infrastructure)
3. [Unit Tests](#unit-tests)
4. [Integration Tests](#integration-tests)
5. [Performance Tests](#performance-tests)
6. [Security Tests](#security-tests)
7. [Test Coverage Metrics](#test-coverage-metrics)
8. [Test Execution Guide](#test-execution-guide)

## Test Overview

### Current Status
- **Total Test Files**: 232
- **Total Test Cases**: 890+
- **Overall Coverage**: 80% (Target: 85%)
- **Unit Test Coverage**: 85%
- **Integration Test Coverage**: 75%
- **Critical Path Coverage**: 95%

### Test Categories

| Category | Files | Tests | Coverage | Status |
|----------|-------|-------|----------|--------|
| Unit Tests | 150 | 600+ | 85% | ✅ Active |
| Integration Tests | 50 | 200+ | 75% | ✅ Active |
| Performance Tests | 10 | 30+ | N/A | ✅ Active |
| Security Tests | 15 | 45+ | 90% | ✅ Active |
| E2E Tests | 7 | 15+ | 70% | 🔄 In Progress |

## Test Infrastructure

### Test Configuration

```java
// Base test configuration
@TestConfiguration
@Import({TestSecurityConfig.class, TestDatabaseConfig.class, TestRedisConfig.class})
public class BaseTestConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4); // Faster for tests
    }

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2024-03-10T10:15:30Z"), ZoneOffset.UTC);
    }
}
```

### Test Profiles

```properties
# application-test.properties
spring.profiles.active=test
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.redis.host=localhost
spring.redis.port=6370
jwt.secret=test-secret-key-for-testing-only
jwt.expiration=3600000
```

### TestContainers Setup

```java
@TestContainers
@SpringBootTest
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("identity_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}
```

## Unit Tests

### Controller Tests

```java
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Should register new user successfully")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Password123!");
        UserDto expectedResponse = new UserDto(UUID.randomUUID(), "testuser", "test@example.com");

        when(authService.register(any(RegisterRequest.class)))
            .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Should return 400 for invalid email format")
    void testInvalidEmailRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "invalid-email", "Password123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }
}
```

### Service Tests

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should authenticate user and return tokens")
    void testSuccessfulAuthentication() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password");
        User user = createTestUser();

        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash()))
            .thenReturn(true);
        when(tokenProvider.generateAccessToken(user))
            .thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user))
            .thenReturn("refresh-token");

        // When
        AuthResponse response = authService.authenticate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should handle account lockout after failed attempts")
    void testAccountLockout() {
        // Given
        User user = createTestUser();
        user.setFailedLoginAttempts(4);

        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(false);

        // When/Then
        assertThrows(AccountLockedException.class, () -> {
            authService.authenticate(new LoginRequest("testuser", "wrong"));
        });

        verify(userRepository).save(argThat(u ->
            u.isAccountLocked() && u.getFailedLoginAttempts() == 5
        ));
    }
}
```

### Repository Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should enforce unique constraint on email")
    void testUniqueEmailConstraint() {
        // Given
        User user1 = createUser("user1", "same@example.com");
        User user2 = createUser("user2", "same@example.com");

        // When
        userRepository.save(user1);

        // Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}
```

## Integration Tests

### OAuth2 Flow Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class OAuth2IntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should complete authorization code flow with PKCE")
    void testAuthorizationCodeFlowWithPKCE() throws Exception {
        // Step 1: Authorization request
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MvcResult authResult = mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("code_challenge", codeChallenge)
                .param("code_challenge_method", "S256")
                .param("state", "xyz"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = authResult.getResponse().getHeader("Location");
        String code = extractCodeFromLocation(location);

        // Step 2: Token exchange
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", code)
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("code_verifier", codeVerifier)
                .param("client_id", "test-client")
                .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600));
    }
}
```

### Notification Service Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceIntegrationTest {

    @MockBean
    private NotificationServiceClient notificationClient;

    @Autowired
    private NotificationServiceIntegration notificationService;

    @Test
    @DisplayName("Should send welcome email via notification service")
    void testSendWelcomeEmail() {
        // Given
        User user = createTestUser();
        NotificationResponse mockResponse = NotificationResponse.builder()
            .id("notif-123")
            .status("SENT")
            .build();

        when(notificationClient.sendNotification(any()))
            .thenReturn(mockResponse);

        // When
        notificationService.sendWelcomeEmail(user);

        // Then
        verify(notificationClient).sendNotification(argThat(request ->
            "WELCOME".equals(request.getType()) &&
            user.getEmail().equals(request.getRecipient())
        ));
    }

    @Test
    @DisplayName("Should handle notification service failure gracefully")
    void testNotificationServiceFallback() {
        // Given
        User user = createTestUser();
        when(notificationClient.sendNotification(any()))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When/Then - should not throw
        assertDoesNotThrow(() -> notificationService.sendWelcomeEmail(user));
    }
}
```

### End-to-End Registration Flow

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationE2ETest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should complete full registration flow")
    void testCompleteRegistrationFlow() {
        // Step 1: Register
        RegisterRequest request = new RegisterRequest(
            "newuser", "new@example.com", "Password123!"
        );

        ResponseEntity<UserDto> registerResponse = restTemplate.postForEntity(
            "/api/auth/register", request, UserDto.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 2: Verify email sent (check mock)
        User user = userRepository.findByEmail("new@example.com").orElseThrow();
        assertThat(user.getEmailVerificationToken()).isNotNull();

        // Step 3: Verify email
        ResponseEntity<Void> verifyResponse = restTemplate.postForEntity(
            "/api/auth/verify-email?token=" + user.getEmailVerificationToken(),
            null, Void.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 4: Login
        LoginRequest loginRequest = new LoginRequest("newuser", "Password123!");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
            "/api/auth/login", loginRequest, AuthResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().getAccessToken()).isNotNull();
    }
}
```

## Performance Tests

### Load Testing

```java
@Test
@DisplayName("Should handle 1000 concurrent login requests")
void testConcurrentLogins() throws InterruptedException {
    int numberOfThreads = 100;
    int requestsPerThread = 10;
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < requestsPerThread; j++) {
                    LoginRequest request = new LoginRequest("user" + j, "password");
                    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                        "/api/auth/login", request, AuthResponse.class
                    );

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(30, TimeUnit.SECONDS);

    assertThat(successCount.get()).isGreaterThan(900); // 90% success rate
    assertThat(failureCount.get()).isLessThan(100);
}
```

### Response Time Testing

```java
@Test
@DisplayName("Should respond within 200ms for authentication")
void testAuthenticationResponseTime() {
    LoginRequest request = new LoginRequest("testuser", "password");

    long startTime = System.currentTimeMillis();
    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
        "/api/auth/login", request, AuthResponse.class
    );
    long endTime = System.currentTimeMillis();

    long responseTime = endTime - startTime;

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseTime).isLessThan(200); // Less than 200ms
}
```

## Security Tests

### SQL Injection Tests

```java
@Test
@DisplayName("Should prevent SQL injection in login")
void testSQLInjectionPrevention() {
    LoginRequest maliciousRequest = new LoginRequest(
        "admin' OR '1'='1", "password"
    );

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/auth/login", maliciousRequest, ErrorResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().getError()).isEqualTo("invalid_credentials");
}
```

### XSS Prevention Tests

```java
@Test
@DisplayName("Should sanitize user input to prevent XSS")
void testXSSPrevention() {
    RegisterRequest request = new RegisterRequest(
        "<script>alert('xss')</script>",
        "test@example.com",
        "Password123!"
    );

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/auth/register", request, ErrorResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getError()).isEqualTo("validation_error");
}
```

### Rate Limiting Tests

```java
@Test
@DisplayName("Should enforce rate limiting")
void testRateLimiting() {
    LoginRequest request = new LoginRequest("testuser", "password");

    // Make 100 requests rapidly
    List<ResponseEntity<AuthResponse>> responses = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        responses.add(restTemplate.postForEntity(
            "/api/auth/login", request, AuthResponse.class
        ));
    }

    // Check that some requests were rate limited
    long rateLimitedCount = responses.stream()
        .filter(r -> r.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
        .count();

    assertThat(rateLimitedCount).isGreaterThan(0);
}
```

## Test Coverage Metrics

### Coverage by Package

| Package | Line Coverage | Branch Coverage | Tests |
|---------|--------------|-----------------|-------|
| controller | 92% | 88% | 45 |
| service | 88% | 85% | 120 |
| repository | 95% | 90% | 35 |
| security | 85% | 80% | 60 |
| config | 75% | 70% | 25 |
| dto | 100% | N/A | 15 |
| entity | 100% | N/A | 20 |
| exception | 80% | 75% | 15 |
| util | 90% | 85% | 30 |

### Critical Path Coverage

| Flow | Coverage | Tests | Priority |
|------|----------|-------|----------|
| User Registration | 95% | 12 | P0 |
| User Login | 98% | 15 | P0 |
| OAuth2 Authorization | 90% | 10 | P0 |
| Token Refresh | 92% | 8 | P1 |
| Password Reset | 88% | 6 | P1 |
| Persona Switching | 85% | 5 | P2 |
| Two-Factor Auth | 82% | 7 | P2 |

## Test Execution Guide

### Running Tests

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew unitTest

# Run integration tests only
./gradlew integrationTest

# Run specific test class
./gradlew test --tests "AuthControllerTest"

# Run with coverage
./gradlew test jacocoTestReport

# Run performance tests
./gradlew performanceTest

# Run security tests
./gradlew securityTest
```

### Test Reports

- **JUnit Report**: `build/reports/tests/test/index.html`
- **Coverage Report**: `build/reports/jacoco/test/html/index.html`
- **Performance Report**: `build/reports/gatling/index.html`

### CI/CD Integration

```yaml
# GitHub Actions workflow
name: Test Suite

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Run tests
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: test
          DATABASE_URL: jdbc:postgresql://localhost:5432/test
          DATABASE_USERNAME: postgres
          DATABASE_PASSWORD: test

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml

      - name: Archive test reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: build/reports/
```

### Troubleshooting Tests

#### Common Issues

1. **TestContainers not starting**
   - Ensure Docker is running
   - Check Docker resources (memory/CPU)
   - Use `@TestPropertySource` to override container settings

2. **Flaky OAuth2 tests**
   - Use fixed clock in tests
   - Mock time-sensitive components
   - Ensure proper test isolation

3. **Database connection issues**
   - Check test profile configuration
   - Verify PostgreSQL container is running
   - Use H2 for faster unit tests

4. **Redis connection failures**
   - Disable Redis in unit tests
   - Use embedded Redis for integration tests
   - Mock Redis operations where possible

---

*For detailed test implementation examples and patterns, refer to the test source code in `src/test/java/com/focushive/identity/`*