# FocusHive Music Frontend Module

## Overview

The FocusHive Music Module provides a comprehensive frontend interface for the Music Recommendation
Service, offering rich music player functionality, collaborative playlist management, Spotify
integration, and real-time music synchronization features.

## Architecture

### Module Structure

```
src/features/music/
├── components/           # React components
│   ├── MusicPlayer/     # Main music player component (mini/full modes)
│   ├── PlaylistSelector/ # Playlist selection and management
│   ├── CollaborativeQueue/ # Real-time collaborative queue
│   ├── MoodSelector/    # Mood and task type selection
│   └── SpotifyConnect/  # Spotify OAuth2 integration
├── context/             # React context providers
│   ├── MusicContext.tsx # Main music state management
│   └── SpotifyContext.tsx # Spotify integration state
├── hooks/               # Custom React hooks
│   ├── useCollaborativePlaylist.ts # Collaborative playlist management
│   ├── useMusicRecommendations.ts  # Recommendation engine interface
│   ├── useMusicWebSocket.ts        # Real-time WebSocket connection
│   ├── usePlaybackControl.ts       # Advanced playback controls
│   └── useSpotifyPlayer.ts         # Spotify Web SDK integration
├── services/            # API and external service integration
│   ├── musicApi.ts      # Music Service API client
│   └── spotifyService.ts # Spotify Web API integration
├── types/               # TypeScript type definitions
│   └── music.ts         # Comprehensive type definitions
└── utils/               # Utility functions
```

## Key Components

### MusicPlayer Component

The main music player component with support for both mini and full display modes.

**Features:**

- **Dual Display Modes**: Mini player (fixed position) and full player (embedded)
- **Spotify Integration**: Seamless switching between local and Spotify Premium playback
- **Advanced Controls**: Play/pause, skip, seek, volume, shuffle, repeat
- **Real-time Updates**: Live progress tracking and state synchronization
- **Rich Metadata Display**: Album art, track info, collaborative indicators

**Usage:**

```typescript
import { MusicPlayer } from '@/features/music'

// Mini player (floating)
<MusicPlayer mode="mini" />

// Full player (embedded)
<MusicPlayer 
  mode="full" 
  showQueue={true} 
  showLyrics={true} 
/>
```

**Props:**

```typescript
interface MusicPlayerProps {
  mode?: 'mini' | 'full'           // Display mode
  className?: string               // Additional CSS classes
  showQueue?: boolean             // Show queue panel
  showLyrics?: boolean            // Show lyrics panel
}
```

### PlaylistSelector Component

Playlist selection and management interface with search, filtering, and creation capabilities.

**Features:**

- **Smart Filtering**: Filter by type (personal, hive, smart playlists)
- **Search Functionality**: Real-time playlist and track search
- **Creation Interface**: Modal-based playlist creation
- **Collaborative Support**: Hive-specific playlists
- **Visual Indicators**: Collaborative status, track counts, duration

**Usage:**

```typescript
import { PlaylistSelector } from '@/features/music'

<PlaylistSelector
  onPlaylistSelect={(playlist) => console.log('Selected:', playlist)}
  selectedPlaylistId="current-playlist-id"
  showCreateButton={true}
  hiveId="hive-uuid"
  type="hive"
/>
```

### CollaborativeQueue Component

Real-time collaborative playlist management with voting and drag-drop reordering.

**Features:**

- **Democratic Voting**: Upvote/downvote tracks in collaborative sessions
- **Real-time Updates**: Live queue updates via WebSocket
- **Drag-and-Drop Reordering**: Intuitive queue management
- **Permission Management**: Role-based queue modification
- **Visual Feedback**: Vote counts, position indicators, user attribution

**Usage:**

```typescript
import { CollaborativeQueue } from '@/features/music'

<CollaborativeQueue
  hiveId="hive-uuid"
  showVoting={true}
  maxQueueSize={50}
  allowReordering={true}
  showAddButton={true}
/>
```

### MoodSelector Component

Intelligent mood and task type selection for personalized recommendations.

**Features:**

- **Mood Selection**: Pre-defined moods with visual indicators
- **Task Type Integration**: Context-aware recommendations
- **Energy Level Control**: Fine-tune energy preferences
- **Custom Tags**: User-defined mood descriptors
- **Visual Design**: Color-coded mood states with icons

