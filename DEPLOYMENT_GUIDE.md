# FocusHive Local Deployment Guide

## Prerequisites

1. **Docker Desktop** installed and running
2. **Java 21** (for local development)
3. **Node.js 20+** (for frontend development)
4. At least 4GB of free RAM

## Quick Start

### 1. Deploy Everything

```bash
# Run the deployment script
./deploy-local.sh
```

This will:
- Build all backend services
- Start PostgreSQL databases
- Start Redis instances
- Deploy Identity Service
- Deploy Main Backend
- Start Frontend (if configured)

### 2. Verify Services

After deployment, verify all services are running:

```bash
docker-compose ps
```

You should see:
- `focushive-db` (PostgreSQL for main app)
- `focushive-redis` (Redis for presence/cache)
- `identity-db` (PostgreSQL for identity service)
- `identity-redis` (Redis for identity service)
- `identity-service` (Authentication microservice)
- `focushive-backend` (Main application)

## Testing the Real-time Presence System

### 1. Register and Login

First, create a test user:

```bash
# Register a new user
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "password123",
    "fullName": "Test User One"
  }'

# Login to get access token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "password123"
  }'
```

Save the access token from the login response.

### 2. Create a Hive

```bash
# Replace YOUR_TOKEN with the actual token
curl -X POST http://localhost:8080/api/v1/hives \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Study Room 1",
    "description": "A focused study environment",
    "type": "STUDY",
    "visibility": "PUBLIC",
    "maxMembers": 10
  }'
```

### 3. Test WebSocket Presence

You can test the WebSocket presence system using a WebSocket client or this Node.js script:

```javascript
// save as test-presence.js
const WebSocket = require('ws');
const { v4: uuidv4 } = require('uuid');

const token = 'YOUR_ACCESS_TOKEN'; // Replace with actual token
const hiveId = 'YOUR_HIVE_ID'; // Replace with actual hive ID

// Connect to WebSocket
const ws = new WebSocket(`ws://localhost:8080/ws`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

// STOMP protocol handshake
ws.on('open', () => {
  console.log('Connected to WebSocket');
  
  // Send STOMP CONNECT frame
  ws.send('CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0');
});

ws.on('message', (data) => {
  console.log('Received:', data.toString());
  
  // After CONNECTED frame, subscribe and send presence
  if (data.toString().includes('CONNECTED')) {
    // Subscribe to hive presence updates
    const subId = uuidv4();
    ws.send(`SUBSCRIBE\nid:${subId}\ndestination:/topic/hive/${hiveId}/presence\n\n\0`);
    
    // Join hive presence
    ws.send(`SEND\ndestination:/app/hive/${hiveId}/join\n\n{}\0`);
    
    // Send presence update
    ws.send(`SEND\ndestination:/app/presence/update\n\n${JSON.stringify({
      status: 'ONLINE',
      hiveId: hiveId,
      activity: 'Studying for exam'
    })}\0`);
    
    // Start heartbeat
    setInterval(() => {
      ws.send('SEND\ndestination:/app/presence/heartbeat\n\n{}\0');
    }, 20000); // every 20 seconds
  }
});

// Run: npm install ws uuid && node test-presence.js
```

### 4. REST API Testing

Test presence REST endpoints:

```bash
# Get current user presence
curl -X GET http://localhost:8080/api/v1/presence/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get hive presence info
curl -X GET http://localhost:8080/api/v1/presence/hives/YOUR_HIVE_ID \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get multiple hives presence
curl -X POST http://localhost:8080/api/v1/presence/hives/batch \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '["hive-id-1", "hive-id-2"]'
```

### 5. Start a Focus Session

```bash
# Using WebSocket (in the test-presence.js script)
ws.send(`SEND\ndestination:/app/session/start\n\n${JSON.stringify({
  hiveId: hiveId,
  durationMinutes: 25
})}\0`);

# Or check active session via REST
curl -X GET http://localhost:8080/api/v1/presence/sessions/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Monitoring

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f identity-service

# Filter for presence-related logs
docker-compose logs -f backend | grep -i presence
```

### Check Redis Data

```bash
# Connect to Redis CLI
docker exec -it focushive-redis redis-cli -a focushive_pass

# View presence keys
KEYS presence:*

# Get specific user presence
GET presence:user:USER_ID

# View hive members
GET presence:hive:HIVE_ID
```

### Database Access

```bash
# Connect to main database
docker exec -it focushive-db psql -U focushive_user -d focushive

# Connect to identity database
docker exec -it identity-db psql -U identity_user -d identity_db
```

## API Documentation

When running, you can access:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## Common Issues

### 1. Port Already in Use
```bash
# Check what's using the port
lsof -i :8080

# Stop conflicting service or change port in docker-compose.yml
```

### 2. Database Connection Failed
```bash
# Ensure databases are healthy
docker-compose ps
docker-compose logs db

# Restart database
docker-compose restart db
```

### 3. Redis Connection Issues
```bash
# Check Redis is running
docker exec -it focushive-redis redis-cli -a focushive_pass ping
# Should return: PONG
```

### 4. Build Failures
```bash
# Clean and rebuild
docker-compose down -v
docker system prune -a
./deploy-local.sh
```

## Stopping Services

```bash
# Stop all services (preserves data)
docker-compose down

# Stop and remove all data
docker-compose down -v

# Stop specific service
docker-compose stop backend
```

## Development Tips

1. **Hot Reload**: The frontend supports hot reload. Backend requires restart for changes.

2. **Direct Database Access**: Use pgAdmin or DBeaver with:
   - Host: localhost
   - Port: 5432 (main) or 5433 (identity)
   - Username/Password: See docker-compose.yml

3. **Redis GUI**: Use RedisInsight or Redis Commander:
   ```bash
   docker run -d -p 8001:8001 redislabs/redisinsight:latest
   ```

4. **API Testing**: Use Postman, Insomnia, or Thunder Client with the provided curl examples.

## Next Steps

1. **Test Multiple Users**: Create multiple users and test real-time presence updates
2. **Load Testing**: Use Apache JMeter or k6 for WebSocket load testing
3. **Frontend Integration**: The frontend at http://localhost:5173 should connect automatically

## Useful Commands Cheatsheet

```bash
# Deploy everything
./deploy-local.sh

# View all logs
docker-compose logs -f

# Restart a service
docker-compose restart backend

# Execute commands in container
docker exec -it focushive-backend sh

# Clean everything
docker-compose down -v
docker system prune -a

# Check service health
docker-compose ps

# View resource usage
docker stats
```