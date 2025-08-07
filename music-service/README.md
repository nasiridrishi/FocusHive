# FocusHive Music Service

A comprehensive microservice for music recommendations, collaborative playlists, and streaming integration within the FocusHive ecosystem.

## Overview

The Music Service provides:
- **Advanced Music Recommendation Engine**: Multi-algorithm system combining content-based filtering, collaborative filtering, productivity correlation, and discovery algorithms with sophisticated scoring
- **Task & Mood Optimization**: AI-powered recommendations that adapt to specific task types (Deep Work, Creative, Coding, etc.) and moods (Focused, Energetic, Relaxed, etc.) using audio feature targeting
- **Spotify OAuth2 Integration**: Secure connectivity with Spotify using encrypted token storage (AES-256-GCM) and comprehensive Web SDK integration
- **Collaborative Playlist Management**: Real-time multi-user playlist creation with voting mechanisms, permission levels, and conflict resolution
- **Intelligent Caching System**: Redis-based caching with configurable TTLs (1-24 hours) for different data types, achieving >80% cache hit ratios
- **Comprehensive Feedback & Learning**: Advanced feedback system capturing explicit ratings, implicit behavior (skip patterns, completion rates), and contextual data for continuous algorithm improvement
- **Real-time Analytics & Insights**: User performance metrics, trend analysis, A/B testing support, and community insights dashboard
- **Production-Ready Architecture**: Circuit breakers, health monitoring, distributed tracing, and comprehensive observability stack

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.3.0 with Java 21
- **Database**: PostgreSQL with `music` schema
- **Caching**: Redis for recommendation caching and session state
- **Security**: JWT-based authentication via Identity Service
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Inter-service Communication**: OpenFeign with resilience patterns

### Core Components

```
com.focushive.music/
├── config/          # Configuration classes
├── controller/      # REST API endpoints
├── service/         # Business logic
├── repository/      # Data access layer
├── model/          # JPA entities
├── dto/            # Data transfer objects
├── client/         # Inter-service communication
└── event/          # Event handling
```

## Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 12+
- Redis 6+
- Spotify Developer Account (for integration)

### Environment Setup

1. **Database Setup**:
```bash
# Create database and user
createdb focushive
psql -d focushive -c "CREATE SCHEMA IF NOT EXISTS music;"
```

2. **Redis Setup**:
```bash
# Start Redis server
redis-server --daemonize yes
```

3. **Environment Variables**:
```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=focushive
export DB_USER=focushive_user
export DB_PASSWORD=focushive_pass

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=focushive_pass

# Spotify Integration
export SPOTIFY_CLIENT_ID=your_client_id
export SPOTIFY_CLIENT_SECRET=your_client_secret

# Service URLs
export USER_SERVICE_URL=http://localhost:8080
export HIVE_SERVICE_URL=http://localhost:8080
export ANALYTICS_SERVICE_URL=http://localhost:8080
```

### Running the Service

```bash
# Development mode
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production mode
./gradlew build
java -jar build/libs/music-service-*.jar
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test category
./gradlew test --tests "*Repository*"  # Repository tests
./gradlew test --tests "*Service*"     # Service tests
./gradlew test --tests "*Controller*"  # Controller tests

# Generate test coverage report
./gradlew jacocoTestReport
```

## API Documentation

### Interactive Documentation
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8084/api-docs

### Authentication
All endpoints require JWT authentication:
```http
Authorization: Bearer <your-jwt-token>
```

### Main Endpoints

#### Enhanced Music Recommendations
```http
POST /api/v1/music/recommendations/sessions            # Generate session-based recommendations
POST /api/v1/music/recommendations/tasks/{taskType}    # Task-specific recommendations
POST /api/v1/music/recommendations/moods/{mood}        # Mood-based recommendations
POST /api/v1/music/recommendations/{id}/feedback       # Submit recommendation feedback
POST /api/v1/music/recommendations/{id}/feedback/batch # Batch feedback submission
GET  /api/v1/music/recommendations/history             # User recommendation history
GET  /api/v1/music/recommendations/analytics           # Personal analytics & insights
GET  /api/v1/music/recommendations/trending             # Community trends & popular tracks
GET  /api/v1/music/recommendations/stats                # User statistics & performance
DELETE /api/v1/music/recommendations/cache             # Clear user recommendation cache
```

