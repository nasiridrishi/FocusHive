# FocusHive Exam Q&A Reference Document

This document contains all exam-style questions answered about the FocusHive project to maintain consistency in the final report.

---

## Q1: What was the title of this project?

**Answer:** The project is titled **"FocusHive"** - a name that encapsulates creating collaborative digital spaces for focused work and study sessions. The name combines "Focus" (deep work and concentration) with "Hive" (collaborative workspace where individuals work independently yet together).

---

## Q2: What templates does this project use?

**Answer:** FocusHive integrates three University of London Final Year Project templates:

1. **PRIMARY (70% focus)** - CM3055 Interaction Design: "Emotion-Aware Adaptive Email and Task Manager"
   - Implemented as real-time presence system with WebSocket communication
   - Activity-based emotion detection instead of camera monitoring
   - Adaptive UI with Material UI and responsive design

2. **SECONDARY (25% focus)** - CM3035 Advanced Web Design: "Identity and Profile Management API"
   - Separate identity-service microservice
   - OAuth2 provider (not just consumer)
   - JWT authentication with multiple personas support

3. **SUPPORTING (5% focus)** - CM3065 Intelligent Signal Processing: "Gamified Smart Environment"
   - Points, achievements, and leaderboards
   - Productivity tracking and analytics
   - Adaptive challenges based on performance

---

## Q3: Describe what this project is about

**Answer:** FocusHive is a web-based platform that recreates the psychological benefits of physical co-working spaces digitally. It addresses remote worker isolation, lack of accountability, and focus fragmentation by creating virtual "hives" where users work individually while maintaining virtual presence with others.

Key features:
- Real-time presence indicators (WebSocket, <100ms latency)
- Pomodoro-style synchronized focus sessions
- Buddy system for accountability partnerships
- Microservices architecture (frontend, backend, identity-service)
- Zero-trust security with Cloudflare tunnels
- Privacy-first design (no camera requirements)

---

## Q4: How does this project relate to the template and its flexibility?

**Answer:** FocusHive creatively interprets the CM3055 template rather than literally implementing an email manager:
- "Email Manager" → Real-time communication platform
- "Emotion-Aware" → Activity-based mood inference (not cameras)
- "Adaptive" → Context-responsive interface
- "Task Manager" → Focus session coordinator

The template's flexibility allowed choosing the tech stack (Spring Boot, React), architecture (microservices), and privacy-respecting emotion detection. This freedom enabled innovation but also led to scope creep and decision paralysis. The project demonstrates that template flexibility enables superior learning outcomes when managed strategically.

---

## Q5: Concepts of inclusive design and radical inclusion

**Answer (Short Version):** FocusHive embedded inclusive design by avoiding camera-based monitoring in favor of activity indicators, accommodating users with limited bandwidth or privacy concerns. The 70KB optimized bundle ensures access on older devices. Radical inclusion manifested in our "constraints-first" philosophy - designing for edge cases (no camera, unstable internet) created better universal design. We succeeded at privacy-respecting presence but failed at internationalization. The key outcome: designing for margins improved the product for all users.

---

## Q6: How inclusive design related to this project's development

**Answer:** FocusHive's inclusive choices centered on rejecting camera monitoring for activity-based presence, broadening accessibility for users with bandwidth limitations, privacy concerns, or disabilities. The bundle optimization (3.86MB to 70KB) assumed limited data plans. Material UI provided WCAG compliance. Our privacy-first approach accidentally created better universal design - deaf users could fully participate without audio/video. The zero-trust architecture made FocusHive accessible from restrictive networks. Main failure: no internationalization or offline support.

---

## Q7: Aims, Outcomes, and Lessons Learned

**Answer:** FocusHive achieved ~75% of initial aims. Successes include:
- Real-time presence with <100ms latency
- Microservices architecture with OAuth2 provider
- Zero-trust security via Cloudflare tunnels

