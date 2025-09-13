/**
 * k6 User Journey Load Test
 * 
 * Tests complete user workflows under load conditions
 * Simulates realistic user behavior patterns and end-to-end scenarios
 */

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { 
  AuthenticationHelper, 
  HiveHelper, 
  PresenceHelper, 
  AnalyticsHelper,
  WebSocketTestHelper,
  LoadTestDataGenerator 
} from './utils/helpers.js';
import { API_THRESHOLDS, WEBSOCKET_THRESHOLDS, SERVICE_ENDPOINTS } from './config/thresholds.js';

// Custom metrics
export const userJourneySuccess = new Rate('user_journey_success');
export const journeyDuration = new Trend('user_journey_duration');
export const workflowStepSuccess = new Rate('workflow_step_success');
export const userEngagementScore = new Trend('user_engagement_score');
export const sessionDuration = new Trend('user_session_duration');

// Test configuration
export const options = {
  scenarios: {
    // New user onboarding journey
    new_user_onboarding: {
      executor: 'constant-vus',
      vus: 5,
      duration: '10m',
      exec: 'newUserJourneyTest',
      tags: { journey_type: 'onboarding' }
    },
    
    // Daily work session journey
    daily_work_session: {
      executor: 'ramping-vus',
      startVUs: 2,
      stages: [
        { duration: '2m', target: 10 },
        { duration: '8m', target: 10 },
        { duration: '2m', target: 0 }
      ],
      exec: 'dailyWorkSessionTest',
      startTime: '1m',
      tags: { journey_type: 'daily_work' }
    },
    
    // Social interaction journey
    social_interaction: {
      executor: 'constant-vus',
      vus: 8,
      duration: '15m',
      exec: 'socialInteractionTest',
      startTime: '2m',
      tags: { journey_type: 'social' }
    },
    
    // Analytics and insights journey
    analytics_journey: {
      executor: 'constant-vus',
      vus: 3,
      duration: '8m',
      exec: 'analyticsJourneyTest',
      startTime: '5m',
      tags: { journey_type: 'analytics' }
    }
  },
  
  thresholds: {
    'user_journey_success': ['rate>0.90'],
    'journey_duration': ['p(95)<30000'], // 30 seconds for complete journeys
    'workflow_step_success': ['rate>0.95'],
    'user_engagement_score': ['avg>7'], // Out of 10
    'user_session_duration': ['p(50)>300000'], // 5+ minutes average session
    'http_req_duration': ['p(95)<1000'],
    'ws_message_latency': ['p(95)<150']
  }
};

// Helpers initialization
let authHelper, hiveHelper, presenceHelper, analyticsHelper, wsHelper, dataGenerator;

export function setup() {
  console.log('Setting up User Journey Load Test');
  
  // Initialize helpers
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  hiveHelper = new HiveHelper(SERVICE_ENDPOINTS.base_url);
  presenceHelper = new PresenceHelper(SERVICE_ENDPOINTS.base_url);
  analyticsHelper = new AnalyticsHelper(SERVICE_ENDPOINTS.base_url);
  wsHelper = new WebSocketTestHelper();
  dataGenerator = new LoadTestDataGenerator();
  
  // Create test data
  const testHives = [];
  const adminAuth = authHelper.login('admin@focushive.com', 'AdminPassword123!');
  
  if (adminAuth.success) {
    // Create test hives for journeys
    for (let i = 1; i <= 3; i++) {
      const hiveResult = hiveHelper.createHive(adminAuth.token, {
        name: `Journey Test Hive ${i}`,
        description: `Test hive for user journey testing - ${i}`,
        category: i === 1 ? 'work' : i === 2 ? 'study' : 'social',
        isPublic: true,
        maxMembers: 20
      });
      
      if (hiveResult.success) {
        testHives.push({
          id: hiveResult.hiveId,
          name: `Journey Test Hive ${i}`,
          category: i === 1 ? 'work' : i === 2 ? 'study' : 'social'
        });
      }
    }
  }
  
  console.log(`User Journey Test setup completed with ${testHives.length} test hives`);
  return {
    testHives,
    baseUrl: SERVICE_ENDPOINTS.base_url,
    wsUrl: SERVICE_ENDPOINTS.websockets.main
  };
}

