package com.focushive.identity.controller;

import com.focushive.identity.dto.*;
import com.focushive.identity.service.OAuth2AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 Authorization Server endpoints.
 * Provides OAuth2 authorization server capabilities as per CM3035 Advanced Web Design template.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2")
@Tag(name = "OAuth2 Authorization Server", description = "OAuth2 authorization server endpoints for identity federation")
@RequiredArgsConstructor
public class OAuth2AuthorizationController {

    private final OAuth2AuthorizationService oauth2AuthorizationService;

    @Operation(
            summary = "Authorization endpoint",
            description = "OAuth2 authorization code flow initiation endpoint. Redirects to login if user not authenticated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to login or callback with authorization code"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Client not found or invalid")
    })
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @Parameter(description = "OAuth2 client ID") @RequestParam(value = "client_id", required = false) String clientId,
            @Parameter(description = "Response type (must be 'code')") @RequestParam(value = "response_type", required = false) String responseType,
            @Parameter(description = "Redirect URI") @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(description = "Space-separated scopes") @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "State parameter for CSRF protection") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "Challenge for PKCE") @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @Parameter(description = "Challenge method for PKCE") @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Validate required parameters
        if (clientId == null || clientId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (responseType == null || responseType.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("OAuth2 authorization request for client: {}", clientId);
        
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.builder()
                .clientId(clientId.trim())
                .responseType(responseType.trim())
                .redirectUri(redirectUri.trim())
                .scope(scope)
                .state(state)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .build();

        oauth2AuthorizationService.authorize(authorizeRequest, authentication, request, response);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Token endpoint",
            description = "OAuth2 token endpoint for exchanging authorization codes for access tokens"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token issued successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or grant"),
            @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OAuth2TokenResponse> token(
            @Parameter(description = "Grant type") @RequestParam(value = "grant_type", required = false) String grantType,
            @Parameter(description = "Authorization code (for authorization_code grant)") @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "Redirect URI (for authorization_code grant)") @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(description = "PKCE code verifier") @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @Parameter(description = "Refresh token (for refresh_token grant)") @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @Parameter(description = "Scope (for client_credentials grant)") @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "Client ID (alternative to Authorization header)") @RequestParam(value = "client_id", required = false) String clientId,
            @Parameter(description = "Client secret (alternative to Authorization header)") @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {

        try {
            // Validate required parameters
            if (grantType == null || grantType.trim().isEmpty()) {
                log.warn("OAuth2 token request missing grant_type");
                return ResponseEntity.badRequest().build();
            }

            log.info("OAuth2 token request for grant type: {}", grantType);

            OAuth2TokenRequest tokenRequest = OAuth2TokenRequest.builder()
                    .grantType(grantType.trim())
                    .code(code)
                    .redirectUri(redirectUri)
                    .codeVerifier(codeVerifier)
                    .refreshToken(refreshToken)
                    .scope(scope)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .authorizationHeader(authorizationHeader)
                    .build();

            OAuth2TokenResponse tokenResponse = oauth2AuthorizationService.token(tokenRequest, request);
            log.info("OAuth2 token response generated successfully");
            return ResponseEntity.ok(tokenResponse);
        } catch (IllegalArgumentException e) {
            log.error("OAuth2 token request validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("OAuth2 token request processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Token introspection endpoint",
            description = "OAuth2 token introspection endpoint (RFC 7662) for validating access tokens"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token information returned"),
            @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OAuth2IntrospectionResponse> introspect(
            @Parameter(description = "Token to introspect") @RequestParam(value = "token", required = false) String token,
            @Parameter(description = "Token type hint") @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @Parameter(description = "Client ID (alternative to Authorization header)") @RequestParam(value = "client_id", required = false) String clientId,
            @Parameter(description = "Client secret (alternative to Authorization header)") @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        // Validate required parameters
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("OAuth2 token introspection request");

        OAuth2IntrospectionRequest introspectionRequest = OAuth2IntrospectionRequest.builder()
                .token(token.trim())
                .tokenTypeHint(tokenTypeHint)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationHeader(authorizationHeader)
                .build();

        OAuth2IntrospectionResponse response = oauth2AuthorizationService.introspect(introspectionRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Token revocation endpoint",
            description = "OAuth2 token revocation endpoint (RFC 7009) for invalidating tokens"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    @PostMapping(value = "/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> revoke(
            @Parameter(description = "Token to revoke") @RequestParam(value = "token", required = false) String token,
            @Parameter(description = "Token type hint") @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @Parameter(description = "Client ID (alternative to Authorization header)") @RequestParam(value = "client_id", required = false) String clientId,
            @Parameter(description = "Client secret (alternative to Authorization header)") @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        // Validate required parameters
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("OAuth2 token revocation request");

        OAuth2RevocationRequest revocationRequest = OAuth2RevocationRequest.builder()
                .token(token.trim())
                .tokenTypeHint(tokenTypeHint)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationHeader(authorizationHeader)
                .build();

        oauth2AuthorizationService.revoke(revocationRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "User info endpoint",
            description = "OAuth2 user info endpoint for retrieving user information based on access token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information returned"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired access token")
    })
    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "OAuth2")
    public ResponseEntity<OAuth2UserInfoResponse> userInfo(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.debug("OAuth2 user info request");
        
        OAuth2UserInfoResponse userInfo = oauth2AuthorizationService.getUserInfo(authorizationHeader);
        return ResponseEntity.ok(userInfo);
    }

    @Operation(
            summary = "Authorization server metadata",
            description = "OAuth2 Authorization Server Metadata endpoint (RFC 8414)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization server metadata")
    })
    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OAuth2ServerMetadata> serverMetadata(HttpServletRequest request) {
        log.debug("OAuth2 server metadata request");
        
        OAuth2ServerMetadata metadata = oauth2AuthorizationService.getServerMetadata(request);
        return ResponseEntity.ok(metadata);
    }

    @Operation(
            summary = "JWK Set endpoint",
            description = "JSON Web Key Set endpoint for token signature verification"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWK Set returned")
    })
    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwkSet() {
        log.debug("JWK Set request");
        
        Map<String, Object> jwkSet = oauth2AuthorizationService.getJwkSet();
        return ResponseEntity.ok(jwkSet);
    }

    @Operation(
            summary = "Client registration endpoint",
            description = "Dynamic client registration endpoint (RFC 7591)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid client metadata")
    })
    @PostMapping(value = "/client-registration", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<OAuth2ClientResponse> registerClient(
            @Valid @RequestBody OAuth2ClientRegistrationRequest request,
            Authentication authentication) {

        log.info("OAuth2 client registration request");

        OAuth2ClientResponse response = oauth2AuthorizationService.registerClient(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Client management endpoint",
            description = "Get registered OAuth2 clients for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping(value = "/clients", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<OAuth2ClientListResponse> getClients(Authentication authentication) {
        log.debug("OAuth2 client list request");

        OAuth2ClientListResponse response = oauth2AuthorizationService.getUserClients(authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update OAuth2 client",
            description = "Update an existing OAuth2 client configuration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify client")
    })
    @PutMapping(value = "/clients/{clientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<OAuth2ClientResponse> updateClient(
            @PathVariable String clientId,
            @Valid @RequestBody OAuth2ClientUpdateRequest request,
            Authentication authentication) {

        log.info("OAuth2 client update request for client: {}", clientId);

        OAuth2ClientResponse response = oauth2AuthorizationService.updateClient(clientId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete OAuth2 client",
            description = "Delete an OAuth2 client registration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete client")
    })
    @DeleteMapping("/clients/{clientId}")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Void> deleteClient(
            @PathVariable String clientId,
            Authentication authentication) {

        log.info("OAuth2 client deletion request for client: {}", clientId);

        oauth2AuthorizationService.deleteClient(clientId, authentication);
        return ResponseEntity.noContent().build();
    }
}