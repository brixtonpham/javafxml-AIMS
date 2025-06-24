import React, { createContext, useContext, useReducer, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Order, OrderStatus, PaginatedResponse } from '../types';
import type { OrderSearchFilters, OrderStatusStats } from '../types/order';
import { orderService } from '../services/orderService';

interface OrderContextState {
  orders: Order[];
  currentOrder: Order | null;
  filters: OrderSearchFilters;
  stats: OrderStatusStats | null;
  isLoading: boolean;
  error: string | null;
}

type OrderAction =
  | { type: 'SET_ORDERS'; payload: Order[] }
  | { type: 'SET_CURRENT_ORDER'; payload: Order | null }
  | { type: 'UPDATE_ORDER'; payload: Order }
  | { type: 'SET_FILTERS'; payload: OrderSearchFilters }
  | { type: 'SET_STATS'; payload: OrderStatusStats }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'RESET_STATE' };

const initialState: OrderContextState = {
  orders: [],
  currentOrder: null,
  filters: {
    status: 'ALL',
    page: 1,
    pageSize: 10
  },
  stats: null,
  isLoading: false,
  error: null
};

function orderReducer(state: OrderContextState, action: OrderAction): OrderContextState {
  switch (action.type) {
    case 'SET_ORDERS':
      return { ...state, orders: action.payload };
    
    case 'SET_CURRENT_ORDER':
      return { ...state, currentOrder: action.payload };
    
    case 'UPDATE_ORDER':
      return {
        ...state,
        orders: state.orders.map(order =>
          order.id === action.payload.id ? action.payload : order
        ),
        currentOrder: state.currentOrder?.id === action.payload.id 
          ? action.payload 
          : state.currentOrder
      };
    
    case 'SET_FILTERS':
      return { ...state, filters: { ...state.filters, ...action.payload } };
    
    case 'SET_STATS':
      return { ...state, stats: action.payload };
    
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    
    case 'RESET_STATE':
      return initialState;
    
    default:
      return state;
  }
}

interface OrderContextValue extends OrderContextState {
  // Actions
  fetchUserOrders: () => void;
  fetchOrderById: (orderId: string) => void;
  updateFilters: (filters: Partial<OrderSearchFilters>) => void;
  cancelOrder: (orderId: string, reason?: string) => Promise<void>;
  refreshOrder: (orderId: string) => void;
  refreshOrders: () => void;
  
  // Query states
  ordersQuery: {
    isLoading: boolean;
    error: Error | null;
    refetch: () => void;
  };
  
  orderDetailQuery: {
    isLoading: boolean;
    error: Error | null;
    refetch: () => void;
  };
}

const OrderContext = createContext<OrderContextValue | undefined>(undefined);

export const useOrderContext = () => {
  const context = useContext(OrderContext);
  if (context === undefined) {
    throw new Error('useOrderContext must be used within an OrderProvider');
  }
  return context;
};

interface OrderProviderProps {
  children: React.ReactNode;
}

export const OrderProvider: React.FC<OrderProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(orderReducer, initialState);
  const queryClient = useQueryClient();

  // Query for user orders with filters
  const ordersQuery = useQuery({
    queryKey: ['orders', 'user', state.filters],
    queryFn: () => orderService.getUserOrders(undefined, state.filters.page, state.filters.pageSize)
  });

  // Query for single order details
  const orderDetailQuery = useQuery({
    queryKey: ['order', state.currentOrder?.id],
    queryFn: () => state.currentOrder?.id ? orderService.getOrderById(state.currentOrder.id) : null,
    enabled: !!state.currentOrder?.id
  });

  // Handle orders query data
  React.useEffect(() => {
    if (ordersQuery.data) {
      dispatch({ type: 'SET_ORDERS', payload: ordersQuery.data.items });
    }
    if (ordersQuery.error) {
      dispatch({ type: 'SET_ERROR', payload: ordersQuery.error.message });
    }
  }, [ordersQuery.data, ordersQuery.error]);

  // Handle order detail query data
  React.useEffect(() => {
    if (orderDetailQuery.data) {
      dispatch({ type: 'SET_CURRENT_ORDER', payload: orderDetailQuery.data });
    }
    if (orderDetailQuery.error) {
      dispatch({ type: 'SET_ERROR', payload: orderDetailQuery.error.message });
    }
  }, [orderDetailQuery.data, orderDetailQuery.error]);

  // Mutation for cancelling orders
  const cancelOrderMutation = useMutation({
    mutationFn: (orderId: string) => orderService.cancelOrder(orderId),
    onSuccess: (updatedOrder: Order) => {
      dispatch({ type: 'UPDATE_ORDER', payload: updatedOrder });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
    onError: (error: Error) => {
      dispatch({ type: 'SET_ERROR', payload: error.message });
    }
  });

  // Actions
  const fetchUserOrders = useCallback(() => {
    ordersQuery.refetch();
  }, [ordersQuery]);

  const fetchOrderById = useCallback((orderId: string) => {
    dispatch({ type: 'SET_CURRENT_ORDER', payload: { id: orderId } as Order });
  }, []);

  const updateFilters = useCallback((filters: Partial<OrderSearchFilters>) => {
    dispatch({ type: 'SET_FILTERS', payload: filters });
  }, []);

  const cancelOrder = useCallback(async (orderId: string, reason?: string) => {
    try {
      await cancelOrderMutation.mutateAsync(orderId);
    } catch (error) {
      throw error;
    }
  }, [cancelOrderMutation]);

  const refreshOrder = useCallback((orderId: string) => {
    queryClient.invalidateQueries({ queryKey: ['order', orderId] });
  }, [queryClient]);

  const refreshOrders = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['orders'] });
  }, [queryClient]);

  // Calculate order stats from current orders
  const calculateStats = useCallback((orders: Order[]): OrderStatusStats => {
    // Safety check: ensure orders is an array
    const ordersArray = orders || [];
    const stats = ordersArray.reduce((acc, order) => {
      acc.total++;
      switch (order.status) {
        case 'PENDING':
          acc.pending++;
          break;
        case 'APPROVED':
          acc.approved++;
          break;
        case 'SHIPPED':
          acc.shipped++;
          break;
        case 'DELIVERED':
          acc.delivered++;
          break;
        case 'CANCELLED':
          acc.cancelled++;
          break;
        case 'REJECTED':
          acc.rejected++;
          break;
      }
      return acc;
    }, {
      total: 0,
      pending: 0,
      approved: 0,
      shipped: 0,
      delivered: 0,
      cancelled: 0,
      rejected: 0
    });

    return stats;
  }, []);

  // Update stats when orders change
  React.useEffect(() => {
    if (state.orders) {
      const stats = calculateStats(state.orders);
      dispatch({ type: 'SET_STATS', payload: stats });
    }
  }, [state.orders, calculateStats]);

  const value: OrderContextValue = {
    ...state,
    fetchUserOrders,
    fetchOrderById,
    updateFilters,
    cancelOrder,
    refreshOrder,
    refreshOrders,
    ordersQuery: {
      isLoading: ordersQuery.isLoading,
      error: ordersQuery.error,
      refetch: ordersQuery.refetch
    },
    orderDetailQuery: {
      isLoading: orderDetailQuery.isLoading,
      error: orderDetailQuery.error,
      refetch: orderDetailQuery.refetch
    }
  };

  return (
    <OrderContext.Provider value={value}>
      {children}
    </OrderContext.Provider>
  );
};