/**
 * Responsive Utility Hooks
 *
 * Collection of utility hooks for advanced responsive behavior
 * Includes performance optimizations, adaptive loading, and smart UI patterns
 */

import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {useResponsive} from './useResponsive'

// Hook for viewport visibility (Intersection Observer)
export const useViewportVisibility = (options: IntersectionObserverInit = {}) => {
  const [isVisible, setIsVisible] = useState(false)
  const [hasBeenVisible, setHasBeenVisible] = useState(false)
  const elementRef = useRef<Element>(null)

  useEffect(() => {
    const element = elementRef.current
    if (!element) return

    const observer = new IntersectionObserver(
        ([entry]) => {
          setIsVisible(entry.isIntersecting)
          if (entry.isIntersecting && !hasBeenVisible) {
            setHasBeenVisible(true)
          }
        },
        {
          threshold: 0.1,
          rootMargin: '50px',
          ...options,
        }
    )

    observer.observe(element)

    return () => {
      observer.unobserve(element)
      observer.disconnect()
    }
  }, [hasBeenVisible, options])

  return {
    isVisible,
    hasBeenVisible,
    elementRef,
  }
}

// Hook for lazy loading with responsive image support
export const useLazyImage = (src: string, responsiveSizes?: { [key: string]: string }) => {
  const [imageSrc, setImageSrc] = useState<string>('')
  const [isLoaded, setIsLoaded] = useState(false)
  const [isError, setIsError] = useState(false)
  const {isVisible, elementRef} = useViewportVisibility({threshold: 0.1})
  const {currentBreakpoint} = useResponsive()

  // Get responsive image source
  const getResponsiveSource = useCallback(() => {
    if (responsiveSizes && responsiveSizes[currentBreakpoint]) {
      return responsiveSizes[currentBreakpoint]
    }
    return src
  }, [src, responsiveSizes, currentBreakpoint])

  useEffect(() => {
    if (!isVisible) return

    const imageToLoad = getResponsiveSource()
    if (!imageToLoad || imageSrc === imageToLoad) return

    const img = new Image()

    img.onload = () => {
      setImageSrc(imageToLoad)
      setIsLoaded(true)
      setIsError(false)
    }

    img.onerror = () => {
      setIsError(true)
      setIsLoaded(false)
    }

    img.src = imageToLoad
  }, [isVisible, getResponsiveSource, imageSrc])

  return {
    src: imageSrc,
    isLoaded,
    isError,
    elementRef,
  }
}

// Hook for adaptive loading based on connection speed
export const useAdaptiveLoading = () => {
  const [connectionSpeed, setConnectionSpeed] = useState<'slow' | 'fast' | 'unknown'>('unknown')
  const [dataUsageMode, setDataUsageMode] = useState<'normal' | 'reduced'>('normal')

  useEffect(() => {
    // Check for Network Information API
    if ('connection' in navigator) {
      const connection = (navigator as Navigator & {
        connection: {
          effectiveType: string;
          saveData: boolean;
          addEventListener: (event: string, listener: () => void) => void;
          removeEventListener: (event: string, listener: () => void) => void;
        };
      }).connection

      const updateConnectionInfo = (): void => {
        const effectiveType = connection.effectiveType
        const saveData = connection.saveData

        // Determine connection speed
        if (effectiveType === '4g') {
          setConnectionSpeed('fast')
        } else if (effectiveType === '3g' || effectiveType === '2g') {
          setConnectionSpeed('slow')
        } else {
          setConnectionSpeed('unknown')
        }

        // Respect data saver mode
        setDataUsageMode(saveData ? 'reduced' : 'normal')
      }

      updateConnectionInfo()
      connection.addEventListener('change', updateConnectionInfo)

      return () => {
        connection.removeEventListener('change', updateConnectionInfo)
      }
    }
  }, [])

  return {
    connectionSpeed,
    dataUsageMode,
    shouldLoadHighQualityImages: connectionSpeed === 'fast' && dataUsageMode === 'normal',
    shouldPreloadContent: connectionSpeed === 'fast',
    shouldReduceAnimations: connectionSpeed === 'slow' || dataUsageMode === 'reduced',
  }
}

