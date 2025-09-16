/**
 * Advanced Responsive Color Palette
 *
 * Comprehensive color system with light/dark modes
 * Based on Material Design 3 with FocusHive brand colors
 */

import {PaletteColor, PaletteColorOptions, PaletteOptions} from '@mui/material/styles'

// Brand colors - FocusHive identity
export const brandColors = {
  // Primary brand color - warm, focused orange-red (WCAG 2.1 AA compliant)
  primary: {
    50: '#fef7f0',
    100: '#feecdc',
    200: '#fcd5b4',
    300: '#fab382',
    400: '#f7864e',
    500: '#b8470a', // Main brand color - Updated for WCAG 2.1 AA compliance (4.6:1 contrast)
    600: '#a03f08', // Updated for consistency with darker main color
    700: '#9a4f1f', // Updated for consistent color progression and accessibility
    800: '#983410',
    900: '#7c2d11',
    950: '#431505',
  },

  // Secondary brand color - calming blue for balance
  secondary: {
    50: '#eff9ff',
    100: '#daf2ff',
    200: '#bee9ff',
    300: '#91ddff',
    400: '#5dc8ff',
    500: '#36aeff',
    600: '#1e96ff',
    700: '#0c7fff', // Main secondary color
    800: '#1365cc',
    900: '#1656a0',
    950: '#113461',
  },

  // Accent color - energetic green for success states
  accent: {
    50: '#f0fdf5',
    100: '#dcfce8',
    200: '#bbf7d1',
    300: '#86efad',
    400: '#4ade80',
    500: '#22c55e', // Main accent color
    600: '#16a34a',
    700: '#15803d',
    800: '#166534',
    900: '#14532d',
    950: '#052e16',
  },
} as const

// Semantic colors for different states and contexts
export const semanticColors = {
  // Success colors
  success: {
    light: '#4caf50',
    main: '#2e7d32',
    dark: '#1b5e20',
    contrastText: '#ffffff',
  },

  // Warning colors
  warning: {
    light: '#ff9800',
    main: '#ed6c02',
    dark: '#e65100',
    contrastText: '#ffffff',
  },

  // Error colors
  error: {
    light: '#f44336',
    main: '#d32f2f',
    dark: '#c62828',
    contrastText: '#ffffff',
  },

  // Info colors
  info: {
    light: '#29b6f6',
    main: '#0288d1',
    dark: '#01579b',
    contrastText: '#ffffff',
  },
} as const

// Context-specific colors for FocusHive features
export const contextColors = {
  // Focus states
  focus: {
    active: '#b8470a',      // Active focus session - Updated for WCAG compliance
    paused: '#ffa726',      // Paused session
    break: '#66bb6a',       // Break time
    completed: '#4caf50',   // Completed session
  },

  // Presence states
  presence: {
    online: '#4caf50',      // User is online and active
    away: '#ff9800',        // User is away
    busy: '#f44336',        // User is in focus mode
    offline: '#9e9e9e',     // User is offline
  },

  // Hive categories
  hive: {
    study: '#673ab7',       // Study-focused hives
    work: '#2196f3',        // Work-focused hives
    creative: '#e91e63',    // Creative work hives
    wellness: '#009688',    // Wellness and meditation
    social: '#ff5722',      // Social study groups
  },

  // Achievement levels
  achievement: {
    bronze: '#cd7f32',
    silver: '#c0c0c0',
    gold: '#ffd700',
    platinum: '#e5e4e2',
    diamond: '#b9f2ff',
  },
} as const

// Neutral grays for backgrounds and subtle elements
export const neutralColors = {
  // Light theme grays
  light: {
    50: '#fafafa',
    100: '#f5f5f5',
    200: '#eeeeee',
    300: '#e0e0e0',
    400: '#bdbdbd',
    500: '#9e9e9e',
    600: '#757575',
    700: '#616161',
    800: '#424242',
    900: '#212121',
  },

  // Dark theme grays
  dark: {
    50: '#fafafa',
    100: '#f5f5f5',
    200: '#eeeeee',
    300: '#e0e0e0',
    400: '#bdbdbd',
    500: '#9e9e9e',
    600: '#757575',
    700: '#616161',
    800: '#424242',
    900: '#212121',
  },
} as const