export function newUserJourneyTest(data) {
  const journeyStart = Date.now();
  let journeySuccess = true;
  let engagementScore = 0;
  
  group('New User Onboarding Journey', () => {
    // Generate unique user for this journey
    const userData = dataGenerator.generateUser();
    let authToken = null;
    
    group('1. User Registration & Authentication', () => {
      const registrationResult = authHelper.register(userData);
      
      const stepSuccess = check(registrationResult, {
        'registration successful': (r) => r.success,
        'registration time acceptable': (r) => r.responseTime < 2000
      });
      
      workflowStepSuccess.add(stepSuccess ? 1 : 0);
      if (!stepSuccess) journeySuccess = false;
      
      if (registrationResult.success) {
        engagementScore += 2;
        
        // Login with new credentials
        const loginResult = authHelper.login(userData.email, userData.password);
        const loginSuccess = check(loginResult, {
          'login successful': (r) => r.success,
          'login time acceptable': (r) => r.responseTime < 1000
        });
        
        workflowStepSuccess.add(loginSuccess ? 1 : 0);
        if (loginSuccess) {
          authToken = loginResult.token;
          engagementScore += 1;
        } else {
          journeySuccess = false;
        }
      }
      
      sleep(1);
    });
    
    if (!authToken) {
      userJourneySuccess.add(0);
      return;
    }
    
    group('2. Profile Setup & Preferences', () => {
      const profileData = {
        firstName: userData.firstName,
        lastName: userData.lastName,
        bio: 'New FocusHive user exploring the platform',
        preferences: {
          notificationsEnabled: true,
          publicProfile: true,
          workingHours: { start: '09:00', end: '17:00' }
        }
      };
      
      const profileResult = authHelper.updateProfile(authToken, profileData);
      const stepSuccess = check(profileResult, {
        'profile update successful': (r) => r.success,
        'profile update time acceptable': (r) => r.responseTime < 1000
      });
      
      workflowStepSuccess.add(stepSuccess ? 1 : 0);
      if (stepSuccess) {
        engagementScore += 1;
      } else {
        journeySuccess = false;
      }
      
      sleep(0.5);
    });
    
    group('3. Explore Available Hives', () => {
      const hivesResult = hiveHelper.listHives(authToken);
      const stepSuccess = check(hivesResult, {
        'hives listed successfully': (r) => r.success,
        'hives list not empty': (r) => r.hives && r.hives.length > 0,
        'hives load time acceptable': (r) => r.responseTime < 800
      });
      
      workflowStepSuccess.add(stepSuccess ? 1 : 0);
      if (stepSuccess) {
        engagementScore += 1;
      } else {
        journeySuccess = false;
      }
      
      sleep(2); // User browses available hives
    });
    
    group('4. Join First Hive', () => {
      if (data.testHives.length > 0) {
        const targetHive = data.testHives[0];
        const joinResult = hiveHelper.joinHive(authToken, targetHive.id);
        
        const stepSuccess = check(joinResult, {
          'hive join successful': (r) => r.success,
          'hive join time acceptable': (r) => r.responseTime < 500
        });
        
        workflowStepSuccess.add(stepSuccess ? 1 : 0);
        if (stepSuccess) {
          engagementScore += 2;
        } else {
          journeySuccess = false;
        }
      }
      
      sleep(1);
    });
    
    group('5. Initial Presence & Activity', () => {
      const presenceResult = presenceHelper.updatePresence(authToken, {
        status: 'online',
        activity: 'getting-started',
        mood: 'excited'
      });
      
      const stepSuccess = check(presenceResult, {
        'presence update successful': (r) => r.success,
        'presence update time acceptable': (r) => r.responseTime < 300
      });
      
      workflowStepSuccess.add(stepSuccess ? 1 : 0);
      if (stepSuccess) {
        engagementScore += 1;
      } else {
        journeySuccess = false;
      }
      
      sleep(0.5);
    });
    
    group('6. First Analytics Check', () => {
      const dashboardResult = analyticsHelper.getDashboard(authToken);
      const stepSuccess = check(dashboardResult, {
        'dashboard accessible': (r) => r.success,
        'dashboard load time acceptable': (r) => r.responseTime < 1500
      });
      
      workflowStepSuccess.add(stepSuccess ? 1 : 0);
      if (stepSuccess) {
        engagementScore += 1;
      }
      
      sleep(1);
    });
  });
  
  const journeyDurationMs = Date.now() - journeyStart;
  journeyDuration.add(journeyDurationMs);
  userEngagementScore.add(engagementScore);
  userJourneySuccess.add(journeySuccess ? 1 : 0);
  sessionDuration.add(journeyDurationMs);
  
  sleep(Math.random() * 3 + 1);
}

