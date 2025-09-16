# Complete Cleanup List - Notification Service

## 🔴 CRITICAL: Files to Remove Immediately

### Python Test Scripts (2 files)
```bash
test-jwt-token.py         # Test script for JWT tokens
test-rabbitmq.py          # RabbitMQ test script (keep if still needed for testing)
```

### Shell Test Scripts (6 files) - REMOVE ALL
```bash
final_comprehensive_test.sh
final_test.sh
test_custom_fixes.sh
test_endpoints.sh
test_fix.sh
test_fixed_endpoints.sh
```

### Test Output Files (2 files)
```bash
test_output.txt
test_results.txt
```

### Log Files (18 files) - 8.5MB+ total
```bash
# Root directory logs (9 files)
app.log
app2.log
app3.log
app4.log
app5.log
app6.log
app7.log
bootrun.log
notification-test.log

# Logs directory (9 files)
logs/notification-service.log
logs/notification-service.log.2025-09-14.0.gz
logs/notification-service.log.2025-09-15.0.gz
logs/notification-service.log.2025-09-16.0.gz
logs/notification-service.log.2025-09-17.0.gz
logs/notification-service.log.2025-09-18.0.gz
logs/notification-service.log.2025-09-18.1.gz
logs/notification-service.log.2025-09-18.2.gz
logs/notification-service.log.2025-09-18.3.gz
```

## 🟡 REVIEW: Files That May Need Cleanup

### Shell Scripts - Keep or Remove?
```bash
docker-deploy.sh          # ✅ KEEP - Production deployment
monitor-health.sh         # ✅ KEEP - Health monitoring
performance-test.sh       # ❓ REVIEW - Keep if performance testing needed
run-act-remote.sh         # ❓ REVIEW - GitHub Actions related
start-optimized.sh        # ✅ KEEP - Optimized startup
scripts/setup-postgres.sh # ✅ KEEP - Database setup
```

### Temporary Directories
```bash
temp-disabled/            # Contains 2 Java files - review if needed
  - CompressionConfig.java (9.3KB)
  - Http2Config.java (7.3KB)
```

### Backup Files
```bash
rabbitmq/definitions.json.backup  # RabbitMQ backup - remove if outdated
```

### Configuration Files
```bash
owasp-suppressions.xml       # ✅ KEEP - Security suppressions
```

## 🟢 CODE: Debug Statements to Remove

### Java Files with Debug Output
```java
// SecurityConfig.java - Lines 47-49
System.out.println("SecurityConfig initialized with custom components");
System.out.println("CustomAuthenticationEntryPoint: " + customAuthenticationEntryPoint);
System.out.println("RequestValidationFilter: " + requestValidationFilter);

// EndpointExistenceChecker.java - Line 36
System.out.println("=== EndpointExistenceChecker INITIALIZED ===");

// CustomAuthenticationEntryPoint.java - Lines 43-44, 51
System.out.println("=== CustomAuthenticationEntryPoint.commence called ===");
System.out.println("Path: " + path + ", Method: " + method);
System.out.println("Endpoint exists: " + endpointExists);

// RequestValidationFilter.java - Lines 64-65
System.out.println("=== RequestValidationFilter called ===");
System.out.println("Path: " + path + ", Method: " + method + ", ContentType: " + contentType);
```

## 📊 Cleanup Impact Analysis

### Space to be Freed
- **Log Files**: ~8.5MB
- **Test Scripts**: ~50KB
- **Test Outputs**: ~20KB
- **Temp Files**: ~20KB
- **Total**: ~8.6MB

### Files Summary
- **Total Files to Remove**: 28 files
- **Files to Review**: 8 files
- **Code Lines to Clean**: ~10 debug statements

## 🚀 Cleanup Commands

### Phase 1: Safe Immediate Cleanup
```bash
# Remove all log files
rm -f *.log
rm -rf logs/

# Remove test outputs
rm -f test_output.txt test_results.txt

# Remove test scripts
rm -f test_*.sh final_*.sh

# Remove Python test scripts (review first)
rm -f test-jwt-token.py  # Keep test-rabbitmq.py if still needed
```

### Phase 2: Code Cleanup
```bash
# Remove debug statements from Java files
# Use your IDE to find and remove System.out.println statements
grep -r "System.out.println" src/main/java/ --include="*.java"
```

### Phase 3: Review and Decide
```bash
# Review temp-disabled directory
ls -la temp-disabled/

# Check if performance-test.sh is needed
cat performance-test.sh | head -20

# Now using single docker-compose.yml file
```

## ✅ Updated .gitignore Entries

Already added to .gitignore:
```
# Logs
*.log
logs/
log/
*.log.*
bootrun.log
app*.log
notification-test.log

# Test artifacts
test_*.sh
final_*.sh
test_output.txt
test_results.txt
*.txt
!requirements.txt

# Temporary files
temp-*/
temp-disabled/
```

## 🎯 Priority Order

1. **IMMEDIATE** (Do Now):
   - Remove all log files (18 files, 8.5MB)
   - Remove test output files (2 files)
   - Remove test shell scripts (6 files)

2. **SOON** (Before Next Commit):
   - Remove debug System.out.println statements
   - Clean up Python test scripts
   - Review temp-disabled directory

3. **LATER** (Next Sprint):
   - Archive old backups
   - Review shell script necessity

## 📝 Git Commands After Cleanup

```bash
# Remove cached files that are now ignored
git rm --cached *.log
git rm --cached test_*.sh
git rm --cached final_*.sh
git rm --cached test_output.txt test_results.txt

# Verify cleanup
git status

# Commit cleanup
git add .gitignore
git commit -m "chore: comprehensive cleanup and update .gitignore"
```

---
*Total files identified for cleanup: 36 files*
*Estimated space savings: 8.6MB*
*Code cleanup: 10 debug statements*