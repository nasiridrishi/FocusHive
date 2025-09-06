import { useTheme as useMuiTheme } from '@mui/material/styles';

// Re-export Material-UI's useTheme hook
export const useTheme = useMuiTheme;

export const useThemeMode = () => {
  const theme = useMuiTheme();
  return theme.palette.mode;
};

// Note: Material-UI theme switching should be handled at the app level
// This is a simplified implementation - for dynamic theme switching,
// you would need to implement a theme context at the app level
export const useToggleTheme = () => {
  console.warn('useToggleTheme not implemented - theme switching should be handled at app level');
  return () => {
    // Placeholder - theme switching needs app-level implementation
  };
};