export interface LazyFeatureConfig {
  name: string;
  description?: string;
  requiredPermissions?: string[];
  dependencies?: string[];
  preload?: boolean;
}

export type FeatureStatus = 'loading' | 'loaded' | 'error' | 'idle';

export interface FeatureState {
  status: FeatureStatus;
  error?: Error;
  component?: React.ComponentType;
}

export const FEATURE_NAMES = {
  GAMIFICATION: 'gamification',
  ANALYTICS: 'analytics',
  MUSIC_PLAYER: 'music-player',
  SPOTIFY_CONNECT: 'spotify-connect',
  CHAT: 'chat',
  FORUM: 'forum',
  BUDDY: 'buddy',
} as const;

export type FeatureName = typeof FEATURE_NAMES[keyof typeof FEATURE_NAMES];