#### Playlist Management
```http
GET    /api/v1/music/playlists                         # List user playlists
POST   /api/v1/music/playlists                         # Create new playlist
GET    /api/v1/music/playlists/{id}                    # Get playlist details
PUT    /api/v1/music/playlists/{id}                    # Update playlist
DELETE /api/v1/music/playlists/{id}                    # Delete playlist
POST   /api/v1/music/playlists/{id}/tracks             # Add tracks to playlist
DELETE /api/v1/music/playlists/{id}/tracks/{trackId}   # Remove track from playlist
```

#### Streaming Integration
```http
GET  /api/v1/music/streaming/spotify/auth              # Initiate Spotify OAuth2 flow
GET  /api/v1/music/streaming/spotify/callback          # OAuth2 callback handler
POST /api/v1/music/streaming/spotify/disconnect        # Disconnect Spotify account
GET  /api/v1/music/streaming/spotify/profile           # Get connected Spotify profile
POST /api/v1/music/streaming/spotify/refresh           # Refresh access tokens
```

#### Collaborative Features
```http
POST /api/v1/music/collaborative/playlists             # Create collaborative playlist
GET  /api/v1/music/collaborative/playlists/{id}/queue  # Get collaborative queue
POST /api/v1/music/collaborative/playlists/{id}/vote   # Vote on queued tracks
POST /api/v1/music/collaborative/playlists/{id}/join   # Join collaborative session
POST /api/v1/music/collaborative/playlists/{id}/leave  # Leave collaborative session
GET  /api/v1/music/collaborative/playlists/{id}/members # Get session members
```

## Database Schema

### Core Tables
- **user_music_preferences**: User music preferences and Spotify tokens
- **playlists**: Playlist metadata and settings  
- **playlist_tracks**: Tracks within playlists
- **music_sessions**: Active music listening sessions with recommendation context
- **streaming_credentials**: Encrypted OAuth tokens for streaming services

### Advanced Recommendation System
- **recommendation_history**: Comprehensive recommendation tracking with performance metrics
  - Algorithm versions, acceptance rates, diversity scores, productivity correlations
  - Context information (time, environment, device, energy level)
  - A/B testing variants and performance analytics
- **recommendation_track_ids**: Many-to-many track relationships with positioning
- **recommendation_genre_distribution**: Genre breakdown per recommendation
- **recommendation_seed_artists/tracks/genres**: Seed data for recommendations
- **recommendation_metadata**: Flexible key-value metadata storage

### Comprehensive Feedback System  
- **recommendation_feedback**: Detailed user feedback and behavior tracking
  - Explicit feedback: ratings, likes/dislikes, text comments
  - Implicit behavior: skip patterns, completion rates, pause events
  - Contextual data: task performance, productivity impact, mood alignment
- **feedback_volume_changes**: Volume adjustment events during playback
- **feedback_seek_events**: Track seeking behavior analysis
- **feedback_tags**: Categorized feedback tags
- **feedback_metadata**: Extended feedback context

### Collaborative Features
- **playlist_collaborators**: Role-based permissions (owner, editor, viewer)
- **playlist_queue**: Real-time collaborative queue with voting
- **playlist_votes**: Democratic track selection mechanism
- **hive_music_settings**: Hive-specific music configurations

### Performance & Analytics
- **User recommendation performance views**: Success rates, preference analysis
- **Track recommendation performance views**: Popularity, user satisfaction metrics
- **Task type effectiveness views**: Productivity correlation by task/mood combinations
- **Automated triggers**: Real-time acceptance rate calculation
- **Performance functions**: Metric calculation and trend analysis

