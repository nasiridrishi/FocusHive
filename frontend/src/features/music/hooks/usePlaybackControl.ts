import { useState, useCallback, useEffect, useRef } from 'react'
import { Track } from '../types'
import { useMusic } from '../context'

interface PlaybackControlOptions {
  autoNext?: boolean
  crossfade?: number // in seconds
  gaplessPlayback?: boolean
  scrobbleThreshold?: number // percentage of track to scrobble (0-1)
}

interface PlaybackHistory {
  track: Track
  playedAt: Date
  duration: number
  completed: boolean
}

/**
 * Custom hook for advanced playback control
 * Provides enhanced audio control with features like crossfade, gapless playback
 */
export const usePlaybackControl = (options: PlaybackControlOptions = {}) => {
  const musicContext = useMusic()
  const { state, play, pause, resume, stop, seekTo, setVolume, toggleMute, skipNext, skipPrevious } = musicContext
  const { playbackState, currentTrack, queue } = state
  const [history, setHistory] = useState<PlaybackHistory[]>([])
  const [isShuffling, setIsShuffling] = useState(false)
  const [repeatMode, setRepeatMode] = useState<'none' | 'one' | 'all'>('none')
  const [crossfadeState, setCrossfadeState] = useState({
    isActive: false,
    progress: 0,
  })
  
  const scrobbleTimerRef = useRef<NodeJS.Timeout | null>(null)
  const crossfadeTimerRef = useRef<NodeJS.Timeout | null>(null)
  const lastScrobbledTrack = useRef<string | null>(null)
  
  const {
    autoNext = true,
    crossfade = 0,
    gaplessPlayback = false,
    scrobbleThreshold = 0.5,
  } = options

  // TODO: Implement gapless playback feature
  void gaplessPlayback; // Mark as intentionally used for future feature

  // Enhanced play function with crossfade
  const playWithCrossfade = useCallback(async (track?: Track) => {
    if (crossfade > 0 && playbackState.isPlaying) {
      // Start crossfade
      setCrossfadeState({ isActive: true, progress: 0 })
      
      const fadeOutDuration = crossfade * 1000
      const fadeOutSteps = 20
      const fadeOutInterval = fadeOutDuration / fadeOutSteps
      const volumeStep = playbackState.volume / fadeOutSteps
      
      let currentVolume = playbackState.volume
      
      const fadeOutTimer = setInterval(() => {
        currentVolume -= volumeStep
        if (currentVolume <= 0) {
          clearInterval(fadeOutTimer)
          currentVolume = 0
        }
        setVolume(currentVolume)
        setCrossfadeState(prev => ({ 
          ...prev, 
          progress: (playbackState.volume - currentVolume) / playbackState.volume 
        }))
      }, fadeOutInterval)
      
      // Switch track after fade out
      setTimeout(async () => {
        await play(track)
        
        // Fade in new track
        const fadeInSteps = 20
        const fadeInInterval = fadeOutDuration / fadeInSteps
        const targetVolume = playbackState.volume
        currentVolume = 0
        setVolume(0)
        
        const fadeInTimer = setInterval(() => {
          currentVolume += targetVolume / fadeInSteps
          if (currentVolume >= targetVolume) {
            clearInterval(fadeInTimer)
            currentVolume = targetVolume
            setCrossfadeState({ isActive: false, progress: 0 })
          }
          setVolume(currentVolume)
        }, fadeInInterval)
      }, fadeOutDuration)
      
    } else {
      await play(track)
    }
  }, [crossfade, playbackState.isPlaying, playbackState.volume, setVolume, play])

  // Enhanced skip next with repeat and shuffle logic
  const skipNextEnhanced = useCallback(() => {
    if (repeatMode === 'one' && currentTrack) {
      playWithCrossfade(currentTrack)
      return
    }

    const currentIndex = queue.findIndex(item => item.id === currentTrack?.id)
    let nextIndex = currentIndex + 1

    if (isShuffling) {
      // Shuffle mode: pick random track
      const availableIndices = queue
        .map((_, index) => index)
        .filter(index => index !== currentIndex)
      nextIndex = availableIndices[Math.floor(Math.random() * availableIndices.length)]
    } else if (nextIndex >= queue.length) {
      // End of queue
      if (repeatMode === 'all') {
        nextIndex = 0
      } else {
        // No more tracks
        return
      }
    }

    if (nextIndex < queue.length && nextIndex >= 0) {
      const nextTrack = queue[nextIndex]
      playWithCrossfade(nextTrack)
    }
  }, [repeatMode, currentTrack, queue, isShuffling, playWithCrossfade])

  // Enhanced skip previous
  const skipPreviousEnhanced = useCallback(() => {
    if (playbackState.currentTime > 3) {
      // If more than 3 seconds played, restart current track
      seekTo(0)
      return
    }

    const currentIndex = queue.findIndex(item => item.id === currentTrack?.id)
    
    if (currentIndex > 0) {
      const previousTrack = queue[currentIndex - 1]
      playWithCrossfade(previousTrack)
    } else if (repeatMode === 'all' && queue.length > 0) {
      const lastTrack = queue[queue.length - 1]
      playWithCrossfade(lastTrack)
    }
  }, [playbackState.currentTime, queue, currentTrack, repeatMode, seekTo, playWithCrossfade])

  // Toggle shuffle mode
  const toggleShuffle = useCallback(() => {
    setIsShuffling(prev => !prev)
  }, [])

  // Cycle through repeat modes
  const toggleRepeat = useCallback(() => {
    setRepeatMode(prev => {
      switch (prev) {
        case 'none': return 'one'
        case 'one': return 'all'
        case 'all': return 'none'
        default: return 'none'
      }
    })
  }, [])

  // Quick seek (±10 seconds)
  const quickSeekBackward = useCallback(() => {
    const newTime = Math.max(0, playbackState.currentTime - 10)
    seekTo(newTime)
  }, [playbackState.currentTime, seekTo])

  const quickSeekForward = useCallback(() => {
    const newTime = Math.min(playbackState.duration, playbackState.currentTime + 10)
    seekTo(newTime)
  }, [playbackState.currentTime, playbackState.duration, seekTo])

  // Volume fade
  const fadeVolume = useCallback((targetVolume: number, duration: number = 1000) => {
    const steps = 20
    const interval = duration / steps
    const currentVolume = playbackState.volume
    const volumeStep = (targetVolume - currentVolume) / steps
    
    let step = 0
    const timer = setInterval(() => {
      step++
      const newVolume = currentVolume + (volumeStep * step)
      setVolume(Math.max(0, Math.min(1, newVolume)))
      
      if (step >= steps) {
        clearInterval(timer)
        setVolume(targetVolume)
      }
    }, interval)
    
    return () => clearInterval(timer)
  }, [playbackState.volume, setVolume])

  // Mute with fade
  const muteWithFade = useCallback(() => {
    return fadeVolume(0, 500)
  }, [fadeVolume])

  // Unmute with fade
  const unmuteWithFade = useCallback((targetVolume: number = 1) => {
    return fadeVolume(targetVolume, 500)
  }, [fadeVolume])

  // Track scrobbling (for analytics)
  const handleScrobble = useCallback((track: Track, playTime: number, completed: boolean) => {
    if (lastScrobbledTrack.current === track.id && !completed) {
      return // Avoid duplicate scrobbles
    }
    
    const historyEntry: PlaybackHistory = {
      track,
      playedAt: new Date(),
      duration: playTime,
      completed,
    }
    
    setHistory(prev => [historyEntry, ...prev.slice(0, 99)]) // Keep last 100 tracks
    
    // TODO: Send to analytics service
    
    if (completed) {
      lastScrobbledTrack.current = null
    } else {
      lastScrobbledTrack.current = track.id
    }
  }, [])

  // Monitor playback for scrobbling
  useEffect(() => {
    if (!currentTrack || !playbackState.isPlaying) {
      if (scrobbleTimerRef.current) {
        clearTimeout(scrobbleTimerRef.current)
        scrobbleTimerRef.current = null
      }
      return
    }

    const track = currentTrack
    const scrobbleTime = track.duration * scrobbleThreshold

    scrobbleTimerRef.current = setTimeout(() => {
      handleScrobble(track, playbackState.currentTime, false)
    }, scrobbleTime * 1000)

    return () => {
      if (scrobbleTimerRef.current) {
        clearTimeout(scrobbleTimerRef.current)
        scrobbleTimerRef.current = null
      }
    }
  }, [currentTrack, playbackState.isPlaying, scrobbleThreshold, handleScrobble, playbackState.currentTime])

  // Handle track end for scrobbling
  useEffect(() => {
    if (!playbackState.isPlaying && currentTrack && 
        playbackState.currentTime >= playbackState.duration - 1) {
      // Track completed
      handleScrobble(currentTrack, playbackState.duration, true)
      
      if (autoNext) {
        skipNextEnhanced()
      }
    }
  }, [playbackState.isPlaying, currentTrack, playbackState.currentTime, playbackState.duration, autoNext, skipNextEnhanced, handleScrobble])

  // Keyboard shortcuts
  const handleKeyPress = useCallback((event: KeyboardEvent) => {
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
      return // Don't handle shortcuts in input fields
    }

    switch (event.code) {
      case 'Space':
        event.preventDefault()
        if (playbackState.isPlaying) {
          pause()
        } else {
          resume()
        }
        break
      case 'ArrowRight':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          skipNextEnhanced()
        }
        break
      case 'ArrowLeft':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          skipPreviousEnhanced()
        }
        break
      case 'ArrowUp':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          const newVolume = Math.min(1, playbackState.volume + 0.1)
          setVolume(newVolume)
        }
        break
      case 'ArrowDown':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          const newVolume = Math.max(0, playbackState.volume - 0.1)
          setVolume(newVolume)
        }
        break
      case 'KeyM':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          if (playbackState.isMuted) {
            unmuteWithFade()
          } else {
            muteWithFade()
          }
        }
        break
      case 'KeyS':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          toggleShuffle()
        }
        break
      case 'KeyR':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          toggleRepeat()
        }
        break
    }
  }, [playbackState, pause, resume, skipNextEnhanced, skipPreviousEnhanced, setVolume, unmuteWithFade, muteWithFade, toggleShuffle, toggleRepeat])

  // Register keyboard shortcuts
  useEffect(() => {
    window.addEventListener('keydown', handleKeyPress)
    return () => window.removeEventListener('keydown', handleKeyPress)
  }, [handleKeyPress])

  // Cleanup timers
  useEffect(() => {
    return () => {
      if (scrobbleTimerRef.current) clearTimeout(scrobbleTimerRef.current)
      if (crossfadeTimerRef.current) clearTimeout(crossfadeTimerRef.current)
    }
  }, [])

  return {
    // Enhanced playback controls
    playWithCrossfade,
    skipNextEnhanced,
    skipPreviousEnhanced,
    quickSeekBackward,
    quickSeekForward,
    
    // Volume controls
    fadeVolume,
    muteWithFade,
    unmuteWithFade,
    
    // Playback modes
    isShuffling,
    repeatMode,
    toggleShuffle,
    toggleRepeat,
    
    // State
    history,
    crossfadeState,
    
    // Utilities
    clearHistory: () => setHistory([]),
    
    // Basic controls (re-exported for convenience)
    play,
    pause,
    resume,
    stop,
    seekTo,
    setVolume,
    skipNext,
    skipPrevious,
    toggleMute,
  }
}