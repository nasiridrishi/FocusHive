/**
 * Accessible Theme Configuration
 * 
 * Extends the base theme with WCAG 2.1 AA compliant colors,
 * focus indicators, and accessibility-focused component overrides.
 */

import { createTheme, ThemeOptions } from '@mui/material/styles';
import { createLightTheme, createDarkTheme } from '../../theme/theme';
import { calculateContrastRatio, WCAG_CONTRAST_RATIOS } from '../utils/colorContrast';
import { CONTRAST_RATIOS, TOUCH_TARGETS, FOCUS } from '../constants/wcag';

/**
 * WCAG 2.1 AA Compliant Color Palette
 */
export const accessibleColors = {
  // High contrast text colors (7:1 ratio for AAA compliance)
  text: {
    primary: '#000000',        // 21:1 contrast on white
    secondary: '#424242',      // 9.7:1 contrast on white
    tertiary: '#616161',       // 5.7:1 contrast on white (AA large text)
    disabled: '#9e9e9e',       // 2.9:1 contrast (for disabled state)
  },
  
  // High contrast background colors
  background: {
    primary: '#ffffff',
    secondary: '#f5f5f5',      // Subtle background with maintained contrast
    tertiary: '#eeeeee',       // Card backgrounds
  },
  
  // Accessible focus indicators
  focus: {
    primary: '#005fcc',        // Blue focus ring (4.5:1 on white)
    secondary: '#0d47a1',      // Darker blue (7:1 on white)
    error: '#c62828',          // Red focus for errors (5.1:1 on white)
    warning: '#f57c00',        // Orange focus for warnings (3.4:1 on white, AA large)
    success: '#2e7d32',        // Green focus for success (4.9:1 on white)
  },
  
  // Dark theme accessible colors
  dark: {
    text: {
      primary: '#ffffff',      // 21:1 contrast on black
      secondary: '#e0e0e0',    // 12.6:1 contrast on black
      tertiary: '#bdbdbd',     // 7.4:1 contrast on black
      disabled: '#757575',     // 4.6:1 contrast on black
    },
    background: {
      primary: '#000000',
      secondary: '#121212',
      tertiary: '#1e1e1e',
    },
    focus: {
      primary: '#64b5f6',      // Light blue focus for dark theme
      secondary: '#90caf9',    // Lighter blue
      error: '#ef5350',        // Light red
      warning: '#ffb74d',      // Light orange
      success: '#81c784',      // Light green
    }
  }
};

/**
 * Focus ring configuration
 */
export const focusRingConfig = {
  width: FOCUS.FOCUS_RING_WIDTH,
  offset: FOCUS.FOCUS_RING_OFFSET,
  style: 'solid' as const,
  color: accessibleColors.focus.primary,
  
  // Focus ring variants
  variants: {
    default: {
      outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${accessibleColors.focus.primary}`,
      outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
    },
    inset: {
      boxShadow: `inset 0 0 0 ${FOCUS.FOCUS_RING_WIDTH}px ${accessibleColors.focus.primary}`,
    },
    double: {
      outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${accessibleColors.focus.primary}`,
      outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
      boxShadow: `0 0 0 ${FOCUS.FOCUS_RING_WIDTH + FOCUS.FOCUS_RING_OFFSET}px #ffffff`,
    }
  }
};

/**
 * Touch target configurations
 */
export const touchTargetConfig = {
  minSize: TOUCH_TARGETS.MIN_SIZE,
  recommendedSize: TOUCH_TARGETS.RECOMMENDED_SIZE,
  minSpacing: TOUCH_TARGETS.MIN_SPACING,
  recommendedSpacing: TOUCH_TARGETS.RECOMMENDED_SPACING,
};

/**
 * Accessibility-focused component overrides
 */
