# FocusHive Load Testing Suite

Comprehensive performance and load testing infrastructure for the FocusHive platform, supporting multiple testing tools and methodologies.

## 🏗️ Architecture Overview

This testing suite provides multi-tool load testing capabilities:

- **k6** - Modern JavaScript-based load testing tool
- **JMeter** - Enterprise-grade Java load testing platform  
- **Gatling** - High-performance Scala-based load testing framework
- **Playwright** - Frontend performance and E2E testing integration

## 📊 Performance Targets

### Core Web Vitals (Frontend)
- **First Contentful Paint (FCP)**: < 1s
- **Largest Contentful Paint (LCP)**: < 2.5s
- **Time to Interactive (TTI)**: < 3.5s
- **Cumulative Layout Shift (CLS)**: < 0.1
- **First Input Delay (FID)**: < 100ms

### API Performance Targets
- **Response Time**: 95th percentile < 500ms, 99th percentile < 1000ms
- **Success Rate**: > 99%
- **Error Rate**: < 1%
- **Throughput**: > 100 requests/second per service

### WebSocket Performance Targets
- **Connection Time**: < 1s
- **Message Latency**: 95th percentile < 100ms, 99th percentile < 200ms
- **Connection Stability**: > 99%
- **Message Throughput**: > 50 messages/second

### System Performance Targets
- **Memory Usage**: < 100MB for typical user session
- **Bundle Size**: Initial < 500KB, Total < 2MB
- **Concurrent Users**: Support 1000+ concurrent users
- **Uptime**: 99.9% availability

## 🚀 Quick Start

### Prerequisites

1. **Node.js 18+** (for k6 and frontend tests)
2. **Java 11+** (for JMeter and Gatling)
3. **Apache JMeter** (for JMeter tests)
4. **SBT** (for Gatling tests)
5. **Running FocusHive Services** (all 8 microservices)

### Running Tests

#### k6 Tests (Recommended for CI/CD)
```bash
# Navigate to k6 directory
cd load-tests/k6

# Run API load test
k6 run api-load-test.js

# Run WebSocket test
k6 run websocket-load-test.js

# Run user journey test
k6 run user-journey-test.js

# Run stress test
k6 run stress-test.js

# Run spike test
k6 run spike-test.js

# Run soak test (long duration)
k6 run soak-test.js
```

#### JMeter Tests
```bash
# Navigate to JMeter directory
cd load-tests/jmeter

# Run all tests
./scripts/run-load-tests.sh all

# Run specific test types
./scripts/run-load-tests.sh smoke
./scripts/run-load-tests.sh load
./scripts/run-load-tests.sh stress
./scripts/run-load-tests.sh spike
```

#### Gatling Tests
```bash
# Navigate to Gatling directory
cd load-tests/gatling

# Run all tests
./run-gatling-tests.sh all

# Run specific tests
./run-gatling-tests.sh api 20 300      # 20 users, 300 seconds
./run-gatling-tests.sh stress 50 600   # 50 users, 600 seconds
./run-gatling-tests.sh websocket 15    # 15 users, default duration
```

#### Frontend Performance Tests
```bash
# Navigate to frontend directory
cd frontend

# Run all performance tests
npm run test:performance

# Run specific performance tests
npx playwright test --project=performance tests/performance/
```

## 📁 Directory Structure

```
load-tests/
├── README.md                          # This file
├── k6/                                # k6 Load Tests
│   ├── config/
│   │   └── thresholds.js              # Performance thresholds and scenarios
│   ├── utils/
│   │   └── helpers.js                 # Test utilities and helper functions
│   ├── api-load-test.js               # REST API load testing
│   ├── websocket-load-test.js         # WebSocket performance testing
│   ├── user-journey-test.js           # Complete user workflow testing
│   ├── spike-test.js                  # Sudden load increase testing
│   ├── stress-test.js                 # High load stress testing
│   └── soak-test.js                   # Extended duration stability testing
├── jmeter/                            # JMeter Test Plans
│   ├── test-plans/
│   │   └── focushive-api-load-test.jmx # JMeter test plan
│   ├── config/
│   │   └── test-variables.properties   # JMeter configuration
│   ├── data/
│   │   ├── test-users.csv             # Test user data
│   │   └── test-hives.csv             # Test hive data
│   ├── scripts/
│   │   └── run-load-tests.sh          # JMeter test runner script
│   └── reports/                       # Generated test reports
├── gatling/                           # Gatling Simulations
│   ├── src/test/scala/focushive/      # Scala simulation files
│   │   ├── FocusHiveApiLoadTest.scala # API load testing simulation
│   │   ├── FocusHiveStressTest.scala  # Stress testing simulation
│   │   └── FocusHiveWebSocketTest.scala # WebSocket testing simulation
│   ├── user-files/data/
│   │   └── users.csv                  # Test user data
│   ├── conf/
│   │   └── gatling.conf              # Gatling configuration
│   ├── build.sbt                     # SBT build configuration
│   ├── project/                      # SBT project files
│   └── run-gatling-tests.sh          # Gatling test runner script
└── reports/                          # Consolidated test reports
```

