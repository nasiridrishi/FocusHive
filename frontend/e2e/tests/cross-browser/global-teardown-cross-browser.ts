/**
 * Global Teardown for Cross-Browser Testing
 * Cleans up test environment and generates final reports
 */

import { FullConfig } from '@playwright/test';
import * as fs from 'fs/promises';
import * as path from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

interface TestResult {
  browser: string;
  passed: number;
  failed: number;
  skipped: number;
  total: number;
  duration: number;
}

interface CompatibilityReport {
  timestamp: string;
  duration: number;
  summary: {
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    successRate: number;
  };
  browsers: { [key: string]: TestResult };
  features: { [key: string]: { [browser: string]: boolean } };
  issues: Array<{
    browser: string;
    category: string;
    description: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
  }>;
  recommendations: string[];
}

async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting Cross-Browser Test Environment Teardown...');

  const setupTime = process.env.CROSS_BROWSER_SETUP_TIME || Date.now().toString();
  const totalDuration = Date.now() - parseInt(setupTime);

  try {
    // Collect test results from all browsers
    console.log('📊 Collecting test results...');
    const testResults: TestResult[] = [];
    const compatibilityIssues: Array<{
      browser: string;
      category: string;
      description: string;
      severity: 'low' | 'medium' | 'high' | 'critical';
    }> = [];

    // Read test results from various browsers
    const browserResults = {
      chrome: await collectBrowserResults('chrome'),
      firefox: await collectBrowserResults('firefox'),
      safari: await collectBrowserResults('safari'),
      edge: await collectBrowserResults('edge')
    };

    // Analyze feature compatibility
    console.log('🔍 Analyzing feature compatibility...');
    const featureCompatibility = await analyzeFeatureCompatibility();

    // Generate compatibility issues list
    const issues = await identifyCompatibilityIssues(browserResults, featureCompatibility);
    compatibilityIssues.push(...issues);

    // Calculate overall statistics
    const totalTests = Object.values(browserResults).reduce((sum, result) => sum + result.total, 0);
    const totalPassed = Object.values(browserResults).reduce((sum, result) => sum + result.passed, 0);
    const totalFailed = Object.values(browserResults).reduce((sum, result) => sum + result.failed, 0);
    const totalSkipped = Object.values(browserResults).reduce((sum, result) => sum + result.skipped, 0);
    const successRate = totalTests > 0 ? (totalPassed / totalTests) * 100 : 0;

    // Generate recommendations
    const recommendations = generateRecommendations(compatibilityIssues, featureCompatibility);

    // Create comprehensive compatibility report
    const compatibilityReport: CompatibilityReport = {
      timestamp: new Date().toISOString(),
      duration: totalDuration,
      summary: {
        totalTests,
        passed: totalPassed,
        failed: totalFailed,
        skipped: totalSkipped,
        successRate: Math.round(successRate * 100) / 100
      },
      browsers: browserResults,
      features: featureCompatibility,
      issues: compatibilityIssues,
      recommendations
    };

    // Write final compatibility report
    await fs.writeFile(
      'cross-browser-compatibility-report.json',
      JSON.stringify(compatibilityReport, null, 2)
    );

    // Generate HTML report
    await generateHTMLReport(compatibilityReport);

    // Generate markdown summary
    await generateMarkdownSummary(compatibilityReport);

    // Create CSV export for external analysis
    await generateCSVReport(compatibilityReport);

    // Clean up temporary files
    console.log('🗑️  Cleaning up temporary files...');
    await cleanupTempFiles();

    // Generate final console summary
    printFinalSummary(compatibilityReport);

    console.log('✅ Cross-Browser Test Environment Teardown Complete');

  } catch (error) {
    console.error('❌ Error during teardown:', error);
    throw error;
  }
}

async function collectBrowserResults(browser: string): Promise<TestResult> {
  try {
    // Look for test results in common locations
    const possiblePaths = [
      `test-results/${browser}-results.json`,
      `playwright-report/${browser}.json`,
      `${browser}-test-results.json`
    ];

    for (const testPath of possiblePaths) {
      try {
        const resultData = await fs.readFile(testPath, 'utf8');
        const results = JSON.parse(resultData);
        
        return {
          browser,
          passed: results.passed || 0,
          failed: results.failed || 0,
          skipped: results.skipped || 0,
          total: results.total || 0,
          duration: results.duration || 0
        };
      } catch {
        // File doesn't exist or is invalid, try next path
        continue;
      }
    }

    // If no results file found, return empty results
    return {
      browser,
      passed: 0,
      failed: 0,
      skipped: 0,
      total: 0,
      duration: 0
    };
  } catch (error) {
    console.warn(`Failed to collect results for ${browser}:`, error);
    return {
      browser,
      passed: 0,
      failed: 0,
      skipped: 0,
      total: 0,
      duration: 0
    };
  }
}

