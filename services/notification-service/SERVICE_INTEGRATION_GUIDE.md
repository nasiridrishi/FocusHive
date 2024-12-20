# Notification Service Integration Guide

This document provides comprehensive guidance for integrating with the FocusHive Notification Service, including authentication, API usage, and best practices.

## Service Overview

The Notification Service is a centralized microservice that handles all notification delivery across the FocusHive platform. It provides:
- Multi-channel notification delivery (Email, In-App, Push, SMS)
- Template-based message rendering
- User preference management
- Delivery tracking and analytics
- Asynchronous processing via RabbitMQ

## Service Access

### Public URL (via Cloudflared)
- **Base URL**: `https://notification.focushive.app`
- **Health Check**: `https://notification.focushive.app/actuator/health`
- **API Documentation**: `https://notification.focushive.app/swagger-ui.html`

### Internal Docker Network
- **Service Name**: `focushive-notification-service-app`
- **Port**: 8083
- **Network**: `focushive-shared-network`

## Authentication

The Notification Service uses JWT-based authentication, integrated with the Identity Service.

### JWT Token Requirements
- **Algorithm**: RS256 (RSA with SHA-256)
- **Issuer**: `https://identity.focushive.app/identity`
- **JWK Set URI**: `https://identity.focushive.app/.well-known/jwks.json`

### Required Token Claims
```json
{
  "sub": "user-id-123",
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "iss": "https://identity.focushive.app/identity",
  "exp": 1234567890,
  "iat": 1234567890
}
```

### Authorization Header
```http
Authorization: Bearer <jwt-token>
```

## API Endpoints

### 1. Create Notification

**Endpoint**: `POST /api/v1/notifications`

**Request Body**:
```json
{
  "userId": "user-123",
  "type": "SYSTEM_ALERT",
  "title": "Important Update",
  "content": "Your focus session has been completed",
  "priority": "HIGH",
  "data": {
    "sessionId": "session-456",
    "duration": 25
  }
}
```

**Response**:
```json
{
  "id": "notif-789",
  "userId": "user-123",
  "type": "SYSTEM_ALERT",
  "title": "Important Update",
  "isRead": false,
  "createdAt": "2025-09-21T16:00:00Z"
}
```

### 2. Get User Notifications

**Endpoint**: `GET /api/v1/notifications`

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 20)
- `isRead` (optional: true/false)
- `type` (optional: notification type)

**Response**:
```json
{
  "content": [
    {
      "id": "notif-789",
      "type": "SYSTEM_ALERT",
      "title": "Important Update",
      "content": "Your focus session has been completed",
      "isRead": false,
      "createdAt": "2025-09-21T16:00:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

### 3. Mark Notification as Read

**Endpoint**: `PUT /api/v1/notifications/{id}/read`

**Response**:
```json
{
  "id": "notif-789",
  "isRead": true,
  "readAt": "2025-09-21T16:05:00Z"
}
```

### 4. Delete Notification

**Endpoint**: `DELETE /api/v1/notifications/{id}`

**Response**: `204 No Content`

### 5. Get Notification Preferences

**Endpoint**: `GET /api/v1/preferences`

**Response**:
```json
{
  "userId": "user-123",
  "emailEnabled": true,
  "pushEnabled": false,
  "inAppEnabled": true,
  "frequency": "IMMEDIATE",
  "quietHours": {
    "enabled": true,
    "startTime": "22:00",
    "endTime": "08:00"
  }
}
```

### 6. Update Notification Preferences

**Endpoint**: `PUT /api/v1/preferences`

**Request Body**:
```json
{
  "emailEnabled": true,
  "pushEnabled": true,
  "frequency": "DAILY_DIGEST",
  "quietHours": {
    "enabled": true,
    "startTime": "23:00",
    "endTime": "07:00"
  }
}
```

## Integration via RabbitMQ

For asynchronous notification processing, services can publish messages to RabbitMQ.

### Queue Configuration
- **Exchange**: `focushive.notifications`
- **Queue**: `notifications`
- **Routing Key**: `notification.created`

### Message Format
```json
{
  "userId": "user-123",
  "type": "HIVE_INVITATION",
  "templateId": "hive-invite-v1",
  "variables": {
    "inviterName": "John Doe",
    "hiveName": "Study Group Alpha"
  },
  "channels": ["EMAIL", "IN_APP"],
  "priority": "NORMAL"
}
```

## Service-to-Service Integration Examples

### Example 1: Identity Service Integration

```java
// IdentityService.java
@Service
@RequiredArgsConstructor
public class IdentityNotificationService {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:https://notification.focushive.app}")
    private String notificationServiceUrl;