**Usage:**

```typescript
import { MoodSelector } from '@/features/music'

<MoodSelector
  onMoodChange={(mood) => generateRecommendations(mood)}
  currentMood={currentMoodState}
  showEnergySlider={true}
  showTaskTypeSelector={true}
  showCustomTags={true}
/>
```

### SpotifyConnect Component

Spotify OAuth2 integration with premium feature detection and connection management.

**Features:**

- **OAuth2 Flow**: Secure Spotify authentication
- **Premium Detection**: Automatic feature availability detection
- **Connection Management**: Connect/disconnect functionality
- **Visual Status**: Connection state indicators
- **Fallback Handling**: Graceful degradation for free accounts

**Usage:**

```typescript
import { SpotifyConnectButton } from '@/features/music'

<SpotifyConnectButton 
  variant="card"           // 'button' | 'card'
  showDetails={true}       // Show account details
  onConnect={() => {}}     // Connection callback
  onDisconnect={() => {}}  // Disconnection callback
/>
```

## Context Providers

### MusicContext

Central state management for music functionality with comprehensive API integration.

**State Management:**

- Current track and playback state
- Queue management and voting
- Playlist CRUD operations
- Recommendation integration
- WebSocket real-time updates

**Usage:**

```typescript
import { useMusicContext } from '@/features/music/context'

const MusicComponent = () => {
  const {
    state,
    play,
    pause,
    addToQueue,
    getRecommendations,
    connectToHive
  } = useMusicContext()

  // Use music functionality
  return <div>{/* Component JSX */}</div>
}
```

### SpotifyContext

Spotify-specific state management and Web SDK integration.

**Features:**

- OAuth2 token management with refresh
- Spotify Web SDK player initialization
- Premium feature detection
- Device management and selection
- Encrypted token storage

**Usage:**

```typescript
import { useSpotifyContext } from '@/features/music/context'

const SpotifyComponent = () => {
  const {
    state,
    authenticate,
    disconnect,
    player,
    isConnected,
    isPremium
  } = useSpotifyContext()

  return <div>{/* Spotify integration JSX */}</div>
}
```

## Custom Hooks

### useMusicRecommendations

Hook for generating and managing music recommendations.

**Features:**

- Session-based recommendations
- Task and mood optimization
- Feedback submission and learning
- Caching with intelligent invalidation
- Performance analytics integration

**API:**

```typescript
const {
  recommendations,
  loading,
  error,
  generateRecommendations,
  submitFeedback,
  clearCache
} = useMusicRecommendations()

// Generate recommendations
await generateRecommendations({
  taskType: 'DEEP_WORK',
  mood: 'FOCUSED',
  expectedDuration: 120,
  hiveId: 'hive-uuid'
})
```

### useCollaborativePlaylist

Real-time collaborative playlist management hook.

**Features:**

- Real-time queue synchronization
- Voting system integration
- Member management
- Permission handling
- Optimistic UI updates

**API:**

```typescript
const {
  queue,
  members,
  vote,
  addTrack,
  joinSession,
  leaveSession
} = useCollaborativePlaylist(hiveId)

// Vote on track
await vote('track-id', 'up')
```

### usePlaybackControl

Advanced playback control with crossfading and smart features.

**Features:**

- Crossfade transitions
- Smart skip logic
- Quick seek controls (10s forward/backward)
- Shuffle and repeat modes
- Volume management with fade

**API:**

```typescript
const {
  playWithCrossfade,
  skipNextEnhanced,
  quickSeekBackward,
  toggleShuffle,
  isShuffling,
  repeatMode
} = usePlaybackControl()
```

### useSpotifyPlayer

Spotify Web SDK integration hook for premium users.

**Features:**

- Web SDK player initialization
- Device management
- Premium feature access
- Token management
- Connection state handling

**API:**

```typescript
const {
  player,
  isReady,
  isConnected,
  deviceId,
  playbackState,
  togglePlay,
  skipNext,
  setVolume
} = useSpotifyPlayer({ token: spotifyToken })
```

### useMusicWebSocket

Real-time WebSocket connection for collaborative features.

