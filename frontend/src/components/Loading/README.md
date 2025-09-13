# Loading Components System

A comprehensive loading state management system for FocusHive React application. This system provides consistent loading indicators, skeleton screens, and form loading states to improve user experience during async operations.

## Features

- ✅ **Multiple Loading Patterns**: Spinners, skeletons, overlays, form wrappers
- ✅ **State Management**: Advanced hooks for concurrent loading operations
- ✅ **Form Integration**: Disable interactions and prevent multiple submissions
- ✅ **Accessibility**: ARIA-compliant loading indicators
- ✅ **TypeScript**: Full type safety with proper interfaces
- ✅ **Testing**: Comprehensive test coverage
- ✅ **Performance**: Optimized with proper memoization

## Components Overview

### 1. useLoadingState Hook

Manages multiple concurrent loading operations with error and success states.

```tsx
import { useLoadingState } from '@/components/Loading';

function MyComponent() {
  const {
    isLoading,
    setLoading,
    setError,
    setSuccess,
    isOperationLoading,
    hasError,
    clearState
  } = useLoadingState();

  const handleLogin = async () => {
    setLoading('login', true);
    try {
      await loginAPI();
      setSuccess('login', true);
    } catch (error) {
      setError('login', error);
    } finally {
      setLoading('login', false);
    }
  };

  return (
    <div>
      {isOperationLoading('login') && <span>Logging in...</span>}
      <button onClick={handleLogin}>Login</button>
    </div>
  );
}
```

### 2. LoadingSpinner

Versatile loading spinner with multiple sizes and variants.

```tsx
import { LoadingSpinner, InlineSpinner, CenteredSpinner, OverlaySpinner } from '@/components/Loading';

// Basic usage
<LoadingSpinner size="medium" message="Loading..." />

// Variants
<InlineSpinner message="Processing..." />
<CenteredSpinner size="large" message="Loading hives..." />
<OverlaySpinner overlay message="Saving changes..." />

// With custom styling
<LoadingSpinner 
  size="large"
  customColor="#ff5722"
  minDisplayTime={500}
  message="Please wait..."
/>
```

### 3. SkeletonLoader

Skeleton screens for better perceived performance.

```tsx
import { SkeletonLoader, HiveCardSkeleton, UserProfileSkeleton } from '@/components/Loading';

// Predefined skeletons
<HiveCardSkeleton count={6} />
<UserProfileSkeleton />
<ChatMessageSkeleton count={10} />

// Custom skeletons
<SkeletonLoader variant="text" lines={3} />
<SkeletonLoader variant="list" count={5} />
<SkeletonLoader variant="card" count={3} />
```

### 4. LoadingOverlay

Full-screen loading overlays with progress support.

```tsx
import { LoadingOverlay, useLoadingOverlay } from '@/components/Loading';

function MyComponent() {
  const { isOpen, showOverlay, hideOverlay, updateProgress } = useLoadingOverlay();

  const handleUpload = async () => {
    showOverlay('Uploading files...');
    
    // Simulate progress
    for (let i = 0; i <= 100; i += 10) {
      updateProgress(i);
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    hideOverlay();
  };

  return (
    <>
      <button onClick={handleUpload}>Upload Files</button>
      <LoadingOverlay
        open={isOpen}
        message="Uploading..."
        showProgress
        progress={progress}
      />
    </>
  );
}
```

### 5. ButtonWithLoading

Buttons with integrated loading states.

```tsx
import { ButtonWithLoading, IconButtonWithLoading, FabWithLoading } from '@/components/Loading';

// Button with loading
<ButtonWithLoading
  loading={isSubmitting}
  loadingText="Saving..."
  onClick={handleSave}
  variant="contained"
>
  Save Changes
</ButtonWithLoading>

// Icon button with loading
<IconButtonWithLoading
  loading={isDeleting}
  onClick={handleDelete}
  success={deleted}
  successIcon={<CheckIcon />}
>
  <DeleteIcon />
</IconButtonWithLoading>

// FAB with loading
<FabWithLoading
  loading={isCreating}
  onClick={handleCreate}
  color="primary"
>
  <AddIcon />
</FabWithLoading>
```

### 6. FormWithLoading

Form wrapper with loading states and interaction prevention.

```tsx
import { FormWithLoading, useFormLoading } from '@/components/Loading';

function MyForm() {
  const { loading, startLoading, stopLoading, updateProgress } = useFormLoading();

  const handleSubmit = async (data) => {
    startLoading('Saving changes...');
    try {
      await saveData(data);
    } finally {
      stopLoading();
    }
  };

  return (
    <FormWithLoading
      loading={loading}
      variant="overlay"
      loadingMessage="Processing form..."
      preventMultipleSubmissions
    >
      <form onSubmit={handleSubmit}>
        {/* Form fields */}
      </form>
    </FormWithLoading>
  );
}
```

## Advanced Usage Patterns

### 1. Conditional Loading with HOCs

```tsx
import { withLoading, withSkeleton } from '@/components/Loading';

const MyComponentWithLoading = withLoading(MyComponent);
const MyComponentWithSkeleton = withSkeleton(MyComponent);

// Usage
<MyComponentWithLoading 
  isLoading={loading} 
  loadingProps={{ message: "Loading data..." }}
/>

<MyComponentWithSkeleton 
  isLoading={loading} 
  skeletonProps={{ variant: "hive-card", count: 6 }}
/>
```

### 2. Complex Form Loading States

