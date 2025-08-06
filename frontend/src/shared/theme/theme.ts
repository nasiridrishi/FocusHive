/**
 * Advanced Responsive Theme Configuration
 * 
 * Comprehensive Material-UI theme with responsive design system
 * Integrates typography, spacing, colors, and breakpoints
 */

import { createTheme, ThemeOptions, responsiveFontSizes, Theme } from '@mui/material/styles'
import { breakpointValues, containerBreakpoints } from './breakpoints'
import { createResponsiveTypography } from './typography'
import { createLightPalette, createDarkPalette } from './palette'
import { createSpacingFunction } from './spacing'

// Base theme configuration shared between light and dark themes
const baseThemeConfig: ThemeOptions = {
  // Custom breakpoints
  breakpoints: {
    values: breakpointValues,
  },
  
  // Responsive spacing function
  spacing: createSpacingFunction(),
  
  // Shape and border radius
  shape: {
    borderRadius: 12, // More modern, rounded corners
  },
  
  // Component style overrides
  components: {
    // Global CSS baseline
    MuiCssBaseline: {
      styleOverrides: {
        html: {
          // Smooth scrolling
          scrollBehavior: 'smooth',
          // Better font rendering
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },
        body: {
          // Prevent horizontal scroll
          overflowX: 'hidden',
          // Better line height for readability
          lineHeight: 1.6,
        },
        // Custom scrollbar styling
        '*::-webkit-scrollbar': {
          width: '8px',
          height: '8px',
        },
        '*::-webkit-scrollbar-track': {
          backgroundColor: 'transparent',
        },
        '*::-webkit-scrollbar-thumb': {
          backgroundColor: 'rgba(0, 0, 0, 0.2)',
          borderRadius: '4px',
          '&:hover': {
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    },
    
    // Container component with responsive max-widths
    MuiContainer: {
      styleOverrides: {
        root: {
          // Mobile-first responsive padding
          paddingLeft: '16px',
          paddingRight: '16px',
          [`@media (min-width: ${breakpointValues.tablet}px)`]: {
            paddingLeft: '24px',
            paddingRight: '24px',
          },
          [`@media (min-width: ${breakpointValues.desktop}px)`]: {
            paddingLeft: '32px',
            paddingRight: '32px',
          },
        },
      },
    },
    
    // Card component with responsive design
    MuiCard: {
      styleOverrides: {
        root: (theme) => ({
          borderRadius: theme.theme.shape.borderRadius,
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)',
          transition: 'all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)',
          '&:hover': {
            boxShadow: '0 14px 28px rgba(0, 0, 0, 0.25), 0 10px 10px rgba(0, 0, 0, 0.22)',
            transform: 'translateY(-2px)',
          },
          // Responsive padding
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            borderRadius: theme.theme.shape.borderRadius * 0.75,
          },
        }),
      },
    },
    
    // Button component with responsive sizing
    MuiButton: {
      styleOverrides: {
        root: (theme) => ({
          borderRadius: theme.theme.shape.borderRadius,
          textTransform: 'none',
          fontWeight: 500,
          transition: 'all 0.2s ease-in-out',
          // Touch-friendly sizing on mobile
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            minHeight: 48, // Touch target size
            fontSize: '1rem',
          },
        }),
        sizeSmall: {
          padding: '6px 12px',
          fontSize: '0.875rem',
        },
        sizeMedium: {
          padding: '8px 16px',
          fontSize: '1rem',
        },
        sizeLarge: {
          padding: '12px 24px',
          fontSize: '1.125rem',
        },
      },
    },
    
    // AppBar with responsive styling
    MuiAppBar: {
      styleOverrides: {
        root: (theme) => ({
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.12)',
          backdropFilter: 'blur(10px)',
          // Responsive height
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            '& .MuiToolbar-root': {
              minHeight: 56,
              paddingLeft: theme.theme.spacing(2),
              paddingRight: theme.theme.spacing(2),
            },
          },
          [`@media (min-width: ${breakpointValues.tablet}px)`]: {
            '& .MuiToolbar-root': {
              minHeight: 64,
              paddingLeft: theme.theme.spacing(3),
              paddingRight: theme.theme.spacing(3),
            },
          },
        }),
      },
    },
    
    // Drawer with responsive behavior
    MuiDrawer: {
      styleOverrides: {
        paper: () => ({
          // Mobile drawer styles
          [`@media (max-width: ${breakpointValues.laptop - 1}px)`]: {
            width: 280,
          },
          // Desktop drawer styles
          [`@media (min-width: ${breakpointValues.laptop}px)`]: {
            width: 320,
          },
          [`@media (min-width: ${breakpointValues.desktopLg}px)`]: {
            width: 360,
          },
        }),
      },
    },
    
    // Responsive Grid component
    MuiGrid: {
      styleOverrides: {
        container: {
          // Responsive spacing
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            spacing: 2,
          },
          [`@media (min-width: ${breakpointValues.tablet}px)`]: {
            spacing: 3,
          },
          [`@media (min-width: ${breakpointValues.desktop}px)`]: {
            spacing: 4,
          },
        },
      },
    },
    
    // Dialog with responsive behavior
    MuiDialog: {
      styleOverrides: {
        paper: (theme) => ({
          borderRadius: theme.theme.shape.borderRadius,
          // Full screen on mobile
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            margin: theme.theme.spacing(2),
            width: `calc(100% - ${theme.theme.spacing(4)})`,
            maxHeight: `calc(100% - ${theme.theme.spacing(4)})`,
          },
        }),
      },
    },
    
    // Responsive Chip component
    MuiChip: {
      styleOverrides: {
        root: () => ({
          // Touch-friendly sizing on mobile
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            height: 36,
            fontSize: '0.875rem',
          },
        }),
      },
    },
    
    // Form controls with responsive styling
    MuiTextField: {
      styleOverrides: {
        root: () => ({
          // Touch-friendly input sizing
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            '& .MuiInputBase-root': {
              fontSize: '16px', // Prevents zoom on iOS
              minHeight: 48,
            },
          },
        }),
      },
    },
    
    // Responsive FAB positioning
    MuiFab: {
      styleOverrides: {
        root: ({ theme }) => ({
          // Mobile positioning
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            position: 'fixed',
            bottom: theme.spacing(2),
            right: theme.spacing(2),
            zIndex: 1050,
          },
          // Desktop positioning
          [`@media (min-width: ${breakpointValues.tablet}px)`]: {
            position: 'fixed',
            bottom: theme.spacing(3),
            right: theme.spacing(3),
            zIndex: 1050,
          },
        }),
      },
    },
    
    // Responsive data tables
    MuiTableContainer: {
      styleOverrides: {
        root: () => ({
          // Horizontal scroll on mobile
          [`@media (max-width: ${breakpointValues.tablet - 1}px)`]: {
            overflowX: 'auto',
            '& .MuiTable-root': {
              minWidth: 650,
            },
          },
        }),
      },
    },
  },
  
  // Transitions with reduced motion support
  transitions: {
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
    easing: {
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
  },
  
  // Z-index values for layering
  zIndex: {
    mobileStepper: 1000,
    fab: 1050,
    speedDial: 1050,
    appBar: 1100,
    drawer: 1200,
    modal: 1300,
    snackbar: 1400,
    tooltip: 1500,
  },
}

