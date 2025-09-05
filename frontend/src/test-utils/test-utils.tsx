import React from 'react';
import { render, RenderOptions, RenderResult, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { I18nextProvider } from 'react-i18next';
import i18n from '@lib/i18n';
import theme from '@shared/theme/theme';
import { AuthProvider } from '@features/auth/contexts/AuthContext';
import type { User } from '@shared/types/auth';

// Custom render options
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[];
  user?: User | null;
  preloadedState?: Record<string, unknown>;
  withRouter?: boolean;
  withAuth?: boolean;
  withTheme?: boolean;
  withI18n?: boolean;
  withQueryClient?: boolean;
}

// Mock user for testing
export const mockUser: User = {
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: undefined,
  isEmailVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-15T00:00:00Z'
};

// Create a test query client with disabled retries and logging
export const createTestQueryClient = () => {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });
};

// Mock AuthContext for testing
const MockAuthProvider: React.FC<{ 
  children: React.ReactNode; 
  user?: User | null 
}> = ({ children, user = null }) => {
  const mockAuthValue = {
    user,
    isAuthenticated: !!user,
    loading: false,
    error: null,
    login: jest.fn(),
    register: jest.fn(),
    logout: jest.fn(),
    refreshToken: jest.fn(),
    clearError: jest.fn(),
  };

  // For testing, we'll just use the regular AuthProvider
  // In a real test scenario, you might want to mock the context value
  return (
    <AuthProvider>
      {children}
    </AuthProvider>
  );
};

// All providers wrapper
function AllTheProviders({
  children,
  initialEntries = ['/'],
  user = null,
  withRouter = true,
  withAuth = true,
  withTheme = true,
  withI18n = true,
  withQueryClient = true,
}: {
  children: React.ReactNode;
} & Pick<
  CustomRenderOptions,
  | 'initialEntries'
  | 'user'
  | 'withRouter'
  | 'withAuth'
  | 'withTheme'
  | 'withI18n'
  | 'withQueryClient'
>) {
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

// Custom render function
export function renderWithProviders(
  ui: React.ReactElement,
  options: CustomRenderOptions = {}
): RenderResult {
  const {
    initialEntries,
    user,
    withRouter = true,
    withAuth = true,
    withTheme = true,
    withI18n = true,
    withQueryClient = true,
    ...renderOptions
  } = options;

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <AllTheProviders
      initialEntries={initialEntries}
      user={user}
      withRouter={withRouter}
      withAuth={withAuth}
      withTheme={withTheme}
      withI18n={withI18n}
      withQueryClient={withQueryClient}
    >
      {children}
    </AllTheProviders>
  );

  return render(ui, { wrapper: Wrapper, ...renderOptions });
}

// Custom render for components that only need specific providers
export const renderWithAuth = (ui: React.ReactElement, user?: User | null) => {
  return renderWithProviders(ui, { 
    user, 
    withRouter: false, 
    withQueryClient: false 
  });
};

export const renderWithRouter = (ui: React.ReactElement, initialEntries?: string[]) => {
  return renderWithProviders(ui, { 
    initialEntries, 
    withAuth: false, 
    withQueryClient: false 
  });
};

export const renderWithTheme = (ui: React.ReactElement) => {
  return renderWithProviders(ui, { 
    withRouter: false, 
    withAuth: false, 
    withQueryClient: false 
  });
};

export const renderWithQueryClient = (ui: React.ReactElement) => {
  return renderWithProviders(ui, { 
    withRouter: false, 
    withAuth: false, 
    withTheme: false, 
    withI18n: false 
  });
};

// Utility functions for common test scenarios
export const waitForElementToBeRemoved = async (element: HTMLElement) => {
  return screen.findByText(/loading/i).then(() => {
    // Wait for loading to disappear
    return new Promise((resolve) => {
      const observer = new MutationObserver(() => {
        if (!document.contains(element)) {
          observer.disconnect();
          resolve(undefined);
        }
      });
      observer.observe(document.body, { childList: true, subtree: true });
    });
  });
};

// Common test selectors and utilities
export const getByTextContent = (content: string) => {
  return screen.getByText((_, element) => {
    return element?.textContent === content;
  });
};

export const queryByTextContent = (content: string) => {
  return screen.queryByText((_, element) => {
    return element?.textContent === content;
  });
};

// Form testing utilities
export const fillForm = async (formData: Record<string, string>) => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();

  for (const [field, value] of Object.entries(formData)) {
    const input = screen.getByLabelText(new RegExp(field, 'i'));
    await user.clear(input);
    await user.type(input, value);
  }
};

export const submitForm = async () => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();

  const submitButton = screen.getByRole('button', { name: /submit|login|register/i });
  await user.click(submitButton);
};

// Re-export everything from testing-library
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';

// Re-export the custom render as the default render
export { renderWithProviders as render };