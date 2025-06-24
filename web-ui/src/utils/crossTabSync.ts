/**
 * Cross-Tab Synchronization Manager
 * Handles cart synchronization across multiple browser tabs using BroadcastChannel API
 */

import type { Cart } from '../types';

export interface TabSyncMessage {
  type: 'CART_UPDATED' | 'CART_CLEARED' | 'ITEM_ADDED' | 'ITEM_REMOVED' | 'QUANTITY_UPDATED' | 'SESSION_CHANGED';
  payload: any;
  timestamp: number;
  tabId: string;
  sessionId?: string;
}

export interface TabSyncOptions {
  channelName?: string;
  debounceMs?: number;
  ignoreOwnMessages?: boolean;
}

export interface TabSyncEventHandlers {
  onCartUpdated?: (cart: Cart, message: TabSyncMessage) => void;
  onCartCleared?: (message: TabSyncMessage) => void;
  onItemAdded?: (payload: { productId: string; quantity: number }, message: TabSyncMessage) => void;
  onItemRemoved?: (payload: { productId: string }, message: TabSyncMessage) => void;
  onQuantityUpdated?: (payload: { productId: string; quantity: number }, message: TabSyncMessage) => void;
  onSessionChanged?: (payload: { oldSessionId: string; newSessionId: string }, message: TabSyncMessage) => void;
  onMessage?: (message: TabSyncMessage) => void;
  onError?: (error: Error) => void;
}

export class CrossTabSyncManager {
  private channel: BroadcastChannel | null = null;
  private options: Required<TabSyncOptions>;
  private handlers: TabSyncEventHandlers;
  private tabId: string;
  private messageQueue = new Map<string, TabSyncMessage>();
  private debounceTimers = new Map<string, number>();
  private isDestroyed = false;

  constructor(options: TabSyncOptions = {}, handlers: TabSyncEventHandlers = {}) {
    this.options = {
      channelName: 'aims-cart-sync',
      debounceMs: 100,
      ignoreOwnMessages: true,
      ...options
    };
    this.handlers = handlers;
    this.tabId = this.generateTabId();
    this.initializeChannel();
  }

  /**
   * Initialize the BroadcastChannel
   */
  private initializeChannel(): void {
    try {
      if (typeof BroadcastChannel === 'undefined') {
        console.warn('BroadcastChannel is not supported in this browser');
        return;
      }

      this.channel = new BroadcastChannel(this.options.channelName);
      
      this.channel.onmessage = (event) => {
        this.handleMessage(event.data as TabSyncMessage);
      };

      this.channel.onmessageerror = (error) => {
        this.handlers.onError?.(new Error(`BroadcastChannel message error: ${error}`));
      };

    } catch (error) {
      this.handlers.onError?.(error as Error);
    }
  }

  /**
   * Send a cart update message to other tabs
   */
  broadcastCartUpdate(cart: Cart, sessionId?: string): void {
    this.sendMessage({
      type: 'CART_UPDATED',
      payload: cart,
      sessionId
    });
  }

  /**
   * Send a cart cleared message to other tabs
   */
  broadcastCartClear(sessionId?: string): void {
    this.sendMessage({
      type: 'CART_CLEARED',
      payload: {},
      sessionId
    });
  }

  /**
   * Send an item added message to other tabs
   */
  broadcastItemAdded(productId: string, quantity: number, sessionId?: string): void {
    this.sendMessage({
      type: 'ITEM_ADDED',
      payload: { productId, quantity },
      sessionId
    });
  }

  /**
   * Send an item removed message to other tabs
   */
  broadcastItemRemoved(productId: string, sessionId?: string): void {
    this.sendMessage({
      type: 'ITEM_REMOVED',
      payload: { productId },
      sessionId
    });
  }

  /**
   * Send a quantity updated message to other tabs
   */
  broadcastQuantityUpdate(productId: string, quantity: number, sessionId?: string): void {
    this.sendMessage({
      type: 'QUANTITY_UPDATED',
      payload: { productId, quantity },
      sessionId
    });
  }

  /**
   * Send a session changed message to other tabs
   */
  broadcastSessionChange(oldSessionId: string, newSessionId: string): void {
    this.sendMessage({
      type: 'SESSION_CHANGED',
      payload: { oldSessionId, newSessionId }
    });
  }

  /**
   * Check if cross-tab sync is supported
   */
  isSupported(): boolean {
    return typeof BroadcastChannel !== 'undefined';
  }

