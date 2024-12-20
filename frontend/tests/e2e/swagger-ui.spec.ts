import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test.describe('Swagger UI Testing - Identity Service', () => {
  const swaggerUrl = 'http://localhost:8081/swagger-ui/index.html';
  const reportPath = 'test-results/swagger-report';
  let issues: Array<{
    type: string;
    severity: 'critical' | 'high' | 'medium' | 'low';
    description: string;
    element?: string;
    screenshot?: string;
  }> = [];

  test.beforeAll(async () => {
    // Create report directory
    if (!fs.existsSync(reportPath)) {
      fs.mkdirSync(reportPath, { recursive: true });
    }
  });

  test.afterAll(async () => {
    // Generate HTML report
    const report = generateHTMLReport(issues);
    fs.writeFileSync(path.join(reportPath, 'swagger-test-report.html'), report);
    console.log(`\n📊 Test Report generated at: ${reportPath}/swagger-test-report.html`);
    
    // Generate JSON report
    fs.writeFileSync(path.join(reportPath, 'issues.json'), JSON.stringify(issues, null, 2));
  });

  test('Swagger UI Accessibility and Functionality Tests', async ({ page }) => {
    console.log('🚀 Starting Swagger UI tests...\n');
    
    // Navigate to Swagger UI
    const response = await page.goto(swaggerUrl, { 
      waitUntil: 'networkidle',
      timeout: 30000 
    });

    // Test 1: Check if page loads successfully
    test.step('1. Page Load Test', async () => {
      if (!response || response.status() !== 200) {
        issues.push({
          type: 'Page Load',
          severity: 'critical',
          description: `Failed to load Swagger UI. Status: ${response?.status() || 'No response'}`,
          screenshot: 'page-load-error.png'
        });
        await page.screenshot({ 
          path: path.join(reportPath, 'page-load-error.png'),
          fullPage: true 
        });
      } else {
        console.log('✅ Page loaded successfully');
      }
    });

    // Test 2: Check for Swagger UI title
    await test.step('2. Swagger UI Title Test', async () => {
      const title = await page.title();
      const swaggerTitle = await page.locator('.swagger-ui .info .title').first();
      
      if (await swaggerTitle.isVisible()) {
        const titleText = await swaggerTitle.textContent();
        console.log(`✅ API Title found: ${titleText}`);
      } else {
        issues.push({
          type: 'UI Element',
          severity: 'medium',
          description: 'Swagger UI title not found',
          element: '.swagger-ui .info .title'
        });
      }
      
      await page.screenshot({ 
        path: path.join(reportPath, 'swagger-header.png'),
        fullPage: false 
      });
    });

    // Test 3: Check for API version
    await test.step('3. API Version Test', async () => {
      const versionElement = await page.locator('.swagger-ui .info .version').first();
      
      if (await versionElement.isVisible()) {
        const version = await versionElement.textContent();
        console.log(`✅ API Version: ${version}`);
      } else {
        issues.push({
          type: 'UI Element',
          severity: 'low',
          description: 'API version information not displayed',
          element: '.swagger-ui .info .version'
        });
      }
    });

    // Test 4: Check for API endpoints
    await test.step('4. API Endpoints Test', async () => {
      const endpoints = await page.locator('.opblock').all();
      
      if (endpoints.length === 0) {
        issues.push({
          type: 'Content',
          severity: 'critical',
          description: 'No API endpoints found in Swagger UI',
          screenshot: 'no-endpoints.png'
        });
        await page.screenshot({ 
          path: path.join(reportPath, 'no-endpoints.png'),
          fullPage: true 
        });
      } else {
        console.log(`✅ Found ${endpoints.length} API endpoints`);
        
        // Capture endpoints screenshot
        await page.screenshot({ 
          path: path.join(reportPath, 'api-endpoints.png'),
          fullPage: true 
        });
      }
    });

    // Test 5: Check authorization functionality
    await test.step('5. Authorization Test', async () => {
      const authButton = page.locator('.authorize');
      const authorizeBtn = page.locator('button').filter({ hasText: /Authorize/i }).first();
      
      if (await authButton.isVisible() || await authorizeBtn.isVisible()) {
        console.log('✅ Authorization button found');
        
        // Click authorize button
        if (await authButton.isVisible()) {
          await authButton.click();
        } else {
          await authorizeBtn.click();
        }
        
        // Wait for modal
        await page.waitForTimeout(1000);
        
        // Check for auth modal
        const authModal = page.locator('.modal-ux');
        if (await authModal.isVisible()) {
          console.log('✅ Authorization modal opens correctly');
          
          await page.screenshot({ 
            path: path.join(reportPath, 'auth-modal.png'),
            fullPage: false 
          });
          
          // Close modal
          const closeButton = page.locator('.modal-ux .close-modal');
          if (await closeButton.isVisible()) {
            await closeButton.click();
          } else {
            await page.keyboard.press('Escape');
          }
        }
      } else {
        issues.push({
          type: 'Security',
          severity: 'high',
          description: 'Authorization button not found - API security might not be properly configured',
          screenshot: 'no-auth-button.png'
        });
        await page.screenshot({ 
          path: path.join(reportPath, 'no-auth-button.png'),
          fullPage: false 
        });
      }
    });

    // Test 6: Check for Models/Schemas section
    await test.step('6. Models/Schemas Test', async () => {
      const modelsSection = page.locator('.models');
      
      if (await modelsSection.isVisible()) {
        console.log('✅ Models/Schemas section found');
        
        // Expand models section if collapsed
        const modelsToggle = page.locator('.models h4').first();
        if (await modelsToggle.isVisible()) {
          await modelsToggle.click();
          await page.waitForTimeout(500);
        }
        
        await page.screenshot({ 
          path: path.join(reportPath, 'models-section.png'),
          fullPage: false 
        });
      } else {
        issues.push({
          type: 'Documentation',
          severity: 'medium',
          description: 'Models/Schemas section not found - API documentation might be incomplete',
          element: '.models'
        });
      }
    });

    // Test 7: Test endpoint expansion
    await test.step('7. Endpoint Expansion Test', async () => {
      const firstEndpoint = page.locator('.opblock').first();
      
      if (await firstEndpoint.isVisible()) {
        // Click to expand
        await firstEndpoint.locator('.opblock-summary').click();
        await page.waitForTimeout(500);
        
        // Check if expanded
        const tryItButton = firstEndpoint.locator('.try-out__btn');
        if (await tryItButton.isVisible()) {
          console.log('✅ Endpoint expansion works correctly');
          
          await page.screenshot({ 
            path: path.join(reportPath, 'expanded-endpoint.png'),
            fullPage: false 
          });
        } else {
          issues.push({
            type: 'Functionality',
            severity: 'medium',
            description: 'Endpoint does not expand properly or Try it out button not visible',
            element: '.try-out__btn'
          });
        }
      }
    });

    // Test 8: Check for CORS headers
    await test.step('8. CORS Configuration Test', async () => {
      // Try to fetch the swagger.json
      try {
        const swaggerJsonUrl = 'http://localhost:8081/v3/api-docs';
        const apiDocsResponse = await page.evaluate(async (url) => {
          try {
            const response = await fetch(url);
            return {
              status: response.status,
              headers: {
                'access-control-allow-origin': response.headers.get('access-control-allow-origin'),
                'access-control-allow-methods': response.headers.get('access-control-allow-methods'),
                'access-control-allow-headers': response.headers.get('access-control-allow-headers')
              }
            };
          } catch (e) {
            return { error: e.message };
          }
        }, swaggerJsonUrl);

        if ('error' in apiDocsResponse) {
          issues.push({
            type: 'CORS',
            severity: 'high',
            description: `CORS issue detected: ${apiDocsResponse.error}`,
            element: 'API endpoint'
          });
        } else {
          console.log('✅ API docs endpoint accessible');
          if (!apiDocsResponse.headers['access-control-allow-origin']) {
            issues.push({
              type: 'CORS',
              severity: 'medium',
              description: 'CORS headers not properly configured on API',
              element: 'API headers'
            });
          }
        }
      } catch (e) {
        console.error('Failed to test CORS:', e);
      }
    });

    // Test 9: Check for broken links
    await test.step('9. Links Test', async () => {
      const links = await page.locator('a[href]').all();
      let brokenLinks = 0;
      
      for (const link of links.slice(0, 5)) { // Test first 5 links
        const href = await link.getAttribute('href');
        if (href && (href.startsWith('http://') || href.startsWith('https://'))) {
          try {
            const response = await page.request.head(href).catch(() => null);
            if (!response || response.status() >= 400) {
              brokenLinks++;
            }
          } catch {
            brokenLinks++;
          }
        }
      }
      
      if (brokenLinks > 0) {
        issues.push({
          type: 'Links',
          severity: 'low',
          description: `Found ${brokenLinks} broken external links`,
          element: 'External links'
        });
      } else {
        console.log('✅ No broken links found');
      }
    });

    // Test 10: Performance check
    await test.step('10. Performance Test', async () => {
      const metrics = await page.evaluate(() => {
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        return {
          domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
          loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
          domInteractive: navigation.domInteractive - navigation.fetchStart
        };
      });
      
      console.log(`⚡ Performance Metrics:
        - DOM Content Loaded: ${metrics.domContentLoaded}ms
        - Load Complete: ${metrics.loadComplete}ms
        - DOM Interactive: ${metrics.domInteractive}ms`);
      
      if (metrics.domInteractive > 3000) {
        issues.push({
          type: 'Performance',
          severity: 'medium',
          description: `Slow page load detected: ${metrics.domInteractive}ms to interactive`,
          element: 'Page load'
        });
      }
    });

    // Final screenshot
    await page.screenshot({ 
      path: path.join(reportPath, 'final-state.png'),
      fullPage: true 
    });
  });

  test('Console Errors Check', async ({ page }) => {
    const consoleErrors: string[] = [];
    
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    page.on('pageerror', (error) => {
      consoleErrors.push(error.message);
    });

    await page.goto(swaggerUrl, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    if (consoleErrors.length > 0) {
      issues.push({
        type: 'Console Errors',
        severity: 'medium',
        description: `Found ${consoleErrors.length} console errors: ${consoleErrors.join(', ')}`,
        screenshot: 'console-errors.png'
      });
      
      await page.screenshot({ 
        path: path.join(reportPath, 'console-errors.png'),
        fullPage: true 
      });
    } else {
      console.log('✅ No console errors detected');
    }
  });

  function generateHTMLReport(issues: any[]): string {
    const severityColors = {
      critical: '#dc3545',
      high: '#fd7e14',
      medium: '#ffc107',
      low: '#0dcaf0'
    };

    const issuesByType = issues.reduce((acc, issue) => {
      if (!acc[issue.type]) acc[issue.type] = [];
      acc[issue.type].push(issue);
      return acc;
    }, {} as Record<string, any[]>);

    return `
<!DOCTYPE html>
<html>
<head>
    <title>Swagger UI Test Report</title>
    <style>
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
        }
        .container { max-width: 1200px; margin: 0 auto; }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .stat-value { font-size: 36px; font-weight: bold; margin: 10px 0; }
        .issue-card {
            background: white;
            padding: 20px;
            margin-bottom: 15px;
            border-radius: 8px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            border-left: 4px solid #ddd;
        }
        .severity-critical { border-left-color: #dc3545; }
        .severity-high { border-left-color: #fd7e14; }
        .severity-medium { border-left-color: #ffc107; }
        .severity-low { border-left-color: #0dcaf0; }
        .severity-badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            color: white;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }
        .screenshot {
            margin-top: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
            max-width: 100%;
        }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .error { color: #dc3545; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔍 Swagger UI Test Report</h1>
            <p>Identity Service - http://localhost:8081/swagger-ui/index.html</p>
            <p>Generated: ${new Date().toLocaleString()}</p>
        </div>
        
        <div class="summary">
            <div class="stat-card">
                <div>Total Issues</div>
                <div class="stat-value ${issues.length === 0 ? 'success' : 'warning'}">${issues.length}</div>
            </div>
            <div class="stat-card">
                <div>Critical Issues</div>
                <div class="stat-value error">${issues.filter(i => i.severity === 'critical').length}</div>
            </div>
            <div class="stat-card">
                <div>High Priority</div>
                <div class="stat-value warning">${issues.filter(i => i.severity === 'high').length}</div>
            </div>
            <div class="stat-card">
                <div>Test Status</div>
                <div class="stat-value ${issues.filter(i => i.severity === 'critical').length > 0 ? 'error' : 'success'}">
                    ${issues.filter(i => i.severity === 'critical').length > 0 ? '❌ Failed' : '✅ Passed'}
                </div>
            </div>
        </div>

        ${issues.length === 0 ? `
            <div class="issue-card" style="border-left-color: #28a745;">
                <h3 class="success">🎉 No Issues Found!</h3>
                <p>All Swagger UI tests passed successfully.</p>
            </div>
        ` : `
            <h2>Issues Found</h2>
            ${Object.entries(issuesByType).map(([type, typeIssues]) => `
                <h3>${type} Issues (${typeIssues.length})</h3>
                ${typeIssues.map(issue => `
                    <div class="issue-card severity-${issue.severity}">
                        <span class="severity-badge" style="background: ${severityColors[issue.severity]};">
                            ${issue.severity}
                        </span>
                        <h4>${issue.type}</h4>
                        <p>${issue.description}</p>
                        ${issue.element ? `<p><small>Element: <code>${issue.element}</code></small></p>` : ''}
                        ${issue.screenshot ? `
                            <details>
                                <summary>View Screenshot</summary>
                                <img src="${issue.screenshot}" alt="Screenshot" class="screenshot" />
                            </details>
                        ` : ''}
                    </div>
                `).join('')}
            `).join('')}
        `}

        <h2>Screenshots</h2>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
            <div class="stat-card">
                <h4>API Endpoints</h4>
                <img src="api-endpoints.png" style="width: 100%; border-radius: 4px;" />
            </div>
            <div class="stat-card">
                <h4>Swagger Header</h4>
                <img src="swagger-header.png" style="width: 100%; border-radius: 4px;" />
            </div>
            <div class="stat-card">
                <h4>Final State</h4>
                <img src="final-state.png" style="width: 100%; border-radius: 4px;" />
            </div>
        </div>
    </div>
</body>
</html>
    `;
  }
});