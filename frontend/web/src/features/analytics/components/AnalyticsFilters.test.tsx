import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AnalyticsFilters } from './AnalyticsFilters';
import { AnalyticsFiltersProps, AnalyticsFilter } from '../types';

const mockFilter: AnalyticsFilter = {
  timeRange: {
    start: new Date('2024-01-01'),
    end: new Date('2024-01-31'),
    period: 'month'
  },
  viewType: 'individual',
  selectedHives: ['hive-1'],
  selectedMembers: ['user-1', 'user-2'],
  metrics: ['focus-time', 'sessions', 'goals']
};

const mockHives = [
  { id: 'hive-1', name: 'Development Team' },
  { id: 'hive-2', name: 'Design Squad' },
  { id: 'hive-3', name: 'Research Group' }
];

const mockMembers = [
  { id: 'user-1', name: 'Alice Johnson' },
  { id: 'user-2', name: 'Bob Smith' },
  { id: 'user-3', name: 'Carol Davis' },
  { id: 'user-4', name: 'David Wilson' }
];

const defaultProps: AnalyticsFiltersProps = {
  filter: mockFilter,
  onFilterChange: vi.fn(),
  availableHives: mockHives,
  availableMembers: mockMembers
};

describe('AnalyticsFilters', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('displays time range selector with current period', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Time Range')).toBeInTheDocument();
    expect(screen.getByDisplayValue('month')).toBeInTheDocument();
  });

  it('shows predefined time range options', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    const timeRangeSelect = screen.getByDisplayValue('month');
    fireEvent.mouseDown(timeRangeSelect);
    
    expect(screen.getByText('Today')).toBeInTheDocument();
    expect(screen.getByText('This Week')).toBeInTheDocument();
    expect(screen.getByText('This Month')).toBeInTheDocument();
    expect(screen.getByText('This Quarter')).toBeInTheDocument();
    expect(screen.getByText('This Year')).toBeInTheDocument();
    expect(screen.getByText('Custom Range')).toBeInTheDocument();
  });

  it('changes time range when new period is selected', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    const timeRangeSelect = screen.getByDisplayValue('month');
    fireEvent.mouseDown(timeRangeSelect);
    fireEvent.click(screen.getByText('This Week'));
    
    expect(onFilterChange).toHaveBeenCalledWith({
      timeRange: expect.objectContaining({
        period: 'week'
      })
    });
  });

  it('shows custom date pickers when custom range is selected', () => {
    const customFilter = {
      ...mockFilter,
      timeRange: { ...mockFilter.timeRange, period: 'custom' as const }
    };
    
    render(<AnalyticsFilters {...defaultProps} filter={customFilter} />);
    
    expect(screen.getByLabelText('Start Date')).toBeInTheDocument();
    expect(screen.getByLabelText('End Date')).toBeInTheDocument();
  });

  it('displays view type selector with current selection', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('View Type')).toBeInTheDocument();
    expect(screen.getByText('Individual')).toBeInTheDocument();
  });

  it('shows view type options', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Individual')).toBeInTheDocument();
    expect(screen.getByText('Hive')).toBeInTheDocument();
    expect(screen.getByText('Comparison')).toBeInTheDocument();
  });

  it('changes view type when toggled', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    fireEvent.click(screen.getByText('Hive'));
    
    expect(onFilterChange).toHaveBeenCalledWith({
      viewType: 'hive'
    });
  });

  it('shows hive selector when hives are available', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Select Hives')).toBeInTheDocument();
    expect(screen.getByText('Development Team')).toBeInTheDocument();
  });

  it('allows multiple hive selection', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    const hiveCheckbox = screen.getByLabelText('Design Squad');
    fireEvent.click(hiveCheckbox);
    
    expect(onFilterChange).toHaveBeenCalledWith({
      selectedHives: ['hive-1', 'hive-2']
    });
  });

  it('shows member selector when members are available', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Select Members')).toBeInTheDocument();
    expect(screen.getByText('Alice Johnson')).toBeInTheDocument();
    expect(screen.getByText('Bob Smith')).toBeInTheDocument();
  });

  it('allows multiple member selection', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    const memberCheckbox = screen.getByLabelText('Carol Davis');
    fireEvent.click(memberCheckbox);
    
    expect(onFilterChange).toHaveBeenCalledWith({
      selectedMembers: ['user-1', 'user-2', 'user-3']
    });
  });

  it('displays metrics selector with current selections', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByLabelText('Focus Time')).toBeChecked();
    expect(screen.getByLabelText('Sessions')).toBeChecked();
    expect(screen.getByLabelText('Goals')).toBeChecked();
    expect(screen.getByLabelText('Engagement')).not.toBeChecked();
    expect(screen.getByLabelText('Productivity')).not.toBeChecked();
  });

  it('toggles metrics when clicked', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    const engagementCheckbox = screen.getByLabelText('Engagement');
    fireEvent.click(engagementCheckbox);
    
    expect(onFilterChange).toHaveBeenCalledWith({
      metrics: ['focus-time', 'sessions', 'goals', 'engagement']
    });
  });

  it('removes metrics when unchecked', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    const focusTimeCheckbox = screen.getByLabelText('Focus Time');
    fireEvent.click(focusTimeCheckbox);
    
    expect(onFilterChange).toHaveBeenCalledWith({
      metrics: ['sessions', 'goals']
    });
  });

  it('renders in compact mode when specified', () => {
    render(<AnalyticsFilters {...defaultProps} compact={true} />);
    
    const container = screen.getByTestId('analytics-filters');
    expect(container).toHaveClass('filters-compact');
  });

  it('shows reset filters button', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    expect(screen.getByText('Reset Filters')).toBeInTheDocument();
  });

  it('resets filters when reset button is clicked', () => {
    const onFilterChange = vi.fn();
    render(<AnalyticsFilters {...defaultProps} onFilterChange={onFilterChange} />);
    
    fireEvent.click(screen.getByText('Reset Filters'));
    
    expect(onFilterChange).toHaveBeenCalledWith({
      timeRange: {
        start: expect.any(Date),
        end: expect.any(Date),
        period: 'week'
      },
      viewType: 'individual',
      selectedHives: [],
      selectedMembers: [],
      metrics: ['focus-time', 'sessions']
    });
  });

  it('hides hive selector when no hives available', () => {
    render(<AnalyticsFilters {...defaultProps} availableHives={[]} />);
    
    expect(screen.queryByText('Select Hives')).not.toBeInTheDocument();
  });

  it('hides member selector when no members available', () => {
    render(<AnalyticsFilters {...defaultProps} availableMembers={[]} />);
    
    expect(screen.queryByText('Select Members')).not.toBeInTheDocument();
  });

  it('shows active filter count badge', () => {
    render(<AnalyticsFilters {...defaultProps} />);
    
    // Should show count of active filters (selected hives + members + metrics)
    expect(screen.getByText('6')).toBeInTheDocument(); // 1 hive + 2 members + 3 metrics
  });

  it('updates start date when custom date is changed', () => {
    const onFilterChange = vi.fn();
    const customFilter = {
      ...mockFilter,
      timeRange: { ...mockFilter.timeRange, period: 'custom' as const }
    };
    
    render(<AnalyticsFilters {...defaultProps} filter={customFilter} onFilterChange={onFilterChange} />);
    
    const startDateInput = screen.getByLabelText('Start Date');
    fireEvent.change(startDateInput, { target: { value: '2024-02-01' } });
    
    expect(onFilterChange).toHaveBeenCalledWith({
      timeRange: expect.objectContaining({
        start: new Date('2024-02-01')
      })
    });
  });

  it('updates end date when custom date is changed', () => {
    const onFilterChange = vi.fn();
    const customFilter = {
      ...mockFilter,
      timeRange: { ...mockFilter.timeRange, period: 'custom' as const }
    };
    
    render(<AnalyticsFilters {...defaultProps} filter={customFilter} onFilterChange={onFilterChange} />);
    
    const endDateInput = screen.getByLabelText('End Date');
    fireEvent.change(endDateInput, { target: { value: '2024-02-29' } });
    
    expect(onFilterChange).toHaveBeenCalledWith({
      timeRange: expect.objectContaining({
        end: new Date('2024-02-29')
      })
    });
  });

  it('validates that end date is after start date', () => {
    const customFilter = {
      ...mockFilter,
      timeRange: { 
        start: new Date('2024-02-01'), 
        end: new Date('2024-01-15'), // End before start
        period: 'custom' as const 
      }
    };
    
    render(<AnalyticsFilters {...defaultProps} filter={customFilter} />);
    
    expect(screen.getByText('End date must be after start date')).toBeInTheDocument();
  });

  it('disables member selector when view type is individual', () => {
    const individualFilter = { ...mockFilter, viewType: 'individual' as const };
    render(<AnalyticsFilters {...defaultProps} filter={individualFilter} />);
    
    const memberSection = screen.getByTestId('member-selector');
    expect(memberSection).toHaveAttribute('aria-disabled', 'true');
  });

  it('enables member selector when view type is comparison', () => {
    const comparisonFilter = { ...mockFilter, viewType: 'comparison' as const };
    render(<AnalyticsFilters {...defaultProps} filter={comparisonFilter} />);
    
    const memberSection = screen.getByTestId('member-selector');
    expect(memberSection).not.toHaveAttribute('aria-disabled');
  });
});