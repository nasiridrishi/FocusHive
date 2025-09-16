# Forum Features & Buddy System Design Document

## Executive Summary

This document outlines the technical design for two critical FocusHive features:
1. **Forum/Community Features (UOL-39)** - Discussion boards for community interaction
2. **Buddy System (UOL-38)** - One-on-one accountability partnerships

Both features integrate with existing FocusHive architecture and emphasize real-time collaboration.

---

## 1. Buddy System Design

### 1.1 Overview
The Buddy System creates accountability partnerships where users can pair up for mutual support, goal tracking, and motivation.

### 1.2 Core Features
- **Buddy Matching**: Algorithm-based or manual partner selection
- **Goal Sharing**: Share and track mutual goals
- **Check-ins**: Regular accountability check-ins
- **Progress Tracking**: Visual progress indicators
- **Communication**: Direct messaging between buddies
- **Buddy Sessions**: Synchronized focus sessions

### 1.3 Database Schema

```sql
-- Buddy relationships
CREATE TABLE buddy_relationships (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    initiated_by BIGINT REFERENCES users(id),
    matched_at TIMESTAMP,
    ended_at TIMESTAMP,
    match_score DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_buddy_pair UNIQUE(user1_id, user2_id),
    CONSTRAINT status_check CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED', 'BLOCKED'))
);

-- Buddy goals
CREATE TABLE buddy_goals (
    id BIGSERIAL PRIMARY KEY,
    relationship_id BIGINT REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    created_by BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    progress INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Buddy check-ins
CREATE TABLE buddy_checkins (
    id BIGSERIAL PRIMARY KEY,
    relationship_id BIGINT REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id),
    message TEXT,
    mood VARCHAR(20),
    productivity_score INTEGER CHECK (productivity_score >= 1 AND productivity_score <= 10),
    goals_progress JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Buddy preferences for matching
CREATE TABLE buddy_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    preferred_timezone VARCHAR(50),
    preferred_work_hours JSONB,
    focus_areas TEXT[],
    communication_style VARCHAR(50),
    matching_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_preferences UNIQUE(user_id)
);

-- Indexes for performance
CREATE INDEX idx_buddy_relationships_users ON buddy_relationships(user1_id, user2_id);
CREATE INDEX idx_buddy_relationships_status ON buddy_relationships(status);
CREATE INDEX idx_buddy_goals_relationship ON buddy_goals(relationship_id);
CREATE INDEX idx_buddy_checkins_relationship ON buddy_checkins(relationship_id);
CREATE INDEX idx_buddy_checkins_created ON buddy_checkins(created_at DESC);
```

### 1.4 REST API Endpoints

```yaml
Buddy Management:
  POST   /api/v1/buddies/request:
    description: Send buddy request to another user
    body: { targetUserId, message }
    
  GET    /api/v1/buddies/requests:
    description: Get pending buddy requests
    query: { type: sent|received }
    
  POST   /api/v1/buddies/requests/{requestId}/accept:
    description: Accept buddy request
    
  POST   /api/v1/buddies/requests/{requestId}/reject:
    description: Reject buddy request
    
  GET    /api/v1/buddies/current:
    description: Get current buddy relationship
    
  POST   /api/v1/buddies/end:
    description: End current buddy relationship
    
  GET    /api/v1/buddies/matches:
    description: Get suggested buddy matches
    query: { limit: 10 }

Goals & Check-ins:
  POST   /api/v1/buddies/goals:
    description: Create shared goal
    body: { title, description, targetDate }
    
  GET    /api/v1/buddies/goals:
    description: Get all buddy goals
    
  PUT    /api/v1/buddies/goals/{goalId}:
    description: Update goal progress
    body: { progress, notes }
    
  POST   /api/v1/buddies/checkins:
    description: Create check-in
    body: { message, mood, productivityScore, goalsProgress }
    
  GET    /api/v1/buddies/checkins:
    description: Get check-in history
    query: { limit: 20, offset: 0 }

Preferences:
  GET    /api/v1/buddies/preferences:
    description: Get user's buddy preferences
    
  PUT    /api/v1/buddies/preferences:
    description: Update buddy preferences
    body: { timezone, workHours, focusAreas, communicationStyle }
```

### 1.5 Matching Algorithm

