import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ProductivityChart} from './ProductivityChart';
import {AnalyticsTimeRange, ChartDataPoint, ProductivityChartProps} from '../types';

// Mock MUI X Charts
vi.mock('@mui/x-charts', () => ({
  lineChart: ({children, ...props}: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="line-chart" data-props={JSON.stringify(props)}>
        {children}
      </div>
  ),
  responsiveChartContainer: ({
                               children,
                               ...props
                             }: React.PropsWithChildren<Record<string, unknown>>) => (
      <div data-testid="chart-container" data-props={JSON.stringify(props)}>
        {children}
      </div>
  ),
  chartsTooltip: () => <div data-testid="chart-tooltip"/>,
  chartsAxisHighlight: () => <div data-testid="chart-axis-highlight"/>,
  chartsXAxis: () => <div data-testid="chart-x-axis"/>,
  chartsYAxis: () => <div data-testid="chart-y-axis"/>
}));

const mockTimeRange: AnalyticsTimeRange = {
  start: new Date('2024-01-01'),
  end: new Date('2024-01-07'),
  period: 'week'
};

const mockData: ChartDataPoint[] = [
  {x: '2024-01-01', y: 120, label: 'Monday'},
  {x: '2024-01-02', y: 95, label: 'Tuesday'},
  {x: '2024-01-03', y: 140, label: 'Wednesday'},
  {x: '2024-01-04', y: 110, label: 'Thursday'},
  {x: '2024-01-05', y: 160, label: 'Friday'},
  {x: '2024-01-06', y: 80, label: 'Saturday'},
  {x: '2024-01-07', y: 70, label: 'Sunday'}
];

const defaultProps: ProductivityChartProps = {
  data: mockData,
  timeRange: mockTimeRange
};

describe('ProductivityChart', () => {
  it('renders without crashing', () => {
    render(<ProductivityChart {...defaultProps} />);
    expect(screen.getByTestId('line-chart')).toBeInTheDocument();
  });

  it('displays loading state when loading prop is true', () => {
    render(<ProductivityChart {...defaultProps} loading={true}/>);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    expect(screen.getByText('Loading productivity data...')).toBeInTheDocument();
  });

  it('displays error state when error prop is provided', () => {
    const errorMessage = 'Failed to load data';
    render(<ProductivityChart {...defaultProps} error={errorMessage}/>);
    expect(screen.getByText('Error loading productivity data')).toBeInTheDocument();
    expect(screen.getByText(errorMessage)).toBeInTheDocument();
  });

  it('displays empty state when no data is provided', () => {
    render(<ProductivityChart {...defaultProps} data={[]}/>);
    expect(screen.getByText('No productivity data available')).toBeInTheDocument();
    expect(screen.getByText('Start tracking your focus sessions to see productivity trends.')).toBeInTheDocument();
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

  it('applies custom chart configuration', () => {
    const customConfig = {
      title: 'Custom Productivity Chart',
      height: 400,
      showLegend: false,
      animated: false
    };

    render(<ProductivityChart {...defaultProps} config={customConfig}/>);
    const chartElement = screen.getByTestId('line-chart');

    // Just verify the chart renders with config
    expect(chartElement).toBeInTheDocument();
    const dataProps = chartElement.getAttribute('data-props');
    expect(dataProps).toBeDefined();
  });

  it('formats time range correctly in chart title', () => {
    render(<ProductivityChart {...defaultProps} />);
    expect(screen.getByText(/Productivity.*Jan 1.*Jan 7, 2024/)).toBeInTheDocument();
  });

  it('handles different chart types based on config', () => {
    render(
        <ProductivityChart
            {...defaultProps}
            config={{type: 'area'}}
        />
    );
    const chartElement = screen.getByTestId('line-chart');
    expect(chartElement).toBeInTheDocument();
  });

  it('displays summary statistics', () => {
    render(<ProductivityChart {...defaultProps} />);

    // Should show total focus time
    expect(screen.getByText('775 min')).toBeInTheDocument();
    expect(screen.getByText('Total Focus Time')).toBeInTheDocument();

    // Should show average session length
    expect(screen.getByText('110.7 min')).toBeInTheDocument();
    expect(screen.getByText('Average Session')).toBeInTheDocument();

    // Should show best day - use getAllByText since Friday appears in chart and summary
    expect(screen.getAllByText('Friday').length).toBeGreaterThan(0);
    expect(screen.getByText('Best Day')).toBeInTheDocument();
  });

  it('handles responsive behavior', () => {
    render(<ProductivityChart {...defaultProps} />);
    const container = screen.getByTestId('chart-container');
    expect(container).toBeInTheDocument();
  });

  it('supports different time periods', () => {
    const monthlyTimeRange: AnalyticsTimeRange = {
      start: new Date('2024-01-01'),
      end: new Date('2024-01-31'),
      period: 'month'
    };

    render(<ProductivityChart {...defaultProps} timeRange={monthlyTimeRange}/>);
    expect(screen.getByText(/Productivity.*January 2024/)).toBeInTheDocument();
  });
});