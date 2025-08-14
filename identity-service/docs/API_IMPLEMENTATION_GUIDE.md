# FocusHive Identity Service API Implementation Guide

## Overview

This document provides a comprehensive guide to the FocusHive Identity Service API implementation, which fulfills the CM3035 Advanced Web Design template requirements for a sophisticated identity and authentication system.

## Architecture Summary

The Identity Service implements a complete OAuth2 authorization server with advanced features:

- **JWT-based Authentication**: Secure token-based authentication with refresh token rotation
- **Multiple Personas**: Users can have different profiles for work/study/personal contexts
- **OAuth2 Authorization Server**: Full OAuth2 provider capabilities (not just consumer)
- **GDPR Compliance**: Complete privacy controls and data portability
- **Federation Support**: Cross-service identity management and SSO
- **Advanced Preferences**: Context-aware user preferences management

## API Endpoints Summary

### 1. Authentication & Core Identity (`/api/v1/auth`)

#### Core Authentication
- `POST /auth/register` - User registration with default persona
- `POST /auth/login` - User authentication with JWT tokens
- `POST /auth/refresh` - Access token refresh
- `POST /auth/logout` - Session invalidation
- `POST /auth/validate` - Token validation for services
- `POST /auth/introspect` - Detailed token information

#### Password Management
- `POST /auth/password/reset-request` - Request password reset
- `POST /auth/password/reset` - Complete password reset

#### Context Switching
- `POST /auth/personas/switch` - Switch active persona context

### 2. Persona Management (`/api/v1/personas`)

#### CRUD Operations
- `GET /personas` - List user's personas
- `POST /personas` - Create new persona
- `GET /personas/{id}` - Get specific persona
- `PUT /personas/{id}` - Update persona
- `DELETE /personas/{id}` - Delete persona (except default)

#### Context Management
- `GET /personas/active` - Get currently active persona
- `POST /personas/{id}/switch` - Switch to persona
- `POST /personas/{id}/default` - Set as default persona

#### Templates
- `GET /personas/templates` - Available persona templates
- `POST /personas/templates/{type}` - Create from template

### 3. OAuth2 Authorization Server (`/api/v1/oauth2`)

#### Core OAuth2 Flow
- `GET /oauth2/authorize` - Authorization code flow initiation
- `POST /oauth2/token` - Token exchange endpoint
- `GET /oauth2/userinfo` - User information endpoint
- `POST /oauth2/introspect` - Token introspection (RFC 7662)
- `POST /oauth2/revoke` - Token revocation (RFC 7009)

#### Server Metadata
- `GET /oauth2/.well-known/oauth-authorization-server` - Server metadata (RFC 8414)
- `GET /oauth2/jwks` - JSON Web Key Set

#### Client Management
- `POST /oauth2/register` - Dynamic client registration (RFC 7591)
- `GET /oauth2/clients` - User's registered clients
- `PUT /oauth2/clients/{id}` - Update client configuration
- `DELETE /oauth2/clients/{id}` - Delete client registration

### 4. Privacy & Data Management (`/api/v1/privacy`)

#### Privacy Preferences
- `GET /privacy/preferences` - Current privacy settings
- `PUT /privacy/preferences` - Update privacy preferences

#### Data Rights (GDPR Compliance)
- `POST /privacy/data/export` - Request data export (Article 20)
- `GET /privacy/data/export/{id}/download` - Download exported data
- `GET /privacy/data/exports` - Export history
- `POST /privacy/data/delete` - Request account deletion (Article 17)
- `POST /privacy/data/delete/cancel` - Cancel deletion request
- `POST /privacy/data/rectify` - Request data correction (Article 16)

#### Access & Consent Management
- `GET /privacy/data/access-log` - Data access audit log (Article 15)
- `GET /privacy/data/processing` - Processing activities info (Articles 13-14)
- `POST /privacy/data/object` - Object to processing (Article 21)
- `POST /privacy/consent/grant` - Grant consent
- `POST /privacy/consent/revoke` - Revoke consent
- `GET /privacy/consent/history` - Consent history

### 5. Identity Federation & SSO (`/api/v1/federation`)

#### External Identity Providers
- `GET /federation/providers` - Available identity providers
- `GET /federation/auth/{provider}` - Initiate external auth
- `GET /federation/callback/{provider}` - Handle auth callback

#### Account Linking
- `POST /federation/accounts/link` - Link external account
- `DELETE /federation/accounts/{provider}` - Unlink account
- `GET /federation/accounts` - List linked accounts

#### SAML SSO
- `GET /federation/saml/metadata` - SAML IdP metadata
- `POST /federation/saml/sso` - SAML SSO endpoint
- `POST /federation/saml/slo` - SAML Single Logout

#### Cross-Service Federation
- `POST /federation/services` - Register FocusHive service
- `GET /federation/services` - List registered services
- `POST /federation/services/{id}/token` - Issue service token
- `POST /federation/services/validate-token` - Validate service token
- `POST /federation/identity/propagate` - Propagate identity changes

### 6. User Preferences (`/api/v1/preferences`)

#### Global & Persona Preferences
- `GET /preferences/global` - Global user preferences
- `PUT /preferences/global` - Update global preferences
- `GET /preferences/personas/{id}` - Persona-specific preferences
- `PUT /preferences/personas/{id}` - Update persona preferences
- `GET /preferences/active` - Active persona preferences

