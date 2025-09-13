/**
 * k6 Soak Load Test
 * 
 * Tests system stability over extended periods
 * Identifies memory leaks, resource degradation, and long-term reliability issues
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

// Custom metrics for soak testing
export const memoryLeakIndicator = new Trend('memory_leak_indicator');
export const responseTimeDegradation = new Trend('response_time_degradation');
export const systemStabilityOverTime = new Rate('system_stability_over_time');
export const longTermErrorRate = new Rate('long_term_error_rate');
export const resourceUsageGrowth = new Gauge('resource_usage_growth');
export const connectionStabilityRate = new Rate('connection_stability_rate');
export const performanceBaselineDeviation = new Trend('performance_baseline_deviation');
export const cumulativeErrorCount = new Counter('cumulative_errors');

// Test configuration for extended duration
export const options = {
  scenarios: {
    // Extended API stability test
    api_stability_soak: {
      executor: 'constant-vus',
      vus: 20,
      duration: '60m', // 1 hour soak test
      exec: 'apiStabilitySoakTest',
      tags: { soak_type: 'api_stability' }
    },
    
    // WebSocket long-term connection test
    websocket_long_term: {
      executor: 'constant-vus',
      vus: 15,
      duration: '45m', // 45 minutes WebSocket soak
      exec: 'webSocketLongTermTest',
      startTime: '5m',
      tags: { soak_type: 'websocket_longterm' }
    },
    
    // Database connection pool soak
    database_pool_soak: {
      executor: 'constant-vus',
      vus: 25,
      duration: '40m', // 40 minutes database soak
      exec: 'databasePoolSoakTest',
      startTime: '10m',
      tags: { soak_type: 'database_pool' }
    },
    
    // Memory leak detection test
    memory_leak_detection: {
      executor: 'constant-vus',
      vus: 10,
      duration: '90m', // 90 minutes for memory pattern detection
      exec: 'memoryLeakDetectionTest',
      startTime: '15m',
      tags: { soak_type: 'memory_leak' }
    }
  },
  
  // Strict thresholds for soak testing
  thresholds: {
    'http_req_duration': ['p(95)<1000', 'p(99)<2000'],  // Consistent performance
    'http_req_failed': ['rate<0.005'],                   // Very low error rate
    'memory_leak_indicator': ['trend<0.1'],              // No significant memory growth
    'response_time_degradation': ['trend<0.05'],         // Minimal performance degradation
    'system_stability_over_time': ['rate>0.98'],         // 98% stability
    'long_term_error_rate': ['rate<0.002'],              // 0.2% long-term error rate
    'connection_stability_rate': ['rate>0.99'],          // 99% connection stability
    'performance_baseline_deviation': ['p(95)<200'],     // Within 200ms of baseline
    'cumulative_errors': ['count<50']                    // Less than 50 total errors
  }
};

// Soak test users
const soakTestUsers = [];
for (let i = 1; i <= 30; i++) {
  soakTestUsers.push({
    email: `soaktest${i}@focushive.com`,
    password: 'SoakTest123!',
    id: `soak-user-${i}`
  });
}

let authHelper, hiveHelper, presenceHelper, analyticsHelper, wsHelper;
let testStartTime;
let baselineMetrics = {
  responseTime: 0,
  errorRate: 0,
  initialTimestamp: 0
};

export function setup() {
  console.log('Setting up Soak Load Test');
  testStartTime = Date.now();
  
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  hiveHelper = new HiveHelper(SERVICE_ENDPOINTS.base_url);
  presenceHelper = new PresenceHelper(SERVICE_ENDPOINTS.base_url);
  analyticsHelper = new AnalyticsHelper(SERVICE_ENDPOINTS.base_url);
  wsHelper = new WebSocketTestHelper();
  
  // Establish baseline metrics
  const adminAuth = authHelper.login('admin@focushive.com', 'AdminPassword123!');
  
  if (adminAuth.success) {
    const baselineStart = Date.now();
    const baselineOps = [
      () => hiveHelper.listHives(adminAuth.token),
      () => presenceHelper.updatePresence(adminAuth.token, { status: 'online' }),
      () => analyticsHelper.getDashboard(adminAuth.token)
    ];
    
    let totalResponseTime = 0;
    let errorCount = 0;
    
    baselineOps.forEach(op => {
      const opStart = Date.now();
      const result = op();
      const opEnd = Date.now();
      
      totalResponseTime += (opEnd - opStart);
      if (!result || !result.success) {
        errorCount++;
      }
    });
    
    baselineMetrics = {
      responseTime: totalResponseTime / baselineOps.length,
      errorRate: errorCount / baselineOps.length,
      initialTimestamp: baselineStart
    };
    
    console.log(`Baseline metrics established: ${baselineMetrics.responseTime}ms avg response, ${(baselineMetrics.errorRate * 100)}% error rate`);
  }
  
  // Create persistent test hives
  const soakTestHives = [];
  if (adminAuth.success) {
    for (let i = 1; i <= 5; i++) {
      const hiveResult = hiveHelper.createHive(adminAuth.token, {
        name: `Soak Test Hive ${i}`,
        description: `Long-running hive for soak testing - ${i}`,
        category: 'work',
        isPublic: true,
        maxMembers: 50
      });
      
      if (hiveResult.success) {
        soakTestHives.push({
          id: hiveResult.hiveId,
          name: `Soak Test Hive ${i}`
        });
      }
    }
  }
  
  console.log(`Soak test setup completed with ${soakTestHives.length} persistent hives`);
  return {
    soakTestHives,
    baselineMetrics,
    testStartTime,
    baseUrl: SERVICE_ENDPOINTS.base_url,
    wsUrl: SERVICE_ENDPOINTS.websockets.main
  };
}

export function apiStabilitySoakTest(data) {
  const currentTime = Date.now();
  const elapsedTime = currentTime - data.testStartTime;
  const timePhase = Math.floor(elapsedTime / (10 * 60 * 1000)); // 10-minute phases
  
  group('API Stability Soak Test', () => {
    const userIndex = (__VU - 1) % soakTestUsers.length;
    const testUser = soakTestUsers[userIndex];
    
    group('Long-term API Operations', () => {
      const operationStart = Date.now();
      const loginResult = authHelper.login(testUser.email, testUser.password);
      
      if (!loginResult.success) {
        longTermErrorRate.add(1);
        cumulativeErrorCount.add(1);
        systemStabilityOverTime.add(0);
        return;
      }
      
      const authToken = loginResult.token;
      systemStabilityOverTime.add(1);
      
      // Track response time trends over time
      const baselineDeviation = loginResult.responseTime - data.baselineMetrics.responseTime;
      performanceBaselineDeviation.add(baselineDeviation);
      
      // Detect response time degradation pattern
      if (timePhase > 0) {
        const expectedDegradation = timePhase * 10; // Allow 10ms degradation per 10-minute phase
        const actualDegradation = baselineDeviation;
        responseTimeDegradation.add(actualDegradation - expectedDegradation);
      }
      
      // Execute consistent API operations
      const soakOperations = [
        () => hiveHelper.listHives(authToken),
        () => presenceHelper.updatePresence(authToken, {
          status: 'online',
          activity: `soak-test-phase-${timePhase}`,
          timestamp: Date.now()
        }),
        () => analyticsHelper.getDashboard(authToken),
        () => authHelper.getProfile(authToken)
      ];
      
      let operationErrors = 0;
      let totalResponseTime = 0;
      
      soakOperations.forEach((operation, index) => {
        const opStart = Date.now();
        
        try {
          const result = operation();
          const opEnd = Date.now();
          const responseTime = opEnd - opStart;
          
          totalResponseTime += responseTime;
          
          if (result && result.success) {
            systemStabilityOverTime.add(1);
            
            // Track memory usage indicators
            if (responseTime > (data.baselineMetrics.responseTime * 2)) {
              memoryLeakIndicator.add(responseTime / data.baselineMetrics.responseTime);
            }
            
          } else {
            operationErrors++;
            longTermErrorRate.add(1);
            cumulativeErrorCount.add(1);
            systemStabilityOverTime.add(0);
          }
          
        } catch (error) {
          operationErrors++;
          longTermErrorRate.add(1);
          cumulativeErrorCount.add(1);
          systemStabilityOverTime.add(0);
          console.error(`Soak test operation error: ${error.message}`);
        }
        
        sleep(0.5); // Consistent pacing for soak test
      });
      
      // Calculate resource usage growth indicator
      const avgResponseTime = totalResponseTime / soakOperations.length;
      const growthFactor = avgResponseTime / data.baselineMetrics.responseTime;
      resourceUsageGrowth.add(growthFactor);
      
      // Log periodic status
      if ((__ITER % 100) === 0) {
        console.log(`Soak test phase ${timePhase}: ${operationErrors} errors, ${avgResponseTime}ms avg response`);
      }
    });
    
    // Simulate realistic user behavior intervals
    const phaseBasedSleep = Math.min(5 + (timePhase * 0.5), 10);
    sleep(phaseBasedSleep);
  });
}

export function webSocketLongTermTest(data) {
  const currentTime = Date.now();
  const elapsedTime = currentTime - data.testStartTime;
  
  group('WebSocket Long-term Connection Test', () => {
    const userIndex = (__VU - 1) % soakTestUsers.length;
    const testUser = soakTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      connectionStabilityRate.add(0);
      return;
    }
    
    const wsUrl = `${data.wsUrl}?token=${authResult.token}`;
    const connectionStart = Date.now();
    
    const response = ws.connect(wsUrl, {
      headers: {
        'Authorization': `Bearer ${authResult.token}`
      }
    }, (socket) => {
      let connectionAlive = true;
      let messagesSent = 0;
      let messagesReceived = 0;
      let lastHeartbeat = Date.now();
      
      socket.on('open', () => {
        connectionStabilityRate.add(1);
        const connectionTime = Date.now() - connectionStart;
        
        // Track connection establishment degradation
        const connectionDeviation = connectionTime - 500; // 500ms baseline
        performanceBaselineDeviation.add(connectionDeviation);
        
        // Send periodic messages to maintain connection and detect issues
        const messageInterval = socket.setInterval(() => {
          if (!connectionAlive) return;
          
          const messages = [
            wsHelper.createPresenceMessage({
              userId: testUser.id,
              status: 'online',
              activity: `long-term-soak-${Math.floor(elapsedTime / 60000)}min`,
              timestamp: Date.now()
            }),
            wsHelper.createHeartbeatMessage()
          ];
          
          messages.forEach(msg => {
            try {
              socket.send(JSON.stringify(msg));
              messagesSent++;
              lastHeartbeat = Date.now();
            } catch (error) {
              longTermErrorRate.add(1);
              cumulativeErrorCount.add(1);
              connectionAlive = false;
            }
          });
          
          // Detect connection health degradation
          const timeSinceLastMessage = Date.now() - lastHeartbeat;
          if (timeSinceLastMessage > 60000) { // 1 minute without activity
            memoryLeakIndicator.add(timeSinceLastMessage / 60000);
          }
          
        }, 15000); // Every 15 seconds
        
        // Periodic connection health check
        const healthCheckInterval = socket.setInterval(() => {
          if (!connectionAlive) {
            socket.clearInterval(healthCheckInterval);
            return;
          }
          
          const healthCheck = wsHelper.createHeartbeatMessage();
          const healthStart = Date.now();
          
          try {
            socket.send(JSON.stringify(healthCheck));
            
            socket.setTimeout(() => {
              const healthLatency = Date.now() - healthStart;
              if (healthLatency > 1000) {
                responseTimeDegradation.add(healthLatency / 1000);
              }
            }, 1000);
            
          } catch (error) {
            connectionStabilityRate.add(0);
            longTermErrorRate.add(1);
            connectionAlive = false;
          }
          
        }, 30000); // Every 30 seconds
        
        // Clean up intervals when connection ends
        socket.on('close', () => {
          connectionAlive = false;
          socket.clearInterval(messageInterval);
          socket.clearInterval(healthCheckInterval);
        });
      });
      
      socket.on('message', (message) => {
        try {
          const data = JSON.parse(message);
          messagesReceived++;
          
          // Track message processing performance over time
          if (data.timestamp) {
            const messageLatency = Date.now() - data.timestamp;
            const latencyDeviation = messageLatency - 50; // 50ms baseline
            performanceBaselineDeviation.add(latencyDeviation);
            
            if (messageLatency > 500) {
              responseTimeDegradation.add(messageLatency / 100);
            }
          }
          
          systemStabilityOverTime.add(1);
          
        } catch (error) {
          longTermErrorRate.add(1);
          cumulativeErrorCount.add(1);
          systemStabilityOverTime.add(0);
        }
      });
      
      socket.on('error', (error) => {
        connectionStabilityRate.add(0);
        longTermErrorRate.add(1);
        cumulativeErrorCount.add(1);
        connectionAlive = false;
        console.error(`Long-term WebSocket error: ${error.message}`);
      });
      
      socket.on('close', () => {
        const sessionDuration = Date.now() - connectionStart;
        const expectedMessages = Math.floor(sessionDuration / 15000) * 2; // Expected based on interval
        
        if (messagesReceived < expectedMessages * 0.95) {
          memoryLeakIndicator.add((expectedMessages - messagesReceived) / expectedMessages);
        }
        
        console.log(`Long-term WebSocket session ended: ${messagesSent} sent, ${messagesReceived} received over ${sessionDuration}ms`);
      });
      
      // Keep connection alive for extended period (30+ minutes)
      socket.setTimeout(() => {
        socket.close();
      }, 2400000);
    });
    
    if (!response || response.status !== 101) {
      connectionStabilityRate.add(0);
      longTermErrorRate.add(1);
    } else {
      // Hold connection for long-term test
      sleep(2400); // 40 minutes
    }
  });
}

export function databasePoolSoakTest(data) {
  const currentTime = Date.now();
  const elapsedTime = currentTime - data.testStartTime;
  
  group('Database Connection Pool Soak Test', () => {
    const userIndex = (__VU - 1) % soakTestUsers.length;
    const testUser = soakTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      longTermErrorRate.add(1);
      return;
    }
    
    const authToken = authResult.token;
    
    group('Sustained Database Operations', () => {
      const dbOperations = [
        // Read operations
        () => hiveHelper.listHives(authToken),
        () => presenceHelper.getPresenceHistory(authToken),
        () => analyticsHelper.getDashboard(authToken),
        
        // Write operations
        () => presenceHelper.updatePresence(authToken, {
          status: 'online',
          activity: `db-soak-${Math.floor(elapsedTime / 60000)}`,
          timestamp: Date.now()
        }),
        
        // Complex query operations
        () => analyticsHelper.getProductivityInsights(authToken),
        () => hiveHelper.searchHives(authToken, 'soak')
      ];
      
      // Execute sustained database load
      const opsPerCycle = 6;
      let totalDbTime = 0;
      let dbErrors = 0;
      
      for (let i = 0; i < opsPerCycle; i++) {
        const operation = dbOperations[i % dbOperations.length];
        const dbStart = Date.now();
        
        try {
          const result = operation();
          const dbEnd = Date.now();
          const dbDuration = dbEnd - dbStart;
          
          totalDbTime += dbDuration;
          
          if (result && result.success) {
            systemStabilityOverTime.add(1);
            
            // Track database performance degradation
            const expectedDbTime = 200; // 200ms baseline
            const dbDeviation = dbDuration - expectedDbTime;
            performanceBaselineDeviation.add(dbDeviation);
            
            // Detect potential connection pool exhaustion
            if (dbDuration > 5000) {
              memoryLeakIndicator.add(dbDuration / 1000);
            }
            
          } else {
            dbErrors++;
            longTermErrorRate.add(1);
            cumulativeErrorCount.add(1);
            systemStabilityOverTime.add(0);
            
            // Check for pool exhaustion errors
            if (result && result.error && 
                result.error.includes('connection')) {
              resourceUsageGrowth.add(2);
            }
          }
          
        } catch (error) {
          dbErrors++;
          longTermErrorRate.add(1);
          cumulativeErrorCount.add(1);
          systemStabilityOverTime.add(0);
          
          if (error.message.includes('pool') || 
              error.message.includes('connection')) {
            memoryLeakIndicator.add(1);
          }
        }
        
        // Consistent intervals to maintain steady database load
        sleep(2);
      }
      
      // Calculate database performance metrics
      const avgDbTime = totalDbTime / opsPerCycle;
      const dbGrowthFactor = avgDbTime / data.baselineMetrics.responseTime;
      resourceUsageGrowth.add(dbGrowthFactor);
      
      if (dbErrors > 0) {
        console.warn(`Database soak test cycle: ${dbErrors} errors out of ${opsPerCycle} operations`);
      }
    });
    
    sleep(5); // 5-second cycles for sustained load
  });
}

export function memoryLeakDetectionTest(data) {
  const currentTime = Date.now();
  const elapsedMinutes = Math.floor((currentTime - data.testStartTime) / 60000);
  
  group('Memory Leak Detection Test', () => {
    const userIndex = (__VU - 1) % soakTestUsers.length;
    const testUser = soakTestUsers[userIndex];
    
    const authResult = authHelper.login(testUser.email, testUser.password);
    if (!authResult.success) {
      longTermErrorRate.add(1);
      return;
    }
    
    const authToken = authResult.token;
    
    group('Memory-Intensive Operations', () => {
      // Operations that could potentially cause memory leaks
      const memoryTestOps = [
        // Large data operations
        () => analyticsHelper.getHistoricalData(authToken, {
          period: 'month',
          granularity: 'hour'
        }),
        
        // Repeated object creation
        () => hiveHelper.createHive(authToken, {
          name: `Memory Test Hive ${Date.now()}`,
          description: 'A'.repeat(1000), // Large description
          category: 'work',
          isPublic: false,
          maxMembers: 10
        }),
        
        // Complex search operations
        () => hiveHelper.searchHives(authToken, 'memory leak detection test long query'),
        
        // Bulk operations
        () => presenceHelper.getBulkPresence(authToken, 
          data.soakTestHives.map(h => h.id)
        )
      ];
      
      // Track response time growth as memory leak indicator
      let responseTimeSum = 0;
      let operationCount = 0;
      
      memoryTestOps.forEach((operation, index) => {
        const memOpStart = Date.now();
        
        try {
          const result = operation();
          const memOpEnd = Date.now();
          const responseTime = memOpEnd - memOpStart;
          
          responseTimeSum += responseTime;
          operationCount++;
          
          // Calculate memory leak indicators
          const expectedGrowth = elapsedMinutes * 2; // Allow 2ms growth per minute
          const actualResponseTime = responseTime;
          const baselineResponseTime = data.baselineMetrics.responseTime;
          
          const memoryIndicator = (actualResponseTime - baselineResponseTime - expectedGrowth) / baselineResponseTime;
          memoryLeakIndicator.add(Math.max(0, memoryIndicator));
          
          if (result && result.success) {
            systemStabilityOverTime.add(1);
            
            // Track severe performance degradation (potential memory leak)
            if (responseTime > (baselineResponseTime * 3)) {
              responseTimeDegradation.add(responseTime / baselineResponseTime);
            }
            
          } else {
            longTermErrorRate.add(1);
            cumulativeErrorCount.add(1);
            systemStabilityOverTime.add(0);
          }
          
        } catch (error) {
          longTermErrorRate.add(1);
          cumulativeErrorCount.add(1);
          systemStabilityOverTime.add(0);
          
          if (error.message.includes('memory') || 
              error.message.includes('heap')) {
            memoryLeakIndicator.add(2);
          }
        }
        
        sleep(3);
      });
      
      // Calculate average response time for this cycle
      if (operationCount > 0) {
        const avgResponseTime = responseTimeSum / operationCount;
        const resourceGrowth = avgResponseTime / data.baselineMetrics.responseTime;
        resourceUsageGrowth.add(resourceGrowth);
        
        // Log memory leak detection progress
        if ((__ITER % 20) === 0) {
          console.log(`Memory leak detection (${elapsedMinutes}min): ${avgResponseTime}ms avg, ${resourceGrowth}x baseline`);
        }
      }
    });
    
    // Longer sleep for memory leak detection (simulate real usage patterns)
    sleep(Math.min(10 + (elapsedMinutes * 0.1), 20));
  });
}

export function teardown(data) {
  const testDuration = Date.now() - data.testStartTime;
  const testDurationMinutes = testDuration / 60000;
  
  console.log('Soak Load Test completed');
  console.log(`Test duration: ${testDurationMinutes.toFixed(1)} minutes`);
  console.log(`System stability rate: ${(systemStabilityOverTime.rate * 100).toFixed(3)}%`);
  console.log(`Long-term error rate: ${(longTermErrorRate.rate * 100).toFixed(3)}%`);
  console.log(`Connection stability rate: ${(connectionStabilityRate.rate * 100).toFixed(3)}%`);
  console.log(`Total cumulative errors: ${cumulativeErrorCount.value}`);
  console.log(`Average resource usage growth: ${resourceUsageGrowth.value.toFixed(2)}x baseline`);
  console.log(`Average memory leak indicator: ${memoryLeakIndicator.avg.toFixed(3)}`);
  console.log(`Average response time degradation: ${responseTimeDegradation.avg.toFixed(2)}`);
  console.log(`Average performance baseline deviation: ${performanceBaselineDeviation.avg.toFixed(2)}ms`);
  
  // Soak test analysis and recommendations
  console.log('\n=== Soak Test Analysis ===');
  
  // Memory leak assessment
  if (memoryLeakIndicator.avg < 0.05) {
    console.log('✅ Memory Management: No significant memory leaks detected');
  } else if (memoryLeakIndicator.avg < 0.15) {
    console.log('⚠️ Memory Management: Minor memory growth patterns detected');
  } else {
    console.log('❌ Memory Management: Significant memory leak indicators present');
  }
  
  // System stability assessment
  if (systemStabilityOverTime.rate > 0.99) {
    console.log('✅ System Stability: Excellent long-term stability');
  } else if (systemStabilityOverTime.rate > 0.95) {
    console.log('⚠️ System Stability: Good but some stability concerns');
  } else {
    console.log('❌ System Stability: Poor long-term stability');
  }
  
  // Performance degradation assessment
  if (responseTimeDegradation.avg < 0.1) {
    console.log('✅ Performance: Minimal performance degradation over time');
  } else if (responseTimeDegradation.avg < 0.3) {
    console.log('⚠️ Performance: Moderate performance degradation detected');
  } else {
    console.log('❌ Performance: Significant performance degradation over time');
  }
  
  console.log('\n=== Recommendations ===');
  
  if (memoryLeakIndicator.avg > 0.1) {
    console.log('• Implement memory profiling to identify leak sources');
    console.log('• Review object lifecycle management and garbage collection');
  }
  
  if (responseTimeDegradation.avg > 0.2) {
    console.log('• Monitor and optimize long-running processes');
    console.log('• Implement connection pooling optimization');
  }
  
  if (resourceUsageGrowth.value > 1.5) {
    console.log('• Review resource allocation and cleanup procedures');
    console.log('• Consider implementing periodic resource cleanup tasks');
  }
  
  if (cumulativeErrorCount.value > 30) {
    console.log('• Investigate root causes of recurring errors');
    console.log('• Implement more robust error recovery mechanisms');
  }
  
  console.log('\n=== Overall Soak Test Result ===');
  
  if (systemStabilityOverTime.rate > 0.98 && 
      memoryLeakIndicator.avg < 0.1 && 
      responseTimeDegradation.avg < 0.15) {
    console.log('🎉 PASSED: System demonstrates excellent long-term stability');
  } else if (systemStabilityOverTime.rate > 0.95 && 
             memoryLeakIndicator.avg < 0.2 && 
             responseTimeDegradation.avg < 0.3) {
    console.log('⚠️ PARTIAL: System shows good stability with minor concerns');
  } else {
    console.log('❌ FAILED: System requires improvements for long-term operation');
  }
}