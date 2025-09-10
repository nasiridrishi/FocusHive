/**
 * Advanced Responsive Hooks
 * 
 * Comprehensive set of hooks for responsive design
 * Includes breakpoint detection, device type detection, and responsive utilities
 */

import { useState, useEffect, useCallback, useMemo } from 'react'
import { useTheme, useMediaQuery } from '@mui/material'
import { breakpointValues, deviceTypes, type BreakpointKey } from '../theme/breakpoints'

// Hook for breakpoint-aware responsive behavior
export const useResponsive = () => {
  
  // Individual breakpoint checks
  const isMobile = useMediaQuery(`(max-width: ${breakpointValues.tablet - 1}px)`)
  const isMobileLg = useMediaQuery(`(min-width: ${breakpointValues.mobileLg}px) and (max-width: ${breakpointValues.tablet - 1}px)`)
  const isTablet = useMediaQuery(`(min-width: ${breakpointValues.tablet}px) and (max-width: ${breakpointValues.laptop - 1}px)`)
  const isTabletLg = useMediaQuery(`(min-width: ${breakpointValues.tabletLg}px) and (max-width: ${breakpointValues.laptop - 1}px)`)
  const isLaptop = useMediaQuery(`(min-width: ${breakpointValues.laptop}px) and (max-width: ${breakpointValues.desktop - 1}px)`)
  const isDesktop = useMediaQuery(`(min-width: ${breakpointValues.desktop}px) and (max-width: ${breakpointValues.desktopLg - 1}px)`)
  const isDesktopLg = useMediaQuery(`(min-width: ${breakpointValues.desktopLg}px) and (max-width: ${breakpointValues.desktopXl - 1}px)`)
  const isDesktopXl = useMediaQuery(`(min-width: ${breakpointValues.desktopXl}px)`)
  
  // Device type detection
  const isTouchDevice = useMediaQuery(deviceTypes.touchDevice)
  const isMouseDevice = useMediaQuery(deviceTypes.mouseDevice)
  const prefersReducedMotion = useMediaQuery(deviceTypes.reducedMotion)
  const isHighDPI = useMediaQuery(deviceTypes.highDPI)
  
  // Current active breakpoint
  const currentBreakpoint = useMemo((): BreakpointKey => {
    if (isDesktopXl) return 'desktopXl'
    if (isDesktopLg) return 'desktopLg'
    if (isDesktop) return 'desktop'
    if (isLaptop) return 'laptop'
    if (isTabletLg) return 'tabletLg'
    if (isTablet) return 'tablet'
    if (isMobileLg) return 'mobileLg'
    return 'mobile'
  }, [isDesktopXl, isDesktopLg, isDesktop, isLaptop, isTabletLg, isTablet, isMobileLg])
  
  // Viewport dimensions
  const [viewport, setViewport] = useState({
    width: typeof window !== 'undefined' ? window.innerWidth : 0,
    height: typeof window !== 'undefined' ? window.innerHeight : 0,
  })
  
  // Update viewport dimensions on resize
  useEffect(() => {
    const handleResize = () => {
      setViewport({
        width: window.innerWidth,
        height: window.innerHeight,
      })
    }
    
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])
  
  // Helper functions
  const isBreakpointUp = useCallback((breakpoint: BreakpointKey): boolean => {
    return viewport.width >= breakpointValues[breakpoint]
  }, [viewport.width])
  
  const isBreakpointDown = useCallback((breakpoint: BreakpointKey): boolean => {
    const breakpointKeys = Object.keys(breakpointValues) as BreakpointKey[]
    const index = breakpointKeys.indexOf(breakpoint)
    const nextBreakpoint = breakpointKeys[index + 1]
    if (!nextBreakpoint) return true
    return viewport.width < breakpointValues[nextBreakpoint]
  }, [viewport.width])
  
  const isBreakpointOnly = useCallback((breakpoint: BreakpointKey): boolean => {
    return isBreakpointUp(breakpoint) && isBreakpointDown(breakpoint)
  }, [isBreakpointUp, isBreakpointDown])
  
  // Responsive value selector
  const responsiveValue = useCallback(<T>(values: Partial<Record<BreakpointKey, T>>, defaultValue?: T): T => {
    const breakpointKeys = Object.keys(breakpointValues) as BreakpointKey[]
    const sortedKeys = breakpointKeys.sort((a, b) => breakpointValues[b] - breakpointValues[a])
    
    for (const key of sortedKeys) {
      if (values[key] !== undefined && isBreakpointUp(key)) {
        return values[key]!
      }
    }
    
    return defaultValue as T
  }, [isBreakpointUp])
  
  return {
    // Individual breakpoint flags
    isMobile,
    isMobileLg,
    isTablet,
    isTabletLg,
    isLaptop,
    isDesktop,
    isDesktopLg,
    isDesktopXl,
    
    // Device capabilities
    isTouchDevice,
    isMouseDevice,
    prefersReducedMotion,
    isHighDPI,
    
    // Current state
    currentBreakpoint,
    viewport,
    
    // Helper functions
    isBreakpointUp,
    isBreakpointDown,
    isBreakpointOnly,
    responsiveValue,
    
    // Convenience getters
    isMobileOrTablet: isMobile || isTablet,
    isDesktopOrLarger: isLaptop || isDesktop || isDesktopLg || isDesktopXl,
    isSmallScreen: isMobile || isMobileLg,
    isLargeScreen: isDesktop || isDesktopLg || isDesktopXl,
  }
}

