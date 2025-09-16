#!/bin/bash

# Simple JWT generator for testing
# This uses the same secret configured in .env file

# Get the JWT secret from .env
JWT_SECRET=$(grep "^JWT_SECRET=" .env | cut -d'=' -f2)

# Default values
USER_ID=${1:-"test-user-1"}
ROLE=${2:-"USER"}
EXPIRY=${3:-$(($(date +%s) + 86400))}  # 24 hours from now

# Create header
header='{"alg":"HS256","typ":"JWT"}'
header_base64=$(echo -n "$header" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Create payload
payload=$(cat <<EOF2
{
  "sub": "$USER_ID",
  "userId": "$USER_ID",
  "roles": ["$ROLE"],
  "exp": $EXPIRY,
  "iat": $(date +%s)
}
EOF2
)
payload_base64=$(echo -n "$payload" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Create signature (simplified for testing)
# In production, use proper HMAC-SHA256
signature_input="${header_base64}.${payload_base64}"
# For testing purposes, we'll create a mock signature
signature_base64="test-signature"

# Output the JWT
echo "${header_base64}.${payload_base64}.${signature_base64}"
