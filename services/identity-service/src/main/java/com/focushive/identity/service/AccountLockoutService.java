package com.focushive.identity.service;

import com.focushive.identity.config.AccountLockoutProperties;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.AccountLockedException;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service handling account lockout functionality.
 * Implements OWASP A04:2021 Insecure Design protection against brute force attacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final AccountLockoutProperties lockoutProperties;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    /**
     * Check if an account is locked and throw exception if locked.
     *
     * @param user The user to check
     * @throws AccountLockedException if account is locked
     */
    public void checkAccountLockout(User user) {
        if (!lockoutProperties.isEnabled()) {
            return;
        }

        // Auto-unlock expired lockouts
        if (lockoutProperties.isAutoUnlockEnabled() && user.shouldAutoUnlock()) {
            unlockAccount(user, "AUTO_UNLOCK_EXPIRED");
            return;
        }

        if (!user.isAccountNonLocked()) {
            String reason = user.getAccountLockedUntil() != null ?
                    String.format("Account locked until %s", user.getAccountLockedUntil()) :
                    "Account locked indefinitely";

            log.warn("Login attempt blocked for locked account: {} - {}", user.getUsername(), reason);

            if (lockoutProperties.isAuditLockoutEvents()) {
                auditService.logSecurityEvent(user.getId(), "LOGIN_ATTEMPT_LOCKED_ACCOUNT",
                        "Login attempt on locked account", null);
            }

            throw new AccountLockedException("Account is locked due to multiple failed login attempts");
        }
    }

    /**
     * Record a failed login attempt and potentially lock the account.
     *
     * @param user The user with failed login
     * @param ipAddress IP address of the failed attempt (optional)
     */
    @Transactional
    public void recordFailedLoginAttempt(User user, String ipAddress) {
        if (!lockoutProperties.isEnabled()) {
            return;
        }

        log.warn("Failed login attempt for user: {} (attempt #{}) from IP: {}",
                user.getUsername(), user.getFailedLoginAttempts() + 1, ipAddress);

        boolean wasLocked = !user.isAccountNonLocked();
        user.recordFailedLoginAttempt(
                lockoutProperties.getMaxFailedAttempts(),
                lockoutProperties.getLockoutDurationMinutes()
        );

        userRepository.save(user);

        // Log lockout event if account was just locked
        if (!wasLocked && !user.isAccountNonLocked()) {
            log.error("Account locked for user: {} after {} failed attempts",
                    user.getUsername(), user.getFailedLoginAttempts());

            String lockoutDuration = lockoutProperties.getLockoutDurationMinutes() > 0 ?
                    lockoutProperties.getLockoutDurationMinutes() + " minutes" : "indefinite";

            // Send lockout notification email
            try {
                emailService.sendAccountLockoutEmail(user, lockoutDuration, user.getFailedLoginAttempts());
            } catch (Exception e) {
                log.error("Failed to send account lockout email to user: {} - Error: {}",
                         user.getUsername(), e.getMessage());
                // Don't fail the lockout process if email fails
            }

            if (lockoutProperties.isAuditLockoutEvents()) {
                auditService.logSecurityEvent(user.getId(), "ACCOUNT_LOCKED",
                        String.format("Account locked after %d failed attempts. Duration: %s",
                                user.getFailedLoginAttempts(), lockoutDuration), ipAddress);
            }
        } else if (lockoutProperties.isAuditLockoutEvents()) {
            auditService.logSecurityEvent(user.getId(), "FAILED_LOGIN_ATTEMPT",
                    String.format("Failed login attempt %d/%d",
                            user.getFailedLoginAttempts(), lockoutProperties.getMaxFailedAttempts()), ipAddress);
        }
    }

    /**
     * Reset failed login attempts after successful authentication.
     *
     * @param user The user with successful login
     * @param ipAddress IP address of successful attempt (optional)
     */
    @Transactional
    public void recordSuccessfulLogin(User user, String ipAddress) {
        if (!lockoutProperties.isEnabled() || !lockoutProperties.isResetAttemptsOnSuccess()) {
            return;
        }

        if (user.getFailedLoginAttempts() > 0) {
            log.info("Resetting failed login attempts for user: {} (had {} failed attempts)",
                    user.getUsername(), user.getFailedLoginAttempts());

            user.resetFailedLoginAttempts();
            userRepository.save(user);

            if (lockoutProperties.isAuditLockoutEvents()) {
                auditService.logSecurityEvent(user.getId(), "LOGIN_SUCCESS_RESET_ATTEMPTS",
                        "Successful login reset failed attempt counter", ipAddress);
            }
        }
    }

    /**
     * Manually unlock an account (admin action).
     *
     * @param user The user to unlock
     * @param reason Reason for unlocking
     */
    @Transactional
    public void unlockAccount(User user, String reason) {
        if (!user.isAccountNonLocked()) {
            log.info("Unlocking account for user: {} - Reason: {}", user.getUsername(), reason);

            user.unlockAccount();
            userRepository.save(user);

            if (lockoutProperties.isAuditLockoutEvents()) {
                auditService.logSecurityEvent(user.getId(), "ACCOUNT_UNLOCKED",
                        "Account manually unlocked - " + reason, null);
            }
        }
    }

    /**
     * Get account lockout status information.
     *
     * @param user The user to check
     * @return Lockout status information
     */
    public AccountLockoutStatus getAccountLockoutStatus(User user) {
        return AccountLockoutStatus.builder()
                .isLocked(!user.isAccountNonLocked())
                .failedAttempts(user.getFailedLoginAttempts())
                .maxAttempts(lockoutProperties.getMaxFailedAttempts())
                .lastFailedAttempt(user.getLastFailedLoginAt())
                .lockedAt(user.getAccountLockedAt())
                .lockedUntil(user.getAccountLockedUntil())
                .autoUnlockEnabled(lockoutProperties.isAutoUnlockEnabled())
                .canAutoUnlock(user.shouldAutoUnlock())
                .build();
    }

    /**
     * Data class for account lockout status information.
     */
    @lombok.Builder
    @lombok.Data
    public static class AccountLockoutStatus {
        private boolean isLocked;
        private int failedAttempts;
        private int maxAttempts;
        private Instant lastFailedAttempt;
        private Instant lockedAt;
        private Instant lockedUntil;
        private boolean autoUnlockEnabled;
        private boolean canAutoUnlock;
    }
}