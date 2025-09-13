# 🧪 FocusHive E2E Testing Environment

Comprehensive End-to-End testing environment for FocusHive with all 8 microservices, mock external dependencies, and 558 test scenarios.

## 🏗️ Architecture

The E2E testing environment includes:

### Core Services (8 Microservices)
- **FocusHive Backend** (8080) - Core application logic
- **Identity Service** (8081) - OAuth2 provider and persona management
- **Music Service** (8082) - Spotify integration with mocks
- **Notification Service** (8083) - Email/push notifications with MailHog
- **Chat Service** (8084) - Real-time messaging
- **Analytics Service** (8085) - Productivity tracking and insights
- **Forum Service** (8086) - Community discussions
- **Buddy Service** (8087) - Accountability partner matching

### Infrastructure
- **PostgreSQL** - Multiple test databases (one per service)
- **Redis** - Session storage and real-time features
- **Frontend** - React app configured for E2E testing

### Mock Services
- **Spotify Mock** (8090) - WireMock server with Spotify API responses
- **Email Mock** (8025) - MailHog for email testing

## 📁 File Structure

```
├── docker-compose.e2e.yml           # Complete E2E environment
├── docker/
│   ├── postgres/
│   │   └── init-multiple-databases.sh  # Multi-database setup
│   ├── test-data/
│   │   ├── seed-data.sql              # Shared test data
│   │   ├── seed-users.sql             # User and hive data
│   │   ├── seed-hives.sql             # Additional hive data
│   │   └── seed-oauth-clients.sql     # OAuth2 test clients
│   └── mocks/
│       └── spotify/
│           └── mappings/              # Spotify API mocks
├── scripts/
│   ├── setup-e2e-environment.sh      # Initial setup
│   └── run-e2e-tests.sh              # Test runner
├── frontend/e2e/                     # E2E test suites
└── .env.e2e                          # Test environment variables
```

## 🚀 Quick Start

### 1. Initial Setup

```bash
# Run the setup script
./scripts/setup-e2e-environment.sh

# This creates all required directories and files
```

### 2. Start E2E Environment

```bash
# Start all services (takes ~2-3 minutes)
./scripts/run-e2e-tests.sh start
```

### 3. Run Tests

```bash
# Run all 558 E2E test scenarios
./scripts/run-e2e-tests.sh

# Or run tests manually
cd frontend
npm run test:e2e
```

### 4. Stop Environment

```bash
./scripts/run-e2e-tests.sh stop
```

## 🧪 Test Categories

The E2E test suite includes **558 test scenarios** across **17 test suites**:

### Authentication & Identity (8081)
- Login/logout flows
- OAuth2 authorization code flow
- Multi-persona switching
- Privacy controls

### Core Functionality (8080)
- Hive creation and management
- Real-time presence system
- Timer sessions (Pomodoro, custom)
- WebSocket connections

### Music Integration (8082)
- Spotify playlist integration
- Collaborative playlists
- Music playback controls

### Real-time Features
- Chat messaging (8084)
- Presence updates
- Live activity feeds

### Community Features
- Forum discussions (8086)
- Buddy system matching (8087)

### Analytics & Tracking (8085)
- Focus time tracking
- Productivity metrics
- Achievement system

### Cross-browser Testing
- Chrome, Firefox, Safari
- Mobile responsiveness
- Accessibility compliance

### Security Testing
- Input validation
- XSS prevention
- CSRF protection
- Rate limiting

## 🔧 Configuration

### Environment Variables

Key variables in `.env.e2e`:

```bash
# Services
JWT_SECRET=test_jwt_secret_key_for_e2e_testing_only
POSTGRES_PASSWORD=test_pass
REDIS_PASSWORD=test_redis_pass

# URLs
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WEBSOCKET_URL=ws://localhost:8080/ws
VITE_IDENTITY_SERVICE_URL=http://localhost:8081

# Test Configuration
E2E_HEADLESS=true
E2E_TIMEOUT=30000
E2E_PARALLEL_TESTS=4
```

### Service Endpoints

When environment is running:

```
Frontend:           http://localhost:3000
Backend API:        http://localhost:8080
Identity Service:   http://localhost:8081
Music Service:      http://localhost:8082
Notification:       http://localhost:8083
Chat Service:       http://localhost:8084
Analytics:          http://localhost:8085
Forum Service:      http://localhost:8086
Buddy Service:      http://localhost:8087
Spotify Mock:       http://localhost:8090
Email Mock UI:      http://localhost:8025
```

## 📊 Test Data

