import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'

// Mock axios before any imports
vi.mock('axios', () => {
  const mockInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn(),
      },
      response: {
        use: vi.fn(),
      },
    },
  }
  
  return {
    default: {
      create: vi.fn(() => mockInstance),
    },
  }
})

// Import after mocking
import axios from 'axios'
import { default as musicApi } from './musicApi'
import type { CreatePlaylistRequest, SearchTracksRequest } from '../types/music'

// Get the mocked axios instance
const mockedAxios = vi.mocked(axios)

// Define the mock axios instance type
interface MockAxiosInstance {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
  put: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
  patch: ReturnType<typeof vi.fn>
  interceptors: {
    request: { use: ReturnType<typeof vi.fn> }
    response: { use: ReturnType<typeof vi.fn> }
  }
}

const mockAxiosInstance = mockedAxios.create() as unknown as MockAxiosInstance

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
    mockLocalStorage.getItem.mockReturnValue('mock-token')
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('Playlist Management', () => {
    it('should get user playlists', async () => {
      const mockPlaylists = [
        {
          id: '1',
          name: 'Test Playlist',
          description: 'A test playlist',
          tracks: [],
          isPublic: true,
          ownerId: 'user1',
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      ]

      mockAxiosInstance.get.mockResolvedValue({
        data: { data: mockPlaylists, success: true },
        status: 200,
      })

      const result = await musicApi.getUserPlaylists()
      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/playlists/user', { params: {} })
      expect(result).toEqual(mockPlaylists)
    })

    it('should handle pagination in get user playlists', async () => {
      mockAxiosInstance.get.mockResolvedValue({
        data: {
          data: [],
          success: true,
          pagination: { page: 2, limit: 10, total: 0, totalPages: 1 },
        },
        status: 200,
      })

      await musicApi.getUserPlaylists('user123')
      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/playlists/user', {
        params: { userId: 'user123' },
      })
    })

    it('should create a playlist', async () => {
      const createRequest: CreatePlaylistRequest = {
        name: 'New Playlist',
        description: 'A new playlist',
        isPublic: true,
        isCollaborative: false,
        trackIds: ['track1', 'track2'],
      }

      const mockPlaylist = {
        id: '1',
        ...createRequest,
        tracks: [],
        ownerId: 'user1',
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      mockAxiosInstance.post.mockResolvedValue({
        data: { data: mockPlaylist, success: true },
        status: 201,
      })

      const result = await musicApi.createPlaylist(createRequest)
      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/playlists', createRequest)
      expect(result).toEqual(mockPlaylist)
    })
  })

  describe('Track Search', () => {
    it('should search tracks', async () => {
      const searchRequest: SearchTracksRequest = {
        query: 'test song',
        limit: 20,
      }

      const mockResponse = {
        tracks: [
          {
            id: 'track1',
            title: 'Test Song',
            artist: 'Test Artist',
            duration: 180,
            url: 'http://example.com/track1',
            thumbnail: 'http://example.com/thumb1',
          },
        ],
        total: 1,
        page: 1,
        limit: 20,
      }

      mockAxiosInstance.get.mockResolvedValue({
        data: { data: mockResponse, success: true },
        status: 200,
      })

      const result = await musicApi.searchTracks(searchRequest)
      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/search', {
        params: searchRequest,
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('Error Handling', () => {
    it('should handle API errors', async () => {
      const errorResponse = {
        response: {
          status: 400,
          data: { error: 'Bad Request', message: 'Invalid parameters' },
        },
      }

      mockAxiosInstance.get.mockRejectedValue(errorResponse)

      await expect(musicApi.getUserPlaylists()).rejects.toThrow()
    })

    it('should handle network errors', async () => {
      // Mock a network error - the interceptor will transform it to a MusicError
      const _networkError = new Error('Network Error')
      // The response interceptor creates a MusicError object
      const expectedMusicError = {
        name: 'MusicError',
        message: 'Network Error',
        code: 'UNKNOWN_ERROR',
        details: undefined,
        timestamp: expect.any(String),
      }
      mockAxiosInstance.get.mockRejectedValue(expectedMusicError)

      await expect(musicApi.getUserPlaylists()).rejects.toMatchObject({
        name: 'MusicError',
        message: 'Network Error',
        code: 'UNKNOWN_ERROR',
      })
    })
  })
})