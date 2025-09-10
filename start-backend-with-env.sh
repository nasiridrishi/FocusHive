#!/bin/bash

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Export critical environment variables
export JWT_SECRET="t2+oCqx61sNBNLnJ3jm7YtdH58rrWqS7dQ4yLSD5q7c="
export DATABASE_PASSWORD="password"
export REDIS_PASSWORD="redis123"
export DB_PASSWORD="focushive123"
export FOCUSHIVE_CLIENT_SECRET="test-client-secret"
export KEY_STORE_PASSWORD="keystore123"
export PRIVATE_KEY_PASSWORD="privatekey123"

# Print loaded variables (without sensitive values)
echo "Environment variables loaded:"
echo "JWT_SECRET: ***"
echo "DATABASE_PASSWORD: ***"
echo "REDIS_PASSWORD: ***"

# Change to backend directory and start the service
cd services/focushive-backend
./gradlew bootRun