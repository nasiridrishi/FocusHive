# Cloudflare Tunnel Configuration for Identity Service

## Overview
The Identity Service is configured to use Cloudflare Tunnel for secure, public access without exposing ports directly. All traffic is routed through Cloudflare's global network using an encrypted tunnel.

## Public URL
- **Public Endpoint**: `https://identity.focushive.app`
- **Internal Service**: `focushive-identity-service:8081`

## Configuration Changes

### 1. Environment Variables (.env)
The following URLs have been updated to use the public Cloudflare tunnel endpoints:

```bash
# Cloudflare Tunnel Token (DO NOT COMMIT)
CLOUDFLARE_TUNNEL_TOKEN=<token>

# JWT Issuer URLs (Public URLs for token validation)
AUTH_ISSUER=https://identity.focushive.app
JWT_ISSUER=https://identity.focushive.app/identity
JWT_ISSUER_URI=https://identity.focushive.app
JWT_JWK_SET_URI=https://identity.focushive.app/.well-known/jwks.json

# OAuth2 Configuration
OAUTH2_ISSUER=https://identity.focushive.app
APP_BASE_URL=https://identity.focushive.app
ISSUER_URI=https://identity.focushive.app

# Service-to-Service Communication (via public URLs)
NOTIFICATION_SERVICE_URL=https://notification.focushive.app

# Frontend URL
FRONTEND_URL=https://focushive.app

# CORS Origins (includes all public service URLs)
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,https://focushive.app,https://identity.focushive.app,https://notification.focushive.app,https://backend.focushive.app,https://buddy.focushive.app
```

### 2. Docker Compose Changes
- Added `cloudflared` service as a sidecar container
- Removed port exposure from identity service (no more `ports:` section)
- Service only accessible through Cloudflare tunnel

### 3. Security Benefits
- ✅ No exposed ports on host machine
- ✅ All traffic encrypted via Cloudflare
- ✅ DDoS protection from Cloudflare
- ✅ IP address hidden from public
- ✅ Access logs and analytics via Cloudflare dashboard

## Deployment Instructions

### 1. Start the Services
```bash
# From the identity-service directory
docker-compose up -d

# Check service health
docker-compose ps
```

### 2. Monitor Cloudflare Tunnel
```bash
# View tunnel logs
docker logs focushive-identity-app-tunnel

# Check tunnel status
docker exec focushive-identity-app-tunnel cloudflared tunnel info
```

### 3. Test Connectivity
```bash
# Run the test script
./test-tunnel.sh

# Or test manually
curl https://identity.focushive.app/actuator/health
curl https://identity.focushive.app/.well-known/jwks.json
```

## Important Endpoints

### Public Endpoints (via Cloudflare)
- Health Check: `https://identity.focushive.app/actuator/health`
- JWKS: `https://identity.focushive.app/.well-known/jwks.json`
- OpenID Config: `https://identity.focushive.app/.well-known/openid-configuration`
- Login: `https://identity.focushive.app/api/auth/login`
- Register: `https://identity.focushive.app/api/auth/register`

### Internal Endpoints (within Docker network)
- Internal health: `http://focushive-identity-service:8081/actuator/health`
- Used only for Docker health checks and internal monitoring

## Troubleshooting

### 1. Tunnel Not Connecting
```bash
# Check tunnel logs
docker logs focushive-identity-app-tunnel

# Verify token is set
docker exec focushive-identity-app-tunnel env | grep TUNNEL_TOKEN

# Restart tunnel
docker-compose restart cloudflared
```

### 2. Service Not Accessible
```bash
# Check if identity service is healthy
docker-compose ps

# Check identity service logs
docker logs focushive-identity-app

# Test internal connectivity
docker exec focushive-identity-app curl -f http://localhost:8081/actuator/health
```

### 3. JWT Validation Issues
- Ensure all services are using the public URLs for JWT validation
- Check that `JWT_ISSUER` matches exactly in all services
- Verify JWKS endpoint is accessible: `curl https://identity.focushive.app/.well-known/jwks.json`

### 4. CORS Issues
- Verify all service public URLs are in `CORS_ORIGINS`
- Check browser console for specific CORS errors
- Ensure preflight requests are handled correctly

## Service Communication Flow

```
Internet Users
      ↓
Cloudflare Network (DDoS Protection, WAF, CDN)
      ↓
Cloudflare Tunnel (Encrypted)
      ↓
cloudflared container (Sidecar)
      ↓
focushive-identity-service (Port 8081, internal only)
      ↓
Other services validate JWTs via public URL
```

## Monitoring

### Cloudflare Dashboard
- Monitor traffic at: https://dash.cloudflare.com
- View tunnel status and metrics
- Configure additional security rules

### Local Monitoring
```bash
# Container resource usage
docker stats focushive-identity-app focushive-identity-app-tunnel

# Service logs
docker-compose logs -f

# Health status
watch -n 5 'curl -s https://identity.focushive.app/actuator/health | jq .'
```

## Security Notes

1. **Never commit the CLOUDFLARE_TUNNEL_TOKEN** to version control
2. The tunnel token should be stored securely (e.g., environment secrets, vault)
3. Regularly rotate tokens through Cloudflare dashboard
4. Monitor Cloudflare analytics for unusual traffic patterns
5. Configure Cloudflare Access policies if needed for additional security

## Rollback Procedure

If you need to revert to direct port exposure:

1. Comment out the `cloudflared` service in docker-compose.yml
2. Uncomment the `ports:` section in the identity service
3. Update .env URLs back to local/internal addresses
4. Restart services: `docker-compose down && docker-compose up -d`