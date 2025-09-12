# Field-Level Encryption Implementation Guide

## Overview

This document describes the field-level encryption implementation for protecting Personally Identifiable Information (PII) in the FocusHive Identity Service. The implementation uses AES-256-GCM encryption to ensure GDPR compliance and protect sensitive user data.

## Table of Contents

1. [Architecture](#architecture)
2. [Encrypted Fields](#encrypted-fields)
3. [Technical Implementation](#technical-implementation)
4. [Configuration](#configuration)
5. [Data Migration](#data-migration)
6. [Testing](#testing)
7. [Security Considerations](#security-considerations)
8. [Troubleshooting](#troubleshooting)

## Architecture

### Encryption Flow

```
User Input → JPA Entity → Converter → EncryptionService → Database
    ↓                                        ↓
Plain Text                            Encrypted + IV + Tag
    
Database → Converter → EncryptionService → JPA Entity → User Output
    ↓                          ↓                           ↓
Encrypted                  Decrypted                  Plain Text
```

### Components

1. **EncryptionService** (`com.focushive.identity.security.encryption.EncryptionService`)
   - Core encryption/decryption logic
   - AES-256-GCM implementation
   - Key derivation using PBKDF2
   - Secure IV generation
   - SHA-256 hashing for searchable fields

2. **JPA Converters** (`com.focushive.identity.security.encryption.converters.*`)
   - `EncryptedStringConverter`: Basic string encryption
   - `SearchableEncryptedStringConverter`: Encryption with search capability
   - `EncryptedBooleanMapConverter`: Map<String, Boolean> encryption
   - `EncryptedStringMapConverter`: Map<String, String> encryption
   - `EncryptedJsonConverter`: Generic JSON encryption

3. **Base Entity** (`com.focushive.identity.entity.BaseEncryptedEntity`)
   - Abstract base class for entities with encrypted fields
   - Automatic hash generation for searchable fields
   - Lifecycle hooks for encryption operations

## Encrypted Fields

### User Entity

| Field | Type | Converter | Searchable |
|-------|------|-----------|------------|
| email | String | SearchableEncryptedStringConverter | Yes (via email_hash) |
| firstName | String | EncryptedStringConverter | No |
| lastName | String | EncryptedStringConverter | No |
| twoFactorSecret | String | EncryptedStringConverter | No |
| lastLoginIp | String | EncryptedStringConverter | No |

### Persona Entity

| Field | Type | Converter | Searchable |
|-------|------|-----------|------------|
| displayName | String | EncryptedStringConverter | No |
| bio | String | EncryptedStringConverter | No |
| statusMessage | String | EncryptedStringConverter | No |
| customAttributes | Map<String, String> | EncryptedStringMapConverter | No |
| notificationPreferences | Map<String, Boolean> | EncryptedBooleanMapConverter | No |

## Technical Implementation

### Encryption Algorithm

- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Size**: 256 bits (32 bytes)
- **IV Size**: 96 bits (12 bytes)
- **Tag Size**: 128 bits (16 bytes)
- **Key Derivation**: PBKDF2-HMAC-SHA256 with 65,536 iterations

### Encrypted Data Format

```
Base64(IV || Ciphertext || AuthTag)
```

Where:
- IV: 12 bytes initialization vector
- Ciphertext: Encrypted data
- AuthTag: 16 bytes authentication tag

### Searchable Fields

For fields that need to be searchable (like email), we store both:
1. The encrypted value in the main column
2. A SHA-256 hash in a separate column for searching

Example:
```sql
-- Search by email
SELECT * FROM users WHERE email_hash = ?
```

## Configuration

### Environment Variables

```bash
# Required for production
ENCRYPTION_MASTER_KEY=<base64-encoded-32-byte-key>
ENCRYPTION_SALT=<base64-encoded-16-byte-salt>
```

### Generating Secure Keys

```bash
# Generate master key (32 bytes)
openssl rand -base64 32

# Generate salt (16 bytes)
openssl rand -base64 16
```

### Application Configuration

```yaml
# application.yml
encryption:
  master-key: ${ENCRYPTION_MASTER_KEY}
  salt: ${ENCRYPTION_SALT:FocusHive2024Salt}
```

## Data Migration

### Initial Migration

For existing data, run the migration tool:

```bash
# Set migration flag
export ENCRYPTION_MIGRATION_ENABLED=true

# Run with migration profile
java -jar identity-service.jar --spring.profiles.active=migration

# Remove flag after completion
unset ENCRYPTION_MIGRATION_ENABLED
```

### Database Schema Changes

```sql
-- V10__add_field_level_encryption_support.sql
ALTER TABLE users 
ADD COLUMN email_hash VARCHAR(64),
ALTER COLUMN email TYPE VARCHAR(500),
ALTER COLUMN first_name TYPE VARCHAR(500),
ALTER COLUMN last_name TYPE VARCHAR(500);

CREATE INDEX idx_user_email_hash ON users(email_hash);
```

## Testing

### Unit Tests

```java
@Test
void testEncryptionDecryption() {
    String plaintext = "sensitive data";
    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);
    
    assertThat(decrypted).isEqualTo(plaintext);
    assertThat(encrypted).isNotEqualTo(plaintext);
}
```

### Integration Tests

See `FieldLevelEncryptionTest.java` for comprehensive integration tests covering:
- User PII encryption/decryption
- Persona PII encryption/decryption
- Null value handling
- Email hash searching
- Data integrity with special characters
- Large text encryption

### Manual Testing

1. Create a user with PII data
2. Query the database directly to verify encryption:
   ```sql
   SELECT email, email_hash, first_name FROM users WHERE username = 'testuser';
   ```
3. Verify the application can still authenticate and retrieve user data
4. Test search functionality using email

## Security Considerations

### Key Management

1. **Never commit keys to version control**
2. **Use a secure key management system** (AWS KMS, HashiCorp Vault, etc.)
3. **Backup keys securely** - losing keys means permanent data loss
4. **Rotate keys periodically** with proper migration procedures
5. **Use different keys for different environments** (dev, staging, prod)

### Access Control

1. **Limit database access** to encrypted fields
2. **Audit access** to encryption keys
3. **Use separate database users** for different operations
4. **Implement field-level permissions** in the application layer

### Compliance

- **GDPR Article 32**: Technical measures for data protection
- **GDPR Article 25**: Data protection by design and default
- **PCI DSS**: If handling payment card data
- **HIPAA**: If handling health information

### Performance Considerations

1. **Indexed hash columns** for searchable fields
2. **Batch operations** for bulk encryption/decryption
3. **Connection pooling** for database operations
4. **Caching** of frequently accessed encrypted data (in decrypted form)

## Troubleshooting

### Common Issues

#### 1. "EncryptionService not initialized" Error

**Cause**: Spring dependency injection failure
**Solution**: Ensure `@SpringBootApplication` includes component scanning for encryption package

#### 2. "JWT secret must be at least 32 characters" Error

**Cause**: Missing or weak encryption key
**Solution**: Set `ENCRYPTION_MASTER_KEY` environment variable with proper key

#### 3. Data corruption after key change

**Cause**: Encryption key was changed after data was encrypted
**Solution**: Use data migration tool with old key to decrypt, then re-encrypt with new key

#### 4. Search by email not working

**Cause**: Missing email_hash values
**Solution**: Run migration tool to generate hashes for existing records

### Debugging

Enable debug logging:
```yaml
logging:
  level:
    com.focushive.identity.security.encryption: DEBUG
```

### Recovery Procedures

#### Lost Encryption Key

1. **If you have backups**: Restore from backup with known key
2. **If no backups**: Data is permanently lost (by design for security)

#### Corrupted Encrypted Data

1. Identify affected records:
   ```sql
   SELECT id FROM users WHERE email NOT LIKE 'v1:%';
   ```
2. Restore from backup or mark for re-entry

## Best Practices

1. **Always test encryption in staging** before production deployment
2. **Monitor encryption/decryption performance** in production
3. **Regularly audit** encrypted field access patterns
4. **Document all key rotation** procedures
5. **Train team members** on encryption key handling
6. **Implement alerting** for encryption failures
7. **Maintain encryption inventory** of all encrypted fields
8. **Review and update** encryption algorithms periodically

## Future Enhancements

1. **Key Rotation**: Implement automated key rotation with zero downtime
2. **Encryption Versioning**: Support multiple encryption versions simultaneously
3. **Hardware Security Module (HSM)**: Use HSM for key storage
4. **Field-Level Access Logging**: Detailed audit trail for PII access
5. **Homomorphic Encryption**: Search on encrypted data without decryption
6. **Format-Preserving Encryption**: Maintain data format after encryption

## References

- [NIST SP 800-38D](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf) - GCM Specification
- [OWASP Cryptographic Storage](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [GDPR Encryption Guidelines](https://gdpr.eu/encryption/)
- [Spring Security Crypto](https://docs.spring.io/spring-security/reference/features/integrations/cryptography.html)

## Contact

For questions or issues related to field-level encryption, contact the security team or create an issue in the project repository.