package com.focushive.user.repository;

import com.focushive.user.entity.Notification;
import com.focushive.user.entity.Notification.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    // Find by user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find unread notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") String userId, Pageable pageable);
    
    // Find by type
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
        String userId, String type, Pageable pageable);
    
    // Find by priority
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.priority = :priority " +
           "AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByPriority(
        @Param("userId") String userId, 
        @Param("priority") NotificationPriority priority);
    
    // Count unread
    long countByUserIdAndIsReadFalse(String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId " +
           "AND n.isRead = false AND n.priority = :priority")
    long countUnreadByPriority(
        @Param("userId") String userId, 
        @Param("priority") NotificationPriority priority);
    
    // Mark as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsRead(
        @Param("notificationId") String notificationId, 
        @Param("userId") String userId,
        @Param("now") LocalDateTime now);
    
    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    // Mark as archived
    @Modifying
    @Query("UPDATE Notification n SET n.isArchived = true " +
           "WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsArchived(
        @Param("notificationId") String notificationId, 
        @Param("userId") String userId);
    
    // Delete old read notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId " +
           "AND n.isRead = true AND n.readAt < :before")
    int deleteOldReadNotifications(
        @Param("userId") String userId, 
        @Param("before") LocalDateTime before);
    
    // Find recent notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
        @Param("userId") String userId, 
        @Param("since") LocalDateTime since);
    
    // Batch operations
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.id IN :notificationIds AND n.user.id = :userId")
    int markMultipleAsRead(
        @Param("notificationIds") List<String> notificationIds,
        @Param("userId") String userId,
        @Param("now") LocalDateTime now);
    
    // Find by action URL pattern
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND n.actionUrl LIKE :urlPattern ORDER BY n.createdAt DESC")
    List<Notification> findByActionUrlPattern(
        @Param("userId") String userId,
        @Param("urlPattern") String urlPattern);
    
    // Clean up archived
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isArchived = true " +
           "AND n.createdAt < :before")
    int deleteArchivedNotifications(@Param("before") LocalDateTime before);
}