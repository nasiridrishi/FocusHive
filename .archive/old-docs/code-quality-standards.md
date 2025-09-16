# FocusHive Code Quality Standards

## Overview

This document outlines the code quality standards and best practices for the FocusHive project, based on recent improvements completed in August 2025.

## Production Code Standards

### 1. Console Logging Policy

#### Frontend (JavaScript/TypeScript)

**Prohibited in Production:**
- `console.log()` statements
- `console.error()` statements (except in error boundaries)
- `console.warn()` statements
- `console.debug()` statements
- `console.table()` and other console methods

**Acceptable Alternatives:**
```typescript
// Use a proper logging service
import { logger } from '@/utils/logger';

// Instead of console.log
logger.info('User action', { userId, action });

// Instead of console.error
logger.error('Failed to fetch data', error);

// For development only
if (process.env.NODE_ENV === 'development') {
  console.log('Debug info');
}
```

**Enforcement:**
- ESLint rule: `no-console` set to 'error' in production builds
- Pre-commit hooks to catch console statements
- Regular code audits (completed August 9, 2025: removed 70+ console statements)

### 2. Backend Logging Standards

#### Java/Spring Boot

**Debug Logging Guidelines:**

1. **Always gate debug logs:**
```java
// Good
if (log.isDebugEnabled()) {
    log.debug("Processing request for user: {}", userId);
}

// Bad
log.debug("Processing request for user: " + userId);
```

2. **Never log sensitive information:**
```java
// Bad - exposes sensitive data
log.debug("User {} logged in with password {}", username, password);

// Good - sanitized logging
log.debug("User authentication attempt for username: {}", username);
```

3. **Production Logging Levels:**
- ERROR: System failures requiring immediate attention
- WARN: Potential issues that should be investigated
- INFO: Important business events and milestones
- DEBUG: Detailed diagnostic information (disabled in production)

**Enforcement:**
- Configure logback.xml with appropriate levels per environment
- Use parameterized logging to avoid string concatenation
- Code review checklist includes logging review

## Architecture Standards

### 1. Single Responsibility Principle (SRP)

**Service Class Guidelines:**

**Maximum Service Size:**
- Soft limit: 500 lines
- Hard limit: 1000 lines
- If exceeding limits, refactor into multiple services

**Example Refactoring Pattern:**
```java
// Before: Monolithic service
public class PlaylistManagementService {
    // 900+ lines handling multiple responsibilities
}

// After: Focused services with facade
public class PlaylistManagementService { // Facade
    private final PlaylistCrudService crudService;
    private final PlaylistTrackService trackService;
    private final SmartPlaylistService smartService;
    private final PlaylistSharingService sharingService;
}
```

**Service Responsibilities:**
- Each service should have ONE primary responsibility
- Services should be cohesive and loosely coupled
- Use facade pattern for backward compatibility when refactoring

### 2. Method Complexity

**Guidelines:**
- Maximum cyclomatic complexity: 10
- Maximum method length: 50 lines
- Maximum parameters: 5 (use request objects for more)

**Example:**
```java
// Bad - too complex
public void processPlaylist(Long id, String name, List<Track> tracks, 
                           User owner, Settings settings, boolean isPublic) {
    // 100+ lines of complex logic
}

// Good - simplified with request object
public void processPlaylist(ProcessPlaylistRequest request) {
    validateRequest(request);
    Playlist playlist = createPlaylist(request);
    addTracks(playlist, request.getTracks());
    applySettings(playlist, request.getSettings());
}
```

## Code Organization

### 1. Package Structure

**Standard Layout:**
```
com.focushive.[service]/
├── controller/       # REST API endpoints
├── service/         # Business logic
├── repository/      # Data access layer
├── model/          # JPA entities
├── dto/            # Data transfer objects
├── exception/      # Custom exceptions
├── config/         # Configuration classes
├── event/          # Event publishers/listeners
└── util/           # Utility classes
```

### 2. Dependency Management

**Principles:**
- Constructor injection preferred over field injection
- Use `@RequiredArgsConstructor` from Lombok
- Avoid circular dependencies
- Maximum dependencies per class: 8

**Example:**
```java
@Service
@RequiredArgsConstructor  // Preferred
public class AnalyticsService {
    private final SessionRepository sessionRepository;
    private final DailySummaryRepository summaryRepository;
    private final UserService userService;
    private final HiveService hiveService;
}
```

## Testing Standards

### 1. Test Coverage Requirements

**Minimum Coverage:**
- Overall: 80%
- Critical paths: 95%
- New code: 90%

**Test Categories:**
- Unit tests: Test individual components
- Integration tests: Test component interactions
- E2E tests: Test complete user workflows

### 2. Test Naming Convention

```java
@Test
void methodName_StateUnderTest_ExpectedBehavior() {
    // Example
    void startSession_UserHasActiveSession_ThrowsException() {
        // test implementation
    }
}
```

