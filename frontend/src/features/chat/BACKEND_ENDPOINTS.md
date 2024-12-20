# Backend Endpoints Required for Chat Feature

## REST API Endpoints

### Message Management Endpoints
1. **GET /api/chat/hives/{hiveId}/messages**
   - Get messages for a specific hive
   - Query params: `?limit=50&offset=0&before={timestamp}`
   - Response: `ChatMessage[]`
   - Headers: `Authorization: Bearer {token}`

2. **POST /api/chat/hives/{hiveId}/messages**
   - Send a new message to a hive
   - Request body: `{ content: string, type: 'text'|'image'|'file'|'system', replyTo?: string, metadata?: object }`
   - Response: `ChatMessage`
   - Headers: `Authorization: Bearer {token}`

3. **PUT /api/chat/messages/{messageId}**
   - Edit an existing message
   - Request body: `{ content: string }`
   - Response: `ChatMessage`
   - Headers: `Authorization: Bearer {token}`

4. **DELETE /api/chat/messages/{messageId}**
   - Delete a message
   - Response: `{ success: boolean }`
   - Headers: `Authorization: Bearer {token}`

### Reactions Endpoints
5. **POST /api/chat/messages/{messageId}/reactions**
   - Add a reaction to a message
   - Request body: `{ emoji: string }`
   - Response: `{ success: boolean, reactions: Reaction[] }`
   - Headers: `Authorization: Bearer {token}`

6. **DELETE /api/chat/messages/{messageId}/reactions/{emoji}**
   - Remove a reaction from a message
   - Response: `{ success: boolean, reactions: Reaction[] }`
   - Headers: `Authorization: Bearer {token}`

### Thread & Reply Endpoints
7. **GET /api/chat/messages/{messageId}/thread**
   - Get thread replies for a message
   - Query params: `?limit=20&offset=0`
   - Response: `ChatMessage[]`
   - Headers: `Authorization: Bearer {token}`

### Read Receipts Endpoints
8. **POST /api/chat/hives/{hiveId}/read**
   - Mark messages as read in a hive
   - Request body: `{ lastReadMessageId: string }`
   - Response: `{ success: boolean, unreadCount: number }`
   - Headers: `Authorization: Bearer {token}`

9. **GET /api/chat/hives/{hiveId}/unread**
   - Get unread message count for a hive
   - Response: `{ unreadCount: number, lastReadMessageId?: string }`
   - Headers: `Authorization: Bearer {token}`

### File Upload Endpoints
10. **POST /api/chat/upload**
    - Upload files for chat messages
    - Request: `multipart/form-data` with file
    - Response: `{ url: string, filename: string, size: number, type: string }`
    - Headers: `Authorization: Bearer {token}`

### Search & History
11. **GET /api/chat/hives/{hiveId}/search**
    - Search messages in a hive
    - Query params: `?query={searchText}&limit=20&offset=0`
    - Response: `ChatMessage[]`
    - Headers: `Authorization: Bearer {token}`

## WebSocket Events

### Client -> Server Events
- `chat.message.send`: Send a new message
  - Payload: `{ hiveId, content, type, replyTo?, metadata? }`
- `chat.message.edit`: Edit a message
  - Payload: `{ messageId, content }`
- `chat.message.delete`: Delete a message
  - Payload: `{ messageId }`
- `chat.typing.start`: Start typing indicator
  - Payload: `{ hiveId }`
- `chat.typing.stop`: Stop typing indicator
  - Payload: `{ hiveId }`
- `chat.reaction.add`: Add reaction to message
  - Payload: `{ messageId, emoji }`
- `chat.reaction.remove`: Remove reaction from message
  - Payload: `{ messageId, emoji }`
- `chat.message.read`: Mark messages as read
  - Payload: `{ hiveId, lastReadMessageId }`
- `chat.join.hive`: Join a hive's chat room
  - Payload: `{ hiveId }`
- `chat.leave.hive`: Leave a hive's chat room
  - Payload: `{ hiveId }`

### Server -> Client Events
- `chat.message.new`: New message received
  - Payload: `ChatMessage`
- `chat.message.edited`: Message was edited
  - Payload: `ChatMessage`
