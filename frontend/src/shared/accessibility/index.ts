/**
 * FocusHive Accessibility System
 * 
 * Comprehensive WCAG 2.1 AA compliant accessibility system
 * providing components, hooks, utilities, and testing tools.
 */

// Components
export * from './components';

// Hooks
export * from './hooks/useAnnouncement';
export * from './hooks/useFocusTrap';
export * from './hooks/useKeyboardNavigation';
export * from './hooks/useLiveRegion';

// Utilities
export * from './utils/colorContrast';
export * from './utils/focusManagement';
export * from './utils/ariaUtils';

// Constants
export * from './constants/wcag';

// Types
export * from './types/accessibility';

// Theme
export * from './theme/accessibleTheme';

// Patterns
export * from './patterns/KeyboardPatterns';

// Testing
export * from './testing/accessibilityTestUtils';

// Default exports for convenience
export { default as AccessibilityTester } from './testing/accessibilityTestUtils';
export { default as KeyboardPatterns } from './patterns/KeyboardPatterns';