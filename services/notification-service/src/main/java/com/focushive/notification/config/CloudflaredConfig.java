package com.focushive.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Cloudflared tunnel integration.
 * Manages service URLs based on whether Cloudflared is enabled.
 *
 * When Cloudflared is enabled, services communicate through public URLs.
 * When disabled, services use internal Docker network names.
 */
@Configuration
public class CloudflaredConfig {

    @Value("${cloudflared.enabled:false}")
    private boolean cloudflaredEnabled;

    @Value("${cloudflared.public.url:https://notification.focushive.app}")
    private String publicUrl;

    @Value("${identity.service.url:https://identity.focushive.app}")
    private String identityServiceUrl;

    @Value("${backend.service.url:https://backend.focushive.app}")
    private String backendServiceUrl;

    @Value("${buddy.service.url:https://buddy.focushive.app}")
    private String buddyServiceUrl;

    @Value("${frontend.url:https://focushive.app}")
    private String frontendUrl;

    /**
     * Get the appropriate identity service URL based on Cloudflared configuration.
     * @return The identity service URL (public if Cloudflared enabled, internal otherwise)
     */
    public String getIdentityServiceUrl() {
        if (cloudflaredEnabled) {
            return identityServiceUrl;
        }
        // Use internal Docker service name when Cloudflared is disabled
        return "http://focushive-identity-service-app:8081";
    }

    /**
     * Get the appropriate backend service URL based on Cloudflared configuration.
     * @return The backend service URL (public if Cloudflared enabled, internal otherwise)
     */
    public String getBackendServiceUrl() {
        if (cloudflaredEnabled) {
            return backendServiceUrl;
        }
        // Use internal Docker service name when Cloudflared is disabled
        return "http://focushive-backend-app:8080";
    }

    /**
     * Get the appropriate buddy service URL based on Cloudflared configuration.
     * @return The buddy service URL (public if Cloudflared enabled, internal otherwise)
     */
    public String getBuddyServiceUrl() {
        if (cloudflaredEnabled) {
            return buddyServiceUrl;
        }
        // Use internal Docker service name when Cloudflared is disabled
        return "http://focushive-buddy-app:8087";
    }

    /**
     * Get the appropriate frontend URL based on Cloudflared configuration.
     * @return The frontend URL (public if Cloudflared enabled, internal otherwise)
     */
    public String getFrontendUrl() {
        if (cloudflaredEnabled) {
            return frontendUrl;
        }
        // Use internal Docker service name when Cloudflared is disabled
        return "http://focushive-frontend:3000";
    }

    /**
     * Get the public URL for this notification service.
     * @return The public URL for the notification service
     */
    public String getPublicUrl() {
        return publicUrl;
    }

    /**
     * Check if Cloudflared tunnel is enabled.
     * @return true if Cloudflared is enabled, false otherwise
     */
    public boolean isCloudflaredEnabled() {
        return cloudflaredEnabled;
    }
}