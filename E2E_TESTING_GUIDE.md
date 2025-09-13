# 🧪 FocusHive E2E Testing Infrastructure

Comprehensive End-to-End testing infrastructure for the FocusHive application, featuring complete service orchestration, automated health checks, and realistic test data seeding.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Components](#components)
- [Usage Guide](#usage-guide)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Advanced Usage](#advanced-usage)

## 🎯 Overview

The E2E testing infrastructure provides:

- **Complete Service Stack**: All 8 microservices + infrastructure
- **Automated Orchestration**: Setup, health checks, data seeding, and testing
- **Mock External Dependencies**: Spotify API, Email services
- **Comprehensive Health Monitoring**: Service-specific health validation
- **Realistic Test Data**: User accounts, hives, conversations, analytics
- **Flexible Test Execution**: Multiple test suites and browsers
- **Resource Management**: Automatic cleanup and optimization

### Key Statistics
- **Services**: 13 containers (8 microservices + 5 infrastructure)
- **Test Data**: 50+ users, 15+ hives, 200+ messages, 100+ sessions
- **Health Checks**: 15+ endpoint validations
- **Mock Services**: Spotify API, MailHog email testing
- **Databases**: 8 PostgreSQL schemas + Redis
- **Scripts**: 5 comprehensive orchestration scripts

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                E2E Test Environment                  │
└─────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐ ┌─────────▼─────────┐ ┌───────▼──────┐
│ Infrastructure │ │    Microservices    │ │  Test Tools   │
│   Services     │ │    (8 Services)     │ │              │
└────────────────┘ └─────────────────────┘ └──────────────┘
        │                   │                   │
┌───────▼──────┐ ┌─────────▼─────────┐ ┌───────▼──────┐
│ • PostgreSQL │ │ • Identity (8081) │ │ • Health      │
│ • Redis      │ │ • Backend (8080)  │ │ • Data Seed   │
│ • Spotify    │ │ • Music (8082)    │ │ • Setup       │
│ • MailHog    │ │ • Notification    │ │ • Cleanup     │
│              │ │   (8083)          │ │ • Orchestration│
│              │ │ • Chat (8084)     │ │              │
│              │ │ • Analytics       │ │              │
│              │ │   (8085)          │ │              │
│              │ │ • Forum (8086)    │ │              │
│              │ │ • Buddy (8087)    │ │              │
│              │ │ • Frontend (3000) │ │              │
└──────────────┘ └─────────────────────┘ └──────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Node.js 18+ (for frontend tests)
- 4GB+ available RAM
- 10GB+ free disk space

### Run All Tests

```bash
# Run complete E2E test suite
./scripts/run-e2e-tests.sh

# Run specific test suite
./scripts/run-e2e-tests.sh test --suite smoke

# Run with visible browser
./scripts/run-e2e-tests.sh test --headed --workers 1
```

### Quick Commands

```bash
# Setup environment only
./scripts/run-e2e-tests.sh setup

# Start environment (keep running)
./scripts/run-e2e-tests.sh start

# Check environment status
./scripts/run-e2e-tests.sh status

# View service logs
./scripts/run-e2e-tests.sh logs focushive-backend

# Clean up everything
./scripts/run-e2e-tests.sh clean
```

## 🔧 Components

### 1. Master Orchestration Script
**File**: `scripts/run-e2e-tests.sh`

Main entry point for all E2E operations.

**Commands**:
- `setup` - Set up E2E environment only
- `start` - Start environment (keep running)  
- `test` - Run E2E tests (default)
- `stop` - Stop environment
- `clean` - Clean up environment
- `status` - Show environment status
- `logs [service]` - Show service logs
- `shell [service]` - Open service shell

**Options**:
- `--suite SUITE` - Test suite (all, critical, smoke, integration)
- `--browser BROWSER` - Browser (chromium, firefox, webkit)
- `--headless/--headed` - Browser display mode
- `--workers N` - Parallel workers (default: 4)
- `--retries N` - Test retries (default: 2)
- `--timeout MS` - Test timeout (default: 30000)
- `--skip-setup` - Skip environment setup
- `--skip-health-check` - Skip health checks
- `--skip-data-seeding` - Skip test data seeding
- `--force-rebuild` - Force rebuild images
- `--keep-running` - Keep environment running after tests
- `--verbose/-v` - Enable verbose logging
- `--dry-run` - Show what would be done

### 2. Environment Setup Script
**File**: `scripts/setup-e2e-env.sh`

Comprehensive environment setup with Docker validation.

**Features**:
- Prerequisites validation (Docker, disk space, memory)
- Docker Compose configuration validation
- Service dependency management
- Parallel image building
- Health check integration
- Resource usage monitoring

**Commands**:
- `setup` - Full setup (default)
- `quick` - Quick start with existing images
- `rebuild` - Force rebuild from scratch
- `status` - Show environment status

### 3. Health Check Script
**File**: `scripts/health-check.sh`

Comprehensive service health validation.

**Features**:
- Individual service health checks
- Database connection validation
- WebSocket endpoint testing
- Mock service verification
- Detailed error reporting
- Resource usage monitoring

**Commands**:
- `check` - Complete health check (default)
- `quick` - Critical services only
- `wait` - Wait for services to become healthy
- `logs SERVICE` - Show service logs

**Health Checks**:
- **PostgreSQL**: Connection + schema validation
- **Redis**: Connection + auth validation
- **Spring Services**: Actuator health endpoints
- **Mock Services**: Admin endpoints
- **Frontend**: Application endpoint
- **WebSocket**: Connection capability

### 4. Test Data Seeding Script
**File**: `scripts/seed-test-data.sh`

Realistic test data generation for all services.

**Features**:
- Service-specific data seeding
- Realistic user personas
- Diverse hive configurations  
- Chat conversations
- Analytics data
- Forum discussions
- Buddy relationships
- OAuth2 clients

**Commands**:
- `all` - Seed all data (default)
- `users` - User accounts only
- `hives` - Hives and memberships
- `analytics` - Productivity sessions
- `chat` - Chat messages
- `forum` - Forum posts and replies
- `identity` - OAuth2 clients
- `notifications` - Sample notifications
- `buddies` - Accountability relationships

**Test Data**:
- **Users**: 50 diverse user personas
- **Hives**: 15 hives with various configurations
- **Sessions**: 100+ productivity tracking sessions
- **Messages**: 200+ realistic chat conversations
- **Forum**: 30+ posts with replies
- **Achievements**: User accomplishments
- **Buddies**: Accountability partner relationships

### 5. Cleanup Script
**File**: `scripts/cleanup-e2e.sh`

Safe environment cleanup with resource management.

**Features**:
- Graceful container shutdown
- Volume management
- Docker image cleanup
- Network cleanup
- Temporary file cleanup
- Docker system cleanup
- Resource usage reporting

**Options**:
- `--force/-f` - Force cleanup without prompts
- `--keep-volumes` - Preserve test data
- `--keep-images` - Preserve Docker images
- `--keep-networks` - Preserve Docker networks
- `--compose-file FILE` - Custom compose file

### 6. Docker Compose Configuration
**File**: `docker-compose.e2e.yml`

Complete service stack definition.

**Services**:
- **test-db**: PostgreSQL with multiple schemas
- **test-redis**: Redis with authentication
- **spotify-mock**: WireMock Spotify API simulation
- **email-mock**: MailHog email testing
- **identity-service**: OAuth2 provider (8081)
- **focushive-backend**: Core backend (8080)
- **music-service**: Music integration (8082)
- **notification-service**: Notifications (8083)
- **chat-service**: Real-time messaging (8084)
- **analytics-service**: Productivity tracking (8085)
- **forum-service**: Community discussions (8086)
- **buddy-service**: Accountability partners (8087)
- **frontend-e2e**: React application (3000)

### 7. Environment Configuration
**File**: `.env.e2e`

Comprehensive environment variables for all services.

**Categories**:
- **Security**: JWT secrets, OAuth2 keys (test-only)
- **Database**: Connection strings, pool settings
- **Redis**: Cache configuration, TTL settings
- **Services**: Internal/external URLs
- **Frontend**: Vite build configuration
- **External**: Mock service settings
- **CORS**: Permissive settings for testing
- **WebSocket**: Real-time configuration
- **Rate Limiting**: Relaxed limits for testing
- **Logging**: Debug-level logging
- **E2E**: Test execution settings
- **Performance**: JVM and optimization settings
- **Features**: Feature flags and toggles

## 📖 Usage Guide

### Running Tests

#### Complete Test Suite
```bash
# Run all tests with default settings
./scripts/run-e2e-tests.sh

# Run with custom browser and workers  
./scripts/run-e2e-tests.sh test --browser firefox --workers 2
```

#### Test Suites
```bash
# Smoke tests (quick validation)
./scripts/run-e2e-tests.sh test --suite smoke

# Critical user journeys
./scripts/run-e2e-tests.sh test --suite critical

# Integration tests
./scripts/run-e2e-tests.sh test --suite integration

# Service-specific tests
./scripts/run-e2e-tests.sh test --suite auth
./scripts/run-e2e-tests.sh test --suite hive
./scripts/run-e2e-tests.sh test --suite chat
```

#### Development Testing
```bash
# Run tests with visible browser (for debugging)
./scripts/run-e2e-tests.sh test --headed --workers 1

# Keep environment running after tests
./scripts/run-e2e-tests.sh test --keep-running

# Skip setup if environment is already running
./scripts/run-e2e-tests.sh test --skip-setup
```

### Environment Management

#### Setup and Start
```bash
# Full environment setup
./scripts/run-e2e-tests.sh setup

# Quick start (assumes images exist)
./scripts/run-e2e-tests.sh start

# Force rebuild all images
./scripts/run-e2e-tests.sh setup --force-rebuild
```

#### Monitoring
```bash
# Check environment status
./scripts/run-e2e-tests.sh status

# Health check all services
./scripts/health-check.sh

# Quick health check
./scripts/health-check.sh quick

# Monitor specific service logs
./scripts/run-e2e-tests.sh logs identity-service
```

#### Debugging
```bash
# Open shell in service container
./scripts/run-e2e-tests.sh shell focushive-backend

# Check database
./scripts/run-e2e-tests.sh shell test-db

# View detailed service logs
docker compose -f docker-compose.e2e.yml logs -f --tail=100
```

#### Cleanup
```bash
# Standard cleanup (with prompts)
./scripts/run-e2e-tests.sh clean

# Force cleanup everything  
./scripts/cleanup-e2e.sh --force

# Keep some resources
./scripts/cleanup-e2e.sh --keep-volumes --keep-images
```

### Data Management

#### Seeding Test Data
```bash
# Seed all test data
./scripts/seed-test-data.sh

# Seed specific data types
./scripts/seed-test-data.sh users
./scripts/seed-test-data.sh hives
./scripts/seed-test-data.sh analytics

# Clean existing test data
./scripts/seed-test-data.sh clean
```

#### Test Accounts
After seeding, these accounts are available:

| Email | Password | Role | Persona |
|-------|----------|------|---------|
| admin@test.focushive.app | password123 | Admin | System admin |
| alice@test.focushive.app | password123 | User | CS Student |
| bob@test.focushive.app | password123 | User | Math Student |
| david@test.focushive.app | password123 | User | Developer |
| eve@test.focushive.app | password123 | User | Designer |
| grace@test.focushive.app | password123 | User | Remote Worker |

## ⚙️ Configuration

### Environment Variables

#### Core Configuration
```bash
# Docker Compose file
COMPOSE_FILE=docker-compose.e2e.yml

# Test execution
E2E_SUITE=all              # Test suite to run
E2E_BROWSER=chromium       # Browser for tests
E2E_HEADLESS=true         # Headless mode
E2E_WORKERS=4             # Parallel workers
E2E_RETRIES=2             # Retry count
E2E_TIMEOUT=30000         # Test timeout
```

#### Service URLs
```bash
# Internal (Docker network)
IDENTITY_SERVICE_URL=http://identity-service:8081
FOCUSHIVE_BACKEND_URL=http://focushive-backend:8080

# External (host machine)
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WEBSOCKET_URL=ws://localhost:8080/ws
```

#### Database Configuration
```bash
# PostgreSQL
DB_HOST=test-db
DB_USER=test_user
DB_PASSWORD=test_pass
POSTGRES_MULTIPLE_DATABASES=identity_test,music_test,notification_test,chat_test,analytics_test,forum_test,buddy_test

# Redis
REDIS_HOST=test-redis
REDIS_PASSWORD=test_redis_pass
```

### Docker Compose Override

Create `docker-compose.e2e.override.yml` for local customizations:

```yaml
version: '3.8'
services:
  focushive-backend:
    environment:
      LOG_LEVEL: TRACE
    volumes:
      - ./custom-config:/app/config

  frontend-e2e:
    ports:
      - "3001:80"  # Alternative port
```

### Test Configuration

#### Playwright Configuration
Create `frontend/playwright.config.e2e.ts`:

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: parseInt(process.env.E2E_TIMEOUT || '30000'),
  retries: parseInt(process.env.E2E_RETRIES || '2'),
  workers: parseInt(process.env.E2E_WORKERS || '4'),
  
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:3000',
    headless: process.env.E2E_HEADLESS !== 'false',
  },
  
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],
});
```

## 🐛 Troubleshooting

### Common Issues

#### Services Won't Start
```bash
# Check Docker daemon
docker info

