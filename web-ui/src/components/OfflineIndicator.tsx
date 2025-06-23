import React, { useState, useEffect } from 'react';
import { Button } from './ui';
import useNetworkStatus from '../hooks/useNetworkStatus';
import { useToast } from './common/Toast';

interface OfflineIndicatorProps {
  className?: string;
  showDetails?: boolean;
  autoReconnect?: boolean;
}

const OfflineIndicator: React.FC<OfflineIndicatorProps> = ({
  className = '',
  showDetails = true,
  autoReconnect = true,
}) => {
  const { 
    online, 
    reconnect, 
    isReconnecting, 
    lastConnected, 
    consecutiveFailures,
    effectiveType,
    downlink,
    rtt 
  } = useNetworkStatus();
  
  const { addNotification } = useToast();
  const [wasOffline, setWasOffline] = useState(false);
  const [showExtended, setShowExtended] = useState(false);

  useEffect(() => {
    if (!online && !wasOffline) {
      setWasOffline(true);
      addNotification({
        type: 'warning',
        title: 'Connection Lost',
        message: 'You are currently offline. Some features may be limited.',
        duration: 0, // Persistent until back online
      });
    } else if (online && wasOffline) {
      setWasOffline(false);
      addNotification({
        type: 'success',
        title: 'Back Online',
        message: 'Your connection has been restored.',
        duration: 3000,
      });
    }
  }, [online, wasOffline, addNotification]);

  const handleReconnect = async () => {
    const success = await reconnect();
    if (success) {
      addNotification({
        type: 'success',
        title: 'Reconnected',
        message: 'Successfully reconnected to the internet.',
        duration: 3000,
      });
    } else {
      addNotification({
        type: 'error',
        title: 'Reconnection Failed',
        message: 'Unable to establish connection. Please check your network.',
        duration: 5000,
      });
    }
  };

  const getConnectionQuality = (): { level: string; color: string; description: string } => {
    if (!online) {
      return { level: 'offline', color: 'text-red-600', description: 'No connection' };
    }

    if (effectiveType === '4g' || (downlink && downlink > 10)) {
      return { level: 'excellent', color: 'text-green-600', description: 'Excellent connection' };
    }

    if (effectiveType === '3g' || (downlink && downlink > 1.5)) {
      return { level: 'good', color: 'text-blue-600', description: 'Good connection' };
    }

    if (effectiveType === '2g' || (downlink && downlink > 0.15)) {
      return { level: 'slow', color: 'text-yellow-600', description: 'Slow connection' };
    }

    return { level: 'poor', color: 'text-orange-600', description: 'Poor connection' };
  };

  const connectionQuality = getConnectionQuality();

  const formatLastConnected = (): string => {
    if (!lastConnected) return 'Never';
    
    const now = new Date();
    const diff = now.getTime() - lastConnected.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    return 'Just now';
  };

  // Don't show indicator when online unless showing details
  if (online && !showDetails) {
    return null;
  }

  return (
    <div className={`${className}`}>
      {/* Compact Indicator */}
      <div 
        className={`
          flex items-center space-x-2 px-3 py-2 rounded-lg border cursor-pointer
          transition-all duration-200 hover:shadow-md
          ${online 
            ? 'bg-green-50 border-green-200 hover:bg-green-100' 
            : 'bg-red-50 border-red-200 hover:bg-red-100'
          }
        `}
        onClick={() => setShowExtended(!showExtended)}
      >
        {/* Connection Status Icon */}
        <div className={`flex-shrink-0 ${connectionQuality.color}`}>
          {online ? (
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path d="M16.971 6.251a.75.75 0 01-.018 1.042l-2.062 2.062a.75.75 0 11-1.042-1.062l2.062-2.062a.75.75 0 011.06.02zM9.232 8.87a.75.75 0 00-1.061 0L6.232 10.809a.75.75 0 101.061 1.061l1.939-1.939a.75.75 0 000-1.061zm5.772-.892a.75.75 0 00-1.061 0l-2.767 2.767a.75.75 0 101.061 1.061l2.767-2.767a.75.75 0 000-1.061zM10 2a.75.75 0 01.75.75v2.5a.75.75 0 01-1.5 0v-2.5A.75.75 0 0110 2zM5.404 4.343a.75.75 0 010 1.061l-1.768 1.768a.75.75 0 11-1.061-1.061l1.768-1.768a.75.75 0 011.061 0zm8.192 0a.75.75 0 011.061 0l1.768 1.768a.75.75 0 01-1.061 1.061l-1.768-1.768a.75.75 0 010-1.061zM10 16a.75.75 0 01.75.75v2.5a.75.75 0 01-1.5 0v-2.5A.75.75 0 0110 16z"/>
            </svg>
          ) : (
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06L3.28 2.22zM5.704 7.147a.75.75 0 00-.518 1.406 8.963 8.963 0 004.814 1.415c1.748 0 3.391-.499 4.814-1.415a.75.75 0 00-.518-1.406A7.463 7.463 0 0010 8.969a7.463 7.463 0 00-4.296-1.822z" clipRule="evenodd"/>
            </svg>
          )}
        </div>

        {/* Status Text */}
        <div className="flex-1 min-w-0">
          <div className={`text-sm font-medium ${connectionQuality.color}`}>
            {online ? 'Online' : 'Offline'}
          </div>
          {showDetails && (
            <div className="text-xs text-gray-600">
              {connectionQuality.description}
            </div>
          )}
        </div>

        {/* Reconnect Button (when offline) */}
        {!online && (
          <Button
            variant="outline"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              handleReconnect();
            }}
            disabled={isReconnecting}
            className="text-xs"
          >
            {isReconnecting ? 'Connecting...' : 'Retry'}
          </Button>
        )}

        {/* Expand/Collapse Icon */}
        {showDetails && (
          <div className="flex-shrink-0 text-gray-400">
            <svg 
              className={`h-4 w-4 transform transition-transform ${showExtended ? 'rotate-180' : ''}`}
              fill="currentColor" 
              viewBox="0 0 20 20"
            >
              <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/>
            </svg>
          </div>
        )}
      </div>

      {/* Extended Details */}
      {showExtended && showDetails && (
        <div className="mt-2 p-4 bg-white rounded-lg border border-gray-200 shadow-sm">
          <h4 className="text-sm font-medium text-gray-900 mb-3">Connection Details</h4>
          
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-500">Status:</span>
              <span className={`ml-2 font-medium ${connectionQuality.color}`}>
                {online ? 'Connected' : 'Disconnected'}
              </span>
            </div>
            
            {online && effectiveType && (
              <div>
                <span className="text-gray-500">Type:</span>
                <span className="ml-2 font-medium text-gray-900 uppercase">
                  {effectiveType}
                </span>
              </div>
            )}
            
            {online && downlink && (
              <div>
                <span className="text-gray-500">Speed:</span>
                <span className="ml-2 font-medium text-gray-900">
                  {downlink.toFixed(1)} Mbps
                </span>
              </div>
            )}
            
            {online && rtt && (
              <div>
                <span className="text-gray-500">Latency:</span>
                <span className="ml-2 font-medium text-gray-900">
                  {rtt}ms
                </span>
              </div>
            )}
            
            {!online && (
              <>
                <div>
                  <span className="text-gray-500">Last Connected:</span>
                  <span className="ml-2 font-medium text-gray-900">
                    {formatLastConnected()}
                  </span>
                </div>
                
                <div>
                  <span className="text-gray-500">Failed Attempts:</span>
                  <span className="ml-2 font-medium text-gray-900">
                    {consecutiveFailures}
                  </span>
                </div>
              </>
            )}
          </div>

          {!online && (
            <div className="mt-4 pt-3 border-t border-gray-200">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">
                  {autoReconnect ? 'Auto-reconnecting...' : 'Manual reconnection required'}
                </span>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleReconnect}
                  disabled={isReconnecting}
                >
                  {isReconnecting ? 'Connecting...' : 'Reconnect Now'}
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default OfflineIndicator;