import React, { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { Button, Card } from './ui';

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  errorId: string | null;
}

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo, errorId: string) => void;
  showDetails?: boolean;
  recovery?: {
    enabled: boolean;
    maxRetries?: number;
    resetTimeoutMs?: number;
  };
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  private retryCount = 0;
  private readonly maxRetries: number;
  private resetTimeout: NodeJS.Timeout | null = null;

  constructor(props: ErrorBoundaryProps) {
    super(props);
    
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    };

    this.maxRetries = props.recovery?.maxRetries ?? 3;
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    const errorId = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    return {
      hasError: true,
      error,
      errorId,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    const errorId = this.state.errorId || `error_${Date.now()}`;
    
    this.setState({
      error,
      errorInfo,
      errorId,
    });

    // Log error to console for development
    console.group(`🚨 Error Boundary Caught Error [${errorId}]`);
    console.error('Error:', error);
    console.error('Error Info:', errorInfo);
    console.error('Component Stack:', errorInfo.componentStack);
    console.groupEnd();

    // Report error to monitoring service
    this.reportError(error, errorInfo, errorId);

    // Call onError callback if provided
    if (this.props.onError) {
      this.props.onError(error, errorInfo, errorId);
    }
  }

  private reportError = async (error: Error, errorInfo: ErrorInfo, errorId: string) => {
    try {
      // In a real application, you would send this to your error reporting service
      // For now, we'll just log it
      const errorReport = {
        errorId,
        message: error.message,
        stack: error.stack,
        componentStack: errorInfo.componentStack,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        userId: localStorage.getItem('user_id') || 'anonymous',
      };

      // Simulate error reporting
      console.info('📊 Error Report:', errorReport);
      
      // TODO: Integrate with actual error reporting service (e.g., Sentry, LogRocket)
      // await errorReportingService.captureException(errorReport);
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError);
    }
  };

  private handleRetry = () => {
    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      console.info(`🔄 Retrying component recovery (attempt ${this.retryCount}/${this.maxRetries})`);
      
      this.setState({
        hasError: false,
        error: null,
        errorInfo: null,
        errorId: null,
      });
    }
  };

  private handleReset = () => {
    this.retryCount = 0;
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    });
  };

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  componentDidUpdate(prevProps: ErrorBoundaryProps, prevState: ErrorBoundaryState) {
    // Auto-recovery mechanism
    if (
      this.state.hasError &&
      !prevState.hasError &&
      this.props.recovery?.enabled &&
      this.retryCount === 0
    ) {
      const resetTimeoutMs = this.props.recovery?.resetTimeoutMs ?? 5000;
      
      this.resetTimeout = setTimeout(() => {
        console.info('🔄 Auto-recovery timeout triggered');
        this.handleRetry();
      }, resetTimeoutMs);
    }
  }

  componentWillUnmount() {
    if (this.resetTimeout) {
      clearTimeout(this.resetTimeout);
    }
  }

  render() {
    if (this.state.hasError) {
      // Use custom fallback if provided
      if (this.props.fallback) {
        return this.props.fallback;
      }

      const { error, errorInfo, errorId } = this.state;
      const canRetry = this.retryCount < this.maxRetries && this.props.recovery?.enabled;

      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <Card className="max-w-lg w-full text-center">
            <div className="mb-6">
              <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 mb-4">
                <svg
                  className="h-6 w-6 text-red-600"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.694-.833-2.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"
                  />
                </svg>
              </div>
              <h1 className="text-xl font-semibold text-gray-900 mb-2">
                Something went wrong
              </h1>
              <p className="text-gray-600 mb-4">
                We apologize for the inconvenience. The application encountered an unexpected error.
              </p>
              {errorId && (
                <p className="text-xs text-gray-500 mb-4">
                  Error ID: <code className="bg-gray-100 px-2 py-1 rounded">{errorId}</code>
                </p>
              )}
            </div>

            <div className="flex flex-col sm:flex-row gap-3 mb-6">
              {canRetry && (
                <Button variant="primary" onClick={this.handleRetry} className="flex-1">
                  Try Again {this.retryCount > 0 && `(${this.maxRetries - this.retryCount} left)`}
                </Button>
              )}
              <Button variant="secondary" onClick={this.handleReload} className="flex-1">
                Reload Page
              </Button>
              <Button variant="outline" onClick={this.handleGoHome} className="flex-1">
                Go Home
              </Button>
            </div>

            {this.props.showDetails && error && (
              <details className="text-left">
                <summary className="cursor-pointer text-sm text-gray-600 hover:text-gray-800 mb-2">
                  Technical Details
                </summary>
                <div className="bg-gray-50 p-3 rounded text-xs space-y-2">
                  <div>
                    <strong>Error:</strong>
                    <pre className="mt-1 whitespace-pre-wrap break-words">{error.message}</pre>
                  </div>
                  {error.stack && (
                    <div>
                      <strong>Stack Trace:</strong>
                      <pre className="mt-1 whitespace-pre-wrap break-words text-gray-600">
                        {error.stack}
                      </pre>
                    </div>
                  )}
                  {errorInfo && (
                    <div>
                      <strong>Component Stack:</strong>
                      <pre className="mt-1 whitespace-pre-wrap break-words text-gray-600">
                        {errorInfo.componentStack}
                      </pre>
                    </div>
                  )}
                </div>
              </details>
            )}

            <div className="mt-6 pt-4 border-t border-gray-200">
              <p className="text-xs text-gray-500">
                If this problem persists, please contact support with the error ID above.
              </p>
            </div>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}

// Higher-order component for easier usage
export const withErrorBoundary = <P extends object>(
  Component: React.ComponentType<P>,
  errorBoundaryProps?: Omit<ErrorBoundaryProps, 'children'>
) => {
  const WrappedComponent: React.FC<P> = (props) => (
    <ErrorBoundary {...errorBoundaryProps}>
      <Component {...props} />
    </ErrorBoundary>
  );

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`;
  return WrappedComponent;
};

// Hook for manual error reporting
export const useErrorHandler = () => {
  const reportError = React.useCallback((error: Error, context?: Record<string, any>) => {
    const errorId = `manual_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    console.group(`🚨 Manual Error Report [${errorId}]`);
    console.error('Error:', error);
    console.error('Context:', context);
    console.groupEnd();

    // Report to monitoring service
    // In a real application, integrate with your error reporting service
    const errorReport = {
      errorId,
      message: error.message,
      stack: error.stack,
      context,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      userId: localStorage.getItem('user_id') || 'anonymous',
    };

    console.info('📊 Manual Error Report:', errorReport);
  }, []);

  return { reportError };
};

export default ErrorBoundary;