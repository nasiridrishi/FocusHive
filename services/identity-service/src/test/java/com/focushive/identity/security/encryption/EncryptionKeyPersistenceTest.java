package com.focushive.identity.security.encryption;

import com.focushive.identity.repository.EncryptionKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for encryption key persistence across service restarts.
 * Ensures encryption keys remain stable to maintain data integrity.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.encryption.master-key=dGVzdC1tYXN0ZXIta2V5LWZvci10ZXN0aW5nLXB1cnBvc2Vz",
    "app.encryption.auto-rotation-enabled=false"
})
@Transactional
class EncryptionKeyPersistenceTest {

    @Autowired
    private EncryptionKeyService encryptionKeyService;

    @Autowired
    private EncryptionKeyRepository encryptionKeyRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing keys for clean test
        encryptionKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist encryption key on creation")
    void testEncryptionKeyPersistence() {
        // Given: Service is initialized
        encryptionKeyService.initialize();

        // When: Getting the current key
        EncryptionKey firstKey = encryptionKeyService.getCurrentEncryptionKey();
        String firstVersion = firstKey.getVersion();

        // Then: Key should be persisted in database
        assertThat(encryptionKeyRepository.findByVersion(firstVersion))
            .isPresent()
            .hasValueSatisfying(persistedKey -> {
                assertThat(persistedKey.getVersion()).isEqualTo(firstVersion);
                assertThat(persistedKey.isActive()).isTrue();
                assertThat(persistedKey.getAlgorithm()).isEqualTo("AES-256-GCM");
            });
    }

    @Test
    @DisplayName("Should load existing key on service restart")
    void testEncryptionKeyLoadOnRestart() {
        // Given: Service is initialized with a key
        encryptionKeyService.initialize();
        EncryptionKey originalKey = encryptionKeyService.getCurrentEncryptionKey();
        String originalVersion = originalKey.getVersion();

        // When: Simulating service restart
        EncryptionKeyService newService = new EncryptionKeyService(encryptionKeyRepository);
        // Set the master key for the new service instance
        ReflectionTestUtils.setField(newService, "masterKeyBase64",
            "dGVzdC1tYXN0ZXIta2V5LWZvci10ZXN0aW5nLXB1cnBvc2Vz");
        newService.initialize();

        // Then: Should load the same key, not create a new one
        EncryptionKey loadedKey = newService.getCurrentEncryptionKey();
        assertThat(loadedKey.getVersion()).isEqualTo(originalVersion);

        // And: Key material should be the same
        assertThat(loadedKey.getKeyBytes()).isEqualTo(originalKey.getKeyBytes());
    }

    @Test
    @DisplayName("Should create key only if none exists")
    void testCreateKeyOnlyIfNoneExists() {
        // Given: No keys exist in database
        assertThat(encryptionKeyRepository.count()).isZero();

        // When: Service is initialized
        encryptionKeyService.initialize();

        // Then: Exactly one key should be created
        assertThat(encryptionKeyRepository.count()).isEqualTo(1);

        // When: Service is initialized again
        encryptionKeyService.initialize();

        // Then: Still only one key should exist
        assertThat(encryptionKeyRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should maintain key version across multiple operations")
    void testKeyVersionStability() {
        // Given: Service is initialized
        encryptionKeyService.initialize();
        String version1 = encryptionKeyService.getCurrentEncryptionKey().getVersion();

        // When: Getting the key multiple times
        EncryptionKey key1 = encryptionKeyService.getCurrentEncryptionKey();
        EncryptionKey key2 = encryptionKeyService.getCurrentEncryptionKey();
        EncryptionKey key3 = encryptionKeyService.getCurrentEncryptionKey();

        // Then: Key version should remain the same
        assertThat(key1.getVersion()).isEqualTo(version1);
        assertThat(key2.getVersion()).isEqualTo(version1);
        assertThat(key3.getVersion()).isEqualTo(version1);

        // And: Key material should be identical
        assertThat(key2.getKeyBytes()).isEqualTo(key1.getKeyBytes());
        assertThat(key3.getKeyBytes()).isEqualTo(key1.getKeyBytes());
    }

    @Test
    @DisplayName("Should handle key rotation correctly")
    void testKeyRotation() {
        // Given: Service is initialized with a key
        encryptionKeyService.initialize();
        String originalVersion = encryptionKeyService.getCurrentEncryptionKey().getVersion();

        // When: Rotating the key
        String newVersion = encryptionKeyService.rotateKey();

        // Then: New version should be different
        assertThat(newVersion).isNotEqualTo(originalVersion);

        // And: Both keys should be persisted
        assertThat(encryptionKeyRepository.count()).isEqualTo(2);

        // And: New key should be active
        assertThat(encryptionKeyRepository.findByVersion(newVersion))
            .isPresent()
            .hasValueSatisfying(key -> assertThat(key.isActive()).isTrue());

        // And: Old key should still exist for decryption
        assertThat(encryptionKeyRepository.findByVersion(originalVersion))
            .isPresent();
    }
}