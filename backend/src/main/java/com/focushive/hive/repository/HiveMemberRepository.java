package com.focushive.hive.repository;

import com.focushive.hive.entity.HiveMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface HiveMemberRepository extends JpaRepository<HiveMember, String> {
    
    // Find membership
    Optional<HiveMember> findByHiveIdAndUserId(String hiveId, String userId);
    
    // Find members by hive
    Page<HiveMember> findByHiveId(String hiveId, Pageable pageable);
    
    @Query("SELECT hm FROM HiveMember hm WHERE hm.hive.id = :hiveId ORDER BY hm.lastActiveAt DESC NULLS LAST")
    Page<HiveMember> findByHiveIdOrderByActivity(@Param("hiveId") String hiveId, Pageable pageable);
    
    // Find hives by user
    Page<HiveMember> findByUserId(String userId, Pageable pageable);
    
    @Query("SELECT hm FROM HiveMember hm WHERE hm.user.id = :userId AND hm.hive.isActive = true")
    List<HiveMember> findActiveHivesByUserId(@Param("userId") String userId);
    
    // Find by role
    @Query("SELECT hm FROM HiveMember hm WHERE hm.hive.id = :hiveId AND hm.role = :role")
    List<HiveMember> findByHiveIdAndRole(@Param("hiveId") String hiveId, @Param("role") HiveMember.MemberRole role);
    
    // Check membership
    boolean existsByHiveIdAndUserId(String hiveId, String userId);
    
    // Update activity
    @Modifying
    @Query("UPDATE HiveMember hm SET hm.lastActiveAt = :now WHERE hm.id = :memberId")
    void updateLastActiveAt(@Param("memberId") String memberId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE HiveMember hm SET hm.totalMinutes = hm.totalMinutes + :minutes WHERE hm.id = :memberId")
    void incrementTotalMinutes(@Param("memberId") String memberId, @Param("minutes") Integer minutes);
    
    // Active members
    @Query("SELECT hm FROM HiveMember hm WHERE hm.hive.id = :hiveId AND hm.lastActiveAt > :since")
    List<HiveMember> findActiveMembersSince(@Param("hiveId") String hiveId, @Param("since") LocalDateTime since);
    
    // Count members
    long countByHiveId(String hiveId);
    
    @Query("SELECT COUNT(hm) FROM HiveMember hm WHERE hm.hive.id = :hiveId AND hm.role = :role")
    long countByHiveIdAndRole(@Param("hiveId") String hiveId, @Param("role") HiveMember.MemberRole role);
    
    // Leaderboard
    @Query("SELECT hm FROM HiveMember hm WHERE hm.hive.id = :hiveId ORDER BY hm.totalMinutes DESC")
    Page<HiveMember> findHiveLeaderboard(@Param("hiveId") String hiveId, Pageable pageable);
    
    // Batch operations
    @Modifying
    @Query("DELETE FROM HiveMember hm WHERE hm.hive.id = :hiveId")
    void deleteAllByHiveId(@Param("hiveId") String hiveId);
    
    // Statistics
    @Query("SELECT SUM(hm.totalMinutes) FROM HiveMember hm WHERE hm.hive.id = :hiveId")
    Long getTotalMinutesByHiveId(@Param("hiveId") String hiveId);
    
    // Check if user is moderator or owner
    @Query("SELECT CASE WHEN COUNT(hm) > 0 THEN true ELSE false END FROM HiveMember hm " +
           "WHERE hm.hive.id = :hiveId AND hm.user.id = :userId " +
           "AND hm.role IN ('OWNER', 'MODERATOR')")
    boolean isUserModeratorOrOwner(@Param("hiveId") String hiveId, @Param("userId") String userId);
}