export interface LazyChartWrapperProps {
  title?: string
  width?: string | number
  height?: number
  showFallback?: boolean
  fallbackComponent?: React.ComponentType
  children?: React.ReactNode
  series?: unknown[]
  data?: unknown[]

  [key: string]: unknown
}

export const preloadChartLibrary = (): void => {
  // Preload the most commonly used charts after a delay
  setTimeout(() => {
    import('@mui/x-charts/LineChart')
    import('@mui/x-charts/BarChart')
  }, 3000)
}

export const chartBundleInfo = {
  '@mui/x-charts': {
    estimatedSize: '~150KB',
    components: [
      'LineChart', 'BarChart', 'PieChart', 'ScatterChart',
      'Gauge', 'SparkLineChart'
    ],
    note: 'Large bundle - lazy loading recommended'
  }
};