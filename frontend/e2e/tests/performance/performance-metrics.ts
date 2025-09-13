/**
 * Performance Metrics Collection and Analysis Utilities
 * 
 * Provides standardized metrics collection, analysis, and reporting
 * for FocusHive frontend performance testing
 */

export interface PerformanceTestResult {
  testName: string;
  timestamp: number;
  duration: number;
  success: boolean;
  metrics: PerformanceMetrics;
  thresholds: PerformanceThresholds;
  violations: ThresholdViolation[];
  recommendations: string[];
}

export interface PerformanceMetrics {
  coreWebVitals?: CoreWebVitals;
  reactPerformance?: ReactPerformanceMetrics;
  memoryMetrics?: MemoryMetrics;
  networkMetrics?: NetworkMetrics;
  webSocketMetrics?: WebSocketMetrics;
  largeDataMetrics?: LargeDataMetrics;
  deviceMetrics?: DeviceMetrics;
}

export interface CoreWebVitals {
  fcp: number; // First Contentful Paint
  lcp: number; // Largest Contentful Paint
  tti: number; // Time to Interactive
  cls: number; // Cumulative Layout Shift
  fid?: number; // First Input Delay
  ttfb: number; // Time to First Byte
  tbt?: number; // Total Blocking Time
}

export interface ReactPerformanceMetrics {
  renderTime: number;
  rerenderCount: number;
  componentCount: number;
  memoryUsage: number;
  fps: number;
  interactionDelay: number;
  bundleSize: number;
  unusedCode: number;
}

export interface MemoryMetrics {
  initialUsage: number;
  peakUsage: number;
  finalUsage: number;
  leakDetected: boolean;
  leakRate: number; // MB per minute
  gcCount: number;
  heapSize: number;
}

export interface NetworkMetrics {
  requestCount: number;
  failedRequests: number;
  averageLatency: number;
  totalBytes: number;
  cachedResources: number;
  compressionRatio: number;
  httpVersion: string;
}

export interface WebSocketMetrics {
  connectionTime: number;
  messageLatency: number;
  throughput: number; // messages per second
  reliability: number; // percentage
  reconnects: number;
}

export interface LargeDataMetrics {
  renderTime: number;
  scrollPerformance: number;
  searchTime: number;
  virtualizationEfficiency: number;
  memoryGrowth: number;
}

export interface DeviceMetrics {
  deviceType: 'desktop' | 'tablet' | 'mobile';
  cpuThrottling: number;
  networkCondition: string;
  batteryStatus?: 'charging' | 'discharging' | 'unknown';
  orientation?: 'portrait' | 'landscape';
}

export interface PerformanceThresholds {
  coreWebVitals: {
    fcp: ThresholdValues;
    lcp: ThresholdValues;
    tti: ThresholdValues;
    cls: ThresholdValues;
    fid: ThresholdValues;
    ttfb: ThresholdValues;
  };
  react: {
    renderTime: ThresholdValues;
    fps: ThresholdValues;
    bundleSize: ThresholdValues;
  };
  memory: {
    usage: ThresholdValues;
    leakRate: ThresholdValues;
  };
  network: {
    latency: ThresholdValues;
    throughput: ThresholdValues;
  };
}

export interface ThresholdValues {
  excellent: number;
  good: number;
  needsImprovement: number;
  poor: number;
}

export interface ThresholdViolation {
  metric: string;
  value: number;
  threshold: number;
  severity: 'low' | 'medium' | 'high' | 'critical';
  impact: string;
}

export class PerformanceMetricsCollector {
  private results: PerformanceTestResult[] = [];
  private startTime: number = 0;

  /**
   * Start a performance test session
   */
  startTest(testName: string): void {
    this.startTime = Date.now();
    console.log(`🚀 Starting performance test: ${testName}`);
  }

  /**
   * End a performance test session and analyze results
   */
  endTest(testName: string, metrics: PerformanceMetrics): PerformanceTestResult {
    const duration = Date.now() - this.startTime;
    const thresholds = this.getStandardThresholds();
    const violations = this.analyzeViolations(metrics, thresholds);
    const recommendations = this.generateRecommendations(violations);
    
    const result: PerformanceTestResult = {
      testName,
      timestamp: Date.now(),
      duration,
      success: violations.filter(v => v.severity === 'critical').length === 0,
      metrics,
      thresholds,
      violations,
      recommendations
    };

    this.results.push(result);
    return result;
  }

