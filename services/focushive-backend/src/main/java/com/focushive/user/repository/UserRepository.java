package com.focushive.user.repository;

import com.focushive.user.entity.User;
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
public interface UserRepository extends JpaRepository<User, String> {
    
    // Find by unique fields
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    
    // Find active users
    Optional<User> findByEmailAndEnabledTrue(String email);
    Optional<User> findByUsernameAndEnabledTrue(String username);
    
    // Check existence
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
    // Soft delete support
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActive(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndActive(@Param("id") String id);
    
    // Update last login
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    // Search users
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND u.deletedAt IS NULL")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    
    // Find users by role
    Page<User> findByRoleAndDeletedAtIsNull(User.UserRole role, Pageable pageable);
    
    // Batch operations
    @Query("SELECT u FROM User u WHERE u.id IN :ids AND u.deletedAt IS NULL")
    List<User> findAllByIdsActive(@Param("ids") List<String> ids);
    
    // Statistics
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countActiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since AND u.deletedAt IS NULL")
    long countNewUsersSince(@Param("since") LocalDateTime since);
}