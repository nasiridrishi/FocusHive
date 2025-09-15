package com.focushive.websocket.config;

import com.focushive.api.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor that validates JWT tokens.
 * Extracts and validates JWT from query parameters or Authorization header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtValidator jwtValidator;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                  ServerHttpResponse response,
                                  WebSocketHandler wsHandler,
                                  Map<String, Object> attributes) throws Exception {

        log.debug("WebSocket handshake initiated from: {}", request.getRemoteAddress());

        // Extract JWT token from request
        String token = extractToken(request);

        if (token == null) {
            log.warn("No JWT token found in WebSocket handshake request");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Validate token
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        if (!result.isValid()) {
            log.warn("Invalid JWT token in WebSocket handshake: {}", result.getError());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Store user information in WebSocket session attributes
        attributes.put("userId", result.getUserId());
        attributes.put("email", result.getEmail());
        attributes.put("username", result.getSubject());
        attributes.put("authenticated", true);

        log.debug("WebSocket handshake authenticated for user: {}", result.getSubject());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * Extract JWT token from request.
     * Tries multiple sources:
     * 1. Authorization header (Bearer token)
     * 2. Query parameter 'token'
     * 3. Query parameter 'access_token'
     */
    private String extractToken(ServerHttpRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try query parameters (common for WebSocket connections)
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    if ("token".equals(key) || "access_token".equals(key)) {
                        return value;
                    }
                }
            }
        }

        return null;
    }
}