  /**
   * Analyze metrics against thresholds
   */
  private analyzeViolations(metrics: PerformanceMetrics, thresholds: PerformanceThresholds): ThresholdViolation[] {
    const violations: ThresholdViolation[] = [];

    // Core Web Vitals analysis
    if (metrics.coreWebVitals) {
      const cwv = metrics.coreWebVitals;
      
      if (cwv.fcp > thresholds.coreWebVitals.fcp.poor) {
        violations.push({
          metric: 'First Contentful Paint',
          value: cwv.fcp,
          threshold: thresholds.coreWebVitals.fcp.good,
          severity: cwv.fcp > thresholds.coreWebVitals.fcp.poor * 1.5 ? 'critical' : 'high',
          impact: 'Users see blank screen for too long, affecting perceived performance'
        });
      }

      if (cwv.lcp > thresholds.coreWebVitals.lcp.poor) {
        violations.push({
          metric: 'Largest Contentful Paint',
          value: cwv.lcp,
          threshold: thresholds.coreWebVitals.lcp.good,
          severity: cwv.lcp > thresholds.coreWebVitals.lcp.poor * 1.5 ? 'critical' : 'high',
          impact: 'Main content loads too slowly, poor user experience'
        });
      }

      if (cwv.cls > thresholds.coreWebVitals.cls.poor) {
        violations.push({
          metric: 'Cumulative Layout Shift',
          value: cwv.cls,
          threshold: thresholds.coreWebVitals.cls.good,
          severity: cwv.cls > thresholds.coreWebVitals.cls.poor * 2 ? 'critical' : 'high',
          impact: 'Page layout shifts unexpectedly, disrupting user interactions'
        });
      }

      if (cwv.tti > thresholds.coreWebVitals.tti.poor) {
        violations.push({
          metric: 'Time to Interactive',
          value: cwv.tti,
          threshold: thresholds.coreWebVitals.tti.good,
          severity: cwv.tti > thresholds.coreWebVitals.tti.poor * 1.5 ? 'critical' : 'high',
          impact: 'Page becomes interactive too late, frustrating user interactions'
        });
      }
    }

    // React Performance analysis
    if (metrics.reactPerformance) {
      const react = metrics.reactPerformance;
      
      if (react.fps < thresholds.react.fps.poor) {
        violations.push({
          metric: 'Frames Per Second',
          value: react.fps,
          threshold: thresholds.react.fps.good,
          severity: react.fps < 30 ? 'critical' : 'high',
          impact: 'Animations and interactions appear janky, poor user experience'
        });
      }

      if (react.bundleSize > thresholds.react.bundleSize.poor * 1024) {
        violations.push({
          metric: 'Bundle Size',
          value: react.bundleSize / 1024,
          threshold: thresholds.react.bundleSize.good,
          severity: react.bundleSize > thresholds.react.bundleSize.poor * 1024 * 2 ? 'critical' : 'medium',
          impact: 'Large bundle size increases loading time, especially on slow connections'
        });
      }
    }

    // Memory analysis
    if (metrics.memoryMetrics) {
      const memory = metrics.memoryMetrics;
      
      if (memory.leakDetected && memory.leakRate > thresholds.memory.leakRate.poor) {
        violations.push({
          metric: 'Memory Leak Rate',
          value: memory.leakRate,
          threshold: thresholds.memory.leakRate.good,
          severity: memory.leakRate > thresholds.memory.leakRate.poor * 2 ? 'critical' : 'high',
          impact: 'Memory leaks can cause browser crashes and poor performance over time'
        });
      }

      if (memory.peakUsage > thresholds.memory.usage.poor) {
        violations.push({
          metric: 'Peak Memory Usage',
          value: memory.peakUsage,
          threshold: thresholds.memory.usage.good,
          severity: memory.peakUsage > thresholds.memory.usage.poor * 2 ? 'critical' : 'medium',
          impact: 'High memory usage can cause performance issues on low-end devices'
        });
      }
    }

    // Network analysis
    if (metrics.networkMetrics) {
      const network = metrics.networkMetrics;
      
      if (network.averageLatency > thresholds.network.latency.poor) {
        violations.push({
          metric: 'Network Latency',
          value: network.averageLatency,
          threshold: thresholds.network.latency.good,
          severity: network.averageLatency > thresholds.network.latency.poor * 2 ? 'critical' : 'medium',
          impact: 'High network latency slows down all API interactions'
        });
      }
    }

    return violations;
  }

