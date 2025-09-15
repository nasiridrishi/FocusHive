# Cloudflare Tunnel Setup for Buddy Service

## Overview

This document describes the production-ready Cloudflare tunnel configuration for the FocusHive Buddy Service. The service uses Cloudflare Zero Trust tunnels to provide secure, scalable access without exposing ports to the public internet.

## Key Features

✅ **No Port Exposure**: All ports are internal to the Docker network
✅ **Automatic HTTPS**: All traffic is automatically encrypted
✅ **DDoS Protection**: Built-in Cloudflare protection
✅ **Global CDN**: Cloudflare's global network for low latency
✅ **Zero Trust Security**: Authentication and access control at the edge

## Configuration

### Public URL
- **Domain**: `https://buddy.focushive.app`
- **Internal Service**: `http://buddy-service:8087`

### Tunnel Token
The tunnel token is stored in the `.env` file:
```env
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiZWNhM2U0YjQ0MzQ3OTkwYmY4OTg1NTA4YTdlMzI5YjUiLCJ0IjoiN2FmMjY4MTEtMmY2MC00Y2JiLWE0MGYtZWE3MjkxZjBhYmI0IiwicyI6IllXSTVZVFpoTURNdE56a3lNaTAwTkdRekxUZzJPV0V0WkRZMFlqQm1PRGxqWm1JNCJ9
```

### Inter-Service Communication
All services communicate through Cloudflare tunnels using public URLs:
```env
IDENTITY_SERVICE_URL=https://identity.focushive.app
BACKEND_SERVICE_URL=https://backend.focushive.app
NOTIFICATION_SERVICE_URL=https://notification.focushive.app
```

## Docker Compose Configuration

### Service Definition
```yaml
buddy-service:
  image: buddy-service:1.0.0
  container_name: focushive-buddy-app
  # NO PORTS EXPOSED - all traffic through Cloudflare
  networks:
    - focushive-shared-network
```

### Cloudflared Container
```yaml
cloudflared:
  image: cloudflare/cloudflared:latest
  container_name: focushive-buddy-tunnel
  command: tunnel run
  environment:
    - TUNNEL_TOKEN=${CLOUDFLARE_TUNNEL_TOKEN}
    - TUNNEL_URL=http://buddy-service:8087
    - TUNNEL_METRICS=0.0.0.0:2000
    - TUNNEL_LOGLEVEL=info
  depends_on:
    buddy-service:
      condition: service_healthy
  networks:
    - focushive-shared-network
  restart: unless-stopped
```

## Security Benefits

### 1. No Direct Internet Exposure
- Application ports (8087, 8088) are not exposed to the host
- All traffic must go through Cloudflare's network
- Reduces attack surface significantly

### 2. Automatic TLS/SSL
- End-to-end encryption without managing certificates
- Always up-to-date with latest TLS versions
- Perfect SSL Labs score out of the box

### 3. DDoS Protection
- Cloudflare automatically mitigates DDoS attacks
- Rate limiting at the edge
- Geographic restrictions if needed

### 4. Web Application Firewall (WAF)
- OWASP rule sets available
- Custom firewall rules
- Bot protection

## Deployment

### 1. Build the Application
```bash
./gradlew clean build -x test
```

### 2. Build Docker Image
```bash
docker-compose build buddy-service
```

### 3. Start Services
```bash
docker-compose up -d
```

### 4. Verify Tunnel Connection
```bash
# Check tunnel status
docker logs focushive-buddy-tunnel

# Test health endpoint through tunnel
curl https://buddy.focushive.app/api/v1/health
```

## Monitoring

### Container Health
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Tunnel Metrics
The cloudflared container exposes metrics on port 2000:
```bash
docker exec focushive-buddy-tunnel curl -s localhost:2000/metrics
```

### Logs
```bash
# Application logs
docker logs focushive-buddy-app

# Tunnel logs
docker logs focushive-buddy-tunnel
```

## Troubleshooting

### Issue: 502 Bad Gateway
**Cause**: Cloudflare can't reach the backend service
**Solution**:
1. Ensure buddy-service container is healthy
2. Check service name matches in docker-compose
3. Verify internal networking is working

### Issue: 404 Not Found
**Cause**: Ingress rules misconfigured
**Solution**: Check tunnel configuration in Cloudflare dashboard

### Issue: SSL Certificate Error
**Cause**: Accessing via IP instead of domain
**Solution**: Always use the configured domain (buddy.focushive.app)

### Testing Internal Connectivity
```bash
# Test from cloudflared to buddy service
docker exec focushive-buddy-tunnel curl http://buddy-service:8087/api/v1/health
```

## Best Practices

1. **Token Security**
   - Never commit tunnel tokens to version control
   - Store in `.env` file and add to `.gitignore`
   - Rotate tokens periodically

2. **Monitoring**
   - Set up health checks for both application and tunnel
   - Monitor tunnel metrics for performance
   - Set up alerts for tunnel disconnections

3. **Scaling**
   - Cloudflared automatically handles multiple connections (4 by default)
   - Load balancing is handled by Cloudflare
   - Can run multiple cloudflared instances for redundancy

4. **Updates**
   - Keep cloudflared image updated
   - Use specific version tags in production
   - Test updates in staging first

## Network Architecture

```
Internet → Cloudflare Edge → Tunnel → Docker Network → Buddy Service
         ↑                           ↑                ↑
    DDoS Protection            No Port Exposure   Internal Only
    WAF Rules                  Encrypted          Container Network
    SSL/TLS                    Zero Trust         Service Mesh
```

## Related Services

All FocusHive services use the same tunnel architecture:

| Service | Public URL | Internal URL | Port |
|---------|------------|--------------|------|
| Frontend | focushive.app | http://frontend:3000 | - |
| Backend | backend.focushive.app | http://focushive-backend:8080 | - |
| Identity | identity.focushive.app | http://identity-service:8081 | - |
| Notification | notification.focushive.app | http://notification-service:8083 | - |
| Buddy | buddy.focushive.app | http://buddy-service:8087 | - |

## Maintenance

### Viewing Active Tunnels
Check the Cloudflare Zero Trust dashboard:
1. Navigate to Access → Tunnels
2. View tunnel status and connections
3. Check ingress rules and traffic

### Updating Tunnel Configuration
1. Update in Cloudflare dashboard or via API
2. Restart cloudflared container to apply changes
3. Verify new configuration in logs

### Rotating Tokens
1. Generate new token in Cloudflare dashboard
2. Update `.env` file
3. Restart cloudflared container

## Conclusion

The Cloudflare tunnel setup provides enterprise-grade security and performance without the complexity of traditional networking. No ports are exposed to the internet, all traffic is encrypted, and the service benefits from Cloudflare's global infrastructure.

For more information:
- [Cloudflare Zero Trust Documentation](https://developers.cloudflare.com/cloudflare-one/)
- [Cloudflared GitHub Repository](https://github.com/cloudflare/cloudflared)