    public void sendWelcomeEmail(String userId, String email, String name) {
        String url = notificationServiceUrl + "/api/v1/notifications";

        NotificationRequest request = NotificationRequest.builder()
            .userId(userId)
            .type("USER_REGISTERED")
            .title("Welcome to FocusHive!")
            .content("Welcome " + name + "! Your account has been created.")
            .priority("HIGH")
            .data(Map.of(
                "email", email,
                "registrationDate", Instant.now().toString()
            ))
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getServiceToken()); // Internal service token

        HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<NotificationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                NotificationResponse.class
            );

            log.info("Welcome notification sent: {}", response.getBody().getId());
        } catch (Exception e) {
            log.error("Failed to send welcome notification", e);
            // Implement fallback or retry logic
        }
    }

    public void sendPasswordResetEmail(String userId, String email, String resetToken) {
        // Similar implementation for password reset
    }

    public void sendSecurityAlert(String userId, String alertType, Map<String, Object> details) {
        // Similar implementation for security alerts
    }
}
```

### Example 2: Buddy Service Integration

```java
// BuddyService.java
@Service
@RequiredArgsConstructor
public class BuddyNotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notifications:focushive.notifications}")
    private String notificationExchange;

    @Value("${rabbitmq.routingkey.notification:notification.created}")
    private String notificationRoutingKey;

    public void sendBuddyRequest(String fromUserId, String toUserId, String fromUserName) {
        NotificationMessage message = NotificationMessage.builder()
            .userId(toUserId)
            .type("BUDDY_REQUEST")
            .templateId("buddy-request-v1")
            .variables(Map.of(
                "senderName", fromUserName,
                "senderId", fromUserId,
                "timestamp", Instant.now().toString()
            ))
            .channels(List.of("EMAIL", "IN_APP", "PUSH"))
            .priority("HIGH")
            .build();

        rabbitTemplate.convertAndSend(
            notificationExchange,
            notificationRoutingKey,
            message
        );

        log.info("Buddy request notification queued for user: {}", toUserId);
    }

    public void sendBuddyAccepted(String userId, String buddyName) {
        // Similar implementation
    }
}
```

### Example 3: FocusHive Backend Integration

```java
// FocusHiveBackendService.java
@Service
@RequiredArgsConstructor
public class HiveNotificationService {

    private final WebClient notificationClient;

    @PostConstruct
    public void init() {
        this.notificationClient = WebClient.builder()
            .baseUrl("https://notification.focushive.app")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Mono<NotificationResponse> sendHiveInvitation(
            String inviterId,
            String inviteeId,
            String hiveName,
            String jwtToken) {

        NotificationRequest request = NotificationRequest.builder()
            .userId(inviteeId)
            .type("HIVE_INVITATION")
            .title("You've been invited to join " + hiveName)
            .content("Join your colleagues in a focused work session")
            .actionUrl("/hives/join/" + hiveName)
            .priority("NORMAL")
            .data(Map.of(
                "inviterId", inviterId,
                "hiveName", hiveName
            ))
            .build();

        return notificationClient.post()
            .uri("/api/v1/notifications")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            .body(Mono.just(request), NotificationRequest.class)
            .retrieve()
            .bodyToMono(NotificationResponse.class)
            .doOnSuccess(response ->
                log.info("Hive invitation sent: {}", response.getId()))
            .doOnError(error ->
                log.error("Failed to send hive invitation", error));
    }

    public Mono<Void> notifySessionComplete(
            String userId,
            String sessionId,
            Duration duration,
            String jwtToken) {
        // Implementation for session completion notification
    }
}
```

## Notification Types

The service supports the following notification types:

```java
public enum NotificationType {
    // Hive-related
    HIVE_INVITATION,
    HIVE_JOINED,
    HIVE_LEFT,

    // Task-related
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TASK_REMINDER,

    // Achievement-related
    ACHIEVEMENT_UNLOCKED,
    MILESTONE_REACHED,

    // Buddy-related
    BUDDY_REQUEST,
    BUDDY_REQUEST_ACCEPTED,
    BUDDY_REQUEST_DECLINED,

    // Session-related
    SESSION_REMINDER,
    SESSION_STARTED,
    SESSION_COMPLETED,

    // System
    SYSTEM_ALERT,
    MAINTENANCE_NOTICE,
    FEATURE_ANNOUNCEMENT,

