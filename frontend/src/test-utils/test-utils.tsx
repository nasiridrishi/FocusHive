import React from 'react';
import { render, RenderOptions, RenderResult, screen } from '@testing-library/react';
import type { User } from '@shared/types/auth';

// Import components from separate file to avoid Fast Refresh warnings
import { AllTheProviders } from './testProviders';

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
  withDatePickers?: boolean;
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
    withDatePickers = false,
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
      withDatePickers={withDatePickers}
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

// Custom utility function with different name to avoid conflict
export const waitForLoadingToDisappear = async (element: HTMLElement) => {
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

// Re-export form utilities from separate file
export { fillForm, submitForm } from './formUtils';

// Re-export specific functions from testing-library (not using * to avoid Fast Refresh warning)
export {
  act,
  cleanup,
  fireEvent,
  getByLabelText,
  getByPlaceholderText,
  getByText,
  getByAltText,
  getByTitle,
  getByDisplayValue,
  getByRole,
  getByTestId,
  queryByLabelText,
  queryByPlaceholderText,
  queryByText,
  queryByAltText,
  queryByTitle,
  queryByDisplayValue,
  queryByRole,
  queryByTestId,
  findByLabelText,
  findByPlaceholderText,
  findByText,
  findByAltText,
  findByTitle,
  findByDisplayValue,
  findByRole,
  findByTestId,
  getAllByLabelText,
  getAllByPlaceholderText,
  getAllByText,
  getAllByAltText,
  getAllByTitle,
  getAllByDisplayValue,
  getAllByRole,
  getAllByTestId,
  queryAllByLabelText,
  queryAllByPlaceholderText,
  queryAllByText,
  queryAllByAltText,
  queryAllByTitle,
  queryAllByDisplayValue,
  queryAllByRole,
  queryAllByTestId,
  findAllByLabelText,
  findAllByPlaceholderText,
  findAllByText,
  findAllByAltText,
  findAllByTitle,
  findAllByDisplayValue,
  findAllByRole,
  findAllByTestId,
  waitFor,
  waitForElementToBeRemoved,
  within,
  prettyDOM,
  configure,
  screen
} from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';

// Re-export the custom render as the default render
export { renderWithProviders as render };