```tsx
function ComplexForm() {
  const {
    isLoading,
    setLoading,
    isOperationLoading,
    hasError,
    setError
  } = useLoadingState();

  const handleValidation = async () => {
    setLoading('validation', true);
    try {
      await validateForm();
    } catch (error) {
      setError('validation', error);
    } finally {
      setLoading('validation', false);
    }
  };

  const handleSubmit = async () => {
    setLoading('submit', true);
    try {
      await submitForm();
    } finally {
      setLoading('submit', false);
    }
  };

  return (
    <FormWithLoading
      loading={isOperationLoading('submit')}
      variant="disable"
    >
      <form>
        <ButtonWithLoading
          loading={isOperationLoading('validation')}
          onClick={handleValidation}
        >
          Validate
        </ButtonWithLoading>
        
        <ButtonWithLoading
          loading={isOperationLoading('submit')}
          type="submit"
          onClick={handleSubmit}
        >
          Submit
        </ButtonWithLoading>
      </form>
    </FormWithLoading>
  );
}
```

### 3. Global Loading Context

```tsx
import { createContext, useContext } from 'react';
import { useLoadingState } from '@/components/Loading';

const LoadingContext = createContext();

export function LoadingProvider({ children }) {
  const loadingState = useLoadingState();
  
  return (
    <LoadingContext.Provider value={loadingState}>
      {children}
    </LoadingContext.Provider>
  );
}

export function useGlobalLoading() {
  const context = useContext(LoadingContext);
  if (!context) {
    throw new Error('useGlobalLoading must be used within LoadingProvider');
  }
  return context;
}
```

## Component Props Reference

### LoadingSpinner Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `size` | `'small' \| 'medium' \| 'large' \| 'extra-large'` | `'medium'` | Size of the spinner |
| `variant` | `'inline' \| 'overlay' \| 'centered'` | `'inline'` | Layout variant |
| `message` | `string` | - | Loading message |
| `overlay` | `boolean` | `false` | Show backdrop overlay |
| `customColor` | `string` | - | Custom color override |
| `disabled` | `boolean` | `false` | Hide component |

### SkeletonLoader Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `SkeletonVariant` | `'text'` | Type of skeleton |
| `count` | `number` | `1` | Number of items |
| `lines` | `number` | `1` | Lines for text skeleton |
| `animated` | `boolean` | `true` | Enable animation |

### FormWithLoading Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `loading` | `boolean` | `false` | Loading state |
| `variant` | `'overlay' \| 'inline' \| 'disable' \| 'progress-only'` | `'overlay'` | Loading variant |
| `loadingMessage` | `string` | `'Processing...'` | Loading message |
| `showProgress` | `boolean` | `false` | Show progress bar |
| `progress` | `number` | `0` | Progress value (0-100) |

## Best Practices

1. **Use Appropriate Loading Patterns**:
   - Spinners for quick operations (< 2 seconds)
   - Skeleton screens for content loading
   - Progress bars for file uploads/downloads
   - Overlays for form submissions

2. **Provide Meaningful Messages**:
   ```tsx
   // Good
   <LoadingSpinner message="Saving your changes..." />
   
   // Avoid
   <LoadingSpinner message="Loading..." />
   ```

3. **Handle Error States**:
   ```tsx
   const { setLoading, setError, hasError, getError } = useLoadingState();
   
   if (hasError('operation')) {
     return <Alert severity="error">{getError('operation').message}</Alert>;
   }
   ```

4. **Prevent Multiple Submissions**:
   ```tsx
   <FormWithLoading
     loading={isSubmitting}
     preventMultipleSubmissions
     disableInteraction
   >
     {/* Form content */}
   </FormWithLoading>
   ```

5. **Use Minimum Display Times**:
   ```tsx
   <LoadingSpinner minDisplayTime={500} /> // Prevent flash
   ```

## Testing

The loading components include comprehensive test coverage:

```bash
# Run loading component tests
npm test -- src/components/Loading/__tests__

# Run specific tests
npm test -- src/components/Loading/__tests__/LoadingSpinner.test.tsx
npm test -- src/components/Loading/__tests__/useLoadingState.test.ts
```

## Integration Examples

### 1. API Hook Integration

```tsx
import { useQuery } from '@tanstack/react-query';
import { HiveCardSkeleton } from '@/components/Loading';

function HiveList() {
  const { data: hives, isLoading, error } = useQuery({
    queryKey: ['hives'],
    queryFn: fetchHives
  });

  if (isLoading) {
    return <HiveCardSkeleton count={6} />;
  }

  if (error) {
    return <Alert severity="error">Failed to load hives</Alert>;
  }

  return (
    <Grid container spacing={2}>
      {hives.map(hive => (
        <Grid item xs={12} sm={6} md={4} key={hive.id}>
          <HiveCard hive={hive} />
        </Grid>
      ))}
    </Grid>
  );
}
```

### 2. Router Integration

```tsx
import { Suspense } from 'react';
import { CenteredSpinner } from '@/components/Loading';

function App() {
  return (
    <Suspense fallback={<CenteredSpinner message="Loading page..." />}>
      <Routes>
        <Route path="/hives" element={<HivesPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Routes>
    </Suspense>
  );
}
```

## Performance Considerations

- Components use `React.memo` for performance optimization
- Hooks include proper dependency arrays
- Animations are optimized for 60fps
- Large lists use skeleton screens instead of blocking
- Loading states include debouncing for fast operations

## Accessibility

All loading components include proper accessibility features:
- ARIA labels and roles
- Screen reader announcements
- Keyboard navigation support
- Focus management during loading states
- High contrast support

## Migration Guide

If upgrading from existing loading components:

1. Replace individual spinners with `LoadingSpinner`
2. Use `FormWithLoading` for all form submissions
3. Implement skeleton screens for content loading
4. Migrate to `useLoadingState` for complex operations

For more examples and advanced usage, see the component test files and integration examples in the codebase.