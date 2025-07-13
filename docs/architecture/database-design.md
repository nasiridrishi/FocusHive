# FocusHive Database Design

## Overview

This document outlines the complete database schema design for FocusHive, including PostgreSQL tables for persistent data and Redis structures for real-time features and caching.

## Design Principles

1. **Data Integrity**: Strong foreign key constraints and validation rules
2. **Performance**: Strategic indexing and denormalization where appropriate
3. **Privacy**: User data isolation and soft deletes for compliance
4. **Scalability**: Partitioning-ready design for large tables
5. **Audit Trail**: Comprehensive tracking of user actions
6. **Time Zones**: All timestamps in UTC with user timezone preferences

## PostgreSQL Schema

### Core Tables

#### users
Primary user account information with authentication details.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    bio TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    role VARCHAR(20) DEFAULT 'USER',
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_active ON users(is_active, is_verified) WHERE deleted_at IS NULL;
```

#### user_profiles
Extended user information and preferences.

```sql
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    occupation VARCHAR(100),
    company VARCHAR(100),
    location VARCHAR(200),
    website VARCHAR(500),
    github_username VARCHAR(100),
    linkedin_url VARCHAR(500),
    twitter_handle VARCHAR(100),
    focus_goals TEXT,
    preferred_work_hours JSONB,
    notification_preferences JSONB DEFAULT '{}',
    privacy_settings JSONB DEFAULT '{"profile_visible": true, "stats_visible": true}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### oauth_connections
Social login connections for users.

```sql
CREATE TABLE oauth_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    profile_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_user_id)
);

CREATE INDEX idx_oauth_user ON oauth_connections(user_id);
```

#### password_reset_tokens
Secure password reset functionality.

```sql
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token_hash) WHERE used_at IS NULL;
```

### Hive Management

#### hives
Virtual co-working spaces.

```sql
CREATE TABLE hives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) DEFAULT 'GENERAL',
    max_members INTEGER DEFAULT 10 CHECK (max_members > 0 AND max_members <= 100),
    is_public BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    background_image VARCHAR(500),
    theme_color VARCHAR(7),
    rules TEXT,
    tags TEXT[],
    settings JSONB DEFAULT '{}',
    member_count INTEGER DEFAULT 0,
    total_focus_minutes BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_hives_owner ON hives(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_public ON hives(is_public, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_slug ON hives(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_tags ON hives USING GIN(tags) WHERE deleted_at IS NULL;
```

#### hive_members
Membership tracking with roles and permissions.

```sql
CREATE TABLE hive_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP,
    total_minutes INTEGER DEFAULT 0,
    consecutive_days INTEGER DEFAULT 0,
    is_muted BOOLEAN DEFAULT FALSE,
    notification_settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hive_id, user_id)
);

CREATE INDEX idx_hive_members_user ON hive_members(user_id);
CREATE INDEX idx_hive_members_hive ON hive_members(hive_id);
CREATE INDEX idx_hive_members_active ON hive_members(hive_id, last_active_at);
```

#### hive_invitations
Invitation links for private hives.

```sql
CREATE TABLE hive_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    created_by_id UUID NOT NULL REFERENCES users(id),
    code VARCHAR(32) UNIQUE NOT NULL,
    max_uses INTEGER,
    uses_count INTEGER DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hive_invitations_code ON hive_invitations(code) WHERE expires_at > CURRENT_TIMESTAMP;
```

### Analytics & Sessions

#### focus_sessions
Individual work sessions with detailed tracking.

```sql
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hive_id UUID REFERENCES hives(id) ON DELETE SET NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    scheduled_duration INTEGER NOT NULL,
    actual_duration INTEGER,
    type VARCHAR(20) DEFAULT 'FOCUS',
    completed BOOLEAN DEFAULT FALSE,
    breaks_taken INTEGER DEFAULT 0,
    distractions_logged INTEGER DEFAULT 0,
    productivity_score INTEGER CHECK (productivity_score >= 0 AND productivity_score <= 100),
    notes TEXT,
    tags TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user ON focus_sessions(user_id);
CREATE INDEX idx_sessions_hive ON focus_sessions(hive_id) WHERE hive_id IS NOT NULL;
CREATE INDEX idx_sessions_time ON focus_sessions(user_id, start_time);
CREATE INDEX idx_sessions_completed ON focus_sessions(user_id, completed) WHERE completed = TRUE;
```

#### session_breaks
Break tracking within sessions.

```sql
CREATE TABLE session_breaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES focus_sessions(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration INTEGER,
    reason VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_breaks_session ON session_breaks(session_id);
```

#### daily_summaries
Aggregated daily statistics for performance.

