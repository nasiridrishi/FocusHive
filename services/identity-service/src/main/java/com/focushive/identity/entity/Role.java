package com.focushive.identity.entity;

import lombok.Getter;

/**
 * Role enumeration defining the hierarchical role-based access control system.
 * Roles are ordered from lowest to highest privilege level.
 */
@Getter
public enum Role {
    USER("USER", "Basic authenticated user", 1),
    PREMIUM_USER("PREMIUM_USER", "Paid subscription user", 2),
    HIVE_OWNER("HIVE_OWNER", "Can create and manage hives", 3),
    MODERATOR("MODERATOR", "Can moderate content and users", 4),
    ADMIN("ADMIN", "System administrator", 5),
    SUPER_ADMIN("SUPER_ADMIN", "Full system access", 6);

    private final String name;
    private final String description;
    private final int hierarchyLevel;

    Role(String name, String description, int hierarchyLevel) {
        this.name = name;
        this.description = description;
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * Check if this role has equal or higher privilege than the target role.
     * 
     * @param targetRole The role to compare against
     * @return true if this role has equal or higher privilege
     */
    public boolean hasPrivilegeLevel(Role targetRole) {
        return this.hierarchyLevel >= targetRole.hierarchyLevel;
    }

    /**
     * Check if this role has higher privilege than the target role.
     * 
     * @param targetRole The role to compare against
     * @return true if this role has higher privilege
     */
    public boolean hasHigherPrivilegeThan(Role targetRole) {
        return this.hierarchyLevel > targetRole.hierarchyLevel;
    }

    /**
     * Get the Spring Security authority name.
     * 
     * @return The authority name prefixed with "ROLE_"
     */
    public String getAuthority() {
        return "ROLE_" + this.name;
    }

    /**
     * Check if this role can perform administrative actions.
     * 
     * @return true if this is an administrative role
     */
    public boolean isAdministrative() {
        return this.hierarchyLevel >= MODERATOR.hierarchyLevel;
    }

    /**
     * Check if this role can manage hives.
     * 
     * @return true if this role can create and manage hives
     */
    public boolean canManageHives() {
        return this.hierarchyLevel >= HIVE_OWNER.hierarchyLevel;
    }

    /**
     * Check if this role has premium features.
     * 
     * @return true if this role includes premium features
     */
    public boolean hasPremiumFeatures() {
        return this.hierarchyLevel >= PREMIUM_USER.hierarchyLevel;
    }
}