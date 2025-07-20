package com.focushive.user.service;

import com.focushive.api.security.JwtTokenProvider;
import com.focushive.events.UserRegisteredEvent;
import com.focushive.user.controller.AuthController;
import com.focushive.user.dto.RegisterRequest;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        user.setEmailVerified(false); // Email verification can be implemented later

        // Save user
        user = userRepository.save(user);
        
        // Publish registration event
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getUsername()));

        // Generate tokens
        String accessToken = tokenProvider.generateToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        log.info("User registered successfully with ID: {}", user.getId());

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public AuthenticationResponse login(AuthController.LoginRequest request) {
        log.info("User login attempt for: {}", request.getUsername());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();

        // Update last login time
        userRepository.updateLastLogin(user.getId(), java.time.LocalDateTime.now());

        // Generate tokens
        String accessToken = tokenProvider.generateToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        log.info("User logged in successfully: {}", user.getId());

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public AuthenticationResponse refreshToken(AuthController.RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Extract username from token
        String username = tokenProvider.extractUsername(refreshToken);
        
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new tokens
        String newAccessToken = tokenProvider.generateToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getId());

        return new AuthenticationResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public record AuthenticationResponse(
            String accessToken,
            String refreshToken,
            String userId,
            String username,
            String email,
            String role
    ) {}
}