# Buddy System E2E Tests Documentation

This directory contains comprehensive End-to-End (E2E) tests for FocusHive's Buddy System functionality, implementing the requirements from Linear ticket UOL-311.

## Overview

The Buddy System E2E tests validate all aspects of the accountability partner feature, including:

- Buddy matching and pairing algorithms
- Accountability features and goal tracking
- Peer support and communication tools
- Collaborative productivity features
- Buddy relationship management
- Privacy and safety controls
- Real-time updates and performance
- Responsive design and accessibility compliance

## Test Structure

### Files Organization

```
frontend/e2e/tests/buddy/
├── buddy-system.spec.ts    # Main test suite
├── README.md              # This documentation
```

### Supporting Files

```
frontend/e2e/pages/
├── BuddyPage.ts          # Page Object Model

frontend/e2e/helpers/
├── buddy.helper.ts       # Test utilities and mock data
```

## Test Coverage

### 1. Dashboard Loading and Basic Functionality
- **Dashboard loading**: Validates buddy dashboard loads within performance thresholds
- **Error handling**: Tests graceful handling of API errors and network failures
- **Loading states**: Verifies loading spinners and empty states
- **Navigation**: Tests tab switching and data filtering

### 2. Buddy Matching and Pairing
- **Compatibility scoring**: Tests display of compatibility scores (90%+ accuracy requirement)
- **Match filtering**: Validates filtering by focus areas, timezone, communication style
- **Manual search**: Tests buddy search functionality
- **Request sending**: Validates buddy request workflow
- **Algorithm details**: Tests compatibility breakdown display
- **Relationship limits**: Validates maximum buddy relationship constraints

### 3. Accountability Features
- **Goal tracking**: Tests shared goal creation and progress monitoring
- **Check-ins**: Validates regular accountability check-ins
- **Progress updates**: Tests status sharing and progress reporting
- **Streak tracking**: Validates accountability streak rewards
- **Reminder system**: Tests gentle nudging mechanisms
- **Privacy controls**: Validates information sharing preferences

### 4. Peer Support and Communication
- **Direct messaging**: Tests secure communication between buddies
- **Encouragement tools**: Validates motivation and support features
- **Shared workspace**: Tests collaborative work areas
- **Notification system**: Tests buddy-specific alerts and reminders
- **Help requests**: Validates support-seeking mechanisms

### 5. Collaborative Productivity
- **Shared focus sessions**: Tests co-working session scheduling
- **Joint goal setting**: Validates collaborative goal creation
- **Challenges**: Tests buddy competitions and gamification
- **Study groups**: Validates group formation and management
- **Peer mentoring**: Tests mentorship relationship features

### 6. Buddy Relationship Management
- **Profile viewing**: Tests buddy profile display and customization
- **Relationship status**: Validates status tracking and history
- **Switching/rotation**: Tests buddy relationship changes
- **Feedback system**: Validates rating and review features
- **Conflict resolution**: Tests issue reporting and resolution tools

### 7. Privacy and Safety
- **Harassment prevention**: Tests blocking and reporting mechanisms
- **Secure communications**: Validates message encryption and privacy
- **Privacy enforcement**: Tests privacy setting compliance
- **Safety controls**: Validates user protection measures

### 8. Real-time Updates and Performance
- **Live status updates**: Tests real-time buddy status changes
- **Performance monitoring**: Validates response times and scalability
- **Concurrent interactions**: Tests system stability under load
- **WebSocket connectivity**: Tests real-time communication channels

### 9. Responsive Design and Accessibility
- **Multi-device support**: Tests functionality across screen sizes
- **WCAG 2.1 AA compliance**: Validates accessibility standards
- **Keyboard navigation**: Tests full keyboard accessibility
- **Color contrast**: Validates visual accessibility requirements
- **Screen reader support**: Tests assistive technology compatibility

## Test Data and Mocking

### Mock Data Generation

The `BuddyHelper` class provides comprehensive mock data generation:

```typescript
const mockData = buddyHelper.generateMockBuddyData({
  activeBuddyCount: 3,
  includePotentialMatches: true,
  includeGoals: true,
  includeCheckins: true,
  // ... other options
});
```

### Mock Data Types

- **Active Buddies**: Current accountability partnerships
- **Potential Matches**: Compatibility-scored potential partners
- **Goals**: Individual and joint goal tracking
- **Check-ins**: Progress and mood assessments
- **Sessions**: Scheduled focus sessions
- **Challenges**: Competitive gamification elements
- **Study Groups**: Collaborative learning groups
- **Stats**: Performance metrics and achievements

### API Mocking

All buddy system endpoints are mocked for consistent testing:

- `/api/v1/buddy/dashboard` - Main dashboard data
- `/api/v1/buddy/relationships/active` - Active buddy relationships
- `/api/v1/buddy/matches` - Potential buddy matches
- `/api/v1/buddy/goals` - Goal management
- `/api/v1/buddy/checkins` - Check-in system
- `/api/v1/buddy/sessions` - Session scheduling
- WebSocket connections for real-time updates

## Performance Requirements

The tests validate specific performance thresholds:

- **Dashboard Load Time**: < 3 seconds
- **API Response Time**: < 2 seconds
- **Chart Render Time**: < 1.5 seconds
- **Real-time Updates**: < 500ms latency
- **Concurrent Users**: Support for 50+ simultaneous users

## Accessibility Compliance

Tests validate WCAG 2.1 AA compliance including:

- **Keyboard Navigation**: Full functionality without mouse
- **Screen Reader**: Proper ARIA labels and descriptions
- **Color Contrast**: Minimum 4.5:1 ratio for normal text
- **Focus Management**: Clear focus indicators
- **Alternative Text**: Descriptive content for images/icons

## Running the Tests

