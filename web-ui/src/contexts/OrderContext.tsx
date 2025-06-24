import React, { createContext, useContext, useReducer, useCallback, useEffect, useRef, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Order, OrderStatus } from '../types';
import type { OrderSearchFilters, OrderStatusStats, OrderTimeline, EnhancedOrder } from '../types/order';
import { orderService } from '../services/orderService';
import { WebSocketManager, type WebSocketMessage } from '../utils/websocket';
import { getNextValidStatuses, formatOrderTimeline } from '../types/order';

// Order State Machine Management
export interface OrderStateMachine {
  currentStatus: OrderStatus;
  validNextStates: OrderStatus[];
  canTransition: (toStatus: OrderStatus) => boolean;
  validateTransition: (toStatus: OrderStatus) => { valid: boolean; reason?: string };
}

// Real-time Order Lifecycle Event
export interface OrderLifecycleEvent {
  id: string;
  orderId: string;
  type: 'STATUS_UPDATE' | 'TIMELINE_UPDATE' | 'NOTIFICATION' | 'ANALYTICS_UPDATE';
  timestamp: string;
  data: any;
  source: 'SYSTEM' | 'USER' | 'EXTERNAL';
}

// Enhanced Order Context State
interface OrderContextState {
  orders: Order[];
  currentOrder: Order | null;
  enhancedOrders: Map<string, EnhancedOrder>;
  filters: OrderSearchFilters;
  stats: OrderStatusStats | null;
  isLoading: boolean;
  error: string | null;
  // Real-time capabilities
  isConnected: boolean;
  lastSync: string | null;
  // State machine management
  stateMachines: Map<string, OrderStateMachine>;
  // Lifecycle events
  recentEvents: OrderLifecycleEvent[];
  // Analytics
  realTimeMetrics: {
    activeOrders: number;
    recentTransitions: number;
    avgProcessingTime: number;
    lastUpdated: string;
  } | null;
}

type OrderAction =
  | { type: 'SET_ORDERS'; payload: Order[] }
  | { type: 'SET_CURRENT_ORDER'; payload: Order | null }
  | { type: 'UPDATE_ORDER'; payload: Order }
  | { type: 'SET_ENHANCED_ORDER'; payload: EnhancedOrder }
  | { type: 'SET_FILTERS'; payload: OrderSearchFilters }
  | { type: 'SET_STATS'; payload: OrderStatusStats }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_CONNECTION_STATUS'; payload: boolean }
  | { type: 'SET_LAST_SYNC'; payload: string }
  | { type: 'UPDATE_STATE_MACHINE'; payload: { orderId: string; stateMachine: OrderStateMachine } }
  | { type: 'ADD_LIFECYCLE_EVENT'; payload: OrderLifecycleEvent }
  | { type: 'UPDATE_REAL_TIME_METRICS'; payload: any }
  | { type: 'RESET_STATE' };

