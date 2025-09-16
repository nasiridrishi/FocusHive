# Demo Accounts for Testing

## Primary Demo Account

**Username:** demo_user  
**Email:** demo@focushive.com  
**Password:** Demo123!  

## Account Details

- **Display Name:** Demo User
- **Role:** USER
- **Email Verified:** Yes
- **Account Status:** Active and enabled
- **Timezone:** UTC
- **Locale:** en-US

## Testing Instructions

1. Access the frontend at: http://192.168.1.37:5173/
2. Use the credentials above to log in
3. The password is BCrypt-hashed in the database for security

## Notes

- The backend needs to be running for authentication to work
- Currently the backend has Flyway migration issues that need to be resolved
- The demo account was created directly in the database with proper BCrypt password hashing
- Once the backend is fixed, you can use the `/api/v1/auth/login` endpoint to authenticate

## Backend Status

⚠️ **Note:** The backend is currently not running due to Flyway migration checksum mismatches. To fix this:

1. Clean and rebuild the backend:
```bash
cd backend
./gradlew clean build
```

2. Skip Flyway validation temporarily by adding to application.yml:
```yaml
spring:
  flyway:
    validate-on-migrate: false
```

3. Or repair the migrations:
```bash
./gradlew flywayRepair
```

## Frontend Access

✅ The frontend is running and accessible at: http://192.168.1.37:5173/

You can explore the UI but login functionality requires the backend to be operational.