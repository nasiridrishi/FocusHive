# Accessibility System Documentation

This comprehensive accessibility system provides WCAG 2.1 AA compliant components, utilities, and
patterns for the FocusHive application.

## Table of Contents

- [Overview](#overview)
- [Components](#components)
- [Utilities](#utilities)
- [Hooks](#hooks)
- [Testing](#testing)
- [Patterns](#patterns)
- [Usage Examples](#usage-examples)
- [WCAG Compliance](#wcag-compliance)
- [Best Practices](#best-practices)

## Overview

The accessibility system is designed to:

- Ensure WCAG 2.1 AA compliance across all UI components
- Provide comprehensive screen reader support
- Implement proper keyboard navigation patterns
- Maintain color contrast standards (4.5:1 for normal text, 3:1 for large text)
- Support high contrast mode and reduced motion preferences
- Enable focus management and trapping for modals
- Provide live regions for dynamic content announcements

## Components

### Base Components

#### ScreenReaderOnly

Visually hides content while keeping it available to screen readers.

```tsx
import { ScreenReaderOnly } from '@/shared/accessibility/components';

<ScreenReaderOnly>
  Additional context for screen readers
</ScreenReaderOnly>
```

#### SkipNavigation

Provides skip links for keyboard users to navigate quickly to main content areas.

```tsx
import { SkipNavigation } from '@/shared/accessibility/components';

<SkipNavigation 
  targets={[
    { id: 'main', label: 'Skip to main content' },
    { id: 'navigation', label: 'Skip to navigation' }
  ]} 
/>
```

### Form Components

#### AccessibleButton

Enhanced button with loading states, confirmation, and proper ARIA attributes.

```tsx
import { AccessibleButton } from '@/shared/accessibility/components';

<AccessibleButton
  loading={isLoading}
  loadingText="Saving..."
  successText="Saved successfully"
  requiresConfirmation={true}
  confirmationText="Are you sure?"
  onClick={handleSave}
>
  Save Document
</AccessibleButton>
```

#### AccessibleTextField

Text field with validation announcements and character counting.

```tsx
import { AccessibleTextField } from '@/shared/accessibility/components';

<AccessibleTextField
  label="Email Address"
  error={hasError}
  errorMessage="Please enter a valid email"
  instructions="We'll never share your email"
  maxLength={100}
  showCharacterCount={true}
  announceValidation={true}
/>
```

#### AccessibleSelect

Select dropdown with proper ARIA relationships.

```tsx
import { AccessibleSelect } from '@/shared/accessibility/components';

<AccessibleSelect
  label="Choose a category"
  options={[
    { value: 'work', label: 'Work Tasks' },
    { value: 'personal', label: 'Personal Tasks' }
  ]}
  instructions="Select the category that best describes your task"
/>
```

### Modal Components

#### AccessibleModal

Modal with focus trapping and proper ARIA attributes.

```tsx
import { AccessibleModal } from '@/shared/accessibility/components';

<AccessibleModal
  open={isOpen}
  title="Edit Profile"
  description="Update your profile information"
  onClose={handleClose}
  autoFocusSelector="#first-name"
  returnFocusSelector="#edit-button"
>
  <form>...</form>
</AccessibleModal>
```

### Navigation Components

#### AccessibleNavigation

Navigation with keyboard support and ARIA relationships.

```tsx
import { AccessibleNavigation } from '@/shared/accessibility/components';

<AccessibleNavigation
  items={[
    { id: 'dashboard', label: 'Dashboard', href: '/dashboard', current: true },
    { id: 'tasks', label: 'Tasks', href: '/tasks', badge: 5 }
  ]}
  onItemClick={handleNavigation}
/>
```

#### AccessibleBreadcrumbs

Breadcrumb navigation with proper ARIA labels.

```tsx
import { AccessibleBreadcrumbs } from '@/shared/accessibility/components';

<AccessibleBreadcrumbs
  items={[
    { label: 'Home', href: '/' },
    { label: 'Projects', href: '/projects' },
    { label: 'Current Project', current: true }
  ]}
  showHomeIcon={true}
/>
```

## Utilities

### Color Contrast

Utilities for checking WCAG color contrast compliance.

```typescript
import { calculateContrastRatio, meetsContrastRequirement } from '@/shared/accessibility/utils/colorContrast';

const ratio = calculateContrastRatio('#000000', '#ffffff'); // 21:1
const isCompliant = meetsContrastRequirement('#333333', '#ffffff', 'AA', 'normal'); // true
```

### Focus Management

Utilities for managing focus and creating focus traps.

```typescript
import { FocusTrap, FocusManager } from '@/shared/accessibility/utils/focusManagement';

const focusTrap = new FocusTrap({
  element: modalElement,
  initialFocus: '#first-input',
  returnFocus: '#open-modal-button'
});

focusTrap.activate();
```

### ARIA Utilities

Utilities for screen reader announcements and ARIA management.

```typescript
import { LiveRegionManager, generateId } from '@/shared/accessibility/utils/ariaUtils';

const liveRegion = new LiveRegionManager();
liveRegion.announcePolite('Task completed successfully');

const uniqueId = generateId('form-field'); // 'form-field-123'
```

## Hooks

### useFocusTrap

Hook for implementing focus traps in modals and overlays.

```tsx
import { useFocusTrap } from '@/shared/accessibility/hooks/useFocusTrap';

function Modal({ isOpen, onClose }) {
  const { focusTrapProps } = useFocusTrap({
    isActive: isOpen,
    onDeactivate: onClose,
    initialFocus: '#first-input'
  });

  return (
    <div {...focusTrapProps}>
      {/* Modal content */}
    </div>
  );
}
```

### useAnnouncement

Hook for screen reader announcements.

```tsx
import { useAnnouncement } from '@/shared/accessibility/hooks/useAnnouncement';

function TaskManager() {
  const { announcePolite, announceAssertive } = useAnnouncement();

  const handleTaskComplete = () => {
    announcePolite('Task completed successfully');
  };

  const handleError = () => {
    announceAssertive('Error: Unable to save task');
  };
}
```

### useKeyboardNavigation

Hook for implementing keyboard navigation patterns.

```tsx
import { useKeyboardNavigation } from '@/shared/accessibility/hooks/useKeyboardNavigation';

function Menu() {
  const { keyboardNavigationProps, focusedIndex } = useKeyboardNavigation({
    orientation: 'vertical',
    wrap: true,
    activateOnFocus: false
  });

  return (
    <div role="menu" {...keyboardNavigationProps}>
      {/* Menu items */}
    </div>
  );
}
```

### useLiveRegion

Hook for managing ARIA live regions.

```tsx
import { useLiveRegion } from '@/shared/accessibility/hooks/useLiveRegion';

function StatusUpdater() {
  const { liveRegionProps, announce } = useLiveRegion({
    level: 'polite',
    atomic: true
  });

  return (
    <div {...liveRegionProps}>
      {message}
    </div>
  );
}
```

## Testing

### Accessibility Test Utilities

Comprehensive testing utilities for validating WCAG compliance.

```typescript
import { AccessibilityTester, setupAccessibilityTests } from '@/shared/accessibility/testing';

// Setup accessibility tests
setupAccessibilityTests();

// Test component accessibility
test('component should be accessible', async () => {
  const { container } = render(<MyComponent />);
  const user = userEvent.setup();
  
  const testSuite = await AccessibilityTester.runAccessibilityTests(container, user);
  expect(testSuite.passed).toBe(true);
});

// Custom matchers
test('button should have valid contrast', () => {
  const { getByRole } = render(<Button>Click me</Button>);
  const button = getByRole('button');
  expect(button).toHaveValidContrast();
});
```

### Test Categories

1. **Color Contrast Tests**
    - Text contrast ratios
    - Focus indicator contrast
    - Interactive element contrast

2. **Focus Management Tests**
    - Tab order verification
    - Focus trap functionality
    - Focus restoration

3. **ARIA Tests**
    - Label relationships
    - Live region functionality
    - Role assignments

4. **Keyboard Navigation Tests**
    - Arrow key navigation
    - Enter/Space activation
    - Escape key handling

5. **Touch Target Tests**
    - Minimum size requirements (44px)
    - Target spacing

## Patterns

### Complex Keyboard Navigation

Advanced patterns for data grids, tree views, and carousels.

```tsx
import { AccessibleDataGrid, AccessibleTreeView } from '@/shared/accessibility/patterns/KeyboardPatterns';

// Data grid with keyboard navigation
<AccessibleDataGrid
  data={tableData}
  columns={columns}
  selectionMode="multiple"
  onSelectionChange={handleSelection}
/>

// Tree view with expandable nodes
<AccessibleTreeView
  data={treeData}
  selectionMode="single"
  onNodeSelect={handleNodeSelect}
  onNodeToggle={handleNodeToggle}
/>
```

## Usage Examples

### Complete Form Example

```tsx
import {
  AccessibleTextField,
  AccessibleSelect,
  AccessibleCheckbox,
  AccessibleButton,
  FormValidationSummary
} from '@/shared/accessibility/components';

function UserProfileForm() {
  const [errors, setErrors] = useState([]);
  
  return (
    <form aria-label="User profile form">
      <FormValidationSummary 
        errors={errors}
        title="Please fix the following errors:"
      />
      
      <AccessibleTextField
        label="Full Name"
        required
        error={hasNameError}
        errorMessage="Name is required"
      />
      
      <AccessibleSelect
        label="Department"
        options={departments}
        instructions="Select your primary department"
      />
      
      <AccessibleCheckbox
        label="Receive email notifications"
        description="Get notified about important updates"
      />
      
      <AccessibleButton
        type="submit"
        loading={isSubmitting}
        loadingText="Saving profile..."
        successText="Profile saved successfully"
      >
        Save Profile
      </AccessibleButton>
    </form>
  );
}
```

### Modal with Focus Management

```tsx
import { AccessibleModal, AccessibleButton } from '@/shared/accessibility/components';
import { useFocusTrap } from '@/shared/accessibility/hooks/useFocusTrap';

function EditTaskModal({ task, isOpen, onClose, onSave }) {
  return (
    <AccessibleModal
      open={isOpen}
      title="Edit Task"
      description="Modify task details and settings"
      onClose={onClose}
      autoFocusSelector="#task-title"
      showCloseButton={true}
    >
      <form onSubmit={handleSave}>
        <AccessibleTextField
          id="task-title"
          label="Task Title"
          defaultValue={task.title}
          required
        />
        
        <AccessibleTextField
          label="Description"
          multiline
          rows={4}
          defaultValue={task.description}
        />
        
        <div style={{ marginTop: 16, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <AccessibleButton
            variant="outlined"
            onClick={onClose}
          >
            Cancel
          </AccessibleButton>
          
          <AccessibleButton
            type="submit"
            variant="contained"
            loading={isSaving}
            loadingText="Saving..."
            successText="Task saved successfully"
          >
            Save Task
          </AccessibleButton>
        </div>
      </form>
    </AccessibleModal>
  );
}
```

## WCAG Compliance

### Level AA Requirements Met

1. **Perceivable**
    - ✅ Color contrast ratios (4.5:1 normal text, 3:1 large text)
    - ✅ Alternative text for images and icons
    - ✅ Resizable text up to 200%
    - ✅ Color not used as the only visual means of conveying information

2. **Operable**
    - ✅ Keyboard accessible functionality
    - ✅ No seizure-inducing content
    - ✅ Sufficient time for interactions
    - ✅ Focus indicators visible and high contrast

3. **Understandable**
    - ✅ Readable text and predictable functionality
    - ✅ Input assistance and error identification
    - ✅ Consistent navigation and identification

4. **Robust**
    - ✅ Compatible with assistive technologies
    - ✅ Valid HTML and ARIA markup
    - ✅ Progressive enhancement support

### Accessibility Features

- **Screen Reader Support**: All components work with NVDA, JAWS, VoiceOver
- **Keyboard Navigation**: Full keyboard support with logical tab order
- **High Contrast Mode**: Supports Windows high contrast themes
- **Reduced Motion**: Respects prefers-reduced-motion user preference
- **Touch Accessibility**: Minimum 44px touch targets
- **Focus Management**: Visible focus indicators and focus trapping
- **Live Regions**: Dynamic content announcements
- **Error Handling**: Clear error messages and validation feedback

## Best Practices

### Component Development

1. **Always provide accessible names** for interactive elements
2. **Use semantic HTML** before adding ARIA roles
3. **Test with keyboard only** before adding mouse interactions
4. **Include focus indicators** for all interactive elements
5. **Provide context** for screen readers when visual cues are used

### Testing Guidelines

1. **Test with multiple screen readers** (NVDA, JAWS, VoiceOver)
2. **Verify keyboard navigation** in all components
3. **Check color contrast** for all text and interactive elements
4. **Test in high contrast mode** and with reduced motion
5. **Validate touch targets** meet minimum size requirements

### Implementation Checklist

- [ ] Component has proper ARIA labels and roles
- [ ] Keyboard navigation works as expected
- [ ] Color contrast meets WCAG AA standards
- [ ] Focus management is implemented correctly
- [ ] Error states are announced to screen readers
- [ ] Touch targets are at least 44px in size
- [ ] Component works in high contrast mode
- [ ] Reduced motion preferences are respected
- [ ] Component has been tested with assistive technology

### Common Patterns

1. **Form Validation**: Always announce validation results
2. **Dynamic Content**: Use live regions for updates
3. **Navigation**: Implement skip links and landmarks
4. **Modals**: Trap focus and restore on close
5. **Data Tables**: Use proper table markup and navigation

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [ARIA Authoring Practices Guide](https://www.w3.org/WAI/ARIA/apg/)
- [WebAIM Screen Reader Testing](https://webaim.org/articles/screenreader_testing/)
- [Material UI Accessibility](https://mui.com/material-ui/guides/accessibility/)

For questions or contributions to the accessibility system, please refer to the component
documentation and test suites.