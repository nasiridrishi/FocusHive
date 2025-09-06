import { RouteLoadingFallback, FeatureLoadingFallback } from '@shared/components/loading'
import { createLazyComponent } from './lazyRouteUtils'

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
// These components should be imported directly from @shared/components/lazy-features
// Not re-exported here to avoid Fast Refresh warnings

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
// Music components should be imported directly from @shared/components/lazy-features
// Not re-exported here to avoid Fast Refresh warnings

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
// Communication components should be imported directly from @shared/components/lazy-features
// Not re-exported here to avoid Fast Refresh warnings

// Forum post view remains as local component for now
export const LazyForumPostView = createLazyComponent(
  () => import('@features/forum/components/ForumPostView'),
  <FeatureLoadingFallback featureName="Forum Post" />,
  'LazyForumPostView'
)

// Utilities should be imported directly from './lazyRouteUtils'
// Not re-exported here to avoid Fast Refresh warnings

// Re-export type using 'export type'
export type { LazyRouteConfig } from './lazyRouteUtils'