- `chat.message.deleted`: Message was deleted
  - Payload: `{ messageId, hiveId }`
- `chat.typing.users`: Users typing update
  - Payload: `{ hiveId, users: TypingUser[] }`
- `chat.reaction.updated`: Reactions updated on a message
  - Payload: `{ messageId, reactions: Reaction[] }`
- `chat.user.joined`: User joined the hive
  - Payload: `{ hiveId, user: User }`
- `chat.user.left`: User left the hive
  - Payload: `{ hiveId, userId }`
- `chat.unread.updated`: Unread count updated
  - Payload: `{ hiveId, unreadCount }`
- `chat.error`: Chat error occurred
  - Payload: `{ error: string, code: string }`

## Example WebSocket Connection
```javascript
// Using Socket.io
const socket = io('ws://localhost:8080', {
  auth: {
    token: 'JWT_TOKEN'
  },
  transports: ['websocket']
});

// Join a hive chat
socket.emit('chat.join.hive', { hiveId: 'hive-123' });

// Listen for new messages
socket.on('chat.message.new', (message) => {
  console.log('New message:', message);
});

// Send a message
socket.emit('chat.message.send', {
  hiveId: 'hive-123',
  content: 'Hello everyone!',
  type: 'text'
});

// Using STOMP (alternative)
const stompClient = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: {
    Authorization: 'Bearer JWT_TOKEN'
  },
  onConnect: () => {
    // Subscribe to hive messages
    stompClient.subscribe('/topic/hive/hive-123/messages', (message) => {
      const chatMessage = JSON.parse(message.body);
      console.log('New message:', chatMessage);
    });
    
    // Subscribe to personal queue
    stompClient.subscribe('/user/queue/chat', (message) => {
      const update = JSON.parse(message.body);
      console.log('Personal update:', update);
    });
  }
});
```

## Data Models (matching TypeScript types)

### ChatMessage
```typescript
interface ChatMessage {
  id: string;
  hiveId: string;
  authorId: string;
  author: {
    id: string;
    name: string;
    avatar?: string;
  };
  content: string;
  type: 'text' | 'image' | 'file' | 'system';
  createdAt: string;
  updatedAt?: string;
  editedAt?: string;
  deletedAt?: string;
  replyTo?: string;
  threadCount?: number;
  reactions?: Reaction[];
  metadata?: {
    fileUrl?: string;
    fileName?: string;
    fileSize?: number;
    mimeType?: string;
    imageWidth?: number;
    imageHeight?: number;
  };
  isEdited: boolean;
  isDeleted: boolean;
}
```

### Reaction
```typescript
interface Reaction {
  emoji: string;
  users: Array<{
    id: string;
    name: string;
  }>;
  count: number;
}
```

### TypingUser
```typescript
interface TypingUser {
  userId: string;
  user: {
    id: string;
    name: string;
    avatar?: string;
  };
  startedAt: string;
}
```

### ChatState
```typescript
interface ChatState {
  messages: Record<string, ChatMessage[]>;
  typingUsers: Record<string, TypingUser[]>;
  unreadCounts: Record<string, number>;
  hasMoreMessages: Record<string, boolean>;
  isLoading: boolean;
  error: string | null;
  lastReadMessages: Record<string, string>;
}
```

## Rate Limiting
- Message sending: 30 messages per minute per user
- Reaction adding: 60 reactions per minute per user
- File upload: 10 files per minute per user
- Message editing: 10 edits per minute per user
- Typing indicators: Automatically throttled to 1 per 3 seconds

## Notes for Backend Implementation
1. Messages should be persisted in PostgreSQL with proper indexing
2. Use Redis for caching recent messages (last 100 per hive)
3. Implement message pagination with cursor-based pagination
4. Typing indicators should auto-expire after 5 seconds of inactivity
5. Support message threading for replies
6. Implement soft delete for messages (mark as deleted, don't remove)
7. Store reactions in a separate table for efficient queries
8. Use WebSocket rooms/channels for hive-specific broadcasts
9. Implement read receipts with last read message tracking
10. Support file uploads with virus scanning and size limits (10MB)
11. Add profanity filter for message content
12. Implement message search with full-text search indexes
