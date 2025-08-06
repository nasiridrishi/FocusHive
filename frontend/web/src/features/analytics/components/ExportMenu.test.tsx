import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ExportMenu } from './ExportMenu';
import { ExportMenuProps, AnalyticsFilter } from '../types';

const mockFilter: AnalyticsFilter = {
  timeRange: {
    start: new Date('2024-01-01'),
    end: new Date('2024-01-31'),
    period: 'month'
  },
  viewType: 'individual',
  selectedHives: ['hive-1'],
  selectedMembers: ['user-1'],
  metrics: ['focus-time', 'sessions']
};

const defaultProps: ExportMenuProps = {
  onExport: vi.fn(),
  currentFilter: mockFilter
};

describe('ExportMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    render(<ExportMenu {...defaultProps} />);
    expect(screen.getByText('Export Data')).toBeInTheDocument();
  });

  it('shows export button with icon', () => {
    render(<ExportMenu {...defaultProps} />);
    
    const exportButton = screen.getByLabelText('export data');
    expect(exportButton).toBeInTheDocument();
    expect(screen.getByTestId('download-icon')).toBeInTheDocument();
  });

  it('opens export dialog when button is clicked', () => {
    render(<ExportMenu {...defaultProps} />);
    
    const exportButton = screen.getByLabelText('export data');
    fireEvent.click(exportButton);
    
    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();
    expect(screen.getByText('Choose your export preferences')).toBeInTheDocument();
  });

  it('shows format selection options', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByText('Export Format')).toBeInTheDocument();
    expect(screen.getByLabelText('CSV')).toBeInTheDocument();
    expect(screen.getByLabelText('JSON')).toBeInTheDocument();
    expect(screen.getByLabelText('PDF Report')).toBeInTheDocument();
    expect(screen.getByLabelText('PNG Image')).toBeInTheDocument();
  });

  it('selects CSV format by default', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const csvOption = screen.getByLabelText('CSV');
    expect(csvOption).toBeChecked();
  });

  it('allows format selection', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const jsonOption = screen.getByLabelText('JSON');
    fireEvent.click(jsonOption);
    
    expect(jsonOption).toBeChecked();
    expect(screen.getByLabelText('CSV')).not.toBeChecked();
  });

  it('shows date range selector', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByText('Date Range')).toBeInTheDocument();
    expect(screen.getByText('Use Current Filter')).toBeInTheDocument();
    expect(screen.getByText('Custom Range')).toBeInTheDocument();
    expect(screen.getByText('All Time')).toBeInTheDocument();
  });

  it('uses current filter date range by default', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const currentFilterOption = screen.getByLabelText('Use Current Filter');
    expect(currentFilterOption).toBeChecked();
    expect(screen.getByText('Jan 1 - Jan 31, 2024')).toBeInTheDocument();
  });

  it('shows custom date inputs when custom range is selected', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByLabelText('Custom Range'));
    
    expect(screen.getByLabelText('Export Start Date')).toBeInTheDocument();
    expect(screen.getByLabelText('Export End Date')).toBeInTheDocument();
  });

  it('shows content options', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByText('Content Options')).toBeInTheDocument();
    expect(screen.getByLabelText('Include Charts')).toBeInTheDocument();
    expect(screen.getByLabelText('Include Raw Data')).toBeInTheDocument();
  });

  it('shows section selection checkboxes', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByText('Sections to Include')).toBeInTheDocument();
    expect(screen.getByLabelText('Productivity Metrics')).toBeInTheDocument();
    expect(screen.getByLabelText('Goal Progress')).toBeInTheDocument();
    expect(screen.getByLabelText('Hive Activity')).toBeInTheDocument();
    expect(screen.getByLabelText('Member Engagement')).toBeInTheDocument();
  });

  it('selects all sections by default', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByLabelText('Productivity Metrics')).toBeChecked();
    expect(screen.getByLabelText('Goal Progress')).toBeChecked();
    expect(screen.getByLabelText('Hive Activity')).toBeChecked();
    expect(screen.getByLabelText('Member Engagement')).toBeChecked();
  });

  it('allows section deselection', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const hiveActivityOption = screen.getByLabelText('Hive Activity');
    fireEvent.click(hiveActivityOption);
    
    expect(hiveActivityOption).not.toBeChecked();
  });

  it('shows export preview information', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    expect(screen.getByText('Export Preview')).toBeInTheDocument();
    expect(screen.getByText(/Estimated file size/)).toBeInTheDocument();
    expect(screen.getByText(/Data points/)).toBeInTheDocument();
  });

  it('calls onExport with correct options when export button is clicked', async () => {
    const onExport = vi.fn().mockResolvedValue(undefined);
    render(<ExportMenu {...defaultProps} onExport={onExport} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    // Change some options
    fireEvent.click(screen.getByLabelText('JSON'));
    fireEvent.click(screen.getByLabelText('Include Charts'));
    fireEvent.click(screen.getByLabelText('Hive Activity')); // Deselect
    
    fireEvent.click(screen.getByText('Export'));
    
    await waitFor(() => {
      expect(onExport).toHaveBeenCalledWith({
        format: 'json',
        dateRange: mockFilter.timeRange,
        includeCharts: true,
        includeRawData: false,
        sections: ['productivity', 'goals', 'member-engagement']
      });
    });
  });

  it('shows loading state during export', async () => {
    render(<ExportMenu {...defaultProps} loading={true} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByText('Export'));
    
    expect(screen.getByText('Exporting...')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('disables export button when disabled prop is true', () => {
    render(<ExportMenu {...defaultProps} disabled={true} />);
    
    const exportButton = screen.getByLabelText('export data');
    expect(exportButton).toBeDisabled();
  });

  it('closes dialog when cancel is clicked', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Export Analytics Data')).not.toBeInTheDocument();
  });

  it('closes dialog when backdrop is clicked', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();
    
    const backdrop = screen.getByTestId('export-dialog-backdrop');
    fireEvent.click(backdrop);
    expect(screen.queryByText('Export Analytics Data')).not.toBeInTheDocument();
  });

  it('validates required sections selection', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    // Deselect all sections
    fireEvent.click(screen.getByLabelText('Productivity Metrics'));
    fireEvent.click(screen.getByLabelText('Goal Progress'));
    fireEvent.click(screen.getByLabelText('Hive Activity'));
    fireEvent.click(screen.getByLabelText('Member Engagement'));
    
    fireEvent.click(screen.getByText('Export'));
    
    expect(screen.getByText('Please select at least one section to export')).toBeInTheDocument();
  });

  it('validates custom date range', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByLabelText('Custom Range'));
    
    // Set invalid date range
    const startDateInput = screen.getByLabelText('Export Start Date');
    const endDateInput = screen.getByLabelText('Export End Date');
    
    fireEvent.change(startDateInput, { target: { value: '2024-02-01' } });
    fireEvent.change(endDateInput, { target: { value: '2024-01-01' } });
    
    fireEvent.click(screen.getByText('Export'));
    
    expect(screen.getByText('End date must be after start date')).toBeInTheDocument();
  });

  it('shows format-specific options for PDF', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByLabelText('PDF Report'));
    
    expect(screen.getByText('Include Cover Page')).toBeInTheDocument();
    expect(screen.getByText('Include Table of Contents')).toBeInTheDocument();
    expect(screen.getByText('Page Orientation')).toBeInTheDocument();
  });

  it('shows format-specific options for PNG', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByLabelText('PNG Image'));
    
    expect(screen.getByText('Image Quality')).toBeInTheDocument();
    expect(screen.getByText('Background Color')).toBeInTheDocument();
    expect(screen.getByText('Include Legends')).toBeInTheDocument();
  });

  it('updates file size estimate based on selections', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const initialSize = screen.getByText(/Estimated file size: ~\d+KB/);
    expect(initialSize).toBeInTheDocument();
    
    // Add charts - should increase size
    fireEvent.click(screen.getByLabelText('Include Charts'));
    
    const updatedSize = screen.getByText(/Estimated file size: ~\d+KB/);
    expect(updatedSize).toBeInTheDocument();
  });

  it('shows export history when available', () => {
    render(<ExportMenu {...defaultProps} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    
    const historyTab = screen.getByText('Recent Exports');
    fireEvent.click(historyTab);
    
    expect(screen.getByText('Export History')).toBeInTheDocument();
  });

  it('handles export error gracefully', async () => {
    const onExport = vi.fn().mockRejectedValue(new Error('Export failed'));
    render(<ExportMenu {...defaultProps} onExport={onExport} />);
    
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByText('Export'));
    
    await waitFor(() => {
      expect(screen.getByText('Export failed. Please try again.')).toBeInTheDocument();
    });
  });
});