const accessibilityComponentOverrides: ThemeOptions['components'] = {
  // Enhanced button accessibility
  MuiButton: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Ensure minimum touch target size
        minHeight: touchTargetConfig.recommendedSize,
        minWidth: touchTargetConfig.recommendedSize,
        
        // Focus styles with high contrast
        '&:focus-visible': {
          ...focusRingConfig.variants.default,
          // Ensure focus is visible even with background
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
        
        // Ensure text contrast
        '&.MuiButton-contained': {
          color: theme.palette.getContrastText(theme.palette.primary.main),
        },
      }),
      
      // Size variants with accessibility considerations
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
        minHeight: 56, // Larger than recommended for better accessibility
        fontSize: '1.125rem',
        padding: '12px 24px',
      },
    },
  },
  
  // Enhanced form field accessibility
  MuiTextField: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Ensure proper spacing for touch targets
        marginBottom: touchTargetConfig.recommendedSpacing,
        
        '& .MuiInputBase-root': {
          minHeight: touchTargetConfig.recommendedSize,
          
          // Focus styles
          '&:focus-within': {
            ...focusRingConfig.variants.default,
          },
          
          // High contrast support
          '@media (prefers-contrast: high)': {
            border: '2px solid currentColor',
            '&:focus-within': {
              borderColor: theme.palette.primary.main,
              outline: '2px solid currentColor',
              outlineOffset: '2px',
            },
          },
        },
        
        // Error state accessibility
        '&.Mui-error .MuiInputBase-root': {
          '&:focus-within': {
            outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${accessibleColors.focus.error}`,
            outlineOffset: `${FOCUS.FOCUS_RING_OFFSET}px`,
          },
        },
      }),
    },
  },
  
  // Enhanced link accessibility
  MuiLink: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Ensure sufficient color contrast
        color: theme.palette.primary.main,
        textDecorationColor: 'currentColor',
        
        // Focus styles
        '&:focus-visible': {
          ...focusRingConfig.variants.default,
          borderRadius: '2px',
        },
        
        // Hover styles that work with high contrast
        '&:hover': {
          textDecoration: 'underline',
          textDecorationThickness: '2px',
          textUnderlineOffset: '2px',
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          textDecoration: 'underline',
          textDecorationThickness: '2px',
          '&:focus-visible': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced card accessibility
  MuiCard: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Focus styles for interactive cards
        '&[tabindex]:focus-visible': {
          ...focusRingConfig.variants.default,
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          border: '1px solid currentColor',
          '&[tabindex]:focus-visible': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced chip accessibility
  MuiChip: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Ensure minimum touch target
        minHeight: touchTargetConfig.minSize,
        
        // Focus styles
        '&:focus-visible': {
          ...focusRingConfig.variants.default,
        },
        
        // Clickable chips
        '&.MuiChip-clickable': {
          minHeight: touchTargetConfig.recommendedSize,
          '&:hover': {
            // Ensure hover state is visible in high contrast
            '@media (prefers-contrast: high)': {
              backgroundColor: 'ButtonHighlight',
              color: 'ButtonText',
            },
          },
        },
        
        // Delete button accessibility
        '& .MuiChip-deleteIcon': {
          minWidth: touchTargetConfig.minSize,
          minHeight: touchTargetConfig.minSize,
          
          '&:focus-visible': {
            ...focusRingConfig.variants.inset,
          },
        },
      }),
    },
  },
  
  // Enhanced fab accessibility
  MuiFab: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Ensure minimum size
        minWidth: touchTargetConfig.recommendedSize,
        minHeight: touchTargetConfig.recommendedSize,
        
        // Focus styles
        '&:focus-visible': {
          ...focusRingConfig.variants.default,
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          border: '2px solid currentColor',
          '&:focus-visible': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
      
      sizeSmall: {
        minWidth: touchTargetConfig.minSize,
        minHeight: touchTargetConfig.minSize,
      },
      
      sizeMedium: {
        minWidth: touchTargetConfig.recommendedSize,
        minHeight: touchTargetConfig.recommendedSize,
      },
      
      sizeLarge: {
        minWidth: 64,
        minHeight: 64,
      },
    },
  },
  
  // Enhanced tab accessibility
  MuiTab: {
    styleOverrides: {
      root: ({ theme }) => ({
        minHeight: touchTargetConfig.recommendedSize,
        minWidth: touchTargetConfig.recommendedSize,
        
        // Focus styles
        '&:focus-visible': {
          ...focusRingConfig.variants.default,
          zIndex: 1, // Ensure focus ring is above other tabs
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          border: '1px solid transparent',
          '&.Mui-selected': {
            borderColor: 'currentColor',
            borderBottomColor: 'transparent',
          },
          '&:focus-visible': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced checkbox accessibility
  MuiCheckbox: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Focus styles
        '&:focus-visible': {
          '& .MuiSvgIcon-root': {
            ...focusRingConfig.variants.default,
            borderRadius: '2px',
          },
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          '& .MuiSvgIcon-root': {
            border: '1px solid currentColor',
          },
          '&:focus-visible .MuiSvgIcon-root': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced radio accessibility
  MuiRadio: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Focus styles
        '&:focus-visible': {
          '& .MuiSvgIcon-root': {
            ...focusRingConfig.variants.default,
            borderRadius: '50%',
          },
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          '& .MuiSvgIcon-root': {
            border: '1px solid currentColor',
          },
          '&:focus-visible .MuiSvgIcon-root': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced switch accessibility
  MuiSwitch: {
    styleOverrides: {
      root: ({ theme }) => ({
        // Focus styles
        '&:focus-within': {
          '& .MuiSwitch-thumb': {
            ...focusRingConfig.variants.default,
          },
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          '& .MuiSwitch-track': {
            border: '1px solid currentColor',
          },
          '&:focus-within .MuiSwitch-thumb': {
            outline: '3px solid currentColor',
            outlineOffset: '2px',
          },
        },
      }),
    },
  },
  
  // Enhanced menu accessibility
  MuiMenu: {
    styleOverrides: {
      paper: ({ theme }) => ({
        // High contrast support
        '@media (prefers-contrast: high)': {
          border: '1px solid currentColor',
        },
      }),
    },
  },
  
  MuiMenuItem: {
    styleOverrides: {
      root: ({ theme }) => ({
        minHeight: touchTargetConfig.recommendedSize,
        
        // Focus styles
        '&:focus-visible': {
          backgroundColor: theme.palette.action.focus,
          outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${accessibleColors.focus.primary}`,
          outlineOffset: `-${FOCUS.FOCUS_RING_WIDTH}px`,
        },
        
        // High contrast support
        '@media (prefers-contrast: high)': {
          '&:focus-visible': {
            backgroundColor: 'Highlight',
            color: 'HighlightText',
            outline: '2px solid currentColor',
            outlineOffset: '-2px',
          },
        },
      }),
    },
  },
  
  // Global CSS baseline with accessibility enhancements
  MuiCssBaseline: {
    styleOverrides: {
      // Focus-visible polyfill support
      '*:focus:not(:focus-visible)': {
        outline: 'none',
      },
      
      '*:focus-visible': {
        outline: `${FOCUS.FOCUS_RING_WIDTH}px solid ${accessibleColors.focus.primary}`,
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
        border: `2px solid ${accessibleColors.focus.primary}`,
        
        '&:focus': {
          top: '0',
        },
      },
    },
  },
};