```sql
CREATE TABLE daily_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_minutes INTEGER DEFAULT 0,
    sessions_count INTEGER DEFAULT 0,
    completed_sessions INTEGER DEFAULT 0,
    average_session_length INTEGER,
    productivity_score INTEGER,
    most_productive_hour INTEGER,
    breaks_taken INTEGER DEFAULT 0,
    goals_achieved INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, date)
);

CREATE INDEX idx_daily_summaries_user_date ON daily_summaries(user_id, date DESC);
```

#### user_achievements
Gamification and milestone tracking.

```sql
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_type VARCHAR(50) NOT NULL,
    achievement_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    points INTEGER DEFAULT 0,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress JSONB,
    UNIQUE(user_id, achievement_key)
);

CREATE INDEX idx_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_achievements_unlocked ON user_achievements(user_id, unlocked_at DESC);
```

### Communication

#### chat_messages
Real-time messaging within hives.

```sql
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'TEXT',
    metadata JSONB,
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_messages_hive ON chat_messages(hive_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_sender ON chat_messages(sender_id);
```

#### notifications
User notifications for various events.

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    data JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE is_read = FALSE;
```

### Audit & Compliance

#### audit_logs
Comprehensive activity tracking for security and debugging.

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action, created_at DESC);

-- Partition by month for scalability
CREATE TABLE audit_logs_2025_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
```

## Redis Data Structures

### User Presence
Real-time user status tracking.

```redis
# User online status
presence:user:{userId} = {
    status: "online|away|busy|offline",
    lastSeen: timestamp,
    currentHive: hiveId,
    activity: "focusing|break|idle"
}
TTL: 5 minutes (refreshed on activity)

# Hive active members
hive:{hiveId}:members = SET of userIds
TTL: None (managed by presence service)

# User's active session
session:user:{userId} = {
    sessionId: UUID,
    hiveId: UUID,
    startTime: timestamp,
    targetDuration: minutes
}
TTL: Session duration + buffer
```

### Caching Layer
Performance optimization caches.

```redis
# User profile cache
cache:user:{userId} = JSON user data
TTL: 1 hour

# Hive details cache
cache:hive:{hiveId} = JSON hive data
TTL: 30 minutes

# Daily summary cache
cache:summary:{userId}:{date} = JSON summary data
TTL: 24 hours

# Leaderboard cache
cache:leaderboard:{hiveId}:{period} = ZSET of userId:score
TTL: 15 minutes
```

### Rate Limiting
API rate limiting per user.

```redis
# API rate limit
rate:api:{userId}:{endpoint} = counter
TTL: 1 minute

# Session creation limit
rate:session:{userId} = counter
TTL: 1 hour
```

### Real-time Features
WebSocket and notification support.

```redis
# Pending notifications
notifications:{userId} = LIST of notification data
TTL: 7 days

# WebSocket connection tracking
ws:connections:{userId} = SET of connection IDs
TTL: None (managed by WebSocket service)

# Typing indicators
typing:{hiveId}:{userId} = 1
TTL: 5 seconds
```

## Migration Strategy

1. **Initial Schema**: Create all tables with Flyway migrations
2. **Data Seeding**: Add essential data (achievement types, default settings)
3. **Indexes**: Add after initial data load for performance
4. **Partitioning**: Implement for audit_logs and chat_messages as needed
5. **Archival**: Move old data to archive tables after 1 year

## Performance Considerations

1. **Connection Pooling**: Use HikariCP with appropriate pool sizes
2. **Query Optimization**: EXPLAIN ANALYZE critical queries
3. **Batch Operations**: Use batch inserts for bulk data
4. **Read Replicas**: Consider for analytics queries
5. **Caching Strategy**: Cache frequently accessed, rarely changed data

## Security Measures

1. **Encryption**: Encrypt sensitive fields (tokens, personal data)
2. **Row-Level Security**: Implement for multi-tenant isolation
3. **Audit Trail**: Log all data modifications
4. **Soft Deletes**: Maintain data for compliance
5. **PII Handling**: Separate PII into encrypted tables

## Backup & Recovery

1. **Daily Backups**: Full database backup
2. **Point-in-Time Recovery**: WAL archiving enabled
3. **Geo-Replication**: For disaster recovery
4. **Test Restores**: Monthly restore testing
5. **Data Retention**: 7 years for audit logs, 1 year for messages

## Database Design Decisions

### 1. **UUID vs Sequential IDs**
- **Decision**: Use UUIDs for all primary keys
- **Rationale**: 
  - Enables distributed ID generation without coordination
  - Prevents enumeration attacks
  - Facilitates data migration and merging
  - Supports offline-first architecture for future desktop app

