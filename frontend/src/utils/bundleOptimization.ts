/**
 * Bundle Optimization Utilities
 * 
 * Centralized utilities for managing code splitting, preloading, and bundle optimization
 */

// Import preloader functions
import { preloadCriticalRoutes, routePreloaders } from '@app/routes/lazyRouteUtils'
import { preloadCommonIcons } from '@shared/components/dynamic-icon'
import { preloadChartLibrary } from '@shared/components/lazy-charts'
import { preloadDatePickers } from '@shared/components/lazy-date-pickers/datePickerUtils'
import { preloadHeavyFeatures as _preloadHeavyFeatures, featurePreloaders as _featurePreloaders } from '@shared/components/lazy-features/featureUtils'
import { libraryPreloader as _libraryPreloader, smartPreloader as _smartPreloader } from '@shared/utils/dynamicImports'

export interface PreloadOptions {
  /**
   * Delay before starting preloading (to not interfere with critical rendering)
   */
  delay?: number
  
  /**
   * Preload critical routes immediately
   */
  criticalRoutes?: boolean
  
  /**
   * Preload common icons
   */
  commonIcons?: boolean
  
  /**
   * Preload chart library
   */
  charts?: boolean
  
  /**
   * Preload date pickers
   */
  datePickers?: boolean
  
  /**
   * Preload based on user navigation patterns
   */
  adaptive?: boolean
  
  /**
   * Preload heavy features
   */
  heavyFeatures?: boolean
  
  /**
   * Preload utility libraries
   */
  utilityLibs?: boolean
  
  /**
   * Use smart preloading based on connection
   */
  smartPreloading?: boolean
}

export interface BundleAnalyticsData {
  totalChunks: number
  chunkSizes: Record<string, number>
  loadTimes: Record<string, number>
  cacheHits: number
  cacheMisses: number
  preloadSuccess: number
  preloadFailures: number
}

// Bundle analytics tracking
class BundleAnalyticsTracker {
  private static instance: BundleAnalyticsTracker
  private analytics: BundleAnalyticsData = {
    totalChunks: 0,
    chunkSizes: {},
    loadTimes: {},
    cacheHits: 0,
    cacheMisses: 0,
    preloadSuccess: 0,
    preloadFailures: 0
  }

  static getInstance(): BundleAnalyticsTracker {
    if (!BundleAnalyticsTracker.instance) {
      BundleAnalyticsTracker.instance = new BundleAnalyticsTracker()
    }
    return BundleAnalyticsTracker.instance
  }

  recordChunkLoad(chunkName: string, size: number, loadTime: number) {
    this.analytics.chunkSizes[chunkName] = size
    this.analytics.loadTimes[chunkName] = loadTime
    this.analytics.totalChunks++
  }

  recordCacheHit() {
    this.analytics.cacheHits++
  }

  recordCacheMiss() {
    this.analytics.cacheMisses++
  }

  recordPreloadSuccess() {
    this.analytics.preloadSuccess++
  }

  recordPreloadFailure() {
    this.analytics.preloadFailures++
  }

  getAnalytics() {
    return { ...this.analytics }
  }

  getCacheEfficiency() {
    const total = this.analytics.cacheHits + this.analytics.cacheMisses
    return total > 0 ? (this.analytics.cacheHits / total) * 100 : 0
  }

  getPreloadEfficiency() {
    const total = this.analytics.preloadSuccess + this.analytics.preloadFailures
    return total > 0 ? (this.analytics.preloadSuccess / total) * 100 : 0
  }
}

const analytics = BundleAnalyticsTracker.getInstance()

/**
 * Intelligent preloader that manages feature preloading based on user behavior
 */
export class FeaturePreloader {
  private preloadedFeatures = new Set<string>()
  private userInteractions: string[] = []
  private preloadQueue: Array<{ feature: string; priority: number; loader: () => Promise<unknown> }> = []

