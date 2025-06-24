import type { AxiosError } from 'axios';

// Import API_BASE_URL to construct correct URLs
const API_BASE_URL = typeof window !== 'undefined' 
  ? (import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api')
  : 'http://localhost:8080/api';

export interface ErrorReport {
  errorId: string;
  message: string;
  stack?: string;
  context: Record<string, any>;
  timestamp: string;
  userAgent: string;
  url: string;
  userId?: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  category: 'network' | 'validation' | 'authentication' | 'authorization' | 'runtime' | 'unknown';
}

export interface RetryConfig {
  maxRetries: number;
  delay: number;
  backoffMultiplier: number;
  retryCondition?: (error: any) => boolean;
}

export interface UserFriendlyError {
  title: string;
  message: string;
  action?: {
    label: string;
    handler: () => void;
  };
  recovery?: {
    label: string;
    handler: () => Promise<boolean>;
  };
}

// Default retry configuration
const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxRetries: 3,
  delay: 1000,
  backoffMultiplier: 2,
  retryCondition: (error) => {
    // Retry on network errors and 5xx server errors
    if (!error.response) return true; // Network error
    return error.response.status >= 500;
  },
};

// Enhanced retry configuration specifically for 500-level errors
const SERVER_ERROR_RETRY_CONFIG: RetryConfig = {
  maxRetries: 5,
  delay: 1000,
  backoffMultiplier: 2.5,
  retryCondition: (error) => {
    if (!error.response) return true; // Network error
    const status = error.response.status;
    // More aggressive retry for server errors
    return status >= 500 && status < 600;
  },
};

// Error classification
export const classifyError = (error: any): { severity: ErrorReport['severity']; category: ErrorReport['category'] } => {
  // Network errors
  if (!error.response) {
    return { severity: 'medium', category: 'network' };
  }

  const status = error.response?.status;

  switch (status) {
    case 400:
      return { severity: 'low', category: 'validation' };
    case 401:
      return { severity: 'medium', category: 'authentication' };
    case 403:
      return { severity: 'medium', category: 'authorization' };
    case 404:
      return { severity: 'low', category: 'runtime' };
    case 422:
      return { severity: 'low', category: 'validation' };
    case 429:
      return { severity: 'medium', category: 'network' };
    case 500:
    case 502:
    case 503:
    case 504:
      return { severity: 'high', category: 'runtime' };
    default:
      return { severity: 'medium', category: 'unknown' };
  }
};

// Create specific messages for server errors (500-level)
const createServerErrorMessage = (status: number, error: any, context?: Record<string, any>): UserFriendlyError => {
  const baseRetryHandler = async () => {
    // Enhanced retry logic for server errors
    if (context?.retryFn && typeof context.retryFn === 'function') {
      return await context.retryFn();
    }
    return true;
  };

  switch (status) {
    case 500:
      return {
        title: 'Server Error',
        message: 'Our server is experiencing technical difficulties. We\'re working to fix this quickly.',
        recovery: {
          label: 'Try Again',
          handler: baseRetryHandler,
        },
      };
    
    case 502:
      return {
        title: 'Service Temporarily Down',
        message: 'Our service is temporarily unavailable due to maintenance or high traffic. Please try again in a few moments.',
        recovery: {
          label: 'Retry',
          handler: baseRetryHandler,
        },
      };
    
    case 503:
      return {
        title: 'Service Unavailable',
        message: 'Our service is currently under maintenance. We\'ll be back shortly. Thank you for your patience.',
        recovery: {
          label: 'Try Again',
          handler: baseRetryHandler,
        },
      };
    
    case 504:
      return {
        title: 'Request Timeout',
        message: 'Your request is taking longer than expected. This might be due to high server load.',
        recovery: {
          label: 'Retry',
          handler: baseRetryHandler,
        },
      };
    
    default:
      return {
        title: 'Server Error',
        message: `We're experiencing server issues (Error ${status}). Our team has been notified and is working on a fix.`,
        recovery: {
          label: 'Try Again',
          handler: baseRetryHandler,
        },
      };
  }
};

