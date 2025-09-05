# FocusHive Deployment Guide

Complete guide for deploying FocusHive to production environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Development Deployment](#development-deployment)
4. [Production Deployment](#production-deployment)
5. [SSL/HTTPS Setup](#sslhttps-setup)
6. [Monitoring & Logging](#monitoring--logging)
7. [Backup & Recovery](#backup--recovery)
8. [CI/CD Pipeline](#cicd-pipeline)
9. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

- **OS**: Ubuntu 20.04+ / Debian 11+ / CentOS 8+
- **CPU**: 4+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 50GB minimum
- **Network**: Public IP with ports 80, 443 open

### Software Requirements

- Docker 24.0+
- Docker Compose 2.20+
- Git
- Node.js 20+ (for local development)
- Java 21 (for backend development)

### Installation

```bash
# Install Docker
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker --version
docker-compose --version
```

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/focushive.git
cd focushive
```

### 2. Configure Environment

```bash
# Copy environment template
cp .env.docker.example .env

# Edit configuration
nano .env
```

### 3. Start Services

```bash
# Development mode
./docker/scripts/start.sh dev

# Production mode
./docker/scripts/start.sh prod
```

### 4. Access Application

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Identity Service: http://localhost:8081/auth

## Development Deployment

### Using Docker Compose

```bash
# Start all services with development tools
docker-compose --profile dev up -d

# View logs
docker-compose logs -f

# Stop services
./docker/scripts/stop.sh
```

### Development Tools

When running with `--profile dev`:

- pgAdmin: http://localhost:5050
- Redis Commander: http://localhost:8082
- Grafana: http://localhost:3001
- Prometheus: http://localhost:9090

### Local Development

For frontend development with hot reload:

```bash
cd frontend
npm install
npm run dev
```

For backend development:

```bash
cd services/focushive-backend
./gradlew bootRun
```

## Production Deployment

### 1. Server Setup

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install required packages
sudo apt install -y curl git nginx certbot python3-certbot-nginx

# Configure firewall
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### 2. SSL Certificate Setup

```bash
# Setup Let's Encrypt SSL
sudo ./docker/ssl/setup-letsencrypt.sh yourdomain.com admin@yourdomain.com
```

### 3. Production Deployment

```bash
# Set production environment variables
export NODE_ENV=production
export SPRING_PROFILES_ACTIVE=prod

# Deploy with production configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale backend=3
```

### 4. Health Checks

```bash
# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:3000/health
```

## SSL/HTTPS Setup

### Let's Encrypt (Production)

```bash
# Automated setup
./docker/ssl/setup-letsencrypt.sh focushive.com admin@focushive.com

# Manual setup
docker run -it --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -p 80:80 \
  certbot/certbot certonly --standalone \
  -d focushive.com -d www.focushive.com
```

### Self-Signed (Development)

```bash
# Generate self-signed certificate
./docker/ssl/generate-self-signed.sh localhost

# Trust certificate on your system
cd docker/nginx/ssl
./trust-certificate.sh
```

### SSL Configuration

Update `.env` file:

```env
SSL_CERT_PATH=/etc/letsencrypt/live/focushive.com/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/focushive.com/privkey.pem
PRODUCTION_API_URL=https://api.focushive.com
PRODUCTION_WS_URL=wss://api.focushive.com
```

## Monitoring & Logging

### Start Monitoring Stack

```bash
# Deploy monitoring services
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Access dashboards
open http://localhost:3001  # Grafana (admin/admin123)
open http://localhost:9090  # Prometheus
open http://localhost:5601  # Kibana
```

### Configure Alerts

1. Access Grafana: http://localhost:3001
2. Import dashboards from `docker/monitoring/grafana/dashboards/`
3. Configure notification channels (email, Slack, PagerDuty)

### View Logs

```bash
# Container logs
docker-compose logs -f backend

# Aggregated logs in Kibana
open http://localhost:5601

# Application metrics
open http://localhost:3001/d/focushive-overview
```

## Backup & Recovery

### Automated Backups

```bash
# Create backup
./docker/scripts/backup.sh

# Schedule daily backups
crontab -e
# Add: 0 2 * * * /path/to/focushive/docker/scripts/backup.sh
```

### Manual Backup

```bash
# Database backup
docker exec focushive-postgres pg_dump -U focushive focushive > backup.sql

# Redis backup
docker exec focushive-redis redis-cli BGSAVE
docker cp focushive-redis:/data/dump.rdb redis-backup.rdb

# Full application backup
tar -czf focushive-backup-$(date +%Y%m%d).tar.gz \
  --exclude=node_modules \
  --exclude=.git \
  .
```

### Restore from Backup

```bash
# Restore database
./docker/scripts/backup.sh restore backups/focushive_20240101_120000.sql.gz

# Restore Redis
docker cp redis-backup.rdb focushive-redis:/data/dump.rdb
docker restart focushive-redis
```

## CI/CD Pipeline

### GitHub Actions

The project includes automated CI/CD pipelines:

- **CI Pipeline** (`.github/workflows/ci.yml`):
  - Runs on push to main/develop
  - Tests all services
  - Builds Docker images
  - Runs security scans

- **Deploy Pipeline** (`.github/workflows/deploy.yml`):
  - Deploys to production on main branch
  - Automated rollback on failure

### Manual Deployment

```bash
# Build and push images
docker build -t focushive/backend:latest -f docker/backend/Dockerfile .
docker push focushive/backend:latest

# Deploy on server
ssh production-server
cd /opt/focushive
git pull
docker-compose pull
docker-compose up -d
```

## Troubleshooting

### Common Issues

#### Services Not Starting

```bash
# Check logs
docker-compose logs backend
docker-compose logs identity-service

# Check resource usage
docker stats

# Restart services
docker-compose restart backend
```

#### Database Connection Issues

```bash
# Check PostgreSQL
docker exec focushive-postgres pg_isready

# Reset database
docker-compose down -v
docker-compose up -d
```

#### SSL Certificate Issues

```bash
# Renew certificate
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  certbot/certbot renew

# Check certificate
openssl x509 -in /etc/letsencrypt/live/focushive.com/cert.pem -text -noout
```

#### High Memory Usage

```bash
# Check memory usage
docker stats --no-stream

# Limit container memory
docker-compose down
# Edit docker-compose.yml to add memory limits
docker-compose up -d
```

### Debug Mode

```bash
# Enable debug logging
export LOG_LEVEL=DEBUG
docker-compose up

# Access container shell
docker exec -it focushive-backend /bin/sh

# View real-time logs
docker logs -f focushive-backend --tail 100
```

### Performance Tuning

```bash
# Database optimization
docker exec focushive-postgres psql -U focushive -c "VACUUM ANALYZE;"

# Redis optimization
docker exec focushive-redis redis-cli CONFIG SET maxmemory 512mb
docker exec focushive-redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

## Security Considerations

### Best Practices

1. **Environment Variables**: Never commit `.env` files
2. **Secrets Management**: Use Docker secrets or external vault
3. **Network Security**: Use firewall rules and VPN for admin access
4. **Regular Updates**: Keep Docker images and dependencies updated
5. **Monitoring**: Set up alerts for suspicious activities

### Security Checklist

- [ ] SSL/TLS certificates configured
- [ ] Firewall rules configured
- [ ] Database passwords changed from defaults
- [ ] JWT secrets are strong and unique
- [ ] Admin interfaces restricted to VPN/specific IPs
- [ ] Regular security updates applied
- [ ] Backup encryption enabled
- [ ] Monitoring and alerting configured
- [ ] Rate limiting enabled on APIs
- [ ] CORS properly configured

## Support

### Documentation

- [Frontend Documentation](frontend/README.md)
- [Backend Documentation](services/focushive-backend/README.md)
- [Identity Service Documentation](services/identity-service/README.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)

### Getting Help

- GitHub Issues: https://github.com/yourusername/focushive/issues
- Discord: https://discord.gg/focushive
- Email: support@focushive.com

### Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

See [LICENSE](LICENSE) for licensing information.