# Validate compose file
docker compose -f docker-compose.e2e.yml config

# Check resource usage
docker system df
```

#### Health Checks Fail
```bash
# Run detailed health check
./scripts/health-check.sh

# Check specific service logs
./scripts/run-e2e-tests.sh logs identity-service

# Test manual connection
curl http://localhost:8081/actuator/health
```

#### Database Connection Issues
```bash
# Check database logs
docker compose -f docker-compose.e2e.yml logs test-db

# Test database connection
docker compose -f docker-compose.e2e.yml exec test-db psql -U test_user -d focushive_test -c "SELECT 1;"

# Reset database
docker compose -f docker-compose.e2e.yml restart test-db
```

#### Tests Fail
```bash
# Run with visible browser for debugging
./scripts/run-e2e-tests.sh test --headed --workers 1

# Check test artifacts
ls frontend/test-results/
ls frontend/playwright-report/

# Increase timeout
E2E_TIMEOUT=60000 ./scripts/run-e2e-tests.sh test
```

#### Out of Resources
```bash
# Check disk space
df -h

# Check memory usage  
free -m

# Clean up Docker resources
docker system prune -a --volumes

# Use resource-optimized setup
./scripts/cleanup-e2e.sh --force
```

### Debug Commands

#### Service Debugging
```bash
# Check all container status
docker compose -f docker-compose.e2e.yml ps

