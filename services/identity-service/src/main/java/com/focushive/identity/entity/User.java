package com.focushive.identity.entity;

import com.focushive.identity.security.encryption.IEncryptionService;
import com.focushive.identity.security.encryption.converters.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Comparator;

/**
 * User entity representing the core identity in the system.
 * A user can have multiple personas for different contexts.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email_hash", columnList = "email_hash"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_created_at", columnList = "created_at"),
    @Index(name = "idx_user_last_login", columnList = "last_login_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(exclude = {"password", "personas", "oauthClients"})
@NamedEntityGraph(
    name = "User.withPersonas",
    attributeNodes = {
        @NamedAttributeNode("personas")
    }
)
@NamedEntityGraph(
    name = "User.withPersonasAndAttributes", 
    attributeNodes = {
        @NamedAttributeNode(value = "personas", subgraph = "persona-details")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "persona-details",
            attributeNodes = {
                @NamedAttributeNode("customAttributes")
            }
        )
    }
)
public class User extends BaseEncryptedEntity implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(unique = true, nullable = false)
    @Convert(converter = com.focushive.identity.security.encryption.converters.SearchableEncryptedStringConverter.class)
    private String email;
    
    @Column(name = "email_hash")
    private String emailHash;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = false, length = 255)
    @Convert(converter = com.focushive.identity.security.encryption.converters.EncryptedStringConverter.class)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 255)
    @Convert(converter = com.focushive.identity.security.encryption.converters.EncryptedStringConverter.class)
    private String lastName;
    
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "phone_number", length = 20)
    @Convert(converter = com.focushive.identity.security.encryption.converters.EncryptedStringConverter.class)
    private String phoneNumber;

    @Column(name = "phone_number_verified", nullable = false)
    @Builder.Default
    private boolean phoneNumberVerified = false;
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_token_expiry")
    private Instant passwordResetTokenExpiry;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    @Column(name = "account_non_expired", nullable = false)
    @Builder.Default
    private boolean accountNonExpired = true;
    
    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;
    
    @Column(name = "credentials_non_expired", nullable = false)
    @Builder.Default
    private boolean credentialsNonExpired = true;

    // Account lockout fields for OWASP A04:2021 Insecure Design protection
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "account_locked_at")
    private Instant accountLockedAt;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private boolean twoFactorEnabled = false;
    
    @Column(name = "two_factor_secret")
    @Convert(converter = com.focushive.identity.security.encryption.converters.EncryptedStringConverter.class)
    private String twoFactorSecret;
    
    // User preferences
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";
    
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";
    
    @Column(name = "notification_preferences")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Builder.Default
    private Map<String, Boolean> notificationPreferences = new HashMap<>();
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @org.hibernate.annotations.BatchSize(size = 16)
    @Builder.Default
    private List<Persona> personas = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OAuthClient> oauthClients = new HashSet<>();
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    @Column(name = "last_login_ip", length = 255)
    @Convert(converter = com.focushive.identity.security.encryption.converters.EncryptedStringConverter.class)
    private String lastLoginIp;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Account deletion and recovery fields
    @Column(name = "account_deleted_at")
    private Instant accountDeletedAt;

    @Column(name = "deletion_token")
    private String deletionToken;

    // Token invalidation timestamp for session management
    @Column(name = "token_invalidated_at")
    private Instant tokenInvalidatedAt;

    // Role-based access control
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_additional_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<Role> additionalRoles = new HashSet<>();
    
    // UserDetails implementation
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // Add primary role
        authorities.add(new SimpleGrantedAuthority(role.getAuthority()));

        // Add additional roles
        additionalRoles.forEach(additionalRole ->
            authorities.add(new SimpleGrantedAuthority(additionalRole.getAuthority()))
        );

        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked && !isAccountCurrentlyLocked();
    }

    /**
     * Check if account is currently locked based on lockout timestamp.
     */
    public boolean isAccountCurrentlyLocked() {
        if (accountLockedUntil == null) {
            return false;
        }
        return Instant.now().isBefore(accountLockedUntil);
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && deletedAt == null;
    }
    
    /**
     * Get the active persona for this user.
     */
    @Transient
    public Optional<Persona> getActivePersona() {
        return personas.stream()
                .filter(Persona::isActive)
                .findFirst();
    }
    
    /**
     * Get the default persona for this user.
     */
    @Transient
    public Optional<Persona> getDefaultPersona() {
        return personas.stream()
                .filter(Persona::isDefault)
                .findFirst();
    }
    
    /**
     * Add a persona to this user.
     */
    public void addPersona(Persona persona) {
        if (persona != null) {
            personas.add(persona);
            persona.setUser(this);
        }
    }
    
    /**
     * Remove a persona from this user.
     */
    public void removePersona(Persona persona) {
        if (persona != null) {
            personas.remove(persona);
            persona.setUser(null);
        }
    }
    
    /**
     * Check if user has a specific role (primary or additional).
     */
    public boolean hasRole(Role roleToCheck) {
        return role.equals(roleToCheck) || additionalRoles.contains(roleToCheck);
    }
    
    /**
     * Check if user has a role with equal or higher privilege level.
     */
    public boolean hasRoleLevel(Role minimumRole) {
        return role.hasPrivilegeLevel(minimumRole) || 
               additionalRoles.stream().anyMatch(r -> r.hasPrivilegeLevel(minimumRole));
    }
    
    /**
     * Add an additional role to this user.
     */
    public void addRole(Role roleToAdd) {
        if (roleToAdd != null && !roleToAdd.equals(this.role)) {
            additionalRoles.add(roleToAdd);
        }
    }
    
    /**
     * Remove an additional role from this user.
     */
    public void removeRole(Role roleToRemove) {
        additionalRoles.remove(roleToRemove);
    }
    
    /**
     * Get all roles (primary and additional).
     */
    public Set<Role> getAllRoles() {
        Set<Role> allRoles = new HashSet<>(additionalRoles);
        allRoles.add(role);
        return allRoles;
    }
    
    /**
     * Get the highest privilege role.
     */
    public Role getHighestRole() {
        return getAllRoles().stream()
                .max(Comparator.comparing(Role::getHierarchyLevel))
                .orElse(role);
    }
    
    /**
     * Increment failed login attempts and lock account if threshold reached.
     * Following OWASP A04:2021 Insecure Design best practices.
     *
     * @param maxAttempts Maximum number of failed attempts before lockout
     * @param lockoutDurationMinutes Duration of lockout in minutes
     */
    public void recordFailedLoginAttempt(int maxAttempts, int lockoutDurationMinutes) {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = Instant.now();

        if (this.failedLoginAttempts >= maxAttempts) {
            lockAccount(lockoutDurationMinutes);
        }
    }

    /**
     * Lock the account for the specified duration.
     *
     * @param lockoutDurationMinutes Duration in minutes (0 for indefinite)
     */
    public void lockAccount(int lockoutDurationMinutes) {
        this.accountNonLocked = false;
        this.accountLockedAt = Instant.now();

        if (lockoutDurationMinutes > 0) {
            this.accountLockedUntil = Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES);
        } else {
            this.accountLockedUntil = null; // Indefinite lockout
        }
    }

    /**
     * Reset failed login attempts after successful login.
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;

        // Unlock account if it was locked due to failed attempts
        if (!this.accountNonLocked && this.accountLockedAt != null) {
            unlockAccount();
        }
    }

    /**
     * Unlock the account manually (admin action or automatic recovery).
     */
    public void unlockAccount() {
        this.accountNonLocked = true;
        this.accountLockedAt = null;
        this.accountLockedUntil = null;
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }

    /**
     * Check if account lockout has expired and should be automatically unlocked.
     */
    public boolean shouldAutoUnlock() {
        return !accountNonLocked &&
               accountLockedUntil != null &&
               Instant.now().isAfter(accountLockedUntil);
    }

    /**
     * Update searchable hashes for encrypted fields.
     * Called before persisting or updating the entity.
     */
    @Override
    protected void updateSearchableHashes(IEncryptionService encryptionService) {
        // Update email hash for searchable encrypted email field
        if (email != null) {
            this.emailHash = encryptionService.hash(email.toLowerCase());
        }

        // Note: Other PII fields (firstName, lastName, etc.) don't need hashes
        // as they are not searchable - they use regular encryption only
    }
}