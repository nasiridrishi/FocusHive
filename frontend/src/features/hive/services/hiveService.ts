import { webSocketService } from '@/services/websocket/WebSocketService';
import { authService } from '@/services/auth/authService';
import type {
  Hive,
  CreateHiveRequest,
  UpdateHiveRequest,
  HiveMember,
  HiveSearchParams
} from '@/contracts/hive';
import type { PaginatedResponse } from '@/contracts/common';
import type { IMessage } from '@stomp/stompjs';

/**
 * HiveService - Business logic layer for hive management
 *
 * This service provides:
 * - CRUD operations for hives with caching
 * - Real-time updates via WebSocket
 * - Presence management
 * - Batch operations
 * - Error handling with retry logic
 * - Optimistic updates
 */
export class HiveService {
  private cache: Map<number, { hive: Hive; timestamp: number }> = new Map();
  private cacheTimeout = 5 * 60 * 1000; // 5 minutes cache
  private subscriptions: Map<string, () => void> = new Map();
  private presenceTimers: Map<number, NodeJS.Timeout> = new Map();
  private retryAttempts = 3;
  private retryDelay = 1000;
  private apiUrl = 'http://localhost:8080/api/v1/hives';

  constructor() {}

  /**
   * Create a new hive
   */
  async createHive(request: CreateHiveRequest): Promise<Hive> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(this.apiUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to create hive: ${response.status} ${response.statusText}`);
      }

      const hive = await response.json();

      // Cache the new hive
      this.cacheHive(hive);

      // Subscribe to updates for the new hive
      this.subscribeToHiveUpdates(hive.id, () => {
        this.invalidateCache(hive.id);
      });

      return hive;
    } catch (error) {
      console.error('Error creating hive:', error);
      throw error;
    }
  }

  /**
   * Get all hives with pagination
   */
  async getHives(page = 0, size = 20): Promise<PaginatedResponse<Hive>> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });

      const response = await fetch(`${this.apiUrl}?${params}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch hives: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      // Ensure page property exists (for compatibility)
      if (!('page' in data) && 'number' in data) {
        data.page = data.number;
      }

      // Cache individual hives
      if (data.content) {
        data.content.forEach((hive: Hive) => this.cacheHive(hive));
      }

      return data;
    } catch (error) {
      console.error('Error fetching hives:', error);
      throw error;
    }
  }


  /**
   * Get a single hive by ID
   */
  async getHive(id: number): Promise<Hive> {
    // Check cache first
    const cached = this.getCachedHive(id);
    if (cached) {
      return cached;
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/${id}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch hive: ${response.status} ${response.statusText}`);
      }

      const hive = await response.json();
      this.cacheHive(hive);
      return hive;
    } catch (error) {
      console.error('Error fetching hive:', error);
      throw error;
    }
  }

  /**
   * Update a hive
   */
  async updateHive(id: number, request: UpdateHiveRequest): Promise<Hive> {
    // Optimistic update
    const cachedHive = this.getCachedHive(id);
    if (cachedHive) {
      const optimisticHive = { ...cachedHive, ...request };
      this.cacheHive(optimisticHive as Hive);
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/${id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to update hive: ${response.status} ${response.statusText}`);
      }

      const hive = await response.json();

      // Update cache with real data
      this.cacheHive(hive);

      // Notify real-time subscribers
      this.notifyHiveUpdate(id, hive);

      return hive;
    } catch (error) {
      // Revert optimistic update on error
      this.invalidateCache(id);
      console.error('Error updating hive:', error);
      throw error;
    }
  }

  /**
   * Delete a hive
   */
  async deleteHive(id: number): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to delete hive: ${response.status} ${response.statusText}`);
      }

      // Clean up cache and subscriptions
      this.invalidateCache(id);
      this.unsubscribeFromHive(id);

    } catch (error) {
      console.error('Error deleting hive:', error);
      throw error;
    }
  }

  /**
   * Join a hive
   */
  async joinHive(hiveId: number): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/${hiveId}/join`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to join hive: ${response.status} ${response.statusText}`);
      }

      // Invalidate cache to force refresh with updated member count
      this.invalidateCache(hiveId);

      // Subscribe to hive updates
      this.subscribeToHiveUpdates(hiveId, () => {
        this.invalidateCache(hiveId);
      });

      // Start presence heartbeat
      this.startPresenceHeartbeat(hiveId);

    } catch (error) {
      console.error('Error joining hive:', error);
      throw error;
    }
  }

  /**
   * Leave a hive
   */
  async leaveHive(hiveId: number): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/${hiveId}/leave`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to leave hive: ${response.status} ${response.statusText}`);
      }

      // Clean up
      this.invalidateCache(hiveId);
      this.unsubscribeFromHive(hiveId);
      this.stopPresenceHeartbeat(hiveId);

    } catch (error) {
      console.error('Error leaving hive:', error);
      throw error;
    }
  }

  /**
   * Get hive members
   */
  async getHiveMembers(hiveId: number, page = 0, size = 50): Promise<PaginatedResponse<HiveMember>> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString()
      });

      const response = await fetch(`${this.apiUrl}/${hiveId}/members?${params}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch hive members: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      // Ensure page property exists (for compatibility)
      if (!('page' in data) && 'number' in data) {
        data.page = data.number;
      }

      return data;
    } catch (error) {
      console.error('Error fetching hive members:', error);
      throw error;
    }
  }

  /**
   * Search hives
   */
  async searchHives(params: HiveSearchParams): Promise<PaginatedResponse<Hive>> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const queryParams = new URLSearchParams();
      if (params.query) queryParams.append('query', params.query);
      if (params.type) queryParams.append('type', params.type);
      if (params.visibility) queryParams.append('visibility', params.visibility);
      if (params.tags) params.tags.forEach(tag => queryParams.append('tags', tag));
      if (params.page !== undefined) queryParams.append('page', params.page.toString());
      if (params.size !== undefined) queryParams.append('size', params.size.toString());

      const response = await fetch(`${this.apiUrl}/search?${queryParams}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to search hives: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      // Ensure page property exists (for compatibility)
      if (!('page' in data) && 'number' in data) {
        data.page = data.number;
      }

      // Cache search results
      if (data.content) {
        data.content.forEach((hive: Hive) => this.cacheHive(hive));
      }

      return data;
    } catch (error) {
      console.error('Error searching hives:', error);
      throw error;
    }
  }

  /**
   * Get multiple hives by IDs (batch operation)
   */
  async getHivesByIds(ids: number[]): Promise<Hive[]> {
    const hives: Hive[] = [];
    const missingIds: number[] = [];

    // Check cache first
    for (const id of ids) {
      const cached = this.getCachedHive(id);
      if (cached) {
        hives.push(cached);
      } else {
        missingIds.push(id);
      }
    }

    // Fetch missing hives
    if (missingIds.length > 0) {
      const fetchPromises = missingIds.map(id => this.getHive(id));
      const fetchedHives = await Promise.all(fetchPromises);
      hives.push(...fetchedHives);
    }

    return hives;
  }

  /**
   * Subscribe to real-time hive updates
   */
  subscribeToHiveUpdates(hiveId: number, callback: (hive: Hive) => void): () => void {
    const topic = `/topic/hive/${hiveId}`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const hive = JSON.parse(message.body) as Hive;
        this.cacheHive(hive);
        callback(hive);
      } catch (error) {
        console.error('Failed to parse hive update:', error);
      }
    });

    const subscriptionKey = `hive-${hiveId}`;

    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);

    return unsubscribe;
  }

  /**
   * Subscribe to presence updates for a hive
   */
  subscribeToPresence(hiveId: number, callback: (presence: any) => void): () => void {
    const topic = `/topic/presence/${hiveId}`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const presence = JSON.parse(message.body);
        callback(presence);
      } catch (error) {
        console.error('Failed to parse presence update:', error);
      }
    });

    const subscriptionKey = `presence-${hiveId}`;

    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);

    return unsubscribe;
  }

  /**
   * Update user's presence status in a hive
   */
  async updatePresenceStatus(hiveId: number, status: 'active' | 'away' | 'offline'): Promise<void> {
    const destination = '/app/presence/status';
    const payload = {
      hiveId,
      status,
      timestamp: new Date().toISOString()
    };

    webSocketService.sendMessage(destination, payload);
  }

  /**
   * Check if a hive is cached
   */
  isCached(hiveId: number): boolean {
    const cached = this.cache.get(hiveId);
    if (!cached) return false;

    const now = Date.now();
    const isExpired = now - cached.timestamp > this.cacheTimeout;

    if (isExpired) {
      this.cache.delete(hiveId);
      return false;
    }

    return true;
  }

  /**
   * Handle network errors with retry logic
   */
  async handleNetworkError<T>(
    operation: () => Promise<T>,
    attempt = 1
  ): Promise<T> {
    try {
      return await operation();
    } catch (error: any) {
      if (attempt >= this.retryAttempts) {
        throw error;
      }

      // Check if it's a network error
      if (error.code === 'ECONNABORTED' ||
          error.message?.includes('Network') ||
          error.message?.includes('timeout')) {

        // Wait before retry with exponential backoff
        await new Promise(resolve =>
          setTimeout(resolve, this.retryDelay * Math.pow(2, attempt - 1))
        );

        return this.handleNetworkError(operation, attempt + 1);
      }

      throw error;
    }
  }

  // Private helper methods

  private cacheHive(hive: Hive): void {
    this.cache.set(hive.id, {
      hive,
      timestamp: Date.now()
    });
  }

  getCachedHive(id: number): Hive | null {
    if (!this.isCached(id)) {
      return null;
    }
    return this.cache.get(id)!.hive;
  }

  private invalidateCache(hiveId: number): void {
    this.cache.delete(hiveId);
  }

  private notifyHiveUpdate(hiveId: number, hive: Hive): void {
    const destination = '/app/hive/update';
    webSocketService.sendMessage(destination, {
      hiveId,
      hive,
      timestamp: new Date().toISOString()
    });
  }

  private unsubscribeFromHive(hiveId: number): void {
    // Unsubscribe from hive updates
    const hiveKey = `hive-${hiveId}`;
    const hiveUnsub = this.subscriptions.get(hiveKey);
    if (hiveUnsub) {
      hiveUnsub();
      this.subscriptions.delete(hiveKey);
    }

    // Unsubscribe from presence
    const presenceKey = `presence-${hiveId}`;
    const presenceUnsub = this.subscriptions.get(presenceKey);
    if (presenceUnsub) {
      presenceUnsub();
      this.subscriptions.delete(presenceKey);
    }
  }

  private startPresenceHeartbeat(hiveId: number): void {
    // Send presence update every 30 seconds
    const timer = setInterval(() => {
      this.updatePresenceStatus(hiveId, 'active');
    }, 30000);

    this.presenceTimers.set(hiveId, timer);
  }

  private stopPresenceHeartbeat(hiveId: number): void {
    const timer = this.presenceTimers.get(hiveId);
    if (timer) {
      clearInterval(timer);
      this.presenceTimers.delete(hiveId);
    }
  }

  private handleError(error: any, message: string): Error {
    console.error(message, error);

    if (error.response?.status === 401) {
      throw new Error('Unauthorized: Please login again');
    }

    if (error.response?.status === 403) {
      throw new Error('Forbidden: You do not have permission');
    }

    if (error.response?.status === 404) {
      throw new Error('Not found');
    }

    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }

    throw new Error(message);
  }

  /**
   * Clear cache - for testing purposes
   */
  clearCache(): void {
    this.cache.clear();
  }


  /**
   * Cleanup method to be called when service is destroyed
   */
  cleanup(): void {
    // Clear all subscriptions
    this.subscriptions.forEach(unsub => unsub());
    this.subscriptions.clear();

    // Clear all presence timers
    this.presenceTimers.forEach(timer => clearInterval(timer));
    this.presenceTimers.clear();

    // Clear cache
    this.cache.clear();
  }
}

// Export singleton instance for convenience
let hiveServiceInstance: HiveService | null = null;

export function getHiveService(): HiveService {
  if (!hiveServiceInstance) {
    hiveServiceInstance = new HiveService();
  }
  return hiveServiceInstance;
}

// Export singleton instance directly
export const hiveService = new HiveService();

