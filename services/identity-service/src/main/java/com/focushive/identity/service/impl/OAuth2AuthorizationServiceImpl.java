package com.focushive.identity.service.impl;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.*;
import com.focushive.identity.repository.*;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.OAuth2AuthorizationService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Implementation of OAuth2 Authorization Server Service.
 * Provides comprehensive OAuth2 and OpenID Connect functionality.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OAuth2AuthorizationServiceImpl implements OAuth2AuthorizationService {

    private final OAuthClientRepository clientRepository;
    private final OAuthAccessTokenRepository accessTokenRepository;
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final OAuthAuthorizationCodeRepository authorizationCodeRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    // OAuth2 Configuration Constants
    private static final int AUTHORIZATION_CODE_LENGTH = 128;
    private static final int AUTHORIZATION_CODE_EXPIRY_MINUTES = 10;
    private static final int ACCESS_TOKEN_EXPIRY_SECONDS = 3600; // 1 hour
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void authorize(OAuth2AuthorizeRequest request, Authentication authentication,
                         HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        
        log.debug("Processing OAuth2 authorization request for client: {}", request.getClientId());
        
        // Validate client
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(request.getClientId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid client_id"));
            
        // Validate redirect URI
        if (!client.getRedirectUris().contains(request.getRedirectUri())) {
            throw new IllegalArgumentException("Invalid redirect_uri");
        }
        
        // Validate response type
        if (!"code".equals(request.getResponseType())) {
            redirectWithError(httpResponse, request.getRedirectUri(), "unsupported_response_type", 
                            "Only 'code' response type is supported", request.getState());
            return;
        }
        
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            // Redirect to login with return URL
            String loginUrl = "/login?return_url=" + httpRequest.getRequestURL() + "?" + httpRequest.getQueryString();
            httpResponse.sendRedirect(loginUrl);
            return;
        }
        
        // Generate authorization code
        String authCode = generateAuthorizationCode(
            request.getClientId(), 
            request.getRedirectUri(), 
            request.getScope(), 
            authentication
        );
        
        // Redirect with authorization code
        String redirectUrl = buildRedirectUrl(request.getRedirectUri(), authCode, request.getState());
        httpResponse.sendRedirect(redirectUrl);
    }

    @Override
    public OAuth2TokenResponse token(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
        log.debug("Processing OAuth2 token request for grant type: {}", request.getGrantType());
        
        switch (request.getGrantType()) {
            case "authorization_code":
                return handleAuthorizationCodeGrant(request);
            case "refresh_token":
                return handleRefreshTokenGrant(request);
            case "client_credentials":
                return handleClientCredentialsGrant(request);
            default:
                throw new IllegalArgumentException("Unsupported grant type: " + request.getGrantType());
        }
    }
    
    private OAuth2TokenResponse handleAuthorizationCodeGrant(OAuth2TokenRequest request) {
        // Extract client credentials from Authorization header or request parameters
        String[] credentials = extractClientCredentials(request);
        String clientId = credentials[0];
        String clientSecret = credentials[1];
        
        // Validate client credentials
        if (!validateClientCredentials(clientId, clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        // Find and validate authorization code
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
            
        Optional<com.focushive.identity.entity.OAuthAuthorizationCode> authCodeOpt = 
            authorizationCodeRepository.findValidCodeForClient(
                request.getCode(), client.getId(), request.getRedirectUri(), Instant.now()
            );
            
        if (authCodeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired authorization code");
        }
        
        com.focushive.identity.entity.OAuthAuthorizationCode authCode = authCodeOpt.get();
        
        // Mark code as used and save immediately
        Instant now = Instant.now();
        authCode.setUsed(true);
        authCode.setUsedAt(now);
        authorizationCodeRepository.save(authCode);
        
        // Flush to ensure immediate persistence
        entityManager.flush();
        
        // Generate tokens
        return generateAccessToken(
            authCode.getUserId().toString(),
            authCode.getClientId().toString(),
            String.join(" ", authCode.getScopes())
        );
    }
    
    private OAuth2TokenResponse handleRefreshTokenGrant(OAuth2TokenRequest request) {
        // Extract client credentials from Authorization header or request parameters
        String[] credentials = extractClientCredentials(request);
        String clientId = credentials[0];
        String clientSecret = credentials[1];
        
        // Validate client credentials
        if (!validateClientCredentials(clientId, clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        String tokenHash = hashToken(request.getRefreshToken());
        OAuthRefreshToken refreshToken = refreshTokenRepository
            .findValidTokenByHash(tokenHash, Instant.now())
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
            
        // Verify token belongs to this client
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
            
        if (!refreshToken.getClientId().equals(client.getId())) {
            throw new IllegalArgumentException("Refresh token does not belong to this client");
        }
        
        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setRevocationReason("Token rotation");
        refreshTokenRepository.save(refreshToken);
        
        // Generate new tokens
        return generateAccessToken(
            refreshToken.getUserId().toString(),
            refreshToken.getClientId().toString(),
            String.join(" ", refreshToken.getScopes())
        );
    }
    
    private OAuth2TokenResponse handleClientCredentialsGrant(OAuth2TokenRequest request) {
        // Extract client credentials from Authorization header or request parameters
        String[] credentials = extractClientCredentials(request);
        String clientId = credentials[0];
        String clientSecret = credentials[1];
        
        // Validate client credentials
        if (!validateClientCredentials(clientId, clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
            
        // For client credentials, there's no user - client acts on its own behalf
        String scope = request.getScope() != null ? request.getScope() : "client";
        
        return generateClientCredentialsToken(client.getId().toString(), scope);
    }

    @Override
    public OAuth2IntrospectionResponse introspect(OAuth2IntrospectionRequest request) {
        log.debug("Processing token introspection request");
        
        // Extract client credentials from Authorization header
        String[] credentials = extractClientCredentialsFromHeader(request.getAuthorizationHeader());
        
        // Validate client credentials
        if (!validateClientCredentials(credentials[0], credentials[1])) {
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }
        
        OAuth2TokenInfo tokenInfo = validateAccessToken(request.getToken());
        if (tokenInfo == null) {
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }
        
        return OAuth2IntrospectionResponse.builder()
            .active(tokenInfo.isActive())
            .clientId(tokenInfo.getClientId())
            .username(tokenInfo.getUsername())
            .scope(tokenInfo.getScope())
            .exp(tokenInfo.getExp() != null ? tokenInfo.getExp().getEpochSecond() : null)
            .iat(tokenInfo.getIat() != null ? tokenInfo.getIat().getEpochSecond() : null)
            .sub(tokenInfo.getSub())
            .aud(tokenInfo.getAud())
            .iss(tokenInfo.getIss())
            .tokenType("Bearer")
            .build();
    }

    @Override
    public void revoke(OAuth2RevocationRequest request) {
        log.debug("Processing token revocation request");
        
        // Extract client credentials from Authorization header
        String[] credentials = extractClientCredentialsFromHeader(request.getAuthorizationHeader());
        
        // Validate client credentials
        if (!validateClientCredentials(credentials[0], credentials[1])) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        String tokenHash = hashToken(request.getToken());
        
        // Try to revoke as access token
        accessTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            OAuthClient client = clientRepository.findById(token.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
                
            if (!client.getClientId().equals(credentials[0])) {
                throw new IllegalArgumentException("Token does not belong to this client");
            }
            
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            token.setRevocationReason("Explicit revocation");
            accessTokenRepository.save(token);
        });
        
        // Try to revoke as refresh token
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            OAuthClient client = clientRepository.findById(token.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
                
            if (!client.getClientId().equals(credentials[0])) {
                throw new IllegalArgumentException("Token does not belong to this client");
            }
            
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            token.setRevocationReason("Explicit revocation");
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public OAuth2UserInfoResponse getUserInfo(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        
        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        OAuth2TokenInfo tokenInfo = validateAccessToken(accessToken);
        
        if (tokenInfo == null || !tokenInfo.isActive()) {
            throw new IllegalArgumentException("Invalid or expired access token");
        }
        
        User user = userRepository.findById(UUID.fromString(tokenInfo.getUserId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        return OAuth2UserInfoResponse.builder()
            .sub(user.getId().toString())
            .email(user.getEmail())
            .emailVerified(user.isEmailVerified())
            .name(user.getDisplayName())
            .preferredUsername(user.getUsername())
            .givenName(user.getDisplayName()) // Using displayName as givenName
            .familyName("") // No separate family name field
            .picture("") // No profile image field
            .locale(user.getPreferredLanguage())
            .zoneinfo(user.getTimezone())
            .build();
    }

    @Override
    public OAuth2ServerMetadata getServerMetadata(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        
        return OAuth2ServerMetadata.builder()
            .issuer(baseUrl)
            .authorizationEndpoint(baseUrl + "/oauth2/authorize")
            .tokenEndpoint(baseUrl + "/oauth2/token")
            .introspectionEndpoint(baseUrl + "/oauth2/introspect")
            .revocationEndpoint(baseUrl + "/oauth2/revoke")
            .jwksUri(baseUrl + "/oauth2/jwks")
            .userinfoEndpoint(baseUrl + "/userinfo")
            .registrationEndpoint(baseUrl + "/oauth2/register")
            .grantTypesSupported(Arrays.asList(
                "authorization_code", 
                "refresh_token", 
                "client_credentials",
                "urn:ietf:params:oauth:grant-type:device_code"
            ))
            .responseTypesSupported(Arrays.asList("code"))
            .scopesSupported(Arrays.asList("openid", "profile", "email"))
            .tokenEndpointAuthMethodsSupported(Arrays.asList(
                "client_secret_basic", 
                "client_secret_post"
            ))
            .codeChallengeMethodsSupported(Arrays.asList("S256", "plain"))
            .build();
    }

    @Override
    public Map<String, Object> getJwkSet() {
        // Implementation would return JWK Set for token verification
        // This is typically handled by Spring Authorization Server's JWKSource
        return jwtTokenProvider.getJwkSet();
    }

    @Override
    public OAuth2ClientResponse registerClient(OAuth2ClientRegistrationRequest request, Authentication authentication) {
        log.debug("Registering new OAuth2 client: {}", request.getClientName());
        
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        // Generate client credentials
        String clientId = generateClientId();
        String clientSecret = generateClientSecret();
        
        OAuthClient client = OAuthClient.builder()
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientName(request.getClientName())
            .description(request.getDescription())
            .user(user)
            .redirectUris(new HashSet<>(request.getRedirectUris()))
            .authorizedGrantTypes(new HashSet<>(request.getGrantTypes()))
            .authorizedScopes(new HashSet<>(request.getScopes()))
            .accessTokenValiditySeconds(request.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds())
            .autoApprove(request.getAutoApprove())
            .enabled(true)
            .build();
            
        client = clientRepository.save(client);
        
        return OAuth2ClientResponse.builder()
            .clientId(clientId)
            .clientSecret(clientSecret) // Only returned during registration
            .clientName(client.getClientName())
            .description(client.getDescription())
            .redirectUris(client.getRedirectUris())
            .grantTypes(client.getAuthorizedGrantTypes())
            .scopes(client.getAuthorizedScopes())
            .accessTokenValiditySeconds(client.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds())
            .autoApprove(client.isAutoApprove())
            .enabled(client.isEnabled())
            .createdAt(client.getCreatedAt())
            .build();
    }

    @Override
    public OAuth2ClientListResponse getUserClients(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        List<OAuthClient> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<OAuth2ClientResponse> clientResponses = clients.stream()
            .map(this::mapToClientResponse)
            .toList();
            
        return OAuth2ClientListResponse.builder()
            .clients(clientResponses)
            .totalCount(clientResponses.size())
            .build();
    }

    @Override
    public OAuth2ClientResponse updateClient(String clientId, OAuth2ClientUpdateRequest request, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
        // Verify ownership
        if (!client.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Client does not belong to this user");
        }
        
        // Update fields
        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }
        if (request.getDescription() != null) {
            client.setDescription(request.getDescription());
        }
        if (request.getRedirectUris() != null) {
            client.setRedirectUris(new HashSet<>(request.getRedirectUris()));
        }
        if (request.getScopes() != null) {
            client.setAuthorizedScopes(new HashSet<>(request.getScopes()));
        }
        
        client = clientRepository.save(client);
        return mapToClientResponse(client);
    }

    @Override
    public void deleteClient(String clientId, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
        // Verify ownership
        if (!client.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Client does not belong to this user");
        }
        
        // Revoke all tokens for this client
        Instant now = Instant.now();
        accessTokenRepository.revokeAllTokensForClient(client.getId(), now, "Client deleted");
        refreshTokenRepository.revokeAllTokensForClient(client.getId(), now, "Client deleted");
        
        clientRepository.delete(client);
    }

    @Override
    public boolean validateClientCredentials(String clientId, String clientSecret) {
        return clientRepository.findByClientIdAndEnabledTrue(clientId)
            .map(client -> passwordEncoder.matches(clientSecret, client.getClientSecret()))
            .orElse(false);
    }

    @Override
    public String generateAuthorizationCode(String clientId, String redirectUri, String scope, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
        String code = generateSecureRandomString(AUTHORIZATION_CODE_LENGTH);
        
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .code(code)
            .userId(user.getId())
            .clientId(client.getId())
            .redirectUri(redirectUri)
            .scopes(parseScopes(scope))
            .expiresAt(Instant.now().plus(AUTHORIZATION_CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES))
            .used(false)
            .build();
            
        authorizationCodeRepository.save(authCode);
        return code;
    }

    @Override
    public OAuth2AuthorizationCode validateAuthorizationCode(String code, String clientId, String redirectUri) {
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElse(null);
            
        if (client == null) {
            return null;
        }
        
        return authorizationCodeRepository.findValidCodeForClient(
            code, client.getId(), redirectUri, Instant.now()
        ).map(authCode -> com.focushive.identity.dto.OAuth2AuthorizationCode.builder()
            .code(authCode.getCode())
            .userId(authCode.getUserId().toString())
            .clientId(authCode.getClientId().toString())
            .redirectUri(authCode.getRedirectUri())
            .scope(String.join(" ", authCode.getScopes()))
            .expiresAt(authCode.getExpiresAt())
            .build()
        ).orElse(null);
    }

    @Override
    public OAuth2TokenResponse generateAccessToken(String userId, String clientId, String scope) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        OAuthClient client = clientRepository.findById(UUID.fromString(clientId))
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
        // Generate JWT access token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("aud", client.getClientId());
        claims.put("scope", scope);
        claims.put("client_id", client.getClientId());
        
        String accessToken = jwtTokenProvider.generateToken(user.getEmail(), claims, ACCESS_TOKEN_EXPIRY_SECONDS);
        
        // Save access token record
        OAuthAccessToken tokenRecord = OAuthAccessToken.builder()
            .tokenHash(hashToken(accessToken))
            .userId(user.getId())
            .clientId(client.getId())
            .scopes(parseScopes(scope))
            .expiresAt(Instant.now().plus(ACCESS_TOKEN_EXPIRY_SECONDS, ChronoUnit.SECONDS))
            .revoked(false)
            .build();
            
        accessTokenRepository.save(tokenRecord);
        
        // Generate refresh token
        String refreshToken = generateSecureRandomString(128);
        OAuthRefreshToken refreshTokenRecord = OAuthRefreshToken.builder()
            .tokenHash(hashToken(refreshToken))
            .userId(user.getId())
            .clientId(client.getId())
            .accessToken(tokenRecord)
            .scopes(parseScopes(scope))
            .expiresAt(Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS))
            .revoked(false)
            .build();
            
        refreshTokenRepository.save(refreshTokenRecord);
        
        return OAuth2TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .refreshToken(refreshToken)
            .scope(scope)
            .build();
    }
    
    private OAuth2TokenResponse generateClientCredentialsToken(String clientId, String scope) {
        OAuthClient client = clientRepository.findById(UUID.fromString(clientId))
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
        // Generate JWT access token for client credentials
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", client.getClientId());
        claims.put("aud", client.getClientId());
        claims.put("scope", scope);
        claims.put("client_id", client.getClientId());
        claims.put("grant_type", "client_credentials");
        
        String accessToken = jwtTokenProvider.generateToken(client.getClientId(), claims, ACCESS_TOKEN_EXPIRY_SECONDS);
        
        // Save access token record (no user for client credentials)
        OAuthAccessToken tokenRecord = OAuthAccessToken.builder()
            .tokenHash(hashToken(accessToken))
            .userId(null) // No user for client credentials
            .clientId(client.getId())
            .scopes(parseScopes(scope))
            .expiresAt(Instant.now().plus(ACCESS_TOKEN_EXPIRY_SECONDS, ChronoUnit.SECONDS))
            .revoked(false)
            .build();
            
        accessTokenRepository.save(tokenRecord);
        
        return OAuth2TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .scope(scope)
            .build();
    }

    @Override
    public OAuth2TokenInfo validateAccessToken(String accessToken) {
        String tokenHash = hashToken(accessToken);
        
        return accessTokenRepository.findValidTokenByHash(tokenHash, Instant.now())
            .map(token -> {
                User user = token.getUserId() != null 
                    ? userRepository.findById(token.getUserId()).orElse(null)
                    : null;
                    
                OAuthClient client = clientRepository.findById(token.getClientId())
                    .orElse(null);
                    
                if (client == null) {
                    return null;
                }
                
                return OAuth2TokenInfo.builder()
                    .active(true)
                    .clientId(client.getClientId())
                    .userId(token.getUserId() != null ? token.getUserId().toString() : null)
                    .username(user != null ? user.getUsername() : null)
                    .scope(String.join(" ", token.getScopes()))
                    .exp(token.getExpiresAt())
                    .iat(token.getCreatedAt())
                    .sub(user != null ? user.getId().toString() : client.getClientId())
                    .aud(client.getClientId())
                    .iss(getIssuer())
                    .build();
            })
            .orElse(null);
    }
    
    // Helper methods
    
    /**
     * Extract client credentials from OAuth2TokenRequest.
     * Checks Authorization header first, then falls back to request parameters.
     */
    private String[] extractClientCredentials(OAuth2TokenRequest request) {
        if (StringUtils.hasText(request.getAuthorizationHeader())) {
            return extractClientCredentialsFromHeader(request.getAuthorizationHeader());
        } else if (StringUtils.hasText(request.getClientId()) && StringUtils.hasText(request.getClientSecret())) {
            return new String[]{request.getClientId(), request.getClientSecret()};
        } else {
            throw new IllegalArgumentException("Client credentials not provided");
        }
    }
    
    /**
     * Extract client credentials from Authorization header using Basic authentication.
     */
    private String[] extractClientCredentialsFromHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        
        try {
            String encodedCredentials = authorizationHeader.substring(6);
            String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
            String[] credentials = decodedCredentials.split(":", 2);
            
            if (credentials.length != 2) {
                throw new IllegalArgumentException("Invalid client credentials format");
            }
            
            return credentials;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse client credentials", e);
        }
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private String generateSecureRandomString(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
            .substring(0, length);
    }
    
    private String generateClientId() {
        return "client_" + generateSecureRandomString(16);
    }
    
    private String generateClientSecret() {
        return generateSecureRandomString(32);
    }
    
    private Set<String> parseScopes(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(scope.split("\\s+")));
    }
    
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        url.append(contextPath);
        return url.toString();
    }
    
    private String getIssuer() {
        return "https://identity.focushive.com"; // Should be configurable
    }
    
    private void redirectWithError(HttpServletResponse response, String redirectUri, String error, 
                                 String errorDescription, String state) throws IOException {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("error=").append(error);
        url.append("&error_description=").append(errorDescription);
        if (state != null) {
            url.append("&state=").append(state);
        }
        response.sendRedirect(url.toString());
    }
    
    private String buildRedirectUrl(String redirectUri, String code, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("code=").append(code);
        if (state != null) {
            url.append("&state=").append(state);
        }
        return url.toString();
    }
    
    private OAuth2ClientResponse mapToClientResponse(OAuthClient client) {
        return OAuth2ClientResponse.builder()
            .clientId(client.getClientId())
            // Don't expose client secret in responses
            .clientName(client.getClientName())
            .description(client.getDescription())
            .redirectUris(client.getRedirectUris())
            .grantTypes(client.getAuthorizedGrantTypes())
            .scopes(client.getAuthorizedScopes())
            .accessTokenValiditySeconds(client.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds())
            .autoApprove(client.isAutoApprove())
            .enabled(client.isEnabled())
            .createdAt(client.getCreatedAt())
            .lastUsedAt(client.getLastUsedAt())
            .build();
    }
}