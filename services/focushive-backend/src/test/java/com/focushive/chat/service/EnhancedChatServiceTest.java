package com.focushive.chat.service;

import com.focushive.chat.dto.*;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.entity.ChatThread;
import com.focushive.chat.entity.MessageAttachment;
import com.focushive.chat.entity.MessageReaction;
import com.focushive.chat.enums.MessageType;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.chat.repository.ChatThreadRepository;
import com.focushive.chat.repository.MessageAttachmentRepository;
import com.focushive.chat.repository.MessageReactionRepository;
import com.focushive.chat.service.impl.ChatServiceImpl;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.user.dto.UserDto;
import com.focushive.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for enhanced ChatService functionality.
 * Tests threading, reactions, attachments, search, and WebSocket features.
 */
@ExtendWith(MockitoExtension.class)
class EnhancedChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private MessageAttachmentRepository messageAttachmentRepository;

    @Mock
    private ChatThreadRepository chatThreadRepository;

    @Mock
    private HiveMemberRepository hiveMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private String hiveId;
    private String userId;
    private String messageId;
    private String parentMessageId;
    private String threadId;
    private ChatMessage testMessage;
    private ChatMessage parentMessage;
    private ChatThread testThread;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        hiveId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        messageId = UUID.randomUUID().toString();
        parentMessageId = UUID.randomUUID().toString();
        threadId = UUID.randomUUID().toString();

        testUser = UserDto.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        parentMessage = ChatMessage.builder()
                .id(parentMessageId)
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername("testuser")
                .content("Parent message")
                .messageType(MessageType.TEXT)
                .replyCount(0)
                .reactionCount(0)
                .attachmentCount(0)
                .build();
        parentMessage.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        testMessage = ChatMessage.builder()
                .id(messageId)
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername("testuser")
                .content("Test message")
                .messageType(MessageType.TEXT)
                .parentMessageId(parentMessageId)
                .threadId(threadId)
                .replyCount(0)
                .reactionCount(0)
                .attachmentCount(0)
                .build();
        testMessage.setCreatedAt(LocalDateTime.now());

        testThread = ChatThread.builder()
                .id(threadId)
                .hiveId(hiveId)
                .parentMessageId(parentMessageId)
                .title("Test Thread")
                .replyCount(1)
                .lastActivityAt(LocalDateTime.now())
                .lastReplyUserId(userId)
                .lastReplyUsername("testuser")
                .archived(false)
                .build();
    }

    @Nested
    @DisplayName("Threading Tests")
    class ThreadingTests {

        @Test
        @DisplayName("Should send reply message and update thread")
        void sendReplyMessage_Success() {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("This is a reply")
                    .parentMessageId(parentMessageId)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(userService.getUserById(userId)).thenReturn(testUser);
            when(chatMessageRepository.findById(parentMessageId)).thenReturn(Optional.of(parentMessage));
            when(chatThreadRepository.findByParentMessageId(parentMessageId)).thenReturn(Optional.of(testThread));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);
            when(chatThreadRepository.save(any(ChatThread.class))).thenReturn(testThread);

            // When
            ChatMessageDto result = chatService.sendMessage(hiveId, userId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getParentMessageId()).isEqualTo(parentMessageId);
            assertThat(result.getThreadId()).isEqualTo(threadId);
            assertThat(result.isReply()).isTrue();

            // Verify thread was updated
            verify(chatThreadRepository).save(any(ChatThread.class));
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/messages"),
                    any(ChatMessageDto.class)
            );
        }

        @Test
        @DisplayName("Should create new thread for first reply")
        void sendFirstReply_CreatesNewThread() {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("First reply")
                    .parentMessageId(parentMessageId)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(userService.getUserById(userId)).thenReturn(testUser);
            when(chatMessageRepository.findById(parentMessageId)).thenReturn(Optional.of(parentMessage));
            when(chatThreadRepository.findByParentMessageId(parentMessageId)).thenReturn(Optional.empty());
            when(chatThreadRepository.save(any(ChatThread.class))).thenReturn(testThread);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

            // When
            ChatMessageDto result = chatService.sendMessage(hiveId, userId, request);

            // Then
            assertThat(result).isNotNull();
            verify(chatThreadRepository).save(any(ChatThread.class));
        }

        @Test
        @DisplayName("Should get thread replies")
        void getThreadReplies_Success() {
            // Given
            List<ChatMessage> replies = Arrays.asList(testMessage);
            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(replies);

            // When
            List<ChatMessageDto> result = chatService.getThreadReplies(threadId, userId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getThreadId()).isEqualTo(threadId);
        }
    }

    @Nested
    @DisplayName("Reaction Tests")
    class ReactionTests {

        @Test
        @DisplayName("Should add reaction to message")
        void addReaction_Success() {
            // Given
            String emoji = "👍";
            MessageReaction reaction = MessageReaction.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .username("testuser")
                    .emoji(emoji)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(messageReactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                    .thenReturn(Optional.empty());
            when(messageReactionRepository.save(any(MessageReaction.class))).thenReturn(reaction);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

            // When
            MessageReactionResponse result = chatService.addReaction(messageId, userId, emoji);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmoji()).isEqualTo(emoji);
            assertThat(result.getUsername()).isEqualTo("testuser");

            verify(chatMessageRepository).save(testMessage);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/reactions"),
                    any(MessageReactionResponse.class)
            );
        }

        @Test
        @DisplayName("Should remove existing reaction")
        void removeReaction_Success() {
            // Given
            String emoji = "👍";
            MessageReaction existingReaction = MessageReaction.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .emoji(emoji)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(messageReactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                    .thenReturn(Optional.of(existingReaction));

            // When
            chatService.removeReaction(messageId, userId, emoji);

            // Then
            verify(messageReactionRepository).delete(existingReaction);
            verify(chatMessageRepository).save(testMessage);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/reactions/remove"),
                    any()
            );
        }

        @Test
        @DisplayName("Should get message reactions grouped by emoji")
        void getMessageReactions_Success() {
            // Given
            List<MessageReaction> reactions = Arrays.asList(
                    MessageReaction.builder().emoji("👍").userId("user1").username("user1").build(),
                    MessageReaction.builder().emoji("👍").userId("user2").username("user2").build(),
                    MessageReaction.builder().emoji("❤️").userId("user1").username("user1").build()
            );

            when(messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(reactions);

            // When
            List<ChatMessageDto.ReactionSummary> result = chatService.getMessageReactionSummary(messageId, userId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmoji()).isEqualTo("👍");
            assertThat(result.get(0).getCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Attachment Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should handle message with attachments")
        void sendMessageWithAttachments_Success() {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Message with attachment")
                    .build();

            MessageAttachment attachment = MessageAttachment.builder()
                    .messageId(messageId)
                    .originalFilename("test.pdf")
                    .storedFilename("stored_test.pdf")
                    .fileSize(1024L)
                    .fileType("pdf")
                    .mimeType("application/pdf")
                    .build();

            testMessage.setAttachmentCount(1);

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(userService.getUserById(userId)).thenReturn(testUser);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);
            when(messageAttachmentRepository.findByMessageIdOrderByCreatedAtAsc(messageId))
                    .thenReturn(Arrays.asList(attachment));

            // When
            ChatMessageDto result = chatService.sendMessage(hiveId, userId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAttachmentCount()).isEqualTo(1);
            assertThat(result.isHasAttachments()).isTrue();
        }

        @Test
        @DisplayName("Should get attachment download URL")
        void getAttachmentDownloadUrl_Success() {
            // Given
            String attachmentId = UUID.randomUUID().toString();
            MessageAttachment attachment = MessageAttachment.builder()
                    .id(attachmentId)
                    .messageId(messageId)
                    .originalFilename("test.pdf")
                    .storedFilename("stored_test.pdf")
                    .fileSize(1024L)
                    .downloadCount(5L)
                    .build();

            when(messageAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);

            // When
            String downloadUrl = chatService.getAttachmentDownloadUrl(attachmentId, userId);

            // Then
            assertThat(downloadUrl).isNotNull();
            verify(messageAttachmentRepository).save(attachment);
            assertThat(attachment.getDownloadCount()).isEqualTo(6L);
        }
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should search messages by content")
        void searchMessages_ByContent_Success() {
            // Given
            MessageSearchRequest searchRequest = MessageSearchRequest.builder()
                    .query("test")
                    .page(0)
                    .size(20)
                    .build();

            List<ChatMessage> searchResults = Arrays.asList(testMessage);
            Page<ChatMessage> searchPage = new PageImpl<>(searchResults, PageRequest.of(0, 20), 1);

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.searchMessagesByContent(eq(hiveId), eq("test"), any(Pageable.class)))
                    .thenReturn(searchPage);

            // When
            Page<ChatMessageDto> result = chatService.searchMessages(hiveId, userId, searchRequest);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should search messages by sender")
        void searchMessages_BySender_Success() {
            // Given
            MessageSearchRequest searchRequest = MessageSearchRequest.builder()
                    .senderUsername("testuser")
                    .build();

            List<ChatMessage> searchResults = Arrays.asList(testMessage);

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.searchMessagesBySender(hiveId, "testuser"))
                    .thenReturn(searchResults);

            // When
            List<ChatMessageDto> result = chatService.searchMessagesBySender(hiveId, userId, "testuser");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSenderUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Pinned Messages Tests")
    class PinnedMessagesTests {

        @Test
        @DisplayName("Should pin message")
        void pinMessage_Success() {
            // Given
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(hiveMemberRepository.isUserModeratorOrOwner(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

            // When
            ChatMessageDto result = chatService.pinMessage(messageId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(testMessage.getPinned()).isTrue();
            assertThat(testMessage.getPinnedAt()).isNotNull();
            assertThat(testMessage.getPinnedByUserId()).isEqualTo(userId);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/pins"),
                    any(ChatMessageDto.class)
            );
        }

        @Test
        @DisplayName("Should get pinned messages")
        void getPinnedMessages_Success() {
            // Given
            testMessage.pin(userId);
            List<ChatMessage> pinnedMessages = Arrays.asList(testMessage);

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(chatMessageRepository.findByHiveIdAndPinnedTrueOrderByPinnedAtDesc(hiveId))
                    .thenReturn(pinnedMessages);

            // When
            List<ChatMessageDto> result = chatService.getPinnedMessages(hiveId, userId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPinned()).isTrue();
        }
    }

    @Nested
    @DisplayName("WebSocket Features Tests")
    class WebSocketTests {

        @Test
        @DisplayName("Should broadcast typing indicator")
        void broadcastTypingIndicator_Success() {
            // Given
            TypingIndicatorMessage typingMessage = TypingIndicatorMessage.builder()
                    .hiveId(hiveId)
                    .userId(userId)
                    .username("testuser")
                    .typing(true)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);

            // When
            chatService.broadcastTypingIndicator(hiveId, userId, "testuser", true);

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/typing"),
                    any(TypingIndicatorMessage.class)
            );
        }

        @Test
        @DisplayName("Should broadcast message update")
        void broadcastMessageUpdate_Success() {
            // Given
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

            // When
            chatService.broadcastMessageUpdate(messageId, "EDIT");

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/hive/" + hiveId + "/messages/edit"),
                    any(ChatMessageDto.class)
            );
        }
    }

    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {

        @Test
        @DisplayName("Should deny access to non-member")
        void sendMessage_NonMember_ThrowsForbidden() {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Test message")
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> chatService.sendMessage(hiveId, userId, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You must be a member of the hive to send messages");
        }

        @Test
        @DisplayName("Should deny pinning to non-moderator")
        void pinMessage_NonModerator_ThrowsForbidden() {
            // Given
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(hiveMemberRepository.isUserModeratorOrOwner(hiveId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> chatService.pinMessage(messageId, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only moderators can pin messages");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle message not found")
        void editMessage_NotFound_ThrowsException() {
            // Given
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.editMessage(messageId, userId, "New content"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Message not found");
        }

        @Test
        @DisplayName("Should handle parent message not found for threading")
        void sendReply_ParentNotFound_ThrowsException() {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Reply")
                    .parentMessageId(parentMessageId)
                    .build();

            when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
            when(userService.getUserById(userId)).thenReturn(testUser);
            when(chatMessageRepository.findById(parentMessageId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.sendMessage(hiveId, userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Parent message not found");
        }
    }
}