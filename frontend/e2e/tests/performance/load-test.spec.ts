/**
 * Load Testing Suite
 * Safe, scalable load testing with resource monitoring
 */

import {BrowserContext, Page, test} from '@playwright/test';
import * as os from 'os';
import {
  calculateResourceRequirements,
  getLoadTestConfig,
  LoadTestConfig,
  performanceThresholds,
  scenarios
} from './load-testing-config';

// Type definitions
interface StepAction {
  action: 'navigate' | 'login' | 'createHive' | 'joinHive' | 'startTimer' | 'sendMessage' | 'logout' | 'quickJoin' | 'completeTimer' | 'leave' | 'updatePresence' | 'receiveNotifications';
  url?: string;
  credentials?: string;
  name?: string;
  hiveId?: string;
  hiveType?: string;
  waitTime?: number;
  duration?: number;
  text?: string;
  count?: number;
  interval?: number;
  subscribe?: boolean;
}

interface UserMetrics {
  userId: string;
  requestCount: number;
  errorCount: number;
  responseTimes: number[];
  avgResponseTime: number;
  errorRate: number;
}

interface ResourceReport {
  maxMemoryUsage: number;
  maxCpuUsage: number;
  measurements: Array<{
    timestamp: number;
    memory: number;
    cpu: number;
  }>;
  averageMemory: number;
  averageCpu: number;
}

interface LoadTestResults {
  startTime: number;
  endTime: number;
  totalUsers: number;
  successfulUsers: number;
  failedUsers: number;
  metrics: UserMetrics[];
  resourceReport: ResourceReport;
}

// Resource monitoring utilities
class ResourceMonitor {
  private initialMemory: number;
  private maxMemoryUsage: number = 0;
  private maxCpuUsage: number = 0;
  private measurements: Array<{
    timestamp: number;
    memory: number;
    cpu: number;
  }> = [];

  constructor() {
    this.initialMemory = os.freemem();
  }

  measure(): Record<string, unknown> {
    const currentMemory = os.freemem();
    const totalMemory = os.totalmem();
    const memoryUsage = ((totalMemory - currentMemory) / totalMemory) * 100;
    const cpuUsage = os.loadavg()[0] * 100 / os.cpus().length;

    this.measurements.push({
      timestamp: Date.now(),
      memory: memoryUsage,
      cpu: cpuUsage,
    });

    this.maxMemoryUsage = Math.max(this.maxMemoryUsage, memoryUsage);
    this.maxCpuUsage = Math.max(this.maxCpuUsage, cpuUsage);

    return {memoryUsage, cpuUsage};
  }

  shouldThrottle(): boolean {
    const {memoryUsage, cpuUsage} = this.measure();
    return memoryUsage > 85 || cpuUsage > 90;
  }

  getReport(): unknown {
    return {
      maxMemoryUsage: this.maxMemoryUsage,
      maxCpuUsage: this.maxCpuUsage,
      measurements: this.measurements,
      averageMemory: this.measurements.reduce((acc, m) => acc + m.memory, 0) / this.measurements.length,
      averageCpu: this.measurements.reduce((acc, m) => acc + m.cpu, 0) / this.measurements.length,
    };
  }
}

// Virtual user simulation
class VirtualUser {
  private page: Page;
  private context: BrowserContext;
  private userId: string;
  private metrics: {
    requestCount: number;
    errorCount: number;
    responseTimes: number[];
  };

  constructor(page: Page, context: BrowserContext, userId: string) {
    this.page = page;
    this.context = context;
    this.userId = userId;
    this.metrics = {
      requestCount: 0,
      errorCount: 0,
      responseTimes: [],
    };
  }

  async executeScenario(scenarioName: string): Promise<void> {
    const scenario = scenarios[scenarioName];
    if (!scenario) {
      throw new Error(`Scenario '${scenarioName}' not found`);
    }

    for (const step of scenario.steps) {
      const startTime = Date.now();
      try {
        await this.executeStep(step);
        this.metrics.requestCount++;
        this.metrics.responseTimes.push(Date.now() - startTime);
      } catch (error) {
        this.metrics.errorCount++;
        console.error(`User ${this.userId} error in step ${step.action}:`, error);
      }
    }
  }

  getMetrics(): unknown {
    return {
      userId: this.userId,
      ...this.metrics,
      avgResponseTime: this.metrics.responseTimes.length > 0
          ? this.metrics.responseTimes.reduce((a, b) => a + b, 0) / this.metrics.responseTimes.length
          : 0,
      errorRate: this.metrics.requestCount > 0
          ? this.metrics.errorCount / this.metrics.requestCount
          : 0,
    };
  }

