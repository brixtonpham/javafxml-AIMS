/**
 * Test script to verify the CartContext optimistic update fix
 */

import { describe, it, expect } from 'vitest';
import { OptimisticUpdateManager } from '../../utils/optimisticUpdates';
import type { Cart } from '../../types';

describe('OptimisticUpdateManager Fix', () => {
  it('should handle ADD_ITEM without product information', () => {
    const optimisticManager = new OptimisticUpdateManager();
    
    const mockCart: Cart = {
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    };

    // This should not crash anymore
    const result = optimisticManager.applyOptimisticUpdate({
      type: 'ADD_ITEM',
      payload: { productId: 'test-product', quantity: 1 }, // No product info
      maxRetries: 3,
      originalState: mockCart
    });

    // The state should remain unchanged since no product info was provided
    expect(result.optimisticState).toEqual(mockCart);
  });

  it('should handle ADD_ITEM with product information', () => {
    const optimisticManager = new OptimisticUpdateManager();
    
    const mockCart: Cart = {
      sessionId: 'test-session',
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: [],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    };

    const mockProduct = {
      id: 'test-product',
      productId: 'test-product',
      title: 'Test Product',
      price: 10.00,
      valueAmount: 10.00,
      quantityInStock: 5,
      category: 'test',
      description: 'test description',
      productType: 'BOOK' as const,
      entryDate: '2024-01-01'
    };

    // This should work correctly
    const result = optimisticManager.applyOptimisticUpdate({
      type: 'ADD_ITEM',
      payload: { productId: 'test-product', quantity: 1, product: mockProduct },
      maxRetries: 3,
      originalState: mockCart
    });

    // The state should be updated with the new item
    expect(result.optimisticState?.items).toHaveLength(1);
    expect(result.optimisticState?.items[0].productId).toBe('test-product');
    expect(result.optimisticState?.items[0].quantity).toBe(1);
    expect(result.optimisticState?.items[0].subtotal).toBe(10.00);
  });
});
