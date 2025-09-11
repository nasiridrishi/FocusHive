package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for PersonaProfile entity covering all business logic,
 * validation, data type conversions, metadata management, and masking functionality.
 */
@DisplayName("PersonaProfile Entity Unit Tests")
class PersonaProfileUnitTest {

    private Persona testPersona;
    private Map<String, String> testMetadata;
    private Map<String, String> testValidationRules;
    private Map<String, String> testSourceMetadata;

    @BeforeEach
    void setUp() {
        testPersona = Persona.builder()
                .id(UUID.randomUUID())
                .name("Work Persona")
                .bio("Professional activities")
                .isDefault(true)
                .build();

        testMetadata = new HashMap<>();
        testMetadata.put("display_name", "Professional Email");
        testMetadata.put("priority", "high");

        testValidationRules = new HashMap<>();
        testValidationRules.put("min_length", "5");
        testValidationRules.put("max_length", "100");

        testSourceMetadata = new HashMap<>();
        testSourceMetadata.put("import_source", "linkedin");
        testSourceMetadata.put("import_date", "2024-01-01");
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create PersonaProfile using builder with all fields")
        void shouldCreatePersonaProfileUsingBuilderWithAllFields() {
            // Given
            UUID profileId = UUID.randomUUID();
            Instant now = Instant.now();

            // When
            PersonaProfile profile = PersonaProfile.builder()
                    .id(profileId)
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("john.doe@company.com")
                    .category("contact_info")
                    .dataType("EMAIL")
                    .visibility("PUBLIC")
                    .description("Primary work email address")
                    .enabled(true)
                    .displayOrder(1)
                    .requiredField(true)
                    .userEditable(true)
                    .source("user_input")
                    .metadata(testMetadata)
                    .validationRules(testValidationRules)
                    .sourceMetadata(testSourceMetadata)
                    .verifiedAt(now)
                    .verificationMethod("email_verification")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // Then
            assertThat(profile.getId()).isEqualTo(profileId);
            assertThat(profile.getPersona()).isEqualTo(testPersona);
            assertThat(profile.getProfileKey()).isEqualTo("email");
            assertThat(profile.getProfileValue()).isEqualTo("john.doe@company.com");
            assertThat(profile.getCategory()).isEqualTo("contact_info");
            assertThat(profile.getDataType()).isEqualTo("EMAIL");
            assertThat(profile.getVisibility()).isEqualTo("PUBLIC");
            assertThat(profile.getDescription()).isEqualTo("Primary work email address");
            assertThat(profile.isEnabled()).isTrue();
            assertThat(profile.getDisplayOrder()).isEqualTo(1);
            assertThat(profile.isRequiredField()).isTrue();
            assertThat(profile.isUserEditable()).isTrue();
            assertThat(profile.getSource()).isEqualTo("user_input");
            assertThat(profile.getVerifiedAt()).isEqualTo(now);
            assertThat(profile.getVerificationMethod()).isEqualTo("email_verification");
        }

        @Test
        @DisplayName("Should create PersonaProfile with default values")
        void shouldCreatePersonaProfileWithDefaultValues() {
            // When
            PersonaProfile profile = PersonaProfile.builder()
                    .persona(testPersona)
                    .profileKey("name")
                    .profileValue("John Doe")
                    .build();

            // Then
            assertThat(profile.getDataType()).isEqualTo("STRING");
            assertThat(profile.getVisibility()).isEqualTo("PRIVATE");
            assertThat(profile.isEnabled()).isTrue();
            assertThat(profile.getDisplayOrder()).isEqualTo(0);
            assertThat(profile.isRequiredField()).isFalse();
            assertThat(profile.isUserEditable()).isTrue();
            assertThat(profile.getSource()).isEqualTo("user_input");
            assertThat(profile.getMetadata()).isNotNull().isEmpty();
            assertThat(profile.getValidationRules()).isNotNull().isEmpty();
            assertThat(profile.getSourceMetadata()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create PersonaProfile using no-args constructor")
        void shouldCreatePersonaProfileUsingNoArgsConstructor() {
            // When
            PersonaProfile profile = new PersonaProfile();

            // Then
            assertThat(profile.getId()).isNull();
            assertThat(profile.getPersona()).isNull();
            assertThat(profile.getProfileKey()).isNull();
            assertThat(profile.getProfileValue()).isNull();
            assertThat(profile.getMetadata()).isNotNull();
            assertThat(profile.getValidationRules()).isNotNull();
            assertThat(profile.getSourceMetadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Data Type Conversion Tests")
    class DataTypeConversionTests {

        @Test
        @DisplayName("Should get boolean value correctly")
        void shouldGetBooleanValueCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("enabled", "true", "BOOLEAN");

            // When & Then
            assertThat(profile.getBooleanValue()).isTrue();

            // Test false value
            profile.setProfileValue("false");
            assertThat(profile.getBooleanValue()).isFalse();

            // Test non-boolean data type
            profile.setDataType("STRING");
            assertThat(profile.getBooleanValue()).isNull();
        }

        @Test
        @DisplayName("Should get numeric value correctly")
        void shouldGetNumericValueCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("score", "85.5", "NUMBER");

            // When & Then
            assertThat(profile.getNumericValue()).isEqualTo(85.5);

            // Test integer value
            profile.setProfileValue("100");
            assertThat(profile.getNumericValue()).isEqualTo(100.0);

            // Test invalid numeric value
            profile.setProfileValue("not_a_number");
            assertThat(profile.getNumericValue()).isNull();

            // Test non-numeric data type
            profile.setDataType("STRING");
            assertThat(profile.getNumericValue()).isNull();
        }

        @Test
        @DisplayName("Should set boolean value correctly")
        void shouldSetBooleanValueCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("enabled", "false", "STRING");

            // When
            profile.setBooleanValue(true);

            // Then
            assertThat(profile.getProfileValue()).isEqualTo("true");
            assertThat(profile.getDataType()).isEqualTo("BOOLEAN");
            assertThat(profile.getBooleanValue()).isTrue();
        }

        @Test
        @DisplayName("Should set numeric value correctly")
        void shouldSetNumericValueCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("age", "0", "STRING");

            // When
            profile.setNumericValue(25);

            // Then
            assertThat(profile.getProfileValue()).isEqualTo("25");
            assertThat(profile.getDataType()).isEqualTo("NUMBER");
            assertThat(profile.getNumericValue()).isEqualTo(25.0);

            // Test with double value
            profile.setNumericValue(25.5);
            assertThat(profile.getProfileValue()).isEqualTo("25.5");
            assertThat(profile.getNumericValue()).isEqualTo(25.5);
        }
    }

    @Nested
    @DisplayName("Metadata Management Tests")
    class MetadataManagementTests {

        @Test
        @DisplayName("Should add and get metadata correctly")
        void shouldAddAndGetMetadataCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("email", "test@example.com", "EMAIL");

            // When
            profile.addMetadata("display_name", "Work Email");
            profile.addMetadata("priority", "high");

            // Then
            assertThat(profile.getMetadata("display_name")).isEqualTo("Work Email");
            assertThat(profile.getMetadata("priority")).isEqualTo("high");
            assertThat(profile.getMetadata("non_existent")).isNull();
        }

        @Test
        @DisplayName("Should handle null metadata map when adding")
        void shouldHandleNullMetadataMapWhenAdding() {
            // Given
            PersonaProfile profile = createTestProfile("test", "value", "STRING");
            profile.setMetadata(null);

            // When
            profile.addMetadata("key", "value");

            // Then
            assertThat(profile.getMetadata()).isNotNull();
            assertThat(profile.getMetadata("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("Should add and get validation rules correctly")
        void shouldAddAndGetValidationRulesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            // When
            profile.addValidationRule("min_length", "2");
            profile.addValidationRule("max_length", "50");

            // Then
            assertThat(profile.getValidationRule("min_length")).isEqualTo("2");
            assertThat(profile.getValidationRule("max_length")).isEqualTo("50");
            assertThat(profile.getValidationRule("pattern")).isNull();
        }

        @Test
        @DisplayName("Should handle null validation rules map when adding")
        void shouldHandleNullValidationRulesMapWhenAdding() {
            // Given
            PersonaProfile profile = createTestProfile("test", "value", "STRING");
            profile.setValidationRules(null);

            // When
            profile.addValidationRule("required", "true");

            // Then
            assertThat(profile.getValidationRules()).isNotNull();
            assertThat(profile.getValidationRule("required")).isEqualTo("true");
        }

        @Test
        @DisplayName("Should add and get source metadata correctly")
        void shouldAddAndGetSourceMetadataCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("email", "test@example.com", "EMAIL");

            // When
            profile.addSourceMetadata("import_source", "linkedin");
            profile.addSourceMetadata("import_date", "2024-01-01");

            // Then
            assertThat(profile.getSourceMetadata("import_source")).isEqualTo("linkedin");
            assertThat(profile.getSourceMetadata("import_date")).isEqualTo("2024-01-01");
            assertThat(profile.getSourceMetadata("non_existent")).isNull();
        }

        @Test
        @DisplayName("Should handle null source metadata map when adding")
        void shouldHandleNullSourceMetadataMapWhenAdding() {
            // Given
            PersonaProfile profile = createTestProfile("test", "value", "STRING");
            profile.setSourceMetadata(null);

            // When
            profile.addSourceMetadata("source", "manual");

            // Then
            assertThat(profile.getSourceMetadata()).isNotNull();
            assertThat(profile.getSourceMetadata("source")).isEqualTo("manual");
        }
    }

    @Nested
    @DisplayName("Value Validation Tests")
    class ValueValidationTests {

        @Test
        @DisplayName("Should validate boolean values correctly")
        void shouldValidateBooleanValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("enabled", "true", "BOOLEAN");

            // Valid boolean values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("false");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("TRUE");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("False");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid boolean values
            profile.setProfileValue("yes");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("1");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate number values correctly")
        void shouldValidateNumberValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("age", "25", "NUMBER");

            // Valid number values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("25.5");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("-10");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("0");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid number values
            profile.setProfileValue("not_a_number");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("25.5.5");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate email values correctly")
        void shouldValidateEmailValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("email", "test@example.com", "EMAIL");

            // Valid email values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("user.name+tag@domain.co.uk");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("123@456.org");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid email values
            profile.setProfileValue("invalid_email");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("@example.com");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("test@");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate phone values correctly")
        void shouldValidatePhoneValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("phone", "+1-555-123-4567", "PHONE");

            // Valid phone values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("555-123-4567");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("(555) 123-4567");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("5551234567");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid phone values
            profile.setProfileValue("invalid_phone");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("123-abc-4567");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate URL values correctly")
        void shouldValidateUrlValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("website", "https://example.com", "URL");

            // Valid URL values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("http://example.com");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("https://subdomain.example.com/path?query=1");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid URL values
            profile.setProfileValue("ftp://example.com");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("example.com");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("not_a_url");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate date values correctly")
        void shouldValidateDateValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("birth_date", "2024-01-01T00:00:00Z", "DATE");

            // Valid date values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("2024-12-31T23:59:59.999Z");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid date values
            profile.setProfileValue("2024-01-01");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("not_a_date");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("2024-13-01T00:00:00Z");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate JSON values correctly")
        void shouldValidateJsonValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("preferences", "{\"theme\":\"dark\"}", "JSON");

            // Valid JSON values
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("[1, 2, 3]");
            assertThat(profile.isValidValue()).isTrue();

            // Invalid JSON values (note: this is a simple validation, not actual JSON parsing)
            profile.setProfileValue("not_json");
            assertThat(profile.isValidValue()).isFalse();

            profile.setProfileValue("simple_string");
            assertThat(profile.isValidValue()).isFalse();
        }

        @Test
        @DisplayName("Should validate string values correctly")
        void shouldValidateStringValuesCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John Doe", "STRING");

            // String values are always valid
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("");
            assertThat(profile.isValidValue()).isTrue();

            profile.setProfileValue("Any string value 123!@#");
            assertThat(profile.isValidValue()).isTrue();
        }

