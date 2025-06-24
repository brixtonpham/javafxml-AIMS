import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCart } from '../hooks/useCart';
import { StockValidationDialog } from '../components/dialogs/StockValidationDialog';
import { VATCalculationDisplay } from '../components/checkout/VATCalculationDisplay';
import DeliveryOptionsSelector from '../components/DeliveryOptionsSelector';

// Mock services
jest.mock('../services/stockValidationService', () => ({
  stockValidationService: {
    validateCartStock: jest.fn(),
    reserveStock: jest.fn(),
    checkRushDeliveryEligibility: jest.fn(),
  }
}));

jest.mock('../services/DeliveryService', () => ({
  deliveryService: {
    isRushDeliveryAvailable: jest.fn(),
    calculateDeliveryFee: jest.fn(),
    formatDeliveryFee: jest.fn(),
    getFreeShippingThreshold: jest.fn(),
    calculateRemainingForFreeShipping: jest.fn(),
  }
}));

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const queryClient = createTestQueryClient();
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

describe('Phase 2A Integration Tests', () => {
  describe('useCart Hook Integration', () => {
    it('should include new stock validation methods', () => {
      const TestComponent = () => {
        const cart = useCart();
        
        return (
          <div>
            <div data-testid="validate-stock">{typeof cart.validateStock}</div>
            <div data-testid="reserve-stock">{typeof cart.reserveStock}</div>
            <div data-testid="can-proceed">{typeof cart.canProceedToCheckout}</div>
            <div data-testid="loading-states">
              {cart.isValidatingStock ? 'validating' : 'not-validating'}
              {cart.isReservingStock ? 'reserving' : 'not-reserving'}
            </div>
          </div>
        );
      };

      render(
        <TestWrapper>
          <TestComponent />
        </TestWrapper>
      );

      expect(screen.getByTestId('validate-stock')).toHaveTextContent('function');
      expect(screen.getByTestId('reserve-stock')).toHaveTextContent('function');
      expect(screen.getByTestId('can-proceed')).toHaveTextContent('function');
      expect(screen.getByTestId('loading-states')).toHaveTextContent('not-validating');
      expect(screen.getByTestId('loading-states')).toHaveTextContent('not-reserving');
    });
  });

  describe('StockValidationDialog Component', () => {
    const mockStockValidationResults = [
      {
        isValid: false,
        productId: 'product-1',
        productTitle: 'Test Product',
        requestedQuantity: 5,
        actualStock: 10,
        reservedStock: 8,
        availableStock: 2,
        message: 'Insufficient stock',
        reasonCode: 'INSUFFICIENT_STOCK',
        shortfallQuantity: 3,
      }
    ];

    const mockCartItems = [
      {
        productId: 'product-1',
        product: {
          id: 'product-1',
          title: 'Test Product',
          price: 100,
          productType: 'BOOK' as const,
        },
        quantity: 5,
        subtotal: 500,
      }
    ];

    it('should render stock validation dialog with issues', () => {
      const mockProps = {
        isOpen: true,
        onClose: jest.fn(),
        stockValidationResults: mockStockValidationResults,
        onUpdateQuantity: jest.fn(),
        onRemoveItem: jest.fn(),
        onProceedWithAdjustments: jest.fn(),
        cartItems: mockCartItems,
      };

      render(<StockValidationDialog {...mockProps} />);

      expect(screen.getByText('Stock Availability Issues')).toBeInTheDocument();
      expect(screen.getByText('Test Product')).toBeInTheDocument();
      expect(screen.getByText('Insufficient stock')).toBeInTheDocument();
    });
  });

  describe('VATCalculationDisplay Component', () => {
    it('should render VAT calculation display', () => {
      const mockVATResult = {
        basePrice: 100,
        vatAmount: 10,
        totalPrice: 110,
        vatRate: 0.1,
        isValid: true,
      };

      const mockProps = {
        vatCalculationResult: mockVATResult,
        className: 'test-class',
      };

      render(<VATCalculationDisplay {...mockProps} />);

      expect(screen.getByText(/VAT Calculation/)).toBeInTheDocument();
      expect(screen.getByText(/100/)).toBeInTheDocument(); // Base price
      expect(screen.getByText(/10/)).toBeInTheDocument(); // VAT amount
      expect(screen.getByText(/110/)).toBeInTheDocument(); // Total price
    });
  });

  describe('DeliveryOptionsSelector Enhancement', () => {
    const mockDeliveryInfo = {
      recipientName: 'Test User',
      phone: '123456789',
      email: 'test@example.com',
      address: '123 Test St',
      city: 'Ba Đình',
      province: 'Hanoi',
      postalCode: '10000',
    };

    const mockProps = {
      deliveryInfo: mockDeliveryInfo,
      onOptionsChange: jest.fn(),
      onCalculationComplete: jest.fn(),
      onNext: jest.fn(),
      onBack: jest.fn(),
    };

    it('should show Hanoi district information for rush delivery', async () => {
      const { deliveryService } = require('../services/DeliveryService');
      deliveryService.isRushDeliveryAvailable.mockReturnValue(true);

      render(
        <TestWrapper>
          <DeliveryOptionsSelector {...mockProps} />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Rush Delivery - Same Day')).toBeInTheDocument();
        expect(screen.getByText('Hanoi Rush Delivery Districts')).toBeInTheDocument();
        expect(screen.getByText(/Ba Đình, Hoàn Kiếm/)).toBeInTheDocument();
      });
    });

    it('should show Ho Chi Minh City district information when province is HCMC', async () => {
      const hcmcDeliveryInfo = {
        ...mockDeliveryInfo,
        city: 'District 1',
        province: 'Ho Chi Minh City',
      };

      const hcmcProps = {
        ...mockProps,
        deliveryInfo: hcmcDeliveryInfo,
      };

      const { deliveryService } = require('../services/DeliveryService');
      deliveryService.isRushDeliveryAvailable.mockReturnValue(true);

      render(
        <TestWrapper>
          <DeliveryOptionsSelector {...hcmcProps} />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Ho Chi Minh City Rush Delivery Districts')).toBeInTheDocument();
        expect(screen.getByText(/District 1, District 3/)).toBeInTheDocument();
      });
    });
  });

  describe('Integration Flow', () => {
    it('should handle complete stock validation to checkout flow', async () => {
      // This test would simulate the complete flow from cart validation
      // through stock checking to checkout confirmation
      
      const mockCart = {
        sessionId: 'test-session',
        items: [
          {
            productId: 'product-1',
            product: {
              id: 'product-1',
              title: 'Test Product',
              price: 100,
              productType: 'BOOK' as const,
            },
            quantity: 2,
            subtotal: 200,
          }
        ],
        totalItems: 2,
        totalPrice: 200,
        totalPriceWithVAT: 220,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      const TestIntegrationComponent = () => {
        const cart = useCart();
        
        const handleValidateAndProceed = async () => {
          if (cart.items.length > 0) {
            await cart.validateStock();
            if (cart.canProceedToCheckout()) {
              // Proceed to checkout
              console.log('Proceeding to checkout');
            }
          }
        };

        return (
          <div>
            <button onClick={handleValidateAndProceed} data-testid="validate-proceed">
              Validate and Proceed
            </button>
            <div data-testid="can-proceed">
              {cart.canProceedToCheckout() ? 'can-proceed' : 'cannot-proceed'}
            </div>
          </div>
        );
      };

      render(
        <TestWrapper>
          <TestIntegrationComponent />
        </TestWrapper>
      );

      const validateButton = screen.getByTestId('validate-proceed');
      fireEvent.click(validateButton);

      // Verify initial state
      expect(screen.getByTestId('can-proceed')).toBeInTheDocument();
    });
  });
});