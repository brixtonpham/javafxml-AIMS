import React from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CartProvider } from '../contexts/CartContext';
import { TestCartProvider } from './TestCartProvider';
import { vi } from 'vitest';

// Mock services
vi.mock('../services/cartService', () => ({
  cartService: {
    getCart: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    addToCart: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    updateItemQuantity: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    removeFromCart: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    clearCart: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    associateCartWithUser: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    createCart: vi.fn().mockResolvedValue({
      id: 'test-cart',
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }),
    getCartSessionId: vi.fn().mockReturnValue('test-session'),
    setCartSessionId: vi.fn(),
    clearCartSessionId: vi.fn(),
    ensureCartSession: vi.fn().mockResolvedValue('test-session'),
  },
}));

vi.mock('../services/authService', () => ({
  authService: {
    login: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
    validateSession: vi.fn(),
    changePassword: vi.fn(),
    isAuthenticated: vi.fn().mockReturnValue(false),
    getStoredUser: vi.fn().mockReturnValue(null),
    clearAuthData: vi.fn(),
  },
}));

// Custom render function with providers
const AllProviders: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        staleTime: 0,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <TestCartProvider>
          {children}
        </TestCartProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

const customRender = (
  ui: React.ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) => render(ui, { wrapper: AllProviders, ...options });

// Re-export everything
export * from '@testing-library/react';
export { customRender as render };

// Helper functions for testing
export const createMockProduct = (overrides = {}) => ({
  id: 'test-product-1',
  title: 'Test Product',
  productType: 'BOOK' as const,
  price: 100000,
  quantity: 10,
  description: 'Test product description',
  imageUrl: '/test-image.jpg',
  category: 'Test Category',
  value: 100000,
  entryDate: '2024-01-15T10:00:00Z',
  metadata: {
    author: 'Test Author',
    category: 'Test Category',
  },
  ...overrides,
});

export const createMockCartItem = (overrides = {}) => ({
  productId: 'test-product-1',
  productTitle: 'Test Product',
  productType: 'BOOK',
  quantity: 1,
  unitPrice: 100000,
  subtotal: 100000,
  productMetadata: {
    author: 'Test Author',
    category: 'Test Category',
  },
  ...overrides,
});

export const createMockOrder = (overrides = {}) => ({
  id: 'test-order-1',
  userId: 'test-user-1',
  items: [createMockCartItem()],
  subtotal: 100000,
  shippingFee: 22000,
  total: 122000,
  status: 'CONFIRMED',
  createdAt: new Date('2024-01-15T10:00:00Z'),
  deliveryInfo: {
    recipientName: 'Test User',
    phone: '0123456789',
    email: 'test@example.com',
    address: '123 Test Street',
    city: 'HN_HOAN_KIEM',
    province: 'HN',
    postalCode: '10000',
    deliveryInstructions: 'Test instructions',
    isValidated: true,
  },
  paymentMethod: 'VNPAY',
  paymentStatus: 'COMPLETED',
  ...overrides,
});

export const createMockUser = (overrides = {}) => ({
  id: 'test-user-1',
  email: 'test@example.com',
  name: 'Test User',
  role: 'CUSTOMER',
  phone: '0123456789',
  address: '123 Test Street, Test City',
  ...overrides,
});

// Mock delivery info
export const createMockDeliveryInfo = (overrides = {}) => ({
  recipientName: 'Test User',
  phone: '0123456789',
  email: 'test@example.com',
  address: '123 Test Street',
  city: 'HN_HOAN_KIEM',
  province: 'HN',
  postalCode: '10000',
  deliveryInstructions: 'Test instructions',
  isValidated: true,
  ...overrides,
});

// Mock calculation result
export const createMockCalculationResult = (overrides = {}) => ({
  baseShippingFee: 22000,
  rushDeliveryFee: 0,
  totalShippingFee: 22000,
  estimatedDeliveryDays: 1,
  freeShippingDiscount: 0,
  breakdown: {
    totalWeight: 0.3,
    baseWeightFee: 22000,
    additionalWeightFee: 0,
    rushFeePerItem: 0,
    applicableItems: 1,
    location: 'Hoàn Kiếm, Hà Nội',
    deliveryZone: 'Hanoi Inner City',
  },
  ...overrides,
});

// Wait for loading states
export const waitForLoadingToFinish = () => {
  return new Promise(resolve => setTimeout(resolve, 0));
};

// Mock IntersectionObserver
export const mockIntersectionObserver = () => {
  const mockIntersectionObserver = vi.fn();
  mockIntersectionObserver.mockReturnValue({
    observe: () => null,
    unobserve: () => null,
    disconnect: () => null,
  });
  window.IntersectionObserver = mockIntersectionObserver;
};

// Mock window.matchMedia
export const mockMatchMedia = (matches = false) => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => ({
      matches,
      media: query,
      onchange: null,
      addListener: vi.fn(), // deprecated
      removeListener: vi.fn(), // deprecated
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
};

// Mock localStorage
export const mockLocalStorage = () => {
  const localStorageMock = {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
  };
  Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
  });
  return localStorageMock;
};

// Mock window.scrollTo
export const mockScrollTo = () => {
  Object.defineProperty(window, 'scrollTo', {
    value: vi.fn(),
    writable: true,
  });
};

// Test data factories
export const TestData = {
  product: createMockProduct,
  cartItem: createMockCartItem,
  order: createMockOrder,
  user: createMockUser,
  deliveryInfo: createMockDeliveryInfo,
  calculationResult: createMockCalculationResult,
};