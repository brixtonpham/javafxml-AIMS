import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  ShoppingCartIcon, 
  ArrowLeftIcon, 
  TrashIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  HeartIcon
} from '@heroicons/react/24/outline';
import AppLayout from '../components/layout/AppLayout';
import { Card, Button } from '../components/ui';
import CartItem from '../components/cart/CartItem';
import CartSummary from '../components/cart/CartSummary';
import { useCartContext } from '../contexts/CartContext';

const CartPage: React.FC = () => {
  const navigate = useNavigate();
  const { 
    cart, 
    items, 
    totalItems, 
    stockWarnings, 
    clearCart, 
    isClearingCart,
    hasStockWarnings 
  } = useCartContext();
  
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [favoriteItems, setFavoriteItems] = useState<Set<string>>(new Set());

  const handleContinueShopping = () => {
    navigate('/products');
  };

  const handleProceedToCheckout = () => {
    if (hasStockWarnings()) {
      // Show warning about stock issues
      return;
    }
    navigate('/checkout');
  };

  const handleClearCart = async () => {
    if (!showClearConfirm) {
      setShowClearConfirm(true);
      return;
    }

    try {
      await clearCart();
      setShowClearConfirm(false);
    } catch (error) {
      console.error('Failed to clear cart:', error);
    }
  };

  const handleToggleFavorite = (productId: string) => {
    setFavoriteItems(prev => {
      const newSet = new Set(prev);
      if (newSet.has(productId)) {
        newSet.delete(productId);
      } else {
        newSet.add(productId);
      }
      return newSet;
    });
  };

  const isEmpty = !items || items.length === 0;

  if (isEmpty) {
    return (
      <AppLayout title="Shopping Cart - AIMS">
        <div className="max-w-4xl mx-auto">
          {/* Empty Cart State */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center py-16"
          >
            <Card className="p-12">
              <div className="text-6xl mb-6">🛒</div>
              <h1 className="text-2xl font-bold text-gray-900 mb-4">
                Your cart is empty
              </h1>
              <p className="text-gray-600 mb-8 max-w-md mx-auto">
                Looks like you haven't added any items to your cart yet. 
                Start shopping to find great products!
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button 
                  size="lg" 
                  onClick={handleContinueShopping}
                  className="flex items-center justify-center"
                >
                  <ShoppingCartIcon className="w-5 h-5 mr-2" />
                  Start Shopping
                </Button>
                <Button 
                  variant="outline" 
                  size="lg"
                  onClick={() => navigate('/')}
                >
                  Go Home
                </Button>
              </div>
            </Card>
          </motion.div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout title="Shopping Cart - AIMS">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate(-1)}
            >
              <ArrowLeftIcon className="w-4 h-4 mr-2" />
              Back
            </Button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Shopping Cart</h1>
              <p className="text-gray-600">
                {totalItems} {totalItems === 1 ? 'item' : 'items'} in your cart
              </p>
            </div>
          </div>

          {/* Clear Cart */}
          {!isEmpty && (
            <div className="flex items-center space-x-2">
              <AnimatePresence>
                {showClearConfirm ? (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.8 }}
                    className="flex items-center space-x-2"
                  >
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setShowClearConfirm(false)}
                    >
                      Cancel
                    </Button>
                    <Button
                      size="sm"
                      variant="primary"
                      onClick={handleClearCart}
                      isLoading={isClearingCart}
                      className="bg-red-600 hover:bg-red-700 border-red-600"
                    >
                      Clear All Items
                    </Button>
                  </motion.div>
                ) : (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleClearCart}
                    className="text-red-600 border-red-200 hover:bg-red-50"
                  >
                    <TrashIcon className="w-4 h-4 mr-1" />
                    Clear Cart
                  </Button>
                )}
              </AnimatePresence>
            </div>
          )}
        </div>

        {/* Stock Warnings */}
        {hasStockWarnings() && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-6"
          >
            <Card className="p-4 border-orange-200 bg-orange-50">
              <div className="flex items-start">
                <ExclamationTriangleIcon className="w-5 h-5 text-orange-500 mt-0.5 mr-3 flex-shrink-0" />
                <div>
                  <h3 className="font-medium text-orange-800 mb-1">
                    Stock Issues Detected
                  </h3>
                  <p className="text-sm text-orange-700 mb-2">
                    Some items in your cart have stock limitations. Please review and adjust quantities.
                  </p>
                  <ul className="text-sm text-orange-700 space-y-1">
                    {stockWarnings?.map((warning, index) => (
                      <li key={index}>• {warning.message}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-2">
            <Card className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-gray-900">
                  Cart Items ({totalItems})
                </h2>
                {favoriteItems.size > 0 && (
                  <div className="flex items-center text-sm text-gray-500">
                    <HeartIcon className="w-4 h-4 mr-1 text-red-500" />
                    {favoriteItems.size} favorited
                  </div>
                )}
              </div>

              <AnimatePresence>
                <div className="space-y-0">
                  {items.map((item, index) => (
                    <CartItem
                      key={item.productId}
                      item={item}
                      showDivider={index < items.length - 1}
                      onToggleFavorite={handleToggleFavorite}
                      isFavorite={favoriteItems.has(item.productId)}
                    />
                  ))}
                </div>
              </AnimatePresence>

              {/* Continue Shopping */}
              <div className="mt-6 pt-6 border-t border-gray-200">
                <Button
                  variant="outline"
                  onClick={handleContinueShopping}
                  className="w-full sm:w-auto"
                >
                  <ArrowLeftIcon className="w-4 h-4 mr-2" />
                  Continue Shopping
                </Button>
              </div>
            </Card>
          </div>

          {/* Cart Summary */}
          <div className="lg:col-span-1">
            <div className="sticky top-6">
              <CartSummary
                cart={cart}
                stockWarnings={stockWarnings}
                onCheckout={handleProceedToCheckout}
                isLoading={false}
              />

              {/* Additional Information */}
              <Card className="p-4 mt-4">
                <h3 className="font-medium text-gray-900 mb-3">
                  Secure Checkout
                </h3>
                <div className="space-y-2 text-sm text-gray-600">
                  <div className="flex items-center">
                    <CheckCircleIcon className="w-4 h-4 text-green-500 mr-2" />
                    SSL encrypted checkout
                  </div>
                  <div className="flex items-center">
                    <CheckCircleIcon className="w-4 h-4 text-green-500 mr-2" />
                    Money-back guarantee
                  </div>
                  <div className="flex items-center">
                    <CheckCircleIcon className="w-4 h-4 text-green-500 mr-2" />
                    24/7 customer support
                  </div>
                </div>
              </Card>

              {/* Recommended Products */}
              <Card className="p-4 mt-4">
                <h3 className="font-medium text-gray-900 mb-3">
                  You might also like
                </h3>
                <div className="space-y-3">
                  {/* Placeholder for recommended products */}
                  {[1, 2].map((i) => (
                    <div key={i} className="flex items-center space-x-3">
                      <div className="w-12 h-12 bg-gray-200 rounded flex-shrink-0"></div>
                      <div className="flex-1 min-w-0">
                        <div className="h-3 bg-gray-200 rounded mb-1"></div>
                        <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          </div>
        </div>

        {/* Mobile Checkout Footer */}
        <div className="lg:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-10">
          <div className="flex items-center justify-between mb-2">
            <span className="font-medium text-gray-900">Total:</span>
            <span className="text-lg font-bold text-primary-600">
              {cart?.totalPriceWithVAT ? 
                new Intl.NumberFormat('vi-VN', {
                  style: 'currency',
                  currency: 'VND',
                }).format(cart.totalPriceWithVAT) : 
                '0 VND'
              }
            </span>
          </div>
          <Button
            fullWidth
            size="lg"
            onClick={handleProceedToCheckout}
            disabled={hasStockWarnings()}
          >
            Proceed to Checkout
          </Button>
        </div>

        {/* Add bottom padding for mobile footer */}
        <div className="lg:hidden h-32"></div>
      </div>
    </AppLayout>
  );
};

export default CartPage;