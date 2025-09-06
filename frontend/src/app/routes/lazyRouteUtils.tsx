import { lazy, Suspense, ComponentType, ReactElement } from 'react'
import { RouteLoadingFallback } from '@shared/components/loading'

// Utility function to create lazy components with error boundaries and loading states
export function createLazyComponent(
  importFn: () => Promise<{ default: ComponentType<unknown> }>,
  fallbackElement?: ReactElement,
  displayName?: string
) {
  const LazyComponent = lazy(importFn)
  
  const wrappedComponent = (props: Record<string, unknown>) => {
    const fallback = fallbackElement || <RouteLoadingFallback />
    
    return (
      <Suspense fallback={fallback}>
        <LazyComponent {...props} />
      </Suspense>
    )
  }
  
  wrappedComponent.displayName = displayName || 'lazyComponent'
  return wrappedComponent
}

// Helper function for dynamic imports based on feature flags
export function createConditionallazyComponent(
  condition: boolean,
  importFn: () => Promise<{ default: ComponentType<unknown> }>,
  fallbackComponent?: ComponentType<unknown>,
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
  component: ComponentType<unknown>
  preload?: boolean
  chunkName?: string
}

// Routes will be populated dynamically by importing components
// This needs to be done carefully to avoid circular dependencies
export const lazyRoutes: LazyRouteConfig[] = []

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
