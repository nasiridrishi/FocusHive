import React, {useState} from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  FormGroup,
  IconButton,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  Menu,
  MenuItem,
  Radio,
  RadioGroup,
  Select,
  Slider,
  Tab,
  Tabs,
  Typography
} from '@mui/material';
import {Close, Code, Download, GetApp, Image, PictureAsPdf, TableChart} from '@mui/icons-material';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import {ExportMenuProps, ExportOptions} from '../types';
import {format} from 'date-fns';

const formatIcons = {
  csv: <TableChart/>,
  json: <Code/>,
  pdf: <PictureAsPdf/>,
  png: <Image/>
};

const formatDescriptions = {
  csv: 'Comma-separated values for spreadsheet analysis',
  json: 'Structured data format for developers',
  pdf: 'Professional report with charts and summaries',
  png: 'High-quality image of charts and visualizations'
};

const estimateFileSize = (options: Partial<ExportOptions>): string => {
  let baseSize = 50; // KB

  if (options.includeCharts) baseSize += 500;
  if (options.includeRawData) baseSize += 200;

  const sectionMultiplier = (options.sections?.length || 1) * 0.5;
  baseSize *= sectionMultiplier;

  if (options.format === 'pdf') baseSize *= 2;
  if (options.format === 'png') baseSize *= 3;

  return baseSize > 1000 ? `${(baseSize / 1000).toFixed(1)}MB` : `${Math.round(baseSize)}KB`;
};

const estimateDataPoints = (options: Partial<ExportOptions>): number => {
  const basePoints = 100;
  const sectionsMultiplier = (options.sections?.length || 1) * 50;
  return basePoints + sectionsMultiplier;
};

