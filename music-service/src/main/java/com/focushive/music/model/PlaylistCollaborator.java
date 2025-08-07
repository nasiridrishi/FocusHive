package com.focushive.music.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a user's collaboration permissions on a playlist.
 * 
 * Defines what actions a user can perform on a collaborative playlist,
 * such as adding tracks, removing tracks, reordering, editing metadata, etc.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "playlist_collaborators", schema = "music", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_playlist_collaborators_playlist_user", 
                           columnNames = {"playlist_id", "user_id"})
       },
       indexes = {
           @Index(name = "idx_playlist_collaborators_playlist_id", columnList = "playlist_id"),
           @Index(name = "idx_playlist_collaborators_user_id", columnList = "user_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PlaylistCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The playlist this collaboration applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    /**
     * User ID of the collaborator.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * The collaboration permission level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.CONTRIBUTOR;

    /**
     * Whether the user can add new tracks to the playlist.
     */
    @Column(name = "can_add_tracks")
    @Builder.Default
    private Boolean canAddTracks = true;

    /**
     * Whether the user can remove tracks from the playlist.
     */
    @Column(name = "can_remove_tracks")
    @Builder.Default
    private Boolean canRemoveTracks = false;

    /**
     * Whether the user can reorder tracks in the playlist.
     */
    @Column(name = "can_reorder_tracks")
    @Builder.Default
    private Boolean canReorderTracks = false;

    /**
     * Whether the user can edit playlist metadata (name, description, image).
     */
    @Column(name = "can_edit_playlist")
    @Builder.Default
    private Boolean canEditPlaylist = false;

    /**
     * Whether the user can invite other collaborators.
     */
    @Column(name = "can_invite_others")
    @Builder.Default
    private Boolean canInviteOthers = false;

    /**
     * User ID of who added this collaborator to the playlist.
     */
    @Column(name = "added_by", nullable = false)
    private String addedBy;

    /**
     * Timestamp when the collaborator was added.
     */
    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /**
     * Enumeration of permission levels for playlist collaboration.
     */
    public enum PermissionLevel {
        /**
         * Full control over the playlist, including deleting it and managing collaborators.
         */
        OWNER,
        
        /**
         * Can perform most actions except deleting the playlist.
         */
        ADMIN,
        
        /**
         * Can add, remove, and reorder tracks but cannot edit playlist metadata.
         */
        CONTRIBUTOR,
        
        /**
         * Can only view the playlist and its contents.
         */
        VIEWER
    }

    /**
     * Checks if this collaborator has admin-level or owner permissions.
     * 
     * @return true if the user has admin or owner permissions
     */
    public boolean hasAdminPermissions() {
        return permissionLevel == PermissionLevel.OWNER || 
               permissionLevel == PermissionLevel.ADMIN;
    }

    /**
     * Checks if this collaborator can perform a specific action.
     * 
     * @param action The action to check
     * @return true if the user can perform the action
     */
    public boolean canPerform(CollaborationAction action) {
        return switch (action) {
            case ADD_TRACKS -> canAddTracks;
            case REMOVE_TRACKS -> canRemoveTracks;
            case REORDER_TRACKS -> canReorderTracks;
            case EDIT_PLAYLIST -> canEditPlaylist;
            case INVITE_OTHERS -> canInviteOthers;
            case DELETE_PLAYLIST -> permissionLevel == PermissionLevel.OWNER;
            case MANAGE_COLLABORATORS -> hasAdminPermissions();
        };
    }

    /**
     * Enumeration of possible collaboration actions.
     */
    public enum CollaborationAction {
        ADD_TRACKS,
        REMOVE_TRACKS,
        REORDER_TRACKS,
        EDIT_PLAYLIST,
        INVITE_OTHERS,
        DELETE_PLAYLIST,
        MANAGE_COLLABORATORS
    }

    /**
     * Sets default permissions based on the permission level.
     */
    public void setDefaultPermissions() {
        switch (permissionLevel) {
            case OWNER -> {
                canAddTracks = true;
                canRemoveTracks = true;
                canReorderTracks = true;
                canEditPlaylist = true;
                canInviteOthers = true;
            }
            case ADMIN -> {
                canAddTracks = true;
                canRemoveTracks = true;
                canReorderTracks = true;
                canEditPlaylist = true;
                canInviteOthers = true;
            }
            case CONTRIBUTOR -> {
                canAddTracks = true;
                canRemoveTracks = false;
                canReorderTracks = false;
                canEditPlaylist = false;
                canInviteOthers = false;
            }
            case VIEWER -> {
                canAddTracks = false;
                canRemoveTracks = false;
                canReorderTracks = false;
                canEditPlaylist = false;
                canInviteOthers = false;
            }
        }
    }
}