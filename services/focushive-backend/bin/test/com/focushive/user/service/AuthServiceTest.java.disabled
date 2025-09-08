package com.focushive.user.service;

import com.focushive.api.security.JwtTokenProvider;
import com.focushive.events.UserRegisteredEvent;
import com.focushive.user.controller.AuthController;
import com.focushive.user.dto.RegisterRequest;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("hashedPassword");
        testUser.setDisplayName("Test User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setDisplayName("New User");
    }

    @Test
    void register_withValidRequest_createsUserAndReturnsToken() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("new-user-id");
            return user;
        });
        when(tokenProvider.generateToken(any(User.class))).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthService.AuthenticationResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo("new-user-id");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("newuser@example.com");

        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals(registerRequest.getEmail()) &&
            user.getUsername().equals(registerRequest.getUsername()) &&
            user.getPassword().equals("hashedPassword") &&
            user.getDisplayName().equals(registerRequest.getDisplayName())
        ));
    }

    @Test
    void register_withExistingEmail_throwsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void register_withExistingUsername_throwsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(testUser)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh-token");

        AuthService.AuthenticationResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo("test-user-id");
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void login_withInvalidCredentials_throwsException() {
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void refreshToken_withValidToken_returnsNewToken() {
        AuthController.RefreshRequest refreshRequest = new AuthController.RefreshRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken(testUser)).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        AuthService.AuthenticationResponse response = authService.refreshToken(refreshRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.userId()).isEqualTo("test-user-id");
    }

    @Test
    void refreshToken_withInvalidToken_throwsException() {
        AuthController.RefreshRequest refreshRequest = new AuthController.RefreshRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        when(tokenProvider.validateToken("invalid-refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refreshToken_withUserNotFound_throwsException() {
        AuthController.RefreshRequest refreshRequest = new AuthController.RefreshRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid-refresh-token")).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }
}