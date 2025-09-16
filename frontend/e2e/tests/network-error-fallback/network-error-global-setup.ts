/**
 * Global Setup for NetworkErrorFallback E2E Testing
 * Initializes services, mock servers, and test environment
 */

import { chromium, FullConfig } from '@playwright/test';
import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs';

interface GlobalSetupContext {
  mockBackendPid?: number;
  websocketServerPid?: number;
  cdnMockPid?: number;
  serviceWorkerRegistered: boolean;
  testDataSeeded: boolean;
}

const MOCK_BACKEND_PORT = process.env.E2E_MOCK_BACKEND_PORT || '8080';
const WS_SERVER_PORT = process.env.E2E_WS_SERVER_PORT || '8081';
const CDN_MOCK_PORT = process.env.E2E_CDN_MOCK_PORT || '8082';
const _SETUP_TIMEOUT = 120000; // 2 minutes

// Store global context
const contextFile = path.join(__dirname, '.test-context.json');

/**
 * Wait for service to be ready
 */
async function waitForService(url: string, timeout: number = 30000): Promise<void> {
  const startTime = Date.now();
  while (Date.now() - startTime < timeout) {
    try {
      const response = await fetch(url);
      if (response.ok) {
        console.log(`✅ Service ready at ${url}`);
        return;
      }
    } catch {
      // Service not ready yet
    }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  throw new Error(`Timeout waiting for service at ${url}`);
}

/**
 * Start mock backend service
 */
async function startMockBackend(): Promise<number> {
  console.log('🚀 Starting mock backend service...');
  
  const mockBackendScript = path.join(__dirname, 'mock-backend.js');
  const process = spawn('node', [mockBackendScript], {
    env: {
      ...process.env,
      E2E_MOCK_BACKEND_PORT: MOCK_BACKEND_PORT,
      NODE_ENV: 'test'
    },
    stdio: 'pipe',
    detached: false
  });

  // Wait for service to be ready
  await waitForService(`http://localhost:${MOCK_BACKEND_PORT}/health`);
  
  console.log(`✅ Mock backend started (PID: ${process.pid})`);
  return process.pid!;
}

/**
 * Start WebSocket mock server
 */
async function startWebSocketServer(): Promise<number> {
  console.log('🚀 Starting WebSocket mock server...');
  
  const wsServerScript = path.join(__dirname, 'ws-mock-server.js');
  
  // Create WebSocket mock server if it doesn't exist
  if (!fs.existsSync(wsServerScript)) {
    const wsServerCode = `
const WebSocket = require('ws');
const express = require('express');

const PORT = process.env.E2E_WS_SERVER_PORT || 8081;
const wss = new WebSocket.Server({ port: PORT });

let connectionFailures = false;
let messageFailures = false;

wss.on('connection', function connection(ws) {
  if (connectionFailures) {
    ws.close(1011, 'Server error');
    return;
  }

  console.log('WebSocket client connected');

  ws.on('message', function incoming(message) {
    if (messageFailures) {
      ws.close(1011, 'Message handling failed');
      return;
    }

    // Echo message back
    ws.send(message);
  });

  ws.on('close', function close() {
    console.log('WebSocket client disconnected');
  });

  // Send periodic heartbeat
  const heartbeat = setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.ping();
    } else {
      clearInterval(heartbeat);
    }
  }, 30000);
});

// Control API
const app = express();
app.use(express.json());

app.post('/test/ws/fail-connections', (req, res) => {
  connectionFailures = req.body.enabled;
  res.json({ connectionFailures });
});

app.post('/test/ws/fail-messages', (req, res) => {
  messageFailures = req.body.enabled;
  res.json({ messageFailures });
});

app.get('/test/ws/status', (req, res) => {
  res.json({ 
    connectionFailures, 
    messageFailures,
    connectedClients: wss.clients.size 
  });
});

app.listen(parseInt(PORT) + 1, () => {
  console.log(\`WebSocket control API running on port \${parseInt(PORT) + 1}\`);
});

console.log(\`WebSocket server running on port \${PORT}\`);
`;
    fs.writeFileSync(wsServerScript, wsServerCode);
  }

  const process = spawn('node', [wsServerScript], {
    env: {
      ...process.env,
      E2E_WS_SERVER_PORT: WS_SERVER_PORT
    },
    stdio: 'pipe'
  });

  // Wait for WebSocket server to be ready
  await new Promise(resolve => setTimeout(resolve, 2000));
  
  console.log(`✅ WebSocket server started (PID: ${process.pid})`);
  return process.pid!;
}

/**
 * Start CDN mock server for static assets
 */
async function startCDNMockServer(): Promise<number> {
  console.log('🚀 Starting CDN mock server...');
  
  const cdnServerScript = path.join(__dirname, 'cdn-mock-server.js');
  
  if (!fs.existsSync(cdnServerScript)) {
    const cdnServerCode = `
const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.E2E_CDN_MOCK_PORT || 8082;

let simulateOutage = false;

// Control API
app.post('/test/cdn/outage', (req, res) => {
  simulateOutage = req.body.enabled;
  res.json({ outage: simulateOutage });
});

app.get('/test/cdn/status', (req, res) => {
  res.json({ outage: simulateOutage });
});

// Mock static assets
app.get('/assets/*', (req, res) => {
  if (simulateOutage) {
    return res.status(503).json({ error: 'CDN unavailable' });
  }
  
  // Serve mock content
  const assetPath = req.path;
  if (assetPath.endsWith('.js')) {
    res.setHeader('Content-Type', 'application/javascript');
    res.send('console.log("Mock CDN asset");');
  } else if (assetPath.endsWith('.css')) {
    res.setHeader('Content-Type', 'text/css');
    res.send('/* Mock CDN stylesheet */');
  } else {
    res.json({ mockAsset: assetPath });
  }
});

app.listen(PORT, () => {
  console.log(\`CDN mock server running on port \${PORT}\`);
});
`;
    fs.writeFileSync(cdnServerScript, cdnServerCode);
  }

  const process = spawn('node', [cdnServerScript], {
    env: {
      ...process.env,
      E2E_CDN_MOCK_PORT: CDN_MOCK_PORT
    },
    stdio: 'pipe'
  });

  await waitForService(`http://localhost:${CDN_MOCK_PORT}/test/cdn/status`);
  
  console.log(`✅ CDN mock server started (PID: ${process.pid})`);
  return process.pid!;
}

/**
 * Register service worker for PWA tests
 */
async function registerServiceWorker(): Promise<boolean> {
  console.log('🔧 Registering service worker for PWA tests...');
  
  try {
    const browser = await chromium.launch();
    const page = await browser.newPage();
    
    // Navigate to app
    await page.goto(process.env.E2E_BASE_URL || 'http://localhost:5173');
    
    // Register service worker
    await page.evaluate(() => {
      if ('serviceWorker' in navigator) {
        return navigator.serviceWorker.register('/sw.js', { scope: '/' });
      }
    });
    
    await browser.close();
    console.log('✅ Service worker registration completed');
    return true;
  } catch (error) {
    console.warn('⚠️  Service worker registration failed:', error);
    return false;
  }
}

/**
 * Seed test data
 */
async function seedTestData(): Promise<boolean> {
  console.log('🌱 Seeding test data...');
  
  try {
    const response = await fetch(`http://localhost:${MOCK_BACKEND_PORT}/test/seed`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        users: [
          { id: 'test-user-1', name: 'E2E Test User 1', email: 'e2e-user1@test.com' },
          { id: 'test-user-2', name: 'E2E Test User 2', email: 'e2e-user2@test.com' }
        ],
        projects: [
          { id: 'test-project-1', name: 'E2E Test Project', description: 'For testing' }
        ]
      })
    });

    if (response.ok) {
      console.log('✅ Test data seeded successfully');
      return true;
    }
  } catch (error) {
    console.warn('⚠️  Test data seeding failed:', error);
  }
  
  return false;
}

