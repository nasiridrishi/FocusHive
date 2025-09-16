# Testing Documentation

## Coverage Summary
- **Backend**: 92% coverage
- **Frontend**: 80% coverage
- **Overall**: 86% coverage

## Running Tests

### Backend Services
```bash
# All backend tests
cd services/focushive-backend && ./gradlew test
cd services/identity-service && ./gradlew test
cd services/notification-service && ./gradlew test
cd services/buddy-service && ./gradlew test

# Quick test (skip integration)
./gradlew test -PexcludeIntegrationTests
```

### Frontend
```bash
cd frontend
npm test
npm run test:coverage
```

### E2E Tests
```bash
cd frontend
npm run test:e2e
```

## Test Stack
- **Backend**: JUnit 5, Mockito, Spring Boot Test
- **Frontend**: Jest, React Testing Library
- **E2E**: Playwright

## CI/CD
All tests run automatically on push via GitHub Actions.