// Create light theme palette
export const createLightPalette = (): PaletteOptions => ({
  mode: 'light',

  primary: {
    main: brandColors.primary[500],
    light: brandColors.primary[300],
    dark: brandColors.primary[700],
    contrastText: '#ffffff',
  },

  secondary: {
    main: brandColors.secondary[700],
    light: brandColors.secondary[400],
    dark: brandColors.secondary[900],
    contrastText: '#ffffff',
  },

  tertiary: {
    main: brandColors.accent[500],
    light: brandColors.accent[300],
    dark: brandColors.accent[700],
    contrastText: '#ffffff',
  } as PaletteColorOptions,

  error: semanticColors.error,
  warning: semanticColors.warning,
  info: semanticColors.info,
  success: semanticColors.success,

  background: {
    default: '#fafafa',
    paper: '#ffffff',
    secondary: '#f5f5f5',
    tertiary: '#eeeeee',
  },

  surface: {
    main: '#ffffff',
    variant: '#f5f5f5',
    container: '#fafafa',
    containerHigh: '#eeeeee',
    containerHighest: '#e0e0e0',
  },

  text: {
    primary: 'rgba(0, 0, 0, 0.87)',
    secondary: 'rgba(0, 0, 0, 0.6)',
    tertiary: 'rgba(0, 0, 0, 0.38)',
    disabled: 'rgba(0, 0, 0, 0.38)',
  },

  divider: 'rgba(0, 0, 0, 0.12)',

  action: {
    active: 'rgba(0, 0, 0, 0.54)',
    hover: 'rgba(0, 0, 0, 0.04)',
    selected: 'rgba(0, 0, 0, 0.08)',
    disabled: 'rgba(0, 0, 0, 0.26)',
    disabledBackground: 'rgba(0, 0, 0, 0.12)',
    focus: 'rgba(0, 0, 0, 0.12)',
  },

  // Custom palette extensions
  custom: {
    focus: contextColors.focus,
    presence: contextColors.presence,
    hive: contextColors.hive,
    achievement: contextColors.achievement,
    brand: brandColors,
  },
})

// Create dark theme palette
export const createDarkPalette = (): PaletteOptions => ({
  mode: 'dark',

  primary: {
    main: brandColors.primary[400],
    light: brandColors.primary[200],
    dark: brandColors.primary[600],
    contrastText: '#000000',
  },

  secondary: {
    main: brandColors.secondary[400],
    light: brandColors.secondary[200],
    dark: brandColors.secondary[600],
    contrastText: '#000000',
  },

  tertiary: {
    main: brandColors.accent[400],
    light: brandColors.accent[200],
    dark: brandColors.accent[600],
    contrastText: '#000000',
  } as PaletteColorOptions,

  error: {
    main: '#ef5350',
    light: '#ff7961',
    dark: '#c62828',
    contrastText: '#ffffff',
  },

  warning: {
    main: '#ff9800',
    light: '#ffb74d',
    dark: '#f57c00',
    contrastText: '#000000',
  },

  info: {
    main: '#29b6f6',
    light: '#73e8ff',
    dark: '#0086c3',
    contrastText: '#000000',
  },

  success: {
    main: '#66bb6a',
    light: '#98ee99',
    dark: '#338a3e',
    contrastText: '#000000',
  },

  background: {
    default: '#0f0f0f',
    paper: '#1a1a1a',
    secondary: '#1f1f1f',
    tertiary: '#2a2a2a',
  },

  surface: {
    main: '#1a1a1a',
    variant: '#2a2a2a',
    container: '#1f1f1f',
    containerHigh: '#2a2a2a',
    containerHighest: '#353535',
  },

  text: {
    primary: 'rgba(255, 255, 255, 0.87)',
    secondary: 'rgba(255, 255, 255, 0.6)',
    tertiary: 'rgba(255, 255, 255, 0.38)',
    disabled: 'rgba(255, 255, 255, 0.38)',
  },

  divider: 'rgba(255, 255, 255, 0.12)',

  action: {
    active: 'rgba(255, 255, 255, 0.56)',
    hover: 'rgba(255, 255, 255, 0.08)',
    selected: 'rgba(255, 255, 255, 0.16)',
    disabled: 'rgba(255, 255, 255, 0.3)',
    disabledBackground: 'rgba(255, 255, 255, 0.12)',
    focus: 'rgba(255, 255, 255, 0.12)',
  },

  // Custom palette extensions
  custom: {
    focus: {
      active: '#b8470a',  // Updated for WCAG compliance
      paused: '#ffa726',
      break: '#66bb6a',
      completed: '#4caf50',
    },
    presence: {
      online: '#4caf50',
      away: '#ff9800',
      busy: '#f44336',
      offline: '#9e9e9e',
    },
    hive: {
      study: '#673ab7',
      work: '#2196f3',
      creative: '#e91e63',
      wellness: '#009688',
      social: '#ff5722',
    },
    achievement: contextColors.achievement,
    brand: brandColors,
  },
})