## API Design Standards

### 1. RESTful Conventions

**URL Patterns:**
```
GET    /api/v1/resource          # List resources
GET    /api/v1/resource/{id}     # Get single resource
POST   /api/v1/resource          # Create resource
PUT    /api/v1/resource/{id}     # Update resource
DELETE /api/v1/resource/{id}     # Delete resource
```

**Response Format:**
```json
{
  "success": true,
  "data": { },
  "message": "Operation successful",
  "timestamp": "2025-08-09T12:00:00Z"
}
```

### 2. Error Handling

**Standard Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested resource was not found",
    "details": { }
  },
  "timestamp": "2025-08-09T12:00:00Z"
}
```

**HTTP Status Codes:**
- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 422: Unprocessable Entity
- 500: Internal Server Error

## Performance Standards

### 1. Database Queries

**Guidelines:**
- Use pagination for list endpoints
- Implement proper indexing
- Avoid N+1 queries
- Use projections for read-only operations

**Example:**
```java
// Good - with pagination
Page<Playlist> getUserPlaylists(Long userId, Pageable pageable) {
    return playlistRepository.findByUserId(userId, pageable);
}

// Bad - loading everything
List<Playlist> getAllUserPlaylists(Long userId) {
    return playlistRepository.findAllByUserId(userId);
}
```

### 2. Caching Strategy

**Cache Levels:**
- User data: 6 hours
- Session data: 1 hour
- Analytics: 30 minutes
- Real-time data: No caching

**Implementation:**
```java
@Cacheable(value = "userStats", key = "#userId")
public UserStats getUserStatistics(Long userId) {
    // Expensive calculation
}

@CacheEvict(value = "userStats", key = "#userId")
public void invalidateUserStats(Long userId) {
    // Clear cache when data changes
}
```

## Security Standards

### 1. Authentication & Authorization

**Requirements:**
- All endpoints require JWT authentication (except health checks)
- Role-based access control (RBAC)
- Method-level security annotations

**Example:**
```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public UserStats getUserStatistics(Long userId) {
    // Implementation
}
```

### 2. Data Protection

**Guidelines:**
- Never log passwords or tokens
- Encrypt sensitive data at rest
- Use HTTPS for all communications
- Validate and sanitize all inputs

## Documentation Standards

### 1. Code Documentation

**JavaDoc Requirements:**
```java
/**
 * Generates music recommendations based on user preferences and context.
 * 
 * @param request the recommendation request containing user preferences
 * @return a list of recommended tracks with metadata
 * @throws RecommendationException if generation fails
 * @since 1.0.0
 */
public RecommendationResponse generateRecommendations(RecommendationRequest request) {
    // Implementation
}
```

### 2. API Documentation

**OpenAPI/Swagger:**
- All endpoints must be documented
- Include request/response examples
- Document error responses
- Specify required vs optional parameters

## Code Review Checklist

### Pre-Commit Checklist

- [ ] No console.log/debug statements in production code
- [ ] All tests pass
- [ ] Code follows naming conventions
- [ ] Complex logic is documented
- [ ] No hardcoded values
- [ ] Error handling is comprehensive
- [ ] Security considerations addressed
- [ ] Performance impact assessed

### Review Focus Areas

1. **Business Logic**: Correctness and completeness
2. **Code Quality**: Readability and maintainability
3. **Performance**: Efficiency and scalability
4. **Security**: Vulnerabilities and data protection
5. **Testing**: Coverage and edge cases
6. **Documentation**: Clarity and completeness

## Continuous Improvement

### Regular Audits

**Quarterly Reviews:**
- Code quality metrics analysis
- Technical debt assessment
- Performance bottleneck identification
- Security vulnerability scanning

**Metrics Tracked:**
- Code coverage percentage
- Cyclomatic complexity
- Technical debt ratio
- Bug density
- Performance benchmarks

### Recent Improvements (August 2025)

1. **Console Logging Cleanup**: Removed 70+ console statements from frontend
2. **Debug Logging Optimization**: Added proper gating to 23 Java service files
3. **Service Refactoring**: Split 900+ line service into 4 focused services
4. **Analytics Implementation**: Completed 4 core analytics endpoints

## Enforcement

### Automated Tools

1. **ESLint**: Frontend code quality
2. **SonarQube**: Backend code analysis
3. **Git Hooks**: Pre-commit validation
4. **CI/CD Pipeline**: Automated testing and quality gates

### Manual Processes

1. **Code Reviews**: Required for all PRs
2. **Architecture Reviews**: For significant changes
3. **Security Reviews**: For sensitive features
4. **Performance Testing**: Before major releases

---

**Last Updated**: August 9, 2025  
**Version**: 1.0.0  
**Status**: Active  
**Next Review**: Q4 2025