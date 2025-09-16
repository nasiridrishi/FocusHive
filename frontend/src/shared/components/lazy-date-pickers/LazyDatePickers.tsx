import React, {lazy, Suspense} from 'react'
import {Box, Skeleton, TextField} from '@mui/material'
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider'
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns'

// Lazy load heavy date picker components
export const LazyDatePicker = lazy(() =>
    import('@mui/x-date-pickers/DatePicker').then(module => ({
      default: module.DatePicker
    }))
)

export const LazyDateTimePicker = lazy(() =>
    import('@mui/x-date-pickers/DateTimePicker').then(module => ({
      default: module.DateTimePicker
    }))
)

export const LazyTimePicker = lazy(() =>
    import('@mui/x-date-pickers/TimePicker').then(module => ({
      default: module.TimePicker
    }))
)

// Note: DateRangePicker requires @mui/x-date-pickers-pro
// For demo purposes, we'll use regular DatePicker as fallback
export const LazyDateRangePicker = lazy(() =>
    import('@mui/x-date-pickers/DatePicker').then(module => ({
      default: module.DatePicker
    }))
)

export const LazyMobileDatePicker = lazy(() =>
    import('@mui/x-date-pickers/MobileDatePicker').then(module => ({
      default: module.MobileDatePicker
    }))
)

export const LazyDesktopDatePicker = lazy(() =>
    import('@mui/x-date-pickers/DesktopDatePicker').then(module => ({
      default: module.DesktopDatePicker
    }))
)

export const LazyStaticDatePicker = lazy(() =>
    import('@mui/x-date-pickers/StaticDatePicker').then(module => ({
      default: module.StaticDatePicker
    }))
)

// Loading fallback component
const DatePickerLoadingFallback = ({
                                     label,
                                     fullWidth = false,
                                     variant = 'outlined'
                                   }: {
  label?: string
  fullWidth?: boolean
  variant?: 'outlined' | 'filled' | 'standard'
}) => (
    <Box sx={{width: fullWidth ? '100%' : 'auto'}}>
      <Skeleton variant="rectangular" height={56}>
        <TextField
            label={label || 'Loading date picker...'}
            fullWidth={fullWidth}
            variant={variant}
            disabled
        />
      </Skeleton>
    </Box>
)

// Wrapper components with Suspense and localization
export interface LazyDatePickerWrapperProps {
  label?: string
  fullWidth?: boolean
  variant?: 'outlined' | 'filled' | 'standard'
  showFallback?: boolean
  fallbackComponent?: React.ComponentType
}

export const DatePickerWrapper = ({
                                    label,
                                    fullWidth = false,
                                    variant = 'outlined',
                                    showFallback = true,
                                    fallbackComponent,
                                    ...props
                                  }: LazyDatePickerWrapperProps & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <DatePickerLoadingFallback
                    label={label}
                    fullWidth={fullWidth}
                    variant={variant}
                />
            )
        ) : null
      }>
        <LazyDatePicker
            label={label}
            slotProps={{
              textField: {
                fullWidth,
                variant
              }
            }}
            {...props}
        />
      </Suspense>
    </LocalizationProvider>
)

export const DateTimePickerWrapper = ({
                                        label,
                                        fullWidth = false,
                                        variant = 'outlined',
                                        showFallback = true,
                                        fallbackComponent,
                                        ...props
                                      }: LazyDatePickerWrapperProps & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <DatePickerLoadingFallback
                    label={label}
                    fullWidth={fullWidth}
                    variant={variant}
                />
            )
        ) : null
      }>
        <LazyDateTimePicker
            label={label}
            slotProps={{
              textField: {
                fullWidth,
                variant
              }
            }}
            {...props}
        />
      </Suspense>
    </LocalizationProvider>
)

export const TimePickerWrapper = ({
                                    label,
                                    fullWidth = false,
                                    variant = 'outlined',
                                    showFallback = true,
                                    fallbackComponent,
                                    ...props
                                  }: LazyDatePickerWrapperProps & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <DatePickerLoadingFallback
                    label={label}
                    fullWidth={fullWidth}
                    variant={variant}
                />
            )
        ) : null
      }>
        <LazyTimePicker
            label={label}
            slotProps={{
              textField: {
                fullWidth,
                variant
              }
            }}
            {...props}
        />
      </Suspense>
    </LocalizationProvider>
)

export const MobileDatePickerWrapper = ({
                                          label,
                                          fullWidth = false,
                                          variant = 'outlined',
                                          showFallback = true,
                                          fallbackComponent,
                                          ...props
                                        }: LazyDatePickerWrapperProps & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <DatePickerLoadingFallback
                    label={label}
                    fullWidth={fullWidth}
                    variant={variant}
                />
            )
        ) : null
      }>
        <LazyMobileDatePicker
            label={label}
            slotProps={{
              textField: {
                fullWidth,
                variant
              }
            }}
            {...props}
        />
      </Suspense>
    </LocalizationProvider>
)

export const DesktopDatePickerWrapper = ({
                                           label,
                                           fullWidth = false,
                                           variant = 'outlined',
                                           showFallback = true,
                                           fallbackComponent,
                                           ...props
                                         }: LazyDatePickerWrapperProps & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <DatePickerLoadingFallback
                    label={label}
                    fullWidth={fullWidth}
                    variant={variant}
                />
            )
        ) : null
      }>
        <LazyDesktopDatePicker
            label={label}
            slotProps={{
              textField: {
                fullWidth,
                variant
              }
            }}
            {...props}
        />
      </Suspense>
    </LocalizationProvider>
)

export const StaticDatePickerWrapper = ({
                                          showFallback = true,
                                          fallbackComponent,
                                          ...props
                                        }: Omit<LazyDatePickerWrapperProps, 'label' | 'fullWidth' | 'variant'> & unknown) => (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Suspense fallback={
        showFallback ? (
            fallbackComponent ? (
                React.createElement(fallbackComponent)
            ) : (
                <Skeleton
                    variant="rectangular"
                    width={320}
                    height={320}
                    sx={{borderRadius: 2}}
                />
            )
        ) : null
      }>
        <LazyStaticDatePicker {...props} />
      </Suspense>
    </LocalizationProvider>
)

export default {
  DatePicker: DatePickerWrapper,
  DateTimePicker: DateTimePickerWrapper,
  TimePicker: TimePickerWrapper,
  MobileDatePicker: MobileDatePickerWrapper,
  DesktopDatePicker: DesktopDatePickerWrapper,
  StaticDatePicker: StaticDatePickerWrapper
}