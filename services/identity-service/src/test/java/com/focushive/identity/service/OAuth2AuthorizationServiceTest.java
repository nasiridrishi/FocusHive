package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.OAuthAccessToken;
import com.focushive.identity.entity.OAuthAuthorizationCode;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.OAuthRefreshToken;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.OAuthAccessTokenRepository;
import com.focushive.identity.repository.OAuthAuthorizationCodeRepository;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.OAuthRefreshTokenRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.impl.OAuth2AuthorizationServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2AuthorizationService implementation.
 * Following TDD approach - tests written before implementation.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthorizationServiceTest {

    @Mock
    private OAuthClientRepository clientRepository;
    
    @Mock
    private OAuthAccessTokenRepository accessTokenRepository;
    
    @Mock
    private OAuthRefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private OAuthAuthorizationCodeRepository authorizationCodeRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private HttpServletRequest httpRequest;
    
    @Mock
    private HttpServletResponse httpResponse;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private UserDetails userDetails;

    private OAuth2AuthorizationServiceImpl oauth2Service;
    
    private User testUser;
    private OAuthClient testClient;
    
    @BeforeEach
    void setUp() {
        oauth2Service = new OAuth2AuthorizationServiceImpl(
            clientRepository,
            accessTokenRepository,
            refreshTokenRepository,
            authorizationCodeRepository,
            userRepository,
            jwtTokenProvider,
            passwordEncoder,
            entityManager
        );
        
        // Set up test data
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .username("testuser")
            .build();
            
        testClient = OAuthClient.builder()
            .id(UUID.randomUUID())
            .clientId("test-client")
            .clientSecret("encoded-secret")
            .clientName("Test Client")
            .authorizedGrantTypes(Set.of("authorization_code", "refresh_token"))
            .redirectUris(Set.of("http://localhost:8080/callback"))
            .authorizedScopes(Set.of("read", "write"))
            .enabled(true)
            .build();
    }
    
    @Test
    void testValidateClientCredentials_ValidCredentials_ReturnsTrue() {
        // Given
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        // When
        boolean result = oauth2Service.validateClientCredentials("test-client", "client-secret");
        
        // Then
        assertThat(result).isTrue();
        verify(clientRepository).findByClientIdAndEnabledTrue("test-client");
        verify(passwordEncoder).matches("client-secret", "encoded-secret");
    }
    
    @Test
    void testValidateClientCredentials_InvalidCredentials_ReturnsFalse() {
        // Given
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrong-secret", "encoded-secret"))
            .thenReturn(false);
            
        // When
        boolean result = oauth2Service.validateClientCredentials("test-client", "wrong-secret");
        
        // Then
        assertThat(result).isFalse();
        verify(clientRepository).findByClientIdAndEnabledTrue("test-client");
        verify(passwordEncoder).matches("wrong-secret", "encoded-secret");
    }
    
    @Test
    void testValidateClientCredentials_ClientNotFound_ReturnsFalse() {
        // Given
        when(clientRepository.findByClientIdAndEnabledTrue("non-existent"))
            .thenReturn(Optional.empty());
            
        // When
        boolean result = oauth2Service.validateClientCredentials("non-existent", "secret");
        
        // Then
        assertThat(result).isFalse();
        verify(clientRepository).findByClientIdAndEnabledTrue("non-existent");
        verifyNoInteractions(passwordEncoder);
    }
    
    @Test
    void testGenerateAuthorizationCode_ValidRequest_ReturnsCode() {
        // Given
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
        when(authorizationCodeRepository.save(any(OAuthAuthorizationCode.class)))
            .thenAnswer(invocation -> {
                OAuthAuthorizationCode code = invocation.getArgument(0);
                ReflectionTestUtils.setField(code, "id", UUID.randomUUID());
                return code;
            });
            
        // When
        String authCode = oauth2Service.generateAuthorizationCode(
            "test-client", 
            "http://localhost:8080/callback", 
            "read write", 
            authentication
        );
        
        // Then
        assertThat(authCode).isNotNull();
        assertThat(authCode).hasSize(128); // Generated code length
        verify(authorizationCodeRepository).save(any(OAuthAuthorizationCode.class));
    }
    
    @Test
    void testValidateAuthorizationCode_ValidCode_ReturnsCodeInfo() {
        // Given
        com.focushive.identity.entity.OAuthAuthorizationCode authCode = com.focushive.identity.entity.OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("test-auth-code")
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
            
        when(authorizationCodeRepository.findValidCodeForClient(
            eq("test-auth-code"), 
            eq(testClient.getId()), 
            eq("http://localhost:8080/callback"), 
            any(Instant.class)
        )).thenReturn(Optional.of(authCode));
        
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
            
        // When
        com.focushive.identity.dto.OAuth2AuthorizationCode result = oauth2Service.validateAuthorizationCode(
            "test-auth-code", 
            "test-client", 
            "http://localhost:8080/callback"
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("test-auth-code");
        assertThat(result.getUserId()).isEqualTo(testUser.getId().toString());
        assertThat(result.getClientId()).isEqualTo(testClient.getId().toString());
    }
    
    @Test
    void testValidateAuthorizationCode_ExpiredCode_ReturnsNull() {
        // Given
        when(authorizationCodeRepository.findValidCodeForClient(
            any(), any(), any(), any(Instant.class)
        )).thenReturn(Optional.empty());
        
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
            
        // When
        OAuth2AuthorizationCode result = oauth2Service.validateAuthorizationCode(
            "expired-code", 
            "test-client", 
            "http://localhost:8080/callback"
        );
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void testGenerateAccessToken_ValidRequest_ReturnsTokenResponse() {
        // Given
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        String jwtToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...";
        when(jwtTokenProvider.generateToken(eq("test@example.com"), any(Map.class), eq(3600)))
            .thenReturn(jwtToken);
            
        when(accessTokenRepository.save(any(OAuthAccessToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(OAuthRefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
            
        // When
        OAuth2TokenResponse result = oauth2Service.generateAccessToken(
            testUser.getId().toString(),
            testClient.getId().toString(),
            "read write"
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(jwtToken);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getRefreshToken()).isNotNull();
        assertThat(result.getScope()).isEqualTo("read write");
        
        verify(accessTokenRepository).save(any(OAuthAccessToken.class));
        verify(refreshTokenRepository).save(any(OAuthRefreshToken.class));
    }
    
    @Test
    void testValidateAccessToken_ValidToken_ReturnsTokenInfo() {
        // Given
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash") // Will be matched by any() matcher
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(accessToken));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        // When
        OAuth2TokenInfo result = oauth2Service.validateAccessToken("bearer-token");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId().toString());
        assertThat(result.getClientId()).isEqualTo(testClient.getClientId());
        assertThat(result.isActive()).isTrue();
    }
    
    @Test
    void testValidateAccessToken_InvalidToken_ReturnsNull() {
        // Given
        when(accessTokenRepository.findValidTokenByHash(any(), any(Instant.class)))
            .thenReturn(Optional.empty());
            
        // When
        OAuth2TokenInfo result = oauth2Service.validateAccessToken("invalid-token");
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void testTokenIntrospection_ValidToken_ReturnsActiveResponse() {
        // Given
        OAuth2IntrospectionRequest request = new OAuth2IntrospectionRequest();
        request.setToken("valid-token");
        request.setAuthorizationHeader("Basic " + Base64.getEncoder().encodeToString("test-client:client-secret".getBytes()));
        
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        // Mock token validation
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash") // Will be matched by any() matcher
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(accessToken));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        // When
        OAuth2IntrospectionResponse result = oauth2Service.introspect(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getClientId()).isEqualTo("test-client");
        assertThat(result.getScope()).contains("read", "write");
    }
    
    @Test
    void testTokenIntrospection_InvalidClientCredentials_ReturnsInactiveResponse() {
        // Given
        OAuth2IntrospectionRequest request = new OAuth2IntrospectionRequest();
        request.setToken("some-token");
        request.setAuthorizationHeader("Basic " + Base64.getEncoder().encodeToString("test-client:wrong-secret".getBytes()));
        
        // Mock invalid client credentials
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrong-secret", "encoded-secret"))
            .thenReturn(false);
            
        // When
        OAuth2IntrospectionResponse result = oauth2Service.introspect(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        verifyNoInteractions(accessTokenRepository);
    }
    
    @Test
    void testTokenRevocation_ValidRequest_RevokesToken() {
        // Given
        OAuth2RevocationRequest request = new OAuth2RevocationRequest();
        request.setToken("token-to-revoke");
        request.setAuthorizationHeader("Basic " + Base64.getEncoder().encodeToString("test-client:client-secret".getBytes()));
        
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash") // Will be matched by any() matcher
            .clientId(testClient.getId())
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findByTokenHash(anyString()))
            .thenReturn(Optional.of(accessToken));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        // When & Then
        assertThatCode(() -> oauth2Service.revoke(request))
            .doesNotThrowAnyException();
            
        verify(accessTokenRepository).save(any(OAuthAccessToken.class));
    }
    
    @Test
    void testGetServerMetadata_ReturnsCorrectMetadata() {
        // Given
        when(httpRequest.getScheme()).thenReturn("https");
        when(httpRequest.getServerName()).thenReturn("identity.example.com");
        when(httpRequest.getServerPort()).thenReturn(443);
        when(httpRequest.getContextPath()).thenReturn("");
        
        // When
        OAuth2ServerMetadata result = oauth2Service.getServerMetadata(httpRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIssuer()).isEqualTo("https://identity.example.com");
        assertThat(result.getAuthorizationEndpoint()).isEqualTo("https://identity.example.com/oauth2/authorize");
        assertThat(result.getTokenEndpoint()).isEqualTo("https://identity.example.com/oauth2/token");
        assertThat(result.getIntrospectionEndpoint()).isEqualTo("https://identity.example.com/oauth2/introspect");
        assertThat(result.getRevocationEndpoint()).isEqualTo("https://identity.example.com/oauth2/revoke");
        assertThat(result.getJwksUri()).isEqualTo("https://identity.example.com/oauth2/jwks");
        assertThat(result.getUserinfoEndpoint()).isEqualTo("https://identity.example.com/userinfo");
        
        assertThat(result.getGrantTypesSupported()).containsExactlyInAnyOrder(
            "authorization_code", "refresh_token", "client_credentials", "urn:ietf:params:oauth:grant-type:device_code"
        );
        assertThat(result.getResponseTypesSupported()).containsExactlyInAnyOrder("code");
        assertThat(result.getScopesSupported()).containsExactlyInAnyOrder("openid", "profile", "email");
    }
    
    @Test
    void testGetServerMetadata_WithNonStandardPort_IncludesPort() {
        // Given
        when(httpRequest.getScheme()).thenReturn("http");
        when(httpRequest.getServerName()).thenReturn("localhost");
        when(httpRequest.getServerPort()).thenReturn(8081);
        when(httpRequest.getContextPath()).thenReturn("/identity");
        
        // When
        OAuth2ServerMetadata result = oauth2Service.getServerMetadata(httpRequest);
        
        // Then
        assertThat(result.getIssuer()).isEqualTo("http://localhost:8081/identity");
        assertThat(result.getAuthorizationEndpoint()).isEqualTo("http://localhost:8081/identity/oauth2/authorize");
    }
    
    @Test
    void testGetJwkSet_CallsTokenProvider() {
        // Given
        Map<String, Object> mockJwkSet = Map.of("keys", List.of());
        when(jwtTokenProvider.getJwkSet()).thenReturn(mockJwkSet);
        
        // When
        Map<String, Object> result = oauth2Service.getJwkSet();
        
        // Then
        assertThat(result).isEqualTo(mockJwkSet);
        verify(jwtTokenProvider).getJwkSet();
    }
    
    @Test
    void testRegisterClient_ValidRequest_CreatesClient() throws Exception {
        // Given
        OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
            .clientName("Test Application")
            .description("Test OAuth2 client")
            .redirectUris(Set.of("http://localhost:8080/callback"))
            .grantTypes(Set.of("authorization_code", "refresh_token"))
            .scopes(Set.of("read", "write"))
            .accessTokenValiditySeconds(3600)
            .refreshTokenValiditySeconds(86400)
            .autoApprove(false)
            .build();
            
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-secret");
        when(clientRepository.save(any(OAuthClient.class)))
            .thenAnswer(invocation -> {
                OAuthClient client = invocation.getArgument(0);
                ReflectionTestUtils.setField(client, "createdAt", Instant.now());
                return client;
            });
        
        // When
        OAuth2ClientResponse result = oauth2Service.registerClient(request, authentication);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClientName()).isEqualTo("Test Application");
        assertThat(result.getDescription()).isEqualTo("Test OAuth2 client");
        assertThat(result.getClientId()).startsWith("client_");
        assertThat(result.getClientSecret()).isNotNull();
        assertThat(result.getRedirectUris()).contains("http://localhost:8080/callback");
        assertThat(result.getGrantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token");
        assertThat(result.getScopes()).containsExactlyInAnyOrder("read", "write");
        
        verify(clientRepository).save(any(OAuthClient.class));
    }
    
    @Test
    void testRegisterClient_UserNotFound_ThrowsException() {
        // Given
        OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
            .clientName("Test App")
            .build();
            
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com"))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> oauth2Service.registerClient(request, authentication))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }
    
    @Test
    void testGetUserClients_ReturnsClientList() {
        // Given
        OAuthClient client1 = OAuthClient.builder()
            .id(UUID.randomUUID())
            .clientId("client1")
            .clientName("App 1")
            .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
            .build();
            
        OAuthClient client2 = OAuthClient.builder()
            .id(UUID.randomUUID())
            .clientId("client2")
            .clientName("App 2")
            .createdAt(Instant.now())
            .build();
            
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(List.of(client2, client1)); // Newest first
        
        // When
        OAuth2ClientListResponse result = oauth2Service.getUserClients(authentication);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getClients()).hasSize(2);
        assertThat(result.getClients().get(0).getClientName()).isEqualTo("App 2");
        assertThat(result.getClients().get(1).getClientName()).isEqualTo("App 1");
    }
    
    @Test
    void testUpdateClient_ValidRequest_UpdatesClient() {
        // Given
        String clientId = "test-client";
        OAuth2ClientUpdateRequest request = OAuth2ClientUpdateRequest.builder()
            .clientName("Updated App Name")
            .description("Updated description")
            .redirectUris(Set.of("http://localhost:9000/callback"))
            .scopes(Set.of("read", "write", "admin"))
            .build();
            
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findByClientId(clientId))
            .thenReturn(Optional.of(testClient));
            
        testClient.setUser(testUser); // Ensure ownership
        
        when(clientRepository.save(any(OAuthClient.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OAuth2ClientResponse result = oauth2Service.updateClient(clientId, request, authentication);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(testClient.getClientName()).isEqualTo("Updated App Name");
        assertThat(testClient.getDescription()).isEqualTo("Updated description");
        assertThat(testClient.getRedirectUris()).contains("http://localhost:9000/callback");
        assertThat(testClient.getAuthorizedScopes()).containsExactlyInAnyOrder("read", "write", "admin");
        
        verify(clientRepository).save(testClient);
    }
    
    @Test
    void testUpdateClient_NotOwner_ThrowsException() {
        // Given
        String clientId = "test-client";
        OAuth2ClientUpdateRequest request = OAuth2ClientUpdateRequest.builder()
            .clientName("Hacked App")
            .build();
            
        User otherUser = User.builder()
            .id(UUID.randomUUID())
            .email("other@example.com")
            .build();
            
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findByClientId(clientId))
            .thenReturn(Optional.of(testClient));
            
        testClient.setUser(otherUser); // Different owner
        
        // When & Then
        assertThatThrownBy(() -> oauth2Service.updateClient(clientId, request, authentication))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Client does not belong to this user");
    }
    
    @Test
    void testDeleteClient_ValidRequest_DeletesClientAndRevokesTokens() {
        // Given
        String clientId = "test-client";
        
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findByClientId(clientId))
            .thenReturn(Optional.of(testClient));
            
        testClient.setUser(testUser); // Ensure ownership
        
        doNothing().when(accessTokenRepository).revokeAllTokensForClient(
            eq(testClient.getId()), any(Instant.class), eq("Client deleted"));
        doNothing().when(refreshTokenRepository).revokeAllTokensForClient(
            eq(testClient.getId()), any(Instant.class), eq("Client deleted"));
        doNothing().when(clientRepository).delete(testClient);
        
        // When
        assertThatCode(() -> oauth2Service.deleteClient(clientId, authentication))
            .doesNotThrowAnyException();
        
        // Then
        verify(accessTokenRepository).revokeAllTokensForClient(
            eq(testClient.getId()), any(Instant.class), eq("Client deleted"));
        verify(refreshTokenRepository).revokeAllTokensForClient(
            eq(testClient.getId()), any(Instant.class), eq("Client deleted"));
        verify(clientRepository).delete(testClient);
    }
    
    @Test
    void testGetUserInfo_ValidToken_ReturnsUserInfo() {
        // Given
        String authHeader = "Bearer valid-access-token";
        
        OAuth2TokenInfo tokenInfo = OAuth2TokenInfo.builder()
            .active(true)
            .userId(testUser.getId().toString())
            .clientId("test-client")
            .scope("openid profile email")
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(OAuthAccessToken.builder()
                .userId(testUser.getId())
                .clientId(testClient.getId())
                .build()));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
        
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmailVerified(true);
        testUser.setPreferredLanguage("en-US");
        testUser.setTimezone("America/New_York");
        
        // When
        OAuth2UserInfoResponse result = oauth2Service.getUserInfo(authHeader);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSub()).isEqualTo(testUser.getId().toString());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getEmailVerified()).isTrue();
        assertThat(result.getName()).isEqualTo(testUser.getUsername());
        assertThat(result.getPreferredUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getGivenName()).isEqualTo("John");
        assertThat(result.getFamilyName()).isEqualTo("Doe");
        assertThat(result.getLocale()).isEqualTo("en-US");
        assertThat(result.getZoneinfo()).isEqualTo("America/New_York");
    }
    
    @Test
    void testGetUserInfo_InvalidAuthHeader_ThrowsException() {
        // Given
        String invalidHeader = "Invalid header";
        
        // When & Then
        assertThatThrownBy(() -> oauth2Service.getUserInfo(invalidHeader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid authorization header");
    }
    
    @Test
    void testGetUserInfo_ExpiredToken_ThrowsException() {
        // Given
        String authHeader = "Bearer expired-token";
        
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> oauth2Service.getUserInfo(authHeader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid or expired access token");
    }
    
    @Test
    void testTokenEndpoint_AuthorizationCodeGrant_ReturnsTokens() {
        // Given
        OAuth2TokenRequest request = OAuth2TokenRequest.builder()
            .grantType("authorization_code")
            .code("auth-code-123")
            .redirectUri("http://localhost:8080/callback")
            .clientId("test-client")
            .clientSecret("client-secret")
            .build();
            
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        // Mock authorization code validation
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("auth-code-123")
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
            
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
        when(authorizationCodeRepository.findValidCodeForClient(
            eq("auth-code-123"), eq(testClient.getId()), 
            eq("http://localhost:8080/callback"), any(Instant.class)))
            .thenReturn(Optional.of(authCode));
        when(authorizationCodeRepository.save(any(OAuthAuthorizationCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
            
        // Mock token generation
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
        when(jwtTokenProvider.generateToken(eq("test@example.com"), any(Map.class), eq(3600)))
            .thenReturn("jwt-access-token");
        when(accessTokenRepository.save(any(OAuthAccessToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(OAuthRefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OAuth2TokenResponse result = oauth2Service.token(request, httpRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getRefreshToken()).isNotNull();
        
        // Verify authorization code was marked as used
        verify(authorizationCodeRepository).save(argThat(code -> code.isUsed()));
        verify(entityManager).flush();
    }
    
    @Test
    void testTokenEndpoint_RefreshTokenGrant_ReturnsNewTokens() {
        // Given
        OAuth2TokenRequest request = OAuth2TokenRequest.builder()
            .grantType("refresh_token")
            .refreshToken("valid-refresh-token")
            .clientId("test-client")
            .clientSecret("client-secret")
            .build();
            
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        // Mock refresh token validation
        OAuthRefreshToken refreshToken = OAuthRefreshToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("hashed-refresh-token")
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();
            
        when(refreshTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(refreshToken));
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
        when(refreshTokenRepository.save(any(OAuthRefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
            
        // Mock new token generation
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
        when(jwtTokenProvider.generateToken(eq("test@example.com"), any(Map.class), eq(3600)))
            .thenReturn("new-jwt-access-token");
        when(accessTokenRepository.save(any(OAuthAccessToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OAuth2TokenResponse result = oauth2Service.token(request, httpRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-jwt-access-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getRefreshToken()).isNotNull();
        
        // Verify old refresh token was revoked
        verify(refreshTokenRepository).save(argThat(token -> 
            token.isRevoked() && "Token rotation".equals(token.getRevocationReason())));
    }
    
    @Test
    void testTokenEndpoint_ClientCredentialsGrant_ReturnsToken() {
        // Given
        OAuth2TokenRequest request = OAuth2TokenRequest.builder()
            .grantType("client_credentials")
            .scope("api")
            .clientId("test-client")
            .clientSecret("client-secret")
            .build();
            
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
        when(clientRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testClient));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        when(jwtTokenProvider.generateToken(eq("test-client"), any(Map.class), eq(3600)))
            .thenReturn("client-credentials-token");
        when(accessTokenRepository.save(any(OAuthAccessToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OAuth2TokenResponse result = oauth2Service.token(request, httpRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("client-credentials-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getRefreshToken()).isNull(); // No refresh token for client credentials
        assertThat(result.getScope()).isEqualTo("api");
    }
    
    @Test
    void testTokenEndpoint_UnsupportedGrantType_ThrowsException() {
        // Given
        OAuth2TokenRequest request = OAuth2TokenRequest.builder()
            .grantType("password")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> oauth2Service.token(request, httpRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported grant type: password");
    }
    
    @Test
    void testTokenRevocation_RefreshToken_RevokesToken() {
        // Given
        OAuth2RevocationRequest request = new OAuth2RevocationRequest();
        request.setToken("refresh-token-to-revoke");
        request.setAuthorizationHeader("Basic " + Base64.getEncoder().encodeToString("test-client:client-secret".getBytes()));
        
        // Mock client validation
        when(clientRepository.findByClientIdAndEnabledTrue("test-client"))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "encoded-secret"))
            .thenReturn(true);
            
        // No access token found
        when(accessTokenRepository.findByTokenHash(anyString()))
            .thenReturn(Optional.empty());
            
        // Refresh token found
        OAuthRefreshToken refreshToken = OAuthRefreshToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash")
            .clientId(testClient.getId())
            .revoked(false)
            .build();
            
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(Optional.of(refreshToken));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
        
        // When & Then
        assertThatCode(() -> oauth2Service.revoke(request))
            .doesNotThrowAnyException();
            
        verify(refreshTokenRepository).save(argThat(token -> 
            token.isRevoked() && "Explicit revocation".equals(token.getRevocationReason())));
    }
    
    @Test
    void testValidateAccessToken_ClientCredentialsToken_ReturnsTokenInfo() {
        // Given
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash")
            .userId(null) // No user for client credentials
            .clientId(testClient.getId())
            .scopes(Set.of("api"))
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(accessToken));
        when(clientRepository.findById(testClient.getId()))
            .thenReturn(Optional.of(testClient));
            
        // When
        OAuth2TokenInfo result = oauth2Service.validateAccessToken("client-token");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNull(); // No user for client credentials
        assertThat(result.getUsername()).isNull();
        assertThat(result.getClientId()).isEqualTo(testClient.getClientId());
        assertThat(result.getSub()).isEqualTo(testClient.getClientId()); // Client ID as subject
        assertThat(result.isActive()).isTrue();
    }
    
    @Test
    void testValidateAccessToken_ClientNotFound_ReturnsNull() {
        // Given
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash("any-hash")
            .userId(testUser.getId())
            .clientId(UUID.randomUUID()) // Non-existent client
            .scopes(Set.of("read"))
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(anyString(), any(Instant.class)))
            .thenReturn(Optional.of(accessToken));
        when(clientRepository.findById(any(UUID.class)))
            .thenReturn(Optional.empty());
            
        // When
        OAuth2TokenInfo result = oauth2Service.validateAccessToken("invalid-client-token");
        
        // Then
        assertThat(result).isNull();
    }
}