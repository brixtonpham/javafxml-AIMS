/**
 * Cart Persistence and Recovery Mechanisms
 * Handles cart state persistence with offline support and automatic recovery
 */

import type { Cart, CartItem } from '../types';

export interface CartSnapshot {
  cart: Cart;
  timestamp: number;
  version: string;
  sessionId: string;
  checksum: string;
}

export interface PersistenceOptions {
  storageKey?: string;
  maxSnapshots?: number;
  compressionEnabled?: boolean;
  encryptionKey?: string;
  autoSaveInterval?: number;
  checksumValidation?: boolean;
}

export interface RecoveryOptions {
  maxAge?: number; // Maximum age of snapshots to consider for recovery (ms)
  preferNewerSnapshots?: boolean;
  validateIntegrity?: boolean;
  mergeStrategy?: 'REPLACE' | 'MERGE_ITEMS' | 'KEEP_NEWER';
}

export interface OfflineOperation {
  id: string;
  type: 'ADD_ITEM' | 'UPDATE_QUANTITY' | 'REMOVE_ITEM' | 'CLEAR_CART';
  payload: any;
  timestamp: number;
  sessionId: string;
  retryCount: number;
  applied: boolean;
}

export class CartPersistenceManager {
  private options: Required<PersistenceOptions>;
  private recoveryOptions: Required<RecoveryOptions>;
  private autoSaveTimer: number | null = null;
  private isDestroyed = false;
  private currentSessionId: string | null = null;

  constructor(
    options: PersistenceOptions = {},
    recoveryOptions: RecoveryOptions = {}
  ) {
    this.options = {
      storageKey: 'aims_cart_persistence',
      maxSnapshots: 10,
      compressionEnabled: false,
      encryptionKey: '',
      autoSaveInterval: 30000, // 30 seconds
      checksumValidation: true,
      ...options
    };

    this.recoveryOptions = {
      maxAge: 24 * 60 * 60 * 1000, // 24 hours
      preferNewerSnapshots: true,
      validateIntegrity: true,
      mergeStrategy: 'MERGE_ITEMS',
      ...recoveryOptions
    };

    this.startAutoSave();
  }

  /**
   * Save a cart snapshot to localStorage
   */
  async saveSnapshot(cart: Cart): Promise<void> {
    if (this.isDestroyed) return;

    try {
      const snapshot: CartSnapshot = {
        cart: { ...cart },
        timestamp: Date.now(),
        version: '1.0.0',
        sessionId: cart.sessionId,
        checksum: this.options.checksumValidation ? await this.calculateChecksum(cart) : ''
      };

      const snapshots = this.getSnapshots();
      
      // Remove old snapshots for the same session
      const filteredSnapshots = snapshots.filter(s => s.sessionId !== cart.sessionId);
      
      // Add new snapshot
      filteredSnapshots.push(snapshot);
      
      // Keep only the most recent snapshots
      if (filteredSnapshots.length > this.options.maxSnapshots) {
        filteredSnapshots.sort((a, b) => b.timestamp - a.timestamp);
        filteredSnapshots.splice(this.options.maxSnapshots);
      }

      await this.saveSnapshots(filteredSnapshots);
      
    } catch (error) {
      console.error('Failed to save cart snapshot:', error);
    }
  }

  /**
   * Load the most recent cart snapshot
   */
  async loadLatestSnapshot(sessionId?: string): Promise<Cart | null> {
    try {
      const snapshots = this.getSnapshots();
      
      if (snapshots.length === 0) return null;

      // Filter by session ID if provided
      const relevantSnapshots = sessionId 
        ? snapshots.filter(s => s.sessionId === sessionId)
        : snapshots;

      if (relevantSnapshots.length === 0) return null;

      // Sort by timestamp (newest first)
      relevantSnapshots.sort((a, b) => b.timestamp - a.timestamp);

      // Find the first valid snapshot
      for (const snapshot of relevantSnapshots) {
        if (await this.validateSnapshot(snapshot)) {
          return snapshot.cart;
        }
      }

      return null;
    } catch (error) {
      console.error('Failed to load cart snapshot:', error);
      return null;
    }
  }

  /**
   * Recover cart state from snapshots with conflict resolution
   */
  async recoverCart(currentCart: Cart | null, sessionId: string): Promise<Cart | null> {
    try {
      const savedCart = await this.loadLatestSnapshot(sessionId);
      
      if (!savedCart) return currentCart;
      
      if (!currentCart) return savedCart;

      // Both carts exist, apply merge strategy
      return this.mergeCartStates(currentCart, savedCart);
      
    } catch (error) {
      console.error('Failed to recover cart:', error);
      return currentCart;
    }
  }

