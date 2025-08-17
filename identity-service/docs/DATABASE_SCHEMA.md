# Identity Service Database Schema

## Overview

The Identity Service implements a comprehensive database schema supporting:

- **Core Identity Management**: Users, personas, and detailed profiles
- **OAuth2 Authorization Server**: Complete OAuth2 server functionality with PKCE support
- **Advanced Privacy Controls**: Granular privacy settings and GDPR compliance
- **Audit & Compliance**: Comprehensive audit logging and data export capabilities

## Schema Architecture

### Core Entity Relationships

```
users (1) ─── (M) personas ─── (M) persona_profiles
  │                │
  │                └── (M) persona_activities
  │
  ├── (M) oauth_clients
  ├── (M) privacy_settings
  ├── (M) data_permissions
  ├── (M) consent_records
  ├── (M) data_export_requests
  └── (M) audit_logs
```

## Core Identity Tables

### users
**Primary identity table storing core user information**
- `id` (UUID, PK): Unique user identifier
- `username` (VARCHAR, UNIQUE): User's unique username
- `email` (VARCHAR, UNIQUE): User's email address
- `password` (VARCHAR): Hashed password
- `display_name` (VARCHAR): User's display name
- `email_verified` (BOOLEAN): Email verification status
- Authentication fields: token fields, 2FA settings
- Preferences: language, timezone, notifications (JSONB)
- Audit fields: created_at, updated_at, last_login_at, deleted_at

**Key Features:**
- Soft delete support via `deleted_at`
- UserDetails implementation for Spring Security
- JSON-based notification preferences
- Comprehensive audit trail

### personas
**Multiple personas per user for different contexts**
- `id` (UUID, PK): Unique persona identifier
- `user_id` (UUID, FK): Reference to users table
- `name` (VARCHAR): Persona name (e.g., "work", "personal")
- `type` (ENUM): PersonaType (WORK, PERSONAL, GAMING, STUDY, CUSTOM)
- `is_default` (BOOLEAN): Whether this is the default persona
- `is_active` (BOOLEAN): Whether this persona is currently active
- `display_name` (VARCHAR): Human-readable persona name
- Profile fields: avatar_url, bio, status_message
- Privacy settings: embedded PrivacySettings object
- Preferences: theme, language, notifications (JSONB)

**Key Features:**
- One active persona per user constraint
- One default persona per user constraint
- Embedded privacy settings
- Custom attributes via persona_attributes table

### persona_profiles
**Detailed profile information for each persona**
- `id` (UUID, PK): Unique profile field identifier
- `persona_id` (UUID, FK): Reference to personas table
- `profile_key` (VARCHAR): Field name (unique per persona)
- `profile_value` (VARCHAR(2000)): Field value
- `category` (VARCHAR): Grouping category
- `data_type` (ENUM): STRING, NUMBER, BOOLEAN, DATE, JSON, EMAIL, PHONE, URL
- `visibility` (ENUM): PUBLIC, FRIENDS, PRIVATE
- `description` (VARCHAR): Field description
- Metadata: enabled, display_order, required_field, user_editable
- Source tracking: source, source_metadata
- Verification: verified_at, verification_method

**Key Features:**
- Flexible key-value storage with data type validation
- Granular visibility controls
- Verification support for EMAIL/PHONE fields
- Rich metadata and validation rules
- Profile masking for unverified sensitive data

## OAuth2 Authorization Server Tables

### oauth_clients
**OAuth2 client applications** (already exists from V1)
- Client registration and configuration
- Redirect URIs, grant types, scopes
- Security settings and access controls

### oauth_access_tokens
**OAuth2 access tokens with security features**
- `id` (UUID, PK): Unique token identifier
- `token_hash` (VARCHAR, UNIQUE): SHA-256 hash of actual token
- `user_id` (UUID, FK): Token owner
- `client_id` (UUID, FK): Requesting client
- `scopes` (Set): Granted permissions via oauth_access_token_scopes
- `expires_at` (TIMESTAMP): Token expiration
- Security: revoked, revoked_at, revocation_reason
- Analytics: usage_count, last_used_at
- Audit: issued_ip, user_agent, created_at

**Key Features:**
- Secure token storage (hash only)
- Comprehensive audit trail
- Usage analytics
- Automatic cleanup of expired tokens

### oauth_refresh_tokens
**OAuth2 refresh tokens with rotation support**
- Similar structure to access tokens
- `access_token_id` (UUID, FK): Associated access token
- `replaced_token_id` (UUID, FK): Previous token in rotation chain
- `session_id` (VARCHAR): Session tracking
- Token rotation and long-lived session support

