# FocusHive Technical Specification

## Architecture Overview

### Frontend Architecture
```
React App
├── Auth Context (User state, login/logout)
├── Socket Context (Real-time connection)
├── App State (Zustand)
│   ├── Current Room
│   ├── Timer State
│   ├── Participants
│   └── User Stats
└── Router
    ├── Public Routes (Login, Register)
    └── Protected Routes (Dashboard, Room, Profile)
```

### Backend Architecture
```
Express Server
├── REST API
│   ├── Auth Endpoints
│   ├── User Endpoints
│   ├── Room Endpoints
│   └── Stats Endpoints
├── Socket.io Server
│   ├── Connection Manager
│   ├── Room Manager
│   ├── Timer Service
│   └── Chat Service
└── Data Store (In-Memory)
    ├── Users Map
    ├── Rooms Map
    ├── Sessions Map
    └── Messages Array
```

## API Endpoints

### Authentication
```typescript
POST   /api/auth/register     { email, username, password }
POST   /api/auth/login        { email, password }
POST   /api/auth/logout       { }
GET    /api/auth/me           { } → User
```

### Users
```typescript
GET    /api/users/:id         → User
PUT    /api/users/:id         { username?, avatar? }
GET    /api/users/online      → User[]
GET    /api/users/buddies     → User[] (looking for buddy)
```

### Rooms
```typescript
GET    /api/rooms             → Room[] (public rooms)
POST   /api/rooms             { name, type, isPublic, settings }
GET    /api/rooms/:id         → Room
DELETE /api/rooms/:id         { }
GET    /api/rooms/:id/participants → ParticipantStatus[]
```

### Stats
```typescript
GET    /api/stats/leaderboard  { period: 'daily'|'weekly'|'all' }
GET    /api/stats/user/:id     → UserStats
POST   /api/stats/session      { duration, roomId }
```

## Socket.io Events

### Connection Events
```typescript
// Client → Server
'user:authenticate'     { token: string }
'user:disconnect'       { }

// Server → Client
'user:authenticated'    { user: User }
'user:unauthorized'     { error: string }
```

### Room Events
```typescript
// Client → Server
'room:join'            { roomId: string }
'room:leave'           { roomId: string }
'room:create'          { room: RoomConfig }
'participant:update'   { status: Status, task?: string }

// Server → Client
'room:joined'          { room: Room, participants: ParticipantStatus[] }
'room:user-joined'     { user: User, status: ParticipantStatus }
'room:user-left'       { userId: string }
'room:participant-updated' { userId: string, status: ParticipantStatus }
```

### Timer Events
```typescript
// Client → Server
'timer:start'          { roomId: string }
'timer:pause'          { roomId: string }
'timer:skip'           { roomId: string }

// Server → Client
'timer:tick'           { time: number, phase: Phase }
'timer:phase-change'   { phase: Phase, duration: number }
'timer:started'        { startedBy: string }
'timer:paused'         { pausedBy: string }
```

### Chat Events
```typescript
// Client → Server
'chat:message'         { roomId: string, message: string }

// Server → Client
'chat:new-message'     { message: ChatMessage }
'chat:enabled'         { enabled: boolean }
```

## Data Models (Detailed)

### User Model
```typescript
interface User {
  id: string;                    // UUID
  email: string;
  username: string;
  password: string;              // Hashed (even for prototype)
  avatar: string;                // URL or placeholder
  totalFocusTime: number;        // Total minutes focused
  currentStreak: number;         // Days
  longestStreak: number;         // Days
  points: number;                // Total points earned
  lastActiveDate: string;        // ISO date
  lookingForBuddy: boolean;
  preferences: {
    darkMode: boolean;
    soundEnabled: boolean;
    defaultPomodoro: {
      focusDuration: number;     // Default 25
      breakDuration: number;     // Default 5
    };
  };
  createdAt: string;            // ISO timestamp
  updatedAt: string;            // ISO timestamp
}
```

### Room Model
```typescript
interface Room {
  id: string;                    // UUID
  name: string;
  description?: string;
  type: 'Study' | 'Work' | 'Mixed';
  isPublic: boolean;
  password?: string;             // For private rooms
  maxParticipants: number;       // Default 12
  creatorId: string;             // User ID
  
  pomodoroSettings: {
    focusDuration: number;       // Minutes (default 25)
    shortBreakDuration: number;  // Minutes (default 5)
    longBreakDuration: number;   // Minutes (default 15)
    sessionsUntilLongBreak: number; // Default 4
  };
  
  timerState: {
    phase: 'focus' | 'shortBreak' | 'longBreak' | 'paused' | 'idle';
    currentSession: number;      // 1-4
    startedAt?: number;          // Timestamp
    pausedAt?: number;           // Timestamp
    remainingTime?: number;      // Seconds when paused
  };
  
  participants: string[];        // User IDs
  bannedUsers: string[];         // User IDs
  
  stats: {
    totalSessions: number;
    totalFocusTime: number;      // Minutes
    peakParticipants: number;
  };
  
  createdAt: string;
  updatedAt: string;
}
```

