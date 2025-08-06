// Common types used across the application

export interface ApiResponse<T = unknown> {
  data: T
  message?: string
  status: 'success' | 'error'
  timestamp: string
}

export interface ApiError {
  code: string
  message: string
  details?: Record<string, unknown>
  timestamp: string
}

export interface PaginatedResponse<T> {
  data: T[]
  pagination: {
    page: number
    limit: number
    total: number
    totalPages: number
    hasNext: boolean
    hasPrev: boolean
  }
}

export interface QueryOptions {
  page?: number
  limit?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  filter?: Record<string, unknown>
  search?: string
}

export interface LoadingState {
  isLoading: boolean
  error: string | null
}

export interface FormErrors {
  [key: string]: string[]
}

export interface ValidationRule {
  required?: boolean
  minLength?: number
  maxLength?: number
  pattern?: RegExp
  custom?: (value: unknown) => string | null
}

export interface FormField<T = unknown> {
  name: string
  label: string
  type: 'text' | 'email' | 'password' | 'number' | 'textarea' | 'select' | 'checkbox' | 'radio' | 'file'
  value: T
  validation?: ValidationRule
  options?: Array<{ label: string; value: unknown }>
  placeholder?: string
  disabled?: boolean
  required?: boolean
}

export interface ModalProps {
  open: boolean
  onClose: () => void
  title?: string
  children: React.ReactNode
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  fullWidth?: boolean
  disableBackdropClick?: boolean
}

export interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  severity?: 'info' | 'warning' | 'error' | 'success'
}

export interface NotificationData {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  autoHideDuration?: number
  persistent?: boolean
  actions?: Array<{
    label: string
    onClick: () => void
    color?: 'primary' | 'secondary'
  }>
}

export interface FileUpload {
  file: File
  progress: number
  status: 'pending' | 'uploading' | 'completed' | 'error'
  url?: string
  error?: string
}

export interface SearchResult<T> {
  items: T[]
  query: string
  totalResults: number
  suggestions?: string[]
}

export interface Theme {
  mode: 'light' | 'dark'
  primaryColor: string
  secondaryColor: string
  customizations?: Record<string, unknown>
}

export interface UserPreferences {
  theme: Theme
  language: string
  timezone: string
  notifications: {
    email: boolean
    push: boolean
    inApp: boolean
  }
  privacy: {
    showOnlineStatus: boolean
    allowDirectMessages: boolean
    shareActivityData: boolean
  }
}

export type SortDirection = 'asc' | 'desc'
export type ComponentSize = 'small' | 'medium' | 'large'
export type ComponentVariant = 'outlined' | 'contained' | 'text'
export type ColorScheme = 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'