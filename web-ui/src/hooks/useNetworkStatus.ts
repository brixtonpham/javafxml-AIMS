import { useState, useEffect, useCallback } from 'react';

// Get API base URL for health checks
const API_BASE_URL = typeof window !== 'undefined' 
  ? (import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api')
  : 'http://localhost:8080/api';

export interface NetworkStatus {
  online: boolean;
  downlink?: number;
  effectiveType?: string;
  rtt?: number;
  saveData?: boolean;
  type?: string;
}

export interface NetworkStatusHook extends NetworkStatus {
  reconnect: () => Promise<boolean>;
  isReconnecting: boolean;
  lastConnected: Date | null;
  consecutiveFailures: number;
}

const useNetworkStatus = (): NetworkStatusHook => {
  const [networkStatus, setNetworkStatus] = useState<NetworkStatus>(() => ({
    online: navigator.onLine,
    downlink: (navigator as any).connection?.downlink,
    effectiveType: (navigator as any).connection?.effectiveType,
    rtt: (navigator as any).connection?.rtt,
    saveData: (navigator as any).connection?.saveData,
    type: (navigator as any).connection?.type,
  }));

  const [isReconnecting, setIsReconnecting] = useState(false);
  const [lastConnected, setLastConnected] = useState<Date | null>(
    navigator.onLine ? new Date() : null
  );
  const [consecutiveFailures, setConsecutiveFailures] = useState(0);

  const updateNetworkStatus = useCallback(() => {
    const connection = (navigator as any).connection;
    const isOnline = navigator.onLine;

    setNetworkStatus({
      online: isOnline,
      downlink: connection?.downlink,
      effectiveType: connection?.effectiveType,
      rtt: connection?.rtt,
      saveData: connection?.saveData,
      type: connection?.type,
    });

    if (isOnline) {
      setLastConnected(new Date());
      setConsecutiveFailures(0);
    } else {
      setConsecutiveFailures(prev => prev + 1);
    }
  }, []);

  const reconnect = useCallback(async (): Promise<boolean> => {
    if (isReconnecting) return false;

    setIsReconnecting(true);

    try {
      // Test connectivity with a simple fetch to a reliable endpoint
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);

      const response = await fetch(`${API_BASE_URL}/health-check`, {
        method: 'HEAD',
        cache: 'no-cache',
        signal: controller.signal,
      }).catch(() => {
        // Fallback: try a different endpoint or method
        return fetch('/?_health=1', {
          method: 'HEAD',
          cache: 'no-cache',
          signal: controller.signal,
        });
      });

      clearTimeout(timeoutId);

      if (response.ok) {
        updateNetworkStatus();
        return true;
      }

      return false;
    } catch (error) {
      console.warn('Network reconnection failed:', error);
      return false;
    } finally {
      setIsReconnecting(false);
    }
  }, [isReconnecting, updateNetworkStatus]);

  useEffect(() => {
    const handleOnline = () => {
      console.info('🌐 Network: Back online');
      updateNetworkStatus();
    };

    const handleOffline = () => {
      console.warn('🌐 Network: Gone offline');
      updateNetworkStatus();
    };

    const handleConnectionChange = () => {
      updateNetworkStatus();
    };

    // Listen for online/offline events
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Listen for connection changes (if supported)
    const connection = (navigator as any).connection;
    if (connection) {
      connection.addEventListener('change', handleConnectionChange);
    }

    // Periodic connectivity check when offline
    let intervalId: NodeJS.Timeout | null = null;
    if (!networkStatus.online) {
      intervalId = setInterval(() => {
        reconnect();
      }, 30000); // Check every 30 seconds
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      
      if (connection) {
        connection.removeEventListener('change', handleConnectionChange);
      }

      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [networkStatus.online, updateNetworkStatus, reconnect]);

  return {
    ...networkStatus,
    reconnect,
    isReconnecting,
    lastConnected,
    consecutiveFailures,
  };
};

export default useNetworkStatus;