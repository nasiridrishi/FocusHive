/**
 * Load Testing Configuration
 * Safe, scalable configuration for performance testing
 */

export interface LoadTestConfig {
  name: string;
  description: string;
  users: number;
  rampUpTime: number; // seconds
  duration: number; // seconds
  headless: boolean;
  maxConcurrentBrowsers: number;
  resourceMonitoring: boolean;
  safeMode: boolean;
}

export const loadTestProfiles: Record<string, LoadTestConfig> = {
  // Local testing profiles (safe for MacBook)
  'local-minimal': {
    name: 'Local Minimal',
    description: 'Minimal load test for local development',
    users: 5,
    rampUpTime: 10,
    duration: 60,
    headless: true,
    maxConcurrentBrowsers: 5,
    resourceMonitoring: true,
    safeMode: true,
  },
  'local-small': {
    name: 'Local Small',
    description: 'Small load test safe for local machines',
    users: 10,
    rampUpTime: 20,
    duration: 120,
    headless: true,
    maxConcurrentBrowsers: 10,
    resourceMonitoring: true,
    safeMode: true,
  },
  'local-medium': {
    name: 'Local Medium',
    description: 'Medium load test with resource monitoring',
    users: 25,
    rampUpTime: 30,
    duration: 180,
    headless: true,
    maxConcurrentBrowsers: 15,
    resourceMonitoring: true,
    safeMode: true,
  },
  'local-stress': {
    name: 'Local Stress',
    description: 'Stress test for finding local limits',
    users: 50,
    rampUpTime: 60,
    duration: 300,
    headless: true,
    maxConcurrentBrowsers: 20,
    resourceMonitoring: true,
    safeMode: true,
  },

  // Cloud/CI profiles (for future implementation)
  'cloud-standard': {
    name: 'Cloud Standard',
    description: 'Standard load test for CI/CD',
    users: 100,
    rampUpTime: 120,
    duration: 600,
    headless: true,
    maxConcurrentBrowsers: 50,
    resourceMonitoring: true,
    safeMode: false,
  },
  'cloud-peak': {
    name: 'Cloud Peak',
    description: 'Peak load simulation',
    users: 500,
    rampUpTime: 300,
    duration: 900,
    headless: true,
    maxConcurrentBrowsers: 100,
    resourceMonitoring: true,
    safeMode: false,
  },
  'cloud-endurance': {
    name: 'Cloud Endurance',
    description: 'Long-running endurance test',
    users: 200,
    rampUpTime: 240,
    duration: 3600, // 1 hour
    headless: true,
    maxConcurrentBrowsers: 75,
    resourceMonitoring: true,
    safeMode: false,
  },
  'cloud-spike': {
    name: 'Cloud Spike',
    description: 'Sudden traffic spike simulation',
    users: 300,
    rampUpTime: 30, // Fast ramp-up
    duration: 600,
    headless: true,
    maxConcurrentBrowsers: 100,
    resourceMonitoring: true,
    safeMode: false,
  },
};

export const performanceThresholds = {
  responseTime: {
    p50: 200,  // 50th percentile
    p75: 400,  // 75th percentile
    p90: 800,  // 90th percentile
    p95: 1200, // 95th percentile
    p99: 2000, // 99th percentile
  },
  errorRate: 0.01, // 1% max error rate
  throughput: {
    min: 100, // requests per second
    target: 500,
  },
  resources: {
    cpu: 80, // max CPU usage %
    memory: 85, // max memory usage %
  },
};

export const scenarios = {
  'user-journey': {
    name: 'Complete User Journey',
    steps: [
      { action: 'navigate', url: '/' },
      { action: 'navigate', url: '/login' },
      { action: 'login', credentials: 'test-user' },
      { action: 'navigate', url: '/dashboard' },
      { action: 'createHive', name: 'Test Hive' },
      { action: 'joinHive', waitTime: 5000 },
      { action: 'startTimer', duration: 25 },
      { action: 'sendMessage', text: 'Hello team!' },
      { action: 'navigate', url: '/analytics' },
      { action: 'logout' },
    ],
  },
  'quick-session': {
    name: 'Quick Focus Session',
    steps: [
      { action: 'navigate', url: '/' },
      { action: 'quickJoin', hiveType: 'public' },
      { action: 'startTimer', duration: 15 },
      { action: 'completeTimer' },
      { action: 'leave' },
    ],
  },
  'heavy-interaction': {
    name: 'Heavy Real-time Interaction',
    steps: [
      { action: 'login', credentials: 'test-user' },
      { action: 'joinHive', hiveId: 'busy-hive' },
      { action: 'startTimer', duration: 25 },
      { action: 'sendMessage', count: 10, interval: 2000 },
      { action: 'updatePresence', interval: 5000 },
      { action: 'receiveNotifications', subscribe: true },
    ],
  },
};

export const getLoadTestConfig = (profile: string = 'local-minimal'): LoadTestConfig => {
  const config = loadTestProfiles[profile];
  if (!config) {
    console.warn(`Profile '${profile}' not found, using 'local-minimal'`);
    return loadTestProfiles['local-minimal'];
  }
  return config;
};

export const calculateResourceRequirements = (config: LoadTestConfig) => {
  const memoryPerBrowser = config.headless ? 50 : 150; // MB
  const cpuPerBrowser = config.headless ? 5 : 15; // % CPU
  
  return {
    estimatedMemory: config.maxConcurrentBrowsers * memoryPerBrowser,
    estimatedCPU: Math.min(100, config.maxConcurrentBrowsers * cpuPerBrowser),
    recommendedRAM: Math.ceil((config.maxConcurrentBrowsers * memoryPerBrowser) / 1000) + 2, // GB
    warning: config.users > 50 && config.safeMode 
      ? 'Consider using cloud infrastructure for this load level' 
      : null,
  };
};