## 🧪 Test Types and Scenarios

### 1. Smoke Tests
- **Purpose**: Basic functionality verification
- **Duration**: 1-2 minutes
- **Load**: 1-2 users
- **Frequency**: After every deployment

### 2. Load Tests
- **Purpose**: Normal expected load testing
- **Duration**: 5-10 minutes
- **Load**: 10-50 users
- **Frequency**: Daily or before releases

### 3. Stress Tests
- **Purpose**: High load and breaking point identification
- **Duration**: 10-20 minutes
- **Load**: 100-500 users
- **Frequency**: Weekly or before major releases

### 4. Spike Tests
- **Purpose**: Sudden load increase handling
- **Duration**: 3-5 minutes
- **Load**: Rapid increase to 100+ users
- **Frequency**: Before anticipated traffic spikes

### 5. Soak Tests
- **Purpose**: Long-term stability and memory leak detection
- **Duration**: 30-90 minutes
- **Load**: Moderate consistent load
- **Frequency**: Weekly or monthly

### 6. User Journey Tests
- **Purpose**: End-to-end workflow testing
- **Duration**: 10-15 minutes
- **Load**: Mixed user behavior patterns
- **Frequency**: Before releases

## 🔧 Configuration

### Environment Variables

Common environment variables across all testing tools:

```bash
# Service URLs
export BASE_URL=http://localhost:8080
export IDENTITY_URL=http://localhost:8081
export MUSIC_URL=http://localhost:8082
export NOTIFICATION_URL=http://localhost:8083
export CHAT_URL=http://localhost:8084
export ANALYTICS_URL=http://localhost:8085
export FORUM_URL=http://localhost:8086
export BUDDY_URL=http://localhost:8087

# WebSocket URL
export WEBSOCKET_URL=ws://localhost:8080/ws

# Test Parameters
export TEST_DURATION=300         # Test duration in seconds
export RAMP_DURATION=60          # Ramp-up duration in seconds
export MAX_USERS=100             # Maximum concurrent users
export THINK_TIME=1000           # Think time between requests (ms)

# Performance Thresholds
export RESPONSE_TIME_THRESHOLD=1000    # Max response time (ms)
export ERROR_RATE_THRESHOLD=1.0        # Max error rate (%)
export SUCCESS_RATE_THRESHOLD=99.0     # Min success rate (%)
```

### k6 Configuration

Edit `k6/config/thresholds.js` to customize:
- Performance thresholds per endpoint
- Load test scenarios and stages
- Service-specific WebSocket thresholds
- Environment-specific configurations

### JMeter Configuration

Edit `jmeter/config/test-variables.properties` to customize:
- Server endpoints and ports
- Test data file locations
- Thread group configurations
- Reporting preferences

### Gatling Configuration

Edit `gatling/conf/gatling.conf` to customize:
- Base URLs and service endpoints
- Performance thresholds
- WebSocket configurations
- Reporting settings

## 📈 Monitoring and Reporting

### Real-time Monitoring

During test execution, monitor:
- **Application Metrics**: Response times, error rates, throughput
- **System Resources**: CPU, memory, disk I/O, network
- **Database Performance**: Connection pools, query performance
- **WebSocket Connections**: Active connections, message latency

### Generated Reports

Each tool generates comprehensive reports:

#### k6 Reports
- Console output with real-time metrics
- JSON summary with detailed statistics
- HTML reports (with extensions)
- Custom metrics tracking

#### JMeter Reports
- HTML dashboard reports
- CSV result files for analysis
- Response time graphs
- Error analysis reports

#### Gatling Reports
- Interactive HTML reports
- Detailed response time distributions
- Request/response statistics
- Performance trends over time

### Key Metrics to Monitor

1. **Response Time Metrics**
   - Average, median, 95th, 99th percentiles
   - Response time distribution
   - Trend analysis over time

2. **Throughput Metrics**
   - Requests per second
   - Transactions per second
   - Data transfer rates

3. **Error Metrics**
   - Error rate percentage
   - Error types and frequency
   - Failed transaction analysis

4. **Resource Utilization**
   - CPU usage per service
   - Memory consumption
   - Database connection utilization
   - WebSocket connection counts

## 🎯 Performance Optimization Guidelines

### API Performance
1. **Response Time Optimization**
   - Implement response caching for static data
   - Optimize database queries and indexes
   - Use connection pooling
   - Implement request rate limiting

