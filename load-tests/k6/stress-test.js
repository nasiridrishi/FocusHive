/**
 * k6 Stress Load Test
 * 
 * Tests system behavior under extreme load conditions
 * Identifies breaking points and system limits
 */

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { 
  AuthenticationHelper, 
  HiveHelper, 
  PresenceHelper, 
  AnalyticsHelper,
  WebSocketTestHelper 
} from './utils/helpers.js';
import { LOAD_TEST_SCENARIOS, SERVICE_ENDPOINTS } from './config/thresholds.js';

// Custom metrics for stress testing
export const systemBreakingPoint = new Gauge('system_breaking_point_vus');
export const degradationRate = new Rate('performance_degradation');
export const resourceExhaustionRate = new Rate('resource_exhaustion');
export const criticalErrorRate = new Rate('critical_errors');
export const systemRecoveryTime = new Trend('system_recovery_time');
export const extremeLoadResponseTime = new Trend('extreme_load_response_time');
export const memoryPressureIndicator = new Gauge('memory_pressure_indicator');
export const connectionFailureRate = new Rate('connection_failures');

// Test configuration with progressive stress levels
export const options = {
  scenarios: {
    // Progressive API stress test
    progressive_api_stress: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '2m', target: 20 },   // Normal load
        { duration: '3m', target: 50 },   // Increased load
        { duration: '3m', target: 100 },  // High load
        { duration: '3m', target: 200 },  // Extreme load
        { duration: '3m', target: 300 },  // Breaking point attempt
        { duration: '2m', target: 0 }     // Recovery
      ],
      exec: 'progressiveApiStressTest',
      tags: { stress_type: 'progressive_api' }
    },
    
    // Database stress test
    database_stress: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '1m', target: 30 },   // Warm up database
        { duration: '2m', target: 75 },   // Moderate DB load
        { duration: '3m', target: 150 },  // High DB operations
        { duration: '3m', target: 250 },  // Extreme DB stress
        { duration: '2m', target: 0 }     // Cool down
      ],
      exec: 'databaseStressTest',
      startTime: '2m',
      tags: { stress_type: 'database' }
    },
    
    // WebSocket connection stress
    websocket_connection_stress: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 50 },   // Normal WS connections
        { duration: '2m', target: 150 },  // High connection count
        { duration: '3m', target: 300 },  // Extreme connections
        { duration: '3m', target: 500 },  // Connection limit test
        { duration: '2m', target: 0 }     // Connection cleanup
      ],
      exec: 'webSocketStressTest',
      startTime: '3m',
      tags: { stress_type: 'websocket_connections' }
    },
    
    // Memory and resource stress
    resource_exhaustion_test: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '2m', target: 25 },   // Baseline resource usage
        { duration: '3m', target: 100 },  // Increased resource demand
        { duration: '4m', target: 200 },  // High resource consumption
        { duration: '3m', target: 400 },  // Resource exhaustion attempt
        { duration: '3m', target: 0 }     // Resource recovery
      ],
      exec: 'resourceExhaustionTest',
      startTime: '4m',
      tags: { stress_type: 'resource_exhaustion' }
    }
  },
  
  // Relaxed thresholds for stress conditions
  thresholds: {
    'http_req_duration': ['p(95)<5000'],        // Very relaxed response time
    'http_req_failed': ['rate<0.25'],           // 25% error rate acceptable under extreme stress
    'performance_degradation': ['rate<0.50'],   // 50% degradation acceptable
    'critical_errors': ['rate<0.10'],           // 10% critical errors maximum
    'system_recovery_time': ['p(95)<60000'],    // 1 minute recovery time
    'connection_failures': ['rate<0.30'],       // 30% connection failures acceptable
    'extreme_load_response_time': ['p(90)<3000'] // Response time under extreme load
  }
};

// Stress test users
const stressTestUsers = [];
for (let i = 1; i <= 100; i++) {
  stressTestUsers.push({
    email: `stresstest${i}@focushive.com`,
    password: 'StressTest123!',
    id: `stress-user-${i}`
  });
}

let authHelper, hiveHelper, presenceHelper, analyticsHelper, wsHelper;

