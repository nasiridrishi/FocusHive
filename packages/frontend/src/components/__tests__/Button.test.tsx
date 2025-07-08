import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from '../Button';

describe('Button', () => {
  it('renders children correctly', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('calls onClick handler when clicked', () => {
    const handleClick = jest.fn();
    render(<Button onClick={handleClick}>Click me</Button>);
    
    fireEvent.click(screen.getByText('Click me'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('does not call onClick when disabled', () => {
    const handleClick = jest.fn();
    render(
      <Button onClick={handleClick} disabled>
        Click me
      </Button>
    );
    
    fireEvent.click(screen.getByText('Click me'));
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('renders with correct variant classes', () => {
    const { rerender } = render(<Button variant="primary">Primary</Button>);
    expect(screen.getByText('Primary')).toHaveClass('bg-blue-500');

    rerender(<Button variant="secondary">Secondary</Button>);
    expect(screen.getByText('Secondary')).toHaveClass('bg-gray-300');

    rerender(<Button variant="danger">Danger</Button>);
    expect(screen.getByText('Danger')).toHaveClass('bg-red-500');
  });

  it('renders with correct size classes', () => {
    const { rerender } = render(<Button size="small">Small</Button>);
    expect(screen.getByText('Small')).toHaveClass('text-sm');

    rerender(<Button size="medium">Medium</Button>);
    expect(screen.getByText('Medium')).toHaveClass('px-4');

    rerender(<Button size="large">Large</Button>);
    expect(screen.getByText('Large')).toHaveClass('text-lg');
  });

  it('renders with correct type attribute', () => {
    const { rerender } = render(<Button type="submit">Submit</Button>);
    expect(screen.getByText('Submit')).toHaveAttribute('type', 'submit');

    rerender(<Button type="reset">Reset</Button>);
    expect(screen.getByText('Reset')).toHaveAttribute('type', 'reset');
  });

  it('applies disabled styles when disabled', () => {
    render(<Button disabled>Disabled</Button>);
    expect(screen.getByText('Disabled')).toHaveClass('opacity-50');
    expect(screen.getByText('Disabled')).toBeDisabled();
  });
});