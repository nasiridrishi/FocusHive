# FocusHive Frontend Naming Conventions

## Overview

This document establishes consistent naming conventions across the FocusHive frontend codebase. These conventions follow React best practices and industry standards to ensure maintainable, readable, and scalable code.

## Component Naming

### Component Files
**Convention:** PascalCase with `.tsx` extension
```
✅ UserProfile.tsx
✅ LoginForm.tsx
✅ PasswordStrengthIndicator.tsx

❌ userProfile.tsx
❌ login-form.tsx
❌ password_strength_indicator.tsx
```

### Component Names
**Convention:** PascalCase matching the file name
```tsx
// UserProfile.tsx
✅ export const UserProfile: React.FC = () => { ... }
✅ export default function UserProfile() { ... }

❌ export const userProfile = () => { ... }
❌ export default function user_profile() { ... }
```

### Component Props Interfaces
**Convention:** PascalCase + `Props` suffix
```tsx
✅ interface UserProfileProps {
    userId: string;
    onUpdate: () => void;
}

✅ interface LoginFormProps {
    onSubmit: (data: LoginRequest) => void;
    isLoading?: boolean;
}

❌ interface UserProfilePropsInterface { ... }
❌ interface userProfileProps { ... }
❌ interface IUserProfileProps { ... }
```

## Directory Structure

### Feature Directories
**Convention:** kebab-case
```
✅ src/features/user-profile/
✅ src/features/auth/
✅ src/features/hive-management/

❌ src/features/UserProfile/
❌ src/features/user_profile/
❌ src/features/userProfile/
```

### Component Subdirectories
**Convention:** kebab-case
```
✅ src/shared/components/error-boundary/
✅ src/shared/components/dynamic-icon/
✅ src/shared/components/loading-states/

❌ src/shared/components/ErrorBoundary/
❌ src/shared/components/DynamicIcon/
❌ src/shared/components/loading_states/
```

### Utility Directories
**Convention:** kebab-case
```
✅ src/utils/date-formatting/
✅ src/utils/api-helpers/
✅ src/utils/validation/

❌ src/utils/dateFormatting/
❌ src/utils/api_helpers/
❌ src/utils/ValidationUtils/
```

## JavaScript/TypeScript Naming

### Variables and Functions
**Convention:** camelCase
```tsx
✅ const userName = 'john_doe';
✅ const isAuthenticated = true;
✅ const handleSubmit = () => { ... };
✅ const validatePassword = (password: string) => { ... };

❌ const user_name = 'john_doe';
❌ const UserName = 'john_doe';
❌ const IsAuthenticated = true;
```

### Constants
**Convention:** UPPER_SNAKE_CASE
```tsx
✅ const API_BASE_URL = 'https://api.focushive.com';
✅ const MAX_PASSWORD_LENGTH = 128;
✅ const VALIDATION_MESSAGES = {
    REQUIRED: 'This field is required',
    INVALID_EMAIL: 'Please enter a valid email'
};

❌ const apiBaseUrl = 'https://api.focushive.com';
❌ const maxPasswordLength = 128;
❌ const validationMessages = { ... };
```

### Custom Hooks
**Convention:** camelCase starting with "use"
```tsx
✅ export const useAuth = () => { ... };
✅ export const useWebSocket = () => { ... };
✅ export const useLocalStorage = () => { ... };

❌ export const UseAuth = () => { ... };
❌ export const authHook = () => { ... };
❌ export const getAuth = () => { ... };
```

### Event Handlers
**Convention:** "handle" or "on" prefix + PascalCase
```tsx
// Component internal handlers
✅ const handleSubmit = (event: React.FormEvent) => { ... };
✅ const handleUserClick = () => { ... };
✅ const handlePasswordChange = (value: string) => { ... };

// Props for event handlers
✅ interface ButtonProps {
    onClick: () => void;
    onSubmit: (data: FormData) => void;
    onUserSelect: (userId: string) => void;
}

❌ const submit = () => { ... };
❌ const userclick = () => { ... };
❌ const password_change = () => { ... };
```

## File Naming

### Utility Files
**Convention:** camelCase
```
✅ formatDate.ts
✅ apiHelpers.ts
✅ validationUtils.ts

❌ format-date.ts
❌ format_date.ts
❌ FormatDate.ts
```

