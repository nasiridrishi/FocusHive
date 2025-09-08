import React, { createContext, useReducer, useCallback, useRef, useEffect } from 'react'
import { io, Socket } from 'socket.io-client'
import {
  MusicState,
  MusicContextType,
  Track,
  Playlist,
  QueueItem,
  PlaybackState,
  MoodState,
  CreatePlaylistRequest,
  UpdatePlaylistRequest,
  SessionRecommendationRequest,
  SearchTracksRequest,
  AddToQueueRequest,
  VoteRequest
} from '../types'
import { musicApi } from '../services'

// Initial state
const initialPlaybackState: PlaybackState = {
  isPlaying: false,
  isPaused: false,
  isBuffering: false,
  currentTime: 0,
  duration: 0,
  volume: 1,
  isMuted: false,
  playbackRate: 1,
}

const initialState: MusicState = {
  currentTrack: null,
  queue: [],
  playlists: [],
  playbackState: initialPlaybackState,
  isLoading: false,
  error: null,
  recommendations: [],
  currentMood: null,
}

// Action types
type MusicAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_CURRENT_TRACK'; payload: Track | null }
  | { type: 'SET_QUEUE'; payload: QueueItem[] }
  | { type: 'ADD_TO_QUEUE'; payload: QueueItem }
  | { type: 'REMOVE_FROM_QUEUE'; payload: string }
  | { type: 'REORDER_QUEUE'; payload: { fromIndex: number; toIndex: number } }
  | { type: 'CLEAR_QUEUE' }
  | { type: 'UPDATE_PLAYBACK_STATE'; payload: Partial<PlaybackState> }
  | { type: 'SET_PLAYLISTS'; payload: Playlist[] }
  | { type: 'ADD_PLAYLIST'; payload: Playlist }
  | { type: 'UPDATE_PLAYLIST'; payload: Playlist }
  | { type: 'REMOVE_PLAYLIST'; payload: string }
  | { type: 'SET_RECOMMENDATIONS'; payload: Track[] }
  | { type: 'SET_MOOD'; payload: MoodState | null }
  | { type: 'UPDATE_QUEUE_ITEM'; payload: QueueItem }

// Reducer
function musicReducer(state: MusicState, action: MusicAction): MusicState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload }
    
    case 'SET_ERROR':
      return { ...state, error: action.payload }
    
    case 'SET_CURRENT_TRACK':
      return { ...state, currentTrack: action.payload }
    
    case 'SET_QUEUE':
      return { ...state, queue: action.payload }
    
    case 'ADD_TO_QUEUE':
      return { ...state, queue: [...state.queue, action.payload] }
    
    case 'REMOVE_FROM_QUEUE':
      return {
        ...state,
        queue: state.queue.filter(item => item.queueId !== action.payload)
      }
    
    case 'REORDER_QUEUE': {
      const { fromIndex, toIndex } = action.payload
      const newQueue = [...state.queue]
      const [movedItem] = newQueue.splice(fromIndex, 1)
      newQueue.splice(toIndex, 0, movedItem)
      return { ...state, queue: newQueue }
    }
    
    case 'CLEAR_QUEUE':
      return { ...state, queue: [] }
    
    case 'UPDATE_PLAYBACK_STATE':
      return {
        ...state,
        playbackState: { ...state.playbackState, ...action.payload }
      }
    
    case 'SET_PLAYLISTS':
      return { ...state, playlists: action.payload }
    
    case 'ADD_PLAYLIST':
      return { ...state, playlists: [...state.playlists, action.payload] }
    
    case 'UPDATE_PLAYLIST':
      return {
        ...state,
        playlists: state.playlists.map(p => 
          p.id === action.payload.id ? action.payload : p
        )
      }
    
    case 'REMOVE_PLAYLIST':
      return {
        ...state,
        playlists: state.playlists.filter(p => p.id !== action.payload)
      }
    
    case 'SET_RECOMMENDATIONS':
      return { ...state, recommendations: action.payload }
    
    case 'SET_MOOD':
      return { ...state, currentMood: action.payload }
    
    case 'UPDATE_QUEUE_ITEM':
      return {
        ...state,
        queue: state.queue.map(item => 
          item.queueId === action.payload.queueId ? action.payload : item
        )
      }
    
    default:
      return state
  }
}

