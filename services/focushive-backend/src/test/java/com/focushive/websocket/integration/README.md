# WebSocket Integration Tests

This directory contains comprehensive WebSocket integration tests for the FocusHive backend following strict Test-Driven Development (TDD) principles.

## Test Structure

### Test Utilities (`util/`)

1. **WebSocketTestClient.java**
   - Central utility for WebSocket connection management
   - Supports both STOMP and raw WebSocket protocols
   - JWT authentication handling
   - Message capture and verification
   - Connection lifecycle management

2. **StompTestSession.java**
   - Enhanced wrapper for STOMP session testing
   - Type-safe message capture
   - Subscription management
   - Multi-destination support

3. **WebSocketTestUtils.java**
   - Static utility methods for common test operations
   - JWT token generation (mock)
   - Test data creation helpers
   - Latency measurement
   - JSON serialization utilities

### Integration Test Classes

1. **WebSocketConnectionIntegrationTest.java** (11 Tests)
   - Connection establishment with valid/invalid JWT
   - HTTP to WebSocket upgrade verification
   - Different client library support
   - Connection timeout handling
   - Maximum concurrent connections testing
   - Reconnection scenario testing
   - Custom headers support
   - Connection latency measurement

2. **StompRoutingIntegrationTest.java** (10 Tests)
   - STOMP connect frame authentication
   - Topic subscription management (/topic/*)
   - User queue routing (/user/queue/*)
   - Message publishing to topics
   - User-specific message routing
   - Broadcast to all subscribers
   - Topic permission validation
   - Message acknowledgment handling
   - Multiple destination routing
   - STOMP protocol compliance

3. **PresenceSyncIntegrationTest.java** (8 Tests)
   - User joining hive broadcasts
   - Multi-user status synchronization
   - User disconnection notifications
   - Presence list retrieval for new joiners
   - Concurrent user updates handling
   - Presence persistence across reconnections
   - Presence cleanup on timeout
   - High-frequency update handling

4. **WebSocketAuthIntegrationTest.java** (12 Tests)
   - JWT validation during handshake
   - Invalid/expired token rejection
   - Authentication state changes
   - Token refresh scenarios
   - Permission-based topic access
   - Force disconnect on token expiry
   - Role-based channel access
   - Security headers validation
   - Cross-origin request handling
   - Authentication error propagation

5. **WebSocketErrorHandlingIntegrationTest.java** (10 Tests)
   - Graceful disconnect handling
   - Abrupt connection loss recovery
   - Message queue during disconnects
   - Resource cleanup verification
   - Error message propagation
   - Connection limit exceeded handling
   - Heartbeat timeout detection
   - Malformed message handling
   - Network interruption recovery
   - Concurrent error scenarios

### Test Configuration

1. **WebSocketIntegrationTestConfig.java**
   - Test-specific WebSocket configuration
   - Simplified message broker setup
   - STOMP endpoint registration
   - Optimized for testing environment

2. **MockPresenceTrackingService.java**
   - Mock implementation of presence tracking
   - In-memory presence state management
   - Hive membership tracking
   - Focus/buddy session simulation

## Test Features

### TDD Approach
- All tests written before implementation
- Clear test structure: Given → When → Then
- Comprehensive edge case coverage
- Explicit failure scenarios

### Technology Stack
- **Spring Boot Test**: Integration test framework
- **Testcontainers**: PostgreSQL and Redis for realistic testing
- **STOMP Protocol**: WebSocket messaging standard
- **JWT Authentication**: Token-based security testing
- **SLF4J Logging**: Comprehensive test logging

### Coverage Areas

#### Connection Management
- Authentication flows
- Connection lifecycle
- Error handling
- Resource cleanup

#### Message Routing
- Topic/queue distinctions
- User-specific routing
- Broadcast mechanisms
- Permission validation

#### Real-time Features
- Presence synchronization
- Multi-user scenarios
- High-frequency updates
- State persistence

#### Error Resilience
- Network failures
- Malformed data
- Resource limits
- Concurrent operations

## Running the Tests

### Prerequisites
- Java 21
- Docker (for Testcontainers)
- PostgreSQL and Redis containers

### Execution
```bash
# Run all WebSocket integration tests
./gradlew test --tests "com.focushive.websocket.integration.*"

# Run specific test class
./gradlew test --tests "WebSocketConnectionIntegrationTest"

# Run with debug logging
./gradlew test --tests "com.focushive.websocket.integration.*" --debug
```

### Test Profiles
- Tests use `test` profile automatically
- Testcontainers provide isolated database/cache
- Mock services replace external dependencies

## Performance Expectations

### Latency Targets
- Connection establishment: < 5 seconds
- Message round-trip: < 1 second
- Heartbeat response: < 2 seconds

### Concurrency Limits
- Tests validate up to 30 concurrent connections
- High-frequency updates (50ms intervals)
- Resource cleanup verification

### Resource Management
- Automatic connection cleanup
- Memory leak prevention
- Thread pool management

## Integration with Main Application

### Service Dependencies
- **PresenceTrackingService**: User presence management
- **WebSocketConfig**: Connection configuration
- **WebSocketSecurityConfig**: Authentication setup
- **WebSocketEventHandler**: Connection lifecycle events

### Message Flow
1. Client connects with JWT authentication
2. Subscribe to relevant topics/queues
3. Send/receive real-time messages
4. Handle disconnection gracefully

### Error Handling
- Graceful degradation
- Automatic reconnection support
- Comprehensive error reporting
- Resource cleanup guarantees

## Test Maintenance

### Adding New Tests
1. Follow TDD principles (test first)
2. Use existing utilities for common operations
3. Include both positive and negative test cases
4. Document expected behavior clearly

### Updating Tests
- Maintain compatibility with existing utilities
- Update mock services as needed
- Preserve test isolation
- Keep performance expectations realistic

### Debugging Issues
- Enable debug logging for detailed output
- Use message capture utilities for verification
- Verify Testcontainer startup
- Check authentication token generation

---

## Summary

This comprehensive test suite ensures the WebSocket functionality in FocusHive is robust, scalable, and reliable. The tests cover all critical paths, error scenarios, and performance requirements while following industry best practices for integration testing.

Total Test Count: **51 tests** across **5 test classes**
Coverage Areas: **Connection Management, Message Routing, Presence Sync, Authentication, Error Handling**
Testing Approach: **Strict TDD with comprehensive edge case coverage**