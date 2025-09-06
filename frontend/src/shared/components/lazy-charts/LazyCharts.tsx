import React, { lazy, Suspense } from 'react'
import { Box, Skeleton, Typography } from '@mui/material'
import { ComponentLoadingFallback as _ComponentLoadingFallback } from '@shared/components/loading'
import type { LazyChartWrapperProps } from './lazyChartUtils'
import { preloadChartLibrary, chartBundleInfo } from './lazyChartUtils'

// Chart loading fallback component
const ChartLoadingFallback = ({ 
  width = '100%', 
  height = 400, 
  title 
}: { 
  width?: string | number
  height?: number
  title?: string 
}) => (
  <Box sx={{ width, height, p: 2 }}>
    {title && (
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
    )}
    <Skeleton 
      variant="rectangular" 
      width="100%" 
      height={height - (title ? 60 : 20)}
      sx={{ borderRadius: 2 }}
    />
    <Box display="flex" justifyContent="center" mt={1}>
      <Typography variant="body2" color="text.secondary">
        Loading chart...
      </Typography>
    </Box>
  </Box>
)

// Lazy load MUI X Charts components
export const LazyLineChart = lazy(() => 
  import('@mui/x-charts/LineChart').then(module => ({ 
    default: module.LineChart 
  }))
)

export const LazyBarChart = lazy(() => 
  import('@mui/x-charts/BarChart').then(module => ({ 
    default: module.BarChart 
  }))
)

export const LazyPieChart = lazy(() => 
  import('@mui/x-charts/PieChart').then(module => ({ 
    default: module.PieChart 
  }))
)

export const LazyScatterChart = lazy(() => 
  import('@mui/x-charts/ScatterChart').then(module => ({ 
    default: module.ScatterChart 
  }))
)

export const LazyGauge = lazy(() => 
  import('@mui/x-charts/Gauge').then(module => ({ 
    default: module.Gauge 
  }))
)

export const LazySparkLineChart = lazy(() => 
  import('@mui/x-charts/SparkLineChart').then(module => ({ 
    default: module.SparkLineChart 
  }))
)

// Wrapper components with Suspense and loading states
// Interface moved to lazyChartUtils.ts to avoid Fast Refresh warnings

export const LineChartWrapper = ({ 
  title, 
  width, 
  height = 400, 
  showFallback = true,
  fallbackComponent,
  children,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <ChartLoadingFallback width={width} height={height} title={title} />
      )
    ) : null
  }>
    <LazyLineChart 
      {...(props as LazyChartWrapperProps)}
      series={[]} 
      width={typeof width === 'string' ? parseInt(width, 10) : width} 
      height={height}
    >
      {children}
    </LazyLineChart>
  </Suspense>
)

export const BarChartWrapper = ({ 
  title, 
  width, 
  height = 400, 
  showFallback = true,
  fallbackComponent,
  children,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <ChartLoadingFallback width={width} height={height} title={title} />
      )
    ) : null
  }>
    <LazyBarChart 
      {...(props as LazyChartWrapperProps)}
      series={[]} 
      width={typeof width === 'string' ? parseInt(width, 10) : width} 
      height={height}
    >
      {children}
    </LazyBarChart>
  </Suspense>
)

export const PieChartWrapper = ({ 
  title, 
  width, 
  height = 400, 
  showFallback = true,
  fallbackComponent,
  children,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <ChartLoadingFallback width={width} height={height} title={title} />
      )
    ) : null
  }>
    <LazyPieChart 
      {...(props as LazyChartWrapperProps)}
      series={[]} 
      width={typeof width === 'string' ? parseInt(width, 10) : width} 
      height={height}
    >
      {children}
    </LazyPieChart>
  </Suspense>
)

export const ScatterChartWrapper = ({ 
  title, 
  width, 
  height = 400, 
  showFallback = true,
  fallbackComponent,
  children,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <ChartLoadingFallback width={width} height={height} title={title} />
      )
    ) : null
  }>
    <LazyScatterChart 
      {...(props as LazyChartWrapperProps)}
      series={[]} 
      width={typeof width === 'string' ? parseInt(width, 10) : width} 
      height={height}
    >
      {children}
    </LazyScatterChart>
  </Suspense>
)

export const GaugeWrapper = ({ 
  title, 
  width, 
  height = 200, 
  showFallback = true,
  fallbackComponent,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <ChartLoadingFallback width={width} height={height} title={title} />
      )
    ) : null
  }>
    <LazyGauge width={typeof width === 'string' ? parseInt(width, 10) : width} height={height} {...props} />
  </Suspense>
)

export const SparkLineChartWrapper = ({ 
  title, 
  width, 
  height = 100, 
  showFallback = false, // Sparklines are small, less intrusive without loading
  fallbackComponent,
  ...props 
}: LazyChartWrapperProps & unknown) => (
  <Suspense fallback={
    showFallback ? (
      fallbackComponent ? (
        React.createElement(fallbackComponent)
      ) : (
        <Skeleton variant="rectangular" width={width || 200} height={height} />
      )
    ) : null
  }>
    <LazySparkLineChart 
      {...(props as LazyChartWrapperProps)}
      data={[]} 
      width={typeof width === 'string' ? parseInt(width, 10) : width} 
      height={height}
    />
  </Suspense>
)

// Chart preloader function
// Function moved to lazyChartUtils.ts to avoid Fast Refresh warnings

// Constant moved to lazyChartUtils.ts to avoid Fast Refresh warnings

export default {
  LineChart: LineChartWrapper,
  BarChart: BarChartWrapper,
  PieChart: PieChartWrapper,
  ScatterChart: ScatterChartWrapper,
  Gauge: GaugeWrapper,
  SparkLineChart: SparkLineChartWrapper,
  preload: preloadChartLibrary,
  bundleInfo: chartBundleInfo
}