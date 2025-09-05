import React, { lazy, Suspense } from 'react'
import { Box, Skeleton, Typography } from '@mui/material'
import { ComponentLoadingFallback as _ComponentLoadingFallback } from '@shared/components/loading'

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
export interface LazyChartWrapperProps {
  title?: string
  width?: string | number
  height?: number
  showFallback?: boolean
  fallbackComponent?: React.ComponentType
}

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
    <LazyLineChart width={width} height={height} {...props}>
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
    <LazyBarChart width={width} height={height} {...props}>
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
    <LazyPieChart width={width} height={height} {...props}>
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
    <LazyScatterChart width={width} height={height} {...props}>
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
    <LazyGauge width={width} height={height} {...props} />
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
    <LazySparkLineChart width={width} height={height} {...props} />
  </Suspense>
)

// Chart preloader function
export const preloadChartLibrary = () => {
  // Preload the most commonly used charts after a delay
  setTimeout(() => {
    import('@mui/x-charts/LineChart')
    import('@mui/x-charts/BarChart')
  }, 3000)
}

// Chart bundle size information (for documentation)
export const chartBundleInfo = {
  '@mui/x-charts': {
    estimatedSize: '~150KB',
    components: [
      'LineChart', 'BarChart', 'PieChart', 'ScatterChart', 
      'Gauge', 'SparkLineChart'
    ],
    note: 'Large bundle - lazy loading recommended'
  }
}

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