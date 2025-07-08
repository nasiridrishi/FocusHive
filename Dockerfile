# Multi-stage Dockerfile for FocusHive monorepo
FROM node:20-alpine AS base
WORKDIR /app

# Install build dependencies
RUN apk add --no-cache python3 make g++

# Copy root package files
COPY package*.json ./
COPY nx.json ./
COPY tsconfig.base.json ./

# Copy all packages
COPY packages ./packages

# Install all dependencies
RUN npm install

# Build all packages
RUN npm run build

# Backend service
FROM node:20-alpine AS backend
WORKDIR /app

# Copy backend source and built files
COPY --from=base /app/packages/backend ./packages/backend
COPY --from=base /app/packages/shared ./packages/shared
COPY --from=base /app/node_modules ./node_modules

WORKDIR /app/packages/backend
EXPOSE 3000
CMD ["npm", "run", "start"]

# Frontend service
FROM node:20-alpine AS frontend
WORKDIR /app

# Copy frontend source (Vite needs source for dev server)
COPY --from=base /app/packages/frontend ./packages/frontend
COPY --from=base /app/packages/shared ./packages/shared
COPY --from=base /app/node_modules ./node_modules

WORKDIR /app/packages/frontend
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]