```java
@Service
public class BuddyMatchingService {
    
    public List<BuddyMatch> findMatches(Long userId) {
        User user = userRepository.findById(userId);
        BuddyPreferences userPrefs = preferencesRepository.findByUserId(userId);
        
        List<User> candidates = userRepository.findMatchingCandidates(userId);
        
        return candidates.stream()
            .map(candidate -> calculateMatchScore(user, userPrefs, candidate))
            .filter(match -> match.getScore() > 0.5)
            .sorted(Comparator.comparing(BuddyMatch::getScore).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private BuddyMatch calculateMatchScore(User user, BuddyPreferences userPrefs, User candidate) {
        double score = 0.0;
        BuddyPreferences candidatePrefs = preferencesRepository.findByUserId(candidate.getId());
        
        // Timezone compatibility (30% weight)
        score += calculateTimezoneScore(userPrefs, candidatePrefs) * 0.3;
        
        // Focus area overlap (25% weight)
        score += calculateFocusAreaScore(userPrefs, candidatePrefs) * 0.25;
        
        // Work hours compatibility (20% weight)
        score += calculateWorkHoursScore(userPrefs, candidatePrefs) * 0.2;
        
        // Communication style (15% weight)
        score += calculateCommunicationScore(userPrefs, candidatePrefs) * 0.15;
        
        // Activity level similarity (10% weight)
        score += calculateActivityScore(user, candidate) * 0.1;
        
        return new BuddyMatch(candidate, score);
    }
}
```

---

## 2. Forum/Community Features Design

### 2.1 Overview
Community forums provide space for discussions, knowledge sharing, and peer support within hives or globally.

### 2.2 Core Features
- **Hierarchical Forums**: Global, Hive-specific, and Topic-based
- **Threaded Discussions**: Posts with nested replies
- **Rich Content**: Markdown support, code blocks, media
- **Voting System**: Upvote/downvote for quality content
- **Moderation**: Community moderation tools
- **Search**: Full-text search across forums
- **Notifications**: Real-time updates for replies and mentions

### 2.3 Database Schema

```sql
-- Forum categories
CREATE TABLE forum_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    slug VARCHAR(100) UNIQUE NOT NULL,
    parent_id BIGINT REFERENCES forum_categories(id),
    hive_id BIGINT REFERENCES hives(id),
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Forum posts
CREATE TABLE forum_posts (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES forum_categories(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_html TEXT,
    slug VARCHAR(255),
    tags TEXT[],
    view_count INTEGER DEFAULT 0,
    reply_count INTEGER DEFAULT 0,
    vote_score INTEGER DEFAULT 0,
    is_pinned BOOLEAN DEFAULT false,
    is_locked BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    edited_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_post_slug UNIQUE(category_id, slug)
);

-- Forum replies
CREATE TABLE forum_replies (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT REFERENCES forum_posts(id) ON DELETE CASCADE,
    parent_reply_id BIGINT REFERENCES forum_replies(id),
    user_id BIGINT REFERENCES users(id),
    content TEXT NOT NULL,
    content_html TEXT,
    vote_score INTEGER DEFAULT 0,
    is_accepted BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    edited_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Voting system
CREATE TABLE forum_votes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES forum_posts(id) ON DELETE CASCADE,
    reply_id BIGINT REFERENCES forum_replies(id) ON DELETE CASCADE,
    vote_type SMALLINT NOT NULL CHECK (vote_type IN (-1, 1)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_post_vote UNIQUE(user_id, post_id),
    CONSTRAINT unique_user_reply_vote UNIQUE(user_id, reply_id),
    CONSTRAINT vote_target CHECK (
        (post_id IS NOT NULL AND reply_id IS NULL) OR 
        (post_id IS NULL AND reply_id IS NOT NULL)
    )
);

-- Forum subscriptions
CREATE TABLE forum_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES forum_categories(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES forum_posts(id) ON DELETE CASCADE,
    notification_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_target CHECK (
        (category_id IS NOT NULL AND post_id IS NULL) OR 
        (category_id IS NULL AND post_id IS NOT NULL)
    )
);

-- Forum moderation
CREATE TABLE forum_moderation_logs (
    id BIGSERIAL PRIMARY KEY,
    moderator_id BIGINT REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_forum_posts_category ON forum_posts(category_id);
CREATE INDEX idx_forum_posts_user ON forum_posts(user_id);
CREATE INDEX idx_forum_posts_created ON forum_posts(created_at DESC);
CREATE INDEX idx_forum_posts_votes ON forum_posts(vote_score DESC);
CREATE INDEX idx_forum_replies_post ON forum_replies(post_id);
CREATE INDEX idx_forum_replies_user ON forum_replies(user_id);
CREATE INDEX idx_forum_votes_user ON forum_votes(user_id);
CREATE INDEX idx_forum_subscriptions_user ON forum_subscriptions(user_id);

-- Full text search
CREATE INDEX idx_forum_posts_search ON forum_posts USING GIN(to_tsvector('english', title || ' ' || content));
CREATE INDEX idx_forum_replies_search ON forum_replies USING GIN(to_tsvector('english', content));
```

