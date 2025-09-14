package com.focushive.identity.security.encryption;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PersonaType;
import com.focushive.identity.entity.Role;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for field-level encryption of PII data.
 * Verifies that sensitive data is encrypted in the database and properly decrypted when retrieved.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Field-Level Encryption Integration Tests")
class FieldLevelEncryptionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        userRepository.deleteAll();
        personaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should encrypt and decrypt user PII fields")
    void testUserPIIEncryption() {
        // Given: A user with PII data
        String email = "test@example.com";
        String firstName = "John";
        String lastName = "Doe";
        String twoFactorSecret = "JBSWY3DPEHPK3PXP";
        String lastLoginIp = "192.168.1.100";

        testUser = User.builder()
                .username("testuser")
                .email(email)
                .password("hashedPassword123")
                .firstName(firstName)
                .lastName(lastName)
                .twoFactorSecret(twoFactorSecret)
                .lastLoginIp(lastLoginIp)
                .role(Role.USER)
                .build();

        // When: Save the user
        User savedUser = userRepository.save(testUser);
        userRepository.flush();

        // Then: Verify data is encrypted in database
        Map<String, Object> dbData = jdbcTemplate.queryForMap(
                "SELECT email, email_hash, first_name, last_name, two_factor_secret, last_login_ip FROM users WHERE id = ?",
                savedUser.getId()
        );

        // Email should be encrypted (not plaintext)
        assertThat(dbData.get("email")).isNotEqualTo(email);
        assertThat(dbData.get("email").toString()).contains(":");  // Encrypted format includes version prefix

        // Email hash should be generated for searching
        assertThat(dbData.get("email_hash")).isNotNull();
        assertThat(dbData.get("email_hash")).isEqualTo(encryptionService.hash(email.toLowerCase()));

        // Other PII fields should be encrypted
        assertThat(dbData.get("first_name")).isNotEqualTo(firstName);
        assertThat(dbData.get("last_name")).isNotEqualTo(lastName);
        assertThat(dbData.get("two_factor_secret")).isNotEqualTo(twoFactorSecret);
        assertThat(dbData.get("last_login_ip")).isNotEqualTo(lastLoginIp);

        // When: Retrieve the user
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());

        // Then: Verify data is properly decrypted
        assertTrue(retrievedUser.isPresent());
        User user = retrievedUser.get();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getTwoFactorSecret()).isEqualTo(twoFactorSecret);
        assertThat(user.getLastLoginIp()).isEqualTo(lastLoginIp);
    }

    @Test
    @DisplayName("Should encrypt and decrypt persona PII fields")
    void testPersonaPIIEncryption() {
        // Given: Create a user first
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        // Given: A persona with PII data
        String displayName = "Professional John";
        String bio = "Software developer with 10 years of experience";
        String statusMessage = "Working on exciting projects";
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("company", "Tech Corp");
        customAttributes.put("position", "Senior Developer");
        
        Map<String, Boolean> notificationPrefs = new HashMap<>();
        notificationPrefs.put("emailNotifications", true);
        notificationPrefs.put("pushNotifications", false);

        testPersona = Persona.builder()
                .user(testUser)
                .name("work")
                .type(PersonaType.WORK)
                .displayName(displayName)
                .bio(bio)
                .statusMessage(statusMessage)
                .customAttributes(customAttributes)
                .notificationPreferences(notificationPrefs)
                .build();

        // When: Save the persona
        Persona savedPersona = personaRepository.save(testPersona);
        personaRepository.flush();

        // Then: Verify data is encrypted in database
        Map<String, Object> dbData = jdbcTemplate.queryForMap(
                "SELECT display_name, bio, status_message, notification_preferences FROM personas WHERE id = ?",
                savedPersona.getId()
        );

        // PII fields should be encrypted
        assertThat(dbData.get("display_name")).isNotEqualTo(displayName);
        assertThat(dbData.get("bio")).isNotEqualTo(bio);
        assertThat(dbData.get("status_message")).isNotEqualTo(statusMessage);
        
        // Notification preferences should be encrypted JSON
        String encryptedPrefs = dbData.get("notification_preferences").toString();
        assertThat(encryptedPrefs).doesNotContain("emailNotifications");
        assertThat(encryptedPrefs).doesNotContain("pushNotifications");

        // When: Retrieve the persona
        Optional<Persona> retrievedPersona = personaRepository.findById(savedPersona.getId());

        // Then: Verify data is properly decrypted
        assertTrue(retrievedPersona.isPresent());
        Persona persona = retrievedPersona.get();
        assertThat(persona.getDisplayName()).isEqualTo(displayName);
        assertThat(persona.getBio()).isEqualTo(bio);
        assertThat(persona.getStatusMessage()).isEqualTo(statusMessage);
        assertThat(persona.getNotificationPreferences()).isEqualTo(notificationPrefs);
    }

    @Test
    @DisplayName("Should handle null values in encrypted fields")
    void testNullValueHandling() {
        // Given: A user with null PII fields
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .twoFactorSecret(null)  // Null value
                .lastLoginIp(null)  // Null value
                .role(Role.USER)
                .build();

        // When: Save and retrieve the user
        User savedUser = userRepository.save(testUser);
        userRepository.flush();
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());

        // Then: Null values should remain null
        assertTrue(retrievedUser.isPresent());
        User user = retrievedUser.get();
        assertNull(user.getTwoFactorSecret());
        assertNull(user.getLastLoginIp());
    }

    @Test
    @DisplayName("Should be able to search by email hash")
    void testSearchByEmailHash() {
        // Given: Multiple users with different emails
        String targetEmail = "findme@example.com";
        
        User user1 = User.builder()
                .username("user1")
                .email("other1@example.com")
                .password("pass1")
                .firstName("User")
                .lastName("One")
                .role(Role.USER)
                .build();
        
        User user2 = User.builder()
                .username("user2")
                .email(targetEmail)
                .password("pass2")
                .firstName("User")
                .lastName("Two")
                .role(Role.USER)
                .build();
        
        User user3 = User.builder()
                .username("user3")
                .email("other2@example.com")
                .password("pass3")
                .firstName("User")
                .lastName("Three")
                .role(Role.USER)
                .build();

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.flush();

        // When: Search by email hash
        String emailHash = encryptionService.hash(targetEmail.toLowerCase());
        UUID foundUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email_hash = ?",
                UUID.class,
                emailHash
        );

        // Then: Should find the correct user
        assertThat(foundUserId).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should maintain data integrity through encryption/decryption cycle")
    void testDataIntegrity() {
        // Given: A user with special characters in PII
        String email = "user+test@example.com";
        String firstName = "José";
        String lastName = "O'Brien-Smith";
        String bio = "🚀 Developer with émojis and spëcial çharacters";

        testUser = User.builder()
                .username("specialuser")
                .email(email)
                .password("pass123")
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        testPersona = Persona.builder()
                .user(testUser)
                .name("personal")
                .type(PersonaType.PERSONAL)
                .bio(bio)
                .build();
        testPersona = personaRepository.save(testPersona);
        
        userRepository.flush();
        personaRepository.flush();

        // When: Retrieve the data
        User retrievedUser = userRepository.findById(testUser.getId()).orElseThrow();
        Persona retrievedPersona = personaRepository.findById(testPersona.getId()).orElseThrow();

        // Then: Special characters should be preserved
        assertThat(retrievedUser.getEmail()).isEqualTo(email);
        assertThat(retrievedUser.getFirstName()).isEqualTo(firstName);
        assertThat(retrievedUser.getLastName()).isEqualTo(lastName);
        assertThat(retrievedPersona.getBio()).isEqualTo(bio);
    }

    @Test
    @DisplayName("Should handle large text in encrypted fields")
    void testLargeTextEncryption() {
        // Given: A persona with large bio text
        StringBuilder largeBio = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeBio.append("This is line ").append(i).append(" of a very long biography. ");
        }
        String bioText = largeBio.toString();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("pass123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        testPersona = Persona.builder()
                .user(testUser)
                .name("detailed")
                .type(PersonaType.PERSONAL)
                .bio(bioText)
                .build();

        // When: Save and retrieve
        Persona savedPersona = personaRepository.save(testPersona);
        personaRepository.flush();
        Persona retrievedPersona = personaRepository.findById(savedPersona.getId()).orElseThrow();

        // Then: Large text should be properly encrypted and decrypted
        assertThat(retrievedPersona.getBio()).isEqualTo(bioText);
    }
}