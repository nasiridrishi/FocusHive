package com.focushive.identity.service.impl;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.*;
import com.focushive.identity.exception.OAuth2AuthorizationException;
import com.focushive.identity.metrics.OAuth2MetricsService;
import com.focushive.identity.repository.*;
import com.focushive.identity.security.RSAJwtTokenProvider;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.OAuth2AuthorizationService;
import com.focushive.identity.service.OAuth2AuditService;
import com.focushive.identity.service.ScopeEnforcementService;
import com.focushive.identity.service.TokenRevocationEventService;
import com.focushive.identity.service.OAuth2RateLimitingService;
import com.focushive.identity.service.OAuth2TokenRotationService;
import com.focushive.identity.service.OAuth2ConsentService;
import com.focushive.identity.dto.OAuth2ConsentRequest;
import com.focushive.identity.dto.OAuth2ConsentResponse;
import com.focushive.identity.exception.InsufficientScopeException;
import com.focushive.identity.exception.RateLimitExceededException;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;

/**
 * Implementation of OAuth2 Authorization Server Service.
 * Provides comprehensive OAuth2 and OpenID Connect functionality.
 */
@Service
@Slf4j
@Transactional
public class OAuth2AuthorizationServiceImpl implements OAuth2AuthorizationService {

    private final OAuthClientRepository clientRepository;
    private final OAuthAccessTokenRepository accessTokenRepository;
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final OAuthAuthorizationCodeRepository authorizationCodeRepository;
    private final UserRepository userRepository;
    private final RSAJwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final TokenRevocationEventService revocationEventService;
    private final ScopeEnforcementService scopeEnforcementService;
    private final OAuth2AuditService auditService;
    private final OAuth2MetricsService metricsService;
    private final OAuth2RateLimitingService rateLimitingService;
    private final OAuth2TokenRotationService tokenRotationService;
    private final OAuth2ConsentService consentService;

    public OAuth2AuthorizationServiceImpl(
        OAuthClientRepository clientRepository,
        OAuthAccessTokenRepository accessTokenRepository,
        OAuthRefreshTokenRepository refreshTokenRepository,
        OAuthAuthorizationCodeRepository authorizationCodeRepository,
        UserRepository userRepository,
        RSAJwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder,
        EntityManager entityManager,
        TokenRevocationEventService revocationEventService,
        ScopeEnforcementService scopeEnforcementService,
        OAuth2AuditService auditService,
        OAuth2MetricsService metricsService,
        OAuth2RateLimitingService rateLimitingService,
        OAuth2TokenRotationService tokenRotationService,
        OAuth2ConsentService consentService
    ) {
        this.clientRepository = clientRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.revocationEventService = revocationEventService;
        this.scopeEnforcementService = scopeEnforcementService;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.rateLimitingService = rateLimitingService;
        this.tokenRotationService = tokenRotationService;
        this.consentService = consentService;
    }

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
        long startTime = System.currentTimeMillis();
        String ipAddress = auditService.getClientIpAddress();