// Hook for specific breakpoint detection
export const useBreakpoint = (breakpoint: BreakpointKey) => {
  const query = `(min-width: ${breakpointValues[breakpoint]}px)`
  return useMediaQuery(query)
}

// Hook for breakpoint range detection
export const useBreakpointBetween = (start: BreakpointKey, end: BreakpointKey) => {
  const query = `(min-width: ${breakpointValues[start]}px) and (max-width: ${breakpointValues[end] - 1}px)`
  return useMediaQuery(query)
}

// Hook for device type detection
export const useDeviceType = () => {
  const isTouchDevice = useMediaQuery(deviceTypes.touchDevice)
  const isMouseDevice = useMediaQuery(deviceTypes.mouseDevice)
  
  // Use direct media queries to avoid circular dependency with useResponsive
  const isMobile = useMediaQuery(`(max-width: ${breakpointValues.tablet - 1}px)`)
  const isTablet = useMediaQuery(`(min-width: ${breakpointValues.tablet}px) and (max-width: ${breakpointValues.laptop - 1}px)`)
  const isDesktopOrLarger = useMediaQuery(`(min-width: ${breakpointValues.laptop}px)`)
  
  return useMemo(() => {
    if (isMobile && isTouchDevice) return 'mobile'
    if (isTablet && isTouchDevice) return 'tablet'
    if (isDesktopOrLarger && isMouseDevice) return 'desktop'
    if (isTouchDevice) return 'touch'
    return 'unknown'
  }, [isMobile, isTablet, isDesktopOrLarger, isTouchDevice, isMouseDevice])
}

// Hook for responsive spacing
export const useResponsiveSpacing = () => {
  const theme = useTheme()
  
  // Use direct media queries to avoid circular dependency with useResponsive
  const isMobile = useMediaQuery(`(max-width: ${breakpointValues.tablet - 1}px)`)
  const isTablet = useMediaQuery(`(min-width: ${breakpointValues.tablet}px) and (max-width: ${breakpointValues.laptop - 1}px)`)
  
  // Determine current breakpoint without calling useResponsive
  const currentBreakpoint = useMemo((): BreakpointKey => {
    if (isMobile) return 'mobile'
    if (isTablet) return 'tablet'
    return 'desktop'
  }, [isMobile, isTablet])
  
  return useMemo(() => ({
    // Get spacing value for current breakpoint
    spacing: (scale: number) => theme.spacing(scale),
    
    // Get responsive spacing based on breakpoint
    responsiveSpacing: (mobileScale: number, desktopScale: number) => {
      const mobileBreakpoints = ['mobile', 'mobileLg'] as BreakpointKey[]
      const isMobileBreakpoint = mobileBreakpoints.includes(currentBreakpoint)
      return theme.spacing(isMobileBreakpoint ? mobileScale : desktopScale)
    },
    
    // Common spacing patterns
    containerPadding: currentBreakpoint === 'mobile' ? theme.spacing(2) : 
                      currentBreakpoint === 'tablet' ? theme.spacing(3) : theme.spacing(4),
    sectionSpacing: currentBreakpoint === 'mobile' ? theme.spacing(4) : 
                    currentBreakpoint === 'tablet' ? theme.spacing(6) : theme.spacing(8),
    elementSpacing: currentBreakpoint === 'mobile' ? theme.spacing(1) : theme.spacing(2),
  }), [theme, currentBreakpoint])
}

