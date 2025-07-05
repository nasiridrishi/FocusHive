# Running the FocusHive Application

## Prerequisites
- Node.js 18+ installed
- All dependencies installed (`npm install` in root directory)

## Running the Application

### Option 1: Using the dev script (Recommended)

In the root directory, run:
```bash
npm run dev
```

This will start both the server and client concurrently:
- Server: http://localhost:3000
- Client: http://localhost:5173

### Option 2: Running services separately

In separate terminal windows:

**Terminal 1 - Server:**
```bash
cd server
npm start
```

**Terminal 2 - Client:**
```bash
cd client
npm run dev
```

## Testing the Application

### 1. Server Health Check
Open a new terminal and run:
```bash
curl http://localhost:3000/health
```

Expected response:
```json
{"status":"ok","timestamp":"2025-01-15T..."}
```

### 2. Register a New User
1. Open http://localhost:5173 in your browser
2. Click "Sign up" or go to http://localhost:5173/register
3. Fill in:
   - Email: test@example.com
   - Username: testuser
   - Password: password123
   - Confirm Password: password123
4. Click "Sign Up"
5. You should be redirected to the dashboard

### 3. Test Login
1. Click "Logout" in the dashboard
2. Go to http://localhost:5173/login
3. Enter your credentials:
   - Email: test@example.com
   - Password: password123
4. Click "Log In"
5. You should see the dashboard with your user stats

### 4. Test Protected Routes
1. While logged out, try to access http://localhost:5173/dashboard
2. You should be redirected to the login page
3. After logging in, the dashboard should be accessible
4. Refresh the page - you should remain logged in

## Troubleshooting

### Server won't start
1. Check if port 3000 is already in use:
   ```bash
   lsof -i :3000
   ```
2. Kill any process using the port:
   ```bash
   kill -9 <PID>
   ```

### Client won't start
1. Check if port 5173 is already in use
2. Make sure you're in the client directory
3. Try removing node_modules and reinstalling:
   ```bash
   rm -rf node_modules
   npm install
   ```

### Authentication not working
1. Check the server logs for errors
2. Verify the JWT_SECRET is set in server/.env
3. Check browser console for errors
4. Clear localStorage and try again:
   ```javascript
   localStorage.clear()
   ```

### Connection refused errors
1. Make sure both server and client are running
2. Check that the server is on port 3000 (not 3001)
3. Verify the client API_URL points to http://localhost:3000

## Development Notes

- The server uses an in-memory database, so all data is lost when restarting
- Hot reload is enabled for both client and server in development
- TypeScript compilation happens automatically
- Tailwind CSS classes are compiled on demand

## Next Steps

Once the basic authentication is working, you can proceed with Day 2 implementation:
- Room creation and management
- Real-time presence with Socket.io
- Join/leave room functionality