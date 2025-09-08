# Backend Startup Issues Documentation

## Date: 2025-08-17

## Problem Summary
The FocusHive backend service fails to start due to multiple database migration and entity mapping issues.

## Root Causes Identified

### 1. Database Migration Issues

#### A. Duplicate Table Creation
**Problem**: Multiple migrations trying to create the same tables
- `focus_sessions` table in both V4 and V11
- `chat_messages` table in both V5 and V12
- `notifications` table referenced but not created in order

**Example Error**:
```
ERROR: relation "focus_sessions" already exists
ERROR: relation "chat_messages" already exists
```

#### B. Foreign Key Type Mismatches
**Problem**: Inconsistent ID types across tables
- Some tables use `UUID`
- Others use `VARCHAR(36)`
- Some use `BIGINT`

**Example Error**:
```
ERROR: foreign key constraint "fk_chat_message_hive" cannot be implemented
ERROR: foreign key constraint "buddy_relationships_user1_id_fkey" cannot be implemented
```

#### C. Immutable Function Issues
**Problem**: PostgreSQL 16 requires functions in index predicates to be marked IMMUTABLE
- `CURRENT_TIMESTAMP` used in WHERE clauses
- `DATE()` function used in indexes

**Example Errors**:
```
ERROR: functions in index predicate must be marked IMMUTABLE
ERROR: functions in index expression must be marked IMMUTABLE
```

### 2. Hibernate Entity Mapping Issues

#### A. BuddyPreferences Entity
**Problem**: Unsupported array mapping
```
Property 'com.focushive.buddy.entity.BuddyPreferences.preferredWorkHours' 
is mapped as basic aggregate component array, but this is not yet supported.
```

#### B. Cache Configuration
**Problem**: JCache region factory not available
```
Unable to resolve name [org.hibernate.cache.jcache.internal.JCacheRegionFactory] 
as strategy [org.hibernate.cache.spi.RegionFactory]
```

## Files Affected
1. `/backend/src/main/resources/db/migration/V3__create_hives_tables.sql`
2. `/backend/src/main/resources/db/migration/V4__create_analytics_tables.sql`
3. `/backend/src/main/resources/db/migration/V5__Create_chat_tables.sql`
4. `/backend/src/main/resources/db/migration/V8__create_buddy_system.sql`
5. `/backend/src/main/resources/db/migration/V9__create_notification_system.sql`
6. `/backend/src/main/resources/db/migration/V10__performance_indexes.sql`
7. `/backend/src/main/resources/db/migration/V11__Create_productivity_tracking_tables.sql`
8. `/backend/src/main/resources/db/migration/V12__create_communication_tables.sql`
9. `/backend/src/main/resources/application.yml`
10. `/backend/src/main/java/com/focushive/buddy/entity/BuddyPreferences.java`

## Solution Strategy

### Phase 1: TDD Setup
1. Create test suite for migration validation
2. Write tests for entity mapping validation
3. Set up integration tests for backend startup

### Phase 2: Migration Fixes
1. Audit all migrations for duplicates
2. Standardize ID types to UUID across all tables
3. Remove or replace immutable function issues
4. Reorder migrations to resolve dependencies

### Phase 3: Entity Fixes
1. Fix BuddyPreferences array mapping
2. Disable or properly configure Hibernate cache
3. Validate all entity relationships

### Phase 4: Verification
1. Run all tests
2. Verify backend starts successfully
3. Test all endpoints

## Lessons Learned
1. Always use consistent ID types across all tables
2. Be careful with PostgreSQL version-specific requirements
3. Test migrations in isolation before integration
4. Maintain a clear migration versioning strategy
5. Use TDD to catch issues early

## Fix Implementation
See below for the actual implementation...