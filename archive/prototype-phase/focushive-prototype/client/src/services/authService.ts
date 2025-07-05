import type { AuthResponse, User } from '@focushive/shared';
import { api } from './api';

export const authService = {
  async register(email: string, username: string, password: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/api/auth/register', {
      email,
      username,
      password,
    });
    return response.data;
  },

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/api/auth/login', {
      email,
      password,
    });
    return response.data;
  },

  async getMe(): Promise<User> {
    const response = await api.get<User>('/api/auth/me');
    return response.data;
  },

  async logout(): Promise<void> {
    await api.post('/api/auth/logout');
    localStorage.removeItem('token');
  },

  async updateProfile(updates: { lookingForBuddy?: boolean }): Promise<User> {
    const response = await api.put<User>('/api/auth/profile', updates);
    return response.data;
  },
};