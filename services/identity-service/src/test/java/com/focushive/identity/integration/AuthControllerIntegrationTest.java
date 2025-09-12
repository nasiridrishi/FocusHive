package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.controller.AuthController;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.service.CookieJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController using WebMvcTest.
 * This approach tests only the controller layer and mocks the service layer,
 * avoiding complex Spring Boot application context issues.
 */
@WebMvcTest(controllers = AuthController.class)
@TestPropertySource(properties = {
    "jwt.secret=testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesAbcDefGhiJklMnoPqrStuVwxYz123456789AbcDef"
})
@EnableAutoConfiguration(exclude = {
    com.focushive.identity.config.RateLimitingConfig.class,
    com.focushive.identity.interceptor.RateLimitingInterceptor.class,
    com.focushive.identity.service.RedisRateLimiter.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private CookieJwtService cookieJwtService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthenticationResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create valid registration request
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setPassword("testpassword123");
        validRegisterRequest.setFirstName("Test");
        validRegisterRequest.setLastName("User");
        validRegisterRequest.setPersonaType("PERSONAL");

        // Create valid login request
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsernameOrEmail("testuser");
        validLoginRequest.setPassword("testpassword123");

        // Create mock response
        AuthenticationResponse.PersonaInfo personaInfo = AuthenticationResponse.PersonaInfo.builder()
                .id(UUID.randomUUID())
                .name("Default")
                .type("PERSONAL")
                .isDefault(true)
                .build();

        mockResponse = AuthenticationResponse.builder()
                .tokenType("Bearer")
                .accessToken("mock.access.token")
                .refreshToken("mock.refresh.token")
                .expiresIn(3600L)
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .displayName("testuser")
                .activePersona(personaInfo)
                .build();
    }

    @Test
    void testBasicLogin_ValidCredentials_Success() throws Exception {
        // Mock the authentication service
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(mockResponse);

        // Execute the login request
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").value("mock.access.token"))
            .andExpect(jsonPath("$.refreshToken").value("mock.refresh.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andReturn();

        // Verify the actual response content
        String responseContent = result.getResponse().getContentAsString();
        AuthenticationResponse response = objectMapper.readValue(responseContent, AuthenticationResponse.class);

        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testBasicRegistration_ValidRequest_Success() throws Exception {
        // Mock the authentication service
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(mockResponse);

        // Execute the registration request
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").value("mock.access.token"))
            .andExpect(jsonPath("$.refreshToken").value("mock.refresh.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andReturn();

        // Verify the actual response content
        String responseContent = result.getResponse().getContentAsString();
        AuthenticationResponse response = objectMapper.readValue(responseContent, AuthenticationResponse.class);

        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }
}