export function setup() {
  console.log('Setting up Stress Load Test');
  
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  hiveHelper = new HiveHelper(SERVICE_ENDPOINTS.base_url);
  presenceHelper = new PresenceHelper(SERVICE_ENDPOINTS.base_url);
  analyticsHelper = new AnalyticsHelper(SERVICE_ENDPOINTS.base_url);
  wsHelper = new WebSocketTestHelper();
  
  // Create stress test environment
  const adminAuth = authHelper.login('admin@focushive.com', 'AdminPassword123!');
  const testHives = [];
  
  if (adminAuth.success) {
    // Create multiple hives for stress testing
    for (let i = 1; i <= 10; i++) {
      const hiveResult = hiveHelper.createHive(adminAuth.token, {
        name: `Stress Test Hive ${i}`,
        description: `High-capacity hive for stress testing - ${i}`,
        category: i % 3 === 0 ? 'work' : i % 3 === 1 ? 'study' : 'social',
        isPublic: true,
        maxMembers: 100
      });
      
      if (hiveResult.success) {
        testHives.push({
          id: hiveResult.hiveId,
          name: `Stress Test Hive ${i}`,
          maxMembers: 100
        });
      }
    }
  }
  
  console.log(`Stress test setup completed with ${testHives.length} high-capacity hives`);
  return {
    testHives,
    baseUrl: SERVICE_ENDPOINTS.base_url,
    wsUrl: SERVICE_ENDPOINTS.websockets.main
  };
}

export function progressiveApiStressTest(data) {
  const stressTestStart = Date.now();
  const currentVUs = __VU;
  systemBreakingPoint.add(currentVUs);
  
  group('Progressive API Stress Test', () => {
    const userIndex = (__VU - 1) % stressTestUsers.length;
    const testUser = stressTestUsers[userIndex];
    
    group('Authentication Under Stress', () => {
      const authStart = Date.now();
      const loginResult = authHelper.login(testUser.email, testUser.password);
      const authEnd = Date.now();
      
      extremeLoadResponseTime.add(authEnd - authStart);
      
      const authSuccess = check(loginResult, {
        'auth under stress successful': (r) => r.success,
        'auth stress response reasonable': (r) => r.responseTime < 10000
      });
      
      if (!authSuccess) {
        criticalErrorRate.add(1);
        if (authEnd - authStart > 5000) {
          degradationRate.add(1);
        }
      }
      
      if (!loginResult.success) {
        return; // Skip rest of test if auth fails
      }
      
      const authToken = loginResult.token;
      
      // Stress test with rapid API calls
      group('Rapid API Operations Under Stress', () => {
        const stressOperations = [];
        
        // Generate high-frequency operations based on current load level
        const operationCount = Math.min(currentVUs / 10, 20); // Scale with VU count
        
        for (let i = 0; i < operationCount; i++) {
          const operations = [
            () => hiveHelper.listHives(authToken),
            () => hiveHelper.createHive(authToken, {
              name: `Stress Hive ${currentVUs}-${i}-${Date.now()}`,
              description: 'Temporary stress test hive',
              category: 'work',
              isPublic: false,
              maxMembers: 10
            }),
            () => presenceHelper.updatePresence(authToken, {
              status: i % 2 === 0 ? 'online' : 'busy',
              activity: `stress-operation-${i}`
            }),
            () => analyticsHelper.getDashboard(authToken),
            () => authHelper.getProfile(authToken)
          ];
          
          stressOperations.push(operations[i % operations.length]);
        }
        
        // Execute operations with minimal delays
        stressOperations.forEach((operation, index) => {
          const opStart = Date.now();
          
          try {
            const result = operation();
            const opEnd = Date.now();
            
            extremeLoadResponseTime.add(opEnd - opStart);
            
            if (result && result.success) {
              if (opEnd - opStart > 3000) {
                degradationRate.add(1);
              }
            } else {
              criticalErrorRate.add(1);
              
              // Check for resource exhaustion indicators
              if (result && result.error && 
                  (result.error.includes('timeout') || 
                   result.error.includes('connection') ||
                   result.error.includes('memory'))) {
                resourceExhaustionRate.add(1);
              }
            }
            
            // Detect memory pressure indicators
            if (opEnd - opStart > 5000) {
              memoryPressureIndicator.add(1);
            }
            
          } catch (error) {
            criticalErrorRate.add(1);
            console.error(`Critical error in stress test: ${error.message}`);
          }
          
          // Minimal sleep - stress test pushes limits
          sleep(0.01);
        });
      });
    });
  });
  
  const stressTestEnd = Date.now();
  const recoveryTime = stressTestEnd - stressTestStart;
  systemRecoveryTime.add(recoveryTime);
  
  // Adaptive sleep based on current stress level
  const stressLevel = Math.min(currentVUs / 100, 1);
  sleep(0.1 * (1 - stressLevel)); // Less sleep under higher stress
}

