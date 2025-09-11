package com.focushive.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Simple diagnostic test to check Spring Authorization Server endpoint availability.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimpleOAuth2DiagnosticTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testClientCredentialsFlowReachesEndpoint() throws Exception {
        String clientCredentialsAuth = "Basic " + java.util.Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());
        
        try {
            mockMvc.perform(post("/oauth2/token")
                    .header("Authorization", clientCredentialsAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "client_credentials")
                    .param("scope", "read"))
                    .andDo(result -> {
                        System.out.println("=== DIAGNOSTIC OUTPUT ===");
                        System.out.println("Status Code: " + result.getResponse().getStatus());
                        System.out.println("Response Headers: " + result.getResponse().getHeaderNames());
                        System.out.println("Content Type: " + result.getResponse().getContentType());
                        System.out.println("Response Body: " + result.getResponse().getContentAsString());
                        System.out.println("=== END DIAGNOSTIC OUTPUT ===");
                    });
        } catch (Exception e) {
            System.out.println("Exception during test: " + e.getMessage());
            throw e;
        }
    }
}