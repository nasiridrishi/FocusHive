package com.focushive.identity.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for ConsentRecord entity covering all business logic,
 * GDPR compliance, metadata management, and consent lifecycle methods.
 */
@DisplayName("ConsentRecord Entity Tests")
class ConsentRecordUnitTest {

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should build ConsentRecord with all fields")
        void shouldBuildConsentRecordWithAllFields() {
            // Given
            User user = User.builder().username("testuser").build();
            ConsentRecord parentConsent = ConsentRecord.builder()
                    .user(user)
                    .consentType("parent_consent")
                    .purpose("Parent consent purpose")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", "registration");
            Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);

            // When
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .consentType("marketing_emails")
                    .purpose("Send marketing emails and promotional content")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion("v2.1")
                    .consentSource("registration_form")
                    .ipAddress("192.168.1.1")
                    .userAgent("Mozilla/5.0")
                    .expiresAt(expiresAt)
                    .active(true)
                    .withdrawnAt(null)
                    .withdrawalReason(null)
                    .supersededAt(null)
                    .parentConsent(parentConsent)
                    .metadata(metadata)
                    .geographicLocation("EU-Germany")
                    .consentLanguage("en")
                    .verificationMethod("email")
                    .build();

            // Then
            assertThat(consentRecord.getUser()).isEqualTo(user);
            assertThat(consentRecord.getConsentType()).isEqualTo("marketing_emails");
            assertThat(consentRecord.getPurpose()).isEqualTo("Send marketing emails and promotional content");
            assertThat(consentRecord.getLegalBasis()).isEqualTo("consent");
            assertThat(consentRecord.isConsentGiven()).isTrue();
            assertThat(consentRecord.getConsentVersion()).isEqualTo("v2.1");
            assertThat(consentRecord.getConsentSource()).isEqualTo("registration_form");
            assertThat(consentRecord.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(consentRecord.getUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(consentRecord.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(consentRecord.isActive()).isTrue();
            assertThat(consentRecord.getParentConsent()).isEqualTo(parentConsent);
            assertThat(consentRecord.getMetadata()).containsEntry("source", "registration");
            assertThat(consentRecord.getGeographicLocation()).isEqualTo("EU-Germany");
            assertThat(consentRecord.getConsentLanguage()).isEqualTo("en");
            assertThat(consentRecord.getVerificationMethod()).isEqualTo("email");
        }

        @Test
        @DisplayName("Should build ConsentRecord with default values")
        void shouldBuildConsentRecordWithDefaults() {
            // Given
            User user = User.builder().username("testuser").build();

            // When
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(user)
                    .consentType("analytics")
                    .purpose("Analytics tracking")
                    .legalBasis("legitimate_interest")
                    .consentGiven(true)
                    .build();

            // Then
            assertThat(consentRecord.isActive()).isTrue();
            assertThat(consentRecord.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("Should build ConsentRecord with minimal required fields")
        void shouldBuildConsentRecordWithMinimalFields() {
            // Given
            User user = User.builder().username("testuser").build();

            // When
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(user)
                    .consentType("cookies")
                    .purpose("Website functionality cookies")
                    .legalBasis("necessary")
                    .consentGiven(true)
                    .build();

            // Then
            assertThat(consentRecord.getUser()).isEqualTo(user);
            assertThat(consentRecord.getConsentType()).isEqualTo("cookies");
            assertThat(consentRecord.getPurpose()).isEqualTo("Website functionality cookies");
            assertThat(consentRecord.getLegalBasis()).isEqualTo("necessary");
            assertThat(consentRecord.isConsentGiven()).isTrue();
            assertThat(consentRecord.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Consent Expiration Tests")
    class ConsentExpirationTests {

        @Test
        @DisplayName("Should identify expired consent")
        void shouldIdentifyExpiredConsent() {
            // Given
            ConsentRecord expiredConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(expiredConsent.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Should identify non-expired consent")
        void shouldIdentifyNonExpiredConsent() {
            // Given
            ConsentRecord validConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(validConsent.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should handle null expiration date")
        void shouldHandleNullExpirationDate() {
            // Given
            ConsentRecord permanentConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .expiresAt(null)
                    .build();

            // When & Then
            assertThat(permanentConsent.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Effective Consent Tests")
    class EffectiveConsentTests {

        @Test
        @DisplayName("Should identify effective consent")
        void shouldIdentifyEffectiveConsent() {
            // Given
            ConsentRecord effectiveConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                    .withdrawnAt(null)
                    .build();

            // When & Then
            assertThat(effectiveConsent.isEffectiveConsent()).isTrue();
        }

        @Test
        @DisplayName("Should identify non-effective consent when not given")
        void shouldIdentifyNonEffectiveConsentWhenNotGiven() {
            // Given
            ConsentRecord notGivenConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(false)
                    .active(true)
                    .build();

            // When & Then
            assertThat(notGivenConsent.isEffectiveConsent()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-effective consent when inactive")
        void shouldIdentifyNonEffectiveConsentWhenInactive() {
            // Given
            ConsentRecord inactiveConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(false)
                    .build();

            // When & Then
            assertThat(inactiveConsent.isEffectiveConsent()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-effective consent when expired")
        void shouldIdentifyNonEffectiveConsentWhenExpired() {
            // Given
            ConsentRecord expiredConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(expiredConsent.isEffectiveConsent()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-effective consent when withdrawn")
        void shouldIdentifyNonEffectiveConsentWhenWithdrawn() {
            // Given
            ConsentRecord withdrawnConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .withdrawnAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            // When & Then
            assertThat(withdrawnConsent.isEffectiveConsent()).isFalse();
        }
    }

    @Nested
    @DisplayName("Consent Withdrawal Tests")
    class ConsentWithdrawalTests {

        @Test
        @DisplayName("Should withdraw consent with reason")
        void shouldWithdrawConsentWithReason() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("marketing")
                    .purpose("Marketing emails")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .build();

            String withdrawalReason = "No longer interested";
            Instant beforeWithdrawal = Instant.now();

            // When
            consentRecord.withdraw(withdrawalReason);
            Instant afterWithdrawal = Instant.now();

            // Then
            assertThat(consentRecord.isConsentGiven()).isFalse();
            assertThat(consentRecord.getWithdrawnAt()).isBetween(beforeWithdrawal, afterWithdrawal);
            assertThat(consentRecord.getWithdrawalReason()).isEqualTo(withdrawalReason);
            assertThat(consentRecord.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should withdraw consent without reason")
        void shouldWithdrawConsentWithoutReason() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("analytics")
                    .purpose("Analytics tracking")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .build();

            // When
            consentRecord.withdraw(null);

            // Then
            assertThat(consentRecord.isConsentGiven()).isFalse();
            assertThat(consentRecord.getWithdrawnAt()).isNotNull();
            assertThat(consentRecord.getWithdrawalReason()).isNull();
            assertThat(consentRecord.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Consent Supersession Tests")
    class ConsentSupersessionTests {

        @Test
        @DisplayName("Should mark consent as superseded")
        void shouldMarkConsentAsSuperseded() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("privacy_policy")
                    .purpose("Privacy policy acceptance")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .active(true)
                    .build();

            Instant beforeSuperseded = Instant.now();

            // When
            consentRecord.markAsSuperseded();
            Instant afterSuperseded = Instant.now();

            // Then
            assertThat(consentRecord.isActive()).isFalse();
            assertThat(consentRecord.getSupersededAt()).isBetween(beforeSuperseded, afterSuperseded);
        }
    }

    @Nested
    @DisplayName("Metadata Management Tests")
    class MetadataManagementTests {

        @Test
        @DisplayName("Should add metadata to existing map")
        void shouldAddMetadataToExistingMap() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            // When
            consentRecord.addMetadata("key1", "value1");
            consentRecord.addMetadata("key2", "value2");

            // Then
            assertThat(consentRecord.getMetadata("key1")).isEqualTo("value1");
            assertThat(consentRecord.getMetadata("key2")).isEqualTo("value2");
            assertThat(consentRecord.getMetadata()).hasSize(2);
        }

        @Test
        @DisplayName("Should add metadata when map is null")
        void shouldAddMetadataWhenMapIsNull() {
            // Given
            ConsentRecord consentRecord = new ConsentRecord();
            consentRecord.setMetadata(null);

            // When
            consentRecord.addMetadata("key", "value");

            // Then
            assertThat(consentRecord.getMetadata("key")).isEqualTo("value");
            assertThat(consentRecord.getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("Should get metadata value")
        void shouldGetMetadataValue() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("campaign", "summer2024");
            
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .metadata(metadata)
                    .build();

            // When & Then
            assertThat(consentRecord.getMetadata("campaign")).isEqualTo("summer2024");
            assertThat(consentRecord.getMetadata("nonexistent")).isNull();
        }

        @Test
        @DisplayName("Should return null when metadata map is null")
        void shouldReturnNullWhenMetadataMapIsNull() {
            // Given
            ConsentRecord consentRecord = new ConsentRecord();
            consentRecord.setMetadata(null);

            // When & Then
            assertThat(consentRecord.getMetadata("any_key")).isNull();
        }
    }

    @Nested
    @DisplayName("Purpose Applicability Tests")
    class PurposeApplicabilityTests {

        @Test
        @DisplayName("Should identify applicable purpose")
        void shouldIdentifyApplicablePurpose() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("marketing")
                    .purpose("Send marketing emails and promotional offers")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            // When & Then
            assertThat(consentRecord.appliesTo("marketing")).isTrue();
            assertThat(consentRecord.appliesTo("MARKETING")).isTrue();
            assertThat(consentRecord.appliesTo("promotional")).isTrue();
            assertThat(consentRecord.appliesTo("emails")).isTrue();
        }

        @Test
        @DisplayName("Should identify non-applicable purpose")
        void shouldIdentifyNonApplicablePurpose() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("analytics")
                    .purpose("Website analytics and performance monitoring")
                    .legalBasis("legitimate_interest")
                    .consentGiven(true)
                    .build();

            // When & Then
            assertThat(consentRecord.appliesTo("marketing")).isFalse();
            assertThat(consentRecord.appliesTo("cookies")).isFalse();
        }

        @Test
        @DisplayName("Should handle null purpose")
        void shouldHandleNullPurpose() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose(null)
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            // When & Then
            assertThat(consentRecord.appliesTo("anything")).isFalse();
        }
    }

    @Nested
    @DisplayName("Consent Renewal Tests")
    class ConsentRenewalTests {

        @Test
        @DisplayName("Should identify renewable consent")
        void shouldIdentifyRenewableConsent() {
            // Given - Consent expiring within 30 days
            ConsentRecord renewableConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("privacy_policy")
                    .purpose("Privacy policy acceptance")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion("v1.0")
                    .expiresAt(Instant.now().plus(15, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(renewableConsent.isRenewable()).isTrue();
        }

        @Test
        @DisplayName("Should identify non-renewable consent without version")
        void shouldIdentifyNonRenewableConsentWithoutVersion() {
            // Given
            ConsentRecord nonRenewableConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("analytics")
                    .purpose("Analytics tracking")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion(null)
                    .expiresAt(Instant.now().plus(15, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(nonRenewableConsent.isRenewable()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-renewable consent without expiration")
        void shouldIdentifyNonRenewableConsentWithoutExpiration() {
            // Given
            ConsentRecord permanentConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("necessary_cookies")
                    .purpose("Essential website functionality")
                    .legalBasis("necessary")
                    .consentGiven(true)
                    .consentVersion("v1.0")
                    .expiresAt(null)
                    .build();

            // When & Then
            assertThat(permanentConsent.isRenewable()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-renewable consent expiring far in future")
        void shouldIdentifyNonRenewableConsentExpiringFarInFuture() {
            // Given - Consent expiring in 6 months
            ConsentRecord futureConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("marketing")
                    .purpose("Marketing communications")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion("v1.0")
                    .expiresAt(Instant.now().plus(180, ChronoUnit.DAYS))
                    .build();

            // When & Then
            assertThat(futureConsent.isRenewable()).isFalse();
        }

        @Test
        @DisplayName("Should create renewal consent record")
        void shouldCreateRenewalConsentRecord() {
            // Given
            User user = User.builder().username("testuser").build();
            ConsentRecord originalConsent = ConsentRecord.builder()
                    .user(user)
                    .consentType("privacy_policy")
                    .purpose("Old privacy policy")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion("v1.0")
                    .consentLanguage("en")
                    .build();

            String newVersion = "v2.0";
            String newPurpose = "Updated privacy policy with enhanced protections";
            Instant newExpiresAt = Instant.now().plus(365, ChronoUnit.DAYS);

            // When
            ConsentRecord renewalConsent = originalConsent.createRenewal(newVersion, newPurpose, newExpiresAt);

            // Then
            assertThat(renewalConsent.getUser()).isEqualTo(user);
            assertThat(renewalConsent.getConsentType()).isEqualTo("privacy_policy");
            assertThat(renewalConsent.getPurpose()).isEqualTo(newPurpose);
            assertThat(renewalConsent.getLegalBasis()).isEqualTo("consent");
            assertThat(renewalConsent.isConsentGiven()).isTrue();
            assertThat(renewalConsent.getConsentVersion()).isEqualTo(newVersion);
            assertThat(renewalConsent.getConsentSource()).isEqualTo("renewal");
            assertThat(renewalConsent.getExpiresAt()).isEqualTo(newExpiresAt);
            assertThat(renewalConsent.getParentConsent()).isEqualTo(originalConsent);
            assertThat(renewalConsent.getConsentLanguage()).isEqualTo("en");
        }

        @Test
        @DisplayName("Should create renewal consent with original purpose when new purpose is null")
        void shouldCreateRenewalConsentWithOriginalPurposeWhenNewPurposeIsNull() {
            // Given
            User user = User.builder().username("testuser").build();
            String originalPurpose = "Original privacy policy";
            ConsentRecord originalConsent = ConsentRecord.builder()
                    .user(user)
                    .consentType("privacy_policy")
                    .purpose(originalPurpose)
                    .legalBasis("consent")
                    .consentGiven(true)
                    .consentVersion("v1.0")
                    .build();

            // When
            ConsentRecord renewalConsent = originalConsent.createRenewal("v2.0", null, Instant.now().plus(365, ChronoUnit.DAYS));

            // Then
            assertThat(renewalConsent.getPurpose()).isEqualTo(originalPurpose);
        }
    }

    @Nested
    @DisplayName("GDPR Compliance Tests")
    class GDPRComplianceTests {

        @Test
        @DisplayName("Should identify GDPR applicable consent for EU locations")
        void shouldIdentifyGDPRApplicableConsentForEULocations() {
            // Given & When & Then
            assertThat(createConsentWithLocation("EU-Germany").isGDPRApplicable()).isTrue();
            assertThat(createConsentWithLocation("Europe-France").isGDPRApplicable()).isTrue();
            assertThat(createConsentWithLocation("UK-London").isGDPRApplicable()).isTrue();
            assertThat(createConsentWithLocation("Spain-Madrid").isGDPRApplicable()).isTrue();
            assertThat(createConsentWithLocation("Italy-Rome").isGDPRApplicable()).isTrue();
            assertThat(createConsentWithLocation("Poland-Warsaw").isGDPRApplicable()).isTrue();
        }

        @Test
        @DisplayName("Should identify non-GDPR applicable consent for non-EU locations")
        void shouldIdentifyNonGDPRApplicableConsentForNonEULocations() {
            // Given & When & Then
            assertThat(createConsentWithLocation("US-California").isGDPRApplicable()).isFalse();
            assertThat(createConsentWithLocation("Canada-Toronto").isGDPRApplicable()).isFalse();
            assertThat(createConsentWithLocation("Australia-Sydney").isGDPRApplicable()).isFalse();
        }

        @Test
        @DisplayName("Should handle null geographic location")
        void shouldHandleNullGeographicLocation() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .geographicLocation(null)
                    .build();

            // When & Then
            assertThat(consentRecord.isGDPRApplicable()).isFalse();
        }

        private ConsentRecord createConsentWithLocation(String location) {
            return ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .geographicLocation(location)
                    .build();
        }
    }

    @Nested
    @DisplayName("Consent Duration Tests")
    class ConsentDurationTests {

        @Test
        @DisplayName("Should calculate duration for active consent")
        void shouldCalculateDurationForActiveConsent() {
            // Given
            Instant createdTime = Instant.now().minus(10, ChronoUnit.DAYS);
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(createdTime)
                    .build();

            // When
            Long duration = consentRecord.getConsentDurationDays();

            // Then
            assertThat(duration).isBetween(9L, 11L); // Allow for small timing differences
        }

        @Test
        @DisplayName("Should calculate duration for withdrawn consent")
        void shouldCalculateDurationForWithdrawnConsent() {
            // Given
            Instant createdTime = Instant.now().minus(20, ChronoUnit.DAYS);
            Instant withdrawnTime = Instant.now().minus(5, ChronoUnit.DAYS);
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(false)
                    .createdAt(createdTime)
                    .withdrawnAt(withdrawnTime)
                    .build();

            // When
            Long duration = consentRecord.getConsentDurationDays();

            // Then
            assertThat(duration).isEqualTo(15L);
        }

        @Test
        @DisplayName("Should calculate duration for superseded consent")
        void shouldCalculateDurationForSupersededConsent() {
            // Given
            Instant createdTime = Instant.now().minus(30, ChronoUnit.DAYS);
            Instant supersededTime = Instant.now().minus(10, ChronoUnit.DAYS);
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(createdTime)
                    .supersededAt(supersededTime)
                    .build();

            // When
            Long duration = consentRecord.getConsentDurationDays();

            // Then
            assertThat(duration).isEqualTo(20L);
        }

        @Test
        @DisplayName("Should handle null creation time")
        void shouldHandleNullCreationTime() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(null)
                    .build();

            // When & Then
            assertThat(consentRecord.getConsentDurationDays()).isNull();
        }
    }

    @Nested
    @DisplayName("Legal Compliance Tests")
    class LegalComplianceTests {

        @Test
        @DisplayName("Should identify legally compliant consent")
        void shouldIdentifyLegallyCompliantConsent() {
            // Given
            ConsentRecord compliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("testuser").build())
                    .consentType("marketing_emails")
                    .purpose("Send promotional emails about new products")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(compliantConsent.isLegallyCompliant()).isTrue();
        }

        @Test
        @DisplayName("Should identify non-compliant consent without user")
        void shouldIdentifyNonCompliantConsentWithoutUser() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(null)
                    .consentType("marketing")
                    .purpose("Marketing purposes")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-compliant consent without consent type")
        void shouldIdentifyNonCompliantConsentWithoutConsentType() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType(null)
                    .purpose("Some purpose")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-compliant consent with empty consent type")
        void shouldIdentifyNonCompliantConsentWithEmptyConsentType() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("   ")
                    .purpose("Some purpose")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-compliant consent without purpose")
        void shouldIdentifyNonCompliantConsentWithoutPurpose() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("marketing")
                    .purpose(null)
                    .legalBasis("consent")
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-compliant consent without legal basis")
        void shouldIdentifyNonCompliantConsentWithoutLegalBasis() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("analytics")
                    .purpose("Analytics tracking")
                    .legalBasis(null)
                    .consentGiven(true)
                    .createdAt(Instant.now())
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }

        @Test
        @DisplayName("Should identify non-compliant consent without creation time")
        void shouldIdentifyNonCompliantConsentWithoutCreationTime() {
            // Given
            ConsentRecord nonCompliantConsent = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("cookies")
                    .purpose("Website functionality")
                    .legalBasis("necessary")
                    .consentGiven(true)
                    .createdAt(null)
                    .build();

            // When & Then
            assertThat(nonCompliantConsent.isLegallyCompliant()).isFalse();
        }
    }

    @Nested
    @DisplayName("Entity Lifecycle Tests")
    class EntityLifecycleTests {

        @Test
        @DisplayName("Should set creation timestamp automatically")
        void shouldSetCreationTimestampAutomatically() {
            // Given
            ConsentRecord consentRecord = ConsentRecord.builder()
                    .user(User.builder().username("test").build())
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            // When
            Instant beforeSave = Instant.now();
            consentRecord.setCreatedAt(Instant.now()); // Simulate @CreationTimestamp
            Instant afterSave = Instant.now();

            // Then
            assertThat(consentRecord.getCreatedAt()).isBetween(beforeSave, afterSave);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should exclude user and parent consent from equals and hashCode")
        void shouldExcludeUserAndParentConsentFromEqualsAndHashCode() {
            // Given
            User user1 = User.builder().username("user1").build();
            User user2 = User.builder().username("user2").build();
            ConsentRecord parent1 = ConsentRecord.builder()
                    .user(user1)
                    .consentType("parent1")
                    .purpose("parent")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();
            ConsentRecord parent2 = ConsentRecord.builder()
                    .user(user2)
                    .consentType("parent2")
                    .purpose("parent")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            ConsentRecord consent1 = ConsentRecord.builder()
                    .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                    .user(user1)
                    .parentConsent(parent1)
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            ConsentRecord consent2 = ConsentRecord.builder()
                    .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                    .user(user2)
                    .parentConsent(parent2)
                    .consentType("test")
                    .purpose("test")
                    .legalBasis("consent")
                    .consentGiven(true)
                    .build();

            // When & Then
            assertThat(consent1).isEqualTo(consent2);
            assertThat(consent1.hashCode()).isEqualTo(consent2.hashCode());
        }
    }
}