**Features:**

- Automatic connection management
- Message routing and handling
- Reconnection logic
- Event-based architecture
- Connection state tracking

**API:**

```typescript
const {
  isConnected,
  sendMessage,
  lastMessage
} = useMusicWebSocket({
  hiveId: 'hive-uuid',
  onMessage: (message) => handleMessage(message),
  onConnect: () => console.log('Connected'),
  onDisconnect: () => console.log('Disconnected')
})
```

## Type Definitions

### Core Types

**Track Interface:**

```typescript
interface Track {
  id: string
  title: string
  artist: string
  album?: string
  duration: number           // seconds
  albumArt?: string
  spotifyId?: string
  previewUrl?: string
  explicit: boolean
  popularity?: number
  addedBy?: User
  votes?: number
  userVote?: 'up' | 'down' | null
}
```

**Playlist Interface:**

```typescript
interface Playlist {
  id: string
  name: string
  description?: string
  isPublic: boolean
  isCollaborative: boolean
  trackCount: number
  duration: number           // total duration in seconds
  tracks: Track[]
  createdBy: User
  type: 'personal' | 'hive' | 'smart'
  hiveId?: string
}
```

**PlaybackState Interface:**

```typescript
interface PlaybackState {
  isPlaying: boolean
  isPaused: boolean
  isBuffering: boolean
  currentTime: number
  duration: number
  volume: number
  isMuted: boolean
  playbackRate: number
}
```

### Recommendation Types

**MoodState Interface:**

```typescript
interface MoodState {
  mood: string
  energy: number             // 0-100
  taskType: TaskType
  customTags: string[]
}

type TaskType = 'focus' | 'creative' | 'break' | 'exercise' | 'relax' | 'social'
```

**SessionRecommendationRequest Interface:**

```typescript
interface SessionRecommendationRequest {
  hiveId?: string
  mood?: string
  energy?: number
  taskType?: TaskType
  duration?: number
  previousTracks?: string[]
}
```

## API Integration

### Music Service API Client

The `musicApi.ts` service provides comprehensive integration with the Music Service backend.

**Key Methods:**

```typescript
// Recommendations
const recommendations = await musicApi.generateSessionRecommendations(request)
const feedback = await musicApi.submitFeedback(recommendationId, feedbackData)
const analytics = await musicApi.getPersonalAnalytics()

// Playlists
const playlists = await musicApi.getPlaylists()
const playlist = await musicApi.createPlaylist(playlistData)
const updated = await musicApi.updatePlaylist(playlistId, updates)

// Collaborative Features
const queue = await musicApi.getCollaborativeQueue(playlistId)
const vote = await musicApi.voteOnTrack(playlistId, trackId, voteType)
const joined = await musicApi.joinCollaborativeSession(playlistId)
```

### Spotify Web API Integration

The `spotifyService.ts` handles Spotify Web API integration for track metadata and premium features.

**Key Methods:**

```typescript
// Authentication
const authUrl = await spotifyService.getAuthUrl()
const tokens = await spotifyService.exchangeCodeForTokens(code)

// Track Operations
const track = await spotifyService.getTrack(trackId)
const features = await spotifyService.getAudioFeatures(trackId)
const search = await spotifyService.searchTracks(query)

// Player Control (Premium)
await spotifyService.play(trackUri)
await spotifyService.pause()
await spotifyService.skipNext()
await spotifyService.setVolume(volume)
```

## Real-time Features

### WebSocket Integration

The music module integrates with the Music Service WebSocket endpoints for real-time collaborative
features.

**Supported Events:**

- `TRACK_ADDED`: New track added to collaborative queue
- `TRACK_VOTED`: Vote cast on collaborative track
- `QUEUE_UPDATED`: Queue order or content changed
- `TRACK_CHANGED`: Current playing track changed
- `USER_JOINED`: User joined collaborative session
- `USER_LEFT`: User left collaborative session

**Event Handling:**

```typescript
const handleWebSocketMessage = (message: WebSocketMessage) => {
  switch (message.type) {
    case 'TRACK_ADDED':
      updateQueue(message.payload.queue)
      break
    case 'TRACK_VOTED':
      updateTrackVotes(message.payload.trackId, message.payload.votes)
      break
    // Handle other events...
  }
}
```

