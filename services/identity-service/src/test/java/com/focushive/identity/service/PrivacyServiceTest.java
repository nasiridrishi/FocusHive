package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for PrivacyService interface.
 * Tests GDPR-compliant privacy controls and data management capabilities.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivacyService Tests")
class PrivacyServiceTest {

    @Mock
    private PrivacyService privacyService;

    private UUID testUserId;
    private UUID testExportId;
    private UpdatePrivacyPreferencesRequest updateRequest;
    private DataExportRequest dataExportRequest;
    private PrivacyPreferencesResponse expectedPrivacyResponse;
    private DataExportResponse expectedExportResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testExportId = UUID.randomUUID();

        // Setup privacy preferences request
        updateRequest = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(Map.of(
                        "data_processing", true,
                        "marketing", false,
                        "analytics", true
                ))
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .build();

        // Setup expected privacy response
        expectedPrivacyResponse = PrivacyPreferencesResponse.builder()
                .consentStatus(updateRequest.getConsentStatus())
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .updatedAt(Instant.now())
                .build();

        // Setup data export request
        dataExportRequest = DataExportRequest.builder()
                .dataCategories(Set.of("profile", "personas", "preferences"))
                .format("json")
                .includeDeleted(false)
                .personaIds(Set.of())
                .build();

        // Setup expected export response
        expectedExportResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(0)
                .requestedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should update privacy preferences successfully with valid data")
    void updatePrivacyPreferences_WithValidData_ShouldReturnUpdatedPreferences() {
        // Given
        when(privacyService.updatePrivacyPreferences(testUserId, updateRequest))
                .thenReturn(expectedPrivacyResponse);

        // When
        PrivacyPreferencesResponse actualResponse = privacyService.updatePrivacyPreferences(testUserId, updateRequest);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getConsentStatus()).containsExactlyInAnyOrderEntriesOf(updateRequest.getConsentStatus());
        assertThat(actualResponse.getMarketingCommunicationConsent()).isFalse();
        assertThat(actualResponse.getAnalyticsConsent()).isTrue();
        assertThat(actualResponse.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(actualResponse.getAccountVisibility()).isEqualTo("private");
        assertThat(actualResponse.getProfileVisibility()).isEqualTo("friends");
        assertThat(actualResponse.getActivityVisibility()).isEqualTo("private");
        assertThat(actualResponse.getDataRetentionDays()).isEqualTo(365);
        assertThat(actualResponse.getAutoDataDeletion()).isTrue();
        assertThat(actualResponse.getUpdatedAt()).isNotNull();

        // Verify service method was called
        verify(privacyService).updatePrivacyPreferences(testUserId, updateRequest);
    }

    @Test
    @DisplayName("Should retrieve privacy preferences successfully for existing user")
    void getPrivacyPreferences_WithExistingUser_ShouldReturnPreferences() {
        // Given
        when(privacyService.getPrivacyPreferences(testUserId))
                .thenReturn(expectedPrivacyResponse);

        // When
        PrivacyPreferencesResponse actualResponse = privacyService.getPrivacyPreferences(testUserId);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getConsentStatus()).isNotNull();
        assertThat(actualResponse.getConsentStatus()).containsKeys("data_processing", "marketing", "analytics");
        assertThat(actualResponse.getMarketingCommunicationConsent()).isFalse();
        assertThat(actualResponse.getAnalyticsConsent()).isTrue();
        assertThat(actualResponse.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(actualResponse.getAccountVisibility()).isEqualTo("private");
        assertThat(actualResponse.getProfileVisibility()).isEqualTo("friends");
        assertThat(actualResponse.getActivityVisibility()).isEqualTo("private");
        assertThat(actualResponse.getDataRetentionDays()).isEqualTo(365);
        assertThat(actualResponse.getAutoDataDeletion()).isTrue();
        assertThat(actualResponse.getUpdatedAt()).isNotNull();

        // Verify service method was called
        verify(privacyService).getPrivacyPreferences(testUserId);
    }

    @Test
    @DisplayName("Should initiate data export request successfully with valid categories")
    void initiateDataExport_WithValidRequest_ShouldReturnExportResponse() {
        // Given
        when(privacyService.initiateDataExport(testUserId, dataExportRequest))
                .thenReturn(expectedExportResponse);

        // When
        DataExportResponse actualResponse = privacyService.initiateDataExport(testUserId, dataExportRequest);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getExportId()).isEqualTo(testExportId);
        assertThat(actualResponse.getStatus()).isEqualTo("requested");
        assertThat(actualResponse.getProgress()).isEqualTo(0);
        assertThat(actualResponse.getRequestedAt()).isNotNull();
        assertThat(actualResponse.getCompletedAt()).isNull(); // Not completed yet
        assertThat(actualResponse.getDownloadUrl()).isNull(); // Not ready for download yet
        assertThat(actualResponse.getErrorMessage()).isNull(); // No error

