package com.focushive.identity.service;

import com.focushive.identity.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Service for managing JWT tokens in secure httpOnly cookies.
 * Provides methods to set, get, and clear JWT tokens securely.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieJwtService {
    
    private final JwtTokenProvider tokenProvider;
    
    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessTokenCookieName;
    
    @Value("${jwt.cookie.refresh-token-name:refresh_token}")
    private String refreshTokenCookieName;
    
    @Value("${jwt.cookie.domain:#{null}}")
    private String cookieDomain;
    
    @Value("${jwt.cookie.path:/}")
    private String cookiePath;
    
    @Value("${jwt.cookie.secure:true}")
    private boolean secureCookies;
    
    @Value("${jwt.cookie.same-site:Strict}")
    private String sameSite;
    
    /**
     * Set access token as httpOnly cookie
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        long expirySeconds = tokenProvider.getAccessTokenExpirationSeconds();
        Cookie cookie = createSecureCookie(accessTokenCookieName, token, (int) expirySeconds);
        response.addCookie(cookie);
        
        // Add SameSite attribute via header (not supported by Cookie API)
        String cookieHeader = buildCookieHeader(accessTokenCookieName, token, (int) expirySeconds, true);
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("Set access token cookie with expiry: {} seconds", expirySeconds);
    }
    
    /**
     * Set refresh token as httpOnly cookie with longer expiry
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        // Refresh tokens typically have 30 day expiry (configurable via tokenProvider)
        int expirySeconds = 30 * 24 * 60 * 60; // 30 days
        Cookie cookie = createSecureCookie(refreshTokenCookieName, token, expirySeconds);
        response.addCookie(cookie);
        
        // Add SameSite attribute via header
        String cookieHeader = buildCookieHeader(refreshTokenCookieName, token, expirySeconds, true);
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("Set refresh token cookie with expiry: {} seconds", expirySeconds);
    }
    
    /**
     * Get access token from cookie
     */
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, accessTokenCookieName);
    }
    
    /**
     * Get refresh token from cookie
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, refreshTokenCookieName);
    }
    
    /**
     * Clear access token cookie
     */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        clearCookie(response, accessTokenCookieName);
    }
    
    /**
     * Clear refresh token cookie
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        clearCookie(response, refreshTokenCookieName);
    }
    
    /**
     * Clear all JWT cookies
     */
    public void clearAllCookies(HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
        log.debug("Cleared all JWT cookies");
    }
    
    /**
     * Check if request has valid access token cookie
     */
    public boolean hasValidAccessTokenCookie(HttpServletRequest request) {
        String token = getAccessTokenFromCookie(request);
        return StringUtils.hasText(token) && tokenProvider.validateToken(token);
    }
    
    /**
     * Extract JWT from request - checks cookies first, then Authorization header
     */
    public String extractJwtFromRequest(HttpServletRequest request) {
        // First check cookies
        String tokenFromCookie = getAccessTokenFromCookie(request);
        if (StringUtils.hasText(tokenFromCookie)) {
            return tokenFromCookie;
        }
        
        // Fallback to Authorization header for backward compatibility
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * Create secure cookie with standard security attributes
     */
    private Cookie createSecureCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true); // Prevent XSS attacks
        cookie.setSecure(secureCookies); // Only send over HTTPS in production
        cookie.setPath(cookiePath); // Scope to application path
        cookie.setMaxAge(maxAgeSeconds);
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        return cookie;
    }
    
    /**
     * Build complete cookie header with SameSite attribute
     */
    private String buildCookieHeader(String name, String value, int maxAgeSeconds, boolean httpOnly) {
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(name).append("=").append(value);
        cookieHeader.append("; Max-Age=").append(maxAgeSeconds);
        cookieHeader.append("; Path=").append(cookiePath);
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookieHeader.append("; Domain=").append(cookieDomain);
        }
        
        if (httpOnly) {
            cookieHeader.append("; HttpOnly");
        }
        
        if (secureCookies) {
            cookieHeader.append("; Secure");
        }
        
        cookieHeader.append("; SameSite=").append(sameSite);
        
        return cookieHeader.toString();
    }
    
    /**
     * Get cookie value by name
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
    
    /**
     * Clear cookie by setting empty value and zero max age
     */
    private void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookies);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
        
        // Also add via header to ensure SameSite is set
        String cookieHeader = buildCookieHeader(cookieName, "", 0, true);
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("Cleared cookie: {}", cookieName);
    }
}