### Prerequisites

1. **Frontend Application**: Running on port 5173
2. **Backend Services**: Mock or real services on ports 8080-8087
3. **Test Environment**: Properly configured test databases

### Execution Commands

```bash
# Run all buddy system tests
npx playwright test tests/buddy/

# Run specific test file
npx playwright test tests/buddy/buddy-system.spec.ts

# Run with specific browser
npx playwright test tests/buddy/ --project=chromium

# Run in debug mode
npx playwright test tests/buddy/ --debug

# Generate test report
npx playwright test tests/buddy/ --reporter=html
```

### Test Configuration

Tests support various configuration options:

```bash
# Set test timeout
TIMEOUT=30000 npx playwright test tests/buddy/

# Configure API endpoints
E2E_API_BASE_URL=http://localhost:8080 npx playwright test tests/buddy/

# Enable verbose logging
DEBUG=1 npx playwright test tests/buddy/
```

## Test Scenarios

### Critical User Journeys

1. **New User Onboarding**:
   - Create account → Set preferences → Find matches → Send request → Accept buddy

2. **Daily Accountability**:
   - Login → Check buddy status → Update progress → Send encouragement → Schedule session

3. **Goal Management**:
   - Create goal → Share with buddy → Track progress → Complete milestone → Celebrate

4. **Session Collaboration**:
   - Schedule session → Join session → Work together → Provide feedback → Plan next session

5. **Relationship Management**:
   - View relationship stats → Update preferences → Handle conflicts → Rotate buddies

### Edge Cases

- **Network Connectivity**: Offline/online transitions
- **Concurrent Actions**: Multiple users acting simultaneously
- **Data Consistency**: Synchronization across devices
- **Error Recovery**: Graceful handling of failures
- **Boundary Conditions**: Maximum limits and constraints

## Error Handling

Tests validate comprehensive error scenarios:

### API Errors
- **500 Internal Server Error**: Server-side failures
- **404 Not Found**: Missing resources
- **401 Unauthorized**: Authentication issues
- **429 Rate Limited**: Too many requests

### Network Issues
- **Connection Timeout**: Slow network responses
- **Connection Lost**: Network interruptions
- **Malformed Responses**: Invalid API data

### User Input Errors
- **Validation Failures**: Invalid form data
- **Constraint Violations**: Business rule violations
- **Duplicate Actions**: Repeated operations

## Maintenance and Updates

### Adding New Tests

1. **Identify Feature**: Define new buddy system functionality
2. **Update Mock Data**: Extend `BuddyHelper` with new data types
3. **Add Selectors**: Update `BuddyPage` with new UI elements
4. **Write Tests**: Create test cases following existing patterns
5. **Update Documentation**: Maintain this README

### Test Data Updates

When buddy system schemas change:

1. Update `MockBuddyData` interface in `buddy.helper.ts`
2. Modify `generateMockBuddyData()` method
3. Update API route mocking
4. Verify test compatibility

### Page Object Maintenance

When UI components change:

1. Update selectors in `BuddyPage.ts`
2. Add new interaction methods
3. Update existing method implementations
4. Test selector reliability

## Troubleshooting

### Common Issues

1. **Test Timeouts**: Increase timeout values or optimize loading
2. **Selector Failures**: Update selectors for UI changes
3. **Mock Data Misalignment**: Sync mock data with API schemas
4. **Performance Failures**: Optimize test execution or adjust thresholds

### Debug Strategies

1. **Screenshots**: Capture test execution state
2. **Video Recording**: Record test runs for analysis
3. **Console Logs**: Monitor browser console output
4. **Network Monitoring**: Track API call patterns
5. **Performance Profiling**: Identify bottlenecks

### Environment Issues

- **Port Conflicts**: Ensure services run on correct ports
- **Database State**: Reset test data between runs
- **Authentication**: Verify test user credentials
- **Service Dependencies**: Check all required services are running

## Continuous Integration

The tests integrate with CI/CD pipelines:

### GitHub Actions

```yaml
- name: Run Buddy System E2E Tests
  run: npx playwright test tests/buddy/
  env:
    E2E_API_BASE_URL: ${{ secrets.TEST_API_URL }}
```

### Test Reports

- **HTML Reports**: Visual test execution results
- **JUnit XML**: Integration with CI reporting
- **JSON Reports**: Machine-readable test data
- **Screenshots**: Visual failure documentation

## Security Considerations

Tests validate security requirements:

- **Data Privacy**: Personal information protection
- **Message Encryption**: Secure communication channels
- **Access Control**: Proper authorization checks
- **Input Sanitization**: XSS and injection prevention
- **Audit Logging**: User action tracking

## Compliance and Standards

The test suite ensures compliance with:

- **GDPR**: Data protection and user rights
- **WCAG 2.1 AA**: Web accessibility standards
- **OWASP**: Security best practices
- **ISO 27001**: Information security management
- **SOC 2**: Service organization controls

## Contributing

When contributing to buddy system tests:

1. **Follow Patterns**: Use established test patterns
2. **Mock Appropriately**: Don't rely on external services
3. **Test Thoroughly**: Cover happy path and edge cases
4. **Document Changes**: Update this README
5. **Performance Focus**: Maintain fast test execution

## Future Enhancements

Planned test improvements:

- **AI-Powered Matching**: Tests for ML-based buddy selection
- **Video Calling**: WebRTC communication testing
- **Mobile App**: Native app test integration
- **Advanced Analytics**: Detailed performance metrics
- **Integration Testing**: Cross-service validation

## Contact and Support

For questions about the buddy system E2E tests:

- **Development Team**: FocusHive developers
- **QA Team**: Quality assurance specialists
- **DevOps Team**: CI/CD pipeline support
- **Documentation**: This README and inline comments