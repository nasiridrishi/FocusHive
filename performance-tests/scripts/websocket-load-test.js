#!/usr/bin/env node

/**
 * WebSocket Load Testing Script for FocusHive Real-time Features
 * 
 * Tests WebSocket performance including:
 * - Connection establishment
 * - Message throughput
 * - Latency measurements
 * - Connection stability
 * - Memory usage tracking
 */

const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');

// Configuration
const DEFAULT_CONFIG = {
    url: 'ws://localhost:8080/ws',
    clients: 50,
    duration: 300, // 5 minutes
    messageInterval: 1000, // 1 second
    connectInterval: 100, // 100ms between connections
    output: 'websocket-results.json',
    verbose: false
};

class WebSocketLoadTester {
    constructor(config) {
        this.config = { ...DEFAULT_CONFIG, ...config };
        this.clients = [];
        this.metrics = {
            connections: {
                attempted: 0,
                successful: 0,
                failed: 0,
                concurrent: 0
            },
            messages: {
                sent: 0,
                received: 0,
                errors: 0
            },
            latency: {
                min: Infinity,
                max: 0,
                sum: 0,
                count: 0,
                samples: []
            },
            throughput: {
                messagesPerSecond: 0,
                bytesPerSecond: 0
            },
            errors: [],
            timeline: []
        };
        this.startTime = null;
        this.endTime = null;
        this.isRunning = false;
    }

    log(message) {
        if (this.config.verbose) {
            console.log(`[${new Date().toISOString()}] ${message}`);
        }
    }

    recordMetric(timestamp, metric, value) {
        this.metrics.timeline.push({
            timestamp,
            metric,
            value
        });
    }

