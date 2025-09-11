package com.focushive.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Test OAuth2 Configuration providing minimal beans needed for OAuth2AuthorizationController tests.
 * This configuration provides test-specific OAuth2 server beans without the full authorization server setup.
 */
@TestConfiguration
@Profile("test")
public class TestOAuth2Config {

    /**
     * Test JWT key source for token signing and validation.
     */
    @Bean
    @Primary
    public JWKSource<SecurityContext> testJwkSource() {
        KeyPair keyPair = generateTestRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("test-key-id")
                .build();
                
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Test JWT decoder for validating tokens in tests.
     */
    @Bean
    @Primary
    public JwtDecoder testJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSetUri("http://localhost/jwks").build();
    }

    /**
     * Test registered client repository with a sample client.
     */
    @Bean
    @Primary
    public RegisteredClientRepository testRegisteredClientRepository() {
        RegisteredClient testClient = RegisteredClient.withId("test-client-uuid")
                .clientId("test-client")
                .clientSecret("{noop}test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:3000/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientName("Test Client")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(1))
                        .authorizationCodeTimeToLive(Duration.ofMinutes(10))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(testClient);
    }

    /**
     * Test authorization server settings.
     */
    @Bean
    @Primary
    public AuthorizationServerSettings testAuthorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8081")
                .authorizationEndpoint("/api/v1/oauth2/authorize")
                .tokenEndpoint("/api/v1/oauth2/token")
                .tokenIntrospectionEndpoint("/api/v1/oauth2/introspect")
                .tokenRevocationEndpoint("/api/v1/oauth2/revoke")
                .jwkSetEndpoint("/api/v1/oauth2/jwks")
                .oidcUserInfoEndpoint("/api/v1/oauth2/userinfo")
                .build();
    }

    /**
     * Generate a test RSA key pair.
     */
    private KeyPair generateTestRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate test RSA key pair", ex);
        }
        return keyPair;
    }
}