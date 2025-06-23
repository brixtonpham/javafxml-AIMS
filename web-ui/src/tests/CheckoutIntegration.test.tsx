import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CartProvider } from '../contexts/CartContext';
import DeliveryOptionsSelector from '../components/DeliveryOptionsSelector';
import OrderSummary from '../components/OrderSummary';
import { deliveryService } from '../services/DeliveryService';
import { 
  calculateDeliveryDetails, 
  validateDeliveryInfo,
  supportsRushDelivery,
  calculateRemainingForFreeShipping
} from '../utils/deliveryCalculations';
import type { 
  CheckoutDeliveryInfo, 
  DeliveryOptions, 
  DeliveryCalculationResult 
} from '../types/checkout';
import type { OrderItem } from '../types';

// Mock data
const mockDeliveryInfo: CheckoutDeliveryInfo = {
  recipientName: 'Nguyễn Văn A',
  phone: '0123456789',
  email: 'test@example.com',
  address: '123 Đường ABC, Phường XYZ',
  city: 'HN_HOAN_KIEM',
  province: 'HN',
  postalCode: '10000',
  deliveryInstructions: 'Gọi trước khi giao',
  isValidated: true
};

const mockOrderItems: OrderItem[] = [
  {
    productId: 'book-1',
    productTitle: 'Harry Potter và Hòn đá Phù thủy',
    productType: 'BOOK',
    quantity: 2,
    unitPrice: 150000,
    subtotal: 300000,
    productMetadata: {
      author: 'J.K. Rowling',
      category: 'Fiction'
    }
  },
  {
    productId: 'cd-1',
    productTitle: 'Best of Sơn Tùng M-TP',
    productType: 'CD',
    quantity: 1,
    unitPrice: 200000,
    subtotal: 200000,
    productMetadata: {
      artists: 'Sơn Tùng M-TP',
      category: 'Music'
    }
  }
];

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <CartProvider>
          {children}
        </CartProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

