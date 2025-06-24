import React, { useState, useEffect } from 'react';
import Card from '../ui/Card';
import Button from '../ui/Button';

interface SystemStatusProps {
  showHeader?: boolean;
  className?: string;
}

interface SystemHealth {
  overall: 'healthy' | 'warning' | 'critical';
  database: 'connected' | 'disconnected' | 'slow';
  api: 'operational' | 'degraded' | 'down';
  storage: 'available' | 'limited' | 'full';
  lastChecked: Date;
}

const SystemStatus: React.FC<SystemStatusProps> = ({
  showHeader = true,
  className = '',
}) => {
  const [systemHealth, setSystemHealth] = useState<SystemHealth | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSystemHealth();
    
    // Set up periodic health checks every 5 minutes
    const interval = setInterval(fetchSystemHealth, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  const fetchSystemHealth = async () => {
    try {
      setError(null);
      // Note: This would be a real endpoint in production
      // For now, we'll simulate the data
      const mockHealth: SystemHealth = {
        overall: 'healthy',
        database: 'connected',
        api: 'operational',
        storage: 'available',
        lastChecked: new Date(),
      };
      
      setSystemHealth(mockHealth);
    } catch (err) {
      setError('Failed to fetch system health');
      console.error('Error fetching system health:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
      case 'connected':
      case 'operational':
      case 'available':
        return 'text-green-600 bg-green-100';
      case 'warning':
      case 'slow':
      case 'degraded':
      case 'limited':
        return 'text-yellow-600 bg-yellow-100';
      case 'critical':
      case 'disconnected':
      case 'down':
      case 'full':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
      case 'connected':
      case 'operational':
      case 'available':
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        );
      case 'warning':
      case 'slow':
      case 'degraded':
      case 'limited':
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        );
      case 'critical':
      case 'disconnected':
      case 'down':
      case 'full':
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
          </svg>
        );
      default:
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  const formatLastChecked = (date: Date) => {
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));
    
    if (diffInMinutes < 1) {
      return 'Just now';
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else {
      const diffInHours = Math.floor(diffInMinutes / 60);
      return `${diffInHours}h ago`;
    }
  };

  if (isLoading) {
    return (
      <Card className={`p-6 ${className}`}>
        {showHeader && (
          <h3 className="text-lg font-semibold text-gray-900 mb-4">System Status</h3>
        )}
        <div className="space-y-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="flex items-center justify-between animate-pulse">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
                <div className="h-4 bg-gray-200 rounded w-24"></div>
              </div>
              <div className="h-6 bg-gray-200 rounded w-16"></div>
            </div>
          ))}
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={`p-6 ${className}`}>
        {showHeader && (
          <h3 className="text-lg font-semibold text-gray-900 mb-4">System Status</h3>
        )}
        <div className="text-center py-4">
          <p className="text-red-600 mb-2">{error}</p>
          <Button variant="outline" size="sm" onClick={fetchSystemHealth}>
            Retry
          </Button>
        </div>
      </Card>
    );
  }

  if (!systemHealth) {
    return null;
  }

  const statusItems = [
    { label: 'Overall Health', value: systemHealth.overall },
    { label: 'Database', value: systemHealth.database },
    { label: 'API Status', value: systemHealth.api },
    { label: 'Storage', value: systemHealth.storage },
  ];

  return (
    <Card className={`p-6 ${className}`}>
      {showHeader && (
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">System Status</h3>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchSystemHealth}
            className="text-xs"
          >
            Refresh
          </Button>
        </div>
      )}
      
      <div className="space-y-4">
        {statusItems.map((item) => (
          <div key={item.label} className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center ${getStatusColor(item.value)}`}>
                {getStatusIcon(item.value)}
              </div>
              <span className="text-sm font-medium text-gray-900">{item.label}</span>
            </div>
            <span className={`px-2 py-1 text-xs font-medium rounded-full capitalize ${getStatusColor(item.value)}`}>
              {item.value}
            </span>
          </div>
        ))}
      </div>
      
      <div className="mt-6 pt-4 border-t border-gray-200">
        <div className="flex items-center justify-between text-sm text-gray-500">
          <span>Last checked: {formatLastChecked(systemHealth.lastChecked)}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.location.href = '/admin/system/health'}
            className="text-xs"
          >
            View Details
          </Button>
        </div>
      </div>
    </Card>
  );
};

export default SystemStatus;