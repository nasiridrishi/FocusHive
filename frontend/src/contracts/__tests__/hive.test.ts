/**
 * Hive Contract Tests
 * Following TDD principles - Writing tests FIRST before implementation
 */

import { describe, it, expect, expectTypeOf } from 'vitest';
import type {
  Hive,
  HiveStatus,
  HiveType,
  HiveSettings,
  HiveMember,
  HiveMemberRole,
  HiveMemberStatus,
  HiveInvite,
  HiveInviteStatus,
  HiveActivity,
  HiveActivityType,
  HiveStatistics,
  CreateHiveRequest,
  UpdateHiveRequest,
  JoinHiveRequest,
  LeaveHiveRequest,
  InviteToHiveRequest,
  HiveResponse,
  HiveListResponse,
  HiveMemberListResponse,
  HiveGoal,
  HiveSession,
  HiveSessionStatus,
  HiveTag,
  HivePermission,
  HiveNotificationSettings
} from '../hive';

describe('Hive Contracts', () => {
  describe('Hive Interface', () => {
    it('should have required hive properties', () => {
      const hive: Hive = {
        id: 'hive-123',
        name: 'Study Group Alpha',
        description: 'A focused study group for computer science',
        type: 'study' as HiveType,
        status: 'active' as HiveStatus,
        createdBy: 'user-456',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        memberCount: 5,
        maxMembers: 10,
        isPrivate: false,
        requiresApproval: false
      };

      expect(hive.id).toBeDefined();
      expect(hive.name).toBeDefined();
      expect(hive.type).toBeDefined();
      expect(hive.status).toBeDefined();
      expectTypeOf(hive.id).toEqualTypeOf<string>();
      expectTypeOf(hive.type).toMatchTypeOf<HiveType>();
      expectTypeOf(hive.status).toMatchTypeOf<HiveStatus>();
      expectTypeOf(hive.memberCount).toEqualTypeOf<number>();
    });

    it('should support optional hive properties', () => {
      const hiveWithOptionals: Hive = {
        id: 'hive-123',
        name: 'Study Group Alpha',
        description: 'A focused study group',
        type: 'study' as HiveType,
        status: 'active' as HiveStatus,
        createdBy: 'user-456',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        memberCount: 5,
        maxMembers: 10,
        isPrivate: true,
        requiresApproval: true,
        imageUrl: 'https://example.com/hive.jpg',
        bannerUrl: 'https://example.com/banner.jpg',
        tags: ['computer-science', 'algorithms'],
        settings: {
          allowChat: true,
          allowVideo: false,
          allowScreenShare: true,
          autoMute: true,
          sessionDuration: 1500000,
          breakDuration: 300000,
          theme: 'dark'
        },
        goals: [],
        statistics: {
          totalSessions: 42,
          totalHours: 63,
          averageSessionDuration: 1500000,
          activeMembers: 5,
          completionRate: 85
        }
      };

      expect(hiveWithOptionals.imageUrl).toBeDefined();
      expect(hiveWithOptionals.tags).toBeDefined();
      expect(hiveWithOptionals.settings).toBeDefined();
      expectTypeOf(hiveWithOptionals.tags).toMatchTypeOf<string[] | undefined>();
      expectTypeOf(hiveWithOptionals.settings).toMatchTypeOf<HiveSettings | undefined>();
      expectTypeOf(hiveWithOptionals.statistics).toMatchTypeOf<HiveStatistics | undefined>();
    });
  });

  describe('Hive Types and Status', () => {
    it('should support all hive types', () => {
      const types: HiveType[] = ['study', 'work', 'creative', 'fitness', 'meditation', 'social', 'custom'];

      types.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<HiveType>();
      });
    });

    it('should support all hive statuses', () => {
      const statuses: HiveStatus[] = ['active', 'inactive', 'archived', 'deleted', 'suspended'];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<HiveStatus>();
      });
    });
  });

  describe('HiveMember Interface', () => {
    it('should have required member properties', () => {
      const member: HiveMember = {
        id: 'member-123',
        userId: 'user-456',
        hiveId: 'hive-789',
        role: 'member' as HiveMemberRole,
        status: 'active' as HiveMemberStatus,
        joinedAt: new Date().toISOString(),
        lastActiveAt: new Date().toISOString()
      };

      expect(member.id).toBeDefined();
      expect(member.userId).toBeDefined();
      expect(member.hiveId).toBeDefined();
      expect(member.role).toBeDefined();
      expectTypeOf(member.role).toMatchTypeOf<HiveMemberRole>();
      expectTypeOf(member.status).toMatchTypeOf<HiveMemberStatus>();
    });

    it('should support member statistics', () => {
      const memberWithStats: HiveMember = {
        id: 'member-123',
        userId: 'user-456',
        hiveId: 'hive-789',
        role: 'member' as HiveMemberRole,
        status: 'active' as HiveMemberStatus,
        joinedAt: new Date().toISOString(),
        lastActiveAt: new Date().toISOString(),
        statistics: {
          sessionsAttended: 15,
          totalMinutes: 375,
          streakDays: 7,
          contributionScore: 85
        },
        permissions: ['chat', 'share_screen']
      };

      expect(memberWithStats.statistics).toBeDefined();
      expect(memberWithStats.permissions).toBeDefined();
      expectTypeOf(memberWithStats.statistics).toMatchTypeOf<Record<string, any> | undefined>();
      expectTypeOf(memberWithStats.permissions).toMatchTypeOf<string[] | undefined>();
    });
  });

  describe('Member Roles and Status', () => {
    it('should support all member roles', () => {
      const roles: HiveMemberRole[] = ['owner', 'admin', 'moderator', 'member', 'guest'];

      roles.forEach(role => {
        expectTypeOf(role).toMatchTypeOf<HiveMemberRole>();
      });
    });

    it('should support all member statuses', () => {
      const statuses: HiveMemberStatus[] = ['active', 'inactive', 'banned', 'muted', 'left'];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<HiveMemberStatus>();
      });
    });
  });

  describe('HiveInvite Interface', () => {
    it('should have required invite properties', () => {
      const invite: HiveInvite = {
        id: 'invite-123',
        hiveId: 'hive-456',
        invitedBy: 'user-789',
        invitedEmail: 'invited@example.com',
        status: 'pending' as HiveInviteStatus,
        createdAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
      };

      expect(invite.id).toBeDefined();
      expect(invite.hiveId).toBeDefined();
      expect(invite.status).toBeDefined();
      expectTypeOf(invite.status).toMatchTypeOf<HiveInviteStatus>();
      expectTypeOf(invite.invitedUserId).toMatchTypeOf<string | undefined>();
    });

    it('should support all invite statuses', () => {
      const statuses: HiveInviteStatus[] = ['pending', 'accepted', 'rejected', 'expired', 'cancelled'];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<HiveInviteStatus>();
      });
    });
  });

  describe('HiveActivity Interface', () => {
    it('should have activity tracking properties', () => {
      const activity: HiveActivity = {
        id: 'activity-123',
        hiveId: 'hive-456',
        userId: 'user-789',
        type: 'session_started' as HiveActivityType,
        timestamp: new Date().toISOString(),
        data: {
          sessionId: 'session-123',
          duration: 1500000
        }
      };

      expect(activity.id).toBeDefined();
      expect(activity.type).toBeDefined();
      expectTypeOf(activity.type).toMatchTypeOf<HiveActivityType>();
      expectTypeOf(activity.data).toMatchTypeOf<Record<string, any> | undefined>();
    });

    it('should support all activity types', () => {
      const types: HiveActivityType[] = [
        'member_joined',
        'member_left',
        'session_started',
        'session_ended',
        'message_sent',
        'goal_created',
        'goal_completed',
        'settings_updated',
        'member_promoted',
        'member_demoted'
      ];

      types.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<HiveActivityType>();
      });
    });
  });

  describe('HiveSession Interface', () => {
    it('should have session properties', () => {
      const session: HiveSession = {
        id: 'session-123',
        hiveId: 'hive-456',
        status: 'active' as HiveSessionStatus,
        startedAt: new Date().toISOString(),
        scheduledDuration: 1500000,
        actualDuration: 0,
        participantCount: 5,
        participants: ['user-1', 'user-2', 'user-3', 'user-4', 'user-5']
      };

      expect(session.id).toBeDefined();
      expect(session.status).toBeDefined();
      expect(session.participantCount).toBeGreaterThan(0);
      expectTypeOf(session.status).toMatchTypeOf<HiveSessionStatus>();
      expectTypeOf(session.endedAt).toMatchTypeOf<string | undefined>();
    });

    it('should support all session statuses', () => {
      const statuses: HiveSessionStatus[] = ['scheduled', 'active', 'paused', 'completed', 'cancelled'];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<HiveSessionStatus>();
      });
    });
  });

  describe('HiveGoal Interface', () => {
    it('should have goal properties', () => {
      const goal: HiveGoal = {
        id: 'goal-123',
        hiveId: 'hive-456',
        title: 'Complete Chapter 5',
        description: 'Finish reading and exercises for Chapter 5',
        createdBy: 'user-789',
        createdAt: new Date().toISOString(),
        targetDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        completed: false,
        completedAt: undefined,
        completedBy: undefined
      };

      expect(goal.id).toBeDefined();
      expect(goal.title).toBeDefined();
      expect(goal.completed).toBe(false);
      expectTypeOf(goal.completed).toEqualTypeOf<boolean>();
      expectTypeOf(goal.priority).toMatchTypeOf<string | undefined>();
    });
  });

  describe('HiveSettings Interface', () => {
    it('should have settings properties', () => {
      const settings: HiveSettings = {
        allowChat: true,
        allowVideo: false,
        allowScreenShare: true,
        autoMute: false,
        sessionDuration: 1500000,
        breakDuration: 300000,
        maxSessionsPerDay: 4,
        theme: 'light',
        language: 'en',
        timezone: 'America/New_York',
        notificationSettings: {
          sessionStart: true,
          sessionEnd: true,
          memberJoined: true,
          memberLeft: false,
          goalCompleted: true,
          dailyReminder: true
        }
      };

      expect(settings.allowChat).toBeDefined();
      expect(settings.sessionDuration).toBeGreaterThan(0);
      expectTypeOf(settings.allowVideo).toEqualTypeOf<boolean>();
      expectTypeOf(settings.notificationSettings).toMatchTypeOf<HiveNotificationSettings | undefined>();
    });
  });

  describe('Create Hive Request', () => {
    it('should have required creation properties', () => {
      const createRequest: CreateHiveRequest = {
        name: 'New Study Group',
        description: 'A group for studying together',
        type: 'study' as HiveType,
        isPrivate: false,
        requiresApproval: false,
        maxMembers: 10
      };

      expect(createRequest.name).toBeDefined();
      expect(createRequest.type).toBeDefined();
      expectTypeOf(createRequest.name).toEqualTypeOf<string>();
      expectTypeOf(createRequest.type).toMatchTypeOf<HiveType>();
    });

    it('should support optional creation properties', () => {
      const createWithOptionals: CreateHiveRequest = {
        name: 'New Study Group',
        description: 'A group for studying together',
        type: 'study' as HiveType,
        isPrivate: true,
        requiresApproval: true,
        maxMembers: 10,
        imageUrl: 'https://example.com/image.jpg',
        tags: ['math', 'calculus'],
        settings: {
          allowChat: true,
          allowVideo: false,
          allowScreenShare: true,
          autoMute: false,
          sessionDuration: 1500000,
          breakDuration: 300000
        },
        initialMembers: ['user-123', 'user-456']
      };

      expect(createWithOptionals.tags).toBeDefined();
      expect(createWithOptionals.settings).toBeDefined();
      expectTypeOf(createWithOptionals.initialMembers).toMatchTypeOf<string[] | undefined>();
    });
  });

  describe('Join and Leave Requests', () => {
    it('should have join request properties', () => {
      const joinRequest: JoinHiveRequest = {
        hiveId: 'hive-123',
        message: 'I would like to join this study group'
      };

      expect(joinRequest.hiveId).toBeDefined();
      expectTypeOf(joinRequest.message).toMatchTypeOf<string | undefined>();
    });

    it('should have leave request properties', () => {
      const leaveRequest: LeaveHiveRequest = {
        hiveId: 'hive-123',
        reason: 'Schedule conflict'
      };

      expect(leaveRequest.hiveId).toBeDefined();
      expectTypeOf(leaveRequest.reason).toMatchTypeOf<string | undefined>();
    });
  });

  describe('HivePermission Interface', () => {
    it('should define permission structure', () => {
      const permission: HivePermission = {
        id: 'perm-123',
        name: 'manage_members',
        description: 'Can add, remove, and manage hive members',
        roles: ['owner', 'admin']
      };

      expect(permission.id).toBeDefined();
      expect(permission.name).toBeDefined();
      expect(permission.roles).toContain('owner');
      expectTypeOf(permission.roles).toMatchTypeOf<string[]>();
    });
  });

  describe('HiveStatistics Interface', () => {
    it('should have statistics properties', () => {
      const stats: HiveStatistics = {
        totalSessions: 100,
        totalHours: 150,
        averageSessionDuration: 1500000,
        activeMembers: 8,
        completionRate: 92,
        weeklyActive: 7,
        monthlyActive: 8,
        topContributors: ['user-1', 'user-2', 'user-3']
      };

      expect(stats.totalSessions).toBeGreaterThanOrEqual(0);
      expect(stats.completionRate).toBeLessThanOrEqual(100);
      expectTypeOf(stats.totalHours).toEqualTypeOf<number>();
      expectTypeOf(stats.topContributors).toMatchTypeOf<string[] | undefined>();
    });
  });
});