export function databaseStressTest(data) {
  const dbStressStart = Date.now();
  const currentVUs = __VU;
  
  group('Database Stress Test', () => {
    const userIndex = (__VU - 1) % stressTestUsers.length;
    const testUser = stressTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      criticalErrorRate.add(1);
      return;
    }
    
    const authToken = authResult.token;
    
    group('Database-Intensive Operations', () => {
      const dbOperations = [
        // Create multiple hives (database writes)
        () => hiveHelper.createHive(authToken, {
          name: `DB Stress Hive ${currentVUs}-${Date.now()}`,
          description: 'Database stress test hive with long description to increase DB load',
          category: 'work',
          isPublic: true,
          maxMembers: 50
        }),
        
        // Complex hive queries (database reads)
        () => hiveHelper.searchHives(authToken, 'stress'),
        () => hiveHelper.listHives(authToken),
        
        // Presence operations (frequent updates)
        () => presenceHelper.updatePresence(authToken, {
          status: 'online',
          activity: `db-stress-${Date.now()}`,
          mood: 'focused',
          location: 'stress-test-environment'
        }),
        () => presenceHelper.getPresenceHistory(authToken),
        
        // Analytics operations (complex queries)
        () => analyticsHelper.getDashboard(authToken),
        () => analyticsHelper.getProductivityInsights(authToken),
        () => analyticsHelper.getHistoricalData(authToken, {
          period: 'week',
          metrics: ['all']
        })
      ];
      
      // Execute multiple DB operations rapidly
      const dbOpCount = Math.min(currentVUs / 5, 15);
      
      for (let i = 0; i < dbOpCount; i++) {
        const operation = dbOperations[i % dbOperations.length];
        const dbOpStart = Date.now();
        
        try {
          const result = operation();
          const dbOpEnd = Date.now();
          
          extremeLoadResponseTime.add(dbOpEnd - dbOpStart);
          
          // Check for database stress indicators
          if (dbOpEnd - dbOpStart > 5000) {
            degradationRate.add(1);
            memoryPressureIndicator.add(1);
          }
          
          if (!result || !result.success) {
            if (result && result.error && 
                (result.error.includes('connection') || 
                 result.error.includes('timeout') ||
                 result.error.includes('database'))) {
              resourceExhaustionRate.add(1);
            }
            criticalErrorRate.add(1);
          }
          
        } catch (error) {
          criticalErrorRate.add(1);
          console.error(`Database stress error: ${error.message}`);
        }
        
        sleep(0.02);
      }
    });
    
    // Test database transaction handling under stress
    group('Transaction Stress', () => {
      const targetHive = data.testHives[currentVUs % data.testHives.length];
      
      if (targetHive) {
        // Rapid join/leave operations
        for (let i = 0; i < 3; i++) {
          const joinStart = Date.now();
          const joinResult = hiveHelper.joinHive(authToken, targetHive.id);
          const joinEnd = Date.now();
          
          extremeLoadResponseTime.add(joinEnd - joinStart);
          
          if (!joinResult.success) {
            criticalErrorRate.add(1);
          }
          
          sleep(0.05);
          
          const leaveStart = Date.now();
          const leaveResult = hiveHelper.leaveHive(authToken, targetHive.id);
          const leaveEnd = Date.now();
          
          extremeLoadResponseTime.add(leaveEnd - leaveStart);
          
          if (!leaveResult.success) {
            criticalErrorRate.add(1);
          }
          
          sleep(0.05);
        }
      }
    });
  });
  
  const dbStressEnd = Date.now();
  systemRecoveryTime.add(dbStressEnd - dbStressStart);
  
  sleep(0.05);
}

