# Identity Service API Documentation

## Base URL
- **Production**: `https://identity.focushive.app`
- **API Version**: `/api/v1`
- **Full Base Path**: `https://identity.focushive.app/api/v1`

## Table of Contents
1. [Authentication Endpoints](#authentication-endpoints)
2. [OAuth2 Endpoints](#oauth2-endpoints)
3. [User Management Endpoints](#user-management-endpoints)
4. [Password Requirements](#password-requirements)
5. [Error Handling](#error-handling)
6. [Rate Limiting](#rate-limiting)

---

## Authentication Endpoints

### 1. User Registration
**Endpoint**: `POST /api/v1/auth/register`

**Description**: Register a new user account

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",           // Required: Valid email format
  "username": "johndoe",                 // Required: 3-50 characters
  "password": "SecurePass123@",          // Required: See password requirements below
  "confirmPassword": "SecurePass123@",   // Required: Must match password
  "firstName": "John",                   // Required: Max 50 characters
  "lastName": "Doe",                     // Required: Max 50 characters
  "personaType": "PERSONAL",            // Optional: Defaults to "PERSONAL"
  "personaName": "My Profile"            // Optional: Max 100 characters
}
```

**Success Response** (201 Created):
```json
{
  "accessToken": "eyJraWQiOi...",
  "refreshToken": "eyJraWQiOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "6e8c8c55-e1a9-4795-b2d6-fde367b11bf5",
  "username": "johndoe",
  "email": "user@example.com",
  "displayName": "johndoe",
  "activePersona": {
    "id": "f9eb0e58-f1c6-453a-80f8-9e9468501813",
    "name": "Default",
    "type": "PERSONAL",
    "avatarUrl": null,
    "privacySettings": {
      "showRealName": false,
      "showEmail": false,
      "showActivity": true,
      "allowDirectMessages": true,
      "visibilityLevel": "FRIENDS"
    },
    "default": true
  },
  "availablePersonas": [...]
}
```

**Error Responses**:
- `400 Bad Request` - Invalid input data
- `409 Conflict` - Email or username already exists
- `429 Too Many Requests` - Rate limit exceeded (2 per minute per IP)

---

### 2. User Login
**Endpoint**: `POST /api/v1/auth/login`

**Description**: Authenticate user and receive JWT tokens

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "usernameOrEmail": "user@example.com",  // Required: Username or email
  "password": "SecurePass123@"            // Required: User's password
}
```

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJraWQiOi...",
  "refreshToken": "eyJraWQiOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "6e8c8c55-e1a9-4795-b2d6-fde367b11bf5",
  "username": "johndoe",
  "email": "user@example.com",
  "displayName": "johndoe",
  "activePersona": {...},
  "availablePersonas": [...]
}
```

**Error Responses**:
- `400 Bad Request` - Missing or invalid request body
- `401 Unauthorized` - Invalid credentials
- `429 Too Many Requests` - Rate limit exceeded (5 per minute per IP)

---

### 3. Refresh Token
**Endpoint**: `POST /api/v1/auth/refresh`

**Description**: Refresh access token using refresh token

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "refreshToken": "eyJraWQiOi..."  // Required: Valid refresh token
}
```

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJraWQiOi...",
  "refreshToken": "eyJraWQiOi...",  // New refresh token
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Responses**:
- `400 Bad Request` - Invalid request format
- `401 Unauthorized` - Invalid or expired refresh token

---

### 4. Logout
**Endpoint**: `POST /api/v1/auth/logout`

**Description**: Logout user and invalidate tokens

**Request Headers**:
```
Content-Type: application/json
Authorization: Bearer <access_token>
```

**Request Body**:
```json
{
  "accessToken": "eyJraWQiOi...",   // Required: Current access token
  "refreshToken": "eyJraWQiOi..."   // Required: Current refresh token
}
```

**Success Response** (200 OK):
```json
{
  "message": "Logged out successfully"
}
```

**Error Responses**:
- `400 Bad Request` - Invalid request body
- `401 Unauthorized` - Missing or invalid authorization

---

### 5. Request Password Reset
**Endpoint**: `POST /api/v1/auth/password/reset-request`

**Description**: Request password reset link via email

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com"  // Required: Registered email address
}
```

**Success Response** (200 OK):
```json
{
  "message": "If an account exists with this email, a reset link has been sent."
}
```

**Note**: Always returns 200 OK to prevent user enumeration attacks

---

### 6. Reset Password
**Endpoint**: `POST /api/v1/auth/password/reset`

**Description**: Reset password using token from email

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "token": "reset-token-from-email",      // Required: Reset token
  "newPassword": "NewSecurePass123@",     // Required: See password requirements
  "confirmPassword": "NewSecurePass123@"  // Required: Must match newPassword
}
```

**Success Response** (200 OK):
```json
{
  "message": "Password has been successfully reset."
}
```

**Error Responses**:
- `400 Bad Request` - Invalid token or password requirements not met
- `429 Too Many Requests` - Rate limit exceeded (3 per 5 minutes per IP)

---

## OAuth2 Endpoints

### 1. Authorization Endpoint
**Endpoint**: `GET /oauth2/authorize`

**Query Parameters**:
- `response_type=code` - Required
- `client_id=<client_id>` - Required
- `redirect_uri=<uri>` - Required
- `scope=openid profile email` - Optional
- `state=<state>` - Recommended for CSRF protection
- `code_challenge=<challenge>` - Required for PKCE
- `code_challenge_method=S256` - Required for PKCE

**Response**: Redirects to login page or consent screen

---

### 2. Token Endpoint
**Endpoint**: `POST /oauth2/token`

**Request Headers**:
```
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <base64(client_id:client_secret)>
```

**Request Body** (Authorization Code):
```
grant_type=authorization_code
code=<authorization_code>
redirect_uri=<same_as_authorize>
code_verifier=<pkce_verifier>
```

**Success Response** (200 OK):
```json
{
  "access_token": "eyJraWQiOi...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "eyJraWQiOi...",
  "id_token": "eyJraWQiOi...",
  "scope": "openid profile email"
}
```

---

### 3. JWKS Endpoint
**Endpoint**: `GET /.well-known/jwks.json`

**Description**: JSON Web Key Set for JWT validation

**Response** (200 OK):
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "focushive-2025-01",
      "alg": "RS256",
      "n": "ALmEyOJC96bDRCxe...",
      "e": "AQAB"
    }
  ]
}
```

---

### 4. OpenID Configuration
**Endpoint**: `GET /.well-known/openid-configuration`

**Description**: OpenID Connect discovery document

**Response** (200 OK):
```json
{
  "issuer": "https://identity.focushive.app",
  "authorization_endpoint": "https://identity.focushive.app/oauth2/authorize",
  "token_endpoint": "https://identity.focushive.app/oauth2/token",
  "jwks_uri": "https://identity.focushive.app/oauth2/jwks",
  "userinfo_endpoint": "https://identity.focushive.app/userinfo",
  "end_session_endpoint": "https://identity.focushive.app/connect/logout",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "refresh_token", "client_credentials"],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid", "profile", "email"],
  "code_challenge_methods_supported": ["S256"]
}
```

---

## Password Requirements

All passwords must meet the following criteria:
- **Minimum length**: 8 characters
- **Maximum length**: 128 characters
- **Required character types**:
  - At least 1 uppercase letter (A-Z)
  - At least 1 lowercase letter (a-z)
  - At least 1 digit (0-9)
  - At least 1 special character (@, #, $, %, ^, &, +, =, !, etc.)
- **Additional checks**:
  - Cannot be a common password (e.g., "Password123!")
  - Cannot contain SQL injection patterns
  - Cannot contain NoSQL injection patterns

**Example valid passwords**:
- `SecurePass123@`
- `MyP@ssw0rd2024!`
- `Test123@Pass`

---

## Field Validation Rules

### Email
- Must be a valid email format
- Maximum 255 characters
- Cannot contain injection patterns

### Username
- Minimum 3 characters
- Maximum 50 characters
- Alphanumeric and underscore only
- Must be unique

### Names (First/Last)
- Maximum 50 characters
- Cannot contain special characters that might indicate injection attempts

---

## Error Response Format

All error responses follow this format:

```json
{
  "timestamp": "2025-09-21T16:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "validationErrors": {
    "password": "Password must contain at least one uppercase letter",
    "email": "Email must be valid"
  }
}
```

---

## Rate Limiting

The following rate limits are enforced:

| Endpoint | Rate Limit | Window | Type | Progressive Penalties |
|----------|-----------|--------|------|----------------------|
| Registration | 2 requests | 1 minute | Per IP | Yes |
| Login | 5 requests | 1 minute | Per IP | Yes |
| Password Reset Request | 1 request | 1 minute | Per IP | Yes |
| Password Reset | 3 requests | 5 minutes | Per IP | No |

When rate limit is exceeded, response will be:
```json
{
  "error": "Too many requests. Please wait before trying again.",
  "retryAfter": 60
}
```

---

## JWT Token Structure

### Access Token Claims
```json
{
  "kid": "focushive-2025-01",
  "alg": "RS256",
  "emailVerified": false,
  "displayName": "johndoe",
  "personaType": "PERSONAL",
  "personaName": "Default",
  "type": "access",
  "userId": "6e8c8c55-e1a9-4795-b2d6-fde367b11bf5",
  "personaId": "f9eb0e58-f1c6-453a-80f8-9e9468501813",
  "email": "user@example.com",
  "sub": "johndoe",
  "jti": "35478e77-dbed-43af-b618-b33cb6de808f",
  "iat": 1758471467,
  "exp": 1758475067,
  "iss": "https://identity.focushive.app/identity"
}
```

### Token Expiration
- **Access Token**: 1 hour (3600 seconds)
- **Refresh Token**: 30 days (2592000 seconds)

---

## Authentication Headers

For protected endpoints, include the JWT token in the Authorization header:

```
Authorization: Bearer eyJraWQiOiJmb2N1c2hpdmUtMjAyNS0wMSIsImFsZy...
```

---

## CORS Configuration

The following origins are allowed:
- `https://focushive.app`
- `https://identity.focushive.app`
- `https://notification.focushive.app`
- `https://backend.focushive.app`
- `https://buddy.focushive.app`
- `http://localhost:3000` (development)
- `http://localhost:5173` (development)

---

## Health Check

**Endpoint**: `GET /actuator/health`

**Description**: Service health status

**Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

## Testing

Use the provided test script:
```bash
./test-all-endpoints.sh
```

Or test individual endpoints:
```bash
# Register
curl -X POST https://identity.focushive.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "Test123@Pass",
    "confirmPassword": "Test123@Pass",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST https://identity.focushive.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "Test123@Pass"
  }'
```