### Test Files
**Convention:** Match component name + `.test.tsx` or `.test.ts`
```
✅ UserProfile.test.tsx
✅ formatDate.test.ts
✅ apiHelpers.test.ts

❌ userProfile.test.tsx
❌ user-profile.test.tsx
❌ UserProfileTest.tsx
```

### Type Definition Files
**Convention:** camelCase + `.ts` (for utility types) or PascalCase + `.ts` (for component-specific types)
```
✅ src/types/auth.ts
✅ src/types/common.ts
✅ src/features/user/types/UserProfile.ts

❌ src/types/Auth.ts
❌ src/types/user_types.ts
❌ src/features/user/types/user-profile.ts
```

## API and Service Naming

### API Service Files
**Convention:** camelCase + "Api" suffix
```
✅ authApi.ts
✅ userApi.ts
✅ hiveApi.ts

❌ auth-api.ts
❌ AuthAPI.ts
❌ auth_service.ts
```

### Service Classes/Objects
**Convention:** PascalCase + "Service" suffix
```tsx
✅ export class WebSocketService { ... }
✅ export const AuthService = { ... };
✅ export class NotificationService { ... }

❌ export class websocketService { ... }
❌ export const authservice = { ... };
❌ export class notification_service { ... }
```

## CSS and Styling

### CSS Classes (when using CSS modules)
**Convention:** kebab-case
```css
✅ .user-profile { ... }
✅ .login-form-container { ... }
✅ .password-strength-indicator { ... }

❌ .userProfile { ... }
❌ .LoginForm { ... }
❌ .password_strength_indicator { ... }
```

### Styled Components
**Convention:** PascalCase
```tsx
✅ const StyledButton = styled.button`...`;
✅ const UserProfileContainer = styled.div`...`;
✅ const NavigationWrapper = styled.nav`...`;

❌ const styledButton = styled.button`...`;
❌ const styled_button = styled.button`...`;
❌ const STYLED_BUTTON = styled.button`...`;
```

## Environment and Configuration

### Environment Variables
**Convention:** UPPER_SNAKE_CASE with descriptive prefixes
```
✅ VITE_API_BASE_URL
✅ VITE_WEBSOCKET_URL
✅ VITE_FEATURE_FLAG_ANALYTICS

❌ vite_api_url
❌ ViteApiUrl
❌ API_URL
```

### Configuration Objects
**Convention:** camelCase with descriptive names
```tsx
✅ const apiConfig = { ... };
✅ const websocketConfig = { ... };
✅ const themeConfig = { ... };

❌ const API_CONFIG = { ... };
❌ const ApiConfig = { ... };
❌ const api_config = { ... };
```

## Validation Rules Summary

| Element Type | Convention | Example |
|-------------|------------|---------|
| Component Files | PascalCase.tsx | `UserProfile.tsx` |
| Component Names | PascalCase | `UserProfile` |
| Props Interfaces | PascalCase + Props | `UserProfileProps` |
| Directories | kebab-case | `user-profile/` |
| Variables/Functions | camelCase | `userName`, `handleClick` |
| Constants | UPPER_SNAKE_CASE | `API_BASE_URL` |
| Custom Hooks | use + camelCase | `useAuth`, `useWebSocket` |
| Utility Files | camelCase.ts | `formatDate.ts` |
| API Services | camelCase + Api | `authApi.ts` |
| CSS Classes | kebab-case | `.user-profile` |
| Styled Components | PascalCase | `StyledButton` |
| Environment Variables | UPPER_SNAKE_CASE | `VITE_API_BASE_URL` |

## Enforcement

These naming conventions are enforced through:

1. **ESLint Rules**: Automated linting rules check naming patterns
2. **TypeScript**: Strong typing ensures interface naming consistency
3. **Code Reviews**: Manual verification during pull request reviews
4. **Documentation**: This guide serves as the authoritative reference

## Migration Strategy

When updating existing code to follow these conventions:

1. **Rename files** following the new conventions
2. **Update all imports** to reflect new file names
3. **Rename variables, functions, and classes** to match conventions
4. **Update tests** to reflect new naming
5. **Verify functionality** after renaming

## Tools Integration

### VS Code Extensions
- **Auto Rename Tag**: Helps with component renaming
- **TypeScript Importer**: Auto-updates imports when files are renamed
- **ESLint**: Provides real-time feedback on naming violations

### Build Tools
- **ESLint**: Configured with naming convention rules
- **TypeScript**: Strict mode enabled for better type checking
- **Prettier**: Consistent code formatting (doesn't affect naming but improves readability)