import { api } from './api';
import type { 
  Cart, 
  CartItem, 
  ApiResponse 
} from '../types';

export const cartService = {
  // Get current cart for session
  async getCart(sessionId?: string): Promise<Cart> {
    const params = sessionId ? { sessionId } : {};
    const response = await api.get<Cart>('/cart', { params });
    return response.data;
  },

  // Add item to cart
  async addToCart(productId: string, quantity: number, sessionId?: string): Promise<Cart> {
    const response = await api.post<Cart>('/cart/items', {
      productId,
      quantity,
      sessionId
    });
    return response.data;
  },

  // Update item quantity in cart
  async updateItemQuantity(productId: string, quantity: number, sessionId?: string): Promise<Cart> {
    const response = await api.put<Cart>(`/cart/items/${productId}`, {
      quantity,
      sessionId
    });
    return response.data;
  },

  // Remove item from cart
  async removeFromCart(productId: string, sessionId?: string): Promise<Cart> {
    const params = sessionId ? { sessionId } : {};
    const response = await api.delete<Cart>(`/cart/items/${productId}`, { params });
    return response.data;
  },

  // Clear entire cart
  async clearCart(sessionId?: string): Promise<Cart> {
    const params = sessionId ? { sessionId } : {};
    const response = await api.delete<Cart>('/cart', { params });
    return response.data;
  },

  // Associate guest cart with user after login
  async associateCartWithUser(sessionId: string): Promise<Cart> {
    const response = await api.post<Cart>('/cart/associate', { sessionId });
    return response.data;
  },

  // Create new cart session
  async createCart(): Promise<Cart> {
    const response = await api.post<Cart>('/cart/create');
    return response.data;
  },

  // Get cart session ID from localStorage
  getCartSessionId(): string | null {
    return localStorage.getItem('cart_session_id');
  },

  // Store cart session ID in localStorage
  setCartSessionId(sessionId: string): void {
    localStorage.setItem('cart_session_id', sessionId);
  },

  // Clear cart session ID from localStorage
  clearCartSessionId(): void {
    localStorage.removeItem('cart_session_id');
  },

  // Helper to ensure we have a cart session
  async ensureCartSession(): Promise<string> {
    let sessionId = this.getCartSessionId();
    
    if (!sessionId) {
      const cart = await this.createCart();
      sessionId = cart.sessionId;
      this.setCartSessionId(sessionId);
    }
    
    return sessionId;
  }
};