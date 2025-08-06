/**
 * Advanced Responsive Breakpoints Configuration
 * 
 * Mobile-first approach with comprehensive device coverage
 * Based on Material Design 3 guidelines and industry standards
 */

// Custom breakpoint values for FocusHive
export const breakpointValues = {
  // Mobile devices
  mobile: 0,      // 0px+     - Small phones
  mobileLg: 390,  // 390px+   - Large phones (iPhone 14 Pro, etc.)
  
  // Tablet devices  
  tablet: 640,    // 640px+   - Small tablets, large phones landscape
  tabletLg: 840,  // 840px+   - Large tablets
  
  // Desktop devices
  laptop: 1024,   // 1024px+  - Small laptops, tablet landscape
  desktop: 1280,  // 1280px+  - Standard desktop
  desktopLg: 1440, // 1440px+ - Large desktop
  desktopXl: 1920, // 1920px+ - Extra large desktop, 4K displays
} as const

// TypeScript type for breakpoint keys
export type BreakpointKey = keyof typeof breakpointValues

// Responsive design patterns
export const responsivePatterns = {
  // Content max-widths for readability
  contentMaxWidths: {
    mobile: '100%',
    tablet: '640px', 
    laptop: '840px',
    desktop: '1200px',
    desktopLg: '1400px',
    desktopXl: '1600px',
  },
  
  // Sidebar widths
  sidebarWidths: {
    mobile: 280,      // Full-width drawer on mobile
    tablet: 280,      // Standard drawer width
    desktop: 320,     // Wider drawer for desktop
    desktopLg: 360,   // Extra wide for large screens
  },
  
  // Grid columns for different layouts
  gridColumns: {
    mobile: 4,        // 4-column grid on mobile
    tablet: 8,        // 8-column grid on tablet
    desktop: 12,      // 12-column grid on desktop
    desktopLg: 16,    // 16-column grid on large screens
  },
  
  // Header heights for different devices
  headerHeights: {
    mobile: 56,       // Compact header on mobile
    tablet: 64,       // Standard header height
    desktop: 72,      // Taller header on desktop
    desktopLg: 80,    // Extra tall for large screens
  },
} as const

// Media query helpers for styled-components and manual usage
export const mediaQueries = {
  up: (breakpoint: BreakpointKey) => `@media (min-width: ${breakpointValues[breakpoint]}px)`,
  down: (breakpoint: BreakpointKey) => {
    const keys = Object.keys(breakpointValues) as BreakpointKey[]
    const index = keys.indexOf(breakpoint)
    const nextBreakpoint = keys[index + 1]
    if (!nextBreakpoint) return '@media (max-width: 9999px)' // Fallback
    return `@media (max-width: ${breakpointValues[nextBreakpoint] - 1}px)`
  },
  between: (start: BreakpointKey, end: BreakpointKey) => 
    `@media (min-width: ${breakpointValues[start]}px) and (max-width: ${breakpointValues[end] - 1}px)`,
  only: (breakpoint: BreakpointKey) => {
    const keys = Object.keys(breakpointValues) as BreakpointKey[]
    const index = keys.indexOf(breakpoint)
    const nextBreakpoint = keys[index + 1]
    if (!nextBreakpoint) return mediaQueries.up(breakpoint)
    return mediaQueries.between(breakpoint, nextBreakpoint)
  },
} as const

// Device detection helpers
export const deviceTypes = {
  mobile: '(max-width: 639px)',
  tablet: '(min-width: 640px) and (max-width: 1023px)', 
  desktop: '(min-width: 1024px)',
  touchDevice: '(pointer: coarse)',
  mouseDevice: '(pointer: fine)',
  darkMode: '(prefers-color-scheme: dark)',
  lightMode: '(prefers-color-scheme: light)',
  reducedMotion: '(prefers-reduced-motion: reduce)',
  highDPI: '(-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi)',
} as const

// Container query breakpoints (for component-level responsiveness)
export const containerBreakpoints = {
  xs: 320,
  sm: 480,
  md: 640,
  lg: 840,
  xl: 1024,
  xxl: 1280,
} as const

export type ContainerBreakpointKey = keyof typeof containerBreakpoints