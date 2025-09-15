package com.focushive.identity.entity;

import com.focushive.identity.security.encryption.IEncryptionService;
import com.focushive.identity.security.encryption.converters.SpringContextUtil;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Base class for entities that have encrypted fields.
 * Provides common functionality for managing encrypted fields and their hashes.
 * Implements Serializable for Redis caching support.
 */
@MappedSuperclass
public abstract class BaseEncryptedEntity implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(BaseEncryptedEntity.class);
    
    /**
     * Called before persisting or updating the entity.
     * Generates hashes for searchable encrypted fields.
     */
    @PrePersist
    @PreUpdate
    public void generateHashes() {
        IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
        if (encryptionService == null) {
            log.warn("EncryptionService not available during entity lifecycle");
            return;
        }
        
        try {
            updateSearchableHashes(encryptionService);
        } catch (Exception e) {
            log.error("Failed to generate hashes for encrypted fields", e);
            throw new RuntimeException("Hash generation failed", e);
        }
    }
    
    /**
     * Called after loading the entity from database.
     * Can be used for any post-load encryption-related processing.
     */
    @PostLoad
    public void postLoadProcessing() {
        IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
        if (encryptionService == null) {
            log.debug("EncryptionService not available during post-load");
            return;
        }
        
        try {
            performPostLoadEncryptionTasks(encryptionService);
        } catch (Exception e) {
            log.error("Failed to perform post-load encryption tasks", e);
            // Don't throw here as it might break entity loading
        }
    }
    
    /**
     * Abstract method to be implemented by subclasses to update their searchable hashes.
     * This is called before persisting or updating the entity.
     * 
     * @param encryptionService the encryption service to use for generating hashes
     */
    protected abstract void updateSearchableHashes(IEncryptionService encryptionService);
    
    /**
     * Abstract method to be implemented by subclasses for any post-load encryption tasks.
     * This is called after loading the entity from the database.
     * Default implementation does nothing.
     * 
     * @param encryptionService the encryption service
     */
    protected void performPostLoadEncryptionTasks(IEncryptionService encryptionService) {
        // Default implementation does nothing
    }
}