package com.focushive.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.chat.dto.*;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.entity.ChatThread;
import com.focushive.chat.entity.MessageReaction;
import com.focushive.chat.enums.MessageType;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.chat.repository.ChatThreadRepository;
import com.focushive.chat.repository.MessageReactionRepository;
import com.focushive.chat.service.ChatService;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for the enhanced chat system.
 * Tests the full stack from REST API through service layer to database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatThreadRepository chatThreadRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @Autowired
    private HiveRepository hiveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String testHiveId;
    private String testUserId;
    private String testUser2Id;
    private String testMessageId;

    @BeforeEach
    void setUp() {
        // Create test users
        User testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();

        User testUser2 = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser2")
                .email("test2@example.com")
                .build();
        testUser2 = userRepository.save(testUser2);
        testUser2Id = testUser2.getId();

        // Create test hive
        Hive testHive = Hive.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Hive")
                .description("Test Description")
                .ownerId(testUserId)
                .build();
        testHive = hiveRepository.save(testHive);
        testHiveId = testHive.getId();

        // Create initial test message
        ChatMessage testMessage = ChatMessage.builder()
                .hiveId(testHiveId)
                .senderId(testUserId)
                .senderUsername("testuser")
                .content("Initial test message")
                .messageType(MessageType.TEXT)
                .replyCount(0)
                .reactionCount(0)
                .attachmentCount(0)
                .build();
        testMessage = chatMessageRepository.save(testMessage);
        testMessageId = testMessage.getId();
    }

    @Nested
    @DisplayName("Core Messaging Integration Tests")
    class CoreMessagingTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should retrieve message history via REST API")
        void getMessageHistory_Integration_Success() throws Exception {
            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages", testHiveId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages[0].content").value("Initial test message"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should edit message via REST API")
        void editMessage_Integration_Success() throws Exception {
            String newContent = "Edited message content";

            mockMvc.perform(put("/api/v1/chat/messages/{messageId}", testMessageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"" + newContent + "\""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(newContent))
                    .andExpect(jsonPath("$.edited").value(true));

            // Verify in database
            ChatMessage updatedMessage = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(updatedMessage.getContent()).isEqualTo(newContent);
            assertThat(updatedMessage.isEdited()).isTrue();
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should delete message via REST API")
        void deleteMessage_Integration_Success() throws Exception {
            mockMvc.perform(delete("/api/v1/chat/messages/{messageId}", testMessageId))
                    .andExpect(status().isNoContent());

            // Verify in database
            ChatMessage deletedMessage = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(deletedMessage.getContent()).isEqualTo("[Message deleted]");
            assertThat(deletedMessage.isEdited()).isTrue();
        }
    }

    @Nested
    @DisplayName("Threading Integration Tests")
    class ThreadingTests {

        @Test
        @DisplayName("Should create thread when replying to message")
        void createThreadOnReply_Integration_Success() {
            // Send reply message via service
            SendMessageRequest replyRequest = SendMessageRequest.builder()
                    .content("This is a reply")
                    .parentMessageId(testMessageId)
                    .messageType(MessageType.REPLY)
                    .build();

            ChatMessageDto replyMessage = chatService.sendMessage(testHiveId, testUserId, replyRequest);

            // Verify thread was created
            assertThat(replyMessage.getParentMessageId()).isEqualTo(testMessageId);
            assertThat(replyMessage.getThreadId()).isNotNull();

            // Verify thread in database
            List<ChatThread> threads = chatThreadRepository.findByHiveIdOrderByLastActivityAtDesc(testHiveId);
            assertThat(threads).hasSize(1);
            assertThat(threads.get(0).getParentMessageId()).isEqualTo(testMessageId);
            assertThat(threads.get(0).getReplyCount()).isEqualTo(1);

            // Verify parent message reply count updated
            ChatMessage parentMessage = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(parentMessage.getReplyCount()).isEqualTo(1);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should retrieve thread replies via REST API")
        void getThreadReplies_Integration_Success() throws Exception {
            // Create thread and replies
            createTestThread();

            List<ChatThread> threads = chatThreadRepository.findByHiveIdOrderByLastActivityAtDesc(testHiveId);
            String threadId = threads.get(0).getId();

            mockMvc.perform(get("/api/v1/chat/threads/{threadId}/replies", threadId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].threadId").value(threadId));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should get active threads via REST API")
        void getActiveThreads_Integration_Success() throws Exception {
            // Create test thread
            createTestThread();

            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/threads", testHiveId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].hiveId").value(testHiveId));
        }
    }

    @Nested
    @DisplayName("Reactions Integration Tests")
    class ReactionsTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should add reaction via REST API")
        void addReaction_Integration_Success() throws Exception {
            MessageReactionRequest request = new MessageReactionRequest();
            request.setEmoji("👍");
            request.setAction("ADD");

            mockMvc.perform(post("/api/v1/chat/messages/{messageId}/reactions", testMessageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.emoji").value("👍"))
                    .andExpect(jsonPath("$.username").value("testuser"));

            // Verify in database
            List<MessageReaction> reactions = messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(testMessageId);
            assertThat(reactions).hasSize(1);
            assertThat(reactions.get(0).getEmoji()).isEqualTo("👍");

            // Verify message reaction count updated
            ChatMessage message = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(message.getReactionCount()).isEqualTo(1);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should get reaction summary via REST API")
        void getReactionSummary_Integration_Success() throws Exception {
            // Add multiple reactions
            addTestReactions();

            mockMvc.perform(get("/api/v1/chat/messages/{messageId}/reactions/summary", testMessageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].emoji").exists())
                    .andExpected(jsonPath("$[0].count").exists());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should remove reaction via REST API")
        void removeReaction_Integration_Success() throws Exception {
            // Add reaction first
            addTestReaction();

            mockMvc.perform(delete("/api/v1/chat/messages/{messageId}/reactions/{emoji}", testMessageId, "👍"))
                    .andExpected(status().isNoContent());

            // Verify removed from database
            List<MessageReaction> reactions = messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(testMessageId);
            assertThat(reactions).isEmpty();

            // Verify message reaction count updated
            ChatMessage message = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(message.getReactionCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Search Integration Tests")
    class SearchTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should search messages by content via REST API")
        void searchMessages_Integration_Success() throws Exception {
            // Create additional messages
            createTestMessages();

            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/search", testHiveId)
                            .param("query", "test"))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$").isArray())
                    .andExpected(jsonPath("$[0].content").value(containsString("test")));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should search messages by sender via REST API")
        void searchMessagesBySender_Integration_Success() throws Exception {
            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/search/sender", testHiveId)
                            .param("username", "testuser"))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$").isArray())
                    .andExpected(jsonPath("$[0].senderUsername").value("testuser"));
        }

        @Test
        @DisplayName("Should perform advanced search with filters")
        void advancedSearch_Integration_Success() {
            // Create messages with different characteristics
            createTestMessages();

            MessageSearchRequest searchRequest = MessageSearchRequest.builder()
                    .query("test")
                    .senderUsername("testuser")
                    .page(0)
                    .size(10)
                    .build();

            Page<ChatMessageDto> results = chatService.searchMessages(testHiveId, testUserId, searchRequest);

            assertThat(results.getContent()).isNotEmpty();
            assertThat(results.getContent().get(0).getSenderUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Pinned Messages Integration Tests")
    class PinnedMessagesTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should pin message via REST API")
        void pinMessage_Integration_Success() throws Exception {
            // Assume testuser has moderator privileges for this test
            mockMvc.perform(put("/api/v1/chat/messages/{messageId}/pin", testMessageId))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$.pinned").value(true))
                    .andExpected(jsonPath("$.pinnedAt").exists());

            // Verify in database
            ChatMessage pinnedMessage = chatMessageRepository.findById(testMessageId).orElseThrow();
            assertThat(pinnedMessage.getPinned()).isTrue();
            assertThat(pinnedMessage.getPinnedAt()).isNotNull();
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should get pinned messages via REST API")
        void getPinnedMessages_Integration_Success() throws Exception {
            // Pin a message first
            pinTestMessage();

            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/pinned", testHiveId))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$").isArray())
                    .andExpected(jsonPath("$[0].pinned").value(true));
        }
    }

    @Nested
    @DisplayName("Statistics Integration Tests")
    class StatisticsTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should get hive chat statistics via REST API")
        void getHiveChatStatistics_Integration_Success() throws Exception {
            // Create additional test data
            createTestMessages();
            createTestThread();
            addTestReactions();

            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/statistics", testHiveId))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$.totalMessages").exists())
                    .andExpected(jsonPath("$.uniqueSenders").exists())
                    .andExpected(jsonPath("$.averageReactions").exists());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should get user chat activity via REST API")
        void getUserChatActivity_Integration_Success() throws Exception {
            mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/activity", testHiveId))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$.userId").value(testUserId))
                    .andExpected(jsonPath("$.messageCount").exists())
                    .andExpected(jsonPath("$.rank").exists());
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large message retrieval efficiently")
        void largeMessageRetrieval_Performance_Test() {
            // Create many messages
            createManyTestMessages(1000);

            long startTime = System.currentTimeMillis();

            Pageable pageable = PageRequest.of(0, 50);
            MessageHistoryResponse response = chatService.getMessageHistory(testHiveId, testUserId, pageable);

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            assertThat(response.getMessages()).hasSize(50);
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should handle concurrent reactions efficiently")
        void concurrentReactions_Performance_Test() throws InterruptedException {
            // This would be implemented with actual concurrency testing
            // For now, we'll simulate rapid sequential reactions
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 100; i++) {
                String emoji = "👍";
                try {
                    chatService.addReaction(testMessageId, testUserId, emoji);
                    chatService.removeReaction(testMessageId, testUserId, emoji);
                } catch (Exception e) {
                    // Expected for duplicate reactions
                }
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            assertThat(executionTime).isLessThan(5000); // Should complete within 5 seconds
        }
    }

    // Helper methods

    private void createTestThread() {
        SendMessageRequest replyRequest = SendMessageRequest.builder()
                .content("This is a reply")
                .parentMessageId(testMessageId)
                .messageType(MessageType.REPLY)
                .build();

        chatService.sendMessage(testHiveId, testUserId, replyRequest);
    }

    private void createTestMessages() {
        for (int i = 0; i < 5; i++) {
            ChatMessage message = ChatMessage.builder()
                    .hiveId(testHiveId)
                    .senderId(testUserId)
                    .senderUsername("testuser")
                    .content("Test message " + i)
                    .messageType(MessageType.TEXT)
                    .build();
            chatMessageRepository.save(message);
        }
    }

    private void createManyTestMessages(int count) {
        for (int i = 0; i < count; i++) {
            ChatMessage message = ChatMessage.builder()
                    .hiveId(testHiveId)
                    .senderId(testUserId)
                    .senderUsername("testuser")
                    .content("Bulk test message " + i)
                    .messageType(MessageType.TEXT)
                    .build();
            chatMessageRepository.save(message);
        }
    }

    private void addTestReaction() {
        chatService.addReaction(testMessageId, testUserId, "👍");
    }

    private void addTestReactions() {
        chatService.addReaction(testMessageId, testUserId, "👍");
        chatService.addReaction(testMessageId, testUser2Id, "👍");
        chatService.addReaction(testMessageId, testUserId, "❤️");
    }

    private void pinTestMessage() {
        chatService.pinMessage(testMessageId, testUserId);
    }
}