import React, {useState} from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Badge,
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Checkbox,
  Chip,
  FormControl,
  FormControlLabel,
  FormGroup,
  InputLabel,
  MenuItem,
  Select,
  ToggleButton,
  ToggleButtonGroup,
  Typography
} from '@mui/material';
import {
  Assessment,
  DateRange,
  ExpandMore,
  FilterList,
  Group,
  Person,
  Refresh,
  Visibility
} from '@mui/icons-material';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {AnalyticsFilter, AnalyticsFiltersProps, Hive, Member} from '../types';
import {
  endOfDay,
  endOfMonth,
  endOfQuarter,
  endOfWeek,
  endOfYear,
  startOfDay,
  startOfMonth,
  startOfQuarter,
  startOfWeek,
  startOfYear
} from 'date-fns';

const getTimeRangePreset = (period: AnalyticsFilter['timeRange']['period']): {
  start: Date;
  end: Date;
  period: AnalyticsFilter['timeRange']['period'];
} => {
  const now = new Date();

  switch (period) {
    case 'day':
      return {
        start: startOfDay(now),
        end: endOfDay(now),
        period
      };
    case 'week':
      return {
        start: startOfWeek(now, {weekStartsOn: 1}),
        end: endOfWeek(now, {weekStartsOn: 1}),
        period
      };
    case 'month':
      return {
        start: startOfMonth(now),
        end: endOfMonth(now),
        period
      };
    case 'quarter':
      return {
        start: startOfQuarter(now),
        end: endOfQuarter(now),
        period
      };
    case 'year':
      return {
        start: startOfYear(now),
        end: endOfYear(now),
        period
      };
    default:
      return {
        start: startOfWeek(now, {weekStartsOn: 1}),
        end: endOfWeek(now, {weekStartsOn: 1}),
        period: 'week' as const
      };
  }
};

const getDefaultFilter = (): AnalyticsFilter => ({
  timeRange: getTimeRangePreset('week'),
  viewType: 'individual',
  selectedHives: [],
  selectedMembers: [],
  metrics: ['focus-time', 'sessions']
});

