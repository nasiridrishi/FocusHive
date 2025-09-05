#!/bin/bash

# Start Backend Service with all necessary configurations
echo "Starting FocusHive Backend Service..."

cd services/focushive-backend

# Export environment variables
export DB_HOST=localhost
export DB_PORT=5434
export DB_NAME=focushive
export DB_USER=focushive_user
export DB_PASSWORD=focushive_pass
export REDIS_HOST=localhost
export REDIS_PORT=6380
export REDIS_PASSWORD=focushive_pass
export JWT_SECRET=mySecretKeyForFocusHiveApplicationThatIsAtLeast256BitsLongForHS256Algorithm
export JWT_EXPIRATION=86400000
export JWT_REFRESH_EXPIRATION=604800000

# Start the application with reduced validation
./gradlew bootRun --args='--spring.jpa.hibernate.ddl-auto=none --spring.flyway.validate-on-migrate=false'