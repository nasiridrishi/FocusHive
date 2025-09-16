import {useCallback, useEffect, useRef, useState} from 'react'
import {useSpotify} from '../context/useSpotifyContext'
import {PlaybackState, Track} from '../types'
import type {UseSpotifyPlayerOptions} from '../../../types/spotify'

// Enhanced playback state for the hook (extending the base PlaybackState)
interface SpotifyPlaybackState extends PlaybackState {
  currentTrack: Track | null
  canSkipNext: boolean
  canSkipPrevious: boolean
  canSeek: boolean
}

/**
 * Enhanced Spotify Player Hook
 * Integrates with SpotifyContext for full SDK functionality
 */
export const useSpotifyPlayer = (options?: Partial<UseSpotifyPlayerOptions>): {
  isConnected: boolean;
  isAuthenticated: boolean;
  isPremium: boolean;
  deviceId: string | null;
  isLoading: boolean;
  playbackState: SpotifyPlaybackState;
  error: string | null;
  play: (trackUri?: string) => Promise<void>;
  pause: () => Promise<void>;
  togglePlay: () => Promise<void>;
  seekTo: (positionSeconds: number) => Promise<void>;
  setVolume: (volume: number) => Promise<void>;
  skipNext: () => Promise<void>;
  skipPrevious: () => Promise<void>;
  getCurrentState: () => Promise<import('../../../types/spotify').Spotify.PlaybackState | null>;
  transferPlayback: () => Promise<boolean>;
  playTrack: (spotifyUri: string) => Promise<boolean>;
  playPlaylist: (playlistUri: string, trackOffset?: number) => Promise<boolean>;
  connect: () => Promise<boolean>;
  disconnect: () => void;
  canPlay: boolean;
  canConnect: boolean;
  clearError: () => void;
} => {
  const spotify = useSpotify()
  const [playbackState, setPlaybackState] = useState<SpotifyPlaybackState>({
    isPlaying: false,
    isPaused: true,
    isBuffering: false,
    currentTime: 0,
    duration: 0,
    volume: options?.volume || 0.5,
    isMuted: false,
    playbackRate: 1,
    currentTrack: null,
    canSkipNext: true,
    canSkipPrevious: true,
    canSeek: true
  })
  const [error, setError] = useState<string | null>(null)
  const positionUpdateRef = useRef<NodeJS.Timeout | null>(null)
  const lastStateRef = useRef<import('../../../types/spotify').Spotify.PlaybackState | null>(null)

  // Monitor player state from SDK
  useEffect(() => {
    const player = spotify.getPlayerInstance()
    if (!player) return

    const handlePlayerStateChange = (state: import('../../../types/spotify').Spotify.PlaybackState | null): unknown[] => {
      if (!state) {
        setPlaybackState(prev => ({...prev, isPlaying: false, isPaused: true, currentTrack: null}))
        return
      }

      lastStateRef.current = state

      // Convert Spotify track to our Track format
      let currentTrack: Track | null = null
      if (state.track_window.current_track) {
        const spotifyTrack = state.track_window.current_track
        currentTrack = {
          id: spotifyTrack.id,
          title: spotifyTrack.name,
          artist: spotifyTrack.artists.map(a => a.name).join(', '),
          album: spotifyTrack.album.name,
          duration: spotifyTrack.duration_ms / 1000,
          albumArt: spotifyTrack.album.images[0]?.url,
          spotifyId: spotifyTrack.id,
          explicit: spotifyTrack.explicit,
          popularity: spotifyTrack.popularity || 50
        }
      }

      setPlaybackState(prev => ({
        ...prev,
        isPlaying: !state.paused,
        isPaused: state.paused,
        isBuffering: false, // Spotify SDK doesn't provide loading state directly
        currentTime: state.position / 1000, // Convert to seconds
        duration: currentTrack?.duration || 0,
        currentTrack,
        canSkipNext: !state.disallows.skipping_next,
        canSkipPrevious: !state.disallows.skipping_prev,
        canSeek: !state.disallows.seeking
      }))
    }

    // Add listener for player state changes
    player.addListener('player_state_changed', handlePlayerStateChange)

    // Initial state fetch
    player.getCurrentState().then(handlePlayerStateChange).catch(() => {
    })

    return () => {
      player.removeListener('player_state_changed', handlePlayerStateChange)
    }
  }, [spotify])

  // Position tracking for smooth updates
  useEffect(() => {
    if (playbackState.isPlaying && spotify.state.player.isConnected) {
      positionUpdateRef.current = setInterval(() => {
        setPlaybackState(prev => ({
          ...prev,
          currentTime: Math.min(prev.currentTime + 1, prev.duration)
        }))
      }, 1000)
    } else {
      if (positionUpdateRef.current) {
        clearInterval(positionUpdateRef.current)
        positionUpdateRef.current = null
      }
    }

    return () => {
      if (positionUpdateRef.current) {
        clearInterval(positionUpdateRef.current)
      }
    }
  }, [playbackState.isPlaying, spotify.state.player.isConnected])

  // Player controls with error handling
  const play = useCallback(async (trackUri?: string) => {
    try {
      setError(null)
      if (trackUri) {
        await spotify.play({uris: [trackUri]})
      } else {
        await spotify.play()
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to play'
      setError(message)
      throw err
    }
  }, [spotify])

  const pause = useCallback(async () => {
    try {
      setError(null)
      await spotify.pause()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to pause'
      setError(message)
      throw err
    }
  }, [spotify])

  const togglePlay = useCallback(async () => {
    if (playbackState.isPlaying) {
      await pause()
    } else {
      await play()
    }
  }, [playbackState.isPlaying, play, pause])

  const seekTo = useCallback(async (positionSeconds: number) => {
    if (!playbackState.canSeek) {
      throw new Error('Seeking not allowed for this track')
    }

    try {
      setError(null)
      await spotify.seek(positionSeconds * 1000) // Convert to ms
      setPlaybackState(prev => ({...prev, currentTime: positionSeconds}))
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to seek'
      setError(message)
      throw err
    }
  }, [spotify, playbackState.canSeek])

  const setVolume = useCallback(async (volume: number) => {
    const clampedVolume = Math.max(0, Math.min(1, volume))
    try {
      setError(null)
      await spotify.setVolume(clampedVolume)
      setPlaybackState(prev => ({...prev, volume: clampedVolume}))
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to set volume'
      setError(message)
      throw err
    }
  }, [spotify])

  const skipNext = useCallback(async () => {
    if (!playbackState.canSkipNext) {
      throw new Error('Skipping to next track not allowed')
    }

    try {
      setError(null)
      await spotify.next()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to skip to next track'
      setError(message)
      throw err
    }
  }, [spotify, playbackState.canSkipNext])

  const skipPrevious = useCallback(async () => {
    if (!playbackState.canSkipPrevious) {
      throw new Error('Skipping to previous track not allowed')
    }

    try {
      setError(null)
      await spotify.previous()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to skip to previous track'
      setError(message)
      throw err
    }
  }, [spotify, playbackState.canSkipPrevious])

  const getCurrentState = useCallback(async () => {
    try {
      const player = spotify.getPlayerInstance()
      if (player) {
        return await player.getCurrentState()
      }
      return null
    } catch {
      // console.error('Failed to get Spotify player state');
      return null
    }
  }, [spotify])

  const transferPlayback = useCallback(async () => {
    try {
      setError(null)
      return await spotify.transferPlaybackHere()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to transfer playback'
      setError(message)
      throw err
    }
  }, [spotify])

  const playTrack = useCallback(async (spotifyUri: string) => {
    try {
      setError(null)
      await play(spotifyUri)
      return true
    } catch {
      // console.error('Failed to play track');
      return false
    }
  }, [play])

  const playPlaylist = useCallback(async (playlistUri: string, trackOffset?: number) => {
    try {
      setError(null)
      const playOptions: {
        context_uri: string;
        offset?: { position: number }
      } = {context_uri: playlistUri}
      if (trackOffset !== undefined) {
        playOptions.offset = {position: trackOffset}
      }
      await spotify.play(playOptions)
      return true
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to play playlist'
      setError(message)
      return false
    }
  }, [spotify])

  return {
    // State from context
    isConnected: spotify.state.player.isConnected,
    isAuthenticated: spotify.state.auth.isAuthenticated,
    isPremium: spotify.state.auth.isPremium,
    deviceId: spotify.state.player.deviceId,
    isLoading: spotify.state.isLoading,

    // Playback state
    playbackState,
    error: error || spotify.state.error,

    // Basic controls
    play,
    pause,
    togglePlay,
    seekTo,
    setVolume,
    skipNext,
    skipPrevious,

    // Advanced controls
    getCurrentState,
    transferPlayback,
    playTrack,
    playPlaylist,

    // Connection management
    connect: spotify.connectPlayer,
    disconnect: spotify.disconnectPlayer,

    // Feature availability
    canPlay: spotify.isFeatureAvailable('play'),
    canConnect: spotify.isFeatureAvailable('connect'),

    // Utilities
    clearError: () => setError(null)
  }
}