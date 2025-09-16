package com.focushive.identity.controller;

import com.focushive.identity.dto.OAuth2ConsentPageData;
import com.focushive.identity.dto.OAuth2ConsentRequest;
import com.focushive.identity.dto.OAuth2ConsentResponse;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.OAuth2AuditService;
import com.focushive.identity.service.OAuth2ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Controller for OAuth2 consent management.
 * Handles consent page display and consent processing.
 */
@RestController
@RequestMapping("/oauth2")
@Tag(name = "OAuth2 Consent", description = "OAuth2 consent management endpoints")
@RequiredArgsConstructor
@Slf4j
public class OAuth2ConsentController {

    private final OAuth2ConsentService consentService;
    private final OAuthClientRepository clientRepository;
    private final UserRepository userRepository;
    private final OAuth2AuditService auditService;

    /**
     * Display the consent page data.
     * This endpoint returns JSON data that can be used by a frontend to display the consent page.
     */
    @GetMapping("/consent")
    @Operation(summary = "Get consent page data",
               description = "Returns data needed to display the consent page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consent page data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ConsentPageData> getConsentPageData(
            @Parameter(description = "Client ID") @RequestParam("client_id") String clientId,
            @Parameter(description = "Requested scopes") @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "OAuth2 state parameter") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "Redirect URI") @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(description = "Response type") @RequestParam(value = "response_type", required = false) String responseType,
            @Parameter(description = "PKCE code challenge") @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @Parameter(description = "PKCE code challenge method") @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod) {

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user details
        User user = userRepository.findByEmail(authentication.getName())
            .orElse(null);
        if (user == null) {
            log.error("Authenticated user not found: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get client details
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElse(null);
        if (client == null) {
            log.error("OAuth client not found: {}", clientId);
            return ResponseEntity.notFound().build();
        }

        // Parse requested scopes
        Set<String> requestedScopes = scope != null ?
            new LinkedHashSet<>(Arrays.asList(scope.split(" "))) :
            new LinkedHashSet<>();

        // Get existing consent if any
        Optional<OAuth2ConsentResponse> existingConsentOpt = consentService.getConsent(user.getId(), clientId);

        // Prepare consent page data
        OAuth2ConsentPageData pageData = OAuth2ConsentPageData.builder()
            .clientId(clientId)
            .clientName(client.getClientName())
            .clientDescription(client.getDescription())
            .requestedScopes(requestedScopes)
            .existingConsent(existingConsentOpt.orElse(null))
            .state(state)
            .redirectUri(redirectUri)
            .responseType(responseType)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .build();

        return ResponseEntity.ok(pageData);
    }

    /**
     * Process user consent decision.
     */
    @PostMapping("/consent")
    @Operation(summary = "Process user consent",
               description = "Process the user's consent decision for the requested scopes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect to authorization endpoint with consent decision"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public void processConsent(
            @Parameter(description = "Consent decision") @RequestParam("decision") String decision,
            @Parameter(description = "Client ID") @RequestParam("client_id") String clientId,
            @Parameter(description = "Granted scopes") @RequestParam(value = "granted_scopes", required = false) List<String> grantedScopes,
            @Parameter(description = "Remember consent") @RequestParam(value = "remember_consent", defaultValue = "false") boolean rememberConsent,
            @Parameter(description = "OAuth2 state") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "Redirect URI") @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(description = "Response type") @RequestParam(value = "response_type", required = false) String responseType,
            @Parameter(description = "PKCE code challenge") @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @Parameter(description = "PKCE code challenge method") @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        // Get user details
        User user = userRepository.findByEmail(authentication.getName())
            .orElse(null);
        if (user == null) {
            log.error("Authenticated user not found: {}", authentication.getName());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
            return;
        }

        // Process consent based on decision
        if ("approve".equalsIgnoreCase(decision)) {
            // User approved - process consent
            Set<String> approvedScopes = grantedScopes != null ?
                new LinkedHashSet<>(grantedScopes) :
                new LinkedHashSet<>();

            OAuth2ConsentRequest consentRequest = OAuth2ConsentRequest.builder()
                .userId(user.getId())
                .clientId(clientId)
                .approved(true)
                .requestedScopes(approvedScopes) // For simplicity, using approved as requested
                .approvedScopes(approvedScopes)
                .rememberConsent(rememberConsent)
                .ipAddress(auditService.getClientIpAddress())
                .userAgent(request.getHeader("User-Agent"))
                .build();

            OAuth2ConsentResponse consentResponse = consentService.processConsent(consentRequest);

            if (consentResponse.isGranted()) {
                // Redirect back to authorization endpoint with consent approval
                String authUrl = buildAuthorizationRedirectUrl(true, clientId, approvedScopes,
                    state, redirectUri, responseType, codeChallenge, codeChallengeMethod, request);
                response.sendRedirect(authUrl);
            } else {
                // Consent processing failed
                redirectWithError(response, redirectUri, "server_error",
                    "Failed to process consent", state);
            }
        } else {
            // User denied - redirect with error
            redirectWithError(response, redirectUri, "access_denied",
                "User denied consent", state);
        }
    }

    /**
     * Revoke consent for a client.
     */
    @DeleteMapping("/consent/{clientId}")
    @Operation(summary = "Revoke consent",
               description = "Revoke previously granted consent for a client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Consent revoked successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "Consent not found")
    })
    public ResponseEntity<Void> revokeConsent(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Parameter(description = "Revocation reason") @RequestParam(value = "reason", required = false) String reason) {

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user details
        User user = userRepository.findByEmail(authentication.getName())
            .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Revoke consent
        boolean revoked = consentService.revokeConsent(user.getId(), clientId, reason);
        if (revoked) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all consents for the current user.
     */
    @GetMapping("/consents")
    @Operation(summary = "List user consents",
               description = "Get all consents granted by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consents retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<List<OAuth2ConsentResponse>> getUserConsents() {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user details
        User user = userRepository.findByEmail(authentication.getName())
            .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get all consents for user
        List<OAuth2ConsentResponse> consents = consentService.getUserConsents(user.getId());
        return ResponseEntity.ok(consents);
    }

    /**
     * Build the authorization redirect URL with consent decision.
     */
    private String buildAuthorizationRedirectUrl(boolean approved, String clientId,
                                                 Set<String> approvedScopes, String state,
                                                 String redirectUri, String responseType,
                                                 String codeChallenge, String codeChallengeMethod,
                                                 HttpServletRequest request) {
        try {
            StringBuilder url = new StringBuilder();
            url.append(request.getScheme()).append("://");
            url.append(request.getServerName());

            int port = request.getServerPort();
            if (("http".equals(request.getScheme()) && port != 80) ||
                ("https".equals(request.getScheme()) && port != 443)) {
                url.append(":").append(port);
            }

            url.append(request.getContextPath());
            url.append("/oauth2/authorize");
            url.append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString()));

            if (approved && approvedScopes != null && !approvedScopes.isEmpty()) {
                url.append("&scope=").append(URLEncoder.encode(String.join(" ", approvedScopes), StandardCharsets.UTF_8.toString()));
            }

            if (state != null) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8.toString()));
            }

            if (redirectUri != null) {
                url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()));
            }

            if (responseType != null) {
                url.append("&response_type=").append(URLEncoder.encode(responseType, StandardCharsets.UTF_8.toString()));
            }

            if (codeChallenge != null) {
                url.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.toString()));
            }

            if (codeChallengeMethod != null) {
                url.append("&code_challenge_method=").append(URLEncoder.encode(codeChallengeMethod, StandardCharsets.UTF_8.toString()));
            }

            // Add consent approval flag
            url.append("&consent_approved=").append(approved);

            return url.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode authorization URL parameters", e);
        }
    }

    /**
     * Redirect with error to the redirect URI.
     */
    private void redirectWithError(HttpServletResponse response, String redirectUri,
                                   String error, String errorDescription, String state) throws IOException {
        if (redirectUri == null || redirectUri.isEmpty()) {
            // No redirect URI - return error response
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorDescription);
            return;
        }

        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("error=").append(error);
        url.append("&error_description=").append(URLEncoder.encode(errorDescription, StandardCharsets.UTF_8));

        if (state != null) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }

        response.sendRedirect(url.toString());
    }
}