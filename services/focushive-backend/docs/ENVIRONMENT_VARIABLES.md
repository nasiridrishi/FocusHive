# Environment Variables Documentation

**FocusHive Backend Service** environment configuration guide.

## Overview

The FocusHive Backend Service uses a comprehensive environment variable strategy with profile-specific configurations and smart defaults. This document describes all environment variables, their purposes, and configuration requirements.

## Configuration Hierarchy

Configuration values are resolved in the following precedence order:

1. **Command-line arguments** (highest precedence)
2. **Environment variables**
3. **Profile-specific configuration files** (application-{profile}.yml)
4. **Base configuration file** (application.yml)
5. **Default values in EnvironmentConfig** (lowest precedence)

## Profile Strategy

### Available Profiles

- **`default`**: Base configuration with H2 database (development/testing)
- **`dev`**: Development profile with enhanced logging and features enabled
- **`staging`**: Pre-production environment with production-like settings
- **`prod`**: Production profile with security-focused configuration
- **`test`**: Test profile for automated testing

### Profile Activation

```bash
# Activate specific profile
java -jar app.jar --spring.profiles.active=dev

# Activate multiple profiles
java -jar app.jar --spring.profiles.active=dev,monitoring

# Using environment variable
export SPRING_PROFILES_ACTIVE=prod
```

## Required Environment Variables

These variables **MUST** be set in all environments:

### Database Configuration

| Variable | Description | Example | Profiles |
|----------|-------------|---------|----------|
| `DATABASE_PASSWORD` | Database password | `secure_db_password` | All except H2 default |
| `DB_PASSWORD` | Production database password | `prod_secure_password` | `prod` |
| `STAGING_DB_PASSWORD` | Staging database password | `staging_password` | `staging` |

### Security Configuration

| Variable | Description | Example | Profiles |
|----------|-------------|---------|----------|
| `JWT_SECRET` | JWT signing secret (32+ chars) | `my_very_secure_jwt_secret_key_32_chars` | All |
| `REDIS_PASSWORD` | Redis authentication password | `redis_secure_password` | All |
| `STAGING_JWT_SECRET` | Staging JWT secret | `staging_jwt_secret_key` | `staging` |
| `STAGING_REDIS_PASSWORD` | Staging Redis password | `staging_redis_password` | `staging` |

## Optional Environment Variables

These variables have sensible defaults but can be overridden:

### Database Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `DATABASE_URL` | H2 in-memory | Database JDBC URL | `jdbc:postgresql://localhost:5432/focushive` |
| `DATABASE_USERNAME` | `sa` | Database username | `focushive_user` |
| `DATABASE_DRIVER` | `org.h2.Driver` | JDBC driver class | `org.postgresql.Driver` |
| `DB_HOST` | `localhost` | Database host (production) | `prod-db.example.com` |
| `DB_PORT` | `5432` | Database port (production) | `5432` |
| `DB_NAME` | `focushive` | Database name (production) | `focushive_prod` |
| `DB_USERNAME` | `focushive_user` | Database username (production) | `prod_user` |

### Staging Database Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `STAGING_DB_HOST` | `localhost` | Staging database host | `staging-db.example.com` |
| `STAGING_DB_PORT` | `5432` | Staging database port | `5432` |
| `STAGING_DB_NAME` | `focushive_staging` | Staging database name | `focushive_staging` |
| `STAGING_DB_USERNAME` | `focushive_staging_user` | Staging database username | `staging_user` |

### Redis Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `REDIS_HOST` | `localhost` | Redis server host | `prod-redis.example.com` |
| `REDIS_PORT` | `6379` | Redis server port | `6379` |
| `STAGING_REDIS_HOST` | `localhost` | Staging Redis host | `staging-redis.example.com` |
| `STAGING_REDIS_PORT` | `6379` | Staging Redis port | `6379` |

### JWT Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `JWT_EXPIRATION` | `86400000` | Access token expiration (ms) | `3600000` (1 hour) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token expiration (ms) | `86400000` (1 day) |
| `STAGING_JWT_EXPIRATION` | `3600000` | Staging JWT expiration (ms) | `1800000` (30 min) |
| `STAGING_JWT_REFRESH_EXPIRATION` | `86400000` | Staging refresh expiration (ms) | `43200000` (12 hours) |

### Server Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `SERVER_PORT` | `8080` | Application server port | `8080` |
| `STAGING_SERVER_PORT` | `8080` | Staging server port | `8080` |

### External Services

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `IDENTITY_SERVICE_URL` | `http://localhost:8081` | Identity service URL | `https://identity.example.com` |
| `IDENTITY_SERVICE_CONNECT_TIMEOUT` | `5000` | Connection timeout (ms) | `10000` |
| `IDENTITY_SERVICE_READ_TIMEOUT` | `10000` | Read timeout (ms) | `15000` |
| `IDENTITY_SERVICE_API_KEY` | (empty) | Optional API key | `api_key_123` |
| `STAGING_IDENTITY_SERVICE_URL` | `http://localhost:8081` | Staging identity service URL | `https://staging-identity.example.com` |

