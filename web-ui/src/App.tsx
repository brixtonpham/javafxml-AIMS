import React, { Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AppLayout from './components/layout/AppLayout';
import { Button, Card } from './components/ui';
import { AuthProvider, LoginForm, ProtectedRoute, useAuth } from './components/auth';
import { CartProvider } from './contexts/CartContext';
import { OrderProvider } from './contexts/OrderContext';
import { PaymentProvider } from './contexts/PaymentContext';
import LazyWrapper, { LazyWrapperPresets } from './components/common/LazyWrapper';
import { createLazyComponent, routePreloader } from './utils/lazyLoading';
import { initializePerformanceMonitoring } from './utils/performance';
import ErrorBoundary from './components/ErrorBoundary';
import { ToastProvider, setGlobalToastContext, useToast } from './components/common/Toast';

// Lazy load page components for better performance
const ProductListPage = createLazyComponent(() => import('./pages/ProductListPage').then(m => ({ default: m.default })));
const ProductDetailPage = createLazyComponent(() => import('./pages/ProductDetailPage').then(m => ({ default: m.default })));
const CartPage = createLazyComponent(() => import('./pages/CartPage').then(m => ({ default: m.default })));
const CheckoutPage = createLazyComponent(() => import('./pages/CheckoutPage').then(m => ({ default: m.default })));
const OrdersPage = createLazyComponent(() => import('./pages/OrdersPage').then(m => ({ default: m.OrdersPage })));
const OrderDetailPage = createLazyComponent(() => import('./pages/OrderDetailPage').then(m => ({ default: m.OrderDetailPage })));
const PaymentProcessing = createLazyComponent(() => import('./pages/PaymentProcessing').then(m => ({ default: m.default })));
const PaymentResult = createLazyComponent(() => import('./pages/PaymentResult').then(m => ({ default: m.default })));
const AdminDashboard = createLazyComponent(() => import('./pages/admin/AdminDashboard').then(m => ({ default: m.default })));

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Login page with redirect functionality
const LoginPage: React.FC = () => {
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const handleLoginSuccess = () => {
    window.location.href = from;
  };

  return (
    <AppLayout title="AIMS - Login" showHeader={false}>
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <LoginForm onSuccess={handleLoginSuccess} redirectTo={from} />
      </div>
    </AppLayout>
  );
};

// Protected home page that shows authentication status
const HomePage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();

  return (
    <AppLayout title="AIMS - Home">
      <div className="space-y-6">
        <Card>
          <h1 className="text-3xl font-bold text-gray-900 mb-4">
            Welcome to AIMS Web UI
          </h1>
          {isAuthenticated && user ? (
            <div className="mb-6">
              <p className="text-gray-600 mb-2">
                Welcome back, <span className="font-semibold text-blue-600">{user.fullName || user.username}</span>!
              </p>
              <p className="text-sm text-gray-500">
                Role: {user.roles.map(role => role.name).join(', ')}
              </p>
            </div>
          ) : (
            <p className="text-gray-600 mb-6">
              Please sign in to access all features of the AIMS application.
            </p>
          )}
          <p className="text-gray-600 mb-6">
            This is the new web-based interface for the AIMS (An Internet Media Store) application.
            The foundation has been set up with React 18, TypeScript, Tailwind CSS, and a complete
            service layer that interfaces with the existing backend.
          </p>
          <div className="flex flex-wrap gap-4">
            <Button variant="primary" onClick={() => window.location.href = '/products'}>
              Browse Products
            </Button>
            <Button variant="secondary" onClick={() => window.location.href = '/cart'}>
              View Cart
            </Button>
            <Button variant="outline" onClick={() => window.location.href = '/checkout'}>
              Checkout Demo
            </Button>
            {!isAuthenticated && (
              <Button variant="outline" onClick={() => window.location.href = '/login'}>
                Login
              </Button>
            )}
          </div>
<Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 7: VNPay Payment Integration ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ VNPayProcessor component with secure payment URL generation</li>
            <li>✅ PaymentResult component with transaction confirmation</li>
            <li>✅ RefundService for automatic refund handling</li>
            <li>✅ PaymentContext for payment state management</li>
            <li>✅ VNPay sandbox integration with HMAC security validation</li>
            <li>✅ Payment processing flow with error handling</li>
            <li>✅ Email notifications for payment confirmations</li>
            <li>✅ Mobile-responsive payment interfaces</li>
          </ul>
        </Card>
        </Card>
<Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 5: Order Management System ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ OrdersPage with comprehensive order history and filtering</li>
            <li>✅ OrderDetailPage with timeline, tracking, and cancellation</li>
            <li>✅ Order status management with state machine implementation</li>
            <li>✅ OrderContext for centralized order state management</li>
            <li>✅ Order cancellation with business rule validation</li>
            <li>✅ Invoice generation and print functionality</li>
            <li>✅ Mobile-responsive order management interface</li>
          </ul>
        </Card>
        
        <Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 4: Complete Checkout Flow ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ Multi-step checkout wizard with progress tracking</li>
            <li>✅ DeliveryInfoForm with Vietnam location data and validation</li>
            <li>✅ DeliveryOptionsSelector with rush delivery support</li>
            <li>✅ Complete OrderSummary with price breakdown and terms acceptance</li>
            <li>✅ Real-time delivery fee calculation using AIMS business rules</li>
            <li>✅ Form state persistence across checkout steps</li>
            <li>✅ Mobile-responsive checkout experience</li>
          </ul>
        </Card>

        <Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 3: Shopping Cart & Product Details Complete ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ ProductDetailPage with type-specific specifications and image gallery</li>
            <li>✅ Enhanced shopping cart system with real-time inventory validation</li>
            <li>✅ CartContext for global state management with localStorage persistence</li>
            <li>✅ Advanced cart components (QuantitySelector, AddToCartButton)</li>
            <li>✅ 10% VAT calculations and comprehensive price breakdown</li>
            <li>✅ Mobile-responsive cart and product detail interfaces</li>
          </ul>
        </Card>

        <Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 2.2: Product Browsing System Complete ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ Advanced product search with real-time filtering</li>
            <li>✅ Multi-criteria filtering (type, category, price range)</li>
            <li>✅ URL-based filter state persistence</li>
            <li>✅ Responsive masonry grid layout with infinite scroll</li>
            <li>✅ Smart pagination with load-more functionality</li>
            <li>✅ Mobile-optimized filter panels and product cards</li>
          </ul>
        </Card>

        <Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 2.1: Authentication System Complete ✅
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ JWT-based authentication with React Context</li>
            <li>✅ LoginForm component with validation and error handling</li>
            <li>✅ AuthProvider for global authentication state management</li>
            <li>✅ ProtectedRoute wrapper for route-based access control</li>
            <li>✅ UserMenu component for authenticated user navigation</li>
            <li>✅ Automatic token refresh and secure logout functionality</li>
          </ul>
        </Card>

        <Card>
          <h2 className="text-xl font-semibold text-gray-900 mb-3">
            Phase 1: Foundation Complete
          </h2>
          <ul className="space-y-2 text-gray-600">
            <li>✅ React 18 + TypeScript + Vite setup</li>
            <li>✅ Tailwind CSS with custom design system</li>
            <li>✅ Complete service layer (Products, Cart, Orders, Auth, Users, Payments)</li>
            <li>✅ REST API controllers in Java backend</li>
            <li>✅ Base UI component library</li>
            <li>✅ React Router setup with protected routes</li>
          </ul>
        </Card>
      </div>
    </AppLayout>
  );
};


