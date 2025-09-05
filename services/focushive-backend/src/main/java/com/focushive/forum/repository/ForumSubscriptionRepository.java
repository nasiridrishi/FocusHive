package com.focushive.forum.repository;

import com.focushive.forum.entity.ForumSubscription;
import com.focushive.forum.entity.ForumSubscription.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumSubscriptionRepository extends JpaRepository<ForumSubscription, String> {
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.user.id = :userId " +
           "AND fs.post.id = :postId")
    Optional<ForumSubscription> findByUserIdAndPostId(
        @Param("userId") String userId,
        @Param("postId") String postId
    );
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.user.id = :userId " +
           "AND fs.category.id = :categoryId")
    Optional<ForumSubscription> findByUserIdAndCategoryId(
        @Param("userId") String userId,
        @Param("categoryId") String categoryId
    );
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.post.id = :postId " +
           "AND fs.isMuted = false " +
           "AND fs.notificationType != 'NONE'")
    List<ForumSubscription> findActiveSubscriptionsByPostId(@Param("postId") String postId);
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.category.id = :categoryId " +
           "AND fs.isMuted = false " +
           "AND fs.notificationType != 'NONE'")
    List<ForumSubscription> findActiveSubscriptionsByCategoryId(@Param("categoryId") String categoryId);
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.user.id = :userId " +
           "ORDER BY fs.createdAt DESC")
    List<ForumSubscription> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.user.id = :userId " +
           "AND fs.isMuted = false")
    List<ForumSubscription> findActiveSubscriptionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(fs) FROM ForumSubscription fs " +
           "WHERE fs.post.id = :postId " +
           "AND fs.isMuted = false")
    Long countActiveSubscribersByPostId(@Param("postId") String postId);
    
    @Query("SELECT COUNT(fs) FROM ForumSubscription fs " +
           "WHERE fs.category.id = :categoryId " +
           "AND fs.isMuted = false")
    Long countActiveSubscribersByCategoryId(@Param("categoryId") String categoryId);
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "WHERE fs.user.id = :userId " +
           "AND fs.emailNotifications = true " +
           "AND fs.isMuted = false")
    List<ForumSubscription> findEmailEnabledSubscriptionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT fs FROM ForumSubscription fs " +
           "JOIN fs.post fp " +
           "JOIN fp.category fc " +
           "WHERE fc.hive.id = :hiveId " +
           "AND fs.user.id = :userId")
    List<ForumSubscription> findByUserIdAndHiveId(
        @Param("userId") String userId,
        @Param("hiveId") String hiveId
    );
}