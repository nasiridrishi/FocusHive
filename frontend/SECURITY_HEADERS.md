# Security Headers Configuration Guide

This guide provides comprehensive security headers implementation for the FocusHive React frontend application across different deployment platforms.

## Overview

The FocusHive frontend implements defense-in-depth security through comprehensive HTTP security headers that protect against common web vulnerabilities including XSS, clickjacking, MIME-type confusion, and other attack vectors.

## Security Headers Implemented

### Core Security Headers

| Header | Purpose | Configuration |
|--------|---------|---------------|
| `Content-Security-Policy` | Prevents XSS and code injection | Environment-specific policies |
| `X-Frame-Options` | Prevents clickjacking | `DENY` |
| `X-Content-Type-Options` | Prevents MIME sniffing | `nosniff` |
| `X-XSS-Protection` | Legacy XSS protection | `1; mode=block` |
| `Strict-Transport-Security` | Forces HTTPS (production only) | `max-age=31536000; includeSubDomains; preload` |
| `Referrer-Policy` | Controls referrer information | `strict-origin-when-cross-origin` |

### Modern Security Headers

| Header | Purpose | Configuration |
|--------|---------|---------------|
| `Permissions-Policy` | Restricts browser APIs | Restrictive policy with specific allowances |
| `Cross-Origin-Embedder-Policy` | Cross-origin isolation | `credentialless` |
| `Cross-Origin-Opener-Policy` | Prevents cross-origin attacks | `same-origin` |
| `Cross-Origin-Resource-Policy` | Resource sharing control | `same-origin` |

## Content Security Policy (CSP)

### Development CSP
```
default-src 'self';
script-src 'self' 'unsafe-eval' 'unsafe-inline' https://www.spotify.com;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com data:;
img-src 'self' data: https: blob:;
media-src 'self' data: https: blob:;
connect-src 'self' wss://localhost:* ws://localhost:* https://api.spotify.com;
worker-src 'self' blob:;
child-src 'self';
frame-src 'none';
object-src 'none';
base-uri 'self';
form-action 'self';
frame-ancestors 'none';
```

### Production CSP
```
default-src 'self';
script-src 'self' 'unsafe-inline' https://www.spotify.com;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com data:;
img-src 'self' data: https: blob:;
media-src 'self' data: https: blob:;
connect-src 'self' wss: ws: https://api.spotify.com;
worker-src 'self' blob:;
child-src 'self';
frame-src 'none';
object-src 'none';
base-uri 'self';
form-action 'self';
frame-ancestors 'none';
```

**Key Differences:**
- Development allows `'unsafe-eval'` for hot module replacement
- Production removes `'unsafe-eval'` and tightens localhost restrictions
- WebSocket connections adapted for production domains

## Deployment Platform Configurations

### 1. Netlify

**Configuration**: Uses `public/_headers` file

```
# public/_headers
/*
  Content-Security-Policy: [policy-here]
  X-Frame-Options: DENY
  X-Content-Type-Options: nosniff
  # ... other headers
```

**Features:**
- Automatic HTTPS with Let's Encrypt
- Global CDN with security headers
- Branch-specific deployments

**Setup:**
1. Deploy to Netlify
2. Headers automatically applied from `_headers` file
3. HSTS enabled automatically on custom domains

### 2. Vercel

**Configuration**: Uses `public/_headers` file (same format as Netlify)

**Features:**
- Automatic HTTPS
- Edge functions for custom logic
- Preview deployments with security headers

**Setup:**
1. Deploy to Vercel
2. Headers applied from `_headers` file
3. Configure custom domain for HSTS

### 3. AWS CloudFront + S3

**Configuration**: CloudFront distribution with Lambda@Edge or CloudFront Functions

```javascript
// CloudFront Function example
function handler(event) {
    var response = event.response;
    var headers = response.headers;
    
    headers['strict-transport-security'] = { value: 'max-age=31536000; includeSubDomains; preload' };
    headers['x-content-type-options'] = { value: 'nosniff' };
    headers['x-frame-options'] = { value: 'DENY' };
    // ... add all security headers
    
    return response;
}
```

**Setup:**
1. Create S3 bucket for static hosting
2. Configure CloudFront distribution
3. Add CloudFront Function for security headers
4. Configure SSL certificate via ACM

### 4. NGINX (Self-hosted)

**Configuration**: See `nginx.conf` and `nginx-ssl.conf`

**HTTP Configuration** (`nginx.conf`):
- Development and testing
- No HSTS (not HTTPS)
- All other security headers applied

**HTTPS Configuration** (`nginx-ssl.conf`):
- Production deployment
- Full HSTS implementation
- SSL/TLS hardening
- HTTP to HTTPS redirect

**Setup:**
1. Install NGINX
2. Copy appropriate configuration file
3. Update SSL certificate paths
4. Test configuration: `nginx -t`
5. Reload: `nginx -s reload`

