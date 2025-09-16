# Buddy System Hooks

This directory contains React hooks for the buddy system that integrate with React Query (TanStack Query) for state management.

## Available Hooks

### Profile Management (`useBuddyProfile.ts`)
- `useBuddyProfile()` - Get current user's buddy profile
- `useOtherBuddyProfile(userId)` - Get another user's buddy profile
- `useUpdateBuddyPreferences()` - Update preferences
- `useUpdateBuddyAvailability()` - Update availability
- `useBuddyProfileManagement()` - Combined hook for profile management

### Buddy Matching (`useBuddyMatching.ts`)
- `useSearchBuddies(filters)` - Search for available buddies
- `usePendingMatches()` - Get pending matches
- `useBuddyMatch()` - Request a buddy match
- `useAcceptMatch()` - Accept a match
- `useDeclineMatch()` - Decline a match
- `useCancelMatch()` - Cancel a match
- `useBuddyMatching()` - Combined hook for matching functionality

### Session Management (`useBuddySessions.ts`)
- `useCreateSession()` - Create a new session
- `useActiveSessions()` - Get current active sessions
- `useActiveSession()` - Get the current active session
- `useSessionHistory()` - Get past sessions
- `useBuddySession(sessionId)` - Get specific session with real-time updates
- `useSessionActions()` - Start/pause/resume/end session
- `useSessionCheckIn()` - Check in during session
- `useBuddySessions()` - Combined hook for session management

### Statistics (`useBuddyStats.ts`)
- `useBuddyStats()` - Get personal stats
- `useTopBuddies()` - Get top buddies
- `useBuddyLeaderboard(period)` - Get leaderboard
- `useBuddyStatistics()` - Combined hook for all statistics

### Invitations (`useBuddyInvitations.ts`)
- `useReceivedInvitations()` - Get received invitations
- `useSendInvitation()` - Send buddy invitation
- `useAcceptInvitation()` - Accept invitation
- `useDeclineInvitation()` - Decline invitation
- `useBuddyInvitations()` - Combined hook for invitations

### Messaging (`useBuddyMessages.ts`)
- `useSessionMessages(sessionId)` - Get session messages with real-time updates
- `useSendMessage()` - Send message during session
- `useAddReaction()` - Add emoji reaction
- `useBuddyMessages(sessionId)` - Combined hook for messaging

## Usage Examples

### Basic Profile Management
```typescript
import { useBuddyProfileManagement } from './hooks';

function BuddyProfile() {
  const {
    profile,
    isLoading,
    updatePreferences,
    updateAvailability,
    isUpdatingPreferences,
    isUpdatingAvailability
  } = useBuddyProfileManagement();

  if (isLoading) return <div>Loading...</div>;

  return (
    <div>
      <h1>{profile?.username}</h1>
      <button
        onClick={() => updatePreferences({ autoAccept: true })}
        disabled={isUpdatingPreferences}
      >
        Enable Auto-Accept
      </button>
    </div>
  );
}
```

### Buddy Matching Flow
```typescript
import { useBuddyMatching, useSearchBuddies } from './hooks';

function BuddyFinder() {
  const { requestMatch, pendingMatches, acceptMatch } = useBuddyMatching();
  const { data: buddies } = useSearchBuddies({
    sessionType: 'focus',
    minRating: 4.0
  });

  return (
    <div>
      {buddies?.buddies.map(buddy => (
        <div key={buddy.userId}>
          <h3>{buddy.username}</h3>
          <button onClick={() => requestMatch({
            userId: buddy.userId,
            sessionType: 'focus',
            duration: 60
          })}>
            Request Match
          </button>
        </div>
      ))}

      <h2>Pending Matches</h2>
      {pendingMatches.map(match => (
        <div key={match.id}>
          <span>{match.requester.username} wants to study!</span>
          <button onClick={() => acceptMatch(match.id)}>
            Accept
          </button>
        </div>
      ))}
    </div>
  );
}
```

### Session Management
```typescript
import { useBuddySessions, useBuddyMessages } from './hooks';

function BuddySession({ sessionId }: { sessionId: string }) {
  const {
    currentSession,
    startSession,
    pauseSession,
    endSession,
    checkIn
  } = useBuddySessions();

  const {
    messages,
    sendTextMessage,
    sendEncouragement,
    addReaction
  } = useBuddyMessages(sessionId);

  return (
    <div>
      {currentSession && (
        <div>
          <h2>Current Session: {currentSession.sessionType}</h2>
          <button onClick={() => pauseSession(currentSession.id)}>
            Pause
          </button>
          <button onClick={() => endSession(currentSession.id)}>
            End Session
          </button>
          <button onClick={() => checkIn(currentSession.id, {
            status: 'on_track',
            message: 'Making good progress!',
            progress: 75
          })}>
            Check In
          </button>
        </div>
      )}

      <div>
        <h3>Messages</h3>
        {messages.map(message => (
          <div key={message.id}>
            <span>{message.content}</span>
            <button onClick={() => addReaction({
              messageId: message.id,
              emoji: '👍'
            })}>
              👍
            </button>
          </div>
        ))}

        <button onClick={() => sendTextMessage('Hello!')}>
          Send Message
        </button>
        <button onClick={() => sendEncouragement('Keep going!')}>
          Send Encouragement
        </button>
      </div>
    </div>
  );
}
```

### Statistics Dashboard
```typescript
import { useBuddyStatistics } from './hooks';

function StatsPage() {
  const {
    stats,
    topBuddies,
    weeklyLeaderboard,
    totalSessionTime,
    completionRate,
    currentStreak
  } = useBuddyStatistics();

  return (
    <div>
      <h1>Your Stats</h1>
      <div>
        <p>Total Hours: {totalSessionTime}</p>
        <p>Completion Rate: {completionRate}%</p>
        <p>Current Streak: {currentStreak} days</p>
      </div>

      <h2>Top Buddies</h2>
      {topBuddies.map(buddy => (
        <div key={buddy.userId}>
          {buddy.username} - {buddy.sessionCount} sessions
        </div>
      ))}

      <h2>Weekly Leaderboard</h2>
      {weeklyLeaderboard.map((user, index) => (
        <div key={user.userId}>
          {index + 1}. {user.totalSessions} sessions
        </div>
      ))}
    </div>
  );
}
```

## Features

### Real-time Updates
- Sessions automatically update via WebSocket subscriptions
- Messages sync in real-time during buddy sessions
- Match updates are reflected immediately

### Optimistic Updates
- Messages appear immediately when sent
- Reactions are added instantly with server sync
- Match actions update UI before server confirmation

### Caching Strategy
- Profile data cached for 5-10 minutes
- Statistics cached for 5-15 minutes
- Real-time data (matches, messages) cached for 30 seconds - 2 minutes
- Automatic cache invalidation on mutations

### Error Handling
- All hooks include proper error states
- Failed mutations log errors to console
- Optimistic updates are reverted on error

### Loading States
- Individual loading states for each operation
- Combined loading states in aggregate hooks
- Proper loading indicators for better UX