        @Test
        @DisplayName("Should handle empty values for required fields")
        void shouldHandleEmptyValuesForRequiredFields() {
            // Given
            PersonaProfile profile = createTestProfile("name", "", "STRING");
            profile.setRequiredField(true);

            // Empty value for required field
            assertThat(profile.isValidValue()).isFalse();

            // Non-empty value for required field
            profile.setProfileValue("John");
            assertThat(profile.isValidValue()).isTrue();

            // Empty value for non-required field
            profile.setRequiredField(false);
            profile.setProfileValue("");
            assertThat(profile.isValidValue()).isTrue();
        }

        @Test
        @DisplayName("Should handle null profile values")
        void shouldHandleNullProfileValues() {
            // Given
            PersonaProfile profile = createTestProfile("name", null, "STRING");

            // Null value for non-required field
            assertThat(profile.isValidValue()).isTrue();

            // Null value for required field
            profile.setRequiredField(true);
            assertThat(profile.isValidValue()).isFalse();
        }
    }

    @Nested
    @DisplayName("Visibility Tests")
    class VisibilityTests {

        @Test
        @DisplayName("Should check public visibility correctly")
        void shouldCheckPublicVisibilityCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            profile.setVisibility("PUBLIC");
            assertThat(profile.isPublic()).isTrue();
            assertThat(profile.isFriendsVisible()).isTrue();
            assertThat(profile.isPrivate()).isFalse();

            profile.setVisibility("FRIENDS");
            assertThat(profile.isPublic()).isFalse();

            profile.setVisibility("PRIVATE");
            assertThat(profile.isPublic()).isFalse();
        }

        @Test
        @DisplayName("Should check friends visibility correctly")
        void shouldCheckFriendsVisibilityCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            profile.setVisibility("FRIENDS");
            assertThat(profile.isFriendsVisible()).isTrue();
            assertThat(profile.isPublic()).isFalse();
            assertThat(profile.isPrivate()).isFalse();

            profile.setVisibility("PUBLIC");
            assertThat(profile.isFriendsVisible()).isTrue(); // PUBLIC is also friends visible

            profile.setVisibility("PRIVATE");
            assertThat(profile.isFriendsVisible()).isFalse();
        }

        @Test
        @DisplayName("Should check private visibility correctly")
        void shouldCheckPrivateVisibilityCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            profile.setVisibility("PRIVATE");
            assertThat(profile.isPrivate()).isTrue();
            assertThat(profile.isPublic()).isFalse();
            assertThat(profile.isFriendsVisible()).isFalse();

            profile.setVisibility("PUBLIC");
            assertThat(profile.isPrivate()).isFalse();

            profile.setVisibility("FRIENDS");
            assertThat(profile.isPrivate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should check verification status correctly")
        void shouldCheckVerificationStatusCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("email", "test@example.com", "EMAIL");

            // Not verified initially
            assertThat(profile.isVerified()).isFalse();

            // Verified when both fields are set
            profile.setVerifiedAt(Instant.now());
            profile.setVerificationMethod("email_verification");
            assertThat(profile.isVerified()).isTrue();

            // Not verified when only one field is set
            profile.setVerificationMethod(null);
            assertThat(profile.isVerified()).isFalse();

            profile.setVerificationMethod("email_verification");
            profile.setVerifiedAt(null);
            assertThat(profile.isVerified()).isFalse();
        }

        @Test
        @DisplayName("Should mark as verified correctly")
        void shouldMarkAsVerifiedCorrectly() {
            // Given
            PersonaProfile profile = createTestProfile("phone", "+1-555-123-4567", "PHONE");
            Instant beforeVerification = Instant.now().minusSeconds(1);

            // When
            profile.markAsVerified("sms_verification");

            // Then
            assertThat(profile.isVerified()).isTrue();
            assertThat(profile.getVerificationMethod()).isEqualTo("sms_verification");
            assertThat(profile.getVerifiedAt()).isNotNull();
            assertThat(profile.getVerifiedAt()).isAfter(beforeVerification);
        }

        @Test
        @DisplayName("Should check if verification is needed")
        void shouldCheckIfVerificationIsNeeded() {
            // Given
            PersonaProfile emailProfile = createTestProfile("email", "test@example.com", "EMAIL");
            PersonaProfile phoneProfile = createTestProfile("phone", "555-123-4567", "PHONE");
            PersonaProfile stringProfile = createTestProfile("name", "John", "STRING");

            // Email and phone fields need verification when not verified
            assertThat(emailProfile.needsVerification()).isTrue();
            assertThat(phoneProfile.needsVerification()).isTrue();
            assertThat(stringProfile.needsVerification()).isFalse();

            // Email and phone fields don't need verification when already verified
            emailProfile.markAsVerified("email_verification");
            phoneProfile.markAsVerified("sms_verification");
            assertThat(emailProfile.needsVerification()).isFalse();
            assertThat(phoneProfile.needsVerification()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Rules Tests")
    class ValidationRulesTests {

        @Test
        @DisplayName("Should validate min length rule")
        void shouldValidateMinLengthRule() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");
            profile.addValidationRule("min_length", "3");

            // Valid length
            assertThat(profile.passesValidation()).isTrue();

            // Invalid length (too short)
            profile.setProfileValue("Jo");
            assertThat(profile.passesValidation()).isFalse();

            // Edge case - exactly minimum length
            profile.setProfileValue("Joe");
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should validate max length rule")
        void shouldValidateMaxLengthRule() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");
            profile.addValidationRule("max_length", "10");

            // Valid length
            assertThat(profile.passesValidation()).isTrue();

            // Invalid length (too long)
            profile.setProfileValue("Very Long Name That Exceeds Limit");
            assertThat(profile.passesValidation()).isFalse();

            // Edge case - exactly maximum length
            profile.setProfileValue("1234567890");
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should validate pattern rule")
        void shouldValidatePatternRule() {
            // Given
            PersonaProfile profile = createTestProfile("code", "ABC123", "STRING");
            profile.addValidationRule("pattern", "[A-Z]{3}[0-9]{3}");

            // Valid pattern
            assertThat(profile.passesValidation()).isTrue();

            // Invalid pattern
            profile.setProfileValue("abc123");
            assertThat(profile.passesValidation()).isFalse();

            profile.setProfileValue("ABC12");
            assertThat(profile.passesValidation()).isFalse();
        }

        @Test
        @DisplayName("Should validate required rule")
        void shouldValidateRequiredRule() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");
            profile.addValidationRule("required", "true");

            // Valid non-empty value
            assertThat(profile.passesValidation()).isTrue();

            // Invalid empty value
            profile.setProfileValue("");
            assertThat(profile.passesValidation()).isFalse();

            // Invalid null value
            profile.setProfileValue(null);
            assertThat(profile.passesValidation()).isFalse();

            // Valid when not required
            profile.addValidationRule("required", "false");
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should handle invalid validation rule values")
        void shouldHandleInvalidValidationRuleValues() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            // Invalid min_length value (should pass by default)
            profile.addValidationRule("min_length", "not_a_number");
            assertThat(profile.passesValidation()).isTrue();

            // Invalid max_length value (should pass by default)
            profile.addValidationRule("max_length", "invalid");
            assertThat(profile.passesValidation()).isTrue();

            // Invalid regex pattern (should pass by default)
            profile.addValidationRule("pattern", "[invalid");
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should handle unknown validation rules")
        void shouldHandleUnknownValidationRules() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");
            profile.addValidationRule("unknown_rule", "some_value");

            // Unknown rules should pass by default
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should pass validation when no rules are set")
        void shouldPassValidationWhenNoRulesAreSet() {
            // Given
            PersonaProfile profile = createTestProfile("name", "John", "STRING");

            // Should pass when no validation rules
            assertThat(profile.passesValidation()).isTrue();

            // Should also pass when validation rules map is null
            profile.setValidationRules(null);
            assertThat(profile.passesValidation()).isTrue();
        }

        @Test
        @DisplayName("Should fail validation when base value validation fails")
        void shouldFailValidationWhenBaseValueValidationFails() {
            // Given
            PersonaProfile profile = createTestProfile("email", "invalid_email", "EMAIL");
            profile.addValidationRule("min_length", "1"); // This rule would pass

            // Should fail because base email validation fails
            assertThat(profile.passesValidation()).isFalse();
        }
    }

    @Nested
    @DisplayName("Profile Copying Tests")
    class ProfileCopyingTests {

        @Test
        @DisplayName("Should copy profile for another persona correctly")
        void shouldCopyProfileForAnotherPersonaCorrectly() {
            // Given
            Persona targetPersona = Persona.builder()
                    .id(UUID.randomUUID())
                    .name("Personal Persona")
                    .build();

            PersonaProfile originalProfile = PersonaProfile.builder()
                    .id(UUID.randomUUID())
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("test@example.com")
                    .category("contact")
                    .dataType("EMAIL")
                    .visibility("PUBLIC")
                    .description("Work email")
                    .enabled(true)
                    .displayOrder(1)
                    .requiredField(true)
                    .userEditable(true)
                    .source("user_input")
                    .metadata(testMetadata)
                    .validationRules(testValidationRules)
                    .sourceMetadata(testSourceMetadata)
                    .build();

            // When
            PersonaProfile copiedProfile = originalProfile.copyForPersona(targetPersona);

            // Then
            assertThat(copiedProfile.getId()).isNull(); // New profile, no ID yet
            assertThat(copiedProfile.getPersona()).isEqualTo(targetPersona);
            assertThat(copiedProfile.getProfileKey()).isEqualTo(originalProfile.getProfileKey());
            assertThat(copiedProfile.getProfileValue()).isEqualTo(originalProfile.getProfileValue());
            assertThat(copiedProfile.getCategory()).isEqualTo(originalProfile.getCategory());
            assertThat(copiedProfile.getDataType()).isEqualTo(originalProfile.getDataType());
            assertThat(copiedProfile.getVisibility()).isEqualTo(originalProfile.getVisibility());
            assertThat(copiedProfile.getDescription()).isEqualTo(originalProfile.getDescription());
            assertThat(copiedProfile.isEnabled()).isEqualTo(originalProfile.isEnabled());
            assertThat(copiedProfile.getDisplayOrder()).isEqualTo(originalProfile.getDisplayOrder());
            assertThat(copiedProfile.isRequiredField()).isEqualTo(originalProfile.isRequiredField());
            assertThat(copiedProfile.isUserEditable()).isEqualTo(originalProfile.isUserEditable());
            assertThat(copiedProfile.getSource()).isEqualTo("copied");

            // Maps should be copied, not the same instance
            assertThat(copiedProfile.getMetadata()).isEqualTo(originalProfile.getMetadata());
            assertThat(copiedProfile.getMetadata()).isNotSameAs(originalProfile.getMetadata());
            
            assertThat(copiedProfile.getValidationRules()).isEqualTo(originalProfile.getValidationRules());
            assertThat(copiedProfile.getValidationRules()).isNotSameAs(originalProfile.getValidationRules());
        }

        @Test
        @DisplayName("Should handle null maps when copying profile")
        void shouldHandleNullMapsWhenCopyingProfile() {
            // Given
            Persona targetPersona = Persona.builder().id(UUID.randomUUID()).name("Target").build();
            PersonaProfile originalProfile = createTestProfile("name", "John", "STRING");
            originalProfile.setMetadata(null);
            originalProfile.setValidationRules(null);

            // When
            PersonaProfile copiedProfile = originalProfile.copyForPersona(targetPersona);

            // Then
            assertThat(copiedProfile.getMetadata()).isNotNull().isEmpty();
            assertThat(copiedProfile.getValidationRules()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Display Value and Masking Tests")
    class DisplayValueAndMaskingTests {

        @Test
        @DisplayName("Should return original value for verified fields")
        void shouldReturnOriginalValueForVerifiedFields() {
            // Given
            PersonaProfile emailProfile = createTestProfile("email", "john.doe@example.com", "EMAIL");
            emailProfile.markAsVerified("email_verification");

            PersonaProfile phoneProfile = createTestProfile("phone", "+1-555-123-4567", "PHONE");
            phoneProfile.markAsVerified("sms_verification");

            // When & Then
            assertThat(emailProfile.getDisplayValue()).isEqualTo("john.doe@example.com");
            assertThat(phoneProfile.getDisplayValue()).isEqualTo("+1-555-123-4567");
        }

        @Test
        @DisplayName("Should mask unverified email addresses")
        void shouldMaskUnverifiedEmailAddresses() {
            // Given
            PersonaProfile profile = createTestProfile("email", "john.doe@example.com", "EMAIL");

            // When & Then
            assertThat(profile.getDisplayValue()).isEqualTo("j***e@example.com");

            // Test short username
            profile.setProfileValue("jo@example.com");
            assertThat(profile.getDisplayValue()).isEqualTo("*@example.com");

            // Test single character username
            profile.setProfileValue("j@example.com");
            assertThat(profile.getDisplayValue()).isEqualTo("*@example.com");
        }

        @Test
        @DisplayName("Should mask unverified phone numbers")
        void shouldMaskUnverifiedPhoneNumbers() {
            // Given
            PersonaProfile profile = createTestProfile("phone", "+1-555-123-4567", "PHONE");

            // When & Then
            assertThat(profile.getDisplayValue()).isEqualTo("***-***-4567");

            // Test phone without formatting
            profile.setProfileValue("5551234567");
            assertThat(profile.getDisplayValue()).isEqualTo("***-***-4567");

            // Test short phone number
            profile.setProfileValue("123");
            assertThat(profile.getDisplayValue()).isEqualTo("***");
        }

        @Test
        @DisplayName("Should handle invalid email formats when masking")
        void shouldHandleInvalidEmailFormatsWhenMasking() {
            // Given
            PersonaProfile profile = createTestProfile("email", "invalid_email", "EMAIL");

            // When & Then
            assertThat(profile.getDisplayValue()).isEqualTo("***");

            // Test email without @ symbol
            profile.setProfileValue("notemail");
            assertThat(profile.getDisplayValue()).isEqualTo("***");
        }

        @Test
        @DisplayName("Should handle null profile value in display")
        void shouldHandleNullProfileValueInDisplay() {
            // Given
            PersonaProfile profile = createTestProfile("name", null, "STRING");

            // When & Then
            assertThat(profile.getDisplayValue()).isEqualTo("");
        }

        @Test
        @DisplayName("Should return original value for non-sensitive data types")
        void shouldReturnOriginalValueForNonSensitiveDataTypes() {
            // Given
            PersonaProfile stringProfile = createTestProfile("name", "John Doe", "STRING");
            PersonaProfile numberProfile = createTestProfile("age", "25", "NUMBER");

            // When & Then
            assertThat(stringProfile.getDisplayValue()).isEqualTo("John Doe");
            assertThat(numberProfile.getDisplayValue()).isEqualTo("25");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should test equality based on ID")
        void shouldTestEqualityBasedOnId() {
            // Given
            UUID sameId = UUID.randomUUID();
            PersonaProfile profile1 = PersonaProfile.builder()
                    .id(sameId)
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("test1@example.com")
                    .build();

            PersonaProfile profile2 = PersonaProfile.builder()
                    .id(sameId)
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("test2@example.com") // Different value
                    .build();

            // Then
            assertThat(profile1).isEqualTo(profile2);
            assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode());
        }

        @Test
        @DisplayName("Should test inequality with different IDs")
        void shouldTestInequalityWithDifferentIds() {
            // Given
            PersonaProfile profile1 = PersonaProfile.builder()
                    .id(UUID.randomUUID())
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("test@example.com")
                    .build();

            PersonaProfile profile2 = PersonaProfile.builder()
                    .id(UUID.randomUUID())
                    .persona(testPersona)
                    .profileKey("email")
                    .profileValue("test@example.com")
                    .build();

            // Then
            assertThat(profile1).isNotEqualTo(profile2);
            assertThat(profile1.hashCode()).isNotEqualTo(profile2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should exclude persona from toString")
        void shouldExcludePersonaFromToString() {
            // Given
            PersonaProfile profile = createTestProfile("email", "test@example.com", "EMAIL");

            // When
            String toString = profile.toString();

            // Then
            assertThat(toString).isNotNull();
            assertThat(toString).contains("profileKey");
            assertThat(toString).contains("profileValue");
            assertThat(toString).doesNotContain("persona=");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling Tests")
    class EdgeCasesAndNullHandlingTests {

        @Test
        @DisplayName("Should handle null values gracefully in business methods")
        void shouldHandleNullValuesGracefullyInBusinessMethods() {
            // Given
            PersonaProfile profile = new PersonaProfile();

            // When & Then - should not throw exceptions
            assertThatCode(() -> profile.getBooleanValue()).doesNotThrowAnyException();
            assertThatCode(() -> profile.getNumericValue()).doesNotThrowAnyException();
            assertThatCode(() -> profile.isValidValue()).doesNotThrowAnyException();
            assertThatCode(() -> profile.isPublic()).doesNotThrowAnyException();
            assertThatCode(() -> profile.isFriendsVisible()).doesNotThrowAnyException();
            assertThatCode(() -> profile.isPrivate()).doesNotThrowAnyException();
            assertThatCode(() -> profile.isVerified()).doesNotThrowAnyException();
            assertThatCode(() -> profile.needsVerification()).doesNotThrowAnyException();
            assertThatCode(() -> profile.passesValidation()).doesNotThrowAnyException();
            assertThatCode(() -> profile.getDisplayValue()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in profile values")
        void shouldHandleSpecialCharactersInProfileValues() {
            // Given
            String specialChars = "Special!@#$%^&*()";
            String unicodeChars = "üñïçødé 测试";

            PersonaProfile profile1 = createTestProfile("name", specialChars, "STRING");
            PersonaProfile profile2 = createTestProfile("description", unicodeChars, "STRING");

            // When & Then
            assertThat(profile1.getProfileValue()).isEqualTo(specialChars);
            assertThat(profile1.isValidValue()).isTrue();
            
            assertThat(profile2.getProfileValue()).isEqualTo(unicodeChars);
            assertThat(profile2.isValidValue()).isTrue();
        }

        @Test
        @DisplayName("Should handle whitespace in profile values")
        void shouldHandleWhitespaceInProfileValues() {
            // Given
            PersonaProfile profile = createTestProfile("email", "  test@example.com  ", "EMAIL");

            // Email validation should handle trimming
            assertThat(profile.isValidValue()).isTrue();

            // Test with only whitespace
            profile.setProfileValue("   ");
            profile.setRequiredField(true);
            assertThat(profile.isValidValue()).isFalse();

            profile.setRequiredField(false);
            assertThat(profile.isValidValue()).isTrue();
        }

        @Test
        @DisplayName("Should handle very long profile values")
        void shouldHandleVeryLongProfileValues() {
            // Given
            String longValue = "a".repeat(1000);
            PersonaProfile profile = createTestProfile("description", longValue, "STRING");

            // When & Then
            assertThat(profile.getProfileValue()).hasSize(1000);
            assertThat(profile.isValidValue()).isTrue();

            // Test with max length validation
            profile.addValidationRule("max_length", "500");
            assertThat(profile.passesValidation()).isFalse();
        }
    }

    private PersonaProfile createTestProfile(String key, String value, String dataType) {
        return PersonaProfile.builder()
                .id(UUID.randomUUID())
                .persona(testPersona)
                .profileKey(key)
                .profileValue(value)
                .dataType(dataType)
                .build();
    }
}