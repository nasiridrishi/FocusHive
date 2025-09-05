/**
 * Lazy-loaded feature components to reduce bundle size
 * Splits heavy feature components into separate chunks
 */

import React, { lazy, Suspense, ComponentType } from 'react'
import { Box, Typography, CircularProgress, Skeleton as _Skeleton } from '@mui/material'
import { Dashboard as _Dashboard, EmojiEvents, Analytics } from '@mui/icons-material'

// Feature loading fallback component
const FeatureLoadingFallback = ({ 
  featureName, 
  height = 300, 
  icon: Icon 
}: { 
  featureName?: string
  height?: number
  icon?: React.ComponentType
}) => (
  <Box 
    display="flex" 
    flexDirection="column" 
    alignItems="center" 
    justifyContent="center" 
    minHeight={height}
    gap={2}
    p={3}
  >
    <CircularProgress size={40} />
    <Box display="flex" alignItems="center" gap={1}>
      {Icon && <Icon />}
      <Typography variant="body2" color="text.secondary">
        Loading {featureName || 'feature'}...
      </Typography>
    </Box>
  </Box>
)

// Utility to create lazy feature components with optimized loading
function createLazyFeature<T = Record<string, unknown>>(
  importFn: () => Promise<{ default: ComponentType<T> }>,
  displayName?: string,
  fallbackHeight?: number,
  icon?: React.ComponentType
) {
  const LazyFeature = lazy(importFn)
  
  const WrappedFeature = (props: T) => (
    <Suspense 
      fallback={
        <FeatureLoadingFallback 
          featureName={displayName}
          height={fallbackHeight}
          icon={icon}
        />
      }
    >
      <LazyFeature {...(props as any)} />
    </Suspense>
  )
  
  WrappedFeature.displayName = displayName || 'LazyFeature'
  return WrappedFeature
}

// Lazy-loaded Analytics Components (Heavy with charts)
export const LazyAnalyticsDashboard = createLazyFeature(
  () => import('@features/analytics/components/AnalyticsDashboard'),
  'Analytics Dashboard',
  400,
  Analytics
)

export const LazyProductivityChart = createLazyFeature(
  () => import('@features/analytics/components/ProductivityChart').then(module => ({ default: module.ProductivityChart })),
  'Productivity Chart',
  300
)

export const LazyTaskCompletionRate = createLazyFeature(
  () => import('@features/analytics/components/TaskCompletionRate').then(module => ({ default: module.TaskCompletionRate })),
  'Task Completion Rate',
  300
)

export const LazyHiveActivityHeatmap = createLazyFeature(
  () => import('@features/analytics/components/HiveActivityHeatmap').then(module => ({ default: module.HiveActivityHeatmap })),
  'Activity Heatmap',
  300
)

export const LazyMemberEngagement = createLazyFeature(
  () => import('@features/analytics/components/MemberEngagement').then(module => ({ default: module.MemberEngagement })),
  'Member Engagement',
  300
)

export const LazyGoalProgress = createLazyFeature(
  () => import('@features/analytics/components/GoalProgress').then(module => ({ default: module.GoalProgress })),
  'Goal Progress',
  200
)

// Lazy-loaded Gamification Components (Heavy)
export const LazyGamificationDemo = createLazyFeature(
  () => import('@features/gamification/pages/GamificationDemo'),
  'Gamification System',
  500,
  EmojiEvents
)

export const LazyLeaderboardCard = createLazyFeature(
  () => import('@features/gamification/components/LeaderboardCard'),
  'Leaderboard',
  400
)

export const LazyAchievementBadge = createLazyFeature(
  () => import('@features/gamification/components/AchievementBadge'),
  'Achievement Badge',
  150
)

export const LazyStreakCounter = createLazyFeature(
  () => import('@features/gamification/components/StreakCounter'),
  'Streak Counter',
  100
)

export const LazyPointsDisplay = createLazyFeature(
  () => import('@features/gamification/components/PointsDisplay'),
  'Points Display',
  100
)

