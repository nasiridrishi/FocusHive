/**
 * k6 Spike Load Test
 * 
 * Tests system behavior under sudden load increases
 * Validates system resilience and recovery from traffic spikes
 */

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { 
  AuthenticationHelper, 
  HiveHelper, 
  PresenceHelper,
  WebSocketTestHelper 
} from './utils/helpers.js';
import { LOAD_TEST_SCENARIOS, SERVICE_ENDPOINTS } from './config/thresholds.js';

// Custom metrics for spike testing
export const spikeRecoveryTime = new Trend('spike_recovery_time');
export const systemStabilityRate = new Rate('system_stability');
export const peakPerformanceMetric = new Trend('peak_performance_response');
export const errorSpike = new Counter('spike_errors');
export const concurrentSpikeUsers = new Gauge('concurrent_spike_users');
export const throughputDuringSpikeRate = new Rate('throughput_during_spike');

// Test configuration
export const options = {
  scenarios: {
    // Sudden authentication spike
    auth_spike: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 5 },    // Normal baseline
        { duration: '10s', target: 100 }, // Sudden spike!
        { duration: '2m', target: 100 },  // Sustain spike
        { duration: '10s', target: 5 },   // Drop back
        { duration: '1m', target: 5 }     // Recovery period
      ],
      exec: 'authenticationSpikeTest',
      tags: { spike_type: 'authentication' }
    },
    
    // API traffic spike
    api_traffic_spike: {
      executor: 'ramping-vus',
      startVUs: 2,
      stages: [
        { duration: '2m', target: 10 },   // Normal load
        { duration: '15s', target: 200 }, // Massive spike!
        { duration: '3m', target: 200 },  // Sustain high load
        { duration: '15s', target: 10 },  // Quick drop
        { duration: '2m', target: 10 }    // Recovery
      ],
      exec: 'apiTrafficSpikeTest',
      startTime: '30s',
      tags: { spike_type: 'api_traffic' }
    },
    
    // WebSocket connection spike
    websocket_spike: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 5 },    // Baseline connections
        { duration: '20s', target: 150 }, // Connection surge
        { duration: '4m', target: 150 },  // Sustain connections
        { duration: '20s', target: 5 },   // Connection drop
        { duration: '1m', target: 0 }     // Complete cleanup
      ],
      exec: 'webSocketSpikeTest',
      startTime: '1m',
      tags: { spike_type: 'websocket' }
    },
    
    // Mixed workload spike
    mixed_workload_spike: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 300,
      maxVUs: 500,
      stages: [
        { duration: '1m', target: 10 },   // Normal rate
        { duration: '30s', target: 300 }, // Extreme spike!
        { duration: '2m', target: 300 },  // Sustain extreme load
        { duration: '30s', target: 10 },  // Back to normal
        { duration: '1m', target: 5 }     // Cool down
      ],
      exec: 'mixedWorkloadSpikeTest',
      startTime: '2m',
      tags: { spike_type: 'mixed_workload' }
    }
  },
  
  thresholds: {
    // Relaxed thresholds for spike conditions
    'http_req_duration': ['p(95)<3000'], // Allow higher response times during spike
    'http_req_failed': ['rate<0.15'],    // 15% error rate acceptable during spike
    'spike_recovery_time': ['p(95)<30000'], // Recovery within 30 seconds
    'system_stability': ['rate>0.80'],   // 80% stability during spike
    'peak_performance_response': ['p(90)<2000'], // Performance during peak
    'spike_errors': ['count<1000'],      // Limit total spike errors
    'concurrent_spike_users': ['value<600'], // Maximum concurrent users
    'throughput_during_spike': ['rate>0.70']  // Maintain 70% throughput
  }
};

// Test users for spike testing
const spikeTestUsers = [];
for (let i = 1; i <= 50; i++) {
  spikeTestUsers.push({
    email: `spiketest${i}@focushive.com`,
    password: 'SpikeTest123!'
  });
}

let authHelper, hiveHelper, presenceHelper, wsHelper;

