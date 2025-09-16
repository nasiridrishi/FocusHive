package com.focushive.identity.service;

import com.focushive.identity.dto.*;

import java.util.UUID;

/**
 * User Preferences Service.
 * Manages user preferences across different personas and contexts.
 */
public interface UserPreferencesService {

    /**
     * Get global preferences that apply across all personas.
     */
    GlobalPreferencesResponse getGlobalPreferences(UUID userId);

    /**
     * Update global preferences.
     */
    GlobalPreferencesResponse updateGlobalPreferences(UUID userId, UpdateGlobalPreferencesRequest request);

    /**
     * Get persona-specific preferences.
     */
    PersonaPreferencesResponse getPersonaPreferences(UUID userId, UUID personaId);

    /**
     * Update persona-specific preferences.
     */
    PersonaPreferencesResponse updatePersonaPreferences(UUID userId, UUID personaId, UpdatePersonaPreferencesRequest request);

    /**
     * Get preferences for the active persona.
     */
    PersonaPreferencesResponse getActivePersonaPreferences(UUID userId);

    /**
     * Get notification preferences.
     */
    NotificationPreferencesResponse getNotificationPreferences(UUID userId);

    /**
     * Update notification preferences.
     */
    NotificationPreferencesResponse updateNotificationPreferences(UUID userId, UpdateNotificationPreferencesRequest request);

    /**
     * Get accessibility preferences.
     */
    AccessibilityPreferencesResponse getAccessibilityPreferences(UUID userId);

    /**
     * Update accessibility preferences.
     */
    AccessibilityPreferencesResponse updateAccessibilityPreferences(UUID userId, UpdateAccessibilityPreferencesRequest request);

    /**
     * Get theme preferences.
     */
    ThemePreferencesResponse getThemePreferences(UUID userId);

    /**
     * Update theme preferences.
     */
    ThemePreferencesResponse updateThemePreferences(UUID userId, UpdateThemePreferencesRequest request);

    /**
     * Get integration preferences.
     */
    IntegrationPreferencesResponse getIntegrationPreferences(UUID userId);

    /**
     * Update integration preferences.
     */
    IntegrationPreferencesResponse updateIntegrationPreferences(UUID userId, UpdateIntegrationPreferencesRequest request);

    /**
     * Get preference schema.
     */
    PreferenceSchemaResponse getPreferenceSchema();

    /**
     * Reset preferences to defaults.
     */
    void resetPreferences(UUID userId, ResetPreferencesRequest request);

    /**
     * Copy preferences between personas.
     */
    void copyPreferencesBetweenPersonas(UUID userId, CopyPreferencesRequest request);

    /**
     * Export preferences.
     */
    PreferencesExportResponse exportPreferences(UUID userId, PreferencesExportRequest request);

    /**
     * Import preferences.
     */
    PreferencesImportResponse importPreferences(UUID userId, PreferencesImportRequest request);
}