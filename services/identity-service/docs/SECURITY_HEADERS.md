# Security Headers Configuration

This document describes the comprehensive security headers implementation for the FocusHive Identity Service, which can be reused across all microservices in the platform.

## Overview

The security headers configuration provides enterprise-grade protection against common web vulnerabilities including:

- **Clickjacking attacks** (X-Frame-Options)
- **Cross-site scripting (XSS)** (Content Security Policy, X-XSS-Protection)
- **MIME type sniffing** (X-Content-Type-Options) 
- **Information leakage** (Referrer-Policy)
- **Unwanted browser feature access** (Permissions Policy)
- **Mixed content attacks** (Content Security Policy)
- **Man-in-the-middle attacks** (HTTP Strict Transport Security)

## Architecture

### Core Components

1. **SecurityHeadersConfig** - Main configuration class with environment-aware filter
2. **SecurityHeadersProperties** - Configuration properties with Spring Boot integration
3. **Application Configuration** - Environment-specific YAML configuration files

### Key Features

- **Environment-aware**: Different security policies for development vs production
- **Configurable**: All headers can be customized via application.yml
- **Performance optimized**: Filter runs with highest priority for minimal overhead
- **Standards compliant**: Implements OWASP security recommendations
- **Reusable**: Designed to be copied to other microservices

## Configuration

### Basic Configuration

Add to your `application.yml`:

```yaml
focushive:
  security:
    headers:
      enabled: true
      mode: AUTO # AUTO, DEVELOPMENT, PRODUCTION
```

### Complete Configuration

```yaml
focushive:
  security:
    headers:
      enabled: true
      mode: AUTO
      
      # HTTP Strict Transport Security
      hsts:
        enabled: true
        max-age: 31536000 # 1 year
        include-subdomains: true
        preload: true
      
      # Content Security Policy
      csp:
        enabled: true
        report-only: false
        report-uri: "https://example.com/csp-report"
        custom-directives:
          - "connect-src 'self' https://api.example.com"
      
      # Frame Options
      frame-options:
        enabled: true
        policy: DENY # DENY, SAMEORIGIN, ALLOW_FROM
        allow-from: []
      
      # Permissions Policy
      permissions-policy:
        enabled: true
        disabled-features:
          - camera
          - microphone
          - geolocation
        custom-directives:
          - "autoplay=(self)"
      
      # Additional Security Headers
      additional-headers:
        x-content-type-options: true
        x-xss-protection: true
        referrer-policy: "strict-origin-when-cross-origin"
        cross-origin-embedder-policy: false
        cross-origin-opener-policy: false
        cross-origin-resource-policy: false
```

### Environment-Specific Configuration

#### Development (application-dev.yml)
```yaml
focushive:
  security:
    headers:
      mode: DEVELOPMENT
      hsts:
        enabled: false # No SSL in development
      frame-options:
        policy: SAMEORIGIN # Allow framing for dev tools
      csp:
        report-only: true # Non-blocking for development
```

#### Production (application-prod.yml)
```yaml
focushive:
  security:
    headers:
      mode: PRODUCTION
      hsts:
        enabled: true
        max-age: 63072000 # 2 years for production
      frame-options:
        policy: DENY # Strict framing protection
      csp:
        report-only: false # Enforce CSP in production
        custom-directives:
          - "upgrade-insecure-requests"
          - "block-all-mixed-content"
```

## Security Headers Reference

### Content Security Policy (CSP)

Controls resource loading and prevents XSS attacks.

**Production Policy** (Strict):
```
default-src 'self';
script-src 'self';
style-src 'self' 'unsafe-inline';
img-src 'self' data: https:;
connect-src 'self' https: wss:;
object-src 'none';
frame-ancestors 'none';
upgrade-insecure-requests
```

**Development Policy** (Relaxed):
```
default-src 'self' localhost:* 127.0.0.1:*;
script-src 'self' 'unsafe-eval' 'unsafe-inline' localhost:*;
connect-src 'self' https: http: ws: wss: localhost:*;
```

### HTTP Strict Transport Security (HSTS)

Forces HTTPS connections for enhanced security.

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### Frame Options

Prevents clickjacking attacks by controlling iframe embedding.

- `DENY` - Never allow framing (production default)
- `SAMEORIGIN` - Allow framing from same origin (development default)
- `ALLOW-FROM` - Allow framing from specific origins

### Permissions Policy

Controls access to browser features and APIs.

**Restricted Features** (Production):
```
camera=(), microphone=(), geolocation=(), payment=(), usb=(), 
bluetooth=(), interest-cohort=(), screen-wake-lock=()
```

**Allowed Features**:
```
autoplay=(self), fullscreen=(self), picture-in-picture=(self)
```

### Additional Security Headers

- **X-Content-Type-Options**: `nosniff` - Prevents MIME type sniffing
- **X-XSS-Protection**: `1; mode=block` - Enables XSS filtering (legacy browsers)
- **Referrer-Policy**: `strict-origin-when-cross-origin` - Controls referrer information
- **X-Permitted-Cross-Domain-Policies**: `none` - Blocks Flash/PDF cross-domain policies

## Implementation Guide

### Adding to Existing Microservice

