package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for persisting encryption keys.
 * Ensures encryption keys survive service restarts and maintains audit trail.
 */
@Entity
@Table(name = "encryption_keys",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_encryption_key_version", columnNames = "version")
       },
       indexes = {
           @Index(name = "idx_encryption_keys_active", columnList = "active"),
           @Index(name = "idx_encryption_keys_version", columnList = "version"),
           @Index(name = "idx_encryption_keys_created_at", columnList = "created_at"),
           @Index(name = "idx_encryption_keys_expires_at", columnList = "expires_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"keyMaterial"}) // Never log key material
public class EncryptionKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "version", nullable = false, unique = true, length = 100)
    private String version;

    @Column(name = "key_material", nullable = false, columnDefinition = "TEXT")
    private String keyMaterial; // Base64 encoded encrypted key material

    @Column(name = "salt", nullable = false, length = 255)
    private String salt;

    @Column(name = "algorithm", nullable = false, length = 50)
    @Builder.Default
    private String algorithm = "AES-256-GCM";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "rotated_from", length = 100)
    private String rotatedFrom;

    @Column(name = "rotation_reason", length = 255)
    private String rotationReason;

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "system";

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Check if this key is expired.
     *
     * @return true if the key has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this key is valid for use.
     *
     * @return true if the key is active and not expired
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Convert this entity to the domain object.
     *
     * @return EncryptionKey domain object
     */
    public com.focushive.identity.security.encryption.EncryptionKey toDomainObject() {
        // Decode the key material from Base64
        byte[] keyBytes = java.util.Base64.getDecoder().decode(keyMaterial);

        return com.focushive.identity.security.encryption.EncryptionKey.builder()
                .version(version)
                .keyBytes(keyBytes)
                .salt(salt)
                .algorithm(algorithm)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .active(active)
                .build();
    }

    /**
     * Create entity from domain object.
     *
     * @param key domain object
     * @return entity for persistence
     */
    public static EncryptionKeyEntity fromDomainObject(
            com.focushive.identity.security.encryption.EncryptionKey key) {
        // Encode the key bytes to Base64 for storage
        String encodedKeyMaterial = java.util.Base64.getEncoder()
                .encodeToString(key.getKeyBytes());

        return EncryptionKeyEntity.builder()
                .version(key.getVersion())
                .keyMaterial(encodedKeyMaterial)
                .salt(key.getSalt())
                .algorithm(key.getAlgorithm())
                .createdAt(key.getCreatedAt())
                .expiresAt(key.getExpiresAt())
                .active(key.isActive())
                .build();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (createdBy == null) {
            createdBy = "system";
        }
    }
}