package com.focushive.identity.controller;

import com.focushive.identity.config.TestSecurityConfig;
import com.focushive.identity.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerMinimalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void testControllerIsRegistered() throws Exception {
        // Given - Controller should be registered in Spring context
        
        // When & Then - Make a simple request to verify controller is accessible
        // Since TestSecurityConfig permits all requests, we get 200 OK even for GET on POST endpoint
        // This proves the controller is registered and the endpoint is found in Spring context
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isOk());
    }
}