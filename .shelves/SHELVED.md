# Shelved Features and Services

## Music Service (Spotify Integration)

### Status: SHELVED
**Date Shelved**: December 17, 2024
**Planned Reactivation**: Phase 2 - Post-MVP Launch

### Rationale for Shelving

#### Time Constraints
- Project deadline approaching with limited development resources
- Focus needed on core functionality for MVP launch
- Music integration is a "nice-to-have" rather than critical feature

#### Technical Complexity
- Spotify OAuth2 integration requires additional setup and testing
- Collaborative playlist management adds complexity
- Real-time synchronization of music across hive members needs careful implementation

#### Business Priorities
1. **Core Features First**: Focus on essential co-working functionality
   - Hive management
   - Real-time presence
   - Timer synchronization
   - Chat communication
   - Productivity analytics

2. **User Validation**: Launch MVP without music to validate core concept
   - Gather user feedback on essential features
   - Understand if music integration is truly needed
   - Identify preferred music platforms (Spotify, Apple Music, YouTube Music)

### What Was Completed
- Basic service structure created
- Database schema designed
- API endpoints defined
- Spotify OAuth flow partially implemented

### What Remains
- Complete Spotify OAuth integration
- Implement collaborative playlist creation
- Add music synchronization across hive members
- Create mood-based recommendation engine
- Add music preference settings
- Implement fallback for users without Spotify

### Future Integration Plan

#### Phase 1 (Current - Without Music)
- Launch with 4 core services
- Focus on stability and user growth
- Gather feedback on music feature demand

#### Phase 2 (3-6 months post-launch)
- Reactivate music-service development
- Complete Spotify integration
- Add support for additional music platforms
- Implement basic collaborative features

#### Phase 3 (6-12 months)
- Advanced features: AI-powered recommendations
- Integration with focus timer (music changes with timer phases)
- Community-curated focus playlists
- Music analytics (what music improves productivity)

### Technical Notes

The music-service is fully isolated and can be reintegrated without affecting other services:

1. **No Breaking Dependencies**: Other services don't depend on music-service
2. **Clean Integration Points**: Well-defined API contracts already established
3. **Database Independence**: Separate schema that won't affect existing data
4. **Optional Enhancement**: Adds value but not required for core functionality

### Files Preserved

All music-service code has been preserved in `.shelves/music-service/`:
- Complete Spring Boot application structure
- Spotify integration code
- Database migrations
- API documentation
- Docker configuration

### Reactivation Checklist

When ready to reactivate:

- [ ] Move from `.shelves/music-service/` back to `services/`
- [ ] Update docker-compose.yml to include music-service
- [ ] Update documentation (PROJECT_INDEX.md, API_REFERENCE.md, CLAUDE.md)
- [ ] Complete Spotify OAuth implementation
- [ ] Add environment variables for Spotify credentials
- [ ] Test integration with existing services
- [ ] Update frontend to include music controls
- [ ] Deploy and monitor performance impact

---

## Decision Framework for Shelving

Features/services are shelved when:

1. **Time Impact**: Development would delay critical features
2. **Complexity**: Technical complexity outweighs immediate value
3. **Dependencies**: External dependencies (APIs, licenses) not ready
4. **User Priority**: User research shows it's not a top priority
5. **Resource Constraints**: Limited team bandwidth for proper implementation

Features are reactivated when:

1. **Core Stable**: Main features are stable and well-tested
2. **User Demand**: Clear user demand validated through feedback
3. **Resources Available**: Dedicated resources for implementation
4. **Technical Ready**: Dependencies and infrastructure in place
5. **Business Value**: Clear ROI or strategic value identified

---

*This document serves as a record of shelved features and the reasoning behind these decisions. It ensures we can efficiently reactivate these features when appropriate.*