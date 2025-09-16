import { vi } from 'vitest';

/**
 * Mock factory for HiveService
 * For use by Agent 4 in unit tests
 */
export const createMockHiveService = () => ({
  createHive: vi.fn(),
  getHives: vi.fn(),
  getHiveById: vi.fn(),
  updateHive: vi.fn(),
  deleteHive: vi.fn(),
  joinHive: vi.fn(),
  leaveHive: vi.fn(),
  getHiveMembers: vi.fn(),
  searchHives: vi.fn(),
  getHivesByIds: vi.fn(),
  subscribeToHiveUpdates: vi.fn(),
  subscribeToPresence: vi.fn(),
  updatePresenceStatus: vi.fn(),
  isCached: vi.fn(),
  handleNetworkError: vi.fn(),
  cleanup: vi.fn()
});