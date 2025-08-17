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
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("test-auth-code")
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .redirectUri("http://localhost:8080/callback")
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
        OAuth2AuthorizationCode result = oauth2Service.validateAuthorizationCode(
            "test-auth-code", 
            "test-client", 
            "http://localhost:8080/callback"
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("test-auth-code");
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getClientId()).isEqualTo(testClient.getId());
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
        when(jwtTokenProvider.generateToken(any(), any(), any()))
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
        String tokenHash = "hashed-token";
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash(tokenHash)
            .userId(testUser.getId())
            .clientId(testClient.getId())
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findValidTokenByHash(eq(tokenHash), any(Instant.class)))
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
        request.setClientId("test-client");
        request.setClientSecret("client-secret");
        
        when(oauth2Service.validateClientCredentials("test-client", "client-secret"))
            .thenReturn(true);
            
        OAuth2TokenInfo tokenInfo = OAuth2TokenInfo.builder()
            .active(true)
            .clientId("test-client")
            .userId(testUser.getId().toString())
            .scope("read write")
            .exp(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();
            
        when(oauth2Service.validateAccessToken("valid-token"))
            .thenReturn(tokenInfo);
            
        // When
        OAuth2IntrospectionResponse result = oauth2Service.introspect(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getClientId()).isEqualTo("test-client");
        assertThat(result.getScope()).isEqualTo("read write");
    }
    
    @Test
    void testTokenIntrospection_InvalidClientCredentials_ReturnsInactiveResponse() {
        // Given
        OAuth2IntrospectionRequest request = new OAuth2IntrospectionRequest();
        request.setToken("some-token");
        request.setClientId("test-client");
        request.setClientSecret("wrong-secret");
        
        when(oauth2Service.validateClientCredentials("test-client", "wrong-secret"))
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
        request.setClientId("test-client");
        request.setClientSecret("client-secret");
        
        when(oauth2Service.validateClientCredentials("test-client", "client-secret"))
            .thenReturn(true);
            
        String tokenHash = "hashed-token";
        OAuthAccessToken accessToken = OAuthAccessToken.builder()
            .id(UUID.randomUUID())
            .tokenHash(tokenHash)
            .clientId(testClient.getId())
            .revoked(false)
            .build();
            
        when(accessTokenRepository.findByTokenHash(tokenHash))
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
}