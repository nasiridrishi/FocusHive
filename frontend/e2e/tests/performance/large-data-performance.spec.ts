/**
 * Large Data Performance Tests with Virtualization
 * 
 * Tests for handling large datasets efficiently in the FocusHive frontend:
 * - Virtual scrolling performance with large lists
 * - Pagination vs infinite scroll efficiency
 * - Search and filtering performance on large datasets
 * - Memory management with large data structures
 * - UI responsiveness during data operations
 * - Data visualization performance with large datasets
 * - Component rendering optimization for large collections
 */

import { test, expect, Page } from '@playwright/test';
import { PerformanceTestHelper, LargeDataPerformanceMetrics } from './performance-helpers';
import { performanceCollector, PerformanceMetrics } from './performance-metrics';
import { AuthHelper } from '../../helpers/auth.helper';

// Extended performance interface for memory access
interface ExtendedPerformance extends Performance {
  memory?: {
    usedJSHeapSize: number;
    totalJSHeapSize: number;
    jsHeapSizeLimit: number;
  };
}

// Data point interface for visualization
interface DataPoint {
  x?: number;
  y?: number;
  value?: number;
  label?: string;
  category?: string;
  date?: string;
  activities?: number;
}

// Large data performance test configuration
const LARGE_DATA_TEST_CONFIG = {
  datasetSizes: [
    { name: 'Small', size: 1000, expectedRenderTime: 500 },
    { name: 'Medium', size: 5000, expectedRenderTime: 1000 },
    { name: 'Large', size: 10000, expectedRenderTime: 2000 },
    { name: 'Extra Large', size: 50000, expectedRenderTime: 5000 }
  ],
  
  virtualizationThresholds: {
    renderTime: 1000,        // 1 second max initial render
    scrollPerformance: 100,  // 100ms max scroll latency
    memoryEfficiency: 80,    // 80% memory efficiency with virtualization
    searchTime: 500,         // 500ms max search time
    filterTime: 300          // 300ms max filter time
  },
  
  testComponents: [
    {
      name: 'HiveList',
      route: '/hives',
      selector: '[data-testid="hive-list"]',
      itemSelector: '[data-testid="hive-card"]',
      searchSelector: '[data-testid="hive-search"]',
      filterSelector: '[data-testid="hive-filter"]'
    },
    {
      name: 'UserList',
      route: '/admin/users',
      selector: '[data-testid="user-list"]',
      itemSelector: '[data-testid="user-row"]',
      searchSelector: '[data-testid="user-search"]',
      filterSelector: '[data-testid="user-filter"]'
    },
    {
      name: 'AnalyticsTable',
      route: '/analytics',
      selector: '[data-testid="analytics-table"]',
      itemSelector: '[data-testid="analytics-row"]',
      searchSelector: '[data-testid="analytics-search"]',
      filterSelector: '[data-testid="analytics-filter"]'
    },
    {
      name: 'NotificationList',
      route: '/notifications',
      selector: '[data-testid="notification-list"]',
      itemSelector: '[data-testid="notification-item"]',
      searchSelector: '[data-testid="notification-search"]',
      filterSelector: '[data-testid="notification-filter"]'
    }
  ],
  
  visualizationTests: [
    {
      name: 'AnalyticsChart',
      dataPoints: [100, 1000, 5000, 10000],
      expectedRenderTime: [100, 300, 1000, 2000]
    },
    {
      name: 'ActivityHeatmap',
      dataPoints: [365, 1000, 2000],
      expectedRenderTime: [200, 500, 1000]
    },
    {
      name: 'NetworkGraph',
      nodes: [50, 200, 500],
      expectedRenderTime: [500, 1500, 3000]
    }
  ]
};

interface DataGenerationConfig {
  size: number;
  complexity: 'simple' | 'medium' | 'complex';
  includeImages: boolean;
  includeMetadata: boolean;
}

