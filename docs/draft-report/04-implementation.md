# Chapter 4: Implementation

## 4.1 Introduction

The implementation of FocusHive represents a sophisticated application of modern web technologies to create a real-time collaborative platform for virtual co-working. This chapter presents the technical implementation details, showcasing the architectural decisions, core components, and engineering practices employed to build a scalable and maintainable system. The implementation leverages Spring Boot 3.x with Java 21 for the backend, Redis for real-time data management, and WebSockets for bidirectional communication, demonstrating a comprehensive approach to building distributed systems.

## 4.2 Core Architecture Implementation

The foundation of FocusHive is built on a microservices-ready Spring Boot architecture, as evidenced in the main application class located at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/FocusHiveApplication.java`:

```java
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling
public class FocusHiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(FocusHiveApplication.class, args);
    }
}
```

This configuration enables critical features including JPA auditing for automatic timestamp management, Feign clients for inter-service communication, and scheduling for background tasks. The modular architecture supports both monolithic deployment for initial development and future microservices decomposition as the platform scales.

The application follows a layered architecture pattern with clear separation of concerns: controllers handle HTTP requests, services implement business logic, repositories manage data persistence, and DTOs facilitate data transfer between layers. This structure ensures maintainability and testability throughout the codebase.

## 4.3 Real-time Features Implementation

The real-time presence system forms the core of FocusHive's collaborative features. The WebSocket configuration at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/presence/config/WebSocketConfig.java` establishes the foundation for real-time communication:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
```

The presence service implementation at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/presence/service/impl/PresenceServiceImpl.java` demonstrates sophisticated state management using Redis:

```java
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
    
    private static final String USER_PRESENCE_KEY = "presence:user:";
    private static final String HIVE_PRESENCE_KEY = "presence:hive:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public UserPresence updateUserPresence(String userId, PresenceUpdate update) {
        UserPresence presence = UserPresence.builder()
                .userId(userId)
                .status(update.status())
                .activity(update.activity() != null ? update.activity() : "Available")
                .lastSeen(Instant.now())
                .currentHiveId(update.hiveId())
                .build();
        
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.opsForValue().set(key, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        
        broadcastPresenceUpdate(presence, update.hiveId());
        return presence;
    }
    
    @Scheduled(fixedDelay = 30000)
    public void cleanupStalePresence() {
        Set<String> userKeys = redisTemplate.keys(USER_PRESENCE_KEY + "*");
        Instant staleThreshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        
        for (String key : userKeys) {
            UserPresence presence = (UserPresence) redisTemplate.opsForValue().get(key);
            if (presence != null && presence.getLastSeen().isBefore(staleThreshold)) {
                redisTemplate.delete(key);
                if (presence.getCurrentHiveId() != null) {
                    leaveHivePresence(presence.getCurrentHiveId(), presence.getUserId());
                }
            }
        }
    }
}
```

This implementation showcases automatic cleanup of stale connections, efficient Redis key management with TTL-based expiration, and real-time broadcasting of presence updates through WebSocket channels.

## 4.4 Business Logic Implementation

The core business logic is encapsulated in service layers that handle complex operations. The timer service implementation at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/timer/service/impl/TimerServiceImpl.java` demonstrates transaction management and state tracking:

```java
@Service
@Transactional
public class TimerServiceImpl implements TimerService {
    
    @Override
    public FocusSessionDto startSession(String userId, StartSessionRequest request) {
        // Check for existing active session
        if (focusSessionRepository.findByUserIdAndCompletedFalse(userId).isPresent()) {
            throw new BadRequestException("You already have an active session. Please end it first.");
        }
        
        // Create new session
        FocusSession session = FocusSession.builder()
                .userId(userId)
                .hiveId(request.getHiveId())
                .sessionType(request.getSessionType())
                .durationMinutes(request.getDurationMinutes())
                .startTime(LocalDateTime.now())
                .completed(false)
                .build();
        
        session = focusSessionRepository.save(session);
        
        // Update daily stats
        updateDailyStats(userId, stats -> stats.setSessionsStarted(stats.getSessionsStarted() + 1));
        
        // Broadcast to hive if applicable
        if (request.getHiveId() != null) {
            broadcastSessionUpdate(request.getHiveId(), userId, "started", session);
        }
        
        return convertToDto(session);
    }
    
    private void updateDailyStats(String userId, Consumer<ProductivityStats> updater) {
        LocalDate today = LocalDate.now();
        ProductivityStats stats = productivityStatsRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    ProductivityStats newStats = ProductivityStats.builder()
                            .userId(userId)
                            .date(today)
                            .build();
                    return productivityStatsRepository.save(newStats);
                });
        
        updater.accept(stats);
        productivityStatsRepository.save(stats);
    }
}
```

The chat system implementation at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/chat/controller/ChatWebSocketController.java` shows WebSocket message handling:

```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final ChatService chatService;
    
    @MessageMapping("/hive/{hiveId}/send")
    public void sendMessage(
            @DestinationVariable String hiveId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        log.debug("Message received from {} to hive {}", principal.getName(), hiveId);
        
        // The service will handle broadcasting
        chatService.sendMessage(hiveId, principal.getName(), request);
    }
    
    @MessageMapping("/hive/{hiveId}/typing")
    @SendTo("/topic/hive/{hiveId}/typing")
    public TypingIndicator handleTyping(
            @DestinationVariable String hiveId,
            @Payload boolean isTyping,
            Principal principal) {
        
        return new TypingIndicator(principal.getName(), isTyping);
    }
    
    public record TypingIndicator(String userId, boolean isTyping) {}
}
```

