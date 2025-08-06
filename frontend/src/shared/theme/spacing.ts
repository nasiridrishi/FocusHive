/**
 * Advanced Responsive Spacing System
 * 
 * Implements fluid spacing with device-aware sizing
 * Based on 4px base unit with responsive scaling
 */

import { mediaQueries } from './breakpoints'

// Base spacing unit (4px)
export const BASE_SPACING = 4

// Spacing scale multipliers
export const spacingScale = {
  0: 0,     // 0px
  0.5: 0.5, // 2px
  1: 1,     // 4px
  1.5: 1.5, // 6px
  2: 2,     // 8px
  2.5: 2.5, // 10px
  3: 3,     // 12px
  3.5: 3.5, // 14px
  4: 4,     // 16px
  5: 5,     // 20px
  6: 6,     // 24px
  7: 7,     // 28px
  8: 8,     // 32px
  9: 9,     // 36px
  10: 10,   // 40px
  11: 11,   // 44px
  12: 12,   // 48px
  14: 14,   // 56px
  16: 16,   // 64px
  18: 18,   // 72px
  20: 20,   // 80px
  24: 24,   // 96px
  28: 28,   // 112px
  32: 32,   // 128px
  36: 36,   // 144px
  40: 40,   // 160px
  44: 44,   // 176px
  48: 48,   // 192px
  52: 52,   // 208px
  56: 56,   // 224px
  60: 60,   // 240px
  64: 64,   // 256px
  72: 72,   // 288px
  80: 80,   // 320px
  96: 96,   // 384px
} as const

export type SpacingKey = keyof typeof spacingScale

// Responsive spacing patterns
export const responsiveSpacing = {
  // Container padding - adapts to screen size
  containerPadding: {
    mobile: spacingScale[4],      // 16px
    tablet: spacingScale[6],      // 24px
    desktop: spacingScale[8],     // 32px
    desktopLg: spacingScale[10],  // 40px
  },
  
  // Section spacing - vertical spacing between major sections
  sectionSpacing: {
    mobile: spacingScale[8],      // 32px
    tablet: spacingScale[12],     // 48px
    desktop: spacingScale[16],    // 64px
    desktopLg: spacingScale[20],  // 80px
  },
  
  // Component spacing - spacing within components
  componentSpacing: {
    mobile: spacingScale[3],      // 12px
    tablet: spacingScale[4],      // 16px
    desktop: spacingScale[5],     // 20px
    desktopLg: spacingScale[6],   // 24px
  },
  
  // Element spacing - small spacing between elements
  elementSpacing: {
    mobile: spacingScale[2],      // 8px
    tablet: spacingScale[2.5],    // 10px
    desktop: spacingScale[3],     // 12px
    desktopLg: spacingScale[3],   // 12px
  },
  
  // Grid spacing - spacing between grid items
  gridSpacing: {
    mobile: spacingScale[2],      // 8px
    tablet: spacingScale[3],      // 12px
    desktop: spacingScale[4],     // 16px
    desktopLg: spacingScale[6],   // 24px
  },
} as const

// Fluid spacing using CSS clamp
export const fluidSpacing = {
  // Extra small spacing (2px → 4px)
  xs: 'clamp(0.125rem, 0.1vw + 0.1rem, 0.25rem)',
  
  // Small spacing (4px → 8px)
  sm: 'clamp(0.25rem, 0.2vw + 0.2rem, 0.5rem)',
  
  // Medium spacing (8px → 16px)
  md: 'clamp(0.5rem, 0.5vw + 0.25rem, 1rem)',
  
  // Large spacing (16px → 24px)
  lg: 'clamp(1rem, 0.5vw + 0.75rem, 1.5rem)',
  
  // Extra large spacing (24px → 40px)
  xl: 'clamp(1.5rem, 1vw + 1rem, 2.5rem)',
  
  // Double extra large spacing (32px → 64px)
  xxl: 'clamp(2rem, 2vw + 1rem, 4rem)',
  
  // Triple extra large spacing (48px → 96px)
  xxxl: 'clamp(3rem, 3vw + 1.5rem, 6rem)',
} as const

