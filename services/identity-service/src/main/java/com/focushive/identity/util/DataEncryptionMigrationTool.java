package com.focushive.identity.util;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.encryption.IEncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data migration tool for encrypting existing PII data in the database.
 * 
 * This tool should be run once after deploying the field-level encryption feature
 * to encrypt any existing unencrypted PII data.
 * 
 * To run this tool:
 * 1. Set the environment variable: ENCRYPTION_MIGRATION_ENABLED=true
 * 2. Ensure ENCRYPTION_MASTER_KEY and ENCRYPTION_SALT are properly configured
 * 3. Run the application with the migration profile: --spring.profiles.active=migration
 * 4. The tool will automatically encrypt all unencrypted PII data
 * 5. After completion, remove the ENCRYPTION_MIGRATION_ENABLED variable
 * 
 * WARNING: This is a one-way operation. Ensure you have proper backups before running.
 */
@Component
@Profile("migration")
@ConditionalOnProperty(name = "encryption.migration.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataEncryptionMigrationTool implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionMigrationTool.class);

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final IEncryptionService encryptionService;
    private final DataSource dataSource;

    private static final int BATCH_SIZE = 100;
    private final AtomicInteger usersProcessed = new AtomicInteger(0);
    private final AtomicInteger personasProcessed = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Starting PII Data Encryption Migration");
        log.info("========================================");
        
        // Check if migration is needed
        if (!isMigrationNeeded()) {
            log.info("No unencrypted data found. Migration not needed.");
            return;
        }
        
        log.warn("IMPORTANT: This will encrypt all PII data in the database.");
        log.warn("Ensure you have proper backups before proceeding.");
        
        // Add a delay to allow cancellation
        Thread.sleep(5000);
        
        log.info("Starting migration process...");
        
        // Migrate users
        migrateUsers();
        
        // Migrate personas
        migratePersonas();
        
        // Generate email hashes for existing users
        generateEmailHashes();
        
        // Print summary
        printSummary();
        
        log.info("========================================");
        log.info("PII Data Encryption Migration Complete");
        log.info("========================================");
        
        // Exit after migration
        System.exit(0);
    }
    
    /**
     * Check if migration is needed by looking for unencrypted data.
     */
    private boolean isMigrationNeeded() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Check for users without email hash (indicator of unencrypted data)
            String query = "SELECT COUNT(*) FROM users WHERE email_hash IS NULL";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        log.info("Found {} users without email hash", count);
                        return true;
                    }
                }
            }
            
            // Check if any email field doesn't start with encryption version prefix
            query = "SELECT COUNT(*) FROM users WHERE email NOT LIKE 'v1:%' AND email IS NOT NULL";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        log.info("Found {} users with unencrypted email", count);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Migrate user PII data to encrypted format.
     */
    @Transactional
    protected void migrateUsers() {
        log.info("Migrating user data...");
        
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<User> page;
        
        do {
            page = userRepository.findAll(pageable);
            List<User> usersToUpdate = new ArrayList<>();
            
            for (User user : page.getContent()) {
                try {
                    boolean needsUpdate = false;
                    
                    // Check if email needs encryption (doesn't have version prefix)
                    if (user.getEmail() != null && !user.getEmail().startsWith("v1:")) {
                        // Email is not encrypted, but JPA converter will handle it on save
                        needsUpdate = true;
                    }
                    
                    // Check if email hash needs to be generated
                    if (user.getEmail() != null && user.getEmailHash() == null) {
                        user.setEmailHash(encryptionService.hash(user.getEmail().toLowerCase()));
                        needsUpdate = true;
                    }
                    
                    if (needsUpdate) {
                        usersToUpdate.add(user);
                        usersProcessed.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing user {}: {}", user.getId(), e.getMessage());
                    errors.incrementAndGet();
                }
            }
            
            // Save batch
            if (!usersToUpdate.isEmpty()) {
                userRepository.saveAll(usersToUpdate);
                log.info("Processed {} users", usersProcessed.get());
            }
            
            pageable = page.nextPageable();
            
        } while (page.hasNext());
        
        log.info("User migration complete. Processed: {}, Errors: {}", 
                usersProcessed.get(), errors.get());
    }
    
    /**
     * Migrate persona PII data to encrypted format.
     */
    @Transactional
    protected void migratePersonas() {
        log.info("Migrating persona data...");
        
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<Persona> page;
        
        do {
            page = personaRepository.findAll(pageable);
            List<Persona> personasToUpdate = new ArrayList<>();
            
            for (Persona persona : page.getContent()) {
                try {
                    boolean needsUpdate = false;
                    
                    // Check if displayName needs encryption
                    if (persona.getDisplayName() != null && !persona.getDisplayName().startsWith("v1:")) {
                        needsUpdate = true;
                    }
                    
                    // Check if bio needs encryption
                    if (persona.getBio() != null && !persona.getBio().startsWith("v1:")) {
                        needsUpdate = true;
                    }
                    
                    // Check if statusMessage needs encryption
                    if (persona.getStatusMessage() != null && !persona.getStatusMessage().startsWith("v1:")) {
                        needsUpdate = true;
                    }
                    
                    if (needsUpdate) {
                        personasToUpdate.add(persona);
                        personasProcessed.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing persona {}: {}", persona.getId(), e.getMessage());
                    errors.incrementAndGet();
                }
            }
            
            // Save batch
            if (!personasToUpdate.isEmpty()) {
                personaRepository.saveAll(personasToUpdate);
                log.info("Processed {} personas", personasProcessed.get());
            }
            
            pageable = page.nextPageable();
            
        } while (page.hasNext());
        
        log.info("Persona migration complete. Processed: {}, Errors: {}", 
                personasProcessed.get(), errors.get());
    }
    
    /**
     * Generate email hashes for users that don't have them.
     */
    private void generateEmailHashes() throws Exception {
        log.info("Generating email hashes...");
        
        try (Connection conn = dataSource.getConnection()) {
            // Find users without email hash
            String selectQuery = "SELECT id, email FROM users WHERE email_hash IS NULL AND email IS NOT NULL";
            String updateQuery = "UPDATE users SET email_hash = ? WHERE id = ?";
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
                 ResultSet rs = selectStmt.executeQuery();
                 PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                
                int count = 0;
                while (rs.next()) {
                    UUID userId = UUID.fromString(rs.getString("id"));
                    String email = rs.getString("email");
                    
                    // Decrypt email if it's encrypted
                    if (email.startsWith("v1:")) {
                        email = encryptionService.decrypt(email);
                    }
                    
                    // Generate hash
                    String emailHash = encryptionService.hash(email.toLowerCase());
                    
                    // Update database
                    updateStmt.setString(1, emailHash);
                    updateStmt.setObject(2, userId);
                    updateStmt.addBatch();
                    
                    count++;
                    
                    // Execute batch periodically
                    if (count % BATCH_SIZE == 0) {
                        updateStmt.executeBatch();
                        log.info("Generated {} email hashes", count);
                    }
                }
                
                // Execute remaining
                if (count % BATCH_SIZE != 0) {
                    updateStmt.executeBatch();
                }
                
                log.info("Email hash generation complete. Generated: {}", count);
            }
        }
    }
    
    /**
     * Print migration summary.
     */
    private void printSummary() {
        log.info("");
        log.info("Migration Summary:");
        log.info("==================");
        log.info("Users processed: {}", usersProcessed.get());
        log.info("Personas processed: {}", personasProcessed.get());
        log.info("Errors encountered: {}", errors.get());
        log.info("");
        
        if (errors.get() > 0) {
            log.warn("Migration completed with errors. Please review the logs.");
        } else {
            log.info("Migration completed successfully!");
        }
        
        log.info("");
        log.info("IMPORTANT: Remember to:");
        log.info("1. Remove ENCRYPTION_MIGRATION_ENABLED environment variable");
        log.info("2. Test the application to ensure encryption is working");
        log.info("3. Verify that users can still log in and access their data");
        log.info("4. Keep the encryption keys backed up securely");
    }
}