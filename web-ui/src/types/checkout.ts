// Checkout-specific TypeScript interfaces
import type { DeliveryInfo, OrderItem } from './index';

// Delivery Zone Types
export interface DeliveryZone {
  id: string;
  name: string;
  type: 'inner_city' | 'outer_city' | 'province';
  baseFee: number;
  baseWeight: number; // in kg
  additionalFeePerKg: number;
  rushDeliveryAvailable: boolean;
  rushDeliveryFee: number;
}

// Vietnam Location Types
export interface VietnamProvince {
  code: string;
  name: string;
  cities: VietnamCity[];
  deliveryZone: DeliveryZone;
}

export interface VietnamCity {
  code: string;
  name: string;
  provinceCode: string;
  deliveryZone: DeliveryZone;
}

// Checkout Form Data Types
export interface CheckoutFormData {
  deliveryInfo: CheckoutDeliveryInfo;
  deliveryOptions: DeliveryOptions;
  paymentMethod: string;
}

export interface CheckoutDeliveryInfo extends DeliveryInfo {
  // Additional checkout-specific fields
  saveAsDefault?: boolean;
  isValidated?: boolean;
}

export interface DeliveryOptions {
  isRushOrder: boolean;
  deliveryInstructions?: string;
  preferredDeliveryTime?: 'morning' | 'afternoon' | 'evening' | 'any';
}

// Delivery Calculation Types
export interface DeliveryCalculationInput {
  items: OrderItem[];
  deliveryInfo: CheckoutDeliveryInfo;
  isRushOrder: boolean;
}

export interface DeliveryCalculationResult {
  baseShippingFee: number;
  rushDeliveryFee: number;
  totalShippingFee: number;
  estimatedDeliveryDays: number;
  freeShippingDiscount: number;
  breakdown: DeliveryFeeBreakdown;
  warnings?: string[];
}

export interface DeliveryFeeBreakdown {
  totalWeight: number;
  baseWeightFee: number;
  additionalWeightFee: number;
  rushFeePerItem: number;
  applicableItems: number; // items eligible for rush delivery
  location: string;
  deliveryZone: string;
}

// Checkout Step Types
export type CheckoutStep = 'delivery_info' | 'delivery_options' | 'order_summary' | 'place_order' | 'payment' | 'confirmation';

export interface CheckoutStepConfig {
  id: CheckoutStep;
  title: string;
  description: string;
  isComplete: boolean;
  isActive: boolean;
  isValid: boolean;
}

// Order Preview Types
export interface OrderPreview {
  items: OrderItem[];
  deliveryInfo?: CheckoutDeliveryInfo;
  deliveryOptions?: DeliveryOptions;
  subtotal: number;
  vatAmount: number;
  shippingFee: number;
  rushDeliveryFee: number;
  freeShippingDiscount: number;
  totalAmount: number;
  estimatedDeliveryDays: number;
}

// Form Validation Types
export interface DeliveryInfoFormErrors {
  recipientName?: string;
  phone?: string;
  email?: string;
  address?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  deliveryInstructions?: string;
}

export interface CheckoutFormErrors {
  deliveryInfo?: DeliveryInfoFormErrors;
  deliveryOptions?: {
    isRushOrder?: string;
    deliveryInstructions?: string;
  };
  paymentMethod?: string;
}

// Checkout Context Types
export interface CheckoutContextType {
  // Current state
  currentStep: CheckoutStep;
  steps: CheckoutStepConfig[];
  formData: Partial<CheckoutFormData>;
  orderPreview: OrderPreview | null;
  
  // Loading states
  isCalculatingDelivery: boolean;
  isSubmittingOrder: boolean;
  
  // Errors
  errors: CheckoutFormErrors;
  
  // Actions
  setCurrentStep: (step: CheckoutStep) => void;
  updateFormData: (section: keyof CheckoutFormData, data: any) => void;
  calculateDeliveryFee: (deliveryInfo: CheckoutDeliveryInfo, isRushOrder: boolean) => Promise<void>;
  validateStep: (step: CheckoutStep) => boolean;
  proceedToNextStep: () => void;
  goBackToPreviousStep: () => void;
  submitOrder: () => Promise<string>; // Returns order ID
  resetCheckout: () => void;
  
  // Helpers
  canProceedToStep: (step: CheckoutStep) => boolean;
  getStepProgress: () => number;
  hasUnsavedChanges: () => boolean;
}

// Business Rules Constants
export const DELIVERY_BUSINESS_RULES = {
  // Hanoi/HCMC inner city
  INNER_CITY: {
    BASE_FEE: 22000, // VND
    BASE_WEIGHT: 3, // kg
    ADDITIONAL_FEE_PER_INCREMENT: 2500, // VND per 0.5kg
    WEIGHT_INCREMENT: 0.5, // kg
    RUSH_FEE_PER_ITEM: 10000, // VND
    RUSH_DELIVERY_AVAILABLE: true,
  },
  
  // Other locations
  OUTER_LOCATIONS: {
    BASE_FEE: 30000, // VND
    BASE_WEIGHT: 0.5, // kg
    ADDITIONAL_FEE_PER_INCREMENT: 2500, // VND per 0.5kg
    WEIGHT_INCREMENT: 0.5, // kg
    RUSH_FEE_PER_ITEM: 0, // VND (not available)
    RUSH_DELIVERY_AVAILABLE: false,
  },
  
  // Free shipping
  FREE_SHIPPING: {
    MINIMUM_ORDER_VALUE: 100000, // VND
    MAX_DISCOUNT: 25000, // VND
  },
  
  // VAT
  VAT_RATE: 0.1, // 10%
  
  // Delivery time estimates
  DELIVERY_DAYS: {
    STANDARD_INNER_CITY: 1,
    STANDARD_OUTER_CITY: 2,
    STANDARD_PROVINCE: 3,
    RUSH_INNER_CITY: 0.5, // Same day
  },
} as const;

// Phone number validation patterns
export const PHONE_PATTERNS = {
  VIETNAM_MOBILE: /^(\+84|84|0)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$/,
  VIETNAM_LANDLINE: /^(\+84|84|0)(2[0-9])[0-9]{8}$/,
} as const;

// Email validation pattern
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Address validation constraints
export const ADDRESS_CONSTRAINTS = {
  RECIPIENT_NAME: {
    MIN_LENGTH: 2,
    MAX_LENGTH: 100,
  },
  ADDRESS: {
    MIN_LENGTH: 10,
    MAX_LENGTH: 200,
  },
  POSTAL_CODE: {
    PATTERN: /^\d{5,6}$/,
  },
  DELIVERY_INSTRUCTIONS: {
    MAX_LENGTH: 500,
  },
} as const;