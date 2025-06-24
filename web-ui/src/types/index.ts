// Core API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  errors?: string[];
}

export interface PaginatedResponse<T> {
  items: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    pages: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
}

// Products API has a different response structure
export interface ProductsApiResponse<T> {
  success: boolean;
  items: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    pages: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
  message: string;
}

// Product Types
export interface Product {
  id: string;
  productId: string; // Backend uses productId
  title: string;
  description: string;
  category: string;
  price: number; // VAT-inclusive for customers
  valueAmount: number; // Backend uses valueAmount
  quantityInStock: number; // Backend uses quantityInStock
  quantity?: number; // Keep for compatibility, will be derived from quantityInStock
  imageUrl?: string;
  productType: ProductType;
  entryDate: string;
  // Subtype-specific fields
  author?: string; // For books
  artists?: string; // For CDs/LPs
  director?: string; // For DVDs
  publisher?: string; // For books
  language?: string; // For books/DVDs
  genre?: string; // For all media types
  runtime?: number; // For DVDs (minutes)
  studio?: string; // For CDs/DVDs
  tracklist?: string; // For LPs
  // Additional backend fields
  barcode?: string;
  dimensionsCm?: string;
  weightKg?: number;
}

export const ProductType = {
  BOOK: 'BOOK',
  CD: 'CD',
  DVD: 'DVD',
  LP: 'LP'
} as const;

export type ProductType = typeof ProductType[keyof typeof ProductType];

export interface ProductSearchParams {
  keyword?: string;
  category?: string;
  productType?: string;
  sortBy?: 'title' | 'price' | 'category' | 'entryDate' | 'quantity';
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  pageSize?: number;
}

// Cart Types
export interface CartItem {
  productId: string;
  product: Product;
  quantity: number;
  subtotal: number; // Price * quantity (excluding VAT)
}

export interface Cart {
  sessionId: string;
  userId?: string;
  items: CartItem[];
  totalItems: number;
  totalPrice: number; // Excluding VAT
  totalPriceWithVAT: number; // Including VAT
  createdAt: string;
  updatedAt: string;
  stockWarnings?: StockWarning[];
}

export interface StockWarning {
  productId: string;
  requestedQuantity: number;
  availableQuantity: number;
  message: string;
}

// Order Types
export interface OrderItem {
  productId: string;
  productTitle: string;
  productType: ProductType;
  quantity: number;
  unitPrice: number; // At time of order
  subtotal: number;
  productMetadata?: {
    author?: string;
    artists?: string;
    director?: string;
    category?: string;
  };
}

export interface DeliveryInfo {
  recipientName: string;
  phone: string;
  email: string;
  address: string;
  city: string;
  province: string;
  postalCode?: string;
  deliveryInstructions?: string;
}

export interface Order {
  id: string;
  userId?: string;
  items: OrderItem[];
  deliveryInfo?: DeliveryInfo;
  subtotal: number; // Excluding VAT and shipping
  vatAmount: number;
  shippingFee: number;
  totalAmount: number;
  status: OrderStatus;
  isRushOrder: boolean;
  rushOrderFee?: number;
  paymentMethod?: PaymentMethod;
  paymentStatus?: PaymentStatus;
  trackingNumber?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectedBy?: string;
  rejectedAt?: string;
  rejectionReason?: string;
}

export const OrderStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  SHIPPED: 'SHIPPED',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED'
} as const;

export type OrderStatus = typeof OrderStatus[keyof typeof OrderStatus];

export const PaymentStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  REFUNDED: 'REFUNDED'
} as const;

export type PaymentStatus = typeof PaymentStatus[keyof typeof PaymentStatus];

// Payment Types
export interface PaymentMethod {
  id: string;
  type: PaymentMethodType;
  name: string;
  description?: string;
  isActive: boolean;
}

export const PaymentMethodType = {
  VNPAY: 'VNPAY',
  CARD: 'CARD',
  CASH: 'CASH'
} as const;

export type PaymentMethodType = typeof PaymentMethodType[keyof typeof PaymentMethodType];

export interface PaymentResult {
  success: boolean;
  transactionId?: string;
  paymentUrl?: string; // For VNPay redirect
  message: string;
  errorCode?: string;
}

// User & Authentication Types
export interface User {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  status: UserStatus;
  roles: Role[];
  createdAt: string;
  lastLoginAt?: string;
}

export const UserStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  BLOCKED: 'BLOCKED'
} as const;

export type UserStatus = typeof UserStatus[keyof typeof UserStatus];

