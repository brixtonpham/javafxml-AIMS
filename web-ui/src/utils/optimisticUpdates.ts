/**
 * Optimistic Update Management System
 * Handles optimistic updates with conflict resolution and rollback capabilities
 */

import type { Cart, CartItem } from '../types';

export interface OptimisticOperation {
  id: string;
  type: 'ADD_ITEM' | 'UPDATE_QUANTITY' | 'REMOVE_ITEM' | 'CLEAR_CART';
  timestamp: number;
  payload: any;
  originalState?: Cart | null;
  optimisticState?: Cart | null;
  status: 'PENDING' | 'CONFIRMED' | 'FAILED' | 'ROLLED_BACK';
  retryCount: number;
  maxRetries: number;
}

export interface ConflictResolutionStrategy {
  strategy: 'SERVER_WINS' | 'CLIENT_WINS' | 'MERGE' | 'ASK_USER';
  mergeFunction?: (serverState: Cart, clientState: Cart, operation: OptimisticOperation) => Cart;
}

export interface OptimisticUpdateOptions {
  maxPendingOperations?: number;
  operationTimeout?: number;
  maxRetries?: number;
  conflictResolution?: ConflictResolutionStrategy;
}

export class OptimisticUpdateManager {
  private pendingOperations = new Map<string, OptimisticOperation>();
  private currentState: Cart | null = null;
  private options: Required<OptimisticUpdateOptions>;
  private operationTimers = new Map<string, number>();

  constructor(options: OptimisticUpdateOptions = {}) {
    this.options = {
      maxPendingOperations: 10,
      operationTimeout: 30000, // 30 seconds
      maxRetries: 3,
      conflictResolution: { strategy: 'SERVER_WINS' },
      ...options
    };
  }

  /**
   * Apply an optimistic update to the cart state
   */
  applyOptimisticUpdate(
    operation: Omit<OptimisticOperation, 'id' | 'timestamp' | 'status' | 'retryCount'>
  ): { operationId: string; optimisticState: Cart | null } {
    // Check if we've reached the maximum number of pending operations
    if (this.pendingOperations.size >= this.options.maxPendingOperations) {
      throw new Error('Maximum number of pending operations reached');
    }

    const operationId = this.generateOperationId();
    const timestamp = Date.now();
    
    const fullOperation: OptimisticOperation = {
      id: operationId,
      timestamp,
      status: 'PENDING',
      retryCount: 0,
      originalState: this.currentState ? { ...this.currentState } : null,
      ...operation
    };

    // Apply the optimistic update
    const optimisticState = this.calculateOptimisticState(fullOperation);
    fullOperation.optimisticState = optimisticState;

    // Store the operation
    this.pendingOperations.set(operationId, fullOperation);
    this.currentState = optimisticState;

    // Set timeout for the operation
    const timer = window.setTimeout(() => {
      this.handleOperationTimeout(operationId);
    }, this.options.operationTimeout);
    this.operationTimers.set(operationId, timer);

    return { operationId, optimisticState };
  }

  /**
   * Confirm a successful operation
   */
  confirmOperation(operationId: string, serverState: Cart): Cart {
    const operation = this.pendingOperations.get(operationId);
    if (!operation) {
      console.warn(`Operation ${operationId} not found for confirmation`);
      return serverState;
    }

    // Clear the timeout
    const timer = this.operationTimers.get(operationId);
    if (timer) {
      clearTimeout(timer);
      this.operationTimers.delete(operationId);
    }

    // Check for conflicts
    const hasConflict = this.detectConflict(operation, serverState);
    
    if (hasConflict) {
      return this.resolveConflict(operation, serverState);
    }

    // No conflict, mark as confirmed
    operation.status = 'CONFIRMED';
    this.pendingOperations.delete(operationId);
    this.currentState = serverState;

    return serverState;
  }

  /**
   * Handle a failed operation
   */
  failOperation(operationId: string, error: any): Cart | null {
    const operation = this.pendingOperations.get(operationId);
    if (!operation) {
      console.warn(`Operation ${operationId} not found for failure handling`);
      return this.currentState;
    }

    // Clear the timeout
    const timer = this.operationTimers.get(operationId);
    if (timer) {
      clearTimeout(timer);
      this.operationTimers.delete(operationId);
    }

    // Check if we should retry
    if (operation.retryCount < operation.maxRetries) {
      operation.retryCount++;
      console.warn(`Retrying operation ${operationId} (attempt ${operation.retryCount})`);
      return this.currentState;
    }

    // Max retries reached, rollback
    return this.rollbackOperation(operationId, error);
  }

