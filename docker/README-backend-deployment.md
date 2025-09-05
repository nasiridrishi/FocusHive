# Backend Internal Deployment Guide

This document explains the internal-only backend deployment configuration that follows zero-trust architecture principles.

## Overview

The `docker-compose.backend-internal.yml` configuration creates a fully isolated backend environment with no exposed ports to the host system. All services communicate internally via the `focushive-network` Docker network.

## Architecture

### Zero-Trust Design
- **No Host Ports**: All services run on internal Docker network only
- **Internal Communication**: Services communicate via container names
- **External Access**: Only via Cloudflare tunnel routing
- **Security**: No direct access from host or external networks

### Service Layout
```
┌─────────────────────────────────────────────────┐
│                Docker Host                       │
│                                                 │
│  ┌─────────────────────────────────────────────┐│
│  │            focushive-network                ││
│  │                                             ││
│  │  ┌─────────────┐  ┌─────────────────────┐   ││
│  │  │   backend   │  │  identity-service   │   ││
│  │  │   :8080     │  │      :8081          │   ││
│  │  └─────────────┘  └─────────────────────┘   ││
│  │                                             ││
│  │  ┌─────────────┐  ┌─────────────────────┐   ││
│  │  │     db      │  │    identity-db      │   ││
│  │  │   :5432     │  │      :5432          │   ││
│  │  └─────────────┘  └─────────────────────┘   ││
│  │                                             ││
│  │  ┌─────────────┐  ┌─────────────────────┐   ││
│  │  │   redis     │  │  identity-redis     │   ││
│  │  │   :6379     │  │      :6379          │   ││
│  │  └─────────────┘  └─────────────────────┘   ││
│  └─────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

### External Access via Cloudflare Tunnel
```
Internet → Cloudflare → Tunnel → Docker Network
                     ↓
    dev.focushive.app/api → backend:8080
```

## Services

### Backend Service
- **Container Name**: `backend` (required for Cloudflare routing)
- **Internal Port**: 8080
- **Health Check**: `/actuator/health`
- **Dependencies**: db, redis, identity-service

### Identity Service
- **Container Name**: `identity-service`
- **Internal Port**: 8081
- **Health Check**: `/actuator/health`
- **Dependencies**: identity-db, identity-redis

### Databases
- **focushive-db**: Main application PostgreSQL (port 5432)
- **identity-db**: Identity service PostgreSQL (port 5432)
- **focushive-redis**: Main application Redis (port 6379)
- **identity-redis**: Identity service Redis (port 6379)

## Deployment

### Prerequisites
- Docker and Docker Compose installed
- Cloudflare tunnel configured for external access

### Deploy Backend Services
```bash
cd docker
./deploy-backend.sh
```

The deployment script will:
1. Stop existing containers
2. Build fresh Docker images
3. Start infrastructure services (databases, Redis)
4. Wait for health checks to pass
5. Start application services
6. Verify all services are healthy
7. Test internal connectivity

### Verification
After deployment, verify services are running:
```bash
# Check container status
docker ps | grep -E "(backend|identity|focushive|redis|db)"

# Check health status
docker inspect --format='{{.State.Health.Status}}' backend
docker inspect --format='{{.State.Health.Status}}' identity-service

# Test internal connectivity
docker exec backend wget -q -O - http://localhost:8080/actuator/health
docker exec identity-service wget -q -O - http://localhost:8081/actuator/health
```

## Management

### View Logs
```bash
# Backend service
docker logs backend

# Identity service
docker logs identity-service

# Database logs
docker logs focushive-db
docker logs identity-db
```

### Restart Services
```bash
# Restart specific service
docker compose -f docker-compose.backend-internal.yml restart backend

# Restart all services
docker compose -f docker-compose.backend-internal.yml restart
```

### Stop Services
```bash
# Stop all backend services
docker compose -f docker-compose.backend-internal.yml down

# Stop and remove volumes (data loss!)
docker compose -f docker-compose.backend-internal.yml down -v
```

### Shell Access
```bash
# Access backend container
docker exec -it backend /bin/bash

# Access identity service container
docker exec -it identity-service /bin/bash

# Access database
docker exec -it focushive-db psql -U focushive_user -d focushive
```

## Network Security

### Internal-Only Communication
All services communicate exclusively via the `focushive-network` Docker network:
- Services are accessible by container name (e.g., `http://backend:8080`)
- No ports exposed to Docker host
- No direct external access possible

### CORS Configuration
Services are configured for internal-only CORS:
- **Backend**: Allows all origins (handled by Cloudflare tunnel)
- **Identity Service**: Only allows backend service access

### Health Checks
All services include comprehensive health checks:
- **Databases**: Connection and readiness checks
- **Applications**: Spring Boot Actuator health endpoints
- **Start Period**: Allows services time to initialize
- **Retry Logic**: Automatic recovery from temporary failures

## Troubleshooting

### Service Won't Start
1. Check logs: `docker logs <service-name>`
2. Verify dependencies are healthy
3. Check resource availability
4. Restart with fresh build

### Health Check Failures
1. Increase health check timeout in compose file
2. Verify service is listening on correct port
3. Check application configuration
4. Review dependency health

### Network Connectivity Issues
1. Verify all services are on `focushive-network`
2. Check service names match configuration
3. Test internal connectivity with `docker exec`
4. Review firewall/security group settings

### External Access Issues
1. Verify Cloudflare tunnel is running
2. Check tunnel configuration routes to `backend:8080`
3. Ensure tunnel can reach Docker network
4. Test internal health endpoints first

## Security Considerations

### Zero-Trust Benefits
- **No Attack Surface**: No exposed ports on host system
- **Network Isolation**: Services isolated within Docker network
- **Access Control**: External access only via authenticated tunnel
- **Monitoring**: All traffic flows through controlled entry point

### Production Recommendations
- Use Docker secrets for sensitive configuration
- Implement network policies for additional isolation
- Enable Docker content trust
- Regular security scanning of images
- Monitor and audit all access attempts