/**
 * Create accessible light theme
 */
export function createAccessibleLightTheme() {
  const baseTheme = createLightTheme();
  
  return createTheme({
    ...baseTheme,
    components: {
      ...baseTheme.components,
      ...accessibilityComponentOverrides,
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
        focus: accessibleColors.focus,
        touchTarget: touchTargetConfig,
      },
    },
  });
}

/**
 * Create accessible dark theme
 */
export function createAccessibleDarkTheme() {
  const baseTheme = createDarkTheme();
  
  return createTheme({
    ...baseTheme,
    components: {
      ...baseTheme.components,
      ...accessibilityComponentOverrides,
    },
    palette: {
      ...baseTheme.palette,
      // Override with high contrast colors for dark theme
      text: {
        ...baseTheme.palette.text,
        primary: accessibleColors.dark.text.primary,
        secondary: accessibleColors.dark.text.secondary,
      },
      // Dark theme accessibility extensions
      accessibility: {
        focus: accessibleColors.dark.focus,
        touchTarget: touchTargetConfig,
      },
    },
  });
}

/**
 * Validate theme accessibility
 */
export function validateThemeAccessibility(theme: any) {
  const issues: string[] = [];
  
  // Check text contrast ratios
  const textPrimaryRatio = calculateContrastRatio(
    theme.palette.text.primary,
    theme.palette.background.default
  );
  
  if (textPrimaryRatio < WCAG_CONTRAST_RATIOS.AA_NORMAL) {
    issues.push(`Primary text contrast ratio (${textPrimaryRatio.toFixed(2)}:1) does not meet WCAG AA standards`);
  }
  
  // Check focus indicator contrast
  const focusRatio = calculateContrastRatio(
    accessibleColors.focus.primary,
    theme.palette.background.default
  );
  
  if (focusRatio < CONTRAST_RATIOS.FOCUS_INDICATOR) {
    issues.push(`Focus indicator contrast ratio (${focusRatio.toFixed(2)}:1) does not meet requirements`);
  }
  
  return {
    isValid: issues.length === 0,
    issues,
    recommendations: issues.length > 0 ? [
      'Consider using higher contrast colors',
      'Test with real users who have visual impairments',
      'Use accessibility testing tools to verify compliance'
    ] : []
  };
}

// Theme variants with accessibility enhancements
export const accessibleThemeVariants = {
  light: createAccessibleLightTheme(),
  dark: createAccessibleDarkTheme(),
};

// Export accessibility utilities
export {
  accessibleColors,
  focusRingConfig,
  touchTargetConfig,
  accessibilityComponentOverrides,
};

// Type augmentations for custom palette properties
declare module '@mui/material/styles' {
  interface Palette {
    accessibility?: {
      focus: typeof accessibleColors.focus;
      touchTarget: typeof touchTargetConfig;
    };
  }

  interface PaletteOptions {
    accessibility?: {
      focus?: typeof accessibleColors.focus;
      touchTarget?: typeof touchTargetConfig;
    };
  }
}