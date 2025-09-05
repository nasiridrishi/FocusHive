import axios, { AxiosInstance, AxiosResponse } from 'axios'
import {
  Track,
  Playlist,
  ApiResponse,
  PaginatedResponse,
  SearchTracksRequest,
  SearchTracksResponse,
  CreatePlaylistRequest,
  UpdatePlaylistRequest,
  AddToQueueRequest,
  VoteRequest,
  SessionRecommendationRequest,
  MusicError,
  QueueItem
} from '../types/music'

class MusicApiService {
  private api: AxiosInstance

  constructor() {
    // Music service may run on a different port, but use env variable if available
    const musicApiBaseUrl = import.meta.env.VITE_musicApiBaseUrl || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    this.api = axios.create({
      baseURL: `${musicApiBaseUrl}/api/music`,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor to add auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('authToken')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        const musicError: MusicError = {
          name: 'MusicError',
          message: error.response?.data?.message || error.message || 'Unknown error',
          code: error.response?.data?.code || 'UNKNOWN_ERROR',
          details: error.response?.data,
          timestamp: new Date().toISOString(),
        }
        return Promise.reject(musicError)
      }
    )
  }

  // Playlist endpoints
  async getPlaylists(hiveId?: string): Promise<Playlist[]> {
    const params = hiveId ? { hiveId } : {}
    const response: AxiosResponse<ApiResponse<Playlist[]>> = await this.api.get('/playlists', { params })
    return response.data.data
  }

  // Alias for getUserPlaylists to support existing test code
  async getUserPlaylists(userId?: string): Promise<Playlist[]> {
    const params = userId ? { userId } : {}
    const response: AxiosResponse<ApiResponse<Playlist[]>> = await this.api.get('/playlists/user', { params })
    return response.data.data
  }

  async getPlaylist(id: string): Promise<Playlist> {
    const response: AxiosResponse<ApiResponse<Playlist>> = await this.api.get(`/playlists/${id}`)
    return response.data.data
  }

  async createPlaylist(request: CreatePlaylistRequest): Promise<Playlist> {
    const response: AxiosResponse<ApiResponse<Playlist>> = await this.api.post('/playlists', request)
    return response.data.data
  }

  async updatePlaylist(id: string, request: UpdatePlaylistRequest): Promise<Playlist> {
    const response: AxiosResponse<ApiResponse<Playlist>> = await this.api.put(`/playlists/${id}`, request)
    return response.data.data
  }

  async deletePlaylist(id: string): Promise<void> {
    await this.api.delete(`/playlists/${id}`)
  }

  async addTracksToPlaylist(playlistId: string, trackIds: string[]): Promise<void> {
    await this.api.post(`/playlists/${playlistId}/tracks`, { trackIds })
  }

  async removeTrackFromPlaylist(playlistId: string, trackId: string): Promise<void> {
    await this.api.delete(`/playlists/${playlistId}/tracks/${trackId}`)
  }

  async getPlaylistTracks(playlistId: string, page = 1, limit = 50): Promise<PaginatedResponse<Track>> {
    const response: AxiosResponse<PaginatedResponse<Track>> = await this.api.get(
      `/playlists/${playlistId}/tracks`,
      { params: { page, limit } }
    )
    return response.data
  }

  // Queue endpoints
  async getQueue(hiveId?: string): Promise<QueueItem[]> {
    const params = hiveId ? { hiveId } : {}
    const response: AxiosResponse<ApiResponse<QueueItem[]>> = await this.api.get('/queue', { params })
    return response.data.data
  }

  async addToQueue(request: AddToQueueRequest): Promise<QueueItem> {
    const response: AxiosResponse<ApiResponse<QueueItem>> = await this.api.post('/queue', request)
    return response.data.data
  }

  async removeFromQueue(queueId: string): Promise<void> {
    await this.api.delete(`/queue/${queueId}`)
  }

