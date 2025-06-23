import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { Notification } from '../../types';

interface ToastContextType {
  notifications: Notification[];
  addNotification: (notification: Omit<Notification, 'id'>) => string;
  removeNotification: (id: string) => void;
  clearAll: () => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (context === undefined) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

interface ToastProviderProps {
  children: ReactNode;
  maxNotifications?: number;
  defaultDuration?: number;
}

export const ToastProvider: React.FC<ToastProviderProps> = ({ 
  children, 
  maxNotifications = 5,
  defaultDuration = 5000 
}) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);

  const addNotification = useCallback((notification: Omit<Notification, 'id'>) => {
    const id = `toast_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const newNotification: Notification = {
      ...notification,
      id,
      duration: notification.duration || defaultDuration,
    };

    setNotifications(prev => {
      const updated = [newNotification, ...prev].slice(0, maxNotifications);
      return updated;
    });

    return id;
  }, [maxNotifications, defaultDuration]);

  const removeNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(notification => notification.id !== id));
  }, []);

  const clearAll = useCallback(() => {
    setNotifications([]);
  }, []);

  const value = {
    notifications,
    addNotification,
    removeNotification,
    clearAll,
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer />
    </ToastContext.Provider>
  );
};

const ToastContainer: React.FC = () => {
  const { notifications } = useToast();

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2">
      {notifications.map(notification => (
        <ToastComponent key={notification.id} notification={notification} />
      ))}
    </div>
  );
};

interface ToastComponentProps {
  notification: Notification;
}

const ToastComponent: React.FC<ToastComponentProps> = ({ notification }) => {
  const { removeNotification } = useToast();
  const [isVisible, setIsVisible] = useState(false);
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    // Trigger entrance animation
    const timer = setTimeout(() => setIsVisible(true), 10);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (notification.duration && notification.duration > 0) {
      const timer = setTimeout(() => {
        handleClose();
      }, notification.duration);

      return () => clearTimeout(timer);
    }
  }, [notification.duration]);

  const handleClose = () => {
    setIsExiting(true);
    setTimeout(() => {
      removeNotification(notification.id);
    }, 300); // Match animation duration
  };

  const getIcon = () => {
    switch (notification.type) {
      case 'success':
        return (
          <svg className="h-5 w-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        );
      case 'error':
        return (
          <svg className="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
          </svg>
        );
      case 'warning':
        return (
          <svg className="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        );
      case 'info':
      default:
        return (
          <svg className="h-5 w-5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  const getBackgroundColor = () => {
    switch (notification.type) {
      case 'success':
        return 'bg-green-50 border-green-200';
      case 'error':
        return 'bg-red-50 border-red-200';
      case 'warning':
        return 'bg-yellow-50 border-yellow-200';
      case 'info':
      default:
        return 'bg-blue-50 border-blue-200';
    }
  };

  const getTextColor = () => {
    switch (notification.type) {
      case 'success':
        return 'text-green-800';
      case 'error':
        return 'text-red-800';
      case 'warning':
        return 'text-yellow-800';
      case 'info':
      default:
        return 'text-blue-800';
    }
  };

  return (
    <div
      className={`
        max-w-sm w-full border rounded-lg p-4 shadow-lg backdrop-blur-sm
        transform transition-all duration-300 ease-in-out
        ${getBackgroundColor()}
        ${isVisible && !isExiting 
          ? 'translate-x-0 opacity-100 scale-100' 
          : 'translate-x-full opacity-0 scale-95'
        }
      `}
      role="alert"
      aria-live="polite"
    >
      <div className="flex items-start">
        <div className="flex-shrink-0">
          {getIcon()}
        </div>
        
        <div className="ml-3 flex-1">
          <div className={`text-sm font-medium ${getTextColor()}`}>
            {notification.title}
          </div>
          <div className={`mt-1 text-sm ${getTextColor()} opacity-90`}>
            {notification.message}
          </div>
          
          {notification.action && (
            <div className="mt-3">
              <button
                onClick={notification.action.onClick}
                className={`
                  text-sm font-medium underline hover:no-underline
                  ${getTextColor()}
                `}
              >
                {notification.action.label}
              </button>
            </div>
          )}
        </div>
        
        <div className="ml-4 flex-shrink-0">
          <button
            onClick={handleClose}
            className={`
              inline-flex rounded-md p-1.5 hover:bg-black hover:bg-opacity-10
              focus:outline-none focus:ring-2 focus:ring-offset-2
              ${getTextColor()}
            `}
            aria-label="Close notification"
          >
            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};

// Convenience hooks for different notification types
export const useSuccessToast = () => {
  const { addNotification } = useToast();
  
  return useCallback((title: string, message: string, options?: {
    duration?: number;
    action?: { label: string; onClick: () => void };
  }) => {
    return addNotification({
      type: 'success',
      title,
      message,
      ...options,
    });
  }, [addNotification]);
};

export const useErrorToast = () => {
  const { addNotification } = useToast();
  
  return useCallback((title: string, message: string, options?: {
    duration?: number;
    action?: { label: string; onClick: () => void };
  }) => {
    return addNotification({
      type: 'error',
      title,
      message,
      duration: options?.duration || 8000, // Longer duration for errors
      ...options,
    });
  }, [addNotification]);
};

export const useWarningToast = () => {
  const { addNotification } = useToast();
  
  return useCallback((title: string, message: string, options?: {
    duration?: number;
    action?: { label: string; onClick: () => void };
  }) => {
    return addNotification({
      type: 'warning',
      title,
      message,
      duration: options?.duration || 6000, // Slightly longer for warnings
      ...options,
    });
  }, [addNotification]);
};

export const useInfoToast = () => {
  const { addNotification } = useToast();
  
  return useCallback((title: string, message: string, options?: {
    duration?: number;
    action?: { label: string; onClick: () => void };
  }) => {
    return addNotification({
      type: 'info',
      title,
      message,
      ...options,
    });
  }, [addNotification]);
};

// Global toast functions for use outside React components
let globalToastContext: ToastContextType | null = null;

export const setGlobalToastContext = (context: ToastContextType) => {
  globalToastContext = context;
};

export const toast = {
  success: (title: string, message: string, options?: { duration?: number }) => {
    if (globalToastContext) {
      return globalToastContext.addNotification({
        type: 'success',
        title,
        message,
        ...options,
      });
    }
  },
  error: (title: string, message: string, options?: { duration?: number }) => {
    if (globalToastContext) {
      return globalToastContext.addNotification({
        type: 'error',
        title,
        message,
        duration: options?.duration || 8000,
        ...options,
      });
    }
  },
  warning: (title: string, message: string, options?: { duration?: number }) => {
    if (globalToastContext) {
      return globalToastContext.addNotification({
        type: 'warning',
        title,
        message,
        duration: options?.duration || 6000,
        ...options,
      });
    }
  },
  info: (title: string, message: string, options?: { duration?: number }) => {
    if (globalToastContext) {
      return globalToastContext.addNotification({
        type: 'info',
        title,
        message,
        ...options,
      });
    }
  },
};

export default toast;