# Scripts Directory

This directory contains all shell scripts and database initialization files for the FocusHive project.

## Structure

```
scripts/
├── dev/                    # Development scripts
│   ├── start-backend.sh    # Start backend services locally
│   ├── start-lan.sh        # Start services for LAN access
│   ├── run-local.sh        # Run services without Docker
│   └── stop-local.sh       # Stop local services
├── deploy/                 # Deployment scripts
│   ├── deploy-local.sh     # Deploy to local Docker
│   └── deploy-remote-docker.sh  # Deploy to remote Docker
├── db/                     # Database scripts
│   ├── init-db.sh          # Initialize main database
│   ├── init-identity-db.sh # Initialize identity database
│   ├── dev-init-db.sh      # Development database setup
│   ├── dev-init-identity-db.sh  # Development identity DB setup
│   └── create-demo-user.sql     # Create demo user for testing
└── utils/                  # Utility scripts
    └── setup-git-hooks.sh  # Setup Git hooks for development
```

## Development Scripts

### `dev/start-backend.sh`
Starts the FocusHive backend service locally with proper environment configuration.

```bash
./scripts/dev/start-backend.sh
```

### `dev/start-lan.sh`
Starts all services and makes them accessible on your local network for testing with mobile devices or other computers.

```bash
./scripts/dev/start-lan.sh
```

### `dev/run-local.sh`
Runs all services locally without Docker, useful for development with live reload.

```bash
./scripts/dev/run-local.sh
```

### `dev/stop-local.sh`
Stops all locally running services and cleans up process files.

```bash
./scripts/dev/stop-local.sh
```

## Deployment Scripts

### `deploy/deploy-local.sh`
Deploys the full FocusHive stack to local Docker with production-like configuration.

```bash
./scripts/deploy/deploy-local.sh
```

Features:
- Builds all services from source
- Performs health checks
- Shows running services and access points
- Provides useful management commands

### `deploy/deploy-remote-docker.sh`
Deploys FocusHive to a remote Docker host (configured for 192.168.2.3).

```bash
./scripts/deploy/deploy-remote-docker.sh
```

## Database Scripts

### `db/init-db.sh`
Initializes the main FocusHive PostgreSQL database with required schema and data.

### `db/init-identity-db.sh`
Initializes the identity service PostgreSQL database with OAuth2 and user management schema.

### `db/dev-init-db.sh` & `db/dev-init-identity-db.sh`
Development versions of the database initialization scripts with additional:
- Debug logging enabled
- Development-friendly auth settings
- Sample data for testing

### `db/create-demo-user.sql`
SQL script to create demo users for testing and development.

```sql
-- Creates users with known credentials for testing
-- See file for specific usernames and passwords
```

## Utility Scripts

### `utils/setup-git-hooks.sh`
Sets up Git hooks for the development environment to ensure code quality.

```bash
./scripts/utils/setup-git-hooks.sh
```

## Usage Examples

### Quick Development Setup
```bash
# Start just the databases
./scripts/dev/start-lan.sh

# In another terminal, start the backend
./scripts/dev/start-backend.sh

# Start frontend separately (see frontend/README.md)
cd frontend && npm run dev
```

### Full Local Deployment
```bash
# Deploy everything with Docker
./scripts/deploy/deploy-local.sh

# Access at:
# - Frontend: http://localhost:5173
# - Backend: http://localhost:8080
# - Identity: http://localhost:8081
```

### Database Management
```bash
# Reset development database
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d db

# Create demo users
psql -h localhost -p 5434 -U focushive_user -d focushive -f scripts/db/create-demo-user.sql
```

## Environment Variables

Scripts use environment variables for configuration:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `IDENTITY_SERVICE_URL`
- `JWT_SECRET`, `JWT_EXPIRATION`

Set these in your shell or use the `.env` files in the `docker/` directory.

## Permissions

Make sure scripts are executable:
```bash
find scripts/ -name "*.sh" -exec chmod +x {} \;
```

## Troubleshooting

### Script Execution Issues
- Ensure scripts are executable: `chmod +x scripts/path/to/script.sh`
- Check that Docker is running: `docker info`
- Verify environment variables are set

### Database Connection Issues
- Check database container status: `docker compose -f docker/docker-compose.yml ps`
- Verify database credentials in environment variables
- Check database logs: `docker compose -f docker/docker-compose.yml logs db`

### Service Startup Issues
- Check service logs: `docker compose -f docker/docker-compose.yml logs service-name`
- Verify all dependencies are started
- Check port conflicts: `netstat -tulpn | grep :8080`