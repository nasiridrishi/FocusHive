# FocusHive Backend Architecture & API Documentation

## 🏗️ How the Backend Works

### Core Architecture

The FocusHive backend is a **monolithic Spring Boot application** that serves as the central hub for all business logic and data management. It follows a **layered architecture**:

```
┌─────────────────────────────────────────────────────────┐
│                     Client Layer                        │
│         (React Frontend, Mobile Apps, WebSocket)        │
└────────────────────┬────────────────────────────────────┘
                      │ HTTP/WebSocket
┌─────────────────────▼────────────────────────────────────┐
│                  Controller Layer                        │
│              (REST APIs & WebSocket Handlers)            │
├───────────────────────────────────────────────────────────┤
│                   Service Layer                          │
│              (Business Logic & Orchestration)            │
├───────────────────────────────────────────────────────────┤
│                  Repository Layer                        │
│               (Data Access & Persistence)                │
├───────────────────────────────────────────────────────────┤
│                   Database Layer                         │
│            (PostgreSQL/H2 + Redis Cache)                 │
└───────────────────────────────────────────────────────────┘
```

### Service Domains

The backend is organized into **9 core service domains**, each handling specific business capabilities:

1. **User Service** - Authentication, user profiles, JWT token management
2. **Hive Service** - Virtual workspace creation and management
3. **Presence Service** - Real-time user status and activity tracking
4. **Timer Service** - Pomodoro timers and focus session tracking
5. **Chat Service** - Real-time messaging with threading and reactions
6. **Analytics Service** - Productivity metrics, achievements, gamification
7. **Forum Service** - Community discussions and knowledge sharing
8. **Buddy Service** - Accountability partner matching and tracking
9. **Notification Service** - Multi-channel notification delivery

### Integration Points

#### 1. **Identity Service Integration** (Port 8081)
- External OAuth2 provider for authentication
- JWT token validation
- User profile management
- Circuit breaker pattern for resilience
- Redis caching for token validation

#### 2. **Database Integration**
- **Primary Database**: PostgreSQL (production) / H2 (development)
- **Caching Layer**: Redis (production) / Caffeine (development)
- **Migration Tool**: Flyway for schema versioning
- **ORM**: JPA/Hibernate with optimized queries

#### 3. **Real-time Communication**
- **WebSocket**: STOMP protocol over WebSocket
- **Message Broker**: In-memory broker for development
- **Topics**: `/topic/hive/{id}`, `/topic/presence`, `/topic/timer`
- **User Queues**: `/user/queue/notifications`

#### 4. **External Services** (Future)
- Music Service (Port 8082) - Spotify integration
- Notification Service (Port 8083) - Email/SMS delivery
- Chat Service (Port 8084) - Advanced messaging features

### Security Architecture

```
Request → JWT Filter → Spring Security → Role-Based Access → Controller
                ↓                              ↓
          Token Validation              Permission Check
                ↓                              ↓
          Identity Service              Method Security
                ↓                              ↓
            Redis Cache                 Business Logic
```

- **Authentication**: JWT tokens with 24-hour expiry
- **Authorization**: Role-based (USER, MODERATOR, ADMIN)
- **Rate Limiting**: Per-user and global limits
- **CORS**: Configured for frontend origins
- **Security Headers**: XSS, CSRF, Content-Type protection

## 📡 Complete API Endpoints

### Health & Status
- `GET /api/health` - Basic health check
- `GET /api/health/detailed` - Detailed health with dependencies
- `GET /api/ping` - Simple ping endpoint

### Authentication
- `POST /api/auth/login` - User login (returns JWT token)
- `POST /api/auth/register` - User registration
- `GET /api/auth/check` - Validate current token
- `POST /api/auth/logout` - Logout and invalidate token
- `GET /api/auth/csrf-token` - Get CSRF token