### ParticipantStatus Model
```typescript
interface ParticipantStatus {
  userId: string;
  roomId: string;
  status: 'focusing' | 'break' | 'away' | 'idle';
  currentTask?: string;          // Optional task description
  joinedAt: number;              // Timestamp
  sessionFocusTime: number;      // Minutes in current session
  totalRoomTime: number;         // Total minutes in this room
  isCreator: boolean;
  isMuted: boolean;              // For chat
  camera?: boolean;              // Future: camera on/off
  microphone?: boolean;          // Future: mic on/off
}
```

### Session Model
```typescript
interface Session {
  id: string;
  userId: string;
  roomId: string;
  startTime: number;
  endTime?: number;
  duration: number;              // Minutes
  type: 'focus' | 'break';
  completed: boolean;
  pointsEarned: number;
}
```

### ChatMessage Model
```typescript
interface ChatMessage {
  id: string;
  roomId: string;
  userId: string;
  username: string;              // Denormalized for performance
  message: string;
  timestamp: number;
  type: 'user' | 'system';       // System for join/leave messages
}
```

## Timer Synchronization Logic

### Server-Side Timer Management
```typescript
class TimerService {
  private timers: Map<string, NodeJS.Timeout> = new Map();
  
  startTimer(roomId: string, room: Room) {
    // Clear existing timer if any
    this.stopTimer(roomId);
    
    // Set initial state
    room.timerState.startedAt = Date.now();
    room.timerState.phase = 'focus';
    room.timerState.currentSession = 1;
    
    // Create interval that ticks every second
    const timer = setInterval(() => {
      this.tick(roomId, room);
    }, 1000);
    
    this.timers.set(roomId, timer);
  }
  
  tick(roomId: string, room: Room) {
    const elapsed = Date.now() - room.timerState.startedAt!;
    const phaseDuration = this.getPhaseDuration(room);
    const remaining = phaseDuration * 60 * 1000 - elapsed;
    
    if (remaining <= 0) {
      this.transitionPhase(roomId, room);
    } else {
      // Broadcast current state
      io.to(roomId).emit('timer:tick', {
        phase: room.timerState.phase,
        remaining: Math.floor(remaining / 1000),
        session: room.timerState.currentSession
      });
    }
  }
  
  transitionPhase(roomId: string, room: Room) {
    // Logic for phase transitions
    // focus → shortBreak
    // shortBreak → focus (next session)
    // 4th shortBreak → longBreak
    // longBreak → focus (reset to session 1)
  }
}
```

### Client-Side Timer Display
```typescript
const PomodoroTimer: React.FC = () => {
  const { timerState } = useRoom();
  const [localTime, setLocalTime] = useState(timerState.remaining);
  
  // Sync with server ticks
  useEffect(() => {
    const handler = (data: TimerUpdate) => {
      setLocalTime(data.remaining);
    };
    
    socket.on('timer:tick', handler);
    return () => socket.off('timer:tick', handler);
  }, []);
  
  // Local countdown for smooth display
  useEffect(() => {
    const interval = setInterval(() => {
      setLocalTime(prev => Math.max(0, prev - 1));
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);
  
  return <TimerDisplay seconds={localTime} phase={timerState.phase} />;
};
```

## State Management

### Frontend State (Zustand)
```typescript
interface AppState {
  // User
  user: User | null;
  setUser: (user: User | null) => void;
  
  // Room
  currentRoom: Room | null;
  participants: ParticipantStatus[];
  joinRoom: (roomId: string) => Promise<void>;
  leaveRoom: () => void;
  
  // Timer
  timerState: TimerState;
  updateTimer: (state: TimerState) => void;
  
  // Chat
  messages: ChatMessage[];
  addMessage: (message: ChatMessage) => void;
  clearMessages: () => void;
  
  // Stats
  leaderboard: LeaderboardEntry[];
  fetchLeaderboard: (period: Period) => Promise<void>;
}
```