# Follow all service logs
docker compose -f docker-compose.e2e.yml logs -f

# Check container resource usage
docker stats

# Inspect service configuration
docker compose -f docker-compose.e2e.yml config
```

#### Network Debugging
```bash
# Check network connectivity
docker compose -f docker-compose.e2e.yml exec focushive-backend curl http://identity-service:8081/actuator/health

# Test external connectivity
curl http://localhost:8080/actuator/health

# Check network configuration
docker network ls
docker network inspect focushive-e2e-network
```

## 🔬 Advanced Usage

### Custom Test Data

#### Add Custom Users
```sql
-- Connect to database
docker compose -f docker-compose.e2e.yml exec test-db psql -U test_user -d focushive_test

-- Add custom user
INSERT INTO users (id, username, email, first_name, last_name, password_hash, email_verified, created_at, updated_at, status, role) VALUES
('custom-user-id', 'custom_user', 'custom@test.com', 'Custom', 'User', '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW(), NOW(), 'ACTIVE', 'USER');
```

#### Custom Hive Configuration
```sql
-- Add custom hive
INSERT INTO hives (id, name, description, owner_id, type, privacy, max_members, settings, created_at, updated_at, status) VALUES
('custom-hive-id', 'Custom Test Hive', 'Special hive for advanced testing', 'custom-user-id', 'CUSTOM', 'PRIVATE', 5, '{"focus_mode": "advanced", "custom_features": true}', NOW(), NOW(), 'ACTIVE');
```

### Performance Testing

#### Load Testing Configuration
```bash
# Run with multiple workers
E2E_WORKERS=8 ./scripts/run-e2e-tests.sh test

# Stress test specific service
for i in {1..100}; do
  curl -s http://localhost:8080/actuator/health > /dev/null &
done
wait
```

#### Resource Monitoring
```bash
# Monitor during tests
watch -n 1 'docker stats --no-stream'

# Log resource usage
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" > resource-usage.log &
./scripts/run-e2e-tests.sh test
```

### CI/CD Integration

#### GitHub Actions Example
```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker
        uses: docker/setup-buildx-action@v2
        
      - name: Run E2E Tests
        run: |
          ./scripts/run-e2e-tests.sh test --headless --workers 2
          
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: e2e-results
          path: |
            frontend/playwright-report/
            frontend/test-results/
```

#### Environment Matrix Testing
```bash
# Test with different configurations
E2E_BROWSER=chromium ./scripts/run-e2e-tests.sh test --suite critical
E2E_BROWSER=firefox ./scripts/run-e2e-tests.sh test --suite critical
E2E_BROWSER=webkit ./scripts/run-e2e-tests.sh test --suite critical
```

### Security Testing

#### OAuth2 Flow Testing
```bash
# Test OAuth2 endpoints
curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test-focushive-frontend&client_secret=test_client_secret"
```

#### API Security Testing
```bash
# Test without authentication
curl -v http://localhost:8080/api/hives

# Test with invalid token
curl -v -H "Authorization: Bearer invalid_token" http://localhost:8080/api/hives
```

### Scaling and Performance

#### Horizontal Scaling
```yaml
# docker-compose.e2e.override.yml
version: '3.8'
services:
  focushive-backend:
    deploy:
      replicas: 2
    
  chat-service:
    deploy:
      replicas: 3
```

#### Resource Limits
```yaml
services:
  focushive-backend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

---

## 📞 Support

For issues with the E2E testing infrastructure:

1. **Check the logs**: `./scripts/run-e2e-tests.sh logs [service]`
2. **Run health check**: `./scripts/health-check.sh`
3. **Review this guide**: Especially the troubleshooting section
4. **Clean and retry**: `./scripts/cleanup-e2e.sh --force` then `./scripts/run-e2e-tests.sh setup`

## 🚀 Next Steps

- Add visual regression testing
- Implement performance benchmarking
- Add mobile browser testing
- Create custom test reporters
- Add API contract testing
- Implement chaos engineering tests

---

*This E2E testing infrastructure provides a robust foundation for comprehensive testing of the FocusHive application across all services and user journeys.*