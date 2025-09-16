# FocusHive - Single Docker Compose Deployment

## 🚀 Quick Start (3 Commands)

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Edit environment variables (optional - has defaults)
nano .env

# 3. Deploy everything!
docker-compose up -d
```

## ✨ What Gets Deployed

| Service | Port | Description |
|---------|------|-------------|
| **Identity Service** | 8081 | Authentication & Authorization |
| **Notification Service** | 8083 | Email & Messaging |
| **Buddy Service** | 8087 | Partner Matching & Goals |
| **Backend Service** | 8080 | Core Business Logic |

## 🗄️ Infrastructure (Automatic)

- **4 PostgreSQL databases** (internal only - more secure!)
- **4 Redis instances** (internal only - more secure!)  
- **1 RabbitMQ** (port 5673 + management UI 15673)
- **Shared network** for inter-service communication

### 🔒 Security Benefits
- **Database isolation**: PostgreSQL & Redis only accessible within Docker network
- **No external exposure**: Databases can't be accessed from host machine
- **Service-specific**: Each service has its own dedicated database instance
- **Reduced attack surface**: Only 5 ports exposed instead of 13!

### 🚪 Port Exposure Comparison
```
BEFORE (Old Architecture):
  8081, 8083, 8087, 8080, 5673, 15673  ← Application ports
  5432, 5433, 5434, 5437              ← PostgreSQL ports (EXPOSED)
  6379, 6380, 6381, 26379             ← Redis ports (EXPOSED)
  Total: 13 exposed ports

AFTER (New Architecture):
  8081, 8083, 8087, 8080, 5673        ← Application ports only
  Internal: PostgreSQL & Redis        ← Databases (INTERNAL ONLY)
  Total: 5 exposed ports (62% reduction!)
```

## 🔧 Configuration

### Required Environment Variables (in .env)
```env
# Databases
NOTIFICATION_DB_PASSWORD=your_password
IDENTITY_DB_PASSWORD=your_password
BUDDY_DB_PASSWORD=your_password
POSTGRES_PASSWORD=your_password

# Email (AWS SES)
EMAIL_HOST=email-smtp.us-east-1.amazonaws.com
EMAIL_USERNAME=your_ses_username
EMAIL_PASSWORD=your_ses_password
EMAIL_FROM=noreply@yourdomain.com

# Security
JWT_SECRET=your_256_bit_secret_key
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=secure_admin_password
```

### Optional Variables
All services have sensible defaults. Check `.env.example` for full list.

## 📊 Health Checks & Status

```bash
# Check all services
docker-compose ps

# Check specific service logs
docker-compose logs -f focushive-identity-app
docker-compose logs -f focushive-notification-app

# Health endpoints
curl http://localhost:8081/api/v1/health      # Identity
curl http://localhost:8083/actuator/health    # Notification
curl http://localhost:8087/actuator/health    # Buddy
curl http://localhost:8080/actuator/health    # Backend
```

## 🔗 Inter-Service Communication

Services automatically discover each other using container names:
- `focushive-identity-app:8081`
- `focushive-notification-app:8083`  
- `focushive-buddy-app:8087`
- `focushive-backend-app:8080`

**No manual network setup required!** 🎉

## 🛠️ Development

### Enable Email Testing (MailHog)
```bash
# Add to .env file
COMPOSE_PROFILES=dev

# Restart
docker-compose up -d

# MailHog UI: http://localhost:8025
```

### Update Single Service
```bash
# Rebuild and restart specific service
docker-compose up -d --build focushive-identity-app
```

### Scale Services
```bash
# Scale backend service to 3 instances
docker-compose up -d --scale focushive-backend-app=3
```

## 🧹 Cleanup

```bash
# Stop all services
docker-compose down

# Remove everything including volumes
docker-compose down -v

# Remove images too
docker-compose down -v --rmi all
```

## 🐛 Troubleshooting

### Services Can't Communicate
```bash
# Check network
docker network ls | grep focushive
docker network inspect focushive-shared-network

# Test connectivity
docker exec focushive-identity-app ping focushive-notification-app
```

### Database Issues
```bash
# Check database logs
docker-compose logs -f focushive-notification-postgres

# Connect to databases (internal only - use docker exec)
docker exec -it focushive-notification-postgres psql -U notification_user -d notification_service
docker exec -it focushive-identity-postgres psql -U identity_user -d identity_service
docker exec -it focushive-buddy-postgres psql -U buddy_user -d buddy_service
docker exec -it focushive-backend-postgres psql -U focushive_user -d focushive

# Connect to Redis instances
docker exec -it focushive-notification-redis redis-cli
docker exec -it focushive-identity-redis redis-cli
docker exec -it focushive-buddy-redis redis-cli -a redis_password
docker exec -it focushive-backend-redis redis-cli -a redis_pass
```

### Service Won't Start
```bash
# Check service logs
docker-compose logs focushive-identity-app

# Check dependencies
docker-compose ps
```

## 🎯 Production Deployment

1. **Secure passwords**: Generate strong passwords for all services
2. **External databases**: Consider managed PostgreSQL/Redis for production
3. **SSL/TLS**: Add reverse proxy (nginx) with SSL certificates  
4. **Monitoring**: Enable Prometheus/Grafana profiles
5. **Backup**: Set up database backup strategies

## 📝 Architecture

```
                    🌍 HOST MACHINE (External Access)
┌───────────────────────────────────────────────────────────────────┐
│  :8081        :8083        :8087        :8080        :5673       │
│    │           │           │           │           │         │
└────┼───────────┼───────────┼───────────┼───────────┼─────────┘
     │           │           │           │           │
                    🔒 DOCKER NETWORK (Internal Only)
┌────┴───────────┴───────────┴───────────┴───────────┴─────────┐
│ Identity     Notification   Buddy        Backend      RabbitMQ    │
│ Service      Service        Service      Service      Management  │
│             ┌─────────▶ Email        ┌─────────▶              │
│             │            Provider     │                       │
├─────────────┼──────────────┼─────────────┼───────────────────────┤
│  PostgreSQL    PostgreSQL     PostgreSQL   PostgreSQL                  │
│  (identity)    (notification) (buddy)      (backend)                   │
│      +             +              +            +                       │
│    Redis         Redis          Redis        Redis                     │
│                                                                        │
│           🔒 Internal Only - No External Access 🔒                      │
└────────────────────────────────────────────────────────────────────────┘
```

### 🔒 Security Model:
- **External Access**: Only application services (8081, 8083, 8087, 8080)
- **Internal Only**: All PostgreSQL & Redis instances (no host exposure)
- **Network Isolation**: Databases accessible only within Docker network
- **Service Communication**: Container-to-container using internal DNS

### 🏷️ Clean Container Names:
- **Short & Clear**: `focushive-identity-app` vs `focushive-identity-service-app`
- **Consistent Naming**: All use `focushive-{service}-{component}` pattern
- **No Redundancy**: Removed unnecessary `-service-` parts
- **Easy to Remember**: Shorter names for faster typing and debugging
