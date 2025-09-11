package com.focushive.identity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for UpdatePrivacyPreferencesRequest covering builder patterns,
 * validation, serialization/deserialization, equality, and edge cases.
 */
@DisplayName("UpdatePrivacyPreferencesRequest Unit Tests")
class UpdatePrivacyPreferencesRequestUnitTest {

    private ObjectMapper objectMapper;
    private Validator validator;
    private Map<String, Boolean> testConsentStatus;
    private Map<String, String> testDataSharingPreferences;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testConsentStatus = new HashMap<>();
        testConsentStatus.put("essential", true);
        testConsentStatus.put("functional", true);
        testConsentStatus.put("analytics", false);
        testConsentStatus.put("marketing", false);
        testConsentStatus.put("personalization", true);
        
        testDataSharingPreferences = new HashMap<>();
        testDataSharingPreferences.put("research", "opt-out");
        testDataSharingPreferences.put("partner-services", "opt-in");
        testDataSharingPreferences.put("advertising", "opt-out");
        testDataSharingPreferences.put("analytics", "minimal");
    }

    @Test
    @DisplayName("Should create UpdatePrivacyPreferencesRequest using builder with all fields")
    void shouldCreateUpdatePrivacyPreferencesRequestUsingBuilderWithAllFields() {
        // Given & When
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(testConsentStatus)
                .dataSharingPreferences(testDataSharingPreferences)
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(730)
                .autoDataDeletion(true)
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getConsentStatus()).isEqualTo(testConsentStatus);
        assertThat(request.getDataSharingPreferences()).isEqualTo(testDataSharingPreferences);
        assertThat(request.getMarketingCommunicationConsent()).isFalse();
        assertThat(request.getAnalyticsConsent()).isTrue();
        assertThat(request.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(request.getAccountVisibility()).isEqualTo("private");
        assertThat(request.getProfileVisibility()).isEqualTo("friends");
        assertThat(request.getActivityVisibility()).isEqualTo("private");
        assertThat(request.getDataRetentionDays()).isEqualTo(730);
        assertThat(request.getAutoDataDeletion()).isTrue();
    }

    @Test
    @DisplayName("Should create UpdatePrivacyPreferencesRequest with minimal fields")
    void shouldCreateUpdatePrivacyPreferencesRequestWithMinimalFields() {
        // Given & When
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(false)
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getConsentStatus()).isNull();
        assertThat(request.getDataSharingPreferences()).isNull();
        assertThat(request.getMarketingCommunicationConsent()).isFalse();
        assertThat(request.getAnalyticsConsent()).isNull();
        assertThat(request.getThirdPartyDataSharingConsent()).isNull();
        assertThat(request.getAccountVisibility()).isNull();
        assertThat(request.getProfileVisibility()).isNull();
        assertThat(request.getActivityVisibility()).isNull();
        assertThat(request.getDataRetentionDays()).isNull();
        assertThat(request.getAutoDataDeletion()).isNull();
    }

    @Test
    @DisplayName("Should create UpdatePrivacyPreferencesRequest using no-args constructor and setters")
    void shouldCreateUpdatePrivacyPreferencesRequestUsingNoArgsConstructorAndSetters() {
        // Given
        UpdatePrivacyPreferencesRequest request = new UpdatePrivacyPreferencesRequest();

        // When
        request.setConsentStatus(testConsentStatus);
        request.setDataSharingPreferences(testDataSharingPreferences);
        request.setMarketingCommunicationConsent(true);
        request.setAnalyticsConsent(false);
        request.setThirdPartyDataSharingConsent(true);
        request.setAccountVisibility("public");
        request.setProfileVisibility("public");
        request.setActivityVisibility("friends");
        request.setDataRetentionDays(365);
        request.setAutoDataDeletion(false);

        // Then
        assertThat(request.getConsentStatus()).isEqualTo(testConsentStatus);
        assertThat(request.getDataSharingPreferences()).isEqualTo(testDataSharingPreferences);
        assertThat(request.getMarketingCommunicationConsent()).isTrue();
        assertThat(request.getAnalyticsConsent()).isFalse();
        assertThat(request.getThirdPartyDataSharingConsent()).isTrue();
        assertThat(request.getAccountVisibility()).isEqualTo("public");
        assertThat(request.getProfileVisibility()).isEqualTo("public");
        assertThat(request.getActivityVisibility()).isEqualTo("friends");
        assertThat(request.getDataRetentionDays()).isEqualTo(365);
        assertThat(request.getAutoDataDeletion()).isFalse();
    }

    @Test
    @DisplayName("Should create UpdatePrivacyPreferencesRequest using all-args constructor")
    void shouldCreateUpdatePrivacyPreferencesRequestUsingAllArgsConstructor() {
        // Given & When
        UpdatePrivacyPreferencesRequest request = new UpdatePrivacyPreferencesRequest(
                testConsentStatus,
                testDataSharingPreferences,
                true,
                true,
                false,
                "friends",
                "private",
                "public",
                1095,
                true
        );

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getConsentStatus()).isEqualTo(testConsentStatus);
        assertThat(request.getDataSharingPreferences()).isEqualTo(testDataSharingPreferences);
        assertThat(request.getMarketingCommunicationConsent()).isTrue();
        assertThat(request.getAnalyticsConsent()).isTrue();
        assertThat(request.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(request.getAccountVisibility()).isEqualTo("friends");
        assertThat(request.getProfileVisibility()).isEqualTo("private");
        assertThat(request.getActivityVisibility()).isEqualTo("public");
        assertThat(request.getDataRetentionDays()).isEqualTo(1095);
        assertThat(request.getAutoDataDeletion()).isTrue();
    }

    @Test
    @DisplayName("Should validate successfully with valid data retention days")
    void shouldValidateSuccessfullyWithValidDataRetentionDays() {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(365) // Valid value between 30 and 2555
                .build();

        // When
        Set<ConstraintViolation<UpdatePrivacyPreferencesRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with data retention days below minimum")
    void shouldFailValidationWithDataRetentionDaysBelowMinimum() {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(15) // Below minimum of 30
                .build();

        // When
        Set<ConstraintViolation<UpdatePrivacyPreferencesRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<UpdatePrivacyPreferencesRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Data retention must be at least 30 days");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("dataRetentionDays");
    }

    @Test
    @DisplayName("Should fail validation with data retention days above maximum")
    void shouldFailValidationWithDataRetentionDaysAboveMaximum() {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(3000) // Above maximum of 2555 (7 years)
                .build();

        // When
        Set<ConstraintViolation<UpdatePrivacyPreferencesRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<UpdatePrivacyPreferencesRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Data retention cannot exceed 7 years");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("dataRetentionDays");
    }

    @Test
    @DisplayName("Should validate successfully with boundary data retention values")
    void shouldValidateSuccessfullyWithBoundaryDataRetentionValues() {
        // Test minimum boundary (30 days)
        UpdatePrivacyPreferencesRequest minRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(30)
                .build();
        Set<ConstraintViolation<UpdatePrivacyPreferencesRequest>> minViolations = validator.validate(minRequest);
        assertThat(minViolations).isEmpty();

        // Test maximum boundary (2555 days = 7 years)
        UpdatePrivacyPreferencesRequest maxRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(2555)
                .build();
        Set<ConstraintViolation<UpdatePrivacyPreferencesRequest>> maxViolations = validator.validate(maxRequest);
        assertThat(maxViolations).isEmpty();
    }

    @Test
    @DisplayName("Should serialize UpdatePrivacyPreferencesRequest to JSON correctly")
    void shouldSerializeUpdatePrivacyPreferencesRequestToJsonCorrectly() throws JsonProcessingException {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(testConsentStatus)
                .dataSharingPreferences(testDataSharingPreferences)
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(730)
                .autoDataDeletion(true)
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"consentStatus\":{");
        assertThat(json).contains("\"essential\":true");
        assertThat(json).contains("\"analytics\":false");
        assertThat(json).contains("\"dataSharingPreferences\":{");
        assertThat(json).contains("\"research\":\"opt-out\"");
        assertThat(json).contains("\"marketingCommunicationConsent\":false");
        assertThat(json).contains("\"analyticsConsent\":true");
        assertThat(json).contains("\"thirdPartyDataSharingConsent\":false");
        assertThat(json).contains("\"accountVisibility\":\"private\"");
        assertThat(json).contains("\"profileVisibility\":\"friends\"");
        assertThat(json).contains("\"activityVisibility\":\"private\"");
        assertThat(json).contains("\"dataRetentionDays\":730");
        assertThat(json).contains("\"autoDataDeletion\":true");
    }

    @Test
    @DisplayName("Should deserialize JSON to UpdatePrivacyPreferencesRequest correctly")
    void shouldDeserializeJsonToUpdatePrivacyPreferencesRequestCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "consentStatus": {
                        "essential": true,
                        "functional": false,
                        "analytics": true,
                        "marketing": false
                    },
                    "dataSharingPreferences": {
                        "research": "opt-in",
                        "advertising": "opt-out"
                    },
                    "marketingCommunicationConsent": true,
                    "analyticsConsent": false,
                    "thirdPartyDataSharingConsent": true,
                    "accountVisibility": "public",
                    "profileVisibility": "private",
                    "activityVisibility": "friends",
                    "dataRetentionDays": 1095,
                    "autoDataDeletion": false
                }
                """;

        // When
        UpdatePrivacyPreferencesRequest request = objectMapper.readValue(json, UpdatePrivacyPreferencesRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getConsentStatus()).hasSize(4);
        assertThat(request.getConsentStatus().get("essential")).isTrue();
        assertThat(request.getConsentStatus().get("functional")).isFalse();
        assertThat(request.getConsentStatus().get("analytics")).isTrue();
        assertThat(request.getConsentStatus().get("marketing")).isFalse();
        assertThat(request.getDataSharingPreferences()).hasSize(2);
        assertThat(request.getDataSharingPreferences().get("research")).isEqualTo("opt-in");
        assertThat(request.getDataSharingPreferences().get("advertising")).isEqualTo("opt-out");
        assertThat(request.getMarketingCommunicationConsent()).isTrue();
        assertThat(request.getAnalyticsConsent()).isFalse();
        assertThat(request.getThirdPartyDataSharingConsent()).isTrue();
        assertThat(request.getAccountVisibility()).isEqualTo("public");
        assertThat(request.getProfileVisibility()).isEqualTo("private");
        assertThat(request.getActivityVisibility()).isEqualTo("friends");
        assertThat(request.getDataRetentionDays()).isEqualTo(1095);
        assertThat(request.getAutoDataDeletion()).isFalse();
    }

    @Test
    @DisplayName("Should handle null values in serialization")
    void shouldHandleNullValuesInSerialization() throws JsonProcessingException {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(null)
                .dataSharingPreferences(null)
                .marketingCommunicationConsent(null)
                .analyticsConsent(null)
                .thirdPartyDataSharingConsent(null)
                .accountVisibility(null)
                .profileVisibility(null)
                .activityVisibility(null)
                .dataRetentionDays(null)
                .autoDataDeletion(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"consentStatus\":null");
        assertThat(json).contains("\"dataSharingPreferences\":null");
        assertThat(json).contains("\"marketingCommunicationConsent\":null");
        assertThat(json).contains("\"analyticsConsent\":null");
        assertThat(json).contains("\"thirdPartyDataSharingConsent\":null");
        assertThat(json).contains("\"accountVisibility\":null");
        assertThat(json).contains("\"profileVisibility\":null");
        assertThat(json).contains("\"activityVisibility\":null");
        assertThat(json).contains("\"dataRetentionDays\":null");
        assertThat(json).contains("\"autoDataDeletion\":null");
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        UpdatePrivacyPreferencesRequest request1 = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(testConsentStatus)
                .dataSharingPreferences(testDataSharingPreferences)
                .marketingCommunicationConsent(true)
                .analyticsConsent(false)
                .thirdPartyDataSharingConsent(true)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .build();

        UpdatePrivacyPreferencesRequest request2 = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(testConsentStatus)
                .dataSharingPreferences(testDataSharingPreferences)
                .marketingCommunicationConsent(true)
                .analyticsConsent(false)
                .thirdPartyDataSharingConsent(true)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .build();

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        UpdatePrivacyPreferencesRequest request1 = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(true)
                .accountVisibility("public")
                .dataRetentionDays(365)
                .build();

        UpdatePrivacyPreferencesRequest request2 = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(false)
                .accountVisibility("private")
                .dataRetentionDays(730)
                .build();

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(Map.of("essential", true, "analytics", false))
                .dataSharingPreferences(Map.of("research", "opt-out"))
                .marketingCommunicationConsent(true)
                .analyticsConsent(false)
                .thirdPartyDataSharingConsent(true)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("private")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .build();

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("UpdatePrivacyPreferencesRequest");
        assertThat(toString).contains("consentStatus=");
        assertThat(toString).contains("dataSharingPreferences=");
        assertThat(toString).contains("marketingCommunicationConsent=true");
        assertThat(toString).contains("analyticsConsent=false");
        assertThat(toString).contains("thirdPartyDataSharingConsent=true");
        assertThat(toString).contains("accountVisibility=private");
        assertThat(toString).contains("profileVisibility=friends");
        assertThat(toString).contains("activityVisibility=private");
        assertThat(toString).contains("dataRetentionDays=365");
        assertThat(toString).contains("autoDataDeletion=true");
    }

    @Test
    @DisplayName("Should handle all visibility setting values")
    void shouldHandleAllVisibilitySettingValues() {
        // Test all allowable visibility values: public, private, friends
        
        // Public visibility
        UpdatePrivacyPreferencesRequest publicRequest = UpdatePrivacyPreferencesRequest.builder()
                .accountVisibility("public")
                .profileVisibility("public")
                .activityVisibility("public")
                .build();
        assertThat(publicRequest.getAccountVisibility()).isEqualTo("public");
        assertThat(publicRequest.getProfileVisibility()).isEqualTo("public");
        assertThat(publicRequest.getActivityVisibility()).isEqualTo("public");
        
        // Private visibility
        UpdatePrivacyPreferencesRequest privateRequest = UpdatePrivacyPreferencesRequest.builder()
                .accountVisibility("private")
                .profileVisibility("private")
                .activityVisibility("private")
                .build();
        assertThat(privateRequest.getAccountVisibility()).isEqualTo("private");
        assertThat(privateRequest.getProfileVisibility()).isEqualTo("private");
        assertThat(privateRequest.getActivityVisibility()).isEqualTo("private");
        
        // Friends visibility
        UpdatePrivacyPreferencesRequest friendsRequest = UpdatePrivacyPreferencesRequest.builder()
                .accountVisibility("friends")
                .profileVisibility("friends")
                .activityVisibility("friends")
                .build();
        assertThat(friendsRequest.getAccountVisibility()).isEqualTo("friends");
        assertThat(friendsRequest.getProfileVisibility()).isEqualTo("friends");
        assertThat(friendsRequest.getActivityVisibility()).isEqualTo("friends");
        
        // Mixed visibility settings
        UpdatePrivacyPreferencesRequest mixedRequest = UpdatePrivacyPreferencesRequest.builder()
                .accountVisibility("public")
                .profileVisibility("private")
                .activityVisibility("friends")
                .build();
        assertThat(mixedRequest.getAccountVisibility()).isEqualTo("public");
        assertThat(mixedRequest.getProfileVisibility()).isEqualTo("private");
        assertThat(mixedRequest.getActivityVisibility()).isEqualTo("friends");
    }

    @Test
    @DisplayName("Should handle various consent combinations")
    void shouldHandleVariousConsentCombinations() {
        // All consents true
        UpdatePrivacyPreferencesRequest allTrueRequest = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(true)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(true)
                .autoDataDeletion(true)
                .build();
        assertThat(allTrueRequest.getMarketingCommunicationConsent()).isTrue();
        assertThat(allTrueRequest.getAnalyticsConsent()).isTrue();
        assertThat(allTrueRequest.getThirdPartyDataSharingConsent()).isTrue();
        assertThat(allTrueRequest.getAutoDataDeletion()).isTrue();
        
        // All consents false
        UpdatePrivacyPreferencesRequest allFalseRequest = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(false)
                .analyticsConsent(false)
                .thirdPartyDataSharingConsent(false)
                .autoDataDeletion(false)
                .build();
        assertThat(allFalseRequest.getMarketingCommunicationConsent()).isFalse();
        assertThat(allFalseRequest.getAnalyticsConsent()).isFalse();
        assertThat(allFalseRequest.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(allFalseRequest.getAutoDataDeletion()).isFalse();
        
        // Mixed consents
        UpdatePrivacyPreferencesRequest mixedRequest = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(true)
                .analyticsConsent(false)
                .thirdPartyDataSharingConsent(true)
                .autoDataDeletion(false)
                .build();
        assertThat(mixedRequest.getMarketingCommunicationConsent()).isTrue();
        assertThat(mixedRequest.getAnalyticsConsent()).isFalse();
        assertThat(mixedRequest.getThirdPartyDataSharingConsent()).isTrue();
        assertThat(mixedRequest.getAutoDataDeletion()).isFalse();
        
        // All null consents
        UpdatePrivacyPreferencesRequest nullRequest = UpdatePrivacyPreferencesRequest.builder()
                .marketingCommunicationConsent(null)
                .analyticsConsent(null)
                .thirdPartyDataSharingConsent(null)
                .autoDataDeletion(null)
                .build();
        assertThat(nullRequest.getMarketingCommunicationConsent()).isNull();
        assertThat(nullRequest.getAnalyticsConsent()).isNull();
        assertThat(nullRequest.getThirdPartyDataSharingConsent()).isNull();
        assertThat(nullRequest.getAutoDataDeletion()).isNull();
    }

    @Test
    @DisplayName("Should handle complex consent status maps")
    void shouldHandleComplexConsentStatusMaps() {
        // Empty consent status
        Map<String, Boolean> emptyConsent = new HashMap<>();
        UpdatePrivacyPreferencesRequest emptyRequest = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(emptyConsent)
                .build();
        assertThat(emptyRequest.getConsentStatus()).isNotNull();
        assertThat(emptyRequest.getConsentStatus()).isEmpty();
        
        // Large consent status map
        Map<String, Boolean> largeConsent = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            largeConsent.put("consent" + i, i % 2 == 0);
        }
        UpdatePrivacyPreferencesRequest largeRequest = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(largeConsent)
                .build();
        assertThat(largeRequest.getConsentStatus()).hasSize(20);
        assertThat(largeRequest.getConsentStatus().get("consent0")).isTrue();
        assertThat(largeRequest.getConsentStatus().get("consent1")).isFalse();
        
        // Complex key names
        Map<String, Boolean> complexConsent = new HashMap<>();
        complexConsent.put("analytics-tracking", true);
        complexConsent.put("marketing_email", false);
        complexConsent.put("third.party.sharing", true);
        complexConsent.put("essential cookies", true);
        UpdatePrivacyPreferencesRequest complexRequest = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(complexConsent)
                .build();
        assertThat(complexRequest.getConsentStatus().get("analytics-tracking")).isTrue();
        assertThat(complexRequest.getConsentStatus().get("marketing_email")).isFalse();
        assertThat(complexRequest.getConsentStatus().get("third.party.sharing")).isTrue();
        assertThat(complexRequest.getConsentStatus().get("essential cookies")).isTrue();
    }

    @Test
    @DisplayName("Should handle complex data sharing preferences")
    void shouldHandleComplexDataSharingPreferences() {
        // Empty data sharing preferences
        Map<String, String> emptySharing = new HashMap<>();
        UpdatePrivacyPreferencesRequest emptyRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataSharingPreferences(emptySharing)
                .build();
        assertThat(emptyRequest.getDataSharingPreferences()).isNotNull();
        assertThat(emptyRequest.getDataSharingPreferences()).isEmpty();
        
        // Various preference values
        Map<String, String> variousSharing = new HashMap<>();
        variousSharing.put("research", "opt-in");
        variousSharing.put("advertising", "opt-out");
        variousSharing.put("analytics", "minimal");
        variousSharing.put("partners", "anonymized");
        variousSharing.put("government", "never");
        variousSharing.put("academic", "aggregated");
        
        UpdatePrivacyPreferencesRequest variousRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataSharingPreferences(variousSharing)
                .build();
        assertThat(variousRequest.getDataSharingPreferences()).hasSize(6);
        assertThat(variousRequest.getDataSharingPreferences().get("research")).isEqualTo("opt-in");
        assertThat(variousRequest.getDataSharingPreferences().get("advertising")).isEqualTo("opt-out");
        assertThat(variousRequest.getDataSharingPreferences().get("analytics")).isEqualTo("minimal");
        assertThat(variousRequest.getDataSharingPreferences().get("partners")).isEqualTo("anonymized");
        assertThat(variousRequest.getDataSharingPreferences().get("government")).isEqualTo("never");
        assertThat(variousRequest.getDataSharingPreferences().get("academic")).isEqualTo("aggregated");
    }

    @Test
    @DisplayName("Should handle edge cases for data retention")
    void shouldHandleEdgeCasesForDataRetention() {
        // Exact minimum value (30 days)
        UpdatePrivacyPreferencesRequest minRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(30)
                .build();
        assertThat(minRequest.getDataRetentionDays()).isEqualTo(30);
        
        // Exact maximum value (2555 days = 7 years)
        UpdatePrivacyPreferencesRequest maxRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(2555)
                .build();
        assertThat(maxRequest.getDataRetentionDays()).isEqualTo(2555);
        
        // Common values
        UpdatePrivacyPreferencesRequest oneYearRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(365) // 1 year
                .build();
        assertThat(oneYearRequest.getDataRetentionDays()).isEqualTo(365);
        
        UpdatePrivacyPreferencesRequest twoYearRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(730) // 2 years
                .build();
        assertThat(twoYearRequest.getDataRetentionDays()).isEqualTo(730);
        
        UpdatePrivacyPreferencesRequest fiveYearRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(1825) // 5 years
                .build();
        assertThat(fiveYearRequest.getDataRetentionDays()).isEqualTo(1825);
        
        // Null value (no specific retention preference)
        UpdatePrivacyPreferencesRequest nullRequest = UpdatePrivacyPreferencesRequest.builder()
                .dataRetentionDays(null)
                .build();
        assertThat(nullRequest.getDataRetentionDays()).isNull();
    }

    @Test
    @DisplayName("Should handle collection references correctly")
    void shouldHandleCollectionReferencesCorrectly() {
        // Given
        Map<String, Boolean> originalConsent = new HashMap<>(testConsentStatus);
        Map<String, String> originalSharing = new HashMap<>(testDataSharingPreferences);

        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(originalConsent)
                .dataSharingPreferences(originalSharing)
                .build();

        // When & Then - verify maps are properly set
        assertThat(request.getConsentStatus()).hasSize(testConsentStatus.size());
        assertThat(request.getDataSharingPreferences()).hasSize(testDataSharingPreferences.size());
        assertThat(request.getConsentStatus()).containsAllEntriesOf(testConsentStatus);
        assertThat(request.getDataSharingPreferences()).containsAllEntriesOf(testDataSharingPreferences);
    }

    @Test
    @DisplayName("Should handle special characters and unicode in map keys and values")
    void shouldHandleSpecialCharactersAndUnicodeInMapKeysAndValues() {
        // Given
        Map<String, Boolean> unicodeConsent = new HashMap<>();
        unicodeConsent.put("consentimento-essencial", true);
        unicodeConsent.put("同意_分析", false);
        unicodeConsent.put("согласие-маркетинг", true);
        unicodeConsent.put("موافقة_الإعلان", false);
        
        Map<String, String> unicodeSharing = new HashMap<>();
        unicodeSharing.put("recherche", "opt-in");
        unicodeSharing.put("研究", "opt-out");
        unicodeSharing.put("исследования", "minimal");
        unicodeSharing.put("البحث", "never");

        // When
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(unicodeConsent)
                .dataSharingPreferences(unicodeSharing)
                .accountVisibility("público")
                .profileVisibility("プライベート")
                .activityVisibility("друзья")
                .build();

        // Then
        assertThat(request.getConsentStatus()).containsKey("consentimento-essencial");
        assertThat(request.getConsentStatus()).containsKey("同意_分析");
        assertThat(request.getConsentStatus()).containsKey("согласие-маркетинг");
        assertThat(request.getConsentStatus()).containsKey("موافقة_الإعلان");
        assertThat(request.getDataSharingPreferences()).containsKey("recherche");
        assertThat(request.getDataSharingPreferences()).containsKey("研究");
        assertThat(request.getDataSharingPreferences()).containsKey("исследования");
        assertThat(request.getDataSharingPreferences()).containsKey("البحث");
        assertThat(request.getAccountVisibility()).isEqualTo("público");
        assertThat(request.getProfileVisibility()).isEqualTo("プライベート");
        assertThat(request.getActivityVisibility()).isEqualTo("друзья");
    }

    @Test
    @DisplayName("Should handle builder chaining correctly")
    void shouldHandleBuilderChainingCorrectly() {
        // Test builder reuse
        UpdatePrivacyPreferencesRequest.UpdatePrivacyPreferencesRequestBuilder builder = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(testConsentStatus)
                .accountVisibility("private");

        UpdatePrivacyPreferencesRequest request1 = builder.marketingCommunicationConsent(true).build();
        UpdatePrivacyPreferencesRequest request2 = builder.marketingCommunicationConsent(false).build();

        assertThat(request1.getMarketingCommunicationConsent()).isTrue();
        assertThat(request2.getMarketingCommunicationConsent()).isFalse();
        assertThat(request1.getConsentStatus()).isEqualTo(request2.getConsentStatus());
        assertThat(request1.getAccountVisibility()).isEqualTo(request2.getAccountVisibility());
    }

    @Test
    @DisplayName("Should handle empty string visibility values")
    void shouldHandleEmptyStringVisibilityValues() {
        // Given & When
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .accountVisibility("")
                .profileVisibility("")
                .activityVisibility("")
                .build();

        // Then
        assertThat(request.getAccountVisibility()).isEmpty();
        assertThat(request.getProfileVisibility()).isEmpty();
        assertThat(request.getActivityVisibility()).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex nested scenarios")
    void shouldHandleComplexNestedScenarios() {
        // Given - Complex real-world scenario
        Map<String, Boolean> detailedConsent = new HashMap<>();
        detailedConsent.put("essential-cookies", true);
        detailedConsent.put("functional-cookies", true);
        detailedConsent.put("analytics-cookies", false);
        detailedConsent.put("marketing-cookies", false);
        detailedConsent.put("personalization-cookies", true);
        detailedConsent.put("social-media-integration", false);
        detailedConsent.put("third-party-advertising", false);
        detailedConsent.put("cross-site-tracking", false);
        
        Map<String, String> detailedSharing = new HashMap<>();
        detailedSharing.put("academic-research", "aggregated-only");
        detailedSharing.put("commercial-research", "opt-out");
        detailedSharing.put("government-requests", "legal-only");
        detailedSharing.put("law-enforcement", "warrant-required");
        detailedSharing.put("partner-services", "anonymized");
        detailedSharing.put("advertising-networks", "never");
        detailedSharing.put("data-brokers", "never");
        detailedSharing.put("analytics-providers", "minimal");

        // When
        UpdatePrivacyPreferencesRequest request = UpdatePrivacyPreferencesRequest.builder()
                .consentStatus(detailedConsent)
                .dataSharingPreferences(detailedSharing)
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("friends")
                .profileVisibility("private")
                .activityVisibility("friends")
                .dataRetentionDays(1095) // 3 years
                .autoDataDeletion(true)
                .build();

        // Then
        assertThat(request.getConsentStatus()).hasSize(8);
        assertThat(request.getConsentStatus().get("essential-cookies")).isTrue();
        assertThat(request.getConsentStatus().get("analytics-cookies")).isFalse();
        assertThat(request.getConsentStatus().get("third-party-advertising")).isFalse();
        
        assertThat(request.getDataSharingPreferences()).hasSize(8);
        assertThat(request.getDataSharingPreferences().get("academic-research")).isEqualTo("aggregated-only");
        assertThat(request.getDataSharingPreferences().get("advertising-networks")).isEqualTo("never");
        assertThat(request.getDataSharingPreferences().get("law-enforcement")).isEqualTo("warrant-required");
        
        assertThat(request.getMarketingCommunicationConsent()).isFalse();
        assertThat(request.getAnalyticsConsent()).isTrue();
        assertThat(request.getThirdPartyDataSharingConsent()).isFalse();
        assertThat(request.getAccountVisibility()).isEqualTo("friends");
        assertThat(request.getProfileVisibility()).isEqualTo("private");
        assertThat(request.getActivityVisibility()).isEqualTo("friends");
        assertThat(request.getDataRetentionDays()).isEqualTo(1095);
        assertThat(request.getAutoDataDeletion()).isTrue();
    }
}