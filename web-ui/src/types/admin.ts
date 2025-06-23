// Admin-specific type definitions for AIMS administrative interfaces
import type { User, Product, Order, Role } from './index';

// System metrics and dashboard data
export interface AdminMetrics {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  pendingOrders: number;
  totalRevenue: number;
  monthlyRevenue: number;
  newUsersThisMonth: number;
  ordersThisMonth: number;
}

export interface SystemActivity {
  id: string;
  type: 'USER_CREATED' | 'PRODUCT_CREATED' | 'PRODUCT_UPDATED' | 'PRODUCT_DELETED' | 'ORDER_APPROVED' | 'ORDER_REJECTED' | 'USER_BLOCKED' | 'USER_UNBLOCKED';
  description: string;
  performedBy: string;
  performedByRole: string;
  timestamp: Date;
  metadata?: Record<string, any>;
}

// Product management interfaces
export interface ProductOperation {
  id: string;
  productId: string;
  productName: string;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE' | 'PRICE_UPDATE';
  performedBy: string;
  timestamp: Date;
  oldValue?: any;
  newValue?: any;
  reason?: string;
}

export interface ProductFormState {
  name: string;
  category: string;
  price: number;
  quantity: number;
  weight: number;
  productType: 'BOOK' | 'CD' | 'LP' | 'DVD';
  imageUrl?: string;
  description?: string;
  // Type-specific fields
  author?: string;        // Book
  coverType?: string;     // Book
  publisher?: string;     // Book
  publicationDate?: string; // Book
  pages?: number;         // Book
  language?: string;      // Book
  artist?: string;        // CD, LP
  recordLabel?: string;   // CD, LP
  musicGenre?: string;    // CD, LP
  releaseDate?: string;   // CD, LP, DVD
  trackList?: string[];   // CD, LP
  runtime?: number;       // DVD
  studio?: string;        // DVD
  director?: string;      // DVD
  cast?: string[];        // DVD
  genre?: string;         // DVD
  subtitle?: string;      // DVD
  discType?: string;      // DVD
}

export interface ProductValidationResult {
  isValid: boolean;
  errors: Record<string, string>;
  warnings: string[];
}

export interface BulkProductOperation {
  productIds: string[];
  operation: 'DELETE' | 'UPDATE_PRICE' | 'UPDATE_STOCK';
  newPrice?: number;
  stockChange?: number;
  reason: string;
}

// User management interfaces
export interface UserFormState {
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  roles: string[];
  status: 'ACTIVE' | 'BLOCKED';
}

export interface UserOperation {
  id: string;
  userId: string;
  username: string;
  operationType: 'CREATE' | 'UPDATE' | 'BLOCK' | 'UNBLOCK' | 'ROLE_ASSIGN' | 'ROLE_REMOVE' | 'PASSWORD_RESET';
  performedBy: string;
  timestamp: Date;
  details: string;
  metadata?: Record<string, any>;
}

export interface PasswordResetRequest {
  userId: string;
  newPassword: string;
  sendEmail: boolean;
  reason?: string;
}

// Order approval interfaces
export interface PendingOrderSummary {
  id: string;
  customerName: string;
  customerEmail: string;
  orderDate: Date;
  totalAmount: number;
  itemCount: number;
  deliveryType: 'STANDARD' | 'RUSH';
  paymentStatus: string;
  hasStockIssues: boolean;
}

export interface OrderApprovalDecision {
  orderId: string;
  decision: 'APPROVE' | 'REJECT';
  reason?: string;
  notes?: string;
}

export interface StockValidationResult {
  isValid: boolean;
  issues: StockValidationIssue[];
}

export interface StockValidationIssue {
  productId: string;
  productName: string;
  requestedQuantity: number;
  availableQuantity: number;
  message: string;
}

// Business rule enforcement
export interface BusinessRuleViolation {
  rule: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
  metadata?: Record<string, any>;
}

export interface ProductManagerLimits {
  dailyOperationsUsed: number;
  dailyOperationsLimit: number;
  priceUpdatesToday: Record<string, number>; // productId -> count
  maxPriceUpdatesPerDay: number;
  maxBulkDeleteLimit: number;
}

// Audit and logging
export interface AuditLogEntry {
  id: string;
  entityType: 'USER' | 'PRODUCT' | 'ORDER';
  entityId: string;
  action: string;
  performedBy: string;
  performedByRole: string;
  timestamp: Date;
  oldValue?: any;
  newValue?: any;
  ipAddress?: string;
  userAgent?: string;
  success: boolean;
  errorMessage?: string;
}

export interface AuditLogFilters {
  entityType?: string;
  action?: string;
  performedBy?: string;
  dateFrom?: Date;
  dateTo?: Date;
  page?: number;
  pageSize?: number;
}

// Email notification interfaces
export interface EmailNotification {
  to: string[];
  cc?: string[];
  subject: string;
  template: string;
  data: Record<string, any>;
  priority: 'LOW' | 'NORMAL' | 'HIGH';
}

export interface EmailTemplate {
  id: string;
  name: string;
  subject: string;
  htmlContent: string;
  textContent?: string;
  requiredData: string[];
}

// File upload interfaces
export interface FileUploadResult {
  url: string;
  filename: string;
  size: number;
  mimeType: string;
}

export interface ImageUploadOptions {
  maxSizeBytes: number;
  allowedTypes: string[];
  generateThumbnail: boolean;
  thumbnailSize?: { width: number; height: number };
}

// Dashboard widget interfaces
export interface DashboardWidget {
  id: string;
  title: string;
  type: 'METRIC' | 'CHART' | 'LIST' | 'TABLE';
  data: any;
  refreshInterval?: number;
}

export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string;
    borderColor?: string;
  }[];
}

// Search and filtering
export interface AdminSearchFilters {
  keyword?: string;
  entityType?: string;
  dateFrom?: Date;
  dateTo?: Date;
  status?: string;
  performedBy?: string;
  page?: number;
  pageSize?: number;
}

export interface ProductSearchFilters {
  keyword?: string;
  category?: string;
  productType?: string;
  priceMin?: number;
  priceMax?: number;
  stockMin?: number;
  stockMax?: number;
  dateFrom?: Date;
  dateTo?: Date;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  pageSize?: number;
}

export interface UserSearchFilters {
  keyword?: string;
  role?: string;
  status?: string;
  dateFrom?: Date;
  dateTo?: Date;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  pageSize?: number;
}

// Export functionality
export interface ExportRequest {
  entityType: 'PRODUCTS' | 'USERS' | 'ORDERS' | 'AUDIT_LOG';
  format: 'CSV' | 'EXCEL' | 'PDF';
  filters?: any;
  fields?: string[];
  dateRange?: {
    from: Date;
    to: Date;
  };
}

export interface ExportResult {
  id: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
  error?: string;
  createdAt: Date;
  expiresAt: Date;
}