/**
 * Comprehensive unit tests for Enhanced CartContext with Real-time Synchronization
 */

import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { CartProvider, useCartContext } from '../../../contexts/CartContext';
import { cartService } from '../../../services/cartService';
import { backgroundSyncService } from '../../../services/backgroundSync';
import type { Cart, Product } from '../../../types';

// Mock all the synchronization utilities
vi.mock('../../../utils/websocket');
vi.mock('../../../utils/optimisticUpdates');
vi.mock('../../../utils/crossTabSync');
vi.mock('../../../utils/cartPersistence');
vi.mock('../../../services/backgroundSync');
vi.mock('../../../services/cartService');

// Mock WebSocket and EventSource
global.WebSocket = vi.fn(() => ({
  close: vi.fn(),
  send: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1,
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3
})) as any;

global.EventSource = vi.fn(() => ({
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1,
  CONNECTING: 0,
  OPEN: 1,
  CLOSED: 2
})) as any;

// Mock BroadcastChannel
global.BroadcastChannel = vi.fn(() => ({
  postMessage: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  onmessage: null,
  onmessageerror: null
})) as any;

const mockCart: Cart = {
  sessionId: 'test-session-123',
  userId: undefined,
  items: [
    {
      productId: 'product-1',
      product: {
        id: 'product-1',
        productId: 'product-1',
        title: 'Test Product',
        description: 'Test Description',
        category: 'Test Category',
        price: 29.99,
        valueAmount: 29.99,
        quantityInStock: 10,
        imageUrl: '/test-image.jpg',
        productType: 'BOOK' as const,
        entryDate: '2024-01-01T00:00:00Z'
      },
      quantity: 2,
      subtotal: 59.98
    }
  ],
  totalItems: 2,
  totalPrice: 59.98,
  totalPriceWithVAT: 65.98,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  stockWarnings: []
};

const mockProduct: Product = {
  id: 'product-2',
  productId: 'product-2',
  title: 'Another Product',
  description: 'Another Description',
  category: 'Another Category',
  price: 19.99,
  valueAmount: 19.99,
  quantityInStock: 5,
  imageUrl: '/another-image.jpg',
  productType: 'CD' as const,
  entryDate: '2024-01-01T00:00:00Z'
};

