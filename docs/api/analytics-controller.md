# Analytics Controller API Documentation

## Overview

The Analytics Controller provides comprehensive endpoints for tracking user productivity, managing focus sessions, and generating analytics insights within the FocusHive platform.

## Base URL

```
/api/v1/analytics
```

## Authentication

All endpoints require JWT authentication via Bearer token:

```http
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. Start Focus Session

Creates and starts a new focus session for the authenticated user.

#### Endpoint

```http
POST /api/v1/analytics/sessions/start
```

#### Request Body

```json
{
  "hiveId": 123,
  "taskType": "DEEP_WORK",
  "sessionTitle": "Implementing Analytics Features",
  "plannedDuration": 120,
  "notes": "Working on Linear task UOL-188"
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| hiveId | Long | No | ID of the hive for this session |
| taskType | String | Yes | Type of task (DEEP_WORK, CREATIVE, ADMINISTRATIVE, etc.) |
| sessionTitle | String | Yes | Title/description of the session |
| plannedDuration | Integer | Yes | Planned duration in minutes |
| notes | String | No | Additional notes for the session |

#### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 456,
    "userId": 789,
    "hiveId": 123,
    "taskType": "DEEP_WORK",
    "sessionTitle": "Implementing Analytics Features",
    "plannedDuration": 120,
    "actualDuration": null,
    "startTime": "2025-08-09T10:00:00Z",
    "endTime": null,
    "completed": false,
    "notes": "Working on Linear task UOL-188"
  },
  "message": "Session started successfully"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ACTIVE_SESSION_EXISTS | User already has an active session |
| 403 | HIVE_ACCESS_DENIED | User is not a member of the specified hive |
| 422 | INVALID_DURATION | Planned duration exceeds maximum allowed |

### 2. End Focus Session

Ends an active focus session and updates session statistics.

#### Endpoint

```http
POST /api/v1/analytics/sessions/{sessionId}/end
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| sessionId | Long | ID of the session to end |

#### Request Body

```json
{
  "actualDuration": 115,
  "completed": true,
  "completionNotes": "Completed all planned tasks successfully",
  "distractionCount": 2,
  "breakCount": 1
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| actualDuration | Integer | No | Actual duration in minutes (auto-calculated if not provided) |
| completed | Boolean | Yes | Whether the session was completed successfully |
| completionNotes | String | No | Notes about session completion |
| distractionCount | Integer | No | Number of distractions during session |
| breakCount | Integer | No | Number of breaks taken |

#### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 456,
    "userId": 789,
    "hiveId": 123,
    "taskType": "DEEP_WORK",
    "sessionTitle": "Implementing Analytics Features",
    "plannedDuration": 120,
    "actualDuration": 115,
    "startTime": "2025-08-09T10:00:00Z",
    "endTime": "2025-08-09T11:55:00Z",
    "completed": true,
    "notes": "Working on Linear task UOL-188",
    "completionNotes": "Completed all planned tasks successfully",
    "distractionCount": 2,
    "breakCount": 1
  },
  "message": "Session ended successfully"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 403 | UNAUTHORIZED | User doesn't own this session |
| 404 | SESSION_NOT_FOUND | Session doesn't exist |
| 422 | INVALID_STATE | Session is already ended |

### 3. Get User Statistics

Retrieves comprehensive productivity statistics for a user.

#### Endpoint