  /**
   * Rollback an operation
   */
  rollbackOperation(operationId: string, reason?: any): Cart | null {
    const operation = this.pendingOperations.get(operationId);
    if (!operation) {
      console.warn(`Operation ${operationId} not found for rollback`);
      return this.currentState;
    }

    // Clear the timeout
    const timer = this.operationTimers.get(operationId);
    if (timer) {
      clearTimeout(timer);
      this.operationTimers.delete(operationId);
    }

    operation.status = 'ROLLED_BACK';
    this.pendingOperations.delete(operationId);

    // Recalculate state without this operation
    const newState = this.recalculateState();
    this.currentState = newState;

    console.warn(`Rolled back operation ${operationId}:`, reason);
    return newState;
  }

  /**
   * Get the current optimistic state
   */
  getCurrentState(): Cart | null {
    return this.currentState;
  }

  /**
   * Get pending operations
   */
  getPendingOperations(): OptimisticOperation[] {
    return Array.from(this.pendingOperations.values());
  }

  /**
   * Clear all pending operations and reset to server state
   */
  reset(serverState: Cart | null = null): Cart | null {
    // Clear all timers
    this.operationTimers.forEach(timer => clearTimeout(timer));
    this.operationTimers.clear();

    // Clear all pending operations
    this.pendingOperations.clear();

    this.currentState = serverState;
    return serverState;
  }

  private calculateOptimisticState(operation: OptimisticOperation): Cart | null {
    if (!operation.originalState) return null;

    const state = { ...operation.originalState };

    switch (operation.type) {
      case 'ADD_ITEM':
        return this.applyAddItem(state, operation.payload);
      
      case 'UPDATE_QUANTITY':
        return this.applyUpdateQuantity(state, operation.payload);
      
      case 'REMOVE_ITEM':
        return this.applyRemoveItem(state, operation.payload);
      
      case 'CLEAR_CART':
        return this.applyClearCart(state);
      
      default:
        return state;
    }
  }

  private applyAddItem(state: Cart, payload: { productId: string; quantity: number; product?: any }): Cart {
    // If no product information is provided, we can't perform optimistic updates
    if (!payload.product) {
      console.warn('Cannot perform optimistic update without product information');
      return state;
    }

    const existingItemIndex = state.items.findIndex(item => item.productId === payload.productId);
    
    if (existingItemIndex >= 0) {
      // Update existing item
      const existingItem = state.items[existingItemIndex];
      const newQuantity = existingItem.quantity + payload.quantity;
      const newSubtotal = payload.product.price * newQuantity;
      
      state.items[existingItemIndex] = {
        ...existingItem,
        quantity: newQuantity,
        subtotal: newSubtotal
      };
    } else {
      // Add new item
      const newItem: CartItem = {
        productId: payload.productId,
        product: payload.product,
        quantity: payload.quantity,
        subtotal: payload.product.price * payload.quantity
      };
      state.items.push(newItem);
    }

    return this.recalculateTotals(state);
  }

  private applyUpdateQuantity(state: Cart, payload: { productId: string; quantity: number }): Cart {
    const itemIndex = state.items.findIndex(item => item.productId === payload.productId);
    
    if (itemIndex >= 0) {
      if (payload.quantity <= 0) {
        // Remove item if quantity is 0 or less
        state.items.splice(itemIndex, 1);
      } else {
        // Update quantity
        const item = state.items[itemIndex];
        state.items[itemIndex] = {
          ...item,
          quantity: payload.quantity,
          subtotal: item.product.price * payload.quantity
        };
      }
    }

    return this.recalculateTotals(state);
  }

  private applyRemoveItem(state: Cart, payload: { productId: string }): Cart {
    state.items = state.items.filter(item => item.productId !== payload.productId);
    return this.recalculateTotals(state);
  }

  private applyClearCart(state: Cart): Cart {
    return {
      ...state,
      items: [],
      totalItems: 0,
      totalPrice: 0,
      totalPriceWithVAT: 0,
      stockWarnings: []
    };
  }

  private recalculateTotals(state: Cart): Cart {
    const totalItems = state.items.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = state.items.reduce((sum, item) => sum + item.subtotal, 0);
    const totalPriceWithVAT = totalPrice * 1.1; // Assuming 10% VAT

    return {
      ...state,
      totalItems,
      totalPrice,
      totalPriceWithVAT
    };
  }

