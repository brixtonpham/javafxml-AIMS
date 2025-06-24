import { api } from './api';
import type { StockValidationResult, BulkStockValidationResult, CartItem } from '../types';

export const stockValidationService = {
  /**
   * Validate stock for a single product
   */
  async validateProductStock(productId: string, requestedQuantity: number): Promise<StockValidationResult> {
    const response = await api.post<StockValidationResult>('/stock/validate', {
      productId,
      requestedQuantity
    });
    return response.data;
  },

  /**
   * Validate stock for multiple cart items
   */
  async validateCartStock(cartItems: CartItem[]): Promise<BulkStockValidationResult> {
    const validationRequests = cartItems.map(item => ({
      productId: item.productId,
      requestedQuantity: item.quantity
    }));

    const response = await api.post<BulkStockValidationResult>('/stock/validate-bulk', {
      items: validationRequests
    });
    return response.data;
  },

  /**
   * Get real-time stock information for a product
   */
  async getProductStockInfo(productId: string): Promise<{
    actualStock: number;
    reservedStock: number;
    availableStock: number;
    lastUpdated: string;
  }> {
    const response = await api.get<{
      actualStock: number;
      reservedStock: number;
      availableStock: number;
      lastUpdated: string;
    }>(`/stock/info/${productId}`);
    return response.data;
  },

  /**
   * Reserve stock for checkout process
   */
  async reserveStock(cartItems: CartItem[], sessionId: string): Promise<{
    success: boolean;
    reservationId: string;
    expiresAt: string;
    failedItems: string[];
  }> {
    const response = await api.post<{
      success: boolean;
      reservationId: string;
      expiresAt: string;
      failedItems: string[];
    }>('/stock/reserve', {
      items: cartItems.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      })),
      sessionId
    });
    return response.data;
  },

  /**
   * Release reserved stock
   */
  async releaseReservation(reservationId: string): Promise<void> {
    await api.delete(`/stock/reservations/${reservationId}`);
  },

  /**
   * Check if products are eligible for rush delivery
   */
  async checkRushDeliveryEligibility(productIds: string[]): Promise<{
    isEligible: boolean;
    ineligibleProducts: string[];
    reasons: Record<string, string>;
  }> {
    const response = await api.post<{
      isEligible: boolean;
      ineligibleProducts: string[];
      reasons: Record<string, string>;
    }>('/stock/rush-delivery-eligibility', {
      productIds
    });
    return response.data;
  }
};

export default stockValidationService;