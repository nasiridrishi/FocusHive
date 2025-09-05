import { useState, useCallback, useEffect } from 'react'
import { Track, SessionRecommendationRequest, MoodState, TaskType } from '../types'
import { musicApi } from '../services'
import { useMusic } from '../context'

interface UseRecommendationsOptions {
  autoRefresh?: boolean
  refreshInterval?: number // in milliseconds
  hiveId?: string
}

interface RecommendationState {
  recommendations: Track[]
  isLoading: boolean
  error: string | null
  lastUpdated: Date | null
  source: 'session' | 'personalized' | 'similar' | null
}

/**
 * Custom hook for music recommendations
 * Provides various recommendation strategies and caching
 */
export const useMusicRecommendations = (options: UseRecommendationsOptions = {}) => {
  const musicContext = useMusic()
  const { state } = musicContext
  const { queue, currentMood, currentTrack } = state
  const [recommendationState, setRecommendationState] = useState<RecommendationState>({
    recommendations: [],
    isLoading: false,
    error: null,
    lastUpdated: null,
    source: null,
  })

  const { autoRefresh = false, refreshInterval = 5 * 60 * 1000, hiveId } = options

  // Get session-based recommendations
  const getSessionRecommendations = useCallback(async (request: SessionRecommendationRequest) => {
    try {
      setRecommendationState(prev => ({ ...prev, isLoading: true, error: null }))
      
      const recommendations = await musicApi.getSessionRecommendations({
        hiveId: hiveId || request.hiveId,
        mood: request.mood || currentMood?.mood,
        energy: request.energy || currentMood?.energy,
        taskType: request.taskType,
        duration: request.duration,
        previousTracks: request.previousTracks || queue.map(item => item.id),
      })

      setRecommendationState({
        recommendations,
        isLoading: false,
        error: null,
        lastUpdated: new Date(),
        source: 'session',
      })

      return recommendations
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to get recommendations'
      setRecommendationState(prev => ({
        ...prev,
        isLoading: false,
        error: errorMessage,
      }))
      throw error
    }
  }, [hiveId, currentMood, queue])

  // Get personalized recommendations
  const getPersonalizedRecommendations = useCallback(async (limit = 20) => {
    try {
      setRecommendationState(prev => ({ ...prev, isLoading: true, error: null }))
      
      const recommendations = await musicApi.getPersonalizedRecommendations(limit)

      setRecommendationState({
        recommendations,
        isLoading: false,
        error: null,
        lastUpdated: new Date(),
        source: 'personalized',
      })

      return recommendations
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to get personalized recommendations'
      setRecommendationState(prev => ({
        ...prev,
        isLoading: false,
        error: errorMessage,
      }))
      throw error
    }
  }, [])

  // Get similar tracks
  const getSimilarTracks = useCallback(async (trackId: string, limit = 10) => {
    try {
      setRecommendationState(prev => ({ ...prev, isLoading: true, error: null }))
      
      const recommendations = await musicApi.getSimilarTracks(trackId, limit)

      setRecommendationState({
        recommendations,
        isLoading: false,
        error: null,
        lastUpdated: new Date(),
        source: 'similar',
      })

      return recommendations
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to get similar tracks'
      setRecommendationState(prev => ({
        ...prev,
        isLoading: false,
        error: errorMessage,
      }))
      throw error
    }
  }, [])

  // Get mood-based recommendations
  const getMoodRecommendations = useCallback(async (mood: MoodState) => {
    return getSessionRecommendations({
      hiveId,
      mood: mood.mood,
      energy: mood.energy,
      taskType: mood.taskType,
    })
  }, [getSessionRecommendations, hiveId])

  // Get recommendations for specific task type
  const getTaskRecommendations = useCallback(async (taskType: TaskType, energy?: number) => {
    return getSessionRecommendations({
      hiveId,
      taskType,
      energy: energy || currentMood?.energy || 50,
    })
  }, [getSessionRecommendations, hiveId, currentMood])

  // Get recommendations based on current context
  const getContextualRecommendations = useCallback(async () => {
    if (currentMood) {
      return getMoodRecommendations(currentMood)
    } else if (currentTrack) {
      return getSimilarTracks(currentTrack.id)
    } else {
      return getPersonalizedRecommendations()
    }
  }, [currentMood, currentTrack, getMoodRecommendations, getSimilarTracks, getPersonalizedRecommendations])

  // Refresh current recommendations
  const refreshRecommendations = useCallback(async () => {
    const { source } = recommendationState
    
    switch (source) {
      case 'session':
        return getContextualRecommendations()
      case 'personalized':
        return getPersonalizedRecommendations()
      case 'similar':
        if (currentTrack) {
          return getSimilarTracks(currentTrack.id)
        }
        break
      default:
        return getContextualRecommendations()
    }
  }, [recommendationState.source, getContextualRecommendations, getPersonalizedRecommendations, getSimilarTracks, currentTrack])

  // Clear recommendations
  const clearRecommendations = useCallback(() => {
    setRecommendationState({
      recommendations: [],
      isLoading: false,
      error: null,
      lastUpdated: null,
      source: null,
    })
  }, [])

  // Filter recommendations by criteria
  const filterRecommendations = useCallback((criteria: {
    minDuration?: number
    maxDuration?: number
    explicit?: boolean
    genres?: string[]
    excludeIds?: string[]
  }) => {
    const { recommendations } = recommendationState
    
    return recommendations.filter(track => {
      if (criteria.minDuration && track.duration < criteria.minDuration) return false
      if (criteria.maxDuration && track.duration > criteria.maxDuration) return false
      if (criteria.explicit !== undefined && track.explicit !== criteria.explicit) return false
      if (criteria.excludeIds && criteria.excludeIds.includes(track.id)) return false
      // Note: Genre filtering would require additional track metadata
      return true
    })
  }, [recommendationState.recommendations])

  // Get recommendations for energy boost
  const getEnergyBoostRecommendations = useCallback(async () => {
    return getSessionRecommendations({
      hiveId,
      energy: 80,
      taskType: 'exercise',
    })
  }, [getSessionRecommendations, hiveId])

  // Get focus recommendations
  const getFocusRecommendations = useCallback(async () => {
    return getSessionRecommendations({
      hiveId,
      energy: 30,
      taskType: 'focus',
    })
  }, [getSessionRecommendations, hiveId])

  // Get break recommendations
  const getBreakRecommendations = useCallback(async () => {
    return getSessionRecommendations({
      hiveId,
      energy: 60,
      taskType: 'break',
    })
  }, [getSessionRecommendations, hiveId])

  // Auto-refresh effect
  useEffect(() => {
    if (!autoRefresh || !recommendationState.lastUpdated) return

    const interval = setInterval(() => {
      refreshRecommendations()
    }, refreshInterval)

    return () => clearInterval(interval)
  }, [autoRefresh, refreshInterval, recommendationState.lastUpdated, refreshRecommendations])

  // Smart recommendation suggestions based on context
  const getSmartSuggestions = useCallback(async () => {
    const now = new Date()
    const hour = now.getHours()
    
    // Morning: energetic tracks
    if (hour >= 6 && hour < 12) {
      return getEnergyBoostRecommendations()
    }
    // Afternoon: focus tracks
    else if (hour >= 12 && hour < 17) {
      return getFocusRecommendations()
    }
    // Evening: relaxing tracks
    else if (hour >= 17 && hour < 22) {
      return getSessionRecommendations({
        hiveId,
        energy: 40,
        taskType: 'relax',
      })
    }
    // Night: calm tracks
    else {
      return getSessionRecommendations({
        hiveId,
        energy: 20,
        taskType: 'relax',
      })
    }
  }, [getEnergyBoostRecommendations, getFocusRecommendations, getSessionRecommendations, hiveId])

  return {
    // State
    ...recommendationState,
    
    // Core functions
    getSessionRecommendations,
    getPersonalizedRecommendations,
    getSimilarTracks,
    
    // Context-aware functions
    getMoodRecommendations,
    getTaskRecommendations,
    getContextualRecommendations,
    
    // Utility functions
    refreshRecommendations,
    clearRecommendations,
    filterRecommendations,
    
    // Preset recommendations
    getEnergyBoostRecommendations,
    getFocusRecommendations,
    getBreakRecommendations,
    getSmartSuggestions,
    
    // Helper computed values
    hasRecommendations: recommendationState.recommendations.length > 0,
    isStale: recommendationState.lastUpdated 
      ? Date.now() - recommendationState.lastUpdated.getTime() > refreshInterval 
      : false,
  }
}