async function analyzeFeatureCompatibility(): Promise<{ [key: string]: { [browser: string]: boolean } }> {
  const compatibility: { [key: string]: { [browser: string]: boolean } } = {};

  try {
    const featureMatrix = await fs.readFile('feature-detection-matrix.json', 'utf8');
    const features = JSON.parse(featureMatrix);

    // Analyze each feature category
    for (const [category, categoryFeatures] of Object.entries(features)) {
      for (const [feature, featureData] of Object.entries(categoryFeatures as Record<string, { results: { [browser: string]: boolean } }>)) {
        const featureKey = `${category}.${feature}`;
        compatibility[featureKey] = featureData.results || {
          chrome: true,
          firefox: true,
          safari: true,
          edge: true
        };
      }
    }
  } catch (error) {
    console.warn('Could not analyze feature compatibility:', error);
  }

  return compatibility;
}

async function identifyCompatibilityIssues(
  browserResults: { [browser: string]: TestResult },
  featureCompatibility: { [key: string]: { [browser: string]: boolean } }
): Promise<Array<{
  browser: string;
  category: string;
  description: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
}>> {
  const issues: Array<{
    browser: string;
    category: string;
    description: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
  }> = [];

  // Analyze test failures
  for (const [browser, results] of Object.entries(browserResults)) {
    if (results.failed > 0) {
      const failureRate = (results.failed / results.total) * 100;
      
      let severity: 'low' | 'medium' | 'high' | 'critical';
      if (failureRate > 50) severity = 'critical';
      else if (failureRate > 25) severity = 'high';
      else if (failureRate > 10) severity = 'medium';
      else severity = 'low';

      issues.push({
        browser,
        category: 'test-failures',
        description: `${results.failed} test failures (${failureRate.toFixed(1)}% failure rate)`,
        severity
      });
    }
  }

  // Analyze feature compatibility
  for (const [feature, browserSupport] of Object.entries(featureCompatibility)) {
    const unsupportedBrowsers = Object.entries(browserSupport)
      .filter(([_, supported]) => !supported)
      .map(([browser]) => browser);

    if (unsupportedBrowsers.length > 0) {
      const severity = unsupportedBrowsers.length >= 3 ? 'high' : 'medium';
      
      unsupportedBrowsers.forEach(browser => {
        issues.push({
          browser,
          category: 'feature-compatibility',
          description: `${feature} not supported`,
          severity
        });
      });
    }
  }

  // Known browser-specific issues
  const knownIssues = [
    {
      browser: 'safari',
      category: 'websocket',
      description: 'WebSocket connections may be throttled in background tabs',
      severity: 'medium' as const
    },
    {
      browser: 'safari',
      category: 'indexeddb',
      description: 'IndexedDB may be disabled in private browsing mode',
      severity: 'medium' as const
    },
    {
      browser: 'firefox',
      category: 'media',
      description: 'AVIF image format not supported',
      severity: 'low' as const
    },
    {
      browser: 'edge',
      category: 'legacy',
      description: 'Legacy Edge versions may have compatibility issues',
      severity: 'low' as const
    }
  ];

  issues.push(...knownIssues);

  return issues;
}

function generateRecommendations(
  issues: Array<{ browser: string; category: string; description: string; severity: string }>,
  featureCompatibility: { [key: string]: { [browser: string]: boolean } }
): string[] {
  const recommendations: string[] = [];

  // Critical issues
  const criticalIssues = issues.filter(issue => issue.severity === 'critical');
  if (criticalIssues.length > 0) {
    recommendations.push('Address critical compatibility issues immediately before deployment');
  }

  // Feature-specific recommendations
  const unsupportedFeatures = Object.entries(featureCompatibility)
    .filter(([_, support]) => Object.values(support).includes(false));

  if (unsupportedFeatures.length > 0) {
    recommendations.push('Implement polyfills or fallbacks for unsupported features');
    recommendations.push('Consider progressive enhancement for advanced features');
  }

  // Browser-specific recommendations
  const safariIssues = issues.filter(issue => issue.browser === 'safari').length;
  if (safariIssues > 0) {
    recommendations.push('Test thoroughly on Safari/WebKit due to specific limitations');
  }

  const firefoxIssues = issues.filter(issue => issue.browser === 'firefox').length;
  if (firefoxIssues > 0) {
    recommendations.push('Verify Firefox-specific behaviors and vendor prefixes');
  }

  // General recommendations
  recommendations.push('Test on real devices in addition to browser automation');
  recommendations.push('Monitor browser console for warnings and errors');
  recommendations.push('Keep browser compatibility matrix updated with new browser versions');
  recommendations.push('Consider using feature detection instead of browser detection');

  return recommendations;
}

