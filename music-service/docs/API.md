# FocusHive Music Service API Documentation

## Overview

The FocusHive Music Service provides a comprehensive REST API for music recommendations, playlist management, Spotify integration, and collaborative music features. All endpoints require JWT authentication and return JSON responses.

**Base URL**: `http://localhost:8084/api/v1/music`

**Authentication**: All endpoints require a valid JWT token in the Authorization header:
```http
Authorization: Bearer <your-jwt-token>
```

## Table of Contents

1. [Enhanced Music Recommendations](#enhanced-music-recommendations)
2. [Playlist Management](#playlist-management)
3. [Streaming Integration](#streaming-integration)
4. [Collaborative Features](#collaborative-features)
5. [Common Response Formats](#common-response-formats)
6. [Error Handling](#error-handling)
7. [Rate Limiting](#rate-limiting)

---

## Enhanced Music Recommendations

### Generate Session-Based Recommendations

Generates personalized music recommendations for a user session based on task type, mood, and context.

**Endpoint**: `POST /recommendations/sessions`

**Request Body**:
```json
{
  "sessionId": "uuid-string",
  "taskType": "DEEP_WORK",
  "mood": "FOCUSED",
  "expectedDuration": 120,
  "environment": "OFFICE",
  "deviceType": "DESKTOP",
  "collaborativeSession": false,
  "hiveId": "uuid-string",
  "energyLevel": 7,
  "maxRecommendations": 20,
  "includeSeeds": true,
  "diversityLevel": "MEDIUM",
  "noveltyLevel": "LOW"
}
```

**Request Parameters**:
- `sessionId` (string, optional): Unique session identifier for tracking
- `taskType` (string, required): One of: DEEP_WORK, CREATIVE, ADMINISTRATIVE, CODING, STUDYING, RESEARCH, BRAINSTORMING, CASUAL
- `mood` (string, required): One of: FOCUSED, ENERGETIC, RELAXED, STRESSED, CREATIVE, MELANCHOLIC, HAPPY, NEUTRAL, TIRED, ANXIOUS
- `expectedDuration` (integer, optional): Expected session duration in minutes
- `environment` (string, optional): OFFICE, HOME, CAFE, LIBRARY, OUTDOORS
- `deviceType` (string, optional): DESKTOP, MOBILE, TABLET
- `collaborativeSession` (boolean, optional): Whether this is a collaborative session
- `hiveId` (string, optional): Hive ID for collaborative recommendations
- `energyLevel` (integer, optional): User's current energy level (1-10)
- `maxRecommendations` (integer, optional): Maximum number of recommendations (default: 20, max: 50)
- `includeSeeds` (boolean, optional): Include seed information in response
- `diversityLevel` (string, optional): VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
- `noveltyLevel` (string, optional): VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH

**Response**:
```json
{
  "success": true,
  "data": {
    "recommendationId": "uuid-string",
    "tracks": [
      {
        "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
        "name": "Clair de Lune",
        "artist": "Claude Debussy",
        "album": "Suite Bergamasque",
        "duration": 300000,
        "preview_url": "https://p.scdn.co/mp3-preview/...",
        "external_urls": {
          "spotify": "https://open.spotify.com/track/..."
        },
        "confidence": 0.89,
        "reason": "Perfect for deep work - instrumental, calming",
        "audioFeatures": {
          "energy": 0.25,
          "valence": 0.4,
          "instrumentalness": 0.95,
          "tempo": 73.5
        }
      }
    ],
    "metadata": {
      "totalTracks": 20,
      "averageConfidence": 0.85,
      "diversityScore": 0.72,
      "noveltyScore": 0.15,
      "algorithms": ["content_based", "productivity_correlation"],
      "cacheHit": false,
      "generationTimeMs": 1250
    },
    "seeds": {
      "artists": ["0OdUWJ0sBjDrqHygGUXeCF"],
      "tracks": ["4iV5W9uYEdYUVa79Axb7Rh"],
      "genres": ["classical", "ambient"]
    },
    "context": {
      "taskType": "DEEP_WORK",
      "mood": "FOCUSED",
      "targetAudioFeatures": {
        "energy": 0.3,
        "valence": 0.4,
        "instrumentalness": 0.8
      }
    }
  },
  "timestamp": "2025-08-07T10:30:00Z"
}
```

### Get Task-Specific Recommendations

Get recommendations optimized for a specific task type.

**Endpoint**: `POST /recommendations/tasks/{taskType}`

**Path Parameters**:
- `taskType` (string, required): The task type for recommendations

**Request Body**:
```json
{
  "mood": "FOCUSED",
  "expectedDuration": 90,
  "maxRecommendations": 15,
  "userContext": {
    "energyLevel": 6,
    "environment": "HOME",
    "deviceType": "DESKTOP"
  }
}
```

**Response**: Same format as session-based recommendations

### Get Mood-Based Recommendations

Get recommendations optimized for a specific mood.

**Endpoint**: `POST /recommendations/moods/{mood}`

**Path Parameters**:
- `mood` (string, required): The mood for recommendations

**Request Body**:
```json
{
  "taskType": "CASUAL",
  "expectedDuration": 60,
  "maxRecommendations": 25,
  "includeUpbeat": true
}
```

### Submit Recommendation Feedback

Submit feedback for a recommendation to improve future suggestions.

**Endpoint**: `POST /recommendations/{recommendationId}/feedback`

**Path Parameters**:
- `recommendationId` (string, required): The recommendation ID

**Request Body**:
```json
{
  "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
  "feedbackType": "EXPLICIT_RATING",
  "overallRating": 4,
  "liked": true,
  "interactionType": "COMPLETED",
  "productivityImpact": 8,
  "focusEnhancement": 9,
  "moodAppropriateness": 7,
  "taskSuitability": 8,
  "feedbackText": "Perfect for deep work sessions",
  "listeningBehavior": {
    "listenDurationSeconds": 285,
    "completionPercentage": 0.95,
    "repeatCount": 1,
    "timeToSkipSeconds": null,
    "volumeAdjusted": false,
    "pausedDuringPlay": false,
    "userSeeked": false
  },
  "context": {
    "taskType": "DEEP_WORK",
    "mood": "FOCUSED",
    "sessionDuration": 90,
    "environment": "HOME",
    "soloListening": true
  }
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "feedbackId": "uuid-string",
    "processed": true,
    "influencesFuture": true,
    "confidenceLevel": 4
  },
  "message": "Feedback recorded successfully"
}
```

### Batch Feedback Submission

Submit multiple feedback entries in a single request.

**Endpoint**: `POST /recommendations/{recommendationId}/feedback/batch`

**Request Body**:
```json
{
  "feedbackEntries": [
    {
      "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
      "overallRating": 4,
      "liked": true,
      "completionPercentage": 0.95
    },
    {
      "trackId": "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
      "overallRating": 2,
      "liked": false,
      "skipReason": "TOO_ENERGETIC_FOR_TASK"
    }
  ]
}
```

### Get Recommendation History

Retrieve user's recommendation history with performance metrics.

**Endpoint**: `GET /recommendations/history`

**Query Parameters**:
- `limit` (integer, optional): Number of recommendations to return (default: 20, max: 100)
- `offset` (integer, optional): Number of recommendations to skip
- `taskType` (string, optional): Filter by task type
- `mood` (string, optional): Filter by mood
- `from` (string, optional): Start date (ISO 8601 format)
- `to` (string, optional): End date (ISO 8601 format)
- `minRating` (number, optional): Minimum average rating filter

**Response**:
```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "recommendationId": "uuid-string",
        "createdAt": "2025-08-07T10:30:00Z",
        "taskType": "DEEP_WORK",
        "mood": "FOCUSED",
        "totalTracks": 20,
        "averageRating": 4.2,
        "acceptanceRate": 0.75,
        "productivityScore": 0.82,
        "diversityScore": 0.68,
        "algorithmVersion": "v2.1.0"
      }
    ],
    "pagination": {
      "total": 150,
      "limit": 20,
      "offset": 0,
      "hasMore": true
    }
  }
}
```

### Get Personal Analytics

Get detailed analytics about user's music preferences and recommendation performance.

**Endpoint**: `GET /recommendations/analytics`

**Query Parameters**:
- `period` (string, optional): Time period - DAY, WEEK, MONTH, QUARTER, YEAR (default: MONTH)
- `includeComparison` (boolean, optional): Include comparison with previous period

**Response**:
```json
{
  "success": true,
  "data": {
    "period": "MONTH",
    "summary": {
      "totalRecommendations": 45,
      "averageRating": 4.1,
      "acceptanceRate": 0.78,
      "avgProductivityScore": 0.75,
      "topTaskType": "DEEP_WORK",
      "topMood": "FOCUSED"
    },
    "trends": {
      "ratingTrend": "IMPROVING",
      "acceptanceTrend": "STABLE",
      "productivityTrend": "IMPROVING"
    },
    "preferences": {
      "topGenres": ["classical", "ambient", "lo-fi"],
      "topArtists": ["Ludovico Einaudi", "Max Richter", "Nils Frahm"],
      "audioFeaturePreferences": {
        "energy": 0.35,
        "valence": 0.42,
        "instrumentalness": 0.78
      }
    },
    "taskTypePerformance": [
      {
        "taskType": "DEEP_WORK",
        "count": 20,
        "averageRating": 4.3,
        "productivityScore": 0.85
      }
    ],
    "comparison": {
      "previousPeriod": "JULY_2025",
      "changes": {
        "ratingChange": 0.3,
        "acceptanceRateChange": 0.05,
        "productivityChange": 0.12
      }
    }
  }
}
```

### Get Trending Recommendations

Get community trends and popular tracks.

**Endpoint**: `GET /recommendations/trending`

**Query Parameters**:
- `timeframe` (string, optional): DAY, WEEK, MONTH (default: WEEK)
- `taskType` (string, optional): Filter by task type
- `limit` (integer, optional): Number of trends to return (default: 20)

**Response**:
```json
{
  "success": true,
  "data": {
    "timeframe": "WEEK",
    "trending": {
      "tracks": [
        {
          "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
          "name": "Clair de Lune",
          "artist": "Claude Debussy",
          "recommendationCount": 145,
          "averageRating": 4.6,
          "trendScore": 0.89
        }
      ],
      "genres": [
        {
          "genre": "classical",
          "count": 1230,
          "growth": 0.15
        }
      ],
      "taskCombinations": [
        {
          "taskType": "DEEP_WORK",
          "mood": "FOCUSED",
          "count": 580,
          "averageRating": 4.4
        }
      ]
    }
  }
}
```

### Get User Statistics

Get comprehensive user statistics and performance metrics.

**Endpoint**: `GET /recommendations/stats`

**Response**:
```json
{
  "success": true,
  "data": {
    "userStats": {
      "totalRecommendations": 250,
      "totalFeedback": 180,
      "averageRating": 4.1,
      "acceptanceRate": 0.76,
      "streak": {
        "current": 7,
        "longest": 15
      }
    },
    "performance": {
      "topPerformingTaskType": "DEEP_WORK",
      "topPerformingMood": "FOCUSED",
      "bestProductivityScore": 0.92,
      "improvementRate": 0.08
    },
    "achievements": [
      {
        "name": "Music Connoisseur",
        "description": "Rated 100+ recommendations",
        "earnedAt": "2025-07-15T10:30:00Z"
      }
    ]
  }
}
```

### Clear User Cache

Clear user's recommendation cache to force fresh recommendations.

**Endpoint**: `DELETE /recommendations/cache`

**Response**:
```json
{
  "success": true,
  "message": "User recommendation cache cleared successfully",
  "data": {
    "itemsCleared": 15,
    "cacheTypes": ["recommendations", "preferences", "analytics"]
  }
}
```

---

## Playlist Management

### List User Playlists

Get all playlists created or accessible by the user.

**Endpoint**: `GET /playlists`

**Query Parameters**:
- `limit` (integer, optional): Number of playlists to return (default: 20)
- `offset` (integer, optional): Number of playlists to skip
- `includeCollaborative` (boolean, optional): Include collaborative playlists (default: true)
- `sortBy` (string, optional): CREATED_AT, UPDATED_AT, NAME (default: UPDATED_AT)
- `sortOrder` (string, optional): ASC, DESC (default: DESC)

**Response**:
```json
{
  "success": true,
  "data": {
    "playlists": [
      {
        "playlistId": "uuid-string",
        "name": "Deep Work Focus",
        "description": "Perfect tracks for deep work sessions",
        "isPublic": false,
        "isCollaborative": false,
        "trackCount": 25,
        "totalDuration": 6300000,
        "createdAt": "2025-07-01T10:00:00Z",
        "updatedAt": "2025-08-05T15:30:00Z",
        "creator": {
          "userId": "uuid-string",
          "username": "john_doe"
        },
        "collaboratorCount": 0
      }
    ],
    "pagination": {
      "total": 12,
      "limit": 20,
      "offset": 0,
      "hasMore": false
    }
  }
}
```

### Create New Playlist

Create a new playlist.

**Endpoint**: `POST /playlists`

**Request Body**:
```json
{
  "name": "Coding Playlist",
  "description": "Instrumental tracks for coding sessions",
  "isPublic": false,
  "isCollaborative": false,
  "initialTracks": [
    "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
    "spotify:track:7qiZfU4dY1lWllzX7mPBI3"
  ],
  "tags": ["coding", "instrumental", "focus"]
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "playlistId": "uuid-string",
    "name": "Coding Playlist",
    "description": "Instrumental tracks for coding sessions",
    "isPublic": false,
    "isCollaborative": false,
    "trackCount": 2,
    "createdAt": "2025-08-07T10:30:00Z"
  },
  "message": "Playlist created successfully"
}
```

### Get Playlist Details

Get detailed information about a specific playlist.

**Endpoint**: `GET /playlists/{playlistId}`

**Path Parameters**:
- `playlistId` (string, required): The playlist ID

**Response**:
```json
{
  "success": true,
  "data": {
    "playlistId": "uuid-string",
    "name": "Deep Work Focus",
    "description": "Perfect tracks for deep work sessions",
    "isPublic": false,
    "isCollaborative": false,
    "trackCount": 25,
    "totalDuration": 6300000,
    "createdAt": "2025-07-01T10:00:00Z",
    "updatedAt": "2025-08-05T15:30:00Z",
    "creator": {
      "userId": "uuid-string",
      "username": "john_doe"
    },
    "tracks": [
      {
        "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
        "name": "Clair de Lune",
        "artist": "Claude Debussy",
        "album": "Suite Bergamasque",
        "duration": 300000,
        "addedAt": "2025-07-01T10:05:00Z",
        "addedBy": "uuid-string",
        "position": 1
      }
    ],
    "collaborators": [],
    "tags": ["deep-work", "classical", "instrumental"]
  }
}
```

### Update Playlist

Update playlist information.

**Endpoint**: `PUT /playlists/{playlistId}`

**Request Body**:
```json
{
  "name": "Updated Playlist Name",
  "description": "Updated description",
  "isPublic": true,
  "tags": ["updated", "tags"]
}
```

### Delete Playlist

Delete a playlist.

**Endpoint**: `DELETE /playlists/{playlistId}`

**Response**:
```json
{
  "success": true,
  "message": "Playlist deleted successfully"
}
```

### Add Tracks to Playlist

Add tracks to an existing playlist.

**Endpoint**: `POST /playlists/{playlistId}/tracks`

**Request Body**:
```json
{
  "trackIds": [
    "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
    "spotify:track:7qiZfU4dY1lWllzX7mPBI3"
  ],
  "position": 5
}
```

### Remove Track from Playlist

Remove a specific track from a playlist.

**Endpoint**: `DELETE /playlists/{playlistId}/tracks/{trackId}`

**Response**:
```json
{
  "success": true,
  "message": "Track removed from playlist successfully"
}
```

---

## Streaming Integration

### Initiate Spotify OAuth2 Flow

Start the Spotify authentication process.

**Endpoint**: `GET /streaming/spotify/auth`

**Query Parameters**:
- `scopes` (string, optional): Comma-separated list of required scopes
- `redirectUrl` (string, optional): Custom redirect URL

**Response**:
```json
{
  "success": true,
  "data": {
    "authUrl": "https://accounts.spotify.com/authorize?client_id=...&state=...",
    "state": "random-state-string",
    "scopes": ["streaming", "user-read-email", "playlist-read-private"]
  }
}
```

### OAuth2 Callback Handler

Handle the Spotify OAuth2 callback (typically called by Spotify, not directly).

**Endpoint**: `GET /streaming/spotify/callback`

**Query Parameters**:
- `code` (string, required): Authorization code from Spotify
- `state` (string, required): State parameter for verification
- `error` (string, optional): Error from Spotify

### Disconnect Spotify Account

Disconnect and remove Spotify integration.

**Endpoint**: `POST /streaming/spotify/disconnect`

**Response**:
```json
{
  "success": true,
  "message": "Spotify account disconnected successfully",
  "data": {
    "disconnectedAt": "2025-08-07T10:30:00Z"
  }
}
```

### Get Connected Spotify Profile

Get information about the connected Spotify account.

**Endpoint**: `GET /streaming/spotify/profile`

**Response**:
```json
{
  "success": true,
  "data": {
    "spotifyUserId": "spotify_user_123",
    "displayName": "John Doe",
    "email": "john@example.com",
    "country": "US",
    "product": "premium",
    "followers": 42,
    "connectedAt": "2025-07-01T10:00:00Z",
    "scopes": ["streaming", "user-read-email", "playlist-read-private"],
    "hasValidToken": true
  }
}
```

### Refresh Access Tokens

Manually refresh Spotify access tokens.

**Endpoint**: `POST /streaming/spotify/refresh`

**Response**:
```json
{
  "success": true,
  "data": {
    "tokenRefreshed": true,
    "refreshedAt": "2025-08-07T10:30:00Z",
    "expiresIn": 3600
  }
}
```

---

## Collaborative Features

### Create Collaborative Playlist

Create a new collaborative playlist for group listening.

**Endpoint**: `POST /collaborative/playlists`

**Request Body**:
```json
{
  "name": "Hive Collaboration Session",
  "description": "Music for our team work session",
  "hiveId": "uuid-string",
  "maxContributors": 10,
  "votingEnabled": true,
  "allowAddTracks": true,
  "allowRemoveTracks": false,
  "moderationLevel": "DEMOCRATIC"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "playlistId": "uuid-string",
    "name": "Hive Collaboration Session",
    "collaborationCode": "ABC123",
    "createdAt": "2025-08-07T10:30:00Z",
    "permissions": {
      "votingEnabled": true,
      "allowAddTracks": true,
      "allowRemoveTracks": false
    }
  }
}
```

### Get Collaborative Queue

Get the current collaborative playlist queue with voting information.

**Endpoint**: `GET /collaborative/playlists/{playlistId}/queue`

**Response**:
```json
{
  "success": true,
  "data": {
    "playlistId": "uuid-string",
    "currentTrack": {
      "trackId": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
      "name": "Clair de Lune",
      "artist": "Claude Debussy",
      "votes": 5,
      "addedBy": "uuid-string",
      "isPlaying": true
    },
    "queue": [
      {
        "trackId": "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
        "name": "Song Title",
        "artist": "Artist Name",
        "votes": 3,
        "addedBy": "uuid-string",
        "position": 1
      }
    ],
    "totalVotes": 15,
    "activeMembers": 6
  }
}
```

### Vote on Queued Track

Vote for or against a track in the collaborative queue.

**Endpoint**: `POST /collaborative/playlists/{playlistId}/vote`

**Request Body**:
```json
{
  "trackId": "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
  "voteType": "UPVOTE"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "trackId": "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
    "userVote": "UPVOTE",
    "totalVotes": 4,
    "newPosition": 1
  }
}
```

### Join Collaborative Session

Join an existing collaborative playlist session.

**Endpoint**: `POST /collaborative/playlists/{playlistId}/join`

**Request Body**:
```json
{
  "collaborationCode": "ABC123"
}
```

### Leave Collaborative Session

Leave a collaborative playlist session.

**Endpoint**: `POST /collaborative/playlists/{playlistId}/leave`

### Get Session Members

Get list of active members in a collaborative session.

**Endpoint**: `GET /collaborative/playlists/{playlistId}/members`

**Response**:
```json
{
  "success": true,
  "data": {
    "members": [
      {
        "userId": "uuid-string",
        "username": "john_doe",
        "joinedAt": "2025-08-07T10:00:00Z",
        "role": "OWNER",
        "isActive": true,
        "contributions": 5
      }
    ],
    "totalMembers": 6,
    "activeMembers": 4
  }
}
```

---

## Common Response Formats

### Success Response Format
```json
{
  "success": true,
  "data": {
    // Response data here
  },
  "message": "Optional success message",
  "timestamp": "2025-08-07T10:30:00Z"
}
```

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": [
      {
        "field": "taskType",
        "message": "Task type is required"
      }
    ]
  },
  "timestamp": "2025-08-07T10:30:00Z",
  "requestId": "uuid-string"
}
```

### Pagination Format
```json
{
  "pagination": {
    "total": 150,
    "limit": 20,
    "offset": 0,
    "hasMore": true,
    "nextOffset": 20,
    "prevOffset": null
  }
}
```

---

## Error Handling

### HTTP Status Codes
- `200 OK`: Successful request
- `201 Created`: Resource created successfully
- `204 No Content`: Successful request with no response body
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource conflict (e.g., duplicate playlist name)
- `422 Unprocessable Entity`: Validation errors
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error
- `502 Bad Gateway`: External service error (e.g., Spotify API)
- `503 Service Unavailable`: Service temporarily unavailable

### Error Codes
- `VALIDATION_ERROR`: Request validation failed
- `AUTHENTICATION_ERROR`: Invalid or missing authentication
- `AUTHORIZATION_ERROR`: Insufficient permissions
- `RESOURCE_NOT_FOUND`: Requested resource not found
- `SPOTIFY_ERROR`: Spotify API error
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `CACHE_ERROR`: Caching system error
- `DATABASE_ERROR`: Database operation error
- `ALGORITHM_ERROR`: Recommendation algorithm error
- `EXTERNAL_SERVICE_ERROR`: External service error

### Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Authentication endpoints**: 10 requests per minute per IP
- **Recommendation endpoints**: 60 requests per minute per user
- **Playlist management**: 30 requests per minute per user
- **Feedback submission**: 100 requests per minute per user
- **Analytics endpoints**: 20 requests per minute per user

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1691404800
```

When rate limit is exceeded, a `429 Too Many Requests` response is returned:
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "retryAfter": 60
  }
}
```

---

## WebSocket Events

The Music Service also supports WebSocket connections for real-time features:

**Connection URL**: `ws://localhost:8084/ws/music`

### Events Published
- `RECOMMENDATION_GENERATED`: New recommendations available
- `FEEDBACK_RECEIVED`: Feedback processed
- `COLLABORATIVE_QUEUE_UPDATED`: Collaborative playlist queue changed
- `MEMBER_JOINED`: New member joined collaborative session
- `VOTING_COMPLETED`: Track voting results updated

### Events Subscribed
- `REQUEST_RECOMMENDATIONS`: Request new recommendations
- `SUBMIT_FEEDBACK`: Submit recommendation feedback
- `JOIN_COLLABORATIVE_SESSION`: Join collaborative playlist
- `VOTE_TRACK`: Vote on collaborative track

For detailed WebSocket documentation, see the WebSocket Integration Guide.

---

## SDK and Client Libraries

Official client libraries are available for:
- **JavaScript/TypeScript**: `@focushive/music-client`
- **React**: `@focushive/music-react-hooks`
- **Java**: `com.focushive:music-client-java`

Example usage with the React hooks library:
```typescript
import { useMusicRecommendations } from '@focushive/music-react-hooks';

const { recommendations, loading, error, generateRecommendations } = useMusicRecommendations();

// Generate recommendations
await generateRecommendations({
  taskType: 'DEEP_WORK',
  mood: 'FOCUSED',
  expectedDuration: 120
});
```

---

## Changelog

### v2.1.0 (Current)
- Enhanced recommendation algorithms with productivity correlation
- Advanced feedback system with implicit behavior tracking
- Collaborative playlist features with voting
- Comprehensive analytics and trending data
- Improved caching strategy with configurable TTLs

### v2.0.0
- Complete API redesign with RESTful endpoints
- Task and mood-based recommendations
- Spotify OAuth2 integration
- Real-time WebSocket support

### v1.0.0
- Initial release with basic recommendation features
- Simple playlist management
- Basic Spotify integration