export function webSocketStressTest(data) {
  const wsStressStart = Date.now();
  const currentVUs = __VU;
  
  group('WebSocket Connection Stress Test', () => {
    const userIndex = (__VU - 1) % stressTestUsers.length;
    const testUser = stressTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      connectionFailureRate.add(1);
      return;
    }
    
    const wsUrl = `${data.wsUrl}?token=${authResult.token}`;
    const connectionAttemptStart = Date.now();
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${authResult.token}`
      }
    }, (socket) => {
      const connectionEstablished = Date.now();
      const connectionTime = connectionEstablished - connectionAttemptStart;
      
      extremeLoadResponseTime.add(connectionTime);
      
      if (connectionTime > 3000) {
        degradationRate.add(1);
      }
      
      socket.on('open', () => {
        let messagesSent = 0;
        let messagesReceived = 0;
        
        // Send high-frequency messages to stress WebSocket handling
        const messageInterval = socket.setInterval(() => {
          const stressMessages = [
            wsHelper.createPresenceMessage({
              userId: testUser.id,
              status: 'online',
              activity: `ws-stress-${messagesSent}`,
              timestamp: Date.now()
            }),
            wsHelper.createChatMessage({
              senderId: testUser.id,
              hiveId: data.testHives[0]?.id,
              message: `WebSocket stress message ${messagesSent} from VU ${currentVUs}`,
              timestamp: Date.now()
            })
          ];
          
          stressMessages.forEach(msg => {
            const sendStart = Date.now();
            socket.send(JSON.stringify(msg));
            messagesSent++;
            
            // Check for send latency under stress
            const sendLatency = Date.now() - sendStart;
            if (sendLatency > 100) {
              degradationRate.add(1);
            }
          });
          
          // Increase message frequency under higher VU counts
          const messageFrequency = Math.max(100, 1000 - (currentVUs * 2));
        }, Math.max(50, messageFrequency));
        
        // Stop after connection limit test duration
        socket.setTimeout(() => {
          socket.clearInterval(messageInterval);
        }, 180000); // 3 minutes
      });
      
      socket.on('message', (message) => {
        const receiveStart = Date.now();
        
        try {
          const data = JSON.parse(message);
          messagesReceived++;
          
          if (data.timestamp) {
            const latency = receiveStart - data.timestamp;
            extremeLoadResponseTime.add(latency);
            
            if (latency > 500) {
              degradationRate.add(1);
            }
          }
          
        } catch (error) {
          criticalErrorRate.add(1);
        }
      });
      
      socket.on('error', (error) => {
        connectionFailureRate.add(1);
        criticalErrorRate.add(1);
        
        if (error.message.includes('connection') || 
            error.message.includes('timeout')) {
          resourceExhaustionRate.add(1);
        }
        
        console.error(`WebSocket stress error: ${error.message}`);
      });
      
      socket.on('close', () => {
        console.log(`WebSocket stress test completed: ${messagesSent} sent, ${messagesReceived} received`);
      });
      
      // Hold connection for stress test duration
      socket.setTimeout(() => {
        socket.close();
      }, 190000);
    });
    
    if (!response || response.status !== 101) {
      connectionFailureRate.add(1);
      criticalErrorRate.add(1);
    } else {
      // Hold the connection to maintain stress
      sleep(185);
    }
  });
  
  const wsStressEnd = Date.now();
  systemRecoveryTime.add(wsStressEnd - wsStressStart);
}

export function resourceExhaustionTest(data) {
  const resourceTestStart = Date.now();
  const currentVUs = __VU;
  
  group('Resource Exhaustion Test', () => {
    const userIndex = (__VU - 1) % stressTestUsers.length;
    const testUser = stressTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      criticalErrorRate.add(1);
      return;
    }
    
    const authToken = authResult.token;
    
    group('Memory Pressure Operations', () => {
      // Operations designed to consume server memory
      const memoryIntensiveOps = [
        // Large data retrievals
        () => analyticsHelper.getHistoricalData(authToken, {
          period: 'month',
          metrics: ['all'],
          granularity: 'hour'
        }),
        
        // Complex searches
        () => hiveHelper.searchHives(authToken, 'stress test memory pressure'),
        
        // Large hive operations
        () => hiveHelper.listHives(authToken, { 
          includeMembers: true, 
          includeAnalytics: true 
        }),
        
        // Bulk presence operations
        () => presenceHelper.getBulkPresence(authToken, data.testHives.map(h => h.id))
      ];
      
      // Execute memory-intensive operations
      const memOpsCount = Math.min(currentVUs / 3, 10);
      
      for (let i = 0; i < memOpsCount; i++) {
        const operation = memoryIntensiveOps[i % memoryIntensiveOps.length];
        const memOpStart = Date.now();
        
        try {
          const result = operation();
          const memOpEnd = Date.now();
          
          extremeLoadResponseTime.add(memOpEnd - memOpStart);
          
          // Detect memory pressure indicators
          if (memOpEnd - memOpStart > 8000) {
            memoryPressureIndicator.add(Math.min(currentVUs / 50, 5));
            degradationRate.add(1);
          }
          
          if (!result || !result.success) {
            if (result && result.error && 
                (result.error.includes('memory') || 
                 result.error.includes('resource') ||
                 result.error.includes('limit'))) {
              resourceExhaustionRate.add(1);
            }
            criticalErrorRate.add(1);
          }
          
        } catch (error) {
          criticalErrorRate.add(1);
          
          if (error.message.includes('memory') || 
              error.message.includes('resource')) {
            resourceExhaustionRate.add(1);
          }
        }
        
        sleep(0.03);
      }
    });
    
    group('Connection Pool Exhaustion', () => {
      // Rapidly create and abandon connections
      for (let i = 0; i < 5; i++) {
        const connStart = Date.now();
        
        // Attempt multiple rapid operations to exhaust connection pool
        const rapidOps = [
          () => authHelper.validateToken(authToken),
          () => hiveHelper.listHives(authToken),
          () => presenceHelper.updatePresence(authToken, { status: 'online' })
        ];
        
        rapidOps.forEach(op => {
          try {
            const result = op();
            const connEnd = Date.now();
            
            if (connEnd - connStart > 5000) {
              resourceExhaustionRate.add(1);
            }
            
          } catch (error) {
            if (error.message.includes('connection') || 
                error.message.includes('pool')) {
              resourceExhaustionRate.add(1);
            }
          }
        });
        
        sleep(0.01);
      }
    });
  });
  
  const resourceTestEnd = Date.now();
  systemRecoveryTime.add(resourceTestEnd - resourceTestStart);
  
  sleep(0.02);
}

export function teardown(data) {
  console.log('Stress Load Test completed');
  console.log(`System breaking point detected at: ${systemBreakingPoint.value} VUs`);
  console.log(`Performance degradation rate: ${(degradationRate.rate * 100).toFixed(2)}%`);
  console.log(`Resource exhaustion rate: ${(resourceExhaustionRate.rate * 100).toFixed(2)}%`);
  console.log(`Critical error rate: ${(criticalErrorRate.rate * 100).toFixed(2)}%`);
  console.log(`Average system recovery time: ${systemRecoveryTime.avg}ms`);
  console.log(`Average extreme load response time: ${extremeLoadResponseTime.avg}ms`);
  console.log(`Memory pressure indicator: ${memoryPressureIndicator.value}`);
  console.log(`Connection failure rate: ${(connectionFailureRate.rate * 100).toFixed(2)}%`);
  
  // Stress test analysis
  if (degradationRate.rate < 0.3 && criticalErrorRate.rate < 0.1) {
    console.log('✅ Stress test PASSED - System handles extreme load well');
  } else if (degradationRate.rate < 0.5 && criticalErrorRate.rate < 0.15) {
    console.log('⚠️ Stress test PARTIAL - System shows resilience but needs optimization');
  } else {
    console.log('❌ Stress test FAILED - System requires significant improvements for high load');
  }
  
  console.log(`\nRecommendations:`);
  if (resourceExhaustionRate.rate > 0.2) {
    console.log('- Consider increasing server resources (CPU, memory)');
  }
  if (connectionFailureRate.rate > 0.2) {
    console.log('- Review connection pool configuration');
  }
  if (memoryPressureIndicator.value > 3) {
    console.log('- Implement memory optimization and garbage collection tuning');
  }
  if (degradationRate.rate > 0.3) {
    console.log('- Consider implementing request rate limiting');
  }
}