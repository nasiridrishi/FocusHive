# Migration Guide: Individual → Combined Docker Compose

## 🚨 Important: Individual Docker Compose Files Deprecated

The individual `docker-compose.yml` files in each service directory are **deprecated** in favor of the new **combined docker-compose.yml** in `/services/docker-compose.yml`.

## 🔄 Migration Required

### ❌ Old Approach (Don't use anymore)
```bash
# DON'T DO THIS - Old fragmented approach
cd services/identity-service && docker-compose up -d
cd ../notification-service && docker-compose up -d  
cd ../buddy-service && docker-compose up -d
cd ../focushive-backend && docker-compose up -d
```

### ✅ New Approach (Use this)
```bash
# DO THIS - New unified approach
cd services
docker-compose up -d
```

## 🏗️ Architecture Changes

### Container Name Changes
All container names have been cleaned up:

| Old Name | New Name |
|----------|----------|
| `focushive-identity-service-app` | `focushive-identity-app` |
| `focushive-notification-service-app` | `focushive-notification-app` |  
| `focushive_buddy_service_app` | `focushive-buddy-app` |
| `focushive_backend_main` | `focushive-backend-app` |
| `focushive-identity-service-postgres` | `focushive-identity-postgres` |
| `focushive-notification-service-postgres` | `focushive-notification-postgres` |
| etc... | *All cleaned up!* |

### Network Architecture  
- **Before**: Each service had its own isolated network
- **After**: Single shared network (`focushive-shared-network`)
- **Benefit**: No more `UnknownHostException` between services!

### Security Improvements
- **Before**: All databases exposed external ports
- **After**: Databases are internal-only (more secure!)

## 🔧 Required Updates

### 1. Update .env Files
If you have custom `.env` files in service directories, update container references:

```bash
# In services/identity-service/.env
# BEFORE:
NOTIFICATION_SERVICE_URL=http://focushive-notification-service-app:8083

# AFTER:  
NOTIFICATION_SERVICE_URL=http://focushive-notification-app:8083
```

### 2. Update Application Code
If your applications reference container names directly, update them:

```java
// BEFORE:
String notificationUrl = "http://focushive-notification-service-app:8083";

// AFTER:
String notificationUrl = "http://focushive-notification-app:8083";
```

### 3. Update Scripts/Monitoring
Update any deployment or monitoring scripts:

```bash
# BEFORE:
docker exec focushive-identity-service-app curl localhost:8081/health

# AFTER:
docker exec focushive-identity-app curl localhost:8081/health
```

## 📅 Timeline

- **Now**: Combined docker-compose.yml is ready and recommended
- **Next Week**: Individual docker-compose files will show deprecation warnings  
- **Next Month**: Individual docker-compose files may be removed

## 🆘 Need Help?

### Quick Migration Test
```bash
# Stop old services
docker-compose down  # in each service directory

# Start new unified stack
cd services
docker-compose up -d

# Verify all services running
docker-compose ps
```

### Rollback if Needed
```bash
# If something breaks, you can still use individual files:
cd services/identity-service
docker-compose up -d  # Still works for now
```

## ✅ Benefits of New Approach

🔒 **More Secure**: Databases not exposed externally  
🚀 **Faster Deployment**: One command vs. four  
🌐 **Better Networking**: No inter-service communication issues  
🧹 **Cleaner Names**: Shorter, more readable container names  
📊 **Easier Monitoring**: All services in one place  
🐛 **Simpler Debugging**: Single network to troubleshoot  

## 🎯 Action Items

1. ✅ Test the combined docker-compose.yml  
2. ✅ Update any custom .env files
3. ✅ Update application configuration  
4. ✅ Update deployment scripts
5. ✅ Train team on new approach
6. ✅ Remove old docker-compose files (optional)

**The new unified approach is production-ready and recommended for all deployments!** 🎉