// Spotify Context for managing Spotify Web SDK integration
// Provides authentication, player state, and controls throughout the music feature

import React, { createContext, useContext, useReducer, useCallback, useEffect, useRef } from 'react'
import { getSpotifyService, SpotifyService } from '../services/spotifyService'
import type { 
  SpotifyConfig, 
  SpotifyAuthState, 
  SpotifyPlayerState, 
  SpotifyConnectionStatus,
  PlayOptions
} from '../../../types/spotify'

// Spotify Context State
interface SpotifyContextState {
  auth: SpotifyAuthState
  player: SpotifyPlayerState
  connection: SpotifyConnectionStatus
  isLoading: boolean
  error: string | null
}

// Initial State
const initialState: SpotifyContextState = {
  auth: {
    isAuthenticated: false,
    isAuthenticating: false,
    token: null,
    refreshToken: null,
    expiresAt: null,
    user: null,
    isPremium: false,
    error: null
  },
  player: {
    isReady: false,
    isConnected: false,
    deviceId: null,
    player: null
  },
  connection: {
    isConnected: false,
    isPremium: false,
    deviceName: null,
    error: null
  },
  isLoading: false,
  error: null
}

// Action Types
type SpotifyAction = 
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_AUTH_STATE'; payload: SpotifyAuthState }
  | { type: 'SET_PLAYER_STATE'; payload: SpotifyPlayerState }
  | { type: 'SET_CONNECTION_STATUS'; payload: Partial<SpotifyConnectionStatus> }
  | { type: 'RESET_STATE' }

// Reducer
function spotifyReducer(state: SpotifyContextState, action: SpotifyAction): SpotifyContextState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload }
    
    case 'SET_ERROR':
      return { ...state, error: action.payload }
    
    case 'SET_AUTH_STATE':
      return {
        ...state,
        auth: action.payload,
        connection: {
          ...state.connection,
          isPremium: action.payload.isPremium
        }
      }
    
    case 'SET_PLAYER_STATE':
      return {
        ...state,
        player: action.payload,
        connection: {
          ...state.connection,
          isConnected: action.payload.isConnected,
          deviceName: action.payload.player ? 'FocusHive Music Player' : null
        }
      }
    
    case 'SET_CONNECTION_STATUS':
      return {
        ...state,
        connection: { ...state.connection, ...action.payload }
      }
    
    case 'RESET_STATE':
      return initialState
    
    default:
      return state
  }
}

// Context Type
interface SpotifyContextType {
  state: SpotifyContextState
  
  // Authentication
  login: () => void
  logout: () => void
  handleAuthCallback: (code: string, state: string) => Promise<boolean>
  
  // Player Management
  initializePlayer: () => Promise<boolean>
  connectPlayer: () => Promise<boolean>
  disconnectPlayer: () => void
  transferPlaybackHere: () => Promise<boolean>
  
  // Playback Controls
  play: (options?: PlayOptions) => Promise<void>
  pause: () => Promise<void>
  next: () => Promise<void>
  previous: () => Promise<void>
  seek: (positionMs: number) => Promise<void>
  setVolume: (volume: number) => Promise<void>
  
  // Utility
  getPlayerInstance: () => Spotify.Player | null
  isFeatureAvailable: (feature: 'play' | 'premium' | 'connect') => boolean
}

// Create Context
const SpotifyContext = createContext<SpotifyContextType | undefined>(undefined)

// Environment Configuration
const getSpotifyConfig = (): SpotifyConfig => {
  const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID
  const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI || `${window.location.origin}/music/spotify/callback`
  const musicServiceUrl = import.meta.env.VITE_MUSIC_SERVICE_URL || 'http://localhost:8084'

  if (!clientId) {
    throw new Error('VITE_SPOTIFY_CLIENT_ID environment variable is required')
  }

  return {
    clientId,
    redirectUri,
    musicServiceUrl,
    scopes: [
      'streaming',
      'user-read-email',
      'user-read-private',
      'user-read-playback-state',
      'user-modify-playback-state',
      'user-read-currently-playing'
    ]
  }
}

// Provider Props
interface SpotifyProviderProps {
  children: React.ReactNode
  autoConnect?: boolean
}

