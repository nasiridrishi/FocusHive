# Service Consolidation & Extraction Plan (MERGE.md)

## Overview
This document provides a comprehensive, step-by-step plan to consolidate duplicate service implementations and extract appropriate services as microservices, following the principle: **Test → Verify → Proceed**.

## Architecture Goals
1. **Keep in Backend Monolith**: Analytics, Chat, Forum (already working)
2. **Extract as Microservices**: Buddy and Notification services
3. **Keep Separate**: Identity Service (already separate)

## Pre-Migration Health Check

### Task 0: Baseline Testing ✅
**Goal**: Establish current working state before any changes

#### Test Criteria:
```bash
# 0.1 Backend Health Check
curl -X GET http://localhost:8080/api/health
# Expected: {"status":"UP"}

# 0.2 Identity Service Health Check
curl -X GET http://localhost:8081/actuator/health
# Expected: {"status":"UP"}

# 0.3 Test Core Endpoints
curl -X GET http://localhost:8080/api/hives
# Expected: 200 OK or 401 (authentication required)

# 0.4 Run Existing Tests
cd /Users/nasir/uol/focushive/services/focushive-backend
./gradlew test
# Expected: All tests pass (890+ tests)

# 0.5 Document Current Coverage
./gradlew jacocoTestReport
# Record: Current coverage percentage (92% backend)
```

#### Completion Criteria:
- [ ] All health checks return UP
- [ ] Backend tests pass (890+ tests)
- [ ] Coverage baseline documented
- [ ] Create backup of current working state

---

## Phase 1: Clean Up Unused Standalone Services

### Task 1.1: Archive Analytics Standalone ✅
**Goal**: Remove unused analytics-service standalone implementation

#### Pre-Task Validation:
```bash
# Verify analytics is working in backend
curl -X GET http://localhost:8080/api/analytics/health
# Expected: Service responds

# Check for any references to standalone analytics service
grep -r "analytics-service" /Users/nasir/uol/focushive/services/focushive-backend/
# Expected: No references to port 8085
```

#### Migration Steps:
```bash
# 1. Create archive directory
mkdir -p /Users/nasir/uol/focushive/archived-services

# 2. Move standalone analytics service
mv /Users/nasir/uol/focushive/services/analytics-service \
   /Users/nasir/uol/focushive/archived-services/

# 3. Remove from docker-compose if present
# Edit docker-compose.yml and remove analytics-service entry
```

#### Test Criteria:
```bash
# Analytics endpoints still work via backend
curl -X GET http://localhost:8080/api/analytics/users/test-user/stats
# Expected: 200 OK or valid error

# Run analytics-specific tests
./gradlew test --tests "*Analytics*"
# Expected: All pass
```

#### Completion Criteria:
- [ ] Analytics endpoints work via backend (port 8080)
- [ ] No broken imports or references
- [ ] Analytics tests pass
- [ ] Standalone service archived

---

### Task 1.2: Archive Chat Standalone ✅
**Goal**: Remove unused chat-service standalone implementation

#### Pre-Task Validation:
```bash
# Verify chat is working in backend
curl -X GET http://localhost:8080/api/chat/health
# Expected: Service responds

# Test WebSocket connection
wscat -c ws://localhost:8080/ws
# Expected: Connection established
```

#### Migration Steps:
```bash
# 1. Move standalone chat service
mv /Users/nasir/uol/focushive/services/chat-service \
   /Users/nasir/uol/focushive/archived-services/

# 2. Verify WebSocket STOMP endpoints
# Check /topic/chat/{hiveId} subscription works
```

#### Test Criteria:
```bash
# Chat endpoints work via backend
curl -X GET http://localhost:8080/api/chat/hives/test-hive/messages
# Expected: 200 OK or valid error

# Run chat-specific tests
./gradlew test --tests "*Chat*"
# Expected: All pass
```

#### Completion Criteria:
- [ ] Chat endpoints work via backend
- [ ] WebSocket messaging functional
- [ ] Chat tests pass
- [ ] Standalone service archived

---

### Task 1.3: Archive Forum Standalone ✅
**Goal**: Remove unused forum-service standalone implementation

#### Pre-Task Validation:
```bash
# Verify forum is working in backend
curl -X GET http://localhost:8080/api/forum/categories
# Expected: Service responds
```

#### Migration Steps:
```bash
# 1. Move standalone forum service
mv /Users/nasir/uol/focushive/services/forum-service \
   /Users/nasir/uol/focushive/archived-services/
```

#### Test Criteria:
```bash
# Forum endpoints work via backend
curl -X GET http://localhost:8080/api/forum/posts
# Expected: 200 OK or valid error

# Run forum-specific tests
./gradlew test --tests "*Forum*"
# Expected: All pass
```

