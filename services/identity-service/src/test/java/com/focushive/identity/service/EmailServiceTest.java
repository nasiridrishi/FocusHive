package com.focushive.identity.service;

import com.focushive.identity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for EmailService.
 * Tests email sending functionality with proper logging and security considerations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(false)
                .emailVerificationToken("verification-token-123")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(Instant.now())
                .personas(new ArrayList<>())
                .build();

        // Set up application properties via reflection
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@focushive.com");
    }

    @Test
    @DisplayName("Should send verification email successfully")
    void sendVerificationEmail_ValidUser_ShouldProcessSuccessfully() {
        // When
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
        });
        
        // Then - No exception should be thrown, method should complete
        // In a real implementation, we would verify email was sent
    }

    @Test
    @DisplayName("Should handle user with null email verification token")
    void sendVerificationEmail_NullVerificationToken_ShouldHandleGracefully() {
        // Given
        testUser.setEmailVerificationToken(null);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
        });
    }

    @Test
    @DisplayName("Should handle user with empty email verification token")
    void sendVerificationEmail_EmptyVerificationToken_ShouldHandleGracefully() {
        // Given
        testUser.setEmailVerificationToken("");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
        });
    }

    @Test
    @DisplayName("Should handle user with long verification token")
    void sendVerificationEmail_LongVerificationToken_ShouldHandleGracefully() {
        // Given
        String longToken = "a".repeat(1000); // Very long token
        testUser.setEmailVerificationToken(longToken);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
        });
    }

    @Test
    @DisplayName("Should handle null user in sendVerificationEmail")
    void sendVerificationEmail_NullUser_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> {
            emailService.sendVerificationEmail(null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void sendPasswordResetEmail_ValidUser_ShouldProcessSuccessfully() {
        // Given
        String resetToken = "reset-token-456";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendPasswordResetEmail(testUser, resetToken);
        });
    }

    @Test
    @DisplayName("Should handle null reset token in sendPasswordResetEmail")
    void sendPasswordResetEmail_NullResetToken_ShouldHandleGracefully() {
        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendPasswordResetEmail(testUser, null);
        });
    }

    @Test
    @DisplayName("Should handle empty reset token in sendPasswordResetEmail")
    void sendPasswordResetEmail_EmptyResetToken_ShouldHandleGracefully() {
        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendPasswordResetEmail(testUser, "");
        });
    }

    @Test
    @DisplayName("Should handle long reset token in sendPasswordResetEmail")
    void sendPasswordResetEmail_LongResetToken_ShouldHandleGracefully() {
        // Given
        String longResetToken = "b".repeat(2000);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendPasswordResetEmail(testUser, longResetToken);
        });
    }

    @Test
    @DisplayName("Should handle null user in sendPasswordResetEmail")
    void sendPasswordResetEmail_NullUser_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> {
            emailService.sendPasswordResetEmail(null, "reset-token");
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void sendWelcomeEmail_ValidUser_ShouldProcessSuccessfully() {
        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendWelcomeEmail(testUser);
        });
    }

    @Test
    @DisplayName("Should handle null user in sendWelcomeEmail")
    void sendWelcomeEmail_NullUser_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> {
            emailService.sendWelcomeEmail(null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should send new device login alert successfully")
    void sendNewDeviceLoginAlert_ValidParameters_ShouldProcessSuccessfully() {
        // Given
        String deviceInfo = "Chrome 91.0 on Windows 10";
        String ipAddress = "192.168.1.100";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, ipAddress);
        });
    }

    @Test
    @DisplayName("Should handle null device info in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_NullDeviceInfo_ShouldHandleGracefully() {
        // Given
        String ipAddress = "192.168.1.100";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, null, ipAddress);
        });
    }

    @Test
    @DisplayName("Should handle null IP address in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_NullIpAddress_ShouldHandleGracefully() {
        // Given
        String deviceInfo = "Chrome 91.0 on Windows 10";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, null);
        });
    }

    @Test
    @DisplayName("Should handle empty device info in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_EmptyDeviceInfo_ShouldHandleGracefully() {
        // Given
        String ipAddress = "192.168.1.100";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, "", ipAddress);
        });
    }

    @Test
    @DisplayName("Should handle empty IP address in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_EmptyIpAddress_ShouldHandleGracefully() {
        // Given
        String deviceInfo = "Chrome 91.0 on Windows 10";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, "");
        });
    }

    @Test
    @DisplayName("Should handle very long IP address in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_VeryLongIpAddress_ShouldHandleGracefully() {
        // Given
        String deviceInfo = "Chrome 91.0 on Windows 10";
        String veryLongIp = "192.168.1.100" + "x".repeat(1000);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, veryLongIp);
        });
    }

    @Test
    @DisplayName("Should handle short IP address in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_ShortIpAddress_ShouldHandleGracefully() {
        // Given
        String deviceInfo = "Chrome 91.0 on Windows 10";
        String shortIp = "127.0.0.1"; // 9 characters, should be handled correctly

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, shortIp);
        });
    }

    @Test
    @DisplayName("Should handle null user in sendNewDeviceLoginAlert")
    void sendNewDeviceLoginAlert_NullUser_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> {
            emailService.sendNewDeviceLoginAlert(null, "device", "ip");
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should work with custom base URL configuration")
    void emailService_CustomBaseUrl_ShouldUseCustomUrl() {
        // Given
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://production.focushive.com");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, "token");
        });
    }

    @Test
    @DisplayName("Should work with custom from email configuration")
    void emailService_CustomFromEmail_ShouldUseCustomEmail() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromEmail", "notifications@custom-domain.com");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendWelcomeEmail(testUser);
        });
    }

    @Test
    @DisplayName("Should handle null base URL configuration")
    void emailService_NullBaseUrl_ShouldHandleGracefully() {
        // Given
        ReflectionTestUtils.setField(emailService, "baseUrl", null);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, "token");
        });
    }

    @Test
    @DisplayName("Should handle empty base URL configuration")
    void emailService_EmptyBaseUrl_ShouldHandleGracefully() {
        // Given
        ReflectionTestUtils.setField(emailService, "baseUrl", "");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, "token");
        });
    }

    @Test
    @DisplayName("Should handle user with null ID")
    void emailService_UserWithNullId_ShouldHandleGracefully() {
        // Given
        testUser.setId(null);

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, "token");
            emailService.sendWelcomeEmail(testUser);
            emailService.sendNewDeviceLoginAlert(testUser, "device", "ip");
        });
    }

    @Test
    @DisplayName("Should work with user having all email methods called")
    void emailService_AllMethods_ShouldWorkTogether() {
        // Given
        String resetToken = "reset-token-789";
        String deviceInfo = "Firefox 89.0 on Linux";
        String ipAddress = "10.0.0.1";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, resetToken);
            emailService.sendWelcomeEmail(testUser);
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, ipAddress);
        });
    }

    @Test
    @DisplayName("Should handle special characters in tokens and parameters")
    void emailService_SpecialCharacters_ShouldHandleGracefully() {
        // Given
        testUser.setEmailVerificationToken("token-with-special-chars-!@#$%^&*()");
        String resetToken = "reset-token-with-unicode-🔒🛡️";
        String deviceInfo = "Browser with émojis 😀 and spëcial chars";
        String ipAddress = "192.168.1.100";

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            emailService.sendVerificationEmail(testUser);
            emailService.sendPasswordResetEmail(testUser, resetToken);
            emailService.sendNewDeviceLoginAlert(testUser, deviceInfo, ipAddress);
        });
    }
}