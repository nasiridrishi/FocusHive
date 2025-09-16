/**
 * Global Teardown for NetworkErrorFallback E2E Testing
 * Cleans up services, temporary files, and test environment
 */

import { FullConfig } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { spawn } from 'child_process';

interface GlobalSetupContext {
  mockBackendPid?: number;
  websocketServerPid?: number;
  cdnMockPid?: number;
  serviceWorkerRegistered: boolean;
  testDataSeeded: boolean;
}

const contextFile = path.join(__dirname, '.test-context.json');

/**
 * Safely kill a process by PID
 */
function killProcess(pid: number, name: string): void {
  try {
    console.log(`🔄 Stopping ${name} (PID: ${pid})...`);
    
    // Try graceful shutdown first
    process.kill(pid, 'SIGTERM');
    
    // Wait a bit then force kill if still running
    setTimeout(() => {
      try {
        // Check if still running
        process.kill(pid, 0);
        console.log(`⚡ Force stopping ${name} (PID: ${pid})`);
        process.kill(pid, 'SIGKILL');
      } catch {
        // Process already dead, which is what we want
      }
    }, 2000);
    
    console.log(`✅ ${name} stopped`);
  } catch {
    console.log(`⚠️  ${name} (PID: ${pid}) was already stopped or not found`);
  }
}

/**
 * Clean up temporary files and directories
 */
function cleanupTemporaryFiles(): void {
  console.log('🧹 Cleaning up temporary files...');
  
  const tempFiles = [
    contextFile,
    path.join(__dirname, '.test-context.json'),
    path.join(__dirname, 'ws-mock-server.js'),
    path.join(__dirname, 'cdn-mock-server.js'),
    path.join(__dirname, 'test-data.json'),
    path.join(__dirname, 'performance-traces'),
    path.join(__dirname, 'screenshots'),
    path.join(__dirname, 'videos')
  ];
  
  tempFiles.forEach(filePath => {
    try {
      if (fs.existsSync(filePath)) {
        const stat = fs.statSync(filePath);
        if (stat.isDirectory()) {
          fs.rmSync(filePath, { recursive: true, force: true });
          console.log(`🗂️  Removed directory: ${path.basename(filePath)}`);
        } else {
          fs.unlinkSync(filePath);
          console.log(`📄 Removed file: ${path.basename(filePath)}`);
        }
      }
    } catch (error) {
      console.warn(`⚠️  Could not remove ${filePath}:`, error);
    }
  });
}

/**
 * Clean up service worker registration
 */
async function cleanupServiceWorker(): Promise<void> {
  console.log('🔧 Cleaning up service worker...');
  
  try {
    const { chromium } = await import('@playwright/test');
    const browser = await chromium.launch();
    const page = await browser.newPage();
    
    await page.goto(process.env.E2E_BASE_URL || 'http://localhost:5173');
    
    // Unregister all service workers
    await page.evaluate(async () => {
      if ('serviceWorker' in navigator) {
        const registrations = await navigator.serviceWorker.getRegistrations();
        for (const registration of registrations) {
          await registration.unregister();
        }
      }
    });
    
    // Clear all caches
    await page.evaluate(async () => {
      if ('caches' in window) {
        const cacheNames = await caches.keys();
        await Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        );
      }
    });
    
    await browser.close();
    console.log('✅ Service worker cleanup completed');
  } catch (error) {
    console.warn('⚠️  Service worker cleanup failed:', error);
  }
}

/**
 * Reset environment variables
 */
function resetEnvironment(): void {
  console.log('🌍 Resetting environment variables...');
  
  const testEnvVars = [
    'E2E_MOCK_BACKEND_URL',
    'E2E_WS_SERVER_URL',
    'E2E_CDN_MOCK_URL',
    'E2E_TEST_MODE',
    'E2E_NETWORK_ERROR_TESTING'
  ];
  
  testEnvVars.forEach(varName => {
    if (process.env[varName]) {
      delete process.env[varName];
      console.log(`🗑️  Removed ${varName}`);
    }
  });
}

/**
 * Generate teardown report
 */
function generateTeardownReport(context: GlobalSetupContext): void {
  console.log('📊 Teardown Summary');
  console.log('='.repeat(50));
  console.log(`🖥️  Mock Backend: ${context.mockBackendPid ? 'Stopped' : 'Not running'}`);
  console.log(`🔌 WebSocket Server: ${context.websocketServerPid ? 'Stopped' : 'Not running'}`);
  console.log(`📦 CDN Mock: ${context.cdnMockPid ? 'Stopped' : 'Not running'}`);
  console.log(`🔧 Service Worker: ${context.serviceWorkerRegistered ? 'Cleaned up' : 'Not registered'}`);
  console.log(`🌱 Test Data: ${context.testDataSeeded ? 'Cleaned up' : 'Not seeded'}`);
}

/**
 * Emergency cleanup - kills processes by port if context file is missing
 */
function emergencyCleanup(): void {
  console.log('🚨 Performing emergency cleanup (no context file found)...');
  
  const commonPorts = [8080, 8081, 8082];
  
  commonPorts.forEach(port => {
    try {
      // Find process using port
      const lsofProcess = spawn('lsof', ['-ti', `:${port}`], { stdio: 'pipe' });
      
      lsofProcess.stdout.on('data', (data) => {
        const pid = parseInt(data.toString().trim());
        if (pid) {
          killProcess(pid, `Process on port ${port}`);
        }
      });
      
      lsofProcess.on('error', () => {
        // lsof not available or no process on port
      });
    } catch {
      // Ignore errors in emergency cleanup
    }
  });
}

/**
 * Main global teardown function
 */
async function globalTeardown(_config: FullConfig): Promise<void> {
  console.log('🌍 Starting global teardown for NetworkErrorFallback E2E tests');
  console.log('='.repeat(60));

  const startTime = Date.now();
  let context: GlobalSetupContext = {
    serviceWorkerRegistered: false,
    testDataSeeded: false
  };

  try {
    // Try to load context from setup
    if (fs.existsSync(contextFile)) {
      const contextData = fs.readFileSync(contextFile, 'utf8');
      context = JSON.parse(contextData);
    } else {
      console.warn('⚠️  No context file found, performing emergency cleanup...');
      emergencyCleanup();
    }

    // Stop all services
    if (context.mockBackendPid) {
      killProcess(context.mockBackendPid, 'Mock Backend');
    }

    if (context.websocketServerPid) {
      killProcess(context.websocketServerPid, 'WebSocket Server');
    }

    if (context.cdnMockPid) {
      killProcess(context.cdnMockPid, 'CDN Mock Server');
    }

    // Clean up service worker if it was registered
    if (context.serviceWorkerRegistered) {
      await cleanupServiceWorker();
    }

    // Clean up temporary files
    cleanupTemporaryFiles();

    // Reset environment variables
    resetEnvironment();

    // Wait for processes to fully stop
    await new Promise(resolve => setTimeout(resolve, 3000));

    const teardownTime = Date.now() - startTime;
    console.log('='.repeat(60));
    console.log(`✅ Global teardown completed in ${teardownTime}ms`);
    
    generateTeardownReport(context);

  } catch (error) {
    console.error('❌ Global teardown encountered errors:', error);
    
    // Still try emergency cleanup
    emergencyCleanup();
    
    // Don't throw error to avoid breaking the test run
    console.log('⚠️  Continuing despite teardown errors...');
  }
}

export default globalTeardown;