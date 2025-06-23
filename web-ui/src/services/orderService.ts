import { api, paginatedRequest } from './api';
import type { 
  Order, 
  OrderStatus, 
  DeliveryFormData,
  PaymentResult,
  PaginatedResponse 
} from '../types';

export const orderService = {
  // Create order from cart
  async createOrderFromCart(cartSessionId: string): Promise<Order> {
    const response = await api.post<Order>('/orders/from-cart', {
      cartSessionId
    });
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
  async updateDeliveryInfo(orderId: string, deliveryData: DeliveryFormData): Promise<Order> {
    const response = await api.put<Order>(`/orders/${orderId}/delivery`, deliveryData);
    return response.data;
  },

  // Calculate shipping fee preview
  async calculateShippingFee(deliveryData: DeliveryFormData): Promise<{ fee: number; rushFee?: number }> {
    const response = await api.post<{ fee: number; rushFee?: number }>('/orders/shipping-preview', deliveryData);
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