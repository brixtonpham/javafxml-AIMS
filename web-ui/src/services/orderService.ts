import { api, paginatedRequest } from './api';
import type { 
  Order, 
  OrderStatus, 
  DeliveryInfo,
  PaymentResult,
  PaginatedResponse 
} from '../types';

// Request/Response types for order operations
export interface CreateOrderFromCartRequest {
  cartSessionId: string;
  userId?: string;
}

export interface PlaceOrderRequest {
  cartSessionId: string;
  deliveryInfo: DeliveryInfo;
  isRushOrder: boolean;
  deliveryInstructions?: string;
  paymentMethodId?: string;
}

export interface AddDeliveryInfoRequest {
  deliveryInfo: DeliveryInfo;
  isRushOrder: boolean;
}

export interface CalculateShippingRequest {
  deliveryInfo: DeliveryInfo;
  isRushOrder: boolean;
}

export interface ShippingFeeResponse {
  shippingFee: number;
  rushFee?: number;
  totalShipping: number;
}

export interface OrderTotalResponse {
  total: number;
  subtotal: number;
  vatAmount: number;
  shippingFee: number;
}

export const orderService = {
  // Create order from cart (draft state)
  async createOrderFromCart(cartSessionId: string, userId?: string): Promise<Order> {
    const response = await api.post<Order>('/orders/from-cart', {
      cartSessionId,
      userId
    });
    return response.data;
  },

  // Place final order with all details
  async placeOrder(request: PlaceOrderRequest): Promise<Order> {
    const response = await api.post<Order>('/orders/place', request);
    return response.data;
  },

  // Get order by ID
  async getOrderById(orderId: string): Promise<Order> {
    const response = await api.get<Order>(`/orders/${orderId}`);
    return response.data;
  },

  // Get orders for current user
  async getUserOrders(userId?: string, page = 1, pageSize = 20): Promise<PaginatedResponse<Order>> {
    // If userId is provided, use it; otherwise, the backend should handle getting current user's orders
    const endpoint = userId ? `/orders/user/${userId}` : '/orders/user';
    return paginatedRequest<Order>(endpoint, {
      page,
      pageSize
    });
  },

  // Get orders by status (for admins/managers)
  async getOrdersByStatus(
    status: OrderStatus, 
    page = 1, 
    pageSize = 20
  ): Promise<PaginatedResponse<Order>> {
    return paginatedRequest<Order>('/admin/orders', {
      status,
      page,
      pageSize
    });
  },

  // Update delivery information
  async updateDeliveryInfo(orderId: string, request: AddDeliveryInfoRequest): Promise<Order> {
    const response = await api.post<Order>(`/orders/${orderId}/delivery`, request);
    return response.data;
  },

  // Calculate shipping fee for order
  async calculateShippingFee(orderId: string, request: CalculateShippingRequest): Promise<ShippingFeeResponse> {
    const response = await api.post<ShippingFeeResponse>(`/orders/${orderId}/shipping-fee`, request);
    return response.data;
  },

  // Calculate shipping fee preview (without order)
  async calculateShippingFeePreview(request: CalculateShippingRequest): Promise<ShippingFeeResponse> {
    const response = await api.post<ShippingFeeResponse>('/orders/shipping-preview', request);
    return response.data;
  },

  // Process payment for order
  async processPayment(orderId: string, paymentMethodId: string): Promise<PaymentResult> {
    const response = await api.post<PaymentResult>(`/orders/${orderId}/payment`, {
      paymentMethodId
    });
    return response.data;
  },

  // Update order status (admin/manager only)
  async updateOrderStatus(orderId: string, status: OrderStatus, reason?: string): Promise<Order> {
    const response = await api.put<Order>(`/admin/orders/${orderId}/status`, {
      status,
      reason
    });
    return response.data;
  },

  // Approve order (manager only)
  async approveOrder(orderId: string): Promise<Order> {
    const response = await api.post<Order>(`/admin/orders/${orderId}/approve`);
    return response.data;
  },

  // Reject order (manager only)
  async rejectOrder(orderId: string, reason: string): Promise<Order> {
    const response = await api.post<Order>(`/admin/orders/${orderId}/reject`, {
      reason
    });
    return response.data;
  },

  // Cancel order
  async cancelOrder(orderId: string): Promise<Order> {
    const response = await api.post<Order>(`/orders/${orderId}/cancel`);
    return response.data;
  },

  // Mark order as shipped (admin only)
  async markAsShipped(orderId: string, trackingNumber: string): Promise<Order> {
    const response = await api.post<Order>(`/admin/orders/${orderId}/ship`, {
      trackingNumber
    });
    return response.data;
  },

  // Mark order as delivered (admin only)
  async markAsDelivered(orderId: string): Promise<Order> {
    const response = await api.post<Order>(`/admin/orders/${orderId}/deliver`);
    return response.data;
  },

  // Get order summary for checkout
  async getOrderSummary(orderId: string): Promise<{
    order: Order;
    subtotal: number;
    vatAmount: number;
    shippingFee: number;
    totalAmount: number;
  }> {
    const response = await api.get<{
      order: Order;
      subtotal: number;
      vatAmount: number;
      shippingFee: number;
      totalAmount: number;
    }>(`/orders/${orderId}/summary`);
    return response.data;
  }
};