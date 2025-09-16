# FocusHive Database Migration Strategy

## Overview

This document outlines the database migration strategy for the FocusHive Backend Service, implementing **Task 1.1** of the TDD Production Roadmap. The strategy uses Flyway for version-controlled, repeatable database schema management.

## Migration Strategy Implementation

### 1. Migration File Organization

**Location**: `src/main/resources/db/migration/`

**Naming Convention**: `V{version}__{description}.sql`
- V1, V2, V3, V4, V5, V8, V9, V10, V11, V12, V13, V14, V15, V16
- Sequential numbering (some gaps acceptable for historical reasons)
- Double underscore `__` separator between version and description
- Descriptive names (e.g., `V15__Add_Security_Audit_Log.sql`)

### 2. Current Migration Files

| Version | Description | Tables Created | Purpose |
|---------|-------------|----------------|---------|
| V1 | create_users_table | users | User accounts and authentication |
| V2 | create_user_related_tables | user profiles, settings | User management |
| V3 | create_hives_tables | hives, hive_members, hive_invitations | Core hive functionality |
| V4 | create_analytics_tables | focus_sessions, daily_summaries | Analytics and tracking |
| V5 | Create_chat_tables | chat_messages, reactions | Real-time messaging |
| V8 | create_buddy_system | buddy_requests, buddy_pairs | Accountability partners |
| V9 | create_notification_system | notifications, templates | Multi-channel notifications |
| V10 | performance_indexes | (indexes only) | Performance optimization |
| V11 | Create_productivity_tracking_tables | productivity_goals, achievements | Goal tracking |
| V12 | create_communication_tables | message_read_receipts | Communication features |
| V13 | create_audit_tables | audit_logs | System auditing |
| V14 | initial_data | (data only) | Reference data |
| V15 | Add_Security_Audit_Log | security_audit_log | Security monitoring |
| V16 | additional_performance_indexes | (indexes only) | Advanced performance |

### 3. Flyway Configuration

#### Production Configuration (`application.yml`)

```yaml
spring:
  flyway:
    enabled: true                    # Enable Flyway migrations
    locations: classpath:db/migration
    baseline-on-migrate: true        # Baseline for existing databases
    validate-on-migrate: true        # Validate migrations before applying
    mixed: true                      # Allow mixed transactional/non-transactional

  jpa:
    hibernate:
      ddl-auto: validate             # Validate schema matches migrations
```

#### Test Configuration (`application-migration-test.yml`)

```yaml
spring:
  flyway:
    enabled: true
    clean-disabled: false           # Allow cleaning for tests
    group: true                     # Group migrations for better error reporting
```

### 4. Database Schema Standards

#### Table Design
- **Primary Keys**: All tables use UUID primary keys with `gen_random_uuid()` default
- **Timestamps**: `created_at` and `updated_at` columns with automatic triggers
- **Soft Deletes**: `deleted_at` timestamp for logical deletion
- **Constraints**: Proper foreign key constraints with appropriate cascade rules

#### Index Strategy
- Primary key indexes (automatic)
- Foreign key indexes for join performance
- Composite indexes for common query patterns
- Partial indexes for filtered queries (e.g., WHERE deleted_at IS NULL)