async function generateHTMLReport(report: CompatibilityReport): Promise<void> {
  const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cross-Browser Compatibility Report</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f8f9fa; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 2px 20px rgba(0,0,0,0.1); }
        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
        h2 { color: #34495e; margin-top: 30px; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center; }
        .stat-value { font-size: 2.5em; font-weight: bold; margin-bottom: 5px; }
        .stat-label { font-size: 0.9em; opacity: 0.9; }
        .browser-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 20px 0; }
        .browser-card { border: 1px solid #e1e8ed; border-radius: 8px; padding: 20px; }
        .browser-card h3 { margin-top: 0; color: #2c3e50; }
        .passed { color: #27ae60; font-weight: bold; }
        .failed { color: #e74c3c; font-weight: bold; }
        .skipped { color: #f39c12; font-weight: bold; }
        .progress-bar { background: #ecf0f1; border-radius: 10px; overflow: hidden; height: 20px; margin: 10px 0; }
        .progress-fill { height: 100%; background: linear-gradient(90deg, #27ae60, #2ecc71); transition: width 0.3s ease; }
        .issues-list { list-style: none; padding: 0; }
        .issue-item { padding: 10px; margin: 5px 0; border-left: 4px solid #e74c3c; background: #fdedec; border-radius: 4px; }
        .issue-critical { border-left-color: #c0392b; }
        .issue-high { border-left-color: #e74c3c; }
        .issue-medium { border-left-color: #f39c12; }
        .issue-low { border-left-color: #f1c40f; }
        .recommendations { background: #e8f6f3; border: 1px solid #a3e4d7; border-radius: 8px; padding: 20px; margin: 20px 0; }
        .recommendations ul { margin: 0; }
        .feature-matrix { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; }
        .feature-item { padding: 10px; border-radius: 4px; text-align: center; }
        .supported { background: #d5edda; color: #155724; }
        .unsupported { background: #f8d7da; color: #721c24; }
        .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e1e8ed; color: #6c757d; text-align: center; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🌐 Cross-Browser Compatibility Report</h1>
        
        <div class="summary">
            <div class="stat-card">
                <div class="stat-value">${report.summary.totalTests}</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat-card" style="background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%);">
                <div class="stat-value">${report.summary.passed}</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat-card" style="background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);">
                <div class="stat-value">${report.summary.failed}</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat-card" style="background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);">
                <div class="stat-value">${report.summary.successRate.toFixed(1)}%</div>
                <div class="stat-label">Success Rate</div>
            </div>
        </div>

        <h2>📊 Browser Results</h2>
        <div class="browser-grid">
            ${Object.entries(report.browsers).map(([browser, results]) => `
                <div class="browser-card">
                    <h3>${browser.charAt(0).toUpperCase() + browser.slice(1)}</h3>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${results.total > 0 ? (results.passed / results.total) * 100 : 0}%"></div>
                    </div>
                    <p><span class="passed">${results.passed} passed</span> | <span class="failed">${results.failed} failed</span> | <span class="skipped">${results.skipped} skipped</span></p>
                    <p>Total: ${results.total} tests in ${(results.duration / 1000).toFixed(1)}s</p>
                </div>
            `).join('')}
        </div>

        ${report.issues.length > 0 ? `
        <h2>⚠️ Compatibility Issues</h2>
        <ul class="issues-list">
            ${report.issues.map(issue => `
                <li class="issue-item issue-${issue.severity}">
                    <strong>${issue.browser}</strong> - ${issue.category}: ${issue.description}
                    <span style="float: right; font-size: 0.8em; text-transform: uppercase;">${issue.severity}</span>
                </li>
            `).join('')}
        </ul>
        ` : ''}

        <div class="recommendations">
            <h2>💡 Recommendations</h2>
            <ul>
                ${report.recommendations.map(rec => `<li>${rec}</li>`).join('')}
            </ul>
        </div>

        <div class="footer">
            <p>Generated on ${new Date(report.timestamp).toLocaleString()}</p>
            <p>Test duration: ${(report.duration / 1000 / 60).toFixed(1)} minutes</p>
        </div>
    </div>
</body>
</html>`;

  await fs.writeFile('cross-browser-report.html', html);
}

async function generateMarkdownSummary(report: CompatibilityReport): Promise<void> {
  const markdown = `# Cross-Browser Compatibility Report

Generated: ${new Date(report.timestamp).toLocaleString()}
Duration: ${(report.duration / 1000 / 60).toFixed(1)} minutes

## Summary

- **Total Tests:** ${report.summary.totalTests}
- **Passed:** ${report.summary.passed} ✅
- **Failed:** ${report.summary.failed} ❌
- **Success Rate:** ${report.summary.successRate.toFixed(1)}%

## Browser Results

${Object.entries(report.browsers).map(([browser, results]) => `
### ${browser.charAt(0).toUpperCase() + browser.slice(1)}
- Passed: ${results.passed}
- Failed: ${results.failed}
- Skipped: ${results.skipped}
- Duration: ${(results.duration / 1000).toFixed(1)}s
- Success Rate: ${results.total > 0 ? ((results.passed / results.total) * 100).toFixed(1) : 0}%
`).join('')}

${report.issues.length > 0 ? `## Issues Found

${report.issues.map(issue => `- **${issue.browser}** [${issue.severity.toUpperCase()}]: ${issue.description}`).join('\n')}
` : ''}

## Recommendations

${report.recommendations.map(rec => `- ${rec}`).join('\n')}

---
*Report generated by FocusHive Cross-Browser Test Suite*`;

  await fs.writeFile('cross-browser-summary.md', markdown);
}

async function generateCSVReport(report: CompatibilityReport): Promise<void> {
  const csvData = [
    ['Browser', 'Total Tests', 'Passed', 'Failed', 'Skipped', 'Success Rate', 'Duration (s)'],
    ...Object.entries(report.browsers).map(([browser, results]) => [
      browser,
      results.total.toString(),
      results.passed.toString(),
      results.failed.toString(),
      results.skipped.toString(),
      results.total > 0 ? ((results.passed / results.total) * 100).toFixed(1) + '%' : '0%',
      (results.duration / 1000).toFixed(1)
    ])
  ];

  const csv = csvData.map(row => row.join(',')).join('\n');
  await fs.writeFile('cross-browser-results.csv', csv);
}

async function cleanupTempFiles(): Promise<void> {
  const tempFiles = [
    'cross-browser-compatibility-matrix.json',
    'test-config-summary.json',
    'performance-config.json',
    'feature-detection-matrix.json',
    'test-execution-plan.json',
    'test-data.json',
    'browser-configs.json'
  ];

  for (const file of tempFiles) {
    try {
      await fs.unlink(file);
    } catch {
      // File doesn't exist or can't be deleted, ignore
    }
  }
}

function printFinalSummary(report: CompatibilityReport): void {
  console.log('\n🎯 CROSS-BROWSER TEST SUMMARY');
  console.log('════════════════════════════════');
  console.log(`📊 Total Tests: ${report.summary.totalTests}`);
  console.log(`✅ Passed: ${report.summary.passed}`);
  console.log(`❌ Failed: ${report.summary.failed}`);
  console.log(`⏭️  Skipped: ${report.summary.skipped}`);
  console.log(`📈 Success Rate: ${report.summary.successRate.toFixed(1)}%`);
  console.log(`⏱️  Duration: ${(report.duration / 1000 / 60).toFixed(1)} minutes`);
  
  if (report.issues.length > 0) {
    console.log(`\n⚠️  Issues Found: ${report.issues.length}`);
    const criticalIssues = report.issues.filter(i => i.severity === 'critical').length;
    const highIssues = report.issues.filter(i => i.severity === 'high').length;
    
    if (criticalIssues > 0) console.log(`   🔴 Critical: ${criticalIssues}`);
    if (highIssues > 0) console.log(`   🟡 High: ${highIssues}`);
  }

  console.log('\n📁 Reports Generated:');
  console.log('   • cross-browser-report.html');
  console.log('   • cross-browser-summary.md');
  console.log('   • cross-browser-results.csv');
  console.log('   • cross-browser-compatibility-report.json');
  console.log('════════════════════════════════\n');
}

export default globalTeardown;