        // Check rate limits
        if (!rateLimitingService.checkClientRateLimit(request.getClientId(),
                OAuth2RateLimitingService.OAuth2Endpoint.AUTHORIZE, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(request.getClientId(),
                OAuth2RateLimitingService.OAuth2Endpoint.AUTHORIZE);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(request.getClientId(), "authorize", retryAfter, 0L, null);
        }

        // Log authorization request and metrics
        auditService.logAuthorizationRequest(request, ipAddress);
        metricsService.recordAuthorizationRequest(request.getClientId());

        // Validate client
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(request.getClientId())
            .orElseThrow(() -> {
                auditService.logAuthorizationFailure(request.getClientId(), "invalid_client",
                    "Invalid client_id", ipAddress);
                metricsService.recordAuthorizationFailure(request.getClientId(), "invalid_client");
                return new IllegalArgumentException("Invalid client_id");
            });

        // Validate redirect URI
        if (!client.getRedirectUris().contains(request.getRedirectUri())) {
            auditService.logAuthorizationFailure(request.getClientId(), "invalid_redirect_uri",
                "Invalid redirect_uri", ipAddress);
            metricsService.recordAuthorizationFailure(request.getClientId(), "invalid_redirect_uri");
            throw new IllegalArgumentException("Invalid redirect_uri");
        }

        // Validate response type
        if (!"code".equals(request.getResponseType())) {
            auditService.logAuthorizationFailure(request.getClientId(), "unsupported_response_type",
                "Only 'code' response type is supported", ipAddress);
            metricsService.recordAuthorizationFailure(request.getClientId(), "unsupported_response_type");
            redirectWithError(httpResponse, request.getRedirectUri(), "unsupported_response_type",
                            "Only 'code' response type is supported", request.getState());
            return;
        }

        // Validate PKCE requirements
        if (client.isRequirePkce() && (request.getCodeChallenge() == null || request.getCodeChallenge().trim().isEmpty())) {
            auditService.logPKCEValidationFailure(request.getClientId(), ipAddress);
            metricsService.recordPKCEValidationFailure(request.getClientId());
            throw new IllegalArgumentException("PKCE is required for this client");
        }

        // Validate code challenge method if provided
        if (request.getCodeChallenge() != null && !request.getCodeChallenge().trim().isEmpty()) {
            String method = request.getCodeChallengeMethod();
            if (method == null || method.trim().isEmpty()) {
                // Default to "plain" if not specified
                request.setCodeChallengeMethod("plain");
            } else if (!"S256".equals(method) && !"plain".equals(method)) {
                throw new IllegalArgumentException("Unsupported code challenge method: " + method);
            }
        }

        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            // Redirect to login with return URL
            String loginUrl = "/login?return_url=" + httpRequest.getRequestURL() + "?" + httpRequest.getQueryString();
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        // Get authenticated user
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // Parse requested scopes
        Set<String> requestedScopes = request.getScope() != null ?
            new LinkedHashSet<>(Arrays.asList(request.getScope().split(" "))) :
            new LinkedHashSet<>();

        // Check if user has already approved consent (coming back from consent page)
        String consentApproved = httpRequest.getParameter("consent_approved");
        if (!"true".equals(consentApproved)) {
            // Check if consent is required
            if (consentService.isConsentRequired(user.getId(), request.getClientId(), requestedScopes)) {
                // Redirect to consent page
                String consentUrl = buildConsentUrl(request, httpRequest);
                httpResponse.sendRedirect(consentUrl);
                return;
            }
        }

        // Validate that user has granted required scopes
        if (!consentService.validateConsent(user.getId(), request.getClientId(), requestedScopes)) {
            auditService.logAuthorizationFailure(request.getClientId(), "access_denied",
                "User has not granted required scopes", ipAddress);
            metricsService.recordAuthorizationFailure(request.getClientId(), "access_denied");
            redirectWithError(httpResponse, request.getRedirectUri(), "access_denied",
                            "User has not granted required scopes", request.getState());
            return;
        }

        // Generate authorization code with PKCE support
        String authCode = generateAuthorizationCodeWithPKCE(
            request.getClientId(),
            request.getRedirectUri(),
            request.getScope(),
            request.getCodeChallenge(),
            request.getCodeChallengeMethod(),
            request.getState(),
            authentication
        );

        // Log successful authorization and metrics
        // User already retrieved earlier in the method
        if (user != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            auditService.logAuthorizationSuccess(request.getClientId(), user.getId(),
                authCode, request.getScope(), processingTime);
            metricsService.recordAuthorizationSuccess(request.getClientId(), processingTime);
        }

        // Redirect with authorization code
        String redirectUrl = buildRedirectUrl(request.getRedirectUri(), authCode, request.getState());
        httpResponse.sendRedirect(redirectUrl);
    }

    @Override
    public OAuth2TokenResponse token(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
        log.debug("Processing OAuth2 token request for grant type: {}", request.getGrantType());
        long startTime = System.currentTimeMillis();
        String ipAddress = auditService.getClientIpAddress();

        // Extract client ID for rate limiting
        String clientId = null;
        try {
            clientId = extractClientCredentials(request)[0];
        } catch (Exception e) {
            log.warn("Could not extract client ID for rate limiting", e);
        }

        // Check rate limits
        if (!rateLimitingService.checkClientRateLimit(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.TOKEN, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.TOKEN);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(clientId, "token", retryAfter, 0L, null);
        }

        OAuth2TokenResponse response;
        try {
            switch (request.getGrantType()) {
                case "authorization_code":
                    response = handleAuthorizationCodeGrant(request, httpRequest);
                    break;
                case "refresh_token":
                    response = handleRefreshTokenGrant(request, httpRequest);
                    break;
                case "client_credentials":
                    response = handleClientCredentialsGrant(request, httpRequest);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported grant type: " + request.getGrantType());
            }

            // Log successful token issuance
            long processingTime = System.currentTimeMillis() - startTime;
            // Ensure clientId is set for logging
            if (clientId == null) {
                clientId = extractClientCredentials(request)[0];
            }

            // Get user ID from token response if available
            UUID userId = null;
            if (!"client_credentials".equals(request.getGrantType())) {
                try {
                    OAuth2TokenInfo tokenInfo = validateAccessToken(response.getAccessToken());
                    if (tokenInfo != null && tokenInfo.getUserId() != null) {
                        userId = UUID.fromString(tokenInfo.getUserId());
                    }
                } catch (Exception e) {
                    log.warn("Could not extract user ID from token for audit log", e);
                }
            }

            auditService.logTokenIssuance(clientId, userId, request.getGrantType(),
                response.getScope(), hashToken(response.getAccessToken()), processingTime);

            return response;
        } catch (Exception e) {
            // Log token failure
            try {
                // Use already extracted clientId, or extract if null
                if (clientId == null) {
                    clientId = extractClientCredentials(request)[0];
                }
                auditService.logAuthorizationFailure(clientId, "token_error",
                    e.getMessage(), auditService.getClientIpAddress());
            } catch (Exception auditError) {
                log.error("Failed to log token error to audit log", auditError);
            }
            throw e;
        }
    }
    