  /**
   * Initialize the preloader with user-defined options
   */
  async initialize(options: PreloadOptions = {}) {
    const {
      delay = 2000,
      criticalRoutes = true,
      commonIcons = true,
      charts = false,
      datePickers = false,
      adaptive = true
    } = options

    // Preload critical resources immediately
    if (criticalRoutes) {
      preloadCriticalRoutes()
    }

    // Schedule non-critical preloads
    setTimeout(() => {
      if (commonIcons) {
        this.queuePreload('icons', 1, () => {
          preloadCommonIcons()
          return Promise.resolve()
        })
      }
      
      if (charts) {
        this.queuePreload('charts', 2, () => {
          preloadChartLibrary()
          return Promise.resolve()
        })
      }
      
      if (datePickers) {
        this.queuePreload('datePickers', 3, () => {
          preloadDatePickers()
          return Promise.resolve()
        })
      }
      
      if (adaptive) {
        this.startAdaptivePreloading()
      }
      
      // Process the preload queue
      this.processPreloadQueue()
    }, delay)
  }

  /**
   * Queue a feature for preloading
   */
  queuePreload(feature: string, priority: number, loader: () => Promise<unknown>) {
    if (this.preloadedFeatures.has(feature)) {
      return
    }

    this.preloadQueue.push({ feature, priority, loader })
    this.preloadQueue.sort((a, b) => a.priority - b.priority)
  }

  /**
   * Process the preload queue
   */
  private async processPreloadQueue() {
    for (const { feature, loader } of this.preloadQueue) {
      if (this.preloadedFeatures.has(feature)) {
        continue
      }

      try {
        const startTime = performance.now()
        await loader()
        const loadTime = performance.now() - startTime
        
        this.preloadedFeatures.add(feature)
        analytics.recordPreloadSuccess()
        
        // Preload successful - logging only in development
        if (import.meta.env.DEV) {
          console.log(`[Bundle] Preloaded ${feature} in ${loadTime.toFixed(2)}ms`)
        }
      } catch (error) {
        analytics.recordPreloadFailure()
        // Log preload failures only in development
        if (import.meta.env.DEV) {
          console.warn(`[Bundle] Failed to preload ${feature}:`, error)
        }
      }
    }
  }

  /**
   * Track user interactions for adaptive preloading
   */
  trackUserInteraction(interaction: string) {
    this.userInteractions.push(interaction)
    
    // Keep only last 20 interactions
    if (this.userInteractions.length > 20) {
      this.userInteractions.shift()
    }

    // Trigger adaptive preloading
    this.adaptivePreload(interaction)
  }

  /**
   * Adaptive preloading based on user behavior patterns
   */
  private adaptivePreload(currentInteraction: string) {
    // Preload authentication routes if user is on home page
    if (currentInteraction === 'home-page-visit' && !this.preloadedFeatures.has('auth-routes')) {
      this.queuePreload('auth-routes', 1, () => {
        routePreloaders.preloadAuth()
        return Promise.resolve()
      })
    }
    
    // Preload main app if user logged in
    if (currentInteraction === 'login-success' && !this.preloadedFeatures.has('main-app')) {
      this.queuePreload('main-app', 1, () => {
        routePreloaders.preloadMainApp()
        return Promise.resolve()
      })
    }
    
    // Preload heavy features if user is exploring the app
    if (
      (currentInteraction === 'dashboard-visit' || currentInteraction === 'menu-open') && 
      !this.preloadedFeatures.has('heavy-features')
    ) {
      this.queuePreload('heavy-features', 2, () => {
        routePreloaders.preloadHeavyFeatures()
        return Promise.resolve()
      })
    }
  }

  /**
   * Start adaptive preloading based on connection and device capabilities
   */
  private startAdaptivePreloading() {
    // Check connection quality
    if ('connection' in navigator) {
      const connection = (navigator as { connection?: { effectiveType?: string; saveData?: boolean } }).connection
      if (connection) {
        // Only preload on good connections
        if (connection.effectiveType === '4g' && !connection.saveData) {
          this.queuePreload('charts', 2, () => {
            preloadChartLibrary()
            return Promise.resolve()
          })
          this.queuePreload('datePickers', 3, () => {
            preloadDatePickers()
            return Promise.resolve()
          })
        }
      }
    }

    // Check device memory (if available)
    if ('deviceMemory' in navigator) {
      const memory = (navigator as { deviceMemory?: number }).deviceMemory
      if (memory >= 4) { // 4GB+ devices
        // Preload more aggressively on high-memory devices
        this.queuePreload('heavy-features', 1, () => {
          routePreloaders.preloadHeavyFeatures()
          return Promise.resolve()
        })
      }
    }
  }