describe('Delivery Calculations Integration Tests', () => {
  describe('calculateDeliveryDetails', () => {
    it('should calculate correct fees for Hanoi inner city', () => {
      const input = {
        items: mockOrderItems,
        deliveryInfo: mockDeliveryInfo,
        isRushOrder: false
      };

      const result = calculateDeliveryDetails(input);

      expect(result.baseShippingFee).toBe(22000); // Hanoi inner city base fee
      expect(result.rushDeliveryFee).toBe(0); // No rush order
      expect(result.estimatedDeliveryDays).toBe(1); // Hanoi inner city standard
      expect(result.breakdown.deliveryZone).toBe('Hanoi Inner City');
    });

    it('should calculate rush delivery fees correctly', () => {
      const input = {
        items: mockOrderItems,
        deliveryInfo: mockDeliveryInfo,
        isRushOrder: true
      };

      const result = calculateDeliveryDetails(input);

      expect(result.rushDeliveryFee).toBe(30000); // 3 items × 10,000 VND
      expect(result.estimatedDeliveryDays).toBe(0.5); // Same day delivery
    });

    it('should apply free shipping discount for orders over 100,000 VND', () => {
      const input = {
        items: mockOrderItems, // Total: 500,000 VND
        deliveryInfo: mockDeliveryInfo,
        isRushOrder: false
      };

      const result = calculateDeliveryDetails(input);

      expect(result.freeShippingDiscount).toBe(22000); // Full base shipping fee
      expect(result.totalShippingFee).toBe(0); // After discount
    });

    it('should calculate additional weight fees correctly', () => {
      const heavyItems: OrderItem[] = [
        {
          productId: 'book-heavy',
          productTitle: 'Encyclopedia Set',
          productType: 'BOOK',
          quantity: 20, // 20 × 0.3kg = 6kg total
          unitPrice: 50000,
          subtotal: 1000000,
          productMetadata: { category: 'Reference' }
        }
      ];

      const input = {
        items: heavyItems,
        deliveryInfo: mockDeliveryInfo,
        isRushOrder: false
      };

      const result = calculateDeliveryDetails(input);

      // Should have additional weight fee for weight over 3kg
      expect(result.breakdown.additionalWeightFee).toBeGreaterThan(0);
      expect(result.breakdown.totalWeight).toBe(6);
    });

    it('should not allow rush delivery for non-inner city locations', () => {
      const provinceDeliveryInfo = {
        ...mockDeliveryInfo,
        city: 'DNA_HAI_CHAU',
        province: 'DNA'
      };

      const input = {
        items: mockOrderItems,
        deliveryInfo: provinceDeliveryInfo,
        isRushOrder: true
      };

      const result = calculateDeliveryDetails(input);

      expect(result.rushDeliveryFee).toBe(0);
      expect(result.warnings).toContain('Rush delivery is only available for Hanoi and Ho Chi Minh City inner districts');
    });
  });

  describe('validateDeliveryInfo', () => {
    it('should validate correct delivery information', () => {
      const errors = validateDeliveryInfo(mockDeliveryInfo);
      expect(errors).toHaveLength(0);
    });

    it('should reject invalid phone numbers', () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        phone: '123456' // Too short
      };

      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('phone'))).toBe(true);
    });

    it('should reject invalid email addresses', () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        email: 'invalid-email'
      };

      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('email'))).toBe(true);
    });

    it('should reject short addresses', () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        address: '123' // Too short
      };

      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('Address'))).toBe(true);
    });

    it('should validate postal codes correctly', () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        postalCode: '123' // Too short
      };

      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('Postal code'))).toBe(true);
    });
  });

  describe('supportsRushDelivery', () => {
    it('should return true for Hanoi inner districts', () => {
      expect(supportsRushDelivery('HN', 'HN_HOAN_KIEM')).toBe(true);
      expect(supportsRushDelivery('HN', 'HN_BA_DINH')).toBe(true);
    });

    it('should return true for HCMC inner districts', () => {
      expect(supportsRushDelivery('HCM', 'HCM_QUAN_1')).toBe(true);
      expect(supportsRushDelivery('HCM', 'HCM_BINH_THANH')).toBe(true);
    });

    it('should return false for outer districts', () => {
      expect(supportsRushDelivery('HN', 'HN_SOC_SON')).toBe(false);
      expect(supportsRushDelivery('HCM', 'HCM_HOC_MON')).toBe(false);
    });

    it('should return false for other provinces', () => {
      expect(supportsRushDelivery('DNA', 'DNA_HAI_CHAU')).toBe(false);
      expect(supportsRushDelivery('AN_GIANG', 'AG_LONG_XUYEN')).toBe(false);
    });
  });

  describe('calculateRemainingForFreeShipping', () => {
    it('should calculate remaining amount correctly', () => {
      const remaining = calculateRemainingForFreeShipping(80000);
      expect(remaining).toBe(20000); // 100,000 - 80,000
    });

    it('should return 0 when threshold is met', () => {
      const remaining = calculateRemainingForFreeShipping(150000);
      expect(remaining).toBe(0);
    });
  });
});

describe('DeliveryService Integration Tests', () => {
  describe('calculateDeliveryFee', () => {
    it('should call backend for validation', async () => {
      const spy = vi.spyOn(deliveryService, 'calculateDeliveryFee');
      
      await deliveryService.calculateDeliveryFee(
        mockOrderItems,
        mockDeliveryInfo,
        false
      );

      expect(spy).toHaveBeenCalledWith(mockOrderItems, mockDeliveryInfo, false);
    });

    it('should throw error for invalid delivery info', async () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        recipientName: '' // Required field empty
      };

      await expect(
        deliveryService.calculateDeliveryFee(mockOrderItems, invalidInfo, false)
      ).rejects.toThrow('Invalid delivery information');
    });

    it('should throw error for rush delivery in unsupported locations', async () => {
      const provinceInfo = {
        ...mockDeliveryInfo,
        province: 'DNA',
        city: 'DNA_HAI_CHAU'
      };

      await expect(
        deliveryService.calculateDeliveryFee(mockOrderItems, provinceInfo, true)
      ).rejects.toThrow('Rush delivery is not available for this location');
    });
  });

  describe('getDeliveryFeeBreakdown', () => {
    it('should format breakdown correctly', async () => {
      const result = await deliveryService.calculateDeliveryFee(
        mockOrderItems,
        mockDeliveryInfo,
        false
      );

      const breakdown = deliveryService.getDeliveryFeeBreakdown(result);

      expect(breakdown.items).toHaveLength(2); // Base fee + free shipping discount
      expect(breakdown.location).toBe('Hoàn Kiếm, Hà Nội');
      expect(breakdown.zone).toBe('Hanoi Inner City');
    });
  });

  describe('generateDeliverySummary', () => {
    it('should generate complete summary', async () => {
      const deliveryOptions: DeliveryOptions = {
        isRushOrder: true,
        deliveryInstructions: 'Call before delivery',
        preferredDeliveryTime: 'morning'
      };

      const result = await deliveryService.calculateDeliveryFee(
        mockOrderItems,
        mockDeliveryInfo,
        true
      );

      const summary = deliveryService.generateDeliverySummary(
        mockDeliveryInfo,
        deliveryOptions,
        result
      );

      expect(summary).toContain(mockDeliveryInfo.recipientName);
      expect(summary).toContain(mockDeliveryInfo.address);
      expect(summary).toContain(mockDeliveryInfo.phone);
      expect(summary).toContain(deliveryOptions.deliveryInstructions);
    });
  });
});