// Hook for responsive typography
export const useResponsiveTypography = () => {
  const { currentBreakpoint } = useResponsive()
  
  return useMemo(() => ({
    // Get typography variant for current breakpoint
    getVariant: (baseVariant: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'body1' | 'body2') => {
      // Responsive typography mapping
      const mapping = {
        h1: { mobile: 'h2', tablet: 'h1', desktop: 'h1' },
        h2: { mobile: 'h3', tablet: 'h2', desktop: 'h2' },
        h3: { mobile: 'h4', tablet: 'h3', desktop: 'h3' },
        h4: { mobile: 'h5', tablet: 'h4', desktop: 'h4' },
        h5: { mobile: 'h6', tablet: 'h5', desktop: 'h5' },
        h6: { mobile: 'h6', tablet: 'h6', desktop: 'h6' },
        body1: { mobile: 'body2', tablet: 'body1', desktop: 'body1' },
        body2: { mobile: 'body2', tablet: 'body2', desktop: 'body2' },
      }
      
      const variantMap = mapping[baseVariant]
      if (!variantMap) return baseVariant
      
      if (['mobile', 'mobileLg'].includes(currentBreakpoint)) return variantMap.mobile
      if (['tablet', 'tabletLg'].includes(currentBreakpoint)) return variantMap.tablet
      return variantMap.desktop
    },
    
    // Get responsive font size
    getFontSize: (mobile: string, desktop: string) => {
      const isMobileBreakpoint = ['mobile', 'mobileLg'].includes(currentBreakpoint)
      return isMobileBreakpoint ? mobile : desktop
    },
  }), [currentBreakpoint])
}

// Hook for orientation detection
export const useOrientation = () => {
  const [orientation, setOrientation] = useState<'portrait' | 'landscape'>(() => {
    if (typeof window === 'undefined') return 'portrait'
    return window.innerHeight > window.innerWidth ? 'portrait' : 'landscape'
  })
  
  useEffect(() => {
    const handleOrientationChange = () => {
      setOrientation(window.innerHeight > window.innerWidth ? 'portrait' : 'landscape')
    }
    
    window.addEventListener('resize', handleOrientationChange)
    window.addEventListener('orientationchange', handleOrientationChange)
    
    return () => {
      window.removeEventListener('resize', handleOrientationChange)
      window.removeEventListener('orientationchange', handleOrientationChange)
    }
  }, [])
  
  return orientation
}

// Hook for scroll direction detection
export const useScrollDirection = () => {
  const [scrollDirection, setScrollDirection] = useState<'up' | 'down' | null>(null)
  const [lastScrollY, setLastScrollY] = useState(0)
  
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY
      
      if (currentScrollY > lastScrollY && currentScrollY > 100) {
        setScrollDirection('down')
      } else if (currentScrollY < lastScrollY) {
        setScrollDirection('up')
      }
      
      setLastScrollY(currentScrollY)
    }
    
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [lastScrollY])
  
  return scrollDirection
}

// Hook for reduced motion preference
export const useReducedMotion = () => {
  return useMediaQuery(deviceTypes.reducedMotion)
}

