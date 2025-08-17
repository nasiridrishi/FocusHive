# FocusHive Docker Setup Guide

Complete guide for setting up and managing the FocusHive docker environment including the Identity Service.

## Overview

FocusHive uses a multi-service Docker Compose setup with:

- **Main Backend Service**: Spring Boot application on port 8080
- **Identity Service**: OAuth2 authorization server on port 8081
- **Frontend**: React application on port 5173
- **NGINX**: API Gateway and reverse proxy on port 80
- **Databases**: PostgreSQL instances for each service (ports 5432, 5433)
- **Redis**: Caching and session storage (ports 6379, 6380)

## File Structure

```
focushive/
├── docker-compose.yml              # Main configuration
├── docker-compose.override.yml     # Development overrides (auto-loaded)
├── docker-compose.prod.yml         # Production configuration
├── docker-compose.test.yml         # Testing environment
├── .env.example                    # Environment variables template
├── nginx/
│   ├── nginx.conf                  # Main NGINX configuration
│   ├── conf.d/                     # Shared NGINX configurations
│   ├── dev/                        # Development NGINX config
│   ├── prod/                       # Production NGINX config
│   └── test/                       # Testing NGINX config
└── scripts/
    ├── init-db.sh                  # Main database initialization
    ├── init-identity-db.sh         # Identity database initialization
    ├── dev-init-db.sh              # Development database setup
    └── dev-init-identity-db.sh     # Development identity database setup
```

## Environment Configuration

### 1. Copy Environment Template

```bash
cp .env.example .env
```

### 2. Key Environment Variables

#### Database Configuration
```bash
# Main Application Database
DB_NAME=focushive
DB_USER=focushive_user
DB_PASSWORD=focushive_pass
DB_EXTERNAL_PORT=5432

# Identity Service Database
IDENTITY_DB_NAME=identity_db
IDENTITY_DB_USER=identity_user
IDENTITY_DB_PASSWORD=identity_pass
IDENTITY_DB_EXTERNAL_PORT=5433
```

#### Redis Configuration
```bash
# Main Redis
REDIS_PASSWORD=focushive_pass
REDIS_EXTERNAL_PORT=6379

# Identity Service Redis
IDENTITY_REDIS_PASSWORD=identity_redis_pass
IDENTITY_REDIS_EXTERNAL_PORT=6380
```

#### Security Configuration
```bash
# Identity Service Security
KEY_STORE_PASSWORD=changeme
PRIVATE_KEY_PASSWORD=changeme
FOCUSHIVE_CLIENT_SECRET=secret

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION=86400000
```

#### Service Ports
```bash
BACKEND_PORT=8080
IDENTITY_SERVICE_PORT=8081
FRONTEND_PORT=5173
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
```

## Deployment Environments

### Development Environment (Default)

**Features:**
- Automatic reloading
- Debug logging
- Development tools (Adminer, Redis Commander, Traefik)
- Permissive CORS settings
- Extended logging

**Command:**
```bash
docker-compose up -d
```

**Available Services:**
- Frontend: http://localhost:5173
- API Gateway: http://localhost:80
- Backend: http://localhost:8080
- Identity Service: http://localhost:8081
- Adminer: http://localhost:8082
- Redis Commander: http://localhost:8083
- Traefik Dashboard: http://localhost:8084

### Production Environment

**Features:**
- Optimized JVM settings
- Production security headers
- Monitoring (Prometheus, Grafana)
- Resource limits
- SSL/TLS support

**Command:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**Additional Services:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

### Testing Environment

**Features:**
- Isolated test databases
- Fast startup (optimized for CI/CD)
- Integration test support
- E2E test support

**Command:**
```bash
# Basic test environment
docker-compose -f docker-compose.yml -f docker-compose.test.yml up -d

# Run integration tests
docker-compose -f docker-compose.yml -f docker-compose.test.yml --profile integration-tests up --abort-on-container-exit

# Run E2E tests
docker-compose -f docker-compose.yml -f docker-compose.test.yml --profile e2e-tests up --abort-on-container-exit
```

## Service Details

### Identity Service

**Purpose:** OAuth2 authorization server providing user identity management and multi-persona support.

**Key Features:**
- OAuth2.1 compliant authorization server
- Multiple user personas/profiles
- Advanced privacy controls
- JWT token management
- Inter-service authentication

**Health Check:** `http://localhost:8081/api/v1/health`

**Database:** Dedicated PostgreSQL instance on port 5433
**Cache:** Dedicated Redis instance on port 6380

### NGINX API Gateway

**Purpose:** Reverse proxy and API gateway for all services.

**Routing Rules:**
- `/` → Frontend (React application)
- `/api/*` → Main Backend (Spring Boot)
- `/api/identity/*` → Identity Service
- `/ws` → WebSocket connections to Main Backend

**Features:**
- Rate limiting
- CORS handling
- Security headers
- Load balancing
- SSL termination (production)

### Database Services

**Main Database (PostgreSQL):**
- Port: 5432
- Database: `focushive`
- User: `focushive_user`

**Identity Database (PostgreSQL):**
- Port: 5433
- Database: `identity_db`
- User: `identity_user`

**Redis Services:**
- Main Redis: Port 6379
- Identity Redis: Port 6380

## Health Checks and Monitoring

### Health Check Endpoints

```bash
# Check all service health
curl http://localhost/health                    # NGINX health
curl http://localhost:8080/actuator/health     # Backend health
curl http://localhost:8081/api/v1/health       # Identity Service health
```

### Database Health Checks

