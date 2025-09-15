package com.focushive.identity.config;

import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.repository.OAuthClientRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
 * OAuth2 Authorization Server Configuration.
 * Configures Spring Authorization Server with comprehensive OAuth2 and OpenID Connect support.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
@Profile({"!test", "!owasp-test"})
public class AuthorizationServerConfig {

    private final OAuthClientRepository clientRepository;

    @Value("${auth.issuer}")
    private String issuer;

    /**
     * Configure the authorization server security filter chain.
     * This handles all OAuth2 and OpenID Connect protocol endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = 
            new OAuth2AuthorizationServerConfigurer();

        http
            .securityMatcher(request -> 
                authorizationServerConfigurer.getEndpointsMatcher().matches(request) ||
                request.getRequestURI().startsWith("/api/v1/oauth2/")
            )
            .with(authorizationServerConfigurer, (authorizationServer) ->
                authorizationServer
                    .authorizationEndpoint(authorizationEndpoint ->
                        authorizationEndpoint
                            .consentPage("/oauth2/consent")
                    )
                    .oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
            )
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/oauth2/jwks", "/.well-known/openid-configuration").permitAll()
                    .requestMatchers("/api/v1/oauth2/**").permitAll() // Allow our custom OAuth2 endpoints
                    .anyRequest().authenticated()
            )
            // Disable CSRF for stateless authentication
            .csrf(csrf -> csrf.disable())
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
     * Configure the form login security filter chain for non-OAuth2 endpoints.
     * This filter chain should NOT handle API endpoints - those are handled by the defaultSecurityFilterChain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(request -> {
                String uri = request.getRequestURI();
                // Only handle specific web UI endpoints - form login, consent, etc.
                // MUST NOT handle API endpoints which are handled by defaultSecurityFilterChain
                return uri.equals("/login") ||
                       uri.equals("/logout") ||
                       uri.equals("/oauth2/consent") ||
                       uri.equals("/") ||
                       (uri.startsWith("/static/") || uri.startsWith("/css/") || uri.startsWith("/js/"));
            })
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/static/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/oauth2/consent").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * RegisteredClientRepository with demo clients for development.
     * In production, this would integrate with the database.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("focushive-web")
            .clientSecret("{noop}demo-secret") // In production, use proper encoding
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            // Local development redirect URIs
            .redirectUri("http://localhost:3000/auth/callback")
            .redirectUri("http://localhost:3000/callback")
            .redirectUri("http://localhost:8080/login/oauth2/code/focushive")
            // Production redirect URIs via Cloudflare
            .redirectUri("https://focushive.app/auth/callback")
            .redirectUri("https://focushive.app/callback")
            .redirectUri("https://backend.focushive.app/login/oauth2/code/focushive")
            // Post-logout redirect URIs
            .postLogoutRedirectUri("http://localhost:3000/")
            .postLogoutRedirectUri("https://focushive.app/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .scope("personas")
            .clientName("FocusHive Web Application")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(false) // Make PKCE optional for web clients
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .authorizationCodeTimeToLive(Duration.ofMinutes(10))
                .reuseRefreshTokens(false)
                .build())
            .build();

        RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("focushive-mobile")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // Public client
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("focushive://auth/callback")
            .redirectUri("com.focushive.app://auth/callback") // Custom scheme for mobile
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .scope("personas")
            .clientName("FocusHive Mobile App")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true) // PKCE required for public clients
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .authorizationCodeTimeToLive(Duration.ofMinutes(10))
                .reuseRefreshTokens(false)
                .build())
            .build();

        // Test client for development and testing purposes
        RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("test-client")
            .clientSecret("{noop}test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            // Test redirect URIs - very permissive for testing
            .redirectUri("http://localhost:3000/callback")
            .redirectUri("http://localhost:8080/callback")
            .redirectUri("https://oauth.pstmn.io/v1/callback") // Postman callback
            .redirectUri("https://oidcdebugger.com/debug") // OIDC debugger
            .postLogoutRedirectUri("http://localhost:3000/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .clientName("Test Client")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false) // No consent for testing
                .requireProofKey(false) // PKCE optional for testing
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .authorizationCodeTimeToLive(Duration.ofMinutes(10))
                .reuseRefreshTokens(false)
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(webClient, mobileClient, testClient);
    }

    /**
     * Authorization Server Settings configuration.
     * Uses configured issuer to ensure consistent token generation.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        log.info("Configuring Authorization Server with issuer: {}", issuer);
        return AuthorizationServerSettings.builder()
            .issuer(issuer) // Use configured issuer from environment/properties
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
     * JWT Key Source for signing tokens.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
            
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT Decoder for validating tokens.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // Helper methods

    private KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
        return keyPair;
    }
}