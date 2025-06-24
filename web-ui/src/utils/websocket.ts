/**
 * WebSocket Connection Manager for Real-time Cart Synchronization
 * Handles WebSocket connections with automatic reconnection, heartbeat, and error recovery
 */

export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp: number;
  messageId: string;
}

export interface WebSocketOptions {
  url: string;
  protocols?: string[];
  heartbeatInterval?: number;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
  connectionTimeout?: number;
}

export interface WebSocketEventHandlers {
  onOpen?: (event: Event) => void;
  onMessage?: (message: WebSocketMessage) => void;
  onClose?: (event: CloseEvent) => void;
  onError?: (error: Event) => void;
  onReconnecting?: (attempt: number) => void;
  onReconnected?: () => void;
  onMaxReconnectAttemptsReached?: () => void;
}

export class WebSocketManager {
  private ws: WebSocket | null = null;
  private options: Required<WebSocketOptions>;
  private handlers: WebSocketEventHandlers;
  private reconnectTimer: number | null = null;
  private heartbeatTimer: number | null = null;
  private reconnectAttempts = 0;
  private isReconnecting = false;
  private isDestroyed = false;
  private lastHeartbeat = 0;

  constructor(options: WebSocketOptions, handlers: WebSocketEventHandlers = {}) {
    this.options = {
      protocols: [],
      heartbeatInterval: 30000, // 30 seconds
      reconnectInterval: 5000, // 5 seconds
      maxReconnectAttempts: 10,
      connectionTimeout: 15000, // 15 seconds
      ...options
    };
    this.handlers = handlers;
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isDestroyed) {
        reject(new Error('WebSocket manager has been destroyed'));
        return;
      }

