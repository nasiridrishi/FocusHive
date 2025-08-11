package com.focushive.forum.repository;

import com.focushive.forum.entity.ForumVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ForumVoteRepository extends JpaRepository<ForumVote, Long> {
    
    @Query("SELECT fv FROM ForumVote fv " +
           "WHERE fv.user.id = :userId " +
           "AND fv.post.id = :postId")
    Optional<ForumVote> findByUserIdAndPostId(
        @Param("userId") String userId,
        @Param("postId") Long postId
    );
    
    @Query("SELECT fv FROM ForumVote fv " +
           "WHERE fv.user.id = :userId " +
           "AND fv.reply.id = :replyId")
    Optional<ForumVote> findByUserIdAndReplyId(
        @Param("userId") String userId,
        @Param("replyId") Long replyId
    );
    
    @Query("SELECT COUNT(fv) FROM ForumVote fv " +
           "WHERE fv.post.id = :postId " +
           "AND fv.voteType = :voteType")
    Long countVotesByPostIdAndType(
        @Param("postId") Long postId,
        @Param("voteType") Integer voteType
    );
    
    @Query("SELECT COUNT(fv) FROM ForumVote fv " +
           "WHERE fv.reply.id = :replyId " +
           "AND fv.voteType = :voteType")
    Long countVotesByReplyIdAndType(
        @Param("replyId") Long replyId,
        @Param("voteType") Integer voteType
    );
    
    @Query("SELECT SUM(fv.voteType) FROM ForumVote fv " +
           "WHERE fv.post.id = :postId")
    Integer calculatePostScore(@Param("postId") Long postId);
    
    @Query("SELECT SUM(fv.voteType) FROM ForumVote fv " +
           "WHERE fv.reply.id = :replyId")
    Integer calculateReplyScore(@Param("replyId") Long replyId);
    
    @Query("SELECT fv FROM ForumVote fv " +
           "WHERE fv.user.id = :userId " +
           "ORDER BY fv.createdAt DESC")
    List<ForumVote> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT fv.post.id, COUNT(fv) as voteCount " +
           "FROM ForumVote fv " +
           "WHERE fv.post IS NOT NULL " +
           "AND fv.voteType = 1 " +
           "GROUP BY fv.post.id " +
           "ORDER BY COUNT(fv) DESC")
    List<Object[]> findMostUpvotedPosts();
    
    @Query("SELECT fv.reply.id, COUNT(fv) as voteCount " +
           "FROM ForumVote fv " +
           "WHERE fv.reply IS NOT NULL " +
           "AND fv.voteType = 1 " +
           "GROUP BY fv.reply.id " +
           "ORDER BY COUNT(fv) DESC")
    List<Object[]> findMostUpvotedReplies();
}