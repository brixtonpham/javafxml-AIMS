import React, { createContext, useContext, useEffect, useState, useRef, useCallback } from 'react';
import type { ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cartService } from '../services';
import type { Cart, Product } from '../types';
import { WebSocketManager, SSEManager, type WebSocketMessage } from '../utils/websocket';
import { OptimisticUpdateManager, type OptimisticOperation, ConflictResolutionStrategies } from '../utils/optimisticUpdates';
import { createTabSyncManager, type TabSyncMessage } from '../utils/crossTabSync';
import { CartPersistenceManager } from '../utils/cartPersistence';
import { backgroundSyncService, type SyncResult } from '../services/backgroundSync';

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
  
  // Real-time sync states
  isConnected: boolean;
  connectionStatus: 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING';
  pendingOperations: number;
  isSyncInProgress: boolean;
  lastSyncTime: Date | null;
  
  // Actions
  addToCart: (productId: string, quantity?: number, product?: Product) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  refreshCart: () => Promise<void>;
  
  // Sync actions
  forcSync: () => Promise<void>;
  enableRealTimeSync: () => void;
  disableRealTimeSync: () => void;
  
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
  
  // Real-time sync state
  const [isConnected, setIsConnected] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING'>('DISCONNECTED');
  const [pendingOperations, setPendingOperations] = useState(0);
  const [isSyncInProgress, setIsSyncInProgress] = useState(false);
  const [lastSyncTime, setLastSyncTime] = useState<Date | null>(null);
  const [realTimeSyncEnabled, setRealTimeSyncEnabled] = useState(true);

  // Initialize sync managers
  const wsManagerRef = useRef<WebSocketManager | null>(null);
  const sseManagerRef = useRef<SSEManager | null>(null);
  const optimisticManagerRef = useRef<OptimisticUpdateManager | null>(null);
  const tabSyncManagerRef = useRef<ReturnType<typeof createTabSyncManager> | null>(null);
  const persistenceManagerRef = useRef<CartPersistenceManager | null>(null);

  // Initialize cart session and sync managers
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

  // Initialize sync managers when session is available
  useEffect(() => {
    if (!sessionId || !realTimeSyncEnabled) return;

    // Initialize optimistic update manager
    optimisticManagerRef.current = new OptimisticUpdateManager({
      maxPendingOperations: 10,
      operationTimeout: 30000,
      maxRetries: 3,
      conflictResolution: ConflictResolutionStrategies.MERGE_ITEMS
    });

    // Initialize cart persistence manager
    persistenceManagerRef.current = new CartPersistenceManager(
      {
        storageKey: `aims_cart_${sessionId}`,
        maxSnapshots: 5,
        autoSaveInterval: 30000,
        checksumValidation: true
      },
      {
        maxAge: 24 * 60 * 60 * 1000, // 24 hours
        validateIntegrity: true,
        mergeStrategy: 'MERGE_ITEMS'
      }
    );

    persistenceManagerRef.current.setCurrentSession(sessionId);

    // Initialize cross-tab sync manager
    tabSyncManagerRef.current = createTabSyncManager(
      {
        channelName: `aims-cart-sync-${sessionId}`,
        debounceMs: 100,
        ignoreOwnMessages: true
      },
      {
        onCartUpdated: (cart: Cart) => {
          queryClient.setQueryData(['cart', sessionId], cart);
          setLastSyncTime(new Date());
        },
        onCartCleared: () => {
          queryClient.setQueryData(['cart', sessionId], null);
          setLastSyncTime(new Date());
        },
        onItemAdded: () => {
          refetch();
        },
        onItemRemoved: () => {
          refetch();
        },
        onQuantityUpdated: () => {
          refetch();
        },
        onSessionChanged: (payload) => {
          if (payload.newSessionId !== sessionId) {
            setSessionId(payload.newSessionId);
            cartService.setCartSessionId(payload.newSessionId);
          }
        },
        onError: (error) => {
          console.error('Cross-tab sync error:', error);
        }
      }
    );

    // Try to establish WebSocket connection for real-time sync
    const wsUrl = `${import.meta.env.VITE_WS_URL || 'ws://localhost:8080'}/cart-sync/${sessionId}`;
    wsManagerRef.current = new WebSocketManager(
      {
        url: wsUrl,
        heartbeatInterval: 30000,
        reconnectInterval: 5000,
        maxReconnectAttempts: 10,
        connectionTimeout: 15000
      },
      {
        onOpen: () => {
          setIsConnected(true);
          setConnectionStatus('CONNECTED');
          console.log('WebSocket connected for real-time cart sync');
        },
        onMessage: (message: WebSocketMessage) => {
          handleRealtimeMessage(message);
        },
        onClose: () => {
          setIsConnected(false);
          setConnectionStatus('DISCONNECTED');
          console.log('WebSocket disconnected');
        },
        onError: (error) => {
          console.error('WebSocket error:', error);
          setIsConnected(false);
          setConnectionStatus('DISCONNECTED');
          
          // Fallback to SSE if WebSocket fails
          initializeSSE();
        },
        onReconnecting: (attempt) => {
          setConnectionStatus('RECONNECTING');
          console.log(`WebSocket reconnecting (attempt ${attempt})`);
        },
        onReconnected: () => {
          setIsConnected(true);
          setConnectionStatus('CONNECTED');
          console.log('WebSocket reconnected');
        },
        onMaxReconnectAttemptsReached: () => {
          console.warn('WebSocket max reconnect attempts reached, falling back to SSE');
          initializeSSE();
        }
      }
    );

    // Try to connect WebSocket
    setConnectionStatus('CONNECTING');
    wsManagerRef.current?.connect()?.catch(() => {
      console.warn('WebSocket connection failed, falling back to SSE');
      initializeSSE();
    });

    // Setup background sync service
    backgroundSyncService.onSync((results: SyncResult[]) => {
      setIsSyncInProgress(false);
      setPendingOperations(backgroundSyncService.getPendingCount());
      setLastSyncTime(new Date());
      
      // Handle sync results
      results.forEach(result => {
        if (result.success && result.cart) {
          queryClient.setQueryData(['cart', sessionId], result.cart);
          persistenceManagerRef.current?.saveSnapshot(result.cart);
        }
      });
    });

    backgroundSyncService.onNetworkChange((isOnline) => {
      if (isOnline && pendingOperations > 0) {
        backgroundSyncService.forcSync();
      }
    });

    return () => {
      // Cleanup managers
      wsManagerRef.current?.close();
      sseManagerRef.current?.close();
      optimisticManagerRef.current?.reset();
      tabSyncManagerRef.current?.destroy();
      persistenceManagerRef.current?.destroy();
    };
  }, [sessionId, realTimeSyncEnabled, queryClient]);

  // Initialize SSE fallback
  const initializeSSE = useCallback(() => {
    if (!sessionId || sseManagerRef.current) return;

    const sseUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/cart-events/${sessionId}`;
    sseManagerRef.current = new SSEManager(
      {
        url: sseUrl,
        withCredentials: true,
        reconnectInterval: 5000,
        maxReconnectAttempts: 10
      },
      {
        onOpen: () => {
          setIsConnected(true);
          setConnectionStatus('CONNECTED');
          console.log('SSE connected for real-time cart sync');
        },
        onMessage: (event) => {
          try {
            const message = JSON.parse(event.data);
            handleRealtimeMessage(message);
          } catch (error) {
            console.error('Failed to parse SSE message:', error);
          }
        },
        onError: () => {
          setIsConnected(false);
          setConnectionStatus('DISCONNECTED');
        },
        onReconnecting: (attempt) => {
          setConnectionStatus('RECONNECTING');
          console.log(`SSE reconnecting (attempt ${attempt})`);
        }
      }
    );

    sseManagerRef.current.connect().catch((error) => {
      console.error('SSE connection failed:', error);
      setConnectionStatus('DISCONNECTED');
    });
  }, [sessionId]);

  // Handle real-time messages
  const handleRealtimeMessage = useCallback((message: WebSocketMessage) => {
    if (!sessionId) return;

    switch (message.type) {
      case 'CART_UPDATED':
        const updatedCart = message.payload as Cart;
        queryClient.setQueryData(['cart', sessionId], updatedCart);
        persistenceManagerRef.current?.saveSnapshot(updatedCart);
        tabSyncManagerRef.current?.broadcastCartUpdate(updatedCart, sessionId);
        setLastSyncTime(new Date());
        break;

      case 'CART_CLEARED':
        queryClient.setQueryData(['cart', sessionId], null);
        if (sessionId) {
        tabSyncManagerRef.current?.broadcastCartClear(sessionId);
      }
        setLastSyncTime(new Date());
        break;

      case 'ITEM_ADDED':
        tabSyncManagerRef.current?.broadcastItemAdded(
          message.payload.productId,
          message.payload.quantity,
          sessionId
        );
        refetch();
        break;

      case 'ITEM_REMOVED':
        tabSyncManagerRef.current?.broadcastItemRemoved(message.payload.productId, sessionId);
        refetch();
        break;

      case 'QUANTITY_UPDATED':
        tabSyncManagerRef.current?.broadcastQuantityUpdate(
          message.payload.productId,
          message.payload.quantity,
          sessionId
        );
        refetch();
        break;

      default:
        console.warn('Unknown real-time message type:', message.type);
    }
  }, [sessionId, queryClient]);

  // Get cart query
  const {
    data: cart,
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['cart', sessionId],
    queryFn: async () => {
      if (!sessionId) return null;
      
      try {
        const serverCart = await cartService.getCart(sessionId);
        
        // Try to recover from persistence if server cart is empty but we have persisted data
        if ((!serverCart || serverCart.items.length === 0) && persistenceManagerRef.current) {
          const recoveredCart = await persistenceManagerRef.current.recoverCart(serverCart, sessionId);
          if (recoveredCart && recoveredCart.items.length > 0) {
            return recoveredCart;
          }
        }
        
        // Save successful server response
        if (serverCart && persistenceManagerRef.current) {
          await persistenceManagerRef.current.saveSnapshot(serverCart);
        }
        
        return serverCart;
      } catch (error) {
        // Try to recover from persistence on network error
        if (persistenceManagerRef.current) {
          const recoveredCart = await persistenceManagerRef.current.loadLatestSnapshot(sessionId);
          if (recoveredCart) {
            console.warn('Using persisted cart data due to network error');
            return recoveredCart;
          }
        }
        throw error;
      }
    },
    enabled: !!sessionId,
    staleTime: 30 * 1000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
  });

  // Enhanced mutations with optimistic updates and offline support
  const addToCartMutation = useMutation({
    mutationFn: async ({ productId, quantity, product }: { productId: string; quantity: number; product?: Product }) => {
      if (!sessionId) throw new Error('Cart session not initialized');
      
      // Apply optimistic update only if we have product information
      let operationId: string | undefined;
      if (optimisticManagerRef.current && cart && product) {
        const { operationId: opId } = optimisticManagerRef.current.applyOptimisticUpdate({
          type: 'ADD_ITEM',
          payload: { productId, quantity, product },
          maxRetries: 3,
          originalState: cart
        });
        operationId = opId;
        
        // Update query data with optimistic state
        const optimisticState = optimisticManagerRef.current.getCurrentState();
        if (optimisticState) {
          queryClient.setQueryData(['cart', sessionId], optimisticState);
        }
      }

      try {
        // Try server request
        const result = await cartService.addToCart(productId, quantity, sessionId);
        
        // Confirm optimistic update
        if (operationId && optimisticManagerRef.current) {
          optimisticManagerRef.current.confirmOperation(operationId, result);
        }
        
        return result;
      } catch (error) {
        // Handle failure
        if (operationId && optimisticManagerRef.current) {
          const rollbackState = optimisticManagerRef.current.failOperation(operationId, error);
          if (rollbackState) {
            queryClient.setQueryData(['cart', sessionId], rollbackState);
          }
        }
        
        // Queue for background sync if offline
        if (!navigator.onLine && sessionId) {
          backgroundSyncService.queueOperation({
            type: 'ADD_ITEM',
            payload: { productId, quantity },
            sessionId
          });
          setPendingOperations(prev => prev + 1);
          throw new Error('Operation queued for when online');
        }
        
        throw error;
      }
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      
      // Update session ID if needed
      if (data.sessionId && data.sessionId !== sessionId) {
        setSessionId(data.sessionId);
        cartService.setCartSessionId(data.sessionId);
        queryClient.invalidateQueries({ queryKey: ['cart'] });
      }
      
      // Save to persistence
      if (persistenceManagerRef.current) {
        persistenceManagerRef.current.saveSnapshot(data);
      }
      
      // Broadcast to other tabs
      if (sessionId) {
        tabSyncManagerRef.current?.broadcastCartUpdate(data, sessionId);
      }
    },
    onError: (error) => {
      console.error('Failed to add item to cart:', error);
    }
  });

  // Similar enhanced mutations for other operations...
  const updateQuantityMutation = useMutation({
    mutationFn: async ({ productId, quantity }: { productId: string; quantity: number }) => {
      if (!sessionId) throw new Error('Cart session not initialized');
      
      let operationId: string | undefined;
      if (optimisticManagerRef.current && cart) {
        const { operationId: opId } = optimisticManagerRef.current.applyOptimisticUpdate({
          type: 'UPDATE_QUANTITY',
          payload: { productId, quantity },
          maxRetries: 3,
          originalState: cart
        });
        operationId = opId;
        
        const optimisticState = optimisticManagerRef.current.getCurrentState();
        if (optimisticState) {
          queryClient.setQueryData(['cart', sessionId], optimisticState);
        }
      }

      try {
        const result = await cartService.updateItemQuantity(productId, quantity, sessionId);
        
        if (operationId && optimisticManagerRef.current) {
          optimisticManagerRef.current.confirmOperation(operationId, result);
        }
        
        return result;
      } catch (error) {
        if (operationId && optimisticManagerRef.current) {
          const rollbackState = optimisticManagerRef.current.failOperation(operationId, error);
          if (rollbackState) {
            queryClient.setQueryData(['cart', sessionId], rollbackState);
          }
        }
        
        if (!navigator.onLine && sessionId) {
          backgroundSyncService.queueOperation({
            type: 'UPDATE_QUANTITY',
            payload: { productId, quantity },
            sessionId
          });
          setPendingOperations(prev => prev + 1);
          throw new Error('Operation queued for when online');
        }
        
        throw error;
      }
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      persistenceManagerRef.current?.saveSnapshot(data);
      if (sessionId) {
        tabSyncManagerRef.current?.broadcastCartUpdate(data, sessionId);
      }
    },
    onError: (error) => {
      console.error('Failed to update cart item quantity:', error);
    }
  });

  const removeFromCartMutation = useMutation({
    mutationFn: async ({ productId }: { productId: string }) => {
      if (!sessionId) throw new Error('Cart session not initialized');
      
      let operationId: string | undefined;
      if (optimisticManagerRef.current && cart) {
        const { operationId: opId } = optimisticManagerRef.current.applyOptimisticUpdate({
          type: 'REMOVE_ITEM',
          payload: { productId },
          maxRetries: 3,
          originalState: cart
        });
        operationId = opId;
        
        const optimisticState = optimisticManagerRef.current.getCurrentState();
        if (optimisticState) {
          queryClient.setQueryData(['cart', sessionId], optimisticState);
        }
      }

      try {
        const result = await cartService.removeFromCart(productId, sessionId);
        
        if (operationId && optimisticManagerRef.current) {
          optimisticManagerRef.current.confirmOperation(operationId, result);
        }
        
        return result;
      } catch (error) {
        if (operationId && optimisticManagerRef.current) {
          const rollbackState = optimisticManagerRef.current.failOperation(operationId, error);
          if (rollbackState) {
            queryClient.setQueryData(['cart', sessionId], rollbackState);
          }
        }
        
        if (!navigator.onLine && sessionId) {
          backgroundSyncService.queueOperation({
            type: 'REMOVE_ITEM',
            payload: { productId },
            sessionId
          });
          setPendingOperations(prev => prev + 1);
          throw new Error('Operation queued for when online');
        }
        
        throw error;
      }
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      persistenceManagerRef.current?.saveSnapshot(data);
      if (sessionId) {
        tabSyncManagerRef.current?.broadcastCartUpdate(data, sessionId);
      }
    },
    onError: (error) => {
      console.error('Failed to remove item from cart:', error);
    }
  });

  const clearCartMutation = useMutation({
    mutationFn: async () => {
      if (!sessionId) throw new Error('Cart session not initialized');
      
      let operationId: string | undefined;
      if (optimisticManagerRef.current && cart) {
        const { operationId: opId } = optimisticManagerRef.current.applyOptimisticUpdate({
          type: 'CLEAR_CART',
          payload: {},
          maxRetries: 3,
          originalState: cart
        });
        operationId = opId;
        
        const optimisticState = optimisticManagerRef.current.getCurrentState();
        if (optimisticState) {
          queryClient.setQueryData(['cart', sessionId], optimisticState);
        }
      }

      try {
        const result = await cartService.clearCart(sessionId);
        
        if (operationId && optimisticManagerRef.current) {
          optimisticManagerRef.current.confirmOperation(operationId, result);
        }
        
        return result;
      } catch (error) {
        if (operationId && optimisticManagerRef.current) {
          const rollbackState = optimisticManagerRef.current.failOperation(operationId, error);
          if (rollbackState) {
            queryClient.setQueryData(['cart', sessionId], rollbackState);
          }
        }
        
        if (!navigator.onLine && sessionId) {
          backgroundSyncService.queueOperation({
            type: 'CLEAR_CART',
            payload: {},
            sessionId
          });
          setPendingOperations(prev => prev + 1);
          throw new Error('Operation queued for when online');
        }
        
        throw error;
      }
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['cart', sessionId], data);
      persistenceManagerRef.current?.saveSnapshot(data);
      if (sessionId) {
        tabSyncManagerRef.current?.broadcastCartClear(sessionId);
      }
    },
    onError: (error) => {
      console.error('Failed to clear cart:', error);
    }
  });

  // Enhanced helper functions
  const addToCart = async (productId: string, quantity: number = 1, product?: Product) => {
    if (!sessionId) {
      throw new Error('Cart session not initialized');
    }
    await addToCartMutation.mutateAsync({ productId, quantity, product });
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

  // Sync actions
  const forcSync = async () => {
    if (!sessionId) return;
    
    setIsSyncInProgress(true);
    try {
      await backgroundSyncService.forcSync();
      await refetch();
    } finally {
      setIsSyncInProgress(false);
    }
  };

  const enableRealTimeSync = () => {
    setRealTimeSyncEnabled(true);
  };

  const disableRealTimeSync = () => {
    setRealTimeSyncEnabled(false);
    wsManagerRef.current?.close();
    sseManagerRef.current?.close();
    setIsConnected(false);
    setConnectionStatus('DISCONNECTED');
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
    const productStock = product.quantityInStock ?? product.quantity ?? 0;
    return totalRequestedQuantity <= productStock && productStock > 0;
  };

  // Update pending operations count
  useEffect(() => {
    setPendingOperations(backgroundSyncService.getPendingCount());
    setIsSyncInProgress(backgroundSyncService.isSyncInProgress());
  }, [cart]);

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
    
    // Real-time sync states
    isConnected,
    connectionStatus,
    pendingOperations,
    isSyncInProgress,
    lastSyncTime,
    
    // Actions
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    refreshCart,
    
    // Sync actions
    forcSync,
    enableRealTimeSync,
    disableRealTimeSync,
    
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