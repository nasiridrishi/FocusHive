# FocusHive Docker Configuration

This directory contains all Docker configurations for the FocusHive application.

## Directory Structure

```
docker/
├── .env                           # Environment variables (create from .env.example)
├── README.md                      # This file
├── deploy-frontend.sh             # Frontend deployment script
│
├── docker-compose.yml             # Main development environment (all services)
├── docker-compose.override.yml    # Development overrides (auto-loaded)
├── docker-compose.prod.yml        # Production configuration
├── docker-compose.frontend.yml    # Frontend-only deployment with Cloudflare tunnel
├── docker-compose.backend.yml     # Backend-only deployment
├── docker-compose.monitoring.yml  # Monitoring stack (Prometheus, Grafana, etc.)
├── docker-compose.test.yml        # Testing environment
│
├── frontend/
│   ├── Dockerfile                 # Frontend multi-stage build
│   └── nginx.frontend-only.conf   # Nginx configuration for frontend
│
├── backend/
│   ├── Dockerfile                 # Backend Spring Boot build
│   └── application-docker.yml     # Spring Boot Docker config
│
├── identity/
│   ├── Dockerfile                 # Identity service build
│   └── application-docker.yml     # Identity service Docker config
│
└── postgres/
    └── init.sql                   # Database initialization scripts
```

## Docker Compose Files

### Main Files

| File | Purpose | Usage |
|------|---------|-------|
| `docker-compose.yml` | Complete development environment with all microservices | `docker compose up` |
| `docker-compose.override.yml` | Development overrides (auto-loaded) | Automatically applied |
| `docker-compose.prod.yml` | Production configuration | `docker compose -f docker-compose.yml -f docker-compose.prod.yml up` |

### Specialized Deployments

| File | Purpose | Usage |
|------|---------|-------|
| `docker-compose.frontend.yml` | Frontend-only with Cloudflare tunnel | `./deploy-frontend.sh` or `docker compose --env-file .env -f docker-compose.frontend.yml up` |
| `docker-compose.backend.yml` | Backend services only | `docker compose -f docker-compose.backend.yml up` |
| `docker-compose.monitoring.yml` | Monitoring stack | `docker compose -f docker-compose.monitoring.yml up` |
| `docker-compose.test.yml` | Testing environment | `docker compose -f docker-compose.test.yml up` |

## Security-First Architecture

This deployment follows a **zero-trust, defense-in-depth approach**:

### Security Features
- **No host port exposure** - Application ports are not exposed to the host machine (except for local testing)
- **Internal Docker network** - All services communicate via isolated Docker network
- **Single entry point** - Cloudflare Tunnel is the only external access point
- **Built-in nginx** - Frontend container includes nginx, no separate proxy needed
- **Security headers** - CSP, X-Frame-Options, X-Content-Type-Options configured
- **Rate limiting** - Nginx rate limiting configured for production

### Architecture

```
Internet
    ↓
Cloudflare Edge Network (DDoS protection, WAF)
    ↓
Cloudflare Tunnel (encrypted, zero-trust)
    ↓
Docker Network (internal)
    ↓
App Proxy (nginx:alpine)
    ↓
Frontend Container (nginx + React)
    - Port 80 (internal only)
    - Security headers
    - Rate limiting
    - Optimized bundles
```

## Quick Start

### 1. Setup Environment Variables

```bash
# Go to docker directory
cd docker/

# Create .env file from example
cp .env.example .env

# Edit .env with your configuration
nano .env
```

Required variables for frontend deployment:
```bash
CLOUDFLARE_TUNNEL_TOKEN=your-tunnel-token-here
VITE_API_BASE_URL=https://dev.focushive.app/api
VITE_WEBSOCKET_URL=wss://dev.focushive.app/ws
```

### 2. Frontend Deployment (with Cloudflare Tunnel)

```bash
# Use deployment script
./deploy-frontend.sh

# Or manually
docker compose --env-file .env -f docker-compose.frontend.yml up -d
```

### 3. Development Environment (Full Stack)

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

## Service Ports

### Development Services
- **Frontend**: http://localhost:3000 (via app proxy)
- **Backend API**: http://localhost:8080
- **Identity Service**: http://localhost:8081
- **PostgreSQL**: localhost:5432 (internal only in production)
- **Redis**: localhost:6379 (internal only in production)
- **PgAdmin**: http://localhost:5050 (development only)
- **Redis Commander**: http://localhost:8082 (development only)

