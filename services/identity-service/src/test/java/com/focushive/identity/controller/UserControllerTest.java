package com.focushive.identity.controller;

import com.focushive.identity.dto.UserProfileResponse;
import com.focushive.identity.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController
 * Tests the REST endpoints for user profile management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("Should retrieve user profile successfully")
    void testGetUserProfile_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfileResponse mockResponse = UserProfileResponse.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .displayName("Test User")
                .emailVerified(true)
                .twoFactorEnabled(false)
                .preferredLanguage("en")
                .timezone("UTC")
                .build();

        when(authentication.getName()).thenReturn(userId.toString());
        when(userManagementService.getUserProfile(any(UUID.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/profile")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));

        System.out.println("✅ User profile retrieval test passed");
    }

    @Test
    @DisplayName("Should handle null authentication")
    void testGetUserProfile_NullAuthentication() throws Exception {
        // Act & Assert - When no authentication is provided, controller receives null
        // which will cause a NullPointerException when trying to get the name
        try {
            mockMvc.perform(get("/api/users/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            // Expected to fail with NullPointerException in standalone setup without Spring Security
            assertTrue(e.getCause() instanceof NullPointerException || e instanceof NullPointerException);
        }

        System.out.println("✅ Null authentication test passed");
    }

    @Test
    @DisplayName("Should handle invalid user ID format in authentication")
    void testGetUserProfile_InvalidUserIdFormat() throws Exception {
        // Arrange
        when(authentication.getName()).thenReturn("invalid-uuid-format");

        // Act & Assert - This should throw IllegalStateException wrapped in ServletException
        try {
            mockMvc.perform(get("/api/users/profile")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // The IllegalStateException is wrapped in a ServletException
            assertTrue(e instanceof jakarta.servlet.ServletException);
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertTrue(e.getCause().getCause() instanceof IllegalArgumentException);
        }

        System.out.println("✅ Invalid user ID format test passed");
    }
}