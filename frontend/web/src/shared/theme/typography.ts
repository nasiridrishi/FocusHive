/**
 * Advanced Responsive Typography System
 * 
 * Implements fluid typography with responsive scaling
 * Based on Material Design 3 typography scale with custom enhancements
 */

import { TypographyOptions } from '@mui/material/styles'
import { breakpointValues, mediaQueries } from './breakpoints'

// Font families
export const fontFamilies = {
  primary: '"Inter", "Roboto", "Helvetica Neue", "Arial", sans-serif',
  secondary: '"Roboto Slab", "Georgia", serif',
  mono: '"JetBrains Mono", "Fira Code", "Consolas", monospace',
  display: '"Inter Display", "Inter", sans-serif',
} as const

// Font weights
export const fontWeights = {
  light: 300,
  regular: 400,
  medium: 500,
  semiBold: 600,
  bold: 700,
  extraBold: 800,
} as const

// Fluid typography scale - automatically adjusts between mobile and desktop
export const fluidTypographyScale = {
  // Display styles - for hero sections and major headings
  displayLarge: {
    fontFamily: fontFamilies.display,
    fontWeight: fontWeights.bold,
    fontSize: 'clamp(2.5rem, 4vw + 1rem, 4rem)',     // 40px → 64px
    lineHeight: 'clamp(3rem, 4vw + 1.5rem, 4.5rem)', // 48px → 72px
    letterSpacing: '-0.02em',
  },
  displayMedium: {
    fontFamily: fontFamilies.display,
    fontWeight: fontWeights.bold,
    fontSize: 'clamp(2rem, 3vw + 0.5rem, 3rem)',     // 32px → 48px
    lineHeight: 'clamp(2.5rem, 3vw + 1rem, 3.5rem)', // 40px → 56px
    letterSpacing: '-0.01em',
  },
  displaySmall: {
    fontFamily: fontFamilies.display,
    fontWeight: fontWeights.semiBold,
    fontSize: 'clamp(1.75rem, 2.5vw + 0.5rem, 2.25rem)', // 28px → 36px
    lineHeight: 'clamp(2.25rem, 2.5vw + 1rem, 2.75rem)', // 36px → 44px
    letterSpacing: '-0.005em',
  },

  // Headline styles - for section headings
  headlineLarge: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.semiBold,
    fontSize: 'clamp(1.5rem, 2vw + 0.5rem, 2rem)',   // 24px → 32px
    lineHeight: 'clamp(2rem, 2vw + 1rem, 2.5rem)',   // 32px → 40px
    letterSpacing: '0',
  },
  headlineMedium: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.semiBold,
    fontSize: 'clamp(1.25rem, 1.5vw + 0.5rem, 1.75rem)', // 20px → 28px
    lineHeight: 'clamp(1.75rem, 1.5vw + 1rem, 2.25rem)', // 28px → 36px
    letterSpacing: '0',
  },
  headlineSmall: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(1.125rem, 1vw + 0.5rem, 1.5rem)', // 18px → 24px
    lineHeight: 'clamp(1.5rem, 1vw + 1rem, 2rem)',     // 24px → 32px
    letterSpacing: '0',
  },

  // Title styles - for card titles and component headings
  titleLarge: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(1.125rem, 0.5vw + 0.75rem, 1.375rem)', // 18px → 22px
    lineHeight: 'clamp(1.5rem, 0.5vw + 1.25rem, 1.75rem)',  // 24px → 28px
    letterSpacing: '0',
  },
  titleMedium: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(1rem, 0.25vw + 0.875rem, 1.125rem)',   // 16px → 18px
    lineHeight: 'clamp(1.5rem, 0.25vw + 1.375rem, 1.625rem)', // 24px → 26px
    letterSpacing: '0.01em',
  },
  titleSmall: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(0.875rem, 0.25vw + 0.75rem, 1rem)',   // 14px → 16px
    lineHeight: 'clamp(1.25rem, 0.25vw + 1.125rem, 1.5rem)', // 20px → 24px
    letterSpacing: '0.01em',
  },

  // Body styles - for main content
  bodyLarge: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.regular,
    fontSize: 'clamp(1rem, 0.25vw + 0.875rem, 1.125rem)',   // 16px → 18px
    lineHeight: 'clamp(1.5rem, 0.5vw + 1.25rem, 1.75rem)',  // 24px → 28px
    letterSpacing: '0.01em',
  },
  bodyMedium: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.regular,
    fontSize: 'clamp(0.875rem, 0.25vw + 0.75rem, 1rem)',   // 14px → 16px
    lineHeight: 'clamp(1.25rem, 0.5vw + 1rem, 1.5rem)',    // 20px → 24px
    letterSpacing: '0.02em',
  },
  bodySmall: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.regular,
    fontSize: 'clamp(0.75rem, 0.25vw + 0.625rem, 0.875rem)', // 12px → 14px
    lineHeight: 'clamp(1rem, 0.5vw + 0.75rem, 1.25rem)',     // 16px → 20px
    letterSpacing: '0.025em',
  },

  // Label styles - for UI elements
  labelLarge: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(0.875rem, 0.25vw + 0.75rem, 1rem)',   // 14px → 16px
    lineHeight: 'clamp(1.25rem, 0.25vw + 1.125rem, 1.5rem)', // 20px → 24px
    letterSpacing: '0.01em',
  },
  labelMedium: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(0.75rem, 0.25vw + 0.625rem, 0.875rem)', // 12px → 14px
    lineHeight: 'clamp(1rem, 0.25vw + 0.875rem, 1.25rem)',   // 16px → 20px
    letterSpacing: '0.05em',
  },
  labelSmall: {
    fontFamily: fontFamilies.primary,
    fontWeight: fontWeights.medium,
    fontSize: 'clamp(0.6875rem, 0.25vw + 0.5625rem, 0.75rem)', // 11px → 12px
    lineHeight: 'clamp(1rem, 0.25vw + 0.875rem, 1.125rem)',     // 16px → 18px
    letterSpacing: '0.05em',
  },
} as const

