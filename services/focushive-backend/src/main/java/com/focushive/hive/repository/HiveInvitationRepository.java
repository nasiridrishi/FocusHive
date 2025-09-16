package com.focushive.hive.repository;

import com.focushive.hive.entity.HiveInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HiveInvitationRepository extends JpaRepository<HiveInvitation, String> {

    /**
     * Find invitation by invitation code
     */
    Optional<HiveInvitation> findByInvitationCode(String invitationCode);

    /**
     * Check if an active invitation exists for a user to a specific hive
     */
    boolean existsByHiveIdAndInvitedUserIdAndStatus(String hiveId, String invitedUserId, HiveInvitation.InvitationStatus status);

    /**
     * Find pending invitation for a user to a specific hive
     */
    Optional<HiveInvitation> findByHiveIdAndInvitedUserIdAndStatus(String hiveId, String invitedUserId, HiveInvitation.InvitationStatus status);

    /**
     * Find all invitations for a user
     */
    Page<HiveInvitation> findByInvitedUserIdOrderByCreatedAtDesc(String invitedUserId, Pageable pageable);

    /**
     * Find all invitations sent by a user
     */
    Page<HiveInvitation> findByInvitedByIdOrderByCreatedAtDesc(String invitedById, Pageable pageable);

    /**
     * Find all invitations for a hive
     */
    Page<HiveInvitation> findByHiveIdOrderByCreatedAtDesc(String hiveId, Pageable pageable);

    /**
     * Find expired invitations that need to be updated
     */
    @Query("SELECT i FROM HiveInvitation i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<HiveInvitation> findExpiredInvitations(@Param("now") LocalDateTime now);

    /**
     * Find expired invitations (convenience method without parameter)
     */
    @Query("SELECT i FROM HiveInvitation i WHERE i.status = 'PENDING' AND i.expiresAt < CURRENT_TIMESTAMP")
    List<HiveInvitation> findExpiredInvitations();

    /**
     * Count pending invitations for a hive
     */
    long countByHiveIdAndStatus(String hiveId, HiveInvitation.InvitationStatus status);

    /**
     * Count pending invitations for a user
     */
    long countByInvitedUserIdAndStatus(String invitedUserId, HiveInvitation.InvitationStatus status);

    /**
     * Delete all invitations for a hive (when hive is deleted)
     */
    void deleteByHiveId(String hiveId);

    /**
     * Find active (pending and not expired) invitations for a user
     */
    @Query("SELECT i FROM HiveInvitation i WHERE i.invitedUser.id = :invitedUserId AND i.status = 'PENDING' AND i.expiresAt > CURRENT_TIMESTAMP")
    List<HiveInvitation> findActiveInvitationsForUser(@Param("invitedUserId") String invitedUserId);

    /**
     * Find invitations by status and hive
     */
    List<HiveInvitation> findByHiveIdAndStatus(String hiveId, HiveInvitation.InvitationStatus status);
}