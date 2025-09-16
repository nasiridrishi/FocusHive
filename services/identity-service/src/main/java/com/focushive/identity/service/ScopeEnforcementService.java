package com.focushive.identity.service;

import com.focushive.identity.exception.InsufficientScopeException;

import java.util.Map;
import java.util.Set;

/**
 * Service for enforcing OAuth2 scope requirements and filtering responses.
 * Implements hierarchical and wildcard scope matching.
 */
public interface ScopeEnforcementService {

    /**
     * Check if the provided scopes satisfy the required scope.
     * Supports hierarchical scopes (e.g., api.admin includes api.write and api.read)
     * and wildcard scopes (e.g., hive.* matches hive.create, hive.read, etc.).
     *
     * @param providedScopes The scopes available in the token
     * @param requiredScope The scope required for the operation
     * @return true if the required scope is satisfied
     */
    boolean hasRequiredScope(Set<String> providedScopes, String requiredScope);

    /**
     * Check if the provided scopes satisfy all required scopes.
     *
     * @param providedScopes The scopes available in the token
     * @param requiredScopes The scopes required for the operation
     * @return true if all required scopes are satisfied
     */
    boolean hasRequiredScopes(Set<String> providedScopes, Set<String> requiredScopes);

    /**
     * Enforce that a token has the required scope, throwing exception if not.
     *
     * @param providedScopes The scopes available in the token
     * @param requiredScope The scope required for the operation
     * @throws InsufficientScopeException if scope is not satisfied
     */
    void enforceScope(Set<String> providedScopes, String requiredScope)
        throws InsufficientScopeException;

    /**
     * Enforce that a token has all required scopes.
     *
     * @param providedScopes The scopes available in the token
     * @param requiredScopes The scopes required for the operation
     * @throws InsufficientScopeException if any scope is not satisfied
     */
    void enforceScopes(Set<String> providedScopes, Set<String> requiredScopes)
        throws InsufficientScopeException;

    /**
     * Check if a token has access to a specific resource with a given permission.
     * Supports resource-specific scopes like "hive.abc123.admin".
     *
     * @param providedScopes The scopes available in the token
     * @param resourceType The type of resource (e.g., "hive")
     * @param resourceId The specific resource ID
     * @param permission The required permission (e.g., "admin")
     * @return true if access is granted
     */
    boolean hasResourceScope(Set<String> providedScopes, String resourceType,
                            String resourceId, String permission);

    /**
     * Filter a response object based on the scopes available.
     * Removes fields that require scopes not present in the token.
     *
     * @param response The full response object
     * @param scopes The scopes available in the token
     * @return Filtered response with only allowed fields
     */
    Map<String, Object> filterResponse(Map<String, Object> response, Set<String> scopes);

    /**
     * Resolve dynamic scopes with placeholders.
     * E.g., "hive.{id}.read" with {"id": "123"} becomes "hive.123.read".
     *
     * @param scopes The scopes with possible placeholders
     * @param context The context for resolving placeholders
     * @return Resolved scopes
     */
    Set<String> resolveScopes(Set<String> scopes, Map<String, String> context);

    /**
     * Get the hierarchical parent scope if it exists.
     * E.g., "api.write" has parent "api.admin".
     *
     * @param scope The scope to check
     * @return Parent scope or null if none
     */
    String getParentScope(String scope);

    /**
     * Check if a scope matches a wildcard pattern.
     * E.g., "hive.create" matches "hive.*".
     *
     * @param scope The scope to check
     * @param pattern The wildcard pattern
     * @return true if matches
     */
    boolean matchesWildcard(String scope, String pattern);
}