package com.focushive.identity.service;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Role;
import com.focushive.identity.entity.User;
import com.focushive.identity.security.encryption.EncryptionKeyService;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for creating a default admin user on application startup.
 * This makes initial setup much easier by providing a default administrative account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonaRepository personaRepository;
    private final EncryptionKeyService encryptionKeyService;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.admin.email:admin@focushive.local}")
    private String adminEmail;

    @Value("${app.admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.admin.last-name:Administrator}")
    private String adminLastName;

    @Value("${app.admin.auto-create:true}")
    private boolean autoCreateAdmin;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeAdminUser() {
        if (!autoCreateAdmin) {
            log.info("Admin user auto-creation is disabled");
            return;
        }
        
        log.info("Starting admin user initialization (encryption service should be ready)...");
        
        // Ensure encryption service is initialized before creating users with encrypted fields
        try {
            encryptionKeyService.getCurrentEncryptionKey();
            log.info("Encryption service is ready, proceeding with admin user creation");
        } catch (IllegalStateException e) {
            log.error("Cannot create admin user: Encryption service not initialized. Retrying in 5 seconds...");
            try {
                Thread.sleep(5000);
                encryptionKeyService.getCurrentEncryptionKey();
                log.info("Encryption service is now ready on retry");
            } catch (Exception retryException) {
                log.error("Admin user creation skipped - encryption service unavailable", retryException);
                return;
            }
        }

        try {
            createAdminUserIfNotExists();
        } catch (Exception e) {
            log.error("Failed to create admin user", e);
            throw new RuntimeException("Admin user initialization failed", e);
        }
    }

    private void createAdminUserIfNotExists() {
        // Check if admin user already exists
        Optional<User> existingAdmin = userRepository.findByUsernameOrEmail(adminUsername, adminEmail);
        
        if (existingAdmin.isPresent()) {
            log.info("Admin user '{}' already exists, skipping creation", adminUsername);
            return;
        }

        log.info("Creating default admin user: {}", adminUsername);

        // Create admin user
        User adminUser = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .password(passwordEncoder.encode(adminPassword))
                .emailVerified(true) // Admin user is pre-verified
                .accountNonLocked(true)
                .enabled(true)
                .failedLoginAttempts(0)
                .createdAt(Instant.now())
                .lastLoginAt(null)
                .build();

        // Save admin user
        adminUser = userRepository.save(adminUser);
        log.info("✅ Admin user created with ID: {}", adminUser.getId());

        // Create default admin persona
        Persona adminPersona = Persona.builder()
                .user(adminUser)
                .name("Administrator")
                .type(Persona.PersonaType.WORK)
                .displayName("System Administrator")
                .bio("Default administrative account")
                .isDefault(true)
                .privacySettings(Persona.PrivacySettings.builder()
                        .visibilityLevel("PRIVATE")
                        .showRealName(true)
                        .showEmail(false)
                        .build())
                .createdAt(Instant.now())
                .build();

        personaRepository.save(adminPersona);
        log.info("✅ Admin persona created for user: {}", adminUsername);

        // Log credentials for initial setup (only in development)
        if (isProductionEnvironment()) {
            log.info("🔐 Admin user created successfully. Please change the default credentials.");
        } else {
            log.info("🔐 Admin user created:");
            log.info("   Username: {}", adminUsername);
            log.info("   Email: {}", adminEmail);
            log.info("   Password: [CONFIGURED_VIA_ENV]");
            log.info("   ⚠️  Please change these default credentials in production!");
        }
    }

    private boolean isProductionEnvironment() {
        String env = System.getProperty("spring.profiles.active", "");
        return env.contains("prod") || env.contains("production");
    }
}