```bash
# PostgreSQL health checks
docker-compose exec db pg_isready -U focushive_user -d focushive
docker-compose exec identity-db pg_isready -U identity_user -d identity_db

# Redis health checks
docker-compose exec redis redis-cli -a focushive_pass ping
docker-compose exec identity-redis redis-cli -a identity_redis_pass ping
```

### Service Logs

```bash
# View logs for all services
docker-compose logs

# View logs for specific service
docker-compose logs identity-service
docker-compose logs backend
docker-compose logs nginx
docker-compose logs db
docker-compose logs identity-db

# Follow logs in real-time
docker-compose logs -f identity-service
```

## Database Management

### Database Access

```bash
# Main database
docker-compose exec db psql -U focushive_user -d focushive

# Identity database
docker-compose exec identity-db psql -U identity_user -d identity_db
```

### Using Adminer (Development)

1. Access: http://localhost:8082
2. Server: `db` (for main) or `identity-db` (for identity)
3. Username: `focushive_user` or `identity_user`
4. Password: From your `.env` file
5. Database: `focushive` or `identity_db`

### Redis Management

```bash
# Main Redis CLI
docker-compose exec redis redis-cli -a focushive_pass

# Identity Redis CLI
docker-compose exec identity-redis redis-cli -a identity_redis_pass
```

### Using Redis Commander (Development)

Access: http://localhost:8083

Pre-configured connections to both Redis instances.

## Troubleshooting

### Common Issues

#### 1. Port Conflicts
```bash
# Check if ports are in use
netstat -tulpn | grep -E ':(80|5173|8080|8081|5432|5433|6379|6380) '

# Stop conflicting services
sudo systemctl stop nginx
sudo systemctl stop postgresql
sudo systemctl stop redis
```

#### 2. Container Startup Issues
```bash
# Check container status
docker-compose ps

# Restart unhealthy services
docker-compose restart identity-service
docker-compose restart backend

# Rebuild containers
docker-compose build --no-cache
```

#### 3. Database Connection Issues
```bash
# Check database logs
docker-compose logs db
docker-compose logs identity-db

# Test database connectivity
docker-compose exec backend wget -qO- http://identity-service:8081/api/v1/health
```

#### 4. NGINX Routing Issues
```bash
# Check NGINX configuration
docker-compose exec nginx nginx -t

# Reload NGINX configuration
docker-compose exec nginx nginx -s reload

# Check NGINX logs
docker-compose logs nginx
```

### Debug Commands

```bash
# Enter container shell
docker-compose exec identity-service /bin/sh
docker-compose exec backend /bin/sh

# Check network connectivity
docker-compose exec backend ping identity-service
docker-compose exec identity-service ping identity-db

# View environment variables
docker-compose exec identity-service env | grep -E '(DB_|REDIS_)'
```

### Performance Monitoring

#### Resource Usage
```bash
# Check resource usage
docker stats

# Check specific service
docker stats focushive-identity-service focushive-backend
```

#### Database Performance
```bash
# PostgreSQL performance (development)
docker-compose exec db psql -U focushive_user -d focushive -c "
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;"
```

## Backup and Restore

### Database Backup
```bash
# Backup main database
docker-compose exec db pg_dump -U focushive_user focushive > backup_main.sql

# Backup identity database
docker-compose exec identity-db pg_dump -U identity_user identity_db > backup_identity.sql
```

### Database Restore
```bash
# Restore main database
docker-compose exec -T db psql -U focushive_user focushive < backup_main.sql

# Restore identity database
docker-compose exec -T identity-db psql -U identity_user identity_db < backup_identity.sql
```

## Security Considerations

### Production Security Checklist

- [ ] Change all default passwords
- [ ] Use strong JWT secrets
- [ ] Configure SSL/TLS certificates
- [ ] Set appropriate CORS origins
- [ ] Review NGINX security headers
- [ ] Enable database encryption
- [ ] Set up log rotation
- [ ] Configure firewall rules
- [ ] Regular security updates

### Environment-Specific Security

**Development:**
- Permissive CORS for localhost
- Debug logging enabled
- Development tools exposed

**Production:**
- Strict CORS policy
- Minimal logging
- Security headers enforced
- Resource limits applied
- Monitoring enabled

## Performance Tuning

### JVM Optimization (Production)

Identity Service and Backend use optimized JVM settings:
```bash
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+DisableExplicitGC"
```

### Database Optimization

PostgreSQL configurations are tuned per environment:
- **Development:** Debug logging, relaxed settings
- **Production:** Optimized for performance and reliability

### Redis Configuration

Redis instances are configured with:
- Memory limits
- Persistence settings
- Connection pooling
- Eviction policies

## Migration and Updates

### Updating Services

```bash
# Update all services
docker-compose pull
docker-compose up -d

# Update specific service
docker-compose pull identity-service
docker-compose up -d identity-service
```

### Database Migrations

Database migrations are handled by Flyway within the Spring Boot applications:
- Main Backend: `backend/src/main/resources/db/migration/`
- Identity Service: `identity-service/src/main/resources/db/migration/`

Migrations run automatically on service startup.

## Development Workflow

### Standard Development Flow

1. Start services: `docker-compose up -d`
2. Check health: `curl http://localhost/health`
3. Access logs: `docker-compose logs -f identity-service`
4. Make changes to code
5. Restart service: `docker-compose restart identity-service`
6. Test changes
7. Stop services: `docker-compose down`

### Hot Reloading

In development mode:
- Frontend supports hot module replacement
- Backend/Identity Service require container restart for code changes
- Database schema changes trigger automatic migration

This comprehensive setup provides a robust development, testing, and production environment for the FocusHive platform with full Identity Service integration.