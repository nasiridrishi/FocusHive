# FocusHive Frontend Scripts

This directory contains utility scripts for the FocusHive frontend application.

## Test User Registration Scripts

### Overview
Two scripts are provided to automatically register test users in the Identity Service:

- `register-test-users.sh` - Shell script (Bash)
- `register-test-users.js` - Node.js script

### Usage

#### Shell Script
```bash
# Make executable (if needed)
chmod +x scripts/register-test-users.sh

# Run the registration script
./scripts/register-test-users.sh
```

#### Node.js Script
```bash
# Install dependencies (if axios is not available)
npm install axios

# Run the script
node scripts/register-test-users.js
```

### Prerequisites
- Identity Service running on `http://localhost:8081`
- Network connectivity to the Identity Service
- For Node.js script: axios dependency (usually available in the project)

### What the Scripts Do
1. Check if the Identity Service is healthy and reachable
2. Register 6 predefined test users with the API
3. Verify each registration by testing login
4. Provide detailed status output with colored feedback
5. Handle errors gracefully (e.g., users already exist)

### Test Users Created
The scripts register these test users:
- alice.focused@focushive.test
- bob.productive@focushive.test  
- charlie.organized@focushive.test
- diana.efficient@focushive.test
- eve.motivated@focushive.test
- frank.dedicated@focushive.test

All users share the password: `SecurePass123!`

See `../TEST_USERS.md` for complete details about each test user.

### Success Status
✅ **Status**: All test users have been successfully registered and verified (as of 2025-09-22)

### Troubleshooting

#### Common Issues
1. **Service Not Running**: Ensure Identity Service is running on port 8081
2. **Network Errors**: Check connectivity to localhost:8081
3. **Rate Limiting**: The API has rate limits - scripts include appropriate delays
4. **Users Already Exist**: Scripts handle this gracefully and skip existing users

#### Manual Verification
You can manually verify registration by:
```bash
# Test login for any registered user
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "alice.focused@focushive.test",
    "password": "SecurePass123!"
  }'
```

## Other Scripts

### Icon Generation
- `generate-icons.sh` - Shell script for generating app icons
- `generate-icons-node.cjs` - Node.js version of icon generation
- `create-favicon.cjs` - Favicon creation script

---

**Last Updated**: 2025-09-22