## Configuration

### Required Environment Variables

```env
# API Configuration
VITE_MUSIC_SERVICE_URL=http://localhost:8084
VITE_MUSIC_SERVICE_WS_URL=ws://localhost:8084/ws

# Spotify Integration
VITE_SPOTIFY_CLIENT_ID=your_spotify_client_id
VITE_SPOTIFY_REDIRECT_URI=http://localhost:3000/auth/spotify/callback

# Feature Flags
VITE_ENABLE_SPOTIFY_PREMIUM=true
VITE_ENABLE_COLLABORATIVE_FEATURES=true
VITE_ENABLE_REAL_TIME_UPDATES=true
```

### Theme Configuration

The music module integrates with Material-UI theming for consistent visual design.

**Custom Theme Extensions:**

```typescript
// Theme customization for music components
const musicTheme = {
  palette: {
    spotify: {
      main: '#1DB954',        // Spotify green
      contrastText: '#ffffff'
    },
    music: {
      player: '#121212',      // Dark player background
      progress: '#1976d2',    // Progress bar color
      controls: '#ffffff'     // Control button color
    }
  },
  components: {
    MuiMusicPlayer: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          backdropFilter: 'blur(20px)'
        }
      }
    }
  }
}
```

## Testing

### Unit Tests

The music module includes comprehensive unit tests for all components and hooks.

**Test Coverage:**

- Component rendering and interaction
- Hook state management and API calls
- Context provider functionality
- WebSocket connection handling
- Error scenarios and edge cases

**Example Test:**

```typescript
// MusicPlayer.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { MusicPlayer } from './MusicPlayer'
import { MusicContextProvider } from '../../context'

test('displays track information correctly', () => {
  const mockTrack = {
    id: '1',
    title: 'Test Song',
    artist: 'Test Artist',
    duration: 180
  }

  render(
    <MusicContextProvider>
      <MusicPlayer mode="mini" />
    </MusicContextProvider>
  )

  expect(screen.getByText('Test Song')).toBeInTheDocument()
  expect(screen.getByText('Test Artist')).toBeInTheDocument()
})
```

### Integration Tests

Integration tests verify API interactions and real-time functionality.

**Test Scenarios:**

- Music Service API integration
- Spotify OAuth2 flow
- WebSocket connection and messaging
- Collaborative playlist functionality
- Recommendation generation and feedback

## Performance Optimization

### Lazy Loading

Components are lazy-loaded to reduce initial bundle size:

```typescript
const MusicPlayer = lazy(() => import('./components/MusicPlayer'))
const PlaylistSelector = lazy(() => import('./components/PlaylistSelector'))
```

### Memoization

Strategic use of React.memo and useMemo for performance:

```typescript
const MusicPlayer = React.memo(({ mode, showQueue }) => {
  const formatTime = useMemo(() => {
    return (seconds: number) => {
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
  }, [])
  
  // Component implementation...
})
```

### Virtualization

Large playlist and queue displays use virtualization for optimal performance:

```typescript
import { FixedSizeList as List } from 'react-window'

const VirtualizedPlaylist = ({ tracks }) => (
  <List
    height={400}
    itemCount={tracks.length}
    itemSize={60}
    itemData={tracks}
  >
    {TrackRow}
  </List>
)
```

## Accessibility

### Keyboard Navigation

Full keyboard navigation support for all music controls:

- **Space**: Play/pause
- **Arrow Keys**: Seek forward/backward
- **Enter**: Activate focused button
- **Escape**: Close modals/dropdowns

### Screen Reader Support

Comprehensive ARIA labels and live regions:

```typescript
<IconButton
  aria-label={isPlaying ? 'Pause track' : 'Play track'}
  aria-describedby="track-info"
  onClick={handlePlayPause}
>
  {isPlaying ? <Pause /> : <PlayArrow />}
</IconButton>

<div
  id="track-info"
  aria-live="polite"
  aria-atomic="true"
>
  {currentTrack?.title} by {currentTrack?.artist}
</div>
```

### Color Contrast

All UI elements meet WCAG 2.1 AA contrast requirements with customizable high-contrast mode.

## Browser Compatibility

