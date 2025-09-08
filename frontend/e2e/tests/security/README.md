# Security E2E Tests Documentation

## Overview

This directory contains comprehensive end-to-end security tests for the FocusHive application (UOL-315), covering all major security vulnerabilities and attack vectors as defined by OWASP standards and industry best practices.

## Test Coverage

### 1. Authentication Security
- **Login Attempt Limits**: Tests account lockout after failed attempts
- **JWT Token Validation**: Validates token structure, expiry, and refresh mechanisms
- **Session Hijacking Prevention**: Tests against session fixation and hijacking attacks
- **Password Reset Security**: Validates secure password reset flow with token expiry
- **Multi-Factor Authentication**: Tests MFA implementation and bypass attempts

### 2. Authorization & Access Control
- **Role-Based Access Control (RBAC)**: Tests user role enforcement
- **Resource Ownership**: Validates users can only access their own resources
- **Privilege Escalation Prevention**: Tests horizontal and vertical privilege escalation
- **API Endpoint Authorization**: Validates proper authorization on all endpoints

### 3. Input Validation & Injection Prevention
- **XSS Prevention**: Tests stored, reflected, and DOM-based XSS attacks
- **SQL Injection Prevention**: Tests various SQL injection techniques
- **Command Injection Prevention**: Tests OS command injection attacks
- **Path Traversal Prevention**: Tests directory traversal attacks
- **File Upload Security**: Tests malicious file upload prevention

### 4. Session Management
- **Session Timeout**: Tests automatic session expiry
- **Concurrent Session Limits**: Tests multiple session handling
- **Secure Logout**: Validates token invalidation on logout
- **Remember Me Security**: Tests persistent session security

### 5. Password Security
- **Password Strength**: Tests password complexity requirements
- **Password History**: Validates password reuse prevention
- **Brute Force Protection**: Tests account protection against brute force attacks

### 6. CSRF Protection
- **CSRF Token Validation**: Tests cross-site request forgery protection
- **Same-Origin Policy**: Tests origin validation enforcement

### 7. API Security
- **Rate Limiting**: Tests API rate limiting with 429 responses
- **API Key Validation**: Tests API key security mechanisms
- **Request Size Limits**: Tests payload size restrictions
- **Header Validation**: Tests malicious header handling

### 8. Data Security
- **Sensitive Data Masking**: Tests PII and sensitive data protection
- **Secure Data Transmission**: Tests HTTPS enforcement
- **Data Encryption**: Tests data encryption at rest and in transit

### 9. Security Headers
- **Content Security Policy (CSP)**: Tests XSS protection headers
- **X-Frame-Options**: Tests clickjacking protection
- **X-Content-Type-Options**: Tests MIME sniffing protection
- **Strict-Transport-Security**: Tests HTTPS enforcement headers

### 10. OWASP Top 10 Coverage
- **A01: Broken Access Control**: Comprehensive access control testing
- **A02: Cryptographic Failures**: Tests encryption and data protection
- **A03: Injection**: Tests all injection attack vectors
- **A04: Insecure Design**: Tests security design principles
- **A05: Security Misconfiguration**: Tests configuration security
- **A06: Vulnerable Components**: Tests component security
- **A07: Identification and Authentication Failures**: Tests auth mechanisms
- **A08: Software and Data Integrity Failures**: Tests integrity checks
- **A09: Security Logging and Monitoring Failures**: Tests logging security
- **A10: Server-Side Request Forgery**: Tests SSRF protection

## File Structure

```
security/
├── security.spec.ts          # Main security test suite (50+ test scenarios)
├── README.md                 # This documentation file
└── ../pages/SecurityPage.ts  # Page Object Model for security testing
└── ../helpers/security.helper.ts  # Security utilities and attack payloads
```

## Test Scenarios Summary

### Total Test Count: 47+ Individual Security Tests

