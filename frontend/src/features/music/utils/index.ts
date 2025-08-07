// Music utility functions

/**
 * Format time in seconds to MM:SS or HH:MM:SS format
 */
export const formatTime = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

/**
 * Format duration for display (e.g., "3m 45s" or "1h 23m")
 */
export const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  return `${minutes}m`
}

/**
 * Format time ago (e.g., "2m ago", "1h ago")
 */
export const formatTimeAgo = (dateString?: string): string => {
  if (!dateString) return 'Recently'
  
  const date = new Date(dateString)
  const now = new Date()
  const diffMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
  
  if (diffMinutes < 1) return 'Just now'
  if (diffMinutes < 60) return `${diffMinutes}m ago`
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)}h ago`
  return `${Math.floor(diffMinutes / 1440)}d ago`
}

/**
 * Calculate total duration of tracks
 */
export const calculateTotalDuration = (tracks: { duration: number }[]): number => {
  return tracks.reduce((total, track) => total + track.duration, 0)
}

/**
 * Generate a random color for visualizations
 */
export const generateRandomColor = (): string => {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
    '#DDA0DD', '#FFB6C1', '#87CEEB', '#F0E68C', '#FFA07A'
  ]
  return colors[Math.floor(Math.random() * colors.length)]
}

/**
 * Debounce function for search inputs
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

/**
 * Validate Spotify URI format
 */
export const isValidSpotifyUri = (uri: string): boolean => {
  const spotifyUriRegex = /^spotify:(track|album|artist|playlist):[a-zA-Z0-9]{22}$/
  return spotifyUriRegex.test(uri)
}

/**
 * Extract Spotify ID from URI
 */
export const extractSpotifyId = (uri: string): string | null => {
  const match = uri.match(/spotify:(track|album|artist|playlist):([a-zA-Z0-9]{22})$/)
  return match ? match[2] : null
}

/**
 * Convert seconds to percentage of a total duration
 */
export const secondsToPercentage = (seconds: number, totalSeconds: number): number => {
  if (totalSeconds === 0) return 0
  return Math.min(100, Math.max(0, (seconds / totalSeconds) * 100))
}

/**
 * Convert percentage to seconds of a total duration
 */
export const percentageToSeconds = (percentage: number, totalSeconds: number): number => {
  return Math.min(totalSeconds, Math.max(0, (percentage / 100) * totalSeconds))
}

/**
 * Shuffle array using Fisher-Yates algorithm
 */
export const shuffleArray = <T>(array: T[]): T[] => {
  const shuffled = [...array]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
}

/**
 * Generate a readable file size string
 */
export const formatFileSize = (bytes: number): string => {
  const sizes = ['B', 'KB', 'MB', 'GB']
  if (bytes === 0) return '0 B'
  
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

/**
 * Parse and clean search query
 */
export const cleanSearchQuery = (query: string): string => {
  return query
    .trim()
    .toLowerCase()
    .replace(/[^\w\s-]/g, '') // Remove special characters except spaces and hyphens
    .replace(/\s+/g, ' ') // Replace multiple spaces with single space
}

/**
 * Generate a URL-safe slug from text
 */
export const generateSlug = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '') // Remove special characters
    .replace(/\s+/g, '-') // Replace spaces with hyphens
    .replace(/-+/g, '-') // Replace multiple hyphens with single hyphen
}

/**
 * Check if audio format is supported
 */
export const isSupportedAudioFormat = (mimeType: string): boolean => {
  const audio = document.createElement('audio')
  return audio.canPlayType(mimeType) !== ''
}

/**
 * Get audio format priority (higher is better)
 */
export const getAudioFormatPriority = (mimeType: string): number => {
  const priorities: Record<string, number> = {
    'audio/mp3': 5,
    'audio/mpeg': 5,
    'audio/ogg': 4,
    'audio/webm': 4,
    'audio/wav': 3,
    'audio/flac': 2,
    'audio/m4a': 1,
  }
  return priorities[mimeType] || 0
}

/**
 * Calculate listening statistics
 */
export const calculateListeningStats = (history: Array<{ duration: number; completed: boolean }>) => {
  const totalTracks = history.length
  const completedTracks = history.filter(h => h.completed).length
  const totalTime = history.reduce((sum, h) => sum + h.duration, 0)
  const completionRate = totalTracks > 0 ? (completedTracks / totalTracks) * 100 : 0
  
  return {
    totalTracks,
    completedTracks,
    totalTime,
    completionRate,
    averageDuration: totalTracks > 0 ? totalTime / totalTracks : 0,
  }
}