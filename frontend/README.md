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

## Testing
- Unit tests: Jest + React Testing Library
- E2E tests: Playwright
- Coverage: 80%+