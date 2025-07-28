# FocusHive Usage Guide - Real-time Presence System

## 🚀 Quick Start (Local Development)

Since Docker is having issues, here's how to run locally:

### Prerequisites
- Java 21
- PostgreSQL
- Redis
- Node.js 20+

### 1. Install Dependencies (macOS)

```bash
# Install PostgreSQL
brew install postgresql
brew services start postgresql

# Install Redis
brew install redis
brew services start redis

# Or start Redis with password
redis-server --requirepass focushive_pass
```

### 2. Run Services Locally

```bash
# Make sure you've built the JARs first
cd identity-service && ./gradlew build
cd ../backend && ./gradlew build
cd ..

# Run all services
./run-local.sh

# Stop all services
./stop-local.sh
```

## 📝 Testing the Real-time Presence System

### Step 1: Register Users

```bash
# Register first user
curl -X POST http://192.168.2.3:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123",
    "fullName": "Alice Smith"
  }'

# Register second user
curl -X POST http://192.168.2.3:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "email": "bob@example.com",
    "password": "password123",
    "fullName": "Bob Jones"
  }'
```

### Step 2: Login and Get Tokens

```bash
# Login as Alice
curl -X POST http://192.168.2.3:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "password123"
  }'

# Save the accessToken from response as ALICE_TOKEN

# Login as Bob
curl -X POST http://192.168.2.3:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "password": "password123"
  }'

# Save the accessToken from response as BOB_TOKEN
```

### Step 3: Create a Hive

```bash
# Alice creates a study hive
curl -X POST http://192.168.2.3:8080/api/v1/hives \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Deep Focus Room",
    "description": "A quiet space for deep work",
    "type": "STUDY",
    "visibility": "PUBLIC",
    "maxMembers": 10
  }'

# Save the hive.id from response as HIVE_ID
```

### Step 4: Join the Hive

```bash
# Bob joins Alice's hive
curl -X POST http://192.168.2.3:8080/api/v1/hives/$HIVE_ID/join \
  -H "Authorization: Bearer $BOB_TOKEN"
```

### Step 5: Test WebSocket Presence

Open the test-websocket.html file in two browser windows:

```bash
# Terminal 1 - Serve the HTML file
python3 -m http.server 8000

# Browser - Open two windows
# Window 1: http://192.168.2.3:8000/test-websocket.html
# Window 2: http://192.168.2.3:8000/test-websocket.html
```

In each window:
1. Paste the respective user token (Alice in window 1, Bob in window 2)
2. Click "Connect"
3. Enter the HIVE_ID
4. Click "Join Hive"
5. Try updating presence status and see real-time updates in both windows

### Step 6: Test Focus Sessions

In the WebSocket tester:
1. Set duration to 25 minutes
2. Click "Start Session"
3. See the session appear in both windows
4. Click "End Session" when done

### Step 7: REST API Testing

```bash
# Get current user presence
curl -X GET http://192.168.2.3:8080/api/v1/presence/me \
  -H "Authorization: Bearer $ALICE_TOKEN"

# Get hive presence info
curl -X GET http://192.168.2.3:8080/api/v1/presence/hives/$HIVE_ID \
  -H "Authorization: Bearer $ALICE_TOKEN"

# Get active sessions in hive
curl -X GET http://192.168.2.3:8080/api/v1/presence/hives/$HIVE_ID/sessions \
  -H "Authorization: Bearer $ALICE_TOKEN"
```

## 🔍 WebSocket Protocol Details

### Connection
```javascript
// Connect with JWT token
const socket = new SockJS('http://192.168.2.3:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { 'Authorization': 'Bearer ' + token },
  function(frame) {
    console.log('Connected');
  }
);
```

### Subscriptions
```javascript
// Subscribe to personal updates
stompClient.subscribe('/user/queue/presence', function(message) {
  console.log('Personal update:', JSON.parse(message.body));
});

// Subscribe to hive updates
stompClient.subscribe('/topic/hive/' + hiveId + '/presence', function(message) {
  console.log('Hive update:', JSON.parse(message.body));
});

// Subscribe to session updates
stompClient.subscribe('/topic/hive/' + hiveId + '/sessions', function(message) {
  console.log('Session update:', JSON.parse(message.body));
});
```

