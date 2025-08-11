package com.focushive.forum.repository;

import com.focushive.forum.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.category.id = :categoryId " +
           "AND fp.slug = :slug " +
           "AND fp.isDeleted = false")
    Optional<ForumPost> findByCategoryIdAndSlug(
        @Param("categoryId") Long categoryId,
        @Param("slug") String slug
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.category.id = :categoryId " +
           "AND fp.isDeleted = false " +
           "ORDER BY fp.isPinned DESC, fp.createdAt DESC")
    Page<ForumPost> findByCategoryId(
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.user.id = :userId " +
           "AND fp.isDeleted = false " +
           "ORDER BY fp.createdAt DESC")
    Page<ForumPost> findByUserId(
        @Param("userId") String userId,
        Pageable pageable
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.isDeleted = false " +
           "AND fp.createdAt >= :since " +
           "ORDER BY fp.voteScore DESC")
    List<ForumPost> findTopPostsSince(
        @Param("since") LocalDateTime since,
        Pageable pageable
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.isDeleted = false " +
           "AND (LOWER(fp.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(fp.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ForumPost> searchPosts(
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.isDeleted = false " +
           "AND :tag = ANY(fp.tags)")
    Page<ForumPost> findByTag(
        @Param("tag") String tag,
        Pageable pageable
    );
    
    @Query("SELECT fp FROM ForumPost fp " +
           "JOIN fp.category fc " +
           "WHERE fc.hive.id = :hiveId " +
           "AND fp.isDeleted = false " +
           "ORDER BY fp.createdAt DESC")
    Page<ForumPost> findByHiveId(
        @Param("hiveId") Long hiveId,
        Pageable pageable
    );
    
    @Modifying
    @Query("UPDATE ForumPost fp SET fp.viewCount = fp.viewCount + 1 WHERE fp.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
    
    @Query("SELECT fp FROM ForumPost fp " +
           "WHERE fp.isDeleted = false " +
           "AND fp.replyCount = 0 " +
           "AND fp.createdAt <= :before")
    List<ForumPost> findUnansweredPostsBefore(@Param("before") LocalDateTime before);
    
    @Query("SELECT fp.tags, COUNT(fp) FROM ForumPost fp " +
           "WHERE fp.isDeleted = false " +
           "AND fp.createdAt >= :since " +
           "GROUP BY fp.tags " +
           "ORDER BY COUNT(fp) DESC")
    List<Object[]> findPopularTags(
        @Param("since") LocalDateTime since,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(fp) FROM ForumPost fp " +
           "WHERE fp.user.id = :userId " +
           "AND fp.isDeleted = false " +
           "AND fp.createdAt >= :since")
    Long countUserPostsSince(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
}