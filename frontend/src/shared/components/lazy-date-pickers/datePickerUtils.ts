// Date picker utility functions and constants
// Separated from LazyDatePickers.tsx to avoid fast-refresh warnings

// Preloader function
export const preloadDatePickers = (): void => {
  // Preload commonly used date pickers after a delay
  setTimeout(() => {
    import('@mui/x-date-pickers/DatePicker')
    import('@mui/x-date-pickers/DateTimePicker')
  }, 4000)
}

// Bundle information
export const datePickerBundleInfo = {
  '@mui/x-date-pickers': {
    estimatedSize: '~80KB',
    components: [
      'DatePicker', 'DateTimePicker', 'TimePicker',
      'MobileDatePicker', 'DesktopDatePicker', 'StaticDatePicker'
    ],
    note: 'Medium-size bundle - lazy loading recommended for optional features'
  },
  '@mui/x-date-pickers-pro': {
    estimatedSize: '~120KB',
    components: ['DateRangePicker', 'MultiInputDateRangeField'],
    note: 'Pro features - lazy loading highly recommended'
  }
}