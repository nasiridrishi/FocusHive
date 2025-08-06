import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TaskCompletionRate } from './TaskCompletionRate';
import { TaskCompletionData } from '../types';

const mockData: TaskCompletionData = {
  completed: 23,
  total: 30,
  rate: 0.767,
  trend: 12.5,
  byPriority: {
    high: { completed: 8, total: 10 },
    medium: { completed: 10, total: 12 },
    low: { completed: 5, total: 8 }
  },
  byCategory: [
    { category: 'Development', completed: 8, total: 10, rate: 0.8 }
  ]
};

describe('Simple Analytics Tests', () => {
  it('TaskCompletionRate renders without crashing', () => {
    render(<TaskCompletionRate data={mockData} />);
    expect(screen.getByText('Task Completion Rate')).toBeInTheDocument();
  });

  it('TaskCompletionRate displays completion percentage', () => {
    render(<TaskCompletionRate data={mockData} />);
    expect(screen.getByText('76.7%')).toBeInTheDocument();
  });

  it('TaskCompletionRate shows completed task count', () => {
    render(<TaskCompletionRate data={mockData} />);
    expect(screen.getByText('23 of 30 tasks completed')).toBeInTheDocument();
  });
});