### Hive Management
- `POST /api/hives` - Create new hive
- `GET /api/hives` - List all hives (paginated)
- `GET /api/hives/{id}` - Get hive details
- `GET /api/hives/by-slug/{slug}` - Get hive by slug
- `PUT /api/hives/{id}` - Update hive
- `DELETE /api/hives/{id}` - Delete hive
- `GET /api/hives/search` - Search hives
- `GET /api/hives/my` - Get user's hives
- `GET /api/hives/active` - Get active hives
- `GET /api/hives/popular` - Get popular hives

### Hive Membership
- `POST /api/hives/{hiveId}/join` - Join public hive
- `DELETE /api/hives/{hiveId}/leave` - Leave hive
- `GET /api/hives/{hiveId}/members` - List hive members
- `GET /api/hives/{hiveId}/members/{memberId}` - Get member details
- `PUT /api/hives/{hiveId}/members/{memberId}/role` - Update member role
- `POST /api/hives/{hiveId}/transfer-ownership` - Transfer ownership
- `POST /api/hives/{hiveId}/invitations` - Create invitation
- `POST /api/invitations/{invitationCode}/accept` - Accept invitation
- `POST /api/invitations/{invitationCode}/reject` - Reject invitation
- `DELETE /api/invitations/{invitationCode}` - Cancel invitation
- `GET /api/users/{userId}/invitations` - Get user's invitations
- `GET /api/my/invitations` - Get current user's invitations

### Presence & Status
- `GET /api/presence/users/{userId}` - Get user presence
- `GET /api/presence/me` - Get current user presence
- `GET /api/presence/hives/{hiveId}` - Get hive members' presence
- `POST /api/presence/hives/batch` - Get multiple hives' presence
- `GET /api/presence/sessions/me` - Get current user's sessions
- `GET /api/presence/hives/{hiveId}/sessions` - Get hive sessions

### Timer & Focus Sessions
- `POST /api/timer/sessions/start` - Start focus session
- `POST /api/timer/sessions/{sessionId}/end` - End session
- `POST /api/timer/sessions/{sessionId}/pause` - Pause session
- `POST /api/timer/sessions/{sessionId}/resume` - Resume session
- `POST /api/timer/sessions/{sessionId}/cancel` - Cancel session
- `GET /api/timer/sessions/current` - Get current session
- `GET /api/timer/sessions/history` - Get session history
- `GET /api/timer/stats` - Get timer statistics
- `GET /api/timer/stats/streak` - Get streak information
- `GET /api/timer/templates` - Get timer templates
- `GET /api/timer/templates/system` - Get system templates
- `POST /api/timer/templates` - Create custom template

### Chat & Messaging
- `POST /api/chat/hives/{hiveId}/messages` - Send message
- `GET /api/chat/hives/{hiveId}/messages` - Get messages (paginated)
- `GET /api/chat/hives/{hiveId}/messages/recent` - Get recent messages
- `GET /api/chat/hives/{hiveId}/messages/after` - Get messages after timestamp
- `PUT /api/chat/messages/{messageId}` - Edit message
- `DELETE /api/chat/messages/{messageId}` - Delete message
- `POST /api/chat/messages/{messageId}/read` - Mark as read
- `GET /api/chat/hives/{hiveId}/stats` - Get chat statistics
- `GET /api/chat/hives/{hiveId}/activity` - Get chat activity

### Analytics & Insights
- `POST /api/analytics/sessions/start` - Start analytics session
- `POST /api/analytics/sessions/{sessionId}/end` - End analytics session
- `GET /api/analytics/users/{userId}/stats` - Get user statistics
- `GET /api/analytics/user/{userId}/summary` - Get user summary
- `GET /api/analytics/user/{userId}/report` - Get detailed report
- `GET /api/analytics/streaks/{userId}` - Get streak data
- `GET /api/analytics/achievements/{userId}` - Get achievements
- `POST /api/analytics/goals` - Create daily goal
- `GET /api/analytics/goals/{userId}/today` - Get today's goals
- `PUT /api/analytics/goals/{userId}/progress` - Update goal progress
- `GET /api/analytics/hive/{hiveId}/summary` - Get hive analytics
- `GET /api/analytics/hives/{hiveId}/leaderboard` - Get leaderboard
- `GET /api/analytics/export/{userId}` - Export analytics data
- `GET /api/analytics/admin/platform-stats` - Platform statistics (admin)

