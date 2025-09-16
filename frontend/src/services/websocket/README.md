# WebSocket Integration Guide

This document explains how to use the FocusHive WebSocket integration with Spring Boot STOMP
backend.

## Overview

The WebSocket integration provides real-time communication between the frontend and backend using:

- **STOMP protocol** over WebSocket
- **SockJS fallback** for older browsers
- **Automatic reconnection** with exponential backoff
- **Authentication integration** with JWT tokens
- **Environment-based configuration**

## Configuration

### Environment Variables

Add these variables to your `.env` file:

```env
# WebSocket Configuration
VITE_WEBSOCKET_URL=ws://localhost:8080
VITE_WEBSOCKET_RECONNECT_ATTEMPTS=10
VITE_WEBSOCKET_RECONNECT_DELAY=1000
VITE_WEBSOCKET_HEARTBEAT_INTERVAL=30000
```

### URL Format

- **Development**: `ws://localhost:8080` or `http://localhost:8080`
- **Production**: `wss://yourdomain.com` or `https://yourdomain.com`
- The service automatically converts HTTP(S) URLs to WS(S) format
- Protocol auto-detection based on page protocol (HTTP → WS, HTTPS → WSS)

## Basic Usage

### 1. Simple WebSocket Hook

```tsx
import { useWebSocket } from '../hooks/useWebSocket';

function MyComponent() {
  const webSocket = useWebSocket({
    autoConnect: true,
    onConnect: () => console.log('Connected!'),
    onMessage: (message) => console.log('Message:', message),
    onPresenceUpdate: (presence) => console.log('Presence:', presence)
  });

  return (
    <div>
      <p>Status: {webSocket.connectionState}</p>
      <button onClick={() => webSocket.sendMessage('/app/test', { data: 'hello' })}>
        Send Message
      </button>
    </div>
  );
}
```

### 2. WebSocket with Authentication

```tsx
import { useWebSocketWithAuth } from '../hooks/useWebSocketWithAuth';

function AuthenticatedComponent() {
  const { token } = useAuth(); // Your auth context
  
  const webSocket = useWebSocketWithAuth({
    token,
    autoConnect: true,
    onConnect: () => console.log('Authenticated connection!')
  });

  return (
    <div>
      <p>Connected: {webSocket.isConnected ? 'Yes' : 'No'}</p>
      <p>Has Auth: {webSocket.hasAuth ? 'Yes' : 'No'}</p>
    </div>
  );
}
```

### 3. Connection Status Display

```tsx
import { WebSocketConnectionStatus } from '../shared/ui';

function StatusDisplay() {
  const webSocket = useWebSocket();

  return (
    <WebSocketConnectionStatus
      connectionInfo={webSocket.connectionInfo}
      onRetry={webSocket.retryConnection}
      variant="detailed"
      showDetails={true}
    />
  );
}
```

## Advanced Usage

### Custom Message Handlers

```tsx
function AdvancedComponent() {
  const webSocket = useWebSocket({
    onMessage: (message) => {
      switch (message.type) {
        case 'BUDDY_REQUEST':
          handleBuddyRequest(message.payload);
          break;
        case 'FORUM_NEW_POST':
          handleNewForumPost(message.payload);
          break;
        case 'NOTIFICATION':
          showNotification(message.payload);
          break;
      }
    }
  });
}
```

### Specialized Hooks

The integration provides specialized hooks for different features:

```tsx
// Buddy System
import { useBuddyWebSocket } from '../hooks/useWebSocket';
const buddy = useBuddyWebSocket(relationshipId);

// Forum
import { useForumWebSocket } from '../hooks/useWebSocket';
const forum = useForumWebSocket(postId);

// Hive Presence
import { useHivePresence } from '../hooks/useWebSocket';
const hivePresence = useHivePresence(hiveId);
```

## Message Types

The WebSocket service handles these message types:

### System Messages

- `PING` / `PONG` - Heartbeat
- `NOTIFICATION` - System notifications
- `ERROR` / `SUCCESS` / `WARNING` / `INFO` - Status messages