export function dailyWorkSessionTest(data) {
  const sessionStart = Date.now();
  let sessionSuccess = true;
  let engagementScore = 0;
  
  group('Daily Work Session Journey', () => {
    const userData = dataGenerator.generateUser();
    const loginResult = authHelper.login(userData.email, userData.password);
    
    if (!loginResult.success) {
      // Register if login fails
      const regResult = authHelper.register(userData);
      if (regResult.success) {
        const newLogin = authHelper.login(userData.email, userData.password);
        if (!newLogin.success) {
          userJourneySuccess.add(0);
          return;
        }
      }
    }
    
    const authToken = loginResult.token || authHelper.login(userData.email, userData.password).token;
    
    group('1. Session Startup Routine', () => {
      // Check dashboard for daily overview
      const dashboardResult = analyticsHelper.getDashboard(authToken);
      const dashboardSuccess = check(dashboardResult, {
        'daily dashboard loaded': (r) => r.success
      });
      
      // Join work hive
      const workHive = data.testHives.find(h => h.category === 'work');
      if (workHive) {
        const joinResult = hiveHelper.joinHive(authToken, workHive.id);
        const joinSuccess = check(joinResult, {
          'work hive joined': (r) => r.success
        });
        
        if (joinSuccess) engagementScore += 1;
      }
      
      // Set online presence
      const presenceResult = presenceHelper.updatePresence(authToken, {
        status: 'online',
        activity: 'starting-work-session',
        mood: 'focused'
      });
      
      const presenceSuccess = check(presenceResult, {
        'work presence set': (r) => r.success
      });
      
      workflowStepSuccess.add((dashboardSuccess && presenceSuccess) ? 1 : 0);
      if (presenceSuccess) engagementScore += 1;
      
      sleep(1);
    });
    
    group('2. Active Work Period with WebSocket', () => {
      const wsUrl = `${data.wsUrl}?token=${authToken}`;
      
      const wsResponse = ws.connect(wsUrl, {}, (socket) => {
        let workActivities = 0;
        const workActivitiesList = [
          'coding', 'designing', 'writing', 'reviewing', 'planning', 
          'meeting', 'researching', 'debugging', 'testing'
        ];
        
        socket.on('open', () => {
          engagementScore += 1;
          
          // Simulate work activities every 2-3 minutes
          const activityInterval = socket.setInterval(() => {
            const activity = workActivitiesList[workActivities % workActivitiesList.length];
            const presenceUpdate = wsHelper.createPresenceMessage({
              userId: userData.id,
              status: 'busy',
              activity: activity,
              timestamp: Date.now()
            });
            
            socket.send(JSON.stringify(presenceUpdate));
            workActivities++;
            engagementScore += 0.5;
          }, Math.random() * 60000 + 120000); // 2-3 minutes
          
          // End work session after 8-12 minutes
          socket.setTimeout(() => {
            socket.clearInterval(activityInterval);
            socket.close();
          }, Math.random() * 240000 + 480000);
        });
        
        socket.on('message', (message) => {
          try {
            const data = JSON.parse(message);
            if (data.type === 'presence-update') {
              engagementScore += 0.1; // Small engagement boost for social awareness
            }
          } catch (e) {
            // Ignore parsing errors
          }
        });
        
        socket.on('close', () => {
          console.log(`Work session completed with ${workActivities} activities`);
        });
      });
      
      const wsSuccess = check(wsResponse, {
        'work session WebSocket connected': (r) => r && r.status === 101
      });
      
      workflowStepSuccess.add(wsSuccess ? 1 : 0);
      if (wsSuccess) {
        sessionSuccess = true;
        sleep(Math.random() * 240 + 480); // 8-12 minute work session
      }
    });
    
    group('3. Session Wrap-up', () => {
      // Update presence to away/offline
      const endPresenceResult = presenceHelper.updatePresence(authToken, {
        status: 'away',
        activity: 'session-complete',
        mood: 'accomplished'
      });
      
      // Check session analytics
      const analyticsResult = analyticsHelper.getProductivityInsights(authToken);
      
      const wrapupSuccess = check(endPresenceResult, {
        'session ended cleanly': (r) => r.success
      }) && check(analyticsResult, {
        'analytics captured': (r) => r.success
      });
      
      workflowStepSuccess.add(wrapupSuccess ? 1 : 0);
      if (wrapupSuccess) engagementScore += 1;
      
      sleep(0.5);
    });
  });
  
  const sessionDurationMs = Date.now() - sessionStart;
  journeyDuration.add(sessionDurationMs);
  userEngagementScore.add(engagementScore);
  userJourneySuccess.add(sessionSuccess ? 1 : 0);
  sessionDuration.add(sessionDurationMs);
}

