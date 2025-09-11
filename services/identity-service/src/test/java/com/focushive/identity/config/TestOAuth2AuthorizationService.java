package com.focushive.identity.config;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;

/**
 * Simple OAuth2 authorization service for tests.
 * Uses Spring's built-in InMemoryOAuth2AuthorizationService.
 */
public class TestOAuth2AuthorizationService implements OAuth2AuthorizationService {
    
    private final InMemoryOAuth2AuthorizationService delegate = new InMemoryOAuth2AuthorizationService();

    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }
}