import { lazy, Suspense, ComponentType, ReactElement } from 'react'
import { RouteLoadingFallback, FeatureLoadingFallback } from '@shared/components/loading'

// Utility function to create lazy components with error boundaries and loading states
function createLazyComponent(
  importFn: () => Promise<{ default: ComponentType<any> }>,
  fallbackElement?: ReactElement,
  displayName?: string
) {
  const LazyComponent = lazy(importFn)
  
  const WrappedComponent = (props: any) => {
    const fallback = fallbackElement || <RouteLoadingFallback />
    
    return (
      <Suspense fallback={fallback}>
        <LazyComponent {...props} />
      </Suspense>
    )
  }
  
  WrappedComponent.displayName = displayName || 'LazyComponent'
  return WrappedComponent
}

// Route-level lazy components
export const LazyHomePage = createLazyComponent(
  () => import('@features/auth/pages/HomePage'),
  <RouteLoadingFallback />,
  'LazyHomePage'
)

export const LazyLoginPage = createLazyComponent(
  () => import('@features/auth/pages/LoginPage'),
  <RouteLoadingFallback />,
  'LazyLoginPage'
)

export const LazyRegisterPage = createLazyComponent(
  () => import('@features/auth/pages/RegisterPage'),
  <RouteLoadingFallback />,
  'LazyRegisterPage'
)

export const LazyDashboardPage = createLazyComponent(
  () => import('@features/hive/pages/DashboardPage'),
  <RouteLoadingFallback />,
  'LazyDashboardPage'
)

export const LazyDiscoverPage = createLazyComponent(
  () => import('@features/hive/pages/DiscoverPage'),
  <RouteLoadingFallback />,
  'LazyDiscoverPage'
)

// Import heavy feature components with optimized lazy loading
import { 
  LazyGamificationDemo,
  LazyAnalyticsDashboard
} from '@shared/components/lazy-features'

// Keep existing wrappers for backward compatibility
export { LazyGamificationDemo, LazyAnalyticsDashboard }

export const LazyAnalyticsDemo = createLazyComponent(
  () => import('@features/analytics/pages/AnalyticsDemo'),
  <FeatureLoadingFallback featureName="Analytics" />,
  'LazyAnalyticsDemo'
)

export const LazyProductivityDashboard = createLazyComponent(
  () => import('@features/timer/pages/ProductivityDashboard'),
  <FeatureLoadingFallback featureName="Productivity Tracker" />,
  'LazyProductivityDashboard'
)

// Import music components with optimized lazy loading
import {
  LazyMusicPlayer,
  LazySpotifyConnect
} from '@shared/components/lazy-features'

// Keep existing exports for backward compatibility
export { LazyMusicPlayer, LazySpotifyConnect }

// Demo and development components
export const LazyErrorBoundaryDemo = createLazyComponent(
  () => import('@shared/components/error-boundary/ErrorBoundaryDemo'),
  <RouteLoadingFallback />,
  'LazyErrorBoundaryDemo'
)

export const LazyResponsiveDemo = createLazyComponent(
  () => import('@features/demo/pages/ResponsiveDemo'),
  <FeatureLoadingFallback featureName="Responsive Demo" />,
  'LazyResponsiveDemo'
)

export const LazyLoadingStatesDemo = createLazyComponent(
  () => import('@features/demo/pages/LoadingStatesDemo'),
  <FeatureLoadingFallback featureName="Loading States Demo" />,
  'LazyLoadingStatesDemo'
)

// Import communication and social features with optimized lazy loading
import {
  LazyChatWindow,
  LazyForumHome,
  LazyBuddyDashboard
} from '@shared/components/lazy-features'

// Keep backward compatibility exports
export { LazyChatWindow, LazyForumHome, LazyBuddyDashboard }

// Forum post view remains as local component for now
export const LazyForumPostView = createLazyComponent(
  () => import('@features/forum/components/ForumPostView'),
  <FeatureLoadingFallback featureName="Forum Post" />,
  'LazyForumPostView'
)

// Helper function for dynamic imports based on feature flags
export function createConditionalLazyComponent(
  condition: boolean,
  importFn: () => Promise<{ default: ComponentType<any> }>,
  fallbackComponent?: ComponentType<any>,
  loadingElement?: ReactElement
) {
  if (!condition && fallbackComponent) {
    return fallbackComponent
  }
  
  return createLazyComponent(importFn, loadingElement)
}

// Route configuration with lazy loading
export interface LazyRouteConfig {
  path: string
  component: ComponentType<any>
  preload?: boolean
  chunkName?: string
}

export const lazyRoutes: LazyRouteConfig[] = [
  {
    path: '/',
    component: LazyHomePage,
    preload: true, // Preload home page
    chunkName: 'home'
  },
  {
    path: '/login',
    component: LazyLoginPage,
    preload: true, // Preload auth pages
    chunkName: 'auth'
  },
  {
    path: '/register',
    component: LazyRegisterPage,
    preload: true,
    chunkName: 'auth'
  },
  {
    path: '/dashboard',
    component: LazyDashboardPage,
    preload: false,
    chunkName: 'dashboard'
  },
  {
    path: '/discover',
    component: LazyDiscoverPage,
    preload: false,
    chunkName: 'discover'
  },
  {
    path: '/gamification',
    component: LazyGamificationDemo,
    preload: false,
    chunkName: 'gamification'
  },
  {
    path: '/analytics',
    component: LazyAnalyticsDemo,
    preload: false,
    chunkName: 'analytics'
  },
  {
    path: '/productivity',
    component: LazyProductivityDashboard,
    preload: false,
    chunkName: 'productivity'
  },
  {
    path: '/error-boundary-demo',
    component: LazyErrorBoundaryDemo,
    preload: false,
    chunkName: 'demo'
  }
]

// Preloader function to prefetch critical routes
export function preloadCriticalRoutes() {
  // Critical routes are already loaded with the main bundle
  console.log('[Routes] Critical routes preloaded')
}

// Export route preloading utilities
export const routePreloaders = {
  // Preload authentication routes
  preloadAuth: () => {
    console.log('[Routes] Auth routes ready for preload')
  },
  
  // Preload main app routes
  preloadMainApp: () => {
    console.log('[Routes] Main app routes ready for preload')
  },
  
  // Preload heavy features on user interaction
  preloadHeavyFeatures: () => {
    console.log('[Routes] Heavy features ready for preload')
  }
}