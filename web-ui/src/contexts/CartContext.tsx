import React, { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cartService } from '../services';
import type { Cart, Product } from '../types';

interface CartContextType {
  // Cart data
  cart: Cart | null;
  items: Cart['items'];
  totalItems: number;
  totalPrice: number;
  totalPriceWithVAT: number;
  stockWarnings: Cart['stockWarnings'];
  
  // Loading states
  isLoading: boolean;
  error: string | null;
  isAddingToCart: boolean;
  isUpdatingQuantity: boolean;
  isRemovingFromCart: boolean;
  isClearingCart: boolean;
  
  // Actions
  addToCart: (productId: string, quantity?: number) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  refreshCart: () => Promise<void>;
  
  // Helpers
  getItemQuantity: (productId: string) => number;
  hasStockWarnings: () => boolean;
  isItemInCart: (productId: string) => boolean;
  canAddToCart: (product: Product, quantity?: number) => boolean;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const useCartContext = () => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCartContext must be used within a CartProvider');
  }
  return context;
};

interface CartProviderProps {
  children: ReactNode;
}

export const CartProvider: React.FC<CartProviderProps> = ({ children }) => {
  const queryClient = useQueryClient();
  const [sessionId, setSessionId] = useState<string | null>(null);

  // Initialize cart session on mount
  useEffect(() => {
    const initializeCartSession = async () => {
      const storedSessionId = cartService.getCartSessionId();
      if (storedSessionId) {
        setSessionId(storedSessionId);
      } else {
        try {
          const newSessionId = await cartService.ensureCartSession();
          setSessionId(newSessionId);
        } catch (error) {
          console.error('Failed to initialize cart session:', error);
        }
      }
    };

    initializeCartSession();
  }, []);

  // Get cart query
  const {
    data: cart,
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['cart', sessionId],
    queryFn: () => cartService.getCart(sessionId || undefined),
    enabled: !!sessionId,
    staleTime: 30 * 1000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
  });

  // Add to cart mutation
  const addToCartMutation = useMutation({
    mutationFn: ({ productId, quantity }: { 
      productId: string; 
      quantity: number; 
    }) => cartService.addToCart(productId, quantity, sessionId || undefined),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      // Update session ID if needed
      if (data.sessionId && data.sessionId !== sessionId) {
        setSessionId(data.sessionId);
        cartService.setCartSessionId(data.sessionId);
        queryClient.invalidateQueries({ queryKey: ['cart'] });
      }
    },
    onError: (error) => {
      console.error('Failed to add item to cart:', error);
    }
  });

  // Update quantity mutation
  const updateQuantityMutation = useMutation({
    mutationFn: ({ productId, quantity }: { 
      productId: string; 
      quantity: number; 
    }) => cartService.updateItemQuantity(productId, quantity, sessionId || undefined),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
    },
    onError: (error) => {
      console.error('Failed to update cart item quantity:', error);
    }
  });

  // Remove from cart mutation
  const removeFromCartMutation = useMutation({
    mutationFn: ({ productId }: { productId: string }) => 
      cartService.removeFromCart(productId, sessionId || undefined),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
    },
    onError: (error) => {
      console.error('Failed to remove item from cart:', error);
    }
  });

  // Clear cart mutation
  const clearCartMutation = useMutation({
    mutationFn: () => cartService.clearCart(sessionId || undefined),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
    },
    onError: (error) => {
      console.error('Failed to clear cart:', error);
    }
  });

  // Helper functions
  const addToCart = async (productId: string, quantity: number = 1) => {
    if (!sessionId) {
      throw new Error('Cart session not initialized');
    }
    await addToCartMutation.mutateAsync({ productId, quantity });
  };

  const updateQuantity = async (productId: string, quantity: number) => {
    if (!sessionId) {
      throw new Error('Cart session not initialized');
    }
    if (quantity <= 0) {
      await removeFromCart(productId);
    } else {
      await updateQuantityMutation.mutateAsync({ productId, quantity });
    }
  };

  const removeFromCart = async (productId: string) => {
    if (!sessionId) {
      throw new Error('Cart session not initialized');
    }
    await removeFromCartMutation.mutateAsync({ productId });
  };

  const clearCart = async () => {
    if (!sessionId) {
      throw new Error('Cart session not initialized');
    }
    await clearCartMutation.mutateAsync();
  };

  const refreshCart = async () => {
    await refetch();
  };

  const getItemQuantity = (productId: string): number => {
    const item = cart?.items.find(item => item.productId === productId);
    return item?.quantity || 0;
  };

  const hasStockWarnings = (): boolean => {
    return (cart?.stockWarnings?.length || 0) > 0;
  };

  const isItemInCart = (productId: string): boolean => {
    return getItemQuantity(productId) > 0;
  };

  const canAddToCart = (product: Product, quantity: number = 1): boolean => {
    const currentQuantity = getItemQuantity(product.id);
    const totalRequestedQuantity = currentQuantity + quantity;
    return totalRequestedQuantity <= product.quantity && product.quantity > 0;
  };

  const contextValue: CartContextType = {
    // Cart data
    cart: cart || null,
    items: cart?.items || [],
    totalItems: cart?.totalItems || 0,
    totalPrice: cart?.totalPrice || 0,
    totalPriceWithVAT: cart?.totalPriceWithVAT || 0,
    stockWarnings: cart?.stockWarnings || [],
    
    // Loading states
    isLoading,
    error: error?.message || null,
    isAddingToCart: addToCartMutation.isPending,
    isUpdatingQuantity: updateQuantityMutation.isPending,
    isRemovingFromCart: removeFromCartMutation.isPending,
    isClearingCart: clearCartMutation.isPending,
    
    // Actions
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    refreshCart,
    
    // Helpers
    getItemQuantity,
    hasStockWarnings,
    isItemInCart,
    canAddToCart,
  };

  return (
    <CartContext.Provider value={contextValue}>
      {children}
    </CartContext.Provider>
  );
};

export default CartProvider;