### 2. **Soft Deletes Pattern**
- **Decision**: Implement soft deletes with `deleted_at` timestamp
- **Rationale**:
  - Compliance with data retention regulations
  - Ability to restore accidentally deleted data
  - Maintains referential integrity in related tables
  - Enables audit trail of deletions

### 3. **JSONB for Flexible Data**
- **Decision**: Use JSONB columns for settings, metadata, and preferences
- **Rationale**:
  - Avoids schema migrations for new settings
  - Enables feature flags and A/B testing
  - Supports user-specific customization
  - PostgreSQL provides excellent JSONB querying capabilities

### 4. **Partitioning Strategy**
- **Decision**: Partition audit_logs by month, ready for chat_messages
- **Rationale**:
  - Improves query performance for time-based data
  - Enables efficient data archival
  - Reduces index size and maintenance overhead
  - Supports compliance with data retention policies

### 5. **Denormalization Choices**
- **Decision**: Denormalize member_count and total_focus_minutes in hives table
- **Rationale**:
  - Avoids expensive COUNT queries on hot paths
  - Acceptable trade-off for real-time statistics
  - Updated via triggers for consistency
  - Critical for leaderboard performance

### 6. **Index Strategy**
- **Decision**: Create partial indexes with WHERE clauses
- **Rationale**:
  - Reduces index size by excluding deleted records
  - Improves query performance for active data
  - Supports efficient soft delete pattern
  - Optimizes for common query patterns

### 7. **Relationship Design**
- **Decision**: Use junction tables (hive_members) instead of arrays
- **Rationale**:
  - Enables rich relationship metadata (role, joined_at)
  - Supports efficient queries in both directions
  - Maintains referential integrity
  - Scales better than array columns

### 8. **Time Zone Handling**
- **Decision**: Store all timestamps in UTC, user timezone separate
- **Rationale**:
  - Simplifies time calculations and comparisons
  - Avoids DST complications
  - Standard practice for global applications
  - Enables accurate cross-timezone analytics

### 9. **Versioning Strategy**
- **Decision**: Implement optimistic locking with version column
- **Rationale**:
  - Prevents lost updates in concurrent scenarios
  - Lighter weight than pessimistic locking
  - Works well with REST APIs
  - Supported natively by JPA/Hibernate

### 10. **Redis Data Structure Choices**
- **Decision**: Use specific Redis data types for different use cases
- **Rationale**:
  - Sorted Sets for leaderboards (automatic ranking)
  - Hashes for object caching (efficient field access)
  - Sets for presence tracking (fast membership tests)
  - Lists for activity feeds (natural ordering)

### 11. **Constraint Implementation**
- **Decision**: Implement constraints at both database and application level
- **Rationale**:
  - Database constraints ensure data integrity
  - Application validation provides better error messages
  - Bean Validation annotations for standardization
  - Prevents invalid data even with direct DB access

### 12. **Audit Log Design**
- **Decision**: Separate audit_logs table with monthly partitions
- **Rationale**:
  - Compliance with security requirements
  - Efficient querying of recent events
  - Easy archival of old data
  - No performance impact on main tables

### 13. **Chat Message Storage**
- **Decision**: Store in PostgreSQL with potential future partitioning
- **Rationale**:
  - Maintains consistency with user data
  - Enables full-text search capabilities
  - Supports message threading and reactions
  - Can partition by hive_id if needed

### 14. **Session Management**
- **Decision**: Hybrid approach with database sessions and Redis cache
- **Rationale**:
  - Database provides persistence and audit trail
  - Redis enables fast session validation
  - Supports session revocation
  - Enables "active sessions" feature

### 15. **Achievement System**
- **Decision**: Flexible achievement types with JSONB progress tracking
- **Rationale**:
  - Enables complex achievement criteria
  - Easy to add new achievement types
  - Supports partial progress tracking
  - Minimizes schema changes

## Implementation Notes

1. **JPA Considerations**:
   - Use `@Where(clause = "deleted_at IS NULL")` for soft delete entities
   - Implement custom ID generators for consistent UUID format
   - Use `@Convert` for JSONB to Java object mapping
   - Enable Hibernate's second-level cache for read-heavy entities

2. **Migration Best Practices**:
   - Always include rollback scripts
   - Test migrations on production-like data volumes
   - Use transactional migrations where possible
   - Document any manual steps required

3. **Performance Monitoring**:
   - Set up pg_stat_statements for query analysis
   - Monitor index usage with pg_stat_user_indexes
   - Track table bloat and schedule VACUUM
   - Use EXPLAIN ANALYZE for slow query optimization

4. **Security Implementation**:
   - Use database roles for least privilege access
   - Implement row-level security for multi-tenancy
   - Encrypt sensitive columns with pgcrypto
   - Regular security audits of database access