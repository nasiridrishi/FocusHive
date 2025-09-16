package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import java.util.UUID;

/**
 * Service interface for user management operations.
 * Handles user profile operations, password changes, account deletion and recovery.
 */
public interface UserManagementService {

    /**
     * Retrieve user profile information (USER-001).
     *
     * @param userId the ID of the user
     * @return user profile with non-sensitive information
     */
    UserProfileResponse getUserProfile(UUID userId);

    /**
     * Update user profile information (USER-002).
     *
     * @param userId the ID of the user
     * @param request update request with new profile data
     * @return updated user profile
     */
    UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request);

    /**
     * Change user password (USER-003).
     *
     * @param userId the ID of the user
     * @param request password change request
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

    /**
     * Delete user account with grace period (USER-004).
     *
     * @param userId the ID of the user
     * @param request account deletion request
     * @return deletion response with recovery information
     */
    AccountDeletionResponse deleteAccount(UUID userId, DeleteAccountRequest request);

    /**
     * Recover deleted account within grace period (USER-005).
     *
     * @param recoveryToken the recovery token
     * @return recovery response
     */
    AccountRecoveryResponse recoverAccount(String recoveryToken);
}