// Hook for smart component loading based on device capabilities
export const useSmartLoading = () => {
  const {isTouchDevice, prefersReducedMotion} = useResponsive()
  const {connectionSpeed, dataUsageMode} = useAdaptiveLoading()
  const [deviceMemory, setDeviceMemory] = useState<number>(4) // Default to 4GB

  useEffect(() => {
    // Check device memory if available
    if ('deviceMemory' in navigator) {
      setDeviceMemory((navigator as Navigator & { deviceMemory: number }).deviceMemory)
    }
  }, [])

  const capabilities = useMemo(() => ({
    // Device capability flags
    isLowEndDevice: deviceMemory <= 2,
    isHighEndDevice: deviceMemory >= 8,
    hasSlowConnection: connectionSpeed === 'slow',
    hasFastConnection: connectionSpeed === 'fast',
    prefersReducedData: dataUsageMode === 'reduced',
    prefersReducedMotion,
    isTouchDevice,

    // Loading strategy recommendations
    shouldLazyLoad: true, // Always lazy load for performance
    shouldPreloadCritical: connectionSpeed === 'fast' && deviceMemory >= 4,
    shouldUseImagePlaceholders: connectionSpeed === 'slow' || deviceMemory <= 2,
    shouldReduceImageQuality: connectionSpeed === 'slow' || dataUsageMode === 'reduced',
    shouldDisableAnimations: prefersReducedMotion || connectionSpeed === 'slow' || deviceMemory <= 2,
    shouldUseLightweightComponents: deviceMemory <= 2 || connectionSpeed === 'slow',
    shouldEnableVirtualization: deviceMemory <= 4, // Use virtual scrolling on lower-end devices
  }), [deviceMemory, connectionSpeed, dataUsageMode, prefersReducedMotion, isTouchDevice])

  return capabilities
}

// Hook for responsive grid auto-sizing
export const useResponsiveGrid = (options: {
  minItemWidth: number
  maxColumns?: number
  gap?: number
}) => {
  const {viewport} = useResponsive()
  const {minItemWidth, maxColumns = 12, gap = 16} = options

  const gridConfig = useMemo(() => {
    const availableWidth = viewport.width - (gap * 2) // Account for container padding
    const itemsPerRow = Math.floor(availableWidth / (minItemWidth + gap))
    const columns = Math.min(itemsPerRow, maxColumns)
    const actualItemWidth = (availableWidth - (gap * (columns - 1))) / columns

    return {
      columns: Math.max(1, columns),
      itemWidth: actualItemWidth,
      gap,
      gridTemplate: `repeat(${Math.max(1, columns)}, 1fr)`,
    }
  }, [viewport.width, minItemWidth, maxColumns, gap])

  return gridConfig
}

