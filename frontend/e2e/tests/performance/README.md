# Load Testing Infrastructure

## Overview
Safe, scalable load testing infrastructure for FocusHive with resource monitoring and progressive load profiles.

## Features
- **Resource Monitoring**: Real-time CPU and memory monitoring to prevent system crashes
- **Safe Mode**: Automatic throttling when system resources are high
- **Progressive Profiles**: From minimal (5 users) to stress (50 users) for local testing
- **Cloud-Ready**: Pre-configured profiles for cloud infrastructure (100-500 users)
- **Detailed Reporting**: Performance metrics, percentiles, and resource usage reports

## Load Test Profiles

### Local Testing (Safe for MacBook)
| Profile | Users | Duration | Max Browsers | Headless |
|---------|-------|----------|--------------|----------|
| local-minimal | 5 | 60s | 5 | Yes |
| local-small | 10 | 120s | 10 | Yes |
| local-medium | 25 | 180s | 15 | Yes |
| local-stress | 50 | 300s | 20 | Yes |

### Cloud/CI Testing (Future Implementation)
| Profile | Users | Duration | Max Browsers | Purpose |
|---------|-------|----------|--------------|---------|
| cloud-standard | 100 | 600s | 50 | CI/CD validation |
| cloud-peak | 500 | 900s | 100 | Peak load simulation |
| cloud-endurance | 200 | 3600s | 75 | Long-running test |
| cloud-spike | 300 | 600s | 100 | Traffic spike test |

## Running Load Tests

### Quick Start (Safe Minimal Test)
```bash
npm run test:load:minimal
```

### Available Commands
```bash
# Local testing (safe for development machines)
npm run test:load:minimal   # 5 users, 60s
npm run test:load:small     # 10 users, 120s
npm run test:load:medium    # 25 users, 180s (use with caution)
npm run test:load:stress    # 50 users, 300s (monitor resources!)

# Run specific test
npx playwright test e2e/tests/performance/load-test.spec.ts --grep "Local Minimal"

# Run all load tests (not recommended locally)
npm run test:load:all
```

## Safety Features

### Resource Monitoring
The test infrastructure continuously monitors:
- Memory usage (%)
- CPU usage (%)
- Response times (ms)
- Error rates (%)

### Automatic Throttling
When system resources exceed safe thresholds:
- **Memory > 85%**: Pauses test execution
- **CPU > 90%**: Reduces concurrent browsers
- **Safe Mode**: Prevents system crashes

### Progressive Ramp-Up
Users are added gradually to avoid sudden resource spikes:
- Batched user creation
- Configurable ramp-up time
- Inter-batch delays

## Test Scenarios

### 1. Complete User Journey
- Navigate to homepage
- Login with credentials
- Create/join a hive
- Start focus timer
- Send chat messages
- View analytics
- Logout

### 2. Quick Focus Session
- Quick join public hive
- Start 15-minute timer
- Complete session
- Leave hive

### 3. Heavy Real-time Interaction
- Join busy hive
- Continuous message sending
- Presence updates
- Notification subscriptions

## Performance Thresholds

### Response Times (Target)
- P50: < 200ms
- P75: < 400ms
- P90: < 800ms
- P95: < 1200ms
- P99: < 2000ms

### Other Metrics
- Error Rate: < 1%
- Throughput: > 100 req/s
- CPU Usage: < 80%
- Memory Usage: < 85%

## Report Generation

Tests automatically generate detailed reports including:
- Summary statistics
- Performance percentiles
- Resource usage graphs
- Pass/fail status

Reports are saved to:
```
./test-results/load-test-{profile}-{timestamp}.json
```

## Troubleshooting

### "System resources too high" Error
- Close unnecessary applications
- Increase system memory
- Use a smaller load profile
- Enable headless mode

### High Error Rates
- Check backend services are running
- Verify network connectivity
- Review application logs
- Reduce concurrent users

### Slow Response Times
- Enable performance profiling
- Check database queries
- Review WebSocket connections
- Optimize frontend bundles

## Best Practices

1. **Start Small**: Always begin with `local-minimal` profile
2. **Monitor Resources**: Keep Activity Monitor/Task Manager open
3. **Incremental Testing**: Gradually increase load
4. **Headless Mode**: Use for better performance
5. **Clean State**: Reset database between tests
6. **Regular Testing**: Run load tests before major releases

## Infrastructure Requirements

### Local Testing
- **RAM**: 8GB minimum, 16GB recommended
- **CPU**: 4 cores minimum, 8 cores recommended
- **Network**: Stable connection
- **OS**: macOS, Linux, or Windows

### Cloud Testing (Recommended for 100+ users)
- **Instance Type**: c5.2xlarge or equivalent
- **RAM**: 16GB minimum
- **CPU**: 8 vCPUs
- **Network**: High bandwidth
- **Cost**: ~$0.34/hour on AWS

## Future Enhancements

1. **Distributed Testing**: Multi-node load generation
2. **Real User Monitoring**: Production metrics integration
3. **AI-Driven Analysis**: Automatic bottleneck detection
4. **Continuous Load Testing**: Scheduled performance regression tests
5. **Custom Scenarios**: User-defined test workflows

## Support

For issues or questions about load testing:
1. Check the troubleshooting section
2. Review test logs in `./test-results/`
3. Contact the performance team
4. Open a Linear issue with tag `performance`