# API Gateway Integration Tests

This directory contains comprehensive integration tests for the FocusHive API Gateway service.

## Test Coverage

### 1. BaseIntegrationTest.kt
**Base class for all integration tests**
- TestContainers configuration for Redis
- JWT token generation utilities  
- WebTestClient setup
- Common test infrastructure

### 2. RequestRoutingIntegrationTest.kt
**Tests request routing and forwarding functionality**
- ✅ Route matching based on path patterns
- ✅ Request forwarding to correct services
- ✅ Path rewriting and prefix stripping  
- ✅ Headers preservation during routing
- ✅ Different HTTP methods (GET, POST, PUT, DELETE, PATCH)
- ✅ Query parameter preservation
- ✅ Error handling for unmatched routes
- ✅ Service unavailable scenarios
- ✅ User context header injection

**Key Test Scenarios:**
- Identity service routes (`/auth/**`, `/oauth2/**`)
- Protected service routes (`/hives/**`, `/music/**`)
- Health check routes (`/health/**`)
- Request header preservation and user context injection
- Error handling for unreachable services

### 3. JwtAuthenticationIntegrationTest.kt  
**Tests JWT authentication enforcement**
- ✅ Valid JWT token acceptance
- ✅ Invalid/expired JWT token rejection
- ✅ Missing JWT token rejection for protected routes
- ✅ Public routes accessibility without JWT
- ✅ JWT token validation and user context extraction
- ✅ Error responses for authentication failures
- ✅ Malformed authorization header handling
- ✅ JWT claims extraction and header injection
- ✅ Concurrent authentication requests

**Key Test Scenarios:**
- Public endpoint access without authentication
- Protected endpoint authentication enforcement
- JWT token validation (valid, invalid, expired, malformed)
- User context header extraction from JWT claims
- Multiple protected services authentication

### 4. RateLimitingIntegrationTest.kt
**Tests rate limiting functionality**
- ⚠️ Rate limiting enforcement per service (requires Redis setup)
- ⚠️ Rate limit headers in responses  
- ⚠️ Different rate limits for different services
- ⚠️ Rate limit reset after time window
- ✅ Concurrent request handling
- ✅ Per-user rate limiting concepts

**Note:** Most rate limiting tests are disabled as they require proper Redis configuration and complex setup. Basic functionality tests are included.

### 5. CorsIntegrationTest.kt
**Tests CORS (Cross-Origin Resource Sharing) handling**
- ✅ CORS preflight requests (OPTIONS)
- ✅ CORS headers in actual requests
- ✅ Different origins handling (`localhost`, `127.0.0.1`)
- ✅ Allowed methods and headers
- ✅ Credentials handling
- ✅ CORS configuration across different services
- ✅ Max-Age header handling
- ✅ Exposed headers configuration

**Key Test Scenarios:**
- Preflight OPTIONS requests for public and protected endpoints
- CORS headers in actual requests
- Multiple origin patterns support
- All HTTP methods support
- Standard and custom headers support

### 6. GatewayHealthAndFallbackIntegrationTest.kt
**Tests health monitoring and fallback functionality**
- ✅ Gateway health endpoint accessibility
- ✅ Service health monitoring through gateway
- ✅ Fallback responses when services are unavailable
- ✅ Request retry behavior
- ✅ Timeout scenario handling
- ✅ Request tracing headers
- ✅ Global filter processing
- ✅ Circuit breaker integration
- ✅ Error response formatting

**Key Test Scenarios:**
- Actuator health endpoints (`/actuator/health`, `/actuator/info`, `/actuator/prometheus`)
- Service unavailable handling
- Request retry with scenario-based testing
- Timeout handling with WireMock delays
- Error response structure validation

## Test Infrastructure

### Dependencies Added
- WireMock for service mocking
- TestContainers for Redis integration
- Reactor Test for reactive testing
- Mockito Kotlin for mocking

### Test Configuration
- **application-test.yml**: Test-specific configuration with reduced rate limits and mock service URIs
- **TestContainers**: Redis container for rate limiting tests  
- **WireMock**: Mock backend services for isolated testing
- **Dynamic Properties**: Runtime configuration of test services

### Key Testing Patterns

#### 1. Service Mocking with WireMock
```kotlin
wireMockServer.stubFor(
    get(urlPathMatching("/hives/.*"))
        .willReturn(
            aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"id": "123", "name": "Test Hive"}""")
        )
)
```

#### 2. JWT Token Generation
```kotlin
val validToken = createValidJwtToken(
    userId = "user-123",
    username = "testuser", 
    roles = listOf("USER", "PREMIUM"),
    personaId = "persona-456"
)
```

#### 3. Request Verification
```kotlin
wireMockServer.verify(
    getRequestedFor(urlEqualTo("/hives/123"))
        .withHeader("X-User-Id", equalTo("user-123"))
        .withHeader("X-Username", equalTo("testuser"))
)
```

## Running Tests

### Prerequisites
- Java 21
- Docker (for TestContainers)
- Redis container support

### Execution
```bash
# Run all integration tests
./gradlew test --tests "*IntegrationTest"

# Run specific test class
./gradlew test --tests "RequestRoutingIntegrationTest"

# Run with info logging  
./gradlew test --tests "*IntegrationTest" --info
```

### Test Environment
Tests use:
- Random ports to avoid conflicts
- TestContainers for Redis dependency
- WireMock on different ports (8085-8089) for service mocking
- Test-specific application configuration

## Test Coverage Summary

| Component | Coverage | Tests | Status |
|-----------|----------|-------|--------|
| Request Routing | ✅ Comprehensive | 10 tests | Complete |
| JWT Authentication | ✅ Comprehensive | 11 tests | Complete |
| CORS Handling | ✅ Comprehensive | 11 tests | Complete |
| Rate Limiting | ⚠️ Basic | 7 tests (5 disabled) | Partial |
| Health & Fallback | ✅ Good | 11 tests | Complete |

**Total: 50 integration tests covering all major API Gateway functionality**

## Key Features Tested

### ✅ Fully Tested
- Request routing and path matching
- JWT authentication and authorization
- User context extraction and header injection
- CORS preflight and actual request handling
- Health monitoring and error responses
- Service fallback and retry mechanisms
- Global filter processing

### ⚠️ Partially Tested  
- Rate limiting (basic structure, but complex scenarios disabled)
- Circuit breaker behavior (tested indirectly)

### 🔧 Infrastructure
- TestContainers integration
- WireMock service mocking
- Reactive WebTestClient usage
- Test configuration management

## Best Practices Demonstrated

1. **Test Isolation**: Each test uses fresh WireMock state
2. **Realistic Scenarios**: Tests mirror actual gateway usage patterns  
3. **Error Handling**: Comprehensive error scenario testing
4. **Security Testing**: Authentication and authorization enforcement
5. **Performance Aware**: Timeout and concurrent request testing
6. **Integration Focus**: End-to-end gateway behavior testing

This test suite provides comprehensive coverage of the API Gateway's critical functionality and ensures reliable request routing, authentication, and cross-cutting concerns.