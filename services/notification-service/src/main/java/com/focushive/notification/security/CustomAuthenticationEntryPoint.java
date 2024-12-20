package com.focushive.notification.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point that properly distinguishes between
 * 401 (Unauthorized) for existing endpoints and 404 (Not Found) for non-existent endpoints.
 *
 * This solves the issue where Spring Security returns 401 for all requests,
 * even when the endpoint doesn't exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final EndpointExistenceChecker endpointExistenceChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        String authHeader = request.getHeader("Authorization");
        String serviceName = request.getHeader("X-Service-Name");
        String correlationId = request.getHeader("X-Correlation-ID");
        
        // Log comprehensive authentication failure information
        log.warn("Authentication failed for {} {} from {} | User-Agent: {} | Service: {} | Correlation-ID: {} | Auth-Type: {} | Exception: {}",
            method, path, remoteAddr, 
            userAgent != null ? (userAgent.length() > 50 ? userAgent.substring(0, 50) + "..." : userAgent) : "unknown",
            serviceName != null ? serviceName : "none",
            correlationId != null ? correlationId : "none",
            authHeader != null ? (authHeader.startsWith("Bearer ") ? "JWT" : "Other") : "None",
            authException != null ? authException.getMessage() : "No auth exception");
            
        // Log additional debug info if debug level is enabled
        if (log.isDebugEnabled()) {
            log.debug("Authentication failure details - Exception type: {}, Path parameters: {}, Query string: {}",
                authException != null ? authException.getClass().getSimpleName() : "null",
                path, request.getQueryString());
        }

        // Check if the endpoint exists
        boolean endpointExists = endpointExistenceChecker.doesEndpointExist(request);

        if (!endpointExists) {
            // Endpoint doesn't exist - return 404
            sendErrorResponse(response, HttpStatus.NOT_FOUND,
                "The requested endpoint does not exist",
                path, correlationId);
            log.info("Returned 404 for non-existent endpoint: {} {} (correlation-id: {})", method, path, correlationId);
        } else {
            // Endpoint exists but authentication failed - return 401
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                "Authentication required to access this resource",
                path, correlationId);
            log.info("Returned 401 for protected endpoint: {} {} from service '{}' (correlation-id: {})", 
                method, path, serviceName != null ? serviceName : "unknown", correlationId);
        }
    }

    /**
     * Sends a JSON error response with the specified status code and message.
     *
     * @param response The HTTP response
     * @param status The HTTP status to set
     * @param message The error message
     * @param path The request path
     * @param correlationId The correlation ID for request tracing
     * @throws IOException If writing to response fails
     */
    private void sendErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    String message,
                                    String path,
                                    String correlationId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        if (correlationId != null) {
            errorResponse.put("correlationId", correlationId);
        }

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}