Shortfalls:
- Emotion detection simplified from facial analysis to activity patterns
- Music integration completely cut
- Buddy system and gamification only reached MVP status
- Test coverage at 30% instead of 80% target

Key lessons: Microservices added complexity that monolithic would avoid. Privacy constraints drove better design. Main discrepancies stem from overambitious scoping, learning curve of new technologies, and over-engineering early features.

---

## Q8: Three lessons for future projects

**Answer:** 

1. **Testing and Evaluation**: Adopted "build first, test later" resulting in 30% coverage. Future: Implement TDD from day one, setup testing infrastructure first sprint, allocate 30% of each sprint to testing.

2. **Project Development**: Microservices from start created unnecessary complexity. Future: Start with modular monolith, extract services only after proving concept and identifying actual bottlenecks.

3. **Defining Aims**: Didn't differentiate core features from nice-to-haves. Future: Use MoSCoW prioritization, build in "integration tax" (1.5x estimates for service coordination), define specific success metrics early.

---

## Q9: Why is this a Computer Science project? (4 marks)

**Answer:** FocusHive is fundamentally CS because it solves distributed systems problems - achieving real-time state synchronization across clients using eventual consistency patterns. The algorithmic complexity appears in timer synchronization despite network latency and bundle optimization using graph algorithms. The OAuth2 provider implementation required understanding cryptographic principles behind JWT tokens. The abstraction layers from React components through Spring Boot to PostgreSQL demonstrate fundamental CS principles of information hiding and interface design.

---

## Q10: Theoretical framework critical to project (8 marks)

**Answer:** The publish-subscribe pattern was critical for FocusHive's real-time presence system. Implemented through Redis, it solved broadcasting updates without O(n²) complexity. When user status changes, backend publishes to Redis channel "hive:123:presence" instead of maintaining individual WebSocket connections. This transforms complexity from quadratic to linear. Combined with Redis sorted sets for presence timeout detection. The pattern's guarantee of delivery independence from subscriber count meant we could promise real-time awareness regardless of hive size, directly supporting the goal of creating responsive co-working environment.

---

## Q11: Significant figure from report (8 marks)

**Answer:** The JavaScript bundle size reduction from 3.86MB to 70KB (98.2% reduction) is the most significant data point. This refers to the frontend React/JavaScript bundle (NOT Java JARs) that browsers download. The waterfall chart shows: code splitting (-2.1MB), lazy loading (-1.2MB), emoji→icons (-340KB), tree shaking (-216KB). 

Significance: Initial 3.86MB meant 15-20 second loads on slow connections, excluding bandwidth-limited users. The 70KB final bundle loads in <2 seconds on 3G, proving we balanced functionality with accessibility. This quantitative evidence validates that FocusHive successfully achieved inclusive design while maintaining rich features.

---

## Key Technical Achievements to Reference

1. **Performance**: Bundle optimization from 3.86MB to 70KB
2. **Real-time**: WebSocket presence with <100ms latency
3. **Architecture**: Microservices with identity-service separation
4. **Security**: Zero-trust with no exposed ports, Cloudflare tunnels
5. **Deployment**: dev.focushive.app with Docker containerization
6. **Technologies**: Spring Boot 3.x, React 18, TypeScript, PostgreSQL, Redis
7. **Privacy**: No camera requirements, activity-based presence

## Consistent Project Statistics

- **Timeline**: May 22, 2025 to September 15, 2025
- **Test Coverage**: 30% achieved (80% target)
- **Template Split**: 70% CM3055, 25% CM3035, 5% CM3065
- **Bundle Size**: 3.86MB → 70KB (98.2% reduction)
- **Latency**: <100ms for presence updates
- **Architecture**: 3 microservices (frontend, backend, identity)
- **Success Rate**: ~75% of initial aims achieved

---

*Last Updated: Current Session*
*Purpose: Maintain consistency across exam answers and final report*