package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.AuthenticationException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService.
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PersonaRepository personaRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    private User testUser;
    private Persona testPersona;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setDisplayName("Test User");
        testUser.setEmailVerified(true);
        testUser.setEnabled(true);
        
        testPersona = new Persona();
        testPersona.setId(UUID.randomUUID());
        testPersona.setUser(testUser);
        testPersona.setName("personal");
        testPersona.setType(Persona.PersonaType.PERSONAL);
        testPersona.setDefault(true);
        testPersona.setActive(true);
        
        testUser.getPersonas().add(testPersona);
    }
    
    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        // No confirmPassword field
        request.setDisplayName("New User");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            return persona;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class), any(Persona.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        
        // Act
        AuthenticationResponse response = authenticationService.register(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        
        verify(userRepository).save(any(User.class));
        verify(personaRepository).save(any(Persona.class));
        verify(emailService).sendVerificationEmail(any(User.class));
    }
    
    @Test
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        // No confirmPassword field
        
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Username already taken");
    }
    
    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        // No confirmPassword field
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email already registered");
    }
    
    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.findByUser(testUser)).thenReturn(Collections.singletonList(testPersona));
        when(jwtTokenProvider.generateAccessToken(any(User.class), any(Persona.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        
        // Act
        AuthenticationResponse response = authenticationService.login(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getActivePersona()).isNotNull();
        assertThat(response.getActivePersona().getType()).isEqualTo(Persona.PersonaType.PERSONAL.name());
    }
    
    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
    
    @Test
    void testRefreshToken_Success() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");
        
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid-refresh-token")).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(Optional.of(testPersona));
        when(jwtTokenProvider.generateAccessToken(any(User.class), any(Persona.class))).thenReturn("new-access-token");
        
        // Act
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token"); // Same refresh token is returned
    }
    
    @Test
    void testRefreshToken_InvalidToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");
        
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid refresh token");
    }
    
    @Test
    void testValidateToken_Valid() {
        // Arrange
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("valid-token");
        
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtTokenProvider.extractUsername("valid-token")).thenReturn(testUser.getUsername());
        when(jwtTokenProvider.extractUserId("valid-token")).thenReturn(testUser.getId());
        when(jwtTokenProvider.extractPersonaId("valid-token")).thenReturn(testPersona.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(personaRepository.findById(testPersona.getId())).thenReturn(Optional.of(testPersona));
        
        // Act
        ValidateTokenResponse response = authenticationService.validateToken(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(testUser.getId().toString());
        assertThat(response.getPersonaId()).isEqualTo(testPersona.getId().toString());
    }
    
    @Test
    void testValidateToken_Blacklisted() {
        // Arrange
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("blacklisted-token");
        
        when(jwtTokenProvider.validateToken("blacklisted-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);
        
        // Act
        ValidateTokenResponse response = authenticationService.validateToken(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getError()).isEqualTo("Token has been revoked");
    }
    
    @Test
    void testLogout_Success() {
        // Arrange
        LogoutRequest request = new LogoutRequest();
        request.setAccessToken("access-token");
        request.setRefreshToken("refresh-token");
        
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getExpirationFromToken("access-token")).thenReturn(LocalDateTime.now().plusHours(1));
        when(jwtTokenProvider.getExpirationFromToken("refresh-token")).thenReturn(LocalDateTime.now().plusDays(7));
        
        // Act
        authenticationService.logout(request, testUser);
        
        // Assert
        verify(tokenBlacklistService).blacklistToken(eq("access-token"), any(Instant.class));
        verify(tokenBlacklistService).blacklistToken(eq("refresh-token"), any(Instant.class));
    }
    
    @Test
    void testRequestPasswordReset_UserFound() {
        // Arrange
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("test@example.com");
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        authenticationService.requestPasswordReset(request.getEmail());
        
        // Assert
        verify(userRepository).save(any(User.class));
        verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
    }
    
    @Test
    void testRequestPasswordReset_UserNotFound() {
        // Arrange
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("nonexistent@example.com");
        
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // Act - Should not throw exception for security reasons
        authenticationService.requestPasswordReset(request.getEmail());
        
        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(any(User.class), anyString());
    }
    
    @Test
    void testSwitchPersona_Success() {
        // Arrange
        Persona newPersona = new Persona();
        newPersona.setId(UUID.randomUUID());
        newPersona.setUser(testUser);
        newPersona.setName("work");
        newPersona.setType(Persona.PersonaType.WORK);
        testUser.getPersonas().add(newPersona);
        
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(newPersona.getId());
        
        when(personaRepository.findById(newPersona.getId())).thenReturn(Optional.of(newPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personaRepository.findByUser(testUser)).thenReturn(testUser.getPersonas());
        when(jwtTokenProvider.generateAccessToken(any(User.class), any(Persona.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("new-refresh-token");
        
        // Act
        AuthenticationResponse response = authenticationService.switchPersona(request, testUser);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getActivePersona().getId()).isEqualTo(newPersona.getId());
        assertThat(response.getActivePersona().getType()).isEqualTo("WORK");
        
        // Verify persona states were updated (2 personas to deactivate + 1 to activate = 3 total)
        verify(personaRepository, times(3)).save(any(Persona.class));
    }
    
    @Test
    void testSwitchPersona_PersonaNotFound() {
        // Arrange
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(UUID.randomUUID());
        
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.switchPersona(request, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Persona not found");
    }
    
    @Test
    void testSwitchPersona_UnauthorizedAccess() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        
        Persona otherPersona = new Persona();
        otherPersona.setId(UUID.randomUUID());
        otherPersona.setUser(otherUser);
        
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(otherPersona.getId());
        
        when(personaRepository.findById(otherPersona.getId())).thenReturn(Optional.of(otherPersona));
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.switchPersona(request, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized access to persona");
    }
}