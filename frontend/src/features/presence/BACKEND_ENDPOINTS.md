# Backend Endpoints Required for Presence Feature

## REST API Endpoints

### User Presence Endpoints
1. **GET /api/presence/users/{userId}**
   - Get current presence status for a specific user
   - Response: `UserPresence`

2. **PUT /api/presence/users/{userId}/status**
   - Update user's presence status
   - Request body: `{ status: PresenceStatus, currentActivity?: string }`
   - Response: `UserPresence`

3. **GET /api/presence/users/active**
   - Get all currently active users (online, focusing, break)
   - Query params: `?limit=50&offset=0`
   - Response: `UserPresence[]`

### Hive Presence Endpoints
4. **GET /api/hives/{hiveId}/presence**
   - Get all users' presence in a specific hive
   - Response: `HivePresenceInfo`

5. **POST /api/hives/{hiveId}/presence/join**
   - Join a hive's presence tracking
   - Response: `{ success: boolean, hivePresence: HivePresenceInfo }`

6. **DELETE /api/hives/{hiveId}/presence/leave**
   - Leave a hive's presence tracking
   - Response: `{ success: boolean }`

### Focus Session Endpoints
7. **POST /api/presence/sessions/start**
   - Start a new focus session
   - Request body: `{ hiveId?: string, targetDuration?: number, type: 'pomodoro' | 'continuous' | 'custom' }`
   - Response: `FocusSession`

8. **PUT /api/presence/sessions/{sessionId}/end**
   - End an active focus session
   - Request body: `{ productivity?: { rating: number, notes?: string } }`
   - Response: `FocusSession`

9. **POST /api/presence/sessions/{sessionId}/breaks**
   - Take a break during a session
   - Request body: `{ type: 'short' | 'long' | 'lunch' }`
   - Response: `SessionBreak`

10. **PUT /api/presence/sessions/{sessionId}/breaks/{breakId}/resume**
    - Resume from a break
    - Response: `FocusSession`

### Activity Feed Endpoints
11. **GET /api/presence/activities**
    - Get recent activity events
    - Query params: `?hiveId={hiveId}&limit=50`
    - Response: `ActivityEvent[]`

## WebSocket Events

### Client -> Server Events
- `presence.update`: Update user's presence status
- `presence.join.hive`: Join a hive's presence tracking
- `presence.leave.hive`: Leave a hive's presence tracking
- `session.start`: Start a focus session
- `session.end`: End a focus session
- `session.break.start`: Start a break
- `session.break.end`: End a break

### Server -> Client Events
- `presence.user.updated`: User's presence status changed
- `presence.hive.updated`: Hive presence info updated
- `presence.user.joined`: User joined a hive
- `presence.user.left`: User left a hive
- `session.started`: Focus session started
- `session.ended`: Focus session ended
- `activity.new`: New activity event

## Example WebSocket Connection
```
ws://localhost:8080/ws/presence

// Subscribe to hive presence updates
STOMP.subscribe('/topic/hive/{hiveId}/presence', (message) => {
  // Handle presence updates for the hive
})

// Subscribe to user-specific presence updates
STOMP.subscribe('/user/queue/presence', (message) => {
  // Handle personal presence updates
})
```

## Data Models (matching TypeScript types)
- `UserPresence`: User's current presence information
- `PresenceStatus`: 'online' | 'focusing' | 'break' | 'away' | 'offline'
- `HivePresenceInfo`: Aggregated presence data for a hive
- `FocusSession`: Active focus session details
- `SessionBreak`: Break within a focus session
- `ActivityEvent`: User activity in the system

## Notes for Backend Implementation
1. Presence status should automatically change to 'away' after 5 minutes of inactivity
2. Presence status should change to 'offline' after 15 minutes of inactivity
3. WebSocket heartbeat should be implemented to detect disconnections
4. Presence data should be cached in Redis for real-time performance
5. Activity events should be stored in PostgreSQL for historical tracking