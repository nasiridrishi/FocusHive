import React from 'react';
import {render, RenderOptions, RenderResult, screen, act} from '@testing-library/react';
import type {User} from '@shared/types/auth';

// Import components from separate file to avoid Fast Refresh warnings
import {AllTheProviders} from './testProviders';

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

  const Wrapper = ({children}: { children: React.ReactNode }): React.ReactElement => (
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

  return render(ui, {wrapper: Wrapper, ...renderOptions});
}

// Act-wrapped render for handling async state updates
export function renderWithAct(
    ui: React.ReactElement,
    options: CustomRenderOptions = {}
): Promise<RenderResult> {
  return act(async () => {
    return renderWithProviders(ui, options);
  });
}

// Act-wrapped render for auth components specifically
export function renderAuthWithAct(
    ui: React.ReactElement,
    options: CustomRenderOptions = {}
): Promise<RenderResult> {
  return act(async () => {
    return renderWithProviders(ui, {
      ...options,
      withAuth: options.withAuth ?? false  // Default to false for manual auth provider
    });
  });
}

// Custom render for components that only need specific providers
export const renderWithAuth = (ui: React.ReactElement, {user}: {user?: User | null} = {}): RenderResult => {
  return renderWithProviders(ui, {
    user,
    withRouter: false,
    withQueryClient: false
  });
};

export const renderWithRouter = (ui: React.ReactElement, {initialEntries}: {initialEntries?: string[]} = {}): RenderResult => {
  return renderWithProviders(ui, {
    initialEntries,
    withAuth: false,
    withQueryClient: false
  });
};

export const renderWithTheme = (ui: React.ReactElement, _options: object = {}): RenderResult => {
  return renderWithProviders(ui, {
    withRouter: false,
    withAuth: false,
    withQueryClient: false
  });
};

export const renderWithQueryClient = (ui: React.ReactElement, _options: object = {}): RenderResult => {
  return renderWithProviders(ui, {
    withRouter: false,
    withAuth: false,
    withTheme: false,
    withI18n: false
  });
};

// Custom utility function with different name to avoid conflict
export const waitForLoadingToDisappear = async (element: HTMLElement): Promise<void> => {
  return screen.findByText(/loading/i).then(() => {
    // Wait for loading to disappear
    return new Promise((resolve) => {
      const observer = new MutationObserver(() => {
        if (!document.contains(element)) {
          observer.disconnect();
          resolve(undefined);
        }
      });
      observer.observe(document.body, {childList: true, subtree: true});
    });
  });
};

// Common test selectors and utilities
export const getByTextContent = (content: string, _unused: object = {}): HTMLElement => {
  return screen.getByText((_, element) => {
    return element?.textContent === content;
  });
};

export const queryByTextContent = (content: string, _unused: object = {}): HTMLElement | null => {
  return screen.queryByText((_, element) => {
    return element?.textContent === content;
  });
};

// Re-export form utilities from separate file
export {fillForm, submitForm} from './formUtils';

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
export {default as userEvent} from '@testing-library/user-event';

// Re-export the custom render as the default render
export {renderWithProviders as render};