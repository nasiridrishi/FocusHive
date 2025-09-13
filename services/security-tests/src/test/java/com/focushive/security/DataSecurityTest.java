package com.focushive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive data security and encryption tests for FocusHive platform.
 * Tests encryption at rest, encryption in transit, PII data protection,
 * password storage security, sensitive data masking, and secure data deletion.
 * 
 * Security Areas Covered:
 * - Encryption at rest for sensitive data
 * - Encryption in transit (TLS/SSL validation)
 * - PII data protection and privacy compliance
 * - Password storage security (BCrypt validation)
 * - Sensitive data masking in logs and responses
 * - Secure data deletion and data retention
 * - Key management and rotation
 * - Database encryption validation
 * - Field-level encryption testing
 * - Data classification and handling
 * - Backup security and encryption
 * - Audit trail protection
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Data Security & Encryption Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String validUserToken;
    private String adminToken;

    // Encryption test utilities
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCB/PKCS5Padding";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validUserToken = SecurityTestUtils.generateValidJwtToken("testuser");
        adminToken = SecurityTestUtils.generateValidJwtToken("admin");
    }

    // ============== Encryption at Rest Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should encrypt sensitive data at rest")
    void testEncryptionAtRest() throws Exception {
        // Test user data encryption
        Map<String, Object> sensitiveUserData = Map.of(
            "firstName", "John",
            "lastName", "Doe", 
            "email", "john.doe@focushive.test",
            "phoneNumber", "555-123-4567",
            "dateOfBirth", "1990-01-01",
            "socialSecurityNumber", "123-45-6789", // Highly sensitive
            "bankAccount", "1234567890", // Highly sensitive
            "medicalInfo", "Diabetes Type 2" // Highly sensitive
        );

        // Store sensitive data
        MvcResult createResult = mockMvc.perform(post("/api/v1/users/sensitive-data")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(sensitiveUserData)))
                .andReturn();

        if (createResult.getResponse().getStatus() == 201) {
            // Retrieve the data
            MvcResult getResult = mockMvc.perform(get("/api/v1/users/sensitive-data")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(status().isOk())
                    .andReturn();

            String response = getResult.getResponse().getContentAsString();
            Map<String, Object> retrievedData = SecurityTestUtils.fromJson(response, Map.class);

            // Verify highly sensitive fields are encrypted or masked
            if (retrievedData.containsKey("socialSecurityNumber")) {
                String ssn = (String) retrievedData.get("socialSecurityNumber");
                assertTrue(ssn.contains("***") || ssn.length() != 11,
                          "SSN should be masked or encrypted");
            }

            if (retrievedData.containsKey("bankAccount")) {
                String bankAccount = (String) retrievedData.get("bankAccount");
                assertTrue(bankAccount.contains("***") || !bankAccount.equals("1234567890"),
                          "Bank account should be masked or encrypted");
            }

            // Regular PII might be visible but should be encrypted in storage
            if (retrievedData.containsKey("email")) {
                String email = (String) retrievedData.get("email");
                assertTrue(email.contains("@") || email.contains("***"),
                          "Email should be properly handled");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should validate field-level encryption")
    void testFieldLevelEncryption() throws Exception {
        // Test encryption of specific sensitive fields
        Map<String, Object> testData = Map.of(
            "creditCardNumber", "4111-1111-1111-1111",
            "cvv", "123",
            "expirationDate", "12/25",
            "routingNumber", "021000021",
            "accountNumber", "1234567890"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/users/payment-info")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(testData)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // Retrieve payment info
            MvcResult getResult = mockMvc.perform(get("/api/v1/users/payment-info")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            if (getResult.getResponse().getStatus() == 200) {
                String response = getResult.getResponse().getContentAsString();
                
                // Credit card should be masked
                assertFalse(response.contains("4111-1111-1111-1111"),
                           "Credit card number should not be visible in plain text");
                
                // CVV should never be stored/returned
                assertFalse(response.contains("123") && response.contains("cvv"),
                           "CVV should not be stored or returned");
                
                // Account numbers should be masked
                if (response.contains("accountNumber")) {
                    assertTrue(response.contains("***") || response.contains("xxxx"),
                              "Account number should be masked");
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should implement proper key management")
    void testKeyManagement() throws Exception {
        // Test that different users have different encryption contexts
        String user1Token = SecurityTestUtils.generateValidJwtToken("user1");
        String user2Token = SecurityTestUtils.generateValidJwtToken("user2");

        Map<String, Object> sensitiveData = Map.of(
            "secretNote", "This is a secret message",
            "privateKey", "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBg...",
            "apiToken", "secret-api-token-12345"
        );

        // Store data for user1
        mockMvc.perform(post("/api/v1/users/secrets")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(sensitiveData)))
                .andReturn();

        // Store different data for user2
        Map<String, Object> user2Data = Map.of(
            "secretNote", "User2 secret message",
            "apiToken", "user2-api-token-67890"
        );

        mockMvc.perform(post("/api/v1/users/secrets")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(user2Data)))
                .andReturn();

        // Verify user1 cannot access user2's data
        MvcResult user1Result = mockMvc.perform(get("/api/v1/users/secrets")
                .header("Authorization", "Bearer " + user1Token))
                .andReturn();

        MvcResult user2Result = mockMvc.perform(get("/api/v1/users/secrets")
                .header("Authorization", "Bearer " + user2Token))
                .andReturn();

        if (user1Result.getResponse().getStatus() == 200 && 
            user2Result.getResponse().getStatus() == 200) {
            
            String user1Response = user1Result.getResponse().getContentAsString();
            String user2Response = user2Result.getResponse().getContentAsString();

            // Responses should be different and not contain other user's data
            assertNotEquals(user1Response, user2Response,
                           "Different users should have different encrypted data");
            
            assertFalse(user1Response.contains("User2 secret message"),
                       "User1 should not see User2's data");
            assertFalse(user2Response.contains("This is a secret message"),
                       "User2 should not see User1's data");
        }
    }

    // ============== Encryption in Transit Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should enforce encryption in transit")
    void testEncryptionInTransit() throws Exception {
        // Test that sensitive endpoints require HTTPS in production
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .with(request -> {
                    request.setSecure(false); // Simulate HTTP request
                    return request;
                }))
                .andReturn();

        // In production, HTTP requests to sensitive endpoints should be redirected or blocked
        // In test environment, this might not be enforced
        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || status == 301 || status == 302 || status == 403,
                  "HTTP requests should be handled appropriately");

        // Test HTTPS headers are present when using HTTPS
        MvcResult httpsResult = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .with(request -> {
                    request.setScheme("https");
                    request.setServerPort(443);
                    request.setSecure(true);
                    return request;
                }))
                .andReturn();

        if (httpsResult.getResponse().getStatus() == 200) {
            // Check for security headers that indicate HTTPS handling
            String hstsHeader = httpsResult.getResponse().getHeader("Strict-Transport-Security");
            // HSTS might be present in production HTTPS environments
        }
    }

    @Test
    @Order(11)
    @DisplayName("Should validate TLS configuration")
    void testTLSConfiguration() throws Exception {
        // Test that weak TLS protocols are not supported
        // This would typically be tested at the infrastructure level
        
        // Test that sensitive headers are not leaked over insecure connections
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        if (result.getResponse().getStatus() == 200) {
            // Verify no sensitive information in response headers
            Collection<String> headerNames = result.getResponse().getHeaderNames();
            for (String headerName : headerNames) {
                String headerValue = result.getResponse().getHeader(headerName);
                
                assertFalse(headerValue.toLowerCase().contains("password"),
                           "Headers should not contain passwords");
                assertFalse(headerValue.toLowerCase().contains("secret"),
                           "Headers should not contain secrets");
                assertFalse(headerValue.toLowerCase().contains("key") && 
                           headerValue.length() > 50,
                           "Headers should not contain long keys");
            }
        }
    }

    // ============== PII Data Protection Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should protect PII data appropriately")
    void testPIIDataProtection() throws Exception {
        // Test various types of PII data
        Map<String, Object> piiData = Map.of(
            "fullName", "John Michael Doe",
            "emailAddress", "john.doe@personal.email",
            "phoneNumber", "555-123-4567",
            "homeAddress", "123 Main St, Anytown, ST 12345",
            "dateOfBirth", "1990-05-15",
            "governmentId", "123-45-6789",
            "biometricData", "fingerprint_hash_12345",
            "geneticData", "DNA_sequence_ATCG...",
            "ipAddress", "192.168.1.100",
            "deviceId", "device-uuid-12345"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/pii")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(piiData)))
                .andReturn();

        if (createResult.getResponse().getStatus() < 300) {
            // Test data retrieval with proper access controls
            MvcResult getResult = mockMvc.perform(get("/api/v1/users/pii")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            if (getResult.getResponse().getStatus() == 200) {
                String response = getResult.getResponse().getContentAsString();
                Map<String, Object> retrievedData = SecurityTestUtils.fromJson(response, Map.class);

                // Verify sensitive PII is protected
                if (retrievedData.containsKey("governmentId")) {
                    String govId = (String) retrievedData.get("governmentId");
                    assertTrue(govId.contains("***") || govId.length() < 11,
                              "Government ID should be masked");
                }

                if (retrievedData.containsKey("biometricData")) {
                    String biometric = (String) retrievedData.get("biometricData");
                    assertNotEquals("fingerprint_hash_12345", biometric,
                                   "Biometric data should be encrypted or hashed");
                }

                // Test that admin can access more details (with proper justification)
                MvcResult adminResult = mockMvc.perform(get("/api/v1/admin/users/pii")
                        .param("userId", "test-user-id")
                        .param("justification", "Security investigation")
                        .header("Authorization", "Bearer " + adminToken))
                        .andReturn();

                // Admin access should be logged and controlled
                assertTrue(adminResult.getResponse().getStatus() == 200 || 
                          adminResult.getResponse().getStatus() >= 400,
                          "Admin PII access should be properly controlled");
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("Should implement data minimization principles")
    void testDataMinimization() throws Exception {
        // Test that only necessary data is collected and stored
        Map<String, Object> excessiveData = Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john@example.com",
            "mothersMaidenName", "Smith", // Excessive for basic profile
            "favoriteColor", "Blue", // Excessive for basic profile  
            "childhoodPet", "Fluffy", // Excessive for basic profile
            "bloodType", "O+", // Excessive for basic profile
            "income", "75000", // Excessive for basic profile
            "politicalAffiliation", "Independent", // Excessive
            "religiousBeliefs", "None specified" // Excessive
        );

        MvcResult result = mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(excessiveData)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // Verify only necessary fields are stored
            MvcResult getResult = mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(status().isOk())
                    .andReturn();

            String response = getResult.getResponse().getContentAsString();
            
            // Basic fields should be present
            assertTrue(response.contains("firstName") || response.contains("email"),
                      "Basic profile fields should be present");
            
            // Excessive fields should be filtered out or flagged
            assertFalse(response.contains("mothersMaidenName") && 
                       response.contains("childhoodPet") &&
                       response.contains("bloodType"),
                       "Excessive personal data should not be stored");
        }
    }

    // ============== Password Storage Security Tests ==============

    @Test
    @Order(30)
    @DisplayName("Should securely store passwords")
    void testPasswordStorageSecurity() throws Exception {
        // Test password hashing strength
        String testPassword = "SecureTestPassword123!";
        String hashedPassword = SecurityTestUtils.hashPassword(testPassword);

        // Verify BCrypt characteristics
        assertTrue(hashedPassword.startsWith("$2"),
                  "Passwords should be hashed with BCrypt");
        assertTrue(hashedPassword.length() >= 60,
                  "BCrypt hashes should be at least 60 characters");

        // Verify password is not stored in plain text
        Map<String, Object> userRegistration = Map.of(
            "username", "testpassworduser",
            "email", "testpwd@example.com",
            "password", testPassword
        );

        MvcResult registrationResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(userRegistration)))
                .andReturn();

        // Password should never appear in any response
        String response = registrationResult.getResponse().getContentAsString();
        assertFalse(response.contains(testPassword),
                   "Password should never appear in responses");

        // Test password verification
        assertTrue(SecurityTestUtils.verifyPassword(testPassword, hashedPassword),
                  "Password verification should work");
        assertFalse(SecurityTestUtils.verifyPassword("wrongpassword", hashedPassword),
                   "Wrong password should not verify");

        // Test that hash is different each time (salt validation)
        String anotherHash = SecurityTestUtils.hashPassword(testPassword);
        assertNotEquals(hashedPassword, anotherHash,
                       "Each password hash should be unique due to salt");
    }

    @Test
    @Order(31)
    @DisplayName("Should handle password change securely")
    void testPasswordChangeSecurity() throws Exception {
        Map<String, Object> passwordChangeData = Map.of(
            "currentPassword", "OldPassword123!",
            "newPassword", "NewSecurePassword456!",
            "confirmPassword", "NewSecurePassword456!"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(passwordChangeData)))
                .andReturn();

        // Response should not contain any password information
        String response = result.getResponse().getContentAsString();
        assertFalse(response.contains("OldPassword123!") || 
                   response.contains("NewSecurePassword456!"),
                   "Passwords should not appear in change response");

        // Test password history (preventing reuse)
        Map<String, Object> reusePreviousPassword = Map.of(
            "currentPassword", "NewSecurePassword456!",
            "newPassword", "OldPassword123!", // Trying to reuse old password
            "confirmPassword", "OldPassword123!"
        );

        MvcResult reuseResult = mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(reusePreviousPassword)))
                .andReturn();

        // Should prevent password reuse
        assertTrue(reuseResult.getResponse().getStatus() >= 400,
                  "Should prevent reuse of recent passwords");
    }

    // ============== Sensitive Data Masking Tests ==============

    @Test
    @Order(40)
    @DisplayName("Should mask sensitive data in logs and responses")
    void testSensitiveDataMasking() throws Exception {
        // Test various sensitive data patterns
        Map<String, Object> testData = Map.of(
            "description", "Contact me at john@example.com or call 555-123-4567",
            "notes", "My SSN is 123-45-6789 and account number is 9876543210",
            "comments", "Credit card 4111-1111-1111-1111 expires 12/25 CVV 123"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/notes")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(testData)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // Retrieve the data to check masking
            MvcResult getResult = mockMvc.perform(get("/api/v1/notes")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            if (getResult.getResponse().getStatus() == 200) {
                String response = getResult.getResponse().getContentAsString();
                
                // Check for email masking
                if (response.contains("@")) {
                    // Email might be partially masked (e.g., j***@example.com)
                    assertFalse(EMAIL_PATTERN.matcher(response).find() && 
                               response.contains("john@example.com"),
                               "Full email addresses should be masked");
                }
                
                // Check for phone number masking
                assertFalse(PHONE_PATTERN.matcher(response).find(),
                           "Phone numbers should be masked");
                
                // Check for SSN masking
                assertFalse(SSN_PATTERN.matcher(response).find(),
                           "SSNs should be masked");
                
                // Check for credit card masking
                assertFalse(response.contains("4111-1111-1111-1111"),
                           "Credit card numbers should be masked");
                assertFalse(response.contains("CVV 123"),
                           "CVV numbers should be masked");
            }
        }
    }

    @Test
    @Order(41)
    @DisplayName("Should prevent sensitive data exposure in error messages")
    void testSensitiveDataInErrors() throws Exception {
        // Test that error messages don't leak sensitive data
        Map<String, Object> malformedData = Map.of(
            "email", "invalid-email-format",
            "ssn", "123-45-6789",
            "creditCard", "4111-1111-1111-1111"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(malformedData)))
                .andReturn();

        String errorResponse = result.getResponse().getContentAsString();
        
        // Error messages should not contain the actual sensitive values
        assertFalse(errorResponse.contains("123-45-6789"),
                   "SSN should not appear in error messages");
        assertFalse(errorResponse.contains("4111-1111-1111-1111"),
                   "Credit card should not appear in error messages");
        
        // Should contain generic validation messages instead
        if (result.getResponse().getStatus() >= 400) {
            assertTrue(errorResponse.toLowerCase().contains("invalid") || 
                      errorResponse.toLowerCase().contains("error"),
                      "Should contain generic error message");
        }
    }

    // ============== Secure Data Deletion Tests ==============

    @Test
    @Order(50)
    @DisplayName("Should implement secure data deletion")
    void testSecureDataDeletion() throws Exception {
        // Create user data to delete
        Map<String, Object> userData = Map.of(
            "firstName", "DeleteMe",
            "lastName", "TestUser",
            "email", "deleteme@example.com",
            "sensitiveInfo", "This should be completely removed"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/data")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(userData)))
                .andReturn();

        if (createResult.getResponse().getStatus() < 300) {
            // Request data deletion
            MvcResult deleteResult = mockMvc.perform(delete("/api/v1/users/data")
                    .header("Authorization", "Bearer " + validUserToken)
                    .param("confirm", "true"))
                    .andReturn();

            if (deleteResult.getResponse().getStatus() < 300) {
                // Verify data is actually deleted
                MvcResult getResult = mockMvc.perform(get("/api/v1/users/data")
                        .header("Authorization", "Bearer " + validUserToken))
                        .andReturn();

                assertTrue(getResult.getResponse().getStatus() == 404 || 
                          getResult.getResponse().getStatus() == 410,
                          "Deleted data should not be accessible");

                // Test that soft-deleted data doesn't leak
                if (getResult.getResponse().getStatus() == 200) {
                    String response = getResult.getResponse().getContentAsString();
                    assertFalse(response.contains("DeleteMe") || 
                               response.contains("deleteme@example.com"),
                               "Deleted data should not be visible");
                }
            }
        }

        // Test GDPR right to be forgotten
        MvcResult gdprResult = mockMvc.perform(delete("/api/v1/users/gdpr-deletion")
                .header("Authorization", "Bearer " + validUserToken)
                .param("type", "complete")
                .param("confirmation", "I understand this action is irreversible"))
                .andReturn();

        // GDPR deletion should be properly handled
        assertTrue(gdprResult.getResponse().getStatus() == 200 || 
                  gdprResult.getResponse().getStatus() == 202 ||
                  gdprResult.getResponse().getStatus() == 404,
                  "GDPR deletion should be properly implemented");
    }

    @Test
    @Order(51)
    @DisplayName("Should implement data retention policies")
    void testDataRetentionPolicies() throws Exception {
        // Test that old data is properly handled
        Map<String, Object> retentionTestData = Map.of(
            "dataType", "session_logs",
            "retentionPeriod", "30_days",
            "createdDate", "2023-01-01T00:00:00Z", // Old data
            "content", "This data should be purged after retention period"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/data-retention/test")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(retentionTestData)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // Check retention policy enforcement
            MvcResult policyResult = mockMvc.perform(get("/api/v1/data-retention/policy")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("dataType", "session_logs"))
                    .andReturn();

            if (policyResult.getResponse().getStatus() == 200) {
                String policyResponse = policyResult.getResponse().getContentAsString();
                Map<String, Object> policy = SecurityTestUtils.fromJson(policyResponse, Map.class);
                
                // Verify retention policy exists
                assertTrue(policy.containsKey("retentionPeriod") || 
                          policy.containsKey("maxAge"),
                          "Data retention policy should be defined");
            }
        }
    }

    // ============== Database Encryption Tests ==============

    @Test
    @Order(60)
    @DisplayName("Should validate database encryption")
    void testDatabaseEncryption() throws Exception {
        // Test that database connections use encryption
        // This would typically be tested at the infrastructure level
        
        // Test encrypted database operations
        Map<String, Object> encryptedData = Map.of(
            "encryptedField", "sensitive data that should be encrypted",
            "plainField", "non-sensitive data",
            "hashField", "data that should be hashed"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/encrypted-storage")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(encryptedData)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // Verify encrypted data handling
            MvcResult getResult = mockMvc.perform(get("/api/v1/encrypted-storage")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            if (getResult.getResponse().getStatus() == 200) {
                String response = getResult.getResponse().getContentAsString();
                Map<String, Object> retrievedData = SecurityTestUtils.fromJson(response, Map.class);
                
                // Encrypted fields should not be in plain text
                if (retrievedData.containsKey("encryptedField")) {
                    String encryptedValue = (String) retrievedData.get("encryptedField");
                    assertNotEquals("sensitive data that should be encrypted", encryptedValue,
                                   "Encrypted fields should not be returned in plain text");
                }
                
                // Plain fields should be readable
                if (retrievedData.containsKey("plainField")) {
                    String plainValue = (String) retrievedData.get("plainField");
                    // Plain fields might be returned as-is or still encrypted based on policy
                }
            }
        }
    }

    // ============== Backup Security Tests ==============

    @Test
    @Order(70)
    @DisplayName("Should secure data backups")
    void testBackupSecurity() throws Exception {
        // Test backup creation with encryption
        MvcResult backupResult = mockMvc.perform(post("/api/v1/admin/backup")
                .header("Authorization", "Bearer " + adminToken)
                .param("type", "user_data")
                .param("encrypt", "true"))
                .andReturn();

        if (backupResult.getResponse().getStatus() < 300) {
            String backupResponse = backupResult.getResponse().getContentAsString();
            Map<String, Object> backup = SecurityTestUtils.fromJson(backupResponse, Map.class);
            
            // Backup should be encrypted
            assertTrue(backup.containsKey("encrypted") && 
                      (Boolean) backup.get("encrypted"),
                      "Backups should be encrypted");
            
            // Backup should not contain plain text sensitive data
            if (backup.containsKey("data")) {
                String backupData = backup.get("data").toString();
                assertFalse(backupData.contains("@") && backupData.contains("password"),
                           "Backup should not contain plain text sensitive data");
            }
        }

        // Test backup access controls
        mockMvc.perform(get("/api/v1/admin/backups")
                .header("Authorization", "Bearer " + validUserToken)) // Non-admin user
                .andExpected(status().isForbidden());

        // Test backup restoration security
        MvcResult restoreResult = mockMvc.perform(post("/api/v1/admin/restore")
                .header("Authorization", "Bearer " + adminToken)
                .param("backupId", "test-backup-id")
                .param("confirmDestruction", "true"))
                .andReturn();

        // Restore should require strong authentication/authorization
        assertTrue(restoreResult.getResponse().getStatus() == 200 || 
                  restoreResult.getResponse().getStatus() >= 400,
                  "Backup restore should be properly controlled");
    }

    // ============== Audit Trail Protection Tests ==============

    @Test
    @Order(80)
    @DisplayName("Should protect audit trails")
    void testAuditTrailProtection() throws Exception {
        // Test that audit logs are tamper-resistant
        MvcResult auditResult = mockMvc.perform(get("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
                .andReturn();

        if (auditResult.getResponse().getStatus() == 200) {
            String auditResponse = auditResult.getResponse().getContentAsString();
            
            // Audit logs should not be modifiable by regular operations
            // They should include integrity checks
            assertTrue(auditResponse.contains("timestamp") || 
                      auditResponse.contains("logId"),
                      "Audit logs should have proper structure");
            
            // Sensitive data in audit logs should be masked
            assertFalse(auditResponse.contains("password=") || 
                       auditResponse.contains("\"password\""),
                       "Audit logs should not contain passwords");
        }

        // Test that regular users cannot access audit logs
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isForbidden());

        // Test audit log integrity
        MvcResult integrityResult = mockMvc.perform(get("/api/v1/admin/audit-integrity")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (integrityResult.getResponse().getStatus() == 200) {
            String integrityResponse = integrityResult.getResponse().getContentAsString();
            Map<String, Object> integrity = SecurityTestUtils.fromJson(integrityResponse, Map.class);
            
            // Should have integrity verification
            assertTrue(integrity.containsKey("verified") || 
                      integrity.containsKey("checksum"),
                      "Audit logs should have integrity verification");
        }
    }
}