    async createClient(clientId) {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(this.config.url);
            const client = {
                id: clientId,
                ws,
                connected: false,
                messagesSent: 0,
                messagesReceived: 0,
                lastPing: null,
                connectTime: null
            };

            this.metrics.connections.attempted++;

            ws.on('open', () => {
                client.connected = true;
                client.connectTime = Date.now();
                this.metrics.connections.successful++;
                this.metrics.connections.concurrent++;
                
                this.log(`Client ${clientId} connected`);
                this.recordMetric(Date.now(), 'connection', 1);
                
                resolve(client);
            });

            ws.on('message', (data) => {
                client.messagesReceived++;
                this.metrics.messages.received++;
                
                try {
                    const message = JSON.parse(data);
                    
                    // Calculate latency for ping/pong messages
                    if (message.type === 'pong' && client.lastPing) {
                        const latency = Date.now() - client.lastPing;
                        this.updateLatencyMetrics(latency);
                        client.lastPing = null;
                    }
                } catch (e) {
                    // Non-JSON message, ignore for latency calculation
                }
            });

            ws.on('error', (error) => {
                this.metrics.errors.push({
                    clientId,
                    timestamp: Date.now(),
                    error: error.message
                });
                this.metrics.connections.failed++;
                this.log(`Client ${clientId} error: ${error.message}`);
                reject(error);
            });

            ws.on('close', () => {
                if (client.connected) {
                    this.metrics.connections.concurrent--;
                    client.connected = false;
                    this.log(`Client ${clientId} disconnected`);
                    this.recordMetric(Date.now(), 'disconnection', -1);
                }
            });

            this.clients.push(client);

            // Timeout for connection
            setTimeout(() => {
                if (!client.connected) {
                    this.metrics.connections.failed++;
                    reject(new Error('Connection timeout'));
                }
            }, 10000);
        });
    }

    updateLatencyMetrics(latency) {
        this.metrics.latency.min = Math.min(this.metrics.latency.min, latency);
        this.metrics.latency.max = Math.max(this.metrics.latency.max, latency);
        this.metrics.latency.sum += latency;
        this.metrics.latency.count++;
        
        // Keep last 1000 samples
        this.metrics.latency.samples.push(latency);
        if (this.metrics.latency.samples.length > 1000) {
            this.metrics.latency.samples.shift();
        }
    }

    async sendMessage(client, message) {
        if (!client.connected || client.ws.readyState !== WebSocket.OPEN) {
            return false;
        }

        try {
            const messageData = JSON.stringify(message);
            client.ws.send(messageData);
            client.messagesSent++;
            this.metrics.messages.sent++;
            
            // Track ping messages for latency calculation
            if (message.type === 'ping') {
                client.lastPing = Date.now();
            }
            
            return true;
        } catch (error) {
            this.metrics.messages.errors++;
            this.metrics.errors.push({
                clientId: client.id,
                timestamp: Date.now(),
                error: error.message,
                operation: 'send'
            });
            return false;
        }
    }

    async sendPeriodicMessages() {
        const interval = setInterval(() => {
            if (!this.isRunning) {
                clearInterval(interval);
                return;
            }

            this.clients.forEach(async (client) => {
                if (client.connected) {
                    // Send different types of messages to simulate real usage
                    const messageTypes = [
                        { type: 'ping', timestamp: Date.now() },
                        { type: 'presence_update', status: 'online', activity: 'Working' },
                        { type: 'heartbeat', userId: `user_${client.id}` },
                        { type: 'session_update', sessionId: `session_${client.id}`, status: 'active' }
                    ];
                    
                    const randomMessage = messageTypes[Math.floor(Math.random() * messageTypes.length)];
                    await this.sendMessage(client, randomMessage);
                }
            });
        }, this.config.messageInterval);
    }

    calculateThroughput() {
        if (!this.startTime) return;
        
        const elapsed = (Date.now() - this.startTime) / 1000; // seconds
        this.metrics.throughput.messagesPerSecond = this.metrics.messages.sent / elapsed;
        
        // Estimate bytes (assuming average message size of 100 bytes)
        this.metrics.throughput.bytesPerSecond = this.metrics.throughput.messagesPerSecond * 100;
    }

    async connectClients() {
        this.log(`Connecting ${this.config.clients} clients...`);
        
        const connectionPromises = [];
        
        for (let i = 0; i < this.config.clients; i++) {
            const promise = this.createClient(i)
                .catch(error => {
                    this.log(`Failed to connect client ${i}: ${error.message}`);
                    return null;
                });
            
            connectionPromises.push(promise);
            
            // Stagger connections to avoid overwhelming the server
            if (i < this.config.clients - 1) {
                await new Promise(resolve => setTimeout(resolve, this.config.connectInterval));
            }
        }
        
        const results = await Promise.allSettled(connectionPromises);
        const successfulClients = results.filter(result => result.status === 'fulfilled' && result.value).length;
        
        this.log(`Connected ${successfulClients}/${this.config.clients} clients`);
        return successfulClients;
    }

    async runTest() {
        console.log('Starting WebSocket load test...');
        console.log(`URL: ${this.config.url}`);
        console.log(`Clients: ${this.config.clients}`);
        console.log(`Duration: ${this.config.duration}s`);
        console.log(`Message interval: ${this.config.messageInterval}ms`);
        
        this.startTime = Date.now();
        this.isRunning = true;
        
        // Connect clients
        const connectedClients = await this.connectClients();
        
        if (connectedClients === 0) {
            throw new Error('No clients could connect');
        }
        
        // Start sending periodic messages
        this.sendPeriodicMessages();
        
        // Monitor metrics
        const metricsInterval = setInterval(() => {
            this.calculateThroughput();
            
            if (this.config.verbose) {
                console.log(`Connections: ${this.metrics.connections.concurrent}, Messages sent: ${this.metrics.messages.sent}, Messages received: ${this.metrics.messages.received}`);
            }
        }, 5000);
        
        // Run for specified duration
        await new Promise(resolve => setTimeout(resolve, this.config.duration * 1000));
        
        // Cleanup
        this.isRunning = false;
        clearInterval(metricsInterval);
        
        this.endTime = Date.now();
        
        // Close all connections
        this.clients.forEach(client => {
            if (client.ws.readyState === WebSocket.OPEN) {
                client.ws.close();
            }
        });
        
        // Wait for cleanup
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        this.calculateFinalMetrics();
        
        console.log('WebSocket load test completed');
    }

    calculateFinalMetrics() {
        // Calculate final throughput
        this.calculateThroughput();
        
        // Calculate latency statistics
        if (this.metrics.latency.count > 0) {
            this.metrics.latency.average = this.metrics.latency.sum / this.metrics.latency.count;
            
            // Calculate percentiles
            const sortedSamples = [...this.metrics.latency.samples].sort((a, b) => a - b);
            if (sortedSamples.length > 0) {
                this.metrics.latency.p50 = sortedSamples[Math.floor(sortedSamples.length * 0.5)];
                this.metrics.latency.p95 = sortedSamples[Math.floor(sortedSamples.length * 0.95)];
                this.metrics.latency.p99 = sortedSamples[Math.floor(sortedSamples.length * 0.99)];
            }
        }
        
        // Calculate test duration
        this.metrics.testDuration = this.endTime - this.startTime;
        
        // Calculate success rates
        this.metrics.connectionSuccessRate = this.metrics.connections.attempted > 0 
            ? (this.metrics.connections.successful / this.metrics.connections.attempted) * 100 
            : 0;
        
        this.metrics.messageSuccessRate = this.metrics.messages.sent > 0
            ? ((this.metrics.messages.sent - this.metrics.messages.errors) / this.metrics.messages.sent) * 100
            : 0;
    }

    generateReport() {
        const report = {
            testConfig: this.config,
            testResults: {
                startTime: this.startTime,
                endTime: this.endTime,
                duration: this.metrics.testDuration,
                ...this.metrics
            },
            summary: {
                connectionsSuccessful: this.metrics.connections.successful,
                connectionSuccessRate: `${this.metrics.connectionSuccessRate.toFixed(2)}%`,
                messagesPerSecond: this.metrics.throughput.messagesPerSecond.toFixed(2),
                averageLatency: this.metrics.latency.average ? `${this.metrics.latency.average.toFixed(2)}ms` : 'N/A',
                p95Latency: this.metrics.latency.p95 ? `${this.metrics.latency.p95.toFixed(2)}ms` : 'N/A',
                errorCount: this.metrics.errors.length
            }
        };
        
        return report;
    }

    async saveResults() {
        const report = this.generateReport();
        
        try {
            // Ensure output directory exists
            const outputDir = path.dirname(this.config.output);
            if (!fs.existsSync(outputDir)) {
                fs.mkdirSync(outputDir, { recursive: true });
            }
            
            // Write results
            fs.writeFileSync(this.config.output, JSON.stringify(report, null, 2));
            console.log(`Results saved to ${this.config.output}`);
            
            // Print summary
            console.log('\n=== WebSocket Load Test Results ===');
            console.log(`Successful connections: ${report.summary.connectionsSuccessful}/${this.config.clients} (${report.summary.connectionSuccessRate})`);
            console.log(`Messages per second: ${report.summary.messagesPerSecond}`);
            console.log(`Average latency: ${report.summary.averageLatency}`);
            console.log(`95th percentile latency: ${report.summary.p95Latency}`);
            console.log(`Total errors: ${report.summary.errorCount}`);
            
        } catch (error) {
            console.error(`Failed to save results: ${error.message}`);
        }
    }
}

