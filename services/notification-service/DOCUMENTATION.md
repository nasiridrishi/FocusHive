# FocusHive Notification Service - Complete Documentation

## Service Overview
Multi-channel notification system for FocusHive platform (Port 8083)
- **Purpose**: Handle email, push, and in-app notifications
- **Priority**: HIGH (Level 2) - Essential for user engagement
- **Status**: Production-ready with 92.30% endpoint compliance

## Quick Start

### Local Development
```bash
# Start infrastructure
docker-compose up -d focushive-notification-service-postgres focushive-notification-service-redis focushive-notification-service-rabbitmq

# Run service
./gradlew bootRun --args='--spring.profiles.active=local'

# Test endpoint
curl http://localhost:8083/health
```

### Docker Deployment
```bash
./docker-deploy.sh start
./docker-deploy.sh logs
./docker-deploy.sh stop
```

## Architecture

### Tech Stack
- **Framework**: Spring Boot 3.3.1, Java 21
- **Database**: PostgreSQL 15 (primary), Redis (cache)
- **Messaging**: RabbitMQ 3.12
- **Email**: Spring Mail with SMTP
- **Security**: OAuth2 Resource Server (JWT)
- **Testing**: JUnit 5, TestContainers, GreenMail
- **API Docs**: SpringDoc OpenAPI 2.6.0

### Service Dependencies
1. **Identity Service** (8081): JWT authentication
2. **FocusHive Backend** (8080): Primary event source
3. **Buddy Service** (8087): Accountability notifications
4. **External**: PostgreSQL, RabbitMQ, Redis, SMTP

## Key Features

### 1. Multi-Channel Delivery
- **Email**: SMTP with Thymeleaf templates
- **In-App**: WebSocket real-time notifications
- **Push**: Mobile push (FCM ready)
- **SMS**: Twilio integration (planned)

### 2. Event Processing
- RabbitMQ for async processing
- Dead letter queue for failures
- Retry mechanism with exponential backoff
- Event types: hive, timer, chat, analytics, forum, buddy, security

### 3. User Preferences
- Per-channel preferences
- Frequency controls (immediate/digest/weekly)
- Do not disturb schedules
- Opt-in/opt-out per notification type

### 4. Security Components (Custom)
- **EndpointExistenceChecker**: Validates endpoint existence
- **CustomAuthenticationEntryPoint**: Proper 404/401 handling
- **RequestValidationFilter**: JSON validation before auth

## API Endpoints

### Health & Monitoring (Public)
- `GET /health` - Health check
- `GET /actuator/health` - Detailed health
- `GET /actuator/prometheus` - Metrics
- `GET /api/metrics` - Basic metrics

### Notifications (Protected)
- `GET /api/notifications` - Get user notifications
- `POST /api/notifications/send` - Send notification
- `PUT /api/notifications/{id}/read` - Mark as read
- `DELETE /api/notifications/{id}` - Delete notification

### Preferences (Protected)
- `GET /api/notifications/preferences` - Get preferences
- `PUT /api/notifications/preferences` - Update preferences

### Templates (Admin)
- `GET /api/notifications/templates` - List templates
- `POST /api/notifications/templates` - Create template
- `PUT /api/notifications/templates/{id}` - Update template

## Testing

### Unit Tests
```bash
./gradlew test                    # All tests
./gradlew unitTest               # Unit only
./gradlew test -PexcludeIntegrationTests  # Skip integration
```

### Test Coverage
- **49 test classes**
- **96% pass rate** (48/50 passing)
- **92% backend coverage**

### Integration Testing
```bash
# Test with RabbitMQ
python3 test-rabbitmq.py

# Check database
docker exec focushive-notification-service-postgres psql -U notification_user \
  -d notification_service -c "SELECT * FROM notifications;"

# Email testing (MailHog)
open http://localhost:8025
```

## Configuration

### Environment Variables
```bash
# Required
DB_PASSWORD=notification_secure_password_2024
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=app-specific-password
JWT_SECRET=your-jwt-secret

# Optional
RABBITMQ_USER=admin
REDIS_HOST=localhost
NOTIFICATION_TEST_ENABLED=false
```

### Application Profiles
- **local**: H2 database, mock services
- **test**: H2 in-memory, test containers
- **docker**: Full containerized environment
- **production**: External services, SSL enabled