/**
 * Main global setup function
 */
async function globalSetup(_config: FullConfig): Promise<void> {
  console.log('🌍 Starting global setup for NetworkErrorFallback E2E tests');
  console.log('='.repeat(60));

  const startTime = Date.now();
  const context: GlobalSetupContext = {
    serviceWorkerRegistered: false,
    testDataSeeded: false
  };

  try {
    // Start all services in parallel
    const [mockBackendPid, websocketServerPid, cdnMockPid] = await Promise.all([
      startMockBackend(),
      startWebSocketServer(),
      startCDNMockServer()
    ]);

    context.mockBackendPid = mockBackendPid;
    context.websocketServerPid = websocketServerPid;
    context.cdnMockPid = cdnMockPid;

    // Set environment variables for tests
    process.env.E2E_MOCK_BACKEND_URL = `http://localhost:${MOCK_BACKEND_PORT}`;
    process.env.E2E_WS_SERVER_URL = `ws://localhost:${WS_SERVER_PORT}`;
    process.env.E2E_CDN_MOCK_URL = `http://localhost:${CDN_MOCK_PORT}`;

    // Additional setup tasks
    const [serviceWorkerRegistered, testDataSeeded] = await Promise.all([
      registerServiceWorker(),
      seedTestData()
    ]);

    context.serviceWorkerRegistered = serviceWorkerRegistered;
    context.testDataSeeded = testDataSeeded;

    // Save context for teardown
    fs.writeFileSync(contextFile, JSON.stringify(context, null, 2));

    const setupTime = Date.now() - startTime;
    console.log('='.repeat(60));
    console.log(`✅ Global setup completed in ${setupTime}ms`);
    console.log(`📊 Services started: ${Object.keys(context).filter(k => k.endsWith('Pid')).length}`);
    console.log(`🔧 Service Worker: ${context.serviceWorkerRegistered ? 'Registered' : 'Skipped'}`);
    console.log(`🌱 Test Data: ${context.testDataSeeded ? 'Seeded' : 'Skipped'}`);

  } catch (error) {
    console.error('❌ Global setup failed:', error);
    
    // Cleanup any started services
    if (context.mockBackendPid) {
      try { process.kill(context.mockBackendPid); } catch { /* ignore */ }
    }
    if (context.websocketServerPid) {
      try { process.kill(context.websocketServerPid); } catch { /* ignore */ }
    }
    if (context.cdnMockPid) {
      try { process.kill(context.cdnMockPid); } catch { /* ignore */ }
    }
    
    throw error;
  }
}

export default globalSetup;