package com.focushive.identity.repository;

import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Optional<User> findByEmailVerificationToken(String token);
    
    Optional<User> findByPasswordResetToken(String token);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.id = :id")
    Optional<User> findActiveById(@Param("id") UUID id);
    
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (u.username = :username OR u.email = :email)")
    Optional<User> findActiveByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime, @Param("ipAddress") String ipAddress);
    
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = null WHERE u.id = :userId")
    void verifyEmail(@Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :userId")
    void softDelete(@Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);
    
    // Performance optimized methods using EntityGraph to prevent N+1 queries
    
    @EntityGraph("User.withPersonas")
    @Override
    Optional<User> findById(UUID id);
    
    @EntityGraph("User.withPersonas")
    @Override
    List<User> findAll();
    
    @EntityGraph("User.withPersonas")
    @Query("SELECT u FROM User u")
    List<User> findAllWithPersonas();
    
    // JPQL with explicit JOIN FETCH for complex scenarios
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.personas p " +
           "LEFT JOIN FETCH p.customAttributes " +
           "WHERE u.id IN :userIds")
    List<User> findUsersWithPersonasAndAttributes(@Param("userIds") List<UUID> userIds);
}