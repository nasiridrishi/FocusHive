package com.focushive.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test to diagnose OAuth2 endpoint mappings and handler registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2EndpointMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOAuth2TokenEndpointExists() throws Exception {
        // Just test that the endpoint exists and returns something other than 404
        mockMvc.perform(post("/oauth2/token"))
                .andDo(result -> {
                    System.out.println("Status: " + result.getResponse().getStatus());
                    System.out.println("Content-Type: " + result.getResponse().getContentType());
                    System.out.println("Body: " + result.getResponse().getContentAsString());
                });
    }

    @Test
    void testOAuth2WellKnownEndpointExists() throws Exception {
        // Test well-known endpoint
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andDo(result -> {
                    System.out.println("Well-known Status: " + result.getResponse().getStatus());
                    System.out.println("Well-known Content-Type: " + result.getResponse().getContentType());
                    System.out.println("Well-known Body: " + result.getResponse().getContentAsString());
                });
    }

    @Test
    void testOAuth2JwksEndpointExists() throws Exception {
        // Test JWKS endpoint
        mockMvc.perform(get("/oauth2/jwks"))
                .andDo(result -> {
                    System.out.println("JWKS Status: " + result.getResponse().getStatus());
                    System.out.println("JWKS Content-Type: " + result.getResponse().getContentType());
                    System.out.println("JWKS Body: " + result.getResponse().getContentAsString());
                });
    }
}