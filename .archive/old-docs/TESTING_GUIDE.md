# FocusHive Testing Guide

## Overview
Comprehensive testing documentation for FocusHive platform covering unit tests, integration tests, and end-to-end tests.

## Test Coverage Statistics

| Service | Unit Tests | Integration Tests | E2E Tests | Coverage |
|---------|------------|------------------|-----------|----------|
| focushive-backend | 265 | 45 | 15 | 82% |
| identity-service | 189 | 35 | 12 | 78% |
| notification-service | 156 | 28 | 10 | 75% |
| buddy-service | 178 | 32 | 43 | 85% |
| frontend | 134 | - | 25 | 72% |
| **Total** | **922** | **140** | **105** | **~80%** |

## Testing Stack

- **Backend**: JUnit 5, Mockito, TestContainers, RestAssured
- **Frontend**: Jest, React Testing Library, Playwright
- **Database**: H2 (unit), PostgreSQL TestContainers (integration)
- **E2E**: Playwright, Docker Compose

## Running Tests

### Backend Services

```bash
# Unit tests only
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests with coverage
./gradlew test jacocoTestReport

# Service-specific
cd services/buddy-service
./gradlew test
```

### Frontend

```bash
cd frontend

# Unit tests
npm test

# Coverage
npm run test:coverage

# E2E tests
npm run test:e2e
```

### Full E2E Suite

```bash
# Start all services
docker-compose up -d

# Run E2E tests
cd frontend
npm run test:e2e:all

# Or backend E2E
cd services/buddy-service
./gradlew e2eTest
```

## Test Categories

### Unit Tests
- **Purpose**: Test individual components in isolation
- **Location**: `src/test/java/`, `src/__tests__/`
- **Mocking**: Heavy use of mocks for dependencies
- **Speed**: Fast (< 5 seconds total)

### Integration Tests
- **Purpose**: Test component interactions
- **Location**: `src/test/integration/`
- **Database**: TestContainers with real PostgreSQL
- **Speed**: Medium (30-60 seconds)

### E2E Tests
- **Purpose**: Test complete user flows
- **Location**: `e2e/`, `src/test/e2e/`
- **Environment**: Full Docker environment
- **Speed**: Slow (3-5 minutes)

## Key Test Scenarios

### Authentication Flow
1. User registration with email verification
2. Login with JWT generation
3. OAuth2 authorization flow
4. Persona switching
5. Session management

### Real-time Features
1. WebSocket connection establishment
2. Presence updates across hive members
3. Synchronized timer operations
4. Live chat messaging
5. Activity tracking

### Buddy System
1. Preference matching algorithm
2. Partnership creation/acceptance
3. Goal sharing and tracking
4. Check-in reminders
5. Accountability scoring

## Test Data Management

### Fixtures
```java
// Backend
@Sql("/test-data/users.sql")
@Sql(scripts = "/cleanup.sql", executionPhase = AFTER_TEST_METHOD)

// Frontend
import { mockUser, mockHive } from '@/test/fixtures';
```

### TestContainers
```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7");
}
```

## CI/CD Testing

### GitHub Actions Pipeline
1. **On PR**: Unit tests, linting
2. **On merge to main**: Full test suite
3. **Nightly**: E2E tests, performance tests
4. **Pre-deployment**: Security scans, integration tests

## Performance Testing

### Load Testing Setup
- **Tool**: K6
- **Scenarios**: 100, 500, 1000 concurrent users
- **Metrics**: Response time, throughput, error rate

### Key Benchmarks
- API response: < 200ms (p95)
- WebSocket latency: < 50ms
- Database queries: < 100ms
- Page load: < 3s

## Test Best Practices

### Writing Tests
1. **AAA Pattern**: Arrange, Act, Assert
2. **One assertion per test** (when possible)
3. **Descriptive test names**: `should_throwException_whenUserNotFound`
4. **Independent tests**: No shared state
5. **Use builders**: For complex test data

### Mocking Guidelines
```java
// DO: Mock external dependencies
@Mock
private EmailService emailService;

// DON'T: Mock data objects
User user = User.builder().id("123").build(); // Real object

// DO: Verify important interactions
verify(emailService).sendWelcomeEmail(any());
```

## Common Issues & Solutions

### Flaky Tests
- **Problem**: Random failures in CI
- **Solution**:
  - Add proper waits for async operations
  - Use TestContainers for database consistency
  - Mock time-dependent operations

### Slow Tests
- **Problem**: Test suite takes > 10 minutes
- **Solution**:
  - Parallel execution: `@Execution(CONCURRENT)`
  - Shared test containers
  - In-memory databases for unit tests

### Database State
- **Problem**: Tests affecting each other
- **Solution**:
  - `@Transactional` with rollback
  - `@DirtiesContext` for Spring context
  - Unique test data per test

## Coverage Requirements

### Minimum Coverage Targets
- **Overall**: 80%
- **Service Layer**: 90%
- **Controller Layer**: 85%
- **Critical Paths**: 95%
- **Utility Classes**: 70%

### Excluded from Coverage
- Configuration classes
- DTOs and entities
- Generated code
- Main application classes

## Test Commands Reference

```bash
# Run specific test class
./gradlew test --tests BuddyServiceTest

# Run with specific profile
./gradlew test -Dspring.profiles.active=test

# Debug mode
./gradlew test --debug-jvm

# Parallel execution
./gradlew test --parallel

# Rerun failed tests only
./gradlew test --rerun-tasks
```

## Testing Checklist

### Before Committing
- [ ] All tests pass locally
- [ ] Coverage meets requirements
- [ ] No hardcoded test data
- [ ] Tests are not commented out
- [ ] New features have tests

### Before Release
- [ ] Full E2E suite passes
- [ ] Performance tests within limits
- [ ] Security tests pass
- [ ] Cross-browser testing complete
- [ ] Mobile responsiveness verified