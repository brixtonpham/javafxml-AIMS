import { api, paginatedRequest } from './api';
import type {
  AdminMetrics,
  SystemActivity,
  ProductOperation,
  ProductFormState,
  ProductValidationResult,
  BulkProductOperation,
  UserFormState,
  UserOperation,
  PasswordResetRequest,
  PendingOrderSummary,
  OrderApprovalDecision,
  StockValidationResult,
  ProductManagerLimits,
  AuditLogEntry,
  AuditLogFilters,
  EmailNotification,
  FileUploadResult,
  ExportRequest,
  ExportResult,
  ProductSearchFilters,
  UserSearchFilters
} from '../types/admin';
import type { User, Product, Order, PaginatedResponse, Role } from '../types';

class AdminService {
  // Dashboard and metrics
  async getAdminMetrics(): Promise<AdminMetrics> {
    const response = await api.get<AdminMetrics>('/admin/metrics');
    return response.data;
  }

  async getSystemActivity(page = 1, pageSize = 10): Promise<PaginatedResponse<SystemActivity>> {
    return paginatedRequest<SystemActivity>('/admin/activity', { page, pageSize });
  }

  async getRecentActivity(limit = 5): Promise<SystemActivity[]> {
    const response = await api.get<{ activities: SystemActivity[] }>(`/admin/activity/recent?limit=${limit}`);
    return response.data.activities;
  }

  // Product Management
  async getProductsForAdmin(filters: ProductSearchFilters = {}): Promise<PaginatedResponse<Product>> {
    const queryParams = {
      page: filters.page || 1,
      pageSize: filters.pageSize || 20,
      keyword: filters.keyword,
      category: filters.category,
      productType: filters.productType,
      priceMin: filters.priceMin,
      priceMax: filters.priceMax,
      stockMin: filters.stockMin,
      stockMax: filters.stockMax,
      dateFrom: filters.dateFrom?.toISOString(),
      dateTo: filters.dateTo?.toISOString(),
      sortBy: filters.sortBy || 'entryDate',
      sortOrder: filters.sortOrder || 'DESC',
    };

    return paginatedRequest<Product>('/admin/products', queryParams);
  }

  async createProduct(productData: ProductFormState): Promise<Product> {
    const response = await api.post<Product>('/admin/products', productData);
    return response.data;
  }

  async updateProduct(productId: string, productData: Partial<ProductFormState>): Promise<Product> {
    const response = await api.put<Product>(`/admin/products/${productId}`, productData);
    return response.data;
  }

  async deleteProduct(productId: string): Promise<void> {
    await api.delete<void>(`/admin/products/${productId}`);
  }

  async bulkDeleteProducts(productIds: string[], reason: string): Promise<void> {
    if (productIds.length > 10) {
      throw new Error('Cannot delete more than 10 products at once');
    }

    await api.post<void>('/admin/products/bulk-delete', { productIds, reason });
  }

  async updateProductPrice(productId: string, newPrice: number): Promise<Product> {
    const response = await api.put<Product>(`/admin/products/${productId}/price`, { price: newPrice });
    return response.data;
  }

  async validateProductData(productData: ProductFormState): Promise<ProductValidationResult> {
    const response = await api.post<ProductValidationResult>('/admin/products/validate', productData);
    return response.data;
  }

  async getProductOperationHistory(productId?: string, page = 1, pageSize = 20): Promise<PaginatedResponse<ProductOperation>> {
    const queryParams = { page, pageSize, productId };
    return paginatedRequest<ProductOperation>('/admin/products/operations', queryParams);
  }

  async getProductManagerLimits(): Promise<ProductManagerLimits> {
    const response = await api.get<ProductManagerLimits>('/admin/product-manager/limits');
    return response.data;
  }

  // User Management
  async getAllUsers(filters: UserSearchFilters = {}): Promise<PaginatedResponse<User>> {
    const queryParams = {
      page: filters.page || 1,
      pageSize: filters.pageSize || 20,
      keyword: filters.keyword,
      role: filters.role,
      status: filters.status,
      dateFrom: filters.dateFrom?.toISOString(),
      dateTo: filters.dateTo?.toISOString(),
      sortBy: filters.sortBy || 'createdAt',
      sortOrder: filters.sortOrder || 'DESC',
    };

    return paginatedRequest<User>('/admin/users', queryParams);
  }

  async createUser(userData: UserFormState): Promise<User> {
    const response = await api.post<User>('/admin/users', userData);
    return response.data;
  }

  async updateUser(userId: string, userData: Partial<UserFormState>): Promise<User> {
    const response = await api.put<User>(`/admin/users/${userId}`, userData);
    return response.data;
  }

  async deleteUser(userId: string): Promise<void> {
    await api.delete<void>(`/admin/users/${userId}`);
  }