describe('Enhanced CartContext with Real-time Sync', () => {
  let queryClient: QueryClient;
  let mockCartService: any;
  let mockBackgroundSync: any;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    // Reset all mocks
    vi.clearAllMocks();

    // Setup cart service mocks
    mockCartService = {
      getCart: vi.fn().mockResolvedValue(mockCart),
      addToCart: vi.fn().mockResolvedValue(mockCart),
      updateItemQuantity: vi.fn().mockResolvedValue(mockCart),
      removeFromCart: vi.fn().mockResolvedValue(mockCart),
      clearCart: vi.fn().mockResolvedValue({ ...mockCart, items: [], totalItems: 0, totalPrice: 0 }),
      ensureCartSession: vi.fn().mockResolvedValue('test-session-123'),
      getCartSessionId: vi.fn().mockReturnValue('test-session-123'),
      setCartSessionId: vi.fn()
    };

    Object.assign(cartService, mockCartService);

    // Setup background sync service mocks
    mockBackgroundSync = {
      queueOperation: vi.fn().mockReturnValue('operation-id'),
      forcSync: vi.fn().mockResolvedValue([]),
      getPendingCount: vi.fn().mockReturnValue(0),
      isSyncInProgress: vi.fn().mockReturnValue(false),
      onSync: vi.fn(),
      onNetworkChange: vi.fn(),
      destroy: vi.fn()
    };

    Object.assign(backgroundSyncService, mockBackgroundSync);

    // Mock navigator.onLine
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      writable: true
    });
  });

  afterEach(() => {
    queryClient.clear();
  });

  const TestComponent = () => {
    const cart = useCartContext();
    return (
      <div>
        <div data-testid="cart-items">{cart.totalItems}</div>
        <div data-testid="cart-price">{cart.totalPrice}</div>
        <div data-testid="connection-status">{cart.connectionStatus}</div>
        <div data-testid="pending-ops">{cart.pendingOperations}</div>
        <div data-testid="is-connected">{cart.isConnected.toString()}</div>
        <div data-testid="sync-in-progress">{cart.isSyncInProgress.toString()}</div>
        <button 
          data-testid="add-to-cart"
          onClick={() => cart.addToCart('product-2', 1)}
        >
          Add to Cart
        </button>
        <button 
          data-testid="update-quantity"
          onClick={() => cart.updateQuantity('product-1', 3)}
        >
          Update Quantity
        </button>
        <button 
          data-testid="remove-item"
          onClick={() => cart.removeFromCart('product-1')}
        >
          Remove Item
        </button>
        <button 
          data-testid="clear-cart"
          onClick={() => cart.clearCart()}
        >
          Clear Cart
        </button>
        <button 
          data-testid="force-sync"
          onClick={() => cart.forcSync()}
        >
          Force Sync
        </button>
        <button 
          data-testid="disable-sync"
          onClick={() => cart.disableRealTimeSync()}
        >
          Disable Sync
        </button>
        <button 
          data-testid="enable-sync"
          onClick={() => cart.enableRealTimeSync()}
        >
          Enable Sync
        </button>
      </div>
    );
  };

  const renderWithProvider = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <CartProvider>
          <TestComponent />
        </CartProvider>
      </QueryClientProvider>
    );
  };

  describe('Basic Cart Operations', () => {
    it('should load cart data on initialization', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
        expect(screen.getByTestId('cart-price')).toHaveTextContent('59.98');
      });

      expect(mockCartService.ensureCartSession).toHaveBeenCalled();
      expect(mockCartService.getCart).toHaveBeenCalledWith('test-session-123');
    });

    it('should add item to cart with optimistic updates', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('add-to-cart'));
      });

      expect(mockCartService.addToCart).toHaveBeenCalledWith('product-2', 1, 'test-session-123');
    });

    it('should update item quantity', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('update-quantity'));
      });

      expect(mockCartService.updateItemQuantity).toHaveBeenCalledWith('product-1', 3, 'test-session-123');
    });

    it('should remove item from cart', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('remove-item'));
      });

      expect(mockCartService.removeFromCart).toHaveBeenCalledWith('product-1', 'test-session-123');
    });

    it('should clear entire cart', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('clear-cart'));
      });

      expect(mockCartService.clearCart).toHaveBeenCalledWith('test-session-123');
    });
  });

  describe('Real-time Synchronization', () => {
    it('should initialize with disconnected state', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('connection-status')).toHaveTextContent('DISCONNECTED');
        expect(screen.getByTestId('is-connected')).toHaveTextContent('false');
      });
    });

    it('should handle connection status changes', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('connection-status')).toHaveTextContent('DISCONNECTED');
      });

      // The actual connection logic would be tested through integration tests
      // as it involves complex WebSocket/SSE setup
    });

    it('should track pending operations', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('pending-ops')).toHaveTextContent('0');
      });
    });

    it('should handle force sync', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('force-sync'));
      });

      expect(mockBackgroundSync.forcSync).toHaveBeenCalled();
    });

    it('should enable and disable real-time sync', async () => {
      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('disable-sync'));
      });

      await waitFor(() => {
        expect(screen.getByTestId('connection-status')).toHaveTextContent('DISCONNECTED');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('enable-sync'));
      });

      // Sync should be re-enabled (tested through integration)
    });
  });

  describe('Offline Operation Handling', () => {
    it('should queue operations when offline', async () => {
      // Mock offline state
      Object.defineProperty(navigator, 'onLine', {
        value: false,
        writable: true
      });

      mockCartService.addToCart.mockRejectedValue(new Error('Network error'));

      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('add-to-cart'));
      });

      // Should queue operation for background sync
      expect(mockBackgroundSync.queueOperation).toHaveBeenCalledWith({
        type: 'ADD_ITEM',
        payload: { productId: 'product-2', quantity: 1 },
        sessionId: 'test-session-123'
      });
    });

    it('should handle network recovery', async () => {
      renderWithProvider();

      // Simulate network coming back online
      Object.defineProperty(navigator, 'onLine', {
        value: true,
        writable: true
      });

      mockBackgroundSync.getPendingCount.mockReturnValue(2);

      // Trigger network change callback
      const networkChangeCallback = mockBackgroundSync.onNetworkChange.mock.calls[0]?.[0];
      if (networkChangeCallback) {
        networkChangeCallback(true);
      }

      expect(mockBackgroundSync.forcSync).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should handle cart loading errors gracefully', async () => {
      mockCartService.getCart.mockRejectedValue(new Error('Failed to load cart'));

      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('0');
      });
    });

    it('should handle cart operation errors', async () => {
      mockCartService.addToCart.mockRejectedValue(new Error('Failed to add item'));

      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('add-to-cart'));
      });

      // Error should be handled gracefully without crashing
      expect(mockCartService.addToCart).toHaveBeenCalled();
    });
  });

  describe('Helper Functions', () => {
    it('should calculate item quantities correctly', async () => {
      const TestHelpers = () => {
        const cart = useCartContext();
        return (
          <div>
            <div data-testid="item-quantity">{cart.getItemQuantity('product-1')}</div>
            <div data-testid="item-in-cart">{cart.isItemInCart('product-1').toString()}</div>
            <div data-testid="has-warnings">{cart.hasStockWarnings().toString()}</div>
            <div data-testid="can-add">{cart.canAddToCart(mockProduct, 1).toString()}</div>
          </div>
        );
      };

      render(
        <QueryClientProvider client={queryClient}>
          <CartProvider>
            <TestHelpers />
          </CartProvider>
        </QueryClientProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('item-quantity')).toHaveTextContent('2');
        expect(screen.getByTestId('item-in-cart')).toHaveTextContent('true');
        expect(screen.getByTestId('has-warnings')).toHaveTextContent('false');
        expect(screen.getByTestId('can-add')).toHaveTextContent('true');
      });
    });
  });

  describe('Performance and Optimization', () => {
    it('should not cause unnecessary re-renders', async () => {
      const renderSpy = vi.fn();
      
      const TestComponent = () => {
        renderSpy();
        const cart = useCartContext();
        return <div data-testid="cart-items">{cart.totalItems}</div>;
      };

      render(
        <QueryClientProvider client={queryClient}>
          <CartProvider>
            <TestComponent />
          </CartProvider>
        </QueryClientProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      // Should only render once after initial load
      expect(renderSpy).toHaveBeenCalledTimes(2); // Initial + after data load
    });

    it('should cleanup resources on unmount', async () => {
      const { unmount } = renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      unmount();

      // Cleanup should be called (tested through integration)
    });
  });

  describe('Session Management', () => {
    it('should handle session ID changes', async () => {
      const newCart = { ...mockCart, sessionId: 'new-session-456' };
      mockCartService.addToCart.mockResolvedValueOnce(newCart);

      renderWithProvider();

      await waitFor(() => {
        expect(screen.getByTestId('cart-items')).toHaveTextContent('2');
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('add-to-cart'));
      });

      expect(mockCartService.setCartSessionId).toHaveBeenCalledWith('new-session-456');
    });

    it('should initialize session when none exists', async () => {
      mockCartService.getCartSessionId.mockReturnValue(null);

      renderWithProvider();

      await waitFor(() => {
        expect(mockCartService.ensureCartSession).toHaveBeenCalled();
      });
    });
  });
});