import { useState, useCallback, useEffect } from 'react'
import { Track, WebSocketMessage, UserJoinedPayload, TrackVotedPayload } from '../types'
import { useMusic } from '../context'
import { useMusicWebSocket } from './useMusicWebSocket'

interface CollaborativeState {
  activeUsers: Array<{
    id: string
    name: string
    avatar?: string
    lastSeen: Date
  }>
  currentDJ: {
    id: string
    name: string
    avatar?: string
  } | null
  permissions: {
    canAddTracks: boolean
    canVote: boolean
    canSkip: boolean
    canReorder: boolean
  }
  votingEnabled: boolean
  skipThreshold: number // Number of votes needed to skip
}

interface CollaborativeOptions {
  hiveId: string
  maxQueueSize?: number
  enableVoting?: boolean
  skipThreshold?: number
  djMode?: boolean // If true, only DJ can control playback
}

/**
 * Custom hook for collaborative playlist management
 * Handles real-time collaboration features for music in hives
 */
export const useCollaborativePlaylist = (options: CollaborativeOptions) => {
  const musicContext = useMusic()
  const { state, addToQueue, removeFromQueue, reorderQueue } = musicContext
  const { queue, currentTrack } = state
  const [collaborativeState, setCollaborativeState] = useState<CollaborativeState>({
    activeUsers: [],
    currentDJ: null,
    permissions: {
      canAddTracks: true,
      canVote: true,
      canSkip: false,
      canReorder: false,
    },
    votingEnabled: options.enableVoting ?? true,
    skipThreshold: options.skipThreshold ?? 3,
  })
  
  const [skipVotes, setSkipVotes] = useState<Set<string>>(new Set())
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const {
    hiveId,
    maxQueueSize = 50,
    enableVoting = true,
    djMode = false,
  } = options

  // WebSocket connection for real-time updates
  const { 
    isConnected, 
    sendMessage,
    joinHive,
    leaveHive 
  } = useMusicWebSocket({
    hiveId,
    onMessage: (message: WebSocketMessage<unknown>) => {
      switch (message.type) {
        case 'user_joined': {
          const payload = message.payload as UserJoinedPayload
          setCollaborativeState(prev => ({
            ...prev,
            activeUsers: [...prev.activeUsers.filter(u => u.id !== message.userId), {
              id: message.userId,
              name: payload.name,
              avatar: payload.avatar,
              lastSeen: new Date(),
            }]
          }))
          break
        }
          
        case 'user_left':
          setCollaborativeState(prev => ({
            ...prev,
            activeUsers: prev.activeUsers.filter(u => u.id !== message.userId)
          }))
          break
          
        case 'track_voted': {
          const payload = message.payload as TrackVotedPayload
          // Handle vote updates
          if (payload.type === 'skip') {
            setSkipVotes(prev => new Set([...prev, message.userId]))
          }
          break
        }
          
        default:
          break
      }
    }
  })

  // Join hive on mount
  useEffect(() => {
    if (isConnected && hiveId) {
      joinHive(hiveId)
    }
    
    return () => {
      if (hiveId) {
        leaveHive(hiveId)
      }
    }
  }, [isConnected, hiveId, joinHive, leaveHive])

  // Check if user can perform action
  const canPerformAction = useCallback((action: 'add' | 'vote' | 'skip' | 'reorder') => {
    const permissions = collaborativeState.permissions
    
    switch (action) {
      case 'add':
        return permissions.canAddTracks && queue.length < maxQueueSize
      case 'vote':
        return permissions.canVote && enableVoting
      case 'skip':
        return permissions.canSkip || !djMode
      case 'reorder':
        return permissions.canReorder || !djMode
      default:
        return false
    }
  }, [collaborativeState.permissions, queue.length, maxQueueSize, enableVoting, djMode])

  // Add track to collaborative queue
  const addTrackToQueue = useCallback(async (track: Track, position?: number) => {
    if (!canPerformAction('add')) {
      setError('You do not have permission to add tracks')
      return false
    }

    try {
      setIsLoading(true)
      setError(null)
      
      await addToQueue(track, position)
      
      // Notify other users
      sendMessage('track_added', {
        track,
        position,
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to add track'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [canPerformAction, addToQueue, sendMessage, hiveId])

  // Vote on track in queue
  const voteOnQueueTrack = useCallback(async (queueId: string, vote: 'up' | 'down') => {
    if (!canPerformAction('vote')) {
      setError('You do not have permission to vote')
      return false
    }

    try {
      setIsLoading(true)
      setError(null)
      
      // TODO: Implement voteOnTrack functionality
      console.warn('voteOnTrack not yet implemented', { queueId, vote })
      
      // Notify other users
      sendMessage('track_voted', {
        queueId,
        vote,
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to vote'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [canPerformAction, sendMessage, hiveId])

  // Request to skip current track
  const requestSkip = useCallback(async () => {
    if (!canPerformAction('skip')) {
      setError('You do not have permission to skip tracks')
      return false
    }

    try {
      setIsLoading(true)
      setError(null)
      
      // Add skip vote
      sendMessage('skip_vote', {
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to request skip'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [canPerformAction, sendMessage, hiveId])

  // Reorder queue items
  const reorderQueueItems = useCallback(async (fromIndex: number, toIndex: number) => {
    if (!canPerformAction('reorder')) {
      setError('You do not have permission to reorder tracks')
      return false
    }

    try {
      setIsLoading(true)
      setError(null)
      
      await reorderQueue(fromIndex, toIndex)
      
      // Notify other users
      sendMessage('queue_reordered', {
        fromIndex,
        toIndex,
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to reorder queue'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [canPerformAction, reorderQueue, sendMessage, hiveId])

  // Remove track from queue
  const removeTrackFromQueue = useCallback(async (queueId: string) => {
    try {
      setIsLoading(true)
      setError(null)
      
      await removeFromQueue(queueId)
      
      // Notify other users
      sendMessage('track_removed', {
        queueId,
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to remove track'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [removeFromQueue, sendMessage, hiveId])

  // Become DJ (if DJ mode is enabled)
  const becomeDJ = useCallback(async () => {
    if (!djMode) return false

    try {
      setIsLoading(true)
      setError(null)
      
      sendMessage('become_dj', {
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to become DJ'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [djMode, sendMessage, hiveId])

  // Step down as DJ
  const stepDownDJ = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      
      sendMessage('step_down_dj', {
        hiveId,
      })
      
      return true
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to step down as DJ'
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [sendMessage, hiveId])

  // Get voting summary for current track
  const getVotingSummary = useCallback(() => {
    if (!currentTrack) return null

    const currentQueueItem = queue.find(item => item.id === currentTrack?.id)
    if (!currentQueueItem) return null

    return {
      upVotes: Math.max(0, currentQueueItem.votes || 0),
      downVotes: 0, // Would need to track separately
      netVotes: currentQueueItem.votes || 0,
      userVote: currentQueueItem.userVote,
      skipVotes: skipVotes.size,
      skipThreshold: collaborativeState.skipThreshold,
      canSkip: skipVotes.size >= collaborativeState.skipThreshold,
    }
  }, [currentTrack, queue, skipVotes.size, collaborativeState.skipThreshold])

  // Check if skip threshold is reached
  useEffect(() => {
    if (skipVotes.size >= collaborativeState.skipThreshold) {
      // Auto-skip track
      sendMessage('auto_skip', { hiveId })
      setSkipVotes(new Set()) // Reset skip votes
    }
  }, [skipVotes.size, collaborativeState.skipThreshold, sendMessage, hiveId])

  // Get queue statistics
  const getQueueStats = useCallback(() => {
    const totalDuration = queue.reduce((sum, item) => sum + item.duration, 0)
    const averageVotes = queue.length > 0 
      ? queue.reduce((sum, item) => sum + (item.votes || 0), 0) / queue.length 
      : 0
    
    const contributorCounts = queue.reduce((counts, item) => {
      if (item.addedBy) {
        counts[item.addedBy.id] = (counts[item.addedBy.id] || 0) + 1
      }
      return counts
    }, {} as Record<string, number>)

    return {
      totalTracks: queue.length,
      totalDuration,
      averageVotes,
      topContributors: Object.entries(contributorCounts)
        .sort(([,a], [,b]) => b - a)
        .slice(0, 3)
        .map(([userId, count]) => ({
          userId,
          count,
          user: collaborativeState.activeUsers.find(u => u.id === userId)
        }))
    }
  }, [queue, collaborativeState.activeUsers])

  return {
    // State
    collaborativeState,
    skipVotes: skipVotes.size,
    isLoading,
    error,
    isConnected,
    
    // Actions
    addTrackToQueue,
    voteOnQueueTrack,
    requestSkip,
    reorderQueueItems,
    removeTrackFromQueue,
    
    // DJ mode
    becomeDJ,
    stepDownDJ,
    
    // Utilities
    canPerformAction,
    getVotingSummary,
    getQueueStats,
    
    // Computed values
    queueIsFull: queue.length >= maxQueueSize,
    canAddTracks: canPerformAction('add'),
    canVote: canPerformAction('vote'),
    canSkip: canPerformAction('skip'),
    canReorder: canPerformAction('reorder'),
    isDJ: collaborativeState.currentDJ?.id === 'current-user', // Would need actual user ID
    hasActiveUsers: collaborativeState.activeUsers.length > 0,
  }
}