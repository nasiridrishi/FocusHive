# Backend Startup Issues and Solutions

## Overview
This document captures all the issues encountered when setting up the FocusHive backend service and their solutions. This serves as a troubleshooting guide for future development.

## Issues Encountered

### 1. Docker Network Conflicts
**Error**: Pool overlaps with other one on this address space
**Cause**: Docker network IP range conflicted with existing networks
**Solution**: 
```bash
# Pruned unused networks and created unique network
docker network prune
docker-compose up -d
```

### 2. Port Conflicts
**Error**: Ports 5432 and 6379 already in use
**Cause**: Other services using default PostgreSQL and Redis ports
**Solution**: Used different ports with localhost binding
```yaml
ports:
  - "127.0.0.1:5434:5432"  # PostgreSQL
  - "127.0.0.1:6380:6379"  # Redis
```

### 3. Duplicate Migration Version Numbers
**Error**: Found more than one migration with version 4
**Cause**: Multiple migration files with same version number
**Solution**: Renamed migrations to have unique sequential version numbers (V1-V14)

### 4. Flyway PostgreSQL 16 Compatibility
**Error**: Unsupported Database: PostgreSQL 16.9
**Cause**: Older Flyway version doesn't support PostgreSQL 16
**Solution**: Updated Flyway dependencies in `build.gradle.kts`:
```kotlin
implementation("org.flywaydb:flyway-core:10.17.0")
implementation("org.flywaydb:flyway-database-postgresql:10.17.0")
```

### 5. Immutable Function Requirements in Indexes
**Error**: functions in index predicate must be marked IMMUTABLE
**Cause**: Using CURRENT_TIMESTAMP and DATE() functions in partial indexes
**Solution**: 
```sql
-- Before
WHERE expires_at > CURRENT_TIMESTAMP
CREATE INDEX idx_sessions_date ON focus_sessions(DATE(start_time));

-- After  
WHERE expires_at IS NULL
CREATE INDEX idx_sessions_date ON focus_sessions(start_time);
```

### 6. Foreign Key Type Mismatches
**Error**: Type mismatch in foreign key constraints
**Cause**: Inconsistent ID types (UUID vs VARCHAR(36) vs BIGINT)
**Solution**: Standardized all IDs to UUID type:
```sql
-- Standardized to UUID everywhere
user_id UUID REFERENCES users(id),
hive_id UUID REFERENCES hives(id)
```

### 7. BuddyPreferences Entity Mapping Issues
**Error**: mapped as basic aggregate component array not supported
**Cause**: Hibernate couldn't map JSONB directly
**Solution**: Created custom converter:
```java
@Converter(autoApply = true)
public class WorkHoursMapConverter implements AttributeConverter<Map<String, WorkHours>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(Map<String, WorkHours> attribute) {
        // Convert Map to JSON string
    }
    
    @Override
    public Map<String, WorkHours> convertToEntityAttribute(String dbData) {
        // Convert JSON string back to Map
    }
}
```

### 8. Flyway Migration Checksum Mismatches
**Error**: Migration checksum mismatch for migration version X
**Cause**: Migration files were modified after being applied to database
**Solution**: Run Flyway repair to update checksums:
```bash
./gradlew flywayRepair
./gradlew bootRun
```

## Key Files Modified

| File | Purpose | Changes |
|------|---------|---------|
| `/backend/build.gradle.kts` | Build configuration | Updated Flyway to 10.17.0 |
| `/backend/src/main/resources/db/migration/` | Database migrations | Fixed versioning, removed immutable functions |
| `/backend/src/main/java/com/focushive/buddy/entity/` | Entity classes | Added JSONB converters |
| `/docker-compose.yml` | Container orchestration | Fixed ports and networks |
| `/backend/src/main/resources/application.yml` | Spring configuration | Disabled 2nd level cache |

## Testing

### Unit Tests
```bash
./gradlew test --tests "com.focushive.buddy.entity.BuddyPreferencesUnitTest"
```

### Integration Tests (with Testcontainers)
```bash
./gradlew test --tests "com.focushive.buddy.entity.BuddyPreferencesIntegrationTest"
```

### Manual Testing
```bash
# Start all services
docker-compose up -d

# Start backend
export DB_HOST=localhost DB_PORT=5434 DB_NAME=focushive
export DB_USER=focushive_user DB_PASSWORD=focushive_pass
export REDIS_HOST=localhost REDIS_PORT=6380 REDIS_PASSWORD=focushive_pass
./gradlew bootRun
```

## Lessons Learned

1. **Docker Networking**: Always check for existing networks and use unique names
2. **Port Management**: Use localhost binding (127.0.0.1) for internal services
3. **Migration Versioning**: Keep sequential version numbers, never modify applied migrations
4. **Database Compatibility**: Keep Flyway updated for latest PostgreSQL versions
5. **Index Functions**: Only use immutable functions in PostgreSQL indexes
6. **Data Type Consistency**: Maintain consistent types across foreign key relationships
7. **JSON Mapping**: Create custom converters for complex JSON types in Hibernate
8. **Flyway Validation**: Use `flywayRepair` when checksums don't match after fixes
9. **Test Coverage**: Write both unit and integration tests for entity mappings
10. **Environment Variables**: Use proper environment variable configuration for local development

## Current Status

✅ Docker containers running (PostgreSQL, Redis)
✅ Identity service running on port 8081
✅ Frontend accessible on LAN at http://192.168.1.37:5173/
✅ Backend running on port 8080
✅ All migrations applied successfully
✅ Unit tests passing
✅ Application fully operational

## Next Steps

1. Complete integration test setup with Testcontainers
2. Add health check endpoints for monitoring
3. Set up automated database backup strategy
4. Configure production deployment settings