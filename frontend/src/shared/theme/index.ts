/**
 * Theme System Exports
 * 
 * Central export point for the comprehensive responsive theme system
 */

// Main theme exports
export { default as theme, themeVariants, createLightTheme, createDarkTheme, themeUtils, containerQueries } from './theme'

// Breakpoint system
export { 
  breakpointValues, 
  mediaQueries, 
  deviceTypes, 
  responsivePatterns,
  containerBreakpoints,
  type BreakpointKey,
  type ContainerBreakpointKey 
} from './breakpoints'

// Typography system
export { 
  createResponsiveTypography,
  fluidTypographyScale,
  typography,
  fontFamilies,
  fontWeights,
  responsiveTypography 
} from './typography'

// Color palette system
export {
  createLightPalette,
  createDarkPalette,
  brandColors,
  semanticColors,
  contextColors,
  neutralColors,
  adaptiveColors,
  colorUtils
} from './palette'

// Spacing system
export {
  spacingScale,
  responsiveSpacing,
  fluidSpacing,
  componentSpacing,
  spacingUtils,
  spacingTokens,
  createSpacingFunction,
  type SpacingKey
} from './spacing'