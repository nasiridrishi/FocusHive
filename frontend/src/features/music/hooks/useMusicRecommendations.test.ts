import { renderHook, act, waitFor } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useMusicRecommendations } from './useMusicRecommendations'
import * as musicApiModule from '../services/musicApi'
import * as musicContextModule from '../context'

// Mock the music API
vi.mock('../services/musicApi', () => ({
  musicApi: {
    getSessionRecommendations: vi.fn(),
    getPersonalizedRecommendations: vi.fn(),
    getSimilarTracks: vi.fn(),
  },
}))

// Mock the music context
const mockMusicState = {
  currentTrack: {
    id: 'track-1',
    title: 'Test Song',
    artist: 'Test Artist',
    duration: 180,
    explicit: false,
  },
  queue: [
    { id: 'queue-1', title: 'Queue Song 1', artist: 'Artist 1', duration: 200, explicit: false },
    { id: 'queue-2', title: 'Queue Song 2', artist: 'Artist 2', duration: 220, explicit: false },
  ],
  currentMood: {
    mood: 'focused',
    energy: 60,
    taskType: 'focus' as const,
    customTags: ['work', 'coding'],
  },
  playbackState: {
    isPlaying: false,
    isPaused: false,
    isBuffering: false,
    currentTime: 0,
    duration: 0,
    volume: 1,
    isMuted: false,
    playbackRate: 1,
  },
  playlists: [],
  isLoading: false,
  error: null,
  recommendations: [],
}

vi.mock('../context', () => ({
  useMusic: () => ({
    state: mockMusicState,
  }),
}))

const mockTracks = [
  {
    id: 'rec-1',
    title: 'Recommended Song 1',
    artist: 'Rec Artist 1',
    duration: 190,
    explicit: false,
  },
  {
    id: 'rec-2',
    title: 'Recommended Song 2',
    artist: 'Rec Artist 2',
    duration: 210,
    explicit: false,
  },
  {
    id: 'rec-3',
    title: 'Recommended Song 3',
    artist: 'Rec Artist 3',
    duration: 170,
    explicit: true,
  },
]

