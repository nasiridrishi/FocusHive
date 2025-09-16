# Quick Start Configuration Guide

**FocusHive Backend Service** - Get up and running in 5 minutes.

## 🚀 Quick Setup

### 1. Choose Your Environment

```bash
# Development (default)
export SPRING_PROFILES_ACTIVE=dev

# Staging
export SPRING_PROFILES_ACTIVE=staging

# Production
export SPRING_PROFILES_ACTIVE=prod
```

### 2. Set Required Environment Variables

#### For Development (H2 Database)
```bash
export DATABASE_PASSWORD=dev_password
export REDIS_PASSWORD=dev_redis_password
export JWT_SECRET=development_jwt_secret_key_32_characters_minimum_length
```

#### For Staging (PostgreSQL)
```bash
export STAGING_DB_PASSWORD=staging_password
export STAGING_REDIS_PASSWORD=staging_redis_password
export STAGING_JWT_SECRET=staging_jwt_secret_32_characters_minimum_length
```

#### For Production (PostgreSQL)
```bash
export DB_PASSWORD=production_secure_password
export REDIS_PASSWORD=production_redis_password
export JWT_SECRET=production_jwt_secret_key_32_characters_minimum_length
```

### 3. Start the Application

```bash
# Using Gradle
./gradlew bootRun

# Using Java directly
java -jar build/libs/focushive-backend-*.jar
```

## 🔧 Configuration Profiles

| Profile | Database | Features | Logging | Purpose |
|---------|----------|----------|---------|---------|
| **dev** | H2 (in-memory) | All enabled | DEBUG | Local development |
| **staging** | PostgreSQL | All enabled | INFO | Pre-production testing |
| **prod** | PostgreSQL | Minimal | WARN | Production deployment |
| **test** | H2 (in-memory) | Minimal | WARN | Automated testing |

## ⚡ Environment Templates

### Development (.env.local)
```bash
# Required
DATABASE_PASSWORD=dev_password
REDIS_PASSWORD=dev_redis_password
JWT_SECRET=development_jwt_secret_key_32_characters_minimum_length

# Optional
LOG_LEVEL=DEBUG
SERVER_PORT=8080
```

### Staging (.env.staging)
```bash
# Required
STAGING_DB_PASSWORD=staging_secure_password
STAGING_REDIS_PASSWORD=staging_redis_password
STAGING_JWT_SECRET=staging_jwt_secret_32_characters_minimum_length

# Database
STAGING_DB_HOST=staging-db.example.com
STAGING_DB_NAME=focushive_staging

# Services
STAGING_IDENTITY_SERVICE_URL=https://staging-identity.example.com
```

### Production (.env.prod)
```bash
# Required
DB_PASSWORD=production_secure_password
REDIS_PASSWORD=production_redis_password
JWT_SECRET=production_jwt_secret_32_characters_minimum_length

# Database
DB_HOST=prod-db.example.com
DB_NAME=focushive_prod
DB_USERNAME=prod_user

# Services
IDENTITY_SERVICE_URL=https://identity.example.com

# Security
RATE_LIMIT_ENABLED=true
RATE_LIMIT_USE_REDIS=true
```

## 🔍 Validation Quick Check

The application validates configuration at startup. Look for these log messages:

```
✅ Environment validation successful - FocusHive Backend ready to start
📋 FocusHive Backend Configuration Summary:
  Application Version: 1.0.0
  Server Port: 8080
  Database: jdbc:postgresql://localhost:5432/focushive
  ...
```

## ❌ Common Errors

### JWT Secret Too Short
```
❌ Environment validation failed: JWT_SECRET must be at least 32 characters long
```
**Fix**: Ensure JWT_SECRET has 32+ characters

### Missing Password
```
❌ Environment validation failed: DATABASE_PASSWORD environment variable is required
```
**Fix**: Set DATABASE_PASSWORD for your profile

### Database URL Mismatch
```
❌ Environment validation failed: DATABASE_URL must match DATABASE_DRIVER
```
**Fix**: Use PostgreSQL URL with PostgreSQL driver

## 📚 More Information

- **Complete Guide**: [ENVIRONMENT_VARIABLES.md](./ENVIRONMENT_VARIABLES.md)
- **Service Documentation**: [../CLAUDE.md](../CLAUDE.md)
- **API Reference**: [../../API_REFERENCE.md](../../API_REFERENCE.md)

---

**Need Help?** Check the validation logs or refer to the complete environment variables documentation.