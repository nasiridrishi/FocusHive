# FocusHive Prototype

A real-time virtual co-working platform that demonstrates core presence and collaboration features.

## 🚀 Quick Start

See [Quick Start Guide](docs/development/QUICK_START.md) for rapid setup instructions.

## 📚 Documentation

### Architecture
- [Technical Architecture](docs/architecture/TECHNICAL_ARCHITECTURE.md) - System design and technical specifications

### Development
- [Development Guide](docs/development/DEVELOPMENT_GUIDE.md) - Complete implementation guide with daily breakdown
- [Quick Start](docs/development/QUICK_START.md) - Get up and running quickly

### Daily Implementation Logs
- [Day 1: Authentication](docs/development/daily-logs/DAY1_COMPLETE.md)
- [Day 2: Room Management](docs/development/daily-logs/DAY2_COMPLETE.md)
- [Day 3: Real-time Presence](docs/development/daily-logs/DAY3_COMPLETE.md)
- [Day 4: Pomodoro Timer](docs/development/daily-logs/DAY4_COMPLETE.md)
- [Day 5: Gamification](docs/development/daily-logs/DAY5_COMPLETE.md)
- [Day 6: Break-time Chat](docs/development/daily-logs/DAY6_COMPLETE.md)
- [Day 7: Buddy System](docs/development/daily-logs/DAY7_COMPLETE.md)

### Guides
- [Running the App](docs/guides/RUNNING_THE_APP.md) - How to run the application
- [Timer Test Guide](docs/guides/TIMER_TEST_GUIDE.md) - Testing the Pomodoro timer feature

## 🎯 Features Implemented

- ✅ User authentication (JWT-based)
- ✅ Real-time presence system
- ✅ Virtual focus rooms
- ✅ Synchronized Pomodoro timer
- ✅ Gamification (points, achievements, leaderboard)
- ✅ Break-time chat system
- ✅ Buddy matching system
- ✅ Forum for finding study partners

## 🛠️ Tech Stack

- **Frontend**: React + TypeScript + Tailwind CSS
- **Backend**: Node.js + Express + Socket.io
- **Real-time**: WebSockets via Socket.io
- **State**: In-memory stores (simulating Redis)
- **Testing**: Jest + Socket.io client

## 📊 Current Status

- 190+ tests passing
- All core features implemented
- Ready for evaluation and demonstration

## 🏃‍♂️ Running the Application

```bash
# From the focushive-prototype directory
./test-app.sh
```

Or manually:
```bash
# Terminal 1: Start backend
cd server
npm run dev

# Terminal 2: Start frontend
cd client
npm run dev
```

Access the application at http://localhost:5173