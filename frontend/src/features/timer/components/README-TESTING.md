# Timer Component Test Suite Documentation

This document provides a comprehensive overview of the test suite created for the FocusHive Timer components, including test coverage, patterns used, and how to run the tests.

## Test Files Overview

### 1. FocusTimer.test.tsx
**Main component integration tests**

- **File**: `/src/features/timer/components/FocusTimer.test.tsx`
- **Lines of Code**: 1,000+
- **Test Categories**: 10 describe blocks
- **Total Tests**: 47 test cases

**Coverage Areas:**
- Component rendering (compact/full modes)
- Timer functionality (start, pause, resume, stop, skip)
- Time display and progress calculation
- Settings menu integration
- Session management (creation, goals, distractions)
- Fullscreen mode toggle
- Accessibility features
- Sound and notification handling
- Edge cases and error scenarios
- Props callbacks (onSessionStart, onSessionEnd)

### 2. CircularTimer.test.tsx
**Sub-component focused tests**

- **File**: `/src/features/timer/components/CircularTimer.test.tsx`
- **Lines of Code**: 600+
- **Test Categories**: 7 describe blocks
- **Total Tests**: 28 test cases

**Coverage Areas:**
- SVG rendering and circle elements
- Time formatting (various formats and edge cases)
- Progress calculation accuracy
- Phase display (focus, short-break, long-break, idle)
- Visual styling and colors
- Responsive design with different sizes
- Accessibility considerations
- Edge cases (negative time, large numbers)

### 3. TimerSettingsMenu.test.tsx
**Settings component tests**

- **File**: `/src/features/timer/components/TimerSettingsMenu.test.tsx`
- **Lines of Code**: 500+
- **Test Categories**: 7 describe blocks
- **Total Tests**: 20 test cases

**Coverage Areas:**
- Menu rendering and visibility
- Sound setting toggle functionality
- Notification setting toggle functionality
- Menu interaction (open/close)
- Settings persistence in localStorage
- Accessibility with ARIA roles
- Keyboard navigation support
- Edge cases (rapid clicking, corrupt data)

### 4. TimerContext.test.tsx
**Context and state management integration tests**

- **File**: `/src/features/timer/contexts/TimerContext.test.tsx`
- **Lines of Code**: 800+
- **Test Categories**: 9 describe blocks
- **Total Tests**: 35 test cases

**Coverage Areas:**
- Initial state and default settings
- Complete timer lifecycle (start → pause → resume → stop)
- Session management (creation, goals, distractions, end)
- Settings management and persistence
- WebSocket integration and event handling
- Audio context and notification setup
- Cleanup and memory management
- Error handling scenarios
- Edge cases and boundary conditions

### 5. FocusTimer.performance.test.tsx
**Performance and memory leak prevention tests**

- **File**: `/src/features/timer/components/FocusTimer.performance.test.tsx`
- **Lines of Code**: 400+
- **Test Categories**: 6 describe blocks
- **Total Tests**: 18 test cases

**Coverage Areas:**
- Timer interval management (creation/cleanup)
- Memory leak prevention
- Performance benchmarks
- Timer accuracy over extended periods
- Resource usage optimization
- Concurrent instance handling

## Test Patterns and Best Practices

### 1. Testing Utilities Used
```typescript
import { 
  renderWithProviders, 
  screen, 
  userEvent, 
  waitFor,
  act 
} from '../../../test-utils/test-utils'
```

### 2. Mocking Strategy
- **WebSocket Context**: Mocked with emit/on functions
- **Presence Context**: Mocked with updatePresence function
- **Web Audio API**: Fully mocked AudioContext and related objects
- **Notification API**: Mocked browser notifications
- **Timer Functions**: Using vi.useFakeTimers() for controlled time advancement

### 3. Test Component Patterns
```typescript
// Test component to access context values
const TimerStateInspector: React.FC = () => {
  const timer = useTimer()
  return (
    <div data-testid="timer-inspector">
      <div data-testid="current-phase">{timer.timerState.currentPhase}</div>
      // ... other state values
    </div>
  )
}

// Wrapper component with providers
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <TimerProvider userId={mockUser.id}>
      {children}
    </TimerProvider>
  )
}
```

### 4. Async Testing Patterns
```typescript
// User interactions
const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
await act(async () => {
  await user.click(playButton)
})

// Time advancement
act(() => {
  vi.advanceTimersByTime(1000) // Advance 1 second
})

// State changes
await waitFor(() => {
  expect(screen.getByTestId('is-running')).toHaveTextContent('true')
})
```