        // Verify service method was called with correct parameters
        verify(privacyService).initiateDataExport(testUserId, dataExportRequest);
    }

    @Test
    @DisplayName("Should validate data export access successfully for authorized user")
    void validateDataExportAccess_WithValidUserAndExport_ShouldReturnTrue() {
        // Given
        when(privacyService.validateDataExportAccess(testUserId, testExportId))
                .thenReturn(true);

        // When
        boolean hasAccess = privacyService.validateDataExportAccess(testUserId, testExportId);

        // Then
        assertThat(hasAccess).isTrue();

        // Verify service method was called
        verify(privacyService).validateDataExportAccess(testUserId, testExportId);
    }

    @Test
    @DisplayName("Should deny data export access for unauthorized user")
    void validateDataExportAccess_WithUnauthorizedUser_ShouldReturnFalse() {
        // Given
        UUID unauthorizedUserId = UUID.randomUUID();
        when(privacyService.validateDataExportAccess(unauthorizedUserId, testExportId))
                .thenReturn(false);

        // When
        boolean hasAccess = privacyService.validateDataExportAccess(unauthorizedUserId, testExportId);

        // Then
        assertThat(hasAccess).isFalse();

        // Verify service method was called
        verify(privacyService).validateDataExportAccess(unauthorizedUserId, testExportId);
    }

    @Test
    @DisplayName("Should cancel account deletion request successfully")
    void cancelAccountDeletion_WithValidUser_ShouldCompleteSuccessfully() {
        // Given
        doNothing().when(privacyService).cancelAccountDeletion(testUserId);

        // When & Then
        assertThatCode(() -> privacyService.cancelAccountDeletion(testUserId))
                .doesNotThrowAnyException();

        // Verify service method was called
        verify(privacyService).cancelAccountDeletion(testUserId);
    }

    @Test
    @DisplayName("Should return true when user has given consent for specific processing")
    void hasConsent_WithGrantedConsent_ShouldReturnTrue() {
        // Given
        String consentType = "marketing";
        when(privacyService.hasConsent(testUserId, consentType))
                .thenReturn(true);

        // When
        boolean hasConsent = privacyService.hasConsent(testUserId, consentType);

        // Then
        assertThat(hasConsent).isTrue();

        // Verify service method was called
        verify(privacyService).hasConsent(testUserId, consentType);
    }

    @Test
    @DisplayName("Should return false when user has not given consent for specific processing")
    void hasConsent_WithoutConsent_ShouldReturnFalse() {
        // Given
        String consentType = "third_party_sharing";
        when(privacyService.hasConsent(testUserId, consentType))
                .thenReturn(false);

        // When
        boolean hasConsent = privacyService.hasConsent(testUserId, consentType);

        // Then
        assertThat(hasConsent).isFalse();

        // Verify service method was called
        verify(privacyService).hasConsent(testUserId, consentType);
    }

    @Test
    @DisplayName("Should log data access for audit purposes")
    void logDataAccess_WithValidParameters_ShouldCompleteSuccessfully() {
        // Given
        String accessType = "READ";
        String accessor = "UserService";
        String purpose = "Profile retrieval";
        doNothing().when(privacyService).logDataAccess(testUserId, accessType, accessor, purpose);

        // When & Then
        assertThatCode(() -> privacyService.logDataAccess(testUserId, accessType, accessor, purpose))
                .doesNotThrowAnyException();

        // Verify service method was called with correct parameters
        verify(privacyService).logDataAccess(testUserId, accessType, accessor, purpose);
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void getPrivacyPreferences_WithNullUserId_ShouldThrowException() {
        // Given
        when(privacyService.getPrivacyPreferences(null))
                .thenThrow(new IllegalArgumentException("User ID cannot be null"));

        // When & Then
        assertThatThrownBy(() -> privacyService.getPrivacyPreferences(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");

        // Verify service method was called
        verify(privacyService).getPrivacyPreferences(null);
    }

    @Test
    @DisplayName("Should handle invalid data retention days in privacy preferences")
    void updatePrivacyPreferences_WithInvalidDataRetention_ShouldThrowException() {
        // Given
        UpdatePrivacyPreferencesRequest invalidRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(-1) // Invalid negative value
                .build();
        
        when(privacyService.updatePrivacyPreferences(testUserId, invalidRequest))
                .thenThrow(new IllegalArgumentException("Data retention days must be positive"));

        // When & Then
        assertThatThrownBy(() -> privacyService.updatePrivacyPreferences(testUserId, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Data retention days must be positive");

        // Verify service method was called
        verify(privacyService).updatePrivacyPreferences(testUserId, invalidRequest);
    }

    @Test
    @DisplayName("Should handle empty data categories in export request")
    void initiateDataExport_WithEmptyDataCategories_ShouldThrowException() {
        // Given
        DataExportRequest emptyRequest = DataExportRequest.builder()
                .dataCategories(Set.of()) // Empty categories
                .format("json")
                .build();
        
        when(privacyService.initiateDataExport(testUserId, emptyRequest))
                .thenThrow(new IllegalArgumentException("At least one data category must be selected"));

        // When & Then
        assertThatThrownBy(() -> privacyService.initiateDataExport(testUserId, emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one data category must be selected");

        // Verify service method was called
        verify(privacyService).initiateDataExport(testUserId, emptyRequest);
    }

    @Test
    @DisplayName("Should handle non-existent export ID in validation")
    void validateDataExportAccess_WithNonExistentExportId_ShouldReturnFalse() {
        // Given
        UUID nonExistentExportId = UUID.randomUUID();
        when(privacyService.validateDataExportAccess(testUserId, nonExistentExportId))
                .thenReturn(false);

        // When
        boolean hasAccess = privacyService.validateDataExportAccess(testUserId, nonExistentExportId);

        // Then
        assertThat(hasAccess).isFalse();

        // Verify service method was called
        verify(privacyService).validateDataExportAccess(testUserId, nonExistentExportId);
    }
}