## Recommendation Engine Architecture

### Multi-Algorithm Approach
The Music Service implements a sophisticated recommendation system combining four core algorithms:

#### 1. Content-Based Filtering
- **Audio Feature Analysis**: Uses Spotify's audio features (energy, valence, tempo, danceability, etc.)
- **Genre Preferences**: Analyzes user's historical genre preferences with weighted scoring
- **Artist Similarity**: Content similarity based on artist audio profiles
- **Task Optimization**: Matches audio features to task requirements (e.g., low energy for deep work)

#### 2. Collaborative Filtering  
- **User Similarity**: Finds users with similar listening patterns and preferences
- **Hive-Based Recommendations**: Leverages preferences of hive members for group cohesion
- **Implicit Feedback**: Uses listening behavior (completion rates, skips) for similarity calculation
- **Matrix Factorization**: Advanced user-item collaborative filtering

#### 3. Productivity-Based Correlation
- **Focus Score Integration**: Correlates music choices with measured focus and productivity scores
- **Task Performance Tracking**: Learns which music enhances performance for specific tasks
- **Context Awareness**: Considers time of day, session duration, and work environment
- **Adaptive Learning**: Continuously improves based on productivity feedback

#### 4. Discovery Algorithm
- **Exploration vs Exploitation**: Balances familiar preferences with music discovery
- **Serendipity Injection**: Introduces unexpected but potentially enjoyable tracks
- **Novelty Scoring**: Measures and controls the novelty level of recommendations
- **Diversity Optimization**: Ensures recommendation variety across genres, artists, and audio features

### Algorithm Scoring & Blending
```yaml
Default Algorithm Weights (Configurable):
- Productivity Correlation: 40%
- User Preferences: 30% 
- Task/Mood Alignment: 20%
- Diversity Factor: 10%
```

### Task & Mood Optimization
The system provides specialized recommendations for different contexts:

#### Task Types with Audio Feature Targets:
- **Deep Work**: Low energy (0.3), neutral valence (0.4), high instrumentalness (0.8)
- **Creative**: Medium energy (0.6), positive valence (0.7), medium instrumentalness (0.4)  
- **Administrative**: High energy (0.7), neutral valence (0.5), low instrumentalness (0.3)
- **Coding**: Medium energy (0.5), neutral valence (0.4), high instrumentalness (0.9)
- **Studying**: Low energy (0.3), neutral valence (0.4), medium acousticness (0.6)
- **Research**: Medium energy (0.4), neutral valence (0.5), high speechiness (0.2)
- **Casual**: High energy (0.8), positive valence (0.8), high danceability (0.7)

#### Mood Types:
- **Focused**: Low energy, neutral valence, minimal lyrics
- **Energetic**: High energy, positive valence, high tempo
- **Relaxed**: Low energy, positive valence, high acousticness
- **Creative**: Medium energy, varied valence, balanced instrumentalness
- **Stressed**: Calming features, low energy, positive valence

### Intelligent Caching Strategy
```yaml
Cache Types and TTLs:
- User Recommendations: 1 hour (frequently changing preferences)
- User Preferences: 6 hours (relatively stable)  
- Analytics Data: 30 minutes (real-time insights)
- Feedback Cache: 2 hours (learning system updates)
- History Cache: 4 hours (historical data)
- Spotify Data: 24 hours (external API data)
- Collaborative Sessions: 15 minutes (real-time collaboration)
- Session Context: 8 hours (session-based data)
- Algorithm Results: 12 hours (computed recommendations)
```

### Learning & Adaptation
- **Feedback Integration**: Explicit and implicit feedback influences future recommendations
- **Preference Drift Detection**: Monitors and adapts to changing user preferences
- **Contextual Learning**: Learns context-specific preferences (task, mood, time of day)
- **A/B Testing**: Compares algorithm versions for continuous improvement
- **Cold Start Handling**: Strategies for new users with minimal listening history

