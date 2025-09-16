package com.focushive.identity.controller;

import com.focushive.identity.security.RSAJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenID Connect Discovery endpoint controller.
 * Provides /.well-known/openid_configuration endpoint for OAuth2/OpenID Connect compliance.
 *
 * This endpoint allows OAuth2 clients to discover the authorization server's capabilities
 * and endpoint URLs automatically, which is essential for full OAuth2/OIDC compliance.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OpenIdConnectDiscoveryController {

    @Autowired(required = false)
    private RSAJwtTokenProvider rsaJwtTokenProvider;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @GetMapping(value = {
        "/.well-known/openid_configuration",  // Standard with underscore
        "/.well-known/openid-configuration",  // Variant with hyphen
        "/.well-known/openid-configuration/identity"  // With identity suffix for notification service
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> openIdConfiguration() {
        log.debug("OpenID Connect discovery endpoint accessed");

        String issuer = getIssuerUrl();
        
        Map<String, Object> config = new HashMap<>();
        
        // Required OpenID Connect Discovery fields
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/oauth2/authorize");
        config.put("token_endpoint", issuer + "/oauth2/token");
        config.put("userinfo_endpoint", issuer + "/api/v1/users/me");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("end_session_endpoint", issuer + "/api/v1/auth/logout");
        config.put("introspection_endpoint", issuer + "/api/v1/auth/introspect");
        config.put("revocation_endpoint", issuer + "/api/v1/auth/revoke");
        
        // Supported response types
        config.put("response_types_supported", Arrays.asList(
            "code",
            "token",
            "id_token",
            "code token",
            "code id_token",
            "token id_token",
            "code token id_token"
        ));
        
        // Supported subject types
        config.put("subject_types_supported", Arrays.asList("public"));
        
        // Supported ID token signing algorithms
        config.put("id_token_signing_alg_values_supported", Arrays.asList(
            "HS256", 
            "HS512",
            "RS256"
        ));
        
        // Supported scopes
        config.put("scopes_supported", Arrays.asList(
            "openid",
            "profile", 
            "email",
            "personas",
            "identity.read",
            "identity.write"
        ));
        
        // Supported grant types
        config.put("grant_types_supported", Arrays.asList(
            "authorization_code",
            "client_credentials", 
            "refresh_token"
        ));
        
        // Supported token endpoint authentication methods
        config.put("token_endpoint_auth_methods_supported", Arrays.asList(
            "client_secret_basic",
            "client_secret_post"
        ));
        
        // Supported claims
        config.put("claims_supported", Arrays.asList(
            "sub",
            "iss", 
            "aud",
            "exp",
            "iat",
            "email",
            "email_verified",
            "name",
            "given_name",
            "family_name",
            "preferred_username",
            "profile",
            "persona_id",
            "persona_name",
            "persona_type"
        ));
        
        // Code challenge methods for PKCE
        config.put("code_challenge_methods_supported", Arrays.asList("plain", "S256"));
        
        // Additional OAuth2 capabilities
        config.put("response_modes_supported", Arrays.asList("query", "fragment", "form_post"));
        config.put("token_endpoint_auth_signing_alg_values_supported", Arrays.asList("HS256", "RS256"));
        config.put("request_parameter_supported", true);
        config.put("request_uri_parameter_supported", false);
        config.put("require_request_uri_registration", false);
        config.put("claims_parameter_supported", false);
        
        // FocusHive-specific extensions
        Map<String, Object> focushiveExtensions = new HashMap<>();
        focushiveExtensions.put("personas_endpoint", issuer + "/api/v1/personas");
        focushiveExtensions.put("persona_switch_endpoint", issuer + "/api/v1/personas/{id}/switch");
        focushiveExtensions.put("privacy_settings_endpoint", issuer + "/api/v1/privacy/settings");
        focushiveExtensions.put("version", "1.0.0");
        focushiveExtensions.put("features", Arrays.asList("personas", "privacy_controls", "rate_limiting"));
        
        config.put("focushive_extensions", focushiveExtensions);
        
        return ResponseEntity.ok(config);
    }

    /**
     * JWKS (JSON Web Key Set) endpoint for public key discovery
     * This is required for JWT signature verification
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        log.debug("JWKS endpoint accessed");

        Map<String, Object> jwks = new HashMap<>();

        // Get public keys from RSA provider if available
        if (rsaJwtTokenProvider != null) {
            List<Map<String, Object>> keys = rsaJwtTokenProvider.getJWKS();
            jwks.put("keys", keys);

            if (!keys.isEmpty()) {
                log.debug("Returning {} public keys in JWKS response", keys.size());
            } else {
                log.warn("RSA provider returned empty key set - may be using HMAC mode");
            }
        } else {
            // Fallback for HMAC mode - cannot expose symmetric keys
            log.warn("RSA JWT provider not available - returning empty key set");
            jwks.put("keys", Arrays.asList());
        }

        return ResponseEntity.ok(jwks);
    }

    private String getIssuerUrl() {
        // Use configured base URL or construct from server info
        if (baseUrl != null && !baseUrl.startsWith("http://localhost")) {
            return baseUrl;
        }
        
        // Fallback to localhost construction
        String protocol = "http"; // TODO: Support HTTPS detection
        String port = serverPort.equals("80") || serverPort.equals("443") ? "" : ":" + serverPort;
        return protocol + "://localhost" + port + contextPath;
    }
}