# Identity Service - Production Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the FocusHive Identity Service to a production environment. The Identity Service is an OAuth2 Authorization Server that provides authentication and authorization capabilities for the FocusHive ecosystem.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Security Setup](#security-setup)
5. [Database Setup](#database-setup)
6. [Deployment Process](#deployment-process)
7. [Monitoring and Alerting](#monitoring-and-alerting)
8. [Backup and Recovery](#backup-and-recovery)
9. [Troubleshooting](#troubleshooting)
10. [Maintenance](#maintenance)

## Prerequisites

### Infrastructure Requirements

- **Kubernetes Cluster**: v1.24+
- **PostgreSQL**: v14+ with SSL support
- **Redis**: v6+ (cluster mode recommended)
- **Load Balancer**: HTTPS/TLS termination
- **DNS**: Domain with SSL certificate
- **Monitoring**: Prometheus + Grafana stack

### Required Tools

- `kubectl` v1.24+
- `docker` v20.10+
- `helm` v3.8+ (optional)
- `curl` for health checks
- `jq` for JSON processing

### Minimum Resource Requirements

| Component | CPU | Memory | Storage | Replicas |
|-----------|-----|--------|---------|----------|
| Identity Service | 250m | 512Mi | 5Gi | 3 |
| PostgreSQL | 500m | 1Gi | 100Gi | 1 |
| Redis | 250m | 512Mi | 10Gi | 3 |

## Architecture

```
                                   ┌─────────────────┐
                                   │   Load Balancer │
                                   │  (TLS/SSL Term) │
                                   └─────────┬───────┘
                                             │
                                   ┌─────────▼───────┐
                                   │     Ingress     │
                                   │   (nginx/istio) │
                                   └─────────┬───────┘
                                             │
                          ┌──────────────────┼──────────────────┐
                          │                  │                  │
                   ┌──────▼──────┐   ┌──────▼──────┐   ┌──────▼──────┐
                   │  Identity   │   │  Identity   │   │  Identity   │
                   │  Service    │   │  Service    │   │  Service    │
                   │  Pod-1      │   │  Pod-2      │   │  Pod-3      │
                   └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
                          │                  │                  │
                          └──────────────────┼──────────────────┘
                                             │
                              ┌──────────────┼──────────────────┐
                              │              │                  │
                    ┌─────────▼──────┐  ┌────▼────────┐  ┌─────▼─────────┐
                    │  PostgreSQL    │  │   Redis     │  │   Zipkin      │
                    │  (Primary)     │  │  Cluster    │  │ (Tracing)     │
                    └────────────────┘  └─────────────┘  └───────────────┘
```

## Configuration

### Environment Variables

#### Required Variables (Must be set)

```bash
# Database Configuration
DB_HOST=postgresql.focushive.svc.cluster.local
DB_PORT=5432
DB_NAME=identity_db
DB_USER=identity_user
DB_PASSWORD=<secure-password>

# Redis Configuration
REDIS_CLUSTER_NODES=redis-cluster.focushive.svc.cluster.local:6379
REDIS_PASSWORD=<secure-password>

# JWT Configuration
JWT_ISSUER=https://identity.focushive.com
JWT_PRIVATE_KEY_PASSWORD=<secure-password>
JWT_KEY_STORE_PASSWORD=<secure-password>

# OAuth2 Configuration
ISSUER_URI=https://identity.focushive.com

# OAuth2 Clients
FOCUSHIVE_FRONTEND_CLIENT_ID=focushive-frontend
FOCUSHIVE_FRONTEND_CLIENT_SECRET=<secure-client-secret>
FOCUSHIVE_BACKEND_CLIENT_ID=focushive-backend
FOCUSHIVE_BACKEND_CLIENT_SECRET=<secure-client-secret>

# CORS Configuration
CORS_ORIGINS=https://app.focushive.com,https://admin.focushive.com

# Email Configuration
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=<sendgrid-api-key>
EMAIL_FROM=noreply@focushive.com

# Application Configuration
APP_BASE_URL=https://identity.focushive.com
```

#### Optional Variables (With defaults)

```bash
# Performance Configuration
DB_POOL_MAX_SIZE=25
DB_POOL_MIN_IDLE=5
REDIS_POOL_MAX_ACTIVE=20
TOMCAT_MAX_THREADS=200

# Security Configuration
AUTH_RATE_LIMIT_RPM=10
API_RATE_LIMIT_RPM=100
BRUTE_FORCE_MAX_ATTEMPTS=5

# Monitoring Configuration
ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
TRACING_SAMPLING_PROBABILITY=0.1
```

### Secrets Management

Create Kubernetes secrets for sensitive data:

```bash
# Database credentials
kubectl create secret generic identity-service-db \
  --from-literal=host=postgresql.focushive.svc.cluster.local \
  --from-literal=username=identity_user \
  --from-literal=password=<secure-password> \
  --from-literal=database=identity_db \
  -n focushive

# Redis credentials
kubectl create secret generic identity-service-redis \
  --from-literal=password=<secure-password> \
  -n focushive

# JWT keystore
kubectl create secret generic identity-service-jwt \
  --from-file=keystore=jwt-keystore.p12 \
  --from-literal=keystore-password=<secure-password> \
  --from-literal=private-key-password=<secure-password> \
  -n focushive

# OAuth2 clients
kubectl create secret generic identity-service-oauth2 \
  --from-literal=frontend-client-id=focushive-frontend \
  --from-literal=frontend-client-secret=<secure-client-secret> \
  --from-literal=backend-client-id=focushive-backend \
  --from-literal=backend-client-secret=<secure-client-secret> \
  -n focushive

# SMTP credentials
kubectl create secret generic identity-service-smtp \
  --from-literal=username=apikey \
  --from-literal=password=<sendgrid-api-key> \
  -n focushive
```

## Security Setup

### SSL/TLS Configuration

1. **Generate SSL certificates** for the domain:
   ```bash
   # Using Let's Encrypt with cert-manager
   kubectl apply -f - <<EOF
   apiVersion: cert-manager.io/v1
   kind: ClusterIssuer
   metadata:
     name: letsencrypt-prod
   spec:
     acme:
       server: https://acme-v02.api.letsencrypt.org/directory
       email: admin@focushive.com
       privateKeySecretRef:
         name: letsencrypt-prod
       solvers:
       - http01:
           ingress:
             class: nginx
   EOF
   ```

2. **Generate JWT keys**:
   ```bash
   # Generate RSA key pair for JWT signing
   openssl genpkey -algorithm RSA -out jwt-private-key.pem -pkcs8 -aes256
   openssl rsa -pubout -in jwt-private-key.pem -out jwt-public-key.pem
   
   # Create PKCS12 keystore
   openssl pkcs12 -export -in jwt-private-key.pem -out jwt-keystore.p12 -name jwt-signing-key
   ```

### Network Security

1. **Network Policies**:
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: NetworkPolicy
   metadata:
     name: identity-service-netpol
     namespace: focushive
   spec:
     podSelector:
       matchLabels:
         app: identity-service
     policyTypes:
     - Ingress
     - Egress
     ingress:
     - from:
       - podSelector:
           matchLabels:
             app: nginx-ingress
       ports:
       - protocol: TCP
         port: 8081
     egress:
     - to:
       - podSelector:
           matchLabels:
             app: postgresql
       ports:
       - protocol: TCP
         port: 5432
     - to:
       - podSelector:
           matchLabels:
             app: redis
       ports:
       - protocol: TCP
         port: 6379
   ```

2. **Pod Security Policy**:
   ```yaml
   apiVersion: policy/v1beta1
   kind: PodSecurityPolicy
   metadata:
     name: identity-service-psp
   spec:
     privileged: false
     runAsUser:
       rule: MustRunAsNonRoot
     seLinux:
       rule: RunAsAny
     fsGroup:
       rule: RunAsAny
     volumes:
     - 'configMap'
     - 'emptyDir'
     - 'projected'
     - 'secret'
     - 'downwardAPI'
     - 'persistentVolumeClaim'
   ```

## Database Setup

### PostgreSQL Configuration

1. **Create database and user**:
   ```sql
   CREATE USER identity_user WITH PASSWORD '<secure-password>';
   CREATE DATABASE identity_db OWNER identity_user;
   GRANT ALL PRIVILEGES ON DATABASE identity_db TO identity_user;
   
   -- Enable required extensions
   \c identity_db
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   CREATE EXTENSION IF NOT EXISTS "pgcrypto";
   ```

2. **Configure SSL** (in postgresql.conf):
   ```
   ssl = on
   ssl_cert_file = '/etc/ssl/certs/server.crt'
   ssl_key_file = '/etc/ssl/private/server.key'
   ssl_ca_file = '/etc/ssl/certs/ca.crt'
   ssl_require = on
   ```

3. **Optimize for production** (in postgresql.conf):
   ```
   # Memory settings
   shared_buffers = 256MB
   effective_cache_size = 1GB
   work_mem = 4MB
   maintenance_work_mem = 64MB
   
   # Connection settings
   max_connections = 100
   
   # Logging settings
   log_statement = 'mod'
   log_min_duration_statement = 1000
   ```

### Redis Configuration

1. **Redis cluster setup**:
   ```bash
   # Create Redis cluster configuration
   kubectl apply -f - <<EOF
   apiVersion: apps/v1
   kind: StatefulSet
   metadata:
     name: redis-cluster
     namespace: focushive
   spec:
     serviceName: redis-cluster
     replicas: 6
     selector:
       matchLabels:
         app: redis-cluster
     template:
       spec:
         containers:
         - name: redis
           image: redis:6.2-alpine
           args:
           - redis-server
           - /etc/redis/redis.conf
           - --cluster-enabled
           - yes
           - --cluster-config-file
           - nodes.conf
           - --cluster-node-timeout
           - "5000"
           - --appendonly
           - yes
           - --requirepass
           - $(REDIS_PASSWORD)
           env:
           - name: REDIS_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: redis-secret
                 key: password
   EOF
   ```

## Deployment Process

### Step 1: Pre-deployment Checks

```bash
# Run pre-deployment validation
./deploy-prod.sh --pre-deploy --tag v1.0.0

# Verify cluster connectivity
kubectl cluster-info

# Check namespace
kubectl get namespace focushive || kubectl create namespace focushive

# Verify secrets
kubectl get secrets -n focushive
```

### Step 2: Build and Push Image

```bash
# Build production image
./build-prod.sh --tag v1.0.0

# Verify image
docker images focushive/identity-service:v1.0.0
```

### Step 3: Deploy to Production

```bash
# Dry run deployment
./deploy-prod.sh --dry-run --tag v1.0.0

# Deploy to production
./deploy-prod.sh --tag v1.0.0

# Monitor deployment
kubectl rollout status deployment/identity-service -n focushive --timeout=600s
```

### Step 4: Post-deployment Validation

```bash
# Health check
./deploy-prod.sh --health-check

# Verify all pods are running
kubectl get pods -n focushive -l app=identity-service

# Check logs
kubectl logs -f deployment/identity-service -n focushive

# Test OAuth2 endpoints
curl -k https://identity.focushive.com/.well-known/openid-configuration
curl -k https://identity.focushive.com/actuator/health
```

## Monitoring and Alerting

### Prometheus Metrics

The service exposes the following key metrics:

- `http_server_requests_seconds_*`: HTTP request metrics
- `jvm_memory_*`: JVM memory usage
- `hikaricp_connections_*`: Database connection pool metrics
- `authentication_attempts_total`: Authentication attempt counters
- `oauth2_token_*`: OAuth2 token metrics
- `rate_limit_exceeded_total`: Rate limiting metrics

### Grafana Dashboards

Import the provided Grafana dashboard from `k8s/servicemonitor.yml` to visualize:

- Request rate and response times
- Error rates and status codes
- JVM and system metrics
- Database connection health
- Authentication and OAuth2 metrics

### Alerts

Key alerts configured in `k8s/servicemonitor.yml`:

1. **Critical Alerts**:
   - Service down (>1 minute)
   - High error rate (>5% for 5 minutes)
   - Database connection failures

2. **Warning Alerts**:
   - High response time (>1s 95th percentile)
   - High memory usage (>85%)
   - High CPU usage (>80%)
   - Authentication failures (>10/second)

### Log Aggregation

Configure log forwarding to your centralized logging system:

```yaml
apiVersion: logging.coreos.com/v1
kind: ClusterLogForwarder
metadata:
  name: identity-service-logs
spec:
  outputs:
  - name: elasticsearch
    type: elasticsearch
    url: https://elasticsearch.logging.svc.cluster.local:9200
  pipelines:
  - name: identity-service-logs
    inputRefs:
    - application
    filterRefs:
    - identity-service-filter
    outputRefs:
    - elasticsearch
```

## Backup and Recovery

### Database Backup

```bash
# Daily backup script
#!/bin/bash
BACKUP_DIR="/backups/postgresql/$(date +%Y/%m/%d)"
mkdir -p "$BACKUP_DIR"

pg_dump -h postgresql.focushive.svc.cluster.local \
        -U identity_user \
        -d identity_db \
        -f "$BACKUP_DIR/identity_db_$(date +%H%M%S).sql"

# Compress backup
gzip "$BACKUP_DIR/identity_db_$(date +%H%M%S).sql"

# Clean old backups (keep 30 days)
find /backups/postgresql -type f -mtime +30 -delete
```

### Configuration Backup

```bash
# Backup Kubernetes configurations
kubectl get all,configmap,secret,ingress,pv,pvc -n focushive -o yaml > backup-$(date +%Y%m%d).yaml
```

### Disaster Recovery

1. **Database Recovery**:
   ```bash
   # Restore from backup
   psql -h postgresql.focushive.svc.cluster.local \
        -U identity_user \
        -d identity_db \
        -f backup.sql
   ```

2. **Service Recovery**:
   ```bash
   # Rollback deployment
   ./deploy-prod.sh --rollback
   
   # Or restore from backup
   kubectl apply -f backup-$(date +%Y%m%d).yaml
   ```

## Troubleshooting

### Common Issues

1. **Service Won't Start**:
   ```bash
   # Check pod status
   kubectl describe pod -n focushive -l app=identity-service
   
   # Check logs
   kubectl logs -f deployment/identity-service -n focushive
   
   # Check configuration
   kubectl get configmap identity-service-config -n focushive -o yaml
   ```

2. **Database Connection Issues**:
   ```bash
   # Test database connectivity
   kubectl run -it --rm debug --image=postgres:14 --restart=Never -- \
     psql -h postgresql.focushive.svc.cluster.local -U identity_user -d identity_db
   
   # Check database logs
   kubectl logs -f statefulset/postgresql -n focushive
   ```

3. **High Memory Usage**:
   ```bash
   # Check JVM settings
   kubectl describe pod -n focushive -l app=identity-service | grep -A 10 "Environment:"
   
   # Adjust memory limits
   kubectl patch deployment identity-service -n focushive -p \
     '{"spec":{"template":{"spec":{"containers":[{"name":"identity-service","resources":{"limits":{"memory":"4Gi"}}}]}}}}'
   ```

4. **OAuth2 Issues**:
   ```bash
   # Test OAuth2 endpoints
   curl -v https://identity.focushive.com/.well-known/openid-configuration
   curl -v https://identity.focushive.com/oauth2/authorize?client_id=test&response_type=code&redirect_uri=http://localhost
   
   # Check JWT configuration
   kubectl exec -it deployment/identity-service -n focushive -- \
     ls -la /etc/ssl/private/
   ```

### Performance Tuning

1. **JVM Tuning**:
   ```yaml
   env:
   - name: JAVA_OPTS
     value: >-
       -XX:+UseContainerSupport
       -XX:MaxRAMPercentage=75.0
       -XX:+UseG1GC
       -XX:MaxGCPauseMillis=200
       -XX:G1HeapRegionSize=16m
       -XX:+UseStringDeduplication
   ```

2. **Database Connection Pool**:
   ```yaml
   env:
   - name: DB_POOL_MAX_SIZE
     value: "50"
   - name: DB_POOL_MIN_IDLE
     value: "10"
   - name: DB_CONNECTION_TIMEOUT
     value: "30000"
   ```

3. **Redis Performance**:
   ```yaml
   env:
   - name: REDIS_POOL_MAX_ACTIVE
     value: "50"
   - name: REDIS_POOL_MAX_IDLE
     value: "20"
   - name: REDIS_TIMEOUT
     value: "5000"
   ```

## Maintenance

### Regular Maintenance Tasks

1. **Weekly**:
   - Review error logs and alerts
   - Check disk usage and clean up old logs
   - Verify backup integrity
   - Update security patches

2. **Monthly**:
   - Review performance metrics and optimize if needed
   - Update dependencies and base images
   - Test disaster recovery procedures
   - Review and update documentation

3. **Quarterly**:
   - Security audit and penetration testing
   - Review capacity planning
   - Update SSL certificates if needed
   - Performance testing and optimization

### Scaling

#### Horizontal Scaling

```bash
# Scale up replicas
kubectl scale deployment identity-service --replicas=5 -n focushive

# Update HPA for automatic scaling
kubectl patch hpa identity-service-hpa -n focushive -p \
  '{"spec":{"maxReplicas":10,"targetCPUUtilizationPercentage":70}}'
```

#### Vertical Scaling

```bash
# Increase resource limits
kubectl patch deployment identity-service -n focushive -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"identity-service","resources":{"limits":{"cpu":"2","memory":"4Gi"},"requests":{"cpu":"500m","memory":"1Gi"}}}]}}}}'
```

### Updates and Upgrades

1. **Rolling Updates**:
   ```bash
   # Deploy new version
   ./deploy-prod.sh --tag v1.1.0
   
   # Monitor rollout
   kubectl rollout status deployment/identity-service -n focushive
   ```

2. **Blue-Green Deployment**:
   ```bash
   # Create blue environment
   kubectl create namespace focushive-blue
   ./deploy-prod.sh --namespace focushive-blue --tag v1.1.0
   
   # Switch traffic after validation
   kubectl patch ingress identity-service-ingress -n focushive -p \
     '{"spec":{"rules":[{"host":"identity.focushive.com","http":{"paths":[{"path":"/","pathType":"Prefix","backend":{"service":{"name":"identity-service","namespace":"focushive-blue","port":{"number":8081}}}}]}}]}}'
   ```

### Security Updates

```bash
# Update base image with security patches
./build-prod.sh --tag v1.0.1-security

# Deploy security update
./deploy-prod.sh --tag v1.0.1-security

# Verify security scan
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image focushive/identity-service:v1.0.1-security
```

## Support and Contact

For production issues or questions:

- **Emergency**: Create incident in monitoring system
- **General Support**: Create GitHub issue
- **Security Issues**: security@focushive.com
- **Documentation**: [https://docs.focushive.com](https://docs.focushive.com)

## Changelog

- **v1.0.0**: Initial production release
- **v1.0.1**: Security updates and performance improvements
- **v1.1.0**: Added multi-persona support and enhanced monitoring

---

**Last Updated**: September 10, 2024  
**Version**: 1.0  
**Maintainer**: FocusHive DevOps Team