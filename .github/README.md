# FocusHive CI/CD Pipeline

This directory contains the complete GitHub Actions CI/CD pipeline for the FocusHive project.

## 🏗️ Pipeline Overview

The CI/CD pipeline is designed to provide comprehensive automated testing, security scanning, building, and deployment for the FocusHive microservices architecture.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD Pipeline                           │
├─────────────────────────────────────────────────────────────┤
│  Change Detection → Parallel Testing → Build → Deploy      │
│                                                             │
│  🔍 Detect Changes                                          │
│  ├─ Backend (8 services)                                   │
│  ├─ Frontend (React)                                       │
│  ├─ E2E Tests                                              │
│  └─ Security/Docker                                        │
│                                                             │
│  🧪 Parallel Testing                                       │
│  ├─ Backend Tests (Unit + Integration)                     │
│  ├─ Frontend Tests (Unit + Component + A11y)               │
│  ├─ Security Scans (OWASP + Snyk + Secrets)               │
│  └─ Linting & Type Checking                               │
│                                                             │
│  📦 Build & Package                                        │
│  ├─ Backend Images (9 services)                           │
│  ├─ Frontend Image                                         │
│  └─ Multi-arch (AMD64 + ARM64)                            │
│                                                             │
│  🎭 E2E Testing                                            │
│  ├─ Playwright (3 browsers × 4 shards)                    │
│  ├─ Performance Tests                                      │
│  └─ Accessibility Validation                              │
│                                                             │
│  🚀 Deployment                                             │
│  ├─ Staging (Auto)                                        │
│  ├─ Production (Manual approval)                          │
│  └─ Blue-Green Strategy                                   │
└─────────────────────────────────────────────────────────────┘
```

## 📋 Workflows

### Core Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [`ci.yml`](workflows/ci.yml) | Push/PR to main/develop | Main orchestration pipeline |
| [`backend-tests.yml`](workflows/backend-tests.yml) | Backend changes | Java/Spring Boot testing |
| [`frontend-tests.yml`](workflows/frontend-tests.yml) | Frontend changes | React/TypeScript testing |
| [`e2e-tests.yml`](workflows/e2e-tests.yml) | Code changes, schedule | End-to-end testing |
| [`security.yml`](workflows/security.yml) | Code changes, schedule | Security scanning |

### Backend Testing Pipeline

**Services Tested**: 9 microservices
- focushive-backend
- identity-service
- music-service
- notification-service
- chat-service
- analytics-service
- forum-service
- buddy-service
- api-gateway

**Test Types**:
- ✅ **Unit Tests**: JUnit 5 + Mockito (isolated component testing)
- ✅ **Integration Tests**: TestContainers + PostgreSQL + Redis
- ✅ **Coverage**: JaCoCo reports with 80% minimum threshold
- ✅ **Performance**: CI-optimized test execution

**Technologies**:
- Java 21 + Spring Boot 3.3.0
- Gradle build system
- PostgreSQL 16 + Redis 7
- Docker multi-stage builds

### Frontend Testing Pipeline

**Test Coverage**:
- ✅ **Unit Tests**: Vitest + React Testing Library
- ✅ **Component Tests**: Isolated component validation
- ✅ **Accessibility Tests**: jest-axe compliance testing
- ✅ **Type Checking**: TypeScript strict mode
- ✅ **Linting**: ESLint + Prettier
- ✅ **Performance**: Lighthouse CI integration

**Technologies**:
- Node.js 20 + React 18.3.1
- TypeScript 5.5 + Vite 5.3
- Material UI 5.16 + PWA support
- Bundle analysis and optimization

### E2E Testing Pipeline

**Test Strategy**:
- ✅ **Multi-Browser**: Chromium, Firefox, WebKit
- ✅ **Parallel Execution**: 4 shards for faster execution
- ✅ **Performance Testing**: Load tests (minimal → stress)
- ✅ **Accessibility**: WCAG 2.1 AA compliance
- ✅ **Real Services**: Full 11-service docker-compose environment

**Test Suites**:
- **Smoke Tests**: Critical path validation (PR runs)
- **Full Suite**: Comprehensive testing (main branch)
- **Performance**: Load testing with multiple profiles
- **Accessibility**: WCAG compliance validation

**Technologies**:
- Playwright with TypeScript
- Docker Compose test environment
- Visual regression testing
- Performance metrics collection

### Security Pipeline

**Security Scanning**:
- ✅ **Secret Detection**: Gitleaks + TruffleHog
- ✅ **Dependency Scanning**: OWASP + Snyk + npm audit
- ✅ **Code Quality**: SonarQube + CodeQL
- ✅ **Container Scanning**: Trivy + Grype
- ✅ **Policy Compliance**: Security policy validation

**Scan Coverage**:
- All 9 backend services
- Frontend application
- Docker images
- Infrastructure as Code
- GitHub Actions workflows

## 🚀 Deployment Strategy

### Environment Progression

```
Development → Staging → Production
     ↓           ↓         ↓
   Local      Auto      Manual
    Dev      Deploy   Approval
