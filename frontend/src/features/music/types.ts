// Re-export all types from the canonical location
export * from './types/music'

// Legacy compatibility exports
export type {
  MusicApiResponse as ApiResponse,
  PaginatedResponse as PaginatedMusicResponse
} from './types/music'

// Deprecated - use exports from './types/music' instead
export type {AudioFeatures} from './types/music'