    private OAuth2TokenResponse handleAuthorizationCodeGrant(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
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
    
    private OAuth2TokenResponse handleRefreshTokenGrant(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
        // Extract client credentials from Authorization header or request parameters
        String[] credentials = extractClientCredentials(request);
        String clientId = credentials[0];
        String clientSecret = credentials[1];

        // Validate client credentials
        if (!validateClientCredentials(clientId, clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }

        String tokenHash = hashToken(request.getRefreshToken());
        OAuthRefreshToken oldRefreshToken = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Verify token belongs to this client
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

        if (!oldRefreshToken.getClientId().equals(client.getId())) {
            throw new IllegalArgumentException("Refresh token does not belong to this client");
        }

        // Validate token for rotation eligibility
        tokenRotationService.validateTokenForRotation(oldRefreshToken);

        // Generate new access token
        User user = oldRefreshToken.getUser();
        Persona activePersona = user.getActivePersona().orElse(null);
        String newAccessTokenValue = jwtTokenProvider.generateToken(user, activePersona);
        String newAccessTokenHash = hashToken(newAccessTokenValue);

        // Save new access token
        OAuthAccessToken newAccessToken = OAuthAccessToken.builder()
            .tokenHash(newAccessTokenHash)
            .userId(oldRefreshToken.getUserId())
            .clientId(oldRefreshToken.getClientId())
            .scopes(new LinkedHashSet<>(oldRefreshToken.getScopes()))
            .expiresAt(Instant.now().plus(ACCESS_TOKEN_EXPIRY_SECONDS, ChronoUnit.SECONDS))
            .build();
        newAccessToken = accessTokenRepository.save(newAccessToken);

        // Rotate refresh token
        String ipAddress = auditService.getClientIpAddress();
        String userAgent = httpRequest.getHeader("User-Agent");
        var rotationResult = tokenRotationService.rotateRefreshToken(
            oldRefreshToken,
            newAccessToken,
            clientId,
            oldRefreshToken.getScopes(),
            ipAddress,
            userAgent
        );

        // Use the rotated token value, or keep existing if rotation is disabled
        String newRefreshTokenValue = rotationResult.getTokenValue();
        if (newRefreshTokenValue == null) {
            // Token rotation is disabled, client should keep their existing refresh token
            newRefreshTokenValue = request.getRefreshToken();
        }

        return OAuth2TokenResponse.builder()
            .accessToken(newAccessTokenValue)
            .refreshToken(newRefreshTokenValue)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .scope(String.join(" ", oldRefreshToken.getScopes()))
            .build();
    }
    
    private OAuth2TokenResponse handleClientCredentialsGrant(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
        // Validate that client credentials are provided
        if (!StringUtils.hasText(request.getClientId()) || !StringUtils.hasText(request.getClientSecret())) {
            if (!StringUtils.hasText(request.getAuthorizationHeader())) {
                throw new IllegalArgumentException("Client credentials are required");
            }
        }

        // Extract client credentials from Authorization header or request parameters
        String[] credentials = extractClientCredentials(request);
        String clientId = credentials[0];
        String clientSecret = credentials[1];

        // Validate client exists and is enabled
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client authentication failed",
                OAuth2AuthorizationException.ErrorCodes.INVALID_CLIENT));

        // Validate client credentials
        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new OAuth2AuthorizationException("Invalid client credentials",
                OAuth2AuthorizationException.ErrorCodes.INVALID_CLIENT);
        }

