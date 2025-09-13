/**
 * k6 WebSocket Load Test
 * 
 * Tests WebSocket performance under various load conditions
 * Focuses on real-time presence updates, chat messages, and concurrent connections
 */

import ws from 'k6/ws';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { AuthenticationHelper, WebSocketTestHelper } from './utils/helpers.js';
import { WEBSOCKET_THRESHOLDS, LOAD_TEST_SCENARIOS, SERVICE_ENDPOINTS } from './config/thresholds.js';

// Custom metrics
export const wsConnectionRate = new Rate('ws_connection_success');
export const wsMessageLatency = new Trend('ws_message_latency');
export const wsErrorRate = new Rate('ws_errors');
export const wsMessagesPerSecond = new Rate('ws_messages_per_second');
export const activeConcurrentConnections = new Gauge('ws_active_connections');
export const messageCounter = new Counter('ws_messages_total');

// Test configuration
export const options = {
  scenarios: {
    // Connection stability test
    connection_stability: {
      executor: 'constant-vus',
      vus: 10,
      duration: '2m',
      exec: 'connectionStabilityTest',
      tags: { test_type: 'stability' }
    },
    
    // Message throughput test
    message_throughput: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      exec: 'messageThroughputTest',
      startTime: '30s',
      tags: { test_type: 'throughput' }
    },
    
    // Concurrent connections test
    concurrent_connections: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 0 }
      ],
      exec: 'concurrentConnectionsTest',
      startTime: '3m',
      tags: { test_type: 'concurrent' }
    }
  },
  
  thresholds: {
    ...WEBSOCKET_THRESHOLDS,
    'ws_connection_success': ['rate>0.95'],
    'ws_message_latency': ['p(95)<100', 'p(99)<200'],
    'ws_errors': ['rate<0.01'],
    'ws_messages_per_second': ['rate>10'],
    'ws_active_connections': ['value<150']
  }
};

// Test users and data
const testUsers = [
  { email: 'wstest1@focushive.com', password: 'WSTest123!' },
  { email: 'wstest2@focushive.com', password: 'WSTest123!' },
  { email: 'wstest3@focushive.com', password: 'WSTest123!' },
  { email: 'wstest4@focushive.com', password: 'WSTest123!' },
  { email: 'wstest5@focushive.com', password: 'WSTest123!' }
];

let authHelper;
let wsHelper;

export function setup() {
  console.log('Setting up WebSocket Load Test');
  
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  wsHelper = new WebSocketTestHelper();
  
  // Pre-authenticate test users
  const authenticatedUsers = [];
  
  for (const user of testUsers) {
    const authResult = authHelper.login(user.email, user.password);
    if (authResult.success) {
      authenticatedUsers.push({
        ...user,
        token: authResult.token,
        userId: authResult.userId
      });
    }
  }
  
  if (authenticatedUsers.length === 0) {
    console.error('No users could be authenticated for WebSocket tests');
    return { setupFailed: true };
  }
  
  console.log(`WebSocket Load Test setup completed with ${authenticatedUsers.length} users`);
  return {
    setupFailed: false,
    users: authenticatedUsers,
    wsUrl: SERVICE_ENDPOINTS.websockets.main
  };
}