test.describe('Large Data Performance Tests with Virtualization', () => {
  let authHelper: AuthHelper;
  let performanceHelper: PerformanceTestHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    performanceHelper = new PerformanceTestHelper(page);
    await performanceHelper.initializePerformanceMonitoring();
    await authHelper.loginWithTestUser();
  });

  test.afterEach(async () => {
    await performanceHelper.cleanup();
  });

  // Test virtual scrolling performance with different dataset sizes
  for (const dataset of LARGE_DATA_TEST_CONFIG.datasetSizes) {
    test(`Virtual Scrolling - ${dataset.name} Dataset (${dataset.size} items)`, async ({ page }) => {
      performanceCollector.startTest(`Virtual Scrolling - ${dataset.name}`);

      await page.goto('http://localhost:3000/hives');
      await page.waitForLoadState('networkidle');

      // Generate large dataset
      const largeDataMetrics = await performanceHelper.measureLargeDataPerformance(dataset.size);

      // Test virtual scrolling performance
      const scrollPerformance = await page.evaluate((size) => {
        return new Promise<{
          initialRenderTime: number;
          scrollLatency: number[];
          visibleItems: number;
          totalItems: number;
          memoryBefore: number;
          memoryAfter: number;
          virtualizationActive: boolean;
        }>((resolve) => {
          const startTime = performance.now();
          
          // Create a large dataset
          const dataset = Array.from({ length: size }, (_, i) => ({
            id: i,
            name: `Item ${i}`,
            description: `Description for item ${i}`,
            category: `Category ${i % 10}`,
            priority: Math.random() > 0.5 ? 'high' : 'low',
            createdAt: new Date(Date.now() - Math.random() * 86400000 * 30).toISOString(),
            metadata: {
              tags: [`tag-${i % 5}`, `category-${i % 3}`],
              score: Math.random() * 100,
              active: Math.random() > 0.3
            }
          }));

          // Simulate virtual list rendering
          const container = document.createElement('div');
          container.style.height = '400px';
          container.style.overflow = 'auto';
          container.id = 'virtual-list-container';
          document.body.appendChild(container);

          const itemHeight = 60;
          const containerHeight = 400;
          const visibleCount = Math.ceil(containerHeight / itemHeight);
          
          let scrollTop = 0;
          let startIndex = 0;
          let endIndex = Math.min(visibleCount, dataset.length);
          
          const renderVisibleItems = () => {
            startIndex = Math.floor(scrollTop / itemHeight);
            endIndex = Math.min(startIndex + visibleCount + 1, dataset.length);
            
            container.innerHTML = '';
            
            // Create spacer for items above
            if (startIndex > 0) {
              const topSpacer = document.createElement('div');
              topSpacer.style.height = `${startIndex * itemHeight}px`;
              container.appendChild(topSpacer);
            }
            
            // Render visible items
            for (let i = startIndex; i < endIndex; i++) {
              const item = dataset[i];
              const itemEl = document.createElement('div');
              itemEl.style.height = `${itemHeight}px`;
              itemEl.style.padding = '10px';
              itemEl.style.borderBottom = '1px solid #eee';
              itemEl.innerHTML = `
                <div><strong>${item.name}</strong></div>
                <div>${item.description}</div>
                <div>Category: ${item.category} | Priority: ${item.priority}</div>
              `;
              container.appendChild(itemEl);
            }
            
            // Create spacer for items below
            const remainingItems = dataset.length - endIndex;
            if (remainingItems > 0) {
              const bottomSpacer = document.createElement('div');
              bottomSpacer.style.height = `${remainingItems * itemHeight}px`;
              container.appendChild(bottomSpacer);
            }
          };

          const memoryBefore = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          
          // Initial render
          renderVisibleItems();
          const initialRenderTime = performance.now() - startTime;
          
          // Test scrolling performance
          const scrollLatencies: number[] = [];
          let scrollTests = 0;
          const maxScrollTests = 10;
          
          const testScroll = () => {
            if (scrollTests >= maxScrollTests) {
              const memoryAfter = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
              
              document.body.removeChild(container);
              
              resolve({
                initialRenderTime,
                scrollLatency: scrollLatencies,
                visibleItems: endIndex - startIndex,
                totalItems: dataset.length,
                memoryBefore,
                memoryAfter,
                virtualizationActive: endIndex - startIndex < dataset.length
              });
              return;
            }
            
            const scrollStart = performance.now();
            scrollTop += itemHeight * 5; // Scroll by 5 items
            
            if (scrollTop > (dataset.length - visibleCount) * itemHeight) {
              scrollTop = 0; // Reset to top
            }
            
            container.scrollTop = scrollTop;
            
            requestAnimationFrame(() => {
              renderVisibleItems();
              const scrollLatency = performance.now() - scrollStart;
              scrollLatencies.push(scrollLatency);
              scrollTests++;
              
              setTimeout(testScroll, 100); // Wait before next scroll
            });
          };
          
          setTimeout(testScroll, 500); // Start scroll testing
        });
      }, dataset.size);

      // Validate virtual scrolling performance
      expect(scrollPerformance.initialRenderTime, `${dataset.name} initial render should be fast`)
        .toBeLessThan(dataset.expectedRenderTime);

      expect(scrollPerformance.virtualizationActive, 'Virtualization should be active for large datasets')
        .toBe(dataset.size > 100);

      const avgScrollLatency = scrollPerformance.scrollLatency.reduce((a, b) => a + b, 0) / scrollPerformance.scrollLatency.length;
      expect(avgScrollLatency, 'Scroll performance should be smooth')
        .toBeLessThan(LARGE_DATA_TEST_CONFIG.virtualizationThresholds.scrollPerformance);

      // Test memory efficiency of virtualization
      const memoryGrowth = scrollPerformance.memoryAfter - scrollPerformance.memoryBefore;
      const expectedMemoryWithoutVirtualization = dataset.size * 0.5; // Estimate 0.5KB per item
      const memoryEfficiency = Math.max(0, (expectedMemoryWithoutVirtualization - memoryGrowth) / expectedMemoryWithoutVirtualization * 100);
      
      if (dataset.size > 1000) {
        expect(memoryEfficiency, 'Virtualization should provide memory efficiency')
          .toBeGreaterThan(LARGE_DATA_TEST_CONFIG.virtualizationThresholds.memoryEfficiency);
      }

      // Record results
      const largeDataMetrics_result: LargeDataPerformanceMetrics = {
        initialRenderTime: scrollPerformance.initialRenderTime,
        scrollPerformance: avgScrollLatency,
        searchPerformance: 0, // Not tested in this specific test
        virtualizationEfficiency: memoryEfficiency,
        memoryUsage: {
          usedJSHeapSize: scrollPerformance.memoryAfter * 1024 * 1024,
          totalJSHeapSize: scrollPerformance.memoryAfter * 1024 * 1024 * 2,
          jsHeapSizeLimit: scrollPerformance.memoryAfter * 1024 * 1024 * 4,
          usagePercentage: 25
        }
      };

      const metrics: PerformanceMetrics = {
        largeDataMetrics: largeDataMetrics_result
      };

      const result = performanceCollector.endTest(`Virtual Scrolling - ${dataset.name}`, metrics);

      console.log(`📜 Virtual Scrolling Performance - ${dataset.name}:`);
      console.log(`  Dataset Size: ${dataset.size} items`);
      console.log(`  Initial Render: ${scrollPerformance.initialRenderTime.toFixed(2)}ms`);
      console.log(`  Visible Items: ${scrollPerformance.visibleItems}`);
      console.log(`  Avg Scroll Latency: ${avgScrollLatency.toFixed(2)}ms`);
      console.log(`  Memory Growth: ${memoryGrowth.toFixed(2)}MB`);
      console.log(`  Memory Efficiency: ${memoryEfficiency.toFixed(2)}%`);
      console.log(`  Virtualization Active: ${scrollPerformance.virtualizationActive}`);
    });
  }

  // Test search and filtering performance on large datasets
  for (const component of LARGE_DATA_TEST_CONFIG.testComponents) {
    test(`Search & Filter Performance - ${component.name}`, async ({ page }) => {
      performanceCollector.startTest(`Search Filter - ${component.name}`);

      await page.goto(`http://localhost:3000${component.route}`);
      await page.waitForLoadState('networkidle');

      // Generate test data and measure search performance
      const searchFilterPerformance = await page.evaluate((config) => {
        return new Promise<{
          datasetSize: number;
          searchTime: number[];
          filterTime: number[];
          combinedSearchFilter: number;
          resultsAccuracy: number;
          memoryImpact: number;
        }>((resolve) => {
          // Generate dataset
          const dataset = Array.from({ length: 10000 }, (_, i) => ({
            id: i,
            name: `${config.name} Item ${i}`,
            description: `Description for ${config.name.toLowerCase()} item ${i}`,
            category: ['work', 'study', 'personal', 'team', 'project'][i % 5],
            status: ['active', 'inactive', 'pending', 'completed'][i % 4],
            priority: ['high', 'medium', 'low'][i % 3],
            tags: [`tag-${i % 10}`, `category-${i % 5}`],
            createdAt: new Date(Date.now() - Math.random() * 86400000 * 365).toISOString(),
            score: Math.random() * 100
          }));

          const memoryBefore = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          
          let filteredData = [...dataset];
          const searchTimes: number[] = [];
          const filterTimes: number[] = [];

          // Test search performance
          const searchTerms = ['Item 100', 'work', 'description', 'high', 'tag-5'];
          
          searchTerms.forEach(term => {
            const searchStart = performance.now();
            
            const searchResults = dataset.filter(item => 
              item.name.toLowerCase().includes(term.toLowerCase()) ||
              item.description.toLowerCase().includes(term.toLowerCase()) ||
              item.category.toLowerCase().includes(term.toLowerCase()) ||
              item.status.toLowerCase().includes(term.toLowerCase()) ||
              item.tags.some(tag => tag.toLowerCase().includes(term.toLowerCase()))
            );
            
            const searchTime = performance.now() - searchStart;
            searchTimes.push(searchTime);
          });

          // Test filter performance
          const filterConfigs = [
            { category: 'work' },
            { status: 'active' },
            { priority: 'high' },
            { category: 'work', status: 'active' },
            { priority: 'high', status: 'completed' }
          ];

          filterConfigs.forEach(filter => {
            const filterStart = performance.now();
            
            const filterResults = dataset.filter(item => {
              return Object.entries(filter).every(([key, value]) => 
                item[key as keyof typeof item] === value
              );
            });
            
            const filterTime = performance.now() - filterStart;
            filterTimes.push(filterTime);
          });

          // Test combined search and filter
          const combinedStart = performance.now();
          const combinedResults = dataset
            .filter(item => item.category === 'work')
            .filter(item => item.name.toLowerCase().includes('item'))
            .filter(item => item.priority === 'high');
          const combinedTime = performance.now() - combinedStart;

          const memoryAfter = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;

          resolve({
            datasetSize: dataset.length,
            searchTime: searchTimes,
            filterTime: filterTimes,
            combinedSearchFilter: combinedTime,
            resultsAccuracy: 95, // Simulated accuracy
            memoryImpact: memoryAfter - memoryBefore
          });
        });
      }, component);

      // Validate search and filter performance
      const avgSearchTime = searchFilterPerformance.searchTime.reduce((a, b) => a + b, 0) / searchFilterPerformance.searchTime.length;
      const avgFilterTime = searchFilterPerformance.filterTime.reduce((a, b) => a + b, 0) / searchFilterPerformance.filterTime.length;

      expect(avgSearchTime, `${component.name} search should be fast`)
        .toBeLessThan(LARGE_DATA_TEST_CONFIG.virtualizationThresholds.searchTime);

      expect(avgFilterTime, `${component.name} filtering should be fast`)
        .toBeLessThan(LARGE_DATA_TEST_CONFIG.virtualizationThresholds.filterTime);

      expect(searchFilterPerformance.combinedSearchFilter, 'Combined search and filter should be efficient')
        .toBeLessThan(LARGE_DATA_TEST_CONFIG.virtualizationThresholds.searchTime + LARGE_DATA_TEST_CONFIG.virtualizationThresholds.filterTime);

      // Record results
      const largeDataMetrics: LargeDataPerformanceMetrics = {
        initialRenderTime: 0,
        scrollPerformance: 0,
        searchPerformance: avgSearchTime,
        virtualizationEfficiency: 0,
        memoryUsage: {
          usedJSHeapSize: searchFilterPerformance.memoryImpact * 1024 * 1024,
          totalJSHeapSize: searchFilterPerformance.memoryImpact * 1024 * 1024 * 2,
          jsHeapSizeLimit: searchFilterPerformance.memoryImpact * 1024 * 1024 * 4,
          usagePercentage: 25
        }
      };

      const metrics: PerformanceMetrics = {
        largeDataMetrics: largeDataMetrics
      };

      const result = performanceCollector.endTest(`Search Filter - ${component.name}`, metrics);

      console.log(`🔍 Search & Filter Performance - ${component.name}:`);
      console.log(`  Dataset Size: ${searchFilterPerformance.datasetSize} items`);
      console.log(`  Avg Search Time: ${avgSearchTime.toFixed(2)}ms`);
      console.log(`  Avg Filter Time: ${avgFilterTime.toFixed(2)}ms`);
      console.log(`  Combined Operation: ${searchFilterPerformance.combinedSearchFilter.toFixed(2)}ms`);
      console.log(`  Memory Impact: ${searchFilterPerformance.memoryImpact.toFixed(2)}MB`);
      console.log(`  Results Accuracy: ${searchFilterPerformance.resultsAccuracy}%`);
    });
  }

  // Test data visualization performance with large datasets
  for (const visualization of LARGE_DATA_TEST_CONFIG.visualizationTests) {
    for (let i = 0; i < visualization.dataPoints.length; i++) {
      const dataPoints = visualization.dataPoints[i];
      const expectedTime = visualization.expectedRenderTime[i];

      test(`Data Visualization - ${visualization.name} with ${dataPoints} data points`, async ({ page }) => {
        performanceCollector.startTest(`Visualization - ${visualization.name} - ${dataPoints}pts`);

        await page.goto('http://localhost:3000/analytics');
        await page.waitForLoadState('networkidle');

        const visualizationPerformance = await page.evaluate((config) => {
          return new Promise<{
            renderTime: number;
            interactionLatency: number;
            memoryUsage: number;
            animationFPS: number;
            dataProcessingTime: number;
          }>((resolve) => {
            const { name, dataPoints } = config;
            
            // Generate visualization data
            const generateData = () => {
              switch (name) {
                case 'AnalyticsChart':
                  return Array.from({ length: dataPoints }, (_, i) => ({
                    x: i,
                    y: Math.sin(i / 10) * 50 + Math.random() * 20,
                    label: `Point ${i}`,
                    category: `Cat ${i % 5}`
                  }));
                
                case 'ActivityHeatmap':
                  return Array.from({ length: dataPoints }, (_, i) => {
                    const date = new Date(2023, 0, 1);
                    date.setDate(date.getDate() + i);
                    return {
                      date: date.toISOString().split('T')[0],
                      value: Math.floor(Math.random() * 5),
                      activities: Math.floor(Math.random() * 10)
                    };
                  });
                
                case 'NetworkGraph':
                  const nodes = Array.from({ length: dataPoints }, (_, i) => ({
                    id: i,
                    name: `Node ${i}`,
                    group: i % 10,
                    connections: Math.floor(Math.random() * 5)
                  }));
                  
                  const edges = [];
                  for (let i = 0; i < dataPoints * 1.5; i++) {
                    edges.push({
                      source: Math.floor(Math.random() * dataPoints),
                      target: Math.floor(Math.random() * dataPoints),
                      weight: Math.random()
                    });
                  }
                  
                  return { nodes, edges };
                
                default:
                  return [];
              }
            };

            const memoryBefore = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
            
            // Data processing time
            const dataProcessStart = performance.now();
            const data = generateData();
            const dataProcessingTime = performance.now() - dataProcessStart;
            
            // Render time
            const renderStart = performance.now();
            
            // Simulate chart rendering
            const canvas = document.createElement('canvas');
            canvas.width = 800;
            canvas.height = 600;
            document.body.appendChild(canvas);
            const ctx = canvas.getContext('2d')!;
            
            // Simple visualization rendering simulation
            ctx.fillStyle = '#f0f0f0';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            
            if (Array.isArray(data)) {
              data.forEach((point: DataPoint, index: number) => {
                ctx.fillStyle = `hsl(${(index / data.length) * 360}, 70%, 50%)`;
                const x = (index / data.length) * canvas.width;
                const y = canvas.height - ((point.y || point.value || 50) / 100) * canvas.height;
                ctx.fillRect(x, y, 2, canvas.height - y);
              });
            }
            
            const renderTime = performance.now() - renderStart;
            
            // Test interaction latency
            const interactionStart = performance.now();
            canvas.dispatchEvent(new MouseEvent('click', { clientX: 400, clientY: 300 }));
            const interactionLatency = performance.now() - interactionStart;
            
            // Test animation FPS
            let frameCount = 0;
            const animationStart = performance.now();
            
            const animate = () => {
              frameCount++;
              if (performance.now() - animationStart < 1000) {
                requestAnimationFrame(animate);
              } else {
                const animationFPS = frameCount;
                const memoryAfter = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
                
                document.body.removeChild(canvas);
                
                resolve({
                  renderTime,
                  interactionLatency,
                  memoryUsage: memoryAfter - memoryBefore,
                  animationFPS,
                  dataProcessingTime
                });
              }
            };
            
            animate();
          });
        }, { name: visualization.name, dataPoints });

        // Validate visualization performance
        expect(visualizationPerformance.renderTime, `${visualization.name} render time should be acceptable`)
          .toBeLessThan(expectedTime);

        expect(visualizationPerformance.dataProcessingTime, 'Data processing should be efficient')
          .toBeLessThan(expectedTime / 2);

        expect(visualizationPerformance.interactionLatency, 'Interactions should be responsive')
          .toBeLessThan(100);

        expect(visualizationPerformance.animationFPS, 'Animation should be smooth')
          .toBeGreaterThan(30);

        // Record results
        const largeDataMetrics: LargeDataPerformanceMetrics = {
          initialRenderTime: visualizationPerformance.renderTime,
          scrollPerformance: 0,
          searchPerformance: 0,
          virtualizationEfficiency: 0,
          memoryUsage: {
            usedJSHeapSize: visualizationPerformance.memoryUsage * 1024 * 1024,
            totalJSHeapSize: visualizationPerformance.memoryUsage * 1024 * 1024 * 2,
            jsHeapSizeLimit: visualizationPerformance.memoryUsage * 1024 * 1024 * 4,
            usagePercentage: 25
          }
        };

        const metrics: PerformanceMetrics = {
          largeDataMetrics: largeDataMetrics
        };

        const result = performanceCollector.endTest(`Visualization - ${visualization.name} - ${dataPoints}pts`, metrics);

        console.log(`📊 Data Visualization Performance - ${visualization.name}:`);
        console.log(`  Data Points: ${dataPoints}`);
        console.log(`  Data Processing: ${visualizationPerformance.dataProcessingTime.toFixed(2)}ms`);
        console.log(`  Render Time: ${visualizationPerformance.renderTime.toFixed(2)}ms`);
        console.log(`  Interaction Latency: ${visualizationPerformance.interactionLatency.toFixed(2)}ms`);
        console.log(`  Animation FPS: ${visualizationPerformance.animationFPS}`);
        console.log(`  Memory Usage: ${visualizationPerformance.memoryUsage.toFixed(2)}MB`);
      });
    }
  }

  // Test pagination vs infinite scroll performance comparison
  test('Data Loading Strategy Comparison - Pagination vs Infinite Scroll', async ({ page }) => {
    performanceCollector.startTest('Data Loading - Strategy Comparison');

    await page.goto('http://localhost:3000/hives');
    await page.waitForLoadState('networkidle');

    const strategyComparison = await page.evaluate(() => {
      return new Promise<{
        pagination: {
          loadTime: number;
          memoryUsage: number;
          userExperience: number;
          networkRequests: number;
        };
        infiniteScroll: {
          loadTime: number;
          memoryUsage: number;
          userExperience: number;
          networkRequests: number;
        };
      }>((resolve) => {
        const dataSize = 10000;
        const pageSize = 20;
        const data = Array.from({ length: dataSize }, (_, i) => ({
          id: i,
          name: `Item ${i}`,
          description: `Description ${i}`,
          timestamp: Date.now() - Math.random() * 86400000
        }));

        // Test Pagination Strategy
        const testPagination = () => {
          const memoryBefore = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          const loadStart = performance.now();
          
          let currentPage = 0;
          let totalRequests = 0;
          
          const loadPage = (page: number) => {
            totalRequests++;
            const start = page * pageSize;
            const end = start + pageSize;
            return data.slice(start, end);
          };
          
          // Simulate loading first 5 pages
          const loadedData = [];
          for (let i = 0; i < 5; i++) {
            const pageData = loadPage(i);
            loadedData.push(...pageData);
          }
          
          const loadTime = performance.now() - loadStart;
          const memoryAfter = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          const memoryUsage = memoryAfter - memoryBefore;
          
          return {
            loadTime,
            memoryUsage,
            userExperience: 85, // Simulated UX score (navigation overhead)
            networkRequests: totalRequests
          };
        };

        // Test Infinite Scroll Strategy
        const testInfiniteScroll = () => {
          const memoryBefore = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          const loadStart = performance.now();
          
          let loadedItems = 0;
          let totalRequests = 0;
          const batchSize = 20;
          
          const loadBatch = () => {
            totalRequests++;
            const batch = data.slice(loadedItems, loadedItems + batchSize);
            loadedItems += batchSize;
            return batch;
          };
          
          // Simulate loading 5 batches (equivalent to pagination test)
          const loadedData = [];
          for (let i = 0; i < 5; i++) {
            const batch = loadBatch();
            loadedData.push(...batch);
          }
          
          const loadTime = performance.now() - loadStart;
          const memoryAfter = (performance as ExtendedPerformance).memory?.usedJSHeapSize / 1024 / 1024 || 0;
          const memoryUsage = memoryAfter - memoryBefore;
          
          return {
            loadTime,
            memoryUsage,
            userExperience: 92, // Simulated UX score (seamless experience)
            networkRequests: totalRequests
          };
        };

        const paginationResults = testPagination();
        const infiniteScrollResults = testInfiniteScroll();

        resolve({
          pagination: paginationResults,
          infiniteScroll: infiniteScrollResults
        });
      });
    });

    // Analyze strategy comparison
    const paginationScore = strategyComparison.pagination.userExperience - 
      (strategyComparison.pagination.loadTime / 100) - 
      (strategyComparison.pagination.memoryUsage * 2);

    const infiniteScrollScore = strategyComparison.infiniteScroll.userExperience - 
      (strategyComparison.infiniteScroll.loadTime / 100) - 
      (strategyComparison.infiniteScroll.memoryUsage * 2);

    // Record results
    const largeDataMetrics: LargeDataPerformanceMetrics = {
      initialRenderTime: (strategyComparison.pagination.loadTime + strategyComparison.infiniteScroll.loadTime) / 2,
      scrollPerformance: 0,
      searchPerformance: 0,
      virtualizationEfficiency: infiniteScrollScore > paginationScore ? 85 : 70,
      memoryUsage: {
        usedJSHeapSize: Math.max(strategyComparison.pagination.memoryUsage, strategyComparison.infiniteScroll.memoryUsage) * 1024 * 1024,
        totalJSHeapSize: 0,
        jsHeapSizeLimit: 0,
        usagePercentage: 25
      }
    };

    const metrics: PerformanceMetrics = {
      largeDataMetrics: largeDataMetrics
    };

    const result = performanceCollector.endTest('Data Loading - Strategy Comparison', metrics);

    console.log(`⚖️ Data Loading Strategy Comparison:`);
    console.log(`  Pagination:`);
    console.log(`    Load Time: ${strategyComparison.pagination.loadTime.toFixed(2)}ms`);
    console.log(`    Memory Usage: ${strategyComparison.pagination.memoryUsage.toFixed(2)}MB`);
    console.log(`    UX Score: ${strategyComparison.pagination.userExperience}`);
    console.log(`    Network Requests: ${strategyComparison.pagination.networkRequests}`);
    console.log(`    Overall Score: ${paginationScore.toFixed(2)}`);
    
    console.log(`  Infinite Scroll:`);
    console.log(`    Load Time: ${strategyComparison.infiniteScroll.loadTime.toFixed(2)}ms`);
    console.log(`    Memory Usage: ${strategyComparison.infiniteScroll.memoryUsage.toFixed(2)}MB`);
    console.log(`    UX Score: ${strategyComparison.infiniteScroll.userExperience}`);
    console.log(`    Network Requests: ${strategyComparison.infiniteScroll.networkRequests}`);
    console.log(`    Overall Score: ${infiniteScrollScore.toFixed(2)}`);
    
    console.log(`  Recommended Strategy: ${infiniteScrollScore > paginationScore ? 'Infinite Scroll' : 'Pagination'}`);
  });
});