export function setup() {
  console.log('Setting up Spike Load Test');
  
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  hiveHelper = new HiveHelper(SERVICE_ENDPOINTS.base_url);
  presenceHelper = new PresenceHelper(SERVICE_ENDPOINTS.base_url);
  wsHelper = new WebSocketTestHelper();
  
  // Pre-warm system with baseline users
  const preWarmResults = [];
  for (let i = 0; i < 5; i++) {
    const user = spikeTestUsers[i];
    const authResult = authHelper.login(user.email, user.password);
    if (authResult.success) {
      preWarmResults.push({
        email: user.email,
        token: authResult.token
      });
    }
  }
  
  console.log(`Spike test setup completed with ${preWarmResults.length} pre-warmed users`);
  return {
    preWarmedUsers: preWarmResults,
    baseUrl: SERVICE_ENDPOINTS.base_url,
    wsUrl: SERVICE_ENDPOINTS.websockets.main
  };
}

export function authenticationSpikeTest(data) {
  const spikeStart = Date.now();
  concurrentSpikeUsers.add(1);
  
  group('Authentication Spike Test', () => {
    const userIndex = (__VU - 1) % spikeTestUsers.length;
    const testUser = spikeTestUsers[userIndex];
    
    group('High-Volume Authentication', () => {
      const authStart = Date.now();
      const loginResult = authHelper.login(testUser.email, testUser.password);
      const authEnd = Date.now();
      
      const authSuccess = check(loginResult, {
        'authentication successful': (r) => r.success,
        'auth response time reasonable': (r) => r.responseTime < 5000 // Relaxed for spike
      });
      
      systemStabilityRate.add(authSuccess ? 1 : 0);
      peakPerformanceMetric.add(authEnd - authStart);
      
      if (!authSuccess) {
        errorSpike.add(1);
      } else {
        throughputDuringSpikeRate.add(1);
        
        // Quick token validation to add load
        const validateResult = authHelper.validateToken(loginResult.token);
        if (!validateResult.success) {
          errorSpike.add(1);
        }
      }
      
      sleep(0.1); // Minimal sleep for spike simulation
    });
    
    group('Rapid Profile Operations', () => {
      // Attempt multiple quick operations to stress the system
      const operations = [
        () => authHelper.getProfile(data.preWarmedUsers[0]?.token),
        () => hiveHelper.listHives(data.preWarmedUsers[0]?.token),
        () => presenceHelper.updatePresence(data.preWarmedUsers[0]?.token, {
          status: 'online',
          activity: 'spike-testing'
        })
      ];
      
      operations.forEach((operation, index) => {
        try {
          const result = operation();
          if (result && result.success) {
            throughputDuringSpikeRate.add(1);
          } else {
            errorSpike.add(1);
          }
        } catch (error) {
          errorSpike.add(1);
        }
        
        sleep(0.05); // Very quick operations
      });
    });
  });
  
  const spikeEnd = Date.now();
  spikeRecoveryTime.add(spikeEnd - spikeStart);
  concurrentSpikeUsers.add(-1);
}

export function apiTrafficSpikeTest(data) {
  const trafficSpikeStart = Date.now();
  concurrentSpikeUsers.add(1);
  
  group('API Traffic Spike Test', () => {
    const userToken = data.preWarmedUsers[(__VU - 1) % data.preWarmedUsers.length]?.token;
    
    if (!userToken) {
      errorSpike.add(1);
      concurrentSpikeUsers.add(-1);
      return;
    }
    
    group('Rapid API Requests', () => {
      const apiOperations = [
        // Hive operations
        () => hiveHelper.listHives(userToken),
        () => hiveHelper.createHive(userToken, {
          name: `Spike Test Hive ${Date.now()}`,
          description: 'Temporary hive for spike testing',
          category: 'work',
          isPublic: false,
          maxMembers: 5
        }),
        
        // Presence operations
        () => presenceHelper.updatePresence(userToken, {
          status: Math.random() > 0.5 ? 'online' : 'busy',
          activity: `spike-activity-${Math.floor(Math.random() * 100)}`
        }),
        () => presenceHelper.getPresenceHistory(userToken),
        
        // Profile operations
        () => authHelper.getProfile(userToken),
        () => authHelper.updateProfile(userToken, {
          bio: `Updated during spike test at ${Date.now()}`
        })
      ];
      
      // Execute multiple operations rapidly
      const operationsToExecute = Math.floor(Math.random() * 4) + 2; // 2-5 operations
      for (let i = 0; i < operationsToExecute; i++) {
        const operation = apiOperations[i % apiOperations.length];
        const opStart = Date.now();
        
        try {
          const result = operation();
          const opEnd = Date.now();
          
          peakPerformanceMetric.add(opEnd - opStart);
          
          if (result && result.success) {
            systemStabilityRate.add(1);
            throughputDuringSpikeRate.add(1);
          } else {
            systemStabilityRate.add(0);
            errorSpike.add(1);
          }
        } catch (error) {
          errorSpike.add(1);
          systemStabilityRate.add(0);
        }
        
        sleep(0.05); // Minimal delay between operations
      }
    });
    
    sleep(0.1);
  });
  
  const trafficSpikeEnd = Date.now();
  spikeRecoveryTime.add(trafficSpikeEnd - trafficSpikeStart);
  concurrentSpikeUsers.add(-1);
}

