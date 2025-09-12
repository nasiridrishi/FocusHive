#!/bin/bash

# SECURITY WARNING: This script requires environment variables to be set
# Do not hardcode secrets in this script

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Validate required environment variables
if [ -z "${JWT_SECRET}" ]; then
    echo "ERROR: JWT_SECRET environment variable must be set"
    echo "Generate with: openssl rand -base64 64"
    exit 1
fi

if [ -z "${DATABASE_PASSWORD}" ]; then
    echo "ERROR: DATABASE_PASSWORD environment variable must be set"
    exit 1
fi

if [ -z "${REDIS_PASSWORD}" ]; then
    echo "ERROR: REDIS_PASSWORD environment variable must be set"
    exit 1
fi

if [ -z "${DB_PASSWORD}" ]; then
    echo "ERROR: DB_PASSWORD environment variable must be set"
    exit 1
fi

if [ -z "${FOCUSHIVE_CLIENT_SECRET}" ]; then
    echo "ERROR: FOCUSHIVE_CLIENT_SECRET environment variable must be set"
    exit 1
fi

# Optional variables with defaults
export KEY_STORE_PASSWORD=${KEY_STORE_PASSWORD:-"changeme"}
export PRIVATE_KEY_PASSWORD=${PRIVATE_KEY_PASSWORD:-"changeme"}

# Print loaded variables (without sensitive values)
echo "Environment variables loaded:"
echo "JWT_SECRET: ***"
echo "DATABASE_PASSWORD: ***"
echo "REDIS_PASSWORD: ***"

# Change to backend directory and start the service
cd services/focushive-backend
./gradlew bootRun