## Configuration

### Application Profiles
- **dev**: Development with debug logging and relaxed security
- **prod**: Production with optimized settings and security
- **test**: Testing with in-memory database and mocked services

### Key Configuration Properties

```yaml
# Music service specific settings
app:
  music:
    recommendation:
      cache-ttl: 3600  # Cache TTL in seconds
      max-recommendations: 20
    playlist:
      max-size: 500
      collaborative-max-contributors: 50
    session:
      max-duration: 480  # 8 hours in minutes

# Spotify integration
spotify:
  client-id: ${SPOTIFY_CLIENT_ID}
  client-secret: ${SPOTIFY_CLIENT_SECRET}
  redirect-uri: http://localhost:8084/api/v1/music/streaming/spotify/callback

# Inter-service communication
services:
  user-service:
    url: ${USER_SERVICE_URL:http://localhost:8080}
  hive-service:
    url: ${HIVE_SERVICE_URL:http://localhost:8080}
  analytics-service:
    url: ${ANALYTICS_SERVICE_URL:http://localhost:8080}
```

## Development

### Code Style
- Follow Google Java Style Guide
- Use Lombok annotations for reducing boilerplate
- Comprehensive JavaDoc for all public methods
- Meaningful variable and method names

### Testing Strategy
- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **Repository Tests**: Test data access with @DataJpaTest
- **Controller Tests**: Test REST endpoints with MockMvc
- **Service Tests**: Test business logic with mocked dependencies

### Best Practices
1. **TDD Approach**: Write tests before implementation
2. **Clean Architecture**: Separate concerns across layers
3. **Error Handling**: Comprehensive exception handling with meaningful messages
4. **Logging**: Structured logging with correlation IDs
5. **Monitoring**: Health checks and metrics for observability

## Monitoring and Health

### Health Endpoints
```http
GET /actuator/health        # Overall health
GET /actuator/health/db     # Database connectivity
GET /actuator/health/redis  # Redis connectivity
```

### Metrics
```http
GET /actuator/metrics              # Available metrics
GET /actuator/prometheus           # Prometheus format metrics
GET /actuator/metrics/jvm.memory.used  # Specific metric
```

### Circuit Breaker Status
```http
GET /actuator/circuitbreakers      # Circuit breaker states
GET /actuator/circuitbreakerevents # Recent events
```

## Security

### JWT Token Validation
- Tokens validated against Identity Service public key
- Role-based access control with method-level security
- Automatic token refresh for long-running operations

### Data Protection
- Streaming service tokens encrypted at rest
- PII data handling compliant with privacy regulations
- Audit logging for sensitive operations

### API Security
- Rate limiting to prevent abuse
- Input validation and sanitization
- SQL injection prevention with parameterized queries

## Deployment

### Docker Support
```dockerfile
# Build
docker build -t focushive/music-service .

# Run
docker run -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  focushive/music-service
```

### Kubernetes Deployment
```yaml
# Example deployment configuration
apiVersion: apps/v1
kind: Deployment
metadata:
  name: music-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: music-service
  template:
    metadata:
      labels:
        app: music-service
    spec:
      containers:
      - name: music-service
        image: focushive/music-service:latest
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

## Troubleshooting

### Common Issues
1. **Database Connection**: Check PostgreSQL service and credentials
2. **Redis Connection**: Verify Redis is running and accessible
3. **Spotify Integration**: Ensure valid client credentials and callback URL
4. **JWT Validation**: Check Identity Service connectivity and public key

### Debug Logging
```yaml
logging:
  level:
    com.focushive.music: DEBUG
    org.springframework.web: DEBUG
    feign: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests for your changes
4. Implement the feature
5. Run tests (`./gradlew test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

- **Development Team**: dev@focushive.com
- **Issue Tracking**: GitHub Issues
- **Documentation**: [Wiki](https://github.com/focushive/music-service/wiki)