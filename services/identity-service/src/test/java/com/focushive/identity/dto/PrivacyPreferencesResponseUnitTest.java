package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for PrivacyPreferencesResponse DTO.
 */
@DisplayName("PrivacyPreferencesResponse DTO Unit Tests")
class PrivacyPreferencesResponseUnitTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create PrivacyPreferencesResponse using builder")
        void shouldCreatePrivacyPreferencesResponseUsingBuilder() {
            // Given
            Map<String, Boolean> consentStatus = Map.of(
                    "data_processing", true,
                    "email_communication", false
            );
            Map<String, String> dataSharingPreferences = Map.of(
                    "analytics", "restricted",
                    "marketing", "disabled"
            );
            Instant now = Instant.now();

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(consentStatus)
                    .dataSharingPreferences(dataSharingPreferences)
                    .marketingCommunicationConsent(false)
                    .analyticsConsent(true)
                    .thirdPartyDataSharingConsent(false)
                    .accountVisibility("private")
                    .profileVisibility("friends")
                    .activityVisibility("hidden")
                    .dataRetentionDays(365)
                    .autoDataDeletion(true)
                    .updatedAt(now)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getConsentStatus()).isEqualTo(consentStatus),
                    () -> assertThat(response.getDataSharingPreferences()).isEqualTo(dataSharingPreferences),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isFalse(),
                    () -> assertThat(response.getAnalyticsConsent()).isTrue(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isFalse(),
                    () -> assertThat(response.getAccountVisibility()).isEqualTo("private"),
                    () -> assertThat(response.getProfileVisibility()).isEqualTo("friends"),
                    () -> assertThat(response.getActivityVisibility()).isEqualTo("hidden"),
                    () -> assertThat(response.getDataRetentionDays()).isEqualTo(365),
                    () -> assertThat(response.getAutoDataDeletion()).isTrue(),
                    () -> assertThat(response.getUpdatedAt()).isEqualTo(now)
            );
        }

        @Test
        @DisplayName("Should create minimal response using builder")
        void shouldCreateMinimalResponseUsingBuilder() {
            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .marketingCommunicationConsent(false)
                    .analyticsConsent(false)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isFalse(),
                    () -> assertThat(response.getAnalyticsConsent()).isFalse(),
                    () -> assertThat(response.getConsentStatus()).isNull(),
                    () -> assertThat(response.getDataSharingPreferences()).isNull()
            );
        }

        @Test
        @DisplayName("Should create response with all privacy settings enabled")
        void shouldCreateResponseWithAllPrivacySettingsEnabled() {
            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .marketingCommunicationConsent(true)
                    .analyticsConsent(true)
                    .thirdPartyDataSharingConsent(true)
                    .accountVisibility("public")
                    .profileVisibility("public")
                    .activityVisibility("public")
                    .autoDataDeletion(false)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getMarketingCommunicationConsent()).isTrue(),
                    () -> assertThat(response.getAnalyticsConsent()).isTrue(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isTrue(),
                    () -> assertThat(response.getAccountVisibility()).isEqualTo("public"),
                    () -> assertThat(response.getProfileVisibility()).isEqualTo("public"),
                    () -> assertThat(response.getActivityVisibility()).isEqualTo("public"),
                    () -> assertThat(response.getAutoDataDeletion()).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            PrivacyPreferencesResponse response = new PrivacyPreferencesResponse();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getConsentStatus()).isNull(),
                    () -> assertThat(response.getDataSharingPreferences()).isNull(),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isNull(),
                    () -> assertThat(response.getAnalyticsConsent()).isNull(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isNull(),
                    () -> assertThat(response.getAccountVisibility()).isNull(),
                    () -> assertThat(response.getProfileVisibility()).isNull(),
                    () -> assertThat(response.getActivityVisibility()).isNull(),
                    () -> assertThat(response.getDataRetentionDays()).isNull(),
                    () -> assertThat(response.getAutoDataDeletion()).isNull(),
                    () -> assertThat(response.getUpdatedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Should create instance using all-args constructor")
        void shouldCreateInstanceUsingAllArgsConstructor() {
            // Given
            Map<String, Boolean> consentStatus = Map.of("gdpr", true);
            Map<String, String> dataSharingPreferences = Map.of("level", "minimal");
            Instant timestamp = Instant.parse("2023-12-01T10:00:00Z");

            // When
            PrivacyPreferencesResponse response = new PrivacyPreferencesResponse(
                    consentStatus,
                    dataSharingPreferences,
                    true,
                    false,
                    true,
                    "private",
                    "limited",
                    "hidden",
                    90,
                    true,
                    timestamp
            );

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).isEqualTo(consentStatus),
                    () -> assertThat(response.getDataSharingPreferences()).isEqualTo(dataSharingPreferences),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isTrue(),
                    () -> assertThat(response.getAnalyticsConsent()).isFalse(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isTrue(),
                    () -> assertThat(response.getAccountVisibility()).isEqualTo("private"),
                    () -> assertThat(response.getProfileVisibility()).isEqualTo("limited"),
                    () -> assertThat(response.getActivityVisibility()).isEqualTo("hidden"),
                    () -> assertThat(response.getDataRetentionDays()).isEqualTo(90),
                    () -> assertThat(response.getAutoDataDeletion()).isTrue(),
                    () -> assertThat(response.getUpdatedAt()).isEqualTo(timestamp)
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            PrivacyPreferencesResponse response = new PrivacyPreferencesResponse();
            Map<String, Boolean> newConsent = Map.of("updated", true);
            Instant newTimestamp = Instant.now();

            // When
            response.setConsentStatus(newConsent);
            response.setMarketingCommunicationConsent(true);
            response.setAccountVisibility("public");
            response.setDataRetentionDays(180);
            response.setUpdatedAt(newTimestamp);

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).isEqualTo(newConsent),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isTrue(),
                    () -> assertThat(response.getAccountVisibility()).isEqualTo("public"),
                    () -> assertThat(response.getDataRetentionDays()).isEqualTo(180),
                    () -> assertThat(response.getUpdatedAt()).isEqualTo(newTimestamp)
            );
        }
    }

    @Nested
    @DisplayName("Consent Status Tests")
    class ConsentStatusTests {

        @Test
        @DisplayName("Should handle various consent types")
        void shouldHandleVariousConsentTypes() {
            // Given
            Map<String, Boolean> consentStatus = Map.of(
                    "gdpr_compliance", true,
                    "ccpa_compliance", false,
                    "email_marketing", true,
                    "sms_marketing", false,
                    "data_processing", true,
                    "cookies", false
            );

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(consentStatus)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).hasSize(6),
                    () -> assertThat(response.getConsentStatus().get("gdpr_compliance")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("ccpa_compliance")).isFalse(),
                    () -> assertThat(response.getConsentStatus().get("email_marketing")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("sms_marketing")).isFalse(),
                    () -> assertThat(response.getConsentStatus().get("data_processing")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("cookies")).isFalse()
            );
        }

        @Test
        @DisplayName("Should handle empty consent status map")
        void shouldHandleEmptyConsentStatusMap() {
            // Given
            Map<String, Boolean> emptyConsent = new HashMap<>();

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(emptyConsent)
                    .build();

            // Then
            assertThat(response.getConsentStatus()).isEmpty();
        }

        @Test
        @DisplayName("Should handle mutable consent status map")
        void shouldHandleMutableConsentStatusMap() {
            // Given
            Map<String, Boolean> mutableConsent = new HashMap<>();
            mutableConsent.put("initial", true);

            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(mutableConsent)
                    .build();

            // When
            response.getConsentStatus().put("added", false);

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).hasSize(2),
                    () -> assertThat(response.getConsentStatus().get("initial")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("added")).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("Data Sharing Preferences Tests")
    class DataSharingPreferencesTests {

        @Test
        @DisplayName("Should handle various data sharing preferences")
        void shouldHandleVariousDataSharingPreferences() {
            // Given
            Map<String, String> preferences = Map.of(
                    "analytics", "restricted",
                    "marketing", "disabled",
                    "research", "enabled",
                    "personalization", "limited",
                    "third_party", "blocked"
            );

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .dataSharingPreferences(preferences)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getDataSharingPreferences()).hasSize(5),
                    () -> assertThat(response.getDataSharingPreferences().get("analytics")).isEqualTo("restricted"),
                    () -> assertThat(response.getDataSharingPreferences().get("marketing")).isEqualTo("disabled"),
                    () -> assertThat(response.getDataSharingPreferences().get("research")).isEqualTo("enabled"),
                    () -> assertThat(response.getDataSharingPreferences().get("personalization")).isEqualTo("limited"),
                    () -> assertThat(response.getDataSharingPreferences().get("third_party")).isEqualTo("blocked")
            );
        }

        @Test
        @DisplayName("Should handle standard privacy levels")
        void shouldHandleStandardPrivacyLevels() {
            String[] privacyLevels = {"public", "private", "friends", "limited", "hidden", "disabled"};

            for (String level : privacyLevels) {
                // When
                PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                        .accountVisibility(level)
                        .profileVisibility(level)
                        .activityVisibility(level)
                        .build();

                // Then
                assertAll(
                        () -> assertThat(response.getAccountVisibility()).isEqualTo(level),
                        () -> assertThat(response.getProfileVisibility()).isEqualTo(level),
                        () -> assertThat(response.getActivityVisibility()).isEqualTo(level)
                );
            }
        }
    }

    @Nested
    @DisplayName("Data Retention Tests")
    class DataRetentionTests {

        @Test
        @DisplayName("Should handle various data retention periods")
        void shouldHandleVariousDataRetentionPeriods() {
            Integer[] retentionPeriods = {30, 90, 180, 365, 730, 1095}; // 1 month to 3 years

            for (Integer days : retentionPeriods) {
                // When
                PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                        .dataRetentionDays(days)
                        .build();

                // Then
                assertThat(response.getDataRetentionDays()).isEqualTo(days);
            }
        }

        @Test
        @DisplayName("Should handle unlimited retention")
        void shouldHandleUnlimitedRetention() {
            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(null) // null means unlimited/indefinite
                    .autoDataDeletion(false)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getDataRetentionDays()).isNull(),
                    () -> assertThat(response.getAutoDataDeletion()).isFalse()
            );
        }

        @Test
        @DisplayName("Should handle zero retention period")
        void shouldHandleZeroRetentionPeriod() {
            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(0)
                    .autoDataDeletion(true)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getDataRetentionDays()).isEqualTo(0),
                    () -> assertThat(response.getAutoDataDeletion()).isTrue()
            );
        }

        @Test
        @DisplayName("Should handle very large retention periods")
        void shouldHandleVeryLargeRetentionPeriods() {
            // Given - 100 years in days
            Integer maxRetention = 36500;

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(maxRetention)
                    .build();

            // Then
            assertThat(response.getDataRetentionDays()).isEqualTo(36500);
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize complete response to JSON correctly")
        void shouldSerializeCompleteResponseToJsonCorrectly() throws Exception {
            // Given
            Map<String, Boolean> consentStatus = Map.of("gdpr", true, "marketing", false);
            Map<String, String> dataSharingPreferences = Map.of("level", "restricted");
            Instant timestamp = Instant.parse("2023-12-01T10:15:30Z");

            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(consentStatus)
                    .dataSharingPreferences(dataSharingPreferences)
                    .marketingCommunicationConsent(false)
                    .analyticsConsent(true)
                    .thirdPartyDataSharingConsent(false)
                    .accountVisibility("private")
                    .profileVisibility("friends")
                    .activityVisibility("hidden")
                    .dataRetentionDays(365)
                    .autoDataDeletion(true)
                    .updatedAt(timestamp)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"marketingCommunicationConsent\":false"),
                    () -> assertThat(json).contains("\"analyticsConsent\":true"),
                    () -> assertThat(json).contains("\"thirdPartyDataSharingConsent\":false"),
                    () -> assertThat(json).contains("\"accountVisibility\":\"private\""),
                    () -> assertThat(json).contains("\"profileVisibility\":\"friends\""),
                    () -> assertThat(json).contains("\"activityVisibility\":\"hidden\""),
                    () -> assertThat(json).contains("\"dataRetentionDays\":365"),
                    () -> assertThat(json).contains("\"autoDataDeletion\":true"),
                    () -> assertThat(json).contains("\"updatedAt\":\"2023-12-01T10:15:30Z\""),
                    () -> assertThat(json).contains("\"consentStatus\":{"),
                    () -> assertThat(json).contains("\"dataSharingPreferences\":{")
            );
        }

        @Test
        @DisplayName("Should deserialize from JSON correctly")
        void shouldDeserializeFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "consentStatus": {
                        "gdpr": true,
                        "marketing": false
                    },
                    "dataSharingPreferences": {
                        "analytics": "restricted",
                        "marketing": "disabled"
                    },
                    "marketingCommunicationConsent": false,
                    "analyticsConsent": true,
                    "thirdPartyDataSharingConsent": false,
                    "accountVisibility": "private",
                    "profileVisibility": "friends",
                    "activityVisibility": "hidden",
                    "dataRetentionDays": 365,
                    "autoDataDeletion": true,
                    "updatedAt": "2023-12-01T10:15:30Z"
                }
                """;

            // When
            PrivacyPreferencesResponse response = objectMapper.readValue(json, PrivacyPreferencesResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getConsentStatus()).hasSize(2),
                    () -> assertThat(response.getConsentStatus().get("gdpr")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("marketing")).isFalse(),
                    () -> assertThat(response.getDataSharingPreferences()).hasSize(2),
                    () -> assertThat(response.getDataSharingPreferences().get("analytics")).isEqualTo("restricted"),
                    () -> assertThat(response.getDataSharingPreferences().get("marketing")).isEqualTo("disabled"),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isFalse(),
                    () -> assertThat(response.getAnalyticsConsent()).isTrue(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isFalse(),
                    () -> assertThat(response.getAccountVisibility()).isEqualTo("private"),
                    () -> assertThat(response.getProfileVisibility()).isEqualTo("friends"),
                    () -> assertThat(response.getActivityVisibility()).isEqualTo("hidden"),
                    () -> assertThat(response.getDataRetentionDays()).isEqualTo(365),
                    () -> assertThat(response.getAutoDataDeletion()).isTrue(),
                    () -> assertThat(response.getUpdatedAt()).isEqualTo(Instant.parse("2023-12-01T10:15:30Z"))
            );
        }

        @Test
        @DisplayName("Should handle null values in JSON")
        void shouldHandleNullValuesInJson() throws Exception {
            // Given
            String json = """
                {
                    "consentStatus": null,
                    "dataSharingPreferences": null,
                    "marketingCommunicationConsent": null,
                    "analyticsConsent": null,
                    "thirdPartyDataSharingConsent": null,
                    "accountVisibility": null,
                    "profileVisibility": null,
                    "activityVisibility": null,
                    "dataRetentionDays": null,
                    "autoDataDeletion": null,
                    "updatedAt": null
                }
                """;

            // When
            PrivacyPreferencesResponse response = objectMapper.readValue(json, PrivacyPreferencesResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getConsentStatus()).isNull(),
                    () -> assertThat(response.getDataSharingPreferences()).isNull(),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isNull(),
                    () -> assertThat(response.getAnalyticsConsent()).isNull(),
                    () -> assertThat(response.getThirdPartyDataSharingConsent()).isNull(),
                    () -> assertThat(response.getAccountVisibility()).isNull(),
                    () -> assertThat(response.getProfileVisibility()).isNull(),
                    () -> assertThat(response.getActivityVisibility()).isNull(),
                    () -> assertThat(response.getDataRetentionDays()).isNull(),
                    () -> assertThat(response.getAutoDataDeletion()).isNull(),
                    () -> assertThat(response.getUpdatedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle empty JSON object")
        void shouldHandleEmptyJsonObject() throws Exception {
            // Given
            String json = "{}";

            // When
            PrivacyPreferencesResponse response = objectMapper.readValue(json, PrivacyPreferencesResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getConsentStatus()).isNull(),
                    () -> assertThat(response.getDataSharingPreferences()).isNull(),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle empty maps in JSON")
        void shouldHandleEmptyMapsInJson() throws Exception {
            // Given
            String json = """
                {
                    "consentStatus": {},
                    "dataSharingPreferences": {}
                }
                """;

            // When
            PrivacyPreferencesResponse response = objectMapper.readValue(json, PrivacyPreferencesResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).isEmpty(),
                    () -> assertThat(response.getDataSharingPreferences()).isEmpty()
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Given
            Map<String, Boolean> consentStatus = Map.of("gdpr", true);
            Map<String, String> dataSharingPreferences = Map.of("level", "restricted");
            Instant timestamp = Instant.parse("2023-12-01T10:00:00Z");

            PrivacyPreferencesResponse response1 = createCompleteResponse(consentStatus, dataSharingPreferences, timestamp);
            PrivacyPreferencesResponse response2 = createCompleteResponse(consentStatus, dataSharingPreferences, timestamp);

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when consent status differs")
        void shouldNotBeEqualWhenConsentStatusDiffers() {
            // Given
            Map<String, Boolean> consentStatus1 = Map.of("gdpr", true);
            Map<String, Boolean> consentStatus2 = Map.of("gdpr", false);
            Map<String, String> dataSharingPreferences = Map.of("level", "restricted");
            Instant timestamp = Instant.now();

            PrivacyPreferencesResponse response1 = createCompleteResponse(consentStatus1, dataSharingPreferences, timestamp);
            PrivacyPreferencesResponse response2 = createCompleteResponse(consentStatus2, dataSharingPreferences, timestamp);

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when data sharing preferences differ")
        void shouldNotBeEqualWhenDataSharingPreferencesDiffer() {
            // Given
            Map<String, Boolean> consentStatus = Map.of("gdpr", true);
            Map<String, String> dataSharingPreferences1 = Map.of("level", "restricted");
            Map<String, String> dataSharingPreferences2 = Map.of("level", "enabled");
            Instant timestamp = Instant.now();

            PrivacyPreferencesResponse response1 = createCompleteResponse(consentStatus, dataSharingPreferences1, timestamp);
            PrivacyPreferencesResponse response2 = createCompleteResponse(consentStatus, dataSharingPreferences2, timestamp);

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when boolean fields differ")
        void shouldNotBeEqualWhenBooleanFieldsDiffer() {
            // Given
            PrivacyPreferencesResponse response1 = PrivacyPreferencesResponse.builder()
                    .marketingCommunicationConsent(true)
                    .build();

            PrivacyPreferencesResponse response2 = PrivacyPreferencesResponse.builder()
                    .marketingCommunicationConsent(false)
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null values in equality")
        void shouldHandleNullValuesInEquality() {
            // Given
            PrivacyPreferencesResponse response1 = new PrivacyPreferencesResponse();
            PrivacyPreferencesResponse response2 = new PrivacyPreferencesResponse();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .marketingCommunicationConsent(false)
                    .analyticsConsent(true)
                    .accountVisibility("private")
                    .dataRetentionDays(365)
                    .build();

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PrivacyPreferencesResponse"),
                    () -> assertThat(toString).contains("marketingCommunicationConsent=false"),
                    () -> assertThat(toString).contains("analyticsConsent=true"),
                    () -> assertThat(toString).contains("accountVisibility=private"),
                    () -> assertThat(toString).contains("dataRetentionDays=365")
            );
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            PrivacyPreferencesResponse response = new PrivacyPreferencesResponse();

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PrivacyPreferencesResponse"),
                    () -> assertThat(toString).contains("consentStatus=null"),
                    () -> assertThat(toString).contains("marketingCommunicationConsent=null"),
                    () -> assertThat(toString).contains("updatedAt=null")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle mixed consent and sharing preferences")
        void shouldHandleMixedConsentAndSharingPreferences() {
            // Given
            Map<String, Boolean> mixedConsent = Map.of(
                    "essential", true,
                    "analytics", false,
                    "marketing", true,
                    "functional", false
            );
            
            Map<String, String> mixedSharing = Map.of(
                    "research", "enabled",
                    "advertising", "disabled",
                    "personalization", "limited"
            );

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(mixedConsent)
                    .dataSharingPreferences(mixedSharing)
                    .marketingCommunicationConsent(true)
                    .analyticsConsent(false) // Conflicting with consent map - business logic should handle this
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus().get("analytics")).isFalse(),
                    () -> assertThat(response.getAnalyticsConsent()).isFalse(),
                    () -> assertThat(response.getConsentStatus().get("marketing")).isTrue(),
                    () -> assertThat(response.getMarketingCommunicationConsent()).isTrue(),
                    () -> assertThat(response.getDataSharingPreferences().get("advertising")).isEqualTo("disabled")
            );
        }

        @Test
        @DisplayName("Should handle extreme data retention values")
        void shouldHandleExtremeDataRetentionValues() {
            // Given & When
            PrivacyPreferencesResponse responseNegative = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(-1) // Invalid but should be stored as-is
                    .build();

            PrivacyPreferencesResponse responseZero = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(0)
                    .build();

            PrivacyPreferencesResponse responseMax = PrivacyPreferencesResponse.builder()
                    .dataRetentionDays(Integer.MAX_VALUE)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(responseNegative.getDataRetentionDays()).isEqualTo(-1),
                    () -> assertThat(responseZero.getDataRetentionDays()).isEqualTo(0),
                    () -> assertThat(responseMax.getDataRetentionDays()).isEqualTo(Integer.MAX_VALUE)
            );
        }

        @Test
        @DisplayName("Should handle special characters in visibility settings")
        void shouldHandleSpecialCharactersInVisibilitySettings() {
            // Given
            String specialVisibility = "custom_level_with_unicode_αβγ_and_emoji_🔒";

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .accountVisibility(specialVisibility)
                    .profileVisibility(specialVisibility)
                    .activityVisibility(specialVisibility)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccountVisibility()).isEqualTo(specialVisibility),
                    () -> assertThat(response.getProfileVisibility()).isEqualTo(specialVisibility),
                    () -> assertThat(response.getActivityVisibility()).isEqualTo(specialVisibility)
            );
        }

        @Test
        @DisplayName("Should handle large maps with many entries")
        void shouldHandleLargeMapsWithManyEntries() {
            // Given
            Map<String, Boolean> largeConsentMap = new HashMap<>();
            Map<String, String> largePreferencesMap = new HashMap<>();
            
            for (int i = 0; i < 100; i++) {
                largeConsentMap.put("consent_type_" + i, i % 2 == 0);
                largePreferencesMap.put("preference_" + i, "level_" + (i % 5));
            }

            // When
            PrivacyPreferencesResponse response = PrivacyPreferencesResponse.builder()
                    .consentStatus(largeConsentMap)
                    .dataSharingPreferences(largePreferencesMap)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getConsentStatus()).hasSize(100),
                    () -> assertThat(response.getDataSharingPreferences()).hasSize(100),
                    () -> assertThat(response.getConsentStatus().get("consent_type_0")).isTrue(),
                    () -> assertThat(response.getConsentStatus().get("consent_type_1")).isFalse(),
                    () -> assertThat(response.getDataSharingPreferences().get("preference_0")).isEqualTo("level_0"),
                    () -> assertThat(response.getDataSharingPreferences().get("preference_4")).isEqualTo("level_4")
            );
        }
    }

    /**
     * Helper method to create a complete PrivacyPreferencesResponse for testing.
     */
    private PrivacyPreferencesResponse createCompleteResponse(
            Map<String, Boolean> consentStatus, 
            Map<String, String> dataSharingPreferences, 
            Instant timestamp) {
        return PrivacyPreferencesResponse.builder()
                .consentStatus(consentStatus)
                .dataSharingPreferences(dataSharingPreferences)
                .marketingCommunicationConsent(false)
                .analyticsConsent(true)
                .thirdPartyDataSharingConsent(false)
                .accountVisibility("private")
                .profileVisibility("friends")
                .activityVisibility("hidden")
                .dataRetentionDays(365)
                .autoDataDeletion(true)
                .updatedAt(timestamp)
                .build();
    }
}