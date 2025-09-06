export interface DatePickerConfig {
  locale?: string;
  format?: string;
  minDate?: Date;
  maxDate?: Date;
  disablePast?: boolean;
  disableFuture?: boolean;
}

export interface TimePickerConfig {
  locale?: string;
  format?: string;
  ampm?: boolean;
  minutesStep?: number;
}

export interface DateTimePickerConfig extends DatePickerConfig, TimePickerConfig {
  showTime?: boolean;
}

export type PickerVariant = 'inline' | 'dialog' | 'static';
export type PickerView = 'year' | 'month' | 'day' | 'hours' | 'minutes' | 'seconds';