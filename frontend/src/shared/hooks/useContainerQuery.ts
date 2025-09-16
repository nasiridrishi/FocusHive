/**
 * Container Query Hook
 *
 * Modern responsive design using container queries instead of viewport-based media queries
 * Enables component-level responsiveness based on container size
 */

import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {type ContainerBreakpointKey, containerBreakpoints} from '../theme/breakpoints'

interface ContainerDimensions {
  width: number
  height: number
}

interface ContainerQueryResult {
  // Current container dimensions
  dimensions: ContainerDimensions

  // Breakpoint checks
  isXs: boolean
  isSm: boolean
  isMd: boolean
  isLg: boolean
  isXl: boolean
  isXxl: boolean

  // Current active breakpoint
  currentBreakpoint: ContainerBreakpointKey

  // Helper functions
  isBreakpointUp: (breakpoint: ContainerBreakpointKey) => boolean
  isBreakpointDown: (breakpoint: ContainerBreakpointKey) => boolean
  isBreakpointOnly: (breakpoint: ContainerBreakpointKey) => boolean

  // Container ref to attach to the element
  containerRef: React.RefObject<HTMLElement>

  // Responsive value selector
  responsiveValue: <T>(values: Partial<Record<ContainerBreakpointKey, T>>, defaultValue?: T) => T
}

/**
 * Hook for container-based responsive behavior
 *
 * @param debounceMs - Debounce resize events (default: 100ms)
 * @returns Container query utilities and ref
 */
export const useContainerQuery = (debounceMs = 100): ContainerQueryResult => {
  const containerRef = useRef<HTMLElement>(null)
  const [dimensions, setDimensions] = useState<ContainerDimensions>({width: 0, height: 0})
  const resizeTimeoutRef = useRef<number>()

  // Update dimensions when container size changes
  const updateDimensions = useCallback(() => {
    if (!containerRef.current) return

    const {width, height} = containerRef.current.getBoundingClientRect()
    setDimensions({width, height})
  }, [])

  // Debounced update function
  const debouncedUpdate = useCallback(() => {
    if (resizeTimeoutRef.current) {
      clearTimeout(resizeTimeoutRef.current)
    }

    resizeTimeoutRef.current = window.setTimeout(updateDimensions, debounceMs)
  }, [updateDimensions, debounceMs])

  // Set up resize observer
  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    // Initial measurement
    updateDimensions()

    // Use ResizeObserver for more efficient container size tracking
    if ('ResizeObserver' in window) {
      const resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const {width, height} = entry.contentRect
          setDimensions({width, height})
        }
      })

      resizeObserver.observe(container)

      return () => {
        resizeObserver.unobserve(container)
        resizeObserver.disconnect()
      }
    }

    // Fallback to window resize for older browsers (only if container doesn't exist yet)
    return () => {
      if (resizeTimeoutRef.current) {
        clearTimeout(resizeTimeoutRef.current)
      }
    }
  }, [debouncedUpdate, updateDimensions])

  // Calculate current breakpoint based on container width
  const currentBreakpoint = useMemo((): ContainerBreakpointKey => {
    const {width} = dimensions

    if (width >= containerBreakpoints.xxl) return 'xxl'
    if (width >= containerBreakpoints.xl) return 'xl'
    if (width >= containerBreakpoints.lg) return 'lg'
    if (width >= containerBreakpoints.md) return 'md'
    if (width >= containerBreakpoints.sm) return 'sm'
    return 'xs'
  }, [dimensions])

  // Individual breakpoint flags
  const isXs = dimensions.width >= containerBreakpoints.xs && dimensions.width < containerBreakpoints.sm
  const isSm = dimensions.width >= containerBreakpoints.sm && dimensions.width < containerBreakpoints.md
  const isMd = dimensions.width >= containerBreakpoints.md && dimensions.width < containerBreakpoints.lg
  const isLg = dimensions.width >= containerBreakpoints.lg && dimensions.width < containerBreakpoints.xl
  const isXl = dimensions.width >= containerBreakpoints.xl && dimensions.width < containerBreakpoints.xxl
  const isXxl = dimensions.width >= containerBreakpoints.xxl

  // Helper functions
  const isBreakpointUp = useCallback((breakpoint: ContainerBreakpointKey): boolean => {
    return dimensions.width >= containerBreakpoints[breakpoint]
  }, [dimensions.width])

  const isBreakpointDown = useCallback((breakpoint: ContainerBreakpointKey): boolean => {
    const breakpointKeys = Object.keys(containerBreakpoints) as ContainerBreakpointKey[]
    const index = breakpointKeys.indexOf(breakpoint)
    const nextBreakpoint = breakpointKeys[index + 1]
    if (!nextBreakpoint) return true
    return dimensions.width < containerBreakpoints[nextBreakpoint]
  }, [dimensions.width])

  const isBreakpointOnly = useCallback((breakpoint: ContainerBreakpointKey): boolean => {
    return isBreakpointUp(breakpoint) && isBreakpointDown(breakpoint)
  }, [isBreakpointUp, isBreakpointDown])

  // Responsive value selector based on container size
  const responsiveValue = useCallback(<T>(values: Partial<Record<ContainerBreakpointKey, T>>, defaultValue?: T): T => {
    const breakpointKeys = Object.keys(containerBreakpoints) as ContainerBreakpointKey[]
    const sortedKeys = breakpointKeys.sort((a, b) => containerBreakpoints[b] - containerBreakpoints[a])

    for (const key of sortedKeys) {
      if (values[key] !== undefined && isBreakpointUp(key)) {
        return values[key] || values[sortedKeys[0]] || undefined
      }
    }

    return defaultValue as T
  }, [isBreakpointUp])

  return {
    dimensions,
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    isXxl,
    currentBreakpoint,
    isBreakpointUp,
    isBreakpointDown,
    isBreakpointOnly,
    containerRef,
    responsiveValue,
  }
}

