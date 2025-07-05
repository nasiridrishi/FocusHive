# FocusHive Development Guide

## Project Overview

Build a functional web application prototype demonstrating FocusHive's core value proposition: virtual focus rooms for co-working/studying with shared accountability and productivity tools.

## Project Structure

```
focushive-prototype/
├── client/                 # React TypeScript frontend
│   ├── src/
│   │   ├── components/    # Reusable UI components
│   │   ├── pages/        # Page components
│   │   ├── contexts/      # React contexts (auth, socket)
│   │   ├── hooks/         # Custom React hooks
│   │   ├── services/      # API and socket services
│   │   ├── types/         # TypeScript type definitions
│   │   ├── utils/         # Helper functions
│   │   └── styles/        # Global styles
│   └── public/
├── server/                # Node.js Express backend
│   ├── src/
│   │   ├── routes/        # API routes
│   │   ├── services/      # Business logic
│   │   ├── middleware/    # Express middleware
│   │   ├── socket/        # Socket.io handlers
│   │   ├── data/          # In-memory data store
│   │   └── types/         # TypeScript types
│   └── package.json
├── shared/                # Shared types and constants
│   └── src/
│       ├── types.ts
│       └── constants.ts
└── README.md
```

## Technical Architecture

### State Management
- **Frontend**: React Context for auth, Zustand for app state
- **Backend**: In-memory Maps with getter/setter methods
- **Real-time**: Socket.io rooms for broadcasting

### Timer Synchronization Strategy
```typescript
// Server maintains authoritative timer state
interface TimerState {
  roomId: string
  phase: 'focus' | 'break' | 'paused'
  duration: number
  startedAt: number
  pausedAt?: number
  remainingTime?: number
}

// Sync every second to all room participants
// New joiners receive current timer state
```

### Real-time Events Architecture
```typescript
// Client → Server events
socket.emit('room:join', { roomId, userId })
socket.emit('status:update', { status, task })
socket.emit('timer:start', { roomId })
socket.emit('chat:message', { message })

// Server → Client events
socket.to(roomId).emit('room:user-joined', { user })
socket.to(roomId).emit('timer:tick', { timeLeft, phase })
socket.to(roomId).emit('chat:new-message', { message })
```

## Daily Implementation Guide

### Day 1: Project Setup & Authentication

#### Morning: Environment Setup
```bash
# 1. Create project structure
mkdir focushive-prototype && cd focushive-prototype
mkdir client server shared

# 2. Initialize packages
cd shared && npm init -y
cd ../server && npm init -y  
cd ../client && npm create vite@latest . -- --template react-ts

# 3. Install dependencies
# Server
npm install express cors dotenv socket.io bcryptjs jsonwebtoken
npm install -D @types/express @types/node @types/cors @types/bcryptjs @types/jsonwebtoken typescript ts-node nodemon

# Client
npm install socket.io-client zustand react-router-dom axios
npm install -D @types/react-router-dom tailwindcss postcss autoprefixer
```

#### Afternoon: Authentication Backend
```typescript
// server/src/services/authService.ts
export class AuthService {
  async register(email: string, username: string, password: string): Promise<User> {
    // Hash password, create user, generate token
  }
  
  async login(email: string, password: string): Promise<{ user: User, token: string }> {
    // Verify credentials, generate token
  }
}

// server/src/routes/auth.routes.ts
router.post('/register', validateRegister, authController.register);
router.post('/login', validateLogin, authController.login);
router.get('/me', authenticate, authController.getMe);
```

#### Evening: Authentication Frontend
```typescript
// client/src/contexts/AuthContext.tsx
export const AuthProvider: React.FC = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  
  const login = async (email: string, password: string) => {
    const { user, token } = await authService.login(email, password);
    localStorage.setItem('token', token);
    setUser(user);
  };
  
  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
```

### Day 2: Dashboard & Room Management

#### Morning: Dashboard UI
Build the main dashboard with:
- User stats overview (focus time, streak, points)
- Quick actions (create room, quick start, find buddy)
- Active rooms grid

Key components:
- `StatsCard` - Display user statistics
- `RoomCard` - Show room information
- `CreateRoomModal` - Room creation interface
- `QuickStartButton` - Join random room

#### Afternoon: Room Backend
Implement room management service:
```typescript
// server/src/services/roomService.ts
export class RoomService {
  createRoom(creatorId: string, data: CreateRoomDto): Room
  joinRoom(roomId: string, userId: string): Room
  getPublicRooms(): Room[]
}
```

#### Evening: Real-time Setup
Initialize Socket.io for real-time communication:
- Server-side socket authentication
- Client-side socket context
- Basic connection handling

### Day 3: Focus Room Interface

#### Morning: Room Layout
Create the main room interface:
- Room header with controls
- Participant grid layout
- Sidebar with timer and controls
- Responsive design

#### Afternoon: Participant Components
Build participant display components:
- `ParticipantCard` - Individual user display
- `ParticipantGrid` - Grid layout for all participants
- Status indicators (focusing, break, away)
- Current task display

#### Evening: Personal Controls
Implement user controls:
- Status update buttons
- Task input field
- Real-time status broadcasting

### Day 4: Pomodoro Timer Implementation

