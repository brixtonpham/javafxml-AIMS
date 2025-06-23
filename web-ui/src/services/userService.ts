import { api, paginatedRequest } from './api';
import type { 
  User, 
  UserRole,
  ChangePasswordFormData,
  PaginatedResponse 
} from '../types';

export const userService = {
  // Get current user profile
  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/users/profile');
    return response.data;
  },

  // Update user profile
  async updateProfile(userData: Partial<User>): Promise<User> {
    const response = await api.put<User>('/users/profile', userData);
    return response.data;
  },

  // Change password
  async changePassword(passwordData: ChangePasswordFormData): Promise<void> {
    await api.post('/users/change-password', {
      currentPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword,
    });
  },

  // Admin: Get all users
  async getAllUsers(page = 1, pageSize = 20): Promise<PaginatedResponse<User>> {
    return paginatedRequest<User>('/admin/users', {
      page,
      pageSize
    });
  },

  // Admin: Get user by ID
  async getUserById(userId: string): Promise<User> {
    const response = await api.get<User>(`/admin/users/${userId}`);
    return response.data;
  },

  // Admin: Create new user
  async createUser(userData: {
    username: string;
    email: string;
    fullName?: string;
    password: string;
    roles: UserRole[];
  }): Promise<User> {
    const response = await api.post<User>('/admin/users', userData);
    return response.data;
  },

  // Admin: Update user
  async updateUser(userId: string, userData: Partial<User>): Promise<User> {
    const response = await api.put<User>(`/admin/users/${userId}`, userData);
    return response.data;
  },

  // Admin: Delete user
  async deleteUser(userId: string): Promise<void> {
    await api.delete(`/admin/users/${userId}`);
  },

  // Admin: Block user
  async blockUser(userId: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/block`);
    return response.data;
  },

  // Admin: Unblock user
  async unblockUser(userId: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/unblock`);
    return response.data;
  },

  // Admin: Reset user password
  async resetUserPassword(userId: string, newPassword: string): Promise<void> {
    await api.post(`/admin/users/${userId}/reset-password`, {
      newPassword
    });
  },

  // Admin: Assign role to user
  async assignRole(userId: string, roleId: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/roles`, {
      roleId
    });
    return response.data;
  },

  // Admin: Remove role from user
  async removeRole(userId: string, roleId: string): Promise<User> {
    const response = await api.delete<User>(`/admin/users/${userId}/roles/${roleId}`);
    return response.data;
  },

  // Get available roles
  async getAvailableRoles(): Promise<UserRole[]> {
    const response = await api.get<UserRole[]>('/admin/roles');
    return response.data;
  },

  // Search users
  async searchUsers(
    keyword: string, 
    role?: UserRole, 
    page = 1, 
    pageSize = 20
  ): Promise<PaginatedResponse<User>> {
    return paginatedRequest<User>('/admin/users/search', {
      keyword,
      role,
      page,
      pageSize
    });
  }
};