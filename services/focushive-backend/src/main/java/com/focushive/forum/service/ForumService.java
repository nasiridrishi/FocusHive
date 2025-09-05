package com.focushive.forum.service;

import com.focushive.forum.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ForumService {
    
    // Category Management
    ForumCategoryDTO createCategory(ForumCategoryDTO category);
    ForumCategoryDTO updateCategory(String categoryId, ForumCategoryDTO category);
    void deleteCategory(String categoryId);
    ForumCategoryDTO getCategory(String categoryId);
    ForumCategoryDTO getCategoryBySlug(String slug);
    List<ForumCategoryDTO> getAllCategories();
    List<ForumCategoryDTO> getGlobalCategories();
    List<ForumCategoryDTO> getHiveCategories(String hiveId);
    List<ForumCategoryDTO> getRootCategories();
    List<ForumCategoryDTO> getChildCategories(String parentId);
    
    // Post Management
    ForumPostDTO createPost(String userId, ForumPostDTO post);
    ForumPostDTO updatePost(String postId, String userId, ForumPostDTO post);
    void deletePost(String postId, String userId);
    ForumPostDTO getPost(String postId);
    ForumPostDTO getPostBySlug(String categoryId, String slug);
    Page<ForumPostDTO> getPostsByCategory(String categoryId, Pageable pageable);
    Page<ForumPostDTO> getPostsByUser(String userId, Pageable pageable);
    Page<ForumPostDTO> getPostsByHive(String hiveId, Pageable pageable);
    Page<ForumPostDTO> searchPosts(String searchTerm, Pageable pageable);
    Page<ForumPostDTO> getPostsByTag(String tag, Pageable pageable);
    List<ForumPostDTO> getTrendingPosts(int days, int limit);
    void incrementViewCount(String postId);
    void togglePinPost(String postId, String userId);
    void toggleLockPost(String postId, String userId);
    
    // Reply Management
    ForumReplyDTO createReply(String postId, String userId, ForumReplyDTO reply);
    ForumReplyDTO updateReply(String replyId, String userId, ForumReplyDTO reply);
    void deleteReply(String replyId, String userId);
    ForumReplyDTO getReply(String replyId);
    List<ForumReplyDTO> getPostReplies(String postId);
    List<ForumReplyDTO> getTopLevelReplies(String postId);
    List<ForumReplyDTO> getChildReplies(String parentReplyId);
    Page<ForumReplyDTO> getUserReplies(String userId, Pageable pageable);
    void acceptReply(String replyId, String userId);
    void unacceptReply(String replyId, String userId);
    
    // Voting
    ForumVoteDTO voteOnPost(String postId, String userId, Integer voteType);
    ForumVoteDTO voteOnReply(String replyId, String userId, Integer voteType);
    void removeVote(String postId, String replyId, String userId);
    Integer getUserVoteOnPost(String postId, String userId);
    Integer getUserVoteOnReply(String replyId, String userId);
    
    // Subscriptions
    ForumSubscriptionDTO subscribeToPost(String postId, String userId, ForumSubscriptionDTO subscription);
    ForumSubscriptionDTO subscribeToCategory(String categoryId, String userId, ForumSubscriptionDTO subscription);
    void unsubscribeFromPost(String postId, String userId);
    void unsubscribeFromCategory(String categoryId, String userId);
    ForumSubscriptionDTO updateSubscription(String subscriptionId, ForumSubscriptionDTO subscription);
    List<ForumSubscriptionDTO> getUserSubscriptions(String userId);
    void muteSubscription(String subscriptionId, Integer hours);
    void unmuteSubscription(String subscriptionId);
    
    // Statistics
    Long getPostCount(String categoryId);
    Long getUserPostCount(String userId, int days);
    Long getUserReplyCount(String userId, int days);
    Long getUserAcceptedReplyCount(String userId);
    List<String> getPopularTags(int days, int limit);
    
    // Notifications
    void notifyNewPost(String postId);
    void notifyNewReply(String replyId);
    void notifyReplyAccepted(String replyId);
    void notifyMention(String userId, String postId, String replyId);
}