  /**
   * Save offline operations for later synchronization
   */
  async saveOfflineOperation(operation: Omit<OfflineOperation, 'id' | 'timestamp' | 'retryCount' | 'applied'>): Promise<string> {
    const operationId = this.generateOperationId();
    
    const fullOperation: OfflineOperation = {
      id: operationId,
      timestamp: Date.now(),
      retryCount: 0,
      applied: false,
      ...operation
    };

    const operations = this.getOfflineOperations();
    operations.push(fullOperation);
    
    await this.saveOfflineOperations(operations);
    
    return operationId;
  }

  /**
   * Get all pending offline operations
   */
  getPendingOfflineOperations(sessionId?: string): OfflineOperation[] {
    const operations = this.getOfflineOperations();
    
    return operations.filter(op => 
      !op.applied && 
      (!sessionId || op.sessionId === sessionId)
    );
  }

  /**
   * Mark an offline operation as applied
   */
  async markOperationApplied(operationId: string): Promise<void> {
    const operations = this.getOfflineOperations();
    const operation = operations.find(op => op.id === operationId);
    
    if (operation) {
      operation.applied = true;
      await this.saveOfflineOperations(operations);
    }
  }

  /**
   * Remove applied offline operations
   */
  async cleanupAppliedOperations(): Promise<void> {
    const operations = this.getOfflineOperations();
    const pendingOperations = operations.filter(op => !op.applied);
    
    await this.saveOfflineOperations(pendingOperations);
  }

  /**
   * Clear all persisted data for a session
   */
  async clearSession(sessionId: string): Promise<void> {
    // Remove snapshots for the session
    const snapshots = this.getSnapshots();
    const filteredSnapshots = snapshots.filter(s => s.sessionId !== sessionId);
    await this.saveSnapshots(filteredSnapshots);

    // Remove offline operations for the session
    const operations = this.getOfflineOperations();
    const filteredOperations = operations.filter(op => op.sessionId !== sessionId);
    await this.saveOfflineOperations(filteredOperations);
  }

  /**
   * Get storage usage information
   */
  getStorageInfo(): { snapshots: number; operations: number; totalSize: number } {
    const snapshots = this.getSnapshots();
    const operations = this.getOfflineOperations();
    
    const snapshotsSize = JSON.stringify(snapshots).length;
    const operationsSize = JSON.stringify(operations).length;
    
    return {
      snapshots: snapshots.length,
      operations: operations.length,
      totalSize: snapshotsSize + operationsSize
    };
  }

  /**
   * Cleanup old snapshots and operations
   */
  async cleanup(): Promise<void> {
    const cutoffTime = Date.now() - this.recoveryOptions.maxAge;
    
    // Cleanup old snapshots
    const snapshots = this.getSnapshots();
    const recentSnapshots = snapshots.filter(s => s.timestamp > cutoffTime);
    await this.saveSnapshots(recentSnapshots);

    // Cleanup old applied operations
    const operations = this.getOfflineOperations();
    const recentOperations = operations.filter(op => 
      !op.applied || op.timestamp > cutoffTime
    );
    await this.saveOfflineOperations(recentOperations);
  }

  /**
   * Destroy the persistence manager
   */
  destroy(): void {
    this.isDestroyed = true;
    this.stopAutoSave();
  }

  /**
   * Set current session ID for auto-save
   */
  setCurrentSession(sessionId: string): void {
    this.currentSessionId = sessionId;
  }

  private startAutoSave(): void {
    if (this.options.autoSaveInterval > 0) {
      this.autoSaveTimer = window.setInterval(() => {
        this.cleanup().catch(console.error);
      }, this.options.autoSaveInterval);
    }
  }

  private stopAutoSave(): void {
    if (this.autoSaveTimer) {
      clearInterval(this.autoSaveTimer);
      this.autoSaveTimer = null;
    }
  }