### Presence Messages

- `USER_ONLINE` / `USER_OFFLINE` / `USER_AWAY` - User status
- `USER_TYPING` / `USER_STOPPED_TYPING` - Typing indicators

### Buddy System Messages

- `BUDDY_REQUEST` / `BUDDY_REQUEST_ACCEPTED` / `BUDDY_REQUEST_REJECTED`
- `BUDDY_CHECKIN` / `BUDDY_GOAL_UPDATE`
- `BUDDY_SESSION_START` / `BUDDY_SESSION_END`

### Forum Messages

- `FORUM_NEW_POST` / `FORUM_NEW_REPLY`
- `FORUM_POST_VOTED` / `FORUM_REPLY_VOTED`
- `FORUM_MENTION` / `FORUM_POST_EDITED`

### Hive Messages

- `HIVE_USER_JOINED` / `HIVE_USER_LEFT`
- `HIVE_ANNOUNCEMENT`

## Backend Integration

The frontend connects to these Spring Boot STOMP endpoints:

### Connection

- **Endpoint**: `/ws` (SockJS) or `/ws-raw` (native WebSocket)
- **Authentication**: JWT token in `Authorization` header
- **Heartbeat**: 30-second intervals

### Subscriptions

- `/user/queue/notifications` - Personal notifications
- `/topic/presence` - Global presence updates
- `/topic/system/announcements` - System announcements
- `/topic/hive/{hiveId}/presence` - Hive-specific presence
- `/topic/buddy/sessions/{relationshipId}` - Buddy updates
- `/topic/forum/post/{postId}/updates` - Forum post updates

### Publishing (Send Messages)

- `/app/presence/heartbeat` - Heartbeat ping
- `/app/presence/status` - Update user status
- `/app/buddy/request` - Send buddy request
- `/app/forum/post/create` - Create forum post
- `/app/test/message` - Test message

## Error Handling

### Connection States

- `DISCONNECTED` - Initial state or manually disconnected
- `CONNECTING` - Attempting to connect
- `CONNECTED` - Successfully connected
- `RECONNECTING` - Automatically trying to reconnect
- `FAILED` - Max retry attempts reached

### Reconnection Strategy

- **Exponential backoff**: `delay = baseDelay * (2 ^ attemptNumber)`
- **Jitter**: ±25% random variation to prevent thundering herd
- **Max delay**: 30 seconds between attempts
- **Max attempts**: Configurable (default: 10)

### Manual Recovery

```tsx
// After connection fails
if (webSocket.connectionState === 'FAILED') {
  webSocket.retryConnection(); // Reset and try again
}

// After token refresh
webSocket.reconnectWithNewToken(); // Reconnect with new auth
```

## Testing

Use the example component to test WebSocket integration:

```tsx
import { WebSocketIntegrationExample } from '../examples/WebSocketIntegrationExample';

function TestPage() {
  const { token } = useAuth();
  
  return <WebSocketIntegrationExample authToken={token} />;
}
```

## Troubleshooting

### Common Issues

1. **Connection Fails**
    - Check `VITE_WEBSOCKET_URL` in environment
    - Verify backend is running on correct port
    - Check browser console for error messages

2. **Authentication Fails**
    - Ensure JWT token is valid and not expired
    - Check token format: `Bearer <token>`
    - Verify backend accepts the token

3. **Messages Not Received**
    - Check subscription destinations match backend
    - Verify message handlers are registered
    - Check browser network tab for WebSocket frames

### Debug Mode

Development builds automatically enable debug logging:

```javascript
// Console output will show:
STOMP: CONNECT
STOMP: CONNECTED
STOMP: MESSAGE destination:/topic/presence
```

### Network Issues

The integration handles various network conditions:

- **Slow connections**: Extended timeouts
- **Intermittent connectivity**: Automatic reconnection
- **Firewall restrictions**: SockJS fallback transports
- **Proxy servers**: Proper WebSocket upgrade handling