#### Specialized Preferences
- `GET /preferences/notifications` - Notification settings
- `PUT /preferences/notifications` - Update notification settings
- `GET /preferences/accessibility` - Accessibility settings
- `PUT /preferences/accessibility` - Update accessibility settings
- `GET /preferences/theme` - Theme and appearance
- `PUT /preferences/theme` - Update theme settings
- `GET /preferences/integrations` - External service integrations
- `PUT /preferences/integrations` - Update integrations

#### Management & Portability
- `GET /preferences/schema` - Preference schema definition
- `POST /preferences/reset` - Reset to defaults
- `POST /preferences/copy` - Copy between personas
- `POST /preferences/export` - Export preferences
- `POST /preferences/import` - Import preferences

## Key Implementation Features

### 1. Multiple Personas System

Each user can have multiple personas representing different contexts:

```json
{
  "personas": [
    {
      "id": "persona-work-123",
      "name": "Work Profile",
      "type": "WORK",
      "isActive": true,
      "preferences": {
        "theme": "professional",
        "notifications": "minimal",
        "privacy_level": "high"
      }
    },
    {
      "id": "persona-study-456", 
      "name": "Study Profile",
      "type": "STUDY",
      "isActive": false,
      "preferences": {
        "theme": "focus",
        "notifications": "academic",
        "privacy_level": "medium"
      }
    }
  ]
}
```

### 2. OAuth2 Authorization Server

Full OAuth2 2.1 compliance with:
- Authorization Code flow with PKCE
- Client Credentials flow
- Refresh Token rotation
- Token introspection and revocation
- Dynamic client registration
- OpenID Connect support

Example token response:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def456...",
  "scope": "openid profile email",
  "id_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. GDPR Compliance

Complete GDPR rights implementation:
- **Right to Access**: Full data access logs and processing information
- **Right to Rectification**: Data correction requests with approval workflow
- **Right to Erasure**: Account deletion with grace period
- **Right to Portability**: JSON/XML/CSV data exports
- **Right to Object**: Granular objection to processing activities
- **Consent Management**: Audit trail of all consent grants and revocations

### 4. Advanced Privacy Controls

Granular privacy settings:
```json
{
  "privacyPreferences": {
    "consentStatus": {
      "analytics": true,
      "marketing": false,
      "third_party_sharing": false,
      "research": true
    },
    "visibility": {
      "profile": "private",
      "activity": "friends",
      "presence": "public"
    },
    "dataRetentionDays": 730,
    "autoDataDeletion": false
  }
}
```

### 5. Cross-Service Federation

Identity federation capabilities:
- Service-to-service authentication tokens
- Identity propagation across FocusHive services
- SAML 2.0 IdP for external services
- External IdP integration (Google, Microsoft, etc.)

## Security Features

### Authentication Security
- JWT with RS256 signing
- Refresh token rotation
- Rate limiting on auth endpoints
- Account lockout protection
- Password strength enforcement
- Two-factor authentication support

### OAuth2 Security
- PKCE for public clients
- State parameter validation
- Scope-based access control
- Client authentication methods
- Token binding and validation

### Privacy Security
- Audit logging of all data access
- Consent timestamp validation
- Data export encryption
- Secure data deletion
- Access control on sensitive endpoints

## Error Handling

Consistent error responses across all endpoints:

```json
{
  "error": "invalid_request",
  "error_description": "The request is missing a required parameter",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/auth/login",
  "status": 400
}
```

Standard HTTP status codes:
- 200: Success
- 201: Created
- 204: No Content
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 409: Conflict
- 500: Internal Server Error

## Integration Guidelines

### For FocusHive Services

1. **Service Registration**: Register your service for federation
2. **Token Validation**: Use `/oauth2/introspect` for token validation
3. **User Context**: Include active persona information in requests
4. **Privacy Compliance**: Respect user privacy preferences

### For External Applications

1. **Client Registration**: Register OAuth2 client via API
2. **Authorization Flow**: Use standard OAuth2 flows
3. **Scopes**: Request minimal necessary scopes
4. **Token Refresh**: Implement token refresh logic

## Development Notes

### Service Interfaces
All controllers have corresponding service interfaces:
- `AuthenticationService`
- `PersonaService` 
- `OAuth2AuthorizationService`
- `PrivacyService`
- `FederationService`
- `UserPreferencesService`

### Database Considerations
- User table with persona relationships
- OAuth client registrations
- Privacy consent audit logs
- Data export job tracking
- Session and token storage (Redis)

### Performance Considerations
- JWT token validation caching
- Persona preference caching
- Rate limiting implementation
- Database connection pooling
- Async data export processing

## Compliance & Standards

This implementation follows:
- **OAuth 2.1** (RFC 6749, 6750, 7662, 7009, 7591)
- **OpenID Connect 1.0**
- **GDPR** (EU 2016/679)
- **SAML 2.0**
- **JSON Web Token (JWT)** (RFC 7519)
- **JSON Web Key (JWK)** (RFC 7517)

## Next Steps for Implementation

1. **Service Implementation**: Implement all service interfaces
2. **Database Schema**: Create complete database migrations
3. **Security Configuration**: Configure Spring Security filters
4. **Testing**: Create comprehensive test suite
5. **Documentation**: Generate OpenAPI documentation
6. **Deployment**: Configure for production deployment

This API design provides a comprehensive foundation for the CM3035 Advanced Web Design template requirements, implementing sophisticated identity management with modern security practices and full GDPR compliance.