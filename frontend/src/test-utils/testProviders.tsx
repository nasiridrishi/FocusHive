import React from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { I18nextProvider } from 'react-i18next';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import i18n from '@lib/i18n';
import theme from '@shared/theme/theme';
import type { User } from '@shared/types/auth';
import { AuthStateContext, AuthActionsContext } from '../features/auth/contexts/authContexts';
import { createTestQueryClient } from './testSetup';

// Mock AuthProvider for testing
interface MockAuthProviderProps {
  children: React.ReactNode;
  user?: User | null;
}

export function MockAuthProvider({ children, user = null }: MockAuthProviderProps) {
  const mockAuthState = {
    user,
    token: user ? 'mock-token' : null,
    refreshToken: user ? 'mock-refresh-token' : null,
    isLoading: false,
    isAuthenticated: !!user,
    error: null
  };

  const mockAuthActions = {
    login: async () => {},
    register: async () => {},
    logout: async () => {},
    refreshAuth: async () => {},
    updateProfile: async () => {},
    changePassword: async () => {},
    requestPasswordReset: async () => ({ message: 'Mock reset request' }),
    clearError: () => {}
  };

  return (
    <AuthStateContext.Provider value={mockAuthState}>
      <AuthActionsContext.Provider value={mockAuthActions}>
        {children}
      </AuthActionsContext.Provider>
    </AuthStateContext.Provider>
  );
}

// All providers wrapper
interface AllTheProvidersProps {
  children: React.ReactNode;
  initialEntries?: string[];
  user?: User | null;
  withRouter?: boolean;
  withAuth?: boolean;
  withTheme?: boolean;
  withI18n?: boolean;
  withQueryClient?: boolean;
  withDatePickers?: boolean;
}

export function AllTheProviders({
  children,
  initialEntries = ['/'],
  user = null,
  withRouter = true,
  withAuth = true,
  withTheme = true,
  withI18n = true,
  withQueryClient = true,
  withDatePickers = true,
}: AllTheProvidersProps) {
  const queryClient = createTestQueryClient();

  let Wrapper: React.ComponentType<{ children: React.ReactNode }> = ({ children }) => (
    <>{children}</>
  );

  // Apply providers conditionally
  if (withQueryClient) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>
        <PrevWrapper>{children}</PrevWrapper>
      </QueryClientProvider>
    );
  }

  if (withI18n) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <I18nextProvider i18n={i18n}>
        <PrevWrapper>{children}</PrevWrapper>
      </I18nextProvider>
    );
  }

  if (withDatePickers) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <PrevWrapper>{children}</PrevWrapper>
      </LocalizationProvider>
    );
  }

  if (withTheme) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <PrevWrapper>{children}</PrevWrapper>
      </ThemeProvider>
    );
  }

  if (withAuth) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <MockAuthProvider user={user}>
        <PrevWrapper>{children}</PrevWrapper>
      </MockAuthProvider>
    );
  }

  if (withRouter) {
    const PrevWrapper = Wrapper;
    Wrapper = ({ children }) => (
      <MemoryRouter initialEntries={initialEntries}>
        <PrevWrapper>{children}</PrevWrapper>
      </MemoryRouter>
    );
  }

  return <Wrapper>{children}</Wrapper>;
}