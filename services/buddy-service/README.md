# Buddy Service

## Overview
Accountability partner matching and management service for FocusHive platform. Implements smart matching algorithms to pair users based on goals, schedules, and working styles.

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Build**: Gradle 8.11

### Key Features
- Smart partner matching algorithm
- Goal sharing and tracking
- Check-in reminders and accountability scoring
- Partnership health monitoring
- Real-time notifications

## API Endpoints

### Matching
- `PUT /api/v1/buddy/matching/preferences` - Update matching preferences
- `POST /api/v1/buddy/matching/queue/join` - Join matching queue
- `GET /api/v1/buddy/matching/queue/status` - Get queue status
- `DELETE /api/v1/buddy/matching/queue/leave` - Leave queue
- `GET /api/v1/buddy/matching/suggestions` - Get partner suggestions

### Partnerships
- `POST /api/v1/buddy/partnerships/request` - Create partnership request
- `PUT /api/v1/buddy/partnerships/{id}/accept` - Accept request
- `PUT /api/v1/buddy/partnerships/{id}/reject` - Reject request
- `GET /api/v1/buddy/partnerships/active` - Get active partnerships
- `DELETE /api/v1/buddy/partnerships/{id}` - End partnership

### Goals
- `POST /api/v1/buddy/goals` - Create shared goal
- `GET /api/v1/buddy/goals` - Get all goals
- `PUT /api/v1/buddy/goals/{id}` - Update goal
- `POST /api/v1/buddy/goals/{id}/milestones` - Add milestone

### Check-ins
- `POST /api/v1/buddy/checkins` - Create check-in
- `GET /api/v1/buddy/checkins/pending` - Get pending check-ins
- `PUT /api/v1/buddy/checkins/{id}` - Update check-in

## Database Schema

### Core Tables
- `buddy_users` - User profiles and preferences
- `buddy_partnerships` - Active partnerships
- `buddy_preferences` - Matching preferences
- `shared_goals` - Shared goals between partners
- `buddy_checkins` - Daily check-ins
- `accountability_scores` - Performance metrics

## Configuration

### Environment Variables
```yaml
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5437/buddy_service
SPRING_DATASOURCE_USERNAME=buddy_user
SPRING_DATASOURCE_PASSWORD=buddy_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_DATABASE=7

# JWT
JWT_SECRET=your-secret-key
```

### Matching Algorithm Configuration
```properties
buddy.matching.weights.timezone=0.25
buddy.matching.weights.interests=0.20
buddy.matching.weights.goals=0.20
buddy.matching.weights.activity=0.15
buddy.matching.weights.communication=0.10
buddy.matching.weights.personality=0.10
```

## Running the Service

### Local Development
```bash
./gradlew bootRun
```

### Docker
```bash
docker-compose up focushive_buddy_service_app
```

### Testing
```bash
# Unit tests
./gradlew test

# E2E tests
./gradlew e2eTest

# With coverage
./gradlew test jacocoTestReport
```

## Monitoring

### Health Check
`GET /api/v1/health`

### Metrics
- Partnership creation rate
- Matching success rate
- Check-in completion rate
- Average accountability score

### Swagger Documentation
Available at: http://localhost:8087/swagger-ui/index.html

## Ports

- **8087**: Main application port (REST API, Swagger UI)
- **8088**: Management port (Actuator endpoints - health, metrics, prometheus)

### Important: Actuator Endpoints
All Spring Boot Actuator endpoints are served on port **8088** for security isolation:
- Health: `http://localhost:8088/actuator/health`
- Metrics: `http://localhost:8088/actuator/metrics`
- Prometheus: `http://localhost:8088/actuator/prometheus`
- Info: `http://localhost:8088/actuator/info`

The custom application health endpoint remains on the main port:
- Application Health: `http://localhost:8087/api/v1/health`

## Integration Points

### Identity Service
- User authentication via JWT
- Profile information sync

### Notification Service
- Check-in reminders
- Partnership updates
- Goal milestones

### FocusHive Backend
- Activity data for matching
- Productivity metrics

## Security

- JWT authentication required for all endpoints
- User isolation - can only access own partnerships
- Rate limiting on matching operations
- Input validation on all DTOs