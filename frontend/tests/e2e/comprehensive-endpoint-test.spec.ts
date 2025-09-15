import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test.describe('Comprehensive Swagger API Endpoint Testing', () => {
  const swaggerUrl = 'http://localhost:8081/swagger-ui/index.html';
  const apiDocsUrl = 'http://localhost:8081/v3/api-docs';
  const baseUrl = 'http://localhost:8081';
  const reportPath = 'test-results/comprehensive-endpoint-report';
  let swaggerSpec: any;
  let testResults: Array<{
    endpoint: string;
    method: string;
    status: 'passed' | 'failed' | 'skipped';
    responseCode?: number;
    error?: string;
    duration?: number;
    authenticated?: boolean;
  }> = [];

  test.beforeAll(async () => {
    // Create report directory
    if (!fs.existsSync(reportPath)) {
      fs.mkdirSync(reportPath, { recursive: true });
    }

    // Fetch and parse Swagger spec
    try {
      const response = await fetch(apiDocsUrl);
      swaggerSpec = await response.json();
      console.log(`📋 Loaded Swagger spec with ${Object.keys(swaggerSpec.paths).length} endpoints`);
    } catch (error) {
      console.error('Failed to fetch Swagger spec:', error);
      throw error;
    }
  });

  test.afterAll(async () => {
    // Generate comprehensive report
    const report = generateComprehensiveReport(testResults, swaggerSpec);
    fs.writeFileSync(path.join(reportPath, 'comprehensive-endpoint-report.html'), report);
    fs.writeFileSync(path.join(reportPath, 'endpoint-results.json'), JSON.stringify(testResults, null, 2));

    console.log(`\n📊 Comprehensive endpoint test report generated at: ${reportPath}/comprehensive-endpoint-report.html`);
    console.log(`📈 Tested ${testResults.length} endpoint operations`);
    console.log(`✅ Passed: ${testResults.filter(r => r.status === 'passed').length}`);
    console.log(`❌ Failed: ${testResults.filter(r => r.status === 'failed').length}`);
    console.log(`⏭️  Skipped: ${testResults.filter(r => r.status === 'skipped').length}`);
  });

  test('Test All API Endpoints Comprehensively', async ({ page, request }) => {
    console.log('🚀 Starting comprehensive endpoint testing...\n');

    // Get authentication token for protected endpoints
    let authToken: string | null = null;
    try {
      // Try to login first
      const loginResponse = await request.post(`${baseUrl}/api/v1/auth/login`, {
        data: {
          usernameOrEmail: 'test@example.com',
          password: 'TestPassword123!'
        }
      });

      if (loginResponse.ok()) {
        const loginData = await loginResponse.json();
        authToken = loginData.accessToken;
        console.log('✅ Authentication successful');
      } else {
        console.log('⚠️  Authentication failed, testing public endpoints only');
      }
    } catch (error) {
      console.log('⚠️  Authentication not available, testing public endpoints only');
    }

    // Test each endpoint
    for (const [endpointPath, methods] of Object.entries(swaggerSpec.paths)) {
      for (const [method, operation] of Object.entries(methods as Record<string, any>)) {
        const fullUrl = `${baseUrl}${endpointPath}`;
        const startTime = Date.now();

        try {
          // Check if endpoint requires authentication
          const requiresAuth = operation.security && operation.security.length > 0;
          const headers: Record<string, string> = {
            'Content-Type': 'application/json'
          };

          if (requiresAuth && authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
          }

          // Prepare request data based on operation
          let requestData: any = {};
          let queryParams: Record<string, string> = {};

          if (operation.parameters) {
            for (const param of operation.parameters) {
              if (param.in === 'path' && param.required) {
                // Replace path parameters with test values
                const testValue = getTestValueForParameter(param);
                fullUrl.replace(`{${param.name}}`, testValue);
              } else if (param.in === 'query') {
                queryParams[param.name] = getTestValueForParameter(param);
              }
            }
          }

          if (operation.requestBody && operation.requestBody.content) {
            const contentType = Object.keys(operation.requestBody.content)[0];
            if (contentType === 'application/json') {
              const schema = operation.requestBody.content[contentType].schema;
              requestData = generateTestDataFromSchema(schema);
            }
          }

          // Make the request
          let response;
          const requestOptions: any = {
            headers,
            data: Object.keys(requestData).length > 0 ? requestData : undefined
          };

          // Add query parameters to URL
          const url = new URL(fullUrl);
          Object.entries(queryParams).forEach(([key, value]) => {
            url.searchParams.append(key, value);
          });

          switch (method.toUpperCase()) {
            case 'GET':
              response = await request.get(url.toString(), { headers });
              break;
            case 'POST':
              response = await request.post(url.toString(), requestOptions);
              break;
            case 'PUT':
              response = await request.put(url.toString(), requestOptions);
              break;
            case 'DELETE':
              response = await request.delete(url.toString(), { headers });
              break;
            case 'PATCH':
              response = await request.patch(url.toString(), requestOptions);
              break;
            default:
              console.log(`⏭️  Skipping unsupported method: ${method.toUpperCase()} ${endpointPath}`);
              testResults.push({
                endpoint: endpointPath,
                method: method.toUpperCase(),
                status: 'skipped',
                error: 'Unsupported HTTP method'
              });
              continue;
          }

          const duration = Date.now() - startTime;
          const statusCode = response.status();

          // Determine test result
          let status: 'passed' | 'failed' | 'skipped' = 'passed';
          let error: string | undefined;

          if (requiresAuth && !authToken) {
            status = 'skipped';
            error = 'Authentication required but no token available';
          } else if (statusCode >= 400 && statusCode !== 401 && statusCode !== 403) {
            // 401/403 are expected for unauthenticated requests to protected endpoints
            status = 'failed';
            error = `Unexpected status code: ${statusCode}`;
          } else if (statusCode >= 200 && statusCode < 300) {
            status = 'passed';
          } else if (statusCode === 401 || statusCode === 403) {
            if (requiresAuth) {
              status = 'passed'; // Expected for protected endpoints without auth
            } else {
              status = 'failed';
              error = `Unexpected authentication error on public endpoint: ${statusCode}`;
            }
          }

          testResults.push({
            endpoint: endpointPath,
            method: method.toUpperCase(),
            status,
            responseCode: statusCode,
            error,
            duration,
            authenticated: !!authToken && requiresAuth
          });

          console.log(`${status === 'passed' ? '✅' : status === 'failed' ? '❌' : '⏭️'} ${method.toUpperCase()} ${endpointPath} - ${statusCode} (${duration}ms)`);

        } catch (error) {
          const duration = Date.now() - startTime;
          const errorMessage = error instanceof Error ? error.message : String(error);
          testResults.push({
            endpoint: endpointPath,
            method: method.toUpperCase(),
            status: 'failed',
            error: errorMessage,
            duration
          });
          console.log(`❌ ${method.toUpperCase()} ${endpointPath} - Error: ${errorMessage} (${duration}ms)`);
        }
      }
    }
  });

  // Helper functions
  function getTestValueForParameter(param: any): string {
    // Generate appropriate test values based on parameter schema
    if (param.schema && param.schema.type) {
      switch (param.schema.type) {
        case 'string':
          if (param.schema.format === 'uuid') {
            return '550e8400-e29b-41d4-a716-446655440000';
          }
          return 'test-value';
        case 'integer':
          return '123';
        case 'boolean':
          return 'true';
        default:
          return 'test-value';
      }
    }
    return 'test-value';
  }

  function generateTestDataFromSchema(schema: any): any {
    if (!schema) return {};

    if (schema.type === 'object' && schema.properties) {
      const result: any = {};
      for (const [propName, propSchema] of Object.entries(schema.properties as any)) {
        if (schema.required && schema.required.includes(propName)) {
          result[propName] = generateTestDataFromSchema(propSchema);
        }
      }
      return result;
    }

    switch (schema.type) {
      case 'string':
        if (schema.format === 'uuid') {
          return '550e8400-e29b-41d4-a716-446655440000';
        }
        if (schema.enum && schema.enum.length > 0) {
          return schema.enum[0];
        }
        return 'test-string';
      case 'integer':
      case 'number':
        return schema.minimum || 1;
      case 'boolean':
        return true;
      case 'array':
        return [generateTestDataFromSchema(schema.items)];
      default:
        return 'test-value';
    }
  }

  function generateComprehensiveReport(results: any[], spec: any): string {
    const passed = results.filter(r => r.status === 'passed').length;
    const failed = results.filter(r => r.status === 'failed').length;
    const skipped = results.filter(r => r.status === 'skipped').length;
    const total = results.length;

    const endpointStats = results.reduce((acc, result) => {
      const key = `${result.method} ${result.endpoint}`;
      if (!acc[key]) {
        acc[key] = { passed: 0, failed: 0, skipped: 0 };
      }
      acc[key][result.status]++;
      return acc;
    }, {} as Record<string, { passed: number; failed: number; skipped: number }>);

    return `
<!DOCTYPE html>
<html>
<head>
    <title>Comprehensive API Endpoint Test Report</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
        }
        .container { max-width: 1400px; margin: 0 auto; }
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
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            text-align: center;
        }
        .stat-value {
            font-size: 36px;
            font-weight: bold;
            margin: 10px 0;
        }
        .passed { color: #28a745; }
        .failed { color: #dc3545; }
        .skipped { color: #ffc107; }
        .endpoint-card {
            background: white;
            padding: 15px;
            margin-bottom: 10px;
            border-radius: 8px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            border-left: 4px solid #ddd;
        }
        .endpoint-passed { border-left-color: #28a745; }
        .endpoint-failed { border-left-color: #dc3545; }
        .endpoint-skipped { border-left-color: #ffc107; }
        .method-badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            color: white;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
            margin-right: 10px;
        }
        .method-get { background: #61affe; }
        .method-post { background: #49cc90; }
        .method-put { background: #fca130; }
        .method-delete { background: #f93e3e; }
        .method-patch { background: #50e3c2; }
        .status-badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            color: white;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }
        .status-passed { background: #28a745; }
        .status-failed { background: #dc3545; }
        .status-skipped { background: #ffc107; }
        .details { margin-top: 10px; font-size: 14px; color: #666; }
        .error { color: #dc3545; font-weight: bold; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔍 Comprehensive API Endpoint Test Report</h1>
            <p>FocusHive Identity Service - ${spec.info.title} ${spec.info.version}</p>
            <p>Generated: ${new Date().toLocaleString()}</p>
        </div>

        <div class="summary">
            <div class="stat-card">
                <div>Total Endpoints Tested</div>
                <div class="stat-value">${total}</div>
            </div>
            <div class="stat-card">
                <div class="passed">Passed</div>
                <div class="stat-value passed">${passed}</div>
            </div>
            <div class="stat-card">
                <div class="failed">Failed</div>
                <div class="stat-value failed">${failed}</div>
            </div>
            <div class="stat-card">
                <div class="skipped">Skipped</div>
                <div class="stat-value skipped">${skipped}</div>
            </div>
        </div>

        <h2>Test Results by Endpoint</h2>
        ${Object.entries(endpointStats).map(([endpoint, stats]) => `
            <div class="endpoint-card endpoint-${stats.failed > 0 ? 'failed' : stats.passed > 0 ? 'passed' : 'skipped'}">
                <h3>
                    <span class="method-badge method-${endpoint.split(' ')[0].toLowerCase()}">${endpoint.split(' ')[0]}</span>
                    ${endpoint}
                </h3>
                <div>
                    <span class="status-badge status-passed">${stats.passed} passed</span>
                    <span class="status-badge status-failed">${stats.failed} failed</span>
                    <span class="status-badge status-skipped">${stats.skipped} skipped</span>
                </div>
            </div>
        `).join('')}

        <h2>Detailed Results</h2>
        ${results.map(result => `
            <div class="endpoint-card endpoint-${result.status}">
                <h4>
                    <span class="method-badge method-${result.method.toLowerCase()}">${result.method}</span>
                    ${result.endpoint}
                    <span class="status-badge status-${result.status}" style="float: right;">${result.status}</span>
                </h4>
                <div class="details">
                    ${result.responseCode ? `<strong>Response Code:</strong> ${result.responseCode} | ` : ''}
                    ${result.duration ? `<strong>Duration:</strong> ${result.duration}ms | ` : ''}
                    ${result.authenticated !== undefined ? `<strong>Authenticated:</strong> ${result.authenticated ? 'Yes' : 'No'}` : ''}
                    ${result.error ? `<br><span class="error"><strong>Error:</strong> ${result.error}</span>` : ''}
                </div>
            </div>
        `).join('')}
    </div>
</body>
</html>
    `;
  }
});