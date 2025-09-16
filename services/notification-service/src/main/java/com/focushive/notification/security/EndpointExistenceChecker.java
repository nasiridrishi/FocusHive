package com.focushive.notification.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility component to check if an endpoint exists in the application.
 * This is used to differentiate between 404 (Not Found) and 401 (Unauthorized) responses.
 *
 * Uses Spring's RequestMappingHandlerMapping to determine if a request matches any controller endpoint.
 * Implements caching for performance optimization in production.
 */
@Slf4j
@Component
public class EndpointExistenceChecker {

    private final List<RequestMappingHandlerMapping> handlerMappings;

    // Cache for endpoint existence checks (limited size to prevent memory issues)
    private final ConcurrentMap<String, Boolean> endpointCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    @Autowired
    public EndpointExistenceChecker(List<RequestMappingHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings;
        log.info("EndpointExistenceChecker initialized with {} handler mappings", handlerMappings.size());
    }

    /**
     * Checks if the given request matches any registered endpoint in the application.
     *
     * @param request The HTTP request to check
     * @return true if an endpoint exists for this request, false otherwise
     */
    public boolean doesEndpointExist(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        // Create cache key from method and path
        String cacheKey = request.getMethod() + ":" + request.getRequestURI();

        // Check cache first
        Boolean cached = endpointCache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for endpoint check: {} -> {}", cacheKey, cached);
            return cached;
        }

        // Check if endpoint exists using handler mappings
        boolean exists = checkEndpointInHandlerMappings(request);

        // Cache the result if cache size is within limits
        if (endpointCache.size() < MAX_CACHE_SIZE) {
            endpointCache.put(cacheKey, exists);
            log.debug("Cached endpoint check result: {} -> {}", cacheKey, exists);
        } else if (endpointCache.size() == MAX_CACHE_SIZE) {
            log.warn("Endpoint cache reached maximum size of {}, clearing cache", MAX_CACHE_SIZE);
            endpointCache.clear();
            endpointCache.put(cacheKey, exists);
        }

        return exists;
    }

    /**
     * Checks if the request matches any handler in the registered handler mappings.
     *
     * @param request The HTTP request to check
     * @return true if a handler exists for this request, false otherwise
     */
    private boolean checkEndpointInHandlerMappings(HttpServletRequest request) {
        for (RequestMappingHandlerMapping mapping : handlerMappings) {
            try {
                HandlerExecutionChain handler = mapping.getHandler(request);
                if (handler != null) {
                    Object handlerObject = handler.getHandler();

                    // Check if it's a controller method (not a resource handler)
                    if (handlerObject instanceof HandlerMethod) {
                        HandlerMethod handlerMethod = (HandlerMethod) handlerObject;
                        log.debug("Found handler for {}: {}.{}",
                            request.getRequestURI(),
                            handlerMethod.getBeanType().getSimpleName(),
                            handlerMethod.getMethod().getName());
                        return true;
                    }
                }
            } catch (Exception e) {
                // Log but don't fail - continue checking other mappings
                log.trace("Error checking handler mapping for {}: {}", request.getRequestURI(), e.getMessage());
            }
        }

        log.debug("No handler found for: {} {}", request.getMethod(), request.getRequestURI());
        return false;
    }

    /**
     * Clears the endpoint cache. Useful for testing or when mappings change.
     */
    public void clearCache() {
        endpointCache.clear();
        log.info("Endpoint cache cleared");
    }

    /**
     * Gets the current cache size for monitoring purposes.
     *
     * @return The number of cached endpoint checks
     */
    public int getCacheSize() {
        return endpointCache.size();
    }
}