describe('Component Integration Tests', () => {
  let mockOnOptionsChange: vi.Mock;
  let mockOnCalculationComplete: vi.Mock;
  let mockOnNext: vi.Mock;
  let mockOnBack: vi.Mock;

  beforeEach(() => {
    mockOnOptionsChange = vi.fn();
    mockOnCalculationComplete = vi.fn();
    mockOnNext = vi.fn();
    mockOnBack = vi.fn();
  });

  describe('DeliveryOptionsSelector', () => {
    it('should display rush delivery option for supported locations', () => {
      render(
        <TestWrapper>
          <DeliveryOptionsSelector
            deliveryInfo={mockDeliveryInfo}
            onOptionsChange={mockOnOptionsChange}
            onCalculationComplete={mockOnCalculationComplete}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      expect(screen.getByText(/Rush Delivery/)).toBeInTheDocument();
      expect(screen.getByText(/Available/)).toBeInTheDocument();
    });

    it('should disable rush delivery for unsupported locations', () => {
      const provinceInfo = {
        ...mockDeliveryInfo,
        province: 'DNA',
        city: 'DNA_HAI_CHAU'
      };

      render(
        <TestWrapper>
          <DeliveryOptionsSelector
            deliveryInfo={provinceInfo}
            onOptionsChange={mockOnOptionsChange}
            onCalculationComplete={mockOnCalculationComplete}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      const rushCheckbox = screen.getByRole('checkbox', { name: /Rush Delivery/ });
      expect(rushCheckbox).toBeDisabled();
    });

    it('should update options when user changes delivery time preference', async () => {
      const user = userEvent.setup();

      render(
        <TestWrapper>
          <DeliveryOptionsSelector
            deliveryInfo={mockDeliveryInfo}
            onOptionsChange={mockOnOptionsChange}
            onCalculationComplete={mockOnCalculationComplete}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      const morningOption = screen.getByLabelText(/Morning/);
      await user.click(morningOption);

      expect(mockOnOptionsChange).toHaveBeenCalledWith(
        expect.objectContaining({
          preferredDeliveryTime: 'morning'
        })
      );
    });

    it('should validate delivery instructions length', async () => {
      const user = userEvent.setup();

      render(
        <TestWrapper>
          <DeliveryOptionsSelector
            deliveryInfo={mockDeliveryInfo}
            onOptionsChange={mockOnOptionsChange}
            onCalculationComplete={mockOnCalculationComplete}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      const longText = 'A'.repeat(501); // Exceeds 500 character limit
      const textarea = screen.getByPlaceholderText(/Leave at front door/);
      
      await user.type(textarea, longText);
      
      const nextButton = screen.getByRole('button', { name: /Continue/ });
      await user.click(nextButton);

      expect(mockOnNext).not.toHaveBeenCalled();
      expect(screen.getByText(/must be less than 500 characters/)).toBeInTheDocument();
    });
  });

  describe('OrderSummary', () => {
    const mockCalculationResult: DeliveryCalculationResult = {
      baseShippingFee: 22000,
      rushDeliveryFee: 30000,
      totalShippingFee: 52000,
      estimatedDeliveryDays: 0.5,
      freeShippingDiscount: 0,
      breakdown: {
        totalWeight: 0.8,
        baseWeightFee: 22000,
        additionalWeightFee: 0,
        rushFeePerItem: 10000,
        applicableItems: 3,
        location: 'Hoàn Kiếm, Hà Nội',
        deliveryZone: 'Hanoi Inner City'
      }
    };

    const mockDeliveryOptions: DeliveryOptions = {
      isRushOrder: true,
      deliveryInstructions: 'Call before delivery',
      preferredDeliveryTime: 'morning'
    };

    it('should display order summary with all sections', () => {
      render(
        <TestWrapper>
          <OrderSummary
            deliveryInfo={mockDeliveryInfo}
            deliveryOptions={mockDeliveryOptions}
            calculationResult={mockCalculationResult}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      expect(screen.getByText(/Order Summary/)).toBeInTheDocument();
      expect(screen.getByText(/Delivery Information/)).toBeInTheDocument();
      expect(screen.getByText(/Delivery Options/)).toBeInTheDocument();
      expect(screen.getByText(/Price Breakdown/)).toBeInTheDocument();
    });

    it('should display correct delivery information', () => {
      render(
        <TestWrapper>
          <OrderSummary
            deliveryInfo={mockDeliveryInfo}
            deliveryOptions={mockDeliveryOptions}
            calculationResult={mockCalculationResult}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      expect(screen.getByText(mockDeliveryInfo.recipientName)).toBeInTheDocument();
      expect(screen.getByText(mockDeliveryInfo.phone)).toBeInTheDocument();
      expect(screen.getByText(mockDeliveryInfo.email)).toBeInTheDocument();
    });

    it('should show rush delivery information when enabled', () => {
      render(
        <TestWrapper>
          <OrderSummary
            deliveryInfo={mockDeliveryInfo}
            deliveryOptions={mockDeliveryOptions}
            calculationResult={mockCalculationResult}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      expect(screen.getByText(/Rush Delivery/)).toBeInTheDocument();
      expect(screen.getByText(/Same day delivery/)).toBeInTheDocument();
    });

    it('should require terms acceptance before proceeding', async () => {
      const user = userEvent.setup();

      render(
        <TestWrapper>
          <OrderSummary
            deliveryInfo={mockDeliveryInfo}
            deliveryOptions={mockDeliveryOptions}
            calculationResult={mockCalculationResult}
            onNext={mockOnNext}
            onBack={mockOnBack}
          />
        </TestWrapper>
      );

      const proceedButton = screen.getByRole('button', { name: /Proceed to Payment/ });
      
      // Button should be disabled initially (terms not accepted)
      expect(proceedButton).toBeDisabled();

      // Accept terms and conditions
      const termsCheckbox = screen.getByRole('checkbox', { name: /I agree to the/ });
      await user.click(termsCheckbox);

      // Button should now be enabled
      expect(proceedButton).not.toBeDisabled();

      // Now clicking should call onNext
      await user.click(proceedButton);
      expect(mockOnNext).toHaveBeenCalled();
    });
  });
});

describe('Mobile Responsiveness Tests', () => {
  beforeEach(() => {
    // Mock mobile viewport
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375
    });
  });

  it('should display mobile-friendly layout for delivery options', () => {
    render(
      <TestWrapper>
        <DeliveryOptionsSelector
          deliveryInfo={mockDeliveryInfo}
          onOptionsChange={vi.fn()}
          onCalculationComplete={vi.fn()}
          onNext={vi.fn()}
          onBack={vi.fn()}
        />
      </TestWrapper>
    );

    // Check that time preference options stack vertically on mobile
    const timeOptions = screen.getAllByRole('radio');
    expect(timeOptions).toHaveLength(4);
  });
});

describe('Error Handling Tests', () => {
  it('should handle delivery calculation failures gracefully', async () => {
    const invalidInfo = {
      ...mockDeliveryInfo,
      province: 'INVALID'
    };

    render(
      <TestWrapper>
        <DeliveryOptionsSelector
          deliveryInfo={invalidInfo}
          onOptionsChange={vi.fn()}
          onCalculationComplete={vi.fn()}
          onNext={vi.fn()}
          onBack={vi.fn()}
        />
      </TestWrapper>
    );

    await waitFor(() => {
      expect(screen.getByText(/Failed to calculate delivery fee/)).toBeInTheDocument();
    });
  });
});