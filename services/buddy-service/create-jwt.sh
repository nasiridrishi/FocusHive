#!/bin/bash

# Generate JWT token for testing

USER_ID="${1:-test-user-1}"
ROLE="${2:-USER}"
SECRET="${JWT_SECRET:-production-secret-key-change-in-real-deployment-ultra-secure-256-bit}"

# Create header
header='{"alg":"HS256","typ":"JWT"}'
header_base64=$(echo -n "$header" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Create payload
now=$(date +%s)
exp=$((now + 86400))  # 24 hours
payload='{"sub":"'$USER_ID'","userId":"'$USER_ID'","roles":["'$ROLE'"],"iat":'$now',"exp":'$exp'}'
payload_base64=$(echo -n "$payload" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Create signature
signature=$(echo -n "$header_base64.$payload_base64" | openssl dgst -binary -sha256 -hmac "$SECRET" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Combine
token="$header_base64.$payload_base64.$signature"

echo "JWT Token for $USER_ID:"
echo "$token"