### 2.4 REST API Endpoints

```yaml
Categories:
  GET    /api/v1/forum/categories:
    description: Get all forum categories
    query: { hiveId: optional }
    
  GET    /api/v1/forum/categories/{categoryId}:
    description: Get category details with posts
    
  POST   /api/v1/forum/categories:
    description: Create new category (admin/moderator)
    body: { name, description, parentId, hiveId }

Posts:
  GET    /api/v1/forum/posts:
    description: Get posts with pagination
    query: { categoryId, page, limit, sort: latest|top|trending }
    
  GET    /api/v1/forum/posts/{postId}:
    description: Get post with replies
    
  POST   /api/v1/forum/posts:
    description: Create new post
    body: { categoryId, title, content, tags }
    
  PUT    /api/v1/forum/posts/{postId}:
    description: Update post
    body: { title, content, tags }
    
  DELETE /api/v1/forum/posts/{postId}:
    description: Delete post (soft delete)

Replies:
  GET    /api/v1/forum/posts/{postId}/replies:
    description: Get post replies
    query: { page, limit, sort: oldest|newest|top }
    
  POST   /api/v1/forum/posts/{postId}/replies:
    description: Add reply to post
    body: { content, parentReplyId }
    
  PUT    /api/v1/forum/replies/{replyId}:
    description: Update reply
    body: { content }
    
  DELETE /api/v1/forum/replies/{replyId}:
    description: Delete reply (soft delete)

Voting:
  POST   /api/v1/forum/posts/{postId}/vote:
    description: Vote on post
    body: { voteType: 1|-1 }
    
  POST   /api/v1/forum/replies/{replyId}/vote:
    description: Vote on reply
    body: { voteType: 1|-1 }
    
  DELETE /api/v1/forum/posts/{postId}/vote:
    description: Remove vote from post
    
  DELETE /api/v1/forum/replies/{replyId}/vote:
    description: Remove vote from reply

Search & Subscriptions:
  GET    /api/v1/forum/search:
    description: Search forum content
    query: { q: searchQuery, category, tags }
    
  POST   /api/v1/forum/subscribe:
    description: Subscribe to category or post
    body: { categoryId|postId }
    
  DELETE /api/v1/forum/unsubscribe:
    description: Unsubscribe from category or post
    body: { categoryId|postId }
```

---

## 3. WebSocket Events

### 3.1 Buddy System Events

```typescript
// Client -> Server
interface BuddyWebSocketEvents {
  'buddy:request': { targetUserId: number; message: string };
  'buddy:checkin': { message: string; mood: string; score: number };
  'buddy:goal:update': { goalId: number; progress: number };
  'buddy:typing': { isTyping: boolean };
  'buddy:session:start': { sessionId: number };
  'buddy:session:sync': { action: 'pause' | 'resume' | 'end' };
}

// Server -> Client
interface BuddyBroadcastEvents {
  'buddy:request:received': { request: BuddyRequest };
  'buddy:request:accepted': { relationship: BuddyRelationship };
  'buddy:checkin:new': { checkin: BuddyCheckin };
  'buddy:goal:updated': { goal: BuddyGoal };
  'buddy:partner:online': { userId: number; status: 'online' | 'offline' };
  'buddy:typing:status': { userId: number; isTyping: boolean };
  'buddy:session:update': { session: BuddySession; action: string };
}
```

### 3.2 Forum Events