export const ExportMenu: React.FC<ExportMenuProps> = ({
                                                        onExport,
                                                        loading = false,
                                                        disabled = false,
                                                        currentFilter
                                                      }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentTab, setCurrentTab] = useState(0);
  const [exportOptions, setExportOptions] = useState<ExportOptions>({
    format: 'csv',
    dateRange: currentFilter.timeRange,
    includeCharts: false,
    includeRawData: false,
    sections: ['productivity', 'goals', 'hive-activity', 'member-engagement']
  });
  const [customDateRange, setCustomDateRange] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  const handleClick = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = (): void => {
    setAnchorEl(null);
  };

  const handleOpenDialog = (): void => {
    setDialogOpen(true);
    setAnchorEl(null);
  };

  const handleCloseDialog = (): void => {
    setDialogOpen(false);
    setError(null);
    setCurrentTab(0);
  };

  const handleOptionChange = <K extends keyof ExportOptions>(
      key: K,
      value: ExportOptions[K]
  ) => {
    setExportOptions(prev => ({
      ...prev,
      [key]: value
    }));
    setError(null);
  };

  const handleSectionToggle = (section: string, checked: boolean): void => {
    const newSections = checked
        ? [...exportOptions.sections, section as ExportOptions['sections'][0]]
        : exportOptions.sections.filter(s => s !== section);

    handleOptionChange('sections', newSections);
  };

  const handleDateRangeChange = (type: 'current' | 'custom' | 'all'): void => {
    setCustomDateRange(type === 'custom');

    if (type === 'current') {
      handleOptionChange('dateRange', currentFilter.timeRange);
    } else if (type === 'all') {
      handleOptionChange('dateRange', {
        start: new Date(2020, 0, 1), // Arbitrary start date
        end: new Date(),
        period: 'custom'
      });
    }
  };

  const validateExportOptions = (): string | null => {
    if (exportOptions.sections.length === 0) {
      return 'Please select at least one section to export';
    }

    if (customDateRange && exportOptions.dateRange.start >= exportOptions.dateRange.end) {
      return 'End date must be after start date';
    }

    return null;
  };

  const handleExport = async () => {
    const validationError = validateExportOptions();
    if (validationError) {
      setError(validationError);
      return;
    }

    setExporting(true);
    setError(null);

    try {
      await onExport(exportOptions);
      handleCloseDialog();
    } catch {
      // Export error logged to error service
      setError('Export failed. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const renderFormatOptions = () => (
      <FormControl component="fieldset" fullWidth>
        <Typography variant="subtitle2" gutterBottom>
          Export Format
        </Typography>
        <RadioGroup
            value={exportOptions.format}
            onChange={(e) => handleOptionChange('format', e.target.value as ExportOptions['format'])}
        >
          {Object.entries(formatIcons).map(([format, icon]) => (
              <FormControlLabel
                  key={format}
                  value={format}
                  control={<Radio/>}
                  label={
                    <Box display="flex" alignItems="center" gap={1}>
                      {icon}
                      <Box>
                        <Typography variant="body2" sx={{textTransform: 'uppercase'}}>
                          {format}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatDescriptions[format as keyof typeof formatDescriptions]}
                        </Typography>
                      </Box>
                    </Box>
                  }
              />
          ))}
        </RadioGroup>
      </FormControl>
  );

  const renderDateRangeOptions = () => (
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Date Range
        </Typography>
        <RadioGroup
            value={customDateRange ? 'custom' : 'current'}
            onChange={(e) => handleDateRangeChange(e.target.value as 'current' | 'custom' | 'all')}
        >
          <FormControlLabel
              value="current"
              control={<Radio/>}
              label={
                <Box>
                  <Typography variant="body2">Use Current Filter</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {format(currentFilter.timeRange.start, 'MMM d')} - {format(currentFilter.timeRange.end, 'MMM d, yyyy')}
                  </Typography>
                </Box>
              }
          />
          <FormControlLabel
              value="custom"
              control={<Radio/>}
              label="Custom Range"
          />
          <FormControlLabel
              value="all"
              control={<Radio/>}
              label="All Time"
          />
        </RadioGroup>

        {customDateRange && (
            <Box sx={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, mt: 1}}>
              <DatePicker
                  label="Export Start Date"
                  value={exportOptions.dateRange.start}
                  onChange={(date) => date && handleOptionChange('dateRange', {
                    ...exportOptions.dateRange,
                    start: date
                  })}
                  slotProps={{
                    textField: {
                      size: 'small',
                      fullWidth: true
                    }
                  }}
              />
              <DatePicker
                  label="Export End Date"
                  value={exportOptions.dateRange.end}
                  onChange={(date) => date && handleOptionChange('dateRange', {
                    ...exportOptions.dateRange,
                    end: date
                  })}
                  slotProps={{
                    textField: {
                      size: 'small',
                      fullWidth: true
                    }
                  }}
              />
            </Box>
        )}
      </Box>
  );

  const renderContentOptions = () => (
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Content Options
        </Typography>
        <FormGroup>
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.includeCharts}
                    onChange={(e) => handleOptionChange('includeCharts', e.target.checked)}
                />
              }
              label="Include Charts"
          />
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.includeRawData}
                    onChange={(e) => handleOptionChange('includeRawData', e.target.checked)}
                />
              }
              label="Include Raw Data"
          />
        </FormGroup>
      </Box>
  );

  const renderSectionOptions = () => (
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Sections to Include
        </Typography>
        <FormGroup>
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.sections.includes('productivity')}
                    onChange={(e) => handleSectionToggle('productivity', e.target.checked)}
                />
              }
              label="Productivity Metrics"
          />
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.sections.includes('goals')}
                    onChange={(e) => handleSectionToggle('goals', e.target.checked)}
                />
              }
              label="Goal Progress"
          />
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.sections.includes('hive-activity')}
                    onChange={(e) => handleSectionToggle('hive-activity', e.target.checked)}
                />
              }
              label="Hive Activity"
          />
          <FormControlLabel
              control={
                <Checkbox
                    checked={exportOptions.sections.includes('member-engagement')}
                    onChange={(e) => handleSectionToggle('member-engagement', e.target.checked)}
                />
              }
              label="Member Engagement"
          />
        </FormGroup>
      </Box>
  );

  const renderFormatSpecificOptions = (): React.ReactElement | null => {
    if (exportOptions.format === 'pdf') {
      return (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              PDF Options
            </Typography>
            <FormGroup>
              <FormControlLabel
                  control={<Checkbox defaultChecked/>}
                  label="Include Cover Page"
              />
              <FormControlLabel
                  control={<Checkbox defaultChecked/>}
                  label="Include Table of Contents"
              />
            </FormGroup>
            <FormControl fullWidth size="small" sx={{mt: 2}}>
              <InputLabel>Page Orientation</InputLabel>
              <Select defaultValue="portrait" label="Page Orientation">
                <MenuItem value="portrait">Portrait</MenuItem>
                <MenuItem value="landscape">Landscape</MenuItem>
              </Select>
            </FormControl>
          </Box>
      );
    }

    if (exportOptions.format === 'png') {
      return (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Image Options
            </Typography>
            <Typography variant="body2" gutterBottom>
              Image Quality
            </Typography>
            <Slider
                defaultValue={80}
                min={50}
                max={100}
                valueLabelDisplay="auto"
                valueLabelFormat={(value) => `${value}%`}
                sx={{mb: 2}}
            />
            <FormControl fullWidth size="small" sx={{mb: 2}}>
              <InputLabel>Background Color</InputLabel>
              <Select defaultValue="white" label="Background Color">
                <MenuItem value="white">White</MenuItem>
                <MenuItem value="transparent">Transparent</MenuItem>
                <MenuItem value="dark">Dark</MenuItem>
              </Select>
            </FormControl>
            <FormControlLabel
                control={<Checkbox defaultChecked/>}
                label="Include Legends"
            />
          </Box>
      );
    }

    return null;
  };

  const renderExportPreview = () => (
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Export Preview
        </Typography>
        <Box sx={{bgcolor: 'grey.50', p: 2, borderRadius: 1}}>
          <Box sx={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2}}>
            <Typography variant="body2" color="text.secondary">
              Estimated file size: ~{estimateFileSize(exportOptions)}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Data points: ~{estimateDataPoints(exportOptions)}
            </Typography>
            <Box sx={{gridColumn: '1 / -1'}}>
              <Typography variant="body2" color="text.secondary">
                Sections: {exportOptions.sections.length} selected
              </Typography>
              <Box display="flex" gap={0.5} flexWrap="wrap" sx={{mt: 1}}>
                {exportOptions.sections.map(section => (
                    <Chip
                        key={section}
                        label={section.replace('-', ' ')}
                        size="small"
                        variant="outlined"
                    />
                ))}
              </Box>
            </Box>
          </Box>
        </Box>
      </Box>
  );

  const renderExportHistory = () => (
      <Box>
        <Typography variant="h6" gutterBottom>
          Export History
        </Typography>
        <List>
          <ListItem>
            <ListItemText
                primary="Analytics Report - January 2024"
                secondary="PDF • Downloaded 2 days ago • 2.3MB"
            />
          </ListItem>
          <ListItem>
            <ListItemText
                primary="Productivity Data"
                secondary="CSV • Downloaded 1 week ago • 145KB"
            />
          </ListItem>
        </List>
      </Box>
  );

  return (
      <Box data-testid="export-menu">
        <IconButton
            aria-label="export data"
            onClick={handleClick}
            disabled={disabled || loading}
        >
          <Download data-testid="download-icon"/>
        </IconButton>

        <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
        >
          <MenuItem onClick={handleOpenDialog}>
            <GetApp sx={{mr: 2}}/>
            Export Data
          </MenuItem>
        </Menu>

        <Dialog
            open={dialogOpen}
            onClose={handleCloseDialog}
            maxWidth={false}
            fullWidth
            data-testid="export-dialog-backdrop"
        >
          <DialogTitle>
            <Box display="flex" alignItems="center" justifyContent="space-between">
              <Typography variant="h6">Export Analytics Data</Typography>
              <IconButton onClick={handleCloseDialog} aria-label="Close dialog">
                <Close/>
              </IconButton>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Choose your export preferences
            </Typography>
          </DialogTitle>

          <DialogContent>
            <Tabs
                value={currentTab}
                onChange={(_, newValue) => setCurrentTab(newValue)}
                sx={{borderBottom: 1, borderColor: 'divider', mb: 3}}
            >
              <Tab label="Export Options"/>
              <Tab label="Recent Exports"/>
            </Tabs>

            {currentTab === 0 && (
                <Box
                    sx={{display: 'grid', gridTemplateColumns: {xs: '1fr', md: '1fr 1fr'}, gap: 3}}>
                  <Box>
                    {renderFormatOptions()}
                    <Divider sx={{my: 2}}/>
                    {renderDateRangeOptions()}
                  </Box>
                  <Box>
                    {renderContentOptions()}
                    <Divider sx={{my: 2}}/>
                    {renderSectionOptions()}
                    {renderFormatSpecificOptions() && (
                        <>
                          <Divider sx={{my: 2}}/>
                          {renderFormatSpecificOptions()}
                        </>
                    )}
                  </Box>
                  <Box sx={{gridColumn: '1 / -1'}}>
                    <Divider sx={{my: 2}}/>
                    {renderExportPreview()}
                  </Box>
                </Box>
            )}

            {currentTab === 1 && renderExportHistory()}

            {error && (
                <Alert severity="error" sx={{mt: 2}}>
                  {error}
                </Alert>
            )}
          </DialogContent>

          <DialogActions>
            <Button onClick={handleCloseDialog}>
              Cancel
            </Button>
            <Button
                variant="contained"
                onClick={handleExport}
                disabled={exporting || loading}
                startIcon={exporting ? <CircularProgress size={16}/> : <Download/>}
            >
              {exporting ? 'Exporting...' : 'Export'}
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  );
};