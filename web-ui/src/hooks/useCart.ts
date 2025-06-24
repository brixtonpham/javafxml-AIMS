import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cartService } from '../services';
import { stockValidationService } from '../services/stockValidationService';
import type { Cart, CartItem, StockValidationResult, BulkStockValidationResult } from '../types';

export const useCart = (sessionId?: string) => {
  const queryClient = useQueryClient();

  // Get cart query
  const {
    data: cart,
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['cart', sessionId],
    queryFn: () => cartService.getCart(sessionId),
    staleTime: 30 * 1000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
  });

  // Add to cart mutation
  const addToCartMutation = useMutation({
    mutationFn: ({ productId, quantity, sessionId: sid }: { 
      productId: string; 
      quantity: number; 
      sessionId?: string;
    }) => cartService.addToCart(productId, quantity, sid || sessionId),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(['cart', sessionId], data);
      // Invalidate related product caches to ensure fresh stock data
      queryClient.invalidateQueries({ queryKey: ['product', variables.productId] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      
      // Update cart session ID if received from server
      if (data.sessionId && data.sessionId !== sessionId) {
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
    mutationFn: ({ productId, quantity, sessionId: sid }: { 
      productId: string; 
      quantity: number; 
      sessionId?: string;
    }) => cartService.updateItemQuantity(productId, quantity, sid || sessionId),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(['cart', sessionId], data);
      // Invalidate related product caches to ensure fresh stock data
      queryClient.invalidateQueries({ queryKey: ['product', variables.productId] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (error) => {
      console.error('Failed to update cart item quantity:', error);
    }
  });

  // Remove from cart mutation
  const removeFromCartMutation = useMutation({
    mutationFn: ({ productId, sessionId: sid }: { 
      productId: string; 
      sessionId?: string;
    }) => cartService.removeFromCart(productId, sid || sessionId),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(['cart', sessionId], data);
      // Invalidate related product caches to ensure fresh stock data
      queryClient.invalidateQueries({ queryKey: ['product', variables.productId] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (error) => {
      console.error('Failed to remove item from cart:', error);
    }
  });

  // Clear cart mutation
  const clearCartMutation = useMutation({
    mutationFn: ({ sessionId: sid }: { sessionId?: string }) => 
      cartService.clearCart(sid || sessionId),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      // Invalidate all product caches when cart is cleared
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ 
        predicate: (query) => query.queryKey[0] === 'product' 
      });
    },
    onError: (error) => {
      console.error('Failed to clear cart:', error);
    }
  });

  // Helper functions
  const addToCart = (productId: string, quantity: number = 1) => {
    return addToCartMutation.mutateAsync({ productId, quantity, sessionId });
  };

  const updateQuantity = (productId: string, quantity: number) => {
    if (quantity <= 0) {
      return removeFromCart(productId);
    }
    return updateQuantityMutation.mutateAsync({ productId, quantity, sessionId });
  };

  const removeFromCart = (productId: string) => {
    return removeFromCartMutation.mutateAsync({ productId, sessionId });
  };

  // Stock validation mutation
  const validateStockMutation = useMutation({
    mutationFn: async (cartItems: CartItem[]) => {
      return stockValidationService.validateCartStock(cartItems);
    },
    onError: (error) => {
      console.error('Failed to validate cart stock:', error);
    }
  });

  // Reserve stock mutation
  const reserveStockMutation = useMutation({
    mutationFn: async ({ cartItems, sessionId: sid }: {
      cartItems: CartItem[];
      sessionId?: string;
    }) => {
      const validSessionId = sid || sessionId || '';
      return stockValidationService.reserveStock(cartItems, validSessionId);
    },
    onError: (error) => {
      console.error('Failed to reserve stock:', error);
    }
  });

  const clearCart = () => {
    return clearCartMutation.mutateAsync({ sessionId });
  };

  const getItemQuantity = (productId: string): number => {
    const item = cart?.items.find(item => item.productId === productId);
    return item?.quantity || 0;
  };

  const getTotalItems = (): number => {
    return cart?.totalItems || 0;
  };

  const getTotalPrice = (): number => {
    return cart?.totalPriceWithVAT || 0;
  };

  const hasStockWarnings = (): boolean => {
    return (cart?.stockWarnings?.length || 0) > 0;
  };

  // Stock validation helper functions
  const validateStock = (cartItems?: CartItem[]) => {
    const itemsToValidate = cartItems || cart?.items || [];
    if (itemsToValidate.length === 0) return;
    
    return validateStockMutation.mutateAsync(itemsToValidate);
  };

  const reserveStock = (cartItems?: CartItem[]) => {
    const itemsToReserve = cartItems || cart?.items || [];
    if (itemsToReserve.length === 0) return;
    
    return reserveStockMutation.mutateAsync({
      cartItems: itemsToReserve,
      sessionId
    });
  };

  const canProceedToCheckout = (): boolean => {
    if (!cart || cart.items.length === 0) return false;
    if (hasStockWarnings()) return false;
    return true;
  };

  return {
    // Data
    cart,
    items: cart?.items || [],
    totalItems: getTotalItems(),
    totalPrice: getTotalPrice(),
    stockWarnings: cart?.stockWarnings || [],
    
    // State
    isLoading,
    error,
    
    // Mutations loading states
    isAddingToCart: addToCartMutation.isPending,
    isUpdatingQuantity: updateQuantityMutation.isPending,
    isRemovingFromCart: removeFromCartMutation.isPending,
    isClearingCart: clearCartMutation.isPending,
    isValidatingStock: validateStockMutation.isPending,
    isReservingStock: reserveStockMutation.isPending,
    
    // Actions
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    refetch,
    
    // Stock validation actions
    validateStock,
    reserveStock,
    canProceedToCheckout,
    
    // Helpers
    getItemQuantity,
    getTotalItems,
    getTotalPrice,
    hasStockWarnings,
  };
};

export default useCart;