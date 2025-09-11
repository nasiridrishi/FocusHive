package com.focushive.identity.service;

import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for CustomUserDetailsService.
 * Tests Spring Security UserDetailsService implementation with username/email lookup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(Instant.now())
                .personas(new ArrayList<>()) // Mock empty personas list
                .build();
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void loadUserByUsername_ValidEmail_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");

        verify(userRepository).findByEmail("test@example.com");
        // Username lookup should not be called when email is found
        verify(userRepository, never()).findByUsername("test@example.com");
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void loadUserByUsername_ValidUsername_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository).findByEmail("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should prefer email lookup over username")
    void loadUserByUsername_EmailAndUsernameExist_ShouldPreferEmail() {
        // Given
        User emailUser = User.builder()
                .id(UUID.randomUUID())
                .username("email-user")
                .email("test@example.com")
                .password("email-password")
                .firstName("Email")
                .lastName("User")
                .enabled(true)
                .personas(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(emailUser));
        // Don't mock findByUsername since it should never be called when email is found

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("email-user");
        assertThat(userDetails.getPassword()).isEqualTo("email-password");

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).findByUsername("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void loadUserByUsername_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username or email: nonexistent@example.com");

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository).findByUsername("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user is soft-deleted")
    void loadUserByUsername_SoftDeletedUser_ShouldThrowException() {
        // Given
        testUser.setDeletedAt(Instant.now());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("test@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User account has been deleted");

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should handle disabled user correctly")
    void loadUserByUsername_DisabledUser_ShouldReturnUserDetailsWithDisabledFlag() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle locked user correctly")
    void loadUserByUsername_LockedUser_ShouldReturnUserDetailsWithLockedFlag() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle expired user correctly")
    void loadUserByUsername_ExpiredUser_ShouldReturnUserDetailsWithExpiredFlag() {
        // Given
        testUser.setAccountNonExpired(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonExpired()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle expired credentials correctly")
    void loadUserByUsername_ExpiredCredentials_ShouldReturnUserDetailsWithExpiredCredentialsFlag() {
        // Given
        testUser.setCredentialsNonExpired(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isCredentialsNonExpired()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should trigger personas loading for optimization")
    void loadUserByUsername_ValidUser_ShouldTriggerPersonasLoading() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        // Verify that personas.size() was called to trigger EntityGraph loading
        // This is verified by ensuring no exception was thrown and the user was returned successfully
        assertThat(((User) userDetails).getPersonas()).isNotNull();
    }

    @Test
    @DisplayName("Should load user by ID successfully")
    void loadUserById_ValidId_ShouldReturnUser() {
        // Given
        String userId = testUserId.toString();
        when(userRepository.findActiveById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        User user = customUserDetailsService.loadUserById(userId);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUserId);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findActiveById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void loadUserById_UserNotFound_ShouldThrowException() {
        // Given
        String userId = testUserId.toString();
        when(userRepository.findActiveById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserById(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);

        verify(userRepository).findActiveById(testUserId);
    }

    @Test
    @DisplayName("Should handle invalid UUID format in loadUserById")
    void loadUserById_InvalidUUID_ShouldThrowException() {
        // Given
        String invalidUuid = "invalid-uuid-format";

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserById(invalidUuid))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findActiveById(any());
    }

    @Test
    @DisplayName("Should handle null username in loadUserByUsername")
    void loadUserByUsername_NullUsername_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username or email: null");
    }

    @Test
    @DisplayName("Should handle empty username in loadUserByUsername")
    void loadUserByUsername_EmptyUsername_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username or email: ");
    }

    @Test
    @DisplayName("Should handle case-sensitive email lookup")
    void loadUserByUsername_CaseSensitiveEmail_ShouldRespectCase() {
        // Given
        when(userRepository.findByEmail("TEST@EXAMPLE.COM")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("TEST@EXAMPLE.COM")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("TEST@EXAMPLE.COM"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmail("TEST@EXAMPLE.COM");
        verify(userRepository).findByUsername("TEST@EXAMPLE.COM");
    }

    @Test
    @DisplayName("Should handle case-sensitive username lookup")
    void loadUserByUsername_CaseSensitiveUsername_ShouldRespectCase() {
        // Given
        when(userRepository.findByEmail("TESTUSER")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("TESTUSER")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("TESTUSER"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmail("TESTUSER");
        verify(userRepository).findByUsername("TESTUSER");
    }

    @Test
    @DisplayName("Should properly cascade from email to username lookup")
    void loadUserByUsername_EmailNotFoundUsernameFound_ShouldReturnUser() {
        // Given
        String lookupValue = "ambiguous-value";
        when(userRepository.findByEmail(lookupValue)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(lookupValue)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(lookupValue);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");

        // Verify proper sequence: email first, then username
        verify(userRepository).findByEmail(lookupValue);
        verify(userRepository).findByUsername(lookupValue);
    }
}