### Backend State (In-Memory)
```typescript
class DataStore {
  private users = new Map<string, User>();
  private rooms = new Map<string, Room>();
  private sessions = new Map<string, Session[]>();
  private participants = new Map<string, ParticipantStatus>();
  private messages = new Map<string, ChatMessage[]>();
  
  // User methods
  createUser(userData: Partial<User>): User { }
  getUser(id: string): User | undefined { }
  updateUser(id: string, updates: Partial<User>): User { }
  
  // Room methods
  createRoom(roomData: Partial<Room>): Room { }
  getRoom(id: string): Room | undefined { }
  getRooms(filter?: RoomFilter): Room[] { }
  updateRoom(id: string, updates: Partial<Room>): Room { }
  deleteRoom(id: string): void { }
  
  // Participant methods
  addParticipant(roomId: string, userId: string): ParticipantStatus { }
  removeParticipant(roomId: string, userId: string): void { }
  updateParticipantStatus(roomId: string, userId: string, status: Partial<ParticipantStatus>): void { }
  getRoomParticipants(roomId: string): ParticipantStatus[] { }
  
  // Session methods
  createSession(sessionData: Partial<Session>): Session { }
  completeSession(sessionId: string): void { }
  getUserSessions(userId: string): Session[] { }
  
  // Stats methods
  getUserStats(userId: string): UserStats { }
  getLeaderboard(period: 'daily' | 'weekly' | 'all'): LeaderboardEntry[] { }
  updateUserStats(userId: string, focusTime: number, points: number): void { }
}
```

## Security Considerations

### Authentication
- Use bcrypt for password hashing (even in prototype)
- Generate JWT tokens for session management
- Validate tokens on each request
- Rate limit login attempts

### Authorization
- Room creators can control timer
- Only participants can send messages
- Users can only update their own profile
- Private rooms require password

### Data Validation
- Validate all inputs (Zod or Joi)
- Sanitize chat messages
- Limit message length
- Validate timer durations (5-60 minutes)

## Performance Optimizations

### Frontend
- Lazy load routes
- Memoize expensive computations
- Debounce status updates (500ms)
- Virtual scrolling for participant lists
- Optimize re-renders with React.memo

### Backend
- Cache leaderboard calculations (5 minutes)
- Batch socket emissions
- Limit broadcast frequency (1/second for timer)
- Clean up inactive rooms (after 1 hour)
- Paginate large lists

### Real-time
- Use Socket.io rooms for targeted broadcasts
- Compress socket messages
- Implement heartbeat for connection monitoring
- Auto-reconnect with exponential backoff

## Error Handling

### Frontend Errors
```typescript
// Global error boundary
class ErrorBoundary extends Component {
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('React Error:', error, errorInfo);
    // Send to error tracking service
  }
}

// API error handling
const apiCall = async (endpoint: string, options?: RequestInit) => {
  try {
    const response = await fetch(endpoint, options);
    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }
    return response.json();
  } catch (error) {
    // Handle network errors, timeouts, etc.
    showErrorToast(error.message);
    throw error;
  }
};
```

### Backend Errors
```typescript
// Global error handler
app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
  console.error('Server Error:', err);
  
  if (err instanceof ValidationError) {
    return res.status(400).json({ error: err.message });
  }
  
  if (err instanceof AuthError) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  
  res.status(500).json({ error: 'Internal server error' });
});
```

## Testing Strategy

### Manual Testing Checklist
- [ ] Multi-user room interactions
- [ ] Timer synchronization across browsers
- [ ] Points calculation accuracy
- [ ] Streak maintenance across days
- [ ] Chat enable/disable on phase change
- [ ] Buddy matching functionality
- [ ] Responsive design on different screens
- [ ] Dark mode consistency
- [ ] Error recovery scenarios

### Load Testing
- Test with 20+ simultaneous users
- Multiple rooms running concurrently
- Rapid join/leave scenarios
- Network interruption handling

## Deployment Considerations

### Development
```bash
# Frontend
cd client && npm run dev  # Vite dev server on :3000

# Backend
cd server && npm run dev  # Nodemon + ts-node on :3001
```

### Production Build
```bash
# Frontend
cd client && npm run build  # Outputs to dist/

# Backend  
cd server && npm run build  # Compiles TypeScript to dist/
```

### Environment Variables
```env
# Frontend (.env)
VITE_API_URL=http://localhost:3001
VITE_SOCKET_URL=http://localhost:3001

# Backend (.env)
PORT=3001
JWT_SECRET=your-secret-key
NODE_ENV=development
```

## Demo Data Script

```typescript
// seed.ts - Run to populate demo data
const seedData = async () => {
  // Create 10 users with varied stats
  const users = await createDemoUsers(10);
  
  // Create 5 active public rooms
  const rooms = await createDemoRooms(5, users);
  
  // Add participants to rooms
  await populateRooms(rooms, users);
  
  // Generate session history
  await generateSessionHistory(users, 30); // 30 days
  
  // Set some users as "looking for buddy"
  await setLookingForBuddy(users.slice(0, 3));
};
```

This technical specification provides a complete blueprint for implementing the FocusHive prototype with all required features and considerations.