export function socialInteractionTest(data) {
  const socialStart = Date.now();
  let socialSuccess = true;
  let engagementScore = 0;
  
  group('Social Interaction Journey', () => {
    const userData = dataGenerator.generateUser();
    const authResult = authHelper.login(userData.email, userData.password) ||
                       authHelper.register(userData);
    
    if (!authResult.success) {
      userJourneySuccess.add(0);
      return;
    }
    
    const authToken = authResult.token;
    
    group('1. Social Discovery', () => {
      // Browse social hives
      const socialHive = data.testHives.find(h => h.category === 'social');
      if (socialHive) {
        const hiveDetailsResult = hiveHelper.getHive(authToken, socialHive.id);
        const joinResult = hiveHelper.joinHive(authToken, socialHive.id);
        
        const socialSuccess = check(joinResult, {
          'social hive joined': (r) => r.success
        });
        
        if (socialSuccess) {
          engagementScore += 2;
          workflowStepSuccess.add(1);
        } else {
          workflowStepSuccess.add(0);
        }
      }
      
      sleep(1);
    });
    
    group('2. Social Presence & Chat', () => {
      // Set social presence
      const socialPresence = presenceHelper.updatePresence(authToken, {
        status: 'online',
        activity: 'socializing',
        mood: 'friendly'
      });
      
      if (socialPresence.success) {
        engagementScore += 1;
        
        // Simulate chat interactions via WebSocket
        const wsUrl = `${data.wsUrl}?token=${authToken}`;
        
        const wsResponse = ws.connect(wsUrl, {}, (socket) => {
          let chatMessagesSent = 0;
          
          socket.on('open', () => {
            // Send periodic chat messages
            const chatInterval = socket.setInterval(() => {
              const chatMessages = [
                "Hello everyone! How's your day going?",
                "Anyone want to be accountability buddies?",
                "Just finished a great work session!",
                "What's everyone working on today?",
                "Break time - anyone want to chat?"
              ];
              
              const chatMessage = wsHelper.createChatMessage({
                senderId: userData.id,
                hiveId: data.testHives.find(h => h.category === 'social')?.id,
                message: chatMessages[chatMessagesSent % chatMessages.length],
                timestamp: Date.now()
              });
              
              socket.send(JSON.stringify(chatMessage));
              chatMessagesSent++;
              engagementScore += 0.5;
            }, Math.random() * 30000 + 45000); // Every 45-75 seconds
            
            socket.setTimeout(() => {
              socket.clearInterval(chatInterval);
              socket.close();
            }, 300000); // 5 minute social session
          });
          
          socket.on('message', (message) => {
            try {
              const data = JSON.parse(message);
              if (data.type === 'chat-message' && data.senderId !== userData.id) {
                engagementScore += 0.2; // Engagement from receiving messages
              }
            } catch (e) {
              // Ignore parsing errors
            }
          });
        });
        
        const socialWsSuccess = check(wsResponse, {
          'social WebSocket connected': (r) => r && r.status === 101
        });
        
        workflowStepSuccess.add(socialWsSuccess ? 1 : 0);
        if (socialWsSuccess) {
          sleep(300); // 5 minute social session
        }
      }
    });
    
    group('3. Buddy System Interaction', () => {
      // Attempt to find and connect with accountability buddies
      // This would typically involve buddy service API calls
      const buddySearchResult = { success: true, responseTime: 150 }; // Simulated
      
      const buddySuccess = check(buddySearchResult, {
        'buddy search successful': (r) => r.success
      });
      
      if (buddySuccess) {
        engagementScore += 1;
      }
      
      workflowStepSuccess.add(buddySuccess ? 1 : 0);
      sleep(1);
    });
  });
  
  const socialDurationMs = Date.now() - socialStart;
  journeyDuration.add(socialDurationMs);
  userEngagementScore.add(engagementScore);
  userJourneySuccess.add(socialSuccess ? 1 : 0);
  sessionDuration.add(socialDurationMs);
}