### Supported Browsers

- **Chrome/Edge**: Full functionality including Spotify Web SDK
- **Firefox**: Full functionality with Web Audio API fallback
- **Safari**: Partial Spotify integration (iOS limitations)
- **Mobile**: Responsive design with touch-optimized controls

### Feature Detection

Progressive enhancement with feature detection:

```typescript
const hasWebAudioAPI = 'AudioContext' in window || 'webkitAudioContext' in window
const hasMediaSession = 'mediaSession' in navigator
const hasSpotifySupport = window.Spotify && window.Spotify.Player

if (hasMediaSession) {
  navigator.mediaSession.setActionHandler('play', handlePlay)
  navigator.mediaSession.setActionHandler('pause', handlePause)
}
```

## Development Guide

### Local Development Setup

1. **Install Dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Environment Configuration**:
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your Spotify credentials
   ```

3. **Start Development Server**:
   ```bash
   npm run dev
   ```

4. **Run Tests**:
   ```bash
   npm test -- --coverage
   ```

### Adding New Features

1. **Create Component**:
   ```bash
   mkdir src/features/music/components/NewComponent
   touch src/features/music/components/NewComponent/NewComponent.tsx
   touch src/features/music/components/NewComponent/index.ts
   ```

2. **Add Types**:
   ```typescript
   // Add to types/music.ts
   export interface NewComponentProps {
     // Define props
   }
   ```

3. **Write Tests**:
   ```bash
   touch src/features/music/components/NewComponent/NewComponent.test.tsx
   ```

4. **Export Component**:
   ```typescript
   // Add to index.ts
   export { default as NewComponent } from './components/NewComponent'
   ```

### Debugging

**Enable Debug Logging**:

```typescript
localStorage.setItem('debug', 'music:*')
```

**WebSocket Debug Messages**:

```typescript
const ws = useMusicWebSocket({
  hiveId,
  onMessage: (message) => {
    console.debug('WebSocket message:', message)
    handleMessage(message)
  }
})
```

## Contributing

### Code Style

The music module follows the project's ESLint configuration with additional music-specific rules:

```json
{
  "extends": ["../../../.eslintrc.js"],
  "rules": {
    "music/no-direct-spotify-calls": "error",
    "music/require-error-boundaries": "warn"
  }
}
```

### Pull Request Guidelines

1. **Feature Branch**: Create from `main` with descriptive name
2. **Tests**: Include unit and integration tests
3. **Documentation**: Update README and inline documentation
4. **Performance**: Verify no performance regressions
5. **Accessibility**: Test with screen readers and keyboard navigation

### Architecture Decisions

Major architectural decisions are documented in ADR (Architecture Decision Record) format:

- **ADR-001**: Context API vs Redux for state management
- **ADR-002**: Spotify Web SDK integration approach
- **ADR-003**: WebSocket vs Server-Sent Events for real-time updates
- **ADR-004**: Component lazy loading strategy

## Future Enhancements

### Planned Features

1. **AI-Powered Recommendations**:
    - Machine learning model integration
    - Behavioral pattern analysis
    - Cross-user collaborative filtering

2. **Enhanced Collaboration**:
    - Video chat integration during music sessions
    - Synchronized playback across devices
    - Advanced permission management

3. **Extended Platform Support**:
    - Apple Music integration
    - YouTube Music integration
    - Local file support

4. **Advanced Analytics**:
    - Listening habit analytics
    - Productivity correlation insights
    - Music discovery metrics

### Technical Improvements

1. **Performance**:
    - Service worker for offline playback
    - Advanced caching strategies
    - Bundle size optimization

2. **Developer Experience**:
    - Component storybook
    - Enhanced TypeScript coverage
    - Automated visual regression testing

3. **Accessibility**:
    - Voice control integration
    - Enhanced screen reader support
    - Cognitive accessibility features

---

## License

This music module is part of the FocusHive project and is subject to the project's overall licensing
terms.

## Support

For issues and questions related to the music module:

- **GitHub Issues**: [FocusHive Music Issues](https://github.com/focushive/focushive/issues)
- **Documentation**: [Music Module Wiki](https://github.com/focushive/focushive/wiki/music-module)
- **Development Team**: dev-music@focushive.com