```typescript
// Client -> Server
interface ForumWebSocketEvents {
  'forum:post:view': { postId: number };
  'forum:subscribe': { type: 'category' | 'post'; id: number };
  'forum:typing': { postId: number; isTyping: boolean };
}

// Server -> Client
interface ForumBroadcastEvents {
  'forum:post:created': { post: ForumPost; category: string };
  'forum:reply:created': { reply: ForumReply; postId: number };
  'forum:post:updated': { post: ForumPost };
  'forum:vote:updated': { targetType: string; targetId: number; score: number };
  'forum:user:typing': { postId: number; userId: number; username: string };
  'forum:post:trending': { posts: ForumPost[] };
}
```

---

## 4. Service Implementation

### 4.1 Buddy Service

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BuddyService {
    
    private final BuddyRelationshipRepository relationshipRepo;
    private final BuddyGoalRepository goalRepo;
    private final BuddyCheckinRepository checkinRepo;
    private final BuddyMatchingService matchingService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    
    public BuddyRelationship requestBuddy(Long userId, Long targetUserId, String message) {
        // Check for existing relationship
        if (relationshipRepo.existsBetweenUsers(userId, targetUserId)) {
            throw new BuddyException("Relationship already exists");
        }
        
        // Create relationship request
        BuddyRelationship relationship = BuddyRelationship.builder()
            .user1Id(Math.min(userId, targetUserId))
            .user2Id(Math.max(userId, targetUserId))
            .initiatedBy(userId)
            .status(BuddyStatus.PENDING)
            .build();
        
        relationship = relationshipRepo.save(relationship);
        
        // Send notification
        notificationService.sendBuddyRequest(targetUserId, userId, message);
        
        // Broadcast via WebSocket
        webSocketService.sendToUser(targetUserId, "buddy:request:received", relationship);
        
        return relationship;
    }
    
    public BuddyRelationship acceptRequest(Long userId, Long relationshipId) {
        BuddyRelationship relationship = relationshipRepo.findById(relationshipId)
            .orElseThrow(() -> new NotFoundException("Request not found"));
        
        // Validate user is the recipient
        if (!relationship.isRecipient(userId)) {
            throw new UnauthorizedException("Cannot accept this request");
        }
        
        // Update status
        relationship.setStatus(BuddyStatus.ACTIVE);
        relationship.setMatchedAt(LocalDateTime.now());
        relationship = relationshipRepo.save(relationship);
        
        // Notify both users
        Long partnerId = relationship.getPartnerId(userId);
        notificationService.sendBuddyAccepted(partnerId, userId);
        webSocketService.sendToUser(partnerId, "buddy:request:accepted", relationship);
        
        return relationship;
    }
    
    public BuddyGoal createGoal(Long userId, Long relationshipId, BuddyGoalRequest request) {
        // Validate relationship
        BuddyRelationship relationship = validateActiveRelationship(userId, relationshipId);
        
        // Create goal
        BuddyGoal goal = BuddyGoal.builder()
            .relationshipId(relationshipId)
            .createdBy(userId)
            .title(request.getTitle())
            .description(request.getDescription())
            .targetDate(request.getTargetDate())
            .status(GoalStatus.ACTIVE)
            .progress(0)
            .build();
        
        goal = goalRepo.save(goal);
        
        // Notify buddy
        Long buddyId = relationship.getPartnerId(userId);
        webSocketService.sendToUser(buddyId, "buddy:goal:created", goal);
        
        return goal;
    }
    
    public BuddyCheckin createCheckin(Long userId, BuddyCheckinRequest request) {
        // Get active relationship
        BuddyRelationship relationship = relationshipRepo.findActiveByUserId(userId)
            .orElseThrow(() -> new NotFoundException("No active buddy relationship"));
        
        // Create check-in
        BuddyCheckin checkin = BuddyCheckin.builder()
            .relationshipId(relationship.getId())
            .userId(userId)
            .message(request.getMessage())
            .mood(request.getMood())
            .productivityScore(request.getProductivityScore())
            .goalsProgress(request.getGoalsProgress())
            .build();
        
        checkin = checkinRepo.save(checkin);
        
        // Notify buddy
        Long buddyId = relationship.getPartnerId(userId);
        webSocketService.sendToUser(buddyId, "buddy:checkin:new", checkin);
        
        return checkin;
    }
}
```

### 4.2 Forum Service

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ForumService {
    
    private final ForumPostRepository postRepo;
    private final ForumReplyRepository replyRepo;
    private final ForumVoteRepository voteRepo;
    private final ForumSubscriptionRepository subscriptionRepo;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final MarkdownService markdownService;
    
    public ForumPost createPost(Long userId, ForumPostRequest request) {
        // Validate category
        ForumCategory category = categoryRepo.findById(request.getCategoryId())
            .orElseThrow(() -> new NotFoundException("Category not found"));
        
        // Check permissions if hive-specific
        if (category.getHiveId() != null) {
            validateHiveMembership(userId, category.getHiveId());
        }
        
        // Create post
        ForumPost post = ForumPost.builder()
            .categoryId(request.getCategoryId())
            .userId(userId)
            .title(request.getTitle())
            .content(request.getContent())
            .contentHtml(markdownService.toHtml(request.getContent()))
            .slug(generateSlug(request.getTitle()))
            .tags(request.getTags())
            .build();
        
        post = postRepo.save(post);
        
        // Notify category subscribers
        notifySubscribers(category.getId(), post);
        
        // Broadcast to WebSocket
        webSocketService.broadcast("forum:post:created", Map.of(
            "post", post,
            "category", category.getName()
        ));
        
        return post;
    }
    
    public ForumReply addReply(Long userId, Long postId, ForumReplyRequest request) {
        // Validate post
        ForumPost post = postRepo.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        
        if (post.isLocked()) {
            throw new ForumException("Post is locked");
        }
        
        // Create reply
        ForumReply reply = ForumReply.builder()
            .postId(postId)
            .parentReplyId(request.getParentReplyId())
            .userId(userId)
            .content(request.getContent())
            .contentHtml(markdownService.toHtml(request.getContent()))
            .build();
        
        reply = replyRepo.save(reply);
        
        // Update post reply count
        postRepo.incrementReplyCount(postId);
        
        // Notify post author and subscribers
        if (!post.getUserId().equals(userId)) {
            notificationService.sendReplyNotification(post.getUserId(), post, reply);
        }
        notifyPostSubscribers(postId, reply);
        
        // Broadcast via WebSocket
        webSocketService.broadcast("forum:reply:created", Map.of(
            "reply", reply,
            "postId", postId
        ));
        
        return reply;
    }
    
    public void voteOnPost(Long userId, Long postId, int voteType) {
        // Validate vote type
        if (voteType != 1 && voteType != -1) {
            throw new IllegalArgumentException("Invalid vote type");
        }
        
        // Check existing vote
        Optional<ForumVote> existingVote = voteRepo.findByUserAndPost(userId, postId);
        
        if (existingVote.isPresent()) {
            ForumVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Same vote - remove it
                voteRepo.delete(vote);
                postRepo.updateVoteScore(postId, -voteType);
            } else {
                // Different vote - update it
                vote.setVoteType(voteType);
                voteRepo.save(vote);
                postRepo.updateVoteScore(postId, voteType * 2);
            }
        } else {
            // New vote
            ForumVote vote = ForumVote.builder()
                .userId(userId)
                .postId(postId)
                .voteType(voteType)
                .build();
            voteRepo.save(vote);
            postRepo.updateVoteScore(postId, voteType);
        }
        
        // Get updated score
        Integer newScore = postRepo.getVoteScore(postId);
        
        // Broadcast update
        webSocketService.broadcast("forum:vote:updated", Map.of(
            "targetType", "post",
            "targetId", postId,
            "score", newScore
        ));
    }
    
    public Page<ForumPost> searchPosts(String query, Pageable pageable) {
        return postRepo.searchPosts(query, pageable);
    }
}
```