1. **Copy Configuration Classes**:
   ```bash
   cp SecurityHeadersConfig.java your-service/src/main/java/config/
   cp SecurityHeadersProperties.java your-service/src/main/java/config/
   ```

2. **Add Configuration to application.yml**:
   ```yaml
   focushive:
     security:
       headers:
         enabled: true
         mode: AUTO
   ```

3. **Environment-specific Overrides**:
   - Add development overrides to `application-dev.yml`
   - Add production overrides to `application-prod.yml`

4. **Testing**:
   ```bash
   # Test headers in development
   curl -I http://localhost:8081/actuator/health
   
   # Test headers in production
   curl -I https://identity.focushive.com/actuator/health
   ```

### Customization Examples

#### Custom CSP for API Gateway
```yaml
focushive:
  security:
    headers:
      csp:
        custom-directives:
          - "connect-src 'self' https://api.focushive.com https://auth.focushive.com"
          - "img-src 'self' https://cdn.focushive.com data:"
```

#### Relaxed Permissions for Frontend Service
```yaml
focushive:
  security:
    headers:
      permissions-policy:
        disabled-features:
          - payment
          - usb
        custom-directives:
          - "camera=(self)"  # Allow camera for user avatars
          - "microphone=(self)"  # Allow microphone for voice features
```

#### CSP Reporting Setup
```yaml
focushive:
  security:
    headers:
      csp:
        report-uri: "https://focushive.report-uri.com/r/d/csp/enforce"
        report-only: false  # Set to true for monitoring without blocking
```

## Security Testing

### Automated Testing

Run the security headers test suite:

```bash
./mvnw test -Dtest=SecurityHeadersConfigTest
```

### Manual Testing

1. **Check Headers with curl**:
   ```bash
   curl -I http://localhost:8081/api/v1/health
   ```

2. **Browser Developer Tools**:
   - Open Network tab
   - Make request to service
   - Check Response Headers section

3. **Security Scanners**:
   - [Mozilla Observatory](https://observatory.mozilla.org/)
   - [SecurityHeaders.com](https://securityheaders.com/)
   - [OWASP ZAP](https://www.zaproxy.org/)

### Expected Headers

**Development Environment**:
```
Content-Security-Policy: default-src 'self' localhost:*...
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=()...
X-Development-Mode: true
```

**Production Environment**:
```
Content-Security-Policy: default-src 'self'; script-src 'self'...
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=()...
X-Permitted-Cross-Domain-Policies: none
Cache-Control: no-store, no-cache, must-revalidate, private
```

## Common Issues and Solutions

### CSP Violations

**Problem**: Content blocked by CSP
**Solution**: Add specific domains to CSP directives or use report-only mode

```yaml
csp:
  report-only: true  # Monitor violations without blocking
  custom-directives:
    - "script-src 'self' 'unsafe-inline' https://trusted-cdn.com"
```

### Frame Embedding Issues

**Problem**: Service cannot be embedded in iframe
**Solution**: Adjust frame-options policy

```yaml
frame-options:
  policy: SAMEORIGIN  # or ALLOW_FROM with specific domains
  allow-from:
    - "https://trusted-site.com"
```

### Browser Feature Access

**Problem**: Frontend needs camera/microphone access
**Solution**: Customize permissions policy

```yaml
permissions-policy:
  disabled-features:
    - payment  # Keep sensitive features disabled
  custom-directives:
    - "camera=(self)"
    - "microphone=(self)"
```

### HSTS Issues

**Problem**: HSTS errors in development
**Solution**: Disable HSTS for non-SSL environments

```yaml
# application-dev.yml
hsts:
  enabled: false
```

## Best Practices

### Development
- Use `report-only` mode for CSP during development
- Enable `X-Development-Mode` header for debugging
- Allow localhost connections in CSP
- Use `SAMEORIGIN` for frame options

### Production
- Use strict CSP without `unsafe-inline` or `unsafe-eval`
- Enable HSTS with long max-age (1-2 years)
- Use `DENY` for frame options unless framing is required
- Disable development-specific permissions
- Enable CSP reporting for violation monitoring

### Security Monitoring
- Set up CSP violation reporting
- Monitor security header compliance
- Regular security scans with automated tools
- Log and alert on security header bypasses

## Performance Considerations

- **Filter Priority**: Runs at `HIGHEST_PRECEDENCE` for minimal overhead
- **Header Caching**: Headers are static and can be cached by browsers
- **Memory Usage**: Minimal memory footprint with String concatenation
- **CPU Impact**: Negligible processing time per request

## Compliance

This implementation meets security standards for:

- **OWASP Top 10** - Addresses multiple vulnerability categories
- **Mozilla Security Guidelines** - Follows Mozilla's recommended practices
- **PCI DSS** - Supports payment card data protection requirements
- **SOC 2** - Supports security control requirements
- **GDPR** - Supports data protection through security controls

## Migration from Spring Security Headers

If you're currently using Spring Security's built-in header support:

1. **Disable Spring Security headers**:
   ```java
   http.headers(headers -> headers.disable());
   ```

2. **Add FocusHive security headers configuration**
3. **Test that all required headers are present**
4. **Remove any custom header configurations**

This implementation provides more flexibility and environment-awareness than Spring Security's default headers.