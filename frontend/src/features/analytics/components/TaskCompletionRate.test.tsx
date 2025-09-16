import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {TaskCompletionRate} from './TaskCompletionRate';
import {TaskCompletionData, TaskCompletionRateProps} from '../types';

const mockData: TaskCompletionData = {
  completed: 23,
  total: 30,
  rate: 0.767,
  trend: 12.5,
  byPriority: {
    high: {completed: 8, total: 10},
    medium: {completed: 10, total: 12},
    low: {completed: 5, total: 8}
  },
  byCategory: [
    {category: 'Development', completed: 8, total: 10, rate: 0.8},
    {category: 'Design', completed: 6, total: 8, rate: 0.75},
    {category: 'Research', completed: 5, total: 7, rate: 0.714},
    {category: 'Planning', completed: 4, total: 5, rate: 0.8}
  ]
};

const defaultProps: TaskCompletionRateProps = {
  data: mockData
};

describe('TaskCompletionRate', () => {
  it('renders without crashing', () => {
    render(<TaskCompletionRate {...defaultProps} />);
    expect(screen.getByText('Task Completion Rate')).toBeInTheDocument();
  });

  it('displays completion rate percentage correctly', () => {
    render(<TaskCompletionRate {...defaultProps} />);
    expect(screen.getByText('76.7%')).toBeInTheDocument();
  });

  it('displays completed and total task counts', () => {
    render(<TaskCompletionRate {...defaultProps} />);
    expect(screen.getByText('23 of 30 tasks completed')).toBeInTheDocument();
  });

  it('shows positive trend indicator', () => {
    render(<TaskCompletionRate {...defaultProps} showTrend={true}/>);
    expect(screen.getByText('+12.5%')).toBeInTheDocument();
    expect(screen.getByLabelText('trending up')).toBeInTheDocument();
  });

  it('shows negative trend indicator', () => {
    const dataWithNegativeTrend = {...mockData, trend: -8.2};
    render(<TaskCompletionRate data={dataWithNegativeTrend} showTrend={true}/>);
    expect(screen.getByText('-8.2%')).toBeInTheDocument();
    expect(screen.getByLabelText('trending down')).toBeInTheDocument();
  });

  it('hides trend when showTrend is false', () => {
    render(<TaskCompletionRate {...defaultProps} showTrend={false}/>);
    expect(screen.queryByText('+12.5%')).not.toBeInTheDocument();
  });

  it('displays priority breakdown when showBreakdown is true', async () => {
    render(<TaskCompletionRate {...defaultProps} showBreakdown={true}/>);

    // Accordion should be present
    expect(screen.getByText('By Priority')).toBeInTheDocument();

    // Look for priority data within collapsed accordion
    expect(screen.getByText(/8\/10/)).toBeInTheDocument();
    expect(screen.getByText(/10\/12/)).toBeInTheDocument();
    expect(screen.getByText(/5\/8/)).toBeInTheDocument();
  });

  it('displays category breakdown when showBreakdown is true', () => {
    render(<TaskCompletionRate {...defaultProps} showBreakdown={true}/>);

    // Check that breakdown accordions are present
    expect(screen.getByText('By Priority')).toBeInTheDocument();
    expect(screen.getByText('By Category')).toBeInTheDocument();

    // Check that some category names are present
    expect(screen.getByText('Development')).toBeInTheDocument();
    expect(screen.getByText('Design')).toBeInTheDocument();
    expect(screen.getByText('Research')).toBeInTheDocument();
    expect(screen.getByText('Planning')).toBeInTheDocument();
  });

  it('renders in card variant by default', () => {
    render(<TaskCompletionRate {...defaultProps} />);
    expect(screen.getByRole('region')).toHaveClass('MuiCard-root');
  });

  it('renders in widget variant', () => {
    render(<TaskCompletionRate {...defaultProps} variant="widget"/>);
    const container = screen.getByTestId('task-completion-widget');
    expect(container).toBeInTheDocument();
  });

  it('renders in detailed variant with all sections', () => {
    render(<TaskCompletionRate {...defaultProps} variant="detailed"/>);

    // Should include breakdown by default in detailed variant
    expect(screen.getByText('By Priority')).toBeInTheDocument();
    expect(screen.getByText('By Category')).toBeInTheDocument();
  });

  it('displays circular progress indicator', () => {
    render(<TaskCompletionRate {...defaultProps} />);
    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toBeInTheDocument();
    expect(progressBar).toHaveAttribute('aria-valuenow', '76.7');
  });

  it('handles zero completion rate', () => {
    const zeroData: TaskCompletionData = {
      ...mockData,
      completed: 0,
      rate: 0
    };
    render(<TaskCompletionRate data={zeroData}/>);
    expect(screen.getByText('0.0%')).toBeInTheDocument();
  });

  it('handles perfect completion rate', () => {
    const perfectData: TaskCompletionData = {
      ...mockData,
      completed: 30,
      rate: 1.0
    };
    render(<TaskCompletionRate data={perfectData}/>);
    expect(screen.getByText('100.0%')).toBeInTheDocument();
  });

  it('applies correct color coding for completion rates', () => {
    // High completion rate (green)
    render(<TaskCompletionRate {...defaultProps} />);
    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toHaveClass('MuiCircularProgress-colorSuccess');
  });

  it('applies warning color for medium completion rates', () => {
    const mediumData: TaskCompletionData = {
      ...mockData,
      completed: 15,
      rate: 0.5
    };
    render(<TaskCompletionRate data={mediumData}/>);
    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toHaveClass('MuiCircularProgress-colorWarning');
  });

  it('applies error color for low completion rates', () => {
    const lowData: TaskCompletionData = {
      ...mockData,
      completed: 5,
      rate: 0.167
    };
    render(<TaskCompletionRate data={lowData}/>);
    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toHaveClass('MuiCircularProgress-colorError');
  });
});