---

## 5. Frontend Components

### 5.1 Buddy System Components

```typescript
// BuddyDashboard.tsx
interface BuddyDashboardProps {
  userId: number;
}

export const BuddyDashboard: React.FC<BuddyDashboardProps> = ({ userId }) => {
  const { buddy, goals, checkins, loading } = useBuddy(userId);
  const [showMatchingModal, setShowMatchingModal] = useState(!buddy);
  
  if (loading) return <LoadingSpinner />;
  
  if (!buddy) {
    return (
      <>
        <NoBuddyState onFindBuddy={() => setShowMatchingModal(true)} />
        {showMatchingModal && (
          <BuddyMatchingModal onClose={() => setShowMatchingModal(false)} />
        )}
      </>
    );
  }
  
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={4}>
        <BuddyCard buddy={buddy} />
        <BuddyCommunication buddyId={buddy.id} />
      </Grid>
      
      <Grid item xs={12} md={8}>
        <BuddyGoals goals={goals} relationshipId={buddy.relationshipId} />
        <BuddyCheckins checkins={checkins} />
        <BuddyProgress buddy={buddy} goals={goals} />
      </Grid>
    </Grid>
  );
};

// BuddyMatching.tsx
export const BuddyMatchingModal: React.FC = () => {
  const { matches, loading, requestBuddy } = useBuddyMatching();
  
  return (
    <Modal>
      <Typography variant="h5">Find Your Accountability Buddy</Typography>
      <List>
        {matches.map(match => (
          <BuddyMatchCard
            key={match.userId}
            match={match}
            onRequest={() => requestBuddy(match.userId)}
          />
        ))}
      </List>
    </Modal>
  );
};
```

