# FocusHive Authentication System

A complete JWT-based authentication system for React TypeScript applications with security best practices, automatic token refresh, and comprehensive error handling.

## 🚀 Features

- **Secure JWT Authentication**: Access tokens in sessionStorage, refresh tokens in localStorage
- **Automatic Token Refresh**: Seamless token renewal on expiration
- **React Context Pattern**: Global state management with useReducer
- **Protected Routes**: Route-level authentication guards
- **HTTP Interceptors**: Automatic token attachment and error handling
- **Comprehensive Error Handling**: User-friendly error messages and states
- **TypeScript Support**: Full type safety throughout
- **Security Best Practices**: Following 2025 web security standards
- **Material UI Integration**: Consistent design system integration

## 🏗️ Architecture

### Core Components

```
src/features/auth/
├── contexts/
│   ├── AuthContext.tsx          # Main authentication context with useReducer
│   └── index.ts                 # Context exports
├── components/
│   ├── LoginForm.tsx           # Login form component (existing)
│   ├── RegisterForm.tsx        # Registration form component (existing)
│   ├── ProtectedRoute.tsx      # Route protection component (enhanced)
│   └── UserProfileMenu.tsx     # User menu component (existing)
├── pages/
│   ├── LoginPage.tsx           # Login page (updated)
│   ├── RegisterPage.tsx        # Register page (updated)
│   └── HomePage.tsx            # Home page (existing)
└── index.ts                    # Feature exports
```

### Services

```
src/services/api/
├── authApi.ts                  # Authentication API service
├── httpInterceptors.ts         # HTTP request/response interceptors
└── index.ts                    # Centralized API exports
```

## 🔧 Installation & Setup

### 1. AuthProvider Setup

Wrap your application with the AuthProvider at the root level:

```tsx
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { AuthProvider } from '@features/auth';
import App from './App';

function Root() {
  return (
    <ThemeProvider theme={theme}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}
```

### 2. Environment Configuration

Add these environment variables to your `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Route Protection

Use `ProtectedRoute` to guard your routes:

```tsx
import { ProtectedRoute, LoginPage, RegisterPage } from '@features/auth';
import DashboardPage from './pages/DashboardPage';

function App() {
  return (
    <Routes>
      {/* Public routes (redirect if authenticated) */}
      <Route path="/login" element={
        <ProtectedRoute requireAuth={false}>
          <LoginPage />
        </ProtectedRoute>
      } />
      
      <Route path="/register" element={
        <ProtectedRoute requireAuth={false}>
          <RegisterPage />
        </ProtectedRoute>
      } />
      
      {/* Protected routes (require authentication) */}
      <Route path="/dashboard" element={
        <ProtectedRoute requireAuth={true}>
          <DashboardPage />
        </ProtectedRoute>
      } />
      
      <Route path="/" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}
```

## 📋 Usage Examples

### Using Authentication Hooks

```tsx
import { useAuth } from '@features/auth';

function MyComponent() {
  const { authState, login, logout, register } = useAuth();

  // Check authentication status
  if (authState.isLoading) {
    return <div>Loading...</div>;
  }

  if (!authState.isAuthenticated) {
    return <div>Please log in</div>;
  }

  // Access user data
  const user = authState.user;
  console.log(`Welcome ${user?.firstName}!`);

  return (
    <div>
      <h1>Hello, {user?.firstName}!</h1>
      <button onClick={() => logout()}>
        Logout
      </button>
    </div>
  );
}
```

### Login Form Integration

```tsx
import { useAuth } from '@features/auth';
import { LoginRequest } from '@shared/types';

function LoginComponent() {
  const { authState, login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (credentials: LoginRequest) => {
    try {
      await login(credentials);
      navigate('/dashboard');
    } catch (error) {
      // Error is automatically handled by AuthContext
      console.error('Login failed:', error);
    }
  };

  return (
    <LoginForm 
      onSubmit={handleLogin}
      isLoading={authState.isLoading}
      error={authState.error}
    />
  );
}
```

### Making Authenticated API Calls

```tsx
import { apiClient } from '@services/api';

// The apiClient automatically includes authentication tokens
async function fetchUserData() {
  try {
    const response = await apiClient.get('/api/user/profile');
    return response.data;
  } catch (error) {
    // Token refresh is handled automatically
    console.error('API call failed:', error);
  }
}
```

## 🔐 Security Features

### Token Storage Strategy

Following 2025 security best practices:

- **Access Tokens**: Stored in `sessionStorage` (cleared on browser close)
- **Refresh Tokens**: Stored in `localStorage` (persistent across sessions)
- **Automatic Refresh**: Tokens refreshed before expiration
- **Secure Headers**: CSRF protection with correlation IDs

### Security Implementation

```typescript
// Token validation with expiration check
hasValidTokens(): boolean {
  const accessToken = this.getAccessToken();
  
  if (accessToken) {
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const currentTime = Date.now() / 1000;
      
      // Check if token expires in more than 5 minutes
      if (payload.exp && payload.exp > currentTime + 300) {
        return true;
      }
    } catch {
      // Invalid token format
    }
  }
  
  return !!this.getRefreshToken();
}
```

## 🔄 API Integration

### Automatic Token Refresh

The system automatically handles token refresh when receiving 401 responses:

```typescript
// HTTP interceptor handles token refresh automatically
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        // Refresh token and retry request
        const newTokens = await refreshToken();
        // Original request is automatically retried
        return instance(originalRequest);
      } catch (refreshError) {
        // Redirect to login on refresh failure
        tokenStorage.clearAllTokens();
        window.location.href = '/login';
      }
    }
  }
);
```

### Backend Integration

The authentication system works with the existing `/api/demo/login` endpoint:

```typescript
// Expected login response format
interface LoginResponse {
  user: User;
  token: string;        // JWT access token
  refreshToken: string; // Refresh token
}
```

## 🎯 State Management

### AuthContext State Structure

```typescript
interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  error: string | null;
}
```

### Available Actions

```typescript
// Authentication actions
const { 
  login,              // (credentials) => Promise<void>
  register,           // (userData) => Promise<void>
  logout,             // () => Promise<void>
  refreshAuth,        // () => Promise<void>
  updateProfile,      // (userData) => Promise<void>
  changePassword,     // (passwords) => Promise<void>
  requestPasswordReset, // (email) => Promise<{message}>
  clearError          // () => void
} = useAuth();
```

## 🛡️ Error Handling

### Standardized Error Responses

```typescript
interface StandardizedError extends Error {
  status: number;        // HTTP status code
  code: string;          // Error code (e.g., 'UNAUTHORIZED')
  correlationId?: string; // Request tracking ID
  originalError: AxiosError;
}
```

### Error Display

Errors are automatically displayed in forms and can be accessed via:

```tsx
const { authState } = useAuth();