// Create Material-UI typography configuration
export const createResponsiveTypography = (): TypographyOptions => ({
  fontFamily: fontFamilies.primary,
  
  // Override default Material-UI variants with our fluid typography
  h1: {
    ...fluidTypographyScale.displayLarge,
    [mediaQueries.down('tablet')]: {
      fontSize: '2.5rem',
      lineHeight: '3rem',
    },
  },
  h2: {
    ...fluidTypographyScale.displayMedium,
    [mediaQueries.down('tablet')]: {
      fontSize: '2rem',
      lineHeight: '2.5rem',
    },
  },
  h3: {
    ...fluidTypographyScale.displaySmall,
    [mediaQueries.down('tablet')]: {
      fontSize: '1.75rem',
      lineHeight: '2.25rem',
    },
  },
  h4: {
    ...fluidTypographyScale.headlineLarge,
    [mediaQueries.down('tablet')]: {
      fontSize: '1.5rem',
      lineHeight: '2rem',
    },
  },
  h5: {
    ...fluidTypographyScale.headlineMedium,
    [mediaQueries.down('tablet')]: {
      fontSize: '1.25rem',
      lineHeight: '1.75rem',
    },
  },
  h6: {
    ...fluidTypographyScale.headlineSmall,
    [mediaQueries.down('tablet')]: {
      fontSize: '1.125rem',
      lineHeight: '1.5rem',
    },
  },
  subtitle1: {
    ...fluidTypographyScale.titleLarge,
  },
  subtitle2: {
    ...fluidTypographyScale.titleMedium,
  },
  body1: {
    ...fluidTypographyScale.bodyLarge,
  },
  body2: {
    ...fluidTypographyScale.bodyMedium,
  },
  caption: {
    ...fluidTypographyScale.bodySmall,
  },
  overline: {
    ...fluidTypographyScale.labelSmall,
    textTransform: 'uppercase' as const,
  },
  button: {
    ...fluidTypographyScale.labelLarge,
    textTransform: 'none' as const,
  },
})

// Utility for creating responsive typography styles
export const responsiveTypography = {
  // Create a responsive font size using clamp
  fluidSize: (minSize: number, maxSize: number, minVw = 320, maxVw = 1440) => {
    const slope = (maxSize - minSize) / (maxVw - minVw)
    const yAxisIntersection = -minVw * slope + minSize
    return `clamp(${minSize}px, ${yAxisIntersection.toFixed(2)}px + ${(slope * 100).toFixed(2)}vw, ${maxSize}px)`
  },
  
  // Create responsive line height
  fluidLineHeight: (minLh: number, maxLh: number, minVw = 320, maxVw = 1440) => {
    const slope = (maxLh - minLh) / (maxVw - minVw)
    const yAxisIntersection = -minVw * slope + minLh
    return `clamp(${minLh}px, ${yAxisIntersection.toFixed(2)}px + ${(slope * 100).toFixed(2)}vw, ${maxLh}px)`
  },
  
  // Get typography for specific breakpoint
  forBreakpoint: (variant: keyof typeof fluidTypographyScale, breakpoint: keyof typeof breakpointValues) => {
    const baseStyle = fluidTypographyScale[variant]
    
    // Apply specific adjustments per breakpoint
    const adjustments = {
      mobile: { scale: 0.9 },
      tablet: { scale: 1.0 },
      desktop: { scale: 1.1 },
      desktopLg: { scale: 1.2 },
    }
    
    const adjustment = adjustments[breakpoint as keyof typeof adjustments] || { scale: 1.0 }
    
    return {
      ...baseStyle,
      transform: `scale(${adjustment.scale})`,
      transformOrigin: 'left top',
    }
  },
} as const

// Export individual typography variants for component use
export const typography = fluidTypographyScale