// Lazy-loaded Music Components (Heavy with Spotify integration)
export const LazyMusicPlayer = createLazyFeature(
  () => import('@features/music/components/music-player/MusicPlayer'),
  'Music Player',
  200
)

export const LazySpotifyConnect = createLazyFeature(
  () => import('@features/music/components/spotify-connect/SpotifyConnectButton'),
  'Spotify Integration',
  150
)

// Lazy-loaded Communication Components (WebSocket heavy)
export const LazyChatWindow = createLazyFeature(
  () => import('@features/chat/components/ChatWindow'),
  'Chat System',
  400
)

export const LazyForumHome = createLazyFeature(
  () => import('@features/forum/components/ForumHome'),
  'Forum',
  400
)

export const LazyBuddyDashboard = createLazyFeature(
  () => import('@features/buddy/components/BuddyDashboard'),
  'Buddy System',
  400
)

// Component preloaders for specific features
export const featurePreloaders = {
  // Analytics preloaders
  preloadAnalyticsDashboard: () => import('@features/analytics/components/AnalyticsDashboard'),
  preloadProductivityChart: () => import('@features/analytics/components/ProductivityChart'),
  preloadTaskCompletionRate: () => import('@features/analytics/components/TaskCompletionRate'),
  preloadHiveActivityHeatmap: () => import('@features/analytics/components/HiveActivityHeatmap'),
  preloadMemberEngagement: () => import('@features/analytics/components/MemberEngagement'),
  preloadGoalProgress: () => import('@features/analytics/components/GoalProgress'),
  
  // Gamification preloaders
  preloadGamificationDemo: () => import('@features/gamification/pages/GamificationDemo'),
  preloadLeaderboardCard: () => import('@features/gamification/components/LeaderboardCard'),
  preloadAchievementBadge: () => import('@features/gamification/components/AchievementBadge'),
  preloadStreakCounter: () => import('@features/gamification/components/StreakCounter'),
  preloadPointsDisplay: () => import('@features/gamification/components/PointsDisplay'),
  
  // Music preloaders
  preloadMusicPlayer: () => import('@features/music/components/music-player/MusicPlayer'),
  preloadSpotifyConnect: () => import('@features/music/components/spotify-connect/SpotifyConnectButton'),
  
  // Communication preloaders
  preloadChatWindow: () => import('@features/chat/components/ChatWindow'),
  preloadForumHome: () => import('@features/forum/components/ForumHome'),
  preloadBuddyDashboard: () => import('@features/buddy/components/BuddyDashboard'),
}

// Batch preloader for heavy features
export const preloadHeavyFeatures = () => {
  // Only preload on good connections
  if ('connection' in navigator) {
    const connection = (navigator as any).connection
    if (connection?.effectiveType === '4g' && !connection.saveData) {
      setTimeout(() => {
        // Preload most commonly used heavy features
        featurePreloaders.preloadAnalyticsDashboard()
        featurePreloaders.preloadGamificationDemo()
      }, 5000)
    }
  }
  
  console.log('[Features] Heavy features preload scheduled')
}

// Feature bundle information for monitoring
export const featureBundleInfo = {
  analytics: {
    estimatedSize: '~180KB',
    components: ['AnalyticsDashboard', 'ProductivityChart', 'Charts'],
    dependencies: ['recharts', 'd3', '@mui/x-charts']
  },
  gamification: {
    estimatedSize: '~150KB',
    components: ['GamificationDemo', 'LeaderboardCard', 'AchievementBadge'],
    dependencies: ['@mui/material', '@mui/icons-material']
  },
  music: {
    estimatedSize: '~120KB',
    components: ['MusicPlayer', 'SpotifyConnect'],
    dependencies: ['spotify-web-api-sdk']
  },
  communication: {
    estimatedSize: '~90KB',
    components: ['ChatWindow', 'ForumHome', 'BuddyDashboard'],
    dependencies: ['socket.io-client']
  }
}

export { FeatureLoadingFallback }