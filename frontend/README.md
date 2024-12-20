# FocusHive Frontend

React TypeScript application for virtual co-working platform.

## Tech Stack
- React 18.3 with TypeScript
- Material UI for components
- Vite for build tooling
- Socket.io for WebSocket
- React Router for navigation
- Zustand for state management

## Quick Start

```bash
# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Run tests
npm test
```

## Project Structure

```
src/
├── components/     # Shared UI components
├── features/       # Feature modules
│   ├── auth/      # Authentication
│   ├── hive/      # Hive management
│   ├── timer/     # Focus timer
│   └── chat/      # Real-time chat
├── services/      # API and WebSocket
├── hooks/         # Custom React hooks
└── utils/         # Helper functions
```

## Environment Variables

```bash
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
VITE_IDENTITY_URL=http://localhost:8081
```

## Test Credentials

For testing the login functionality, use these pre-configured test accounts:

### Primary Test User
- **Email:** `e2e.auth@focushive.test`
- **Password:** `TestPassword123!`

### Alternative Test User  
- **Email:** `e2e.test@focushive.test`
- **Password:** `TestPassword123!`

### For Testing Error Handling
- **Email:** `nonexistent@focushive.test`
- **Password:** `WrongPassword123!`

> **Note:** These credentials are used by the E2E test suite and should be available if the backend test database is properly seeded.

## Testing
- Unit tests: Jest + React Testing Library
- E2E tests: Playwright
- Coverage: 80%+