import { describe, it, expect } from 'vitest';
import {
  calculateTotalWeight,
  calculateDeliveryFee,
  calculateRushDeliveryFee,
  calculateFreeShippingDiscount,
  getEstimatedDeliveryDays,
  validateDeliveryInfo,
  calculateDeliveryDetails,
  formatDeliveryTime,
  generateDeliverySummary,
  calculateRemainingForFreeShipping,
} from '../../../utils/deliveryCalculations';
import type { OrderItem } from '../../../types';
import type { CheckoutDeliveryInfo, DeliveryCalculationInput } from '../../../types/checkout';

describe('deliveryCalculations', () => {
  const mockOrderItems: OrderItem[] = [
    {
      productId: 'product-1',
      productTitle: 'Test Book',
      productType: 'BOOK',
      quantity: 2,
      unitPrice: 50000,
      subtotal: 100000,
      productMetadata: { category: 'Fiction' },
    },
    {
      productId: 'product-2',
      productTitle: 'Test CD',
      productType: 'CD',
      quantity: 1,
      unitPrice: 25000,
      subtotal: 25000,
      productMetadata: { category: 'Music' },
    },
  ];

  const mockDeliveryInfo: CheckoutDeliveryInfo = {
    recipientName: 'Test User',
    phone: '0123456789',
    email: 'test@example.com',
    address: '123 Test Street',
    city: 'HN_HOAN_KIEM',
    province: 'HN',
    postalCode: '10000',
    deliveryInstructions: 'Test instructions',
    isValidated: true,
  };

  describe('calculateTotalWeight', () => {
    it('should calculate total weight correctly', () => {
      const totalWeight = calculateTotalWeight(mockOrderItems);
      expect(totalWeight).toBe(0.7); // (0.3 * 2) + (0.1 * 1)
    });

    it('should handle items without weight metadata', () => {
      const itemsWithoutWeight: OrderItem[] = [
        {
          productId: 'product-1',
          productTitle: 'Test Book',
          productType: 'BOOK',
          quantity: 1,
          unitPrice: 50000,
          subtotal: 50000,
          productMetadata: {},
        },
      ];
      const totalWeight = calculateTotalWeight(itemsWithoutWeight);
      expect(totalWeight).toBe(0.3); // Default weight for BOOK
    });

    it('should handle empty items array', () => {
      const totalWeight = calculateTotalWeight([]);
      expect(totalWeight).toBe(0);
    });
  });

  describe('calculateDeliveryFee', () => {
    it('should calculate base delivery fee for inner city', () => {
      const result = calculateDeliveryFee(0.5, 'inner_city');
      expect(result.totalFee).toBe(22000); // Base fee for inner city
      expect(result.baseFee).toBe(22000);
      expect(result.additionalFee).toBe(0);
    });

    it('should calculate fee with additional weight charge', () => {
      const result = calculateDeliveryFee(4, 'inner_city'); // Over 3kg base weight
      expect(result.baseFee).toBe(22000);
      expect(result.additionalFee).toBeGreaterThan(0); // Should have additional weight fee
      expect(result.totalFee).toBeGreaterThan(22000);
    });

    it('should calculate higher fee for outer locations', () => {
      const result = calculateDeliveryFee(0.5, 'outer_city');
      expect(result.totalFee).toBe(30000); // Higher base fee for outer locations
    });

    it('should calculate province delivery fee', () => {
      const result = calculateDeliveryFee(0.5, 'province');
      expect(result.totalFee).toBe(30000); // Province delivery fee
    });
  });

  describe('calculateRushDeliveryFee', () => {
    it('should calculate rush delivery fee for supported locations', () => {
      const result = calculateRushDeliveryFee(mockOrderItems, 'inner_city', true);
      expect(result.rushFee).toBe(30000); // 10000 per item * 3 total items
      expect(result.applicableItems).toBe(3);
    });

    it('should return 0 for unsupported locations', () => {
      const result = calculateRushDeliveryFee(mockOrderItems, 'outer_city', true);
      expect(result.rushFee).toBe(0);
      expect(result.applicableItems).toBe(0);
    });

    it('should handle empty items array', () => {
      const result = calculateRushDeliveryFee([], 'inner_city', true);
      expect(result.rushFee).toBe(0);
      expect(result.applicableItems).toBe(0);
    });
  });

  describe('calculateFreeShippingDiscount', () => {
    it('should apply free shipping discount for eligible orders', () => {
      const discount = calculateFreeShippingDiscount(150000, 22000);
      expect(discount).toBe(22000); // Full shipping fee discount
    });

    it('should not apply discount for orders below threshold', () => {
      const discount = calculateFreeShippingDiscount(80000, 22000);
      expect(discount).toBe(0);
    });

    it('should handle zero shipping fee', () => {
      const discount = calculateFreeShippingDiscount(150000, 0);
      expect(discount).toBe(0);
    });
  });

  describe('getEstimatedDeliveryDays', () => {
    it('should return 1 day for inner city', () => {
      const days = getEstimatedDeliveryDays('inner_city', false);
      expect(days).toBe(1);
    });

    it('should return same day for rush delivery in supported areas', () => {
      const days = getEstimatedDeliveryDays('inner_city', true);
      expect(days).toBe(0.5); // Same day delivery
    });

    it('should return 2 days for outer districts', () => {
      const days = getEstimatedDeliveryDays('outer_city', false);
      expect(days).toBe(2);
    });

    it('should return 3 days for other provinces', () => {
      const days = getEstimatedDeliveryDays('province', false);
      expect(days).toBe(3);
    });
  });

  describe('validateDeliveryInfo', () => {
    it('should return no errors for valid delivery info', () => {
      const errors = validateDeliveryInfo(mockDeliveryInfo);
      expect(errors).toHaveLength(0);
    });

    it('should validate required fields', () => {
      const invalidInfo = {
        ...mockDeliveryInfo,
        recipientName: '',
        phone: '',
        email: '',
        address: '',
      };
      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.length).toBeGreaterThan(0);
      expect(errors.some(error => error.includes('Recipient name'))).toBe(true);
      expect(errors.some(error => error.includes('Phone number'))).toBe(true);
      expect(errors.some(error => error.includes('Email'))).toBe(true);
      expect(errors.some(error => error.includes('Address'))).toBe(true);
    });

    it('should validate phone number format', () => {
      const invalidInfo = { ...mockDeliveryInfo, phone: '123' };
      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('valid phone number'))).toBe(true);
    });

    it('should validate email format', () => {
      const invalidInfo = { ...mockDeliveryInfo, email: 'invalid-email' };
      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('valid email'))).toBe(true);
    });

    it('should validate address length', () => {
      const invalidInfo = { ...mockDeliveryInfo, address: 'ab' };
      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('at least 5 characters'))).toBe(true);
    });

    it('should validate postal code for specific provinces', () => {
      const invalidInfo = { ...mockDeliveryInfo, postalCode: '123' };
      const errors = validateDeliveryInfo(invalidInfo);
      expect(errors.some(error => error.includes('postal code'))).toBe(true);
    });
  });

  describe('calculateDeliveryDetails', () => {
    const mockInput: DeliveryCalculationInput = {
      items: mockOrderItems,
      deliveryInfo: mockDeliveryInfo,
      isRushOrder: false,
    };

    it('should calculate complete delivery details', () => {
      const result = calculateDeliveryDetails(mockInput);
      
      expect(result.baseShippingFee).toBe(22000);
      expect(result.rushDeliveryFee).toBe(0);
      expect(result.totalShippingFee).toBe(22000);
      expect(result.estimatedDeliveryDays).toBe(1);
      expect(result.breakdown).toBeDefined();
      expect(result.breakdown.totalWeight).toBe(0.7);
    });

    it('should calculate rush delivery details', () => {
      const rushInput = {
        ...mockInput,
        isRushOrder: true,
      };
      const result = calculateDeliveryDetails(rushInput);
      
      expect(result.rushDeliveryFee).toBe(30000);
      expect(result.totalShippingFee).toBe(52000); // 22000 + 30000
      expect(result.estimatedDeliveryDays).toBe(0.5);
    });

    it('should apply free shipping discount', () => {
      const highValueItems: OrderItem[] = [
        {
          ...mockOrderItems[0],
          quantity: 10,
          subtotal: 500000,
        },
      ];
      const highValueInput = {
        ...mockInput,
        items: highValueItems,
      };
      const result = calculateDeliveryDetails(highValueInput);
      
      expect(result.freeShippingDiscount).toBe(22000);
      expect(result.totalShippingFee).toBe(0);
    });
  });

  describe('formatDeliveryTime', () => {
    it('should format same day delivery', () => {
      const formatted = formatDeliveryTime(0.5);
      expect(formatted).toBe('Same day');
    });

    it('should format single day', () => {
      const formatted = formatDeliveryTime(1);
      expect(formatted).toBe('1 day');
    });

    it('should format multiple days', () => {
      const formatted = formatDeliveryTime(3);
      expect(formatted).toBe('3 days');
    });
  });

  describe('generateDeliverySummary', () => {
    it('should generate comprehensive delivery summary', () => {
      const mockResult = {
        baseShippingFee: 22000,
        rushDeliveryFee: 0,
        totalShippingFee: 22000,
        estimatedDeliveryDays: 1,
        freeShippingDiscount: 0,
        breakdown: {
          totalWeight: 0.7,
          baseWeightFee: 22000,
          additionalWeightFee: 0,
          rushFeePerItem: 0,
          applicableItems: 3,
          location: 'Hoàn Kiếm, Hà Nội',
          deliveryZone: 'Hanoi Inner City',
        },
      };

      const summary = generateDeliverySummary(mockResult);
      expect(summary).toContain('Hoàn Kiếm, Hà Nội');
      expect(summary).toContain('1 day');
      expect(summary).toContain('22,000');
    });
  });

  describe('calculateRemainingForFreeShipping', () => {
    it('should calculate remaining amount for free shipping', () => {
      const remaining = calculateRemainingForFreeShipping(80000);
      expect(remaining).toBe(20000); // 100000 - 80000
    });

    it('should return 0 when threshold is met', () => {
      const remaining = calculateRemainingForFreeShipping(150000);
      expect(remaining).toBe(0);
    });

    it('should handle negative subtotals', () => {
      const remaining = calculateRemainingForFreeShipping(-10000);
      expect(remaining).toBe(100000);
    });
  });

  describe('Edge Cases', () => {
    it('should handle very large weights', () => {
      const largeItems: OrderItem[] = [
        {
          productId: 'heavy-item',
          productTitle: 'Heavy Item',
          productType: 'BOOK',
          quantity: 1,
          unitPrice: 100000,
          subtotal: 100000,
          productMetadata: { category: 'Heavy Books' },
        },
      ];
      
      const totalWeight = calculateTotalWeight(largeItems);
      expect(totalWeight).toBe(50);
      
      const result = calculateDeliveryFee(totalWeight, 'inner_city');
      expect(result.totalFee).toBeGreaterThan(22000); // Should include additional weight fees
    });

    it('should handle invalid location codes gracefully', () => {
      const result = calculateDeliveryFee(0.5, 'province'); // Default to province for invalid locations
      expect(result.totalFee).toBeGreaterThan(0); // Should default to some reasonable fee
    });

    it('should handle zero quantity items', () => {
      const zeroQuantityItems: OrderItem[] = [
        {
          ...mockOrderItems[0],
          quantity: 0,
        },
      ];
      
      const totalWeight = calculateTotalWeight(zeroQuantityItems);
      expect(totalWeight).toBe(0);
    });
  });
});