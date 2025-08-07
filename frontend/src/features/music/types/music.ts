// Music related types and interfaces

export interface Track {
  id: string
  title: string
  artist: string
  album?: string
  duration: number // in seconds
  albumArt?: string
  spotifyId?: string
  previewUrl?: string
  isrc?: string
  explicit: boolean
  popularity?: number
  addedBy?: {
    id: string
    name: string
    avatar?: string
  }
  addedAt?: string
  votes?: number
  userVote?: 'up' | 'down' | null
}

export interface Playlist {
  id: string
  name: string
  description?: string
  coverImage?: string
  isPublic: boolean
  isCollaborative: boolean
  trackCount: number
  duration: number // total duration in seconds
  tracks: Track[]
  createdBy: {
    id: string
    name: string
    avatar?: string
  }
  createdAt: string
  updatedAt: string
  type: 'personal' | 'hive' | 'smart'
  hiveId?: string
}

export interface QueueItem extends Track {
  queueId: string
  position: number
  votes: number
  userVote?: 'up' | 'down' | null
}

export interface PlaybackState {
  isPlaying: boolean
  isPaused: boolean
  isBuffering: boolean
  currentTime: number
  duration: number
  volume: number
  isMuted: boolean
  playbackRate: number
}

export interface MusicState {
  currentTrack: Track | null
  queue: QueueItem[]
  playlists: Playlist[]
  playbackState: PlaybackState
  isLoading: boolean
  error: string | null
  recommendations: Track[]
  currentMood: MoodState | null
}

export interface MoodState {
  mood: string
  energy: number // 0-100
  taskType: TaskType
  customTags: string[]
}

export type TaskType = 'focus' | 'creative' | 'break' | 'exercise' | 'relax' | 'social'

export interface MoodOption {
  id: string
  name: string
  icon: string
  color: string
  energy: number
  description: string
}

export interface MusicRecommendation {
  tracks: Track[]
  reason: string
  confidence: number
}

export interface SessionRecommendationRequest {
  hiveId?: string
  mood?: string
  energy?: number
  taskType?: TaskType
  duration?: number
  previousTracks?: string[]
}

export interface SpotifyPlayerState {
  isReady: boolean
  isConnected: boolean
  deviceId: string | null
  player: any | null // Spotify Web SDK player instance
}

export interface WebSocketMessage {
  type: 'track_added' | 'track_voted' | 'queue_updated' | 'track_changed' | 'user_joined' | 'user_left'
  payload: any
  timestamp: string
  userId: string
}

export interface VoteRequest {
  trackId: string
  vote: 'up' | 'down'
}

export interface AddToQueueRequest {
  trackId: string
  hiveId?: string
  position?: number
}

export interface CreatePlaylistRequest {
  name: string
  description?: string
  isPublic: boolean
  isCollaborative: boolean
  hiveId?: string
  trackIds?: string[]
}

export interface UpdatePlaylistRequest {
  name?: string
  description?: string
  isPublic?: boolean
  isCollaborative?: boolean
  coverImage?: string
}

export interface SearchTracksRequest {
  query: string
  limit?: number
  offset?: number
  type?: 'track' | 'artist' | 'album'
}

export interface SearchTracksResponse {
  tracks: Track[]
  total: number
  offset: number
  limit: number
}

// Context types
export interface MusicContextType {
  state: MusicState
  // Playback control
  play: (track?: Track) => Promise<void>
  pause: () => void
  resume: () => void
  stop: () => void
  seekTo: (position: number) => void
  setVolume: (volume: number) => void
  toggleMute: () => void
  skipNext: () => void
  skipPrevious: () => void
  
  // Queue management
  addToQueue: (track: Track, position?: number) => Promise<void>
  removeFromQueue: (queueId: string) => Promise<void>
  reorderQueue: (fromIndex: number, toIndex: number) => Promise<void>
  clearQueue: () => Promise<void>
  voteOnTrack: (queueId: string, vote: 'up' | 'down') => Promise<void>
  
  // Playlist management
  loadPlaylists: () => Promise<void>
  createPlaylist: (request: CreatePlaylistRequest) => Promise<Playlist>
  updatePlaylist: (id: string, request: UpdatePlaylistRequest) => Promise<Playlist>
  deletePlaylist: (id: string) => Promise<void>
  addTracksToPlaylist: (playlistId: string, trackIds: string[]) => Promise<void>
  removeTrackFromPlaylist: (playlistId: string, trackId: string) => Promise<void>
  loadPlaylist: (id: string) => Promise<Playlist>
  
  // Recommendations
  getRecommendations: (request: SessionRecommendationRequest) => Promise<Track[]>
  setMood: (mood: MoodState) => void
  
  // Search
  searchTracks: (request: SearchTracksRequest) => Promise<SearchTracksResponse>
  
  // WebSocket
  connectToHive: (hiveId: string) => void
  disconnectFromHive: () => void
}

// Hook types
export interface UseSpotifyPlayerOptions {
  token: string
  name: string
  volume?: number
  getOAuthToken?: (cb: (token: string) => void) => void
}

export interface UseMusicWebSocketOptions {
  hiveId?: string
  onMessage?: (message: WebSocketMessage) => void
  onConnect?: () => void
  onDisconnect?: () => void
}

// Component prop types
export interface MusicPlayerProps {
  mode?: 'mini' | 'full'
  className?: string
  showQueue?: boolean
  showLyrics?: boolean
}

export interface PlaylistSelectorProps {
  onPlaylistSelect: (playlist: Playlist) => void
  selectedPlaylistId?: string
  showCreateButton?: boolean
  hiveId?: string
  type?: 'personal' | 'hive' | 'smart' | 'all'
}

export interface CollaborativeQueueProps {
  hiveId?: string
  showVoting?: boolean
  maxQueueSize?: number
  allowReordering?: boolean
  showAddButton?: boolean
}

export interface MoodSelectorProps {
  onMoodChange: (mood: MoodState) => void
  currentMood?: MoodState
  showEnergySlider?: boolean
  showTaskTypeSelector?: boolean
  showCustomTags?: boolean
}

// API response types
export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
  timestamp: string
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  limit: number
  hasMore: boolean
}

// Error types
export interface MusicError extends Error {
  code: string
  details?: any
  timestamp: string
}

export type MusicErrorCode = 
  | 'PLAYBACK_FAILED'
  | 'TRACK_NOT_FOUND'
  | 'PLAYLIST_NOT_FOUND'
  | 'SPOTIFY_ERROR'
  | 'NETWORK_ERROR'
  | 'AUTHENTICATION_ERROR'
  | 'PERMISSION_DENIED'
  | 'QUEUE_FULL'
  | 'UNSUPPORTED_FORMAT'