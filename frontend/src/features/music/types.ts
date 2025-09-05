// Core music types
export interface Track {
  id: string
  title: string
  artist: string
  album: string
  duration: number
  albumArt?: string
  explicit?: boolean
  spotifyId?: string
  previewUrl?: string
  uri?: string
}

export interface Playlist {
  id: string
  name: string
  description?: string
  tracks: Track[]
  ownerId: string
  isPublic: boolean
  createdAt: string
  updatedAt: string
  collaborative?: boolean
  spotifyId?: string
}

export interface QueueItem {
  id: string
  track: Track
  addedBy: string
  addedAt: string
  votes?: number
  position: number
}

// Playback state
export interface PlaybackState {
  isPlaying: boolean
  isPaused: boolean
  isBuffering: boolean
  position: number
  volume: number
  isMuted: boolean
  playbackRate: number
}

// Mood and context
export interface MoodState {
  currentMood?: string
  energy?: number
  valence?: number
  danceability?: number
  acousticness?: number
}

export interface TaskType {
  id: string
  name: string
  description?: string
  suggestedGenres?: string[]
  suggestedTempo?: {
    min: number
    max: number
  }
}

// Music context state
export interface MusicState {
  currentTrack: Track | null
  queue: QueueItem[]
  playbackState: PlaybackState
  currentPlaylist: Playlist | null
  playlists: Playlist[]
  currentMood: MoodState | null
  isConnected: boolean
  error: string | null
}

export interface MusicContextType extends MusicState {
  // Playback controls
  play: (track?: Track) => Promise<void>
  pause: () => void
  stop: () => void
  skipNext: () => void
  skipPrevious: () => void
  seek: (position: number) => void
  setVolume: (volume: number) => void
  toggleMute: () => void
  
  // Enhanced controls
  playWithCrossfade: (track?: Track) => Promise<void>
  skipNextEnhanced: () => void
  skipPreviousEnhanced: () => void
  quickSeekBackward: () => void
  quickSeekForward: () => void
  fadeVolume: (targetVolume: number, duration?: number) => () => void
  
  // Queue management
  addToQueue: (track: Track) => void
  removeFromQueue: (queueItemId: string) => void
  reorderQueue: (fromIndex: number, toIndex: number) => void
  clearQueue: () => void
  shuffleQueue: () => void
  
  // Playlist management
  createPlaylist: (playlist: CreatePlaylistRequest) => Promise<void>
  updatePlaylist: (playlistId: string, updates: UpdatePlaylistRequest) => Promise<void>
  deletePlaylist: (playlistId: string) => Promise<void>
  addTrackToPlaylist: (playlistId: string, track: Track) => Promise<void>
  removeTrackFromPlaylist: (playlistId: string, trackId: string) => Promise<void>
  
  // Search and recommendations
  searchTracks: (request: SearchTracksRequest) => Promise<Track[]>
  getRecommendations: (request: SessionRecommendationRequest) => Promise<Track[]>
  
  // Mood and context
  setMood: (mood: MoodState) => void
  updateContext: (context: Partial<MusicState>) => void
  
  // Connection
  connect: () => void
  disconnect: () => void
}

// API request types
export interface CreatePlaylistRequest {
  name: string
  description?: string
  isPublic?: boolean
  collaborative?: boolean
  tracks?: string[]
}

export interface UpdatePlaylistRequest {
  name?: string
  description?: string
  isPublic?: boolean
  collaborative?: boolean
}

export interface SearchTracksRequest {
  query: string
  limit?: number
  offset?: number
  market?: string
  type?: 'track' | 'album' | 'artist' | 'playlist'
}

export interface SessionRecommendationRequest {
  seedTracks?: string[]
  seedArtists?: string[]
  seedGenres?: string[]
  targetEnergy?: number
  targetValence?: number
  targetDanceability?: number
  targetAcousticness?: number
  targetTempo?: number
  limit?: number
  market?: string
}

export interface AddToQueueRequest {
  trackId: string
  position?: number
}

export interface VoteRequest {
  queueItemId: string
  vote: 'up' | 'down'
}

// WebSocket types
export interface WebSocketMessage {
  type: string
  payload: unknown
  timestamp?: string
  userId?: string
}

export interface UserJoinedPayload {
  userId: string
  username: string
  sessionId: string
}

export interface TrackVotedPayload {
  queueItemId: string
  userId: string
  vote: 'up' | 'down'
  newVoteCount: number
}

// Hook options
export interface UseMusicWebSocketOptions {
  autoConnect?: boolean
  reconnectAttempts?: number
  reconnectDelay?: number
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: Error) => void
}

// Music player component props
export interface MusicPlayerProps {
  className?: string
  showQueue?: boolean
  showPlaylist?: boolean
  compact?: boolean
  mode?: "mini" | "full"
  onTrackChange?: (track: Track | null) => void
  onPlaybackStateChange?: (state: PlaybackState) => void
}

// Playlist selector props
export interface PlaylistSelectorProps {
  selectedPlaylistId?: string
  onPlaylistSelect?: (playlist: Playlist) => void
  onCreatePlaylist?: (playlist: CreatePlaylistRequest) => void
  showCreateButton?: boolean
  className?: string
}

// Response types
export interface MusicApiResponse<T = unknown> {
  data: T
  success: boolean
  message?: string
  error?: string
}

export interface PaginatedMusicResponse<T> {
  items: T[]
  total: number
  offset: number
  limit: number
  next?: string
  previous?: string
}

// Spotify specific types
export interface SpotifyTrack extends Track {
  spotifyId: string
  uri: string
  external_urls: {
    spotify: string
  }
  popularity: number
  explicit: boolean
  available_markets?: string[]
}

export interface SpotifyPlaylist extends Playlist {
  spotifyId: string
  external_urls: {
    spotify: string
  }
  followers?: {
    total: number
  }
  images?: Array<{
    url: string
    height: number | null
    width: number | null
  }>
}

// Audio features
export interface AudioFeatures {
  danceability: number
  energy: number
  key: number
  loudness: number
  mode: number
  speechiness: number
  acousticness: number
  instrumentalness: number
  liveness: number
  valence: number
  tempo: number
  duration_ms: number
  time_signature: number
}
