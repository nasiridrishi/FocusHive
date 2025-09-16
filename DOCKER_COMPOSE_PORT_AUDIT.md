# Docker Compose Port Audit & Standardization Plan

## Executive Summary
Comprehensive audit of all Docker Compose configurations across FocusHive services revealed critical issues:
- **Port conflicts** between services
- **Security risks** from exposed internal services
- **Network isolation** preventing inter-service communication

## 🔴 Critical Issues Found

### 1. Port Conflicts

| Port | Service 1 | Service 2 | Issue |
|------|-----------|-----------|--------|
| 5432 | focushive-backend (exposed) | Multiple internal PostgreSQL | External exposure unnecessary |
| 6379 | focushive-backend (exposed) | Multiple internal Redis | External exposure unnecessary |
| 8081 | focushive-backend/identity | identity-service | Duplicate definition |
| 9090 | prometheus (backend) | Different port in notification (9092) | Inconsistent |

### 2. Security Issues

#### Exposed Internal Services (MUST FIX):
- **PostgreSQL (5432)**: focushive-backend exposes database to host
- **Redis (6379)**: focushive-backend exposes cache to host
- **RabbitMQ (5673)**: notification-service exposes message queue

#### Best Practice Violations:
- Internal databases should NEVER be exposed to host
- Only application ports should be exposed
- Management ports should be on separate interface

### 3. Network Configuration Issues

| Service | Network Name | Issue |
|---------|-------------|--------|
| focushive-backend | focushive-network | Inconsistent naming |
| identity-service | focushive-shared-network | Different from backend |
| notification-service | focushive-shared-network | OK |
| buddy-service | focushive_buddy_service_network | Isolated from others |

## 📋 Standardized Port Allocation

### Application Ports (Exposed to Host)

| Port | Service | Purpose | Status |
|------|---------|---------|--------|
| 8080 | focushive-backend | Main API | ✅ Keep |
| 8081 | identity-service | OAuth2/Auth | ✅ Keep |
| 8083 | notification-service | Notifications | ✅ Keep |
| 8087 | buddy-service | Buddy System | ✅ Keep |
| 8088 | buddy-service | Management/Actuator | ✅ Keep |
| 80 | nginx | Reverse Proxy | ✅ Keep (production) |
| 443 | nginx | HTTPS | 🔄 Add (production) |

### Monitoring Ports (Optional Exposure)

| Port | Service | Purpose | Recommendation |
|------|---------|---------|----------------|
| 9090 | prometheus | Metrics Collection | Profile: monitoring |
| 3000 | grafana | Dashboards | Profile: monitoring |
| 9100 | node-exporter | Host Metrics | Profile: monitoring |
| 15672 | rabbitmq-management | RabbitMQ UI | Profile: dev only |

### Internal Services (MUST NOT EXPOSE)

| Service | Default Port | Current Status | Required Action |
|---------|--------------|----------------|-----------------|
| PostgreSQL | 5432 | ❌ Exposed | Remove port mapping |
| Redis | 6379 | ❌ Exposed | Remove port mapping |
| RabbitMQ | 5672 | ⚠️ Mapped to 5673 | Remove for production |

## 🔧 Required Changes

### 1. focushive-backend/docker-compose.yml

```yaml
# REMOVE these port exposures:
postgres:
  # ports:
  #   - "5432:5432"  # REMOVE - security risk

redis:
  # ports:
  #   - "6379:6379"  # REMOVE - security risk
```

### 2. notification-service/docker-compose.yml

```yaml
# Make internal services truly internal:
focushive-notification-service-postgres:
  # No ports exposed - correct

focushive-notification-service-redis:
  # No ports exposed - correct

focushive-notification-service-rabbitmq:
  ports:
    # - "5673:5672"  # REMOVE for production
    - "15673:15672"  # Keep only management UI for dev
```

### 3. buddy-service/docker-compose.yml

```yaml
# Internal services should not expose ports:
focushive_buddy_service_postgres:
  # No ports section - keep internal

focushive_buddy_service_redis:
  # No ports section - keep internal
```

### 4. Unified Network Configuration

All services should use the same shared network:

```yaml
networks:
  focushive-shared-network:
    driver: bridge
    name: focushive-shared-network  # Explicit name
```

## 🏗️ Implementation Plan

### Phase 1: Fix Security Issues (IMMEDIATE)
1. Remove PostgreSQL port exposures
2. Remove Redis port exposures
3. Remove RabbitMQ AMQP port exposure

### Phase 2: Standardize Networks
1. Update all services to use `focushive-shared-network`
2. Add network aliases for service discovery
3. Configure DNS resolution between services

### Phase 3: Add Docker Profiles
1. Add `monitoring` profile for Prometheus/Grafana
2. Add `dev` profile for development tools
3. Add `production` profile for production deployments

### Phase 4: Testing
1. Verify inter-service connectivity
2. Test health endpoints
3. Confirm no port conflicts
4. Security scan for exposed ports

## 📊 Final Port Matrix

### Production Deployment

```
External Access (Internet):
  └── 80/443 (nginx)
      ├── 8080 (backend)
      ├── 8081 (identity)
      ├── 8083 (notification)
      └── 8087 (buddy)

Internal Only (Docker Network):
  ├── 5432 (PostgreSQL instances)
  ├── 6379 (Redis instances)
  ├── 5672 (RabbitMQ)
  └── 8088 (Buddy actuator)
```

### Development Environment

```
Host Access:
  ├── 8080 (backend)
  ├── 8081 (identity)
  ├── 8083 (notification)
  ├── 8087 (buddy)
  ├── 8088 (buddy-actuator)
  ├── 9090 (prometheus)
  ├── 3000 (grafana)
  └── 15672 (rabbitmq-ui)

Internal Only:
  ├── 5432 (PostgreSQL)
  ├── 6379 (Redis)
  └── 5672 (RabbitMQ)
```

## 🔒 Security Compliance

### Requirements Met:
- ✅ Databases not exposed to host
- ✅ Cache services internal only
- ✅ Message queues internal only
- ✅ Application ports clearly defined
- ✅ Management ports segregated
- ✅ Network isolation enforced

### Production Checklist:
- [ ] Remove all database port exposures
- [ ] Remove all cache port exposures
- [ ] Use reverse proxy for all services
- [ ] Enable TLS/HTTPS
- [ ] Implement firewall rules
- [ ] Use Docker secrets for passwords

## 📝 Migration Commands

```bash
# Stop all services
docker-compose down

# Remove old networks
docker network prune

# Create shared network
docker network create focushive-shared-network

# Start services with new configuration
docker-compose up -d
```

## ⚠️ Breaking Changes

Services that connect directly to exposed ports will need updates:
1. Database clients connecting to localhost:5432 → Use container names
2. Redis clients connecting to localhost:6379 → Use container names
3. External monitoring tools → Update connection strings

## 📚 Documentation Updates Required

1. Update README files with new port mappings
2. Update developer setup guides
3. Update production deployment docs
4. Create network troubleshooting guide