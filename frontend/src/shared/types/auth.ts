// Authentication related types
export interface User {
  id: string
  email: string
  username: string
  firstName: string
  lastName: string
  name: string // Derived from firstName + lastName or username
  avatar?: string
  profilePicture?: string // Alias for avatar
  isEmailVerified: boolean
  isVerified?: boolean // Alias for isEmailVerified
  createdAt: string
  updatedAt: string
}

export interface AuthState {
  user: User | null
  token: string | null
  refreshToken: string | null
  isLoading: boolean
  isAuthenticated: boolean
  error: string | null
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  user: User
  token: string
  refreshToken: string
}

export interface RegisterRequest {
  email: string
  password: string
  username: string
  firstName: string
  lastName: string
}

export interface RegisterResponse {
  user: User
  token: string
  refreshToken: string
}

export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetResponse {
  message: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface AuthContextType {
  authState: AuthState
  login: (credentials: LoginRequest) => Promise<void>
  register: (userData: RegisterRequest) => Promise<void>
  logout: () => void
  refreshAuth: () => Promise<void>
  updateProfile: (userData: Partial<User>) => Promise<void>
}