        // Verify client is authorized for client_credentials grant type
        if (!client.getAuthorizedGrantTypes().contains("client_credentials")) {
            throw new OAuth2AuthorizationException("Client is not authorized for grant type: client_credentials",
                OAuth2AuthorizationException.ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // Handle scope validation
        String requestedScope = request.getScope();
        Set<String> validatedScopes;

        if (!StringUtils.hasText(requestedScope)) {
            // Use all authorized scopes as default
            validatedScopes = client.getAuthorizedScopes();
        } else {
            // Validate requested scopes against authorized scopes
            Set<String> requestedScopes = parseScopes(requestedScope);
            Set<String> unauthorizedScopes = new LinkedHashSet<>(requestedScopes);
            unauthorizedScopes.removeAll(client.getAuthorizedScopes());

            if (!unauthorizedScopes.isEmpty()) {
                throw new OAuth2AuthorizationException(
                    "Requested scope is not authorized: " + String.join(", ", unauthorizedScopes),
                    OAuth2AuthorizationException.ErrorCodes.INVALID_SCOPE);
            }
            validatedScopes = requestedScopes;
        }

        String scope = String.join(" ", validatedScopes);

        // Get client-specific token validity or use default
        int tokenValidity = client.getAccessTokenValiditySeconds() != null
            ? client.getAccessTokenValiditySeconds()
            : ACCESS_TOKEN_EXPIRY_SECONDS;

        // Generate token with audit information
        return generateClientCredentialsToken(client, scope, tokenValidity, httpRequest);
    }

    @Override
    public OAuth2IntrospectionResponse introspect(OAuth2IntrospectionRequest request) {
        log.debug("Processing token introspection request");
        String clientId = null;
        String ipAddress = auditService.getClientIpAddress();

        // Extract client ID for rate limiting
        try {
            // For introspect, client ID is directly provided in the request
            clientId = request.getClientId();
        } catch (Exception e) {
            log.warn("Could not extract client ID for rate limiting", e);
        }

        // Check rate limits
        if (!rateLimitingService.checkClientRateLimit(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.INTROSPECT, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.INTROSPECT);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(clientId, "introspect", retryAfter, 0L, null);
        }

        // RFC 7662: If the authorization fails, the resource server responds with an
        // HTTP 401 (Unauthorized) using the error-handling mechanism of the bearer token
        // specification. For introspection, we return inactive response for invalid auth.

        // Check if Authorization header is present
        if (request.getAuthorizationHeader() == null || request.getAuthorizationHeader().isEmpty()) {
            log.debug("Missing Authorization header in introspection request");
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }

        // Try to extract client credentials from Authorization header
        String[] credentials;
        try {
            credentials = extractClientCredentialsFromHeader(request.getAuthorizationHeader());
        } catch (IllegalArgumentException e) {
            log.debug("Invalid Authorization header format: {}", e.getMessage());
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }

        // Validate client credentials
        clientId = credentials[0];
        if (!validateClientCredentials(credentials[0], credentials[1])) {
            log.debug("Invalid client credentials for introspection");
            auditService.logAuthorizationFailure(clientId, "invalid_client",
                "Invalid client credentials for introspection", auditService.getClientIpAddress());
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }

        // RFC 7662: Support token_type_hint for optimization
        OAuth2TokenInfo tokenInfo = null;

        // Check token_type_hint to optimize lookup
        if ("refresh_token".equals(request.getTokenTypeHint())) {
            // First try as refresh token
            tokenInfo = validateRefreshToken(request.getToken());
        } else if ("access_token".equals(request.getTokenTypeHint())) {
            // First try as access token
            tokenInfo = validateAccessToken(request.getToken());
        } else {
            // No hint or unknown hint - try access token first (most common)
            tokenInfo = validateAccessToken(request.getToken());
            if (tokenInfo == null) {
                // Fallback to refresh token
                tokenInfo = validateRefreshToken(request.getToken());
            }
        }

        // If still not found and we had a hint, try the opposite type
        if (tokenInfo == null && request.getTokenTypeHint() != null) {
            if ("refresh_token".equals(request.getTokenTypeHint())) {
                // We tried refresh, now try access
                tokenInfo = validateAccessToken(request.getToken());
            } else if ("access_token".equals(request.getTokenTypeHint())) {
                // We tried access, now try refresh
                tokenInfo = validateRefreshToken(request.getToken());
            }
        }

        if (tokenInfo == null) {
            // Log introspection for non-existent token
            auditService.logTokenIntrospection(clientId, "unknown", false);
            return OAuth2IntrospectionResponse.builder()
                .active(false)
                .build();
        }

        // Log successful introspection
        String tokenId = tokenInfo.getUserId() != null ? tokenInfo.getUserId() : tokenInfo.getClientId();
        auditService.logTokenIntrospection(clientId, tokenId, tokenInfo.isActive());

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
        String reason = "User requested revocation";
        String ipAddress = auditService.getClientIpAddress();
        String clientId = null;

        // Extract client ID for rate limiting
        try {
            // For introspect, client ID is directly provided in the request
            clientId = request.getClientId();
        } catch (Exception e) {
            log.warn("Could not extract client ID for rate limiting", e);
        }

        // Check rate limits
        if (!rateLimitingService.checkClientRateLimit(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.REVOKE, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.REVOKE);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(clientId, "revoke", retryAfter, 0L, null);
        }

        // Validate client authentication
        if (request.getClientId() == null || request.getClientId().isEmpty()) {
            throw new IllegalArgumentException("Client authentication required");
        }

        // Find and validate client
        OAuthClient client = clientRepository.findByClientId(request.getClientId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

        // Check client credentials based on client type
        if (client.getClientType() == OAuthClient.ClientType.CONFIDENTIAL) {
            // Confidential clients must provide valid secret
            if (request.getClientSecret() == null ||
                !passwordEncoder.matches(request.getClientSecret(), client.getClientSecret())) {
                throw new IllegalArgumentException("Invalid client credentials");
            }
        }
        // Public clients don't require a secret

        String tokenHash = hashToken(request.getToken());
        boolean tokenFound = false;

        // Determine search order based on token type hint
        if ("refresh_token".equals(request.getTokenTypeHint())) {
            // Try refresh token first
            tokenFound = revokeRefreshToken(tokenHash, client.getId());
            if (!tokenFound) {
                // Fallback to access token
                tokenFound = revokeAccessToken(tokenHash, client.getId());
            }
        } else if ("access_token".equals(request.getTokenTypeHint())) {
            // Try access token first
            tokenFound = revokeAccessToken(tokenHash, client.getId());
            if (!tokenFound) {
                // Fallback to refresh token
                tokenFound = revokeRefreshToken(tokenHash, client.getId());
            }
        } else {
            // No hint - try access token first (most common)
            tokenFound = revokeAccessToken(tokenHash, client.getId());
            if (!tokenFound) {
                // Then try refresh token
                tokenFound = revokeRefreshToken(tokenHash, client.getId());
            }
        }

        // Log token revocation
        if (tokenFound) {
            auditService.logTokenRevocation(client.getClientId(), request.getToken(), reason);
        }

        // RFC 7009 Section 2.2: Authorization server MUST NOT indicate whether
        // token was found or not. Silent success even if token doesn't exist.
        log.debug("Token revocation request processed. Token found: {}", tokenFound);
    }

    /**
     * Revoke an access token if it exists and belongs to the client.
     * @return true if token was found and revoked, false otherwise
     */
    private boolean revokeAccessToken(String tokenHash, UUID clientId) {
        Optional<OAuthAccessToken> tokenOpt = accessTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isPresent()) {
            OAuthAccessToken token = tokenOpt.get();

            // Only revoke if token belongs to this client
            if (!token.getClientId().equals(clientId)) {
                log.debug("Token does not belong to requesting client");
                return false;
            }

            // Don't re-revoke already revoked tokens
            if (token.isRevoked()) {
                log.debug("Token already revoked");
                return true;
            }

            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            token.setRevocationReason("Explicit revocation");
            accessTokenRepository.save(token);

            // Publish revocation event for distributed cache invalidation
            if (revocationEventService != null) {
                revocationEventService.publishAccessTokenRevocation(
                    tokenHash, token.getUserId(), clientId, "Explicit revocation");
            }

            log.info("Access token revoked for client: {}", clientId);
            return true;
        }
        return false;
    }

    /**
     * Revoke a refresh token if it exists and belongs to the client.
     * Also revokes all associated access tokens.
     * @return true if token was found and revoked, false otherwise
     */
    private boolean revokeRefreshToken(String tokenHash, UUID clientId) {
        Optional<OAuthRefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isPresent()) {
            OAuthRefreshToken token = tokenOpt.get();

            // Only revoke if token belongs to this client
            if (!token.getClientId().equals(clientId)) {
                log.debug("Token does not belong to requesting client");
                return false;
            }

            // Don't re-revoke already revoked tokens
            if (token.isRevoked()) {
                log.debug("Refresh token already revoked");
                return true;
            }

            // Revoke the refresh token
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            token.setRevocationReason("Explicit revocation");
            refreshTokenRepository.save(token);

            // Publish revocation event for distributed cache invalidation
            if (revocationEventService != null) {
                revocationEventService.publishRefreshTokenRevocation(
                    tokenHash, token.getUserId(), clientId, "Explicit revocation");
            }

            // Cascade revocation to all associated access tokens
            List<OAuthAccessToken> associatedTokens = accessTokenRepository.findByRefreshTokenId(token.getId());
            if (!associatedTokens.isEmpty()) {
                associatedTokens.forEach(accessToken -> {
                    if (!accessToken.isRevoked()) {
                        accessToken.setRevoked(true);
                        accessToken.setRevokedAt(Instant.now());
                        accessToken.setRevocationReason("Refresh token revoked");
                    }
                });
                accessTokenRepository.saveAll(associatedTokens);
                log.info("Revoked {} associated access tokens", associatedTokens.size());
            }

            log.info("Refresh token revoked for client: {}", clientId);
            return true;
        }
        return false;
    }

