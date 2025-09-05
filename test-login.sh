#!/bin/bash

# Test the demo login endpoint
echo "Testing demo login endpoint..."

# Test with correct credentials
echo -e "\n1. Testing with CORRECT credentials (demo@focushive.com / demo123):"
response=$(curl -s -X POST http://localhost:5173/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@focushive.com","password":"demo123"}' 2>/dev/null)

if [ -n "$response" ]; then
  echo "Response: $response"
  # Check if response contains token
  if echo "$response" | grep -q "accessToken"; then
    echo "✅ Login successful!"
  else
    echo "❌ Login failed - no token in response"
  fi
else
  echo "❌ No response from server"
fi

# Test with incorrect credentials
echo -e "\n2. Testing with INCORRECT credentials:"
response=$(curl -s -X POST http://localhost:5173/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"wrong@email.com","password":"wrongpass"}' 2>/dev/null)

if [ -n "$response" ]; then
  echo "Response: $response"
  if echo "$response" | grep -q "error"; then
    echo "✅ Correctly rejected invalid credentials"
  else
    echo "❌ Should have rejected invalid credentials"
  fi
else
  echo "❌ No response from server"
fi

# Test frontend availability
echo -e "\n3. Testing frontend availability:"
frontend_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:5173 2>/dev/null)
if [ "$frontend_status" = "200" ]; then
  echo "✅ Frontend is running at http://localhost:5173"
else
  echo "❌ Frontend not available (HTTP $frontend_status)"
fi

echo -e "\nTest complete!"