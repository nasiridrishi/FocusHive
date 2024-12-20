#!/bin/bash

# Test script for Cloudflare Tunnel connectivity for Identity Service
# This script verifies that the identity service is accessible via the public Cloudflare URL

echo "========================================="
echo "Testing Identity Service via Cloudflare Tunnel"
echo "========================================="
echo ""

PUBLIC_URL="https://identity.focushive.app"

echo "1. Testing health endpoint..."
echo "   URL: ${PUBLIC_URL}/actuator/health"
curl -s -w "\n   Status Code: %{http_code}\n   Response Time: %{time_total}s\n" \
     -o /dev/null \
     "${PUBLIC_URL}/actuator/health"
echo ""

echo "2. Testing JWKS endpoint..."
echo "   URL: ${PUBLIC_URL}/.well-known/jwks.json"
JWKS_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "${PUBLIC_URL}/.well-known/jwks.json")
HTTP_STATUS=$(echo "$JWKS_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
BODY=$(echo "$JWKS_RESPONSE" | sed '/HTTP_STATUS:/d')

if [ "$HTTP_STATUS" = "200" ]; then
    echo "   ✅ JWKS endpoint is accessible (Status: ${HTTP_STATUS})"
    echo "   Response preview:"
    echo "$BODY" | head -3
else
    echo "   ❌ Failed to access JWKS endpoint (Status: ${HTTP_STATUS})"
fi
echo ""

echo "3. Testing OpenID Configuration..."
echo "   URL: ${PUBLIC_URL}/.well-known/openid-configuration"
OIDC_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "${PUBLIC_URL}/.well-known/openid-configuration")
HTTP_STATUS=$(echo "$OIDC_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
BODY=$(echo "$OIDC_RESPONSE" | sed '/HTTP_STATUS:/d')

if [ "$HTTP_STATUS" = "200" ]; then
    echo "   ✅ OpenID Configuration accessible (Status: ${HTTP_STATUS})"
    echo "   Checking issuer configuration..."
    ISSUER=$(echo "$BODY" | grep -o '"issuer":"[^"]*"' | cut -d'"' -f4)
    echo "   Configured Issuer: ${ISSUER}"
    if [ "$ISSUER" = "${PUBLIC_URL}" ] || [ "$ISSUER" = "${PUBLIC_URL}/identity" ]; then
        echo "   ✅ Issuer correctly configured for public access"
    else
        echo "   ⚠️  Issuer may not be correctly configured for public access"
    fi
else
    echo "   ❌ Failed to access OpenID Configuration (Status: ${HTTP_STATUS})"
fi
echo ""

echo "4. Testing login endpoint..."
echo "   URL: ${PUBLIC_URL}/api/auth/login"
echo "   Method: POST (without credentials - expecting 401)"
LOGIN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
     -X POST \
     -H "Content-Type: application/json" \
     -d '{}' \
     "${PUBLIC_URL}/api/auth/login")
HTTP_STATUS=$(echo "$LOGIN_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)

if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "400" ]; then
    echo "   ✅ Login endpoint is accessible (Status: ${HTTP_STATUS} - expected for empty credentials)"
else
    echo "   ❌ Unexpected response from login endpoint (Status: ${HTTP_STATUS})"
fi
echo ""

echo "========================================="
echo "Testing JWT Token Validation from Other Services"
echo "========================================="
echo ""

echo "5. Simulating token validation from notification service..."
echo "   Fetching JWKS from: ${PUBLIC_URL}/.well-known/jwks.json"

# Test that the JWKS endpoint returns valid JSON
if command -v jq &> /dev/null; then
    JWKS=$(curl -s "${PUBLIC_URL}/.well-known/jwks.json")
    if echo "$JWKS" | jq -e '.keys' > /dev/null 2>&1; then
        echo "   ✅ Valid JWKS JSON structure found"
        KEY_COUNT=$(echo "$JWKS" | jq '.keys | length')
        echo "   Number of keys: ${KEY_COUNT}"
    else
        echo "   ❌ Invalid JWKS JSON structure"
    fi
else
    echo "   ⚠️  jq not installed, skipping JSON validation"
fi

echo ""
echo "========================================="
echo "Test Complete"
echo "========================================="
echo ""
echo "Next Steps:"
echo "1. Deploy the services with: docker-compose up -d"
echo "2. Wait for services to be healthy"
echo "3. Run this script again to verify connectivity"
echo "4. Check cloudflared logs with: docker logs focushive-identity-app-tunnel"