### Logging Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `LOG_LEVEL` | `INFO` | Application log level | `DEBUG` |
| `FEIGN_LOG_LEVEL` | `DEBUG` | Feign client log level | `NONE` |
| `STAGING_LOG_LEVEL` | `INFO` | Staging log level | `DEBUG` |

### Rate Limiting

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `RATE_LIMIT_ENABLED` | `false` | Enable rate limiting | `true` |
| `RATE_LIMIT_USE_REDIS` | `false` | Use Redis for rate limiting | `true` |
| `RATE_LIMIT_PUBLIC` | `100` | Public API requests/hour | `60` |
| `RATE_LIMIT_AUTHENTICATED` | `1000` | Authenticated requests/hour | `500` |
| `RATE_LIMIT_ADMIN` | `10000` | Admin requests/hour | `5000` |
| `RATE_LIMIT_WEBSOCKET` | `60` | WebSocket connections/minute | `30` |
| `RATE_LIMIT_BURST` | `20` | Burst capacity | `10` |
| `RATE_LIMIT_WHITELIST` | (empty) | Whitelisted IPs | `192.168.1.0/24` |

### Monitoring & Observability

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | Trace sampling rate | `0.1` |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | Zipkin tracing endpoint | `https://zipkin.example.com/api/v2/spans` |
| `STAGING_TRACING_SAMPLING` | `1.0` | Staging trace sampling | `1.0` |
| `STAGING_ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | Staging Zipkin endpoint | `https://staging-zipkin.example.com/api/v2/spans` |

### Feature Flags

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `STAGING_FEATURE_FORUM` | `true` | Enable forum in staging | `false` |
| `STAGING_FEATURE_BUDDY` | `true` | Enable buddy system in staging | `false` |
| `STAGING_FEATURE_ANALYTICS` | `true` | Enable analytics in staging | `false` |
| `STAGING_FEATURE_AUTH` | `true` | Enable authentication in staging | `true` |
| `STAGING_FEATURE_AUTH_CONTROLLER` | `true` | Enable auth controller in staging | `true` |
| `STAGING_FEATURE_REDIS` | `true` | Enable Redis in staging | `true` |
| `STAGING_FEATURE_HEALTH` | `true` | Enable health checks in staging | `true` |

## Environment-Specific Configuration

### Development Environment

```bash
# Required
DATABASE_PASSWORD=dev_password
REDIS_PASSWORD=dev_redis_password
JWT_SECRET=development_jwt_secret_key_32_characters_minimum

# Optional - enhanced logging
LOG_LEVEL=DEBUG
FEIGN_LOG_LEVEL=DEBUG

# Features enabled by default in dev profile
# - Forum: enabled
# - Buddy system: enabled
# - Analytics: enabled
# - Authentication: enabled
# - Health checks: enabled
```

### Staging Environment

```bash
# Required
STAGING_DB_PASSWORD=staging_secure_password
STAGING_REDIS_PASSWORD=staging_redis_password
STAGING_JWT_SECRET=staging_jwt_secret_key_32_characters_minimum

# Optional staging-specific
STAGING_DB_HOST=staging-db.example.com
STAGING_DB_NAME=focushive_staging
STAGING_REDIS_HOST=staging-redis.example.com
STAGING_IDENTITY_SERVICE_URL=https://staging-identity.example.com

# Shorter JWT expiration for testing
STAGING_JWT_EXPIRATION=3600000  # 1 hour
STAGING_JWT_REFRESH_EXPIRATION=86400000  # 1 day

# Full tracing in staging
STAGING_TRACING_SAMPLING=1.0
```

### Production Environment

```bash
# Required
DB_PASSWORD=production_secure_password
REDIS_PASSWORD=production_redis_password
JWT_SECRET=production_jwt_secret_key_32_characters_minimum_length

# Production database
DB_HOST=prod-db.example.com
DB_NAME=focushive_prod
DB_USERNAME=prod_user

# Production Redis
REDIS_HOST=prod-redis.example.com

# External services
IDENTITY_SERVICE_URL=https://identity.example.com

# Security settings
JWT_EXPIRATION=86400000  # 1 day
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Rate limiting enabled
RATE_LIMIT_ENABLED=true
RATE_LIMIT_USE_REDIS=true
RATE_LIMIT_PUBLIC=60
RATE_LIMIT_AUTHENTICATED=600

# Minimal logging in production
LOG_LEVEL=INFO
FEIGN_LOG_LEVEL=NONE

# Reduced tracing sampling
TRACING_SAMPLING_PROBABILITY=0.1
```

