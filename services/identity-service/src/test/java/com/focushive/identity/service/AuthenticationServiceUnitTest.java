package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.AuthenticationException;
import com.focushive.identity.exception.ResourceNotFoundException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for AuthenticationService without Spring context dependencies.
 * Tests authentication, registration, token management, and persona switching.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PersonaRepository personaRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    private UUID userId;
    private UUID personaId;
    private User testUser;
    private Persona testPersona;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        personaId = UUID.randomUUID();
        
        // Create test user
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("encoded_password")
                .emailVerified(false)
                .enabled(true)
                .build();
        
        // Create test persona
        testPersona = Persona.builder()
                .id(personaId)
                .user(testUser)
                .name("Default")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Test User")
                .isDefault(true)
                .isActive(true)
                .privacySettings(new Persona.PrivacySettings())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Create register request
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setPersonaName("Default");
        registerRequest.setPersonaType("PERSONAL");
        
        // Create login request
        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);
    }
    
    @Test
    @DisplayName("Register user - Success case")
    void register_Success() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh_token");
        
        // When
        AuthenticationResponse response = authenticationService.register(registerRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        
        // Verify interactions
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(personaRepository).save(any(Persona.class));
        verify(emailService).sendVerificationEmail(testUser);
        
        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getPassword()).isEqualTo("encoded_password");
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        
        // Verify persona creation
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getName()).isEqualTo("Default");
        assertThat(savedPersona.getType()).isEqualTo(Persona.PersonaType.PERSONAL);
        assertThat(savedPersona.isDefault()).isTrue();
        assertThat(savedPersona.isActive()).isTrue();
    }
    
    @Test
    @DisplayName("Register user - Email already exists")
    void register_EmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Email already registered");
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Register user - Username already exists")
    void register_UsernameAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Username already taken");
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Login - Success with default persona")
    void login_SuccessWithDefaultPersona() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.empty());
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh_token");
        
        // When
        AuthenticationResponse response = authenticationService.login(loginRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(personaRepository).findByUserAndIsActiveTrue(testUser);
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
        verify(personaRepository).updateActivePersona(userId, personaId);
        verify(tokenProvider).generateRefreshToken(testUser); // Not long-lived since rememberMe = false
    }
    
    @Test
    @DisplayName("Login - Success with specific persona")
    void login_SuccessWithSpecificPersona() {
        // Given
        loginRequest.setPersonaId(personaId.toString());
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh_token");
        
        // When
        AuthenticationResponse response = authenticationService.login(loginRequest);
        
        // Then
        assertThat(response).isNotNull();
        verify(personaRepository).findByIdAndUser(personaId, testUser);
        verify(personaRepository, never()).findByUserAndIsActiveTrue(any());
        verify(personaRepository, never()).findByUserAndIsDefaultTrue(any());
    }
    
    @Test
    @DisplayName("Login - Success with remember me")
    void login_SuccessWithRememberMe() {
        // Given
        loginRequest.setRememberMe(true);
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("access_token");
        when(tokenProvider.generateLongLivedRefreshToken(testUser)).thenReturn("long_refresh_token");
        
        // When
        AuthenticationResponse response = authenticationService.login(loginRequest);
        
        // Then
        assertThat(response.getRefreshToken()).isEqualTo("long_refresh_token");
        verify(tokenProvider).generateLongLivedRefreshToken(testUser);
        verify(tokenProvider, never()).generateRefreshToken(testUser);
    }
    
    @Test
    @DisplayName("Login - Persona not found")
    void login_PersonaNotFound() {
        // Given
        loginRequest.setPersonaId(personaId.toString());
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");
        
        verify(personaRepository).findByIdAndUser(personaId, testUser);
    }
    
    @Test
    @DisplayName("Login - No persona found for user")
    void login_NoPersonaFoundForUser() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.empty());
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No persona found for user");
        
        verify(personaRepository).findByUserAndIsActiveTrue(testUser);
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
    }
    
    @Test
    @DisplayName("Refresh token - Success")
    void refreshToken_Success() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");
        
        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid_refresh_token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("new_access_token");
        
        // When
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("valid_refresh_token"); // Same refresh token
        
        verify(tokenProvider).validateToken("valid_refresh_token");
        verify(tokenProvider).extractUsername("valid_refresh_token");
        verify(userRepository).findByUsername("testuser");
        verify(personaRepository).findByUserAndIsActiveTrue(testUser);
    }
    
    @Test
    @DisplayName("Refresh token - Invalid token")
    void refreshToken_InvalidToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_refresh_token");
        
        when(tokenProvider.validateToken("invalid_refresh_token")).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid refresh token");
        
        verify(tokenProvider).validateToken("invalid_refresh_token");
        verify(tokenProvider, never()).extractUsername(any());
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    @DisplayName("Refresh token - User not found")
    void refreshToken_UserNotFound() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");
        
        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid_refresh_token")).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
        
        verify(tokenProvider).validateToken("valid_refresh_token");
        verify(tokenProvider).extractUsername("valid_refresh_token");
        verify(userRepository).findByUsername("nonexistent");
    }
    
    @Test
    @DisplayName("Refresh token - No persona found")
    void refreshToken_NoPersonaFound() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");
        
        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid_refresh_token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.empty());
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No persona found for user");
        
        verify(personaRepository).findByUserAndIsActiveTrue(testUser);
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
    }
    
    @Test
    @DisplayName("Register user - Custom persona name and type")
    void register_CustomPersonaNameAndType() {
        // Given
        registerRequest.setPersonaName("Work Profile");
        registerRequest.setPersonaType("WORK");
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh_token");
        
        // When
        authenticationService.register(registerRequest);
        
        // Then
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getName()).isEqualTo("Work Profile");
        assertThat(savedPersona.getType()).isEqualTo(Persona.PersonaType.WORK);
    }
    
    @Test
    @DisplayName("Register user - Null persona name defaults to 'Default'")
    void register_NullPersonaNameDefaultsToDefault() {
        // Given
        registerRequest.setPersonaName(null);
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh_token");
        
        // When
        authenticationService.register(registerRequest);
        
        // Then
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getName()).isEqualTo("Default");
    }
    
    @Test
    @DisplayName("Refresh token - Falls back to default persona")
    void refreshToken_FallsBackToDefaultPersona() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");
        
        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.extractUsername("valid_refresh_token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.empty());
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(tokenProvider.generateAccessToken(testUser, testPersona)).thenReturn("new_access_token");
        
        // When
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        
        verify(personaRepository).findByUserAndIsActiveTrue(testUser);
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
    }
}