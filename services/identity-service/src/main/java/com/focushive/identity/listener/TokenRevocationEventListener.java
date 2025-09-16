package com.focushive.identity.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.event.TokenRevocationEvent;
import com.focushive.identity.repository.OAuthAccessTokenRepository;
import com.focushive.identity.repository.OAuthRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for token revocation events.
 * Handles both local Spring events and distributed Redis pub/sub messages.
 */
@Component
@RequiredArgsConstructor
public class TokenRevocationEventListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationEventListener.class);

    private final OAuthAccessTokenRepository accessTokenRepository;
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    // Track processed events to prevent duplicates
    private final Set<String> processedEvents = new HashSet<>();
    private static final int MAX_PROCESSED_EVENTS = 10000; // Prevent memory leak

    /**
     * Handle local Spring application events.
     */
    @Async
    @EventListener
    @Transactional
    public void handleLocalRevocationEvent(TokenRevocationEvent event) {
        log.debug("Handling local revocation event: {}", event.getEventId());
        processRevocationEvent(event);
    }

    /**
     * Handle Redis pub/sub messages for distributed events.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            TokenRevocationEvent event = objectMapper.readValue(messageBody, TokenRevocationEvent.class);

            // Skip if this instance originated the event
            if (isOwnEvent(event)) {
                log.debug("Skipping own revocation event: {}", event.getEventId());
                return;
            }

            log.debug("Handling distributed revocation event: {}", event.getEventId());
            processRevocationEvent(event);

        } catch (Exception e) {
            log.error("Failed to process Redis revocation message", e);
        }
    }

    /**
     * Process a revocation event.
     */
    private void processRevocationEvent(TokenRevocationEvent event) {
        // Prevent duplicate processing
        if (!shouldProcessEvent(event.getEventId())) {
            log.debug("Skipping duplicate event: {}", event.getEventId());
            return;
        }

        try {
            switch (event.getTokenType()) {
                case ACCESS_TOKEN:
                    handleAccessTokenRevocation(event);
                    break;
                case REFRESH_TOKEN:
                    handleRefreshTokenRevocation(event);
                    break;
                case ALL_TOKENS:
                    handleAllTokensRevocation(event);
                    break;
                default:
                    log.warn("Unknown token type in revocation event: {}", event.getTokenType());
            }

            markEventProcessed(event.getEventId());
            log.info("Processed revocation event: {} for token type: {}",
                event.getEventId(), event.getTokenType());

        } catch (Exception e) {
            log.error("Failed to process revocation event: {}", event, e);
        }
    }

    /**
     * Handle access token revocation.
     */
    private void handleAccessTokenRevocation(TokenRevocationEvent event) {
        if (event.getTokenHash() != null) {
            // Mark token as revoked in database
            accessTokenRepository.findByTokenHash(event.getTokenHash())
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevoked(true);
                        token.setRevokedAt(event.getRevokedAt());
                        token.setRevocationReason(event.getRevocationReason());
                        accessTokenRepository.save(token);
                        log.debug("Revoked access token: {}", event.getTokenHash());
                    }
                });
        }
    }

    /**
     * Handle refresh token revocation with cascade.
     */
    private void handleRefreshTokenRevocation(TokenRevocationEvent event) {
        if (event.getTokenHash() != null) {
            // Mark refresh token as revoked
            refreshTokenRepository.findByTokenHash(event.getTokenHash())
                .ifPresent(refreshToken -> {
                    if (!refreshToken.isRevoked()) {
                        refreshToken.setRevoked(true);
                        refreshToken.setRevokedAt(event.getRevokedAt());
                        refreshToken.setRevocationReason(event.getRevocationReason());
                        refreshTokenRepository.save(refreshToken);
                        log.debug("Revoked refresh token: {}", event.getTokenHash());

                        // Cascade revocation to associated access tokens
                        if (event.isCascadeRevocation()) {
                            revokeAssociatedAccessTokens(refreshToken.getId(),
                                event.getRevokedAt(), "Refresh token revoked");
                        }
                    }
                });
        }
    }

    /**
     * Handle revocation of all tokens for a user/client.
     */
    private void handleAllTokensRevocation(TokenRevocationEvent event) {
        Instant revokedAt = event.getRevokedAt();
        String reason = event.getRevocationReason();

        // Revoke all access tokens
        if (event.getClientId() != null) {
            accessTokenRepository.revokeAllTokensForClient(
                event.getClientId(), revokedAt, reason);
            log.debug("Revoked all access tokens for client: {}", event.getClientId());
        }

        // Revoke all refresh tokens
        if (event.getClientId() != null) {
            refreshTokenRepository.revokeAllTokensForClient(
                event.getClientId(), revokedAt, reason);
            log.debug("Revoked all refresh tokens for client: {}", event.getClientId());
        }
    }

    /**
     * Revoke all access tokens associated with a refresh token.
     */
    private void revokeAssociatedAccessTokens(UUID refreshTokenId, Instant revokedAt, String reason) {
        accessTokenRepository.findByRefreshTokenId(refreshTokenId)
            .forEach(accessToken -> {
                if (!accessToken.isRevoked()) {
                    accessToken.setRevoked(true);
                    accessToken.setRevokedAt(revokedAt);
                    accessToken.setRevocationReason(reason);
                    accessTokenRepository.save(accessToken);
                }
            });
        log.debug("Revoked access tokens associated with refresh token: {}", refreshTokenId);
    }

    /**
     * Check if this instance originated the event.
     */
    private boolean isOwnEvent(TokenRevocationEvent event) {
        String instanceId = System.getProperty("instance.id",
            "identity-service-" + UUID.randomUUID().toString().substring(0, 8));
        return instanceId.equals(event.getSourceInstance());
    }

    /**
     * Check if event should be processed (prevent duplicates).
     */
    private synchronized boolean shouldProcessEvent(String eventId) {
        return !processedEvents.contains(eventId);
    }

    /**
     * Mark event as processed.
     */
    private synchronized void markEventProcessed(String eventId) {
        processedEvents.add(eventId);

        // Prevent memory leak by clearing old events
        if (processedEvents.size() > MAX_PROCESSED_EVENTS) {
            processedEvents.clear();
            log.info("Cleared processed events cache");
        }
    }
}