### 5.2 Forum Components

```typescript
// ForumLayout.tsx
export const ForumLayout: React.FC = () => {
  const { categories } = useForumCategories();
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={3}>
        <ForumSidebar 
          categories={categories}
          selectedCategory={selectedCategory}
          onSelectCategory={setSelectedCategory}
        />
      </Grid>
      
      <Grid item xs={12} md={9}>
        <Routes>
          <Route path="/" element={<ForumHome />} />
          <Route path="/category/:categoryId" element={<ForumCategory />} />
          <Route path="/post/:postId" element={<ForumPost />} />
          <Route path="/new" element={<CreatePost />} />
        </Routes>
      </Grid>
    </Grid>
  );
};

// ForumPost.tsx
export const ForumPost: React.FC = () => {
  const { postId } = useParams();
  const { post, replies, loading } = useForumPost(postId);
  const { user } = useAuth();
  
  const handleVote = (voteType: number) => {
    forumApi.voteOnPost(postId, voteType);
  };
  
  const handleReply = (content: string, parentId?: number) => {
    forumApi.addReply(postId, { content, parentReplyId: parentId });
  };
  
  return (
    <Paper sx={{ p: 3 }}>
      <ForumPostHeader post={post} onVote={handleVote} />
      <Divider sx={{ my: 2 }} />
      <ForumPostContent content={post.contentHtml} />
      
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6">
          {post.replyCount} Replies
        </Typography>
        
        <ForumReplyList 
          replies={replies}
          onReply={handleReply}
          onVote={(replyId, voteType) => forumApi.voteOnReply(replyId, voteType)}
        />
        
        {user && (
          <ForumReplyEditor onSubmit={(content) => handleReply(content)} />
        )}
      </Box>
    </Paper>
  );
};
```

---

## 6. Real-time Integration

### 6.1 WebSocket Service

```java
@Service
@RequiredArgsConstructor
public class ForumWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void notifyNewPost(ForumPost post, String categoryName) {
        ForumPostNotification notification = ForumPostNotification.builder()
            .post(post)
            .category(categoryName)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Broadcast to all connected users
        messagingTemplate.convertAndSend("/topic/forum/posts", notification);
        
        // Send to category subscribers
        messagingTemplate.convertAndSend(
            "/topic/forum/category/" + post.getCategoryId(), 
            notification
        );
    }
    
    public void notifyNewReply(ForumReply reply, Long postId) {
        ForumReplyNotification notification = ForumReplyNotification.builder()
            .reply(reply)
            .postId(postId)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to post subscribers
        messagingTemplate.convertAndSend(
            "/topic/forum/post/" + postId, 
            notification
        );
    }
    
    public void broadcastTypingStatus(Long postId, Long userId, String username, boolean isTyping) {
        TypingStatus status = TypingStatus.builder()
            .postId(postId)
            .userId(userId)
            .username(username)
            .isTyping(isTyping)
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/forum/post/" + postId + "/typing", 
            status
        );
    }
}
```

---

## 7. Performance Considerations

### 7.1 Caching Strategy

```java
@Configuration
@EnableCaching
public class ForumCacheConfig {
    
    @Bean
    public CacheManager forumCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(/* ... */)
            .serializeValuesWith(/* ... */);
        
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "forum-posts", config.entryTtl(Duration.ofMinutes(5)),
            "forum-categories", config.entryTtl(Duration.ofHours(1)),
            "forum-trending", config.entryTtl(Duration.ofMinutes(15)),
            "buddy-matches", config.entryTtl(Duration.ofHours(6))
        );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

### 7.2 Database Optimization

- Proper indexing on foreign keys and frequently queried columns
- Pagination for all list endpoints
- Lazy loading for nested relationships
- Database connection pooling
- Query optimization for full-text search

---

## 8. Security Considerations

### 8.1 Authorization

```java
@Component
public class ForumSecurityService {
    