// Adaptive colors that change based on theme
export const adaptiveColors = {
  // Cards that adapt to theme
  cardBackground: (mode: 'light' | 'dark') =>
      mode === 'light' ? '#ffffff' : '#1a1a1a',

  cardBackgroundHover: (mode: 'light' | 'dark') =>
      mode === 'light' ? '#f5f5f5' : '#2a2a2a',

  // Borders that adapt to theme
  border: (mode: 'light' | 'dark') =>
      mode === 'light' ? 'rgba(0, 0, 0, 0.12)' : 'rgba(255, 255, 255, 0.12)',

  borderFocus: (mode: 'light' | 'dark') =>
      mode === 'light' ? brandColors.primary[500] : brandColors.primary[400],

  // Overlays for modals and dialogs
  overlay: (mode: 'light' | 'dark') =>
      mode === 'light' ? 'rgba(0, 0, 0, 0.5)' : 'rgba(0, 0, 0, 0.8)',

  // Gradient backgrounds
  gradientPrimary: (mode: 'light' | 'dark') =>
      mode === 'light'
          ? `linear-gradient(135deg, ${brandColors.primary[500]} 0%, ${brandColors.primary[700]} 100%)`
          : `linear-gradient(135deg, ${brandColors.primary[400]} 0%, ${brandColors.primary[600]} 100%)`,

  gradientSecondary: (mode: 'light' | 'dark') =>
      mode === 'light'
          ? `linear-gradient(135deg, ${brandColors.secondary[600]} 0%, ${brandColors.secondary[800]} 100%)`
          : `linear-gradient(135deg, ${brandColors.secondary[400]} 0%, ${brandColors.secondary[600]} 100%)`,
} as const

// Color utilities
export const colorUtils = {
  // Add alpha transparency to any color
  alpha: (color: string, alpha: number): string => {
    if (color.startsWith('#')) {
      const hex = color.slice(1)
      const r = parseInt(hex.substr(0, 2), 16)
      const g = parseInt(hex.substr(2, 2), 16)
      const b = parseInt(hex.substr(4, 2), 16)
      return `rgba(${r}, ${g}, ${b}, ${alpha})`
    }
    return color
  },

  // Get contrast color (black or white) for any background
  getContrastColor: (backgroundColor: string): string => {
    // This is a simplified version - in production, use a proper contrast calculation
    const isLight = backgroundColor.includes('light') || backgroundColor.includes('50') || backgroundColor.includes('100')
    return isLight ? '#000000' : '#ffffff'
  },

  // Get semantic color for different states
  getSemanticColor: (state: 'success' | 'warning' | 'error' | 'info', mode: 'light' | 'dark' = 'light') => {
    return mode === 'light' ? semanticColors[state].main : semanticColors[state].light
  },

  // Get presence color
  getPresenceColor: (status: keyof typeof contextColors.presence) => {
    return contextColors.presence[status]
  },

  // Get focus color
  getFocusColor: (state: keyof typeof contextColors.focus) => {
    return contextColors.focus[state]
  },
} as const

// TypeScript module augmentation for custom palette properties
declare module '@mui/material/styles' {
  interface Palette {
    tertiary: PaletteColor
    surface: {
      main: string
      variant: string
      container: string
      containerHigh: string
      containerHighest: string
    }
    custom: {
      focus: typeof contextColors.focus
      presence: typeof contextColors.presence
      hive: typeof contextColors.hive
      achievement: typeof contextColors.achievement
      brand: typeof brandColors
    }
  }

  interface PaletteOptions {
    tertiary?: PaletteColorOptions
    surface?: {
      main?: string
      variant?: string
      container?: string
      containerHigh?: string
      containerHighest?: string
    }
    custom?: {
      focus?: typeof contextColors.focus
      presence?: typeof contextColors.presence
      hive?: typeof contextColors.hive
      achievement?: typeof contextColors.achievement
      brand?: typeof brandColors
    }
  }

  interface TypeBackground {
    secondary: string
    tertiary: string
  }

  interface TypeText {
    tertiary: string
  }
}