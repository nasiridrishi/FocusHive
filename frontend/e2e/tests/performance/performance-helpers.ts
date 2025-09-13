/**
 * Performance Testing Helper Utilities for FocusHive Frontend
 * 
 * Comprehensive utilities for:
 * - Core Web Vitals measurement
 * - React component performance profiling
 * - Memory leak detection
 * - Network performance analysis
 * - WebSocket latency testing
 * - Bundle analysis
 * - Device and network simulation
 */

import { Page, BrowserContext, Browser, CDPSession } from '@playwright/test';

// Extended performance interfaces
interface ExtendedPerformance extends Performance {
  memory?: {
    usedJSHeapSize: number;
    totalJSHeapSize: number;
    jsHeapSizeLimit: number;
  };
}

interface PerformanceNavigationTimingExtended extends PerformanceNavigationTiming {
  navigationStart: number;
}

interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
}

interface LongTaskEntry extends PerformanceEntry {
  startTime: number;
  duration: number;
}

interface PaintEntry extends PerformanceEntry {
  name: string;
  startTime: number;
}

interface LCPEntry extends PerformanceEntry {
  startTime: number;
}

interface NetworkResponseEvent {
  response: {
    status: number;
    encodedDataLength?: number;
    fromDiskCache?: boolean;
    fromServiceWorker?: boolean;
    timing?: {
      receiveHeadersEnd?: number;
    };
  };
}

interface CoverageRange {
  startOffset: number;
  endOffset: number;
  count: number;
}

interface CoverageFunction {
  functionName: string;
  ranges: CoverageRange[];
}

interface JSCoverageEntry {
  scriptId: string;
  url: string;
  functions: CoverageFunction[];
}

interface CSSCoverageEntry {
  styleSheetId: string;
  startOffset: number;
  endOffset: number;
  used: boolean;
  text: string;
}

interface JSCoverageResult {
  result: JSCoverageEntry[];
}

interface CSSCoverageResult {
  coverage: CSSCoverageEntry[];
}

interface NavigationMetrics {
  ttfb: number;
  domContentLoaded: number;
  loadComplete: number;
}

interface WebSocketTestMessage {
  timestamp: number;
  type: string;
}

interface WebSocketTestMetrics {
  connectionTime: number;
  messagesSent: number;
  messagesReceived: number;
  averageLatency: number;
  reconnections: number;
}

interface TestDataItem {
  id: number;
  name: string;
  description: string;
  value: number;
}

declare global {
  interface Window {
    largeTestData?: TestDataItem[];
    React?: {
      Profiler?: unknown;
    };
    __REACT_DEVTOOLS_GLOBAL_HOOK__?: {
      onCommitFiberRoot?: (id: number, root: unknown, priorityLevel: unknown, didError: boolean) => void;
    };
  }
}

export interface CoreWebVitals {
  /** First Contentful Paint in milliseconds */
  fcp: number;
  /** Largest Contentful Paint in milliseconds */
  lcp: number;
  /** Time to Interactive in milliseconds */
  tti: number;
  /** Cumulative Layout Shift score */
  cls: number;
  /** First Input Delay in milliseconds */
  fid?: number;
  /** Time to First Byte in milliseconds */
  ttfb: number;
}

export interface ReactPerformanceMetrics {
  /** Component render time in milliseconds */
  renderTime: number;
  /** Number of component re-renders */
  rerenderCount: number;
  /** Memory usage during rendering */
  memoryUsage: MemoryInfo;
  /** Time to first meaningful paint after interaction */
  interactionResponseTime: number;
  /** Frames per second during animations */
  fps: number;
}

export interface MemoryInfo {
  /** Used JavaScript heap size in bytes */
  usedJSHeapSize: number;
  /** Total JavaScript heap size in bytes */
  totalJSHeapSize: number;
  /** JavaScript heap size limit in bytes */
  jsHeapSizeLimit: number;
  /** Memory usage percentage */
  usagePercentage: number;
}

export interface NetworkPerformanceMetrics {
  /** Total number of HTTP requests */
  totalRequests: number;
  /** Number of failed requests */
  failedRequests: number;
  /** Average response time in milliseconds */
  averageResponseTime: number;
  /** Total bytes transferred */
  totalBytesTransferred: number;
  /** Number of cached resources */
  cachedResources: number;
  /** Time to load all resources in milliseconds */
  resourceLoadTime: number;
  /** Bundle size metrics */
  bundleMetrics: BundleMetrics;
}