#### Completion Criteria:
- [ ] Forum endpoints work via backend
- [ ] Forum tests pass
- [ ] Standalone service archived

---

## Phase 2: Extract Notification Service as Microservice

### Task 2.1: Prepare Notification Service ✅
**Goal**: Set up notification-service as independent microservice on port 8083

#### Pre-Task Analysis:
```bash
# Verify standalone is more complete
echo "Standalone files: $(find /Users/nasir/uol/focushive/services/notification-service -name "*.java" | wc -l)"
# Expected: 65 files

echo "Backend module files: $(find /Users/nasir/uol/focushive/services/focushive-backend/src/main/java/com/focushive/notification -name "*.java" | wc -l)"
# Expected: 14 files
```

#### Migration Steps:
```bash
# 1. Configure notification-service application.yml
cat > /Users/nasir/uol/focushive/services/notification-service/src/main/resources/application.yml << 'EOF'
server:
  port: 8083

spring:
  application:
    name: notification-service
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/focushive_notification}

# Email configuration
mail:
  smtp:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}

# SMS configuration (Twilio)
twilio:
  account-sid: ${TWILIO_ACCOUNT_SID:}
  auth-token: ${TWILIO_AUTH_TOKEN:}

# Push notifications (Firebase)
firebase:
  config-path: ${FIREBASE_CONFIG_PATH:}
EOF

# 2. Build and test
cd /Users/nasir/uol/focushive/services/notification-service
./gradlew build
```

#### Test Criteria:
```bash
# Start notification service
./gradlew bootRun &

# Health check
curl -X GET http://localhost:8083/actuator/health
# Expected: {"status":"UP"}

# Test notification endpoints
curl -X POST http://localhost:8083/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","type":"email","message":"test"}'
# Expected: 200 OK
```

#### Completion Criteria:
- [ ] Notification service starts on port 8083
- [ ] Health check passes
- [ ] Email provider configured
- [ ] SMS provider configured (optional)
- [ ] Push notifications configured (optional)

---

### Task 3.2: Integrate Notification Service with Backend ✅
**Goal**: Configure backend to use external Notification Service

#### Migration Steps:
```java
// 1. Create NotificationServiceClient.java
@FeignClient(
    name = "notification-service",
    url = "${notification.service.url:http://localhost:8083}"
)
public interface NotificationServiceClient {
    @PostMapping("/api/v1/notifications")
    void sendNotification(@RequestBody NotificationRequest request);

    @GetMapping("/api/v1/notifications/user/{userId}")
    List<NotificationDto> getUserNotifications(@PathVariable String userId);
}

// 2. Create NotificationIntegrationService.java
@Service
public class NotificationIntegrationService {
    @Async
    @CircuitBreaker(name = "notification-service")
    public void sendNotification(NotificationRequest request) {
        notificationServiceClient.sendNotification(request);
    }
}
```

#### Event Integration:
```java
// Update event listeners to use external service
@EventListener
public void handleUserJoinedHive(UserJoinedHiveEvent event) {
    notificationIntegrationService.sendNotification(
        NotificationRequest.builder()
            .userId(event.getUserId())
            .type("HIVE_JOINED")
            .title("Welcome to " + event.getHiveName())
            .build()
    );
}
```

#### Test Criteria:
```bash
# Test notification flow
# 1. Trigger an event (e.g., join hive)
# 2. Check notification received
curl -X GET http://localhost:8083/api/v1/notifications/user/test-user
# Expected: Notification present
```

#### Completion Criteria:
- [ ] Backend sends notifications via external service
- [ ] Event-driven notifications working
- [ ] Async processing configured
- [ ] Circuit breaker protecting backend

---

### Task 3.3: Remove Backend Notification Module ✅
**Goal**: Complete notification service extraction

#### Migration Steps:
```bash
# 1. Remove notification module from backend
rm -rf /Users/nasir/uol/focushive/services/focushive-backend/src/main/java/com/focushive/notification

# 2. Update all event listeners to use NotificationIntegrationService

# 3. Update application.yml
# Set app.features.notification.external: true
```

#### Test Criteria:
```bash
# Test all notification triggers:
# - User registration
# - Hive join/leave
# - Buddy match
# - Achievement unlock
# - Focus session complete

# Each should create notification via external service
```

#### Completion Criteria:
- [ ] Backend module removed
- [ ] All notification triggers working
- [ ] No broken imports
- [ ] External service handling all notifications

---

## Phase 3: Extract Buddy Service as Microservice

### Task 3.1: Prepare Buddy Service ✅
**Goal**: Set up buddy-service as independent microservice on port 8087

#### Pre-Task Analysis:
```bash
# Compare implementations
echo "Standalone files: $(find /Users/nasir/uol/focushive/services/buddy-service -name "*.java" | wc -l)"
# Expected: 90 files

echo "Backend module files: $(find /Users/nasir/uol/focushive/services/focushive-backend/src/main/java/com/focushive/buddy -name "*.java" | wc -l)"
# Expected: 26 files
```