#### Example Table Structure
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    -- ... other columns
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Performance indexes
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_active ON users(enabled, email_verified) WHERE deleted_at IS NULL;
```

### 5. Migration Execution Environments

#### Development Environment
- H2 in-memory database with PostgreSQL compatibility mode
- JPA `ddl-auto: create` for rapid development (Flyway disabled)
- Automatic schema generation from JPA entities

#### Testing Environment
- TestContainers PostgreSQL for integration tests
- Flyway enabled with clean/migrate cycle
- Separate `application-migration-test.yml` profile
- JPA `ddl-auto: validate` to ensure migration accuracy

#### Production Environment
- PostgreSQL database
- Flyway enabled with validation
- JPA `ddl-auto: validate` or `none`
- No automatic schema changes

### 6. Migration Best Practices

#### Schema Changes
1. **Additive Changes**: New columns, indexes, tables
   - Safe to apply without downtime
   - Use `ALTER TABLE ADD COLUMN` with defaults

2. **Breaking Changes**: Column removal, data type changes
   - Require coordination with application deployment
   - Use multi-step migration process

3. **Data Migrations**:
   - Separate from schema migrations when possible
   - Use `mixed: true` for combining DDL and DML
   - Handle large datasets in batches

#### Naming Conventions
- Tables: lowercase with underscores (`hive_members`)
- Columns: lowercase with underscores (`created_at`)
- Indexes: `idx_{table}_{column(s)}` (`idx_users_email`)
- Constraints: `fk_{table}_{referenced_table}` or similar

### 7. Rollback Strategy

#### Flyway Limitations
- Flyway Community Edition does not support automatic rollback
- Rollbacks must be implemented as forward-only migrations

#### Rollback Procedures

1. **Schema Rollbacks**: Create new migration with reverse changes
   ```sql
   -- V17__Rollback_Security_Audit_Log.sql
   DROP TABLE security_audit_log;
   ```

2. **Data Rollbacks**: Create migration to reverse data changes
   ```sql
   -- V18__Rollback_User_Data_Changes.sql
   UPDATE users SET status = 'active' WHERE status = 'migrated';
   ```

3. **Emergency Rollback**: Database backup restoration
   - Daily automated backups
   - Point-in-time recovery capability
   - Document restore procedures

#### Migration Repair
```bash
# Repair failed migration state
./gradlew flywayRepair

# Check migration status
./gradlew flywayInfo
```

### 8. Validation and Testing

#### Migration Validation Tests
1. **Naming Convention**: Ensure all migrations follow V{n}__{description}.sql pattern
2. **Execution Order**: Verify migrations run in sequence without conflicts
3. **Schema Integrity**: Check all required tables and indexes are created
4. **Data Consistency**: Validate foreign key relationships and constraints
5. **Performance**: Ensure indexes are properly created

#### Test Implementation
```java
@Test
void shouldExecuteMigrationsInOrder() {
    flyway.migrate();
    // Verify all migrations successful
    // Check schema matches expected state
}
```

### 9. Monitoring and Alerts

#### Migration Monitoring
- Monitor `flyway_schema_history` table for failed migrations
- Alert on migration failures in production
- Track migration execution time for performance

#### Health Checks
```java
@Component
public class MigrationHealthIndicator implements HealthIndicator {
    // Check migration status
    // Report any failed or pending migrations
}
```

### 10. Development Workflow

#### Adding New Migrations
1. Create new migration file with next sequential version number
2. Write SQL with appropriate rollback comments
3. Test migration in development environment
4. Run migration validation tests
5. Review and approve through code review process
6. Deploy through CI/CD pipeline

#### Example Workflow
```bash
# 1. Create new migration
# src/main/resources/db/migration/V17__Add_User_Preferences.sql

# 2. Test locally
gradle flywayMigrate

# 3. Run tests
gradle test --tests "*Migration*"

# 4. Commit and push
git add src/main/resources/db/migration/V17__Add_User_Preferences.sql
git commit -m "feat: add user preferences table [UOL-XXX]"
```

### 11. Troubleshooting

#### Common Issues

1. **Migration Checksum Mismatch**
   ```bash
   # Solution: Repair migration state
   gradle flywayRepair
   ```

2. **Failed Migration**
   ```bash
   # Solution: Fix migration file and repair
   gradle flywayRepair
   gradle flywayMigrate
   ```

3. **Schema Validation Errors**
   - Ensure JPA entities match migrated schema
   - Check for missing columns or incorrect types

#### Debug Commands
```bash
# Show migration status
gradle flywayInfo

# Validate current schema
gradle flywayValidate

# Clean and re-migrate (DEVELOPMENT ONLY)
gradle flywayClean flywayMigrate
```

### 12. Security Considerations

#### Migration Security
- Migrations run with database admin privileges
- Store database credentials securely (environment variables)
- Audit all schema changes through version control
- Review migrations for SQL injection vulnerabilities

#### Access Control
- Limit migration execution to authorized users/systems
- Use separate database users for application vs. migration
- Monitor and log all schema changes

## Conclusion

This migration strategy provides:
- ✅ **Version Control**: All schema changes tracked in Git
- ✅ **Repeatability**: Same migrations produce identical schemas
- ✅ **Validation**: Automated testing ensures migration integrity
- ✅ **Production Safety**: Careful rollback procedures and monitoring
- ✅ **Development Efficiency**: Clear workflows and standards

The strategy supports the FocusHive application's evolution while maintaining data integrity and system reliability.