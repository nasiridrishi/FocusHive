/**
 * Enhanced Network Error Fallback E2E Tests
 * Advanced scenarios using the NetworkSimulator utility
 */

import { test, expect } from '@playwright/test';
import { createNetworkSimulator } from './utils/networkSim';
import { NetworkTestHelper } from './test-helpers';

test.describe('Enhanced Network Error Scenarios', () => {
  let networkSim: ReturnType<typeof createNetworkSimulator>;
  let testHelper: NetworkTestHelper;

  test.beforeEach(async ({ page }) => {
    networkSim = createNetworkSimulator(page);
    testHelper = new NetworkTestHelper(page);
    
    // Start with clean state
    await testHelper.resetMockBackend();
    await page.goto('/dashboard');
  });

  test.afterEach(async () => {
    await networkSim.cleanup();
  });

  test.describe('Progressive Web App Offline Support', () => {
    test('should work offline after PWA installation', async ({ page }) => {
      // Verify PWA is installable
      await page.waitForLoadState('networkidle');
      
      // Simulate PWA installation (service worker registration)
      await page.evaluate(async () => {
        if ('serviceWorker' in navigator) {
          await navigator.serviceWorker.register('/sw.js');
        }
      });

      // Go offline
      await networkSim.goOffline();
      
      // Reload page
      await page.reload();
      
      // Should show offline fallback UI
      await expect(page.getByTestId('network-error-fallback')).toBeVisible();
      await expect(page.getByText('You are currently offline')).toBeVisible();
      
      // Check that critical app functions still work
      await expect(page.getByRole('navigation')).toBeVisible();
      await expect(page.getByText('Cached Content Available')).toBeVisible();
    });

    test('should sync data when coming back online', async ({ page }) => {
      // Go offline and make changes
      await networkSim.goOffline();
      await page.reload();
      
      // Simulate user making changes while offline
      await page.evaluate(() => {
        localStorage.setItem('pendingChanges', JSON.stringify([
          { id: '1', action: 'update', data: { title: 'Updated while offline' } }
        ]));
      });
      
      // Come back online
      await networkSim.goOnline();
      
      // Should sync pending changes
      await expect(page.getByText('Syncing pending changes...')).toBeVisible();
      
      // Wait for sync to complete
      await page.waitForFunction(() => {
        const pendingChanges = localStorage.getItem('pendingChanges');
        return !pendingChanges || JSON.parse(pendingChanges).length === 0;
      });
      
      await expect(page.getByText('All changes synced')).toBeVisible();
    });
  });

  test.describe('WebSocket Connection Handling', () => {
    test('should handle WebSocket connection failures gracefully', async ({ page }) => {
      // Break WebSocket connections
      await networkSim.breakWebSocket({ failConnections: true });
      
      // Navigate to a page that uses WebSockets
      await page.goto('/real-time-dashboard');
      
      // Should show WebSocket error fallback
      await expect(page.getByTestId('websocket-error')).toBeVisible();
      await expect(page.getByText('Real-time connection lost')).toBeVisible();
      
      // Should show retry option
      const retryButton = page.getByRole('button', { name: 'Reconnect' });
      await expect(retryButton).toBeVisible();
      
      // Fix WebSocket and retry
      await networkSim.cleanup();
      await retryButton.click();
      
      // Should restore real-time functionality
      await expect(page.getByText('Connected')).toBeVisible();
      await expect(page.getByTestId('websocket-error')).not.toBeVisible();
    });

    test('should handle WebSocket message failures', async ({ page }) => {
      await page.goto('/real-time-dashboard');
      
      // Wait for initial connection
      await expect(page.getByText('Connected')).toBeVisible();
      
      // Break message sending
      await networkSim.breakWebSocket({ failMessages: true });
      
      // Try to send a message
      await page.getByTestId('message-input').fill('Test message');
      await page.getByRole('button', { name: 'Send' }).click();
      
      // Should show message failure
      await expect(page.getByText('Failed to send message')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Retry Message' })).toBeVisible();
    });

    test('should auto-reconnect after WebSocket disconnection', async ({ page }) => {
      await page.goto('/real-time-dashboard');
      await expect(page.getByText('Connected')).toBeVisible();
      
      // Simulate disconnection after 3 seconds
      await networkSim.breakWebSocket({ closeAfter: 3000 });
      
      // Should detect disconnection
      await expect(page.getByText('Reconnecting...')).toBeVisible();
      
      // Should automatically reconnect
      await expect(page.getByText('Connected')).toBeVisible({ timeout: 10000 });
    });
  });

  test.describe('File Upload During Network Issues', () => {
    test('should retry failed file uploads', async ({ page }) => {
      await page.goto('/file-upload');
      
      // Set up intermittent failures
      await networkSim.addFailurePattern('**/api/upload', 0.7);
      
      // Upload a file
      await page.setInputFiles('input[type="file"]', {
        name: 'test.txt',
        mimeType: 'text/plain',
        buffer: Buffer.from('Test file content')
      });
      
      await page.getByRole('button', { name: 'Upload' }).click();
      
      // Should show retry attempts
      await expect(page.getByText(/Upload failed, retrying.../)).toBeVisible();
      
      // Eventually should succeed or show final error
      await expect(
        page.getByText('Upload successful').or(page.getByText('Upload failed after maximum retries'))
      ).toBeVisible({ timeout: 30000 });
    });

    test('should show upload progress during slow network', async ({ page }) => {
      await page.goto('/file-upload');
      
      // Apply slow network conditions
      await networkSim.throttle('SLOW_3G');
      
      // Upload a larger file
      await page.setInputFiles('input[type="file"]', {
        name: 'large-file.txt',
        mimeType: 'text/plain',
        buffer: Buffer.alloc(1024 * 100) // 100KB
      });
      
      await page.getByRole('button', { name: 'Upload' }).click();
      
      // Should show progress indicator
      await expect(page.getByTestId('upload-progress')).toBeVisible();
      await expect(page.getByText(/Uploading.../)).toBeVisible();
      
      // Should show percentage or speed info
      await expect(page.locator('[data-testid="upload-progress"] [role="progressbar"]')).toBeVisible();
    });
  });

  test.describe('Long-Running Recovery Scenarios', () => {
    test('should handle 5-minute disconnection gracefully', async ({ page }) => {
      const _disconnectionDuration = 5 * 60 * 1000; // 5 minutes in ms (shortened for testing)
      const testDuration = 10000; // 10 seconds for test
      
      console.log('Starting long disconnection test...');
      
      // Start normal operation
      await expect(page.getByText('Online')).toBeVisible();
      
      // Simulate long disconnection
      await networkSim.longDisconnect(testDuration);
      
      // Should show appropriate messaging for long disconnection
      await expect(page.getByText('Extended offline period detected')).toBeVisible();
      await expect(page.getByText('Your changes will sync when connection is restored')).toBeVisible();
      
      // Should recover when back online
      await expect(page.getByText('Connection restored')).toBeVisible({ timeout: 15000 });
      await expect(page.getByText('Resuming normal operation')).toBeVisible();
    });

    test('should handle intermittent connectivity patterns', async ({ page }) => {
      // Simulate unstable connection (online/offline cycles)
      await networkSim.simulateIntermittentConnectivity({
        onlineDuration: 2000,
        offlineDuration: 1000,
        cycles: 3
      });
      
      // Should adapt to unstable connection
      await expect(page.getByText('Unstable connection detected')).toBeVisible();
      await expect(page.getByText('Optimizing for intermittent connectivity')).toBeVisible();
      
      // Should eventually stabilize
      await expect(page.getByText('Connection stable')).toBeVisible({ timeout: 15000 });
    });
  });

  test.describe('Storage Persistence During Network Issues', () => {
    test('should maintain localStorage during offline periods', async ({ page }) => {
      // Set some data in localStorage
      await page.evaluate(() => {
        localStorage.setItem('userPreferences', JSON.stringify({ theme: 'dark', language: 'en' }));
        localStorage.setItem('draftContent', 'Important draft content');
      });
      
      // Go offline
      await networkSim.goOffline();
      
      // Reload page (simulates app restart while offline)
      await page.reload();
      
      // Verify localStorage persists
      const preferences = await page.evaluate(() => {
        return JSON.parse(localStorage.getItem('userPreferences') || '{}');
      });
      
      const draftContent = await page.evaluate(() => {
        return localStorage.getItem('draftContent');
      });
      
      expect(preferences.theme).toBe('dark');
      expect(draftContent).toBe('Important draft content');
    });

    test('should handle IndexedDB operations during network issues', async ({ page }) => {
      // Store data in IndexedDB
      await page.evaluate(async () => {
        const request = indexedDB.open('testDB', 1);
        
        return new Promise((resolve) => {
          request.onupgradeneeded = () => {
            const db = request.result;
            const store = db.createObjectStore('data', { keyPath: 'id' });
            store.add({ id: 1, content: 'Test data' });
          };
          
          request.onsuccess = () => {
            resolve(true);
          };
        });
      });
      
      // Go offline
      await networkSim.goOffline();
      
      // Verify IndexedDB still works offline
      const offlineData = await page.evaluate(async () => {
        const request = indexedDB.open('testDB', 1);
        
        return new Promise((resolve) => {
          request.onsuccess = () => {
            const db = request.result;
            const transaction = db.transaction(['data'], 'readonly');
            const store = transaction.objectStore('data');
            const getRequest = store.get(1);
            
            getRequest.onsuccess = () => {
              resolve(getRequest.result);
            };
          };
        });
      });
      
      expect(offlineData).toMatchObject({ id: 1, content: 'Test data' });
    });
  });

  test.describe('Cross-Tab Synchronization', () => {
    test('should sync network status across browser tabs', async ({ context, page }) => {
      // Open second tab
      const secondTab = await context.newPage();
      await secondTab.goto('/dashboard');
      
      // Both tabs should show online initially
      await expect(page.getByTestId('network-status')).toContainText('Online');
      await expect(secondTab.getByTestId('network-status')).toContainText('Online');
      
      // Go offline in first tab
      await networkSim.goOffline();
      
      // Both tabs should show offline status
      await expect(page.getByTestId('network-status')).toContainText('Offline');
      await expect(secondTab.getByTestId('network-status')).toContainText('Offline', { timeout: 5000 });
      
      // Go online in first tab
      await networkSim.goOnline();
      
      // Both tabs should show online status
      await expect(page.getByTestId('network-status')).toContainText('Online');
      await expect(secondTab.getByTestId('network-status')).toContainText('Online', { timeout: 5000 });
    });

    test('should sync pending changes across tabs', async ({ context, page }) => {
      const secondTab = await context.newPage();
      await secondTab.goto('/dashboard');
      
      // Go offline
      await networkSim.goOffline();
      
      // Make changes in first tab
      await page.evaluate(() => {
        localStorage.setItem('pendingChanges', JSON.stringify([
          { id: '1', action: 'create', data: { title: 'New item from tab 1' } }
        ]));
        
        // Dispatch storage event
        window.dispatchEvent(new StorageEvent('storage', {
          key: 'pendingChanges',
          newValue: localStorage.getItem('pendingChanges'),
          storageArea: localStorage
        }));
      });
      
      // Second tab should show pending changes
      await expect(secondTab.getByText('1 pending changes')).toBeVisible();
    });
  });

  test.describe('Mobile Network Transitions', () => {
    test('should handle WiFi to 4G transition', async ({ page }) => {
      // Start with fast connection (WiFi)
      await page.reload();
      await expect(page.getByText('Connected')).toBeVisible();
      
      // Simulate switching to slower mobile network
      await networkSim.throttle('FAST_3G');
      
      // Should adapt UI for slower connection
      await expect(page.getByText('Slow connection detected')).toBeVisible();
      await expect(page.getByText('Optimizing experience')).toBeVisible();
      
      // Should show lower quality images or reduce auto-refresh
      await expect(page.locator('[data-testid="optimized-content"]')).toBeVisible();
    });

    test('should show appropriate messaging for different mobile speeds', async ({ page }) => {
      // Test different mobile connection speeds
      const speeds = ['FAST_3G', 'SLOW_3G', 'SLOW_2G', 'EDGE'];
      
      for (const speed of speeds) {
        await networkSim.throttle(speed);
        await page.reload({ waitUntil: 'domcontentloaded' });
        
        // Should show speed-appropriate messaging
        if (speed === 'SLOW_2G' || speed === 'EDGE') {
          await expect(page.getByText('Very slow connection')).toBeVisible();
          await expect(page.getByText('Consider switching to WiFi')).toBeVisible();
        } else if (speed === 'SLOW_3G') {
          await expect(page.getByText('Slow connection')).toBeVisible();
        }
      }
    });
  });

  test.describe('Advanced Network Failure Scenarios', () => {
    test('should handle DNS resolution failures', async ({ page }) => {
      // Simulate DNS failure for external services
      await networkSim.simulateDNSFailure(['api.external-service.com', 'cdn.example.com']);
      
      await page.goto('/external-integrations');
      
      // Should show appropriate error for DNS failures
      await expect(page.getByText('External service unreachable')).toBeVisible();
      await expect(page.getByText('DNS resolution failed')).toBeVisible();
      
      // Should suggest troubleshooting steps
      await expect(page.getByText('Check your network settings')).toBeVisible();
    });

    test('should handle SSL certificate errors', async ({ page }) => {
      // Simulate SSL errors for HTTPS endpoints
      await networkSim.simulateSSLError(['secure-api.example.com']);
      
      await page.route('**/test-ssl-endpoint', route => {
        route.abort('connectionrefused');
      });
      
      // Try to access HTTPS resource
      await page.goto('/secure-features');
      
      // Should show security-related error messaging
      await expect(page.getByText('Secure connection failed')).toBeVisible();
      await expect(page.getByText('Certificate verification error')).toBeVisible();
    });

    test('should handle rate limiting scenarios', async ({ page }) => {
      // Set up rate limiting simulation
      let requestCount = 0;
      await page.route('**/api/**', route => {
        requestCount++;
        if (requestCount > 5) {
          // Simulate rate limiting
          route.fulfill({
            status: 429,
            headers: {
              'Retry-After': '60',
              'X-RateLimit-Remaining': '0'
            },
            body: JSON.stringify({
              error: 'Rate limit exceeded',
              retryAfter: 60
            })
          });
        } else {
          route.continue();
        }
      });
      
      // Make multiple requests quickly
      for (let i = 0; i < 8; i++) {
        await page.getByTestId('api-request-button').click();
        await page.waitForTimeout(100);
      }
      
      // Should show rate limiting message
      await expect(page.getByText('Rate limit exceeded')).toBeVisible();
      await expect(page.getByText('Please wait 60 seconds')).toBeVisible();
      
      // Should show countdown or retry timer
      await expect(page.getByTestId('retry-countdown')).toBeVisible();
    });
  });

  test.describe('Performance During Network Errors', () => {
    test('should maintain UI responsiveness during network failures', async ({ page }) => {
      // Set up very slow network
      await networkSim.throttle({ 
        downloadThroughput: 1000, // 1KB/s
        latency: 2000 
      });
      
      const startTime = Date.now();
      
      // Trigger network error
      await page.goto('/heavy-data-page');
      
      // UI should remain responsive even if data is slow
      await page.getByRole('button', { name: 'Settings' }).click();
      
      const clickResponseTime = Date.now() - startTime;
      expect(clickResponseTime).toBeLessThan(1000); // UI should respond within 1 second
      
      // Should show loading states
      await expect(page.getByTestId('loading-indicator')).toBeVisible();
      await expect(page.getByText('Loading slowly due to network conditions')).toBeVisible();
    });

    test('should optimize resource loading during poor connectivity', async ({ page }) => {
      await networkSim.throttle('SLOW_2G');
      
      await page.goto('/media-heavy-page');
      
      // Should show optimized/compressed images
      const images = await page.locator('img').all();
      for (const img of images) {
        const src = await img.getAttribute('src');
        // Should use low-resolution or compressed versions
        expect(src).toMatch(/\.(webp|jpg\?quality=low|small\.|thumb\.)/);
      }
      
      // Should defer non-critical resources
      await expect(page.getByText('Loading optimized version')).toBeVisible();
    });
  });
});

// Utility functions for complex scenarios
async function _simulateRealWorldNetworkPattern(networkSim: ReturnType<typeof createNetworkSimulator>) {
  // Simulate a real-world pattern: start with WiFi, go to mobile, experience brief outage
  await networkSim.clearThrottling(); // Fast WiFi
  await new Promise(resolve => setTimeout(resolve, 2000));
  
  await networkSim.throttle('FAST_3G'); // Switch to mobile
  await new Promise(resolve => setTimeout(resolve, 3000));
  
  await networkSim.goOffline(); // Brief outage
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  await networkSim.goOnline();
  await networkSim.throttle('SLOW_3G'); // Slower mobile
}