  async reorderQueue(fromPosition: number, toPosition: number, hiveId?: string): Promise<QueueItem[]> {
    const request = { fromPosition, toPosition, hiveId }
    const response: AxiosResponse<ApiResponse<QueueItem[]>> = await this.api.put('/queue/reorder', request)
    return response.data.data
  }

  async clearQueue(hiveId?: string): Promise<void> {
    const params = hiveId ? { hiveId } : {}
    await this.api.delete('/queue/clear', { params })
  }

  async voteOnTrack(queueId: string, vote: VoteRequest): Promise<void> {
    await this.api.post(`/queue/${queueId}/vote`, vote)
  }

  // Search endpoints
  async searchTracks(request: SearchTracksRequest): Promise<SearchTracksResponse> {
    const response: AxiosResponse<ApiResponse<SearchTracksResponse>> = await this.api.get('/search', {
      params: request
    })
    return response.data.data
  }

  async getTrack(id: string): Promise<Track> {
    const response: AxiosResponse<ApiResponse<Track>> = await this.api.get(`/tracks/${id}`)
    return response.data.data
  }

  // Recommendation endpoints
  async getSessionRecommendations(request: SessionRecommendationRequest): Promise<Track[]> {
    const response: AxiosResponse<ApiResponse<Track[]>> = await this.api.post('/recommendations/session', request)
    return response.data.data
  }

  async getPersonalizedRecommendations(limit = 20): Promise<Track[]> {
    const response: AxiosResponse<ApiResponse<Track[]>> = await this.api.get('/recommendations/personalized', {
      params: { limit }
    })
    return response.data.data
  }

  async getSimilarTracks(trackId: string, limit = 10): Promise<Track[]> {
    const response: AxiosResponse<ApiResponse<Track[]>> = await this.api.get(`/recommendations/similar/${trackId}`, {
      params: { limit }
    })
    return response.data.data
  }

  // Analytics endpoints
  async recordPlayback(trackId: string, duration: number, completed: boolean): Promise<void> {
    await this.api.post('/analytics/playback', {
      trackId,
      duration,
      completed,
      timestamp: new Date().toISOString()
    })
  }

  async recordSkip(trackId: string, position: number): Promise<void> {
    await this.api.post('/analytics/skip', {
      trackId,
      position,
      timestamp: new Date().toISOString()
    })
  }

  // Mood and preferences
  async saveMoodPreference(mood: string, energy: number, trackIds: string[]): Promise<void> {
    await this.api.post('/preferences/mood', {
      mood,
      energy,
      trackIds,
      timestamp: new Date().toISOString()
    })
  }

  async getUserPreferences(): Promise<Record<string, unknown>> {
    const response: AxiosResponse<ApiResponse<Record<string, unknown>>> = await this.api.get('/preferences')
    return response.data.data
  }

  // Spotify integration (if available)
  async getSpotifyAuthUrl(): Promise<string> {
    const response: AxiosResponse<ApiResponse<{ authUrl: string }>> = await this.api.get('/spotify/auth-url')
    return response.data.data.authUrl
  }

  async exchangeSpotifyCode(code: string): Promise<void> {
    await this.api.post('/spotify/exchange-code', { code })
  }

  async getSpotifyTracks(spotifyIds: string[]): Promise<Track[]> {
    const response: AxiosResponse<ApiResponse<Track[]>> = await this.api.post('/spotify/tracks', { spotifyIds })
    return response.data.data
  }

  // Utility methods
  async checkHealth(): Promise<boolean> {
    try {
      const response = await this.api.get('/health')
      return response.status === 200
    } catch {
      return false
    }
  }

  async getApiVersion(): Promise<string> {
    const response: AxiosResponse<ApiResponse<{ version: string }>> = await this.api.get('/version')
    return response.data.data.version
  }

}

// Create and export singleton instance
export const musicApi = new MusicApiService()
export default musicApi