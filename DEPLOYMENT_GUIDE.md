# FocusHive Deployment Guide

## Quick Start (New Machine)

### Option 1: Automated Deployment (Recommended)
```bash
# Clone the repository
git clone <repository-url>
cd focushive

# Run the master deployment script
./deploy-focushive.sh
```

### Option 2: Manual Deployment
```bash
# Step 1: Create the shared network (REQUIRED)
docker network create focushive-shared-network

# Step 2: Deploy services individually
cd services/notification-service && docker-compose up -d
cd ../identity-service && docker-compose up -d  
cd ../buddy-service && docker-compose up -d
cd ../focushive-backend && docker-compose up -d
```

## Why the Shared Network?

All FocusHive services use a shared Docker network (`focushive-shared-network`) to communicate with each other. This prevents `UnknownHostException` errors that occur when services are isolated on separate networks.

**Services communicate using container names:**
- `focushive-identity-service-app:8081`
- `focushive-notification-service-app:8083`
- `focushive_buddy_service_app:8087`
- `focushive_backend_main:8080`

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Identity Service | 8081 | http://localhost:8081 |
| Notification Service | 8083 | http://localhost:8083 |
| Buddy Service | 8087 | http://localhost:8087 |
| Backend Service | 8080 | http://localhost:8080 |

## Troubleshooting

### Network Issues
If services can't communicate:
```bash
# Check if shared network exists
docker network ls | grep focushive-shared-network

# Check which containers are connected
docker network inspect focushive-shared-network

# Recreate network if needed
docker network rm focushive-shared-network
docker network create focushive-shared-network

# Redeploy services
./deploy-focushive.sh
```

### Service Communication Test
```bash
# Test identity → notification service communication
docker exec focushive-identity-service-app curl http://focushive-notification-service-app:8083/actuator/health
```

## Development vs Production

This setup works for both development and production. The shared network ensures consistent inter-service communication regardless of environment.

## What's Automated

✅ **Shared network creation**  
✅ **Service deployment in correct order**  
✅ **Health checks and status reporting**  
✅ **Container connectivity verification**  

## What's Manual

❌ **Environment variables** (create .env files as needed)  
❌ **SSL certificates** (for production HTTPS)  
❌ **External database setup** (if not using Docker containers)