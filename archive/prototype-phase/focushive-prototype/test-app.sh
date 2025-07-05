#!/bin/bash

echo "Starting FocusHive application test..."

# Kill any existing processes on ports 3000 and 5173
lsof -ti:3000 | xargs kill -9 2>/dev/null
lsof -ti:5173 | xargs kill -9 2>/dev/null

# Start the server in background
cd server
npm start > server.log 2>&1 &
SERVER_PID=$!
echo "Server started with PID: $SERVER_PID"

# Wait for server to start
sleep 3

# Test the server
echo "Testing server health endpoint..."
curl -s http://localhost:3000/health | jq || echo "Server test failed"

# Start the client in background
cd ../client
npm run dev > client.log 2>&1 &
CLIENT_PID=$!
echo "Client started with PID: $CLIENT_PID"

# Wait for client to start
sleep 3

echo ""
echo "Application is running!"
echo "Server: http://localhost:3000"
echo "Client: http://localhost:5173"
echo ""
echo "To stop the application, run:"
echo "kill $SERVER_PID $CLIENT_PID"

# Keep script running
wait