  async blockUser(userId: string, reason?: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/block`, { reason });
    return response.data;
  }

  async unblockUser(userId: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/unblock`);
    return response.data;
  }

  async resetUserPassword(request: PasswordResetRequest): Promise<void> {
    await api.post<void>(`/admin/users/${request.userId}/reset-password`, request);
  }

  async assignUserRole(userId: string, roleId: string): Promise<User> {
    const response = await api.post<User>(`/admin/users/${userId}/roles`, { roleId });
    return response.data;
  }

  async removeUserRole(userId: string, roleId: string): Promise<User> {
    const response = await api.delete<User>(`/admin/users/${userId}/roles/${roleId}`);
    return response.data;
  }

  async getAvailableRoles(): Promise<Role[]> {
    const response = await api.get<Role[]>('/admin/roles');
    return response.data;
  }

  async getUserOperationHistory(userId?: string, page = 1, pageSize = 20): Promise<PaginatedResponse<UserOperation>> {
    const queryParams = { page, pageSize, userId };
    return paginatedRequest<UserOperation>('/admin/users/operations', queryParams);
  }

  // Order Approval (Product Manager functionality)
  async getPendingOrders(page = 1, pageSize = 30): Promise<PaginatedResponse<PendingOrderSummary>> {
    return paginatedRequest<PendingOrderSummary>('/pm/orders/pending', { page, pageSize });
  }

  async getOrderForApproval(orderId: string): Promise<Order> {
    const response = await api.get<Order>(`/pm/orders/${orderId}`);
    return response.data;
  }

  async validateOrderStock(orderId: string): Promise<StockValidationResult> {
    const response = await api.get<StockValidationResult>(`/pm/orders/${orderId}/validate-stock`);
    return response.data;
  }

  async approveOrder(decision: OrderApprovalDecision): Promise<Order> {
    const response = await api.post<Order>(`/pm/orders/${decision.orderId}/approve`, {
      reason: decision.reason,
      notes: decision.notes,
    });
    return response.data;
  }

  async rejectOrder(decision: OrderApprovalDecision): Promise<Order> {
    const response = await api.post<Order>(`/pm/orders/${decision.orderId}/reject`, {
      reason: decision.reason,
      notes: decision.notes,
    });
    return response.data;
  }

  // Audit Logging
  async getAuditLog(filters: AuditLogFilters = {}): Promise<PaginatedResponse<AuditLogEntry>> {
    const queryParams = {
      page: filters.page || 1,
      pageSize: filters.pageSize || 20,
      entityType: filters.entityType,
      action: filters.action,
      performedBy: filters.performedBy,
      dateFrom: filters.dateFrom?.toISOString(),
      dateTo: filters.dateTo?.toISOString(),
    };

    return paginatedRequest<AuditLogEntry>('/admin/audit-log', queryParams);
  }

  async logAuditEntry(entry: Omit<AuditLogEntry, 'id' | 'timestamp'>): Promise<void> {
    await api.post<void>('/admin/audit-log', entry);
  }

  // Email Notifications
  async sendEmailNotification(notification: EmailNotification): Promise<void> {
    await api.post<void>('/admin/email/send', notification);
  }

  async sendUserNotification(userId: string, subject: string, template: string, data: Record<string, any>): Promise<void> {
    await api.post<void>(`/admin/users/${userId}/notify`, { subject, template, data });
  }

  // File Upload
  async uploadProductImage(file: File): Promise<FileUploadResult> {
    const formData = new FormData();
    formData.append('image', file);

    const response = await api.post<FileUploadResult>('/admin/upload/product-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async deleteUploadedFile(fileUrl: string): Promise<void> {
    await api.delete<void>('/admin/upload/delete', { data: { fileUrl } });
  }

  // Export functionality
  async requestExport(exportRequest: ExportRequest): Promise<ExportResult> {
    const response = await api.post<ExportResult>('/admin/export', exportRequest);
    return response.data;
  }

  async getExportStatus(exportId: string): Promise<ExportResult> {
    const response = await api.get<ExportResult>(`/admin/export/${exportId}/status`);
    return response.data;
  }

  async downloadExport(exportId: string): Promise<Blob> {
    const response = await fetch(`/api/admin/export/${exportId}/download`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('auth_token')}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to download export');
    }

    return response.blob();
  }

  // Business rule validation
  async validateBusinessRules(operation: string, data: any): Promise<{ isValid: boolean; violations: string[] }> {
    const response = await api.post<{ isValid: boolean; violations: string[] }>('/admin/validate-business-rules', { operation, data });
    return response.data;
  }

  // Quick stats for dashboard widgets
  async getDashboardStats(): Promise<{
    usersToday: number;
    ordersToday: number;
    revenueToday: number;
    pendingOrdersCount: number;
    lowStockProducts: number;
    blockedUsers: number;
  }> {
    const response = await api.get<{
      usersToday: number;
      ordersToday: number;
      revenueToday: number;
      pendingOrdersCount: number;
      lowStockProducts: number;
      blockedUsers: number;
    }>('/admin/dashboard/quick-stats');
    return response.data;
  }
}

export const adminService = new AdminService();