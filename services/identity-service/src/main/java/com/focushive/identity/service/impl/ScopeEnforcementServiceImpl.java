package com.focushive.identity.service.impl;

import com.focushive.identity.exception.InsufficientScopeException;
import com.focushive.identity.service.ScopeEnforcementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of scope enforcement with hierarchical and wildcard support.
 */
@Slf4j
@Service
public class ScopeEnforcementServiceImpl implements ScopeEnforcementService {

    // Define scope hierarchies: parent -> children
    private static final Map<String, Set<String>> SCOPE_HIERARCHY = new HashMap<>();
    
    // Define scope field mappings for response filtering
    private static final Map<String, Set<String>> SCOPE_FIELD_MAPPINGS = new HashMap<>();
    
    static {
        // API scope hierarchy
        SCOPE_HIERARCHY.put("api.admin", Set.of("api.write", "api.read"));
        SCOPE_HIERARCHY.put("api.write", Set.of("api.read"));
        
        // Hive scope hierarchy
        SCOPE_HIERARCHY.put("hive.admin", Set.of("hive.create", "hive.write", "hive.read", "hive.delete"));
        SCOPE_HIERARCHY.put("hive.write", Set.of("hive.read"));
        
        // User management hierarchy
        SCOPE_HIERARCHY.put("admin", Set.of("user.manage", "user.write", "user.read"));
        SCOPE_HIERARCHY.put("user.manage", Set.of("user.write", "user.read"));
        SCOPE_HIERARCHY.put("user.write", Set.of("user.read"));
        
        // Analytics hierarchy
        SCOPE_HIERARCHY.put("analytics.admin", Set.of("analytics.export", "analytics.read"));
        SCOPE_HIERARCHY.put("analytics.export", Set.of("analytics.read"));
        
        // Field mappings for response filtering
        SCOPE_FIELD_MAPPINGS.put("hive.read", Set.of("id", "name", "description"));
        SCOPE_FIELD_MAPPINGS.put("hive.details", Set.of("settings", "analytics"));
        SCOPE_FIELD_MAPPINGS.put("hive.members", Set.of("members"));
        
        // OpenID Connect standard scopes
        SCOPE_FIELD_MAPPINGS.put("openid", Set.of("sub"));
        SCOPE_FIELD_MAPPINGS.put("profile", Set.of("name", "given_name", "family_name", "middle_name", 
            "nickname", "preferred_username", "profile", "picture", "website", "gender", "birthdate", 
            "zoneinfo", "locale", "updated_at"));
        SCOPE_FIELD_MAPPINGS.put("email", Set.of("email", "email_verified"));
        SCOPE_FIELD_MAPPINGS.put("address", Set.of("address"));
        SCOPE_FIELD_MAPPINGS.put("phone", Set.of("phone_number", "phone_number_verified"));
    }

