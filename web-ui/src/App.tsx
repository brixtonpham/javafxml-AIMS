import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AppLayout from './components/layout/AppLayout';
import { Button, Card } from './components/ui';
import { AuthProvider, LoginForm, ProtectedRoute, useAuth } from './components/auth';
import { CartProvider } from './contexts/CartContext';
import { OrderProvider } from './contexts/OrderContext';
import { PaymentProvider } from './contexts/PaymentContext';
import LazyWrapper, { LazyWrapperPresets } from './components/common/LazyWrapper';
import { createLazyComponent } from './utils/lazyLoading';
import { initializePerformanceMonitoring } from './utils/performance';
import ErrorBoundary from './components/ErrorBoundary';
import { ToastProvider, setGlobalToastContext, useToast } from './components/common/Toast';

// DEBUG: Test API calls
import './debug/testAPI';

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

// AIMS Shop Homepage - Clean shop-focused interface
const HomePage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();

  return (
    <AppLayout title="AIMS - Your Internet Media Store">
      <div className="space-y-8">
        {/* Hero Section */}
        <div className="bg-gradient-to-r from-blue-600 to-purple-700 text-white rounded-lg p-8 md:p-12">
          <div className="max-w-4xl mx-auto text-center">
            <h1 className="text-4xl md:text-5xl font-bold mb-4">
              Welcome to AIMS
            </h1>
            <p className="text-xl md:text-2xl mb-2 opacity-90">
              Your Internet Media Store
            </p>
            <p className="text-lg opacity-80 mb-6">
              Discover books, DVDs, and CDs from our extensive collection
            </p>
            {isAuthenticated && user ? (
              <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 mb-6">
                <p className="text-lg">
                  Welcome back, <span className="font-semibold">{user.fullName ?? user.username}</span>!
                </p>
              </div>
            ) : (
              <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 mb-6">
                <p className="text-lg">
                  Start shopping now - no account required!
                </p>
              </div>
            )}
            <div className="flex flex-wrap justify-center gap-4">
              <Button 
                variant="primary" 
                onClick={() => window.location.href = '/products'}
                className="bg-white text-blue-600 hover:bg-gray-100 px-8 py-3 text-lg font-semibold"
              >
                Browse All Products
              </Button>
              <Button 
                variant="outline" 
                onClick={() => window.location.href = '/cart'}
                className="border-white text-white hover:bg-white hover:text-blue-600 px-8 py-3 text-lg font-semibold"
              >
                View Cart
              </Button>
            </div>
          </div>
        </div>

        {/* Quick Access Section */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card className="text-center p-6 hover:shadow-lg transition-shadow">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">📚</span>
            </div>
            <h3 className="text-xl font-semibold mb-2">Books</h3>
            <p className="text-gray-600 mb-4">
              Explore our vast collection of books across all genres
            </p>
            <Button 
              variant="outline" 
              onClick={() => window.location.href = '/products?type=Book'}
            >
              Browse Books
            </Button>
          </Card>

          <Card className="text-center p-6 hover:shadow-lg transition-shadow">
            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">💿</span>
            </div>
            <h3 className="text-xl font-semibold mb-2">DVDs</h3>
            <p className="text-gray-600 mb-4">
              Discover movies and shows in our DVD collection
            </p>
            <Button 
              variant="outline" 
              onClick={() => window.location.href = '/products?type=DVD'}
            >
              Browse DVDs
            </Button>
          </Card>

          <Card className="text-center p-6 hover:shadow-lg transition-shadow">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">🎵</span>
            </div>
            <h3 className="text-xl font-semibold mb-2">CDs</h3>
            <p className="text-gray-600 mb-4">
              Listen to your favorite music with our CD selection
            </p>
            <Button 
              variant="outline" 
              onClick={() => window.location.href = '/products?type=CD'}
            >
              Browse CDs
            </Button>
          </Card>
        </div>

        {/* Features Section */}
        <Card>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 text-center">
            <div>
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">🚚</span>
              </div>
              <h4 className="font-semibold mb-2">Fast Delivery</h4>
              <p className="text-sm text-gray-600">Quick and reliable shipping across Vietnam</p>
            </div>
            <div>
              <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">💳</span>
              </div>
              <h4 className="font-semibold mb-2">Secure Payment</h4>
              <p className="text-sm text-gray-600">Safe transactions with VNPay integration</p>
            </div>
            <div>
              <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">🔍</span>
              </div>
              <h4 className="font-semibold mb-2">Easy Search</h4>
              <p className="text-sm text-gray-600">Find exactly what you're looking for</p>
            </div>
            <div>
              <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">🛒</span>
              </div>
              <h4 className="font-semibold mb-2">Simple Shopping</h4>
              <p className="text-sm text-gray-600">Intuitive cart and checkout experience</p>
            </div>
          </div>
        </Card>

        {/* Call to Action */}
        <div className="text-center bg-gray-50 rounded-lg p-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            Ready to Start Shopping?
          </h2>
          <p className="text-gray-600 mb-6">
            Browse our complete catalog of books, DVDs, and CDs
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Button 
              variant="primary" 
              onClick={() => window.location.href = '/products'}
              className="px-8 py-3 text-lg"
            >
              Shop Now
            </Button>
            {!isAuthenticated && (
              <Button 
                variant="outline" 
                onClick={() => window.location.href = '/login'}
                className="px-8 py-3 text-lg"
              >
                Sign In
              </Button>
            )}
          </div>
        </div>
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
