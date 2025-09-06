export interface ChartData {
  labels: string[];
  datasets: Array<{
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }>;
}

export interface ChartOptions {
  responsive?: boolean;
  maintainAspectRatio?: boolean;
  plugins?: {
    legend?: {
      display?: boolean;
      position?: 'top' | 'bottom' | 'left' | 'right';
    };
    title?: {
      display?: boolean;
      text?: string;
    };
  };
}

export type ChartType = 'line' | 'bar' | 'pie' | 'doughnut' | 'radar' | 'polar';

export interface LazyChartProps {
  type: ChartType;
  data: ChartData;
  options?: ChartOptions;
  height?: number | string;
  width?: number | string;
}