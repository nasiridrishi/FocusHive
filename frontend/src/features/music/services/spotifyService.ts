// Spotify Web SDK Service
// Handles SDK initialization, authentication, and device management

import type { 
  SpotifyConfig, 
  SpotifyAuthState, 
  SpotifyPlayerState,
  TransferPlaybackOptions,
  PlayOptions,
  SpotifyDevice,
  SpotifyUserProfile,
  SpotifyPlaybackContext,
  SpotifyError,
  Spotify
} from '../../../types/spotify'

export class SpotifyService {
  private config: SpotifyConfig
  private authState: SpotifyAuthState = {
    isAuthenticated: false,
    isAuthenticating: false,
    token: null,
    refreshToken: null,
    expiresAt: null,
    user: null,
    isPremium: false,
    error: null
  }
  private playerState: SpotifyPlayerState = {
    isReady: false,
    isConnected: false,
    deviceId: null,
    player: null
  }

  constructor(config: SpotifyConfig) {
    this.config = config
    this.loadAuthFromStorage()
  }

  // Configuration
  getConfig(): SpotifyConfig {
    return this.config
  }

  updateConfig(newConfig: Partial<SpotifyConfig>): void {
    this.config = { ...this.config, ...newConfig }
  }

  // Authentication State Management
  getAuthState(): SpotifyAuthState {
    return this.authState
  }

  getPlayerState(): SpotifyPlayerState {
    return this.playerState
  }

  private updateAuthState(updates: Partial<SpotifyAuthState>): void {
    this.authState = { ...this.authState, ...updates }
    if (updates.token || updates.refreshToken || updates.expiresAt !== undefined) {
      this.saveAuthToStorage()
    }
  }

  private updatePlayerState(updates: Partial<SpotifyPlayerState>): void {
    this.playerState = { ...this.playerState, ...updates }
  }

  // Local Storage Management
  private saveAuthToStorage(): void {
    const authData = {
      token: this.authState.token,
      refreshToken: this.authState.refreshToken,
      expiresAt: this.authState.expiresAt,
      user: this.authState.user,
      isPremium: this.authState.isPremium
    }
    localStorage.setItem('spotify_auth', JSON.stringify(authData))
  }

  private loadAuthFromStorage(): void {
    try {
      const stored = localStorage.getItem('spotify_auth')
      if (stored) {
        const authData = JSON.parse(stored)
        this.updateAuthState({
          ...authData,
          isAuthenticated: authData.token && authData.expiresAt > Date.now()
        })
      }
    } catch (error) {
      console.error('Error:', error);
      this.clearAuth()
    }
  }

  private clearAuthFromStorage(): void {
    localStorage.removeItem('spotify_auth')
  }

  // Authentication Flow
  startAuthFlow(): void {
    const params = new URLSearchParams({
      client_id: this.config.clientId,
      response_type: 'code',
      redirect_uri: this.config.redirectUri,
      scope: this.config.scopes.join(' '),
      state: Math.random().toString(36).substring(2, 15) // CSRF protection
    })

    const authUrl = `${this.config.musicServiceUrl}/spotify/auth?${params}`
    window.location.href = authUrl
  }

