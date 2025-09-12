#!/usr/bin/env node

/**
 * Security Headers Test Script
 * Tests that all required security headers are present and correctly configured
 * 
 * Usage:
 *   node test-security-headers.js [URL]
 *   
 * If no URL provided, tests http://localhost:5173 (Vite dev server)
 */

import https from 'https';
import http from 'http';
import { URL } from 'url';

// Configuration
const testUrl = process.argv[2] || 'http://localhost:5173';
const expectedHeaders = {
    'x-frame-options': {
        expected: 'DENY',
        description: 'Prevents clickjacking attacks'
    },
    'x-content-type-options': {
        expected: 'nosniff',
        description: 'Prevents MIME type sniffing'
    },
    'x-xss-protection': {
        expected: '1; mode=block',
        description: 'Enables XSS protection'
    },
    'referrer-policy': {
        expected: 'strict-origin-when-cross-origin',
        description: 'Controls referrer information'
    },
    'cross-origin-opener-policy': {
        expected: 'same-origin',
        description: 'Prevents cross-origin attacks'
    },
    'cross-origin-embedder-policy': {
        expected: 'credentialless',
        description: 'Cross-origin isolation'
    },
    'cross-origin-resource-policy': {
        expected: 'same-origin',
        description: 'Resource sharing control'
    },
    'content-security-policy': {
        contains: ['default-src', "'self'", 'script-src'],
        description: 'Prevents XSS and code injection'
    },
    'permissions-policy': {
        contains: ['geolocation=()', 'microphone=(self)', 'camera=(self)'],
        description: 'Restricts browser APIs'
    }
};

// HSTS is only expected on HTTPS
if (testUrl.startsWith('https://')) {
    expectedHeaders['strict-transport-security'] = {
        contains: ['max-age='],
        description: 'Forces HTTPS connections'
    };
}

function testSecurityHeaders(url) {
    return new Promise((resolve, reject) => {
        const urlObj = new URL(url);
        const isHttps = urlObj.protocol === 'https:';
        const httpModule = isHttps ? https : http;
        
        const options = {
            hostname: urlObj.hostname,
            port: urlObj.port || (isHttps ? 443 : 80),
            path: urlObj.pathname,
            method: 'HEAD',
            headers: {
                'User-Agent': 'Security-Headers-Test/1.0'
            }
        };

        console.log(`\n🔍 Testing security headers for: ${url}`);
        console.log('=' .repeat(60));

        const req = httpModule.request(options, (res) => {
            const headers = {};
            
            // Normalize header names to lowercase
            Object.keys(res.headers).forEach(key => {
                headers[key.toLowerCase()] = res.headers[key];
            });

            console.log(`\n📊 Response Status: ${res.statusCode}`);
            console.log(`📡 Server: ${headers.server || 'Unknown'}`);
            
            let passedTests = 0;
            let totalTests = 0;

            console.log('\n🛡️  Security Headers Analysis:');
            console.log('-'.repeat(60));

            Object.keys(expectedHeaders).forEach(headerName => {
                totalTests++;
                const config = expectedHeaders[headerName];
                const actualValue = headers[headerName];
                
                if (!actualValue) {
                    console.log(`❌ ${headerName.toUpperCase()}`);
                    console.log(`   Missing: ${config.description}`);
                    return;
                }

                let passed = false;
                
                if (config.expected) {
                    passed = actualValue === config.expected;
                    if (passed) {
                        console.log(`✅ ${headerName.toUpperCase()}`);
                        console.log(`   Value: ${actualValue}`);
                    } else {
                        console.log(`⚠️  ${headerName.toUpperCase()}`);
                        console.log(`   Expected: ${config.expected}`);
                        console.log(`   Actual: ${actualValue}`);
                    }
                } else if (config.contains) {
                    passed = config.contains.every(substring => 
                        actualValue.toLowerCase().includes(substring.toLowerCase())
                    );
                    if (passed) {
                        console.log(`✅ ${headerName.toUpperCase()}`);
                        console.log(`   Contains required values: ${config.contains.join(', ')}`);
                    } else {
                        console.log(`⚠️  ${headerName.toUpperCase()}`);
                        console.log(`   Should contain: ${config.contains.join(', ')}`);
                        console.log(`   Actual: ${actualValue}`);
                    }
                }
                
                if (passed) passedTests++;
            });

            console.log('\n📈 Additional Headers Found:');
            console.log('-'.repeat(60));
            
            const additionalSecurityHeaders = [
                'server', 'cache-control', 'pragma', 'expires', 
                'vary', 'content-encoding'
            ];
            
            additionalSecurityHeaders.forEach(header => {
                if (headers[header]) {
                    console.log(`ℹ️  ${header.toUpperCase()}: ${headers[header]}`);
                }
            });

            console.log('\n📊 Test Summary:');
            console.log('='.repeat(60));
            console.log(`✅ Passed: ${passedTests}/${totalTests} tests`);
            console.log(`⚠️  Issues: ${totalTests - passedTests} headers need attention`);
            
            const score = (passedTests / totalTests) * 100;
            console.log(`🎯 Security Score: ${score.toFixed(1)}%`);
            
            if (score >= 90) {
                console.log('🚀 Excellent security configuration!');
            } else if (score >= 70) {
                console.log('👍 Good security configuration, minor improvements needed');
            } else if (score >= 50) {
                console.log('⚠️  Security configuration needs improvement');
            } else {
                console.log('🚨 Security configuration requires immediate attention');
            }

            resolve({ passedTests, totalTests, score, headers: res.headers });
        });

        req.on('error', (error) => {
            console.error(`❌ Error testing ${url}:`, error.message);
            
            if (error.code === 'ECONNREFUSED') {
                console.log('\n💡 Tip: Make sure your development server is running:');
                console.log('   npm run dev');
            }
            
            reject(error);
        });

        req.setTimeout(10000, () => {
            req.destroy();
            reject(new Error('Request timeout'));
        });

        req.end();
    });
}

// Run the test
testSecurityHeaders(testUrl).catch(error => {
    console.error('\n❌ Test failed:', error.message);
    process.exit(1);
});