2. **Throughput Optimization**
   - Scale horizontally with load balancers
   - Implement asynchronous processing
   - Use CDN for static assets
   - Optimize serialization/deserialization

### WebSocket Performance
1. **Connection Management**
   - Implement connection pooling
   - Use heartbeat mechanisms
   - Handle reconnection gracefully
   - Monitor connection limits

2. **Message Optimization**
   - Compress large messages
   - Batch multiple updates
   - Use binary protocols for large data
   - Implement message prioritization

### Database Performance
1. **Query Optimization**
   - Analyze slow query logs
   - Add appropriate indexes
   - Optimize N+1 query problems
   - Use query result caching

2. **Connection Management**
   - Configure connection pool sizes
   - Monitor connection usage
   - Implement connection timeouts
   - Use read replicas for scaling

## 🚨 Troubleshooting

### Common Issues

#### High Response Times
- Check database query performance
- Verify adequate connection pooling
- Monitor JVM garbage collection
- Check for resource contentions

#### High Error Rates
- Verify service availability
- Check authentication token expiry
- Validate request payloads
- Monitor system resource limits

#### WebSocket Connection Issues
- Verify WebSocket endpoint availability
- Check authentication mechanisms
- Monitor connection limits
- Validate message formats

#### Memory Leaks
- Monitor heap usage over time
- Check for unclosed connections
- Analyze garbage collection patterns
- Review object lifecycle management

### Debug Steps

1. **Check Service Health**
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   # ... check all services
   ```

2. **Monitor Resource Usage**
   ```bash
   htop              # System resources
   docker stats      # Container resources
   jstat -gc PID     # JVM garbage collection
   ```

3. **Analyze Logs**
   ```bash
   # Check service logs
   docker logs focushive-backend
   docker logs identity-service
   
   # Check database logs
   docker logs focushive-postgres
   ```

4. **Network Analysis**
   ```bash
   netstat -an | grep :8080   # Check port availability
   ss -tulpn | grep :8080     # Socket statistics
   ```

## 🔄 CI/CD Integration

### GitHub Actions Example

```yaml
name: Load Testing
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install k6
        run: |
          sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6
          
      - name: Start Services
        run: docker-compose up -d
        
      - name: Wait for Services
        run: |
          timeout 300 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 5; done'
          
      - name: Run Load Tests
        run: |
          cd load-tests/k6
          k6 run --quiet api-load-test.js
          k6 run --quiet websocket-load-test.js
          
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: load-test-results
          path: load-tests/results/
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Setup') {
            steps {
                checkout scm
                sh 'docker-compose up -d'
                sh 'timeout 300 bash -c "until curl -f http://localhost:8080/actuator/health; do sleep 5; done"'
            }
        }
        
        stage('Load Tests') {
            parallel {
                stage('k6 Tests') {
                    steps {
                        dir('load-tests/k6') {
                            sh 'k6 run api-load-test.js'
                            sh 'k6 run websocket-load-test.js'
                        }
                    }
                }
                
                stage('JMeter Tests') {
                    steps {
                        dir('load-tests/jmeter') {
                            sh './scripts/run-load-tests.sh smoke'
                        }
                    }
                }
            }
        }
        
        stage('Report') {
            steps {
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'load-tests/reports',
                    reportFiles: '*.html',
                    reportName: 'Load Test Report'
                ])
            }
        }
    }
    
    post {
        always {
            sh 'docker-compose down'
        }
    }
}
```

## 📚 Additional Resources

### Documentation
- [k6 Documentation](https://k6.io/docs/)
- [JMeter User Manual](https://jmeter.apache.org/usermanual/)
- [Gatling Documentation](https://gatling.io/docs/)
- [Playwright Performance Testing](https://playwright.dev/docs/test-performance)

### Best Practices
- Start with smoke tests before running full load tests
- Monitor system resources during testing
- Use realistic test data and user patterns
- Test incrementally with gradual load increases
- Document performance baselines and track improvements
- Implement automated performance regression testing

### Performance Testing Methodology
1. **Define Requirements**: Establish performance criteria and SLAs
2. **Design Tests**: Create realistic load patterns and scenarios
3. **Execute Tests**: Run tests in production-like environments
4. **Analyze Results**: Identify bottlenecks and optimization opportunities
5. **Optimize**: Implement performance improvements
6. **Validate**: Verify improvements through re-testing
7. **Monitor**: Continuous performance monitoring in production

---

## 📞 Support

For questions or issues with the load testing suite:

1. Check the troubleshooting section above
2. Review tool-specific documentation
3. Check service logs for errors
4. Verify system prerequisites and configuration
5. Create an issue in the project repository

---

**Happy Load Testing! 🚀**