  private getSnapshots(): CartSnapshot[] {
    try {
      const data = localStorage.getItem(`${this.options.storageKey}_snapshots`);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  private async saveSnapshots(snapshots: CartSnapshot[]): Promise<void> {
    try {
      const data = JSON.stringify(snapshots);
      localStorage.setItem(`${this.options.storageKey}_snapshots`, data);
    } catch (error) {
      throw new Error(`Failed to save snapshots: ${error}`);
    }
  }

  private getOfflineOperations(): OfflineOperation[] {
    try {
      const data = localStorage.getItem(`${this.options.storageKey}_operations`);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  private async saveOfflineOperations(operations: OfflineOperation[]): Promise<void> {
    try {
      const data = JSON.stringify(operations);
      localStorage.setItem(`${this.options.storageKey}_operations`, data);
    } catch (error) {
      throw new Error(`Failed to save offline operations: ${error}`);
    }
  }

  private async validateSnapshot(snapshot: CartSnapshot): Promise<boolean> {
    // Check snapshot age
    const age = Date.now() - snapshot.timestamp;
    if (age > this.recoveryOptions.maxAge) {
      return false;
    }

    // Validate checksum if enabled
    if (this.options.checksumValidation && snapshot.checksum) {
      const calculatedChecksum = await this.calculateChecksum(snapshot.cart);
      return calculatedChecksum === snapshot.checksum;
    }

    // Basic structure validation
    return (
      snapshot.cart &&
      typeof snapshot.cart.sessionId === 'string' &&
      Array.isArray(snapshot.cart.items) &&
      typeof snapshot.cart.totalItems === 'number' &&
      typeof snapshot.cart.totalPrice === 'number'
    );
  }

  private async calculateChecksum(cart: Cart): Promise<string> {
    // Simple checksum calculation - in production, use a proper hash function
    const data = JSON.stringify({
      sessionId: cart.sessionId,
      items: cart.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      })),
      totalItems: cart.totalItems,
      totalPrice: cart.totalPrice
    });
    
    // Simple hash function (consider using crypto.subtle.digest for better security)
    let hash = 0;
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    
    return hash.toString(36);
  }

  private mergeCartStates(currentCart: Cart, savedCart: Cart): Cart {
    switch (this.recoveryOptions.mergeStrategy) {
      case 'REPLACE':
        return savedCart;
      
      case 'KEEP_NEWER':
        return new Date(currentCart.updatedAt) > new Date(savedCart.updatedAt) 
          ? currentCart 
          : savedCart;
      
      case 'MERGE_ITEMS':
      default:
        return this.mergeCartItems(currentCart, savedCart);
    }
  }

  private mergeCartItems(currentCart: Cart, savedCart: Cart): Cart {
    const mergedItems = new Map<string, CartItem>();
    
    // Add current cart items
    currentCart.items.forEach(item => {
      mergedItems.set(item.productId, item);
    });
    
    // Merge saved cart items (keeping higher quantities)
    savedCart.items.forEach(savedItem => {
      const currentItem = mergedItems.get(savedItem.productId);
      
      if (!currentItem) {
        mergedItems.set(savedItem.productId, savedItem);
      } else if (savedItem.quantity > currentItem.quantity) {
        mergedItems.set(savedItem.productId, savedItem);
      }
    });
    
    const items = Array.from(mergedItems.values());
    const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = items.reduce((sum, item) => sum + item.subtotal, 0);
    const totalPriceWithVAT = totalPrice * 1.1; // Assuming 10% VAT
    
    return {
      ...currentCart,
      items,
      totalItems,
      totalPrice,
      totalPriceWithVAT,
      updatedAt: new Date().toISOString()
    };
  }

  private generateOperationId(): string {
    return `offline_op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Utility functions for cart persistence
 */
export const CartPersistenceUtils = {
  /**
   * Check if localStorage is available
   */
  isStorageAvailable(): boolean {
    try {
      const test = '__storage_test__';
      localStorage.setItem(test, test);
      localStorage.removeItem(test);
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Get available storage space (approximate)
   */
  getAvailableStorage(): number {
    if (!this.isStorageAvailable()) return 0;
    
    let total = 0;
    for (let key in localStorage) {
      if (localStorage.hasOwnProperty(key)) {
        total += localStorage[key].length + key.length;
      }
    }
    
    // Rough estimate of available space (5MB limit for localStorage)
    return Math.max(0, 5 * 1024 * 1024 - total);
  },

  /**
   * Clear all cart-related storage
   */
  clearAllCartStorage(storageKey: string = 'aims_cart_persistence'): void {
    if (!this.isStorageAvailable()) return;
    
    localStorage.removeItem(`${storageKey}_snapshots`);
    localStorage.removeItem(`${storageKey}_operations`);
  },

  /**
   * Export cart data for backup
   */
  exportCartData(storageKey: string = 'aims_cart_persistence'): string {
    if (!this.isStorageAvailable()) return '{}';
    
    const snapshots = localStorage.getItem(`${storageKey}_snapshots`);
    const operations = localStorage.getItem(`${storageKey}_operations`);
    
    return JSON.stringify({
      snapshots: snapshots ? JSON.parse(snapshots) : [],
      operations: operations ? JSON.parse(operations) : [],
      exportedAt: new Date().toISOString()
    });
  },

  /**
   * Import cart data from backup
   */
  importCartData(data: string, storageKey: string = 'aims_cart_persistence'): boolean {
    if (!this.isStorageAvailable()) return false;
    
    try {
      const parsed = JSON.parse(data);
      
      if (parsed.snapshots) {
        localStorage.setItem(`${storageKey}_snapshots`, JSON.stringify(parsed.snapshots));
      }
      
      if (parsed.operations) {
        localStorage.setItem(`${storageKey}_operations`, JSON.stringify(parsed.operations));
      }
      
      return true;
    } catch {
      return false;
    }
  }
};