// Create context
const MusicContext = createContext<MusicContextType | undefined>(undefined)

// Provider props
interface MusicProviderProps {
  children: React.ReactNode
  hiveId?: string
}

// Provider component
export const MusicProvider: React.FC<MusicProviderProps> = ({ children, hiveId }) => {
  const [state, dispatch] = useReducer(musicReducer, initialState)
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const socketRef = useRef<Socket | null>(null)
  const currentHiveId = useRef<string | null>(hiveId || null)

  // Initialize audio element
  useEffect(() => {
    if (!audioRef.current) {
      audioRef.current = new Audio()
      audioRef.current.preload = 'metadata'
      
      // Set up audio event listeners
      const audio = audioRef.current
      
      audio.addEventListener('loadstart', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { isBuffering: true } })
      })
      
      audio.addEventListener('canplay', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          isBuffering: false,
          duration: audio.duration || 0
        } })
      })
      
      audio.addEventListener('timeupdate', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          currentTime: audio.currentTime || 0 
        } })
      })
      
      audio.addEventListener('play', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          isPlaying: true, 
          isPaused: false 
        } })
      })
      
      audio.addEventListener('pause', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          isPlaying: false, 
          isPaused: true 
        } })
      })
      
      audio.addEventListener('ended', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          isPlaying: false, 
          isPaused: false 
        } })
        // Auto-play next track will be handled by a separate effect
      })
      
      audio.addEventListener('error', () => {
        dispatch({ type: 'SET_ERROR', payload: 'Playback error occurred' })
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          isPlaying: false, 
          isPaused: false,
          isBuffering: false 
        } })
      })
      
      audio.addEventListener('volumechange', () => {
        dispatch({ type: 'UPDATE_PLAYBACK_STATE', payload: { 
          volume: audio.volume,
          isMuted: audio.muted
        } })
      })
    }
    
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current.src = ''
      }
    }
  }, [])

  // WebSocket connection management
  const connectToHive = useCallback((newHiveId: string) => {
    if (socketRef.current) {
      socketRef.current.disconnect()
    }
    
    currentHiveId.current = newHiveId
    socketRef.current = io('ws://localhost:8084/ws/music', {
      transports: ['websocket'],
      query: { hiveId: newHiveId }
    })
    
    const socket = socketRef.current
    
    socket.on('connect', () => {
      // Connected to music WebSocket
    })
    
    socket.on('disconnect', () => {
      // Disconnected from music WebSocket
    })
    
    socket.on('queue_updated', (data: QueueItem[]) => {
      dispatch({ type: 'SET_QUEUE', payload: data })
    })
    
    socket.on('track_added', (data: QueueItem) => {
      dispatch({ type: 'ADD_TO_QUEUE', payload: data })
    })
    
    socket.on('track_voted', (data: QueueItem) => {
      dispatch({ type: 'UPDATE_QUEUE_ITEM', payload: data })
    })
    
    socket.on('track_changed', (data: Track) => {
      dispatch({ type: 'SET_CURRENT_TRACK', payload: data })
    })
    
    socket.on('error', (error: string) => {
      dispatch({ type: 'SET_ERROR', payload: error })
    })
  }, [])

  const disconnectFromHive = useCallback(() => {
    if (socketRef.current) {
      socketRef.current.disconnect()
      socketRef.current = null
    }
    currentHiveId.current = null
  }, [])

  // Auto-connect to hive if provided
  useEffect(() => {
    if (hiveId && hiveId !== currentHiveId.current) {
      connectToHive(hiveId)
    }
    
    return () => {
      if (socketRef.current) {
        socketRef.current.disconnect()
      }
    }
  }, [hiveId, connectToHive])

  // Playback control functions
  const play = useCallback(async (track?: Track) => {
    try {
      if (track && track !== state.currentTrack) {
        dispatch({ type: 'SET_CURRENT_TRACK', payload: track })
        if (audioRef.current && track.previewUrl) {
          audioRef.current.src = track.previewUrl
          await audioRef.current.play()
        }
      } else if (audioRef.current) {
        await audioRef.current.play()
      }
    } catch (error) {
      console.error('Failed to start playback:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to start playback' })
    }
  }, [state.currentTrack])

  const pause = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause()
    }
  }, [])

  const resume = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.play().catch(() => {
        dispatch({ type: 'SET_ERROR', payload: 'Failed to resume playback' })
      })
    }
  }, [])

  const stop = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.currentTime = 0
    }
    dispatch({ type: 'SET_CURRENT_TRACK', payload: null })
  }, [])

  const seekTo = useCallback((position: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = position
    }
  }, [])

  const setVolume = useCallback((volume: number) => {
    if (audioRef.current) {
      audioRef.current.volume = Math.max(0, Math.min(1, volume))
    }
  }, [])

  const toggleMute = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.muted = !audioRef.current.muted
    }
  }, [])

  const skipNext = useCallback(() => {
    const currentIndex = state.queue.findIndex(item => item.id === state.currentTrack?.id)
    if (currentIndex < state.queue.length - 1) {
      const nextTrack = state.queue[currentIndex + 1]
      play(nextTrack)
    }
  }, [state.queue, state.currentTrack, play])

  const skipPrevious = useCallback(() => {
    const currentIndex = state.queue.findIndex(item => item.id === state.currentTrack?.id)
    if (currentIndex > 0) {
      const previousTrack = state.queue[currentIndex - 1]
      play(previousTrack)
    }
  }, [state.queue, state.currentTrack, play])

  // Auto-play next track when current track ends
  useEffect(() => {
    if (!state.playbackState.isPlaying && audioRef.current?.ended) {
      skipNext()
    }
  }, [state.playbackState.isPlaying, skipNext])

  // Queue management
  const addToQueue = useCallback(async (track: Track, position?: number) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const request: AddToQueueRequest = {
        trackId: track.id,
        hiveId: currentHiveId.current || undefined,
        position
      }
      const queueItem = await musicApi.addToQueue(request)
      if (!currentHiveId.current) {
        dispatch({ type: 'ADD_TO_QUEUE', payload: queueItem })
      }
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to add track to queue' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const removeFromQueue = useCallback(async (queueId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      await musicApi.removeFromQueue(queueId)
      if (!currentHiveId.current) {
        dispatch({ type: 'REMOVE_FROM_QUEUE', payload: queueId })
      }
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to remove track from queue' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const reorderQueue = useCallback(async (fromIndex: number, toIndex: number) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const updatedQueue = await musicApi.reorderQueue(fromIndex, toIndex, currentHiveId.current || undefined)
      dispatch({ type: 'SET_QUEUE', payload: updatedQueue })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to reorder queue' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const clearQueue = useCallback(async () => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      await musicApi.clearQueue(currentHiveId.current || undefined)
      dispatch({ type: 'CLEAR_QUEUE' })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to clear queue' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const voteOnTrack = useCallback(async (queueId: string, vote: 'up' | 'down') => {
    try {
      const voteRequest: VoteRequest = { trackId: queueId, vote }
      await musicApi.voteOnTrack(queueId, voteRequest)
      // The WebSocket will handle the update, or we refresh the queue
      if (!currentHiveId.current) {
        const queue = await musicApi.getQueue()
        dispatch({ type: 'SET_QUEUE', payload: queue })
      }
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to vote on track' })
    }
  }, [])

  // Playlist management
  const loadPlaylists = useCallback(async () => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const playlists = await musicApi.getPlaylists(currentHiveId.current || undefined)
      dispatch({ type: 'SET_PLAYLISTS', payload: playlists })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to load playlists' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const createPlaylist = useCallback(async (request: CreatePlaylistRequest): Promise<Playlist> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const playlist = await musicApi.createPlaylist(request)
      dispatch({ type: 'ADD_PLAYLIST', payload: playlist })
      return playlist
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to create playlist' })
      throw error
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const updatePlaylist = useCallback(async (id: string, request: UpdatePlaylistRequest): Promise<Playlist> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const playlist = await musicApi.updatePlaylist(id, request)
      dispatch({ type: 'UPDATE_PLAYLIST', payload: playlist })
      return playlist
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to update playlist' })
      throw error
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const deletePlaylist = useCallback(async (id: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      await musicApi.deletePlaylist(id)
      dispatch({ type: 'REMOVE_PLAYLIST', payload: id })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to delete playlist' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const addTracksToPlaylist = useCallback(async (playlistId: string, trackIds: string[]) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      await musicApi.addTracksToPlaylist(playlistId, trackIds)
      // Refresh the specific playlist
      const updatedPlaylist = await musicApi.getPlaylist(playlistId)
      dispatch({ type: 'UPDATE_PLAYLIST', payload: updatedPlaylist })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to add tracks to playlist' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const removeTrackFromPlaylist = useCallback(async (playlistId: string, trackId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      await musicApi.removeTrackFromPlaylist(playlistId, trackId)
      // Refresh the specific playlist
      const updatedPlaylist = await musicApi.getPlaylist(playlistId)
      dispatch({ type: 'UPDATE_PLAYLIST', payload: updatedPlaylist })
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to remove track from playlist' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const loadPlaylist = useCallback(async (id: string): Promise<Playlist> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const playlist = await musicApi.getPlaylist(id)
      dispatch({ type: 'UPDATE_PLAYLIST', payload: playlist })
      return playlist
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to load playlist' })
      throw error
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  // Recommendations
  const getRecommendations = useCallback(async (request: SessionRecommendationRequest): Promise<Track[]> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const recommendations = await musicApi.getSessionRecommendations(request)
      dispatch({ type: 'SET_RECOMMENDATIONS', payload: recommendations })
      return recommendations
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to get recommendations' })
      throw error
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const setMood = useCallback((mood: MoodState) => {
    dispatch({ type: 'SET_MOOD', payload: mood })
  }, [])

  // Search
  const searchTracks = useCallback(async (request: SearchTracksRequest) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      return await musicApi.searchTracks(request)
    } catch (error) {
      console.error('Error:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to search tracks' })
      throw error
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const contextValue: MusicContextType = {
    state,
    // Playback control
    play,
    pause,
    resume,
    stop,
    seekTo,
    setVolume,
    toggleMute,
    skipNext,
    skipPrevious,
    
    // Queue management
    addToQueue,
    removeFromQueue,
    reorderQueue,
    clearQueue,
    voteOnTrack,
    
    // Playlist management
    loadPlaylists,
    createPlaylist,
    updatePlaylist,
    deletePlaylist,
    addTracksToPlaylist,
    removeTrackFromPlaylist,
    loadPlaylist,
    
    // Recommendations
    getRecommendations,
    setMood,
    
    // Search
    searchTracks,
    
    // WebSocket
    connectToHive,
    disconnectFromHive,
  }

  return (
    <MusicContext.Provider value={contextValue}>
      {children}
    </MusicContext.Provider>
  )
}

// Re-export hook from separate file for backward compatibility
// Hook should be imported directly from '../hooks/useMusicContext' to avoid Fast Refresh warning

// Export context and type for use in hooks file
export default MusicContext
export type { MusicContextType }