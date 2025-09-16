// Common types and interfaces used across the application

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number; // current page number (0-based)
  number: number; // current page number (0-based) - kept for compatibility
  first: boolean;
  last: boolean;
  numberOfElements?: number;
  empty?: boolean;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp: string;
  path?: string;
  errors?: Record<string, string[]>;
}

export interface BaseEntity {
  id: number;
  createdAt: string;
  updatedAt: string;
}

export interface SortOptions {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface FilterOptions {
  [key: string]: string | number | boolean | string[] | number[];
}

export interface SearchRequest {
  query: string;
  page?: number;
  size?: number;
  sort?: SortOptions[];
  filters?: FilterOptions;
}

export interface BatchRequest<T> {
  items: T[];
  operation: 'CREATE' | 'UPDATE' | 'DELETE';
}

export interface BatchResponse<T> {
  successful: T[];
  failed: Array<{
    item: T;
    error: string;
  }>;
  totalProcessed: number;
  successCount: number;
  failureCount: number;
}