// Product Manager Dashboard placeholder
const ProductManagerDashboard: React.FC = () => (
  <AppLayout title="AIMS - Product Manager Dashboard">
    <Card>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Product Manager Dashboard</h1>
      <p className="text-gray-600">
        Product management features will be implemented in future phases.
      </p>
    </Card>
  </AppLayout>
);

// App Routes Component (wrapped with AuthProvider)
const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<LoginPage />} />
      
      {/* Protected Routes */}
      <Route 
        path="/" 
        element={
          <ProtectedRoute requireAuth={false}>
            <HomePage />
          </ProtectedRoute>
        } 
      />
      <Route path="/home" element={<Navigate to="/" replace />} />
      
      {/* Customer Routes */}
      <Route
        path="/products"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <ProductListPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/products/search"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <ProductListPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/cart"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <CartPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/checkout"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <CheckoutPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/products/:id"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <ProductDetailPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders"
        element={
          <ProtectedRoute>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <OrdersPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders/:orderId"
        element={
          <ProtectedRoute>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <OrderDetailPage />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      
      {/* Payment Routes */}
      <Route
        path="/payment/processing"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <PaymentProcessing />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      <Route
        path="/payment/result"
        element={
          <ProtectedRoute requireAuth={false}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <PaymentResult />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      
      {/* Admin Routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute requiredRoles={['ADMIN']}>
            <LazyWrapper {...LazyWrapperPresets.page}>
              <AdminDashboard />
            </LazyWrapper>
          </ProtectedRoute>
        }
      />
      
      {/* Product Manager Routes */}
      <Route 
        path="/pm" 
        element={
          <ProtectedRoute requiredRoles={['PRODUCT_MANAGER', 'ADMIN']}>
            <ProductManagerDashboard />
          </ProtectedRoute>
        } 
      />
      
      {/* Catch all route */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

// Component to initialize global toast context
const ToastInitializer: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const toastContext = useToast();
  
  React.useEffect(() => {
    setGlobalToastContext(toastContext);
  }, [toastContext]);
  
  return <>{children}</>;
};

const App: React.FC = () => {
  // Initialize performance monitoring
  React.useEffect(() => {
    initializePerformanceMonitoring();
  }, []);

  return (
    <ErrorBoundary
      showDetails={process.env.NODE_ENV === 'development'}
      recovery={{ enabled: true, maxRetries: 3, resetTimeoutMs: 5000 }}
      onError={(error, errorInfo, errorId) => {
        // Report to analytics/monitoring service
        console.error('App Error Boundary caught error:', { error, errorInfo, errorId });
      }}
    >
      <QueryClientProvider client={queryClient}>
        <ToastProvider maxNotifications={5} defaultDuration={5000}>
          <ToastInitializer>
            <AuthProvider>
              <CartProvider>
                <OrderProvider>
                  <PaymentProvider>
                    <Router>
                      <ErrorBoundary
                        showDetails={false}
                        recovery={{ enabled: true, maxRetries: 1 }}
                        fallback={
                          <div className="min-h-screen flex items-center justify-center bg-gray-50">
                            <Card className="max-w-md w-full text-center">
                              <h2 className="text-xl font-semibold text-gray-900 mb-2">
                                Navigation Error
                              </h2>
                              <p className="text-gray-600 mb-4">
                                There was an error loading the page.
                              </p>
                              <Button
                                variant="primary"
                                onClick={() => window.location.reload()}
                              >
                                Reload Page
                              </Button>
                            </Card>
                          </div>
                        }
                      >
                        <div className="App">
                          <AppRoutes />
                        </div>
                      </ErrorBoundary>
                    </Router>
                  </PaymentProvider>
                </OrderProvider>
              </CartProvider>
            </AuthProvider>
          </ToastInitializer>
        </ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

export default App;
