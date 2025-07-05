# Comprehensive Development Prompt for FocusHive Project

## Project Overview
Create a productivity platform called "FocusHive" that integrates user identity management, emotion-aware interfaces, gamification elements, recommendation systems, and data synchronization. This full-stack application combines components from five distinct project templates to create a cohesive productivity ecosystem.

## Phase 1: Core Identity and Task Management

### Step 1: Project Setup
```
Initialize a full-stack project with the following structure:
- Backend: Node.js with Express.js
- Frontend: React.js with TypeScript
 - Desktop: Electron setup for macOS and Windows
- Database: Configure MongoDB and PostgreSQL instances
- Set up Git repository with branching strategy
- Configure Docker for containerization
- Implement CI/CD pipeline for testing and deployment
```

### Step 2: User Authentication System
```
Implement a secure authentication system with:
- JWT-based authentication flow
- Registration, login, and password reset functionality
- Email verification process
- Strong password policies and secure storage
- Rate limiting and brute force protection
- OAuth2 integration with Google, Microsoft, and Apple
- Role-based access control (RBAC)
```

### Step 3: Profile Management Service
```
Create a profile management API with:
- RESTful CRUD endpoints for user profiles
- Profile schema storing:
  - Personal details (name, email, preferences)
  - Productivity preferences (work style, focus duration)
  - Theme and UI preferences
  - Permission settings for data sharing
- Profile image upload and storage
- API documentation using Swagger/OpenAPI
- Input validation and sanitization
```

### Step 4: Basic Task Management
```
Develop core task management functionality:
- Create database schema for tasks with:
  - Title, description, due date, priority, status
  - Tags and categories
  - Time estimates and actual duration
  - Parent-child task relationships
- Implement RESTful API for task CRUD operations
- Add filtering and sorting capabilities
- Create basic frontend UI for task management
- Implement drag-and-drop functionality for task organization
```

## Phase 2: Emotion Detection and Adaptation

### Step 5: Sentiment Analysis Integration
```
Implement text sentiment analysis:
- Integrate natural language processing library (e.g., TensorFlow.js)
- Create endpoint for analyzing task descriptions and comments
- Develop sentiment classification (positive, negative, neutral)
- Store sentiment metrics with task history
- Build feedback loop for improving sentiment accuracy
- Implement privacy controls for sentiment data
```

### Step 6: Optional Facial Expression Analysis
```
Add opt-in facial expression analysis:
- Implement WebRTC for camera access
- Integrate face-api.js for expression detection
- Create emotional state classification system
- Develop secure, client-side only processing
- Add clear opt-in/opt-out controls
- Handle proper data disposal after analysis
```

### Step 7: Adaptive UI Components
```
Create an emotion-responsive interface:
- Develop component library with emotion-based variants
- Implement UI transformation logic based on emotional states:
  - Simplified interface during stress
  - Enhanced focus mode during flow states
  - Encouraging elements during fatigue
- Create smooth transitions between interface states
- Add A/B testing framework to measure effectiveness
- Implement user feedback mechanism for UI adaptations
```

### Step 8: Stress-Reduction Features
```
Implement wellness interventions:
- Create guided breathing exercise component
- Develop algorithm for detecting work pattern fatigue
- Implement smart break recommendation system
- Add mindfulness reminders based on stress signals
- Create task reprioritization suggestions
- Develop user control panel for intervention preferences
```

## Phase 3: Gamification and Desktop Experience

### Step 9: Achievement Framework
```
Build gamification system:
- Design hexagonal "honeycomb" achievement visualization
- Create achievement database schema with:
  - Unlock conditions and progress tracking
  - Visual assets and descriptions
  - Rarity levels and special conditions
- Implement achievement unlock notifications
- Add progress tracking for partial completions
- Create achievement showcase for user profiles
```

### Step 10: Experience Points System
```
Implement progression mechanics:
- Create XP calculation algorithms for various activities
- Design level progression system with:
  - Experience thresholds for each level
  - Rewards and unlocks at milestone levels
  - Visual indicators of progression
- Add streak bonuses for consistent usage
- Implement leaderboards (optional and privacy-conscious)
- Create weekly/monthly progress reports
```