```http
GET /api/v1/analytics/users/{userId}/stats
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| userId | Long | ID of the user (use 'me' for current user) |

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| startDate | ISO Date | 30 days ago | Start date for statistics |
| endDate | ISO Date | Today | End date for statistics |
| includeHiveBreakdown | Boolean | false | Include per-hive statistics |

#### Response

```json
{
  "success": true,
  "data": {
    "userId": 789,
    "period": {
      "startDate": "2025-07-10",
      "endDate": "2025-08-09"
    },
    "sessionStats": {
      "totalSessions": 45,
      "completedSessions": 38,
      "completionRate": 84.4,
      "totalMinutes": 5420,
      "averageSessionLength": 120.4,
      "longestSession": 240,
      "shortestSession": 25
    },
    "streaks": {
      "currentStreak": 7,
      "longestStreak": 15,
      "streakStartDate": "2025-08-02"
    },
    "productivity": {
      "mostProductiveDay": "MONDAY",
      "mostProductiveTime": "09:00-11:00",
      "averageFocusScore": 8.2
    },
    "taskTypeBreakdown": {
      "DEEP_WORK": {
        "count": 20,
        "totalMinutes": 2400,
        "completionRate": 90.0
      },
      "CREATIVE": {
        "count": 15,
        "totalMinutes": 1800,
        "completionRate": 86.7
      },
      "ADMINISTRATIVE": {
        "count": 10,
        "totalMinutes": 1220,
        "completionRate": 70.0
      }
    },
    "hiveBreakdown": {
      "123": {
        "hiveName": "Morning Focus Group",
        "sessions": 25,
        "totalMinutes": 3000,
        "averageSessionLength": 120
      },
      "124": {
        "hiveName": "Deep Work Collective",
        "sessions": 20,
        "totalMinutes": 2420,
        "averageSessionLength": 121
      }
    },
    "trends": {
      "productivityTrend": "IMPROVING",
      "weekOverWeekChange": 12.5,
      "consistencyScore": 0.85
    }
  },
  "message": "Statistics retrieved successfully"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 403 | UNAUTHORIZED | Non-admin users can only view their own statistics |
| 404 | USER_NOT_FOUND | User doesn't exist |

### 4. Get Hive Leaderboard

Retrieves the productivity leaderboard for a specific hive.

#### Endpoint

```http
GET /api/v1/analytics/hives/{hiveId}/leaderboard
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| hiveId | Long | ID of the hive |

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| period | Enum | WEEK | Time period (DAY, WEEK, MONTH, ALL_TIME) |
| limit | Integer | 10 | Number of entries to return |
| includeCurrentUser | Boolean | true | Include current user's rank if not in top |

#### Response

```json
{
  "success": true,
  "data": {
    "hiveId": 123,
    "hiveName": "Morning Focus Group",
    "period": "WEEK",
    "startDate": "2025-08-02",
    "endDate": "2025-08-09",
    "leaderboard": [
      {
        "rank": 1,
        "userId": 101,
        "username": "alice_smith",
        "displayName": "Alice Smith",
        "totalMinutes": 1250,
        "sessionsCompleted": 12,
        "completionRate": 92.3,
        "averageSessionLength": 104.2,
        "change": 2
      },
      {
        "rank": 2,
        "userId": 102,
        "username": "bob_jones",
        "displayName": "Bob Jones",
        "totalMinutes": 1180,
        "sessionsCompleted": 10,
        "completionRate": 90.9,
        "averageSessionLength": 118.0,
        "change": -1
      },
      {
        "rank": 3,
        "userId": 103,
        "username": "carol_davis",
        "displayName": "Carol Davis",
        "totalMinutes": 1050,
        "sessionsCompleted": 9,
        "completionRate": 81.8,
        "averageSessionLength": 116.7,
        "change": 0
      }
    ],
    "currentUserRank": {
      "rank": 5,
      "userId": 789,
      "username": "current_user",
      "displayName": "Current User",
      "totalMinutes": 920,
      "sessionsCompleted": 8,
      "completionRate": 80.0,
      "averageSessionLength": 115.0,
      "change": 1
    },
    "statistics": {
      "totalParticipants": 25,
      "averageMinutes": 650,
      "topPerformerBonus": 50
    }
  },
  "message": "Leaderboard retrieved successfully"
}
```

#### Leaderboard Ranking Algorithm

Rankings are determined by:
1. **Primary**: Total productive minutes in the period
2. **Secondary**: Completion rate (if minutes are equal)
3. **Tertiary**: Number of completed sessions

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 403 | UNAUTHORIZED | User is not a member of this hive |
| 404 | HIVE_NOT_FOUND | Hive doesn't exist |

## Data Models

### SessionRequest

```java
public class SessionRequest {
    private Long hiveId;
    private String taskType;
    private String sessionTitle;
    private Integer plannedDuration;
    private String notes;
}
```

### SessionResponse

```java
public class SessionResponse {
    private Long sessionId;
    private Long userId;
    private Long hiveId;
    private String taskType;
    private String sessionTitle;
    private Integer plannedDuration;
    private Integer actualDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean completed;
    private String notes;
    private String completionNotes;
    private Integer distractionCount;
    private Integer breakCount;
}
```

### UserStats

```java
public class UserStats {
    private Long userId;
    private DateRange period;
    private SessionStatistics sessionStats;
    private StreakInfo streaks;
    private ProductivityMetrics productivity;
    private Map<String, TaskTypeStats> taskTypeBreakdown;
    private Map<Long, HiveStats> hiveBreakdown;
    private TrendAnalysis trends;
}
```

### LeaderboardEntry

```java
public class LeaderboardEntry {
    private Integer rank;
    private Long userId;
    private String username;
    private String displayName;
    private Integer totalMinutes;
    private Integer sessionsCompleted;
    private Double completionRate;
    private Double averageSessionLength;
    private Integer rankChange;
}
```

## Business Logic

### Session Management

1. **Concurrent Session Prevention**: Users cannot start a new session while one is active
2. **Hive Membership Validation**: Sessions can only be started in hives where the user is a member
3. **Automatic Duration Calculation**: If actual duration is not provided when ending a session, it's calculated from timestamps
4. **Daily Summary Updates**: Ending a session automatically updates the user's daily summary statistics

### Statistics Calculation

1. **Completion Rate**: (Completed Sessions / Total Sessions) * 100
2. **Current Streak**: Consecutive days with at least one completed session
3. **Focus Score**: Weighted average of completion rate, session consistency, and goal achievement
4. **Productivity Trend**: Calculated using linear regression on weekly focus scores

### Access Control

1. **Own Statistics**: Users can always view their own statistics
2. **Other Users' Statistics**: Only users with ADMIN role can view other users' statistics
3. **Hive Leaderboards**: Only accessible to members of the respective hive
4. **Session Ownership**: Users can only end their own sessions

## Performance Considerations

### Caching

- User statistics are cached for 30 minutes
- Leaderboard data is cached for 5 minutes
- Daily summaries are cached for 1 hour

### Database Optimization

- Indexes on user_id, created_at for efficient queries
- Partitioned tables for historical session data
- Materialized views for frequently accessed statistics

### Rate Limiting

- Session start: Maximum 1 per minute per user
- Statistics retrieval: Maximum 10 requests per minute
- Leaderboard access: Maximum 20 requests per minute

## Error Handling

All errors follow a consistent format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "Additional context"
    }
  },
  "timestamp": "2025-08-09T12:00:00Z"
}
```

## Integration Examples

### JavaScript/TypeScript

```typescript
// Start a session
const startSession = async (sessionData: SessionRequest): Promise<SessionResponse> => {
  const response = await fetch('/api/v1/analytics/sessions/start', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(sessionData)
  });
  
  if (!response.ok) {
    throw new Error(`Failed to start session: ${response.statusText}`);
  }
  
  const result = await response.json();
  return result.data;
};

// Get user statistics
const getUserStats = async (userId: string = 'me'): Promise<UserStats> => {
  const response = await fetch(`/api/v1/analytics/users/${userId}/stats`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  return result.data;
};
```

### Java/Spring

```java
@Service
public class AnalyticsClient {
    
    private final WebClient webClient;
    
    public Mono<SessionResponse> startSession(SessionRequest request) {
        return webClient.post()
            .uri("/api/v1/analytics/sessions/start")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> (SessionResponse) response.getData());
    }
    
    public Mono<UserStats> getUserStats(Long userId) {
        return webClient.get()
            .uri("/api/v1/analytics/users/{userId}/stats", userId)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> (UserStats) response.getData());
    }
}
```

## Webhooks & Events

The Analytics service publishes events that other services can subscribe to:

### Event Types

1. **session.started**: Fired when a new session begins
2. **session.ended**: Fired when a session ends
3. **milestone.achieved**: Fired when user reaches a productivity milestone
4. **streak.updated**: Fired when streak status changes

### Event Payload Example

```json
{
  "eventType": "session.ended",
  "timestamp": "2025-08-09T11:55:00Z",
  "data": {
    "sessionId": 456,
    "userId": 789,
    "hiveId": 123,
    "duration": 115,
    "completed": true
  }
}
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-08-09 | Initial implementation with 4 core endpoints |
| 1.1.0 | TBD | Add export functionality and advanced analytics |

---

**Last Updated**: August 9, 2025  
**API Version**: 1.0.0  
**Status**: Production Ready