    public boolean canEditPost(Long userId, ForumPost post) {
        return post.getUserId().equals(userId) || 
               userService.hasRole(userId, "MODERATOR");
    }
    
    public boolean canDeletePost(Long userId, ForumPost post) {
        return post.getUserId().equals(userId) || 
               userService.hasRole(userId, "MODERATOR");
    }
    
    public boolean canModerate(Long userId, Long categoryId) {
        ForumCategory category = categoryRepo.findById(categoryId);
        if (category.getHiveId() != null) {
            return hiveService.isModerator(userId, category.getHiveId());
        }
        return userService.hasRole(userId, "GLOBAL_MODERATOR");
    }
}
```

### 8.2 Input Validation

- Sanitize HTML content to prevent XSS
- Validate markdown input
- Rate limiting on post/reply creation
- Spam detection
- Profanity filtering

---

## 9. Migration Strategy

### 9.1 Database Migrations

```sql
-- V8__create_buddy_system.sql
-- Run buddy system schema creation

-- V9__create_forum_system.sql  
-- Run forum system schema creation

-- V10__add_indexes_and_constraints.sql
-- Add performance indexes and constraints
```

### 9.2 Deployment Plan

1. **Phase 1**: Deploy database migrations
2. **Phase 2**: Deploy backend services with feature flags
3. **Phase 3**: Deploy frontend with features behind flags
4. **Phase 4**: Enable features progressively
5. **Phase 5**: Monitor and optimize

---

## 10. Testing Strategy

### 10.1 Unit Tests

```java
@Test
void testBuddyMatching() {
    // Given
    User user1 = createUser(timezone: "PST", focusAreas: ["coding", "writing"]);
    User user2 = createUser(timezone: "PST", focusAreas: ["coding", "design"]);
    User user3 = createUser(timezone: "EST", focusAreas: ["marketing"]);
    
    // When
    List<BuddyMatch> matches = matchingService.findMatches(user1.getId());
    
    // Then
    assertThat(matches).hasSize(2);
    assertThat(matches.get(0).getUserId()).isEqualTo(user2.getId());
    assertThat(matches.get(0).getScore()).isGreaterThan(0.5);
}

@Test
void testForumPostCreation() {
    // Given
    ForumPostRequest request = ForumPostRequest.builder()
        .categoryId(1L)
        .title("Test Post")
        .content("Test content")
        .tags(List.of("test", "demo"))
        .build();
    
    // When
    ForumPost post = forumService.createPost(userId, request);
    
    // Then
    assertThat(post.getId()).isNotNull();
    assertThat(post.getSlug()).isEqualTo("test-post");
    verify(webSocketService).broadcast(eq("forum:post:created"), any());
}
```

### 10.2 Integration Tests

- Test buddy matching algorithm with real data
- Test forum search functionality
- Test WebSocket message delivery
- Test notification system
- Test moderation workflows

---

## Implementation Timeline

### Day 1 (Today - August 11)
1. **Morning**: Create database migrations
2. **Afternoon**: Implement Buddy Service backend
3. **Evening**: Implement Forum Service backend

### Day 2 (August 12)
1. **Morning**: Create REST API controllers
2. **Afternoon**: Implement WebSocket handlers
3. **Evening**: Frontend components for buddy system

### Day 3 (August 13)
1. **Morning**: Frontend components for forums
2. **Afternoon**: Integration testing
3. **Evening**: Performance optimization

### Day 4 (August 14)
1. **Morning**: Final testing and bug fixes
2. **Afternoon**: Documentation updates
3. **Evening**: Deployment preparation

---

## Conclusion

This design provides a comprehensive blueprint for implementing both the Buddy System and Forum Features. The architecture integrates seamlessly with existing FocusHive infrastructure while maintaining scalability and performance.

Key benefits:
- **Buddy System**: Enhances accountability through partnerships
- **Forums**: Builds community and knowledge sharing
- **Real-time**: WebSocket integration for immediate updates
- **Scalable**: Designed for growth with proper indexing and caching
- **Secure**: Built-in moderation and authorization

Both features can be implemented in parallel with feature flags for progressive rollout.