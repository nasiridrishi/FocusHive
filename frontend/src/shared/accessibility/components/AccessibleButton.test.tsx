import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test-utils/test-utils';
import { createA11yTestSuite } from '@/test-utils/accessibility-utils';
import AccessibleButton from './AccessibleButton';

const renderAccessibleButton = (props = {}) => {
  return renderWithProviders(
    <AccessibleButton {...props}>
      Click me
    </AccessibleButton>
  );
};

describe('AccessibleButton', () => {
  it('renders with correct ARIA attributes', () => {
    renderWithProviders(
      <AccessibleButton aria-label="Custom button label">
        Click me
      </AccessibleButton>
    );

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('aria-label', 'Custom button label');
    expect(button).toHaveTextContent('Click me');
  });

  it('supports disabled state with proper ARIA', () => {
    renderWithProviders(
      <AccessibleButton disabled>
        Disabled button
      </AccessibleButton>
    );

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
  });

  it('supports loading state with proper ARIA', () => {
    renderWithProviders(
      <AccessibleButton loading>
        Loading button
      </AccessibleButton>
    );

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button).toHaveAttribute('disabled');
  });

  it('handles keyboard interactions', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();

    renderWithProviders(
      <AccessibleButton onClick={onClick}>
        Click me
      </AccessibleButton>
    );

    const button = screen.getByRole('button');
    
    // Should be focusable
    await user.tab();
    expect(button).toHaveFocus();

    // Should activate with Enter
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(1);

    // Should activate with Space
    await user.keyboard(' ');
    expect(onClick).toHaveBeenCalledTimes(2);
  });

  it('provides proper focus management', async () => {
    const user = userEvent.setup();

    renderWithProviders(
      <div>
        <AccessibleButton>First</AccessibleButton>
        <AccessibleButton>Second</AccessibleButton>
      </div>
    );

    const firstButton = screen.getByRole('button', { name: 'First' });
    const secondButton = screen.getByRole('button', { name: 'Second' });

    // Tab navigation
    await user.tab();
    expect(firstButton).toHaveFocus();

    await user.tab();
    expect(secondButton).toHaveFocus();

    // Shift+tab navigation
    await user.tab({ shift: true });
    expect(firstButton).toHaveFocus();
  });

  it('supports different variants with proper semantics', () => {
    const { rerender } = renderWithProviders(
      <AccessibleButton variant="primary">
        Primary
      </AccessibleButton>
    );

    let button = screen.getByRole('button');
    expect(button).toHaveClass(/primary/i);

    rerender(
      <AccessibleButton variant="secondary">
        Secondary
      </AccessibleButton>
    );

    button = screen.getByRole('button');
    expect(button).toHaveClass(/secondary/i);
  });

  it('handles icon buttons with proper labels', () => {
    renderWithProviders(
      <AccessibleButton 
        aria-label="Close dialog"
        variant="icon"
      >
        <span aria-hidden="true">×</span>
      </AccessibleButton>
    );

    const button = screen.getByRole('button', { name: 'Close dialog' });
    expect(button).toBeInTheDocument();
    
    const icon = button.querySelector('[aria-hidden="true"]');
    expect(icon).toBeInTheDocument();
  });

  it('supports tooltip functionality with proper ARIA', () => {
    renderWithProviders(
      <AccessibleButton
        title="This is a tooltip"
        aria-describedby="button-tooltip"
      >
        Hover me
      </AccessibleButton>
    );

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('title', 'This is a tooltip');
    expect(button).toHaveAttribute('aria-describedby', 'button-tooltip');
  });

  it('handles form submission buttons correctly', () => {
    renderWithProviders(
      <form>
        <AccessibleButton type="submit">
          Submit Form
        </AccessibleButton>
      </form>
    );

    const button = screen.getByRole('button', { name: 'Submit Form' });
    expect(button).toHaveAttribute('type', 'submit');
  });
});

// Create comprehensive accessibility test suite
createA11yTestSuite('AccessibleButton', renderAccessibleButton, {
  skipLandmarks: true, // Buttons don't need landmark structure
  skipHeadingStructure: true, // Buttons don't contain headings
});