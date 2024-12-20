/**
 * Custom Accessible Theme with Brand Focus Colors
 * 
 * Extends the accessible theme but uses brand colors for focus rings
 * instead of the default blue color.
 */

import { createTheme } from '@mui/material/styles';
import { createLightTheme } from './theme';
import { accessibleColors, focusRingConfig, touchTargetConfig } from '../accessibility/theme/accessibleTheme';
import { FOCUS } from '../accessibility/constants/wcag';
import { brandColors } from './palette';

// Override focus colors to use brand colors instead of blue
const customFocusColors = {
  primary: brandColors.primary[500],  // Use your brand orange instead of blue
  secondary: brandColors.primary[700], // Darker brand color
  error: '#c62828',
  warning: '#f57c00',
  success: '#2e7d32',
};

// Custom focus ring configuration with brand colors
const customFocusRingConfig = {
  ...focusRingConfig,
  color: customFocusColors.primary,
  variants: {
    default: {
      outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${customFocusColors.primary}`,
      outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
    },
    inset: {
      boxShadow: `inset 0 0 0 ${FOCUS.FOCUS_RING_WIDTH}px ${customFocusColors.primary}`,
    },
    double: {
      outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${customFocusColors.primary}`,
      outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
      boxShadow: `0 0 0 ${FOCUS.FOCUS_RING_WIDTH + FOCUS.FOCUS_RING_OFFSET}px #ffffff`,
    }
  }
};

// Custom accessibility component overrides
const customAccessibilityComponentOverrides = {
  // Enhanced form field accessibility with brand focus colors
  MuiTextField: {
    styleOverrides: {
      root: ({ theme: _theme }: any) => ({
        // Ensure proper spacing for touch targets
        marginBottom: touchTargetConfig.recommendedSpacing,

        '& .MuiInputBase-root': {
          minHeight: touchTargetConfig.recommendedSize,

          // Focus styles with brand colors
          '&:focus-within': {
            ...customFocusRingConfig.variants.default,
          },

          // High contrast support
          '@media (prefers-contrast: high)': {
            border: '2px solid currentColor',
            '&:focus-within': {
              borderColor: customFocusColors.primary,
              outline: '2px solid currentColor',
              outlineOffset: '2px',
            },
          },
        },

        // Error state accessibility
        '&.Mui-error .MuiInputBase-root': {
          '&:focus-within': {
            outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${customFocusColors.error}`,
            outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
          },
        },
      }),
    },
  },

  // Enhanced button accessibility with brand focus colors
  MuiButton: {
    styleOverrides: {
      root: ({ theme: _theme }: any) => ({
        // Ensure minimum touch target size
        minHeight: touchTargetConfig.recommendedSize,
        minWidth: touchTargetConfig.recommendedSize,

        // Focus styles with brand colors
        '&:focus-visible': {
          ...customFocusRingConfig.variants.default,
          backgroundColor: 'transparent',
        },

        // High contrast mode support
        '@media (prefers-contrast: high)': {
          border: '1px solid currentColor',
          '&:focus-visible': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },

        // Reduced motion support
        '@media (prefers-reduced-motion: reduce)': {
          transition: 'none',
        },
      }),

      sizeSmall: {
        minHeight: touchTargetConfig.minSize,
        fontSize: '0.875rem',
        padding: '6px 12px',
      },
      sizeMedium: {
        minHeight: touchTargetConfig.recommendedSize,
        fontSize: '1rem',
        padding: '8px 16px',
      },
      sizeLarge: {
        minHeight: 56,
        fontSize: '1.125rem',
        padding: '12px 24px',
      },
    },
  },

  // Global CSS baseline with custom focus colors
  MuiCssBaseline: {
    styleOverrides: {
      // Focus-visible polyfill support
      '*:focus:not(:focus-visible)': {
        outline: 'none',
      },

      '*:focus-visible': {
        outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${customFocusColors.primary}`,
        outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
      },

      // High contrast mode support
      '@media (prefers-contrast: high)': {
        '*:focus-visible': {
          outline: '3px solid currentColor',
          outlineOffset: '2px',
        },
      },

      // Reduced motion support
      '@media (prefers-reduced-motion: reduce)': {
        '*': {
          animationDuration: '0.01ms !important',
          animationIterationCount: '1 !important',
          transitionDuration: '0.01ms !important',
          scrollBehavior: 'auto !important',
        },
      },

      // Screen reader only utility class
      '.sr-only': {
        position: 'absolute',
        width: '1px',
        height: '1px',
        padding: 0,
        margin: '-1px',
        overflow: 'hidden',
        clip: 'rect(0, 0, 0, 0)',
        whiteSpace: 'nowrap',
        border: 0,
      },

      // Skip link styles
      '.skip-link': {
        position: 'absolute',
        top: '-40px',
        left: '6px',
        background: accessibleColors.background.primary,
        color: accessibleColors.text.primary,
        padding: '8px',
        textDecoration: 'none',
        borderRadius: '0 0 4px 4px',
        zIndex: 1000,
        border: `2px solid ${customFocusColors.primary}`,

        '&:focus': {
          top: '0',
        },
      },
    },
  },
};

/**
 * Create custom accessible light theme with brand focus colors
 */
export function createCustomAccessibleLightTheme() {
  const baseTheme = createLightTheme();

  return createTheme({
    ...baseTheme,
    components: {
      ...baseTheme.components,
      ...customAccessibilityComponentOverrides,
    },
    palette: {
      ...baseTheme.palette,
      // Override with high contrast colors where needed
      text: {
        ...baseTheme.palette.text,
        primary: accessibleColors.text.primary,
        secondary: accessibleColors.text.secondary,
      },
      // Add accessibility-specific palette extensions
      accessibility: {
        focus: customFocusColors,
        touchTarget: touchTargetConfig,
      },
    },
  });
}

export default createCustomAccessibleLightTheme;