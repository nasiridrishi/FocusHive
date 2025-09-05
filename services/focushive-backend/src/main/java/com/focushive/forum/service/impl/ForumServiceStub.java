package com.focushive.forum.service.impl;

import com.focushive.forum.dto.*;
import com.focushive.forum.service.ForumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.features.forum.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class ForumServiceStub implements ForumService {

    @Override
    public ForumCategoryDTO createCategory(ForumCategoryDTO dto) {
        log.warn("Forum service disabled - createCategory called");
        return null;
    }

    @Override
    public ForumCategoryDTO updateCategory(String categoryId, ForumCategoryDTO dto) {
        log.warn("Forum service disabled - updateCategory called");
        return null;
    }

    @Override
    public void deleteCategory(String categoryId) {
        log.warn("Forum service disabled - deleteCategory called");
    }

    @Override
    public ForumCategoryDTO getCategory(String categoryId) {
        log.warn("Forum service disabled - getCategory called");
        return null;
    }

    @Override
    public ForumCategoryDTO getCategoryBySlug(String slug) {
        log.warn("Forum service disabled - getCategoryBySlug called");
        return null;
    }

    @Override
    public List<ForumCategoryDTO> getAllCategories() {
        log.warn("Forum service disabled - getAllCategories called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumCategoryDTO> getGlobalCategories() {
        log.warn("Forum service disabled - getGlobalCategories called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumCategoryDTO> getHiveCategories(String hiveId) {
        log.warn("Forum service disabled - getHiveCategories called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumCategoryDTO> getRootCategories() {
        log.warn("Forum service disabled - getRootCategories called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumCategoryDTO> getChildCategories(String parentId) {
        log.warn("Forum service disabled - getChildCategories called");
        return Collections.emptyList();
    }

    @Override
    public ForumPostDTO createPost(String userId, ForumPostDTO dto) {
        log.warn("Forum service disabled - createPost called");
        return null;
    }

    @Override
    public ForumPostDTO updatePost(String postId, String userId, ForumPostDTO dto) {
        log.warn("Forum service disabled - updatePost called");
        return null;
    }

    @Override
    public void deletePost(String postId, String userId) {
        log.warn("Forum service disabled - deletePost called");
    }

    @Override
    public ForumPostDTO getPost(String postId) {
        log.warn("Forum service disabled - getPost called");
        return null;
    }

    @Override
    public ForumPostDTO getPostBySlug(String categoryId, String slug) {
        log.warn("Forum service disabled - getPostBySlug called");
        return null;
    }

    @Override
    public Page<ForumPostDTO> getPostsByCategory(String categoryId, Pageable pageable) {
        log.warn("Forum service disabled - getPostsByCategory called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<ForumPostDTO> getPostsByUser(String userId, Pageable pageable) {
        log.warn("Forum service disabled - getPostsByUser called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<ForumPostDTO> getPostsByHive(String hiveId, Pageable pageable) {
        log.warn("Forum service disabled - getPostsByHive called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<ForumPostDTO> searchPosts(String searchTerm, Pageable pageable) {
        log.warn("Forum service disabled - searchPosts called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<ForumPostDTO> getPostsByTag(String tag, Pageable pageable) {
        log.warn("Forum service disabled - getPostsByTag called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public List<ForumPostDTO> getTrendingPosts(int days, int limit) {
        log.warn("Forum service disabled - getTrendingPosts called");
        return Collections.emptyList();
    }

    @Override
    public void incrementViewCount(String postId) {
        log.warn("Forum service disabled - incrementViewCount called");
    }

    @Override
    public void togglePinPost(String postId, String userId) {
        log.warn("Forum service disabled - togglePinPost called");
    }

    @Override
    public void toggleLockPost(String postId, String userId) {
        log.warn("Forum service disabled - toggleLockPost called");
    }

    @Override
    public ForumReplyDTO createReply(String postId, String userId, ForumReplyDTO dto) {
        log.warn("Forum service disabled - createReply called");
        return null;
    }

    @Override
    public ForumReplyDTO updateReply(String replyId, String userId, ForumReplyDTO dto) {
        log.warn("Forum service disabled - updateReply called");
        return null;
    }

    @Override
    public void deleteReply(String replyId, String userId) {
        log.warn("Forum service disabled - deleteReply called");
    }

    @Override
    public ForumReplyDTO getReply(String replyId) {
        log.warn("Forum service disabled - getReply called");
        return null;
    }

    @Override
    public List<ForumReplyDTO> getPostReplies(String postId) {
        log.warn("Forum service disabled - getPostReplies called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumReplyDTO> getTopLevelReplies(String postId) {
        log.warn("Forum service disabled - getTopLevelReplies called");
        return Collections.emptyList();
    }

    @Override
    public List<ForumReplyDTO> getChildReplies(String parentReplyId) {
        log.warn("Forum service disabled - getChildReplies called");
        return Collections.emptyList();
    }

    @Override
    public Page<ForumReplyDTO> getUserReplies(String userId, Pageable pageable) {
        log.warn("Forum service disabled - getUserReplies called");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public void acceptReply(String replyId, String userId) {
        log.warn("Forum service disabled - acceptReply called");
    }

    @Override
    public void unacceptReply(String replyId, String userId) {
        log.warn("Forum service disabled - unacceptReply called");
    }

    @Override
    public ForumVoteDTO voteOnPost(String postId, String userId, Integer voteType) {
        log.warn("Forum service disabled - voteOnPost called");
        return null;
    }

    @Override
    public ForumVoteDTO voteOnReply(String replyId, String userId, Integer voteType) {
        log.warn("Forum service disabled - voteOnReply called");
        return null;
    }

    @Override
    public void removeVote(String postId, String replyId, String userId) {
        log.warn("Forum service disabled - removeVote called");
    }

    @Override
    public Integer getUserVoteOnPost(String postId, String userId) {
        log.warn("Forum service disabled - getUserVoteOnPost called");
        return 0;
    }

    @Override
    public Integer getUserVoteOnReply(String replyId, String userId) {
        log.warn("Forum service disabled - getUserVoteOnReply called");
        return 0;
    }

    @Override
    public ForumSubscriptionDTO subscribeToPost(String postId, String userId, ForumSubscriptionDTO dto) {
        log.warn("Forum service disabled - subscribeToPost called");
        return null;
    }

    @Override
    public ForumSubscriptionDTO subscribeToCategory(String categoryId, String userId, ForumSubscriptionDTO dto) {
        log.warn("Forum service disabled - subscribeToCategory called");
        return null;
    }

    @Override
    public void unsubscribeFromPost(String postId, String userId) {
        log.warn("Forum service disabled - unsubscribeFromPost called");
    }

    @Override
    public void unsubscribeFromCategory(String categoryId, String userId) {
        log.warn("Forum service disabled - unsubscribeFromCategory called");
    }

    @Override
    public ForumSubscriptionDTO updateSubscription(String subscriptionId, ForumSubscriptionDTO dto) {
        log.warn("Forum service disabled - updateSubscription called");
        return null;
    }

    @Override
    public List<ForumSubscriptionDTO> getUserSubscriptions(String userId) {
        log.warn("Forum service disabled - getUserSubscriptions called");
        return Collections.emptyList();
    }

    @Override
    public void muteSubscription(String subscriptionId, Integer hours) {
        log.warn("Forum service disabled - muteSubscription called");
    }

    @Override
    public void unmuteSubscription(String subscriptionId) {
        log.warn("Forum service disabled - unmuteSubscription called");
    }

    @Override
    public Long getPostCount(String categoryId) {
        log.warn("Forum service disabled - getPostCount called");
        return 0L;
    }

    @Override
    public Long getUserPostCount(String userId, int days) {
        log.warn("Forum service disabled - getUserPostCount called");
        return 0L;
    }

    @Override
    public Long getUserReplyCount(String userId, int days) {
        log.warn("Forum service disabled - getUserReplyCount called");
        return 0L;
    }

    @Override
    public Long getUserAcceptedReplyCount(String userId) {
        log.warn("Forum service disabled - getUserAcceptedReplyCount called");
        return 0L;
    }

    @Override
    public List<String> getPopularTags(int days, int limit) {
        log.warn("Forum service disabled - getPopularTags called");
        return Collections.emptyList();
    }

    @Override
    public void notifyNewPost(String postId) {
        log.warn("Forum service disabled - notifyNewPost called");
    }

    @Override
    public void notifyNewReply(String replyId) {
        log.warn("Forum service disabled - notifyNewReply called");
    }

    @Override
    public void notifyReplyAccepted(String replyId) {
        log.warn("Forum service disabled - notifyReplyAccepted called");
    }

    @Override
    public void notifyMention(String userId, String postId, String replyId) {
        log.warn("Forum service disabled - notifyMention called");
    }
}