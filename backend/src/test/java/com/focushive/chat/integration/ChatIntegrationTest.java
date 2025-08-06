package com.focushive.chat.integration;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.common.entity.BaseEntity;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import com.focushive.test.TestApplication;
import com.focushive.test.UnifiedTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UnifiedTestConfig.class)
@ActiveProfiles("test")
@Transactional
public class ChatIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HiveRepository hiveRepository;

    @Autowired
    private HiveMemberRepository hiveMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User testUser;
    private User otherUser;
    private Hive testHive;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Clean up
        chatMessageRepository.deleteAll();
        hiveMemberRepository.deleteAll();
        hiveRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashed");
        testUser.setDisplayName("Test User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("hashed");
        otherUser.setDisplayName("Other User");
        otherUser.setRole(User.UserRole.USER);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        // Create test hive
        testHive = new Hive();
        testHive.setName("Test Hive");
        testHive.setDescription("A hive for testing");
        testHive.setMaxMembers(10);
        testHive.setIsPublic(true);
        testHive.setIsActive(true);
        testHive = hiveRepository.save(testHive);

        // Add test user as member
        HiveMember membership = new HiveMember();
        membership.setHive(testHive);
        membership.setUser(testUser);
        membership.setRole(HiveMember.MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setTotalMinutes(0);
        hiveMemberRepository.save(membership);

        // Add other user as regular member
        HiveMember otherMembership = new HiveMember();
        otherMembership.setHive(testHive);
        otherMembership.setUser(otherUser);
        otherMembership.setRole(HiveMember.MemberRole.MEMBER);
        otherMembership.setJoinedAt(LocalDateTime.now());
        otherMembership.setTotalMinutes(0);
        hiveMemberRepository.save(otherMembership);

        // TODO: Set up authentication token
        // For now, we'll skip auth in these tests or use mock auth
    }

    @Test
    void sendAndRetrieveMessages_Success() {
        // Given - create some messages directly
        ChatMessage message1 = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(testUser.getId())
                .senderUsername(testUser.getUsername())
                .content("First message")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        chatMessageRepository.save(message1);

        ChatMessage message2 = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(otherUser.getId())
                .senderUsername(otherUser.getUsername())
                .content("Second message")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        chatMessageRepository.save(message2);

        // When - retrieve messages
        List<ChatMessage> messages = chatMessageRepository.findLastMessagesInHive(testHive.getId(), 10);

        // Then
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(ChatMessage::getContent)
                .containsExactlyInAnyOrder("First message", "Second message");
    }

    @Test
    void messageHistory_PaginationWorks() {
        // Given - create 25 messages
        for (int i = 0; i < 25; i++) {
            ChatMessage message = ChatMessage.builder()
                    .hiveId(testHive.getId())
                    .senderId(testUser.getId())
                    .senderUsername(testUser.getUsername())
                    .content("Message " + i)
                    .messageType(ChatMessage.MessageType.TEXT)
                    .build();
            chatMessageRepository.save(message);
        }

        // When - get first page
        List<ChatMessage> firstPage = chatMessageRepository.findLastMessagesInHive(testHive.getId(), 10);

        // Then
        assertThat(firstPage).hasSize(10);
        assertThat(chatMessageRepository.countByHiveId(testHive.getId())).isEqualTo(25);
    }

    @Test
    void editMessage_UpdatesContent() {
        // Given
        ChatMessage message = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(testUser.getId())
                .senderUsername(testUser.getUsername())
                .content("Original content")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        message = chatMessageRepository.save(message);

        // When
        message.setContent("Edited content");
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        // Then
        ChatMessage updated = chatMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("Edited content");
        assertThat(updated.isEdited()).isTrue();
        assertThat(updated.getEditedAt()).isNotNull();
    }

    @Test
    void deleteMessage_SoftDeletes() {
        // Given
        ChatMessage message = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(testUser.getId())
                .senderUsername(testUser.getUsername())
                .content("To be deleted")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        message = chatMessageRepository.save(message);

        // When - soft delete
        message.setContent("[Message deleted]");
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        // Then
        ChatMessage deleted = chatMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(deleted.getContent()).isEqualTo("[Message deleted]");
        assertThat(deleted.isEdited()).isTrue();
    }

    @Test
    void systemMessages_SavedCorrectly() {
        // Given
        ChatMessage systemMessage = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId("system")
                .senderUsername("System")
                .content("Welcome to the hive!")
                .messageType(ChatMessage.MessageType.SYSTEM)
                .build();

        // When
        systemMessage = chatMessageRepository.save(systemMessage);

        // Then
        assertThat(systemMessage.getMessageType()).isEqualTo(ChatMessage.MessageType.SYSTEM);
        assertThat(systemMessage.getSenderUsername()).isEqualTo("System");
    }

    @Test
    void messagesAfterTimestamp_FiltersCorrectly() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now();
        
        // Create message before cutoff
        ChatMessage oldMessage = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(testUser.getId())
                .senderUsername(testUser.getUsername())
                .content("Old message")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        oldMessage.setCreatedAt(cutoff.minusMinutes(5));
        chatMessageRepository.save(oldMessage);

        // Create message after cutoff
        ChatMessage newMessage = ChatMessage.builder()
                .hiveId(testHive.getId())
                .senderId(testUser.getId())
                .senderUsername(testUser.getUsername())
                .content("New message")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        newMessage.setCreatedAt(cutoff.plusMinutes(5));
        chatMessageRepository.save(newMessage);

        // When
        List<ChatMessage> messages = chatMessageRepository
                .findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(testHive.getId(), cutoff);

        // Then
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("New message");
    }
}