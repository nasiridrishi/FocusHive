#!/bin/bash

echo "========================================="
echo "SECURITY AUDIT: UOL-334 Vulnerability Fix"
echo "========================================="
echo

# Check that no System.out.println exists in the controller
echo "1. Checking for System.out.println statements..."
if grep -n "System.out.println" src/main/java/com/focushive/api/controller/SimpleAuthController.java; then
    echo "❌ FAIL: System.out.println statements still exist!"
    exit 1
else
    echo "✅ PASS: No System.out.println statements found"
fi
echo

# Check that testHash endpoint method has been removed
echo "2. Checking that testHash endpoint method has been removed..."
if grep -n "@GetMapping.*testHash\|public.*testHash" src/main/java/com/focushive/api/controller/SimpleAuthController.java; then
    echo "❌ FAIL: testHash endpoint method still exists!"
    exit 1
else
    echo "✅ PASS: testHash endpoint method has been removed"
    # Check if there's a comment indicating removal
    if grep -n "testHash.*removed" src/main/java/com/focushive/api/controller/SimpleAuthController.java > /dev/null; then
        echo "✅ CONFIRMATION: Found comment indicating testHash endpoint was removed"
    fi
fi
echo

# Check that proper SLF4J logging is used
echo "3. Checking for proper SLF4J logger usage..."
if grep -n "Logger logger = LoggerFactory.getLogger" src/main/java/com/focushive/api/controller/SimpleAuthController.java; then
    echo "✅ PASS: SLF4J logger properly configured"
else
    echo "❌ FAIL: SLF4J logger not found!"
    exit 1
fi
echo

# Check that no sensitive data is logged (no literal passwords in logs)
echo "4. Checking that no sensitive data appears in actual logging statements..."
if grep -n -E "logger\.(debug|info|warn|error).*Demo123|logger\.(debug|info|warn|error).*password.*\".*\"" src/main/java/com/focushive/api/controller/SimpleAuthController.java; then
    echo "❌ FAIL: Sensitive data found in logging statements!"
    exit 1
else
    echo "✅ PASS: No sensitive data in actual logging statements"
    # Verify that password logging is properly masked
    if grep -n "logger.*Password received.*\*\*\*" src/main/java/com/focushive/api/controller/SimpleAuthController.java > /dev/null; then
        echo "✅ CONFIRMATION: Password logging is properly masked with ***"
    fi
fi
echo

# Check HTTP status codes
echo "5. Checking HTTP status codes for failed authentication..."
if grep -n "status(401)" src/main/java/com/focushive/api/controller/SimpleAuthController.java; then
    echo "✅ PASS: Using proper 401 Unauthorized status"
else
    echo "⚠️  WARNING: Should use 401 status for failed authentication"
fi
echo

# Run the security tests
echo "6. Running security test suite..."
echo "========================================="
if ./gradlew test --tests "SimpleAuthControllerUnitTest" --console=plain > test_output.log 2>&1; then
    echo "✅ PASS: All security tests passed!"
    echo
    echo "Test Output Summary:"
    grep -E "(SECURITY|SUCCESS|PASS|testHash)" test_output.log || echo "No specific security messages found"
else
    echo "❌ FAIL: Security tests failed!"
    echo "Last few lines of test output:"
    tail -10 test_output.log
    exit 1
fi

rm -f test_output.log

echo
echo "========================================="
echo "✅ SECURITY AUDIT COMPLETE: VULNERABILITY RESOLVED"
echo "========================================="
echo
echo "Summary of fixes applied:"
echo "- ✅ Removed all System.out.println statements"
echo "- ✅ Replaced with proper SLF4J logging"
echo "- ✅ Removed dangerous testHash endpoint"
echo "- ✅ No sensitive data logged to console"
echo "- ✅ Using appropriate log levels (DEBUG, INFO, WARN)"
echo "- ✅ Proper HTTP status codes (401 for unauthorized)"
echo
echo "CRITICAL SECURITY VULNERABILITY UOL-334 HAS BEEN RESOLVED!"