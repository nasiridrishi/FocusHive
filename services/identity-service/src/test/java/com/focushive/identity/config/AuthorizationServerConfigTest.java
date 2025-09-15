package com.focushive.identity.config;

import com.focushive.identity.repository.OAuthClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2 Authorization Server Configuration Tests")
class AuthorizationServerConfigTest {

    @Mock
    private OAuthClientRepository repository;

    private AuthorizationServerConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthorizationServerConfig(repository);
        ReflectionTestUtils.setField(config, "issuer", "https://identity.focushive.app/identity");
    }

    @Test
    @DisplayName("Should create RegisteredClientRepository with default clients")
    void testRegisteredClientRepository() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();

        // Then
        assertNotNull(clientRepository);

        // Test web client
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");
        assertNotNull(webClient);
        assertEquals("focushive-web", webClient.getClientId());
        assertTrue(webClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE));
        assertTrue(webClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
        assertTrue(webClient.getScopes().contains(OidcScopes.OPENID));
        assertTrue(webClient.getScopes().contains(OidcScopes.PROFILE));
        assertTrue(webClient.getScopes().contains(OidcScopes.EMAIL));

        // Test mobile client
        RegisteredClient mobileClient = clientRepository.findByClientId("focushive-mobile");
        assertNotNull(mobileClient);
        assertEquals("focushive-mobile", mobileClient.getClientId());
        assertTrue(mobileClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        assertTrue(mobileClient.getClientSettings().isRequireProofKey()); // PKCE required
    }

    @Test
    @DisplayName("Should configure web client with correct redirect URIs")
    void testWebClientRedirectUris() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");

        // Then
        assertNotNull(webClient);
        assertTrue(webClient.getRedirectUris().contains("http://localhost:3000/auth/callback"));
        assertTrue(webClient.getRedirectUris().contains("http://localhost:8080/login/oauth2/code/focushive"));
    }

    @Test
    @DisplayName("Should configure mobile client with PKCE support")
    void testMobileClientPKCE() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient mobileClient = clientRepository.findByClientId("focushive-mobile");

        // Then
        assertNotNull(mobileClient);
        assertTrue(mobileClient.getClientSettings().isRequireProofKey());
        assertFalse(mobileClient.getClientSettings().isRequireAuthorizationConsent());
        assertEquals(ClientAuthenticationMethod.NONE, mobileClient.getClientAuthenticationMethods().iterator().next());
    }

    @Test
    @DisplayName("Should configure token settings with correct TTL")
    void testTokenSettings() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");

        // Then
        assertNotNull(webClient);
        assertEquals(Duration.ofHours(1), webClient.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofDays(30), webClient.getTokenSettings().getRefreshTokenTimeToLive());
        assertEquals(Duration.ofMinutes(10), webClient.getTokenSettings().getAuthorizationCodeTimeToLive());
        assertFalse(webClient.getTokenSettings().isReuseRefreshTokens());
    }

    @Test
    @DisplayName("Should configure authorization server settings with correct issuer")
    void testAuthorizationServerSettings() {
        // When
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        // Then
        assertNotNull(settings);
        assertEquals("https://identity.focushive.app/identity", settings.getIssuer());
        assertEquals("/oauth2/authorize", settings.getAuthorizationEndpoint());
        assertEquals("/oauth2/token", settings.getTokenEndpoint());
        assertEquals("/oauth2/jwks", settings.getJwkSetEndpoint());
        assertEquals("/oauth2/introspect", settings.getTokenIntrospectionEndpoint());
        assertEquals("/oauth2/revoke", settings.getTokenRevocationEndpoint());
        assertEquals("/userinfo", settings.getOidcUserInfoEndpoint());
    }

    @Test
    @DisplayName("Should support required grant types")
    void testSupportedGrantTypes() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");

        // Then
        assertTrue(webClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE));
        assertTrue(webClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
        assertTrue(webClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS));
    }

    @Test
    @DisplayName("Should support required scopes")
    void testSupportedScopes() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");

        // Then
        assertTrue(webClient.getScopes().contains(OidcScopes.OPENID));
        assertTrue(webClient.getScopes().contains(OidcScopes.PROFILE));
        assertTrue(webClient.getScopes().contains(OidcScopes.EMAIL));
        assertTrue(webClient.getScopes().contains("read"));
        assertTrue(webClient.getScopes().contains("write"));
        assertTrue(webClient.getScopes().contains("personas"));
    }

    @Test
    @DisplayName("Should configure client authentication methods")
    void testClientAuthenticationMethods() {
        // When
        RegisteredClientRepository clientRepository = config.registeredClientRepository();
        RegisteredClient webClient = clientRepository.findByClientId("focushive-web");

        // Then
        assertTrue(webClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
        assertTrue(webClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_POST));
    }

    @Test
    @DisplayName("Should configure OpenID Connect endpoints")
    void testOpenIDConnectEndpoints() {
        // When
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        // Then
        assertNotNull(settings);
        assertEquals("/connect/logout", settings.getOidcLogoutEndpoint());
        assertEquals("/userinfo", settings.getOidcUserInfoEndpoint());
        assertEquals("/connect/register", settings.getOidcClientRegistrationEndpoint());
    }
}