  /**
   * Get preloading statistics
   */
  getPreloadStats() {
    return {
      preloadedFeatures: Array.from(this.preloadedFeatures),
      queueLength: this.preloadQueue.length,
      userInteractions: this.userInteractions.slice(-10), // Last 10 interactions
      analytics: analytics.getAnalytics()
    }
  }
}

// Global preloader instance
export const featurePreloader = new FeaturePreloader()

/**
 * Resource hints for modern browsers
 */
export function addResourceHints() {
  // Add DNS prefetch for external resources
  const dnsPrefetch = [
    '//fonts.googleapis.com',
    '//fonts.gstatic.com'
  ]

  dnsPrefetch.forEach(domain => {
    const link = document.createElement('link')
    link.rel = 'dns-prefetch'
    link.href = domain
    document.head.appendChild(link)
  })

  // Add preconnect for critical third-party resources
  const preconnect = [
    'https://fonts.googleapis.com',
    'https://fonts.gstatic.com'
  ]

  preconnect.forEach(url => {
    const link = document.createElement('link')
    link.rel = 'preconnect'
    link.href = url
    link.crossOrigin = 'anonymous'
    document.head.appendChild(link)
  })
}

/**
 * Bundle size monitoring utility
 */
export function monitorBundlePerformance() {
  if ('performance' in window && 'navigation' in performance) {
    // Monitor loading performance
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'navigation') {
          const navEntry = entry as PerformanceNavigationTiming
          // Log performance metrics only in development
          if (import.meta.env.DEV) {
            console.log('[Bundle] Initial page load:', {
              duration: navEntry.loadEventEnd - navEntry.fetchStart,
              domContentLoaded: navEntry.domContentLoadedEventEnd - navEntry.fetchStart,
              firstByte: navEntry.responseStart - navEntry.fetchStart
            })
          }
        }
      }
    })
    
    observer.observe({ entryTypes: ['navigation'] })
  }
}

/**
 * Initialize bundle optimization
 * Call this in your main App component
 */
export async function initializeBundleOptimization(options?: PreloadOptions) {
  // Add resource hints
  addResourceHints()
  
  // Monitor performance
  monitorBundlePerformance()
  
  // Initialize feature preloader
  await featurePreloader.initialize(options)
  
  // Log initialization only in development
  if (import.meta.env.DEV) {
    console.log('[Bundle] Optimization initialized')
  }
}

/**
 * Development utilities for bundle analysis
 */
export const devUtils = {
  /**
   * Log current bundle statistics
   */
  logBundleStats() {
    if (import.meta.env.DEV) {
      console.group('[Bundle Stats]')
      console.log('Preloader Stats:', featurePreloader.getPreloadStats())
      console.log('Cache Efficiency:', analytics.getCacheEfficiency().toFixed(2) + '%')
      console.log('Preload Efficiency:', analytics.getPreloadEfficiency().toFixed(2) + '%')
      console.groupEnd()
    }
  },

  /**
   * Trigger specific preloads for testing
   */
  triggerPreload(feature: 'auth' | 'main-app' | 'heavy-features' | 'charts' | 'icons') {
    if (import.meta.env.DEV) {
      switch (feature) {
        case 'auth':
          routePreloaders.preloadAuth()
          break
        case 'main-app':
          routePreloaders.preloadMainApp()
          break
        case 'heavy-features':
          routePreloaders.preloadHeavyFeatures()
          break
        case 'charts':
          preloadChartLibrary()
          break
        case 'icons':
          preloadCommonIcons()
          break
      }
      // Log preload trigger only in development
      if (import.meta.env.DEV) {
        console.log(`[Bundle] Triggered preload for: ${feature}`)
      }
    }
  }
}

// Make dev utils available globally in development
if (import.meta.env.DEV) {
  (window as { bundleDevUtils?: typeof devUtils }).bundleDevUtils = devUtils
}