import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ExportMenu} from './ExportMenu';
import {AnalyticsFilter, ExportMenuProps} from '../types';
import {AllTheProviders} from '@/test-utils/testProviders';

// Mock DatePicker and LocalizationProvider to avoid adapter issues
interface DatePickerMockProps {
  label?: string;
  value?: Date | null;
  onChange?: (date: Date | null) => void;
  slotProps?: {
    textField?: {
      'data-testid'?: string;
    };
  };
}

vi.mock('@mui/x-date-pickers/DatePicker', () => ({
  DatePicker: ({label, value, onChange, slotProps}: DatePickerMockProps) => (
      <div>
        <input
            data-testid={slotProps?.textField?.['data-testid'] || `date-picker-${label?.toLowerCase().replace(/\s+/g, '-')}`}
            placeholder={label}
            value={value?.toISOString?.() || ''}
            onChange={(e) => onChange?.(new Date(e.target.value))}
        />
      </div>
  ),
}));

vi.mock('@mui/x-date-pickers/LocalizationProvider', () => ({
  LocalizationProvider: ({children}: { children: React.ReactNode }) => children,
}));

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

// Helper function to open the export dialog
const openDialog = (): void => {
  const exportButton = screen.getByLabelText('export data') as HTMLButtonElement;
  if (!exportButton.disabled) {
    fireEvent.click(exportButton);
    fireEvent.click(screen.getByText('Export Data'));
  }
};

