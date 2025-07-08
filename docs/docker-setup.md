# Docker Setup for FocusHive

This document provides instructions for running FocusHive using Docker.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)
- At least 4GB of available RAM for Docker

## Architecture

The FocusHive Docker setup includes the following services:

- **Frontend**: React application served by Vite (port 5173)
- **Backend**: Node.js Express API server (internal only)
- **PostgreSQL**: Relational database for users, hives, and memberships (internal only)
- **MongoDB**: Document database for user preferences and activity logs (internal only)
- **Redis**: In-memory cache for real-time presence data (internal only)

All database services use internal Docker networking for security. Only the frontend is exposed externally.

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
   ./docker-utils.sh up
   ```

4. Access the application at http://localhost:5173

## Docker Utilities Script

The project includes a `docker-utils.sh` script for common Docker operations:

```bash
# Start all services
./docker-utils.sh up

# Stop all services
./docker-utils.sh down

# Restart all services
./docker-utils.sh restart

# View logs (all services)
./docker-utils.sh logs

# View logs (specific service)
./docker-utils.sh logs backend

# Open shell in a service
./docker-utils.sh shell backend

# Open database shell
./docker-utils.sh db-shell postgres
./docker-utils.sh db-shell mongodb
./docker-utils.sh db-shell redis

# Clean up (stop services and remove volumes)
./docker-utils.sh clean
```

## Service Details

### Frontend
- **Port**: 5173
- **Access**: http://localhost:5173
- **Hot Reload**: Enabled
- **API URL**: Configured to connect to backend via Docker network

### Backend
- **Port**: 3000 (internal only)
- **Health Check**: http://backend:3000/health
- **API Base**: http://backend:3000/api/v1

### PostgreSQL
- **Port**: 5432 (internal only)
- **Database**: focushive
- **User**: focushive_user
- **Password**: focushive_pass

### MongoDB
- **Port**: 27017 (internal only)
- **Database**: focushive
- **User**: focushive_user
- **Password**: focushive_pass

### Redis
- **Port**: 6379 (internal only)
- **Password**: focushive_pass

## Environment Variables

Key environment variables (configured in `.env`):

```bash
# Database Configuration
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=focushive
POSTGRES_USER=focushive_user
POSTGRES_PASSWORD=focushive_pass

# MongoDB Configuration
MONGODB_URI=mongodb://focushive_user:focushive_pass@mongodb:27017/focushive?authSource=admin

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=focushive_pass

# Application Configuration
NODE_ENV=development
PORT=3000
FRONTEND_URL=http://localhost:5173

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRES_IN=7d
```

## Building Images

To rebuild Docker images after code changes:

```bash
# Rebuild all services
docker-compose build

# Rebuild specific service
docker-compose build backend

# Rebuild without cache
docker-compose build --no-cache
```

## Troubleshooting

### Container fails to start

1. Check logs:
   ```bash
   docker logs focushive-backend
   ```

2. Ensure ports are not in use:
   ```bash
   lsof -i :5173  # Frontend port
   ```

3. Clean rebuild:
   ```bash
   ./docker-utils.sh clean
   ./docker-utils.sh up
   ```

### Database connection issues

1. Ensure containers are healthy:
   ```bash
   docker ps
   ```

2. Test database connections:
   ```bash
   ./docker-utils.sh db-shell postgres
   ./docker-utils.sh db-shell mongodb
   ./docker-utils.sh db-shell redis
   ```

### Frontend can't connect to backend

1. Check backend is running:
   ```bash
   docker exec focushive-backend wget -qO- http://localhost:3000/health
   ```

2. Verify environment variables:
   ```bash
   docker exec focushive-frontend env | grep VITE_API_URL
   ```

## Development Workflow

1. Make code changes in your local editor
2. For backend changes, rebuild and restart:
   ```bash
   docker-compose build backend
   docker restart focushive-backend
   ```
3. For frontend changes, the dev server will hot-reload automatically
4. View logs to debug issues:
   ```bash
   ./docker-utils.sh logs backend
   ```

## Production Considerations

The current Docker setup is optimized for development. For production:

1. Use production builds (already configured)
2. Update passwords and secrets in environment variables
3. Use Docker Swarm or Kubernetes for orchestration
4. Configure proper health checks and monitoring
5. Set up volume backups for databases
6. Use a reverse proxy (nginx) for the frontend
7. Enable HTTPS with proper certificates