// Component-specific spacing patterns
export const componentSpacing = {
  // Card component spacing
  card: {
    padding: {
      mobile: spacingScale[4],    // 16px
      tablet: spacingScale[5],    // 20px
      desktop: spacingScale[6],   // 24px
    },
    margin: {
      mobile: spacingScale[2],    // 8px
      tablet: spacingScale[3],    // 12px
      desktop: spacingScale[4],   // 16px
    },
    gap: {
      mobile: spacingScale[3],    // 12px
      tablet: spacingScale[4],    // 16px
      desktop: spacingScale[5],   // 20px
    },
  },
  
  // Button component spacing
  button: {
    padding: {
      small: { x: spacingScale[3], y: spacingScale[1.5] },    // 12px, 6px
      medium: { x: spacingScale[4], y: spacingScale[2] },     // 16px, 8px
      large: { x: spacingScale[6], y: spacingScale[3] },      // 24px, 12px
    },
    gap: {
      mobile: spacingScale[2],    // 8px
      desktop: spacingScale[3],   // 12px
    },
  },
  
  // Form component spacing
  form: {
    fieldSpacing: {
      mobile: spacingScale[4],    // 16px
      desktop: spacingScale[5],   // 20px
    },
    labelSpacing: {
      mobile: spacingScale[1],    // 4px
      desktop: spacingScale[1.5], // 6px
    },
    groupSpacing: {
      mobile: spacingScale[6],    // 24px
      desktop: spacingScale[8],   // 32px
    },
  },
  
  // Navigation spacing
  navigation: {
    itemSpacing: {
      mobile: spacingScale[1],    // 4px
      desktop: spacingScale[2],   // 8px
    },
    groupSpacing: {
      mobile: spacingScale[4],    // 16px
      desktop: spacingScale[6],   // 24px
    },
    padding: {
      mobile: spacingScale[3],    // 12px
      desktop: spacingScale[4],   // 16px
    },
  },
} as const

// Utility functions for spacing calculations
export const spacingUtils = {
  // Convert spacing scale to pixels
  toPx: (scale: SpacingKey): number => spacingScale[scale] * BASE_SPACING,
  
  // Convert spacing scale to rem
  toRem: (scale: SpacingKey): string => `${(spacingScale[scale] * BASE_SPACING) / 16}rem`,
  
  // Get responsive spacing value
  responsive: (mobileScale: SpacingKey, desktopScale: SpacingKey) => ({
    [mediaQueries.down('tablet')]: spacingUtils.toRem(mobileScale),
    [mediaQueries.up('tablet')]: spacingUtils.toRem(desktopScale),
  }),
  
  // Create fluid spacing between two values
  fluid: (minScale: SpacingKey, maxScale: SpacingKey, minVw = 320, maxVw = 1440) => {
    const minPx = spacingUtils.toPx(minScale)
    const maxPx = spacingUtils.toPx(maxScale)
    const slope = (maxPx - minPx) / (maxVw - minVw)
    const yAxisIntersection = -minVw * slope + minPx
    return `clamp(${minPx}px, ${yAxisIntersection.toFixed(2)}px + ${(slope * 100).toFixed(2)}vw, ${maxPx}px)`
  },
  
  // Get spacing for specific device type
  forDevice: (device: 'mobile' | 'tablet' | 'desktop' | 'desktopLg', category: keyof typeof responsiveSpacing) => {
    return spacingUtils.toRem(responsiveSpacing[category][device] as SpacingKey)
  },
  
  // Create consistent component spacing
  component: (componentType: keyof typeof componentSpacing, property: string) => {
    const component = componentSpacing[componentType]
    const spacing = component[property as keyof typeof component] as SpacingKey | { mobile: SpacingKey; desktop: SpacingKey } | { mobile: SpacingKey; tablet: SpacingKey; desktop: SpacingKey }
    
    if (typeof spacing === 'object' && spacing && 'mobile' in spacing && 'desktop' in spacing) {
      return {
        [mediaQueries.down('tablet')]: spacingUtils.toRem(spacing.mobile as SpacingKey),
        [mediaQueries.up('tablet')]: spacingUtils.toRem(spacing.desktop as SpacingKey),
      }
    }
    
    return spacing
  },
} as const

// Material-UI spacing function override
export const createSpacingFunction = () => {
  return (value: number | string) => {
    if (typeof value === 'number') {
      return `${value * BASE_SPACING}px`
    }
    return value
  }
}

// Spacing tokens for design system
export const spacingTokens = {
  none: 0,
  auto: 'auto',
  px: '1px',
  0.5: spacingUtils.toRem(0.5),
  1: spacingUtils.toRem(1),
  1.5: spacingUtils.toRem(1.5),
  2: spacingUtils.toRem(2),
  2.5: spacingUtils.toRem(2.5),
  3: spacingUtils.toRem(3),
  3.5: spacingUtils.toRem(3.5),
  4: spacingUtils.toRem(4),
  5: spacingUtils.toRem(5),
  6: spacingUtils.toRem(6),
  7: spacingUtils.toRem(7),
  8: spacingUtils.toRem(8),
  9: spacingUtils.toRem(9),
  10: spacingUtils.toRem(10),
  12: spacingUtils.toRem(12),
  14: spacingUtils.toRem(14),
  16: spacingUtils.toRem(16),
  20: spacingUtils.toRem(20),
  24: spacingUtils.toRem(24),
  28: spacingUtils.toRem(28),
  32: spacingUtils.toRem(32),
  36: spacingUtils.toRem(36),
  40: spacingUtils.toRem(40),
  44: spacingUtils.toRem(44),
  48: spacingUtils.toRem(48),
  52: spacingUtils.toRem(52),
  56: spacingUtils.toRem(56),
  60: spacingUtils.toRem(60),
  64: spacingUtils.toRem(64),
  72: spacingUtils.toRem(72),
  80: spacingUtils.toRem(80),
  96: spacingUtils.toRem(96),
} as const