  /**
   * Get the current tab ID
   */
  getTabId(): string {
    return this.tabId;
  }

  /**
   * Destroy the sync manager and clean up resources
   */
  destroy(): void {
    this.isDestroyed = true;
    
    // Clear all debounce timers
    this.debounceTimers.forEach(timer => clearTimeout(timer));
    this.debounceTimers.clear();
    
    // Clear message queue
    this.messageQueue.clear();
    
    // Close the channel
    if (this.channel) {
      this.channel.close();
      this.channel = null;
    }
  }

  /**
   * Send a message to other tabs
   */
  private sendMessage(message: Omit<TabSyncMessage, 'timestamp' | 'tabId'>): void {
    if (!this.channel || this.isDestroyed) return;

    const fullMessage: TabSyncMessage = {
      ...message,
      timestamp: Date.now(),
      tabId: this.tabId
    };

    // Debounce messages of the same type
    const debounceKey = `${message.type}_${JSON.stringify(message.payload)}`;
    
    if (this.debounceTimers.has(debounceKey)) {
      clearTimeout(this.debounceTimers.get(debounceKey)!);
    }

    const timer = window.setTimeout(() => {
      try {
        this.channel?.postMessage(fullMessage);
        this.debounceTimers.delete(debounceKey);
      } catch (error) {
        this.handlers.onError?.(error as Error);
      }
    }, this.options.debounceMs);

    this.debounceTimers.set(debounceKey, timer);
  }

  /**
   * Handle incoming messages from other tabs
   */
  private handleMessage(message: TabSyncMessage): void {
    if (this.isDestroyed) return;

    // Ignore messages from the same tab if configured to do so
    if (this.options.ignoreOwnMessages && message.tabId === this.tabId) {
      return;
    }

    // Check message age - ignore messages older than 5 seconds
    const messageAge = Date.now() - message.timestamp;
    if (messageAge > 5000) {
      console.warn(`Ignoring old cross-tab message: ${messageAge}ms old`);
      return;
    }

    // Call generic message handler
    this.handlers.onMessage?.(message);

    // Call specific handlers based on message type
    try {
      switch (message.type) {
        case 'CART_UPDATED':
          this.handlers.onCartUpdated?.(message.payload as Cart, message);
          break;
        
        case 'CART_CLEARED':
          this.handlers.onCartCleared?.(message);
          break;
        
        case 'ITEM_ADDED':
          this.handlers.onItemAdded?.(message.payload, message);
          break;
        
        case 'ITEM_REMOVED':
          this.handlers.onItemRemoved?.(message.payload, message);
          break;
        
        case 'QUANTITY_UPDATED':
          this.handlers.onQuantityUpdated?.(message.payload, message);
          break;
        
        case 'SESSION_CHANGED':
          this.handlers.onSessionChanged?.(message.payload, message);
          break;
        
        default:
          console.warn(`Unknown cross-tab message type: ${message.type}`);
      }
    } catch (error) {
      this.handlers.onError?.(error as Error);
    }
  }