export interface BundleMetrics {
  /** Initial bundle size in bytes */
  initialBundleSize: number;
  /** Total JavaScript size in bytes */
  totalJSSize: number;
  /** Total CSS size in bytes */
  totalCSSSize: number;
  /** Number of chunks */
  chunkCount: number;
  /** Unused code percentage */
  unusedCodePercentage: number;
}

export interface WebSocketPerformanceMetrics {
  /** Connection establishment time in milliseconds */
  connectionTime: number;
  /** Average message latency in milliseconds */
  averageLatency: number;
  /** Messages sent per second */
  messagesPerSecond: number;
  /** Connection stability percentage */
  connectionStability: number;
  /** Number of reconnections */
  reconnections: number;
}

export interface LargeDataPerformanceMetrics {
  /** Time to render initial viewport in milliseconds */
  initialRenderTime: number;
  /** Time to scroll to end in milliseconds */
  scrollPerformance: number;
  /** Memory usage with large dataset */
  memoryUsage: MemoryInfo;
  /** Virtual scrolling efficiency */
  virtualizationEfficiency: number;
  /** Search performance in large dataset */
  searchPerformance: number;
}

export interface PerformanceThresholds {
  coreWebVitals: {
    fcp: { good: number; needsImprovement: number };
    lcp: { good: number; needsImprovement: number };
    tti: { good: number; needsImprovement: number };
    cls: { good: number; needsImprovement: number };
    fid: { good: number; needsImprovement: number };
    ttfb: { good: number; needsImprovement: number };
  };
  react: {
    renderTime: { good: number; needsImprovement: number };
    rerenderCount: { good: number; needsImprovement: number };
    fps: { good: number; needsImprovement: number };
  };
  memory: {
    usage: { good: number; needsImprovement: number }; // in MB
    leakRate: { good: number; needsImprovement: number }; // MB per minute
  };
  network: {
    totalRequests: { good: number; needsImprovement: number };
    bundleSize: { good: number; needsImprovement: number }; // in KB
    responseTime: { good: number; needsImprovement: number };
  };
}

export class PerformanceTestHelper {
  private cdpSession: CDPSession | null = null;
  private performanceObserver: PerformanceObserver | null = null;
  private memoryLeakDetector: NodeJS.Timeout | null = null;

  constructor(private page: Page) {}

  /**
   * Initialize performance monitoring
   */
  async initializePerformanceMonitoring(): Promise<void> {
    this.cdpSession = await this.page.context().newCDPSession(this.page);
    
    // Enable performance domains
    await this.cdpSession.send('Performance.enable');
    await this.cdpSession.send('Runtime.enable');
    await this.cdpSession.send('Network.enable');
    
    // Enable coverage for bundle analysis
    await this.cdpSession.send('Profiler.enable');
    await this.cdpSession.send('CSS.enable');
  }