## Validation Rules

The `EnvironmentConfig` class validates environment variables at startup:

### Required Variable Validation

- **JWT_SECRET**: Must be at least 32 characters
- **DATABASE_PASSWORD**: Required in all profiles except H2 default
- **REDIS_PASSWORD**: Required for Redis functionality

### Format Validation

- **DATABASE_URL**: Must be valid JDBC URL (PostgreSQL or H2)
- **IDENTITY_SERVICE_URL**: Must be valid HTTP/HTTPS URL
- **SERVER_PORT**: Must be >= 1024
- **JWT_EXPIRATION**: Must be >= 5 minutes (300000ms)
- **JWT_REFRESH_EXPIRATION**: Must be >= 1 day (86400000ms)

### Consistency Validation

- **Database URL and Driver**: Must match (PostgreSQL URL with PostgreSQL driver)
- **Rate Limiting**: Warns if Redis rate limiting enabled but Redis disabled
- **Authentication**: Warns if auth controller enabled but authentication disabled

## Secrets Management

### Development

Use `.env` files or IDE configuration for local development:

```bash
# .env.local (never commit)
DATABASE_PASSWORD=dev_password
REDIS_PASSWORD=dev_redis_password
JWT_SECRET=development_jwt_secret_key_32_characters_minimum
```

### Staging/Production

Use secure secret management:

- **Kubernetes**: Use Secrets and ConfigMaps
- **Docker**: Use Docker secrets
- **Cloud Providers**: Use managed secret services (AWS Secrets Manager, Azure Key Vault)
- **HashiCorp Vault**: For enterprise secret management

### Security Best Practices

1. **Never commit secrets** to version control
2. **Rotate secrets regularly** (especially JWT secrets)
3. **Use strong passwords** (minimum 32 characters for JWT secrets)
4. **Limit secret access** to authorized systems/personnel
5. **Monitor secret usage** and access patterns
6. **Use different secrets** for each environment

## Environment Variable Testing

The application includes comprehensive tests for environment configuration:

- **EnvironmentConfigTest**: Basic configuration loading
- **EnvironmentConfigDefaultTest**: Default values testing
- **EnvironmentConfigValidationTest**: Validation logic testing
- **EnvironmentConfigH2Test**: H2 database configuration
- **EnvironmentConfigPostgreSQLTest**: PostgreSQL configuration
- **EnvironmentConfigStagingTest**: Staging profile testing
- **EnvironmentConfigProfileTest**: Configuration hierarchy testing

## Troubleshooting

### Common Issues

1. **Application won't start**:
   - Check that all required environment variables are set
   - Verify JWT_SECRET is at least 32 characters
   - Ensure DATABASE_PASSWORD is provided

2. **Database connection fails**:
   - Verify DATABASE_URL format is correct
   - Check DATABASE_DRIVER matches the database type
   - Confirm database credentials are valid

3. **Redis connection fails**:
   - Check REDIS_HOST and REDIS_PORT values
   - Verify REDIS_PASSWORD is correct
   - Ensure Redis server is running

4. **Profile not loading**:
   - Check SPRING_PROFILES_ACTIVE environment variable
   - Verify profile-specific configuration files exist
   - Review application startup logs for profile activation

### Debug Configuration

Enable configuration debugging:

```bash
# Show all configuration properties
java -jar app.jar --debug --logging.level.org.springframework.boot.context.config=DEBUG

# Show environment variable resolution
java -jar app.jar --logging.level.com.focushive.backend.config=DEBUG
```

## Example Configuration Files

### Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  focushive-backend:
    image: focushive/backend:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://db:5432/focushive
      DATABASE_USERNAME: focushive_user
      DATABASE_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      IDENTITY_SERVICE_URL: http://identity-service:8081
    depends_on:
      - db
      - redis
```

### Kubernetes

```yaml
# k8s-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: focushive-backend-config
data:
  SPRING_PROFILES_ACTIVE: prod
  DATABASE_URL: jdbc:postgresql://postgres-service:5432/focushive
  DATABASE_USERNAME: focushive_user
  REDIS_HOST: redis-service
  IDENTITY_SERVICE_URL: http://identity-service:8081
  RATE_LIMIT_ENABLED: "true"
  RATE_LIMIT_USE_REDIS: "true"

---
# k8s-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: focushive-backend-secrets
type: Opaque
data:
  DATABASE_PASSWORD: <base64-encoded-password>
  REDIS_PASSWORD: <base64-encoded-password>
  JWT_SECRET: <base64-encoded-jwt-secret>
```

## Version History

| Version | Changes |
|---------|---------|
| 1.0.0 | Initial environment configuration |
| 1.1.0 | Added staging profile support |
| 1.2.0 | Enhanced validation and documentation |

---

**Last Updated**: December 2024
**Maintained By**: FocusHive Backend Team