### Forum & Discussions
- `GET /api/forum/categories` - List all categories
- `GET /api/forum/categories/{categoryId}` - Get category details
- `POST /api/forum/categories` - Create category
- `GET /api/forum/hives/{hiveId}/categories` - Get hive categories
- `GET /api/forum/categories/{categoryId}/posts` - Get category posts
- `GET /api/forum/hives/{hiveId}/posts` - Get hive posts
- `GET /api/forum/posts/{postId}` - Get post details
- `POST /api/forum/posts` - Create post
- `PUT /api/forum/posts/{postId}` - Update post
- `DELETE /api/forum/posts/{postId}` - Delete post
- `POST /api/forum/posts/{postId}/pin` - Pin/unpin post
- `POST /api/forum/posts/{postId}/lock` - Lock/unlock post
- `GET /api/forum/posts/{postId}/replies` - Get post replies
- `POST /api/forum/posts/{postId}/replies` - Add reply
- `PUT /api/forum/replies/{replyId}` - Update reply
- `DELETE /api/forum/replies/{replyId}` - Delete reply
- `POST /api/forum/posts/{postId}/vote` - Vote on post
- `POST /api/forum/replies/{replyId}/vote` - Vote on reply
- `DELETE /api/forum/posts/{postId}/vote` - Remove post vote
- `DELETE /api/forum/replies/{replyId}/vote` - Remove reply vote
- `GET /api/forum/search` - Search forum
- `GET /api/forum/posts/tag/{tag}` - Get posts by tag
- `GET /api/forum/trending` - Get trending posts
- `GET /api/forum/tags/popular` - Get popular tags

### Buddy System
- `GET /api/buddy/matches` - Get potential buddy matches
- `POST /api/buddy/requests` - Send buddy request
- `GET /api/buddy/requests/pending` - Get pending requests
- `POST /api/buddy/requests/{requestId}/accept` - Accept request
- `POST /api/buddy/requests/{requestId}/reject` - Reject request
- `GET /api/buddy/partners` - Get current buddies
- `POST /api/buddy/sessions/schedule` - Schedule buddy session
- `POST /api/buddy/goals` - Set shared goals
- `GET /api/buddy/goals/progress` - Get goal progress
- `POST /api/buddy/feedback` - Give feedback

### Notifications
- `POST /api/notifications` - Create notification
- `GET /api/notifications` - Get notifications (paginated)
- `GET /api/notifications/unread` - Get unread notifications
- `GET /api/notifications/unread/count` - Get unread count
- `PATCH /api/notifications/{id}/read` - Mark as read
- `PATCH /api/notifications/read-all` - Mark all as read
- `DELETE /api/notifications/{id}` - Delete notification
- `PATCH /api/notifications/{id}/archive` - Archive notification
- `GET /api/notifications/type/{type}` - Get by type
- `DELETE /api/notifications/cleanup` - Cleanup old notifications

### WebSocket Endpoints
- `/ws` - WebSocket connection endpoint
- `/topic/hive/{hiveId}` - Hive activity updates
- `/topic/presence/{hiveId}` - Presence updates
- `/topic/timer/{hiveId}` - Timer synchronization
- `/topic/chat/{hiveId}` - Chat messages
- `/user/queue/notifications` - User notifications
- `/user/queue/buddy` - Buddy updates

### Admin Endpoints
- `GET /api/admin/users` - List all users
- `GET /api/admin/system/status` - System status
- `GET /api/admin/config` - System configuration
- `POST /api/admin/actions/test` - Test admin action

## 🔄 Request Flow Example

Here's how a typical request flows through the system:

### Example: User joins a hive

1. **Frontend Request**
   ```
   POST /api/hives/123/join
   Authorization: Bearer <JWT_TOKEN>
   ```

