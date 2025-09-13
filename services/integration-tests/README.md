# FocusHive Cross-Service Integration Tests

Comprehensive integration test suite for validating cross-service data flow and consistency in the FocusHive microservices architecture.

## Overview

This test suite follows **Test-Driven Development (TDD)** principles to validate complex interactions between FocusHive's 8 microservices:

- **FocusHive Backend** (Port 8080) - Core application logic
- **Identity Service** (Port 8081) - OAuth2 and user management  
- **Analytics Service** (Port 8085) - Productivity tracking
- **Notification Service** (Port 8083) - Multi-channel notifications
- **Buddy Service** (Port 8087) - Accountability partners
- **Chat Service** (Port 8084) - Real-time messaging
- **Music Service** (Port 8082) - Spotify integration
- **Forum Service** (Port 8086) - Community discussions

## Test Coverage

### 1. UserAnalyticsFlowIntegrationTest
**Purpose**: Validates User Action → Analytics Update flow

**Test Scenarios**:
- ✅ User joins hive → Analytics records join event
- ✅ Timer session starts → Analytics tracks focus time  
- ✅ Session completes → Productivity metrics updated
- ✅ Achievement earned → Analytics records achievement
- ✅ Data consistency verification across services
- ✅ Service failure resilience testing

**Key Validations**:
- Cross-service event propagation
- Eventual consistency handling
- Response time < 500ms per service
- 95% data consistency across services

### 2. HiveNotificationFlowIntegrationTest
**Purpose**: Validates Hive Activity → Notification Generation flow

**Test Scenarios**:
- ✅ New member joins → Creator receives notification
- ✅ Timer session starts → Members receive updates
- ✅ Hive milestone reached → Celebration notifications
- ✅ Hive inactivity → Warning notifications
- ✅ Notification delivery performance testing
- ✅ User preferences compliance verification

**Key Validations**:
- Multi-channel delivery (in-app, email, push)
- Notification preferences respected
- Delivery time < 5 seconds
- Template accuracy and localization

### 3. BuddyAnalyticsFlowIntegrationTest
**Purpose**: Validates Buddy System → Analytics Integration flow

**Test Scenarios**:
- ✅ Buddy matching → Partnership analytics recorded
- ✅ Joint session → Shared metrics updated
- ✅ Accountability check-in → Progress tracked
- ✅ Buddy achievement → Shared celebration
- ✅ Concurrent operations performance
- ✅ Partnership termination cleanup

**Key Validations**:
- Joint productivity metrics calculation
- Accountability tracking accuracy
- Collaboration score computation
- Concurrent buddy operations handling

### 4. DataConsistencyIntegrationTest
**Purpose**: Validates system-wide data consistency

**Test Scenarios**:
- ✅ User profile update → Propagation to all services
- ✅ Hive deletion → Cascade cleanup verification
- ✅ GDPR user deletion → Complete data removal
- ✅ Service failure recovery → Data integrity maintained
- ✅ High-load consistency testing
- ✅ Transaction boundary verification

**Key Validations**:
- ACID properties across distributed services
- GDPR compliance (right to be forgotten)
- Recovery from partial failures
- Data consistency under concurrent load

### 5. EventOrderingIntegrationTest
**Purpose**: Validates event processing and ordering

**Test Scenarios**:
- ✅ Concurrent events → Correct chronological processing
- ✅ Duplicate events → Deduplication across services
- ✅ Failed events → Retry with exponential backoff
- ✅ Permanent failures → Dead letter queue handling
- ✅ High-volume streams → Performance validation
- ✅ Distributed tracing → Event correlation

**Key Validations**:
- Event ordering guarantees
- Idempotency mechanisms
- Retry policies and circuit breakers
- Event correlation and tracing
- Processing rate ≥ 50 events/second

## Test Infrastructure

### TestContainers Setup
- **PostgreSQL 16**: Isolated database per test class
- **Redis 7**: Cache and pub/sub testing
- **Network isolation**: Shared network for service communication

### WireMock Integration
- **Spotify API Mock**: Authentication and playlist operations
- **Email Service Mock**: Notification delivery simulation
- **Push Service Mock**: Mobile notification testing

### Performance Benchmarks
- **Response Time**: < 500ms per service call
- **Event Processing**: ≥ 50 events/second
- **Notification Delivery**: < 5 seconds end-to-end
- **Data Consistency**: 95% across all services
- **Recovery Time**: < 30 seconds from failure

### Test Data Management
- **Factory Pattern**: Consistent test data creation
- **Cleanup Strategy**: Automatic cleanup after each test
- **Realistic Data**: Production-like data patterns
- **Cross-Service Correlation**: Proper ID management

## Running the Tests

### Prerequisites
```bash
# Required software
- Java 21+
- Docker Desktop
- Gradle 8+

# Verify Docker is running
docker --version
docker-compose --version
```

### Execution Commands

```bash
# Run all cross-service integration tests
./gradlew test

# Run specific test categories
./gradlew crossServiceIntegrationTest
./gradlew performanceIntegrationTest

# Run with specific profiles
./gradlew test -Dspring.profiles.active=integration-test

# Run with debug logging
./gradlew test --debug-jvm

# Run with coverage report
./gradlew test jacocoTestReport
```

### Environment Configuration

```bash
# Set test environment variables
export TESTCONTAINERS_REUSE_ENABLE=false
export SPRING_PROFILES_ACTIVE=integration-test

# Performance tuning
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
```

## Test Architecture

### Base Infrastructure

```java
AbstractCrossServiceIntegrationTest
├── TestContainers Management
├── WireMock Server Setup  
├── REST Assured Configuration
├── Awaitility Settings
├── Cross-Service Utilities
└── Performance Measurement
```