/**
 * Hook for container aspect ratio
 *
 * @param targetRatio - Target aspect ratio (width/height)
 * @returns Aspect ratio information and container ref
 */
export const useContainerAspectRatio = (targetRatio?: number) => {
  const {dimensions, containerRef} = useContainerQuery()

  const aspectRatio = useMemo(() => {
    if (dimensions.height === 0) return 0
    return dimensions.width / dimensions.height
  }, [dimensions])

  const isTargetRatio = useMemo(() => {
    if (!targetRatio) return false
    return Math.abs(aspectRatio - targetRatio) < 0.1 // Allow small tolerance
  }, [aspectRatio, targetRatio])

  return {
    aspectRatio,
    isTargetRatio,
    isLandscape: aspectRatio > 1,
    isPortrait: aspectRatio < 1,
    isSquare: Math.abs(aspectRatio - 1) < 0.1,
    dimensions,
    containerRef,
  }
}

/**
 * Hook for container-based responsive columns
 *
 * @param breakpoints - Column counts for different container sizes
 * @returns Current column count and container ref
 */
export const useContainerColumns = (breakpoints: Partial<Record<ContainerBreakpointKey, number>>) => {
  const {responsiveValue, containerRef, currentBreakpoint} = useContainerQuery()

  const columns = responsiveValue(breakpoints, 1)

  return {
    columns,
    currentBreakpoint,
    containerRef,
  }
}

/**
 * Hook for container-based grid layout
 *
 * @param options - Grid configuration options
 * @returns Grid layout properties and container ref
 */
export const useContainerGrid = (options: {
  columns?: Partial<Record<ContainerBreakpointKey, number>>
  gap?: Partial<Record<ContainerBreakpointKey, number>>
  minItemWidth?: number
}) => {
  const {responsiveValue, containerRef, dimensions} = useContainerQuery()

  const columns = responsiveValue(options.columns || {xs: 1, sm: 2, md: 3, lg: 4}, 1)
  const gap = responsiveValue(options.gap || {xs: 8, sm: 12, md: 16}, 8)

  // Calculate optimal columns based on minimum item width
  const optimalColumns = useMemo(() => {
    if (!options.minItemWidth || dimensions.width === 0) return columns

    const availableWidth = dimensions.width - (gap * (columns - 1))
    const maxColumns = Math.floor(availableWidth / options.minItemWidth)

    return Math.min(columns, Math.max(1, maxColumns))
  }, [columns, gap, dimensions.width, options.minItemWidth])

  // Calculate item width based on columns and gap
  const itemWidth = useMemo(() => {
    if (dimensions.width === 0) return '100%'

    const totalGap = gap * (optimalColumns - 1)
    const availableWidth = dimensions.width - totalGap
    const itemWidthPx = availableWidth / optimalColumns

    return `${Math.floor(itemWidthPx)}px`
  }, [dimensions.width, gap, optimalColumns])

  return {
    columns: optimalColumns,
    gap,
    itemWidth,
    containerRef,
    gridStyles: {
      display: 'grid',
      gridTemplateColumns: `repeat(${optimalColumns}, 1fr)`,
      gap: `${gap}px`,
    },
  }
}

/**
 * Hook for container-based responsive text scaling
 *
 * @param baseSize - Base font size in pixels
 * @param scaleFactor - Scaling factor based on container width
 * @returns Responsive font size and container ref
 */
export const useContainerTextScale = (baseSize = 16, scaleFactor = 0.02) => {
  const {dimensions, containerRef} = useContainerQuery()

  const fontSize = useMemo(() => {
    if (dimensions.width === 0) return baseSize

    // Scale font size based on container width
    const scaledSize = baseSize + (dimensions.width * scaleFactor)

    // Clamp between reasonable bounds
    return Math.max(12, Math.min(24, scaledSize))
  }, [dimensions.width, baseSize, scaleFactor])

  return {
    fontSize: `${fontSize}px`,
    containerRef,
  }
}