// CLI Interface
async function main() {
    const args = process.argv.slice(2);
    const config = {};
    
    for (let i = 0; i < args.length; i += 2) {
        const key = args[i];
        const value = args[i + 1];
        
        switch (key) {
            case '--url':
                config.url = value;
                break;
            case '--clients':
                config.clients = parseInt(value);
                break;
            case '--duration':
                config.duration = parseInt(value);
                break;
            case '--message-interval':
                config.messageInterval = parseInt(value);
                break;
            case '--output':
                config.output = value;
                break;
            case '--verbose':
                config.verbose = true;
                i--; // No value for this flag
                break;
            case '--help':
                console.log(`
Usage: node websocket-load-test.js [options]

Options:
  --url URL                WebSocket URL (default: ${DEFAULT_CONFIG.url})
  --clients NUM            Number of clients (default: ${DEFAULT_CONFIG.clients})
  --duration SECONDS       Test duration (default: ${DEFAULT_CONFIG.duration})
  --message-interval MS    Message interval (default: ${DEFAULT_CONFIG.messageInterval})
  --output FILE            Output file (default: ${DEFAULT_CONFIG.output})
  --verbose                Verbose logging
  --help                   Show this help
`);
                process.exit(0);
        }
    }
    
    const tester = new WebSocketLoadTester(config);
    
    try {
        await tester.runTest();
        await tester.saveResults();
    } catch (error) {
        console.error(`Test failed: ${error.message}`);
        process.exit(1);
    }
}

// Run if called directly
if (require.main === module) {
    main().catch(console.error);
}

module.exports = WebSocketLoadTester;