  /**
   * Measure Core Web Vitals using Chrome DevTools Protocol
   */
  async measureCoreWebVitals(): Promise<CoreWebVitals> {
    const navigationMetrics = await this.page.evaluate(() => {
      return new Promise<NavigationMetrics>((resolve) => {
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const navigationEntry = entries.find(entry => entry.entryType === 'navigation') as PerformanceNavigationTiming;
          
          if (navigationEntry) {
            resolve({
              ttfb: navigationEntry.responseStart - navigationEntry.requestStart,
              domContentLoaded: navigationEntry.domContentLoadedEventEnd - navigationEntry.navigationStart,
              loadComplete: navigationEntry.loadEventEnd - navigationStart
            });
          }
        });
        
        observer.observe({ type: 'navigation', buffered: true });
        
        // Fallback timeout
        setTimeout(() => {
          const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
          resolve({
            ttfb: navigation ? navigation.responseStart - navigation.requestStart : 0,
            domContentLoaded: navigation ? navigation.domContentLoadedEventEnd - navigation.navigationStart : 0,
            loadComplete: navigation ? navigation.loadEventEnd - navigation.navigationStart : 0
          });
        }, 5000);
      });
    });

    // Get paint metrics
    const paintMetrics = await this.page.evaluate(() => {
      const paintEntries = performance.getEntriesByType('paint');
      const fcp = paintEntries.find(entry => entry.name === 'first-contentful-paint')?.startTime || 0;
      return { fcp };
    });

    // Get LCP using PerformanceObserver
    const lcp = await this.page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let lcpValue = 0;
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries() as LCPEntry[];
          const lastEntry = entries[entries.length - 1];
          if (lastEntry) {
            lcpValue = lastEntry.startTime;
          }
        });
        
        observer.observe({ type: 'largest-contentful-paint', buffered: true });
        
        setTimeout(() => resolve(lcpValue), 3000);
      });
    });

    // Get CLS using PerformanceObserver
    const cls = await this.page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let clsValue = 0;
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries() as LayoutShiftEntry[];
          for (const entry of entries) {
            if (!entry.hadRecentInput) {
              clsValue += entry.value;
            }
          }
        });
        
        observer.observe({ type: 'layout-shift', buffered: true });
        
        setTimeout(() => resolve(clsValue), 3000);
      });
    });

    // Calculate TTI (simplified version)
    const tti = await this.calculateTimeToInteractive();

    return {
      fcp: paintMetrics.fcp,
      lcp,
      tti,
      cls,
      ttfb: navigationMetrics.ttfb
    };
  }

  /**
   * Calculate Time to Interactive
   */
  private async calculateTimeToInteractive(): Promise<number> {
    return await this.page.evaluate(() => {
      return new Promise<number>((resolve) => {
        // Simplified TTI calculation based on long tasks
        let tti = 0;
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const longTasks = entries.filter(entry => entry.duration > 50);
          
          if (longTasks.length === 0) {
            tti = performance.now();
          } else {
            const lastLongTask = longTasks[longTasks.length - 1];
            tti = lastLongTask.startTime + lastLongTask.duration;
          }
        });
        
        try {
          observer.observe({ type: 'longtask', buffered: true });
        } catch {
          // Fallback if longtask is not supported
          tti = performance.now();
        }
        
        setTimeout(() => resolve(tti || performance.now()), 5000);
      });
    });
  }

  /**
   * Measure React component performance
   */
  async measureReactPerformance(componentSelector: string): Promise<ReactPerformanceMetrics> {
    // Start React profiling
    await this.page.evaluate(() => {
      if (window.React && window.React.Profiler) {
        // Enable profiling mode
        window.__REACT_DEVTOOLS_GLOBAL_HOOK__?.onCommitFiberRoot?.(1, null, null, true);
      }
    });

    const startTime = Date.now();
    const initialMemory = await this.getMemoryUsage();

    // Trigger component render
    await this.page.locator(componentSelector).first().click();
    await this.page.waitForLoadState('networkidle');

    const endTime = Date.now();
    const finalMemory = await this.getMemoryUsage();

    // Measure FPS during interaction
    const fps = await this.measureFPS();

    return {
      renderTime: endTime - startTime,
      rerenderCount: await this.countReRenders(),
      memoryUsage: finalMemory,
      interactionResponseTime: endTime - startTime,
      fps
    };
  }

  /**
   * Get current memory usage
   */
  async getMemoryUsage(): Promise<MemoryInfo> {
    const memInfo = await this.page.evaluate(() => {
      const memory = (performance as ExtendedPerformance).memory;
      if (memory) {
        return {
          usedJSHeapSize: memory.usedJSHeapSize,
          totalJSHeapSize: memory.totalJSHeapSize,
          jsHeapSizeLimit: memory.jsHeapSizeLimit,
          usagePercentage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100
        };
      }
      return {
        usedJSHeapSize: 0,
        totalJSHeapSize: 0,
        jsHeapSizeLimit: 0,
        usagePercentage: 0
      };
    });

    return memInfo;
  }

  /**
   * Detect memory leaks over time
   */
  async detectMemoryLeaks(durationMs: number, sampleIntervalMs: number = 1000): Promise<{ hasLeak: boolean; leakRate: number; samples: MemoryInfo[] }> {
    const samples: MemoryInfo[] = [];
    const startTime = Date.now();

    while (Date.now() - startTime < durationMs) {
      const memInfo = await this.getMemoryUsage();
      samples.push(memInfo);
      await this.page.waitForTimeout(sampleIntervalMs);
    }

    // Analyze trend
    if (samples.length < 3) {
      return { hasLeak: false, leakRate: 0, samples };
    }

    const firstSample = samples[0];
    const lastSample = samples[samples.length - 1];
    const memoryGrowth = lastSample.usedJSHeapSize - firstSample.usedJSHeapSize;
    const timeElapsed = durationMs / 1000 / 60; // in minutes
    const leakRate = memoryGrowth / 1024 / 1024 / timeElapsed; // MB per minute

    return {
      hasLeak: leakRate > 1, // Consider >1MB/min as a potential leak
      leakRate,
      samples
    };
  }

  /**
   * Measure network performance metrics
   */
  async measureNetworkPerformance(): Promise<NetworkPerformanceMetrics> {
    if (!this.cdpSession) {
      throw new Error('CDP session not initialized');
    }

    const networkRequests: NetworkResponseEvent[] = [];
    let totalBytes = 0;
    let cachedCount = 0;

    // Listen to network events
    this.cdpSession.on('Network.responseReceived', (event) => {
      networkRequests.push(event);
      totalBytes += event.response.encodedDataLength || 0;
      if (event.response.fromDiskCache || event.response.fromServiceWorker) {
        cachedCount++;
      }
    });

    // Reload page to measure
    await this.page.reload();
    await this.page.waitForLoadState('networkidle');

    // Calculate metrics
    const failedRequests = networkRequests.filter(req => req.response.status >= 400).length;
    const responseTimes = networkRequests.map(req => req.response.timing?.receiveHeadersEnd || 0);
    const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;

    // Get bundle metrics
    const bundleMetrics = await this.analyzeBundleSize();

    return {
      totalRequests: networkRequests.length,
      failedRequests,
      averageResponseTime: avgResponseTime,
      totalBytesTransferred: totalBytes,
      cachedResources: cachedCount,
      resourceLoadTime: await this.getResourceLoadTime(),
      bundleMetrics
    };
  }

  /**
   * Analyze bundle size and composition
   */
  private async analyzeBundleSize(): Promise<BundleMetrics> {
    if (!this.cdpSession) {
      throw new Error('CDP session not initialized');
    }

    // Start coverage
    await this.cdpSession.send('Profiler.startPreciseCoverage', { callCount: false, detailed: true });
    await this.cdpSession.send('CSS.startRuleUsageTracking');

    // Navigate to trigger bundle loading
    await this.page.reload();
    await this.page.waitForLoadState('networkidle');

    // Get coverage data
    const jsCoverage = await this.cdpSession.send('Profiler.takePreciseCoverage');
    const cssCoverage = await this.cdpSession.send('CSS.takeCoverageSnapshot');

    // Calculate unused code
    let totalJSBytes = 0;
    let unusedJSBytes = 0;

    for (const entry of jsCoverage.result) {
      totalJSBytes += entry.functions.reduce((total, func) => total + (func.ranges[0]?.count || 0), 0);
      unusedJSBytes += entry.functions.reduce((total, func) => {
        return total + func.ranges.filter(range => range.count === 0).length;
      }, 0);
    }

    const totalCSSBytes = cssCoverage.coverage.reduce((total: number, entry: CSSCoverageEntry) => total + entry.text.length, 0);

    return {
      initialBundleSize: totalJSBytes,
      totalJSSize: totalJSBytes,
      totalCSSSize: totalCSSBytes,
      chunkCount: await this.getChunkCount(),
      unusedCodePercentage: (unusedJSBytes / totalJSBytes) * 100
    };
  }

  /**
   * Measure WebSocket performance
   */
  async measureWebSocketPerformance(websocketUrl: string): Promise<WebSocketPerformanceMetrics> {
    const startTime = Date.now();
    let connectionTime = 0;
    let messageCount = 0;
    let totalLatency = 0;
    let reconnections = 0;

    const metrics = await this.page.evaluate(async (url) => {
      return new Promise<WebSocketTestMetrics>((resolve) => {
        const ws = new WebSocket(url);
        const startTime = Date.now();
        let connectionTime = 0;
        let messagesSent = 0;
        let messagesReceived = 0;
        let totalLatency = 0;
        let reconnections = 0;

        ws.onopen = () => {
          connectionTime = Date.now() - startTime;
          
          // Send test messages
          const sendMessage = () => {
            if (ws.readyState === WebSocket.OPEN) {
              const timestamp = Date.now();
              ws.send(JSON.stringify({ timestamp, type: 'ping' }));
              messagesSent++;
            }
          };

          const interval = setInterval(sendMessage, 100);
          setTimeout(() => {
            clearInterval(interval);
            ws.close();
            
            resolve({
              connectionTime,
              messagesSent,
              messagesReceived,
              averageLatency: messagesReceived > 0 ? totalLatency / messagesReceived : 0,
              reconnections
            });
          }, 5000);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.timestamp) {
              const latency = Date.now() - data.timestamp;
              totalLatency += latency;
              messagesReceived++;
            }
          } catch (e) {
            messagesReceived++;
          }
        };

        ws.onclose = () => {
          reconnections++;
        };

        ws.onerror = () => {
          reconnections++;
        };
      });
    }, websocketUrl);

    return {
      connectionTime: metrics.connectionTime,
      averageLatency: metrics.averageLatency,
      messagesPerSecond: metrics.messagesSent / 5, // 5 second test
      connectionStability: ((metrics.messagesSent - metrics.reconnections) / metrics.messagesSent) * 100,
      reconnections: metrics.reconnections
    };
  }

  /**
   * Test large data performance with virtualization
   */
  async measureLargeDataPerformance(dataSize: number): Promise<LargeDataPerformanceMetrics> {
    const startRenderTime = Date.now();

    // Generate large dataset
    await this.page.evaluate((size) => {
      const largeData = Array.from({ length: size }, (_, i) => ({
        id: i,
        name: `Item ${i}`,
        description: `Description for item ${i}`,
        value: Math.random() * 1000
      }));
      
      // Store in window for component to use
      window.largeTestData = largeData;
    }, dataSize);

    const initialRenderEnd = Date.now();
    const initialMemory = await this.getMemoryUsage();

    // Test scrolling performance
    const scrollStart = Date.now();
    await this.page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight);
    });
    await this.page.waitForTimeout(1000);
    const scrollEnd = Date.now();

    // Test search performance
    const searchStart = Date.now();
    await this.page.locator('input[placeholder*="search"]').first().fill('Item 500');
    await this.page.waitForTimeout(500);
    const searchEnd = Date.now();

    const finalMemory = await this.getMemoryUsage();

    return {
      initialRenderTime: initialRenderEnd - startRenderTime,
      scrollPerformance: scrollEnd - scrollStart,
      memoryUsage: finalMemory,
      virtualizationEfficiency: this.calculateVirtualizationEfficiency(initialMemory, finalMemory, dataSize),
      searchPerformance: searchEnd - searchStart
    };
  }

  /**
   * Simulate different network conditions
   */
  async simulateNetworkConditions(conditions: 'fast-3g' | 'slow-3g' | 'offline'): Promise<void> {
    if (!this.cdpSession) {
      throw new Error('CDP session not initialized');
    }

    const conditionMap = {
      'fast-3g': {
        offline: false,
        downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
        uploadThroughput: 750 * 1024 / 8, // 750 kbps
        latency: 150
      },
      'slow-3g': {
        offline: false,
        downloadThroughput: 500 * 1024 / 8, // 500 kbps
        uploadThroughput: 250 * 1024 / 8, // 250 kbps
        latency: 300
      },
      'offline': {
        offline: true,
        downloadThroughput: 0,
        uploadThroughput: 0,
        latency: 0
      }
    };

    await this.cdpSession.send('Network.emulateNetworkConditions', conditionMap[conditions]);
  }

  /**
   * Simulate CPU throttling
   */
  async simulateCPUThrottling(rate: number): Promise<void> {
    if (!this.cdpSession) {
      throw new Error('CDP session not initialized');
    }

    await this.cdpSession.send('Emulation.setCPUThrottlingRate', { rate });
  }

  /**
   * Get performance thresholds
   */
  static getPerformanceThresholds(): PerformanceThresholds {
    return {
      coreWebVitals: {
        fcp: { good: 1000, needsImprovement: 3000 },
        lcp: { good: 2500, needsImprovement: 4000 },
        tti: { good: 3500, needsImprovement: 5800 },
        cls: { good: 0.1, needsImprovement: 0.25 },
        fid: { good: 100, needsImprovement: 300 },
        ttfb: { good: 600, needsImprovement: 1500 }
      },
      react: {
        renderTime: { good: 16, needsImprovement: 33 }, // 60fps = 16ms/frame
        rerenderCount: { good: 5, needsImprovement: 15 },
        fps: { good: 55, needsImprovement: 30 }
      },
      memory: {
        usage: { good: 50, needsImprovement: 100 }, // MB
        leakRate: { good: 1, needsImprovement: 5 } // MB per minute
      },
      network: {
        totalRequests: { good: 10, needsImprovement: 30 },
        bundleSize: { good: 500, needsImprovement: 2000 }, // KB
        responseTime: { good: 200, needsImprovement: 1000 }
      }
    };
  }

  /**
   * Generate performance report
   */
  async generatePerformanceReport(metrics: {
    coreWebVitals: CoreWebVitals;
    reactPerformance: ReactPerformanceMetrics;
    networkPerformance: NetworkPerformanceMetrics;
    memoryLeak: { hasLeak: boolean; leakRate: number };
  }): Promise<string> {
    const thresholds = PerformanceTestHelper.getPerformanceThresholds();
    
    const report = `
# Frontend Performance Test Report

## Core Web Vitals
- **First Contentful Paint**: ${metrics.coreWebVitals.fcp}ms ${this.getStatus(metrics.coreWebVitals.fcp, thresholds.coreWebVitals.fcp)}
- **Largest Contentful Paint**: ${metrics.coreWebVitals.lcp}ms ${this.getStatus(metrics.coreWebVitals.lcp, thresholds.coreWebVitals.lcp)}
- **Time to Interactive**: ${metrics.coreWebVitals.tti}ms ${this.getStatus(metrics.coreWebVitals.tti, thresholds.coreWebVitals.tti)}
- **Cumulative Layout Shift**: ${metrics.coreWebVitals.cls} ${this.getStatus(metrics.coreWebVitals.cls, thresholds.coreWebVitals.cls)}
- **Time to First Byte**: ${metrics.coreWebVitals.ttfb}ms ${this.getStatus(metrics.coreWebVitals.ttfb, thresholds.coreWebVitals.ttfb)}

## React Performance
- **Render Time**: ${metrics.reactPerformance.renderTime}ms ${this.getStatus(metrics.reactPerformance.renderTime, thresholds.react.renderTime)}
- **FPS**: ${metrics.reactPerformance.fps} ${this.getStatus(metrics.reactPerformance.fps, { good: 55, needsImprovement: 30 })}
- **Memory Usage**: ${(metrics.reactPerformance.memoryUsage.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB

## Network Performance
- **Total Requests**: ${metrics.networkPerformance.totalRequests} ${this.getStatus(metrics.networkPerformance.totalRequests, thresholds.network.totalRequests)}
- **Bundle Size**: ${(metrics.networkPerformance.bundleMetrics.initialBundleSize / 1024).toFixed(2)}KB ${this.getStatus(metrics.networkPerformance.bundleMetrics.initialBundleSize / 1024, thresholds.network.bundleSize)}
- **Average Response Time**: ${metrics.networkPerformance.averageResponseTime}ms ${this.getStatus(metrics.networkPerformance.averageResponseTime, thresholds.network.responseTime)}

## Memory Analysis
- **Memory Leak Detected**: ${metrics.memoryLeak.hasLeak ? 'Yes' : 'No'}
- **Leak Rate**: ${metrics.memoryLeak.leakRate.toFixed(2)}MB/min ${this.getStatus(metrics.memoryLeak.leakRate, thresholds.memory.leakRate)}
`;

    return report;
  }

  // Helper methods
  private getStatus(value: number, threshold: { good: number; needsImprovement: number }): string {
    if (value <= threshold.good) return '✅ Good';
    if (value <= threshold.needsImprovement) return '⚠️ Needs Improvement';
    return '❌ Poor';
  }

  private async measureFPS(): Promise<number> {
    return await this.page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let frames = 0;
        let lastTime = performance.now();
        
        function countFrame(currentTime: number) {
          frames++;
          if (currentTime - lastTime >= 1000) {
            resolve(frames);
            return;
          }
          requestAnimationFrame(countFrame);
        }
        
        requestAnimationFrame(countFrame);
      });
    });
  }

  private async countReRenders(): Promise<number> {
    // This would require React DevTools integration
    // For now, return a placeholder
    return 0;
  }

  private async getResourceLoadTime(): Promise<number> {
    return await this.page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return navigation ? navigation.loadEventEnd - navigation.navigationStart : 0;
    });
  }

  private async getChunkCount(): Promise<number> {
    const scripts = await this.page.locator('script[src]').count();
    const stylesheets = await this.page.locator('link[rel="stylesheet"]').count();
    return scripts + stylesheets;
  }

  private calculateVirtualizationEfficiency(initialMemory: MemoryInfo, finalMemory: MemoryInfo, dataSize: number): number {
    const memoryGrowth = finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize;
    const expectedGrowth = dataSize * 100; // Assume 100 bytes per item without virtualization
    return Math.max(0, 100 - ((memoryGrowth / expectedGrowth) * 100));
  }

  async cleanup(): Promise<void> {
    if (this.cdpSession) {
      await this.cdpSession.detach();
    }
  }
}