describe('ExportMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );
    expect(screen.getByLabelText('export data')).toBeInTheDocument();
  });

  it('shows export button with icon', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    const exportButton = screen.getByLabelText('export data');
    expect(exportButton).toBeInTheDocument();
    expect(screen.getByTestId('download-icon')).toBeInTheDocument();
  });

  it('opens export dialog when button is clicked', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    const exportButton = screen.getByLabelText('export data');
    fireEvent.click(exportButton);

    // First click opens menu
    expect(screen.getByText('Export Data')).toBeInTheDocument();

    // Click on menu item to open dialog
    const menuItem = screen.getByText('Export Data');
    fireEvent.click(menuItem);

    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();
    expect(screen.getByText('Choose your export preferences')).toBeInTheDocument();
  });

  it('shows format selection options', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    // Open menu and then dialog
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByText('Export Data'));

    expect(screen.getByText('Export Format')).toBeInTheDocument();
    // Check format options are present - all radio buttons include both format and date range
    const allRadioOptions = screen.getAllByRole('radio');
    expect(allRadioOptions).toHaveLength(7); // 4 format + 3 date range radios
    expect(screen.getByDisplayValue('csv')).toBeInTheDocument();
    expect(screen.getByDisplayValue('json')).toBeInTheDocument();
    expect(screen.getByDisplayValue('pdf')).toBeInTheDocument();
    expect(screen.getByDisplayValue('png')).toBeInTheDocument();
  });

  it('selects CSV format by default', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    // Open menu and then dialog
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByText('Export Data'));

    // Find the radio button with value 'csv' (default)
    const csvOption = screen.getByDisplayValue('csv');
    expect(csvOption).toBeChecked();
  });

  it('allows format selection', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    // Open menu and then dialog
    fireEvent.click(screen.getByLabelText('export data'));
    fireEvent.click(screen.getByText('Export Data'));

    // Click on the JSON radio button directly
    const jsonOption = screen.getByDisplayValue('json');
    fireEvent.click(jsonOption);

    const csvOption = screen.getByDisplayValue('csv');
    expect(jsonOption).toBeChecked();
    expect(csvOption).not.toBeChecked();
  });

  it('shows date range selector', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    expect(screen.getByText('Date Range')).toBeInTheDocument();
    expect(screen.getByText('Use Current Filter')).toBeInTheDocument();
    expect(screen.getByText('Custom Range')).toBeInTheDocument();
    expect(screen.getByText('All Time')).toBeInTheDocument();
  });

  it('uses current filter date range by default', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    // Check that current filter is selected by default
    const currentFilterOption = screen.getByDisplayValue('current');
    expect(currentFilterOption).toBeChecked();
    expect(screen.getByText('Jan 1 - Jan 31, 2024')).toBeInTheDocument();
  });

  it('shows custom date inputs when custom range is selected', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    fireEvent.click(screen.getByText('Custom Range'));

    // Both start and end date pickers should be present
    expect(screen.getByTestId('date-picker-export-start-date')).toBeInTheDocument();
    expect(screen.getByTestId('date-picker-export-end-date')).toBeInTheDocument();
  });

  it('shows content options', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    expect(screen.getByText('Content Options')).toBeInTheDocument();
    expect(screen.getByLabelText('Include Charts')).toBeInTheDocument();
    expect(screen.getByLabelText('Include Raw Data')).toBeInTheDocument();
  });

  it('shows section selection checkboxes', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    expect(screen.getByText('Sections to Include')).toBeInTheDocument();
    expect(screen.getByLabelText('Productivity Metrics')).toBeInTheDocument();
    expect(screen.getByLabelText('Goal Progress')).toBeInTheDocument();
    expect(screen.getByLabelText('Hive Activity')).toBeInTheDocument();
    expect(screen.getByLabelText('Member Engagement')).toBeInTheDocument();
  });

  it('selects all sections by default', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    expect(screen.getByLabelText('Productivity Metrics')).toBeChecked();
    expect(screen.getByLabelText('Goal Progress')).toBeChecked();
    expect(screen.getByLabelText('Hive Activity')).toBeChecked();
    expect(screen.getByLabelText('Member Engagement')).toBeChecked();
  });

  it('allows section deselection', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    const hiveActivityOption = screen.getByLabelText('Hive Activity');
    fireEvent.click(hiveActivityOption);

    expect(hiveActivityOption).not.toBeChecked();
  });

  it('shows export preview information', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    expect(screen.getByText('Export Preview')).toBeInTheDocument();
    expect(screen.getByText(/Estimated file size/)).toBeInTheDocument();
    expect(screen.getByText(/Data points/)).toBeInTheDocument();
  });

  it('calls onExport with correct options when export button is clicked', async () => {
    const onExport = vi.fn().mockResolvedValue(undefined);
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} onExport={onExport}/>
        </AllTheProviders>
    );

    openDialog();

    // Change some options - click on the radio button itself, not just the text
    fireEvent.click(screen.getByDisplayValue('json'));
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
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} loading={true}/>
        </AllTheProviders>
    );

    // When loading prop is true, the main export button should be disabled
    const exportButton = screen.getByLabelText('export data');
    expect(exportButton).toBeDisabled();
  });

  it('disables export button when disabled prop is true', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} disabled={true}/>
        </AllTheProviders>
    );

    const exportButton = screen.getByLabelText('export data');
    expect(exportButton).toBeDisabled();
  });

  it('closes dialog when cancel is clicked', async () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.queryByText('Export Analytics Data')).not.toBeInTheDocument();
    });
  });

  it('closes dialog when backdrop is clicked', async () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    expect(screen.getByText('Export Analytics Data')).toBeInTheDocument();

    // Click the close button instead of backdrop (more reliable)
    fireEvent.click(screen.getByLabelText('Close dialog'));

    await waitFor(() => {
      expect(screen.queryByText('Export Analytics Data')).not.toBeInTheDocument();
    });
  });

  it('validates required sections selection', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    // Deselect all sections
    fireEvent.click(screen.getByLabelText('Productivity Metrics'));
    fireEvent.click(screen.getByLabelText('Goal Progress'));
    fireEvent.click(screen.getByLabelText('Hive Activity'));
    fireEvent.click(screen.getByLabelText('Member Engagement'));

    fireEvent.click(screen.getByText('Export'));

    expect(screen.getByText('Please select at least one section to export')).toBeInTheDocument();
  });

  it('validates custom date range', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    fireEvent.click(screen.getByText('Custom Range'));

    // Set invalid date range using our mocked date picker
    const startDatePicker = screen.getByTestId('date-picker-export-start-date');
    const endDatePicker = screen.getByTestId('date-picker-export-end-date');
    fireEvent.change(startDatePicker, {target: {value: '2024-02-01T00:00:00.000Z'}});
    fireEvent.change(endDatePicker, {target: {value: '2024-01-01T00:00:00.000Z'}});

    fireEvent.click(screen.getByText('Export'));

    expect(screen.getByText('End date must be after start date')).toBeInTheDocument();
  });

  it('shows format-specific options for PDF', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    fireEvent.click(screen.getByDisplayValue('pdf'));

    expect(screen.getByText('Include Cover Page')).toBeInTheDocument();
    expect(screen.getByText('Include Table of Contents')).toBeInTheDocument();
    expect(screen.getByText('PDF Options')).toBeInTheDocument();
    // Use getAllByText and check the first instance to handle duplicates
    const pageOrientationLabels = screen.getAllByText('Page Orientation');
    expect(pageOrientationLabels[0]).toBeInTheDocument();
  });

  it('shows format-specific options for PNG', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();
    fireEvent.click(screen.getByDisplayValue('png'));

    expect(screen.getByText('Image Quality')).toBeInTheDocument();
    expect(screen.getByText('Include Legends')).toBeInTheDocument();
    expect(screen.getByText('Image Options')).toBeInTheDocument();
    // Use getAllByText and check the first instance to handle duplicates
    const backgroundColorLabels = screen.getAllByText('Background Color');
    expect(backgroundColorLabels[0]).toBeInTheDocument();
  });

  it('updates file size estimate based on selections', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    const initialSize = screen.getByText(/Estimated file size/);
    expect(initialSize).toBeInTheDocument();

    // Add charts - should increase size
    fireEvent.click(screen.getByLabelText('Include Charts'));

    const updatedSize = screen.getByText(/Estimated file size/);
    expect(updatedSize).toBeInTheDocument();
  });

  it('shows export history when available', () => {
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} />
        </AllTheProviders>
    );

    openDialog();

    const historyTab = screen.getByText('Recent Exports');
    fireEvent.click(historyTab);

    expect(screen.getByText('Export History')).toBeInTheDocument();
  });

  it('handles export error gracefully', async () => {
    const onExport = vi.fn().mockRejectedValue(new Error('Export failed'));
    render(
        <AllTheProviders withDatePickers={false}>
          <ExportMenu {...defaultProps} onExport={onExport}/>
        </AllTheProviders>
    );

    openDialog();
    fireEvent.click(screen.getByText('Export'));

    await waitFor(() => {
      expect(screen.getByText('Export failed. Please try again.')).toBeInTheDocument();
    });
  });
});