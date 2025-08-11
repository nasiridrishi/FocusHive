package com.focushive.forum.repository;

import com.focushive.forum.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.post.id = :postId " +
           "AND fr.isDeleted = false " +
           "ORDER BY fr.isAccepted DESC, fr.voteScore DESC, fr.createdAt ASC")
    List<ForumReply> findByPostId(@Param("postId") Long postId);
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.post.id = :postId " +
           "AND fr.parentReply IS NULL " +
           "AND fr.isDeleted = false " +
           "ORDER BY fr.isAccepted DESC, fr.voteScore DESC, fr.createdAt ASC")
    List<ForumReply> findTopLevelRepliesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.parentReply.id = :parentId " +
           "AND fr.isDeleted = false " +
           "ORDER BY fr.createdAt ASC")
    List<ForumReply> findChildReplies(@Param("parentId") Long parentId);
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.user.id = :userId " +
           "AND fr.isDeleted = false " +
           "ORDER BY fr.createdAt DESC")
    Page<ForumReply> findByUserId(
        @Param("userId") Long userId,
        Pageable pageable
    );
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.post.id = :postId " +
           "AND fr.isAccepted = true " +
           "AND fr.isDeleted = false")
    List<ForumReply> findAcceptedRepliesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(fr) FROM ForumReply fr " +
           "WHERE fr.post.id = :postId " +
           "AND fr.isDeleted = false")
    Long countRepliesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(fr) FROM ForumReply fr " +
           "WHERE fr.user.id = :userId " +
           "AND fr.isDeleted = false " +
           "AND fr.createdAt >= :since")
    Long countUserRepliesSince(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.isDeleted = false " +
           "AND fr.createdAt >= :since " +
           "ORDER BY fr.voteScore DESC")
    List<ForumReply> findTopRepliesSince(
        @Param("since") LocalDateTime since,
        Pageable pageable
    );
    
    @Query("SELECT fr FROM ForumReply fr " +
           "WHERE fr.isDeleted = false " +
           "AND LOWER(fr.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ForumReply> searchReplies(
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(fr) FROM ForumReply fr " +
           "WHERE fr.user.id = :userId " +
           "AND fr.isAccepted = true " +
           "AND fr.isDeleted = false")
    Long countAcceptedRepliesByUser(@Param("userId") Long userId);
}