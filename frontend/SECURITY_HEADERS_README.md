# Security Headers Implementation

This directory contains a comprehensive security headers implementation for the FocusHive React frontend application.

## Quick Start

### Development
```bash
# Start dev server with security headers
npm run dev

# Test security headers
npm run test:security-headers
```

### Production Testing
```bash
# Test production deployment
npm run test:security-headers:prod
```

## Files Added/Modified

### Core Configuration
- **`vite.config.ts`** - Updated with security headers middleware for dev server
- **`package.json`** - Added security testing scripts

### Deployment Configurations
- **`public/_headers`** - Netlify/Vercel security headers
- **`nginx.conf`** - Updated NGINX configuration with security headers
- **`nginx-ssl.conf`** - NGINX HTTPS configuration with HSTS
- **`vercel.json`** - Vercel deployment configuration with headers
- **`.htaccess`** - Apache server configuration

### Cloud Deployment
- **`cloudfront-security-headers.js`** - AWS CloudFront function for headers

### Testing & Documentation
- **`test-security-headers.js`** - Security headers testing script
- **`SECURITY_HEADERS.md`** - Comprehensive documentation
- **`SECURITY_HEADERS_README.md`** - This quick reference

## Security Headers Implemented

| Header | Purpose | Configuration |
|--------|---------|---------------|
| Content-Security-Policy | XSS/injection prevention | Environment-specific |
| X-Frame-Options | Clickjacking prevention | DENY |
| X-Content-Type-Options | MIME sniffing prevention | nosniff |
| X-XSS-Protection | XSS protection | 1; mode=block |
| Strict-Transport-Security | HTTPS enforcement | Production only |
| Referrer-Policy | Referrer control | strict-origin-when-cross-origin |
| Permissions-Policy | API restrictions | Restrictive with allowances |
| Cross-Origin-* | Cross-origin protections | Strict same-origin |

## Testing

### Manual Testing
```bash
# Test local development server
npm run test:security-headers

# Test specific URL
node test-security-headers.js https://your-domain.com
```

### Browser Testing
1. Open Developer Tools → Network tab
2. Refresh the page
3. Check Response Headers for security headers

### Online Tools
- [Mozilla Observatory](https://observatory.mozilla.org/)
- [Security Headers](https://securityheaders.com/)

## Deployment Platforms

### Netlify/Vercel
- Uses `public/_headers` file
- Automatic HTTPS with security headers

### NGINX
- Use `nginx.conf` for HTTP
- Use `nginx-ssl.conf` for HTTPS with HSTS

### Apache
- Use `.htaccess` file in document root
- Enable required modules

### AWS CloudFront
- Deploy `cloudfront-security-headers.js` as CloudFront Function
- Attach to distribution's viewer response trigger

## Content Security Policy

### Development CSP
- Allows `unsafe-eval` for HMR
- Allows localhost WebSocket connections
- More permissive for development tools

### Production CSP
- Removes `unsafe-eval`
- Strict script/style sources
- Production domain restrictions

## Troubleshooting

### Common Issues
1. **CSP Violations** - Check browser console, adjust policy
2. **Mixed Content** - Ensure all resources use HTTPS
3. **Font Loading** - Verify Google Fonts domains allowed
4. **WebSocket Issues** - Check connect-src allows WSS

### Quick Fixes
```bash
# Check if headers are working
curl -I http://localhost:5173

# Verbose header check
node test-security-headers.js
```

## Next Steps

1. **Monitor** - Set up CSP violation reporting
2. **Test** - Verify all functionality works with strict CSP
3. **Refine** - Gradually tighten policies based on monitoring
4. **Automate** - Add security header tests to CI/CD

For detailed information, see `SECURITY_HEADERS.md`.