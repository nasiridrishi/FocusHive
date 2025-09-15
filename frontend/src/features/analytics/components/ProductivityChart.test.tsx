import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ProductivityChart, type ProductivityChartProps, type ProductivityData} from './ProductivityChart';

// Mock recharts
vi.mock('recharts', () => ({
  LineChart: ({children, ...props}: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="line-chart" data-props={JSON.stringify(props)}>
        {children}
      </div>
  ),
  AreaChart: ({children, ...props}: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="area-chart" data-props={JSON.stringify(props)}>
        {children}
      </div>
  ),
  BarChart: ({children, ...props}: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="bar-chart" data-props={JSON.stringify(props)}>
        {children}
      </div>
  ),
  ResponsiveContainer: ({children}: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="responsive-container">
        {children}
      </div>
  ),
  XAxis: () => <div data-testid="chart-x-axis"/>,
  YAxis: () => <div data-testid="chart-y-axis"/>,
  CartesianGrid: () => <div data-testid="chart-grid"/>,
  Tooltip: () => <div data-testid="chart-tooltip"/>,
  Legend: () => <div data-testid="chart-legend"/>,
  Line: () => <div data-testid="chart-line"/>,
  Area: () => <div data-testid="chart-area"/>,
  Bar: () => <div data-testid="chart-bar"/>
}));

const mockData: ProductivityData[] = [
  {date: '2024-01-01', focusTime: 120, breakTime: 20, sessions: 3},
  {date: '2024-01-02', focusTime: 95, breakTime: 15, sessions: 2},
  {date: '2024-01-03', focusTime: 140, breakTime: 25, sessions: 4},
  {date: '2024-01-04', focusTime: 110, breakTime: 18, sessions: 3},
  {date: '2024-01-05', focusTime: 160, breakTime: 30, sessions: 5},
  {date: '2024-01-06', focusTime: 80, breakTime: 12, sessions: 2},
  {date: '2024-01-07', focusTime: 70, breakTime: 10, sessions: 2}
];

const defaultProps: ProductivityChartProps = {
  data: mockData,
  view: 'daily',
  onViewChange: vi.fn()
};

describe('ProductivityChart', () => {
  it('renders without crashing', () => {
    render(<ProductivityChart {...defaultProps} />);
    expect(screen.getByTestId('productivity-chart')).toBeInTheDocument();
  });

  it('displays loading state when isLoading prop is true', () => {
    render(<ProductivityChart {...defaultProps} isLoading={true}/>);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('displays error state when error prop is provided', () => {
    const errorMessage = 'Failed to load data';
    render(<ProductivityChart {...defaultProps} error={errorMessage}/>);
    expect(screen.getByText(errorMessage)).toBeInTheDocument();
  });

  it('displays empty state when no data is provided', () => {
    render(<ProductivityChart {...defaultProps} data={[]}/>);
    expect(screen.getByText('No productivity data available')).toBeInTheDocument();
    expect(screen.getByText('Start a focus session to see your productivity')).toBeInTheDocument();
  });

  it('renders chart with correct data points', () => {
    render(<ProductivityChart {...defaultProps} />);
    const chartElement = screen.getByTestId('line-chart');
    expect(chartElement).toBeInTheDocument();

    // Verify chart is rendered with data-props attribute
    const dataProps = chartElement.getAttribute('data-props');
    expect(dataProps).toBeDefined();
    expect(dataProps).not.toBe('{}');
  });

  it('applies custom chart type', () => {
    render(<ProductivityChart {...defaultProps} type="bar"/>);
    const chartElement = screen.getByTestId('bar-chart');

    // Just verify the chart renders with custom type
    expect(chartElement).toBeInTheDocument();
    const dataProps = chartElement.getAttribute('data-props');
    expect(dataProps).toBeDefined();
  });

  it('formats time range correctly in chart title', () => {
    render(<ProductivityChart {...defaultProps} />);
    expect(screen.getByText(/Productivity.*Jan 1.*Jan 7, 2024/)).toBeInTheDocument();
  });

  it('handles different chart types', () => {
    render(
        <ProductivityChart
            {...defaultProps}
            type="area"
        />
    );
    const chartElement = screen.getByTestId('area-chart');
    expect(chartElement).toBeInTheDocument();
  });

  it('displays summary statistics', () => {
    render(<ProductivityChart {...defaultProps} />);

    // Should show total focus time
    expect(screen.getByTestId('total-focus-time')).toBeInTheDocument();
    expect(screen.getByText('Total Focus Time')).toBeInTheDocument();

    // Should show average session time
    expect(screen.getByTestId('average-session-time')).toBeInTheDocument();
    expect(screen.getByText('Average Session Time')).toBeInTheDocument();

    // Should show total sessions
    expect(screen.getByTestId('total-sessions')).toBeInTheDocument();
    expect(screen.getByText('Total Sessions')).toBeInTheDocument();
  });

  it('handles responsive behavior', () => {
    render(<ProductivityChart {...defaultProps} />);
    const container = screen.getByTestId('responsive-container');
    expect(container).toBeInTheDocument();
  });

  it('supports different view types', () => {
    render(<ProductivityChart {...defaultProps} view="monthly"/>);
    expect(screen.getByText('Productivity Overview')).toBeInTheDocument();
  });
});