1. **Authentication Security (5 tests)**
   - Login attempt limits and account lockout
   - JWT token validation and expiry
   - Session hijacking prevention
   - Password reset flow security
   - Multi-factor authentication security

2. **Authorization & Access Control (4 tests)**
   - Role-based access control enforcement
   - Resource ownership validation
   - Privilege escalation prevention
   - API endpoint authorization

3. **Input Validation & Injection Prevention (5 tests)**
   - XSS prevention (stored, reflected, DOM-based)
   - SQL injection prevention
   - Command injection prevention
   - Path traversal prevention
   - File upload security

4. **Session Management (4 tests)**
   - Session timeout handling
   - Concurrent session limits
   - Secure logout with token invalidation
   - Remember me functionality security

5. **Password Security (3 tests)**
   - Password strength requirements
   - Password history validation
   - Brute force protection

6. **CSRF Protection (2 tests)**
   - CSRF token validation
   - Same-origin policy enforcement

7. **API Security (4 tests)**
   - Rate limiting enforcement
   - API key validation
   - Request size limits
   - Header validation

8. **Data Security (3 tests)**
   - Sensitive data masking
   - Secure data transmission
   - Data encryption validation

9. **Security Headers (4 tests)**
   - Content Security Policy implementation
   - X-Frame-Options header
   - X-Content-Type-Options header
   - Strict-Transport-Security header

10. **OWASP Top 10 Coverage (7 tests)**
    - Broken authentication prevention
    - Sensitive data exposure prevention
    - XML external entities (XXE) prevention
    - Security misconfiguration detection
    - Vulnerable component detection
    - Insufficient logging detection
    - Server-side request forgery prevention

11. **Advanced Security Testing (3 tests)**
    - Security under high load conditions
    - Security during error conditions
    - Advanced persistent threat protection

## Usage

### Running Security Tests

```bash
# Run all security tests
npx playwright test tests/security/

# Run specific security test categories
npx playwright test tests/security/ --grep "Authentication Security"
npx playwright test tests/security/ --grep "OWASP Top 10"

# Run with detailed security reporting
npx playwright test tests/security/ --reporter=html --reporter-options="open=always"
```

### Environment Configuration

Ensure the following environment variables are set:

```bash
E2E_BASE_URL=http://localhost:5173          # Frontend URL
E2E_API_BASE_URL=http://localhost:8080      # Backend API URL
E2E_IDENTITY_API_URL=http://localhost:8081  # Identity Service URL
```

### Test Data

The security tests use predefined test users and attack payloads:

- **Valid Test User**: `e2e_test_user@focushive.com`
- **Admin Test User**: Generated dynamically during tests
- **Attack Payloads**: Comprehensive collection of real-world attack vectors

## Security Test Utilities

### SecurityPage Class

The `SecurityPage` class provides methods to interact with security-related UI elements:

- Form interaction methods
- Security message verification
- Access control validation
- Error handling verification

Key methods:
- `verifyAccountLockout()`: Validates account lockout behavior
- `verifyTokenExpiredBehavior()`: Checks token expiry handling
- `verifyAccessDenied()`: Validates access control enforcement
- `verifyXSSPrevention()`: Checks XSS attack prevention

### SecurityHelper Class

The `SecurityHelper` class provides comprehensive security testing utilities:

- Attack payload generation
- Security metric monitoring
- Session manipulation
- Token validation
- File upload testing

Key features:
- **XSS Payloads**: 10+ different XSS attack vectors
- **SQL Injection Payloads**: 10+ SQL injection techniques
- **Command Injection**: Various OS command injection attempts
- **Path Traversal**: Directory traversal attack patterns
- **Session Security**: Session fixation and hijacking tests

## Attack Payload Examples

### XSS Payloads
```javascript
// Basic script injection
<script>alert("XSS")</script>

// Event handler injection
<img src="x" onerror="alert('XSS')">

// Filter bypass techniques
<ScRiPt>alert("XSS")</ScRiPt>
<scr<script>ipt>alert("XSS")</scr</script>ipt>
```