      try {
        this.ws = new WebSocket(this.options.url, this.options.protocols);
        
        const connectionTimeout = setTimeout(() => {
          if (this.ws?.readyState === WebSocket.CONNECTING) {
            this.ws.close();
            reject(new Error('Connection timeout'));
          }
        }, this.options.connectionTimeout);

        this.ws.onopen = (event) => {
          clearTimeout(connectionTimeout);
          this.reconnectAttempts = 0;
          this.isReconnecting = false;
          this.startHeartbeat();
          this.handlers.onOpen?.(event);
          
          if (this.isReconnecting) {
            this.handlers.onReconnected?.();
          }
          
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data);
            
            // Handle heartbeat responses
            if (message.type === 'heartbeat_response') {
              this.lastHeartbeat = Date.now();
              return;
            }
            
            this.handlers.onMessage?.(message);
          } catch (error) {
            console.error('Failed to parse WebSocket message:', error);
          }
        };

        this.ws.onclose = (event) => {
          clearTimeout(connectionTimeout);
          this.stopHeartbeat();
          this.handlers.onClose?.(event);
          
          // Only attempt reconnection if not manually closed and not destroyed
          if (!this.isDestroyed && event.code !== 1000) {
            this.attemptReconnect();
          }
        };

        this.ws.onerror = (error) => {
          clearTimeout(connectionTimeout);
          this.handlers.onError?.(error);
          reject(error);
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  send(message: Omit<WebSocketMessage, 'timestamp' | 'messageId'>): boolean {
    if (!this.isConnected()) {
      console.warn('Cannot send message: WebSocket not connected');
      return false;
    }

    try {
      const fullMessage: WebSocketMessage = {
        ...message,
        timestamp: Date.now(),
        messageId: this.generateMessageId()
      };

      this.ws!.send(JSON.stringify(fullMessage));
      return true;
    } catch (error) {
      console.error('Failed to send WebSocket message:', error);
      return false;
    }
  }

  close(): void {
    this.isDestroyed = true;
    this.stopReconnect();
    this.stopHeartbeat();
    
    if (this.ws) {
      this.ws.close(1000, 'Client closing connection');
      this.ws = null;
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  getConnectionState(): string {
    if (!this.ws) return 'DISCONNECTED';
    
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING: return 'CONNECTING';
      case WebSocket.OPEN: return 'CONNECTED';
      case WebSocket.CLOSING: return 'CLOSING';
      case WebSocket.CLOSED: return 'DISCONNECTED';
      default: return 'UNKNOWN';
    }
  }

  private attemptReconnect(): void {
    if (this.isDestroyed || this.isReconnecting) return;

    if (this.reconnectAttempts >= this.options.maxReconnectAttempts) {
      this.handlers.onMaxReconnectAttemptsReached?.();
      return;
    }

    this.isReconnecting = true;
    this.reconnectAttempts++;
    this.handlers.onReconnecting?.(this.reconnectAttempts);

    this.reconnectTimer = window.setTimeout(() => {
      this.connect().catch(() => {
        // Connection failed, will try again
      });
    }, this.options.reconnectInterval);
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.lastHeartbeat = Date.now();
    
    this.heartbeatTimer = window.setInterval(() => {
      if (!this.isConnected()) return;

      // Send heartbeat
      this.send({
        type: 'heartbeat',
        payload: { timestamp: Date.now() }
      });

      // Check if we received a response to the last heartbeat
      const timeSinceLastHeartbeat = Date.now() - this.lastHeartbeat;
      if (timeSinceLastHeartbeat > this.options.heartbeatInterval * 2) {
        console.warn('Heartbeat timeout detected, closing connection');
        this.ws?.close();
      }
    }, this.options.heartbeatInterval);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private stopReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.isReconnecting = false;
  }

  private generateMessageId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Server-Sent Events Manager for Cart Synchronization
 * Alternative to WebSocket for one-way communication from server
 */
export interface SSEOptions {
  url: string;
  withCredentials?: boolean;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
}

export interface SSEEventHandlers {
  onOpen?: (event: Event) => void;
  onMessage?: (event: MessageEvent) => void;
  onError?: (error: Event) => void;
  onReconnecting?: (attempt: number) => void;
  onMaxReconnectAttemptsReached?: () => void;
}

export class SSEManager {
  private eventSource: EventSource | null = null;
  private options: Required<SSEOptions>;
  private handlers: SSEEventHandlers;
  private reconnectTimer: number | null = null;
  private reconnectAttempts = 0;
  private isReconnecting = false;
  private isDestroyed = false;

  constructor(options: SSEOptions, handlers: SSEEventHandlers = {}) {
    this.options = {
      withCredentials: false,
      reconnectInterval: 5000,
      maxReconnectAttempts: 10,
      ...options
    };
    this.handlers = handlers;
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isDestroyed) {
        reject(new Error('SSE manager has been destroyed'));
        return;
      }

      try {
        this.eventSource = new EventSource(this.options.url, {
          withCredentials: this.options.withCredentials
        });

        this.eventSource.onopen = (event) => {
          this.reconnectAttempts = 0;
          this.isReconnecting = false;
          this.handlers.onOpen?.(event);
          resolve();
        };

        this.eventSource.onmessage = (event) => {
          this.handlers.onMessage?.(event);
        };

        this.eventSource.onerror = (error) => {
          this.handlers.onError?.(error);
          
          if (this.eventSource?.readyState === EventSource.CLOSED) {
            this.attemptReconnect();
          } else {
            reject(error);
          }
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  close(): void {
    this.isDestroyed = true;
    this.stopReconnect();
    
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  isConnected(): boolean {
    return this.eventSource?.readyState === EventSource.OPEN;
  }

  getConnectionState(): string {
    if (!this.eventSource) return 'DISCONNECTED';
    
    switch (this.eventSource.readyState) {
      case EventSource.CONNECTING: return 'CONNECTING';
      case EventSource.OPEN: return 'CONNECTED';
      case EventSource.CLOSED: return 'DISCONNECTED';
      default: return 'UNKNOWN';
    }
  }

  private attemptReconnect(): void {
    if (this.isDestroyed || this.isReconnecting) return;

    if (this.reconnectAttempts >= this.options.maxReconnectAttempts) {
      this.handlers.onMaxReconnectAttemptsReached?.();
      return;
    }

    this.isReconnecting = true;
    this.reconnectAttempts++;
    this.handlers.onReconnecting?.(this.reconnectAttempts);

    this.reconnectTimer = window.setTimeout(() => {
      this.connect().catch(() => {
        // Connection failed, will try again
      });
    }, this.options.reconnectInterval);
  }

  private stopReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.isReconnecting = false;
  }
}