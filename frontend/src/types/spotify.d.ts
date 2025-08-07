// Spotify Web SDK TypeScript definitions
// Reference: https://developer.spotify.com/documentation/web-playback-sdk/

declare global {
  interface Window {
    onSpotifyWebPlaybackSDKReady: () => void;
    Spotify: typeof Spotify;
  }
}

export namespace Spotify {
  interface Player {
    new (options: SpotifyPlayerOptions): Player;
    
    // Connection
    connect(): Promise<boolean>;
    disconnect(): void;
    
    // Listeners
    addListener(event: PlayerEventName, callback: PlayerEventCallback): boolean;
    removeListener(event: PlayerEventName, callback?: PlayerEventCallback): boolean;
    
    // Playback
    getCurrentState(): Promise<PlaybackState | null>;
    setName(name: string): Promise<void>;
    getVolume(): Promise<number>;
    setVolume(volume: number): Promise<void>;
    pause(): Promise<void>;
    resume(): Promise<void>;
    togglePlay(): Promise<void>;
    seek(positionMs: number): Promise<void>;
    previousTrack(): Promise<void>;
    nextTrack(): Promise<void>;
  }

  interface SpotifyPlayerOptions {
    name: string;
    getOAuthToken: (callback: (token: string) => void) => void;
    volume?: number;
  }

  interface PlaybackState {
    context: {
      uri: string;
      metadata: any;
    };
    disallows: {
      pausing: boolean;
      peeking_next: boolean;
      peeking_prev: boolean;
      resuming: boolean;
      seeking: boolean;
      skipping_next: boolean;
      skipping_prev: boolean;
    };
    paused: boolean;
    position: number;
    repeat_mode: 0 | 1 | 2; // 0: off, 1: context, 2: track
    shuffle: boolean;
    track_window: {
      current_track: SpotifyTrack;
      next_tracks: SpotifyTrack[];
      previous_tracks: SpotifyTrack[];
    };
  }

  interface SpotifyTrack {
    id: string;
    uri: string;
    name: string;
    duration_ms: number;
    explicit: boolean;
    popularity: number;
    album: {
      name: string;
      uri: string;
      images: SpotifyImage[];
    };
    artists: SpotifyArtist[];
  }

  interface SpotifyArtist {
    name: string;
    uri: string;
  }

  interface SpotifyImage {
    url: string;
    height: number;
    width: number;
  }

  interface ErrorEvent {
    message: string;
  }

  interface ReadyEvent {
    device_id: string;
  }

  interface NotReadyEvent {
    device_id: string;
  }

  type PlayerEventName = 
    | 'ready'
    | 'not_ready'
    | 'player_state_changed'
    | 'initialization_error'
    | 'authentication_error'
    | 'account_error'
    | 'playback_error';

  type PlayerEventCallback = 
    | ((event: ReadyEvent) => void)
    | ((event: NotReadyEvent) => void)
    | ((state: PlaybackState | null) => void)
    | ((event: ErrorEvent) => void);
}

// Spotify Web API types
export interface SpotifyAuthResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token?: string;
  scope: string;
}

export interface SpotifyUserProfile {
  id: string;
  display_name: string;
  email?: string;
  country?: string;
  product: 'free' | 'premium';
  images?: SpotifyImage[];
  followers: {
    total: number;
  };
}

export interface SpotifyDevice {
  id: string;
  is_active: boolean;
  is_private_session: boolean;
  is_restricted: boolean;
  name: string;
  type: string;
  volume_percent: number;
}

export interface SpotifyPlaybackContext {
  device: SpotifyDevice;
  repeat_state: 'off' | 'track' | 'context';
  shuffle_state: boolean;
  context: {
    external_urls: { spotify: string };
    href: string;
    type: string;
    uri: string;
  } | null;
  timestamp: number;
  progress_ms: number;
  is_playing: boolean;
  item: SpotifyTrack | null;
}

export interface SpotifyError {
  status: number;
  message: string;
}

// Custom types for our application
export interface SpotifyPlayerState {
  isReady: boolean;
  isConnected: boolean;
  deviceId: string | null;
  player: Spotify.Player | null;
}

export interface SpotifyAuthState {
  isAuthenticated: boolean;
  isAuthenticating: boolean;
  token: string | null;
  refreshToken: string | null;
  expiresAt: number | null;
  user: SpotifyUserProfile | null;
  isPremium: boolean;
  error: string | null;
}

export interface UseSpotifyPlayerOptions {
  token: string;
  name: string;
  volume?: number;
  getOAuthToken?: (cb: (token: string) => void) => void;
}

export interface SpotifyConnectionStatus {
  isConnected: boolean;
  isPremium: boolean;
  deviceName: string | null;
  error: string | null;
}

// Configuration types
export interface SpotifyConfig {
  clientId: string;
  redirectUri: string;
  scopes: string[];
  musicServiceUrl: string;
}

export interface SpotifyWebApiOptions {
  baseUrl?: string;
  token: string;
}

export interface TransferPlaybackOptions {
  deviceIds: string[];
  play?: boolean;
}

export interface PlayOptions {
  uris?: string[];
  context_uri?: string;
  offset?: {
    position?: number;
    uri?: string;
  };
  position_ms?: number;
}

export {}; // Make this a module