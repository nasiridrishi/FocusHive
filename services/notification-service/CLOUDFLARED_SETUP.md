# Cloudflared Tunnel Setup Documentation

## Overview

This document describes the Cloudflared tunnel configuration for the FocusHive Notification Service. Cloudflared tunnels provide secure, zero-trust access to services without exposing ports to the public internet.

## Architecture

### Security Benefits
- **No exposed ports**: All services run without exposing ports to the host machine
- **Zero-trust networking**: All connections are authenticated through Cloudflare's network
- **Encrypted communication**: All traffic is encrypted end-to-end
- **DDoS protection**: Built-in protection from Cloudflare's network

### Service Configuration

The notification service is configured with its own Cloudflared tunnel:
- **Public URL**: `https://notification.focushive.app`
- **Internal Service**: `http://focushive-notification-service-app:8083`
- **Tunnel Token**: Configured in `.env` as `CLOUDFLARED_TUNNEL_TOKEN`

## Configuration Files

### 1. Docker Compose Configuration (`docker-compose.yml`)

The Cloudflared service is added as a separate container:

```yaml
focushive-notification-service-cloudflared:
  image: cloudflare/cloudflared:latest
  container_name: focushive-notification-cloudflared
  restart: unless-stopped
  command: tunnel --no-autoupdate run
  environment:
    - TUNNEL_TOKEN=${CLOUDFLARED_TUNNEL_TOKEN}
  networks:
    - focushive-shared-network
  depends_on:
    focushive-notification-service-app:
      condition: service_healthy
```

Key points:
- Uses the official `cloudflare/cloudflared:latest` image
- Runs with `--no-autoupdate` flag to prevent automatic updates
- Depends on the app service being healthy before starting
- Uses the same network for internal communication

### 2. Environment Variables (`.env`)

Required environment variables:

```bash
# Cloudflared Tunnel Configuration
CLOUDFLARED_TUNNEL_TOKEN=<your-tunnel-token>
CLOUDFLARED_ENABLED=true
CLOUDFLARED_PUBLIC_URL=https://notification.focushive.app

# Public Service URLs (via Cloudflared)
IDENTITY_SERVICE_URL=https://identity.focushive.app
BACKEND_SERVICE_URL=https://backend.focushive.app
BUDDY_SERVICE_URL=https://buddy.focushive.app
FRONTEND_URL=https://focushive.app

# JWT Configuration (using public URLs)
JWT_ISSUER_URI=https://identity.focushive.app/identity
JWT_JWK_SET_URI=https://identity.focushive.app/.well-known/jwks.json
```

### 3. Application Configuration (`CloudflaredConfig.java`)

The application dynamically switches between public and internal URLs based on the `cloudflared.enabled` flag:

```java
public String getIdentityServiceUrl() {
    if (cloudflaredEnabled) {
        return identityServiceUrl; // Public URL
    }
    return "http://focushive-identity-service-app:8081"; // Internal URL
}
```

## Security Considerations

### Port Exposure
All external ports have been removed from the docker-compose configuration:
- ❌ No direct access to PostgreSQL (previously 5433)
- ❌ No direct access to Redis (previously 6380)
- ❌ No direct access to RabbitMQ Management (previously 15673)
- ❌ No direct access to Application (previously 8083)
- ✅ All access through Cloudflared tunnels only

### Network Isolation
Services remain on the `focushive-shared-network` for internal communication:
- Services can communicate internally using Docker service names
- External access is only through Cloudflared tunnels
- Network segmentation prevents unauthorized lateral movement

### Token Security
- Tunnel tokens are stored in `.env` file (not committed to version control)
- Tokens are passed as environment variables (not command line arguments)
- Each service has its own unique tunnel token

## Deployment Steps

### 1. Initial Setup
```bash
# Backup existing configuration
cp docker-compose.yml docker-compose.yml.backup

# Create/update .env file with tunnel tokens
echo "CLOUDFLARED_TUNNEL_TOKEN=<your-token>" >> .env
```

### 2. Build and Deploy
```bash
# Build the service
docker-compose build focushive-notification-service-app

# Start all services including Cloudflared
docker-compose up -d

# Verify health
docker-compose ps
docker logs focushive-notification-cloudflared
```

### 3. Verification
```bash
# Check tunnel connectivity (from outside)
curl https://notification.focushive.app/actuator/health

# Check service logs
docker logs focushive-notification-app

# Monitor Cloudflared tunnel
docker logs focushive-notification-cloudflared
```

## Testing

### Integration Test
A dedicated integration test verifies Cloudflared connectivity:

```java
@Test
public void testTunnelConnectivity_whenCloudflaredEnabled_shouldAccessThroughPublicUrl() {
    if (!cloudflaredEnabled) {
        return; // Skip if not enabled
    }

    String healthUrl = publicUrl + "/actuator/health";
    ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("UP");
}
```

Run tests with:
```bash
./gradlew test --tests CloudflaredTunnelIntegrationTest
```

## Troubleshooting

### Common Issues

1. **Tunnel not connecting**
   - Verify token is correct in `.env`
   - Check Cloudflare dashboard for tunnel status
   - Review logs: `docker logs focushive-notification-cloudflared`

2. **Service communication failures**
   - Ensure all services are using public URLs when Cloudflared is enabled
   - Verify JWT issuer URIs are updated to public URLs
   - Check network connectivity between containers

3. **Performance issues**
   - Monitor tunnel metrics in Cloudflare dashboard
   - Check container resource limits
   - Review application logs for timeout errors

### Debug Commands
```bash
# Check tunnel status
docker exec focushive-notification-cloudflared cloudflared tunnel info

# Test internal connectivity
docker exec focushive-notification-app curl http://localhost:8083/actuator/health

# Check environment variables
docker exec focushive-notification-app env | grep CLOUDFLARED

# Monitor tunnel logs
docker logs -f focushive-notification-cloudflared
```

## Rollback Procedure

If issues occur, rollback to the previous configuration:

```bash
# Stop services
docker-compose down

# Restore backup configuration
cp docker-compose.yml.backup docker-compose.yml

# Update .env to disable Cloudflared
sed -i 's/CLOUDFLARED_ENABLED=true/CLOUDFLARED_ENABLED=false/' .env

# Restart with original configuration
docker-compose up -d
```

## Best Practices

1. **Token Management**
   - Never commit tokens to version control
   - Rotate tokens regularly
   - Use different tokens for each environment

2. **Monitoring**
   - Set up alerts for tunnel disconnections
   - Monitor tunnel metrics and performance
   - Log all authentication attempts

3. **Updates**
   - Keep Cloudflared image updated
   - Test updates in staging before production
   - Maintain compatibility with Cloudflare API changes

4. **Network Security**
   - Keep services on isolated Docker networks
   - Use least-privilege principles
   - Implement rate limiting at the tunnel level

## Additional Resources

- [Cloudflare Tunnel Documentation](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [Cloudflared GitHub Repository](https://github.com/cloudflare/cloudflared)
- [Docker Compose Networking](https://docs.docker.com/compose/networking/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)

## Support

For issues or questions:
- Check Cloudflare dashboard for tunnel status
- Review service logs in Docker
- Contact the DevOps team for tunnel token management
- Refer to the main project documentation at `/PROJECT_INDEX.md`