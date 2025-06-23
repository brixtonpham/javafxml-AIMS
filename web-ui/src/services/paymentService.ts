import { api } from './api';
import type { 
  PaymentMethod, 
  PaymentResult, 
  PaymentMethodType 
} from '../types';

export const paymentService = {
  // Get available payment methods
  async getPaymentMethods(): Promise<PaymentMethod[]> {
    const response = await api.get<PaymentMethod[]>('/payment/methods');
    return response.data;
  },

  // Get payment method by ID
  async getPaymentMethodById(paymentMethodId: string): Promise<PaymentMethod> {
    const response = await api.get<PaymentMethod>(`/payment/methods/${paymentMethodId}`);
    return response.data;
  },

  // Process payment for an order
  async processPayment(orderId: string, paymentMethodId: string, additionalParams?: Record<string, any>): Promise<PaymentResult> {
    const response = await api.post<PaymentResult>('/payment/process', {
      orderId,
      paymentMethodId,
      additionalParams
    });
    return response.data;
  },

  // Check payment status
  async checkPaymentStatus(transactionId: string): Promise<{
    status: string;
    message: string;
    transactionData?: any;
  }> {
    const response = await api.get<{
      status: string;
      message: string;
      transactionData?: any;
    }>(`/payment/status/${transactionId}`);
    return response.data;
  },

  // Handle payment callback/return from gateway
  async handlePaymentCallback(callbackData: Record<string, any>): Promise<PaymentResult> {
    const response = await api.post<PaymentResult>('/payment/callback', callbackData);
    return response.data;
  },

  // Process refund (admin only)
  async processRefund(
    orderId: string, 
    transactionId: string, 
    refundAmount: number, 
    reason: string
  ): Promise<PaymentResult> {
    const response = await api.post<PaymentResult>('/admin/payment/refund', {
      orderId,
      transactionId,
      refundAmount,
      reason
    });
    return response.data;
  },

  // Admin: Create/update payment method
  async savePaymentMethod(paymentMethod: Omit<PaymentMethod, 'id'>): Promise<PaymentMethod> {
    const response = await api.post<PaymentMethod>('/admin/payment/methods', paymentMethod);
    return response.data;
  },

  // Admin: Update payment method
  async updatePaymentMethod(paymentMethodId: string, paymentMethod: Partial<PaymentMethod>): Promise<PaymentMethod> {
    const response = await api.put<PaymentMethod>(`/admin/payment/methods/${paymentMethodId}`, paymentMethod);
    return response.data;
  },

  // Admin: Delete payment method
  async deletePaymentMethod(paymentMethodId: string): Promise<void> {
    await api.delete(`/admin/payment/methods/${paymentMethodId}`);
  },

  // Get transaction history for order
  async getOrderTransactions(orderId: string): Promise<any[]> {
    const response = await api.get<any[]>(`/payment/transactions/order/${orderId}`);
    return response.data;
  },

  // Validate payment data before processing
  async validatePaymentData(paymentData: {
    orderId: string;
    paymentMethodId: string;
    amount: number;
  }): Promise<{ valid: boolean; errors?: string[] }> {
    const response = await api.post<{ valid: boolean; errors?: string[] }>('/payment/validate', paymentData);
    return response.data;
  }
};