## Database Schema

### Core Tables
- `notifications` - Notification records
- `notification_preferences` - User preferences
- `notification_templates` - Message templates
- `users` - User cache
- `security_audit_log` - Security events
- `dead_letter_messages` - Failed messages

### Flyway Migrations
```bash
./gradlew flywayMigrate    # Run migrations
./gradlew flywayInfo       # Check status
./gradlew flywayValidate   # Validate schema
```

## Monitoring & Operations

### Health Checks
- Database connectivity
- RabbitMQ status
- Redis availability
- SMTP server reachability
- Circuit breaker states
- Connection pool metrics

### Metrics Exposed
- Notification delivery rates
- Queue depths
- Error rates
- Circuit breaker metrics
- JVM metrics
- Custom business metrics

### Logging
- Structured logging with correlation IDs
- Log levels: INFO (production), DEBUG (development)
- Centralized logging ready (ELK stack compatible)

## Performance

### Optimizations
- Connection pooling (HikariCP)
- Redis caching for preferences
- Template pre-compilation
- Batch email sending
- Async processing via RabbitMQ

### Benchmarks
- Startup time: ~7 seconds
- Request overhead: <1ms for security
- Queue processing: 1000 msg/min
- Email sending: 100/min (rate limited)

## Security

### Authentication
- JWT validation from Identity Service
- OAuth2 Resource Server configuration
- Role-based access (USER, ADMIN)

### Custom Security (Production-Ready)
1. **404 vs 401 Differentiation**: Non-existent endpoints return 404
2. **JSON Validation**: Malformed requests return 400
3. **Request Filtering**: Early validation in filter chain
4. **No Information Leakage**: Consistent error responses

## Troubleshooting

### Common Issues
1. **Port 8083 in use**: Kill existing process
2. **Database connection**: Check PostgreSQL on port 5433
3. **RabbitMQ down**: Management UI at localhost:15672
4. **Email failures**: Check SMTP credentials

### Debug Commands
```bash
# Check logs
tail -f bootrun.log

# Test endpoint
curl -v http://localhost:8083/health

# RabbitMQ queues
rabbitmqctl list_queues

# Database connection
psql -h localhost -p 5433 -U notification_user -d notification_service
```

## Project Structure
```
notification-service/
├── src/main/java/com/focushive/notification/
│   ├── config/          # Spring configurations
│   ├── controller/      # REST endpoints (9 controllers)
│   ├── entity/          # JPA entities (8 entities)
│   ├── repository/      # Data access (7 repositories)
│   ├── service/         # Business logic (23 services)
│   ├── messaging/       # RabbitMQ handlers
│   ├── security/        # Custom security components
│   └── exception/       # Error handling
├── src/main/resources/
│   ├── application.yml  # Base configuration
│   ├── application-*.yml # Profile configs
│   ├── db/migration/    # Flyway scripts
│   └── templates/       # Email templates
└── src/test/            # 49 test classes
```

## Recent Updates

### Security Enhancements (Sept 2025)
- Implemented custom Spring Security components
- Fixed endpoint status codes (404/400/401)
- Improved error handling consistency
- Success rate: 82.05% → 92.30%

## Development Guidelines

### Code Standards
- Java 21 features
- Spring Boot 3.x patterns
- RESTful API design
- Clean architecture
- SOLID principles

### Testing Requirements
- Unit tests for all services
- Integration tests for controllers
- TestContainers for external dependencies
- Minimum 80% coverage

### Git Workflow
```bash
git checkout -b feature/your-feature
# Make changes
./gradlew test
git commit -m "feat: description"
git push origin feature/your-feature
# Create PR
```

## Deployment

### Docker Build
```bash
docker build -t focushive/notification-service:latest .
docker push focushive/notification-service:latest
```

### Kubernetes (Optional)
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### Health Verification
```bash
curl http://localhost:8083/actuator/health
# Should return {"status":"UP"}
```

## Contact & Support
- **Service Owner**: FocusHive Backend Team
- **Priority**: P2 (High)
- **SLA**: 99.9% uptime
- **Repository**: services/notification-service/

---
*Last Updated: September 2025 | Version: 1.0.0 | Status: Production-Ready*