### Step 11: Desktop Application
```
Develop cross-platform desktop application:
- Set up Electron project targeting macOS and Windows
- Implement offline-first architecture
- Create local SQLite (or similar) storage for offline data
- Develop sync mechanism for cross-device usage
- Optimize UI for desktop interaction patterns
- Add native system notifications
- Implement auto-update pipeline
- Create desktop-specific conveniences (tray icon, quick add)
```

### Step 12: Social Features
```
Add optional social elements:
- Create productivity circles for accountability
- Implement anonymous productivity comparisons
- Develop team challenges for workplace implementation
- Add privacy controls for all social features
- Create activity feeds with privacy filters
- Implement messaging/commenting system
```

## Phase 4: Recommendation and Data Intelligence

### Step 13: Music Recommendation Engine
```
Build productivity music system:
- Create music characteristics database
- Implement Spotify and Apple Music API integrations
- Develop recommendation algorithm based on:
  - Task type (creative, analytical, etc.)
  - Time of day and energy patterns
  - Historical productivity with music styles
- Add user feedback system for rating effectiveness
- Create playlist generation for specific work sessions
- Implement audio feature analysis for productivity correlation
```

### Step 14: Data Synchronization
```
Implement robust data handling:
- Create change data capture (CDC) system
- Implement conflict-free replicated data types (CRDTs)
- Develop conflict resolution for offline changes
- Create delta-based synchronization for efficiency
- Implement data validation and cleansing pipeline
- Add historical data reconciliation for analytics
- Develop cross-device session continuity
```

### Step 15: Analytics Dashboard
```
Build comprehensive analytics:
- Design ETL pipeline for user activity data
- Implement time-series database for productivity patterns
- Create machine learning models to identify productivity patterns
- Develop interactive dashboard with:
  - Productivity metrics visualization
  - Focus session analysis
  - Emotional state correlations
  - Achievement progress
  - Improvement recommendations
- Add data export capabilities
- Implement privacy controls for analytics
```

### Step 16: System Integration and Optimization
```
Finalize system architecture:
- Integrate all microservices with API gateway
- Implement caching strategy with Redis
- Optimize database queries and indexes
- Add comprehensive logging system
- Implement performance monitoring
- Create disaster recovery procedures
- Conduct security audit and penetration testing
- Optimize frontend assets and API payload sizes
```

## Testing and Deployment Instructions

### Automated Testing
```
Implement comprehensive testing:
- Unit tests for all core functions (minimum 80% coverage)
- Integration tests for service interactions
- End-to-end tests for critical user journeys
- Performance tests for API endpoints
- Security tests for authentication and data protection
- Accessibility testing for UI components
- Cross-browser and device compatibility tests
```

### User Testing Framework
```
Create user testing protocols:
- Implement A/B testing framework
- Add user feedback collection system
- Create usability testing scripts
- Develop emotional response measurement
- Implement analytics for feature usage
- Add session recording capabilities (opt-in)
```

### Deployment Strategy
```
Configure deployment pipeline:
- Set up staging and production environments
- Implement blue-green deployment strategy
- Configure automated database migrations
- Set up monitoring and alerting
- Create rollback procedures
- Implement feature flags for gradual rollout
- Configure CDN for static assets
```

## Documentation Requirements

### Technical Documentation
```
Create comprehensive documentation:
- API reference with examples
- Database schema documentation
- Architecture diagrams
- Setup and installation guides
- Contribution guidelines
- Security practices
- Testing protocols
```

### User Documentation
```
Develop user-facing documentation:
- Feature guides and tutorials
- FAQ and troubleshooting
- Privacy policy and data usage
- Accessibility documentation
- Best practices for productivity
- Keyboard shortcuts and power features
```

## Implementation Notes

1. Prioritize modular architecture to allow incremental development
2. Implement privacy-by-design principles throughout
3. Use feature flags to enable progressive rollout of advanced features
4. Document integration points between the five project templates
5. Maintain academic references for psychological principles implemented
6. Follow accessibility guidelines (WCAG 2.1 AA minimum)
7. Incorporate security best practices at every development stage

This comprehensive prompt provides a detailed roadmap for developing the FocusHive platform, implementing all five project templates while maintaining a cohesive user experience and technical architecture.