// Create light theme
export const createLightTheme = () => {
  const baseTheme = createTheme({
    ...baseThemeConfig,
    palette: createLightPalette(),
    typography: createResponsiveTypography(),
  })
  
  // Apply responsive font sizes
  return responsiveFontSizes(baseTheme, {
    breakpoints: ['mobile', 'tablet', 'desktop', 'desktopLg'],
    factor: 2.5, // More aggressive responsive scaling
  })
}

// Create dark theme
export const createDarkTheme = () => {
  const baseTheme = createTheme({
    ...baseThemeConfig,
    palette: createDarkPalette(),
    typography: createResponsiveTypography(),
    components: {
      ...baseThemeConfig.components,
      // Dark theme specific overrides
      MuiCssBaseline: {
        styleOverrides: {
          '*::-webkit-scrollbar-thumb': {
            backgroundColor: 'rgba(255, 255, 255, 0.2)',
            borderRadius: '4px',
            '&:hover': {
              backgroundColor: 'rgba(255, 255, 255, 0.3)',
            },
          },
        },
      },
    },
  })
  
  // Apply responsive font sizes
  return responsiveFontSizes(baseTheme, {
    breakpoints: ['mobile', 'tablet', 'desktop', 'desktopLg'],
    factor: 2.5,
  })
}

// Theme variants
export const themeVariants = {
  light: createLightTheme(),
  dark: createDarkTheme(),
}

// Container queries helper (requires CSS Container Queries support)
export const containerQueries = {
  up: (breakpoint: keyof typeof containerBreakpoints) => 
    `@container (min-width: ${containerBreakpoints[breakpoint]}px)`,
  down: (breakpoint: keyof typeof containerBreakpoints) => 
    `@container (max-width: ${containerBreakpoints[breakpoint] - 1}px)`,
  between: (start: keyof typeof containerBreakpoints, end: keyof typeof containerBreakpoints) =>
    `@container (min-width: ${containerBreakpoints[start]}px) and (max-width: ${containerBreakpoints[end] - 1}px)`,
}

// Theme utilities
export const themeUtils = {
  // Get current theme mode
  getThemeMode: (theme: Theme): 'light' | 'dark' => theme.palette.mode,
  
  // Check if mobile breakpoint
  isMobile: (theme: Theme) => `@media (max-width: ${theme.breakpoints.values.tablet - 1}px)`,
  
  // Check if desktop breakpoint
  isDesktop: (theme: Theme) => `@media (min-width: ${theme.breakpoints.values.laptop}px)`,
  
  // Get responsive value
  responsive: <T>(theme: Theme, mobileValue: T, desktopValue: T) => ({
    [themeUtils.isMobile(theme)]: mobileValue,
    [themeUtils.isDesktop(theme)]: desktopValue,
  }),
  
  // Get spacing for breakpoint
  getSpacing: (theme: Theme, mobile: number, desktop: number) => ({
    [themeUtils.isMobile(theme)]: theme.spacing(mobile),
    [themeUtils.isDesktop(theme)]: theme.spacing(desktop),
  }),
}

// Export default light theme
export default themeVariants.light

// Type augmentation for custom breakpoints
declare module '@mui/material/styles' {
  interface BreakpointOverrides {
    xs: false
    sm: false
    md: false
    lg: false
    xl: false
    mobile: true
    mobileLg: true
    tablet: true
    tabletLg: true
    laptop: true
    desktop: true
    desktopLg: true
    desktopXl: true
  }
}