  /**
   * Generate performance improvement recommendations
   */
  private generateRecommendations(violations: ThresholdViolation[]): string[] {
    const recommendations: string[] = [];
    const metricTypes = violations.map(v => v.metric);

    // Core Web Vitals recommendations
    if (metricTypes.includes('First Contentful Paint')) {
      recommendations.push('Optimize critical rendering path - minimize blocking CSS and JavaScript');
      recommendations.push('Implement resource preloading for critical assets');
      recommendations.push('Use a Content Delivery Network (CDN) for faster asset delivery');
    }

    if (metricTypes.includes('Largest Contentful Paint')) {
      recommendations.push('Optimize images - use modern formats (WebP, AVIF) and responsive images');
      recommendations.push('Implement lazy loading for non-critical images');
      recommendations.push('Optimize server response times and consider server-side rendering');
    }

    if (metricTypes.includes('Cumulative Layout Shift')) {
      recommendations.push('Set explicit dimensions for images and embedded content');
      recommendations.push('Avoid inserting content above existing content');
      recommendations.push('Use CSS containment to limit layout thrashing');
    }

    if (metricTypes.includes('Time to Interactive')) {
      recommendations.push('Reduce JavaScript execution time - split code and load progressively');
      recommendations.push('Remove unused JavaScript code');
      recommendations.push('Optimize third-party scripts loading');
    }

    // React-specific recommendations
    if (metricTypes.includes('Frames Per Second')) {
      recommendations.push('Use React.memo() to prevent unnecessary re-renders');
      recommendations.push('Implement virtualization for long lists');
      recommendations.push('Optimize expensive computations with useMemo() and useCallback()');
      recommendations.push('Consider using React Concurrent Features for better user experience');
    }

    if (metricTypes.includes('Bundle Size')) {
      recommendations.push('Implement code splitting and lazy loading for routes');
      recommendations.push('Use tree shaking to eliminate unused code');
      recommendations.push('Analyze bundle composition and remove unnecessary dependencies');
      recommendations.push('Enable compression (gzip/brotli) on the server');
    }

    // Memory recommendations
    if (metricTypes.includes('Memory Leak Rate')) {
      recommendations.push('Fix memory leaks - check event listeners, timers, and subscriptions cleanup');
      recommendations.push('Use React DevTools Profiler to identify memory issues');
      recommendations.push('Implement proper cleanup in useEffect hooks');
    }

    if (metricTypes.includes('Peak Memory Usage')) {
      recommendations.push('Optimize large object handling and implement pagination');
      recommendations.push('Use object pooling for frequently created/destroyed objects');
      recommendations.push('Consider using web workers for heavy computations');
    }

    // Network recommendations
    if (metricTypes.includes('Network Latency')) {
      recommendations.push('Implement request caching and reduce API call frequency');
      recommendations.push('Use GraphQL or request batching to reduce network round trips');
      recommendations.push('Implement offline-first architecture with service workers');
    }

    // General recommendations if multiple issues
    if (violations.length > 3) {
      recommendations.push('Consider implementing a performance budget and monitoring');
      recommendations.push('Set up continuous performance monitoring in CI/CD pipeline');
      recommendations.push('Use performance profiling tools regularly during development');
    }

    return [...new Set(recommendations)]; // Remove duplicates
  }

  /**
   * Get standard performance thresholds
   */
  private getStandardThresholds(): PerformanceThresholds {
    return {
      coreWebVitals: {
        fcp: { excellent: 1000, good: 1800, needsImprovement: 3000, poor: 4000 },
        lcp: { excellent: 2500, good: 4000, needsImprovement: 6000, poor: 8000 },
        tti: { excellent: 3800, good: 5800, needsImprovement: 8000, poor: 10000 },
        cls: { excellent: 0.1, good: 0.25, needsImprovement: 0.5, poor: 0.75 },
        fid: { excellent: 100, good: 300, needsImprovement: 500, poor: 1000 },
        ttfb: { excellent: 800, good: 1500, needsImprovement: 2500, poor: 4000 }
      },
      react: {
        renderTime: { excellent: 16, good: 33, needsImprovement: 50, poor: 100 },
        fps: { excellent: 60, good: 55, needsImprovement: 45, poor: 30 },
        bundleSize: { excellent: 250, good: 500, needsImprovement: 1000, poor: 2000 } // KB
      },
      memory: {
        usage: { excellent: 25, good: 50, needsImprovement: 100, poor: 200 }, // MB
        leakRate: { excellent: 0.5, good: 1, needsImprovement: 3, poor: 5 } // MB/min
      },
      network: {
        latency: { excellent: 100, good: 200, needsImprovement: 500, poor: 1000 }, // ms
        throughput: { excellent: 10, good: 5, needsImprovement: 2, poor: 1 } // MB/s
      }
    };
  }

