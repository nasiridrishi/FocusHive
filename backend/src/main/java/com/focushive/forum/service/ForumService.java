package com.focushive.forum.service;

import com.focushive.forum.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ForumService {
    
    // Category Management
    ForumCategoryDTO createCategory(ForumCategoryDTO category);
    ForumCategoryDTO updateCategory(Long categoryId, ForumCategoryDTO category);
    void deleteCategory(Long categoryId);
    ForumCategoryDTO getCategory(Long categoryId);
    ForumCategoryDTO getCategoryBySlug(String slug);
    List<ForumCategoryDTO> getAllCategories();
    List<ForumCategoryDTO> getGlobalCategories();
    List<ForumCategoryDTO> getHiveCategories(Long hiveId);
    List<ForumCategoryDTO> getRootCategories();
    List<ForumCategoryDTO> getChildCategories(Long parentId);
    
    // Post Management
    ForumPostDTO createPost(Long userId, ForumPostDTO post);
    ForumPostDTO updatePost(Long postId, Long userId, ForumPostDTO post);
    void deletePost(Long postId, Long userId);
    ForumPostDTO getPost(Long postId);
    ForumPostDTO getPostBySlug(Long categoryId, String slug);
    Page<ForumPostDTO> getPostsByCategory(Long categoryId, Pageable pageable);
    Page<ForumPostDTO> getPostsByUser(Long userId, Pageable pageable);
    Page<ForumPostDTO> getPostsByHive(Long hiveId, Pageable pageable);
    Page<ForumPostDTO> searchPosts(String searchTerm, Pageable pageable);
    Page<ForumPostDTO> getPostsByTag(String tag, Pageable pageable);
    List<ForumPostDTO> getTrendingPosts(int days, int limit);
    void incrementViewCount(Long postId);
    void togglePinPost(Long postId, Long userId);
    void toggleLockPost(Long postId, Long userId);
    
    // Reply Management
    ForumReplyDTO createReply(Long postId, Long userId, ForumReplyDTO reply);
    ForumReplyDTO updateReply(Long replyId, Long userId, ForumReplyDTO reply);
    void deleteReply(Long replyId, Long userId);
    ForumReplyDTO getReply(Long replyId);
    List<ForumReplyDTO> getPostReplies(Long postId);
    List<ForumReplyDTO> getTopLevelReplies(Long postId);
    List<ForumReplyDTO> getChildReplies(Long parentReplyId);
    Page<ForumReplyDTO> getUserReplies(Long userId, Pageable pageable);
    void acceptReply(Long replyId, Long userId);
    void unacceptReply(Long replyId, Long userId);
    
    // Voting
    ForumVoteDTO voteOnPost(Long postId, Long userId, Integer voteType);
    ForumVoteDTO voteOnReply(Long replyId, Long userId, Integer voteType);
    void removeVote(Long postId, Long replyId, Long userId);
    Integer getUserVoteOnPost(Long postId, Long userId);
    Integer getUserVoteOnReply(Long replyId, Long userId);
    
    // Subscriptions
    ForumSubscriptionDTO subscribeToPost(Long postId, Long userId, ForumSubscriptionDTO subscription);
    ForumSubscriptionDTO subscribeToCategory(Long categoryId, Long userId, ForumSubscriptionDTO subscription);
    void unsubscribeFromPost(Long postId, Long userId);
    void unsubscribeFromCategory(Long categoryId, Long userId);
    ForumSubscriptionDTO updateSubscription(Long subscriptionId, ForumSubscriptionDTO subscription);
    List<ForumSubscriptionDTO> getUserSubscriptions(Long userId);
    void muteSubscription(Long subscriptionId, Integer hours);
    void unmuteSubscription(Long subscriptionId);
    
    // Statistics
    Long getPostCount(Long categoryId);
    Long getUserPostCount(Long userId, int days);
    Long getUserReplyCount(Long userId, int days);
    Long getUserAcceptedReplyCount(Long userId);
    List<String> getPopularTags(int days, int limit);
    
    // Notifications
    void notifyNewPost(Long postId);
    void notifyNewReply(Long replyId);
    void notifyReplyAccepted(Long replyId);
    void notifyMention(Long userId, Long postId, Long replyId);
}