package com.focushive.user.service;

import com.focushive.user.dto.UserDto;

/**
 * Service interface for user operations.
 */
public interface UserService {
    
    /**
     * Get a user by their ID.
     * 
     * @param userId the user ID
     * @return the user data
     */
    UserDto getUserById(String userId);
    
    /**
     * Get a user by their username.
     * 
     * @param username the username
     * @return the user data
     */
    UserDto getUserByUsername(String username);
}