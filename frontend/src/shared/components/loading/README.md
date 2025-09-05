# Loading Components

Comprehensive loading state components for FocusHive, built with Material UI and designed for optimal user experience.

## Overview

This package provides a complete set of loading indicators, skeleton screens, and loading-aware components that integrate seamlessly with FocusHive's design system and API patterns.

## Components

### LoadingSpinner

Flexible spinner component for various loading scenarios.

```tsx
import { LoadingSpinner } from '@shared/components/Loading'

// Basic spinner
<LoadingSpinner />

// With text
<LoadingSpinner text="Loading data..." />

// Inline usage
<span>Processing <LoadingSpinner inline size={16} /> please wait...</span>

// Centered with custom size
<LoadingSpinner centered size={60} color="primary" />
```

**Props:**
- `text?: string` - Optional loading text
- `centered?: boolean` - Center the spinner (default: true)
- `inline?: boolean` - Inline display (default: false)
- `size?: number` - Spinner size (default: 40)
- All Material UI `CircularProgressProps`

### LoadingBackdrop

Full-screen loading overlay for major operations.

```tsx
import { LoadingBackdrop } from '@shared/components/Loading'

// Simple backdrop
<LoadingBackdrop 
  open={loading} 
  text="Processing..." 
/>

// With progress
<LoadingBackdrop
  open={loading}
  variant="detailed"
  text="Uploading files..."
  progress={75}
/>
```

**Props:**
- `open: boolean` - Controls backdrop visibility
- `text?: string` - Loading message (default: "Loading...")
- `progress?: number` - Progress value 0-100
- `variant?: 'simple' | 'detailed'` - Display style (default: 'simple')
- All Material UI `BackdropProps`

### LoadingSkeleton

Flexible skeleton placeholder for content.

```tsx
import { LoadingSkeleton } from '@shared/components/Loading'

// Basic skeleton
<LoadingSkeleton lines={3} />

// With avatar and actions
<LoadingSkeleton
  lines={4}
  avatar={true}
  actions={true}
  animation="wave"
/>

// Custom dimensions
<LoadingSkeleton
  variant="rectangular"
  width={300}
  height={200}
/>
```

**Props:**
- `lines?: number` - Number of text lines (default: 3)
- `avatar?: boolean` - Show avatar placeholder (default: false)
- `actions?: boolean` - Show action buttons placeholder (default: false)
- All Material UI `SkeletonProps`

### LoadingButton

Button with integrated loading state using Material UI v6.4.0+ features.

```tsx
import { LoadingButton } from '@shared/components/Loading'

// Basic loading button
<LoadingButton
  loading={isSubmitting}
  onClick={handleSubmit}
>
  Submit
</LoadingButton>

// With custom loading text and icon
<LoadingButton
  loading={isUploading}
  loadingText="Uploading..."
  startIcon={<UploadIcon />}
  variant="contained"
  color="primary"
>
  Upload File
</LoadingButton>

// Different loading positions
<LoadingButton
  loading={true}
  loadingPosition="end"
  endIcon={<SendIcon />}
>
  Send Message
</LoadingButton>
```

**Props:**
- `loading?: boolean` - Loading state (default: false)
- `loadingText?: string` - Text during loading
- `loadingPosition?: 'start' | 'center' | 'end'` - Indicator position (default: 'center')
- All Material UI `ButtonProps`

### ContentSkeleton

Pre-built skeletons for specific content types.

```tsx
import { ContentSkeleton } from '@shared/components/Loading'

// Card skeleton
<ContentSkeleton type="card" count={3} />

// Hive-specific skeleton
<ContentSkeleton type="hive" count={5} animation="wave" />

// Chat skeleton
<ContentSkeleton type="chat" count={4} />

// Form skeleton
<ContentSkeleton type="form" />
```

**Props:**
- `type: 'card' | 'list' | 'form' | 'table' | 'chat' | 'hive'` - Content type
- `count?: number` - Number of items (default: 1)
- `animation?: 'pulse' | 'wave' | false` - Animation type (default: 'wave')

### TableSkeleton

Specialized skeleton for data tables.

```tsx
import { TableSkeleton } from '@shared/components/Loading'

// Basic table skeleton
<TableSkeleton rows={5} columns={4} />

// With header and actions
<TableSkeleton
  rows={8}
  columns={6}
  showHeader={true}
  showActions={true}
/>
```

**Props:**
- `rows?: number` - Number of rows (default: 5)
- `columns?: number` - Number of columns (default: 4)
- `showHeader?: boolean` - Show header row (default: true)
- `showActions?: boolean` - Show actions column (default: true)

## Hooks Integration

### useAsync Hook

The loading components work seamlessly with the `useAsync` hook for API calls:

```tsx
import { useAsync } from '@shared/hooks'
import { LoadingSpinner, LoadingButton } from '@shared/components/Loading'

const MyComponent = () => {
  const { data, isLoading, error, execute } = useAsync(
    async () => {
      const response = await api.fetchData()
      return response.data
    }
  )

  if (isLoading) {
    return <LoadingSpinner text="Fetching data..." />
  }

  return (
    <div>
      {data && <div>{data.message}</div>}
      <LoadingButton
        loading={isLoading}
        onClick={() => execute()}
      >
        Refresh Data
      </LoadingButton>
    </div>
  )
}
```

### useAsyncSubmit Hook

For form submissions and actions:

```tsx
import { useAsyncSubmit } from '@shared/hooks'
import { LoadingButton } from '@shared/components/Loading'

const FormComponent = () => {
  const { loading, error, submit } = useAsyncSubmit(
    async (formData) => {
      await api.submitForm(formData)
    },
    {
      onSuccess: () => console.log('Form submitted!'),
      onError: (error) => console.error('Submission failed:', error)
    }
  )

  const handleSubmit = (event) => {
    event.preventDefault()
    const formData = new FormData(event.target)
    submit(Object.fromEntries(formData))
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* form fields */}
      <LoadingButton
        type="submit"
        loading={loading}
        loadingText="Submitting..."
        variant="contained"
      >
        Submit Form
      </LoadingButton>
      {error && <Alert severity="error">{error}</Alert>}
    </form>
  )
}
```

## Usage Patterns

### API Data Fetching

```tsx
// Loading state for data fetching
const UserList = () => {
  const { data: users, isLoading } = useAsyncData(() => api.getUsers())

  if (isLoading) {
    return <ContentSkeleton type="list" count={5} />
  }

  return (
    <div>
      {users?.map(user => <UserCard key={user.id} user={user} />)}
    </div>
  )
}
```

### Form Submissions

```tsx
// Loading state for form submission
const LoginForm = () => {
  const { loading, submit } = useAsyncSubmit(api.login)

  return (
    <form onSubmit={(e) => submit(getFormData(e))}>
      <TextField name="email" disabled={loading} />
      <TextField name="password" type="password" disabled={loading} />
      <LoadingButton
        type="submit"
        loading={loading}
        loadingText="Signing In..."
        startIcon={<LoginIcon />}
        fullWidth
      >
        Sign In
      </LoadingButton>
    </form>
  )
}
```

### Progressive Loading

```tsx
// Multi-step loading process
const FileUpload = () => {
  const [progress, setProgress] = useState(0)
  const [uploading, setUploading] = useState(false)

  const handleUpload = async (file) => {
    setUploading(true)
    // Upload with progress tracking
    await api.uploadFile(file, (progress) => setProgress(progress))
    setUploading(false)
  }

  return (
    <div>
      <LoadingButton
        loading={uploading}
        loadingText={`Uploading... ${progress}%`}
        onClick={() => handleUpload(selectedFile)}
      >
        Upload File
      </LoadingButton>
      
      <LoadingBackdrop
        open={uploading}
        variant="detailed"
        text="Uploading your file..."
        progress={progress}
      />
    </div>
  )
}
```

## Best Practices

1. **Use appropriate loading indicators**: Spinners for quick operations, skeletons for content loading, backdrops for major operations

2. **Provide meaningful feedback**: Always include descriptive text with loading states

3. **Handle loading states consistently**: Use the same patterns across similar operations

4. **Disable interactions during loading**: Prevent users from triggering multiple operations

5. **Show progress when possible**: For operations with measurable progress, show completion percentage

6. **Graceful error handling**: Always handle and display errors appropriately

7. **Responsive design**: Ensure loading states work well on all screen sizes

8. **Accessibility**: Loading states should be announced to screen readers

## Performance Considerations

- Loading components are optimized for performance with minimal re-renders
- Skeleton animations use CSS transforms for smooth 60fps animations
- Backdrop components use React Portal for optimal z-index management
- All components are tree-shakeable for minimal bundle impact

## Browser Support

Loading components work in all modern browsers and gracefully degrade in older browsers:
- Chrome 60+
- Firefox 60+
- Safari 12+
- Edge 79+

## Migration Guide

If migrating from existing loading patterns:

1. Replace `<CircularProgress />` with `<LoadingSpinner />`
2. Replace manual button loading states with `<LoadingButton />`
3. Replace custom skeletons with `<ContentSkeleton />` or `<TableSkeleton />`
4. Use `useAsync` hooks instead of manual loading state management

## Demo

See `LoadingStatesDemo.tsx` for a comprehensive demo of all loading components and patterns.