**Key Features:**
- Token rotation for enhanced security
- Session management
- Relationship tracking between tokens
- Support for non-expiring refresh tokens

### oauth_authorization_codes
**OAuth2 authorization codes with PKCE**
- `id` (UUID, PK): Unique code identifier
- `code` (VARCHAR, UNIQUE): Authorization code value
- `user_id` (UUID, FK): Authorizing user
- `client_id` (UUID, FK): Requesting client
- `redirect_uri` (VARCHAR): Callback URL
- PKCE support: code_challenge, code_challenge_method
- Security: state, expires_at, used, used_at
- Scopes via oauth_authorization_code_scopes

**Key Features:**
- PKCE (Proof Key for Code Exchange) support
- Single-use enforcement
- Short expiration (typically 10 minutes)
- CSRF protection via state parameter

## Privacy & Permissions Tables

### privacy_settings
**Granular privacy controls**
- `id` (UUID, PK): Unique setting identifier
- `user_id` (UUID, FK): Setting owner
- `persona_id` (UUID, FK): Persona-specific setting (nullable)
- `category` (VARCHAR): Setting category
- `setting_key` (VARCHAR): Setting name
- `setting_value` (VARCHAR): Setting value
- Configuration: data_type, enabled, priority_level
- Management: overridable, source, description

**Key Features:**
- User-level and persona-level settings
- Priority-based conflict resolution
- Multiple data types (STRING, BOOLEAN, NUMBER, JSON)
- Source tracking (user, admin, system)
- Hierarchical overrides

### data_permissions
**GDPR-compliant data access permissions**
- `id` (UUID, PK): Unique permission identifier
- `user_id` (UUID, FK): Data owner
- `client_id` (UUID, FK): Requesting client (nullable for internal)
- `data_type` (VARCHAR): Type of data (profile, activity, etc.)
- `permissions` (Set): Granted permissions via data_permission_grants
- GDPR fields: purpose, legal_basis, retention_period_days
- Lifecycle: expires_at, active, revoked_at, revocation_reason
- Hierarchy: parent_permission_id for derived permissions
- Conditions: additional constraints via data_permission_conditions

**Key Features:**
- GDPR Article 6 legal basis tracking
- Data retention policies
- Permission inheritance
- Granular permission grants (read, write, export, etc.)
- Conditions and constraints
- Internal vs external permissions

### consent_records
**Complete consent audit trail**
- `id` (UUID, PK): Unique consent identifier
- `user_id` (UUID, FK): Consent giver
- `consent_type` (VARCHAR): Type of consent
- `purpose` (VARCHAR): GDPR purpose statement
- `legal_basis` (VARCHAR): GDPR legal basis
- `consent_given` (BOOLEAN): Whether consent was granted
- Versioning: consent_version, consent_source
- Context: ip_address, user_agent, geographic_location
- Lifecycle: expires_at, withdrawn_at, superseded_at
- Hierarchy: parent_consent_id for renewals
- Metadata: additional context via consent_record_metadata

**Key Features:**
- Complete GDPR compliance
- Consent versioning and renewal tracking
- Geographic context for GDPR applicability
- Withdrawal and supersession tracking
- Rich metadata support
- Legal compliance validation

## Audit & Compliance Tables

### audit_logs
**Comprehensive security and compliance logging**
- `id` (UUID, PK): Unique log entry identifier
- `user_id` (UUID, FK): Associated user (nullable for system events)
- `client_id` (UUID, FK): Associated OAuth client (nullable)
- `event_type` (VARCHAR): Specific event type
- `event_category` (ENUM): AUTHENTICATION, AUTHORIZATION, SECURITY, DATA_PRIVACY, SYSTEM
- `description` (TEXT): Human-readable description
- `resource` (VARCHAR): Affected resource
- `action` (VARCHAR): Operation performed
- `outcome` (ENUM): SUCCESS, FAILURE, PARTIAL
- `severity` (ENUM): INFO, WARNING, ERROR, CRITICAL
- Context: ip_address, user_agent, session_id, request_id
- Error handling: error_code, error_message
- Performance: duration_ms
- Security: geographic_location, risk_score
- Automation: automated_action_triggered, automated_action_details
- Metadata: additional context via audit_log_metadata

**Key Features:**
- Comprehensive event categorization
- Risk scoring and automated response tracking
- Performance monitoring
- Geographic context
- Rich metadata support
- Real-time security monitoring capabilities

