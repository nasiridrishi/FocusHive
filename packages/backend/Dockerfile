# Development Dockerfile for backend service
FROM node:20-alpine

WORKDIR /app

# Install dependencies for better development experience
RUN apk add --no-cache python3 make g++

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --frozen-lockfile

# Copy application code
COPY . .

# Build TypeScript
RUN npm run build

# Expose port
EXPOSE 3000

# Development command (uses nodemon for hot reload)
CMD ["npm", "run", "dev"]