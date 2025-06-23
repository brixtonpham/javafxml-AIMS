import { api } from './api';
import type { 
  User, 
  AuthCredentials, 
  AuthResponse, 
  ChangePasswordFormData 
} from '../types';

export const authService = {
  async login(credentials: AuthCredentials): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', credentials);
    
    // Store auth data in localStorage
    if (response.data.token) {
      localStorage.setItem('auth_token', response.data.token);
      localStorage.setItem('user_data', JSON.stringify(response.data.user));
    }
    
    return response.data;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      // Always clear local storage regardless of API response
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_data');
    }
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/current');
    return response.data;
  },

  async validateSession(): Promise<User | null> {
    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        return null;
      }
      
      const response = await api.get<User>('/auth/validate');
      return response.data;
    } catch (error) {
      // If validation fails, clear stored auth data
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_data');
      return null;
    }
  },

  async changePassword(passwordData: ChangePasswordFormData): Promise<void> {
    await api.post('/auth/change-password', {
      currentPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword,
    });
  },

  // Check if user is authenticated
  isAuthenticated(): boolean {
    const token = localStorage.getItem('auth_token');
    return !!token;
  },

  // Get stored user data
  getStoredUser(): User | null {
    try {
      const userData = localStorage.getItem('user_data');
      return userData ? JSON.parse(userData) : null;
    } catch {
      return null;
    }
  },

  // Get stored token
  getStoredToken(): string | null {
    return localStorage.getItem('auth_token');
  },

  // Clear auth data
  clearAuthData(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_data');
  },
};