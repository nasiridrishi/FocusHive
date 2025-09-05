import { describe, it, expect } from 'vitest';
import { transformPresenceDTO, transformHiveDTO, transformUserDTO } from '../transformers';
import type { PresenceStatus } from '../../../shared/types/presence';
import type { UserPresence as _UserPresence, Hive as _Hive, User as _User } from '../types';

// Mock types representing what backend returns (DTOs)
interface PresenceDTO {
  userId: string;
  status: string; // raw string from backend
  activity?: string;
  lastSeen: string; // ISO string
  currentHiveId?: string;
  inFocusSession: boolean;
}

interface HiveDTO {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  ownerUsername: string;
  maxMembers: number;
  currentMembers: number;
  isPublic: boolean;
  isActive: boolean;
  type: string;
  backgroundImage?: string;
  createdAt: string; // ISO string
  updatedAt: string; // ISO string
}

interface UserDTO {
  id: string;
  username: string;
  email: string;
  displayName?: string;
  avatar?: string;
  isOnline?: boolean;
  lastSeen?: string; // ISO string
}

describe('DTO Transformers', () => {
  describe('transformPresenceDTO', () => {
    const mockCurrentUserId = 'current-user-123';
    
    it('should transform basic PresenceDTO to Presence type with computed properties', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'online',
        activity: 'Working on project',
        lastSeen: '2024-01-01T12:00:00Z',
        currentHiveId: 'hive-456',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result).toEqual({
        userId: 'user-123',
        user: expect.any(Object), // Will be populated by transformer
        status: 'online' as PresenceStatus,
        currentActivity: 'Working on project',
        sessionStartTime: undefined,
        lastSeen: '2024-01-01T12:00:00Z',
        hiveId: 'hive-456',
        deviceInfo: undefined,
        // Computed properties
        isActive: true,
        isOnline: true,
        isFocusing: false,
        isCurrentUser: false,
        lastSeenFormatted: 'Jan 1, 2024 at 12:00 PM',
        statusDisplayText: 'Online',
        activityDisplayText: 'Working on project'
      });
    });
    
    it('should handle focusing status correctly', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'focusing',
        activity: 'Deep work session',
        lastSeen: '2024-01-01T12:00:00Z',
        currentHiveId: 'hive-456',
        inFocusSession: true
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.status).toBe('focusing');
      expect(result.isFocusing).toBe(true);
      expect(result.isActive).toBe(true);
      expect(result.statusDisplayText).toBe('Focusing');
    });
    
    it('should handle offline status correctly', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'offline',
        lastSeen: '2024-01-01T10:00:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.status).toBe('offline');
      expect(result.isOnline).toBe(false);
      expect(result.isActive).toBe(false);
      expect(result.statusDisplayText).toBe('Offline');
      expect(result.lastSeenFormatted).toBe('Jan 1, 2024 at 10:00 AM');
    });
    
    it('should identify current user correctly', () => {
      const dto: PresenceDTO = {
        userId: mockCurrentUserId,
        status: 'online',
        lastSeen: '2024-01-01T12:00:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.isCurrentUser).toBe(true);
    });
    
    it('should handle break status', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'break',
        activity: 'Taking a short break',
        lastSeen: '2024-01-01T12:00:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.status).toBe('break');
      expect(result.isActive).toBe(true);
      expect(result.statusDisplayText).toBe('On Break');
      expect(result.activityDisplayText).toBe('Taking a short break');
    });
    
    it('should handle away status', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'away',
        lastSeen: '2024-01-01T11:30:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.status).toBe('away');
      expect(result.isActive).toBe(false);
      expect(result.isOnline).toBe(false);
      expect(result.statusDisplayText).toBe('Away');
    });
    
    it('should handle null/undefined values gracefully', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'online',
        lastSeen: '2024-01-01T12:00:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.currentActivity).toBeUndefined();
      expect(result.hiveId).toBeUndefined();
      expect(result.activityDisplayText).toBe('');
    });
    
    it('should handle malformed dates gracefully', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'online',
        lastSeen: 'invalid-date',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.lastSeenFormatted).toBe('Unknown');
    });
    
    it('should handle unknown status gracefully', () => {
      const dto: PresenceDTO = {
        userId: 'user-123',
        status: 'unknown-status',
        lastSeen: '2024-01-01T12:00:00Z',
        inFocusSession: false
      };
      
      const result = transformPresenceDTO(dto, mockCurrentUserId);
      
      expect(result.status).toBe('offline'); // Fallback to offline
      expect(result.statusDisplayText).toBe('Offline');
    });
  });
  
  describe('transformHiveDTO', () => {
    const mockCurrentUserId = 'current-user-123';
    
    it('should transform basic HiveDTO to Hive type with computed properties', () => {
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'Study Group',
        description: 'A collaborative study space',
        ownerId: 'owner-456',
        ownerUsername: 'studymaster',
        maxMembers: 10,
        currentMembers: 5,
        isPublic: true,
        isActive: true,
        type: 'study',
        backgroundImage: 'https://example.com/bg.jpg',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-02T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result).toEqual({
        id: 'hive-123',
        name: 'Study Group',
        description: 'A collaborative study space',
        ownerId: 'owner-456',
        owner: {
          id: 'owner-456',
          username: 'studymaster',
          email: '',
          firstName: '',
          lastName: '',
          name: 'studymaster',
          isEmailVerified: false,
          createdAt: '',
          updatedAt: ''
        },
        maxMembers: 10,
        isPublic: true,
        tags: [],
        settings: expect.any(Object), // Default settings
        currentMembers: 5,
        memberCount: 5,
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-02T12:00:00Z',
        // Computed properties
        isOwner: false,
        isMember: false,
        isFull: false,
        hasSpots: true,
        spotsRemaining: 5,
        membershipStatus: 'not_member' as const,
        displayName: 'Study Group',
        shortDescription: 'A collaborative study space'
      });
    });
    
    it('should identify owner correctly', () => {
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'My Hive',
        description: 'My personal study space',
        ownerId: mockCurrentUserId,
        ownerUsername: 'me',
        maxMembers: 5,
        currentMembers: 1,
        isPublic: false,
        isActive: true,
        type: 'personal',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result.isOwner).toBe(true);
      expect(result.isMember).toBe(true); // Owner is also a member
      expect(result.membershipStatus).toBe('owner');
    });
    
    it('should handle full hive correctly', () => {
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'Full Hive',
        description: 'A full study space',
        ownerId: 'owner-456',
        ownerUsername: 'owner',
        maxMembers: 5,
        currentMembers: 5,
        isPublic: true,
        isActive: true,
        type: 'study',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result.isFull).toBe(true);
      expect(result.hasSpots).toBe(false);
      expect(result.spotsRemaining).toBe(0);
    });
    
    it('should truncate long descriptions', () => {
      const longDescription = 'This is a very long description that should be truncated because it exceeds the maximum length we want to display in preview cards and similar components throughout the application interface.';
      
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'Test Hive',
        description: longDescription,
        ownerId: 'owner-456',
        ownerUsername: 'owner',
        maxMembers: 10,
        currentMembers: 3,
        isPublic: true,
        isActive: true,
        type: 'study',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result.shortDescription).toHaveLength(120); // Should be truncated
      expect(result.shortDescription.endsWith('...')).toBe(true);
    });
    
    it('should handle inactive hive', () => {
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'Inactive Hive',
        description: 'An inactive study space',
        ownerId: 'owner-456',
        ownerUsername: 'owner',
        maxMembers: 10,
        currentMembers: 0,
        isPublic: true,
        isActive: false,
        type: 'study',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result.membershipStatus).toBe('not_member');
      expect(result.currentMembers).toBe(0);
    });
    
    it('should handle missing optional fields', () => {
      const dto: HiveDTO = {
        id: 'hive-123',
        name: 'Minimal Hive',
        description: '',
        ownerId: 'owner-456',
        ownerUsername: 'owner',
        maxMembers: 10,
        currentMembers: 1,
        isPublic: true,
        isActive: true,
        type: 'study',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      
      const result = transformHiveDTO(dto, mockCurrentUserId);
      
      expect(result.description).toBe('');
      expect(result.shortDescription).toBe('');
      expect(result.displayName).toBe('Minimal Hive');
    });
  });
  
  describe('transformUserDTO', () => {
    it('should transform basic UserDTO to User type with computed properties', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'johndoe',
        email: 'john@example.com',
        displayName: 'John Doe',
        avatar: 'https://example.com/avatar.jpg',
        isOnline: true,
        lastSeen: '2024-01-01T12:00:00Z'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result).toEqual({
        id: 'user-123',
        username: 'johndoe',
        email: 'john@example.com',
        displayName: 'John Doe',
        avatar: 'https://example.com/avatar.jpg',
        firstName: '',
        lastName: '',
        name: 'John Doe',
        isEmailVerified: false,
        createdAt: '',
        updatedAt: '',
        // Computed properties
        initials: 'JD',
        displayNameOrUsername: 'John Doe',
        isOnline: true,
        lastSeenFormatted: 'Jan 1, 2024 at 12:00 PM',
        profileUrl: '/profile/johndoe'
      });
    });
    
    it('should generate initials from display name', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'johndoe',
        email: 'john@example.com',
        displayName: 'John Michael Doe'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.initials).toBe('JMD');
    });
    
    it('should generate initials from username if no display name', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'john_doe_123',
        email: 'john@example.com'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.initials).toBe('JD');
      expect(result.displayNameOrUsername).toBe('john_doe_123');
    });
    
    it('should handle single word names', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'john',
        email: 'john@example.com',
        displayName: 'John'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.initials).toBe('J');
    });
    
    it('should handle empty or missing names gracefully', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: '',
        email: 'john@example.com'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.initials).toBe('??');
      expect(result.displayNameOrUsername).toBe('Unknown User');
    });
    
    it('should handle offline users', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'johndoe',
        email: 'john@example.com',
        isOnline: false,
        lastSeen: '2024-01-01T10:00:00Z'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.isOnline).toBe(false);
      expect(result.lastSeenFormatted).toBe('Jan 1, 2024 at 10:00 AM');
    });
    
    it('should handle malformed last seen dates', () => {
      const dto: UserDTO = {
        id: 'user-123',
        username: 'johndoe',
        email: 'john@example.com',
        lastSeen: 'invalid-date'
      };
      
      const result = transformUserDTO(dto);
      
      expect(result.lastSeenFormatted).toBe('Unknown');
    });
  });
  
  describe('Error Handling', () => {
    it('should throw error for null DTO in transformPresenceDTO', () => {
      expect(() => transformPresenceDTO(null as unknown as PresenceDTO, 'user-123')).toThrow('PresenceDTO cannot be null or undefined');
    });
    
    it('should throw error for undefined DTO in transformHiveDTO', () => {
      expect(() => transformHiveDTO(undefined as unknown as HiveDTO, 'user-123')).toThrow('HiveDTO cannot be null or undefined');
    });
    
    it('should throw error for null DTO in transformUserDTO', () => {
      expect(() => transformUserDTO(null as unknown as UserDTO)).toThrow('UserDTO cannot be null or undefined');
    });
    
    it('should throw error for missing required fields in PresenceDTO', () => {
      const invalidDto = {
        status: 'online',
        lastSeen: '2024-01-01T12:00:00Z'
        // Missing userId
      } as PresenceDTO;
      
      expect(() => transformPresenceDTO(invalidDto, 'user-123')).toThrow('PresenceDTO missing required field: userId');
    });
    
    it('should throw error for missing required fields in HiveDTO', () => {
      const invalidDto = {
        name: 'Test Hive',
        description: 'Test description'
        // Missing id and other required fields
      } as HiveDTO;
      
      expect(() => transformHiveDTO(invalidDto, 'user-123')).toThrow('HiveDTO missing required field: id');
    });
    
    it('should throw error for missing required fields in UserDTO', () => {
      const invalidDto = {
        username: 'johndoe',
        email: 'john@example.com'
        // Missing id
      } as UserDTO;
      
      expect(() => transformUserDTO(invalidDto)).toThrow('UserDTO missing required field: id');
    });
  });
});