  private detectConflict(operation: OptimisticOperation, serverState: Cart): boolean {
    if (!operation.optimisticState) return false;

    // Compare key values that might indicate conflicts
    const optimistic = operation.optimisticState;
    
    // Check if server state is significantly different from optimistic state
    const serverItemCount = serverState.items.length;
    const optimisticItemCount = optimistic.items.length;
    const serverTotal = serverState.totalPrice;
    const optimisticTotal = optimistic.totalPrice;

    // Simple conflict detection - more sophisticated logic can be added
    return Math.abs(serverItemCount - optimisticItemCount) > 1 ||
           Math.abs(serverTotal - optimisticTotal) > optimisticTotal * 0.1;
  }

  private resolveConflict(operation: OptimisticOperation, serverState: Cart): Cart {
    const strategy = this.options.conflictResolution;

    switch (strategy.strategy) {
      case 'SERVER_WINS':
        operation.status = 'CONFIRMED';
        this.pendingOperations.delete(operation.id);
        this.currentState = serverState;
        return serverState;

      case 'CLIENT_WINS':
        operation.status = 'CONFIRMED';
        this.pendingOperations.delete(operation.id);
        return operation.optimisticState || serverState;

      case 'MERGE':
        if (strategy.mergeFunction && operation.optimisticState) {
          const mergedState = strategy.mergeFunction(serverState, operation.optimisticState, operation);
          operation.status = 'CONFIRMED';
          this.pendingOperations.delete(operation.id);
          this.currentState = mergedState;
          return mergedState;
        }
        // Fallback to server wins if no merge function
        return this.resolveConflict({ ...operation }, serverState);

      case 'ASK_USER':
        // This would typically trigger a UI prompt
        console.warn('Conflict detected, user intervention required');
        return serverState;

      default:
        return serverState;
    }
  }

  private recalculateState(): Cart | null {
    // Find the last confirmed state or original state
    let baseState: Cart | null = null;
    
    // Get all pending operations sorted by timestamp
    const pendingOps = Array.from(this.pendingOperations.values())
      .filter(op => op.status === 'PENDING')
      .sort((a, b) => a.timestamp - b.timestamp);

    if (pendingOps.length === 0) {
      return null;
    }

    // Use the original state of the first operation as base
    baseState = pendingOps[0].originalState || null;
    if (!baseState) return null;

    // Apply all pending operations in order
    let currentState = { ...baseState };
    for (const operation of pendingOps) {
      const tempOp = { ...operation, originalState: currentState };
      const newState = this.calculateOptimisticState(tempOp);
      if (newState) {
        currentState = newState;
      }
    }

    return currentState;
  }

  private handleOperationTimeout(operationId: string): void {
    console.warn(`Operation ${operationId} timed out`);
    this.failOperation(operationId, new Error('Operation timeout'));
  }

  private generateOperationId(): string {
    return `opt_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Default conflict resolution strategies
 */
export const ConflictResolutionStrategies = {
  SERVER_WINS: { strategy: 'SERVER_WINS' as const },
  CLIENT_WINS: { strategy: 'CLIENT_WINS' as const },
  MERGE_ITEMS: {
    strategy: 'MERGE' as const,
    mergeFunction: (serverState: Cart, clientState: Cart): Cart => {
      // Merge cart items, keeping the higher quantity for each product
      const mergedItems = new Map<string, CartItem>();
      
      // Add server items
      serverState.items.forEach(item => {
        mergedItems.set(item.productId, item);
      });
      
      // Merge with client items
      clientState.items.forEach(clientItem => {
        const serverItem = mergedItems.get(clientItem.productId);
        if (serverItem) {
          // Keep the item with higher quantity
          if (clientItem.quantity > serverItem.quantity) {
            mergedItems.set(clientItem.productId, clientItem);
          }
        } else {
          mergedItems.set(clientItem.productId, clientItem);
        }
      });
      
      const items = Array.from(mergedItems.values());
      const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
      const totalPrice = items.reduce((sum, item) => sum + item.subtotal, 0);
      const totalPriceWithVAT = totalPrice * 1.1;
      
      return {
        ...serverState,
        items,
        totalItems,
        totalPrice,
        totalPriceWithVAT
      };
    }
  }
};