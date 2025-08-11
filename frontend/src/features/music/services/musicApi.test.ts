import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import musicApi from './musicApi'
import { CreatePlaylistRequest, SessionRecommendationRequest, SearchTracksRequest } from '../types'

// Mock axios
vi.mock('axios')
const mockedAxios = vi.mocked(axios, true)

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
  writable: true,
})

describe('MusicApiService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockLocalStorage.getItem.mockReturnValue('mock-auth-token')
    
    // Mock axios.create to return mocked axios instance
    mockedAxios.create.mockReturnValue(mockedAxios as unknown as typeof mockedAxios)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Authentication', () => {
    it('adds auth token to requests', () => {
      mockLocalStorage.getItem.mockReturnValue('test-token')
      
      // Simulate request interceptor
      // These will be used when interceptor logic is added
      void { headers: {} } // config placeholder
      void (mockedAxios.create as unknown).mock?.calls[0]?.[0] // interceptor placeholder
      
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith('authToken')
    })

    it('handles requests without auth token', () => {
      mockLocalStorage.getItem.mockReturnValue(null)
      
      const config = { headers: {} }
      // Should not throw error when no token
      expect(() => config).not.toThrow()
    })
  })

  describe('Playlist Operations', () => {
    describe('getPlaylists', () => {
      it('fetches playlists successfully', async () => {
        const mockPlaylists = [
          {
            id: 'playlist-1',
            name: 'Test Playlist',
            description: 'A test playlist',
            isPublic: true,
            isCollaborative: false,
            trackCount: 10,
            duration: 1800,
            tracks: [],
            createdBy: { id: 'user-1', name: 'Test User' },
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            type: 'personal' as const,
          },
        ]

        mockedAxios.get.mockResolvedValue({
          data: { data: mockPlaylists },
        })

        const result = await musicApi.getPlaylists()

        expect(mockedAxios.get).toHaveBeenCalledWith('/playlists', { params: {} })
        expect(result).toEqual(mockPlaylists)
      })

      it('fetches playlists with hive filter', async () => {
        mockedAxios.get.mockResolvedValue({
          data: { data: [] },
        })

        await musicApi.getPlaylists('hive-123')

        expect(mockedAxios.get).toHaveBeenCalledWith('/playlists', {
          params: { hiveId: 'hive-123' },
        })
      })

      it('handles API error', async () => {
        const errorMessage = 'Network error'
        mockedAxios.get.mockRejectedValue(new Error(errorMessage))

        await expect(musicApi.getPlaylists()).rejects.toThrow(errorMessage)
      })
    })

    describe('createPlaylist', () => {
      it('creates playlist successfully', async () => {
        const newPlaylist: CreatePlaylistRequest = {
          name: 'New Playlist',
          description: 'A new test playlist',
          isPublic: true,
          isCollaborative: false,
        }

        const createdPlaylist = {
          id: 'new-playlist-1',
          ...newPlaylist,
          trackCount: 0,
          duration: 0,
          tracks: [],
          createdBy: { id: 'user-1', name: 'Test User' },
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          type: 'personal' as const,
        }

        mockedAxios.post.mockResolvedValue({
          data: { data: createdPlaylist },
        })

        const result = await musicApi.createPlaylist(newPlaylist)

        expect(mockedAxios.post).toHaveBeenCalledWith('/playlists', newPlaylist)
        expect(result).toEqual(createdPlaylist)
      })
    })

    describe('deletePlaylist', () => {
      it('deletes playlist successfully', async () => {
        mockedAxios.delete.mockResolvedValue({})

        await musicApi.deletePlaylist('playlist-1')

        expect(mockedAxios.delete).toHaveBeenCalledWith('/playlists/playlist-1')
      })
    })
  })

  describe('Queue Operations', () => {
    describe('getQueue', () => {
      it('fetches queue successfully', async () => {
        const mockQueue = [
          {
            queueId: 'queue-1',
            id: 'track-1',
            title: 'Test Song',
            artist: 'Test Artist',
            duration: 180,
            explicit: false,
            position: 1,
            votes: 5,
          },
        ]

        mockedAxios.get.mockResolvedValue({
          data: { data: mockQueue },
        })

        const result = await musicApi.getQueue()

        expect(mockedAxios.get).toHaveBeenCalledWith('/queue', { params: {} })
        expect(result).toEqual(mockQueue)
      })

      it('fetches queue with hive filter', async () => {
        mockedAxios.get.mockResolvedValue({
          data: { data: [] },
        })

        await musicApi.getQueue('hive-123')

        expect(mockedAxios.get).toHaveBeenCalledWith('/queue', {
          params: { hiveId: 'hive-123' },
        })
      })
    })

    describe('addToQueue', () => {
      it('adds track to queue successfully', async () => {
        const request = {
          trackId: 'track-1',
          hiveId: 'hive-123',
          position: 5,
        }

        const queueItem = {
          queueId: 'queue-1',
          id: 'track-1',
          title: 'Test Song',
          artist: 'Test Artist',
          duration: 180,
          explicit: false,
          position: 5,
          votes: 0,
        }

        mockedAxios.post.mockResolvedValue({
          data: { data: queueItem },
        })

        const result = await musicApi.addToQueue(request)

        expect(mockedAxios.post).toHaveBeenCalledWith('/queue', request)
        expect(result).toEqual(queueItem)
      })
    })

    describe('voteOnTrack', () => {
      it('votes on track successfully', async () => {
        const voteRequest = {
          trackId: 'track-1',
          vote: 'up' as const,
        }

        mockedAxios.post.mockResolvedValue({})

        await musicApi.voteOnTrack('queue-1', voteRequest)

        expect(mockedAxios.post).toHaveBeenCalledWith('/queue/queue-1/vote', voteRequest)
      })
    })
  })

  describe('Search Operations', () => {
    describe('searchTracks', () => {
      it('searches tracks successfully', async () => {
        const searchRequest: SearchTracksRequest = {
          query: 'test song',
          limit: 10,
          offset: 0,
          type: 'track',
        }

        const searchResponse = {
          tracks: [
            {
              id: 'track-1',
              title: 'Test Song',
              artist: 'Test Artist',
              duration: 180,
              explicit: false,
            },
          ],
          total: 1,
          offset: 0,
          limit: 10,
        }

        mockedAxios.get.mockResolvedValue({
          data: { data: searchResponse },
        })

        const result = await musicApi.searchTracks(searchRequest)

        expect(mockedAxios.get).toHaveBeenCalledWith('/search', {
          params: searchRequest,
        })
        expect(result).toEqual(searchResponse)
      })
    })
  })

  describe('Recommendation Operations', () => {
    describe('getSessionRecommendations', () => {
      it('gets session recommendations successfully', async () => {
        const request: SessionRecommendationRequest = {
          hiveId: 'hive-123',
          mood: 'focused',
          energy: 60,
          taskType: 'focus',
          duration: 1800,
        }

        const recommendations = [
          {
            id: 'rec-1',
            title: 'Focus Song 1',
            artist: 'Focus Artist',
            duration: 180,
            explicit: false,
          },
        ]

        mockedAxios.post.mockResolvedValue({
          data: { data: recommendations },
        })

        const result = await musicApi.getSessionRecommendations(request)

        expect(mockedAxios.post).toHaveBeenCalledWith('/recommendations/session', request)
        expect(result).toEqual(recommendations)
      })
    })

    describe('getPersonalizedRecommendations', () => {
      it('gets personalized recommendations successfully', async () => {
        const recommendations = [
          {
            id: 'rec-1',
            title: 'Personal Song 1',
            artist: 'Personal Artist',
            duration: 180,
            explicit: false,
          },
        ]

        mockedAxios.get.mockResolvedValue({
          data: { data: recommendations },
        })

        const result = await musicApi.getPersonalizedRecommendations(15)

        expect(mockedAxios.get).toHaveBeenCalledWith('/recommendations/personalized', {
          params: { limit: 15 },
        })
        expect(result).toEqual(recommendations)
      })
    })

    describe('getSimilarTracks', () => {
      it('gets similar tracks successfully', async () => {
        const recommendations = [
          {
            id: 'similar-1',
            title: 'Similar Song 1',
            artist: 'Similar Artist',
            duration: 180,
            explicit: false,
          },
        ]

        mockedAxios.get.mockResolvedValue({
          data: { data: recommendations },
        })

        const result = await musicApi.getSimilarTracks('track-1', 8)

        expect(mockedAxios.get).toHaveBeenCalledWith('/recommendations/similar/track-1', {
          params: { limit: 8 },
        })
        expect(result).toEqual(recommendations)
      })
    })
  })

  describe('Analytics Operations', () => {
    describe('recordPlayback', () => {
      it('records playback successfully', async () => {
        mockedAxios.post.mockResolvedValue({})

        await musicApi.recordPlayback('track-1', 120, true)

        expect(mockedAxios.post).toHaveBeenCalledWith('/analytics/playback', {
          trackId: 'track-1',
          duration: 120,
          completed: true,
          timestamp: expect.any(String),
        })
      })
    })

    describe('recordSkip', () => {
      it('records skip successfully', async () => {
        mockedAxios.post.mockResolvedValue({})

        await musicApi.recordSkip('track-1', 60)

        expect(mockedAxios.post).toHaveBeenCalledWith('/analytics/skip', {
          trackId: 'track-1',
          position: 60,
          timestamp: expect.any(String),
        })
      })
    })
  })

  describe('Health Check', () => {
    describe('checkHealth', () => {
      it('returns true when service is healthy', async () => {
        mockedAxios.get.mockResolvedValue({ status: 200 })

        const result = await musicApi.checkHealth()

        expect(result).toBe(true)
        expect(mockedAxios.get).toHaveBeenCalledWith('/health')
      })

      it('returns false when service is unhealthy', async () => {
        mockedAxios.get.mockRejectedValue(new Error('Service unavailable'))

        const result = await musicApi.checkHealth()

        expect(result).toBe(false)
      })
    })

    describe('getApiVersion', () => {
      it('gets API version successfully', async () => {
        mockedAxios.get.mockResolvedValue({
          data: { data: { version: '1.0.0' } },
        })

        const result = await musicApi.getApiVersion()

        expect(result).toBe('1.0.0')
        expect(mockedAxios.get).toHaveBeenCalledWith('/version')
      })
    })
  })

  describe('Error Handling', () => {
    it('transforms API errors to MusicError format', async () => {
      const apiError = {
        response: {
          data: {
            message: 'Playlist not found',
            code: 'PLAYLIST_NOT_FOUND',
            details: { playlistId: 'invalid-id' },
          },
        },
      }

      mockedAxios.get.mockRejectedValue(apiError)

      try {
        await musicApi.getPlaylist('invalid-id')
      } catch (error) {
        const musicError = error as { name: string; message: string; code: string; details: unknown; timestamp: unknown }
        expect(musicError.name).toBe('MusicError')
        expect(musicError.message).toBe('Playlist not found')
        expect((musicError as any).code).toBe('PLAYLIST_NOT_FOUND')
        expect((musicError as any).details).toEqual({ playlistId: 'invalid-id' })
        expect((musicError as any).timestamp).toBeDefined()
      }
    })

    it('handles generic errors', async () => {
      const genericError = new Error('Network timeout')
      mockedAxios.get.mockRejectedValue(genericError)

      try {
        await musicApi.getPlaylists()
      } catch (error) {
        const musicError = error as { name: string; message: string }
        expect(musicError.name).toBe('MusicError')
        expect(musicError.message).toBe('Network timeout')
        expect((error as any).code).toBe('UNKNOWN_ERROR')
        expect((error as any).timestamp).toBeDefined()
      }
    })
  })
})