export function connectionStabilityTest(data) {
  if (data.setupFailed) {
    console.error('Skipping connection stability test due to setup failure');
    return;
  }
  
  const userIndex = (__VU - 1) % data.users.length;
  const testUser = data.users[userIndex];
  
  group('WebSocket Connection Stability', () => {
    const wsUrl = `${data.wsUrl}?token=${testUser.token}`;
    const connectionStart = Date.now();
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${testUser.token}`
      }
    }, (socket) => {
      const connectionTime = Date.now() - connectionStart;
      
      socket.on('open', () => {
        wsConnectionRate.add(1);
        activeConcurrentConnections.add(1);
        console.log(`WebSocket connected for user ${testUser.email} in ${connectionTime}ms`);
        
        // Send initial presence update
        const presenceMessage = wsHelper.createPresenceMessage({
          userId: testUser.userId,
          status: 'online',
          activity: 'load-testing',
          timestamp: Date.now()
        });
        
        const messageSent = Date.now();
        socket.send(JSON.stringify(presenceMessage));
        messageCounter.add(1);
        
        // Set up periodic heartbeat
        socket.setInterval(() => {
          const heartbeat = wsHelper.createHeartbeatMessage();
          socket.send(JSON.stringify(heartbeat));
          messageCounter.add(1);
        }, 10000);
        
        // Send periodic presence updates
        socket.setInterval(() => {
          const updateMessage = wsHelper.createPresenceMessage({
            userId: testUser.userId,
            status: Math.random() > 0.5 ? 'online' : 'busy',
            activity: `activity-${Math.floor(Math.random() * 10)}`,
            timestamp: Date.now()
          });
          
          const updateSent = Date.now();
          socket.send(JSON.stringify(updateMessage));
          messageCounter.add(1);
          wsMessagesPerSecond.add(1);
        }, 5000);
      });
      
      socket.on('message', (message) => {
        const messageReceived = Date.now();
        
        try {
          const data = JSON.parse(message);
          
          // Calculate latency for relevant message types
          if (data.timestamp) {
            const latency = messageReceived - data.timestamp;
            wsMessageLatency.add(latency);
          }
          
          // Handle different message types
          switch (data.type) {
            case 'presence-update':
              check(data, {
                'presence update valid': (d) => d.userId && d.status,
                'presence timestamp valid': (d) => d.timestamp > 0
              });
              break;
              
            case 'chat-message':
              check(data, {
                'chat message valid': (d) => d.message && d.senderId,
                'chat timestamp valid': (d) => d.timestamp > 0
              });
              break;
              
            case 'hive-notification':
              check(data, {
                'notification valid': (d) => d.type && d.content,
                'notification timestamp valid': (d) => d.timestamp > 0
              });
              break;
          }
          
        } catch (error) {
          wsErrorRate.add(1);
          console.error(`Failed to parse WebSocket message: ${error.message}`);
        }
      });
      
      socket.on('error', (error) => {
        wsErrorRate.add(1);
        console.error(`WebSocket error for user ${testUser.email}: ${error.message}`);
      });
      
      socket.on('close', () => {
        activeConcurrentConnections.add(-1);
        console.log(`WebSocket connection closed for user ${testUser.email}`);
      });
      
      // Keep connection alive for test duration
      socket.setTimeout(() => {
        socket.close();
      }, 110000); // Close slightly before scenario ends
    });
    
    check(response, {
      'WebSocket connection established': (r) => r && r.status === 101
    });
    
    if (!response || response.status !== 101) {
      wsConnectionRate.add(0);
      wsErrorRate.add(1);
    }
  });
}

export function messageThroughputTest(data) {
  if (data.setupFailed) {
    console.error('Skipping message throughput test due to setup failure');
    return;
  }
  
  const userIndex = (__VU - 1) % data.users.length;
  const testUser = data.users[userIndex];
  
  group('WebSocket Message Throughput', () => {
    const wsUrl = `${data.wsUrl}?token=${testUser.token}`;
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${testUser.token}`
      }
    }, (socket) => {
      let messagesSent = 0;
      let messagesReceived = 0;
      
      socket.on('open', () => {
        wsConnectionRate.add(1);
        activeConcurrentConnections.add(1);
        
        // Send high-frequency messages
        const messageInterval = socket.setInterval(() => {
          const messages = [
            wsHelper.createPresenceMessage({
              userId: testUser.userId,
              status: 'online',
              activity: 'high-frequency-test',
              timestamp: Date.now()
            }),
            wsHelper.createChatMessage({
              senderId: testUser.userId,
              hiveId: 'load-test-hive',
              message: `High frequency message ${messagesSent}`,
              timestamp: Date.now()
            })
          ];
          
          messages.forEach(msg => {
            socket.send(JSON.stringify(msg));
            messagesSent++;
            messageCounter.add(1);
            wsMessagesPerSecond.add(1);
          });
        }, 200); // Send messages every 200ms (5 messages per second per connection)
        
        // Stop after 1 minute
        socket.setTimeout(() => {
          socket.clearInterval(messageInterval);
        }, 60000);
      });
      
      socket.on('message', (message) => {
        messagesReceived++;
        
        try {
          const data = JSON.parse(message);
          if (data.timestamp) {
            const latency = Date.now() - data.timestamp;
            wsMessageLatency.add(latency);
          }
        } catch (error) {
          wsErrorRate.add(1);
        }
      });
      
      socket.on('error', (error) => {
        wsErrorRate.add(1);
        console.error(`Throughput test WebSocket error: ${error.message}`);
      });
      
      socket.on('close', () => {
        activeConcurrentConnections.add(-1);
        console.log(`Throughput test completed for user ${testUser.email}: ${messagesSent} sent, ${messagesReceived} received`);
      });
      
      socket.setTimeout(() => {
        socket.close();
      }, 85000); // Close before scenario ends
    });
    
    if (!response || response.status !== 101) {
      wsConnectionRate.add(0);
      wsErrorRate.add(1);
    }
  });
}