  private async executeStep(step: StepAction): Promise<void> {
    switch (step.action) {
      case 'navigate':
        await this.page.goto(step.url, {waitUntil: 'networkidle'});
        break;
      case 'login':
        await this.page.goto('/login');
        await this.page.fill('[data-testid="email-input"]', `user${this.userId}@test.com`);
        await this.page.fill('[data-testid="password-input"]', 'TestPassword123!');
        await this.page.click('[data-testid="login-button"]');
        await this.page.waitForURL('/dashboard');
        break;
      case 'createHive':
        await this.page.click('[data-testid="create-hive-button"]');
        await this.page.fill('[data-testid="hive-name-input"]', step.name || `Hive ${this.userId}`);
        await this.page.click('[data-testid="create-button"]');
        break;
      case 'joinHive':
        await this.page.click('[data-testid="join-hive-button"]');
        if (step.waitTime) {
          await this.page.waitForTimeout(step.waitTime);
        }
        break;
      case 'startTimer':
        await this.page.click('[data-testid="start-timer-button"]');
        await this.page.fill('[data-testid="timer-duration"]', String(step.duration));
        await this.page.click('[data-testid="timer-start"]');
        break;
      case 'sendMessage':
        if (step.count) {
          for (let i = 0; i < step.count; i++) {
            await this.page.fill('[data-testid="chat-input"]', `Message ${i} from user ${this.userId}`);
            await this.page.press('[data-testid="chat-input"]', 'Enter');
            if (step.interval) {
              await this.page.waitForTimeout(step.interval);
            }
          }
        } else {
          await this.page.fill('[data-testid="chat-input"]', step.text || `Hello from user ${this.userId}`);
          await this.page.press('[data-testid="chat-input"]', 'Enter');
        }
        break;
      case 'logout':
        await this.page.click('[data-testid="user-menu"]');
        await this.page.click('[data-testid="logout-button"]');
        break;
      default:
        console.warn(`Unknown action: ${step.action}`);
    }
  }
}