### SQL Injection Payloads
```sql
-- Authentication bypass
admin'--

-- Union-based injection
' UNION SELECT username, password FROM users--

-- Time-based blind injection
'; WAITFOR DELAY '00:00:05'--
```

### Command Injection Payloads
```bash
; cat /etc/passwd
| whoami
& dir
$(whoami)
```

## Security Metrics and Monitoring

The tests collect comprehensive security metrics:

- **Failed Authentication Attempts**: Tracks brute force attempts
- **Rate Limiting Metrics**: Monitors API rate limit enforcement
- **Session Security Metrics**: Tracks session timeout and invalidation
- **Response Status Codes**: Validates proper error responses (401, 403, 429)

## Integration with CI/CD

### GitHub Actions Integration

```yaml
- name: Run Security Tests
  run: |
    npx playwright test tests/security/
    npx playwright show-report
```

### Test Reporting

Security tests generate detailed reports including:
- **Attack Vector Coverage**: Lists all tested attack patterns
- **Security Metric Reports**: Detailed security metrics
- **Vulnerability Detection**: Any detected security issues
- **Performance Impact**: Security measure performance impact

## Security Standards Compliance

This test suite ensures compliance with:

- **OWASP Top 10 (2021)**
- **NIST Cybersecurity Framework**
- **ISO 27001 Security Standards**
- **PCI DSS Requirements** (where applicable)
- **GDPR Privacy Requirements**

## Expected Behaviors

### Successful Security Tests

✅ **Account Lockout**: After 5 failed attempts  
✅ **Token Expiry**: Tokens expire and refresh properly  
✅ **XSS Prevention**: All XSS payloads are escaped/blocked  
✅ **SQL Injection Prevention**: No SQL errors or data exposure  
✅ **Rate Limiting**: 429 responses after rate limit exceeded  
✅ **HTTPS Enforcement**: All traffic uses HTTPS  
✅ **Security Headers**: All required headers present  

### Security Vulnerabilities Detection

🚨 **Critical**: Immediate attention required  
⚠️ **High**: Should be fixed in next release  
ℹ️ **Medium**: Should be addressed in upcoming sprints  
📝 **Low**: Consider for future improvements  

## Troubleshooting

### Common Issues

1. **Test Environment Setup**
   - Ensure all services are running (frontend, backend, identity service)
   - Verify environment variables are set correctly
   - Check network connectivity between services

2. **False Positives**
   - Some security measures may cause legitimate test failures
   - Review error messages to distinguish between security blocks and test issues
   - Check browser console for security-related warnings

3. **Performance Impact**
   - Security tests may run slower due to attack simulation
   - Rate limiting tests require waiting periods
   - Large payload tests may timeout on slower systems

### Debugging Security Tests

Enable debug mode for detailed security test logging:

```bash
DEBUG=security:* npx playwright test tests/security/
```

### Security Test Maintenance

- **Regular Updates**: Update attack payloads as new vulnerabilities are discovered
- **Compliance Reviews**: Regularly review tests against latest security standards
- **Performance Monitoring**: Monitor security test execution time
- **False Positive Management**: Regularly review and tune security tests

## Contributing

When adding new security tests:

1. Follow the established patterns in existing tests
2. Add comprehensive documentation for new attack vectors
3. Include both positive and negative test cases
4. Update this README with new test coverage
5. Ensure all tests handle edge cases gracefully

## Security Reporting

If security tests reveal actual vulnerabilities:

1. **Do not commit** sensitive vulnerability details to version control
2. Follow responsible disclosure practices
3. Report findings to the security team immediately
4. Document remediation steps in secure channels

---

**Last Updated**: January 2025  
**Test Suite Version**: 1.0.0  
**Coverage**: 47+ security test scenarios across 10 major security categories