### Microservices (docker-compose.yml)
- **Music Service**: http://localhost:8090
- **Notification Service**: http://localhost:8091
- **Chat Service**: http://localhost:8092
- **Analytics Service**: http://localhost:8093
- **Forum Service**: http://localhost:8094
- **Buddy Service**: http://localhost:8095

## Common Commands

### Build and Deploy

```bash
# Build all images
docker compose build

# Build without cache (fresh build)
docker compose build --no-cache

# Build specific service
docker compose build frontend

# Deploy with fresh build
docker compose up --build -d
```

### Logs and Debugging

```bash
# View all logs
docker compose logs

# View specific service logs
docker compose logs frontend

# Follow logs (real-time)
docker compose logs -f

# Last 100 lines
docker compose logs --tail 100

# Check container status
docker ps | grep focushive

# Inspect container
docker inspect focushive-dev-frontend
```

### Database Management

```bash
# Access PostgreSQL
docker compose exec postgres psql -U focushive

# Run migrations
docker compose exec backend npm run migrate

# Backup database
docker compose exec postgres pg_dump -U focushive focushive > backup.sql

# Restore database
docker compose exec -T postgres psql -U focushive focushive < backup.sql
```

### Cleanup

```bash
# Stop and remove containers
docker compose down

# Remove with volumes (WARNING: deletes data)
docker compose down -v

# Remove orphan containers
docker compose down --remove-orphans

# Clean up everything (WARNING: removes all Docker data)
docker system prune -a --volumes
```

## Troubleshooting

### Cloudflare Tunnel Issues

```bash
# Check tunnel logs
docker logs focushive-dev-tunnel

# Verify token is set
grep CLOUDFLARE_TUNNEL_TOKEN .env

# Check if token is valid (decode and inspect)
echo "your-token" | base64 -d | jq .

# Restart tunnel
docker restart focushive-dev-tunnel
```

**Common tunnel issues:**
- Invalid token: Get new token from Cloudflare Zero Trust dashboard
- Token has special characters: Ensure proper escaping in .env file
- Connection refused: Check if app service is running on port 3000

### Container Won't Start

```bash
# Check logs
docker compose logs [service-name]

# Check container status
docker ps -a | grep focushive

# Rebuild image
docker compose build --no-cache [service-name]

# Check for port conflicts
lsof -i :3000
```

### Database Connection Issues

```bash
# Check if database is ready
docker compose exec postgres pg_isready

# Check database logs
docker compose logs postgres

# Test connection
docker compose exec postgres psql -U focushive -c "SELECT 1"
```

### Frontend Build Issues

```bash
# Check build logs
docker compose build frontend --progress plain

# Verify environment variables
docker compose config | grep VITE

# Check nginx configuration
docker exec focushive-dev-frontend cat /etc/nginx/conf.d/default.conf
```

## Performance Optimizations

The frontend deployment includes several optimizations:

1. **Code Splitting**: Routes and large libraries are lazy loaded
   - Main bundle: ~70KB (from 3.86MB)
   - MUI Icons: Lazy loaded (3.7MB)
   - Charts: Separate chunk (174KB)

2. **Vendor Chunking**:
   ```javascript
   manualChunks: {
     'vendor-react': ['react', 'react-dom', 'react-router-dom'],
     'vendor-mui-core': ['@mui/material'],
     'vendor-mui-icons': ['@mui/icons-material'],
     'vendor-charts': ['@mui/x-charts'],
     'vendor-date': ['@mui/x-date-pickers', 'date-fns']
   }
   ```

3. **Security Headers**:
   - X-Frame-Options: SAMEORIGIN
   - X-Content-Type-Options: nosniff
   - X-XSS-Protection: 1; mode=block
   - Cache-Control configured per asset type

4. **Compression**: Gzip enabled for all text assets

## Notes

- **Organization**: All Docker files are in `docker/` directory
- **Auto-loading**: `docker-compose.override.yml` is automatically loaded in development
- **Environment**: Always use `--env-file .env` to ensure variables are loaded
- **Security**: Frontend deployment uses internal networks with Cloudflare as the only entry
- **Optimization**: Frontend includes code splitting, security headers, and Material Icons
- **Token Issues**: If Cloudflare token appears invalid, verify it's properly encoded and has no special characters

## Security Best Practices

1. **Never commit** `.env` files or credentials to git
2. **Use strong passwords** for all services
3. **Enable rate limiting** in production
4. **Keep images updated** with security patches
5. **Use internal networks** whenever possible
6. **Implement RBAC** for database access
7. **Monitor logs** for suspicious activity
8. **Regular backups** of persistent data