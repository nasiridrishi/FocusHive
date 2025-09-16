# FocusHive API Usage Guide

## Overview

The FocusHive API provides comprehensive endpoints for managing a digital co-working and co-studying platform. This guide covers common usage patterns, authentication, and real-time features.

## Base URL

- **Development**: `http://localhost:8080`
- **Production**: `https://api.focushive.com`
- **Staging**: `https://staging-api.focushive.com`

## Authentication

### JWT Token Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Public Endpoints

The following endpoints are publicly accessible:
- `GET /actuator/health` - Health check
- `GET /swagger-ui.html` - API documentation
- `GET /api-docs` - OpenAPI specification
- `POST /api/v1/auth/**` - Authentication endpoints

## Core API Patterns

### 1. Hive Management

#### Create a New Hive
```http
POST /api/v1/hives
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Study Group",
  "description": "Computer Science study sessions",
  "maxMembers": 10,
  "isPrivate": false,
  "category": "STUDY"
}
```

#### Join a Hive
```http
POST /api/v1/hives/{hiveId}/join
Authorization: Bearer <token>
```

#### Get Hive Members
```http
GET /api/v1/hives/{hiveId}/members
Authorization: Bearer <token>
```

### 2. Presence Tracking

#### Update User Presence
```http
POST /api/v1/presence/update
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "IN_FOCUS_SESSION",
  "hiveId": "hive-123",
  "activity": "Working on project"
}
```

#### Get Hive Presence
```http
GET /api/v1/presence/hive/{hiveId}
Authorization: Bearer <token>
```

### 3. Focus Timer

#### Start Focus Session
```http
POST /api/v1/timer/start
Authorization: Bearer <token>
Content-Type: application/json

{
  "duration": 1500,
  "sessionType": "FOCUS",
  "hiveId": "hive-123",
  "description": "Deep work on API documentation"
}
```

#### Get Active Session
```http
GET /api/v1/timer/active
Authorization: Bearer <token>
```

### 4. Chat System

#### Send Message
```http
POST /api/chat/messages
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Hello everyone!",
  "hiveId": "hive-123",
  "messageType": "TEXT"
}
```

#### Get Recent Messages
```http
GET /api/chat/hives/{hiveId}/messages?page=0&size=20
Authorization: Bearer <token>
```

### 5. Analytics

#### Get User Analytics
```http
GET /api/v1/analytics/user/productivity?startDate=2025-09-01&endDate=2025-09-15
Authorization: Bearer <token>
```

#### Get Hive Analytics
```http
GET /api/v1/analytics/hive/{hiveId}/summary?period=WEEK
Authorization: Bearer <token>
```

## WebSocket Real-Time Features

### Connection

Connect to WebSocket endpoint:
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer ' + token
}, function(frame) {
    console.log('Connected: ' + frame);
});
```

### Subscriptions

#### Hive Updates
```javascript
stompClient.subscribe('/topic/hive/' + hiveId, function(message) {
    const update = JSON.parse(message.body);
    console.log('Hive update:', update);
});
```

#### Chat Messages
```javascript
stompClient.subscribe('/topic/chat/' + hiveId, function(message) {
    const chatMessage = JSON.parse(message.body);
    console.log('New message:', chatMessage);
});
```

#### Presence Updates
```javascript
stompClient.subscribe('/topic/presence/' + hiveId, function(message) {
    const presenceUpdate = JSON.parse(message.body);
    console.log('Presence update:', presenceUpdate);
});
```

#### Timer Sync
```javascript
stompClient.subscribe('/user/topic/timer/sync', function(message) {
    const timerSync = JSON.parse(message.body);
    console.log('Timer sync:', timerSync);
});
```

### Sending Messages

#### Send Chat Message
```javascript
stompClient.send('/app/chat/send', {}, JSON.stringify({
    content: 'Hello from WebSocket!',
    hiveId: 'hive-123'
}));
```

#### Update Presence
```javascript
stompClient.send('/app/presence/update', {}, JSON.stringify({
    status: 'ONLINE',
    hiveId: 'hive-123',
    activity: 'Coding'
}));
```

## Error Handling

### Standard Error Response
```json
{
  "error": "ValidationError",
  "message": "Invalid request parameters",
  "timestamp": "2025-09-15T12:00:00Z",
  "path": "/api/v1/hives",
  "details": {
    "field": "name",
    "message": "Hive name cannot be empty"
  }
}
```

### Common HTTP Status Codes

- `200 OK` - Success
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

## Rate Limiting

API requests are rate-limited based on:
- **Per User**: 100 requests per minute
- **Per IP**: 1000 requests per hour
- **WebSocket**: 50 messages per minute

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 85
X-RateLimit-Reset: 1694779200
```

## Pagination

List endpoints support pagination:

```http
GET /api/v1/hives?page=0&size=20&sort=createdAt,desc
```

Response includes pagination metadata:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "orders": [{"property": "createdAt", "direction": "DESC"}]
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

## Health and Monitoring

### Health Check
```http
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "hiveService": {"status": "UP"},
    "presenceService": {"status": "UP"},
    "webSocket": {"status": "UP"}
  }
}
```

## SDKs and Libraries

### JavaScript/TypeScript
```bash
npm install @focushive/api-client
```

### Python
```bash
pip install focushive-api-client
```

### Java
```xml
<dependency>
    <groupId>com.focushive</groupId>
    <artifactId>api-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Support

- **Documentation**: [https://docs.focushive.com](https://docs.focushive.com)
- **API Reference**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Support Email**: support@focushive.com
- **GitHub**: [https://github.com/focushive](https://github.com/focushive)

## Changelog

### v1.0.0 (2025-09-15)
- Initial API release
- Complete hive management system
- Real-time presence tracking
- Focus timer with Pomodoro support
- Chat system with WebSocket
- Analytics and gamification
- Forum and community features
- Buddy system for accountability
- Comprehensive monitoring and health checks