### 5. Apache HTTP Server

**Configuration**: `.htaccess` file in document root

```apache
# .htaccess for Apache
Header always set X-Frame-Options "DENY"
Header always set X-Content-Type-Options "nosniff"
Header always set X-XSS-Protection "1; mode=block"
Header always set Referrer-Policy "strict-origin-when-cross-origin"
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' https://www.spotify.com; [...]"

# HSTS for HTTPS
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" env=HTTPS

# Permissions Policy
Header always set Permissions-Policy "geolocation=(), microphone=(self), camera=(self), [...]"

# Enable compression
<IfModule mod_deflate.c>
    AddOutputFilterByType DEFLATE text/plain text/html text/xml text/css text/javascript application/javascript application/json
</IfModule>
```

**Setup:**
1. Enable mod_headers: `a2enmod headers`
2. Enable mod_rewrite: `a2enmod rewrite`
3. Place `.htaccess` in document root
4. Restart Apache: `systemctl restart apache2`

### 6. Docker + NGINX

**Configuration**: Multi-stage Dockerfile with NGINX

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Setup:**
1. Use provided `nginx.conf` for HTTP or `nginx-ssl.conf` for HTTPS
2. For HTTPS, mount SSL certificates as volumes
3. Run: `docker build -t focushive-frontend .`
4. Deploy: `docker run -p 80:80 focushive-frontend`

## Testing Security Headers

### 1. Browser Developer Tools
```javascript
// Check CSP in browser console
console.log(document.querySelector('meta[http-equiv="Content-Security-Policy"]'));

// Check all response headers
fetch(location.href).then(r => {
    console.log([...r.headers.entries()]);
});
```

### 2. Command Line Testing
```bash
# Test security headers
curl -I https://your-domain.com

# Test specific header
curl -H "Accept: text/html" -I https://your-domain.com | grep -i "x-frame-options"
```

### 3. Online Security Scanners
- [Mozilla Observatory](https://observatory.mozilla.org/)
- [Security Headers](https://securityheaders.com/)
- [SSL Labs](https://www.ssllabs.com/ssltest/)

### 4. Automated Testing
```javascript
// Jest test example
describe('Security Headers', () => {
    test('should have CSP header', async () => {
        const response = await fetch('/');
        expect(response.headers.get('content-security-policy')).toContain('default-src');
    });
    
    test('should have X-Frame-Options', async () => {
        const response = await fetch('/');
        expect(response.headers.get('x-frame-options')).toBe('DENY');
    });
});
```

## Troubleshooting

### Common Issues

1. **CSP Violations**
   - Check browser console for CSP errors
   - Adjust policy to allow legitimate resources
   - Use `Content-Security-Policy-Report-Only` for testing

2. **Mixed Content Warnings**
   - Ensure all resources use HTTPS in production
   - Update API endpoints to use secure protocols
   - Check WebSocket connections (`wss://` not `ws://`)

3. **CORS Issues**
   - Configure backend CORS headers properly
   - Ensure `Cross-Origin-Resource-Policy` allows necessary requests
   - Check `Cross-Origin-Opener-Policy` for popup windows

4. **Font Loading Issues**
   - Verify `font-src` includes necessary domains
   - Check for `data:` URIs if using base64 fonts
   - Ensure Google Fonts domains are allowed

### CSP Debugging

1. **Enable Report-Only Mode**
   ```javascript
   // In vite.config.ts for testing
   'Content-Security-Policy-Report-Only': 'your-policy-here'
   ```

2. **Use CSP Reporter**
   ```javascript
   // Add report-uri to CSP
   'report-uri /csp-report'
   ```

3. **Browser Extensions**
   - CSP Evaluator
   - Security Headers Scanner

## Security Best Practices

### 1. Regular Updates
- Review and update CSP policies quarterly
- Monitor for new security headers and browser support
- Update dependencies regularly

### 2. Environment-Specific Policies
- Stricter policies in production
- Development-friendly policies for local development
- Staging environment should mirror production

### 3. Monitoring and Alerting
- Set up CSP violation reporting
- Monitor security header compliance
- Alert on policy violations

### 4. Progressive Enhancement
- Start with permissive policies
- Gradually tighten restrictions
- Test thoroughly in staging

## Related Documentation

- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [MDN CSP Documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [Can I Use - Security Features](https://caniuse.com/?search=security)
- [Content Security Policy Reference](https://content-security-policy.com/)

## File Locations

- `/vite.config.ts` - Development server security headers
- `/public/_headers` - Netlify/Vercel production headers
- `/nginx.conf` - NGINX HTTP configuration
- `/nginx-ssl.conf` - NGINX HTTPS configuration
- `/SECURITY_HEADERS.md` - This documentation

## Maintenance

This security configuration should be reviewed and updated:
- When adding new third-party services
- When changing deployment platforms
- After major framework updates
- Quarterly security reviews
- When security vulnerabilities are discovered