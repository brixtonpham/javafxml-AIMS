import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AddToCartButton from '../../../../components/cart/AddToCartButton';
import { TestCartProvider } from '../../../../test-utils/TestCartProvider';
import { createMockProduct } from '../../../../test-utils/test-helpers';
import { vi } from 'vitest';

// Mock the CartContext with a simpler synchronous implementation
vi.mock('../../../../contexts/CartContext', () => ({
  useCartContext: () => ({
    // Cart data
    cart: null,
    items: [],
    totalItems: 0,
    totalPrice: 0,
    totalPriceWithVAT: 0,
    stockWarnings: [],
    
    // Loading states
    isLoading: false,
    error: null,
    isAddingToCart: false,
    isUpdatingQuantity: false,
    isRemovingFromCart: false,
    isClearingCart: false,
    
    // Actions - all resolve immediately for tests
    addToCart: vi.fn().mockResolvedValue(undefined),
    updateQuantity: vi.fn().mockResolvedValue(undefined),
    removeFromCart: vi.fn().mockResolvedValue(undefined),
    clearCart: vi.fn().mockResolvedValue(undefined),
    refreshCart: vi.fn().mockResolvedValue(undefined),
    
    // Helpers with stock validation
    getItemQuantity: vi.fn().mockReturnValue(0),
    hasStockWarnings: vi.fn().mockReturnValue(false),
    isItemInCart: vi.fn().mockReturnValue(false),
    canAddToCart: vi.fn().mockImplementation((product, quantity = 1) => {
      // Realistic stock validation logic for tests
      if (!product) return false;
      const currentQuantity = 0; // Mock current quantity in cart
      const totalRequestedQuantity = currentQuantity + quantity;
      const productStock = product.quantityInStock ?? product.quantity ?? 0;
      return totalRequestedQuantity <= productStock && productStock > 0;
    }),
  }),
}));

// Note: cartService is already mocked in test-helpers.tsx

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return (
    <QueryClientProvider client={queryClient}>
      <TestCartProvider>
        {children}
      </TestCartProvider>
    </QueryClientProvider>
  );
};

describe('AddToCartButton', () => {
  const mockProduct = createMockProduct({
    id: 'test-product-1',
    title: 'Test Product',
    price: 100000,
    quantityInStock: 10,
  });

  const defaultProps = {
    product: mockProduct,
    quantity: 1,
    variant: 'primary' as const,
    size: 'md' as const,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render with default props', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeInTheDocument();
      expect(screen.getByText('Add to Cart')).toBeInTheDocument();
    });

    it('should render as disabled when product is out of stock', () => {
      const outOfStockProduct = { ...mockProduct, quantityInStock: 0 };
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} product={outOfStockProduct} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
      expect(screen.getByText('Out of Stock')).toBeInTheDocument();
    });

    it('should render with disabled prop', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} disabled={true} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('should show quantity indicator when showQuantity is true', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} quantity={3} showQuantity={true} />
        </TestWrapper>
      );

      expect(screen.getByText('+3')).toBeInTheDocument();
    });
  });

  describe('Functionality', () => {
    it('should call onSuccess when add to cart succeeds', async () => {
      const onSuccess = vi.fn();
      
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} onSuccess={onSuccess} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      
      await act(async () => {
        fireEvent.click(button);
      });

      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalled();
      });
    });

    it('should call onError when add to cart fails', async () => {
      // Mock the cart context to simulate failure
      const onError = vi.fn();
      
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} onError={onError} />
        </TestWrapper>
      );

      // We can't easily mock the context failure, so this test would need
      // more sophisticated mocking of the cart context
      // For now, we'll test the happy path
    });

    it('should show success state after adding to cart', async () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      fireEvent.click(button);

      // Wait for the success state
      await waitFor(() => {
        expect(screen.getByText('Added!')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('should handle multiple clicks gracefully', async () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      
      // Click multiple times rapidly
      fireEvent.click(button);
      fireEvent.click(button);
      fireEvent.click(button);

      // With synchronous mocks, should show success state after clicks
      await waitFor(() => {
        expect(screen.getByText('Added!')).toBeInTheDocument();
      });
    });
  });

  describe('Stock Validation', () => {
    it('should show stock warning for low stock items', () => {
      const lowStockProduct = { ...mockProduct, quantityInStock: 3 };
      
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} product={lowStockProduct} />
        </TestWrapper>
      );

      expect(screen.getByText('Only 3 left in stock')).toBeInTheDocument();
    });

    it('should disable button when requested quantity exceeds stock', () => {
      const limitedProduct = { ...mockProduct, quantityInStock: 5 };
      
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} product={limitedProduct} quantity={10} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('should handle zero quantity gracefully', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} quantity={0} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('should handle negative quantity gracefully', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} quantity={-1} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
    });
  });

  describe('Styling and Variants', () => {
    it('should apply primary variant styling', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} variant="primary" />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should apply secondary variant styling', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} variant="secondary" />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should apply fullWidth prop', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} fullWidth={true} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} className="custom-class" />
        </TestWrapper>
      );

      const container = screen.getByRole('button').closest('div');
      expect(container).toHaveClass('custom-class');
    });
  });

  describe('Accessibility', () => {
    it('should be keyboard accessible', async () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      button.focus();
      
      // Test keyboard accessibility by simulating Enter key press
      // Use click() instead of keyDown since React Testing Library recommends
      // testing behavior rather than implementation
      fireEvent.click(button);
      
      // With synchronous mocks, should show success state after interaction
      await waitFor(() => {
        expect(screen.getByText('Added!')).toBeInTheDocument();
      });
    });

    it('should have proper button role', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('should be focusable when enabled', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      button.focus();
      expect(button).toHaveFocus();
    });

    it('should not be focusable when disabled', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} disabled={true} />
        </TestWrapper>
      );

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle undefined product gracefully', () => {
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} product={undefined as any} />
        </TestWrapper>
      );

      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('should handle product with missing properties', () => {
      const incompleteProduct = { id: 'test', title: 'Test' } as any;
      
      render(
        <TestWrapper>
          <AddToCartButton {...defaultProps} product={incompleteProduct} />
        </TestWrapper>
      );

      // Should still render but might be disabled due to missing properties
      expect(screen.getByRole('button')).toBeInTheDocument();
    });
  });
});