### Sending Messages
```javascript
// Update presence
stompClient.send('/app/presence/update', {}, JSON.stringify({
  status: 'ONLINE',
  hiveId: hiveId,
  activity: 'Working on project'
}));

// Join hive
stompClient.send('/app/hive/' + hiveId + '/join', {}, '{}');

// Start focus session
stompClient.send('/app/session/start', {}, JSON.stringify({
  hiveId: hiveId,
  durationMinutes: 25
}));

// Send heartbeat (every 20 seconds)
stompClient.send('/app/presence/heartbeat', {}, '{}');
```

## 📊 Monitoring

### View Logs
```bash
# Identity Service logs
tail -f logs/identity-service.log

# Backend logs
tail -f logs/backend.log

# Filter for presence logs
tail -f logs/backend.log | grep -i presence
```

### Check Redis Data
```bash
# Connect to Redis
redis-cli -a focushive_pass

# View all presence keys
KEYS presence:*

# Get specific user presence
GET presence:user:USER_ID

# View hive members
GET presence:hive:HIVE_ID

# Monitor real-time Redis commands
MONITOR
```

### Database Queries
```bash
# Connect to PostgreSQL
psql -d focushive

# View hives
SELECT * FROM hives;

# View hive members
SELECT * FROM hive_members;

# View users (in identity_db)
psql -d identity_db
SELECT id, username, email FROM users;
```

## 🎯 Testing Scenarios

### 1. Basic Presence
- User comes online
- User updates status (Away, Busy)
- User goes offline
- Heartbeat keeps user online

### 2. Hive Presence
- Multiple users join hive
- See who's currently in the hive
- User leaves hive
- Hive presence updates in real-time

### 3. Focus Sessions
- Start a 25-minute focus session
- See active sessions in hive
- End session early
- Track actual vs planned duration

### 4. Edge Cases
- User disconnects without proper logout
- Stale presence cleanup (30 seconds)
- Multiple hives at once
- Concurrent focus sessions

## 🛠️ Troubleshooting

### Connection Issues
```bash
# Check if services are running
curl http://192.168.2.3:8080/actuator/health
curl http://192.168.2.3:8081/actuator/health

# Check WebSocket endpoint
curl http://192.168.2.3:8080/ws/info
```

### Redis Issues
```bash
# Test Redis connection
redis-cli -a focushive_pass ping
# Should return: PONG

# Clear all Redis data (WARNING: removes all presence)
redis-cli -a focushive_pass FLUSHALL
```

### Database Issues
```bash
# Check database connections
psql -d focushive -c "SELECT 1"
psql -d identity_db -c "SELECT 1"

# Run migrations manually if needed
cd backend
./gradlew flywayMigrate
```

## 📖 API Documentation

Access Swagger UI when services are running:
- Backend API: http://192.168.2.3:8080/swagger-ui.html
- Identity API: http://192.168.2.3:8081/swagger-ui.html

## 🎨 Frontend Integration

To integrate with a React frontend:

```javascript
// Install dependencies
npm install @stomp/stompjs sockjs-client

// Create presence hook
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const usePresence = (token, hiveId) => {
  const [client, setClient] = useState(null);
  const [presence, setPresence] = useState([]);
  
  useEffect(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('http://192.168.2.3:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        // Subscribe to hive presence
        stompClient.subscribe(`/topic/hive/${hiveId}/presence`, (message) => {
          const data = JSON.parse(message.body);
          setPresence(data.onlineMembers);
        });
        
        // Join hive
        stompClient.publish({
          destination: `/app/hive/${hiveId}/join`,
          body: '{}'
        });
      }
    });
    
    stompClient.activate();
    setClient(stompClient);
    
    return () => stompClient.deactivate();
  }, [token, hiveId]);
  
  return { presence, client };
};
```

## 🚢 Production Deployment

For production:
1. Use proper JWT secrets
2. Configure SSL/TLS for WebSocket
3. Set up Redis cluster for scalability
4. Use connection pooling
5. Implement rate limiting
6. Add monitoring (Prometheus/Grafana)

---

Happy testing! 🎉 The real-time presence system is now ready for use.