    @Override
    public OAuth2UserInfoResponse getUserInfo(String authorizationHeader) {
        String ipAddress = auditService.getClientIpAddress();

        // Validate Authorization header
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            // Check anonymous rate limit for invalid requests
            if (!rateLimitingService.checkClientRateLimit(null,
                    OAuth2RateLimitingService.OAuth2Endpoint.USERINFO, ipAddress)) {
                long resetTime = rateLimitingService.getResetTime(null,
                    OAuth2RateLimitingService.OAuth2Endpoint.USERINFO);
                long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
                throw new RateLimitExceededException(null, "userinfo", retryAfter, 0L, null);
            }

            auditService.logAuthorizationFailure(null, "invalid_authorization",
                "Invalid authorization header for UserInfo", auditService.getClientIpAddress());
            throw new IllegalArgumentException("Invalid authorization header");
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        OAuth2TokenInfo tokenInfo = validateAccessToken(accessToken);

        if (tokenInfo == null || !tokenInfo.isActive()) {
            throw new IllegalArgumentException("Invalid or expired access token");
        }

        // Check rate limits with the actual client ID from the token
        String clientId = tokenInfo.getClientId();
        if (!rateLimitingService.checkClientRateLimit(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.USERINFO, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(clientId,
                OAuth2RateLimitingService.OAuth2Endpoint.USERINFO);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(clientId, "userinfo", retryAfter, 0L, null);
        }

        // Check if this is a client credentials token (no user)
        if (tokenInfo.getUserId() == null || tokenInfo.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Fetch user from database
        User user = userRepository.findById(UUID.fromString(tokenInfo.getUserId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Parse scopes from token to determine what claims to include
        Set<String> scopes = parseScopes(tokenInfo.getScope());

        // OpenID Connect Core 1.0 requires 'openid' scope for UserInfo endpoint
        // However, we'll be lenient and allow access with just profile scope for backward compatibility
        // But enforce profile scope if accessing profile data
        boolean requestingProfile = true; // For now, assume always requesting profile
        if (requestingProfile && !scopes.contains("openid")) {
            // Enforce profile scope for accessing profile information
            scopeEnforcementService.enforceScope(scopes, "profile");
        }

        // Log UserInfo access
        auditService.logUserInfoAccess(user.getId(), tokenInfo.getClientId(), tokenInfo.getScope());

        // Build response based on scopes (OpenID Connect Core 1.0 - Section 5.4)
        OAuth2UserInfoResponse.OAuth2UserInfoResponseBuilder responseBuilder = OAuth2UserInfoResponse.builder();

        // Always include 'sub' (subject) - required by OIDC
        responseBuilder.sub(user.getId().toString());

        // Include profile claims if 'profile' scope is present
        if (scopeEnforcementService.hasRequiredScope(scopes, "profile")) {
            responseBuilder.name(user.getUsername()) // Username is the public display name
                .preferredUsername(user.getUsername())
                .givenName(user.getFirstName())
                .familyName(user.getLastName())
                .locale(user.getPreferredLanguage())
                .zoneinfo(user.getTimezone())
                .picture(""); // No profile image field yet
        }

        // Include email claims if 'email' scope is present
        if (scopeEnforcementService.hasRequiredScope(scopes, "email")) {
            responseBuilder.email(user.getEmail())
                .emailVerified(user.isEmailVerified());
        }

        // Include phone claims if 'phone' scope is present
        if (scopeEnforcementService.hasRequiredScope(scopes, "phone") && user.getPhoneNumber() != null) {
            responseBuilder.phoneNumber(user.getPhoneNumber())
                .phoneNumberVerified(user.isPhoneNumberVerified());
        }

        // For 'openid' scope only, just return sub (minimal response)
        // All other claims require additional scopes

        return responseBuilder.build();
    }

    @Override
    public OAuth2ServerMetadata getServerMetadata(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);

        return OAuth2ServerMetadata.builder()
            // Core OAuth2 endpoints (RFC 8414)
            .issuer(baseUrl)
            .authorizationEndpoint(baseUrl + "/oauth2/authorize")
            .tokenEndpoint(baseUrl + "/oauth2/token")
            .introspectionEndpoint(baseUrl + "/oauth2/introspect")
            .revocationEndpoint(baseUrl + "/oauth2/revoke")
            .jwksUri(baseUrl + "/oauth2/jwks")

            // OpenID Connect specific endpoints
            .userinfoEndpoint(baseUrl + "/oauth2/userinfo")
            .registrationEndpoint(null) // Dynamic registration not yet implemented

            // Supported grant types
            .grantTypesSupported(Arrays.asList(
                "authorization_code",
                "client_credentials",
                "refresh_token",
                "password"  // Resource Owner Password Credentials
            ))

            // Supported response types
            .responseTypesSupported(Arrays.asList(
                "code",
                "token",
                "code token"
            ))

            // Supported scopes
            .scopesSupported(Arrays.asList(
                "openid",
                "profile",
                "email",
                "offline_access",
                "api.read",
                "api.write",
                "admin"
            ))

            // Token endpoint authentication methods
            .tokenEndpointAuthMethodsSupported(Arrays.asList(
                "client_secret_basic",
                "client_secret_post"
            ))

            // PKCE support
            .codeChallengeMethodsSupported(Arrays.asList(
                "S256",
                "plain"
            ))

            // OpenID Connect specific - ID Token signing algorithms
            .idTokenSigningAlgValuesSupported(Arrays.asList(
                "RS256",
                "HS256"
            ))

            // OpenID Connect specific - Subject types
            .subjectTypesSupported(Arrays.asList(
                "public"
            ))

            // OpenID Connect specific - Claims
            .claimsSupported(Arrays.asList(
                "sub", "iss", "aud", "exp", "iat",
                "name", "email", "email_verified",
                "given_name", "family_name",
                "preferred_username", "locale", "zoneinfo"
            ))

            // Service documentation and policies
            .serviceDocumentation("https://docs.focushive.com/oauth2")
            .opPolicyUri("https://focushive.com/privacy")
            .opTosUri("https://focushive.com/terms")

            .build();
    }

    @Override
    public Map<String, Object> getJwkSet() {
        String ipAddress = auditService.getClientIpAddress();

        // Check rate limits (JWKS endpoint is typically anonymous)
        if (!rateLimitingService.checkClientRateLimit(null,
                OAuth2RateLimitingService.OAuth2Endpoint.JWKS, ipAddress)) {
            long resetTime = rateLimitingService.getResetTime(null,
                OAuth2RateLimitingService.OAuth2Endpoint.JWKS);
            long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
            throw new RateLimitExceededException(null, "jwks", retryAfter, 0L, null);
        }

        // Implementation would return JWK Set for token verification
        // This is typically handled by Spring Authorization Server's JWKSource
        // Use RSAJwtTokenProvider's JWKS method
        Map<String, Object> jwkSet = new HashMap<>();
        jwkSet.put("keys", jwtTokenProvider.getJWKS());
        return jwkSet;
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

        // Log client deletion
        auditService.logSecurityAlert(
            com.focushive.identity.audit.OAuth2AuditEvent.EventType.CLIENT_DELETION,
            "OAuth2 client deleted",
            auditService.getClientIpAddress(),
            client.getClientId(),
            Map.of("deletedBy", user.getEmail())
        );

        // Publish event to revoke all tokens in distributed cache
        if (revocationEventService != null) {
            revocationEventService.publishAllTokensRevocation(null, client.getId(), "Client deleted");
        }

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
        return generateAuthorizationCodeWithPKCE(clientId, redirectUri, scope, null, null, null, authentication);
    }

    /**
     * Generate authorization code with PKCE support
     */
    private String generateAuthorizationCodeWithPKCE(String clientId, String redirectUri, String scope,
                                                     String codeChallenge, String codeChallengeMethod,
                                                     String state, Authentication authentication) {
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
            .state(state)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
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
            
        // Generate JWT access token using JwtTokenClaims
        JwtTokenClaims tokenClaims = JwtTokenClaims.builder()
            .subject(user.getId().toString())
            .userId(user.getId().toString())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(user.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(java.util.stream.Collectors.toSet()))
            .scopes(parseScopes(scope))
            .clientId(client.getClientId())
            .audience(client.getClientId())
            .build();

        String accessToken = jwtTokenProvider.generateToken(tokenClaims);
        
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
    
    private OAuth2TokenResponse generateClientCredentialsToken(OAuthClient client, String scope,
                                                               int tokenValidity, HttpServletRequest httpRequest) {
        // Generate JWT access token for client credentials using JwtTokenClaims
        JwtTokenClaims tokenClaims = JwtTokenClaims.builder()
            .subject(client.getClientId())
            .clientId(client.getClientId())
            .audience(client.getClientId())
            .scopes(parseScopes(scope))
            .customClaims(Map.of(
                "client_name", client.getClientName(),
                "grant_type", "client_credentials"
            ))
            .build();

        String accessToken = jwtTokenProvider.generateToken(tokenClaims);

        // Capture audit information
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        // Save access token record (no user for client credentials)
        OAuthAccessToken tokenRecord = OAuthAccessToken.builder()
            .tokenHash(hashToken(accessToken))
            .userId(null) // No user for client credentials
            .clientId(client.getId())
            .scopes(parseScopes(scope))
            .expiresAt(Instant.now().plus(tokenValidity, ChronoUnit.SECONDS))
            .issuedIp(ipAddress)
            .userAgent(userAgent)
            .revoked(false)
            .build();

        accessTokenRepository.save(tokenRecord);

        // Client credentials grant does not return a refresh token per RFC 6749
        return OAuth2TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(tokenValidity)
            .scope(scope)
            .refreshToken(null) // No refresh token for client credentials
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

    /**
     * Validate a refresh token and return its information.
     * Similar to validateAccessToken but for refresh tokens.
     */
    private OAuth2TokenInfo validateRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        return refreshTokenRepository.findValidTokenByHash(tokenHash, Instant.now())
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
                    .tokenType("refresh_token") // Indicate this is a refresh token
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
    
    /**
     * Verify PKCE code challenge against the provided verifier
     */
    private boolean verifyPKCEChallenge(OAuthAuthorizationCode authCode, String codeVerifier) {
        if (authCode.getCodeChallenge() == null || authCode.getCodeChallengeMethod() == null) {
            return true; // No PKCE required
        }

        String method = authCode.getCodeChallengeMethod();
        String storedChallenge = authCode.getCodeChallenge();

        if ("plain".equals(method)) {
            // Plain method: challenge = verifier
            return storedChallenge.equals(codeVerifier);
        } else if ("S256".equals(method)) {
            // S256 method: challenge = BASE64URL(SHA256(verifier))
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String calculatedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
                return storedChallenge.equals(calculatedChallenge);
            } catch (NoSuchAlgorithmException e) {
                log.error("Failed to verify PKCE challenge: SHA-256 not available", e);
                return false;
            }
        } else {
            log.error("Unknown PKCE challenge method: {}", method);
            return false;
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
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(Arrays.asList(scope.split("\\s+")));
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

    /**
     * Build the consent URL for redirecting the user to the consent page.
     */
    private String buildConsentUrl(OAuth2AuthorizeRequest request, HttpServletRequest httpRequest) {
        try {
            StringBuilder url = new StringBuilder();
            url.append(httpRequest.getScheme()).append("://");
            url.append(httpRequest.getServerName());

            int port = httpRequest.getServerPort();
            if (("http".equals(httpRequest.getScheme()) && port != 80) ||
                ("https".equals(httpRequest.getScheme()) && port != 443)) {
                url.append(":").append(port);
            }

            url.append(httpRequest.getContextPath());
            url.append("/oauth2/consent");
            url.append("?client_id=").append(URLEncoder.encode(request.getClientId(), StandardCharsets.UTF_8.toString()));

            if (request.getScope() != null) {
                url.append("&scope=").append(URLEncoder.encode(request.getScope(), StandardCharsets.UTF_8.toString()));
            }

            if (request.getState() != null) {
                url.append("&state=").append(URLEncoder.encode(request.getState(), StandardCharsets.UTF_8.toString()));
            }

            if (request.getRedirectUri() != null) {
                url.append("&redirect_uri=").append(URLEncoder.encode(request.getRedirectUri(), StandardCharsets.UTF_8.toString()));
            }

            if (request.getResponseType() != null) {
                url.append("&response_type=").append(URLEncoder.encode(request.getResponseType(), StandardCharsets.UTF_8.toString()));
            }

            if (request.getCodeChallenge() != null) {
                url.append("&code_challenge=").append(URLEncoder.encode(request.getCodeChallenge(), StandardCharsets.UTF_8.toString()));
            }

            if (request.getCodeChallengeMethod() != null) {
                url.append("&code_challenge_method=").append(URLEncoder.encode(request.getCodeChallengeMethod(), StandardCharsets.UTF_8.toString()));
            }

            return url.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode consent URL parameters", e);
        }
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

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}