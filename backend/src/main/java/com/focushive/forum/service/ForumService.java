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
    ForumPostDTO createPost(String userId, ForumPostDTO post);
    ForumPostDTO updatePost(Long postId, String userId, ForumPostDTO post);
    void deletePost(Long postId, String userId);
    ForumPostDTO getPost(Long postId);
    ForumPostDTO getPostBySlug(Long categoryId, String slug);
    Page<ForumPostDTO> getPostsByCategory(Long categoryId, Pageable pageable);
    Page<ForumPostDTO> getPostsByUser(String userId, Pageable pageable);
    Page<ForumPostDTO> getPostsByHive(Long hiveId, Pageable pageable);
    Page<ForumPostDTO> searchPosts(String searchTerm, Pageable pageable);
    Page<ForumPostDTO> getPostsByTag(String tag, Pageable pageable);
    List<ForumPostDTO> getTrendingPosts(int days, int limit);
    void incrementViewCount(Long postId);
    void togglePinPost(Long postId, String userId);
    void toggleLockPost(Long postId, String userId);
    
    // Reply Management
    ForumReplyDTO createReply(Long postId, String userId, ForumReplyDTO reply);
    ForumReplyDTO updateReply(Long replyId, String userId, ForumReplyDTO reply);
    void deleteReply(Long replyId, String userId);
    ForumReplyDTO getReply(Long replyId);
    List<ForumReplyDTO> getPostReplies(Long postId);
    List<ForumReplyDTO> getTopLevelReplies(Long postId);
    List<ForumReplyDTO> getChildReplies(Long parentReplyId);
    Page<ForumReplyDTO> getUserReplies(String userId, Pageable pageable);
    void acceptReply(Long replyId, String userId);
    void unacceptReply(Long replyId, String userId);
    
    // Voting
    ForumVoteDTO voteOnPost(Long postId, String userId, Integer voteType);
    ForumVoteDTO voteOnReply(Long replyId, String userId, Integer voteType);
    void removeVote(Long postId, Long replyId, String userId);
    Integer getUserVoteOnPost(Long postId, String userId);
    Integer getUserVoteOnReply(Long replyId, String userId);
    
    // Subscriptions
    ForumSubscriptionDTO subscribeToPost(Long postId, String userId, ForumSubscriptionDTO subscription);
    ForumSubscriptionDTO subscribeToCategory(Long categoryId, String userId, ForumSubscriptionDTO subscription);
    void unsubscribeFromPost(Long postId, String userId);
    void unsubscribeFromCategory(Long categoryId, String userId);
    ForumSubscriptionDTO updateSubscription(Long subscriptionId, ForumSubscriptionDTO subscription);
    List<ForumSubscriptionDTO> getUserSubscriptions(String userId);
    void muteSubscription(Long subscriptionId, Integer hours);
    void unmuteSubscription(Long subscriptionId);
    
    // Statistics
    Long getPostCount(Long categoryId);
    Long getUserPostCount(String userId, int days);
    Long getUserReplyCount(String userId, int days);
    Long getUserAcceptedReplyCount(String userId);
    List<String> getPopularTags(int days, int limit);
    
    // Notifications
    void notifyNewPost(Long postId);
    void notifyNewReply(Long replyId);
    void notifyReplyAccepted(Long replyId);
    void notifyMention(String userId, Long postId, Long replyId);
}