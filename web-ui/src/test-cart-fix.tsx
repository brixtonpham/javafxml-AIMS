import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CartProvider, useCartContext } from './contexts/CartContext';

// Mock the cart service to avoid network calls
vi.mock('./services/cartService', () => ({
  cartService: {
    getCartSessionId: () => 'test-session',
    ensureCartSession: () => Promise.resolve('test-session'),
    getCart: () => Promise.resolve({
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: []
    }),
    addToCart: () => Promise.resolve({
      sessionId: 'test-session',
      items: [{
        productId: 'test-product',
        product: {
          id: 'test-product',
          productId: 'test-product',
          title: 'Test Product',
          price: 10.00,
          valueAmount: 10.00,
          quantityInStock: 5,
          category: 'test',
          description: 'test description',
          productType: 'BOOK',
          entryDate: '2024-01-01'
        },
        quantity: 1,
        subtotal: 10.00
      }],
      totalItems: 1,
      totalPrice: 10.00,
      totalPriceWithVAT: 11.00,
      stockWarnings: []
    })
  }
}));

const TestComponent = () => {
  const { addToCart, isLoading } = useCartContext();
  
  const handleAddToCart = () => {
    const testProduct = {
      id: 'test-product',
      productId: 'test-product', 
      title: 'Test Product',
      price: 10.00,
      valueAmount: 10.00,
      quantityInStock: 5,
      category: 'test',
      description: 'test description',
      productType: 'BOOK' as const,
      entryDate: '2024-01-01'
    };
    
    // This should work now without throwing the "Cannot read properties of undefined (reading 'price')" error
    addToCart('test-product', 1, testProduct);
  };

  return (
    <div>
      <button onClick={handleAddToCart} disabled={isLoading}>
        Add to Cart
      </button>
      {isLoading && <div>Loading...</div>}
    </div>
  );
};

const App = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>
      <CartProvider>
        <TestComponent />
      </CartProvider>
    </QueryClientProvider>
  );
};

export default App;
