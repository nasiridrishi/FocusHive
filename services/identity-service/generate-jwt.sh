#!/bin/bash

# JWT header
header='{"alg":"HS256","typ":"JWT"}'

# JWT payload
payload=$(cat <<EOF
{
  "sub": "test-service",
  "iss": "http://localhost:8081",
  "aud": ["notification-service"],
  "exp": $(date -v +1H +%s),
  "iat": $(date +%s),
  "service": "identity-service",
  "type": "service-account",
  "roles": ["SERVICE"]
}
EOF
)

# Secret key (must match what notification service expects)
secret="test-jwt-secret-key-for-testing-only-must-be-at-least-256-bits-long"

# Function to base64url encode
base64url_encode() {
    base64 | tr '+/' '-_' | tr -d '='
}

# Encode header and payload
header_encoded=$(echo -n "$header" | base64url_encode)
payload_encoded=$(echo -n "$payload" | base64url_encode)

# Create signature
signature=$(echo -n "${header_encoded}.${payload_encoded}" | openssl dgst -binary -sha256 -hmac "$secret" | base64url_encode)

# Combine to create JWT
jwt="${header_encoded}.${payload_encoded}.${signature}"

echo "Generated JWT token:"
echo "$jwt"
echo ""
echo "Decoded payload:"
echo "$payload" | jq .