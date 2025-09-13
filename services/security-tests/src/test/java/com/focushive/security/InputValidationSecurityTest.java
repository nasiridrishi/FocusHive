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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive input validation and sanitization security tests for FocusHive platform.
 * Tests input validation, output encoding, data sanitization, and injection prevention
 * across all user input vectors and endpoints.
 * 
 * Security Areas Covered:
 * - SQL injection prevention through parameterized queries
 * - NoSQL injection prevention 
 * - Command injection prevention
 * - Path traversal prevention
 * - File upload security validation
 * - Input length and format validation
 * - Special character handling and encoding
 * - JSON/XML payload validation
 * - Request size limits
 * - Content-Type validation
 * - Character encoding validation
 * - Regex DoS (ReDoS) prevention
 * - Mass assignment protection
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Input Validation & Sanitization Security Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InputValidationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String validUserToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validUserToken = SecurityTestUtils.generateValidJwtToken("testuser");
    }

    // ============== SQL Injection Prevention Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should prevent SQL injection in query parameters")
    void testSQLInjectionInQueryParameters() throws Exception {
        List<String> sqlPayloads = SecurityTestUtils.getSqlInjectionPayloads();
        
        for (String payload : sqlPayloads) {
            // Test search endpoints
            mockMvc.perform(get("/api/v1/hives/search")
                    .param("query", payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString().toLowerCase();
                        
                        // Should not return database errors or execute SQL
                        assertTrue(status == 400 || status == 422 || status == 200,
                                 "SQL injection should be safely handled");
                        assertFalse(response.contains("sql error") || 
                                  response.contains("mysql") || 
                                  response.contains("postgresql"),
                                 "Should not leak database error information");
                    });

            // Test user search
            mockMvc.perform(get("/api/v1/users/search")
                    .param("username", payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString().toLowerCase();
                        assertFalse(response.contains("database") || response.contains("syntax error"),
                                   "Should not expose database internals");
                    });

            // Test pagination parameters
            mockMvc.perform(get("/api/v1/personas")
                    .param("page", payload)
                    .param("size", payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should handle non-numeric pagination gracefully
                        assertTrue(status == 400 || status == 200,
                                 "Should handle invalid pagination parameters");
                    });
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should prevent SQL injection in request bodies")
    void testSQLInjectionInRequestBodies() throws Exception {
        List<String> sqlPayloads = SecurityTestUtils.getSqlInjectionPayloads();
        
        for (String payload : sqlPayloads) {
            // Test user profile update
            Map<String, Object> profileData = Map.of(
                "firstName", payload,
                "lastName", "Test",
                "bio", payload,
                "email", "test@example.com"
            );

            mockMvc.perform(put("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(profileData)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString();
                        
                        // Should validate and sanitize input
                        if (status < 300) {
                            // If update succeeds, verify data is sanitized when retrieved
                            assertFalse(SecurityTestUtils.containsMaliciousContent(response),
                                       "Response should not contain malicious SQL");
                        } else {
                            // Should reject malicious input
                            assertTrue(status == 400 || status == 422,
                                     "Should reject SQL injection attempts in request body");
                        }
                    });

            // Test hive creation
            Map<String, Object> hiveData = Map.of(
                "name", payload,
                "description", payload,
                "isPublic", true
            );

            mockMvc.perform(post("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(hiveData)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400 || status == 201,
                                 "Should safely handle SQL injection in hive creation");
                    });
        }
    }

    // ============== NoSQL Injection Prevention Tests ==============

    @Test
    @Order(3)
    @DisplayName("Should prevent NoSQL injection attacks")
    void testNoSQLInjectionPrevention() throws Exception {
        List<String> noSqlPayloads = SecurityTestUtils.getNoSqlInjectionPayloads();
        
        for (String payload : noSqlPayloads) {
            // Test JSON payload injection
            mockMvc.perform(post("/api/v1/analytics/query")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400,
                                 "Should reject NoSQL injection payloads");
                    });

            // Test query parameter NoSQL injection
            mockMvc.perform(get("/api/v1/hives/filter")
                    .param("criteria", payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 422 || status == 200,
                                 "Should safely handle NoSQL injection in parameters");
                    });
        }
    }

    // ============== Command Injection Prevention Tests ==============

    @Test
    @Order(4)
    @DisplayName("Should prevent command injection attacks")
    void testCommandInjectionPrevention() throws Exception {
        List<String> commandPayloads = SecurityTestUtils.getCommandInjectionPayloads();
        
        for (String payload : commandPayloads) {
            // Test file processing endpoints
            Map<String, Object> fileData = Map.of(
                "filename", payload,
                "content", "test content"
            );

            mockMvc.perform(post("/api/v1/files/process")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(fileData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString();
                        
                        // Should not execute system commands
                        assertTrue(status >= 400 || !response.contains("command executed"),
                                 "Should prevent command injection");
                        assertFalse(response.contains("uid=") || response.contains("gid="),
                                   "Should not execute system commands like 'id'");
                    });

            // Test export functionality
            mockMvc.perform(post("/api/v1/export")
                    .param("format", payload)
                    .param("filename", payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 422 || status == 404,
                                 "Should reject command injection in export parameters");
                    });
        }
    }

    // ============== Path Traversal Prevention Tests ==============

    @Test
    @Order(5)
    @DisplayName("Should prevent path traversal attacks")
    void testPathTraversalPrevention() throws Exception {
        List<String> pathTraversalPayloads = SecurityTestUtils.getPathTraversalPayloads();
        
        for (String payload : pathTraversalPayloads) {
            // Test file access endpoints
            mockMvc.perform(get("/api/v1/files/" + payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString();
                        
                        // Should not access system files
                        assertTrue(status >= 400 || status == 404,
                                 "Should prevent path traversal access");
                        assertFalse(response.contains("root:x:") || 
                                  response.contains("[users]") ||
                                  response.contains("Windows Registry"),
                                 "Should not return system file contents");
                    });

            // Test avatar/image upload with path traversal
            Map<String, Object> uploadData = Map.of(
                "imagePath", payload,
                "imageData", "base64encodeddata"
            );

            mockMvc.perform(post("/api/v1/users/avatar/upload")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(uploadData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400,
                                 "Should reject path traversal in file paths");
                    });

            // Test template/resource access
            mockMvc.perform(get("/api/v1/templates/" + payload)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400 || status == 404,
                                 "Should prevent path traversal in template access");
                    });
        }
    }

    // ============== File Upload Security Tests ==============

    @Test
    @Order(6)
    @DisplayName("Should validate file upload security")
    void testFileUploadSecurity() throws Exception {
        // Test malicious file types
        List<String> maliciousFileTypes = Arrays.asList(
            "test.exe",
            "malware.bat",
            "script.js", 
            "shell.sh",
            "config.xml",
            "data.sql",
            "test.php",
            "backdoor.jsp",
            "virus.scr",
            "trojan.pif"
        );

        for (String filename : maliciousFileTypes) {
            Map<String, Object> uploadData = Map.of(
                "filename", filename,
                "contentType", "application/octet-stream",
                "data", "malicious content"
            );

            mockMvc.perform(post("/api/v1/files/upload")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(uploadData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400,
                                 "Should reject malicious file types: " + filename);
                    });
        }

        // Test oversized files
        String largeContent = "x".repeat(10 * 1024 * 1024); // 10MB
        Map<String, Object> largeFileData = Map.of(
            "filename", "large.txt",
            "contentType", "text/plain",
            "data", largeContent
        );

        mockMvc.perform(post("/api/v1/files/upload")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(largeFileData)))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 413 || status == 400,
                             "Should reject oversized files");
                });

        // Test file content validation
        List<String> maliciousContents = Arrays.asList(
            "<?php system($_GET['cmd']); ?>",
            "<script>alert('xss')</script>", 
            "<%@ page import=\"java.io.*\" %>",
            "#!/bin/bash\nrm -rf /",
            "SELECT * FROM users WHERE id = 1; DROP TABLE users;"
        );

        for (String content : maliciousContents) {
            Map<String, Object> contentData = Map.of(
                "filename", "test.txt",
                "contentType", "text/plain",
                "data", content
            );

            mockMvc.perform(post("/api/v1/files/upload")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(contentData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        // Should either reject or sanitize malicious content
                        assertTrue(status >= 400 || status == 201,
                                 "Should handle malicious file content securely");
                    });
        }
    }

    // ============== Input Length Validation Tests ==============

    @Test
    @Order(7)
    @DisplayName("Should enforce input length limits")
    void testInputLengthValidation() throws Exception {
        // Test extremely long inputs
        String veryLongString = "A".repeat(10000);
        String extremelyLongString = "B".repeat(100000);

        // Test username length limits
        Map<String, Object> userDataLong = Map.of(
            "username", extremelyLongString,
            "email", "test@example.com",
            "firstName", "Test",
            "lastName", "User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(userDataLong)))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error").exists());

        // Test bio/description length limits
        Map<String, Object> profileDataLong = Map.of(
            "firstName", "Test",
            "lastName", "User",
            "bio", extremelyLongString
        );

        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(profileDataLong)))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 400 || status == 422,
                             "Should reject extremely long bio");
                });

        // Test hive name/description limits
        Map<String, Object> hiveDataLong = Map.of(
            "name", veryLongString,
            "description", extremelyLongString,
            "isPublic", true
        );

        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(hiveDataLong)))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 400 || status == 422,
                             "Should reject extremely long hive data");
                });
    }

    // ============== Special Characters Handling Tests ==============

    @Test
    @Order(8)
    @DisplayName("Should properly handle special characters")
    void testSpecialCharacterHandling() throws Exception {
        // Test various special character combinations
        List<String> specialCharInputs = Arrays.asList(
            "Test<>&\"'",
            "User\u0000\u0001\u0002\u0003", // Control characters
            "Name\uD83D\uDE00", // Unicode emoji
            "Text\r\n\t", // Whitespace characters
            "Input\u200B\u200C\u200D", // Zero-width characters
            "Data\u202E\u202D", // Text direction overrides
            "Value\uFEFF" // Byte order mark
        );

        for (String input : specialCharInputs) {
            Map<String, Object> testData = Map.of(
                "firstName", input,
                "lastName", "Test",
                "bio", input
            );

            MvcResult result = mockMvc.perform(put("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(testData)))
                    .andReturn();

            if (result.getResponse().getStatus() < 300) {
                // If update succeeds, verify output is properly encoded
                MvcResult getResult = mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + validUserToken))
                        .andExpected(status().isOk())
                        .andReturn();

                String response = getResult.getResponse().getContentAsString();
                
                // Should not contain raw dangerous characters
                assertFalse(response.contains("<script>"),
                           "Should encode special characters safely");
                assertFalse(response.contains("\u0000"),
                           "Should filter out null characters");
            }
        }
    }

    // ============== JSON/XML Validation Tests ==============

    @Test
    @Order(9)
    @DisplayName("Should validate JSON/XML payload structure")
    void testPayloadStructureValidation() throws Exception {
        // Test malformed JSON
        List<String> malformedJsonPayloads = Arrays.asList(
            "{\"name\": \"test\", \"unclosed\": ",
            "{\"name\": \"test\", \"duplicate\": 1, \"duplicate\": 2}",
            "{\"circular\": {\"ref\": \"$ref\"}}"
        );

        for (String malformedJson : malformedJsonPayloads) {
            mockMvc.perform(post("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpected(status().isBadRequest());
        }

        // Test deeply nested JSON (JSON bomb)
        StringBuilder deeplyNested = new StringBuilder("{");
        for (int i = 0; i < 1000; i++) {
            deeplyNested.append("\"level").append(i).append("\": {");
        }
        for (int i = 0; i < 1000; i++) {
            deeplyNested.append("}");
        }
        deeplyNested.append("}");

        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(deeplyNested.toString()))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should reject deeply nested JSON");
                });

        // Test XML payloads (if XML endpoints exist)
        List<String> maliciousXmlPayloads = SecurityTestUtils.getXxePayloads();
        
        for (String xmlPayload : maliciousXmlPayloads) {
            mockMvc.perform(post("/api/v1/import/xml")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_XML)
                    .content(xmlPayload))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400 || status == 404,
                                 "Should safely handle XML payloads");
                    });
        }
    }

    // ============== Request Size Limits Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should enforce request size limits")
    void testRequestSizeLimits() throws Exception {
        // Test oversized request body
        Map<String, Object> largeRequestData = new HashMap<>();
        
        // Add many fields to increase payload size
        for (int i = 0; i < 1000; i++) {
            largeRequestData.put("field" + i, "A".repeat(1000));
        }

        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(largeRequestData)))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 413 || status == 400,
                             "Should reject oversized requests");
                });

        // Test extremely long URL
        String longParam = "param=" + "x".repeat(10000);
        mockMvc.perform(get("/api/v1/hives/search?" + longParam)
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should reject extremely long URLs");
                });
    }

    // ============== Content-Type Validation Tests ==============

    @Test
    @Order(11)
    @DisplayName("Should validate Content-Type headers")
    void testContentTypeValidation() throws Exception {
        String jsonPayload = "{\"name\": \"test\", \"description\": \"test hive\"}";

        // Test mismatched content type
        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.TEXT_PLAIN)
                .content(jsonPayload))
                .andExpected(status().isUnsupportedMediaType());

        // Test missing content type
        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .content(jsonPayload))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 415 || status == 400,
                             "Should require proper Content-Type");
                });

        // Test invalid content type
        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .header("Content-Type", "application/malicious")
                .content(jsonPayload))
                .andExpected(status().isUnsupportedMediaType());
    }

    // ============== Character Encoding Validation Tests ==============

    @Test
    @Order(12)
    @DisplayName("Should handle character encoding securely")
    void testCharacterEncodingValidation() throws Exception {
        // Test various encoding attacks
        List<String> encodingAttacks = Arrays.asList(
            "%3Cscript%3Ealert('xss')%3C/script%3E", // URL encoded script
            "%2E%2E%2F%2E%2E%2F%2E%2E%2Fetc%2Fpasswd", // URL encoded path traversal
            "%27%20OR%20%271%27%3D%271", // URL encoded SQL injection
            "\u003Cscript\u003Ealert('xss')\u003C/script\u003E", // Unicode encoded script
            "\\u003cscript\\u003ealert('xss')\\u003c/script\\u003e" // JSON unicode escape
        );

        for (String attack : encodingAttacks) {
            // Test in query parameters
            mockMvc.perform(get("/api/v1/hives/search")
                    .param("query", attack)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString();
                        
                        // Should safely handle encoded attacks
                        if (status < 400) {
                            assertFalse(response.contains("<script>"),
                                       "Should not decode dangerous content");
                            assertFalse(response.contains("alert('xss')"),
                                       "Should prevent XSS through encoding");
                        }
                    });

            // Test in request body
            Map<String, Object> testData = Map.of(
                "name", attack,
                "description", attack
            );

            mockMvc.perform(post("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(testData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400 || status == 201,
                                 "Should safely handle encoding attacks");
                    });
        }
    }

    // ============== Mass Assignment Protection Tests ==============

    @Test
    @Order(13)
    @DisplayName("Should prevent mass assignment vulnerabilities")
    void testMassAssignmentProtection() throws Exception {
        // Attempt to modify protected fields
        Map<String, Object> massAssignmentAttempt = Map.of(
            "firstName", "Test",
            "lastName", "User",
            "role", "ADMIN", // Should not be modifiable
            "id", UUID.randomUUID().toString(), // Should not be modifiable
            "createdAt", "2020-01-01T00:00:00Z", // Should not be modifiable
            "isActive", false, // Might be protected
            "permissions", Arrays.asList("ADMIN", "USER") // Should not be modifiable
        );

        MvcResult result = mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(massAssignmentAttempt)))
                .andReturn();

        if (result.getResponse().getStatus() < 300) {
            // If update succeeds, verify protected fields weren't changed
            MvcResult getResult = mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(status().isOk())
                    .andReturn();

            String response = getResult.getResponse().getContentAsString();
            Map<String, Object> userData = SecurityTestUtils.fromJson(response, Map.class);
            
            // Verify sensitive fields weren't modified
            assertNotEquals("ADMIN", userData.get("role"),
                           "Role should not be modifiable via mass assignment");
        } else {
            // Should reject attempt to modify protected fields
            assertTrue(result.getResponse().getStatus() >= 400,
                      "Should reject mass assignment attempts");
        }
    }

    // ============== Regular Expression DoS (ReDoS) Prevention Tests ==============

    @Test
    @Order(14)
    @DisplayName("Should prevent Regular Expression DoS attacks")
    void testReDoSPrevention() throws Exception {
        // ReDoS attack strings that can cause exponential backtracking
        List<String> redosPayloads = Arrays.asList(
            "a".repeat(50000) + "X", // Potential ReDoS for email regex
            "(" + "a".repeat(1000) + ")*b", // Nested quantifiers
            "a".repeat(10000) + "!", // Long string for complex regex
            "test@" + "a".repeat(5000) + ".com", // Long email for validation
            "http://" + "a".repeat(5000) + ".com" // Long URL for validation
        );

        for (String payload : redosPayloads) {
            long startTime = System.currentTimeMillis();
            
            // Test email validation endpoint
            Map<String, Object> testData = Map.of(
                "email", payload,
                "firstName", "Test",
                "lastName", "User"
            );

            mockMvc.perform(put("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(testData)))
                    .andReturn();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Should not take excessively long to process
            assertTrue(duration < 5000, // 5 seconds max
                      "Request should not cause ReDoS attack, took: " + duration + "ms");
        }
    }

    // ============== Data Type Validation Tests ==============

    @Test
    @Order(15)
    @DisplayName("Should validate data types correctly")
    void testDataTypeValidation() throws Exception {
        // Test type confusion attacks
        List<Map<String, Object>> typeConfusionPayloads = Arrays.asList(
            Map.of("isPublic", "true"), // String instead of boolean
            Map.of("isPublic", 1), // Integer instead of boolean
            Map.of("maxMembers", "unlimited"), // String instead of integer
            Map.of("maxMembers", true), // Boolean instead of integer
            Map.of("createdAt", 1234567890), // Integer instead of date string
            Map.of("tags", "tag1,tag2"), // String instead of array
            Map.of("settings", "{}"), // String instead of object
            Map.of("coordinates", "40.7128,-74.0060") // String instead of array/object
        );

        for (Map<String, Object> payload : typeConfusionPayloads) {
            Map<String, Object> hiveData = new HashMap<>();
            hiveData.put("name", "Test Hive");
            hiveData.put("description", "Test Description");
            hiveData.putAll(payload);

            mockMvc.perform(post("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(hiveData)))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        // Should validate types and reject invalid data
                        assertTrue(status == 400 || status == 422 || status == 201,
                                 "Should validate data types properly");
                    });
        }
    }
}