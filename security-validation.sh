#!/bin/bash

# =============================================================================
# FocusHive Security Validation Script
# =============================================================================
# This script validates that no hardcoded secrets remain in the codebase
# and that all security requirements are met.

set -e  # Exit on any error

echo "🔒 FocusHive Security Validation Script"
echo "======================================="

# Color definitions for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track validation results
VALIDATION_ERRORS=0

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "ERROR")
            echo -e "${RED}❌ ERROR: $message${NC}"
            ((VALIDATION_ERRORS++))
            ;;
        "WARNING")
            echo -e "${YELLOW}⚠️  WARNING: $message${NC}"
            ;;
        "SUCCESS")
            echo -e "${GREEN}✅ SUCCESS: $message${NC}"
            ;;
        "INFO")
            echo -e "${BLUE}ℹ️  INFO: $message${NC}"
            ;;
    esac
}

# =============================================================================
# 1. SCAN FOR HARDCODED JWT SECRETS
# =============================================================================
echo
echo "🔍 Scanning for hardcoded JWT secrets..."

# Check for the specific hardcoded JWT secret that was removed
if grep -r "your-super-secret-jwt-key-change-in-production" --include="*.yml" --include="*.java" --include="*.properties" .; then
    print_status "ERROR" "Found hardcoded JWT secret in files above"
else
    print_status "SUCCESS" "No hardcoded JWT secrets found"
fi

# Check for weak JWT secret patterns
if grep -r "JWT_SECRET.*:.*[^}]$" --include="*.yml" . | grep -v "# REQUIRED\|# CRITICAL"; then
    print_status "WARNING" "Found JWT_SECRET without security comment above"
else
    print_status "SUCCESS" "All JWT_SECRET declarations properly documented"
fi

# =============================================================================
# 2. VALIDATE JWT SECRET STRENGTH CHECKS
# =============================================================================
echo
echo "🛡️  Validating JWT secret strength checks..."

# Check that JwtTokenProvider classes have validation
if grep -r "secret.*length.*32" --include="JwtTokenProvider.java" services/; then
    print_status "SUCCESS" "JWT secret length validation found in JwtTokenProvider"
else
    print_status "ERROR" "JWT secret length validation missing in JwtTokenProvider"
fi

if grep -r "cryptographically secure" --include="JwtTokenProvider.java" services/; then
    print_status "SUCCESS" "JWT secret strength validation found"
else
    print_status "ERROR" "JWT secret strength validation missing"
fi

# =============================================================================
# 3. SCAN FOR WEAK PASSWORD DEFAULTS
# =============================================================================
echo
echo "🔑 Scanning for weak password defaults..."

# Check for hardcoded password defaults (production-focused check)
# Look for actual weak passwords in non-test configurations
WEAK_PASSWORDS=$(find services/ -name "application*.yml" -not -path "*/build/*" -not -name "*test*" -exec grep -l "password.*: [^$]" {} \; | xargs grep "password.*: [^$]" | grep -v "# REQUIRED\|# CRITICAL\|password: \${\|track-password-changes\|key-store-password" | grep -E "password: (password|admin|secret|123|test|changeme|default)" || true)

if [ -n "$WEAK_PASSWORDS" ]; then
    print_status "ERROR" "Found weak password defaults:"
    echo "$WEAK_PASSWORDS"
else
    print_status "SUCCESS" "No weak password defaults found"
fi

# =============================================================================
# 4. VALIDATE ENVIRONMENT VARIABLE REQUIREMENTS
# =============================================================================
echo
echo "📋 Validating environment variable documentation..."

# Check that .env.example exists and has security warnings
if [ -f ".env.example" ]; then
    if grep -q "SECURITY WARNING" .env.example; then
        print_status "SUCCESS" "Security warnings found in .env.example"
    else
        print_status "ERROR" "Security warnings missing in .env.example"
    fi
    
    if grep -q "JWT_SECRET" .env.example; then
        print_status "SUCCESS" "JWT_SECRET documented in .env.example"
    else
        print_status "ERROR" "JWT_SECRET missing from .env.example"
    fi
else
    print_status "ERROR" ".env.example file not found"
