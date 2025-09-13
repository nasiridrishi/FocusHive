import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { LoadingSpinner, InlineSpinner, CenteredSpinner, OverlaySpinner } from '../LoadingSpinner';

describe('LoadingSpinner', () => {
  it('renders with default props', () => {
    render(<LoadingSpinner />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders with custom message', () => {
    render(<LoadingSpinner message="Loading data..." />);
    expect(screen.getByText('Loading data...')).toBeInTheDocument();
  });

  it('renders different sizes', () => {
    const { rerender } = render(<LoadingSpinner size="small" />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    
    rerender(<LoadingSpinner size="large" />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('does not render when disabled', () => {
    render(<LoadingSpinner disabled />);
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('renders inline variant correctly', () => {
    render(<InlineSpinner message="Loading..." />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders centered variant correctly', () => {
    render(<CenteredSpinner message="Loading..." />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders overlay variant correctly', () => {
    render(<OverlaySpinner message="Loading..." />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});