export function webSocketSpikeTest(data) {
  const wsSpikeStart = Date.now();
  concurrentSpikeUsers.add(1);
  
  group('WebSocket Connection Spike Test', () => {
    const userToken = data.preWarmedUsers[(__VU - 1) % data.preWarmedUsers.length]?.token;
    
    if (!userToken) {
      errorSpike.add(1);
      concurrentSpikeUsers.add(-1);
      return;
    }
    
    const wsUrl = `${data.wsUrl}?token=${userToken}`;
    const connectionStart = Date.now();
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${userToken}`
      }
    }, (socket) => {
      let messagesExchanged = 0;
      const connectionTime = Date.now() - connectionStart;
      
      socket.on('open', () => {
        systemStabilityRate.add(1);
        peakPerformanceMetric.add(connectionTime);
        
        // Send rapid messages to create WebSocket load
        const messageInterval = socket.setInterval(() => {
          const spikeMessages = [
            wsHelper.createPresenceMessage({
              userId: `spike-user-${__VU}`,
              status: 'online',
              activity: 'spike-testing',
              timestamp: Date.now()
            }),
            wsHelper.createChatMessage({
              senderId: `spike-user-${__VU}`,
              hiveId: 'spike-test-hive',
              message: `Spike test message ${messagesExchanged}`,
              timestamp: Date.now()
            }),
            wsHelper.createHeartbeatMessage()
          ];
          
          spikeMessages.forEach(msg => {
            socket.send(JSON.stringify(msg));
            messagesExchanged++;
            throughputDuringSpikeRate.add(1);
          });
          
          // Increase message frequency during peak spike
          if (__ITER > 60 && __ITER < 300) { // During high load phase
            socket.setTimeout(() => {
              socket.send(JSON.stringify(wsHelper.createHeartbeatMessage()));
              messagesExchanged++;
            }, 50);
          }
        }, 500);
        
        // Stop sending messages after 5 minutes
        socket.setTimeout(() => {
          socket.clearInterval(messageInterval);
        }, 300000);
      });
      
      socket.on('message', (message) => {
        try {
          const data = JSON.parse(message);
          if (data.timestamp) {
            const latency = Date.now() - data.timestamp;
            peakPerformanceMetric.add(latency);
          }
          throughputDuringSpikeRate.add(1);
        } catch (error) {
          errorSpike.add(1);
        }
      });
      
      socket.on('error', (error) => {
        errorSpike.add(1);
        systemStabilityRate.add(0);
        console.error(`WebSocket spike test error: ${error.message}`);
      });
      
      socket.on('close', () => {
        console.log(`WebSocket spike test completed: ${messagesExchanged} messages exchanged`);
      });
      
      // Keep connection alive for spike duration
      socket.setTimeout(() => {
        socket.close();
      }, 320000);
    });
    
    if (!response || response.status !== 101) {
      systemStabilityRate.add(0);
      errorSpike.add(1);
    }
    
    // Hold connection for spike test duration
    sleep(300);
  });
  
  const wsSpikeEnd = Date.now();
  spikeRecoveryTime.add(wsSpikeEnd - wsSpikeStart);
  concurrentSpikeUsers.add(-1);
}

export function mixedWorkloadSpikeTest(data) {
  const mixedSpikeStart = Date.now();
  concurrentSpikeUsers.add(1);
  
  group('Mixed Workload Spike Test', () => {
    const userToken = data.preWarmedUsers[(__VU - 1) % data.preWarmedUsers.length]?.token;
    
    if (!userToken) {
      errorSpike.add(1);
      concurrentSpikeUsers.add(-1);
      return;
    }
    
    // Randomly select workload type to create mixed load
    const workloadType = Math.random();
    
    if (workloadType < 0.3) {
      // Authentication-heavy workload
      group('Auth-Heavy Spike', () => {
        for (let i = 0; i < 3; i++) {
          const authOps = [
            () => authHelper.validateToken(userToken),
            () => authHelper.getProfile(userToken),
            () => authHelper.refreshToken(userToken)
          ];
          
          const operation = authOps[i % authOps.length];
          const result = operation();
          
          if (result && result.success) {
            throughputDuringSpikeRate.add(1);
            systemStabilityRate.add(1);
          } else {
            errorSpike.add(1);
            systemStabilityRate.add(0);
          }
          
          sleep(0.05);
        }
      });
      
    } else if (workloadType < 0.6) {
      // API-heavy workload
      group('API-Heavy Spike', () => {
        const apiOperations = [
          () => hiveHelper.listHives(userToken),
          () => presenceHelper.updatePresence(userToken, {
            status: 'busy',
            activity: 'mixed-spike-test'
          }),
          () => hiveHelper.searchHives(userToken, 'test')
        ];
        
        apiOperations.forEach(operation => {
          const opStart = Date.now();
          const result = operation();
          const opEnd = Date.now();
          
          peakPerformanceMetric.add(opEnd - opStart);
          
          if (result && result.success) {
            throughputDuringSpikeRate.add(1);
            systemStabilityRate.add(1);
          } else {
            errorSpike.add(1);
            systemStabilityRate.add(0);
          }
          
          sleep(0.03);
        });
      });
      
    } else {
      // WebSocket-heavy workload
      group('WebSocket-Heavy Spike', () => {
        const wsUrl = `${data.wsUrl}?token=${userToken}`;
        
        const response = ws.connect(wsUrl, {}, (socket) => {
          socket.on('open', () => {
            systemStabilityRate.add(1);
            
            // Send burst of messages
            for (let i = 0; i < 10; i++) {
              const message = wsHelper.createPresenceMessage({
                userId: `mixed-spike-${__VU}`,
                status: 'online',
                activity: `burst-message-${i}`,
                timestamp: Date.now()
              });
              
              socket.send(JSON.stringify(message));
              throughputDuringSpikeRate.add(1);
            }
            
            socket.setTimeout(() => {
              socket.close();
            }, 2000);
          });
          
          socket.on('error', () => {
            errorSpike.add(1);
            systemStabilityRate.add(0);
          });
        });
        
        if (!response || response.status !== 101) {
          errorSpike.add(1);
          systemStabilityRate.add(0);
        }
        
        sleep(2);
      });
    }
  });
  
  const mixedSpikeEnd = Date.now();
  spikeRecoveryTime.add(mixedSpikeEnd - mixedSpikeStart);
  concurrentSpikeUsers.add(-1);
  
  sleep(0.1);
}

export function teardown(data) {
  console.log('Spike Load Test completed');
  console.log(`Average spike recovery time: ${spikeRecoveryTime.avg}ms`);
  console.log(`System stability rate: ${(systemStabilityRate.rate * 100).toFixed(2)}%`);
  console.log(`Average peak performance response: ${peakPerformanceMetric.avg}ms`);
  console.log(`Total spike errors: ${errorSpike.value}`);
  console.log(`Peak concurrent users: ${concurrentSpikeUsers.value}`);
  console.log(`Throughput during spike: ${(throughputDuringSpikeRate.rate * 100).toFixed(2)}%`);
  
  // Recovery validation
  if (spikeRecoveryTime.avg < 30000 && systemStabilityRate.rate > 0.8) {
    console.log('✅ Spike test PASSED - System demonstrated good resilience');
  } else {
    console.log('⚠️ Spike test CONCERNS - System may need resilience improvements');
  }
}