2. **JWT Authentication Filter**
   - Extracts token from Authorization header
   - Validates token with Identity Service
   - Checks Redis cache for token validity
   - Sets security context with user details

3. **Controller Layer** (`HiveMembershipController`)
   - Receives request with authenticated user
   - Validates request parameters
   - Calls service layer

4. **Service Layer** (`HiveMembershipService`)
   - Business logic validation:
     - Check if hive exists
     - Check if user already member
     - Check hive capacity
   - Creates membership record
   - Publishes events

5. **Repository Layer** (`HiveMemberRepository`)
   - Persists membership to database
   - Returns saved entity

6. **Event Publishing**
   - Publishes `UserJoinedHiveEvent`
   - WebSocket broadcasts to hive members
   - Analytics service records activity
   - Notification service sends welcome

7. **Response**
   ```json
   {
     "success": true,
     "data": {
       "hiveId": "123",
       "userId": "456",
       "role": "MEMBER",
       "joinedAt": "2024-12-15T10:30:00Z"
     }
   }
   ```

## 🔌 Service Integration Details

### Identity Service Integration
```java
@Service
public class IdentityIntegrationService {
    // Circuit breaker for resilience
    @CircuitBreaker(name = "identity-service")
    public UserDetails validateToken(String token) {
        // Try Identity Service
        // Fallback to cache if down
        // Return user details
    }
}
```

### Redis Caching Strategy
- **Token Validation**: 5-minute TTL
- **User Profiles**: 30-minute TTL
- **Hive Data**: 10-minute TTL
- **Presence Data**: Real-time, no cache
- **Analytics**: 1-hour TTL for aggregates

### Database Optimization
- **Connection Pool**: HikariCP with 20 connections
- **Query Optimization**: Strategic indexes on foreign keys
- **Batch Operations**: For bulk inserts
- **Lazy Loading**: For relationships
- **Query Caching**: For frequently accessed data

## 🚀 Performance Features

1. **Async Processing**
   - Event publishing
   - Analytics aggregation
   - Notification delivery

2. **Caching Layers**
   - Redis for distributed cache
   - Caffeine for local cache
   - HTTP caching with ETags

3. **Database Performance**
   - Connection pooling
   - Query optimization
   - Index strategy
   - Read replicas (future)

4. **WebSocket Optimization**
   - Message batching
   - Selective broadcasting
   - Connection pooling
   - Heartbeat mechanism

## 🔒 Security Features

1. **Authentication**
   - JWT tokens with refresh
   - Token blacklisting
   - Session management

2. **Authorization**
   - Role-based access (RBAC)
   - Resource-based permissions
   - Method-level security

3. **Protection**
   - Rate limiting (100 req/min)
   - CORS configuration
   - XSS/CSRF protection
   - Input validation
   - SQL injection prevention

## 📊 Monitoring & Observability

1. **Health Checks**
   - Application health
   - Database connectivity
   - Redis availability
   - External service status

2. **Metrics**
   - Prometheus metrics
   - JVM metrics
   - Custom business metrics
   - Request/response timing

3. **Logging**
   - Structured JSON logs
   - Correlation IDs
   - Request tracing
   - Error tracking

## 🐳 Deployment

The backend can be deployed in multiple ways:

1. **Docker Container**
   ```bash
   docker build -t focushive-backend .
   docker run -p 8080:8080 focushive-backend
   ```

2. **Kubernetes**
   - Horizontal scaling
   - Health probes
   - ConfigMaps for configuration
   - Secrets for credentials

3. **Traditional Server**
   ```bash
   java -jar focushive-backend.jar
   ```

## 📝 Configuration

Key configuration properties:
```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DATABASE_URL}
  redis:
    host: ${REDIS_HOST}
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000  # 24 hours

app:
  features:
    forum: true
    buddy: true
    analytics: true
    chat: true
```

## 🔍 Testing Access

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/api/health
- **WebSocket Test**: Use a WebSocket client to connect to `ws://localhost:8080/ws`