### Users
- **alice@test.com** - Student persona, hive owner
- **bob@test.com** - Developer persona, moderator
- **charlie@test.com** - Writer persona, creative hive
- **diana@test.com** - Manager persona, private hive
- **eve@test.com** - Designer persona, community hive

### Hives
- **Study Group Alpha** - Public study hive (8 members)
- **Work Focus Hive** - Professional workspace (12 members)
- **Creative Writing** - Writers' community (6 members)
- **Private Team Hive** - Closed team workspace (5 members)
- **Large Community** - Big public hive (50+ members)

### Mock Data
- **Spotify Playlists** - Focus beats, study vibes, ambient sounds
- **Email Templates** - Welcome, notifications, reminders
- **OAuth2 Clients** - Frontend, mobile, third-party integrations

## 🐛 Debugging

### Check Service Status
```bash
./scripts/run-e2e-tests.sh status
```

### View Service Logs
```bash
# All services
./scripts/run-e2e-tests.sh logs

# Specific service
./scripts/run-e2e-tests.sh logs focushive-backend
./scripts/run-e2e-tests.sh logs identity-service
```

### Access Databases
```bash
# PostgreSQL (main test DB)
docker exec -it focushive-test-db psql -U test_user -d focushive_test

# Redis
docker exec -it focushive-test-redis redis-cli -a test_redis_pass
```

### Mock Service Admin
- **Spotify Mock Admin**: http://localhost:8090/__admin/
- **Email Mock UI**: http://localhost:8025/

## 🎯 Test Execution

### Automated Test Runner

```bash
# Full test suite with reporting
./scripts/run-e2e-tests.sh

# Keep environment running after tests
KEEP_RUNNING=true ./scripts/run-e2e-tests.sh
```

### Manual Test Execution

```bash
# Start environment
./scripts/run-e2e-tests.sh start

# Run specific test suites
cd frontend
npx playwright test auth/
npx playwright test hive/
npx playwright test real-time/

# Run with UI
npx playwright test --ui

# Generate report
npx playwright show-report
```

### Test Reports

After tests complete, reports are generated in:
- `test-reports/e2e-[timestamp]/`
- `frontend/playwright-report/`
- Service logs and environment info

## 🔒 Security Notes

⚠️ **This is a TEST environment only!**

- Uses hardcoded test secrets
- Disabled CORS protections
- Open database access
- Mock external services
- Debug logging enabled

**Never use these configurations in production!**

## 📈 Performance

### Startup Times
- Database initialization: ~30 seconds
- Service startup: ~60-90 seconds
- Total environment ready: ~2-3 minutes

### Test Execution
- 558 test scenarios: ~15-25 minutes
- Parallel execution: 4 workers
- Includes setup/teardown time

### Resource Usage
- Memory: ~4-6 GB for full environment
- CPU: Moderate during startup, low during tests
- Disk: ~2-3 GB for images and data

## 🤝 Contributing

### Adding New Tests

1. Create test file in `frontend/e2e/tests/[category]/`
2. Follow existing patterns for authentication
3. Use test data from seed scripts
4. Add to appropriate test suite

### Adding New Services

1. Add service to `docker-compose.e2e.yml`
2. Create database in seed scripts
3. Add health check configuration
4. Update service list in test runner

### Mock Integrations

1. Add WireMock mappings in `docker/mocks/[service]/`
2. Configure service to use mock URL
3. Create test scenarios for integration

## 📋 Troubleshooting

### Common Issues

**Services won't start**
- Check Docker is running and has enough resources
- Verify no port conflicts (8080-8087, 3000, 5432, 6379)
- Check service logs for errors

**Tests fail to connect**
- Ensure all services show as "healthy"
- Verify frontend can reach backend APIs
- Check network configuration

**Database errors**
- Verify PostgreSQL initialized correctly
- Check if multiple databases were created
- Ensure seed scripts ran successfully

**Mock services not responding**
- Verify WireMock started correctly
- Check mapping files are valid JSON
- Test mock endpoints manually

### Getting Help

1. Check service logs: `./scripts/run-e2e-tests.sh logs [service]`
2. Verify health endpoints manually
3. Review test reports for detailed error info
4. Check Docker container status

## 🎉 Success Metrics

A successful E2E test run should show:

- ✅ All 8 microservices healthy
- ✅ Mock services responding  
- ✅ Database seeding completed
- ✅ Frontend loads correctly
- ✅ WebSocket connections established
- ✅ 558 test scenarios passing
- ✅ No memory leaks or resource issues

---

**Happy Testing! 🧪✨**