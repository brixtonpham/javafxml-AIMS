/**
 * LazyWrapper component for AIMS Web Application
 * Provides Suspense wrapper with loading states and error boundaries for lazy-loaded components
 */

import React, { Suspense, Component } from 'react';
import type { ReactNode, ComponentType, ErrorInfo } from 'react';

// Loading component props
interface LoadingProps {
  message?: string;
  size?: 'small' | 'medium' | 'large';
  variant?: 'spinner' | 'skeleton' | 'dots';
}

// Error boundary state
interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

// LazyWrapper props
interface LazyWrapperProps {
  children: ReactNode;
  fallback?: ComponentType<LoadingProps>;
  errorFallback?: ComponentType<{ error?: Error; retry: () => void }>;
  loadingProps?: LoadingProps;
  retryOnError?: boolean;
  timeout?: number;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  onLoad?: () => void;
}

// Default loading component
const DefaultLoading: React.FC<LoadingProps> = ({ 
  message = 'Loading...', 
  size = 'medium',
  variant = 'spinner' 
}) => {
  const sizeClasses = {
    small: 'w-4 h-4',
    medium: 'w-8 h-8',
    large: 'w-12 h-12',
  };

  const renderSpinner = () => (
    <div className="flex items-center justify-center p-8">
      <div className="flex flex-col items-center space-y-3">
        <div className={`${sizeClasses[size]} animate-spin`}>
          <svg 
            className="w-full h-full text-blue-600" 
            xmlns="http://www.w3.org/2000/svg" 
            fill="none" 
            viewBox="0 0 24 24"
          >
            <circle 
              className="opacity-25" 
              cx="12" 
              cy="12" 
              r="10" 
              stroke="currentColor" 
              strokeWidth="4"
            />
            <path 
              className="opacity-75" 
              fill="currentColor" 
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        </div>
        <p className="text-gray-600 text-sm font-medium">{message}</p>
      </div>
    </div>
  );

  const renderSkeleton = () => (
    <div className="animate-pulse p-8">
      <div className="space-y-4">
        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
        <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
      </div>
    </div>
  );

  const renderDots = () => (
    <div className="flex items-center justify-center p-8">
      <div className="flex flex-col items-center space-y-3">
        <div className="flex space-x-2">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className={`${sizeClasses[size]} bg-blue-600 rounded-full animate-bounce`}
              style={{ animationDelay: `${i * 0.1}s` }}
            />
          ))}
        </div>
        <p className="text-gray-600 text-sm font-medium">{message}</p>
      </div>
    </div>
  );

  const variants = {
    spinner: renderSpinner,
    skeleton: renderSkeleton,
    dots: renderDots,
  };

  return variants[variant]();
};

// Default error fallback component
const DefaultErrorFallback: React.FC<{ error?: Error; retry: () => void }> = ({ 
  error, 
  retry 
}) => (
  <div className="flex items-center justify-center p-8">
    <div className="text-center max-w-md">
      <div className="mb-4">
        <svg 
          className="mx-auto h-12 w-12 text-red-500" 
          fill="none" 
          viewBox="0 0 24 24" 
          stroke="currentColor"
        >
          <path 
            strokeLinecap="round" 
            strokeLinejoin="round" 
            strokeWidth={2} 
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" 
          />
        </svg>
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">
        Failed to load component
      </h3>
      <p className="text-sm text-gray-600 mb-4">
        {error?.message || 'An unexpected error occurred while loading this component.'}
      </p>
      <button
        onClick={retry}
        className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
      >
        Try again
      </button>
    </div>
  </div>
);

// Error boundary for lazy loaded components
class LazyErrorBoundary extends Component<
  {
    children: ReactNode;
    fallback: ComponentType<{ error?: Error; retry: () => void }>;
    onError?: (error: Error, errorInfo: ErrorInfo) => void;
    onRetry: () => void;
  },
  ErrorBoundaryState
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });
    this.props.onError?.(error, errorInfo);
    
    // Log error to console in development
    if (import.meta.env.DEV) {
      console.error('LazyWrapper Error Boundary caught an error:', error, errorInfo);
    }
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
    this.props.onRetry();
  };

  render() {
    if (this.state.hasError) {
      const ErrorFallback = this.props.fallback;
      return <ErrorFallback error={this.state.error} retry={this.handleRetry} />;
    }

    return this.props.children;
  }
}

// Main LazyWrapper component
const LazyWrapper: React.FC<LazyWrapperProps> = ({
  children,
  fallback: LoadingComponent = DefaultLoading,
  errorFallback: ErrorFallbackComponent = DefaultErrorFallback,
  loadingProps = {},
  retryOnError = true,
  timeout = 10000,
  onError,
  onLoad,
}) => {
  const [retryKey, setRetryKey] = React.useState(0);
  const [isLoading, setIsLoading] = React.useState(true);
  const timeoutRef = React.useRef<NodeJS.Timeout | undefined>(undefined);

  const handleRetry = React.useCallback(() => {
    setRetryKey(prev => prev + 1);
    setIsLoading(true);
  }, []);

  React.useEffect(() => {
    // Set up timeout for loading
    timeoutRef.current = setTimeout(() => {
      if (isLoading) {
        console.warn('LazyWrapper: Component loading timeout exceeded');
      }
    }, timeout);

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [timeout, isLoading, retryKey]);

  React.useEffect(() => {
    // Mark as loaded when children render successfully
    const timer = setTimeout(() => {
      setIsLoading(false);
      onLoad?.();
    }, 100);

    return () => clearTimeout(timer);
  }, [children, onLoad]);

  const loadingFallback = React.useMemo(
    () => <LoadingComponent {...loadingProps} />,
    [LoadingComponent, loadingProps]
  );

  if (!retryOnError) {
    return (
      <Suspense fallback={loadingFallback}>
        {children}
      </Suspense>
    );
  }

  return (
    <LazyErrorBoundary
      key={retryKey}
      fallback={ErrorFallbackComponent}
      onError={onError}
      onRetry={handleRetry}
    >
      <Suspense fallback={loadingFallback}>
        {children}
      </Suspense>
    </LazyErrorBoundary>
  );
};

// Higher-order component for wrapping lazy components
export function withLazyWrapper<P extends object>(
  Component: ComponentType<P>,
  wrapperProps?: Omit<LazyWrapperProps, 'children'>
) {
  const WrappedComponent = React.forwardRef<any, P>((props, ref) => (
    <LazyWrapper {...wrapperProps}>
      <Component {...(props as P)} />
    </LazyWrapper>
  ));

  WrappedComponent.displayName = `withLazyWrapper(${Component.displayName || Component.name})`;
  
  return WrappedComponent;
}

// Preset configurations
export const LazyWrapperPresets = {
  page: {
    loadingProps: { size: 'large' as const, message: 'Loading page...' },
    timeout: 15000,
  },
  component: {
    loadingProps: { size: 'medium' as const, message: 'Loading...' },
    timeout: 10000,
  },
  widget: {
    loadingProps: { size: 'small' as const, variant: 'dots' as const },
    timeout: 5000,
  },
  skeleton: {
    loadingProps: { variant: 'skeleton' as const },
    timeout: 8000,
  },
} as const;

export default LazyWrapper;
export { DefaultLoading, DefaultErrorFallback };