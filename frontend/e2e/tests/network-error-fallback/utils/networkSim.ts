/**
 * Advanced Network Simulator for E2E Testing
 * Provides comprehensive network failure and condition simulation
 */

import { Page, BrowserContext, CDPSession } from '@playwright/test';

export interface ThrottleOptions {
  downloadThroughput?: number; // bytes/second
  uploadThroughput?: number;   // bytes/second
  latency?: number;           // milliseconds
  packetLoss?: number;        // percentage (0-1)
}

export interface NetworkCondition {
  name: string;
  downloadThroughput: number;
  uploadThroughput: number;
  latency: number;
}

export const NETWORK_CONDITIONS: Record<string, NetworkCondition> = {
  FAST_3G: {
    name: 'Fast 3G',
    downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
    uploadThroughput: 750 * 1024 / 8,           // 750 Kbps  
    latency: 150
  },
  SLOW_3G: {
    name: 'Slow 3G',
    downloadThroughput: 500 * 1024 / 8,  // 500 Kbps
    uploadThroughput: 250 * 1024 / 8,    // 250 Kbps
    latency: 300
  },
  SLOW_2G: {
    name: 'Slow 2G',
    downloadThroughput: 50 * 1024 / 8,   // 50 Kbps
    uploadThroughput: 20 * 1024 / 8,     // 20 Kbps
    latency: 800
  },
  EDGE: {
    name: 'Edge',
    downloadThroughput: 240 * 1024 / 8,  // 240 Kbps
    uploadThroughput: 200 * 1024 / 8,    // 200 Kbps
    latency: 840
  },
  OFFLINE: {
    name: 'Offline',
    downloadThroughput: 0,
    uploadThroughput: 0,
    latency: 0
  }
};

export class NetworkSimulator {
  private page: Page;
  private context: BrowserContext;
  private cdpSession: CDPSession | null = null;
  private currentCondition: NetworkCondition | null = null;
  private routePatterns: string[] = [];
  private customLatency: number = 0;
  private failurePatterns: Map<string, number> = new Map();

  constructor(page: Page) {
    this.page = page;
    this.context = page.context();
  }

  /**
   * Initialize CDP session for Chromium-specific features
   */
  private async initializeCDP(): Promise<void> {
    try {
      this.cdpSession = await this.page.context().newCDPSession(this.page);
      await this.cdpSession.send('Network.enable');
    } catch {
      console.warn('CDP not available (likely not Chromium), falling back to route-based simulation');
    }
  }

  /**
   * Set browser to offline mode
   */
  async goOffline(): Promise<void> {
    console.log('🔌 Setting browser to offline mode');
    
    try {
      // Method 1: Browser context offline (works for all browsers)
      await this.context.setOffline(true);
      
      // Method 2: CDP for Chromium (more reliable)
      if (!this.cdpSession) {
        await this.initializeCDP();
      }
      
      if (this.cdpSession) {
        await this.cdpSession.send('Network.emulateNetworkConditions', {
          offline: true,
          downloadThroughput: 0,
          uploadThroughput: 0,
          latency: 0
        });
      }
      
      // Method 3: JavaScript navigator.onLine override
      await this.page.addInitScript(() => {
        Object.defineProperty(navigator, 'onLine', {
          writable: true,
          configurable: true,
          value: false
        });
        
        // Dispatch offline event
        window.dispatchEvent(new Event('offline'));
      });
      
      this.currentCondition = NETWORK_CONDITIONS.OFFLINE;
      console.log('✅ Browser is now offline');
      
    } catch (error) {
      console.error('❌ Failed to set offline mode:', error);
      throw error;
    }
  }

  /**
   * Set browser to online mode
   */
  async goOnline(): Promise<void> {
    console.log('🌐 Setting browser to online mode');
    
    try {
      // Clear offline state
      await this.context.setOffline(false);
      
      if (this.cdpSession) {
        await this.cdpSession.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: -1,
          uploadThroughput: -1,
          latency: 0
        });
      }
      
      // Reset JavaScript navigator.onLine
      await this.page.addInitScript(() => {
        Object.defineProperty(navigator, 'onLine', {
          writable: true,
          configurable: true,
          value: true
        });
        
        // Dispatch online event
        window.dispatchEvent(new Event('online'));
      });
      