#### Migration Steps:
```bash
# 1. Configure buddy-service application.yml
cat > /Users/nasir/uol/focushive/services/buddy-service/src/main/resources/application.yml << 'EOF'
server:
  port: 8087

spring:
  application:
    name: buddy-service
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/focushive_buddy}
    username: ${DATABASE_USERNAME:focushive}
    password: ${DATABASE_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: update

# Identity service integration
identity:
  service:
    url: ${IDENTITY_SERVICE_URL:http://localhost:8081}

# Redis for caching
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
EOF

# 2. Add Feign client for Identity Service
# Copy from backend: IdentityServiceClient.java

# 3. Build and test standalone
cd /Users/nasir/uol/focushive/services/buddy-service
./gradlew build
```

#### Test Criteria:
```bash
# Start buddy service
./gradlew bootRun &

# Health check
curl -X GET http://localhost:8087/actuator/health
# Expected: {"status":"UP"}

# Test buddy endpoints
curl -X GET http://localhost:8087/api/v1/buddy/matches
# Expected: 200 OK or 401
```

#### Completion Criteria:
- [ ] Buddy service starts on port 8087
- [ ] Health check passes
- [ ] Can connect to database
- [ ] Can communicate with Identity Service

---

### Task 3.2: Integrate Buddy Service with Backend ✅
**Goal**: Configure backend to use external Buddy Service

#### Migration Steps:
```java
// 1. Create BuddyServiceClient.java in backend
@FeignClient(
    name = "buddy-service",
    url = "${buddy.service.url:http://localhost:8087}"
)
public interface BuddyServiceClient {
    @GetMapping("/api/v1/buddy/matches")
    List<PotentialMatchDto> getMatches(@RequestHeader("X-User-ID") String userId);

    // Add other buddy endpoints
}

// 2. Create BuddyIntegrationService.java
@Service
public class BuddyIntegrationService {
    @CircuitBreaker(name = "buddy-service")
    @Retry(name = "buddy-service")
    public List<PotentialMatchDto> getMatches(String userId) {
        return buddyServiceClient.getMatches(userId);
    }
}
```

#### Configuration:
```yaml
# Add to backend application.yml
buddy:
  service:
    url: ${BUDDY_SERVICE_URL:http://localhost:8087}

# Add resilience4j config for buddy-service
resilience4j:
  circuitbreaker:
    instances:
      buddy-service:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
```

#### Test Criteria:
```bash
# Test via backend proxy endpoints
curl -X GET http://localhost:8080/api/buddy/matches \
  -H "Authorization: Bearer <token>"
# Expected: Proxied to buddy service

# Test circuit breaker (stop buddy service and try)
# Expected: Fallback response
```

#### Completion Criteria:
- [ ] Backend can communicate with Buddy Service
- [ ] Circuit breaker configured
- [ ] Retry logic working
- [ ] All buddy features accessible

---

### Task 3.3: Migrate Buddy Data & Remove Backend Module ✅
**Goal**: Complete buddy service extraction

#### Data Migration:
```sql
-- If buddy data exists in main database, migrate to buddy database
-- Export buddy-related tables
pg_dump -h localhost -U focushive -d focushive \
  -t buddy_partnerships -t buddy_checkins -t buddy_goals \
  > buddy_data.sql

-- Import to buddy database
psql -h localhost -U focushive -d focushive_buddy < buddy_data.sql
```

#### Remove Backend Module:
```bash
# 1. Remove buddy module from backend
rm -rf /Users/nasir/uol/focushive/services/focushive-backend/src/main/java/com/focushive/buddy

# 2. Update application.yml
# Set app.features.buddy.external: true
```

#### Test Criteria:
```bash
# Full buddy flow test
# 1. Create buddy request
# 2. Match buddies
# 3. Create check-in
# 4. Track goals

# Run integration tests
./gradlew test --tests "*BuddyIntegration*"
# Expected: All pass
```

#### Completion Criteria:
- [ ] Data migrated successfully
- [ ] Backend module removed
- [ ] All buddy features work via external service
- [ ] No broken imports or references

---

## Phase 4: Final Integration Testing

### Task 4.1: End-to-End Testing ✅
**Goal**: Verify complete system functionality

#### Test Scenarios:
```bash
# 1. User Registration Flow
# - Register via Identity Service
# - Create profile
# - Receive welcome notification

# 2. Hive Creation and Join
# - Create hive (backend)
# - Join hive (backend)
# - Real-time presence (backend WebSocket)
# - Chat in hive (backend)

# 3. Focus Session
# - Start timer (backend)
# - Complete session (backend)
# - Update analytics (backend)
# - Unlock achievement (backend)
# - Send notification (notification service)

# 4. Buddy Matching
# - Request buddy (buddy service)
# - Get matched (buddy service)
# - Schedule session (buddy service)
# - Check-in (buddy service)

# 5. Forum Activity
# - Create post (backend)
# - Add reply (backend)
# - Vote on content (backend)
```