export interface Role {
  id: string;
  name: string;
  description?: string;
  permissions: string[];
}

export const UserRole = {
  ADMIN: 'ADMIN',
  PRODUCT_MANAGER: 'PRODUCT_MANAGER',
  CUSTOMER: 'CUSTOMER'
} as const;

export type UserRole = typeof UserRole[keyof typeof UserRole];

export interface AuthCredentials {
  username: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  token: string;
  expiresAt: string;
}

// Form Types
export interface LoginFormData {
  username: string;
  password: string;
}

export interface ChangePasswordFormData {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ProductFormData {
  title: string;
  description: string;
  category: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  productType: ProductType;
  // Subtype-specific fields
  author?: string;
  artists?: string;
  director?: string;
  tracklist?: string;
  publisher?: string;
  genre?: string;
  runtime?: number;
  language?: string;
}

export interface DeliveryFormData extends DeliveryInfo {
  isRushOrder: boolean;
}

// Error Types
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export interface ValidationError {
  field: string;
  message: string;
}

// Component Props Types
export interface LayoutProps {
  children: React.ReactNode;
  title?: string;
  showHeader?: boolean;
  showSidebar?: boolean;
}

export interface TableColumn<T> {
  key: keyof T | string;
  label: string;
  sortable?: boolean;
  render?: (value: any, item: T) => React.ReactNode;
}

export interface TableProps<T> {
  data: T[];
  columns: TableColumn<T>[];
  loading?: boolean;
  onSort?: (key: string, direction: 'asc' | 'desc') => void;
  onRowClick?: (item: T) => void;
}

// Navigation Types
export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon?: React.ComponentType;
  roles?: UserRole[];
  children?: NavItem[];
}

// State Management Types
export interface AppState {
  auth: AuthState;
  cart: CartState;
  products: ProductState;
  orders: OrderState;
  ui: UIState;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export interface CartState {
  cart: Cart | null;
  isLoading: boolean;
  error: string | null;
}

export interface ProductState {
  products: Product[];
  categories: string[];
  productTypes: string[];
  isLoading: boolean;
  error: string | null;
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
}

export interface OrderState {
  orders: Order[];
  currentOrder: Order | null;
  isLoading: boolean;
  error: string | null;
}

export interface UIState {
  theme: 'light' | 'dark';
  sidebarOpen: boolean;
  notifications: Notification[];
}

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

// Stock Validation Types for Phase 2
export interface StockValidationResult {
  isValid: boolean;
  productId: string;
  productTitle: string;
  requestedQuantity: number;
  actualStock: number;
  reservedStock: number;
  availableStock: number;
  message: string;
  reasonCode: string;
  shortfallQuantity?: number;
}

export interface BulkStockValidationResult {
  isAllValid: boolean;
  individualResults: StockValidationResult[];
  failedValidations: StockValidationResult[];
  totalProductsChecked: number;
  totalFailedProducts: number;
  overallMessage: string;
}

export interface StockValidationNotification {
  title: string;
  message: string;
  productMessages: string[];
  suggestedActions: Record<string, string>;
  canProceedWithAvailableStock: boolean;
}

// Enhanced Rush Delivery Types for Phase 2
export interface RushDeliveryEligibilityResult {
  isEligible: boolean;
  message: string;
  reasonCode: string;
  eligibleDistricts: string[];
}

export interface RushDeliveryTimeEstimate {
  deliveryHours: number;
  timeWindow: string;
  cutoffTime: string;
  availableToday: boolean;
}

export interface RushOrderValidationResult {
  isValid: boolean;
  message: string;
  addressEligibility: RushDeliveryEligibilityResult;
  itemEligibility: RushDeliveryEligibilityResult;
  rushFee: number;
  timeEstimate?: RushDeliveryTimeEstimate;
}

// Enhanced Business Rules for Phase 2
export interface VATCalculationResult {
  basePrice: number;
  vatAmount: number;
  totalPrice: number;
  vatRate: number;
  isValid: boolean;
}

export interface DeliveryFeeBreakdown {
  totalFee: number;
  baseFee: number;
  regionalAdjustment: number;
  rushSurcharge: number;
  freeShippingDiscount: number;
}

export interface PaymentFlowState {
  step: 'DELIVERY' | 'PAYMENT_METHOD' | 'PAYMENT_PROCESSING' | 'CONFIRMATION';
  canProceed: boolean;
  validationErrors: string[];
  deliveryValidated: boolean;
  stockValidated: boolean;
  paymentMethodSelected: boolean;
}