#### Morning: Timer Backend Service
Create server-side timer management:
- `TimerService` class for room timers
- Phase transitions (focus → break → focus)
- Timer synchronization logic
- Points awarding on completion

#### Afternoon: Timer Frontend Component
Build the timer UI:
- Circular progress display
- Time countdown
- Phase indicators
- Control buttons (start/pause/skip)

#### Evening: Timer Synchronization Testing
Test timer functionality:
- Late joiner synchronization
- Phase transitions
- Creator disconnect handling
- Points calculation

### Day 5: Gamification & Points System

#### Morning: Backend Points & Stats
Implement statistics tracking:
- Points calculation (1 per minute)
- Streak tracking
- Session history
- Leaderboard generation

#### Afternoon: Stats Dashboard Components
Create statistics UI:
- Stats overview cards
- Leaderboard component
- Personal ranking display
- Period selector (daily/weekly/all-time)

#### Evening: Real-time Stats Updates
Implement live updates:
- Socket events for stats changes
- Achievement notifications
- Cache invalidation
- Progress animations

### Day 6: Chat System & Break Features

#### Morning: Chat Backend
Build chat functionality:
- Message handling
- Break-time only enforcement
- System messages
- Message sanitization

#### Afternoon: Chat UI Component
Create chat interface:
- Message display
- Input form
- Auto-scrolling
- System message styling

#### Evening: Break Features & Notifications
Add break-time features:
- Browser notifications
- Sound alerts
- Phase change handling
- Permission requests

### Day 7: FocusBuddy System

#### Morning: Buddy Matching Backend
Implement matching logic:
- Waiting queue management
- Simple matching algorithm
- Private room creation
- Match notifications

#### Afternoon: Buddy UI Components
Build buddy interface:
- Find buddy card
- Waiting status display
- Match notification modal
- Auto-navigation to room

#### Evening: Quick Start Feature
Add quick start functionality:
- Find available rooms
- Create room if none available
- Random room selection
- Error handling

### Day 8: UI Polish & Dark Mode

#### Morning: Design System Setup
Create consistent design system:
- Color palette
- Typography scale
- Spacing system
- Shadow definitions

#### Afternoon: Component Styling
Polish UI components:
- Button variants
- Card styles
- Loading states
- Hover effects

#### Evening: Responsive Design
Implement responsive layouts:
- Mobile breakpoints
- Tablet adjustments
- Responsive grid
- Touch-friendly controls

### Day 9: Testing & Bug Fixes

#### Morning: Integration Testing
Test key user flows:
- Authentication flow
- Room creation and joining
- Timer synchronization
- Real-time updates
- Points calculation
- Chat functionality
- Buddy matching
- Performance with multiple users

#### Afternoon: Bug Fixes
Address common issues:
- Memory leaks in socket listeners
- Race conditions in timer sync
- Duplicate messages in chat
- Stale user data
- Timer cleanup on unmount

#### Evening: Error Handling
Implement robust error handling:
- Global error boundary
- Network error handling
- Socket reconnection
- User-friendly error messages

### Day 10: Demo Preparation & Deployment

#### Morning: Demo Data Setup
Create realistic demo environment:
- Seed 10+ demo users
- Create active rooms
- Generate session history
- Populate leaderboard

#### Afternoon: Performance Optimization
Optimize for production:
- Code splitting
- Lazy loading
- Image optimization
- Bundle size reduction
- Caching strategies

#### Evening: Deployment & Documentation
Prepare for deployment:
- Environment configuration
- Build scripts
- Docker setup
- README documentation
- Demo script

## Development Guidelines

### Code Style
- Use TypeScript strictly
- Implement proper error handling
- Add loading/error states
- Comment complex logic
- Use semantic HTML
- Follow React best practices

### Performance Considerations
- Debounce status updates
- Optimize re-renders with React.memo
- Lazy load components
- Minimize socket event frequency

### Testing Approach
- Manual testing scenarios
- Socket.io event testing
- Timer synchronization testing
- Multi-user interaction testing

## Success Metrics
- [ ] Users can register and see dashboard within 30 seconds
- [ ] Room joining is instant with presence updates
- [ ] Timer stays synchronized across all participants
- [ ] Points and streaks update correctly
- [ ] UI remains responsive with 10+ participants
- [ ] Dark mode works consistently
- [ ] Mobile responsive down to tablet size

## Demo Script

### 5-Minute Demo Flow
1. **Introduction** (30s) - Platform overview
2. **Registration & Dashboard** (45s) - Quick tour
3. **Joining a Room** (60s) - Live presence demo
4. **Focus Session** (90s) - Timer and real-time updates
5. **Break Time & Chat** (45s) - Social features
6. **Gamification** (30s) - Points and leaderboard
7. **Focus Buddy** (45s) - Matching demonstration
8. **Conclusion** (15s) - Key benefits

## Key Features Demonstrated
- ✅ Real-time co-working rooms
- ✅ Synchronized Pomodoro timer
- ✅ Live participant presence
- ✅ Points and streak tracking
- ✅ Break-time chat
- ✅ Focus buddy matching
- ✅ Responsive design
- ✅ Dark mode support

---

Built with ❤️ for productive co-working