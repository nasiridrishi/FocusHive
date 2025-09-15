package com.focushive.identity.service;

import com.focushive.identity.config.CacheConfig;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads users by username or email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.focushive.identity.security.encryption.IEncryptionService encryptionService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.USER_CACHE, key = "#usernameOrEmail", unless = "#result == null")
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Removed debug log to avoid logging email addresses and usernames

        // Try to find user by checking if input is email or username
        User user = null;

        // Check if the input looks like an email
        if (isValidEmail(usernameOrEmail)) {
            // It's an email - need to use hash-based lookup for encrypted emails
            if (encryptionService != null) {
                try {
                    String emailHash = encryptionService.hash(usernameOrEmail.toLowerCase());
                    user = userRepository.findByEmailHash(emailHash).orElse(null);
                } catch (Exception e) {
                    log.debug("Hash-based email lookup failed: {}", e.getMessage());
                    // Fallback to direct email lookup for test environments
                    user = userRepository.findByEmail(usernameOrEmail).orElse(null);
                }
            } else {
                // Encryption service not available (test mode), use direct lookup
                user = userRepository.findByEmail(usernameOrEmail).orElse(null);
            }
        }

        // If not found by email (or not an email), try username
        if (user == null) {
            user = userRepository.findByUsername(usernameOrEmail).orElse(null);
        }

        if (user == null) {
            throw new UsernameNotFoundException(
                    "User not found with username or email: " + usernameOrEmail);
        }
        
        // Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User account has been deleted");
        }
        
        // Personas will be loaded via EntityGraph when accessed
        // Force initialization to trigger the optimized loading
        user.getPersonas().size();
        
        // Removed debug log to avoid logging usernames
        
        return user;
    }
    
    /**
     * Load user by ID with caching.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.USER_PROFILE_CACHE, key = "#userId", unless = "#result == null")
    public User loadUserById(String userId) {
        // Use findById which now has EntityGraph for personas
        return userRepository.findActiveById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
    
    /**
     * Evict user from cache when user data is updated.
     */
    @CacheEvict(value = {CacheConfig.USER_CACHE, CacheConfig.USER_PROFILE_CACHE}, allEntries = true)
    public void evictUserCache(String usernameOrEmail) {
        log.debug("Evicting user cache for: {}", usernameOrEmail);
    }
    
    /**
     * Evict specific user from cache by ID.
     */
    @CacheEvict(value = CacheConfig.USER_PROFILE_CACHE, key = "#userId")
    public void evictUserCacheById(String userId) {
        log.debug("Evicting user cache for ID: {}", userId);
    }

    /**
     * Simple email validation to check if a string looks like an email.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Simple check for @ and . in the right places
        return email.contains("@") && email.contains(".") &&
               email.indexOf("@") < email.lastIndexOf(".");
    }
}