# FocusHive API Reference

> Complete API documentation for all FocusHive microservices  
> **Version**: 1.0.0 | **Base URL**: `http://localhost:8080/api`

## Table of Contents

1. [Authentication](#authentication)
2. [Core Services](#core-services)
3. [Real-time WebSocket](#real-time-websocket)
4. [Error Handling](#error-handling)
5. [Rate Limiting](#rate-limiting)

---

## Authentication

### Overview
All API endpoints (except authentication endpoints) require JWT authentication.

### Token Management

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "refresh_token_here",
  "user": {
    "id": "user_123",
    "email": "user@example.com",
    "personas": ["work", "study", "personal"]
  },
  "expiresIn": 3600
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}
```

#### Logout
```http
POST /api/auth/logout
Authorization: Bearer <jwt-token>
```

### Authorization Header
Include in all authenticated requests:
```http
Authorization: Bearer <jwt-token>
```

---

## Core Services

### 1. Hive Management API

#### List Hives
```http
GET /api/hives?page=0&size=20&sort=createdAt,desc
```

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field and direction
- `category` (optional): Filter by category
- `search` (optional): Search term

**Response:**
```json
{
  "content": [
    {
      "id": "hive_123",
      "name": "Deep Focus Zone",
      "description": "A quiet space for deep work",
      "category": "study",
      "maxMembers": 10,
      "currentMembers": 3,
      "isPrivate": false,
      "createdBy": "user_456",
      "createdAt": "2025-09-04T10:00:00Z",
      "tags": ["focus", "study", "quiet"]
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "page": 0
}
```

#### Create Hive
```http
POST /api/hives
Content-Type: application/json

{
  "name": "Morning Productivity",
  "description": "Early bird focus sessions",
  "category": "work",
  "maxMembers": 8,
  "isPrivate": false,
  "tags": ["morning", "productivity"],
  "settings": {
    "pomodoroLength": 25,
    "breakLength": 5,
    "requireCamera": false
  }
}
```

#### Join Hive
```http
POST /api/hives/{hiveId}/join

{
  "message": "Looking forward to working with you all!"
}
```

#### Leave Hive
```http
POST /api/hives/{hiveId}/leave
```

### 2. Presence API

#### Update Presence
```http
POST /api/presence/update
Content-Type: application/json

{
  "hiveId": "hive_123",
  "status": "active",
  "activity": "coding",
  "mood": "focused",
  "currentTask": "Implementing user authentication"
}
```

**Status Values:**
- `active` - Actively working
- `break` - On break
- `away` - Temporarily away
- `offline` - Not available

#### Get Hive Presence
```http
GET /api/presence/hive/{hiveId}
```

**Response:**
```json
{
  "hiveId": "hive_123",
  "activeMembers": 5,
  "members": [
    {
      "userId": "user_456",
      "username": "johndoe",
      "status": "active",
      "activity": "writing",
      "joinedAt": "2025-09-04T10:30:00Z",
      "sessionDuration": 1800
    }
  ],
  "updatedAt": "2025-09-04T11:00:00Z"
}
```

### 3. Timer API

#### Start Focus Session
```http
POST /api/timer/start
Content-Type: application/json

{
  "hiveId": "hive_123",
  "duration": 1500,
  "type": "pomodoro",
  "goal": "Complete feature implementation"
}
```

#### Pause/Resume Session
```http
POST /api/timer/pause
POST /api/timer/resume
```

#### End Session
```http
POST /api/timer/stop

{
  "completed": true,
  "notes": "Finished the authentication module"
}
```

#### Get Timer Status
```http
GET /api/timer/status
```

**Response:**
```json
{
  "isRunning": true,
  "type": "pomodoro",
  "duration": 1500,
  "elapsed": 600,
  "remaining": 900,
  "sessionCount": 3,
  "hiveId": "hive_123"
}
```

### 4. Analytics API

#### Get Productivity Stats
```http
GET /api/analytics/productivity?period=week
```

**Query Parameters:**
- `period`: `day`, `week`, `month`, `year`
- `startDate` (optional): ISO date string
- `endDate` (optional): ISO date string

**Response:**
```json
{
  "totalFocusTime": 28800,
  "sessionsCompleted": 12,
  "averageSessionLength": 2400,
  "productivityScore": 85,
  "streak": {
    "current": 5,
    "best": 12,
    "type": "daily"
  },
  "breakdown": {
    "monday": 7200,
    "tuesday": 5400,
    "wednesday": 6000
  }
}
```

#### Get Achievements
```http
GET /api/analytics/achievements
```

**Response:**
```json
{
  "unlocked": [
    {
      "id": "early_bird",
      "name": "Early Bird",
      "description": "Started 5 sessions before 7 AM",
      "unlockedAt": "2025-09-01T06:30:00Z",
      "rarity": "common"
    }
  ],
  "progress": [
    {
      "id": "marathon_runner",
      "name": "Marathon Runner",
      "progress": 8,
      "total": 10,
      "description": "Complete 10 sessions over 2 hours"
    }
  ]
}
```

### 5. Chat API

#### Send Message
```http
POST /api/chat/send
Content-Type: application/json

{
  "hiveId": "hive_123",
  "content": "Great progress today everyone!",
  "type": "text"
}
```

#### Get Message History
```http
GET /api/chat/history/{hiveId}?limit=50&before={timestamp}
```

**Response:**
```json
{
  "messages": [
    {
      "id": "msg_789",
      "userId": "user_456",
      "username": "johndoe",
      "content": "Just completed my third pomodoro!",
      "type": "text",
      "timestamp": "2025-09-04T11:30:00Z",
      "reactions": [
        {
          "emoji": "🎉",
          "users": ["user_123", "user_789"]
        }
      ]
    }
  ],
  "hasMore": true
}
```

### 6. Gamification API

#### Get User Points
```http
GET /api/gamification/points
```

**Response:**
```json
{
  "totalPoints": 1250,
  "level": 8,
  "nextLevelPoints": 1500,
  "weeklyPoints": 180,
  "rank": 15,
  "badges": ["early_bird", "consistent_worker", "team_player"]
}
```

#### Get Leaderboard
```http
GET /api/gamification/leaderboard?scope=weekly&limit=10
```

**Response:**
```json
{
  "leaderboard": [
    {
      "rank": 1,
      "userId": "user_123",
      "username": "topperformer",
      "points": 450,
      "level": 12,
      "avatar": "avatar_url"
    }
  ],
  "userRank": {
    "rank": 5,
    "points": 280
  }
}
```

### 7. Buddy System API

#### Find Buddy Match
```http
POST /api/buddy/match
Content-Type: application/json

{
  "preferences": {
    "timezone": "UTC-5",
    "workStyle": "morning",
    "goals": ["focus", "accountability"],
    "interests": ["programming", "design"]
  }
}
```

#### Send Buddy Request
```http
POST /api/buddy/request
Content-Type: application/json

{
  "targetUserId": "user_789",
  "message": "Would love to be accountability partners!"
}
```

#### Buddy Check-in
```http
POST /api/buddy/checkin
Content-Type: application/json

{
  "buddyId": "user_789",
  "mood": "motivated",
  "todaysGoal": "Complete 5 pomodoros",
  "yesterdayProgress": "Achieved 4/5 goals"
}
```

### 8. Identity & Persona API

#### Get User Personas
```http
GET /api/personas
```

**Response:**
```json
{
  "personas": [
    {
      "id": "persona_work",
      "name": "Work",
      "isActive": true,
      "preferences": {
        "theme": "professional",
        "notifications": "minimal",
        "visibility": "public"
      }
    },
    {
      "id": "persona_study",
      "name": "Study",
      "isActive": false,
      "preferences": {
        "theme": "focus",
        "notifications": "all",
        "visibility": "friends"
      }
    }
  ]
}
```

#### Switch Persona
```http
POST /api/personas/switch
Content-Type: application/json

{
  "personaId": "persona_study"
}
```

---

## Real-time WebSocket

### Connection
```javascript
const socket = new WebSocket('ws://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ 
  Authorization: 'Bearer <jwt-token>' 
}, function(frame) {
  // console.log('Connected: ' + frame);
});
```

### Subscriptions

#### Presence Updates
```javascript
stompClient.subscribe('/topic/hive/{hiveId}/presence', function(message) {
  const presence = JSON.parse(message.body);
  // Handle presence update
});
```

#### Chat Messages
```javascript
stompClient.subscribe('/topic/hive/{hiveId}/chat', function(message) {
  const chatMessage = JSON.parse(message.body);
  // Handle new chat message
});
```

#### Timer Sync
```javascript
stompClient.subscribe('/topic/hive/{hiveId}/timer', function(message) {
  const timerState = JSON.parse(message.body);
  // Handle timer synchronization
});
```

#### Personal Notifications
```javascript
stompClient.subscribe('/user/queue/notifications', function(message) {
  const notification = JSON.parse(message.body);
  // Handle personal notification
});
```

### Sending Messages

#### Update Presence
```javascript
stompClient.send('/app/presence/update', {}, JSON.stringify({
  hiveId: 'hive_123',
  status: 'active',
  activity: 'coding'
}));
```

#### Send Chat Message
```javascript
stompClient.send('/app/chat/send', {}, JSON.stringify({
  hiveId: 'hive_123',
  content: 'Hello everyone!'
}));
```

---

## Error Handling

### Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ],
    "timestamp": "2025-09-04T12:00:00Z",
    "path": "/api/auth/register"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| `UNAUTHORIZED` | 401 | Invalid or missing authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Invalid request parameters |
| `CONFLICT` | 409 | Resource conflict (e.g., duplicate) |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |

---

## Rate Limiting

### Limits by Endpoint

| Endpoint Type | Limit | Window |
|--------------|-------|---------|
| Authentication | 5 requests | 1 minute |
| Read Operations | 100 requests | 1 minute |
| Write Operations | 30 requests | 1 minute |
| WebSocket Messages | 60 messages | 1 minute |

### Rate Limit Headers
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1693828800
```

### Exceeded Response
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests",
    "retryAfter": 30
  }
}
```

---

## Pagination

### Request Parameters
- `page`: Page number (0-indexed)
- `size`: Items per page (max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

### Response Format
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

---

## Webhooks (Future)

### Event Types
- `hive.created`
- `hive.member_joined`
- `session.completed`
- `achievement.unlocked`
- `buddy.matched`

### Webhook Payload
```json
{
  "event": "session.completed",
  "timestamp": "2025-09-04T12:00:00Z",
  "data": {
    "sessionId": "session_123",
    "userId": "user_456",
    "duration": 1500,
    "hiveId": "hive_789"
  }
}
```

---

*API Reference Version 1.0.0 - Generated September 4, 2025*