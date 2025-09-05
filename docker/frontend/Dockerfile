# Multi-stage build for React Vite application

# Stage 1: Build Stage
FROM node:20-alpine AS builder

# Accept build arguments for environment variables
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_WEBSOCKET_URL=ws://localhost:8080
ARG VITE_IDENTITY_SERVICE_URL=http://localhost:8081
ARG VITE_ENABLE_ANALYTICS=false
ARG VITE_SENTRY_DSN=

# Set working directory
WORKDIR /app

# Copy package files
COPY frontend/package*.json ./

# Install all dependencies (including dev dependencies for build)
RUN npm ci

# Copy application source
COPY frontend/ ./

# Set environment variables for build
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WEBSOCKET_URL=$VITE_WEBSOCKET_URL
ENV VITE_IDENTITY_SERVICE_URL=$VITE_IDENTITY_SERVICE_URL
ENV VITE_ENABLE_ANALYTICS=$VITE_ENABLE_ANALYTICS
ENV VITE_SENTRY_DSN=$VITE_SENTRY_DSN

# Build the application (skip TypeScript checks for Docker build)
# Use vite build directly to skip tsc type checking
RUN npx vite build

# Stage 2: Production Stage with Nginx
FROM nginx:1.25-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Remove default nginx configuration
RUN rm -rf /etc/nginx/conf.d/default.conf

# Copy custom nginx configuration (frontend-only for standalone deployment)
COPY docker/frontend/nginx.frontend-only.conf /etc/nginx/conf.d/default.conf

# Copy built application from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Set proper permissions for nginx user (already exists in alpine image)
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# Expose port internally
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# Switch to non-root user
USER nginx

# Start nginx
CMD ["nginx", "-g", "daemon off;"]