### data_export_requests
**GDPR data portability requests**
- `id` (UUID, PK): Unique request identifier
- `user_id` (UUID, FK): Requesting user
- `request_type` (VARCHAR): Type of export
- `format` (ENUM): JSON, CSV, XML, PDF
- `status` (ENUM): PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
- `data_categories` (Set): Categories to export via data_export_categories
- Processing: processing_started_at, completed_at, failed_at
- File management: export_file_path, file_size_bytes, record_count
- Lifecycle: expires_at, retention_days
- Download tracking: downloaded_at, download_count, last_download_ip
- Security: encrypted, encryption_key_id, file_checksum
- Verification: verification_method, reason
- Metadata: additional context via data_export_metadata

**Key Features:**
- Complete GDPR Article 20 compliance
- Multiple export formats
- File encryption and integrity checking
- Download tracking and analytics
- Automatic cleanup and retention
- Comprehensive status tracking

## Indexes and Performance

### Core Performance Indexes
- **User lookups**: username, email (unique)
- **Persona operations**: user_id, active status
- **OAuth tokens**: token_hash (unique), user_id, client_id, expiration
- **Privacy settings**: user_id, persona_id, category
- **Audit logs**: user_id, event_type, created_at, severity

### Partial Indexes for Optimization
- Active personas only
- Valid tokens only
- Recent audit events
- Public profiles only
- Verification needed profiles

### Compliance Indexes
- GDPR export queries
- Data retention cleanup
- Audit trail queries
- Geographic compliance

## Data Types and Validation

### Custom Data Types
- **PersonaType**: WORK, PERSONAL, GAMING, STUDY, CUSTOM
- **ProfileDataType**: STRING, NUMBER, BOOLEAN, DATE, JSON, EMAIL, PHONE, URL
- **VisibilityLevel**: PUBLIC, FRIENDS, PRIVATE
- **AuditSeverity**: INFO, WARNING, ERROR, CRITICAL
- **ExportStatus**: PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED

### Validation Features
- Email format validation
- Phone number format validation
- URL format validation
- JSON structure validation
- Data type consistency checks
- Business rule constraints

## Security Features

### Access Control
- Row-level security considerations
- Persona-based access control
- Privacy setting enforcement
- Data permission validation

### Audit Trail
- Complete operation logging
- Security event monitoring
- Risk assessment scoring
- Automated response tracking

### Data Protection
- Sensitive data masking
- Encryption key management
- Secure token storage (hash only)
- GDPR compliance features

## Migration Strategy

### Version History
- **V1**: Core identity schema (users, personas, oauth_clients, basic audit)
- **V2**: Persona activities and attributes
- **V3**: OAuth2 server tables (access tokens, refresh tokens, authorization codes)
- **V4**: Privacy and permissions tables
- **V5**: Enhanced audit and compliance tables
- **V6**: Persona profiles with rich metadata

### Backward Compatibility
- Existing functionality preserved
- Progressive enhancement approach
- Soft migration of privacy settings
- Audit log enhancement without data loss

## Usage Examples

### Common Queries

```sql
-- Get active persona for user
SELECT p.* FROM personas p 
WHERE p.user_id = ? AND p.is_active = true;

-- Get public profile for persona
SELECT pp.* FROM persona_profiles pp
WHERE pp.persona_id = ? AND pp.visibility = 'PUBLIC' AND pp.enabled = true;

-- Check data permission
SELECT dp.* FROM data_permissions dp
WHERE dp.user_id = ? AND dp.client_id = ? AND dp.data_type = ? 
AND dp.active = true AND (dp.expires_at IS NULL OR dp.expires_at > CURRENT_TIMESTAMP);

-- Get effective consent
SELECT cr.* FROM consent_records cr
WHERE cr.user_id = ? AND cr.consent_type = ? 
AND cr.active = true AND cr.consent_given = true 
AND cr.withdrawn_at IS NULL 
AND (cr.expires_at IS NULL OR cr.expires_at > CURRENT_TIMESTAMP);
```

### Cleanup Operations

```sql
-- Clean up expired tokens
DELETE FROM oauth_access_tokens 
WHERE expires_at < CURRENT_TIMESTAMP AND revoked = false;

-- Clean up expired export files
UPDATE data_export_requests 
SET status = 'EXPIRED' 
WHERE expires_at < CURRENT_TIMESTAMP AND status = 'COMPLETED';
```

This schema provides a robust foundation for the Identity Service with comprehensive support for OAuth2 authorization server functionality, advanced privacy controls, and full GDPR compliance.