/**
 * Theme with Disabled Focus Rings
 * 
 * WARNING: This theme removes focus indicators, which can make
 * your application less accessible for keyboard users and users
 * with disabilities. Use with caution.
 */

import { createTheme } from '@mui/material/styles';
import { createLightTheme } from './theme';

/**
 * Create theme with no focus rings (not recommended for accessibility)
 */
export function createNoFocusTheme() {
  const baseTheme = createLightTheme();

  return createTheme({
    ...baseTheme,
    components: {
      ...baseTheme.components,
      
      // Remove focus rings from text fields
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiInputBase-root': {
              '&:focus-within': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
              '&:focus': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
              '& input:focus': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
              '& textarea:focus': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
            },
            '& .MuiOutlinedInput-root': {
              '&:focus-within': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
              '&.Mui-focused': {
                outline: 'none !important',
                boxShadow: 'none !important',
              },
            },
          },
        },
      },

      // Remove focus rings from buttons
      MuiButton: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      // Remove focus rings from other components
      MuiIconButton: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      MuiCheckbox: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      MuiRadio: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      MuiSwitch: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      MuiLink: {
        styleOverrides: {
          root: {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },

      // Remove global focus rings
      MuiCssBaseline: {
        styleOverrides: {
          '*': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          'input': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          'textarea': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          'button': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          'a': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          // Additional comprehensive overrides
          'div[tabindex]': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          'select': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
            '&:focus-visible': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          // Override any potential MUI focus styles
          '.MuiInputBase-input': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          '.MuiOutlinedInput-input': {
            '&:focus': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
          '.MuiInputBase-root': {
            '&:focus-within': {
              outline: 'none !important',
              boxShadow: 'none !important',
            },
          },
        },
      },
    },
  });
}

export default createNoFocusTheme;