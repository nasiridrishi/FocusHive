package com.focushive.api.service;

import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("hashedPassword");
        testUser.setDisplayName("Test User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEmailVerified(true);
        testUser.setAccountNonLocked(true);
        testUser.setEnabled(true);
        testUser.setDeletedAt(null);
    }

    @Test
    void loadUserByUsername_withEmail_returnsUserDetails() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_withUsername_returnsUserDetails() {
        when(userRepository.findByEmail("testuser"))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: nonexistent@example.com");
    }

    @Test
    void loadUserByUsername_deletedUser_throwsException() {
        testUser.setDeletedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_lockedUser_returnsLockedAccount() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledAccount() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.isEnabled()).isFalse();
    }
}