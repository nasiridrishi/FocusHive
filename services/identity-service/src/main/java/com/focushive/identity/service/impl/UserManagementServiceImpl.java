package com.focushive.identity.service.impl;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.AuthenticationException;
import com.focushive.identity.exception.ResourceNotFoundException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of UserManagementService.
 * Handles user profile operations, password changes, account deletion and recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    private static final int RECOVERY_PERIOD_DAYS = 30;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        log.info("Retrieving user profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Persona> personas = personaRepository.findByUser(user);

        return buildUserProfileResponse(user, personas);
    }

    @Override
    public UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        log.info("Updating user profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate unique constraints
        validateUpdateRequest(user, request);

        // Update user fields
        updateUserFields(user, request);

        // Save updated user
        User savedUser = userRepository.save(user);

        // Create audit log
        auditService.logUserProfileUpdate(userId, "Profile updated");

        // Return updated profile
        List<Persona> personas = personaRepository.findByUser(savedUser);
        return buildUserProfileResponse(savedUser, personas);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Invalidate existing sessions by updating token invalidation timestamp
        user.setTokenInvalidatedAt(Instant.now());

        userRepository.save(user);

        // Create audit log
        auditService.logPasswordChange(userId);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    public AccountDeletionResponse deleteAccount(UUID userId, DeleteAccountRequest request) {
        log.info("Deleting account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if account is already marked for deletion
        if (user.getAccountDeletedAt() != null) {
            throw new IllegalStateException("Account is already marked for deletion");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Password verification failed");
        }

        // Mark account for deletion
        Instant deletionTime = Instant.now();
        String recoveryToken = UUID.randomUUID().toString();

        user.setAccountDeletedAt(deletionTime);
        user.setDeletionToken(recoveryToken);

        userRepository.save(user);

        // Handle data export if requested
        String dataExportUrl = null;
        if (request.isExportData()) {
            dataExportUrl = generateDataExportUrl(user);
            emailService.sendDataExportEmail(user, dataExportUrl);
        }

        // Create audit log
        auditService.logAccountDeletion(userId, request.getReason());

        // Calculate deletion and recovery dates
        LocalDateTime deletionScheduledFor = LocalDateTime.ofInstant(
                deletionTime.plus(RECOVERY_PERIOD_DAYS, ChronoUnit.DAYS), ZoneOffset.UTC);
        LocalDateTime recoveryDeadline = deletionScheduledFor;

        return AccountDeletionResponse.builder()
                .success(true)
                .recoveryToken(recoveryToken)
                .deletionScheduledFor(deletionScheduledFor)
                .recoveryDeadline(recoveryDeadline)
                .dataExportUrl(dataExportUrl)
                .message("Account has been marked for deletion with a 30-day grace period for recovery")
                .build();
    }

    @Override
    public AccountRecoveryResponse recoverAccount(String recoveryToken) {
        log.info("Recovering account with token: {}", recoveryToken);

        User user = userRepository.findByDeletionToken(recoveryToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid recovery token"));

        // Check if account is marked for deletion
        if (user.getAccountDeletedAt() == null) {
            throw new IllegalStateException("Account is not marked for deletion");
        }

        // Check if recovery period has expired
        Instant recoveryDeadline = user.getAccountDeletedAt().plus(RECOVERY_PERIOD_DAYS, ChronoUnit.DAYS);
        if (Instant.now().isAfter(recoveryDeadline)) {
            throw new IllegalStateException("Recovery period has expired");
        }

        // Recover account
        user.setAccountDeletedAt(null);
        user.setDeletionToken(null);
        user.setEnabled(true);

        userRepository.save(user);

        // Create audit log
        auditService.logAccountRecovery(user.getId());

        return AccountRecoveryResponse.builder()
                .success(true)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .recoveredAt(LocalDateTime.now())
                .message("Account successfully recovered")
                .build();
    }

    // ==================== Private Helper Methods ====================

    private UserProfileResponse buildUserProfileResponse(User user, List<Persona> personas) {
        List<PersonaDto> personaDtos = personas.stream()
                .map(this::mapPersonaToDto)
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getUsername()) // Username is the public display name
                .emailVerified(user.isEmailVerified())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .notificationPreferences(user.getNotificationPreferences())
                .createdAt(LocalDateTime.ofInstant(user.getCreatedAt(), ZoneOffset.UTC))
                .lastLoginAt(user.getLastLoginAt() != null ?
                        LocalDateTime.ofInstant(user.getLastLoginAt(), ZoneOffset.UTC) : null)
                .updatedAt(LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneOffset.UTC))
                .personas(personaDtos)
                .build();
    }

    private PersonaDto mapPersonaToDto(Persona persona) {
        return PersonaDto.builder()
                .id(persona.getId())
                .name(persona.getName())
                .type(persona.getType())
                .displayName(persona.getDisplayName())
                .avatarUrl(persona.getAvatarUrl())
                .bio(persona.getBio())
                .statusMessage(persona.getStatusMessage())
                .isDefault(persona.isDefault())
                .isActive(persona.isActive())
                .build();
    }

    private void validateUpdateRequest(User user, UpdateUserProfileRequest request) {
        // Check username uniqueness if changed
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already exists");
            }
        }

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
        }
    }

    private void updateUserFields(User user, UpdateUserProfileRequest request) {
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }
        if (request.getNotificationPreferences() != null) {
            user.setNotificationPreferences(request.getNotificationPreferences());
        }
    }

    private String generateDataExportUrl(User user) {
        // In a real implementation, this would trigger an async data export process
        // and return a URL where the user can download their data
        return String.format("/api/data-export/%s", UUID.randomUUID().toString());
    }
}