// Main load test suite
test.describe('Load Testing Suite', () => {
  test.describe.configure({mode: 'parallel'});

  const runLoadTest = async (profileName: string) => {
    const config = getLoadTestConfig(profileName);
    const requirements = calculateResourceRequirements(config);
    const monitor = new ResourceMonitor();

    console.log(`
    ╔══════════════════════════════════════════════════════════╗
    ║                    LOAD TEST STARTING                     ║
    ╠══════════════════════════════════════════════════════════╣
    ║ Profile: ${config.name.padEnd(48)}║
    ║ Users: ${String(config.users).padEnd(50)}║
    ║ Duration: ${(config.duration + 's').padEnd(47)}║
    ║ Headless: ${String(config.headless).padEnd(47)}║
    ║ Est. Memory: ${(requirements.estimatedMemory + ' MB').padEnd(43)}║
    ║ Est. CPU: ${(requirements.estimatedCPU + '%').padEnd(47)}║
    ╚══════════════════════════════════════════════════════════╝
    `);

    if (requirements.warning) {
      console.warn(`⚠️  WARNING: ${requirements.warning}`);
    }

    // Check system resources before starting
    const {memoryUsage, cpuUsage} = monitor.measure();
    if (config.safeMode && (memoryUsage > 70 || cpuUsage > 70)) {
      console.error('❌ System resources too high to start load test safely');
      console.log(`   Memory: ${memoryUsage.toFixed(1)}%, CPU: ${cpuUsage.toFixed(1)}%`);
      return;
    }

    const results: LoadTestResults = {
      startTime: Date.now(),
      endTime: 0,
      totalUsers: config.users,
      successfulUsers: 0,
      failedUsers: 0,
      metrics: [] as UserMetrics[],
      resourceReport: {
        maxMemoryUsage: 0,
        maxCpuUsage: 0,
        measurements: [],
        averageMemory: 0,
        averageCpu: 0,
      },
    };

    // Create user batches based on max concurrent browsers
    const userBatches: number[][] = [];
    for (let i = 0; i < config.users; i += config.maxConcurrentBrowsers) {
      userBatches.push(
          Array.from(
              {length: Math.min(config.maxConcurrentBrowsers, config.users - i)},
              (_, j) => i + j
          )
      );
    }

    // Monitoring interval
    const monitoringInterval = setInterval(() => {
      const {memoryUsage, cpuUsage} = monitor.measure();
      console.log(`📊 Resources - Memory: ${memoryUsage.toFixed(1)}%, CPU: ${cpuUsage.toFixed(1)}%`);

      if (config.safeMode && monitor.shouldThrottle()) {
        console.warn('⚠️  Resource throttling activated - high system usage detected');
      }
    }, 5000);

    try {
      // Execute load test in batches
      for (const [batchIndex, batch] of userBatches.entries()) {
        console.log(`\n🚀 Starting batch ${batchIndex + 1}/${userBatches.length} with ${batch.length} users`);

        // Check for throttling
        if (config.safeMode && monitor.shouldThrottle()) {
          console.warn('⏸️  Pausing for resource recovery...');
          await new Promise(resolve => setTimeout(resolve, 10000));
        }

        // Run users in parallel within batch
        const batchPromises = batch.map(async (userId) => {
          const browser = await test.playwright.chromium.launch({
            headless: config.headless
          });
          const context = await browser.newContext();
          const page = await context.newPage();
          const user = new VirtualUser(page, context, String(userId));

          try {
            await user.executeScenario('user-journey');
            results.successfulUsers++;
            results.metrics.push(user.getMetrics());
          } catch (error) {
            results.failedUsers++;
            console.error(`User ${userId} failed:`, error);
          } finally {
            await context.close();
            await browser.close();
          }
        });

        await Promise.all(batchPromises);

        // Ramp-up delay between batches
        if (batchIndex < userBatches.length - 1) {
          const delay = (config.rampUpTime * 1000) / userBatches.length;
          console.log(`⏳ Ramp-up delay: ${delay}ms`);
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    } finally {
      clearInterval(monitoringInterval);
      results.endTime = Date.now();
      results.resourceReport = monitor.getReport();
    }

    // Generate report
    generateLoadTestReport(config, results);
  };

  const generateLoadTestReport = (config: LoadTestConfig, results: LoadTestResults): void => {
    const duration = (results.endTime - results.startTime) / 1000;
    const successRate = (results.successfulUsers / results.totalUsers) * 100;

    // Calculate percentiles
    const allResponseTimes = results.metrics.flatMap((m: UserMetrics) => m.responseTimes);
    allResponseTimes.sort((a: number, b: number) => a - b);

    const getPercentile = (p: number): void => {
      const index = Math.ceil((p / 100) * allResponseTimes.length) - 1;
      return allResponseTimes[index] || 0;
    };

    const report = `
╔════════════════════════════════════════════════════════════════╗
║                     LOAD TEST REPORT                           ║
╠════════════════════════════════════════════════════════════════╣
║ Test Profile: ${config.name.padEnd(49)}║
║ Duration: ${(duration.toFixed(2) + 's').padEnd(53)}║
║ Total Users: ${String(results.totalUsers).padEnd(50)}║
║ Successful: ${String(results.successfulUsers).padEnd(51)}║
║ Failed: ${String(results.failedUsers).padEnd(55)}║
║ Success Rate: ${(successRate.toFixed(2) + '%').padEnd(49)}║
╟────────────────────────────────────────────────────────────────╢
║                        PERFORMANCE                             ║
╟────────────────────────────────────────────────────────────────╢
║ Response Times (ms):                                           ║
║   P50: ${String(getPercentile(50)).padEnd(56)}║
║   P75: ${String(getPercentile(75)).padEnd(56)}║
║   P90: ${String(getPercentile(90)).padEnd(56)}║
║   P95: ${String(getPercentile(95)).padEnd(56)}║
║   P99: ${String(getPercentile(99)).padEnd(56)}║
╟────────────────────────────────────────────────────────────────╢
║                      RESOURCE USAGE                            ║
╟────────────────────────────────────────────────────────────────╢
║ Max Memory: ${(results.resourceReport.maxMemoryUsage.toFixed(2) + '%').padEnd(50)}║
║ Avg Memory: ${(results.resourceReport.averageMemory.toFixed(2) + '%').padEnd(50)}║
║ Max CPU: ${(results.resourceReport.maxCpuUsage.toFixed(2) + '%').padEnd(53)}║
║ Avg CPU: ${(results.resourceReport.averageCpu.toFixed(2) + '%').padEnd(53)}║
╟────────────────────────────────────────────────────────────────╢
║                       THRESHOLDS                               ║
╟────────────────────────────────────────────────────────────────╢
║ P95 Target: ${(performanceThresholds.responseTime.p95 + 'ms').padEnd(50)}║
║ P95 Actual: ${(getPercentile(95) + 'ms').padEnd(50)}║
║ Status: ${(getPercentile(95) <= performanceThresholds.responseTime.p95 ? '✅ PASS' : '❌ FAIL').padEnd(55)}║
╚════════════════════════════════════════════════════════════════╝
    `;

    console.log(report);

    // Save detailed report to file
    const fs = await import('fs');
    const reportPath = `./test-results/load-test-${config.name.toLowerCase().replace(/ /g, '-')}-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify({
      config,
      results,
      report: report.trim(),
    }, null, 2));

    console.log(`\n📄 Detailed report saved to: ${reportPath}`);
  };

  // Test cases for different load profiles
  test('Local Minimal Load Test (5 users)', async () => {
    await runLoadTest('local-minimal');
  });

  test('Local Small Load Test (10 users)', async () => {
    await runLoadTest('local-small');
  });

  test.skip('Local Medium Load Test (25 users)', async () => {
    // Skipped by default - enable manually
    await runLoadTest('local-medium');
  });

  test.skip('Local Stress Test (50 users)', async () => {
    // Skipped by default - enable manually
    await runLoadTest('local-stress');
  });
});