// Hook for touch gesture support
export const useTouchGestures = (element: React.RefObject<HTMLElement>) => {
  const [gesture, setGesture] = useState<{
    type: 'swipe' | 'pinch' | 'tap' | null
    direction?: 'left' | 'right' | 'up' | 'down'
    scale?: number
    deltaX?: number
    deltaY?: number
  }>({type: null})

  useEffect(() => {
    const el = element.current
    if (!el || !('ontouchstart' in window)) return

    let startX = 0
    let startY = 0
    let startTime = 0
    let initialDistance = 0

    const handleTouchStart = (e: TouchEvent): void => {
      if (e.touches.length === 1) {
        startX = e.touches[0].clientX
        startY = e.touches[0].clientY
        startTime = Date.now()
      } else if (e.touches.length === 2) {
        const dx = e.touches[0].clientX - e.touches[1].clientX
        const dy = e.touches[0].clientY - e.touches[1].clientY
        initialDistance = Math.sqrt(dx * dx + dy * dy)
      }
    }

    const handleTouchEnd = (e: TouchEvent): void => {
      if (e.changedTouches.length === 1) {
        const endX = e.changedTouches[0].clientX
        const endY = e.changedTouches[0].clientY
        const endTime = Date.now()

        const deltaX = endX - startX
        const deltaY = endY - startY
        const deltaTime = endTime - startTime

        // Detect tap
        if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10 && deltaTime < 300) {
          setGesture({type: 'tap', deltaX, deltaY})
          return
        }

        // Detect swipe
        if (deltaTime < 500 && (Math.abs(deltaX) > 50 || Math.abs(deltaY) > 50)) {
          let direction: 'left' | 'right' | 'up' | 'down'

          if (Math.abs(deltaX) > Math.abs(deltaY)) {
            direction = deltaX > 0 ? 'right' : 'left'
          } else {
            direction = deltaY > 0 ? 'down' : 'up'
          }

          setGesture({type: 'swipe', direction, deltaX, deltaY})
        }
      }
    }

    const handleTouchMove = (e: TouchEvent): void => {
      if (e.touches.length === 2) {
        const dx = e.touches[0].clientX - e.touches[1].clientX
        const dy = e.touches[0].clientY - e.touches[1].clientY
        const distance = Math.sqrt(dx * dx + dy * dy)
        const scale = distance / initialDistance

        setGesture({type: 'pinch', scale})
      }
    }

    el.addEventListener('touchstart', handleTouchStart, {passive: true})
    el.addEventListener('touchend', handleTouchEnd, {passive: true})
    el.addEventListener('touchmove', handleTouchMove, {passive: true})

    return () => {
      el.removeEventListener('touchstart', handleTouchStart)
      el.removeEventListener('touchend', handleTouchEnd)
      el.removeEventListener('touchmove', handleTouchMove)
    }
  }, [element])

  // Clear gesture after a delay
  useEffect(() => {
    if (gesture.type) {
      const timer = setTimeout(() => {
        setGesture({type: null})
      }, 100)

      return () => clearTimeout(timer)
    }
  }, [gesture])

  return gesture
}

// Hook for responsive modal behavior
export const useResponsiveModal = () => {
  const {isMobile, viewport} = useResponsive()

  const modalProps = useMemo(() => ({
    fullScreen: isMobile,
    maxWidth: isMobile ? false : 'md' as const,
    fullWidth: true,
    PaperProps: {
      sx: {
        ...(isMobile && {
          margin: 0,
          width: '100%',
          height: '100%',
          maxHeight: '100%',
          borderRadius: 0,
        }),
        ...(!isMobile && {
          borderRadius: 2,
          maxHeight: viewport.height * 0.9,
        }),
      },
    },
  }), [isMobile, viewport.height])

  return modalProps
}

// Hook for dynamic viewport height (fixes mobile viewport issues)
export const useDynamicViewportHeight = () => {
  const [viewportHeight, setViewportHeight] = useState(() => {
    if (typeof window !== 'undefined') {
      return window.innerHeight
    }
    return 0
  })

  useEffect(() => {
    const updateHeight = (): void => {
      // Use visual viewport API if available (better for mobile)
      if (window.visualViewport) {
        setViewportHeight(window.visualViewport.height)
      } else {
        setViewportHeight(window.innerHeight)
      }
    }

    updateHeight()

    // Listen to both resize and visual viewport changes
    window.addEventListener('resize', updateHeight)
    if (window.visualViewport) {
      window.visualViewport.addEventListener('resize', updateHeight)
    }

    return () => {
      window.removeEventListener('resize', updateHeight)
      if (window.visualViewport) {
        window.visualViewport.removeEventListener('resize', updateHeight)
      }
    }
  }, [])

  return {
    height: viewportHeight,
    // CSS custom property for dynamic viewport height
    cssVar: `${viewportHeight}px`,
  }
}