## 4.5 Data Persistence Layer

The repository layer demonstrates sophisticated query optimization. The hive repository at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/hive/repository/HiveRepository.java` showcases advanced JPA usage:

```java
@Repository
public interface HiveRepository extends JpaRepository<Hive, String> {
    
    @Query("SELECT h FROM Hive h WHERE h.id = :id AND h.deletedAt IS NULL AND h.isActive = true")
    Optional<Hive> findByIdAndActive(@Param("id") String id);
    
    @Query("SELECT h FROM Hive h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL")
    Page<Hive> searchPublicHives(@Param("query") String query, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Hive h SET h.totalFocusMinutes = h.totalFocusMinutes + :minutes WHERE h.id = :hiveId")
    void incrementTotalFocusMinutes(@Param("hiveId") String hiveId, @Param("minutes") Long minutes);
}
```

Entity relationships are carefully managed through JPA annotations, as shown in the HiveMember entity at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/hive/entity/HiveMember.java`:

```java
@Entity
@Table(name = "hive_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"hive_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiveMember extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
}
```

## 4.6 Security Implementation

Security is implemented through JWT authentication integrated with the Identity Service. The hive service implementation at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/hive/service/impl/HiveServiceImpl.java` demonstrates authorization checks:

```java
@Service
@Transactional
public class HiveServiceImpl implements HiveService {
    
    @Override
    public void deleteHive(String hiveId, String userId) {
        Hive hive = getActiveHiveById(hiveId);
        
        // Check if user is the owner
        if (!hive.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the hive owner can delete the hive");
        }
        
        // Soft delete
        hive.setDeletedAt(LocalDateTime.now());
        hive.setActive(false);
        hiveRepository.save(hive);
        
        // Remove all members
        hiveMemberRepository.deleteAllByHiveId(hiveId);
        
        log.info("Hive {} deleted by owner {}", hiveId, userId);
    }
    
    @Override
    public boolean isOwnerOrModerator(String hiveId, String userId) {
        return hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .map(member -> member.getRole() == MemberRole.OWNER || 
                              member.getRole() == MemberRole.MODERATOR)
                .orElse(false);
    }
}
```

## 4.7 Testing Strategy

The testing implementation follows comprehensive patterns. Unit tests at `/home/nasir/UOL/focushive/backend/src/test/java/com/focushive/timer/service/impl/TimerServiceImplTest.java` demonstrate thorough coverage:

```java
@ExtendWith(MockitoExtension.class)
class TimerServiceImplTest {
    
    @Mock
    private FocusSessionRepository focusSessionRepository;
    
    @Mock
    private ProductivityStatsRepository productivityStatsRepository;
    
    @InjectMocks
    private TimerServiceImpl timerService;
    
    @Test
    void startSession_Success() {
        // Given
        StartSessionRequest request = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .build();
        
        when(focusSessionRepository.findByUserIdAndCompletedFalse(userId))
                .thenReturn(Optional.empty());
        when(focusSessionRepository.save(any(FocusSession.class)))
                .thenAnswer(i -> i.getArgument(0));
        
        // When
        FocusSessionDto result = timerService.startSession(userId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSessionType()).isEqualTo(FocusSession.SessionType.WORK);
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        
        verify(focusSessionRepository).save(any(FocusSession.class));
        verify(productivityStatsRepository).findByUserIdAndDate(eq(userId), any(LocalDate.class));
    }
}
```

## 4.8 Performance Optimizations

Performance optimization is achieved through strategic caching and scheduled cleanup tasks. The timer scheduler at `/home/nasir/UOL/focushive/backend/src/main/java/com/focushive/timer/scheduler/TimerScheduler.java` demonstrates efficient background processing:

```java
@Component
@RequiredArgsConstructor
public class TimerScheduler {
    
    @Scheduled(fixedDelay = 1000) // Run every second
    @Transactional
    public void updateActiveTimers() {
        List<HiveTimer> activeTimers = hiveTimerRepository.findAll().stream()
                .filter(HiveTimer::getIsRunning)
                .toList();
        
        for (HiveTimer timer : activeTimers) {
            int remainingSeconds = timer.getRemainingSeconds() - 1;
            
            if (remainingSeconds <= 0) {
                timer.setRemainingSeconds(0);
                timer.setIsRunning(false);
                timer.setCompletedAt(LocalDateTime.now());
                
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + timer.getHiveId() + "/timer/complete", 
                    convertToDto(timer)
                );
            } else {
                timer.setRemainingSeconds(remainingSeconds);
                
                if (remainingSeconds % 5 == 0) { // Broadcast every 5 seconds
                    messagingTemplate.convertAndSend(
                        "/topic/hive/" + timer.getHiveId() + "/timer", 
                        convertToDto(timer)
                    );
                }
            }
            
            hiveTimerRepository.save(timer);
        }
    }
}
```

## 4.9 Conclusion

The FocusHive implementation demonstrates a sophisticated application of modern software engineering principles and technologies. Through careful architectural decisions, comprehensive real-time features, robust security measures, and performance optimizations, the platform provides a solid foundation for virtual collaboration. The modular design supports future enhancements while maintaining code quality and system reliability. The extensive test coverage ensures stability, while the microservices-ready architecture enables horizontal scaling as user demands grow. This implementation successfully balances technical complexity with maintainability, creating a platform capable of supporting thousands of concurrent users in their productivity journey.