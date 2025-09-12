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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Minimal test configuration that provides only essential beans
 * to avoid conflicts with main application configuration.
 */
@TestConfiguration
@Profile("test")
public class MinimalTestConfig {

    /**
     * Test PasswordEncoder bean that supports {noop} prefix for client secrets.
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Test JWKSource that doesn't conflict with main config.
     */
    @Bean("testJwkSource")
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
     * Simple test OAuth2 client repository.
     */
    @Bean
    @Primary
    public RegisteredClientRepository testRegisteredClientRepository() {
        RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                .clientSecret("{noop}test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .scope("write")
                .build();

        return new InMemoryRegisteredClientRepository(testClient);
    }

    /**
     * AuthenticationManager bean for tests to satisfy dependencies.
     */
    @Bean
    @Primary
    public AuthenticationManager testAuthenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Authorization Server Settings for tests.
     */
    @Bean
    @Primary
    public AuthorizationServerSettings testAuthorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8081")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .jwkSetEndpoint("/oauth2/jwks")
            .oidcUserInfoEndpoint("/userinfo")
            .build();
    }

    /**
     * JWT Decoder for token validation in tests.
     */
    @Bean
    @Primary
    public JwtDecoder testJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * OAuth2 Authorization Server security filter chain (highest order).
     */
    @Bean
    @Order(1)
    @Primary
    public SecurityFilterChain testAuthorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());	// Enable OpenID Connect 1.0
        return http.build();
    }

    /**
     * Default security filter chain for non-OAuth2 endpoints.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain testDefaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .build();
    }

    /**
     * ObjectMapper bean for tests (replaces ApplicationConfig).
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Mock RedisTemplate for tests to avoid Redis dependency.
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> testRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    /**
     * Mock JSON RedisTemplate for tests.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> testJsonRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    private KeyPair generateTestRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate test RSA key pair", ex);
        }
    }
}