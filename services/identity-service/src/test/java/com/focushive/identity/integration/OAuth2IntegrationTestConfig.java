package com.focushive.identity.integration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Integration Test Configuration.
 * Provides a complete OAuth2 Authorization Server setup for integration testing.
 * Uses Spring Authorization Server with in-memory storage.
 */
@TestConfiguration
@Profile("integration-test")
public class OAuth2IntegrationTestConfig {

    private static final String TEST_CLIENT_UUID = "12345678-1234-1234-1234-123456789012";
    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_CLIENT_SECRET = "test-secret";

    /**
     * OAuth2 Authorization Server security filter chain for integration tests.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain integrationTestOAuth2SecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .authorizationEndpoint(authorizationEndpoint ->
                                        authorizationEndpoint.consentPage("/oauth2/consent")
                                )
                                .oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/**", "/.well-known/**", "/userinfo", "/connect/**")
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    /**
     * Test OAuth2 client repository with comprehensive client configuration.
     */
    @Bean("registeredClientRepository")
    @Primary
    public RegisteredClientRepository integrationTestRegisteredClientRepository() {
        RegisteredClient testClient = RegisteredClient.withId(TEST_CLIENT_UUID)
                .clientId(TEST_CLIENT_ID)
                .clientSecret("{noop}" + TEST_CLIENT_SECRET)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                .redirectUri("http://localhost:8080/callback")
                .redirectUri("http://localhost:3000/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("read")
                .scope("write")
                .scope("admin")
                .clientName("Integration Test Client")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false) // Auto-approve for tests
                        .requireProofKey(false) // No PKCE for tests
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofMinutes(30))
                        .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(testClient);
    }

    /**
     * Authorization server settings for integration tests.
     */
    @Bean("authorizationServerSettings")
    @Primary
    public AuthorizationServerSettings integrationTestAuthorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8081")
                .authorizationEndpoint("/oauth2/authorize")
                .deviceAuthorizationEndpoint("/oauth2/device_authorization")
                .deviceVerificationEndpoint("/oauth2/device_verification")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .jwkSetEndpoint("/oauth2/jwks")
                .oidcLogoutEndpoint("/connect/logout")
                .oidcUserInfoEndpoint("/userinfo")
                .oidcClientRegistrationEndpoint("/connect/register")
                .build();
    }

    /**
     * OAuth2 authorization service for integration tests.
     */
    @Bean("oAuth2AuthorizationService")
    @Primary
    public OAuth2AuthorizationService integrationTestOAuth2AuthorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    /**
     * JWK source for integration tests with a fixed key pair.
     */
    @Bean("jwkSource")
    @Primary
    public JWKSource<SecurityContext> integrationTestJwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("integration-test-key")
            .build();
            
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT decoder for integration tests.
     */
    @Bean("jwtDecoder")
    @Primary
    public JwtDecoder integrationTestJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Generate RSA key pair for JWT signing in tests.
     */
    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for integration tests", ex);
        }
    }
}