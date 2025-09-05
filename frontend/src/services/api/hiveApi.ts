import { AxiosInstance, AxiosError } from 'axios';
import { apiClient } from './httpInterceptors';
import { API_ENDPOINTS, buildEndpoint } from './index';

/**
 * Hive API Service
 * 
 * Provides comprehensive hive management functionality with:
 * - CRUD operations for hives
 * - Membership management
 * - Search capabilities
 * - Type safety and error handling
 */

export interface CreateHiveRequest {
  name: string;
  description?: string;
  slug?: string;
  isPrivate?: boolean;
  maxMembers?: number;
  tags?: string[];
  settings?: HiveSettings;
}

export interface UpdateHiveRequest {
  name?: string;
  description?: string;
  isPrivate?: boolean;
  maxMembers?: number;
  tags?: string[];
  settings?: HiveSettings;
}

export interface HiveSettings {
  allowChat?: boolean;
  allowMusic?: boolean;
  requireApproval?: boolean;
  workHours?: {
    start: string;
    end: string;
    timezone: string;
  };
}

export interface Hive {
  id: number;
  name: string;
  description?: string;
  slug: string;
  isPrivate: boolean;
  maxMembers: number;
  memberCount: number;
  tags: string[];
  settings: HiveSettings;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface HiveMember {
  id: number;
  userId: number;
  hiveId: number;
  role: 'OWNER' | 'ADMIN' | 'MODERATOR' | 'MEMBER';
  joinedAt: string;
  user: {
    id: number;
    username: string;
    displayName?: string;
    avatar?: string;
  };
}

export interface HiveSearchParams {
  query?: string;
  tags?: string[];
  isPrivate?: boolean;
  hasAvailableSlots?: boolean;
  page?: number;
  size?: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

class HiveApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = apiClient;
  }

  /**
   * Create a new hive
   */
  async createHive(hiveData: CreateHiveRequest): Promise<Hive> {
    try {
      const response = await this.api.post<Hive>(API_ENDPOINTS.HIVES.BASE, hiveData);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to create hive');
      throw error; // This will never execute but satisfies TypeScript
    }
  }

  /**
   * Get hive by ID
   */
  async getHiveById(id: number): Promise<Hive> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.BY_ID, { id });
      const response = await this.api.get<Hive>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive');
      throw error;
    }
  }

  /**
   * Get hive by slug
   */
  async getHiveBySlug(slug: string): Promise<Hive> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.BY_SLUG, { slug });
      const response = await this.api.get<Hive>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive');
      throw error;
    }
  }

  /**
   * Update hive
   */
  async updateHive(id: number, hiveData: UpdateHiveRequest): Promise<Hive> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.BY_ID, { id });
      const response = await this.api.put<Hive>(endpoint, hiveData);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to update hive');
      throw error;
    }
  }

  /**
   * Delete hive
   */
  async deleteHive(id: number): Promise<void> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.BY_ID, { id });
      await this.api.delete(endpoint);
    } catch (error) {
      this.handleError(error, 'Failed to delete hive');
      throw error;
    }
  }

  /**
   * Get all hives (with pagination)
   */
  async getHives(page = 0, size = 20): Promise<PaginatedResponse<Hive>> {
    try {
      const response = await this.api.get<PaginatedResponse<Hive>>(API_ENDPOINTS.HIVES.BASE, {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hives');
      throw error;
    }
  }

  /**
   * Search hives
   */
  async searchHives(params: HiveSearchParams): Promise<PaginatedResponse<Hive>> {
    try {
      const response = await this.api.get<PaginatedResponse<Hive>>(API_ENDPOINTS.HIVES.SEARCH, {
        params
      });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to search hives');
      throw error;
    }
  }

  /**
   * Join a hive
   */
  async joinHive(id: number): Promise<HiveMember> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.JOIN, { id });
      const response = await this.api.post<HiveMember>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to join hive');
      throw error;
    }
  }

  /**
   * Leave a hive
   */
  async leaveHive(id: number): Promise<void> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.LEAVE, { id });
      await this.api.post(endpoint);
    } catch (error) {
      this.handleError(error, 'Failed to leave hive');
      throw error;
    }
  }

  /**
   * Get hive members
   */
  async getHiveMembers(id: number): Promise<HiveMember[]> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.HIVES.MEMBERS, { id });
      const response = await this.api.get<HiveMember[]>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive members');
      throw error;
    }
  }

  /**
   * Error handler for consistent error formatting
   */
  private handleError(error: unknown, defaultMessage: string): never {
    if (error instanceof AxiosError) {
      const message = error.response?.data?.message || 
                     error.response?.data?.error || 
                     defaultMessage;
      throw new Error(message);
    }
    throw new Error('Network error occurred');
  }
}

// Export singleton instance
export const hiveApiService = new HiveApiService();

export default hiveApiService;