  /**
   * Generate comprehensive performance report
   */
  generateReport(): string {
    if (this.results.length === 0) {
      return 'No performance tests have been run yet.';
    }

    const latestResult = this.results[this.results.length - 1];
    const criticalIssues = latestResult.violations.filter(v => v.severity === 'critical').length;
    const highIssues = latestResult.violations.filter(v => v.severity === 'high').length;
    const totalIssues = latestResult.violations.length;

    let report = `
# 📊 Frontend Performance Test Report

**Test:** ${latestResult.testName}
**Date:** ${new Date(latestResult.timestamp).toISOString()}
**Duration:** ${latestResult.duration}ms
**Status:** ${latestResult.success ? '✅ PASS' : '❌ FAIL'}
**Issues:** ${totalIssues} total (${criticalIssues} critical, ${highIssues} high)

## 🎯 Core Web Vitals
`;

    if (latestResult.metrics.coreWebVitals) {
      const cwv = latestResult.metrics.coreWebVitals;
      const thresholds = latestResult.thresholds.coreWebVitals;
      
      report += `
- **First Contentful Paint:** ${cwv.fcp}ms ${this.getStatusIcon(cwv.fcp, thresholds.fcp)}
- **Largest Contentful Paint:** ${cwv.lcp}ms ${this.getStatusIcon(cwv.lcp, thresholds.lcp)}
- **Time to Interactive:** ${cwv.tti}ms ${this.getStatusIcon(cwv.tti, thresholds.tti)}
- **Cumulative Layout Shift:** ${cwv.cls.toFixed(3)} ${this.getStatusIcon(cwv.cls, thresholds.cls)}
- **Time to First Byte:** ${cwv.ttfb}ms ${this.getStatusIcon(cwv.ttfb, thresholds.ttfb)}
`;
    }

    if (latestResult.metrics.reactPerformance) {
      const react = latestResult.metrics.reactPerformance;
      report += `
## ⚛️ React Performance
- **Render Time:** ${react.renderTime}ms
- **FPS:** ${react.fps}
- **Bundle Size:** ${(react.bundleSize / 1024).toFixed(2)}KB
- **Memory Usage:** ${(react.memoryUsage / 1024 / 1024).toFixed(2)}MB
`;
    }

    if (latestResult.metrics.memoryMetrics) {
      const memory = latestResult.metrics.memoryMetrics;
      report += `
## 💾 Memory Analysis
- **Peak Usage:** ${memory.peakUsage.toFixed(2)}MB
- **Memory Leak:** ${memory.leakDetected ? '🚨 Detected' : '✅ None'}
- **Leak Rate:** ${memory.leakRate.toFixed(2)}MB/min
- **Heap Size:** ${memory.heapSize.toFixed(2)}MB
`;
    }

    if (latestResult.violations.length > 0) {
      report += `
## ⚠️ Performance Issues
`;
      latestResult.violations.forEach(violation => {
        const icon = this.getSeverityIcon(violation.severity);
        report += `
### ${icon} ${violation.metric}
- **Current Value:** ${violation.value}
- **Threshold:** ${violation.threshold}
- **Impact:** ${violation.impact}
`;
      });
    }

    if (latestResult.recommendations.length > 0) {
      report += `
## 💡 Recommendations
`;
      latestResult.recommendations.forEach((rec, index) => {
        report += `${index + 1}. ${rec}\n`;
      });
    }

    report += `
## 📈 Historical Trend
`;
    if (this.results.length > 1) {
      const previousResult = this.results[this.results.length - 2];
      if (latestResult.metrics.coreWebVitals && previousResult.metrics.coreWebVitals) {
        const fcpTrend = latestResult.metrics.coreWebVitals.fcp - previousResult.metrics.coreWebVitals.fcp;
        const lcpTrend = latestResult.metrics.coreWebVitals.lcp - previousResult.metrics.coreWebVitals.lcp;
        
        report += `
- **FCP Trend:** ${fcpTrend > 0 ? '📈' : '📉'} ${Math.abs(fcpTrend)}ms
- **LCP Trend:** ${lcpTrend > 0 ? '📈' : '📉'} ${Math.abs(lcpTrend)}ms
`;
      }
    } else {
      report += 'Not enough data for trend analysis. Run more tests to see historical trends.\n';
    }

    return report;
  }

  /**
   * Export results for external analysis
   */
  exportResults(): PerformanceTestResult[] {
    return [...this.results];
  }

  /**
   * Clear all results
   */
  clearResults(): void {
    this.results = [];
  }

  // Helper methods
  private getStatusIcon(value: number, threshold: ThresholdValues): string {
    if (value <= threshold.excellent) return '🟢 Excellent';
    if (value <= threshold.good) return '🟡 Good';
    if (value <= threshold.needsImprovement) return '🟠 Needs Improvement';
    return '🔴 Poor';
  }

  private getSeverityIcon(severity: string): string {
    switch (severity) {
      case 'critical': return '🚨';
      case 'high': return '⚠️';
      case 'medium': return '⚡';
      case 'low': return 'ℹ️';
      default: return '❓';
    }
  }
}

// Singleton instance
export const performanceCollector = new PerformanceMetricsCollector();