## Key Testing Scenarios

### Timer Functionality Tests
1. **Start Timer**: Verifies session creation, WebSocket events, presence updates
2. **Pause/Resume**: Tests state transitions and interval management
3. **Stop Timer**: Confirms cleanup and state reset
4. **Skip Phase**: Tests phase completion handling
5. **Time Countdown**: Verifies accurate time decrementation

### Session Management Tests
1. **Session Creation**: Only for focus sessions, not breaks
2. **Distraction Tracking**: Increment counter and WebSocket events
3. **Goal Management**: Add, complete, remove goals with persistence
4. **Session End**: Productivity rating and cleanup

### Settings Tests
1. **Sound Toggle**: Audio context management and sound playback
2. **Notification Toggle**: Browser notification permissions
3. **Persistence**: LocalStorage save/load functionality
4. **Menu Interaction**: Open/close behavior and accessibility

### Performance Tests
1. **Interval Management**: Single interval creation, proper cleanup
2. **Memory Leaks**: Audio context closure, event listener cleanup
3. **Timer Accuracy**: Precision over extended periods
4. **Resource Usage**: DOM node count, localStorage efficiency

## Running the Tests

### Individual Test Files
```bash
# Run main component tests
npm test FocusTimer.test.tsx

# Run sub-component tests
npm test CircularTimer.test.tsx
npm test TimerSettingsMenu.test.tsx

# Run context tests
npm test TimerContext.test.tsx

# Run performance tests
npm test FocusTimer.performance.test.tsx
```

### All Timer Tests
```bash
# Run all timer-related tests
npm test -- --testPathPattern=timer

# Run with coverage
npm test -- --coverage --testPathPattern=timer
```

### Watch Mode
```bash
# Watch for changes
npm test -- --watch timer
```

## Coverage Expectations

### Component Coverage
- **Statements**: >95%
- **Branches**: >90%
- **Functions**: >95%
- **Lines**: >95%

### Functionality Coverage
- ✅ Timer start/stop/pause/resume
- ✅ Time display and formatting
- ✅ Phase transitions
- ✅ Session management
- ✅ Settings persistence
- ✅ WebSocket integration
- ✅ Audio/notification handling
- ✅ Accessibility features
- ✅ Error scenarios
- ✅ Performance optimization

## Test Maintenance

### Adding New Tests
1. Follow existing patterns for consistency
2. Use descriptive test names following AAA pattern
3. Mock external dependencies appropriately
4. Include both positive and negative test cases
5. Test accessibility features

### Updating Existing Tests
1. Update mocks when component APIs change
2. Maintain test isolation between test cases
3. Update assertions when behavior changes
4. Keep performance thresholds realistic

### Best Practices
1. **Test Behavior, Not Implementation**: Focus on what the user experiences
2. **Use Real User Events**: Prefer userEvent over fireEvent
3. **Proper Cleanup**: Ensure tests clean up timers and resources
4. **Meaningful Assertions**: Test actual functionality, not just presence
5. **Edge Cases**: Include boundary conditions and error scenarios

## Troubleshooting

### Common Issues
1. **Timer Tests Failing**: Ensure vi.useFakeTimers() is properly set up
2. **Async Issues**: Use waitFor() for state changes and act() for user events
3. **Memory Leaks**: Check that intervals and audio contexts are cleaned up
4. **Mock Issues**: Verify mock implementations match real API behavior

### Performance Test Issues
1. **Timing Sensitivity**: Performance tests may be sensitive to CI environment
2. **Threshold Adjustments**: May need to adjust time/memory thresholds for different environments
3. **Resource Cleanup**: Ensure proper cleanup between performance test runs

## Future Test Enhancements

### Potential Additions
1. **Visual Regression Tests**: Screenshot-based testing for UI consistency
2. **Accessibility Automation**: Automated a11y testing with jest-axe
3. **Cross-Browser Tests**: Playwright integration for browser compatibility
4. **Load Testing**: Stress testing with multiple concurrent sessions
5. **Integration Tests**: Full workflow tests with backend services

### Test Optimization
1. **Parallel Execution**: Configure tests to run in parallel for faster feedback
2. **Test Categorization**: Group tests by speed (unit/integration/e2e)
3. **Selective Testing**: Run only relevant tests based on code changes
4. **Coverage Reporting**: Enhanced coverage reports with visual indicators

This test suite provides comprehensive coverage of the Timer component functionality while maintaining good performance and following React testing best practices.