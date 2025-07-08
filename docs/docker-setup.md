# Docker Setup for FocusHive

This document provides instructions for running FocusHive using Docker.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)
- At least 4GB of available RAM for Docker

## Architecture

The FocusHive Docker setup includes the following services:

- **Frontend**: React application served by Vite in development (port 5173)
- **Backend**: Spring Boot API server (port 8080)
- **PostgreSQL**: Relational database for all application data (port 5432)
- **Redis**: In-memory cache for real-time presence data (port 6379)

Note: MongoDB was removed in favor of using PostgreSQL for all data storage needs.

## Quick Start

1. Clone the repository and navigate to the project root:
   ```bash
   git clone <repository-url>
   cd focushive
   ```

2. Copy the environment example file:
   ```bash
   cp .env.example .env
   ```

3. Start all services:
   ```bash
   docker-compose up -d
   ```

4. Access the application:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

## Docker Commands

### Basic Operations

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f backend

# Restart a service
docker-compose restart backend

# Clean up (stop services and remove volumes)
docker-compose down -v
```

### Development Commands

```bash
# Rebuild services after code changes
docker-compose build

# Rebuild specific service
docker-compose build backend

# Rebuild without cache
docker-compose build --no-cache

# Access container shell
docker exec -it focushive-backend sh
docker exec -it focushive-web sh
```

### Database Access

```bash
# PostgreSQL shell
docker exec -it focushive-db psql -U focushive_user -d focushive

# Redis CLI
docker exec -it focushive-redis redis-cli -a focushive_pass
```

## Service Details

### Frontend (React + Vite)
- **Port**: 5173
- **Access**: http://localhost:5173
- **Hot Reload**: Enabled
- **Source**: `./frontend/web`
- **Build**: Multi-stage Dockerfile with nginx for production

### Backend (Spring Boot)
- **Port**: 8080
- **Access**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Base**: http://localhost:8080/api
- **WebSocket**: http://localhost:8080/ws
- **Source**: `./backend`
- **Build**: Multi-stage Dockerfile with JDK 21

### PostgreSQL
- **Port**: 5432
- **Database**: focushive
- **User**: focushive_user
- **Password**: focushive_pass
- **Version**: 16-alpine

### Redis
- **Port**: 6379
- **Password**: focushive_pass
- **Version**: 7-alpine

## Environment Variables

Key environment variables (configured in `.env`):

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=focushive
DB_USER=focushive_user
DB_PASSWORD=focushive_pass

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=focushive_pass

# Application Configuration
SERVER_PORT=8080
LOG_LEVEL=INFO
SHOW_SQL=false

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION=86400000

# Frontend Configuration
VITE_API_URL=http://localhost:8080
```

## Troubleshooting

### Container fails to start

1. Check logs:
   ```bash
   docker-compose logs backend
   docker-compose logs web
   ```

2. Ensure ports are not in use:
   ```bash
   lsof -i :5173  # Frontend port
   lsof -i :8080  # Backend port
   lsof -i :5432  # PostgreSQL port
   lsof -i :6379  # Redis port
   ```

3. Clean rebuild:
   ```bash
   docker-compose down -v
   docker-compose build --no-cache
   docker-compose up -d
   ```

### Database connection issues

1. Ensure containers are healthy:
   ```bash
   docker ps
   docker-compose ps
   ```

2. Test database connection:
   ```bash
   docker exec focushive-db pg_isready -U focushive_user
   ```

3. Check Redis connection:
   ```bash
   docker exec focushive-redis redis-cli -a focushive_pass ping
   ```

### Frontend can't connect to backend

1. Check backend is running:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. Verify environment variables:
   ```bash
   docker exec focushive-web env | grep VITE_API_URL
   ```

3. Check nginx proxy configuration in production mode

## Development Workflow

1. **Backend Changes (Spring Boot)**:
   - Code changes require container rebuild
   - Use `docker-compose build backend && docker-compose up -d backend`
   - Or run locally with `cd backend && ./gradlew bootRun`

2. **Frontend Changes (React)**:
   - Hot reload is enabled in development mode
   - Changes are reflected immediately
   - For production build: `docker-compose build web`

3. **Database Changes**:
   - Use Flyway migrations in `backend/src/main/resources/db/migration`
   - Migrations run automatically on startup

## Production Considerations

The current Docker setup is optimized for development. For production:

1. Use production builds (already configured in multi-stage Dockerfiles)
2. Update passwords and secrets in environment variables
3. Use Docker Swarm or Kubernetes for orchestration
4. Configure proper health checks and monitoring
5. Set up volume backups for databases
6. Enable HTTPS with proper certificates
7. Use external Redis cluster for scalability
8. Configure Spring Boot profiles for production settings
9. Set up proper logging aggregation (ELK stack)
10. Implement security headers and CORS policies