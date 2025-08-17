package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for ConsentRecord entity (GDPR compliance)
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class ConsentRecordTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedpassword")
                .displayName("Test User")
                .build();
        entityManager.persistAndFlush(testUser);
    }

    @Test
    void shouldCreateConsentRecord() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("marketing_emails")
                .purpose("Send promotional emails about new features")
                .legalBasis("consent")
                .consentGiven(true)
                .consentVersion("1.0")
                .consentSource("registration_form")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Test Browser)")
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // Then
        assertThat(savedConsent.getId()).isNotNull();
        assertThat(savedConsent.getUser()).isEqualTo(testUser);
        assertThat(savedConsent.getConsentType()).isEqualTo("marketing_emails");
        assertThat(savedConsent.getPurpose()).isEqualTo("Send promotional emails about new features");
        assertThat(savedConsent.getLegalBasis()).isEqualTo("consent");
        assertThat(savedConsent.isConsentGiven()).isTrue();
        assertThat(savedConsent.getConsentVersion()).isEqualTo("1.0");
        assertThat(savedConsent.getConsentSource()).isEqualTo("registration_form");
        assertThat(savedConsent.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(savedConsent.getUserAgent()).isEqualTo("Mozilla/5.0 (Test Browser)");
        assertThat(savedConsent.getCreatedAt()).isNotNull();
        assertThat(savedConsent.isActive()).isTrue();
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("analytics")
                .purpose("Track user behavior for improvement")
                .legalBasis("legitimate_interest")
                .consentGiven(true)
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // Then
        assertThat(savedConsent.isActive()).isTrue();
        assertThat(savedConsent.getCreatedAt()).isNotNull();
        assertThat(savedConsent.getConsentVersion()).isNull(); // Optional
        assertThat(savedConsent.getIpAddress()).isNull(); // Optional
        assertThat(savedConsent.getUserAgent()).isNull(); // Optional
    }

    @Test
    void shouldHandleConsentWithdrawal() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("data_processing")
                .purpose("Process user data for service delivery")
                .legalBasis("consent")
                .consentGiven(true)
                .consentVersion("1.0")
                .build();
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // When - User withdraws consent
        savedConsent.setConsentGiven(false);
        savedConsent.setWithdrawnAt(Instant.now());
        savedConsent.setWithdrawalReason("User requested data deletion");
        savedConsent.setActive(false);
        ConsentRecord updatedConsent = entityManager.persistAndFlush(savedConsent);

        // Then
        assertThat(updatedConsent.isConsentGiven()).isFalse();
        assertThat(updatedConsent.getWithdrawnAt()).isNotNull();
        assertThat(updatedConsent.getWithdrawalReason()).isEqualTo("User requested data deletion");
        assertThat(updatedConsent.isActive()).isFalse();
    }

    @Test
    void shouldTrackConsentExpiry() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("temporary_access")
                .purpose("Temporary data access")
                .legalBasis("consent")
                .consentGiven(true)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // Then
        assertThat(savedConsent.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedConsent.isExpired()).isFalse();
    }

    @Test
    void shouldDetectExpiredConsent() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("expired_consent")
                .purpose("Expired consent test")
                .legalBasis("consent")
                .consentGiven(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // Then
        assertThat(savedConsent.isExpired()).isTrue();
    }

    @Test
    void shouldHandleConsentRenewal() {
        // Given - Original consent
        ConsentRecord originalConsent = ConsentRecord.builder()
                .user(testUser)
                .consentType("analytics")
                .purpose("Analytics tracking")
                .legalBasis("consent")
                .consentGiven(true)
                .consentVersion("1.0")
                .build();
        ConsentRecord savedOriginal = entityManager.persistAndFlush(originalConsent);

        // When - Renewed consent with new version
        ConsentRecord renewedConsent = ConsentRecord.builder()
                .user(testUser)
                .consentType("analytics")
                .purpose("Enhanced analytics tracking with new features")
                .legalBasis("consent")
                .consentGiven(true)
                .consentVersion("2.0")
                .parentConsent(savedOriginal)
                .build();

        // Deactivate original consent
        savedOriginal.setActive(false);
        savedOriginal.setSupersededAt(Instant.now());
        entityManager.persistAndFlush(savedOriginal);

        ConsentRecord savedRenewed = entityManager.persistAndFlush(renewedConsent);

        // Then
        assertThat(savedRenewed.getParentConsent()).isEqualTo(savedOriginal);
        assertThat(savedRenewed.getConsentVersion()).isEqualTo("2.0");
        assertThat(savedRenewed.isActive()).isTrue();
        assertThat(savedOriginal.isActive()).isFalse();
        assertThat(savedOriginal.getSupersededAt()).isNotNull();
    }

    @Test
    void shouldStoreAdditionalConsentMetadata() {
        // Given
        ConsentRecord consent = ConsentRecord.builder()
                .user(testUser)
                .consentType("cookies")
                .purpose("Store user preferences and tracking cookies")
                .legalBasis("consent")
                .consentGiven(true)
                .consentVersion("1.2")
                .consentSource("cookie_banner")
                .metadata(Map.of(
                    "cookie_categories", "essential,analytics,marketing",
                    "banner_type", "modal",
                    "language", "en",
                    "gdpr_applicable", "true"
                ))
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consent);

        // Then
        assertThat(savedConsent.getMetadata()).containsEntry("cookie_categories", "essential,analytics,marketing");
        assertThat(savedConsent.getMetadata()).containsEntry("banner_type", "modal");
        assertThat(savedConsent.getMetadata()).containsEntry("language", "en");
        assertThat(savedConsent.getMetadata()).containsEntry("gdpr_applicable", "true");
    }

    @Test
    void shouldSupportDifferentLegalBases() {
        // Given - Different legal bases for GDPR compliance
        ConsentRecord consentBased = ConsentRecord.builder()
                .user(testUser)
                .consentType("marketing")
                .purpose("Marketing communications")
                .legalBasis("consent")
                .consentGiven(true)
                .build();

        ConsentRecord legitimateInterest = ConsentRecord.builder()
                .user(testUser)
                .consentType("security_monitoring")
                .purpose("Monitor for security threats")
                .legalBasis("legitimate_interest")
                .consentGiven(true)
                .build();

        ConsentRecord contractual = ConsentRecord.builder()
                .user(testUser)
                .consentType("service_delivery")
                .purpose("Provide contracted services")
                .legalBasis("contract")
                .consentGiven(true)
                .build();

        // When
        ConsentRecord savedConsent = entityManager.persistAndFlush(consentBased);
        ConsentRecord savedLegitimate = entityManager.persistAndFlush(legitimateInterest);
        ConsentRecord savedContractual = entityManager.persistAndFlush(contractual);

        // Then
        assertThat(savedConsent.getLegalBasis()).isEqualTo("consent");
        assertThat(savedLegitimate.getLegalBasis()).isEqualTo("legitimate_interest");
        assertThat(savedContractual.getLegalBasis()).isEqualTo("contract");
    }

    @Test
    void shouldValidateConsentEffectiveness() {
        // Given
        ConsentRecord validConsent = ConsentRecord.builder()
                .user(testUser)
                .consentType("data_processing")
                .purpose("Process user data")
                .legalBasis("consent")
                .consentGiven(true)
                .active(true)
                .build();

        ConsentRecord invalidConsent = ConsentRecord.builder()
                .user(testUser)
                .consentType("invalid_data")
                .purpose("Invalid processing")
                .legalBasis("consent")
                .consentGiven(false)
                .active(false)
                .build();

        // When
        ConsentRecord savedValid = entityManager.persistAndFlush(validConsent);
        ConsentRecord savedInvalid = entityManager.persistAndFlush(invalidConsent);

        // Then
        assertThat(savedValid.isEffectiveConsent()).isTrue();
        assertThat(savedInvalid.isEffectiveConsent()).isFalse();
    }
}