export function concurrentConnectionsTest(data) {
  if (data.setupFailed) {
    console.error('Skipping concurrent connections test due to setup failure');
    return;
  }
  
  const userIndex = (__VU - 1) % data.users.length;
  const testUser = data.users[userIndex];
  
  group('WebSocket Concurrent Connections', () => {
    const wsUrl = `${data.wsUrl}?token=${testUser.token}`;
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${testUser.token}`
      }
    }, (socket) => {
      socket.on('open', () => {
        wsConnectionRate.add(1);
        activeConcurrentConnections.add(1);
        
        // Send presence update to simulate active user
        const presenceMessage = wsHelper.createPresenceMessage({
          userId: testUser.userId,
          status: 'online',
          activity: 'concurrent-test',
          timestamp: Date.now()
        });
        
        socket.send(JSON.stringify(presenceMessage));
        messageCounter.add(1);
        
        // Periodic activity to keep connection active
        const activityInterval = socket.setInterval(() => {
          const activityMessage = wsHelper.createPresenceMessage({
            userId: testUser.userId,
            status: 'online',
            activity: `concurrent-activity-${Date.now()}`,
            timestamp: Date.now()
          });
          
          socket.send(JSON.stringify(activityMessage));
          messageCounter.add(1);
          wsMessagesPerSecond.add(1);
        }, 15000);
        
        socket.setTimeout(() => {
          socket.clearInterval(activityInterval);
        }, 300000);
      });
      
      socket.on('message', (message) => {
        try {
          const data = JSON.parse(message);
          if (data.timestamp) {
            const latency = Date.now() - data.timestamp;
            wsMessageLatency.add(latency);
          }
        } catch (error) {
          wsErrorRate.add(1);
        }
      });
      
      socket.on('error', (error) => {
        wsErrorRate.add(1);
        console.error(`Concurrent connections test WebSocket error: ${error.message}`);
      });
      
      socket.on('close', () => {
        activeConcurrentConnections.add(-1);
      });
      
      socket.setTimeout(() => {
        socket.close();
      }, 330000); // Close before scenario ends
    });
    
    if (!response || response.status !== 101) {
      wsConnectionRate.add(0);
      wsErrorRate.add(1);
    }
    
    // Hold connection for the duration
    sleep(300);
  });
}

export function teardown(data) {
  if (data.setupFailed) {
    console.log('Teardown skipped due to setup failure');
    return;
  }
  
  console.log('WebSocket Load Test completed');
  console.log(`Total WebSocket messages: ${messageCounter.value}`);
  console.log(`Average message latency: ${wsMessageLatency.avg}ms`);
  console.log(`Connection success rate: ${(wsConnectionRate.rate * 100).toFixed(2)}%`);
  console.log(`WebSocket error rate: ${(wsErrorRate.rate * 100).toFixed(2)}%`);
  console.log(`Messages per second rate: ${wsMessagesPerSecond.rate.toFixed(2)}`);
  console.log(`Peak concurrent connections: ${activeConcurrentConnections.value}`);
}