      this.currentCondition = null;
      console.log('✅ Browser is now online');
      
    } catch (error) {
      console.error('❌ Failed to set online mode:', error);
      throw error;
    }
  }

  /**
   * Apply network throttling
   */
  async throttle(conditionOrOptions: string | ThrottleOptions): Promise<void> {
    let condition: NetworkCondition;
    
    if (typeof conditionOrOptions === 'string') {
      const predefined = NETWORK_CONDITIONS[conditionOrOptions.toUpperCase()];
      if (!predefined) {
        throw new Error(`Unknown network condition: ${conditionOrOptions}`);
      }
      condition = predefined;
    } else {
      condition = {
        name: 'Custom',
        downloadThroughput: conditionOrOptions.downloadThroughput || -1,
        uploadThroughput: conditionOrOptions.uploadThroughput || -1,
        latency: conditionOrOptions.latency || 0
      };
    }
    
    console.log(`🐌 Applying network throttling: ${condition.name}`);
    console.log(`   Download: ${(condition.downloadThroughput * 8 / 1024).toFixed(1)} Kbps`);
    console.log(`   Upload: ${(condition.uploadThroughput * 8 / 1024).toFixed(1)} Kbps`);
    console.log(`   Latency: ${condition.latency}ms`);
    
    try {
      if (this.cdpSession) {
        // Use CDP for Chromium
        await this.cdpSession.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: condition.downloadThroughput,
          uploadThroughput: condition.uploadThroughput,
          latency: condition.latency
        });
      } else {
        // Fallback: Route-based throttling for other browsers
        this.customLatency = condition.latency;
        await this.applyRouteBasedThrottling(condition);
      }
      
      this.currentCondition = condition;
      console.log(`✅ Network throttling applied: ${condition.name}`);
      
    } catch (error) {
      console.error('❌ Failed to apply network throttling:', error);
      throw error;
    }
  }

  /**
   * Route-based throttling fallback for non-Chromium browsers
   */
  private async applyRouteBasedThrottling(condition: NetworkCondition): Promise<void> {
    await this.context.route('**/*', async (route) => {
      const startTime = Date.now();
      
      // Add artificial latency
      if (condition.latency > 0) {
        await new Promise(resolve => setTimeout(resolve, condition.latency));
      }
      
      // Simulate bandwidth limitation (simplified)
      const _request = route.request();
      const response = await route.fetch();
      
      if (condition.downloadThroughput > 0) {
        const responseBody = await response.body();
        const simulatedDuration = (responseBody.length / condition.downloadThroughput) * 1000;
        const remainingDelay = Math.max(0, simulatedDuration - (Date.now() - startTime));
        
        if (remainingDelay > 0) {
          await new Promise(resolve => setTimeout(resolve, remainingDelay));
        }
      }
      
      await route.fulfill({
        response: response,
        body: await response.body()
      });
    });
  }

  /**
   * Clear network throttling
   */
  async clearThrottling(): Promise<void> {
    console.log('🚀 Clearing network throttling');
    
    try {
      if (this.cdpSession) {
        await this.cdpSession.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: -1,
          uploadThroughput: -1,
          latency: 0
        });
      }
      
      // Clear route-based throttling
      await this.context.unroute('**/*');
      this.customLatency = 0;
      this.currentCondition = null;
      
      console.log('✅ Network throttling cleared');
      
    } catch (error) {
      console.error('❌ Failed to clear network throttling:', error);
      throw error;
    }
  }

  /**
   * Simulate WebSocket connection failures
   */
  async breakWebSocket(options: { 
    failConnections?: boolean;
    failMessages?: boolean;
    closeAfter?: number;
  } = {}): Promise<void> {
    console.log('🔌 Breaking WebSocket connections');
    
    await this.page.addInitScript((opts) => {
      const OriginalWebSocket = window.WebSocket;
      
      (window as unknown).WebSocket = function(url: string, protocols?: string | string[]) {
        console.log('WebSocket intercepted:', url);
        
        if (opts.failConnections) {
          // Simulate connection failure
          setTimeout(() => {
            const ws = new OriginalWebSocket(url, protocols);
            ws.close(1011, 'Connection failed');
          }, 100);
          
          // Return a mock that fails
          return {
            close: () => {},
            send: () => { throw new Error('WebSocket connection failed'); },
            addEventListener: () => {},
            removeEventListener: () => {},
            readyState: 3, // CLOSED
            CONNECTING: 0,
            OPEN: 1,
            CLOSING: 2,
            CLOSED: 3
          };
        }
        
        const ws = new OriginalWebSocket(url, protocols);
        
        if (opts.closeAfter && opts.closeAfter > 0) {
          setTimeout(() => {
            ws.close(1011, 'Simulated disconnection');
          }, opts.closeAfter);
        }
        
        if (opts.failMessages) {
          const _originalSend = ws.send.bind(ws);
          ws.send = (_data: string | ArrayBufferLike | Blob | ArrayBufferView) => {
            throw new Error('Message sending failed');
          };
        }
        
        return ws;
      };
      
      // Copy static properties
      (window as unknown).WebSocket.CONNECTING = OriginalWebSocket.CONNECTING;
      (window as unknown).WebSocket.OPEN = OriginalWebSocket.OPEN;
      (window as unknown).WebSocket.CLOSING = OriginalWebSocket.CLOSING;
      (window as unknown).WebSocket.CLOSED = OriginalWebSocket.CLOSED;
    }, options);
  }

  /**
   * Simulate long disconnection period
   */
  async longDisconnect(duration: number): Promise<void> {
    console.log(`⏱️  Simulating ${duration}ms disconnection`);
    
    await this.goOffline();
    await new Promise(resolve => setTimeout(resolve, duration));
    await this.goOnline();
    
    console.log('✅ Long disconnection simulation completed');
  }

  /**
   * Simulate intermittent connectivity
   */
  async simulateIntermittentConnectivity(options: {
    onlineDuration: number;
    offlineDuration: number;
    cycles: number;
  }): Promise<void> {
    console.log(`🔄 Simulating intermittent connectivity (${options.cycles} cycles)`);
    
    for (let i = 0; i < options.cycles; i++) {
      console.log(`   Cycle ${i + 1}/${options.cycles}`);
      
      // Go online
      await this.goOnline();
      await new Promise(resolve => setTimeout(resolve, options.onlineDuration));
      
      // Go offline  
      await this.goOffline();
      await new Promise(resolve => setTimeout(resolve, options.offlineDuration));
    }
    
    // End in online state
    await this.goOnline();
    console.log('✅ Intermittent connectivity simulation completed');
  }

  /**
   * Add failure patterns for specific URLs or patterns
   */
  async addFailurePattern(pattern: string, failureRate: number): Promise<void> {
    this.failurePatterns.set(pattern, failureRate);
    
    await this.context.route(pattern, async (route) => {
      const shouldFail = Math.random() < failureRate;
      
      if (shouldFail) {
        console.log(`💥 Simulating failure for: ${route.request().url()}`);
        await route.abort('failed');
      } else {
        await route.continue();
      }
    });
    
    console.log(`🎯 Added failure pattern: ${pattern} (${(failureRate * 100).toFixed(1)}% failure rate)`);
  }

  /**
   * Remove failure pattern
   */
  async removeFailurePattern(pattern: string): Promise<void> {
    this.failurePatterns.delete(pattern);
    await this.context.unroute(pattern);
    console.log(`🗑️  Removed failure pattern: ${pattern}`);
  }

  /**
   * Simulate DNS resolution failures
   */
  async simulateDNSFailure(domains: string[]): Promise<void> {
    console.log('🌐 Simulating DNS resolution failures for:', domains);
    
    for (const domain of domains) {
      await this.context.route(`*://${domain}/**`, async (route) => {
        console.log(`💥 DNS failure for: ${domain}`);
        await route.abort('namenotresolved');
      });
    }
  }

  /**
   * Simulate SSL/TLS certificate errors
   */
  async simulateSSLError(domains: string[]): Promise<void> {
    console.log('🔒 Simulating SSL/TLS errors for:', domains);
    
    for (const domain of domains) {
      await this.context.route(`https://${domain}/**`, async (route) => {
        console.log(`💥 SSL error for: ${domain}`);
        await route.abort('connectionrefused');
      });
    }
  }

  /**
   * Get current network condition
   */
  getCurrentCondition(): NetworkCondition | null {
    return this.currentCondition;
  }

  /**
   * Check if browser is currently offline
   */
  async isOffline(): Promise<boolean> {
    return await this.page.evaluate(() => !navigator.onLine);
  }

  /**
   * Clean up all network modifications
   */
  async cleanup(): Promise<void> {
    console.log('🧹 Cleaning up network simulator');
    
    try {
      await this.clearThrottling();
      await this.goOnline();
      
      // Clear all route patterns
      for (const pattern of this.failurePatterns.keys()) {
        await this.context.unroute(pattern);
      }
      this.failurePatterns.clear();
      
      // Close CDP session
      if (this.cdpSession) {
        await this.cdpSession.detach();
        this.cdpSession = null;
      }
      
      console.log('✅ Network simulator cleanup completed');
      
    } catch (error) {
      console.warn('⚠️  Network simulator cleanup had errors:', error);
    }
  }
}