if (authState.error) {
  return <Alert severity="error">{authState.error}</Alert>;
}
```

## 🔍 Debugging

### Development Logging

The system includes comprehensive logging in development mode:

```typescript
// Request logging
console.log('[HTTP Request]', { method, url, headers, data });

// Response logging  
console.log('[HTTP Response]', { status, data, correlationId });

// Error logging
console.error('[HTTP Error]', { status, error, correlationId });
```

### Correlation IDs

Each request includes a correlation ID for tracking:

```
X-Correlation-ID: 1640995200000-abc123def
```

## 🧪 Testing

### Testing Authentication Hooks

```tsx
import { renderHook, act } from '@testing-library/react';
import { useAuth } from '@features/auth';
import { AuthProvider } from '@features/auth';

function Wrapper({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}

test('should login user', async () => {
  const { result } = renderHook(() => useAuth(), { wrapper: Wrapper });
  
  await act(async () => {
    await result.current.login({
      email: 'test@example.com',
      password: 'password123'
    });
  });
  
  expect(result.current.authState.isAuthenticated).toBe(true);
});
```

### Testing Protected Routes

```tsx
import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from '@features/auth';

test('should redirect unauthenticated users', () => {
  render(
    <ProtectedRoute 
      isAuthenticated={false}
      isLoading={false}
      requireAuth={true}
    >
      <div>Protected Content</div>
    </ProtectedRoute>
  );
  
  // Should redirect to login
  expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
});
```

## 🚀 Migration Guide

### From Existing Auth Implementation

1. **Wrap app with AuthProvider**:
   ```tsx
   <AuthProvider>
     <YourApp />
   </AuthProvider>
   ```

2. **Update login pages**:
   ```tsx
   // Before
   const [isLoading, setIsLoading] = useState(false);
   
   // After
   const { authState, login } = useAuth();
   // Use authState.isLoading instead
   ```

3. **Protect routes**:
   ```tsx
   // Before
   {isAuthenticated && <DashboardPage />}
   
   // After
   <ProtectedRoute requireAuth={true}>
     <DashboardPage />
   </ProtectedRoute>
   ```

4. **Update API calls**:
   ```tsx
   // Before
   axios.get('/api/data', {
     headers: { Authorization: `Bearer ${token}` }
   });
   
   // After
   import { apiClient } from '@services/api';
   apiClient.get('/api/data'); // Token handled automatically
   ```

## 📚 API Reference

### AuthContext Methods

- `login(credentials: LoginRequest): Promise<void>`
- `register(userData: RegisterRequest): Promise<void>`
- `logout(): Promise<void>`
- `refreshAuth(): Promise<void>`
- `updateProfile(userData: Partial<User>): Promise<void>`
- `changePassword(data: ChangePasswordRequest): Promise<void>`
- `requestPasswordReset(data: PasswordResetRequest): Promise<{message}>`
- `clearError(): void`

### ProtectedRoute Props

```typescript
interface ProtectedRouteProps {
  children: React.ReactNode;
  isAuthenticated?: boolean;    // Auto-detected if not provided
  isLoading?: boolean;          // Auto-detected if not provided
  requireAuth?: boolean;        // Default: true
  redirectTo?: string;          // Default: '/login'
  fallback?: React.ReactNode;   // Custom loading component
}
```

## 🤝 Contributing

1. Follow the existing TypeScript patterns
2. Add comprehensive error handling
3. Include JSDoc comments for public APIs
4. Write tests for new functionality
5. Update documentation for changes

## 📄 License

Part of the FocusHive project - University of London Final Year Project.