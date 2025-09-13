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
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.USER_CACHE, key = "#usernameOrEmail", unless = "#result == null")
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Removed debug log to avoid logging email addresses and usernames
        
        // Try to find by email first, then by username
        // Note: EntityGraph on findById will load personas when user is accessed
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));
        
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
}