/**
 * Factory function to create network simulator
 */
export function createNetworkSimulator(page: Page): NetworkSimulator {
  return new NetworkSimulator(page);
}

/**
 * Common network testing scenarios
 */
export const NetworkScenarios = {
  /**
   * Mobile user on slow 3G switching to WiFi
   */
  async mobileToWiFi(simulator: NetworkSimulator): Promise<void> {
    await simulator.throttle('SLOW_3G');
    await new Promise(resolve => setTimeout(resolve, 5000));
    await simulator.clearThrottling();
  },

  /**
   * Office network with intermittent proxy issues
   */
  async proxyIssues(simulator: NetworkSimulator): Promise<void> {
    await simulator.addFailurePattern('**/api/**', 0.3);
    await new Promise(resolve => setTimeout(resolve, 10000));
    await simulator.removeFailurePattern('**/api/**');
  },

  /**
   * CDN outage scenario
   */
  async cdnOutage(simulator: NetworkSimulator): Promise<void> {
    await simulator.addFailurePattern('**/assets/**', 1.0);
    await simulator.addFailurePattern('**/static/**', 1.0);
    await new Promise(resolve => setTimeout(resolve, 8000));
    await simulator.removeFailurePattern('**/assets/**');
    await simulator.removeFailurePattern('**/static/**');
  }
};