  /**
   * Generate a unique tab ID
   */
  private generateTabId(): string {
    return `tab_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Storage-based fallback for browsers that don't support BroadcastChannel
 */
export class StorageBasedTabSync {
  private options: Required<TabSyncOptions>;
  private handlers: TabSyncEventHandlers;
  private tabId: string;
  private storageKey: string;
  private storageListener: (event: StorageEvent) => void;
  private cleanupInterval: number | null = null;
  private isDestroyed = false;

  constructor(options: TabSyncOptions = {}, handlers: TabSyncEventHandlers = {}) {
    this.options = {
      channelName: 'aims-cart-sync',
      debounceMs: 100,
      ignoreOwnMessages: true,
      ...options
    };
    this.handlers = handlers;
    this.tabId = this.generateTabId();
    this.storageKey = `${this.options.channelName}_messages`;
    
    this.storageListener = this.handleStorageEvent.bind(this);
    this.initializeStorage();
  }

  private initializeStorage(): void {
    window.addEventListener('storage', this.storageListener);
    
    // Clean up old messages every 30 seconds
    this.cleanupInterval = window.setInterval(() => {
      this.cleanupOldMessages();
    }, 30000);
  }

  broadcastCartUpdate(cart: Cart, sessionId?: string): void {
    this.sendMessage({
      type: 'CART_UPDATED',
      payload: cart,
      sessionId
    });
  }

  broadcastCartClear(sessionId?: string): void {
    this.sendMessage({
      type: 'CART_CLEARED',
      payload: {},
      sessionId
    });
  }

  broadcastItemAdded(productId: string, quantity: number, sessionId?: string): void {
    this.sendMessage({
      type: 'ITEM_ADDED',
      payload: { productId, quantity },
      sessionId
    });
  }

  broadcastItemRemoved(productId: string, sessionId?: string): void {
    this.sendMessage({
      type: 'ITEM_REMOVED',
      payload: { productId },
      sessionId
    });
  }

  broadcastQuantityUpdate(productId: string, quantity: number, sessionId?: string): void {
    this.sendMessage({
      type: 'QUANTITY_UPDATED',
      payload: { productId, quantity },
      sessionId
    });
  }

  broadcastSessionChange(oldSessionId: string, newSessionId: string): void {
    this.sendMessage({
      type: 'SESSION_CHANGED',
      payload: { oldSessionId, newSessionId }
    });
  }

  isSupported(): boolean {
    return typeof Storage !== 'undefined';
  }

  getTabId(): string {
    return this.tabId;
  }

  destroy(): void {
    this.isDestroyed = true;
    
    window.removeEventListener('storage', this.storageListener);
    
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = null;
    }
  }

  private sendMessage(message: Omit<TabSyncMessage, 'timestamp' | 'tabId'>): void {
    if (this.isDestroyed) return;

    const fullMessage: TabSyncMessage = {
      ...message,
      timestamp: Date.now(),
      tabId: this.tabId
    };

    try {
      const messages = this.getStoredMessages();
      messages.push(fullMessage);
      
      // Keep only the last 50 messages
      if (messages.length > 50) {
        messages.splice(0, messages.length - 50);
      }
      
      localStorage.setItem(this.storageKey, JSON.stringify(messages));
    } catch (error) {
      this.handlers.onError?.(error as Error);
    }
  }

  private handleStorageEvent(event: StorageEvent): void {
    if (event.key !== this.storageKey || this.isDestroyed) return;

    const messages = this.getStoredMessages();
    const lastMessage = messages[messages.length - 1];
    
    if (lastMessage) {
      this.handleMessage(lastMessage);
    }
  }

  private handleMessage(message: TabSyncMessage): void {
    if (this.isDestroyed) return;

    // Ignore messages from the same tab if configured to do so
    if (this.options.ignoreOwnMessages && message.tabId === this.tabId) {
      return;
    }

    // Check message age - ignore messages older than 5 seconds
    const messageAge = Date.now() - message.timestamp;
    if (messageAge > 5000) {
      return;
    }

    // Call generic message handler
    this.handlers.onMessage?.(message);

    // Call specific handlers based on message type
    try {
      switch (message.type) {
        case 'CART_UPDATED':
          this.handlers.onCartUpdated?.(message.payload as Cart, message);
          break;
        
        case 'CART_CLEARED':
          this.handlers.onCartCleared?.(message);
          break;
        
        case 'ITEM_ADDED':
          this.handlers.onItemAdded?.(message.payload, message);
          break;
        
        case 'ITEM_REMOVED':
          this.handlers.onItemRemoved?.(message.payload, message);
          break;
        
        case 'QUANTITY_UPDATED':
          this.handlers.onQuantityUpdated?.(message.payload, message);
          break;
        
        case 'SESSION_CHANGED':
          this.handlers.onSessionChanged?.(message.payload, message);
          break;
      }
    } catch (error) {
      this.handlers.onError?.(error as Error);
    }
  }

  private getStoredMessages(): TabSyncMessage[] {
    try {
      const stored = localStorage.getItem(this.storageKey);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private cleanupOldMessages(): void {
    try {
      const messages = this.getStoredMessages();
      const cutoffTime = Date.now() - 60000; // Remove messages older than 1 minute
      
      const filteredMessages = messages.filter(msg => msg.timestamp > cutoffTime);
      
      if (filteredMessages.length !== messages.length) {
        localStorage.setItem(this.storageKey, JSON.stringify(filteredMessages));
      }
    } catch (error) {
      // Ignore cleanup errors
    }
  }

  private generateTabId(): string {
    return `tab_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Factory function to create the appropriate sync manager based on browser support
 */
export function createTabSyncManager(
  options: TabSyncOptions = {},
  handlers: TabSyncEventHandlers = {}
): CrossTabSyncManager | StorageBasedTabSync {
  if (typeof BroadcastChannel !== 'undefined') {
    return new CrossTabSyncManager(options, handlers);
  } else {
    return new StorageBasedTabSync(options, handlers);
  }
}