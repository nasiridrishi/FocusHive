/**
 * Mock Backend Service for NetworkErrorFallback E2E Testing
 * Provides controlled network failure scenarios for testing
 */

const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// State management for test scenarios
let testScenario = 'normal';
let requestCount = 0;
let failureRate = 0;
let latencyMs = 0;

// Utility functions
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));
const shouldFail = () => Math.random() < failureRate;

// Test scenario endpoints
app.post('/test/scenario', (req, res) => {
  const { scenario, options = {} } = req.body;
  
  testScenario = scenario;
  requestCount = 0;
  
  switch (scenario) {
    case 'intermittent':
      failureRate = options.failureRate || 0.5;
      break;
    case 'slow':
      latencyMs = options.latencyMs || 5000;
      break;
    case 'gradual-failure':
      failureRate = 0.1; // Start with low failure rate
      break;
    case 'complete-failure':
      failureRate = 1;
      break;
    case 'normal':
    default:
      failureRate = 0;
      latencyMs = 0;
      break;
  }
  
  console.log(`Mock backend scenario set to: ${scenario}`, options);
  res.json({ scenario, options, message: 'Scenario configured' });
});

// Health check endpoint
app.get('/health', async (req, res) => {
  requestCount++;
  
  if (latencyMs > 0) {
    await delay(latencyMs);
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    console.log(`Health check failed (scenario: ${testScenario}, request: ${requestCount})`);
    return res.status(500).json({ 
      error: 'Service unavailable',
      scenario: testScenario,
      requestCount 
    });
  }
  
  // Gradual failure - increase failure rate over time
  if (testScenario === 'gradual-failure') {
    failureRate = Math.min(0.9, failureRate + 0.1);
  }
  
  res.json({ 
    status: 'healthy',
    scenario: testScenario,
    requestCount,
    timestamp: new Date().toISOString()
  });
});

// API endpoints that can fail
app.get('/api/users', async (req, res) => {
  requestCount++;
  
  if (latencyMs > 0) {
    await delay(latencyMs);
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    console.log(`API request failed (scenario: ${testScenario}, request: ${requestCount})`);
    return res.status(503).json({ 
      error: 'Network error',
      message: 'Failed to fetch users',
      scenario: testScenario
    });
  }
  
  res.json({
    users: [
      { id: 1, name: 'Test User 1', email: 'user1@test.com' },
      { id: 2, name: 'Test User 2', email: 'user2@test.com' }
    ],
    meta: {
      scenario: testScenario,
      requestCount
    }
  });
});

app.get('/api/dashboard', async (req, res) => {
  requestCount++;
  
  if (latencyMs > 0) {
    await delay(latencyMs);
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    console.log(`Dashboard API failed (scenario: ${testScenario}, request: ${requestCount})`);
    return res.status(503).json({ 
      error: 'Network error',
      message: 'Failed to load dashboard data',
      scenario: testScenario
    });
  }
  
  res.json({
    data: {
      stats: { activeUsers: 42, totalSessions: 156 },
      activities: ['User logged in', 'New session started'],
      charts: { labels: ['Mon', 'Tue', 'Wed'], data: [10, 20, 30] }
    },
    meta: {
      scenario: testScenario,
      requestCount,
      loadedAt: new Date().toISOString()
    }
  });
});

// Auth endpoints
app.post('/api/auth/login', async (req, res) => {
  if (latencyMs > 0) {
    await delay(latencyMs);
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    return res.status(503).json({ 
      error: 'Authentication service unavailable',
      scenario: testScenario
    });
  }
  
  res.json({
    token: 'mock-jwt-token',
    user: { id: 1, name: 'Test User', email: 'test@example.com' },
    scenario: testScenario
  });
});

// WebSocket simulation endpoint
app.get('/api/ws-status', async (req, res) => {
  if (latencyMs > 0) {
    await delay(latencyMs);
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    return res.status(503).json({ 
      error: 'WebSocket service unavailable',
      scenario: testScenario
    });
  }
  
  res.json({
    wsConnected: true,
    activeConnections: 25,
    scenario: testScenario
  });
});

// File upload simulation
app.post('/api/upload', async (req, res) => {
  if (latencyMs > 0) {
    await delay(Math.max(latencyMs, 2000)); // File uploads take time
  }
  
  if (testScenario === 'complete-failure' || shouldFail()) {
    return res.status(503).json({ 
      error: 'Upload service unavailable',
      scenario: testScenario
    });
  }
  
  res.json({
    uploaded: true,
    fileId: 'mock-file-id-' + Date.now(),
    scenario: testScenario
  });
});

// Test status endpoint
app.get('/test/status', (req, res) => {
  res.json({
    scenario: testScenario,
    requestCount,
    failureRate,
    latencyMs,
    uptime: process.uptime(),
    pid: process.pid
  });
});

// Test data seeding endpoint
app.post('/test/seed', (req, res) => {
  const { users, projects } = req.body;
  
  console.log('Seeding test data:', { 
    users: users?.length || 0, 
    projects: projects?.length || 0 
  });
  
  // Store in memory (in real app this would go to database)
  global.testUsers = users || [];
  global.testProjects = projects || [];
  
  res.json({ 
    message: 'Test data seeded successfully',
    data: {
      users: global.testUsers,
      projects: global.testProjects
    }
  });
});

// Reset test state
app.post('/test/reset', (req, res) => {
  testScenario = 'normal';
  requestCount = 0;
  failureRate = 0;
  latencyMs = 0;
  
  // Clear test data
  global.testUsers = [];
  global.testProjects = [];
  
  console.log('Mock backend reset to normal operation');
  res.json({ message: 'Test state reset', scenario: testScenario });
});

// Error simulation middleware
app.use((req, res, next) => {
  // Simulate random server errors for unhandled routes
  if (testScenario === 'random-errors' && Math.random() < 0.3) {
    return res.status(500).json({ 
      error: 'Random server error',
      path: req.path,
      scenario: testScenario
    });
  }
  
  next();
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    error: 'Endpoint not found',
    path: req.path,
    method: req.method,
    scenario: testScenario
  });
});

// Error handler
app.use((error, req, res, next) => {
  console.error('Mock backend error:', error);
  res.status(500).json({
    error: error.message,
    scenario: testScenario,
    stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
  });
});

// Start server
const PORT = process.env.E2E_MOCK_BACKEND_PORT || 8080;

const server = app.listen(PORT, () => {
  console.log(`Mock backend server running on http://localhost:${PORT}`);
  console.log('Available test scenarios:');
  console.log('  - normal: All requests succeed');
  console.log('  - complete-failure: All requests fail');
  console.log('  - intermittent: Random failures based on failure rate');
  console.log('  - slow: All requests delayed by latencyMs');
  console.log('  - gradual-failure: Failure rate increases over time');
  console.log('  - random-errors: Random 500 errors on any endpoint');
  console.log('');
  console.log('Control endpoints:');
  console.log('  POST /test/scenario - Set test scenario');
  console.log('  GET /test/status - Get current test state');
  console.log('  POST /test/reset - Reset to normal operation');
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('Mock backend shutting down...');
  server.close(() => {
    console.log('Mock backend stopped');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('Mock backend shutting down...');
  server.close(() => {
    console.log('Mock backend stopped');
    process.exit(0);
  });
});

module.exports = app;