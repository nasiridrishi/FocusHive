# Test Configuration Consolidation Plan

## Problem Identified
- **23 out of 42 test config files** have conflicting `@Primary` bean definitions
- Multiple files define the same beans: `PasswordEncoder`, `ObjectMapper`, `RedisTemplate`, `EmailService`, `CacheManager`
- This causes `NoUniqueBeanDefinitionException` and test hangs/failures

## Root Cause
Spring context loading fails because multiple `@Primary` beans exist for the same type, making Spring unable to decide which to use.

## Solution Strategy
**Use `NewBaseTestConfig.java` as the single source of truth** and remove/consolidate conflicting configs.

### Files to Remove (Safe deletion - redundant with NewBaseTestConfig.java):

#### Tier 1: Complete Duplicates (SAFE TO DELETE)
These provide identical beans to `NewBaseTestConfig.java`:

1. **ComprehensiveTestConfig.java** - Duplicates: PasswordEncoder, CacheManager, ObjectMapper, RedisTemplate, EmailService
2. **MinimalTestConfig.java** - Duplicates: ObjectMapper, PasswordEncoder  
3. **TestApplicationConfig.java** - Duplicates: ObjectMapper
4. **BaseTestConfig.java** - Duplicates: EmailService, PasswordEncoder, CacheManager, ObjectMapper
5. **AuthControllerTestConfig.java** - Duplicates: ObjectMapper, EmailService, PasswordEncoder
6. **TestSecurityConfig.java** - Duplicates: PasswordEncoder
7. **MinimalOWASPSecurityConfig.java** - Duplicates: PasswordEncoder, ObjectMapper

#### Tier 2: Partially Redundant (REVIEW & CONSOLIDATE)
These may have some unique functionality to preserve:

8. **IntegrationTestConfig.java** - Has some unique OAuth2/integration-specific beans
9. **OAuth2IntegrationTestConfig.java** - OAuth2-specific configurations
10. **TestContainersConfig.java** - TestContainers-specific setup
11. **StandaloneOWASPTest.java** - OWASP-specific test configuration

### Files to Keep (Specialized functionality):
- **NewBaseTestConfig.java** ✅ (Master config - single source of truth)
- **UnifiedTestConfig.java** ✅ (Already disabled - keep for reference)
- **TestRateLimiterConfig.java** (Rate limiting specific)
- **TestEncryptionConfig.java** (Encryption specific)
- **TestDatabaseConfig.java** (Database specific)
- **TestMetricsConfig.java** (Metrics specific)
- **QueryCountTestConfiguration.java** (JPA query counting)

## Implementation Plan

### Phase 1: Remove Complete Duplicates (7 files)
```bash
# Remove completely redundant configs
rm ComprehensiveTestConfig.java
rm MinimalTestConfig.java  
rm TestApplicationConfig.java
rm BaseTestConfig.java
rm AuthControllerTestConfig.java
rm TestSecurityConfig.java
rm MinimalOWASPSecurityConfig.java
```

### Phase 2: Consolidate Partial Duplicates (4 files)
- Review each file for unique beans
- Move unique functionality to specialized configs
- Remove redundant @Primary beans

### Phase 3: Update Test Imports
- Ensure tests import `NewBaseTestConfig.java` instead of removed configs
- Update `@Import` annotations
- Fix any compilation errors

## Expected Outcome
- **Reduce from 42 to ~15 config files** (eliminate ~27 files)
- **Eliminate all @Primary bean conflicts**
- **Fix hanging/failing Spring context tests**
- **Maintain all specialized test functionality**
- **Single source of truth for common test beans**

## Files Analysis Summary

### Core Infrastructure Beans Coverage:
- ✅ **NewBaseTestConfig.java** provides: ObjectMapper, PasswordEncoder, CacheManager, RedisTemplate, EmailService, MeterRegistry
- ❌ **7 redundant configs** duplicate these exact beans with @Primary
- ⚠️ **4 partially redundant** configs mix duplicates with unique functionality

### Verification Tests:
After consolidation, verify with:
```bash
./gradlew test --tests "*AuthController*"
./gradlew test --tests "*ValidationTest*" 
./gradlew test --tests "*IntegrationTest*"
```