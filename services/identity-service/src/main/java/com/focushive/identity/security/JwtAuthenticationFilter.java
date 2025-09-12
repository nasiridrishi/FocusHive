package com.focushive.identity.security;

import com.focushive.identity.entity.User;
import com.focushive.identity.service.CustomUserDetailsService;
import com.focushive.identity.service.TokenBlacklistService;
import com.focushive.identity.service.CookieJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT authentication filter that validates tokens and sets the authentication context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CookieJwtService cookieJwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // First try to get JWT from cookies, then from Authorization header
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && !tokenBlacklistService.isBlacklisted(jwt)) {
                String username = tokenProvider.extractUsername(jwt);
                UUID userId = tokenProvider.extractUserId(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // Authentication successful - detailed logging controlled by config
                    if (log.isDebugEnabled()) {
                        log.debug("User authentication successful via {}", 
                                cookieJwtService.getAccessTokenFromCookie(request) != null ? "cookie" : "header");
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * @deprecated Use CookieJwtService.extractJwtFromRequest() instead
     * This method is kept for backward compatibility
     */
    @Deprecated
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}