export const AnalyticsFilters: React.FC<AnalyticsFiltersProps> = ({
                                                                    filter,
                                                                    onFilterChange,
                                                                    availableHives = [],
                                                                    availableMembers = [],
                                                                    compact = false
                                                                  }) => {
  const [expandedSections, setExpandedSections] = useState<{
    timeRange: boolean;
    viewType: boolean;
    hives: boolean;
    members: boolean;
    metrics: boolean;
  }>({
    timeRange: !compact,
    viewType: !compact,
    hives: false,
    members: false,
    metrics: !compact
  });

  const [dateError, setDateError] = useState<string | null>(null);

  const handleTimeRangeChange = (period: AnalyticsFilter['timeRange']['period']): void => {
    if (period === 'custom') {
      onFilterChange({
        timeRange: {
          ...filter.timeRange,
          period
        }
      });
    } else {
      onFilterChange({
        timeRange: getTimeRangePreset(period)
      });
    }
  };

  const handleCustomDateChange = (field: 'start' | 'end', date: Date | null): void => {
    if (!date) return;

    const newTimeRange = {
      ...filter.timeRange,
      [field]: date
    };

    // Validate date range
    if (newTimeRange.start && newTimeRange.end && newTimeRange.start >= newTimeRange.end) {
      setDateError('End date must be after start date');
      return;
    }

    setDateError(null);
    onFilterChange({
      timeRange: newTimeRange
    });
  };

  const handleViewTypeChange = (viewType: AnalyticsFilter['viewType']): void => {
    onFilterChange({viewType});
  };

  const handleHiveSelection = (hiveId: string, checked: boolean): void => {
    const selectedHives = checked
        ? [...(filter.selectedHives || []), hiveId]
        : (filter.selectedHives || []).filter(id => id !== hiveId);

    onFilterChange({selectedHives});
  };

  const handleMemberSelection = (memberId: string, checked: boolean): void => {
    const selectedMembers = checked
        ? [...(filter.selectedMembers || []), memberId]
        : (filter.selectedMembers || []).filter(id => id !== memberId);

    onFilterChange({selectedMembers});
  };

  const handleMetricToggle = (metric: string, checked: boolean): void => {
    const metrics = checked
        ? [...filter.metrics, metric as AnalyticsFilter['metrics'][0]]
        : filter.metrics.filter(m => m !== metric);

    onFilterChange({metrics});
  };

  const handleReset = (): void => {
    onFilterChange(getDefaultFilter());
    setDateError(null);
  };

  const toggleSection = (section: keyof typeof expandedSections): void => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  // Count active filters
  const activeFilterCount =
      (filter.selectedHives?.length || 0) +
      (filter.selectedMembers?.length || 0) +
      filter.metrics.length;

  const FilterSection: React.FC<{
    title: string;
    icon: React.ReactNode;
    expanded: boolean;
    onToggle: () => void;
    children: React.ReactNode;
    badge?: number;
  }> = ({title, icon, expanded, onToggle, children, badge}) => (
      <Accordion expanded={expanded} onChange={onToggle}>
        <AccordionSummary expandIcon={<ExpandMore/>}>
          <Box display="flex" alignItems="center" gap={1}>
            {icon}
            <Typography variant="subtitle2">{title}</Typography>
            {badge !== undefined && badge > 0 && (
                <Chip size="small" label={badge} color="primary"/>
            )}
          </Box>
        </AccordionSummary>
        <AccordionDetails>
          {children}
        </AccordionDetails>
      </Accordion>
  );

  return (
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Card
            data-testid="analytics-filters"
            className={compact ? 'filters-compact' : ''}
        >
          <CardHeader
              title={
                <Box display="flex" alignItems="center" gap={1}>
                  <FilterList color="primary"/>
                  <Typography variant="h6">Filters</Typography>
                  {activeFilterCount > 0 && (
                      <Badge badgeContent={activeFilterCount} color="primary">
                        <Box/>
                      </Badge>
                  )}
                </Box>
              }
              action={
                <Button
                    startIcon={<Refresh/>}
                    size="small"
                    onClick={handleReset}
                >
                  Reset Filters
                </Button>
              }
          />
          <CardContent>
            {/* Time Range */}
            <FilterSection
                title="Time Range"
                icon={<DateRange/>}
                expanded={expandedSections.timeRange}
                onToggle={() => toggleSection('timeRange')}
            >
              <FormControl fullWidth size="small" sx={{mb: 2}}>
                <InputLabel>Period</InputLabel>
                <Select
                    value={filter.timeRange.period}
                    onChange={(e) => handleTimeRangeChange(e.target.value as AnalyticsFilter['timeRange']['period'])}
                    label="Period"
                >
                  <MenuItem value="day">Today</MenuItem>
                  <MenuItem value="week">This Week</MenuItem>
                  <MenuItem value="month">This Month</MenuItem>
                  <MenuItem value="quarter">This Quarter</MenuItem>
                  <MenuItem value="year">This Year</MenuItem>
                  <MenuItem value="custom">Custom Range</MenuItem>
                </Select>
              </FormControl>

              {filter.timeRange.period === 'custom' && (
                  <Box sx={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2}}>
                    <DatePicker
                        label="Start Date"
                        value={filter.timeRange.start}
                        onChange={(date) => handleCustomDateChange('start', date)}
                        slotProps={{
                          textField: {
                            size: 'small',
                            fullWidth: true,
                            'aria-label': 'Start Date'
                          }
                        }}
                    />
                    <DatePicker
                        label="End Date"
                        value={filter.timeRange.end}
                        onChange={(date) => handleCustomDateChange('end', date)}
                        slotProps={{
                          textField: {
                            size: 'small',
                            fullWidth: true,
                            'aria-label': 'End Date'
                          }
                        }}
                    />
                    {dateError && (
                        <Box sx={{gridColumn: '1 / -1'}}>
                          <Typography variant="body2" color="error">
                            {dateError}
                          </Typography>
                        </Box>
                    )}
                  </Box>
              )}
            </FilterSection>

            {/* View Type */}
            <FilterSection
                title="View Type"
                icon={<Visibility/>}
                expanded={expandedSections.viewType}
                onToggle={() => toggleSection('viewType')}
            >
              <ToggleButtonGroup
                  value={filter.viewType}
                  exclusive
                  onChange={(_, value) => value && handleViewTypeChange(value)}
                  size="small"
                  fullWidth
              >
                <ToggleButton value="individual">
                  <Person sx={{mr: 1}}/>
                  Individual
                </ToggleButton>
                <ToggleButton value="hive">
                  <Group sx={{mr: 1}}/>
                  Hive
                </ToggleButton>
                <ToggleButton value="comparison">
                  <Assessment sx={{mr: 1}}/>
                  Comparison
                </ToggleButton>
              </ToggleButtonGroup>
            </FilterSection>

            {/* Hives */}
            {availableHives.length > 0 && (
                <FilterSection
                    title="Select Hives"
                    icon={<Group/>}
                    expanded={expandedSections.hives}
                    onToggle={() => toggleSection('hives')}
                    badge={filter.selectedHives?.length}
                >
                  <FormGroup>
                    {availableHives.map((hive: Hive) => (
                        <FormControlLabel
                            key={hive.id}
                            control={
                              <Checkbox
                                  checked={filter.selectedHives?.includes(hive.id) || false}
                                  onChange={(e) => handleHiveSelection(hive.id, e.target.checked)}
                                  size="small"
                              />
                            }
                            label={hive.name}
                        />
                    ))}
                  </FormGroup>
                </FilterSection>
            )}

            {/* Members */}
            {availableMembers.length > 0 && (
                <FilterSection
                    title="Select Members"
                    icon={<Person/>}
                    expanded={expandedSections.members}
                    onToggle={() => toggleSection('members')}
                    badge={filter.selectedMembers?.length}
                >
                  <Box
                      data-testid="member-selector"
                      aria-disabled={filter.viewType === 'individual'}
                  >
                    <FormGroup>
                      {availableMembers.map((member: Member) => (
                          <FormControlLabel
                              key={member.id}
                              control={
                                <Checkbox
                                    checked={filter.selectedMembers?.includes(member.id) || false}
                                    onChange={(e) => handleMemberSelection(member.id, e.target.checked)}
                                    size="small"
                                    disabled={filter.viewType === 'individual'}
                                />
                              }
                              label={member.name}
                          />
                      ))}
                    </FormGroup>
                  </Box>
                </FilterSection>
            )}

            {/* Metrics */}
            <FilterSection
                title="Metrics"
                icon={<Assessment/>}
                expanded={expandedSections.metrics}
                onToggle={() => toggleSection('metrics')}
                badge={filter.metrics.length}
            >
              <FormGroup>
                <FormControlLabel
                    control={
                      <Checkbox
                          checked={filter.metrics.includes('focus-time')}
                          onChange={(e) => handleMetricToggle('focus-time', e.target.checked)}
                          size="small"
                      />
                    }
                    label="Focus Time"
                />
                <FormControlLabel
                    control={
                      <Checkbox
                          checked={filter.metrics.includes('sessions')}
                          onChange={(e) => handleMetricToggle('sessions', e.target.checked)}
                          size="small"
                      />
                    }
                    label="Sessions"
                />
                <FormControlLabel
                    control={
                      <Checkbox
                          checked={filter.metrics.includes('goals')}
                          onChange={(e) => handleMetricToggle('goals', e.target.checked)}
                          size="small"
                      />
                    }
                    label="Goals"
                />
                <FormControlLabel
                    control={
                      <Checkbox
                          checked={filter.metrics.includes('engagement')}
                          onChange={(e) => handleMetricToggle('engagement', e.target.checked)}
                          size="small"
                      />
                    }
                    label="Engagement"
                />
                <FormControlLabel
                    control={
                      <Checkbox
                          checked={filter.metrics.includes('productivity')}
                          onChange={(e) => handleMetricToggle('productivity', e.target.checked)}
                          size="small"
                      />
                    }
                    label="Productivity"
                />
              </FormGroup>
            </FilterSection>
          </CardContent>
        </Card>
      </LocalizationProvider>
  );
};