### Test Data Factory

```java
TestDataFactory
├── Users.createTestUser()
├── Hives.createTestHive()
├── TimerSessions.createTimerSession()
├── AnalyticsEvents.createUserJoinEvent()
├── Notifications.createHiveJoinNotification()
├── BuddyData.createBuddyPair()
└── ProductivityMetrics.createDailyMetrics()
```

### Utility Methods

```java
CrossServiceTestUtils
├── waitForEventualConsistency()
├── retryWithBackoff()
├── measureExecutionTime()
├── assertResponseTime()
└── verifyDataConsistency()
```

## TDD Approach Implementation

### 1. Red Phase (Failing Test)
```java
@Test
@DisplayName("TDD: User action should trigger analytics - FAILING TEST")  
void testUserActionAnalytics_ShouldFail() {
    // Given: User and expected behavior
    // When: User performs action
    // Then: Analytics should update (WILL FAIL INITIALLY)
}
```

### 2. Green Phase (Minimal Implementation)
- Implement just enough functionality to pass the test
- Focus on cross-service communication
- Ensure eventual consistency handling

### 3. Refactor Phase (Optimization)
- Improve performance and reliability
- Add error handling and resilience
- Optimize for production readiness

## Performance Monitoring

### Key Metrics Tracked
- **Service Response Times**: Per-endpoint timing
- **Event Processing Rates**: Events per second
- **Data Consistency Lag**: Time to eventual consistency
- **Error Rates**: Failure percentage by service
- **Resource Utilization**: Memory and CPU usage

### Performance Assertions
```java
// Response time validation
CrossServiceTestUtils.assertResponseTime(duration, maxTime);

// Processing rate validation  
assertTrue(eventsPerSecond >= 50, "Processing rate too low");

// Consistency validation
verifyDataConsistency(userId, expectedData);
```

## Error Handling and Resilience

### Retry Mechanisms
- **Exponential Backoff**: 1s, 2s, 4s, 8s, 16s
- **Max Retry Attempts**: 5 per operation
- **Circuit Breaker**: Fail-fast after threshold

### Failure Scenarios Tested
- **Service Unavailability**: Temporary service outages
- **Network Partitions**: Communication failures
- **Database Connectivity**: Connection pool exhaustion
- **External Service Failures**: Third-party API issues

### Recovery Validation
- **Data Integrity**: No partial state after failures  
- **Transaction Rollback**: Proper cleanup on failure
- **Service Health**: Automatic recovery verification
- **Client Resilience**: Graceful degradation

## Continuous Integration

### GitHub Actions Integration
```yaml
- name: Run Integration Tests
  run: ./gradlew integrationTest
  env:
    SPRING_PROFILES_ACTIVE: integration-test
    TESTCONTAINERS_REUSE_ENABLE: false
```

### Test Reports
- **JaCoCo Coverage**: Minimum 80% cross-service coverage
- **Performance Reports**: Timing and throughput metrics
- **Failure Analysis**: Detailed error reporting
- **Trend Analysis**: Performance over time

## Troubleshooting

### Common Issues

**TestContainers Startup Failures**:
```bash
# Check Docker availability
docker ps

# Clean up containers
docker system prune -f

# Increase timeout
export TESTCONTAINERS_STARTUP_TIMEOUT=120
```

**Port Conflicts**:
```bash
# Check port usage
netstat -tulpn | grep :8080

# Kill conflicting processes
sudo kill -9 $(lsof -t -i:8080)
```

**Memory Issues**:
```bash
# Increase JVM heap
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"

# Monitor memory usage
docker stats
```

### Debug Configuration
```yaml
# application-integration-test.yml
logging:
  level:
    com.focushive: DEBUG
    org.springframework.web: DEBUG
    org.testcontainers: INFO
```

## Best Practices

### Test Design
1. **Isolation**: Each test class has independent infrastructure
2. **Determinism**: Tests produce consistent results
3. **Performance**: Tests complete within acceptable time limits
4. **Readability**: Clear test names and assertions
5. **Maintainability**: Modular design with reusable components

### Data Management  
1. **Factory Pattern**: Consistent test data creation
2. **Cleanup**: Automatic cleanup after each test
3. **Realistic Data**: Production-like scenarios
4. **Versioning**: Handle schema evolution

### Error Handling
1. **Expected Failures**: Test error scenarios explicitly
2. **Timeouts**: Use appropriate timeouts for async operations
3. **Retry Logic**: Handle transient failures gracefully
4. **Logging**: Provide detailed failure information

## Contributing

### Adding New Tests
1. Extend `AbstractCrossServiceIntegrationTest`
2. Use `TestDataFactory` for test data
3. Follow TDD approach (Red → Green → Refactor)
4. Add performance assertions
5. Include failure scenario testing

### Test Categories
```java
@Tag("cross-service")
@Tag("data-flow") 
@Tag("integration")
@Tag("performance")
```

### Naming Conventions
- Test classes: `*IntegrationTest.java`
- Test methods: `test*Flow()` or `test*_ShouldFail()`
- Display names: Descriptive business scenarios

## Future Enhancements

### Planned Improvements
- [ ] Chaos engineering integration
- [ ] Load testing with JMeter
- [ ] Contract testing with Pact
- [ ] End-to-end user journey tests
- [ ] Multi-region deployment testing
- [ ] Security penetration testing

### Monitoring Integration
- [ ] Prometheus metrics collection
- [ ] Grafana dashboard integration  
- [ ] Alert manager configuration
- [ ] Distributed tracing with Jaeger

---

**Last Updated**: September 13, 2025  
**Version**: 1.0.0  
**Status**: Production Ready

For questions or contributions, please refer to the main project documentation.