const initialState: OrderContextState = {
  orders: [],
  currentOrder: null,
  enhancedOrders: new Map(),
  filters: {
    status: 'ALL',
    page: 1,
    pageSize: 10
  },
  stats: null,
  isLoading: false,
  error: null,
  isConnected: false,
  lastSync: null,
  stateMachines: new Map(),
  recentEvents: [],
  realTimeMetrics: null
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
    
    case 'SET_ENHANCED_ORDER': {
      const newEnhancedOrders = new Map(state.enhancedOrders);
      newEnhancedOrders.set(action.payload.id, action.payload);
      return { ...state, enhancedOrders: newEnhancedOrders };
    }
    
    case 'SET_FILTERS':
      return { ...state, filters: { ...state.filters, ...action.payload } };
    
    case 'SET_STATS':
      return { ...state, stats: action.payload };
    
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    
    case 'SET_CONNECTION_STATUS':
      return { ...state, isConnected: action.payload };
    
    case 'SET_LAST_SYNC':
      return { ...state, lastSync: action.payload };
    
    case 'UPDATE_STATE_MACHINE': {
      const newStateMachines = new Map(state.stateMachines);
      newStateMachines.set(action.payload.orderId, action.payload.stateMachine);
      return { ...state, stateMachines: newStateMachines };
    }
    
    case 'ADD_LIFECYCLE_EVENT':
      return {
        ...state,
        recentEvents: [action.payload, ...state.recentEvents].slice(0, 50) // Keep last 50 events
      };
    
    case 'UPDATE_REAL_TIME_METRICS':
      return { ...state, realTimeMetrics: action.payload };
    
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
  
  // State Machine Actions
  getOrderStateMachine: (orderId: string) => OrderStateMachine | null;
  canTransitionOrder: (orderId: string, toStatus: OrderStatus) => boolean;
  transitionOrder: (orderId: string, toStatus: OrderStatus, reason?: string) => Promise<void>;
  getValidTransitions: (orderId: string) => OrderStatus[];
  
  // Enhanced Order Actions
  getEnhancedOrder: (orderId: string) => EnhancedOrder | null;
  updateOrderTimeline: (orderId: string, timelineEntry: OrderTimeline) => void;
  
  // Real-time Actions
  connectRealTime: () => Promise<void>;
  disconnectRealTime: () => void;
  subscribeToOrderUpdates: (orderId: string) => void;
  unsubscribeFromOrderUpdates: (orderId: string) => void;
  
  // Analytics Actions
  refreshMetrics: () => void;
  getOrderAnalytics: (fromDate?: string, toDate?: string) => Promise<any>;
  
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
// WebSocket and Real-time Management
  const wsManager = useRef<WebSocketManager | null>(null);
  const subscribedOrders = useRef<Set<string>>(new Set());

  // Helper function to create state machine for an order
  const createStateMachine = useCallback((order: Order): OrderStateMachine => {
    const validNextStates = getNextValidStatuses(order.status);
    
    return {
      currentStatus: order.status,
      validNextStates,
      canTransition: (toStatus: OrderStatus) => validNextStates.includes(toStatus),
      validateTransition: (toStatus: OrderStatus) => {
        if (validNextStates.includes(toStatus)) {
          return { valid: true };
        }
        return { 
          valid: false, 
          reason: `Cannot transition from ${order.status} to ${toStatus}` 
        };
      }
    };
  }, []);

  // Helper function to create enhanced order
  const createEnhancedOrder = useCallback((order: Order): EnhancedOrder => {
    const timeline = formatOrderTimeline ? formatOrderTimeline(order) : [];
    return {
      ...order,
      timeline,
      notifications: [],
      canCancel: order.status === 'PENDING' || order.status === 'APPROVED',
      cancellationDeadline: order.status === 'APPROVED' ? 
        new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString() : undefined
    };
  }, []);

  // State Machine Actions
  const getOrderStateMachine = useCallback((orderId: string): OrderStateMachine | null => {
    return state.stateMachines.get(orderId) || null;
  }, [state.stateMachines]);

  const canTransitionOrder = useCallback((orderId: string, toStatus: OrderStatus): boolean => {
    const stateMachine = state.stateMachines.get(orderId);
    return stateMachine?.canTransition(toStatus) || false;
  }, [state.stateMachines]);

  const transitionOrder = useCallback(async (orderId: string, toStatus: OrderStatus, reason?: string) => {
    try {
      const updatedOrder = await orderService.updateOrderStatus(orderId, toStatus, reason);
      dispatch({ type: 'UPDATE_ORDER', payload: updatedOrder });
      
      // Update state machine
      const newStateMachine = createStateMachine(updatedOrder);
      dispatch({ 
        type: 'UPDATE_STATE_MACHINE', 
        payload: { orderId, stateMachine: newStateMachine }
      });

      // Add lifecycle event
      const event: OrderLifecycleEvent = {
        id: `${Date.now()}-${Math.random()}`,
        orderId,
        type: 'STATUS_UPDATE',
        timestamp: new Date().toISOString(),
        data: { fromStatus: state.currentOrder?.status, toStatus, reason },
        source: 'USER'
      };
      dispatch({ type: 'ADD_LIFECYCLE_EVENT', payload: event });

    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: (error as Error).message });
      throw error;
    }
  }, [state.currentOrder?.status, createStateMachine]);

  const getValidTransitions = useCallback((orderId: string): OrderStatus[] => {
    const stateMachine = state.stateMachines.get(orderId);
    return stateMachine?.validNextStates || [];
  }, [state.stateMachines]);

  // Enhanced Order Actions
  const getEnhancedOrder = useCallback((orderId: string): EnhancedOrder | null => {
    return state.enhancedOrders.get(orderId) || null;
  }, [state.enhancedOrders]);

  const updateOrderTimeline = useCallback((orderId: string, timelineEntry: OrderTimeline) => {
    const enhanced = state.enhancedOrders.get(orderId);
    if (enhanced) {
      const updatedEnhanced: EnhancedOrder = {
        ...enhanced,
        timeline: [...enhanced.timeline, timelineEntry].sort(
          (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        )
      };
      dispatch({ type: 'SET_ENHANCED_ORDER', payload: updatedEnhanced });
    }
  }, [state.enhancedOrders]);

  // Real-time Actions
  const connectRealTime = useCallback(async () => {
    if (wsManager.current?.isConnected()) return;

    const wsOptions = {
      url: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/api/ws/orders`,
      heartbeatInterval: 30000,
      reconnectInterval: 5000,
      maxReconnectAttempts: 10
    };

    const wsHandlers = {
      onOpen: () => {
        console.log('Order WebSocket connected');
        dispatch({ type: 'SET_CONNECTION_STATUS', payload: true });
        dispatch({ type: 'SET_LAST_SYNC', payload: new Date().toISOString() });
      },
      onMessage: (message: WebSocketMessage) => {
        console.log('Order WebSocket message:', message);
        
        switch (message.type) {
          case 'ORDER_STATUS_UPDATE': {
            const updatedOrder = message.payload.order;
            dispatch({ type: 'UPDATE_ORDER', payload: updatedOrder });
            
            // Update state machine
            const newStateMachine = createStateMachine(updatedOrder);
            dispatch({ 
              type: 'UPDATE_STATE_MACHINE', 
              payload: { orderId: updatedOrder.id, stateMachine: newStateMachine }
            });
            break;
          }
            
          case 'ORDER_LIFECYCLE_EVENT':
            dispatch({ type: 'ADD_LIFECYCLE_EVENT', payload: message.payload });
            break;
            
          case 'METRICS_UPDATE':
            dispatch({ type: 'UPDATE_REAL_TIME_METRICS', payload: message.payload });
            break;
        }
        
        dispatch({ type: 'SET_LAST_SYNC', payload: new Date().toISOString() });
      },
      onClose: () => {
        console.log('Order WebSocket disconnected');
        dispatch({ type: 'SET_CONNECTION_STATUS', payload: false });
      },
      onError: (error: Event) => {
        console.error('Order WebSocket error:', error);
        dispatch({ type: 'SET_ERROR', payload: 'WebSocket connection error' });
      },
      onReconnecting: (attempt: number) => {
        console.log(`Order WebSocket reconnecting (attempt ${attempt})`);
        dispatch({ type: 'SET_CONNECTION_STATUS', payload: false });
      },
      onReconnected: () => {
        console.log('Order WebSocket reconnected');
        dispatch({ type: 'SET_CONNECTION_STATUS', payload: true });
        dispatch({ type: 'SET_LAST_SYNC', payload: new Date().toISOString() });
      }
    };

    wsManager.current = new WebSocketManager(wsOptions, wsHandlers);
    
    try {
      await wsManager.current.connect();
    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
      dispatch({ type: 'SET_ERROR', payload: 'Failed to establish real-time connection' });
    }
  }, [createStateMachine]);

  const disconnectRealTime = useCallback(() => {
    if (wsManager.current) {
      wsManager.current.close();
      wsManager.current = null;
      subscribedOrders.current.clear();
      dispatch({ type: 'SET_CONNECTION_STATUS', payload: false });
    }
  }, []);

  const subscribeToOrderUpdates = useCallback((orderId: string) => {
    if (wsManager.current?.isConnected() && !subscribedOrders.current.has(orderId)) {
      wsManager.current.send({
        type: 'SUBSCRIBE_ORDER',
        payload: { orderId }
      });
      subscribedOrders.current.add(orderId);
    }
  }, []);

  const unsubscribeFromOrderUpdates = useCallback((orderId: string) => {
    if (wsManager.current?.isConnected() && subscribedOrders.current.has(orderId)) {
      wsManager.current.send({
        type: 'UNSUBSCRIBE_ORDER',
        payload: { orderId }
      });
      subscribedOrders.current.delete(orderId);
    }
  }, []);

  // Analytics Actions
  const refreshMetrics = useCallback(async () => {
    try {
      // This would call a real analytics endpoint
      const metrics = {
        activeOrders: state.orders.filter(o => ['PENDING', 'APPROVED', 'SHIPPED'].includes(o.status)).length,
        recentTransitions: state.recentEvents.filter(e => 
          e.type === 'STATUS_UPDATE' && 
          new Date(e.timestamp) > new Date(Date.now() - 24 * 60 * 60 * 1000)
        ).length,
        avgProcessingTime: 2.5, // hours
        lastUpdated: new Date().toISOString()
      };
      dispatch({ type: 'UPDATE_REAL_TIME_METRICS', payload: metrics });
    } catch (error) {
      console.error('Failed to refresh metrics:', error);
    }
  }, [state.orders, state.recentEvents]);

  const getOrderAnalytics = useCallback(async (fromDate?: string, toDate?: string) => {
    try {
      // This would call the backend analytics endpoint
      const response = await fetch(`/api/orders/analytics?from=${fromDate}&to=${toDate}`);
      return await response.json();
    } catch (error) {
      console.error('Failed to get order analytics:', error);
      throw error;
    }
  }, []);
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
// Initialize state machines when orders change
  useEffect(() => {
    state.orders.forEach(order => {
      if (!state.stateMachines.has(order.id)) {
        const stateMachine = createStateMachine(order);
        dispatch({ 
          type: 'UPDATE_STATE_MACHINE', 
          payload: { orderId: order.id, stateMachine }
        });
      }
    });
  }, [state.orders, state.stateMachines, createStateMachine]);

  // Initialize enhanced orders when orders change
  useEffect(() => {
    state.orders.forEach(order => {
      if (!state.enhancedOrders.has(order.id)) {
        const enhanced = createEnhancedOrder(order);
        dispatch({ type: 'SET_ENHANCED_ORDER', payload: enhanced });
      }
    });
  }, [state.orders, state.enhancedOrders, createEnhancedOrder]);

  // Auto-connect WebSocket on mount
  useEffect(() => {
    connectRealTime();
    
    // Cleanup on unmount
    return () => {
      disconnectRealTime();
    };
  }, [connectRealTime, disconnectRealTime]);

  // Subscribe to current order updates
  useEffect(() => {
    if (state.currentOrder?.id && state.isConnected) {
      subscribeToOrderUpdates(state.currentOrder.id);
      
      return () => {
        if (state.currentOrder?.id) {
          unsubscribeFromOrderUpdates(state.currentOrder.id);
        }
      };
    }
  }, [state.currentOrder?.id, state.isConnected, subscribeToOrderUpdates, unsubscribeFromOrderUpdates]);

  // Refresh metrics periodically
  useEffect(() => {
    const interval = setInterval(() => {
      refreshMetrics().catch(console.error);
    }, 30000); // Every 30 seconds
    return () => clearInterval(interval);
  }, [refreshMetrics]);

  const fetchOrderById = useCallback((orderId: string) => {
    dispatch({ type: 'SET_CURRENT_ORDER', payload: { id: orderId } as Order });
  }, []);

  const updateFilters = useCallback((filters: Partial<OrderSearchFilters>) => {
    dispatch({ type: 'SET_FILTERS', payload: filters });
  }, []);

  const cancelOrder = useCallback(async (orderId: string, _reason?: string) => {
    try {
      await cancelOrderMutation.mutateAsync(orderId);
    } catch (error) {
      console.error('Failed to cancel order:', error);
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

  // Wrapper functions to fix Promise return type issues
  const refetchOrdersSync = useCallback(() => {
    ordersQuery.refetch().catch(console.error);
  }, [ordersQuery]);

  const refetchOrderDetailSync = useCallback(() => {
    orderDetailQuery.refetch().catch(console.error);
  }, [orderDetailQuery]);

  const refreshMetricsSync = useCallback(() => {
    refreshMetrics().catch(console.error);
  }, [refreshMetrics]);

  const value: OrderContextValue = useMemo(() => ({
    ...state,
    fetchUserOrders,
    fetchOrderById,
    updateFilters,
    cancelOrder,
    refreshOrder,
    refreshOrders,
    // State Machine Actions
    getOrderStateMachine,
    canTransitionOrder,
    transitionOrder,
    getValidTransitions,
    // Enhanced Order Actions
    getEnhancedOrder,
    updateOrderTimeline,
    // Real-time Actions
    connectRealTime,
    disconnectRealTime,
    subscribeToOrderUpdates,
    unsubscribeFromOrderUpdates,
    // Analytics Actions
    refreshMetrics: refreshMetricsSync,
    getOrderAnalytics,
    ordersQuery: {
      isLoading: ordersQuery.isLoading,
      error: ordersQuery.error,
      refetch: refetchOrdersSync
    },
    orderDetailQuery: {
      isLoading: orderDetailQuery.isLoading,
      error: orderDetailQuery.error,
      refetch: refetchOrderDetailSync
    }
  }), [
    state,
    fetchUserOrders,
    fetchOrderById,
    updateFilters,
    cancelOrder,
    refreshOrder,
    refreshOrders,
    getOrderStateMachine,
    canTransitionOrder,
    transitionOrder,
    getValidTransitions,
    getEnhancedOrder,
    updateOrderTimeline,
    connectRealTime,
    disconnectRealTime,
    subscribeToOrderUpdates,
    unsubscribeFromOrderUpdates,
    refreshMetricsSync,
    getOrderAnalytics,
    ordersQuery.isLoading,
    ordersQuery.error,
    refetchOrdersSync,
    orderDetailQuery.isLoading,
    orderDetailQuery.error,
    refetchOrderDetailSync
  ]);

  return (
    <OrderContext.Provider value={value}>
      {children}
    </OrderContext.Provider>
  );
};