// Create user-friendly error messages
export const createUserFriendlyError = (error: any, context?: Record<string, any>): UserFriendlyError => {
  const { category } = classifyError(error);
  const status = error.response?.status;

  switch (category) {
    case 'network':
      if (!navigator.onLine) {
        return {
          title: 'No Internet Connection',
          message: 'Please check your internet connection and try again.',
          action: {
            label: 'Retry',
            handler: () => window.location.reload(),
          },
        };
      }
      return {
        title: 'Connection Problem',
        message: 'Unable to connect to our servers. Please try again in a moment.',
        recovery: {
          label: 'Retry',
          handler: async () => {
            // Implement retry logic
            return true;
          },
        },
      };

    case 'authentication':
      return {
        title: 'Authentication Required',
        message: 'Please sign in to continue.',
        action: {
          label: 'Sign In',
          handler: () => {
            window.location.href = '/login';
          },
        },
      };

    case 'authorization':
      return {
        title: 'Access Denied',
        message: 'You don\'t have permission to perform this action.',
        action: {
          label: 'Go Back',
          handler: () => window.history.back(),
        },
      };

    case 'validation':
      const validationMessage = error.response?.data?.message || 
                               error.response?.data?.errors?.[0] ||
                               'Please check your input and try again.';
      return {
        title: 'Validation Error',
        message: validationMessage,
      };

    case 'runtime':
      if (status === 404) {
        return {
          title: 'Page Not Found',
          message: 'The page you\'re looking for doesn\'t exist.',
          action: {
            label: 'Go Home',
            handler: () => {
              window.location.href = '/';
            },
          },
        };
      }
      
      // Enhanced 500-level error handling
      if (status >= 500) {
        return createServerErrorMessage(status, error, context);
      }
      
      return {
        title: 'Something Went Wrong',
        message: 'We encountered an unexpected error. Our team has been notified.',
        recovery: {
          label: 'Try Again',
          handler: async () => {
            // Implement retry logic based on context
            return true;
          },
        },
      };

    default:
      return {
        title: 'Unexpected Error',
        message: 'An unexpected error occurred. Please try again.',
        recovery: {
          label: 'Retry',
          handler: async () => true,
        },
      };
  }
};

// Generate error report
export const generateErrorReport = (error: any, context: Record<string, any> = {}): ErrorReport => {
  const { severity, category } = classifyError(error);
  const errorId = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  return {
    errorId,
    message: error.message || 'Unknown error',
    stack: error.stack,
    context: {
      ...context,
      url: window.location.href,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
      errorType: error.constructor.name,
      ...(error.response && {
        responseStatus: error.response.status,
        responseData: error.response.data,
        responseHeaders: error.response.headers,
      }),
    },
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href,
    userId: localStorage.getItem('user_id') || undefined,
    severity,
    category,
  };
};

// Report error to monitoring service
export const reportError = async (errorReport: ErrorReport): Promise<void> => {
  try {
    // In development, just log to console
    if (import.meta.env.DEV) {
      console.group(`🚨 Error Report [${errorReport.errorId}]`);
      console.error('Severity:', errorReport.severity);
      console.error('Category:', errorReport.category);
      console.error('Message:', errorReport.message);
      console.error('Context:', errorReport.context);
      if (errorReport.stack) {
        console.error('Stack:', errorReport.stack);
      }
      console.groupEnd();
      return;
    }

    // In production, send to error reporting service
    // TODO: Integrate with actual error reporting service (e.g., Sentry, LogRocket)
    const response = await fetch(`${API_BASE_URL}/errors/report`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(errorReport),
    }).catch(() => {
      // If error reporting fails, store locally for later retry
      const storedErrors = JSON.parse(localStorage.getItem('pending_error_reports') || '[]');
      storedErrors.push(errorReport);
      localStorage.setItem('pending_error_reports', JSON.stringify(storedErrors.slice(-10))); // Keep only last 10
    });

    if (response?.ok) {
      // Successfully reported, clear any pending reports
      localStorage.removeItem('pending_error_reports');
    }
  } catch (reportingError) {
    console.error('Failed to report error:', reportingError);
  }
};