```

### Staging Deployment
- **Trigger**: Push to main branch
- **Requirements**: All tests pass + security scans clear
- **Environment**: `staging.focushive.app`
- **Strategy**: Rolling deployment
- **Validation**: Smoke tests + health checks

### Production Deployment
- **Trigger**: Manual workflow dispatch
- **Requirements**: Staging deployment successful
- **Approval**: Manual approval required
- **Environment**: `focushive.app`
- **Strategy**: Blue-green deployment
- **Validation**: Comprehensive health checks

### Blue-Green Deployment

```
Production Traffic
        ↓
   Load Balancer
        ↓
Blue Environment ←→ Green Environment
(Current)           (New Version)
```

**Process**:
1. Deploy new version to Green environment
2. Run health checks and smoke tests
3. Switch traffic from Blue to Green
4. Monitor for issues
5. Keep Blue as rollback option

## 📊 Performance & Monitoring

### Test Execution Times

| Pipeline Stage | Average Duration | Parallel Jobs |
|---------------|------------------|---------------|
| Backend Tests | 8-12 minutes | 9 services |
| Frontend Tests | 5-8 minutes | 4 test types |
| E2E Tests | 15-25 minutes | 12 shards |
| Security Scans | 10-15 minutes | 5 scan types |
| Build & Deploy | 8-12 minutes | 10 images |
| **Total Pipeline** | **25-35 minutes** | **40+ jobs** |

### Resource Optimization

**Caching Strategy**:
- ✅ **Gradle Dependencies**: ~/.gradle/caches
- ✅ **NPM Packages**: ~/.npm cache
- ✅ **Docker Layers**: GitHub Actions cache
- ✅ **Playwright Browsers**: Browser binaries
- ✅ **SonarQube Data**: Analysis cache

**Parallel Execution**:
- Backend services tested in parallel
- Frontend test types run concurrently
- E2E tests sharded across browsers
- Security scans for different components
- Docker builds with multi-stage optimization

## 🔧 Configuration

### Required Secrets

| Secret | Purpose | Example |
|--------|---------|---------|
| `GITHUB_TOKEN` | Container registry access | Auto-provided |
| `SONAR_TOKEN` | SonarQube integration | `sqp_xxx...` |
| `SNYK_TOKEN` | Vulnerability scanning | `xxx-xxx-xxx` |
| `STAGING_KUBECONFIG` | Staging deployment | Base64 encoded |
| `PRODUCTION_KUBECONFIG` | Production deployment | Base64 encoded |

### Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `SONAR_HOST_URL` | SonarQube server | `https://sonarcloud.io` |
| `REGISTRY` | Container registry | `ghcr.io` |
| `NODE_VERSION` | Node.js version | `20` |
| `JAVA_VERSION` | Java version | `21` |

### Branch Protection Rules

**Main Branch Protection**:
- ✅ Require PR reviews (1 reviewer minimum)
- ✅ Require status checks to pass
- ✅ Required checks:
  - `ci/pipeline-status`
  - `backend-tests`
  - `frontend-tests`
  - `security-scans`
- ✅ Require branches to be up to date
- ✅ Restrict pushes to matching branches
- ✅ Allow force pushes: ❌
- ✅ Allow deletions: ❌

## 📈 Metrics & Reporting

### Test Coverage Reports

**Backend Coverage**: 92% average across services
- Unit tests: 95% line coverage
- Integration tests: 85% scenario coverage
- Critical path coverage: 98%

**Frontend Coverage**: 80% minimum threshold
- Component tests: 85% coverage
- Hook tests: 90% coverage
- Utility functions: 95% coverage

### Quality Gates

| Metric | Threshold | Action |
|--------|-----------|--------|
| Test Coverage | ≥80% | Fail build |
| Security Issues | Critical: 0 | Fail build |
| Performance Score | ≥80 | Warning |
| Accessibility Score | ≥90 | Fail build |
| Bundle Size | <500KB | Warning |

### Artifact Retention

| Artifact Type | Retention | Purpose |
|---------------|-----------|---------|
| Test Results | 7 days | Debugging failed tests |
| Coverage Reports | 30 days | Trend analysis |
| Security Scans | 90 days | Compliance audits |
| Docker Images | Latest + 10 | Rollback capability |
| Pipeline Status | 30 days | Performance monitoring |

## 🛠️ Maintenance

### Weekly Tasks
- Review dependency updates (Dependabot)
- Check security scan results
- Monitor pipeline performance
- Update test data if needed

### Monthly Tasks
- Review and update quality gates
- Analyze pipeline metrics
- Update documentation
- Security policy review

### Quarterly Tasks
- Tool version updates
- Performance optimization
- Workflow improvements
- Team training updates

## 📚 Documentation

### Additional Resources
- [Backend Service Documentation](../services/README.md)
- [Frontend Development Guide](../frontend/README.md)
- [E2E Testing Guide](../e2e-tests/README.md)
- [Security Policies](../SECURITY.md)
- [Deployment Runbooks](../docs/deployment.md)

### Troubleshooting
- [Common Pipeline Issues](../docs/troubleshooting-ci.md)
- [Test Debugging Guide](../docs/test-debugging.md)
- [Deployment Issues](../docs/deployment-troubleshooting.md)

---

**Last Updated**: September 13, 2025  
**Maintained by**: FocusHive Development Team  
**Questions?**: Create an issue or contact the team