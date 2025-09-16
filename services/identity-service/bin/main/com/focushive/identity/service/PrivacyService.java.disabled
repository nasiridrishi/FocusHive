package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Privacy and Data Management Service.
 * Provides GDPR-compliant privacy controls and data management capabilities.
 */
public interface PrivacyService {

    /**
     * Get user's privacy preferences.
     */
    PrivacyPreferencesResponse getPrivacyPreferences(UUID userId);

    /**
     * Update user's privacy preferences.
     */
    PrivacyPreferencesResponse updatePrivacyPreferences(UUID userId, UpdatePrivacyPreferencesRequest request);

    /**
     * Initiate data export process.
     * GDPR Article 20 - Right to Data Portability.
     */
    DataExportResponse initiateDataExport(UUID userId, DataExportRequest request);

    /**
     * Download exported data file.
     */
    void downloadDataExport(UUID userId, UUID exportId, HttpServletResponse response) throws IOException;

    /**
     * Get data export history for user.
     */
    DataExportHistoryResponse getDataExportHistory(UUID userId);

    /**
     * Request account deletion.
     * GDPR Article 17 - Right to Erasure.
     */
    DataDeletionResponse requestAccountDeletion(UUID userId, DataDeletionRequest request);

    /**
     * Cancel pending account deletion request.
     */
    void cancelAccountDeletion(UUID userId);

    /**
     * Get data access log.
     * GDPR Article 15 - Right of Access.
     */
    DataAccessLogResponse getDataAccessLog(UUID userId, int page, int size);

    /**
     * Grant consent for data processing.
     */
    ConsentResponse grantConsent(UUID userId, GrantConsentRequest request);

    /**
     * Revoke previously granted consent.
     */
    ConsentResponse revokeConsent(UUID userId, RevokeConsentRequest request);

    /**
     * Get consent history for user.
     */
    ConsentHistoryResponse getConsentHistory(UUID userId, int page, int size);

    /**
     * Request data rectification.
     * GDPR Article 16 - Right to Rectification.
     */
    DataRectificationResponse requestDataRectification(UUID userId, DataRectificationRequest request);

    /**
     * Get information about data processing activities.
     * GDPR Articles 13-14 - Information to be provided.
     */
    DataProcessingResponse getDataProcessingActivities(UUID userId);

    /**
     * Object to data processing.
     * GDPR Article 21 - Right to Object.
     */
    DataProcessingObjectionResponse objectToDataProcessing(UUID userId, DataProcessingObjectionRequest request);

    /**
     * Log data access for audit purposes.
     */
    void logDataAccess(UUID userId, String accessType, String accessor, String purpose);

    /**
     * Check if user has given consent for specific processing.
     */
    boolean hasConsent(UUID userId, String consentType);

    /**
     * Process scheduled data deletion requests.
     */
    void processScheduledDataDeletions();

    /**
     * Generate data export file for user.
     */
    void generateDataExportFile(UUID userId, DataExportRequest request, UUID exportId);

    /**
     * Validate data export access.
     */
    boolean validateDataExportAccess(UUID userId, UUID exportId);
}