// Retry mechanism with exponential backoff
export const withRetry = async <T>(
  fn: () => Promise<T>,
  config: Partial<RetryConfig> = {}
): Promise<T> => {
  const retryConfig = { ...DEFAULT_RETRY_CONFIG, ...config };
  let lastError: any;

  for (let attempt = 0; attempt <= retryConfig.maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      // Don't retry if this is the last attempt
      if (attempt === retryConfig.maxRetries) {
        break;
      }

      // Check if we should retry this error
      if (retryConfig.retryCondition && !retryConfig.retryCondition(error)) {
        break;
      }

      // Calculate delay with exponential backoff
      const delay = retryConfig.delay * Math.pow(retryConfig.backoffMultiplier, attempt);
      
      console.info(`🔄 Retrying operation (attempt ${attempt + 1}/${retryConfig.maxRetries}) after ${delay}ms`);
      
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw lastError;
};

// Specialized retry function for server errors with enhanced retry logic
export const withServerErrorRetry = async <T>(
  fn: () => Promise<T>,
  config: Partial<RetryConfig> = {}
): Promise<T> => {
  const retryConfig = { ...SERVER_ERROR_RETRY_CONFIG, ...config };
  let lastError: any;
  
  for (let attempt = 0; attempt <= retryConfig.maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      
      // Don't retry if this is the last attempt
      if (attempt === retryConfig.maxRetries) {
        break;
      }
      
      // Check if we should retry this error
      if (retryConfig.retryCondition && !retryConfig.retryCondition(error)) {
        break;
      }
      
      // Enhanced delay calculation for server errors
      const baseDelay = retryConfig.delay;
      const exponentialDelay = baseDelay * Math.pow(retryConfig.backoffMultiplier, attempt);
      
      // Add jitter to prevent thundering herd
      const jitter = Math.random() * 0.1 * exponentialDelay;
      const delay = Math.min(exponentialDelay + jitter, 30000); // Cap at 30 seconds
      
      console.info(`🔄 Retrying server request (attempt ${attempt + 1}/${retryConfig.maxRetries}) after ${Math.round(delay)}ms`);
      
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  throw lastError;
};

// Global error handler
export const setupGlobalErrorHandling = (): void => {
  // Handle unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    const error = event.reason;
    const errorReport = generateErrorReport(error, {
      type: 'unhandledrejection',
      promise: event.promise,
    });

    reportError(errorReport);

    // Prevent the default console error
    event.preventDefault();

    console.error('🚨 Unhandled Promise Rejection:', error);
  });

  // Handle global JavaScript errors
  window.addEventListener('error', (event) => {
    const errorReport = generateErrorReport(event.error || new Error(event.message), {
      type: 'javascript',
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno,
    });

    reportError(errorReport);

    console.error('🚨 Global JavaScript Error:', event.error || event.message);
  });

  // Handle resource loading errors
  window.addEventListener('error', (event) => {
    if (event.target !== window) {
      const target = event.target as HTMLElement;
      const errorReport = generateErrorReport(new Error('Resource loading failed'), {
        type: 'resource',
        tagName: target.tagName,
        src: (target as any).src || (target as any).href,
        outerHTML: target.outerHTML,
      });

      reportError(errorReport);

      console.error('🚨 Resource Loading Error:', target);
    }
  }, true);

  console.info('✅ Global error handling initialized');
};

// Enhanced error boundary error handler
export const handleErrorBoundaryError = (error: Error, errorInfo: any, context: Record<string, any> = {}): string => {
  const errorReport = generateErrorReport(error, {
    ...context,
    componentStack: errorInfo.componentStack,
    type: 'react-error-boundary',
  });

  reportError(errorReport);

  return errorReport.errorId;
};

// API error handler
export const handleApiError = (error: AxiosError, context: Record<string, any> = {}): UserFriendlyError => {
  const errorReport = generateErrorReport(error, {
    ...context,
    type: 'api',
    method: error.config?.method,
    url: error.config?.url,
  });

  reportError(errorReport);

  return createUserFriendlyError(error, context);
};

// Form validation error handler
export const handleValidationError = (errors: Record<string, string[]>, context: Record<string, any> = {}): void => {
  const errorReport = generateErrorReport(new Error('Validation failed'), {
    ...context,
    type: 'validation',
    validationErrors: errors,
  });

  reportError(errorReport);
};

// Retry pending error reports (call on app startup when online)
export const retryPendingErrorReports = async (): Promise<void> => {
  const pendingReports = JSON.parse(localStorage.getItem('pending_error_reports') || '[]');
  
  if (pendingReports.length === 0) return;

  console.info(`📤 Retrying ${pendingReports.length} pending error reports`);

  const promises = pendingReports.map((report: ErrorReport) => reportError(report));
  
  try {
    await Promise.allSettled(promises);
    localStorage.removeItem('pending_error_reports');
    console.info('✅ All pending error reports sent successfully');
  } catch (error) {
    console.error('❌ Some error reports failed to send:', error);
  }
};

export default {
  classifyError,
  createUserFriendlyError,
  generateErrorReport,
  reportError,
  withRetry,
  withServerErrorRetry,
  setupGlobalErrorHandling,
  handleErrorBoundaryError,
  handleApiError,
  handleValidationError,
  retryPendingErrorReports,
  SERVER_ERROR_RETRY_CONFIG,
};