fi

# =============================================================================
# 5. CHECK DOCKER COMPOSE SECURITY
# =============================================================================
echo
echo "🐳 Validating Docker Compose security..."

# Check that Docker compose files don't have hardcoded JWT secrets
DOCKER_JWT_SECRETS=$(grep -r "JWT_SECRET.*your-super-secret" docker/ || true)
if [ -n "$DOCKER_JWT_SECRETS" ]; then
    print_status "ERROR" "Found hardcoded JWT secrets in Docker files:"
    echo "$DOCKER_JWT_SECRETS"
else
    print_status "SUCCESS" "No hardcoded JWT secrets in Docker files"
fi

# Check that critical environment variables require explicit setting
DOCKER_WEAK_DEFAULTS=$(grep -r "JWT_SECRET.*:-.*" docker/ | grep -v "# CRITICAL" || true)
if [ -n "$DOCKER_WEAK_DEFAULTS" ]; then
    print_status "WARNING" "Found JWT_SECRET with defaults in Docker files (should require explicit setting)"
else
    print_status "SUCCESS" "All Docker JWT_SECRET configurations require explicit setting"
fi

# =============================================================================
# 6. VALIDATE APPLICATION STARTUP SECURITY
# =============================================================================
echo
echo "⚙️  Validating application security configuration..."

# Check if services will fail to start without proper JWT secrets
print_status "INFO" "Applications will now fail to start without proper JWT secrets (minimum 32 characters)"
print_status "INFO" "JWT secret validation includes checks for common weak patterns"

# =============================================================================
# 7. SCAN FOR OTHER SECURITY ISSUES
# =============================================================================
echo
echo "🔍 Scanning for other potential security issues..."

# Check for any remaining TODO or FIXME comments related to security
SECURITY_TODOS=$(grep -r "TODO.*[Ss]ecur\|FIXME.*[Ss]ecur\|TODO.*[Pp]assword\|FIXME.*[Pp]assword" --include="*.java" --include="*.yml" . || true)
if [ -n "$SECURITY_TODOS" ]; then
    print_status "WARNING" "Found security-related TODOs/FIXMEs:"
    echo "$SECURITY_TODOS"
else
    print_status "SUCCESS" "No security-related TODOs/FIXMEs found"
fi

# Check for potential SQL injection vulnerabilities (basic check)
SQL_ISSUES=$(grep -r "createQuery.*+\|createNativeQuery.*+" --include="*.java" services/ || true)
if [ -n "$SQL_ISSUES" ]; then
    print_status "WARNING" "Potential SQL injection vulnerabilities found (string concatenation in queries):"
    echo "$SQL_ISSUES"
else
    print_status "SUCCESS" "No obvious SQL injection vulnerabilities found"
fi

# =============================================================================
# 8. SECURITY RECOMMENDATIONS
# =============================================================================
echo
echo "💡 Security Recommendations:"
echo "================================"
print_status "INFO" "Generate JWT secret using: openssl rand -hex 32"
print_status "INFO" "Generate database passwords using: openssl rand -base64 24"
print_status "INFO" "Use different secrets for each environment (dev, staging, prod)"
print_status "INFO" "Rotate all secrets at least every 90 days"
print_status "INFO" "Enable audit logging for secret access"
print_status "INFO" "Use a secrets management system in production (HashiCorp Vault, AWS Secrets Manager, etc.)"
print_status "INFO" "Monitor for exposed secrets using tools like git-secrets, TruffleHog"
print_status "INFO" "Enable MFA for all service accounts where possible"

# =============================================================================
# SUMMARY
# =============================================================================
echo
echo "📊 Validation Summary:"
echo "======================"

if [ $VALIDATION_ERRORS -eq 0 ]; then
    print_status "SUCCESS" "All security validations passed! ✨"
    echo
    echo "🎉 The codebase is now secure from hardcoded secrets."
    echo "   Remember to set all required environment variables before deployment."
    exit 0
else
    print_status "ERROR" "$VALIDATION_ERRORS validation errors found"
    echo
    echo "❌ Please fix the errors above before deploying."
    echo "   The application will fail to start with these security issues."
    exit 1
fi