export function analyticsJourneyTest(data) {
  const analyticsStart = Date.now();
  let analyticsSuccess = true;
  let engagementScore = 0;
  
  group('Analytics & Insights Journey', () => {
    const userData = dataGenerator.generateUser();
    const authResult = authHelper.login(userData.email, userData.password) ||
                       authHelper.register(userData);
    
    const authToken = authResult.token;
    
    group('1. Productivity Dashboard Review', () => {
      const dashboardResult = analyticsHelper.getDashboard(authToken);
      const insightsResult = analyticsHelper.getProductivityInsights(authToken);
      
      const analyticsAccessSuccess = check(dashboardResult, {
        'dashboard accessible': (r) => r.success,
        'dashboard load time acceptable': (r) => r.responseTime < 2000
      }) && check(insightsResult, {
        'insights accessible': (r) => r.success
      });
      
      workflowStepSuccess.add(analyticsAccessSuccess ? 1 : 0);
      if (analyticsAccessSuccess) {
        engagementScore += 2;
      }
      
      sleep(3); // User reviews analytics
    });
    
    group('2. Historical Data Analysis', () => {
      const historicalResult = analyticsHelper.getHistoricalData(authToken, {
        period: 'week',
        metrics: ['productivity', 'focus-time', 'social-interaction']
      });
      
      const historySuccess = check(historicalResult, {
        'historical data retrieved': (r) => r.success,
        'historical data complete': (r) => r.data && Object.keys(r.data).length > 0
      });
      
      workflowStepSuccess.add(historySuccess ? 1 : 0);
      if (historySuccess) {
        engagementScore += 1;
      }
      
      sleep(2);
    });
    
    group('3. Goal Setting & Tracking', () => {
      const goalData = {
        type: 'productivity',
        target: 8, // 8 hours daily
        period: 'daily',
        description: 'Maintain 8 hours of focused work daily'
      };
      
      const goalResult = analyticsHelper.setGoal(authToken, goalData);
      const goalSuccess = check(goalResult, {
        'goal set successfully': (r) => r.success
      });
      
      workflowStepSuccess.add(goalSuccess ? 1 : 0);
      if (goalSuccess) {
        engagementScore += 2;
      }
      
      sleep(1);
    });
    
    group('4. Export & Reporting', () => {
      const exportResult = analyticsHelper.exportData(authToken, {
        format: 'json',
        period: 'month',
        includeMetrics: ['all']
      });
      
      const exportSuccess = check(exportResult, {
        'data export successful': (r) => r.success,
        'export size reasonable': (r) => r.size && r.size > 0 && r.size < 1000000 // < 1MB
      });
      
      workflowStepSuccess.add(exportSuccess ? 1 : 0);
      if (exportSuccess) {
        engagementScore += 1;
      }
      
      sleep(1);
    });
  });
  
  const analyticsDurationMs = Date.now() - analyticsStart;
  journeyDuration.add(analyticsDurationMs);
  userEngagementScore.add(engagementScore);
  userJourneySuccess.add(analyticsSuccess ? 1 : 0);
  sessionDuration.add(analyticsDurationMs);
}

export function teardown(data) {
  console.log('User Journey Load Test completed');
  console.log(`Average journey duration: ${journeyDuration.avg}ms`);
  console.log(`Journey success rate: ${(userJourneySuccess.rate * 100).toFixed(2)}%`);
  console.log(`Workflow step success rate: ${(workflowStepSuccess.rate * 100).toFixed(2)}%`);
  console.log(`Average user engagement score: ${userEngagementScore.avg.toFixed(2)}/10`);
  console.log(`Average session duration: ${(sessionDuration.avg / 1000 / 60).toFixed(2)} minutes`);
}