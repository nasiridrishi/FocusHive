package com.focushive.identity.service;

import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.entity.User;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.security.encryption.IEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Service Email Login Tests")
class AuthenticationServiceEmailLoginTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private ITokenBlacklistService tokenBlacklistService;

    @MockBean
    private AccountLockoutService accountLockoutService;

    @Autowired
    private IEncryptionService encryptionService;

    private User testUser;
    private Persona testPersona;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "TestPassword123!";

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        personaRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user directly in the database
        testUser = User.builder()
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .emailVerified(false)
                .enabled(true)
                .build();

        testUser = userRepository.save(testUser);

        // Create a default persona for the user
        testPersona = Persona.builder()
                .user(testUser)
                .name("Default")
                .type(Persona.PersonaType.PERSONAL)
                .isDefault(true)
                .isActive(true)
                .displayName(TEST_USERNAME)
                .build();

        testPersona = personaRepository.save(testPersona);

        // Mock the account lockout service to allow login
        doNothing().when(accountLockoutService).checkAccountLockout(any(User.class));
        doNothing().when(accountLockoutService).recordSuccessfulLogin(any(User.class), anyString());
    }

    @Test
    @DisplayName("Should successfully login with username")
    void testLoginWithUsername() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // Act
        AuthenticationResponse response = authenticationService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should successfully login with email")
    void testLoginWithEmail() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        // Act
        AuthenticationResponse response = authenticationService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should successfully login with email in different case")
    void testLoginWithEmailDifferentCase() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_EMAIL.toUpperCase()); // Use uppercase email
        loginRequest.setPassword(TEST_PASSWORD);

        // Act
        AuthenticationResponse response = authenticationService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }
}