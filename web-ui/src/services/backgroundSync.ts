/**
 * Background Sync Service for Cart Operations
 * Handles offline operations and background synchronization
 */

import { cartService } from './cartService';
import type { Cart } from '../types';

export interface BackgroundSyncOptions {
  syncInterval?: number;
  maxRetries?: number;
  batchSize?: number;
  enablePeriodicSync?: boolean;
}

export interface SyncOperation {
  id: string;
  type: 'ADD_ITEM' | 'UPDATE_QUANTITY' | 'REMOVE_ITEM' | 'CLEAR_CART';
  payload: any;
  timestamp: number;
  retryCount: number;
  sessionId: string;
}

export interface SyncResult {
  success: boolean;
  operation: SyncOperation;
  cart?: Cart;
  error?: Error;
}

export class BackgroundSyncService {
  private options: Required<BackgroundSyncOptions>;
  private syncTimer: number | null = null;
  private isDestroyed = false;
  private isSyncing = false;
  private pendingOperations: SyncOperation[] = [];
  private onSyncComplete?: (results: SyncResult[]) => void;
  private onNetworkStatusChange?: (isOnline: boolean) => void;

  constructor(options: BackgroundSyncOptions = {}) {
    this.options = {
      syncInterval: 30000, // 30 seconds
      maxRetries: 3,
      batchSize: 10,
      enablePeriodicSync: true,
      ...options
    };

    this.initializeNetworkDetection();
    this.startPeriodicSync();
  }

  /**
   * Add an operation to the sync queue
   */
  queueOperation(operation: Omit<SyncOperation, 'id' | 'timestamp' | 'retryCount'>): string {
    const operationId = this.generateOperationId();
    
    const fullOperation: SyncOperation = {
      id: operationId,
      timestamp: Date.now(),
      retryCount: 0,
      ...operation
    };

    this.pendingOperations.push(fullOperation);
    
    // Try to sync immediately if online
    if (navigator.onLine) {
      this.syncPendingOperations();
    }

    return operationId;
  }

  /**
   * Force sync all pending operations
   */
  async forcSync(): Promise<SyncResult[]> {
    return this.syncPendingOperations();
  }

  /**
   * Get pending operations count
   */
  getPendingCount(): number {
    return this.pendingOperations.length;
  }

  /**
   * Check if service is currently syncing
   */
  isSyncInProgress(): boolean {
    return this.isSyncing;
  }

  /**
   * Set callback for sync completion
   */
  onSync(callback: (results: SyncResult[]) => void): void {
    this.onSyncComplete = callback;
  }

  /**
   * Set callback for network status changes
   */
  onNetworkChange(callback: (isOnline: boolean) => void): void {
    this.onNetworkStatusChange = callback;
  }

  /**
   * Destroy the sync service
   */
  destroy(): void {
    this.isDestroyed = true;
    this.stopPeriodicSync();
    this.pendingOperations = [];
    
    // Remove network event listeners
    window.removeEventListener('online', this.handleOnline);
    window.removeEventListener('offline', this.handleOffline);
  }

  private initializeNetworkDetection(): void {
    window.addEventListener('online', this.handleOnline);
    window.addEventListener('offline', this.handleOffline);
  }

  private handleOnline = (): void => {
    this.onNetworkStatusChange?.(true);
    // Sync when coming back online
    this.syncPendingOperations();
  };

  private handleOffline = (): void => {
    this.onNetworkStatusChange?.(false);
  };

  private startPeriodicSync(): void {
    if (!this.options.enablePeriodicSync) return;

    this.syncTimer = window.setInterval(() => {
      if (!this.isDestroyed && navigator.onLine && this.pendingOperations.length > 0) {
        this.syncPendingOperations();
      }
    }, this.options.syncInterval);
  }

  private stopPeriodicSync(): void {
    if (this.syncTimer) {
      clearInterval(this.syncTimer);
      this.syncTimer = null;
    }
  }

  private async syncPendingOperations(): Promise<SyncResult[]> {
    if (this.isSyncing || this.isDestroyed || this.pendingOperations.length === 0) {
      return [];
    }

    this.isSyncing = true;
    const results: SyncResult[] = [];

    try {
      // Process operations in batches
      const operationsToSync = this.pendingOperations.splice(0, this.options.batchSize);
      
      for (const operation of operationsToSync) {
        try {
          const result = await this.syncOperation(operation);
          results.push(result);
          
          if (!result.success) {
            // Re-queue failed operations if retries remaining
            if (operation.retryCount < this.options.maxRetries) {
              operation.retryCount++;
              this.pendingOperations.unshift(operation);
            }
          }
        } catch (error) {
          results.push({
            success: false,
            operation,
            error: error as Error
          });
          
          // Re-queue with retry
          if (operation.retryCount < this.options.maxRetries) {
            operation.retryCount++;
            this.pendingOperations.unshift(operation);
          }
        }
      }

      this.onSyncComplete?.(results);
      return results;

    } finally {
      this.isSyncing = false;
    }
  }

  private async syncOperation(operation: SyncOperation): Promise<SyncResult> {
    try {
      let cart: Cart;

      switch (operation.type) {
        case 'ADD_ITEM':
          cart = await cartService.addToCart(
            operation.payload.productId,
            operation.payload.quantity,
            operation.sessionId
          );
          break;

        case 'UPDATE_QUANTITY':
          cart = await cartService.updateItemQuantity(
            operation.payload.productId,
            operation.payload.quantity,
            operation.sessionId
          );
          break;

        case 'REMOVE_ITEM':
          cart = await cartService.removeFromCart(
            operation.payload.productId,
            operation.sessionId
          );
          break;

        case 'CLEAR_CART':
          cart = await cartService.clearCart(operation.sessionId);
          break;

        default:
          throw new Error(`Unknown operation type: ${operation.type}`);
      }

      return {
        success: true,
        operation,
        cart
      };

    } catch (error) {
      return {
        success: false,
        operation,
        error: error as Error
      };
    }
  }

  private generateOperationId(): string {
    return `bg_sync_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

// Export a singleton instance
export const backgroundSyncService = new BackgroundSyncService();