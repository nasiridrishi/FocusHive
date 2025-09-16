// Feature utility functions and constants
// Separated from LazyFeatureComponents.tsx to avoid fast-refresh warnings

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
export const preloadHeavyFeatures = (): void => {
  // Only preload on good connections
  if ('connection' in navigator) {
    const connection = (navigator as {
      connection?: { effectiveType?: string; saveData?: boolean }
    }).connection
    if (connection?.effectiveType === '4g' && !connection.saveData) {
      setTimeout(() => {
        // Preload most commonly used heavy features
        featurePreloaders.preloadAnalyticsDashboard()
        featurePreloaders.preloadGamificationDemo()
      }, 5000)
    }
  }

  // console.log('[Features] Heavy features preload scheduled')
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