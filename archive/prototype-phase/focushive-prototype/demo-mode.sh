#!/bin/bash

# Demo Mode Script for FocusHive
# This script enables demo mode with realistic dummy data for screenshots

echo "🎭 FocusHive Demo Mode Setup"
echo "============================="

# Function to start in demo mode
start_demo() {
    echo "📸 Starting FocusHive in Demo Mode..."
    echo ""
    echo "This will populate the application with:"
    echo "- 1 demo user (demo@focushive.com / demo123)"
    echo "- 16 dummy users (8 students, 8 remote workers)"
    echo "- 12 active focus/study rooms with participants"
    echo "- Forum posts from various users"
    echo "- Realistic gamification data"
    echo "- Simulated real-time activity"
    echo ""
    
    # Start the server with DEMO_MODE enabled
    cd server
    DEMO_MODE=true npm run dev &
    SERVER_PID=$!
    
    # Wait for server to start
    sleep 3
    
    # Start the client
    cd ../client
    npm run dev &
    CLIENT_PID=$!
    
    echo ""
    echo "✅ Demo mode is running!"
    echo "📷 Perfect for taking screenshots"
    echo ""
    echo "🔐 Demo User Credentials:"
    echo "   Email: demo@focushive.com"
    echo "   Password: demo123"
    echo ""
    echo "Server PID: $SERVER_PID"
    echo "Client PID: $CLIENT_PID"
    echo ""
    echo "Access the app at: http://localhost:5173"
    echo ""
    echo "Press Ctrl+C to stop demo mode"
    
    # Wait for user to stop
    wait
}

# Function to start in normal mode
start_normal() {
    echo "🚀 Starting FocusHive in Normal Mode..."
    echo ""
    
    cd server
    npm run dev &
    SERVER_PID=$!
    
    sleep 3
    
    cd ../client
    npm run dev &
    CLIENT_PID=$!
    
    echo ""
    echo "✅ Normal mode is running!"
    echo ""
    echo "Access the app at: http://localhost:5173"
    echo ""
    echo "Press Ctrl+C to stop"
    
    wait
}

# Check if demo flag is provided
if [ "$1" == "--demo" ] || [ "$1" == "-d" ]; then
    start_demo
else
    echo "Usage:"
    echo "  ./demo-mode.sh --demo    # Start with dummy data for screenshots"
    echo "  ./demo-mode.sh           # Start in normal mode"
    echo ""
    read -p "Start in demo mode? (y/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        start_demo
    else
        start_normal
    fi
fi

# Cleanup on exit
trap "kill $SERVER_PID $CLIENT_PID 2>/dev/null; exit" INT TERM EXIT