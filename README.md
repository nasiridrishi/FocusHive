# FocusHive

Digital co-working and co-studying platform that creates virtual "hives" - dedicated online spaces where users can work on individual tasks while being visibly present and accountable to others.

## Project Status

This project is currently in the development phase for the University of London BSc Computer Science final project (CM3070).

- **Project Timeline**: May 22, 2025 - September 15, 2025
- **Current Phase**: Phase 1 - Foundation Setup
- **Next Milestone**: Monorepo structure with Nx (UOL-19) ✅

## Monorepo Structure

FocusHive uses an Nx-based monorepo to manage all packages:

```
focushive/
├── packages/
│   ├── backend/         # Node.js Express microservices
│   ├── frontend/        # React TypeScript web application
│   └── shared/          # Shared types and utilities
├── apps/                # Additional applications
├── libs/                # Shared libraries
├── docs/                # Documentation
│   └── monorepo-setup.md
├── nx.json              # Nx workspace configuration
├── workspace.json       # Workspace projects
└── tsconfig.base.json   # Base TypeScript configuration
```

## Development

### Prerequisites

- Node.js 18+
- npm 9+
- Docker (for databases and services)
- Git

### Getting Started

```bash
# Install dependencies
npm install

# Run development servers (backend + frontend)
npm run dev

# Run tests
npm test

# Build all packages
npm run build
```

### Available Scripts

- `npm run dev` - Start backend and frontend in development mode
- `npm run dev:backend` - Start backend services only
- `npm run dev:frontend` - Start frontend application only
- `npm run build` - Build all packages in correct order
- `npm run build:shared` - Build shared package
- `npm run test` - Run monorepo tests
- `npm run test:all` - Run all package tests
- `npm run lint` - Run linting across all packages
- `npm run clean` - Clean all build artifacts and node_modules

## Architecture

- **Backend**: Microservices architecture with Express.js
- **Frontend**: React with TypeScript
- **Real-time**: WebSockets via Socket.io
- **Shared**: Common types and utilities

## Documentation

- [Monorepo Setup Guide](docs/monorepo-setup.md)
- Development Specification (see project docs)
- Project Design (see project docs)
- Todo List (see project docs)

## Testing

The project follows Test-Driven Development (TDD) practices:

1. Tests are written before implementation
2. All packages have their own test suites
3. Monorepo structure is tested with `monorepo.test.js` and `packages.test.js`

## License

This project is part of an academic submission for the University of London.