describe('useMusicRecommendations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  describe('Session Recommendations', () => {
    it('fetches session recommendations successfully', async () => {
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        const recommendations = await result.current.getSessionRecommendations({
          hiveId: 'hive-1',
          mood: 'focused',
          energy: 60,
          taskType: 'focus',
        })

        expect(recommendations).toEqual(mockTracks)
      })

      await waitFor(() => {
        expect(result.current.recommendations).toEqual(mockTracks)
        expect(result.current.source).toBe('session')
        expect(result.current.isLoading).toBe(false)
        expect(result.current.error).toBeNull()
        expect(result.current.lastUpdated).toBeInstanceOf(Date)
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: 'hive-1',
        mood: 'focused',
        energy: 60,
        taskType: 'focus',
        duration: undefined,
        previousTracks: ['queue-1', 'queue-2'],
      })
    })

    it('handles session recommendations error', async () => {
      const errorMessage = 'Failed to get recommendations'
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockRejectedValue(new Error(errorMessage))

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        try {
          await result.current.getSessionRecommendations({ mood: 'focused' })
        } catch (error) {
          expect(error).toBeInstanceOf(Error)
        }
      })

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
        expect(result.current.error).toBe(errorMessage)
        expect(result.current.recommendations).toEqual([])
      })
    })
  })

  describe('Personalized Recommendations', () => {
    it('fetches personalized recommendations', async () => {
      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        const recommendations = await result.current.getPersonalizedRecommendations(10)
        expect(recommendations).toEqual(mockTracks)
      })

      await waitFor(() => {
        expect(result.current.source).toBe('personalized')
        expect(result.current.recommendations).toEqual(mockTracks)
      })

      expect(musicApiModule.musicApi.getPersonalizedRecommendations).toHaveBeenCalledWith(10)
    })
  })

  describe('Similar Tracks', () => {
    it('fetches similar tracks', async () => {
      vi.mocked(musicApiModule.musicApi.getSimilarTracks).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        const recommendations = await result.current.getSimilarTracks('track-1', 5)
        expect(recommendations).toEqual(mockTracks)
      })

      await waitFor(() => {
        expect(result.current.source).toBe('similar')
        expect(result.current.recommendations).toEqual(mockTracks)
      })

      expect(musicApiModule.musicApi.getSimilarTracks).toHaveBeenCalledWith('track-1', 5)
    })
  })

  describe('Contextual Recommendations', () => {
    it('gets mood-based recommendations when mood is set', async () => {
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getContextualRecommendations()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        mood: 'focused',
        energy: 60,
        taskType: 'focus',
      })
    })

    it('gets similar tracks when current track is playing but no mood', async () => {
      // Mock context without mood
      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        state: {
          ...mockMusicState,
          currentMood: null,
        },
      })

      vi.mocked(musicApiModule.musicApi.getSimilarTracks).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getContextualRecommendations()
      })

      expect(musicApiModule.musicApi.getSimilarTracks).toHaveBeenCalledWith('track-1')
    })

    it('gets personalized recommendations when no mood or current track', async () => {
      // Mock context without mood or current track
      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        state: {
          ...mockMusicState,
          currentMood: null,
          currentTrack: null,
        },
      })

      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getContextualRecommendations()
      })

      expect(musicApiModule.musicApi.getPersonalizedRecommendations).toHaveBeenCalled()
    })
  })

  describe('Preset Recommendations', () => {
    it('gets energy boost recommendations', async () => {
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getEnergyBoostRecommendations()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 80,
        taskType: 'exercise',
      })
    })

    it('gets focus recommendations', async () => {
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getFocusRecommendations()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 30,
        taskType: 'focus',
      })
    })

    it('gets break recommendations', async () => {
      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getBreakRecommendations()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 60,
        taskType: 'break',
      })
    })
  })

  describe('Smart Suggestions', () => {
    it('provides morning energy recommendations', async () => {
      // Mock morning time (8 AM)
      vi.setSystemTime(new Date('2024-01-01T08:00:00'))

      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getSmartSuggestions()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 80,
        taskType: 'exercise',
      })
    })

    it('provides afternoon focus recommendations', async () => {
      // Mock afternoon time (2 PM)
      vi.setSystemTime(new Date('2024-01-01T14:00:00'))

      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getSmartSuggestions()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 30,
        taskType: 'focus',
      })
    })

    it('provides evening relax recommendations', async () => {
      // Mock evening time (7 PM)
      vi.setSystemTime(new Date('2024-01-01T19:00:00'))

      vi.mocked(musicApiModule.musicApi.getSessionRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getSmartSuggestions()
      })

      expect(musicApiModule.musicApi.getSessionRecommendations).toHaveBeenCalledWith({
        hiveId: undefined,
        energy: 40,
        taskType: 'relax',
      })
    })
  })

  describe('Filter Recommendations', () => {
    it('filters by minimum duration', () => {
      const { result } = renderHook(() => useMusicRecommendations())

      // Set recommendations first
      act(() => {
        result.current.recommendations = mockTracks
      })

      const filtered = result.current.filterRecommendations({
        minDuration: 200,
      })

      // Should filter out tracks with duration < 200
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('rec-2')
    })

    it('filters by maximum duration', () => {
      const { result } = renderHook(() => useMusicRecommendations())

      act(() => {
        result.current.recommendations = mockTracks
      })

      const filtered = result.current.filterRecommendations({
        maxDuration: 180,
      })

      // Should filter out tracks with duration > 180
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('rec-3')
    })

    it('filters by explicit content', () => {
      const { result } = renderHook(() => useMusicRecommendations())

      act(() => {
        result.current.recommendations = mockTracks
      })

      const filtered = result.current.filterRecommendations({
        explicit: false,
      })

      // Should filter out explicit tracks
      expect(filtered).toHaveLength(2)
      expect(filtered.every(track => !track.explicit)).toBe(true)
    })

    it('filters by excluded IDs', () => {
      const { result } = renderHook(() => useMusicRecommendations())

      act(() => {
        result.current.recommendations = mockTracks
      })

      const filtered = result.current.filterRecommendations({
        excludeIds: ['rec-1', 'rec-3'],
      })

      // Should exclude specified tracks
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('rec-2')
    })
  })

  describe('Auto-refresh', () => {
    it('auto-refreshes recommendations when enabled', async () => {
      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => 
        useMusicRecommendations({ 
          autoRefresh: true, 
          refreshInterval: 1000 
        })
      )

      // Get initial recommendations
      await act(async () => {
        await result.current.getPersonalizedRecommendations()
      })

      expect(musicApiModule.musicApi.getPersonalizedRecommendations).toHaveBeenCalledTimes(1)

      // Fast forward past refresh interval
      act(() => {
        vi.advanceTimersByTime(1001)
      })

      await waitFor(() => {
        expect(musicApiModule.musicApi.getPersonalizedRecommendations).toHaveBeenCalledTimes(2)
      })
    })
  })

  describe('Computed Values', () => {
    it('returns correct hasRecommendations value', async () => {
      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      expect(result.current.hasRecommendations).toBe(false)

      await act(async () => {
        await result.current.getPersonalizedRecommendations()
      })

      await waitFor(() => {
        expect(result.current.hasRecommendations).toBe(true)
      })
    })

    it('returns correct isStale value', async () => {
      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => 
        useMusicRecommendations({ refreshInterval: 1000 })
      )

      await act(async () => {
        await result.current.getPersonalizedRecommendations()
      })

      await waitFor(() => {
        expect(result.current.isStale).toBe(false)
      })

      // Fast forward past refresh interval
      act(() => {
        vi.advanceTimersByTime(1001)
      })

      expect(result.current.isStale).toBe(true)
    })
  })

  describe('Clear Recommendations', () => {
    it('clears recommendations and resets state', async () => {
      vi.mocked(musicApiModule.musicApi.getPersonalizedRecommendations).mockResolvedValue(mockTracks)

      const { result } = renderHook(() => useMusicRecommendations())

      await act(async () => {
        await result.current.getPersonalizedRecommendations()
      })

      await waitFor(() => {
        expect(result.current.recommendations).toEqual(mockTracks)
      })

      act(() => {
        result.current.clearRecommendations()
      })

      expect(result.current.recommendations).toEqual([])
      expect(result.current.source).toBeNull()
      expect(result.current.lastUpdated).toBeNull()
      expect(result.current.error).toBeNull()
    })
  })
})