    // Security
    LOGIN_ALERT,
    PASSWORD_CHANGED,
    SECURITY_WARNING
}
```

## Error Handling

### Common Error Responses

**401 Unauthorized**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required to access this resource",
  "path": "/api/v1/notifications"
}
```

**403 Forbidden**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions",
  "path": "/api/v1/notifications/admin"
}
```

**400 Bad Request**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid notification type: INVALID_TYPE",
  "path": "/api/v1/notifications"
}
```

**429 Too Many Requests**
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 60 seconds",
  "retryAfter": 60
}
```

## Rate Limiting

The service implements rate limiting to prevent abuse:

- **Read Operations**: 100 requests per minute
- **Write Operations**: 50 requests per minute
- **Admin Operations**: 20 requests per minute

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

## Best Practices

### 1. Use Bulk Operations
When sending notifications to multiple users, use bulk endpoints:
```java
POST /api/v1/notifications/bulk
```

### 2. Implement Retry Logic
Use exponential backoff for failed requests:
```java
@Retryable(
    value = {RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendNotification(NotificationRequest request) {
    // Implementation
}
```

### 3. Cache User Preferences
Cache notification preferences to reduce API calls:
```java
@Cacheable(value = "user-preferences", key = "#userId")
public NotificationPreferences getUserPreferences(String userId) {
    // Fetch from notification service
}
```

### 4. Use Appropriate Priority Levels
- **URGENT**: Critical alerts, security issues
- **HIGH**: Important user actions, buddy requests
- **NORMAL**: Regular notifications, updates
- **LOW**: Informational messages, tips

### 5. Respect User Preferences
Always check user preferences before sending notifications:
```java
if (preferences.isEmailEnabled() && !preferences.isInQuietHours()) {
    sendEmailNotification(user);
}
```

## Monitoring and Observability

### Health Endpoints
- `/actuator/health` - Overall service health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

### Metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/metrics` - Application metrics

### Key Metrics to Monitor
- `notifications.sent.total` - Total notifications sent
- `notifications.failed.total` - Failed notifications
- `notifications.delivery.time` - Delivery time histogram
- `notifications.queue.depth` - RabbitMQ queue depth

## Troubleshooting

### Common Issues

1. **JWT Token Validation Failures**
   - Ensure token is not expired
   - Verify issuer matches expected value
   - Check JWK Set URI is accessible

2. **Email Delivery Failures**
   - Verify SMTP credentials
   - Check email address validity
   - Review spam folder settings

3. **RabbitMQ Connection Issues**
   - Verify RabbitMQ is running
   - Check network connectivity
   - Review queue bindings

4. **Rate Limiting**
   - Implement client-side rate limiting
   - Use bulk operations where possible
   - Cache responses when appropriate

## Security Considerations

1. **Always use HTTPS** for API calls
2. **Validate JWT tokens** on every request
3. **Sanitize user input** to prevent XSS attacks
4. **Use parameterized queries** for database operations
5. **Implement request signing** for service-to-service calls
6. **Rotate service credentials** regularly
7. **Monitor for suspicious activity** patterns

## Migration Guide

For services migrating from direct email/notification handling:

1. **Phase 1**: Integrate notification service for new notifications
2. **Phase 2**: Migrate existing notification logic
3. **Phase 3**: Remove legacy notification code
4. **Phase 4**: Update monitoring and alerting

## Support and Contact

- **Service Owner**: FocusHive Platform Team
- **Documentation**: https://notification.focushive.app/swagger-ui.html
- **Health Status**: https://notification.focushive.app/actuator/health
- **Issue Tracking**: GitHub Issues (focushive/notification-service)

## Appendix

### A. Environment Variables

```bash
# Service URLs
NOTIFICATION_SERVICE_URL=https://notification.focushive.app
IDENTITY_SERVICE_URL=https://identity.focushive.app

# RabbitMQ Configuration
RABBITMQ_HOST=focushive-notification-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=focushive

# JWT Configuration
JWT_ISSUER_URI=https://identity.focushive.app/identity
JWT_JWK_SET_URI=https://identity.focushive.app/.well-known/jwks.json
```

### B. Docker Compose Integration

```yaml
services:
  your-service:
    networks:
      - focushive-shared-network
    environment:
      NOTIFICATION_SERVICE_URL: https://notification.focushive.app
    depends_on:
      - focushive-notification-service-app

networks:
  focushive-shared-network:
    external: true
```

### C. Testing Integration

For testing, use the test endpoints (when enabled):
- `POST /api/test/email` - Send test email
- `POST /api/test/notification` - Create test notification
- `GET /api/test/health` - Check test mode status

Note: Test endpoints are only available when `NOTIFICATION_TEST_ENABLED=true`