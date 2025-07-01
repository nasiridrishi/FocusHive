# FocusHive Monorepo Setup Documentation

## Overview

FocusHive uses an Nx-based monorepo structure to manage multiple packages and applications in a single repository. This approach provides:

- Shared code and configurations
- Consistent tooling across all packages
- Efficient builds with caching
- Better code organization and maintainability

## Project Structure

```
focushive/
├── packages/           # Application packages
│   ├── backend/       # Node.js Express microservices
│   ├── frontend/      # React TypeScript web app
│   └── shared/        # Shared types and utilities
├── apps/              # Additional applications (future)
├── libs/              # Shared libraries (future)
├── nx.json            # Nx workspace configuration
├── workspace.json     # Workspace project configuration
├── tsconfig.base.json # Base TypeScript configuration
└── package.json       # Root package.json with workspaces
```

## Key Decisions

### Why Nx?

- **Better TypeScript Support**: Nx provides excellent TypeScript monorepo support out of the box
- **Build Optimization**: Intelligent caching and affected builds reduce CI/CD time
- **Microservices Ready**: Perfect for our microservices architecture
- **Scalability**: Can handle large codebases efficiently

### Package Organization

1. **@focushive/backend**: All backend microservices
2. **@focushive/frontend**: React web application
3. **@focushive/shared**: Shared types, constants, and utilities

## Development Commands

### Installation
```bash
npm install
```

### Development
```bash
# Run backend and frontend in dev mode
npm run dev

# Run specific package
npm run dev:backend
npm run dev:frontend
```

### Building
```bash
# Build all packages
npm run build

# Build specific package
npm run build:shared
npm run build:backend
npm run build:frontend
```

### Testing
```bash
# Run all tests
npm run test:all

# Run monorepo structure tests
npm test
```

### Linting
```bash
npm run lint
```

## TypeScript Configuration

The monorepo uses a shared TypeScript configuration:

- **tsconfig.base.json**: Base configuration for all packages
- Each package extends the base config with its own `tsconfig.json`
- Path mappings configured for `@focushive/shared` imports

## Shared Code

The `@focushive/shared` package contains:

- **types.ts**: Common TypeScript interfaces (User, Hive, Presence)
- **constants.ts**: Shared constants (socket events, timer durations)
- **utils.ts**: Utility functions used across packages

## Next Steps

1. Install dependencies for each package
2. Set up ESLint and Prettier configurations
3. Configure CI/CD with GitHub Actions
4. Set up Docker development environment
5. Implement microservices architecture in backend package