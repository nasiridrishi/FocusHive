package com.focushive.identity.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Example integration for Identity Service to send notifications
 * via the FocusHive Notification Service.
 *
 * This service handles all notification needs for user authentication,
 * registration, password resets, and security alerts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableRetry
public class IdentityNotificationService {

    private final RestTemplate restTemplate;
    private final WebClient notificationWebClient;

    @Value("${notification.service.url:https://notification.focushive.app}")
    private String notificationServiceUrl;

    @Value("${notification.service.internal.url:http://focushive-notification-service-app:8083}")
    private String notificationServiceInternalUrl;

    @Value("${notification.service.use-internal:false}")
    private boolean useInternalUrl;

    @Value("${jwt.service.token}")
    private String serviceToken; // Service-to-service authentication token

    /**
     * Configuration class for setting up RestTemplate and WebClient
     */
    @Configuration
    public static class NotificationClientConfig {

        @Bean
        public RestTemplate notificationRestTemplate(RestTemplateBuilder builder) {
            return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        }

        @Bean
        public WebClient notificationWebClient(WebClient.Builder builder) {
            return builder
                .baseUrl("https://notification.focushive.app")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
        }
    }

    /**
     * Send welcome email to newly registered user
     *
     * @param userId The unique identifier of the user
     * @param email The user's email address
     * @param name The user's display name
     * @return true if notification was sent successfully
     */
    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean sendWelcomeEmail(String userId, String email, String name) {
        log.info("Sending welcome email to user: {} ({})", userId, email);

        String url = getServiceUrl() + "/api/v1/notifications";

        Map<String, Object> notificationRequest = new HashMap<>();
        notificationRequest.put("userId", userId);
        notificationRequest.put("type", "USER_REGISTERED");
        notificationRequest.put("priority", "HIGH");
        notificationRequest.put("title", "Welcome to FocusHive!");
        notificationRequest.put("content", String.format(
            "Hi %s,\n\nWelcome to FocusHive! Your account has been successfully created.\n\n" +
            "Get started by:\n" +
            "1. Creating your first focus hive\n" +
            "2. Setting up your profile\n" +
            "3. Finding accountability buddies\n\n" +
            "Happy focusing!",
            name
        ));
        notificationRequest.put("data", Map.of(
            "email", email,
            "userName", name,
            "registrationDate", Instant.now().toString(),
            "welcomeGuideUrl", "https://focushive.app/welcome"
        ));

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Welcome notification sent successfully. ID: {}",
                    response.getBody().get("id"));
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to send welcome notification for user: {}", userId, e);
            // In production, you might want to queue this for retry or send to a DLQ
        }
        return false;
    }

    /**
     * Send password reset email with secure token
     *
     * @param userId The user's ID
     * @param email The user's email address
     * @param resetToken The secure password reset token
     * @param expirationMinutes Token expiration time in minutes
     */
    public Mono<Boolean> sendPasswordResetEmail(
            String userId,
            String email,
            String resetToken,
            int expirationMinutes) {

        log.info("Sending password reset email to user: {}", userId);

        Map<String, Object> notificationRequest = Map.of(
            "userId", userId,
            "type", "PASSWORD_RESET_REQUEST",
            "priority", "URGENT",
            "title", "Password Reset Request",
            "content", String.format(
                "A password reset has been requested for your account.\n\n" +
                "Click the link below to reset your password:\n" +
                "https://focushive.app/reset-password?token=%s\n\n" +
                "This link will expire in %d minutes.\n\n" +
                "If you didn't request this, please ignore this email or contact support.",
                resetToken, expirationMinutes
            ),
            "data", Map.of(
                "resetToken", resetToken,
                "expirationTime", Instant.now().plusSeconds(expirationMinutes * 60).toString(),
                "ipAddress", getCurrentUserIpAddress(),
                "userAgent", getCurrentUserAgent()
            )
        );

        return notificationWebClient.post()
            .uri("/api/v1/notifications")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthToken())
            .body(Mono.just(notificationRequest), Map.class)
            .retrieve()
            .bodyToMono(Map.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .map(response -> {
                log.info("Password reset notification sent: {}", response.get("id"));
                return true;
            })
            .onErrorResume(error -> {
                log.error("Failed to send password reset notification", error);
                return Mono.just(false);
            });
    }

    /**
     * Send security alert for suspicious activity
     *
     * @param userId The user's ID
     * @param alertType Type of security alert
     * @param details Additional details about the security event
     */
    public void sendSecurityAlert(
            String userId,
            SecurityAlertType alertType,
            Map<String, Object> details) {

        log.warn("Sending security alert to user: {} for: {}", userId, alertType);

        String content = generateSecurityAlertContent(alertType, details);

        Map<String, Object> notificationRequest = Map.of(
            "userId", userId,
            "type", "SECURITY_ALERT",
            "priority", "URGENT",
            "title", "Security Alert: " + alertType.getDisplayName(),
            "content", content,
            "data", Map.of(
                "alertType", alertType.name(),
                "timestamp", Instant.now().toString(),
                "details", details,
                "actionRequired", alertType.isActionRequired()
            )
        );

        // For security alerts, we use a fire-and-forget approach with async processing
        sendAsyncNotification(notificationRequest);
    }

    /**
     * Send email verification link
     *
     * @param userId The user's ID
     * @param email The email address to verify
     * @param verificationToken The verification token
     */
    public void sendEmailVerification(String userId, String email, String verificationToken) {
        log.info("Sending email verification to: {}", email);

        Map<String, Object> notificationRequest = Map.of(
            "userId", userId,
            "type", "EMAIL_VERIFICATION",
            "priority", "HIGH",
            "title", "Verify Your Email Address",
            "content", String.format(
                "Please verify your email address to complete your registration.\n\n" +
                "Click here to verify: https://focushive.app/verify-email?token=%s\n\n" +
                "This link will expire in 24 hours.",
                verificationToken
            ),
            "data", Map.of(
                "email", email,
                "verificationToken", verificationToken,
                "expirationTime", Instant.now().plusHours(24).toString()
            )
        );

        sendNotificationWithFallback(notificationRequest);
    }

    /**
     * Send two-factor authentication code
     *
     * @param userId The user's ID
     * @param code The 2FA code
     * @param expirationMinutes Code expiration time
     */
    public void send2FACode(String userId, String code, int expirationMinutes) {
        log.info("Sending 2FA code to user: {}", userId);

        Map<String, Object> notificationRequest = Map.of(
            "userId", userId,
            "type", "TWO_FACTOR_AUTH",
            "priority", "URGENT",
            "title", "Your Authentication Code",
            "content", String.format(
                "Your authentication code is: %s\n\n" +
                "This code will expire in %d minutes.\n" +
                "Do not share this code with anyone.",
                code, expirationMinutes
            ),
            "data", Map.of(
                "expirationTime", Instant.now().plusMinutes(expirationMinutes).toString()
            )
        );

        // 2FA codes should be sent immediately
        sendImmediateNotification(notificationRequest);
    }

    /**
     * Send account deactivation confirmation
     *
     * @param userId The user's ID
     * @param email The user's email
     * @param reason Reason for deactivation
     */
    public void sendAccountDeactivated(String userId, String email, String reason) {
        log.info("Sending account deactivation notice to user: {}", userId);

        Map<String, Object> notificationRequest = Map.of(
            "userId", userId,
            "type", "ACCOUNT_DEACTIVATED",
            "priority", "HIGH",
            "title", "Account Deactivated",
            "content", String.format(
                "Your FocusHive account has been deactivated.\n\n" +
                "Reason: %s\n\n" +
                "If you believe this is an error or would like to reactivate your account, " +
                "please contact support at support@focushive.app",
                reason
            ),
            "data", Map.of(
                "email", email,
                "deactivationReason", reason,
                "deactivationTime", Instant.now().toString()
            )
        );

        sendNotificationWithFallback(notificationRequest);
    }

    // ============= Helper Methods =============

    private String getServiceUrl() {
        return useInternalUrl ? notificationServiceInternalUrl : notificationServiceUrl;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAuthToken());
        headers.set("X-Service-Name", "identity-service");
        headers.set("X-Correlation-Id", generateCorrelationId());
        return headers;
    }

    private String getAuthToken() {
        // In production, this would fetch a service token or use OAuth2 client credentials
        return serviceToken != null ? serviceToken : "service-token-placeholder";
    }

    private String generateCorrelationId() {
        return "identity-" + System.currentTimeMillis() + "-" +
               Thread.currentThread().getId();
    }

    private String getCurrentUserIpAddress() {
        // Implementation to get current request IP
        return "127.0.0.1"; // Placeholder
    }

    private String getCurrentUserAgent() {
        // Implementation to get current request user agent
        return "Mozilla/5.0"; // Placeholder
    }

    private String generateSecurityAlertContent(
            SecurityAlertType alertType,
            Map<String, Object> details) {

        StringBuilder content = new StringBuilder();
        content.append("A security event has been detected on your account:\n\n");

        switch (alertType) {
            case SUSPICIOUS_LOGIN:
                content.append("Suspicious login attempt detected from:\n");
                content.append("Location: ").append(details.get("location")).append("\n");
                content.append("IP Address: ").append(details.get("ipAddress")).append("\n");
                break;
            case PASSWORD_CHANGED:
                content.append("Your password has been changed.\n");
                content.append("If you didn't make this change, please contact support immediately.\n");
                break;
            case MULTIPLE_FAILED_LOGINS:
                content.append("Multiple failed login attempts detected.\n");
                content.append("Attempts: ").append(details.get("attemptCount")).append("\n");
                content.append("Your account may be temporarily locked for security.\n");
                break;
            case NEW_DEVICE_LOGIN:
                content.append("Login from a new device detected:\n");
                content.append("Device: ").append(details.get("deviceInfo")).append("\n");
                content.append("Location: ").append(details.get("location")).append("\n");
                break;
        }

        content.append("\nIf this was you, you can safely ignore this message.\n");
        content.append("Otherwise, please secure your account immediately.");

        return content.toString();
    }

    private void sendAsyncNotification(Map<String, Object> notificationRequest) {
        // Async implementation using CompletableFuture or @Async
        try {
            String url = getServiceUrl() + "/api/v1/notifications";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);

            restTemplate.postForEntity(url, entity, Map.class);
        } catch (Exception e) {
            log.error("Failed to send async notification", e);
        }
    }

    private void sendImmediateNotification(Map<String, Object> notificationRequest) {
        // Send with highest priority and no retry for time-sensitive notifications
        String url = getServiceUrl() + "/api/v1/notifications";
        HttpHeaders headers = createAuthHeaders();
        headers.set("X-Priority", "immediate");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);

        try {
            restTemplate.postForEntity(url, entity, Map.class);
        } catch (Exception e) {
            log.error("Critical: Failed to send immediate notification", e);
            // Alert monitoring system
        }
    }

    private void sendNotificationWithFallback(Map<String, Object> notificationRequest) {
        try {
            sendAsyncNotification(notificationRequest);
        } catch (Exception primary) {
            log.warn("Primary notification failed, attempting fallback", primary);
            try {
                // Fallback to queue-based delivery or alternative service
                queueNotificationForRetry(notificationRequest);
            } catch (Exception fallback) {
                log.error("Both primary and fallback notification failed", fallback);
            }
        }
    }

    private void queueNotificationForRetry(Map<String, Object> notificationRequest) {
        // Implementation to queue notification for later retry
        // This could use RabbitMQ, Redis, or database queue
        log.info("Notification queued for retry: {}", notificationRequest.get("userId"));
    }

    /**
     * Enum for different types of security alerts
     */
    public enum SecurityAlertType {
        SUSPICIOUS_LOGIN("Suspicious Login Detected", true),
        PASSWORD_CHANGED("Password Changed", false),
        MULTIPLE_FAILED_LOGINS("Multiple Failed Login Attempts", true),
        NEW_DEVICE_LOGIN("New Device Login", false),
        ACCOUNT_LOCKED("Account Locked", true),
        PRIVILEGE_ESCALATION("Privilege Escalation Attempt", true);

        private final String displayName;
        private final boolean actionRequired;

        SecurityAlertType(String displayName, boolean actionRequired) {
            this.displayName = displayName;
            this.actionRequired = actionRequired;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isActionRequired() {
            return actionRequired;
        }
    }
}