#### Performance Testing:
```bash
# Load test each service
ab -n 1000 -c 10 http://localhost:8080/api/health
ab -n 1000 -c 10 http://localhost:8081/api/health
ab -n 1000 -c 10 http://localhost:8083/api/health
ab -n 1000 -c 10 http://localhost:8087/api/health

# Expected: <100ms response time for health checks
```

#### Completion Criteria:
- [ ] All user flows working
- [ ] Performance benchmarks met
- [ ] No errors in logs
- [ ] All services healthy

---

### Task 4.2: Documentation Update ✅
**Goal**: Update all documentation to reflect new architecture

#### Documentation Tasks:
```markdown
1. Update BACKEND_ARCHITECTURE.md
   - Remove duplicate service mentions
   - Clarify monolith vs microservice split

2. Update API_REFERENCE.md
   - Mark buddy endpoints as external (port 8087)
   - Mark notification endpoints as external (port 8083)

3. Update docker-compose.yml
   - Add buddy-service container
   - Add notification-service container
   - Remove unused service containers

4. Update README.md
   - New architecture diagram
   - Service port mapping
   - Development setup instructions
```

#### Completion Criteria:
- [ ] Architecture docs accurate
- [ ] API docs updated
- [ ] Docker compose working
- [ ] README reflects reality

---

## Phase 5: Cleanup & Optimization

### Task 5.1: Remove Archived Services ✅
**Goal**: Clean up archived standalone services

#### Verification Steps:
```bash
# 1. Ensure everything works without archived services
# Run full test suite
./gradlew test

# 2. Check no references remain
grep -r "analytics-service\|chat-service\|forum-service" /Users/nasir/uol/focushive/

# 3. If all clear, permanently remove
rm -rf /Users/nasir/uol/focushive/archived-services/
```

#### Completion Criteria:
- [ ] No references to removed services
- [ ] All tests pass
- [ ] Archived services deleted

---

### Task 5.2: Optimize Configuration ✅
**Goal**: Clean up configuration files

#### Configuration Updates:
```yaml
# backend application.yml - Final state
app:
  features:
    # Internal modules (monolith)
    forum: true
    analytics: true
    chat: true
    timer: true
    presence: true
    hive: true

    # External services
    buddy:
      enabled: true
      external: true
      url: http://localhost:8087

    notification:
      enabled: true
      external: true
      url: http://localhost:8083

    identity:
      enabled: true
      external: true
      url: http://localhost:8081
```

#### Completion Criteria:
- [ ] Configuration clean and clear
- [ ] No obsolete settings
- [ ] Environment variables documented

---

## Rollback Plan

### If Any Phase Fails:

#### Quick Rollback:
```bash
# 1. Restore archived services
mv /Users/nasir/uol/focushive/archived-services/* \
   /Users/nasir/uol/focushive/services/

# 2. Revert configuration changes
git checkout -- application.yml

# 3. Restart backend
./gradlew bootRun
```

#### Data Rollback:
```bash
# If data migration fails
pg_dump focushive_buddy > buddy_backup.sql
psql focushive < original_backup.sql
```

---

## Success Metrics

### Final Validation Checklist:
- [ ] Backend monolith running (port 8080)
- [ ] Identity service running (port 8081)
- [ ] Notification service running (port 8083)
- [ ] Buddy service running (port 8087)
- [ ] All 890+ backend tests passing
- [ ] WebSocket real-time features working
- [ ] No duplicate code or services
- [ ] Clean architecture documentation
- [ ] Performance benchmarks met
- [ ] Zero production errors

### Architecture Achievement:
```
BEFORE: Confusing mix of duplicate implementations
AFTER:  Clean separation of concerns
        - Monolith: Core business logic
        - Microservices: Specialized, scalable services
```

## Timeline Estimate

- **Phase 1**: 2 hours (cleanup unused services)
- **Phase 2**: 4 hours (extract buddy service)
- **Phase 3**: 4 hours (extract notification service)
- **Phase 4**: 2 hours (integration testing)
- **Phase 5**: 1 hour (cleanup)

**Total**: ~13 hours of careful, tested migration

## Important Notes

⚠️ **NEVER** proceed to next task without completing ALL test criteria
⚠️ **ALWAYS** have rollback plan ready before making changes
⚠️ **TEST** after every single step - no assumptions
⚠️ **BACKUP** data before any migration
⚠️ **MONITOR** logs during and after each phase