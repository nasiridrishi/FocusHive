# Music Service Integration Tests

This directory contains comprehensive integration tests for the Music Service following strict TDD (Test-Driven Development) approach. Tests are written first to define expected behavior before implementation.

## Test Structure

### 1. BaseIntegrationTest.java
- Base class providing TestContainers setup (PostgreSQL, Redis)
- Common test configuration and helper methods
- Handles database isolation between tests

### 2. TestFixtures.java
- Realistic test data and mock responses
- Spotify API response examples
- Entity builders for consistent test data
- WebSocket message templates

### 3. Test Classes

#### SpotifyAuthIntegrationTest.java
**OAuth Flow Testing**
- Authorization URL generation
- Token exchange (authorization code → access token)
- Token refresh flow
- Error handling (invalid codes, expired tokens)
- Credential management and security

#### PlaylistIntegrationTest.java
**CRUD Operations with Database Persistence**
- Playlist creation with Spotify integration
- Reading playlists with tracks
- Updating playlist metadata
- Soft deletion of playlists
- Collaborative playlist permissions
- Pagination support

#### SpotifyApiMockIntegrationTest.java
**External API Mocking**
- Track search with WireMock
- Playlist creation/update operations
- Music recommendations
- Error handling (401, 429, 500)
- Network timeout handling
- API response validation

#### CollaborativePlaylistIntegrationTest.java
**WebSocket Real-time Features**
- WebSocket connection establishment
- Real-time track additions/removals
- Participant join/leave notifications
- Concurrent edit conflict resolution
- Track reordering via WebSocket
- Permission validation for playlist access

#### RateLimitingIntegrationTest.java
**API Rate Limiting**
- Endpoint-specific rate limits
- User-specific rate limiting
- Burst request handling
- Retry-After header behavior
- Rate limit window reset
- Premium user bypass (if applicable)

## Running Tests

### Prerequisites
- Docker (for TestContainers)
- Java 21
- Gradle

### Run All Integration Tests
```bash
./gradlew test --tests "*.integration.*"
```

### Run Specific Test Class
```bash
./gradlew test --tests "SpotifyAuthIntegrationTest"
./gradlew test --tests "PlaylistIntegrationTest"
./gradlew test --tests "SpotifyApiMockIntegrationTest"
./gradlew test --tests "CollaborativePlaylistIntegrationTest"
./gradlew test --tests "RateLimitingIntegrationTest"
```

### Run Test Suite
```bash
./gradlew test --tests "IntegrationTestSuite"
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport
```

## Test Configuration

### Environment Variables
```bash
# PostgreSQL TestContainer
TESTCONTAINER_POSTGRES_VERSION=postgres:15

# Redis TestContainer  
TESTCONTAINER_REDIS_VERSION=redis:7-alpine

# Spotify API Mock
SPOTIFY_CLIENT_ID=test-client-id
SPOTIFY_CLIENT_SECRET=test-client-secret
SPOTIFY_REDIRECT_URI=http://localhost:3000/auth/spotify/callback

# Rate Limiting (for testing)
MUSIC_RATE_LIMIT_SEARCH_REQUESTS_PER_MINUTE=10
MUSIC_RATE_LIMIT_PLAYLIST_REQUESTS_PER_MINUTE=20
MUSIC_RATE_LIMIT_DEFAULT_REQUESTS_PER_MINUTE=60
```

### Test Dependencies
```groovy
// TestContainers
testImplementation("org.testcontainers:postgresql")
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.testcontainers:redis")

// WireMock for API mocking
testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")

// Awaitility for async testing
testImplementation("org.awaitility:awaitility:4.2.0")

// WebTestClient for WebSocket testing
testImplementation("org.springframework.boot:spring-boot-starter-webflux")
```

## TDD Approach Implementation

### 1. Write Tests First
- Define expected behavior through test cases
- Cover both positive and negative scenarios
- Include edge cases and error conditions

### 2. Run Tests (Expect Failures)
- Initial test runs should fail (Red)
- Validates test setup and expectations

### 3. Implement Minimal Code
- Write just enough code to make tests pass (Green)
- Focus on functionality, not optimization

### 4. Refactor
- Improve code quality while keeping tests green
- Maintain test coverage throughout refactoring

### 5. Repeat Cycle
- Add new tests for additional functionality
- Continue Red-Green-Refactor cycle

## Test Data Management

### Database Isolation
- Each test starts with clean database state
- Transactions rolled back after each test
- TestContainers provide fresh instances

### Mock Data
- Realistic Spotify API responses
- Valid track IDs and user data
- Error response scenarios
- WebSocket message formats

### Test Fixtures
- Consistent entity builders
- Parameterized test data
- Reusable test scenarios

## Debugging Tests

### Common Issues
1. **TestContainer startup failures**
   - Ensure Docker is running
   - Check port availability
   - Verify TestContainer versions

2. **WireMock setup problems**
   - Check port conflicts
   - Verify mock server configuration
   - Review stub mappings

3. **WebSocket connection issues**
   - Verify STOMP configuration
   - Check WebSocket security settings
   - Review message broker setup

4. **Rate limiting test timing**
   - Adjust test timeouts
   - Consider test execution order
   - Review rate limit window configuration

### Debug Commands
```bash
# Run with debug logging
./gradlew test --tests "SpotifyAuthIntegrationTest" --debug

# Run single test method
./gradlew test --tests "SpotifyAuthIntegrationTest.shouldExchangeAuthorizationCodeForAccessToken"

# Skip integration tests
./gradlew test -PskipIntegrationTests
```

## Performance Considerations

### Test Execution Time
- TestContainer startup: ~30-60 seconds
- Individual tests: 1-5 seconds each
- Full suite: 5-10 minutes

### Optimization Strategies
- Container reuse between tests
- Parallel test execution (where safe)
- Selective test running during development
- Fast feedback for TDD cycles

## Continuous Integration

### CI/CD Integration
```yaml
# GitHub Actions example
- name: Run Integration Tests
  run: ./gradlew test --tests "*.integration.*"
  env:
    TESTCONTAINER_REUSE_ENABLE: true
```

### Test Reports
- JUnit XML reports in `build/test-results/`
- HTML reports in `build/reports/tests/`
- Coverage reports in `build/reports/jacoco/`

## Best Practices

### Test Writing
- Use descriptive test names explaining behavior
- Follow AAA pattern (Arrange, Act, Assert)
- Keep tests independent and idempotent
- Use realistic test data
- Test error conditions thoroughly

### Maintenance
- Update tests when requirements change
- Keep test data current with API changes
- Review and refactor test code regularly
- Monitor test execution performance

### Documentation
- Document complex test scenarios
- Explain mock setups and data flows
- Maintain test coverage metrics
- Update this README with changes