  async handleAuthCallback(code: string, state: string): Promise<boolean> {
    try {
      this.updateAuthState({ isAuthenticating: true, error: null })

      const response = await fetch(`${this.config.musicServiceUrl}/spotify/callback`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ code, state, redirect_uri: this.config.redirectUri })
      })

      if (!response.ok) {
        throw new Error(`Authentication failed: ${response.statusText}`)
      }

      const authData = await response.json()
      
      // Get user profile to check premium status
      const user = await this.getUserProfile(authData.access_token)
      
      this.updateAuthState({
        isAuthenticated: true,
        isAuthenticating: false,
        token: authData.access_token,
        refreshToken: authData.refresh_token,
        expiresAt: Date.now() + (authData.expires_in * 1000),
        user,
        isPremium: user.product === 'premium',
        error: null
      })

      return true
    } catch (error) {
      this.updateAuthState({
        isAuthenticating: false,
        error: error instanceof Error ? error.message : 'Authentication failed'
      })
      return false
    }
  }

  async refreshAuth(): Promise<boolean> {
    if (!this.authState.refreshToken) {
      return false
    }

    try {
      const response = await fetch(`${this.config.musicServiceUrl}/spotify/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refresh_token: this.authState.refreshToken })
      })

      if (!response.ok) {
        throw new Error('Token refresh failed')
      }

      const authData = await response.json()
      
      this.updateAuthState({
        token: authData.access_token,
        expiresAt: Date.now() + (authData.expires_in * 1000),
        error: null
      })

      return true
    } catch (error) {
      console.error('Error:', error);
      this.clearAuth()
      return false
    }
  }

  clearAuth(): void {
    if (this.playerState.player) {
      this.playerState.player.disconnect()
    }
    
    this.updateAuthState({
      isAuthenticated: false,
      isAuthenticating: false,
      token: null,
      refreshToken: null,
      expiresAt: null,
      user: null,
      isPremium: false,
      error: null
    })
    
    this.updatePlayerState({
      isReady: false,
      isConnected: false,
      deviceId: null,
      player: null
    })
    
    this.clearAuthFromStorage()
  }

  // Token Management
  async getValidToken(): Promise<string | null> {
    if (!this.authState.token) {
      return null
    }

    // Check if token is expired (with 5 minute buffer)
    if (this.authState.expiresAt && this.authState.expiresAt < Date.now() + 300000) {
      const refreshed = await this.refreshAuth()
      if (!refreshed) {
        return null
      }
    }

    return this.authState.token
  }

  // Spotify Web SDK Management
  async loadSpotifySDK(): Promise<boolean> {
    if (typeof window === 'undefined') {
      return false
    }

    return new Promise((resolve) => {
      if (window.Spotify) {
        resolve(true)
        return
      }

      // Load Spotify Web SDK script
      const script = document.createElement('script')
      script.src = 'https://sdk.scdn.co/spotify-player.js'
      script.async = true
      script.onerror = () => resolve(false)
      
      window.onSpotifyWebPlaybackSDKReady = () => {
        resolve(true)
      }

      document.body.appendChild(script)
    })
  }

  async initializePlayer(name: string, volume: number = 0.5): Promise<boolean> {
    const token = await this.getValidToken()
    if (!token) {
      this.updatePlayerState({ player: null })
      return false
    }

    const sdkLoaded = await this.loadSpotifySDK()
    if (!sdkLoaded || !window.Spotify) {
      return false
    }

    try {
      const player = new window.Spotify.Player({
        name,
        getOAuthToken: async (cb) => {
          const validToken = await this.getValidToken()
          if (validToken) {
            cb(validToken)
          }
        },
        volume
      })

      this.setupPlayerListeners(player)
      this.updatePlayerState({ player })

      const connected = await player.connect()
      return connected
    } catch (error) {
      console.error('Error:', error);
      return false
    ;
    }
  }

  private setupPlayerListeners(player: Spotify.Player): void {
    // Ready
    player.addListener('ready', ({ device_id: deviceId }: { device_id: string }) => {
      this.updatePlayerState({
        isReady: true,
        isConnected: true,
        deviceId: deviceId
      })
    })

    // Not ready
    player.addListener('not_ready', () => {
      this.updatePlayerState({
        isReady: false,
        isConnected: false
      })
    })

    // Error handling
    player.addListener('initialization_error', ({ message }: { message: string }) => {
      this.updateAuthState({ error: `Initialization error: ${message}` })
    })

    player.addListener('authentication_error', ({ message }: { message: string }) => {
      this.updateAuthState({ error: `Authentication error: ${message}` })
      // Try to refresh token
      this.refreshAuth()
    })

    player.addListener('account_error', ({ message }: { message: string }) => {
      this.updateAuthState({ error: `Account error: ${message}` })
    })

    player.addListener('playback_error', () => {
      // Playback error occurred
    })
  }

  disconnect(): void {
    if (this.playerState.player) {
      this.playerState.player.disconnect()
      this.updatePlayerState({
        isReady: false,
        isConnected: false,
        deviceId: null,
        player: null
      })
    }
  }

  // Spotify Web API calls
  private async makeSpotifyAPICall<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = await this.getValidToken()
    if (!token) {
      throw new Error('No valid Spotify token available')
    }

    const response = await fetch(`https://api.spotify.com/v1${endpoint}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options.headers
      }
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: { message: response.statusText } }))
      const error: SpotifyError = {
        status: response.status,
        message: errorData.error?.message || response.statusText
      }
      throw error
    }

    return response.json()
  }

  async getUserProfile(token?: string): Promise<SpotifyUserProfile> {
    if (token) {
      // Use provided token for initial auth
      const response = await fetch('https://api.spotify.com/v1/me', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      if (!response.ok) {
        throw new Error('Failed to get user profile')
      }
      return response.json()
    }

    return this.makeSpotifyAPICall<SpotifyUserProfile>('/me')
  }

  async getDevices(): Promise<SpotifyDevice[]> {
    const data = await this.makeSpotifyAPICall<{ devices: SpotifyDevice[] }>('/me/player/devices')
    return data.devices
  }

  async getCurrentPlayback(): Promise<SpotifyPlaybackContext | null> {
    try {
      return await this.makeSpotifyAPICall<SpotifyPlaybackContext>('/me/player')
    } catch (error) {
      if ((error as SpotifyError).status === 204) {
        return null // No active device
      }
      throw error
    }
  }

  async transferPlayback(options: TransferPlaybackOptions): Promise<void> {
    await this.makeSpotifyAPICall('/me/player', {
      method: 'PUT',
      body: JSON.stringify({
        device_ids: options.deviceIds,
        play: options.play || false
      })
    })
  }

  async play(deviceId: string, options?: PlayOptions): Promise<void> {
    const endpoint = `/me/player/play${deviceId ? `?device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT',
      body: options ? JSON.stringify(options) : undefined
    })
  }

  async pause(deviceId?: string): Promise<void> {
    const endpoint = `/me/player/pause${deviceId ? `?device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT'
    })
  }

  async next(deviceId?: string): Promise<void> {
    const endpoint = `/me/player/next${deviceId ? `?device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'POST'
    })
  }

  async previous(deviceId?: string): Promise<void> {
    const endpoint = `/me/player/previous${deviceId ? `?device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'POST'
    })
  }

  async seek(positionMs: number, deviceId?: string): Promise<void> {
    const endpoint = `/me/player/seek?position_ms=${positionMs}${deviceId ? `&device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT'
    })
  }

  async setVolume(volumePercent: number, deviceId?: string): Promise<void> {
    const endpoint = `/me/player/volume?volume_percent=${volumePercent}${deviceId ? `&device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT'
    })
  }

  async shuffle(state: boolean, deviceId?: string): Promise<void> {
    const endpoint = `/me/player/shuffle?state=${state}${deviceId ? `&device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT'
    })
  }

  async repeat(state: 'off' | 'track' | 'context', deviceId?: string): Promise<void> {
    const endpoint = `/me/player/repeat?state=${state}${deviceId ? `&device_id=${deviceId}` : ''}`
    await this.makeSpotifyAPICall(endpoint, {
      method: 'PUT'
    })
  }
}

// Singleton instance
let spotifyServiceInstance: SpotifyService | null = null

export function getSpotifyService(config?: SpotifyConfig): SpotifyService {
  if (!spotifyServiceInstance) {
    if (!config) {
      throw new Error('SpotifyService must be initialized with config')
    }
    spotifyServiceInstance = new SpotifyService(config)
  }
  return spotifyServiceInstance
}

export function resetSpotifyService(): void {
  spotifyServiceInstance = null
}