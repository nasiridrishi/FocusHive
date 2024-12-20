package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.AuthenticationException;
import com.focushive.identity.exception.ResourceNotFoundException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.security.encryption.IEncryptionService;
import com.focushive.identity.service.ITokenBlacklistService;
import com.focushive.identity.service.AccountLockoutService;
import com.focushive.identity.exception.AccountLockedException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling all authentication operations including registration,
 * login, token management, and persona switching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final IEncryptionService encryptionService;
    private final AccountLockoutService accountLockoutService;
    
    /**
     * Register a new user with a default persona.
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Processing user registration request");

        // Validate email format
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }

        // Check if user already exists
        // Try hash-based lookup first (for production with encryption)
        // Fallback to direct email lookup (for tests without encryption) 
        if (encryptionService != null) {
            try {
                String emailHash = encryptionService.hash(request.getEmail().toLowerCase());
                if (userRepository.existsByEmailHash(emailHash)) {
                    throw new AuthenticationException("Email already registered");
                }
            } catch (Exception e) {
                // Fallback to direct email lookup if hashing fails
                log.debug("Hash-based email lookup failed, using direct email lookup: {}", e.getMessage());
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new AuthenticationException("Email already registered");
                }
            }
        } else {
            // EncryptionService not available (test mode), use direct email lookup
            log.debug("EncryptionService not available, using direct email lookup");
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AuthenticationException("Email already registered");
            }
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Username already taken");
        }
        
        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailVerified(false)
                .enabled(true)
                .build();
        
        user = userRepository.save(user);
        
        // Create default persona
        Persona defaultPersona = Persona.builder()
                .user(user)
                .name(request.getPersonaName() != null ? request.getPersonaName() : "Default")
                .type(Persona.PersonaType.valueOf(request.getPersonaType()))
                .isDefault(true)
                .isActive(true)
                .displayName(user.getUsername()) // Username is the public display name
                .privacySettings(createDefaultPrivacySettings())
                .build();
        
        defaultPersona = personaRepository.save(defaultPersona);
        
        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user, defaultPersona);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user);
        
        return buildAuthenticationResponse(user, defaultPersona, accessToken, refreshToken);
    }
    
    /**
     * Authenticate user and return tokens.
     */
    public AuthenticationResponse login(LoginRequest request) {
        log.info("Processing login for: {}", request.getUsernameOrEmail());

        String ipAddress = getClientIpAddress();

        // Find user first to check lockout status
        User user = null;
        String usernameOrEmail = request.getUsernameOrEmail();

        // Check if the input looks like an email
        if (isValidEmail(usernameOrEmail)) {
            // It's an email - need to use hash-based lookup for encrypted emails
            if (encryptionService != null) {
                try {
                    String emailHash = encryptionService.hash(usernameOrEmail.toLowerCase());
                    user = userRepository.findByEmailHash(emailHash).orElse(null);
                } catch (Exception e) {
                    log.debug("Hash-based email lookup failed: {}", e.getMessage());
                    // Fallback to direct email lookup for test environments
                    user = userRepository.findByEmail(usernameOrEmail).orElse(null);
                }
            } else {
                // Encryption service not available (test mode), use direct lookup
                user = userRepository.findByEmail(usernameOrEmail).orElse(null);
            }
        } else {
            // It's a username
            user = userRepository.findByUsername(usernameOrEmail).orElse(null);
        }

        if (user != null) {
            // Check account lockout status before authentication attempt
            try {
                accountLockoutService.checkAccountLockout(user);
            } catch (AccountLockedException e) {
                // Account is locked, don't proceed with authentication
                throw e;
            }
        }

        // Authenticate
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Invalid login attempt for: {}", request.getUsernameOrEmail());

            // Record failed attempt if user exists
            if (user != null) {
                accountLockoutService.recordFailedLoginAttempt(user, ipAddress);
            }

            throw new AuthenticationException("Invalid username or password");
        }

        User authenticatedUser = (User) authentication.getPrincipal();

        // Cross-check: Load user from database by username to ensure consistency
        // This fixes issues with cached/stale user objects from authentication context
        Optional<User> dbUser = userRepository.findByUsername(authenticatedUser.getUsername());
        if (dbUser.isPresent()) {
            if (!authenticatedUser.getId().equals(dbUser.get().getId())) {
                log.warn("User ID mismatch detected. Using database user for consistency. Auth context ID: {}, DB ID: {}",
                        authenticatedUser.getId(), dbUser.get().getId());
                authenticatedUser = dbUser.get();
            }
        } else {
            log.error("User {} not found in database during login!", authenticatedUser.getUsername());
            throw new IllegalStateException("Authenticated user not found in database");
        }

        // Record successful login and reset failed attempts
        accountLockoutService.recordSuccessfulLogin(authenticatedUser, ipAddress);
        
        // Determine which persona to activate
        final User finalUser = authenticatedUser; // For lambda usage
        Persona activePersona;
        if (request.getPersonaId() != null) {
            activePersona = personaRepository.findByIdAndUser(
                    UUID.fromString(request.getPersonaId()), authenticatedUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Persona not found"));
        } else {
            activePersona = personaRepository.findByUserAndIsActiveTrue(authenticatedUser)
                    .orElseGet(() -> personaRepository.findByUserAndIsDefaultTrue(finalUser)
                            .orElseThrow(() -> {
                                log.error("No personas found for user {} (ID: {}). Total personas in system: {}",
                                        finalUser.getUsername(), finalUser.getId(), personaRepository.count());
                                return new IllegalStateException("No persona found for user");
                            }));
        }

        // Update last active persona
        personaRepository.updateActivePersona(authenticatedUser.getId(), activePersona.getId());

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(authenticatedUser, activePersona);
        String refreshToken = request.isRememberMe()
                ? tokenProvider.generateLongLivedRefreshToken(authenticatedUser)
                : tokenProvider.generateRefreshToken(authenticatedUser);

        return buildAuthenticationResponse(authenticatedUser, activePersona, accessToken, refreshToken);
    }
    
    /**
     * Refresh access token using refresh token.
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Check if token is blacklisted
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new AuthenticationException("Token has been revoked");
        }
        
        String username = tokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Get active persona
        Persona activePersona = personaRepository.findByUserAndIsActiveTrue(user)
                .orElseGet(() -> personaRepository.findByUserAndIsDefaultTrue(user)
                        .orElseThrow(() -> new IllegalStateException("No persona found for user")));
        
        // Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(user, activePersona);
        
        return AuthenticationResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep the same refresh token
                .expiresIn(3600L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getUsername()) // Username is the public display
                .activePersona(buildPersonaInfo(activePersona))
                .build();
    }
    
    /**
     * Validate token and return user information.
     */
    @Cacheable(value = "tokenValidation", key = "#request.token")
    public ValidateTokenResponse validateToken(ValidateTokenRequest request) {
        String token = request.getToken();
        if (!tokenProvider.validateToken(token)) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Invalid or expired token")
                    .build();
        }
        
        if (tokenBlacklistService.isBlacklisted(token)) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Token has been revoked")
                    .build();
        }
        
        try {
            String username = tokenProvider.extractUsername(token);
            UUID userId = tokenProvider.extractUserId(token);
            UUID personaId = tokenProvider.extractPersonaId(token);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            Persona persona = personaRepository.findById(personaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Persona not found"));
            
            return ValidateTokenResponse.builder()
                    .valid(true)
                    .userId(userId.toString())
                    .personaId(personaId.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Token validation failed")
                    .build();
        }
    }
    
    /**
     * OAuth2 token introspection.
     */
    public IntrospectTokenResponse introspectToken(IntrospectTokenRequest request) {
        String token = request.getToken();
        
        if (!tokenProvider.validateToken(token) || tokenBlacklistService.isBlacklisted(token)) {
            return IntrospectTokenResponse.builder()
                    .active(false)
                    .build();
        }
        
        try {
            String userId = tokenProvider.extractUserId(token).toString();
            String personaId = tokenProvider.extractPersonaId(token).toString();
            Claims claims = tokenProvider.extractAllClaims(token);
            
            return IntrospectTokenResponse.builder()
                    .active(true)
                    .scope("openid profile email personas")
                    .username(tokenProvider.extractUsername(token))
                    .sub(userId)
                    .exp(tokenProvider.extractExpiration(token).getTime() / 1000)
                    .iat(tokenProvider.extractIssuedAt(token).getTime() / 1000)
                    .tokenType("Bearer")
                    .email(tokenProvider.extractEmail(token))
                    .personaId(personaId)
                    .personaType(claims.get("personaType", String.class))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error introspecting token", e);
            return IntrospectTokenResponse.builder()
                    .active(false)
                    .build();
        }
    }
    
    /**
     * Logout user by blacklisting their tokens.
     */
    public void logout(LogoutRequest request, User user) {
        // Blacklist access token if provided
        if (request.getAccessToken() != null && tokenProvider.validateToken(request.getAccessToken())) {
            LocalDateTime expiration = tokenProvider.getExpirationFromToken(request.getAccessToken());
            tokenBlacklistService.blacklistToken(request.getAccessToken(), 
                    expiration.toInstant(java.time.ZoneOffset.UTC));
        }
        
        // Blacklist refresh token if provided
        if (request.getRefreshToken() != null && tokenProvider.validateToken(request.getRefreshToken())) {
            LocalDateTime expiration = tokenProvider.getExpirationFromToken(request.getRefreshToken());
            tokenBlacklistService.blacklistToken(request.getRefreshToken(), 
                    expiration.toInstant(java.time.ZoneOffset.UTC));
        }
        
        log.info("User {} logged out, tokens blacklisted", user.getUsername());
    }
    
    /**
     * Request password reset.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Processing password reset request for email: {}", email);
        
        // Use encryption-aware lookup for email, similar to registration
        Optional<User> userOptional = Optional.empty();
        
        // Try hash-based lookup first (for production with encryption)
        if (encryptionService != null) {
            try {
                String emailHash = encryptionService.hash(email.toLowerCase());
                log.debug("Generated email hash for password reset lookup: {}", emailHash);
                userOptional = userRepository.findByEmailHash(emailHash);
                log.debug("Hash-based lookup result: user found = {}", userOptional.isPresent());
            } catch (Exception e) {
                // Fallback to direct email lookup if hashing fails
                log.debug("Hash-based email lookup failed for password reset, using direct email lookup: {}", e.getMessage());
                userOptional = userRepository.findByEmail(email);
                log.debug("Direct email lookup result: user found = {}", userOptional.isPresent());
            }
        } else {
            // EncryptionService not available (test mode), use direct email lookup
            log.debug("EncryptionService not available, using direct email lookup for password reset");
            userOptional = userRepository.findByEmail(email);
            log.debug("Direct email lookup result: user found = {}", userOptional.isPresent());
        }
        
        userOptional.ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepository.save(user);
            
            emailService.sendPasswordResetEmail(user, resetToken);
            log.info("Password reset requested for user: {}", user.getUsername());
        });
        // Don't reveal if user exists or not
    }
    
    /**
     * Reset password with token.
     */
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Passwords do not match");
        }
        
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AuthenticationException("Invalid reset token"));
        
        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new AuthenticationException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        
        log.info("Password reset successful for user: {}", user.getUsername());
    }
    
    /**
     * Switch active persona for user.
     */
    @Transactional
    public AuthenticationResponse switchPersona(SwitchPersonaRequest request, User user) {
        // Find requested persona
        Persona newPersona = personaRepository.findById(request.getPersonaId())
                .orElseThrow(() -> new RuntimeException("Persona not found"));
        
        if (!newPersona.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to persona");
        }
        
        // Deactivate all personas for the user
        user.getPersonas().forEach(p -> {
            p.setActive(false);
            personaRepository.save(p);
        });
        
        // Activate the new persona
        newPersona.setActive(true);
        personaRepository.save(newPersona);
        
        // Generate new tokens with new persona
        String accessToken = tokenProvider.generateAccessToken(user, newPersona);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        
        log.info("User {} switched to persona: {}", user.getUsername(), newPersona.getName());
        
        return buildAuthenticationResponse(user, newPersona, accessToken, refreshToken);
    }
    
    /**
     * Build authentication response with user and persona information.
     */
    private AuthenticationResponse buildAuthenticationResponse(
            User user, Persona activePersona, String accessToken, String refreshToken) {
        
        List<Persona> personas = personaRepository.findByUser(user);
        
        return AuthenticationResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getUsername()) // Username is the public display
                .activePersona(buildPersonaInfo(activePersona))
                .availablePersonas(personas.stream()
                        .map(this::buildPersonaInfo)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * Build persona info for response.
     */
    private AuthenticationResponse.PersonaInfo buildPersonaInfo(Persona persona) {
        return AuthenticationResponse.PersonaInfo.builder()
                .id(persona.getId())
                .name(persona.getName())
                .type(persona.getType().name())
                .isDefault(persona.isDefault())
                .avatarUrl(persona.getAvatarUrl())
                .privacySettings(AuthenticationResponse.PrivacySettings.builder()
                        .showRealName(persona.getPrivacySettings().isShowRealName())
                        .showEmail(persona.getPrivacySettings().isShowEmail())
                        .showActivity(persona.getPrivacySettings().isShowActivity())
                        .allowDirectMessages(persona.getPrivacySettings().isAllowDirectMessages())
                        .visibilityLevel(persona.getPrivacySettings().getVisibilityLevel())
                        .build())
                .build();
    }
    
    /**
     * Create default privacy settings for new personas.
     */
    private Persona.PrivacySettings createDefaultPrivacySettings() {
        return Persona.PrivacySettings.builder()
                .showRealName(false)
                .showEmail(false)
                .showActivity(true)
                .allowDirectMessages(true)
                .visibilityLevel("FRIENDS")
                .searchable(true)
                .showOnlineStatus(true)
                .shareFocusSessions(true)
                .shareAchievements(true)
                .build();
    }
    
    /**
     * Map User entity to UserDTO.
     */
    private UserDTO mapUserToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getUsername()) // Username is the public display
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .notificationPreferences(user.getNotificationPreferences())
                .createdAt(LocalDateTime.now())
                .lastLoginAt(user.getLastLoginAt() != null ? 
                        LocalDateTime.ofInstant(user.getLastLoginAt(), java.time.ZoneOffset.UTC) : null)
                .personas(user.getPersonas().stream()
                        .map(this::mapPersonaToDTO)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * Map Persona entity to PersonaDto.
     */
    private PersonaDto mapPersonaToDTO(Persona persona) {
        return PersonaDto.builder()
                .id(persona.getId())
                .name(persona.getName())
                .type(persona.getType())
                .displayName(persona.getDisplayName())
                .avatarUrl(persona.getAvatarUrl())
                .bio(persona.getBio())
                .statusMessage(persona.getStatusMessage())
                .isDefault(persona.isDefault())
                .isActive(persona.isActive())
                .build();
    }
    
    /**
     * Validate email format using a simple regex pattern.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Simple email validation regex
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Extract client IP address from the current HTTP request.
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                var request = attributes.getRequest();

                // Check various headers for real IP (for proxies/load balancers)
                String[] headers = {
                        "X-Forwarded-For",
                        "X-Real-IP",
                        "Proxy-Client-IP",
                        "WL-Proxy-Client-IP",
                        "HTTP_X_FORWARDED_FOR",
                        "HTTP_X_FORWARDED",
                        "HTTP_X_CLUSTER_CLIENT_IP",
                        "HTTP_CLIENT_IP",
                        "HTTP_FORWARDED_FOR",
                        "HTTP_FORWARDED",
                        "HTTP_VIA",
                        "REMOTE_ADDR"
                };

                for (String header : headers) {
                    String ip = request.getHeader(header);
                    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                        // X-Forwarded-For can contain multiple IPs, take the first one
                        if (ip.contains(",")) {
                            ip = ip.split(",")[0].trim();
                        }
                        return ip;
                    }
                }

                // Fallback to remote address
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not extract IP address from request", e);
        }

        return "unknown";
    }
}