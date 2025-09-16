/**
 * API Endpoint Verification Utility
 *
 * This utility helps verify that frontend API services are correctly aligned
 * with backend Spring Boot controller endpoints.
 */

import {API_ENDPOINTS} from './index';

export interface EndpointVerification {
  service: string;
  endpoint: string;
  expectedBackendPath: string;
  isAligned: boolean;
  notes?: string;
}

/**
 * Verify that frontend API endpoints match backend controller patterns
 */
export function verifyEndpointAlignment(): EndpointVerification[] {
  const verifications: EndpointVerification[] = [
    // Auth endpoints - DemoController and SimpleAuthController
    {
      service: 'Auth',
      endpoint: API_ENDPOINTS.AUTH.LOGIN,
      expectedBackendPath: '/api/demo/login',
      isAligned: API_ENDPOINTS.AUTH.LOGIN as string === '/api/demo/login',
      notes: 'DemoController @PostMapping("/login")'
    },
    {
      service: 'Auth',
      endpoint: API_ENDPOINTS.AUTH.REGISTER,
      expectedBackendPath: '/api/v1/auth/register',
      isAligned: API_ENDPOINTS.AUTH.REGISTER === '/api/v1/auth/register',
      notes: 'SimpleAuthController @PostMapping("/register")'
    },

    // Hive endpoints - HiveController
    {
      service: 'Hive',
      endpoint: API_ENDPOINTS.HIVES.BASE,
      expectedBackendPath: '/api/v1/hives',
      isAligned: API_ENDPOINTS.HIVES.BASE === '/api/v1/hives',
      notes: 'HiveController @RequestMapping("/api/v1/hives")'
    },
    {
      service: 'Hive',
      endpoint: API_ENDPOINTS.HIVES.JOIN,
      expectedBackendPath: '/api/v1/hives/:id/join',
      isAligned: API_ENDPOINTS.HIVES.JOIN === '/api/v1/hives/:id/join',
      notes: 'HiveController @PostMapping("/{id}/join")'
    },

    // Presence endpoints - PresenceRestController
    {
      service: 'Presence',
      endpoint: API_ENDPOINTS.PRESENCE.BASE,
      expectedBackendPath: '/api/v1/presence',
      isAligned: API_ENDPOINTS.PRESENCE.BASE === '/api/v1/presence',
      notes: 'PresenceRestController @RequestMapping("/api/v1/presence")'
    },
    {
      service: 'Presence',
      endpoint: API_ENDPOINTS.PRESENCE.ME,
      expectedBackendPath: '/api/v1/presence/me',
      isAligned: API_ENDPOINTS.PRESENCE.ME === '/api/v1/presence/me',
      notes: 'PresenceRestController @GetMapping("/me")'
    },

    // Timer endpoints - TimerController
    {
      service: 'Timer',
      endpoint: API_ENDPOINTS.TIMER.BASE,
      expectedBackendPath: '/api/v1/timer',
      isAligned: API_ENDPOINTS.TIMER.BASE === '/api/v1/timer',
      notes: 'TimerController @RequestMapping("/api/v1/timer")'
    },
    {
      service: 'Timer',
      endpoint: API_ENDPOINTS.TIMER.START_SESSION,
      expectedBackendPath: '/api/v1/timer/sessions/start',
      isAligned: API_ENDPOINTS.TIMER.START_SESSION === '/api/v1/timer/sessions/start',
      notes: 'TimerController @PostMapping("/sessions/start")'
    },

    // Analytics endpoints - AnalyticsController
    {
      service: 'Analytics',
      endpoint: API_ENDPOINTS.ANALYTICS.BASE,
      expectedBackendPath: '/api/v1/analytics',
      isAligned: API_ENDPOINTS.ANALYTICS.BASE === '/api/v1/analytics',
      notes: 'AnalyticsController @RequestMapping("/api/v1/analytics")'
    },

    // Chat endpoints - ChatRestController
    {
      service: 'Chat',
      endpoint: API_ENDPOINTS.CHAT.BASE,
      expectedBackendPath: '/api/v1/chat',
      isAligned: API_ENDPOINTS.CHAT.BASE === '/api/v1/chat',
      notes: 'ChatRestController @RequestMapping("/api/v1/chat")'
    },

    // Buddy endpoints - BuddyController
    {
      service: 'Buddy',
      endpoint: API_ENDPOINTS.BUDDY.BASE,
      expectedBackendPath: '/api/buddy',
      isAligned: API_ENDPOINTS.BUDDY.BASE as string === '/api/buddy',
      notes: 'BuddyController @RequestMapping("/api/buddy")'
    },

    // Notifications - NotificationController
    {
      service: 'Notifications',
      endpoint: API_ENDPOINTS.NOTIFICATIONS.BASE,
      expectedBackendPath: '/api/notifications',
      isAligned: API_ENDPOINTS.NOTIFICATIONS.BASE as string === '/api/notifications',
      notes: 'NotificationController @RequestMapping("/api/notifications")'
    }
  ];

  return verifications;
}

/**
 * Generate alignment report
 */
export function generateAlignmentReport(): {
  aligned: EndpointVerification[];
  misaligned: EndpointVerification[];
  totalCount: number;
  alignmentPercentage: number;
} {
  const verifications = verifyEndpointAlignment();
  const aligned = verifications.filter(v => v.isAligned);
  const misaligned = verifications.filter(v => !v.isAligned);

  return {
    aligned,
    misaligned,
    totalCount: verifications.length,
    alignmentPercentage: Math.round((aligned.length / verifications.length) * 100)
  };
}

/**
 * Log verification report to console
 */
export function logVerificationReport(): void {
  const report = generateAlignmentReport();

  // console.group('🔍 API Endpoint Alignment Verification');
  // console.log(`📊 Alignment Status: ${report.alignmentPercentage}% (${report.aligned.length}/${report.totalCount})`);

  if (report.misaligned.length > 0) {
    // console.group('❌ Misaligned Endpoints');
    report.misaligned.forEach(item => {
      // console.log(`${item.service}: ${item.endpoint} !== ${item.expectedBackendPath}`);
      if (item.notes) {
        // console.log(`  Note: ${item.notes}`);
      }
    });
    // console.groupEnd();
  }

  if (report.aligned.length > 0) {
    // console.group('✅ Aligned Endpoints');
    report.aligned.forEach(_item => {
      // console.log(`${_item.service}: ${_item.endpoint}`);
    });
    // console.groupEnd();
  }

  // console.groupEnd();
}

/**
 * Test API service configurations
 */
export function testApiServiceConfigurations(): {
  services: string[];
  baseUrls: Record<string, string>;
  issues: string[];
} {
  const issues: string[] = [];
  const baseUrls: Record<string, string> = {};

  // Check environment variables
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  const musicApiBaseUrl = import.meta.env.VITE_MUSIC_API_BASE_URL;

  if (!apiBaseUrl) {
    issues.push('VITE_API_BASE_URL environment variable not set, using default: http://localhost:8080');
  }

  baseUrls.main = apiBaseUrl || 'http://localhost:8080';
  baseUrls.music = musicApiBaseUrl || baseUrls.main;

  const services = [
    'authApiService',
    'hiveApiService',
    'presenceApiService',
    'timerApiService',
    'analyticsApiService',
    'buddyApiService',
    'forumApiService',
    'musicApiService'
  ];

  return {
    services,
    baseUrls,
    issues
  };
}

// Export default functions for convenience
export default {
  verifyEndpointAlignment,
  generateAlignmentReport,
  logVerificationReport,
  testApiServiceConfigurations
};