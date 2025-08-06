package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 client registration entity.
 * Represents applications that can authenticate users via this Identity Service.
 */
@Entity
@Table(name = "oauth_clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "authorizedScopes"})
@ToString(exclude = {"clientSecret", "user"})
public class OAuthClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    private String clientId;
    
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;
    
    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // The user who created/owns this client
    
    @ElementCollection
    @CollectionTable(name = "oauth_client_redirect_uris", 
                     joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri")
    @Builder.Default
    private Set<String> redirectUris = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "oauth_client_grant_types", 
                     joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type")
    @Builder.Default
    private Set<String> authorizedGrantTypes = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "oauth_client_scopes", 
                     joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    @Builder.Default
    private Set<String> authorizedScopes = new HashSet<>();
    
    @Column(name = "access_token_validity_seconds")
    @Builder.Default
    private Integer accessTokenValiditySeconds = 3600; // 1 hour
    
    @Column(name = "refresh_token_validity_seconds")
    @Builder.Default
    private Integer refreshTokenValiditySeconds = 2592000; // 30 days
    
    @Column(name = "auto_approve", nullable = false)
    @Builder.Default
    private boolean autoApprove = false;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}