// Provider Component
export const SpotifyProvider: React.FC<SpotifyProviderProps> = ({ 
  children, 
  autoConnect = true 
}) => {
  const [state, dispatch] = useReducer(spotifyReducer, initialState)
  const spotifyServiceRef = useRef<SpotifyService | null>(null)
  const playerStateIntervalRef = useRef<NodeJS.Timeout | null>(null)

  // Initialize Spotify service
  useEffect(() => {
    try {
      const config = getSpotifyConfig()
      spotifyServiceRef.current = getSpotifyService(config)
      
      // Load initial auth state
      const authState = spotifyServiceRef.current.getAuthState()
      dispatch({ type: 'SET_AUTH_STATE', payload: authState })
      
      if (authState.isAuthenticated && autoConnect) {
        initializePlayer()
      }
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to initialize Spotify integration' })
    }
  }, [autoConnect])

  // Sync service state with context state
  useEffect(() => {
    if (!spotifyServiceRef.current) return

    const syncState = () => {
      const authState = spotifyServiceRef.current!.getAuthState()
      const playerState = spotifyServiceRef.current!.getPlayerState()
      
      dispatch({ type: 'SET_AUTH_STATE', payload: authState })
      dispatch({ type: 'SET_PLAYER_STATE', payload: playerState })
    }

    // Sync initially and then periodically
    syncState()
    const interval = setInterval(syncState, 5000)

    return () => clearInterval(interval)
  }, [])

  // Authentication functions
  const login = useCallback(() => {
    if (!spotifyServiceRef.current) return
    
    dispatch({ type: 'SET_LOADING', payload: true })
    spotifyServiceRef.current.startAuthFlow()
  }, [])

  const logout = useCallback(() => {
    if (!spotifyServiceRef.current) return
    
    spotifyServiceRef.current.clearAuth()
    dispatch({ type: 'RESET_STATE' })
  }, [])

  const handleAuthCallback = useCallback(async (code: string, state: string): Promise<boolean> => {
    if (!spotifyServiceRef.current) return false
    
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const success = await spotifyServiceRef.current.handleAuthCallback(code, state)
      
      if (success && autoConnect) {
        await initializePlayer()
      }
      
      return success
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Authentication failed' })
      return false
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [autoConnect])

  // Player Management
  const initializePlayer = useCallback(async (): Promise<boolean> => {
    if (!spotifyServiceRef.current) return false
    
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      dispatch({ type: 'SET_ERROR', payload: null })
      
      const success = await spotifyServiceRef.current.initializePlayer(
        'FocusHive Music Player',
        0.5
      )
      
      if (success) {
        // Start monitoring player state
        startPlayerStateMonitoring()
      } else {
        dispatch({ type: 'SET_ERROR', payload: 'Failed to initialize Spotify player' })
      }
      
      return success
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to connect to Spotify' })
      return false
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  const connectPlayer = useCallback(async (): Promise<boolean> => {
    if (!state.player.player) {
      return await initializePlayer()
    }
    
    try {
      const success = await state.player.player.connect()
      return success
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to connect player' })
      return false
    }
  }, [state.player.player, initializePlayer])

  const disconnectPlayer = useCallback(() => {
    if (spotifyServiceRef.current) {
      spotifyServiceRef.current.disconnect()
    }
    
    if (playerStateIntervalRef.current) {
      clearInterval(playerStateIntervalRef.current)
      playerStateIntervalRef.current = null
    }
  }, [])

  const transferPlaybackHere = useCallback(async (): Promise<boolean> => {
    if (!spotifyServiceRef.current || !state.player.deviceId) {
      return false
    }
    
    try {
      await spotifyServiceRef.current.transferPlayback({
        deviceIds: [state.player.deviceId],
        play: false
      })
      return true
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to transfer playback to this device' })
      return false
    }
  }, [state.player.deviceId])

  // Playback Controls
  const play = useCallback(async (options?: PlayOptions): Promise<void> => {
    if (!spotifyServiceRef.current || !state.player.deviceId) {
      throw new Error('Spotify player not ready')
    }
    
    await spotifyServiceRef.current.play(state.player.deviceId, options)
  }, [state.player.deviceId])

  const pause = useCallback(async (): Promise<void> => {
    if (!spotifyServiceRef.current) {
      throw new Error('Spotify player not ready')
    }
    
    if (state.player.player) {
      await state.player.player.pause()
    } else {
      await spotifyServiceRef.current.pause(state.player.deviceId || undefined)
    }
  }, [state.player.player, state.player.deviceId])

  const next = useCallback(async (): Promise<void> => {
    if (!spotifyServiceRef.current) {
      throw new Error('Spotify player not ready')
    }
    
    if (state.player.player) {
      await state.player.player.nextTrack()
    } else {
      await spotifyServiceRef.current.next(state.player.deviceId || undefined)
    }
  }, [state.player.player, state.player.deviceId])

  const previous = useCallback(async (): Promise<void> => {
    if (!spotifyServiceRef.current) {
      throw new Error('Spotify player not ready')
    }
    
    if (state.player.player) {
      await state.player.player.previousTrack()
    } else {
      await spotifyServiceRef.current.previous(state.player.deviceId || undefined)
    }
  }, [state.player.player, state.player.deviceId])

  const seek = useCallback(async (positionMs: number): Promise<void> => {
    if (!spotifyServiceRef.current) {
      throw new Error('Spotify player not ready')
    }
    
    if (state.player.player) {
      await state.player.player.seek(positionMs)
    } else {
      await spotifyServiceRef.current.seek(positionMs, state.player.deviceId || undefined)
    }
  }, [state.player.player, state.player.deviceId])

  const setVolume = useCallback(async (volume: number): Promise<void> => {
    if (!spotifyServiceRef.current) {
      throw new Error('Spotify player not ready')
    }
    
    const volumePercent = Math.round(volume * 100)
    
    if (state.player.player) {
      await state.player.player.setVolume(volume)
    } else {
      await spotifyServiceRef.current.setVolume(volumePercent, state.player.deviceId || undefined)
    }
  }, [state.player.player, state.player.deviceId])

  // Utility functions
  const getPlayerInstance = useCallback((): Spotify.Player | null => {
    return state.player.player
  }, [state.player.player])

  const isFeatureAvailable = useCallback((feature: 'play' | 'premium' | 'connect'): boolean => {
    switch (feature) {
      case 'connect':
        return state.auth.isAuthenticated
      case 'premium':
        return state.auth.isPremium
      case 'play':
        return state.auth.isPremium && state.player.isConnected
      default:
        return false
    }
  }, [state.auth.isAuthenticated, state.auth.isPremium, state.player.isConnected])

  // Helper function to monitor player state
  const startPlayerStateMonitoring = useCallback(() => {
    if (playerStateIntervalRef.current) {
      clearInterval(playerStateIntervalRef.current)
    }

    playerStateIntervalRef.current = setInterval(() => {
      if (spotifyServiceRef.current) {
        const playerState = spotifyServiceRef.current.getPlayerState()
        dispatch({ type: 'SET_PLAYER_STATE', payload: playerState })
      }
    }, 1000)
  }, [])

  // Cleanup
  useEffect(() => {
    return () => {
      if (playerStateIntervalRef.current) {
        clearInterval(playerStateIntervalRef.current)
      }
      disconnectPlayer()
    }
  }, [disconnectPlayer])

  const contextValue: SpotifyContextType = {
    state,
    
    // Authentication
    login,
    logout,
    handleAuthCallback,
    
    // Player Management
    initializePlayer,
    connectPlayer,
    disconnectPlayer,
    transferPlaybackHere,
    
    // Playback Controls
    play,
    pause,
    next,
    previous,
    seek,
    setVolume,
    
    // Utility
    getPlayerInstance,
    isFeatureAvailable
  }

  return (
    <SpotifyContext.Provider value={contextValue}>
      {children}
    </SpotifyContext.Provider>
  )
}

// Hook to use Spotify context
export const useSpotify = (): SpotifyContextType => {
  const context = useContext(SpotifyContext)
  if (context === undefined) {
    throw new Error('useSpotify must be used within a SpotifyProvider')
  }
  return context
}

export default SpotifyContext