    @Override
    public boolean hasRequiredScope(Set<String> providedScopes, String requiredScope) {
        if (providedScopes == null || requiredScope == null) {
            return false;
        }
        
        // Direct match
        if (providedScopes.contains(requiredScope)) {
            return true;
        }
        
        // Check hierarchical scopes
        for (String providedScope : providedScopes) {
            if (isScopeIncludedInHierarchy(providedScope, requiredScope)) {
                return true;
            }
        }
        
        // Check wildcard matches
        for (String providedScope : providedScopes) {
            if (isWildcardMatch(providedScope, requiredScope)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean hasRequiredScopes(Set<String> providedScopes, Set<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true;
        }
        
        for (String requiredScope : requiredScopes) {
            if (!hasRequiredScope(providedScopes, requiredScope)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void enforceScope(Set<String> providedScopes, String requiredScope)
            throws InsufficientScopeException {
        if (!hasRequiredScope(providedScopes, requiredScope)) {
            throw new InsufficientScopeException(requiredScope, providedScopes);
        }
    }

    @Override
    public void enforceScopes(Set<String> providedScopes, Set<String> requiredScopes)
            throws InsufficientScopeException {
        Set<String> missingScopes = new HashSet<>();
        
        for (String requiredScope : requiredScopes) {
            if (!hasRequiredScope(providedScopes, requiredScope)) {
                missingScopes.add(requiredScope);
            }
        }
        
        if (!missingScopes.isEmpty()) {
            String message = String.format("Missing required scopes: %s", 
                String.join(", ", missingScopes));
            throw new InsufficientScopeException(message, 
                String.join(" ", missingScopes), providedScopes);
        }
    }

    @Override
    public boolean hasResourceScope(Set<String> providedScopes, String resourceType,
                                   String resourceId, String permission) {
        if (providedScopes == null) {
            return false;
        }
        
        // Check for specific resource scope
        String specificScope = String.format("%s.%s.%s", resourceType, resourceId, permission);
        if (providedScopes.contains(specificScope)) {
            return true;
        }
        
        // Check for wildcard resource scope
        String wildcardScope = String.format("%s.*.%s", resourceType, permission);
        if (providedScopes.contains(wildcardScope)) {
            return true;
        }
        
        // Check for general resource type permission
        String generalScope = String.format("%s.%s", resourceType, permission);
        return hasRequiredScope(providedScopes, generalScope);
    }

    @Override
    public Map<String, Object> filterResponse(Map<String, Object> response, Set<String> scopes) {
        if (response == null || scopes == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> filteredResponse = new HashMap<>();
        
        // Always include certain base fields
        Set<String> allowedFields = new HashSet<>();
        
        // Add fields based on scopes
        for (String scope : scopes) {
            Set<String> fields = SCOPE_FIELD_MAPPINGS.get(scope);
            if (fields != null) {
                allowedFields.addAll(fields);
            }
            
            // Check hierarchical scopes
            for (Map.Entry<String, Set<String>> entry : SCOPE_FIELD_MAPPINGS.entrySet()) {
                if (hasRequiredScope(scopes, entry.getKey())) {
                    allowedFields.addAll(entry.getValue());
                }
            }
        }
        
        // Special handling for base fields that are always included with certain scopes
        if (scopes.stream().anyMatch(s -> s.startsWith("hive."))) {
            allowedFields.addAll(Set.of("id", "name", "description"));
        }
        
        // Filter response
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if (allowedFields.contains(entry.getKey())) {
                filteredResponse.put(entry.getKey(), entry.getValue());
            }
        }
        
        return filteredResponse;
    }

    @Override
    public Set<String> resolveScopes(Set<String> scopes, Map<String, String> context) {
        if (scopes == null || context == null) {
            return scopes;
        }
        
        Set<String> resolvedScopes = new HashSet<>();
        
        for (String scope : scopes) {
            String resolvedScope = scope;
            
            // Replace placeholders
            for (Map.Entry<String, String> entry : context.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                resolvedScope = resolvedScope.replace(placeholder, entry.getValue());
            }
            
            resolvedScopes.add(resolvedScope);
        }
        
        return resolvedScopes;
    }

    @Override
    public String getParentScope(String scope) {
        // Find parent scope in hierarchy
        for (Map.Entry<String, Set<String>> entry : SCOPE_HIERARCHY.entrySet()) {
            if (entry.getValue().contains(scope)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean matchesWildcard(String scope, String pattern) {
        if (scope == null || pattern == null) {
            return false;
        }
        
        // Convert wildcard pattern to regex
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return Pattern.matches(regex, scope);
    }

    /**
     * Check if a parent scope includes the required child scope through hierarchy.
     */
    private boolean isScopeIncludedInHierarchy(String parentScope, String childScope) {
        Set<String> children = SCOPE_HIERARCHY.get(parentScope);
        
        if (children == null) {
            return false;
        }
        
        // Direct child
        if (children.contains(childScope)) {
            return true;
        }
        
        // Recursive check for grandchildren
        for (String child : children) {
            if (isScopeIncludedInHierarchy(child, childScope)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if a provided scope with wildcard matches the required scope.
     */
    private boolean isWildcardMatch(String providedScope, String requiredScope) {
        if (!providedScope.contains("*")) {
            return false;
        }
        
        return matchesWildcard(requiredScope, providedScope);
    }

    /**
     * Get all scopes implied by a given scope through hierarchy.
     */
    public Set<String> getImpliedScopes(String scope) {
        Set<String> implied = new HashSet<>();
        implied.add(scope);
        
        Set<String> children = SCOPE_HIERARCHY.get(scope);
        if (children != null) {
            for (String child : children) {
                implied.addAll(getImpliedScopes(child));
            }
        }
        
        return implied;
    }

    /**
     * Validate that a scope string is well-formed.
     */
    public boolean isValidScope(String scope) {
        if (scope == null || scope.isEmpty()) {
            return false;
        }
        
        // Check for valid characters and structure
        return Pattern.matches("^[a-zA-Z0-9._*-]+$", scope);
    }
}