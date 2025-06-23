import React from 'react';
import { motion } from 'framer-motion';
import { InformationCircleIcon, TruckIcon, CalculatorIcon } from '@heroicons/react/24/outline';
import { Card, Button } from '../ui';
import type { Cart, StockWarning } from '../../types';

interface CartSummaryProps {
  cart: Cart | null;
  stockWarnings?: StockWarning[];
  showShipping?: boolean;
  onCheckout?: () => void;
  isLoading?: boolean;
  className?: string;
}

const CartSummary: React.FC<CartSummaryProps> = ({
  cart,
  stockWarnings = [],
  showShipping = true,
  onCheckout,
  isLoading = false,
  className = ''
}) => {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  if (!cart || cart.items.length === 0) {
    return (
      <Card className={`p-6 ${className}`}>
        <div className="text-center text-gray-500">
          <div className="text-4xl mb-2">🛒</div>
          <p>Your cart is empty</p>
        </div>
      </Card>
    );
  }

  const subtotal = cart.totalPrice; // Base price without VAT
  const vatRate = 0.1; // 10% VAT as per business rule
  const vatAmount = subtotal * vatRate;
  const shippingFee = showShipping ? 50000 : 0; // Flat shipping fee
  const totalWithVATAndShipping = subtotal + vatAmount + shippingFee;

  // Calculate savings vs individual product prices (which include VAT)
  const individualTotal = cart.items.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  const totalSavings = individualTotal - totalWithVATAndShipping;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={className}
    >
      <Card className="p-6">
        <div className="flex items-center mb-4">
          <CalculatorIcon className="w-5 h-5 text-gray-600 mr-2" />
          <h2 className="text-lg font-semibold text-gray-900">Order Summary</h2>
        </div>

        {/* Stock Warnings */}
        {stockWarnings.length > 0 && (
          <div className="mb-4 p-3 bg-orange-50 border border-orange-200 rounded-lg">
            <div className="flex items-start">
              <InformationCircleIcon className="w-5 h-5 text-orange-500 mt-0.5 mr-2 flex-shrink-0" />
              <div>
                <h4 className="text-sm font-medium text-orange-800 mb-1">Stock Warnings</h4>
                <ul className="text-xs text-orange-700 space-y-1">
                  {stockWarnings.map((warning, index) => (
                    <li key={index}>{warning.message}</li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Order breakdown */}
        <div className="space-y-3 mb-4">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Items ({cart.totalItems}):</span>
            <span>{formatPrice(subtotal)}</span>
          </div>
          
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">VAT (10%):</span>
            <span>{formatPrice(vatAmount)}</span>
          </div>

          {showShipping && (
            <div className="flex justify-between text-sm">
              <div className="flex items-center">
                <TruckIcon className="w-4 h-4 mr-1 text-gray-500" />
                <span className="text-gray-600">Shipping:</span>
              </div>
              <span>{formatPrice(shippingFee)}</span>
            </div>
          )}

          {totalSavings > 0 && (
            <div className="flex justify-between text-sm text-green-600">
              <span>Savings:</span>
              <span>-{formatPrice(totalSavings)}</span>
            </div>
          )}

          <div className="border-t pt-3">
            <div className="flex justify-between font-semibold text-lg">
              <span>Total:</span>
              <span className="text-primary-600">{formatPrice(totalWithVATAndShipping)}</span>
            </div>
          </div>
        </div>

        {/* Pricing breakdown info */}
        <div className="text-xs text-gray-500 mb-4 p-2 bg-gray-50 rounded">
          <p className="flex items-start">
            <InformationCircleIcon className="w-3 h-3 mt-0.5 mr-1 flex-shrink-0" />
            All prices shown include applicable taxes. VAT is calculated on the base product value.
          </p>
        </div>

        {/* Checkout button */}
        {onCheckout && (
          <Button
            fullWidth
            size="lg"
            onClick={onCheckout}
            isLoading={isLoading}
            disabled={stockWarnings.length > 0 || isLoading}
            className="font-semibold"
          >
            {stockWarnings.length > 0 ? 'Resolve Stock Issues' : 'Proceed to Checkout'}
          </Button>
        )}

        {/* Estimated delivery */}
        {showShipping && (
          <div className="mt-3 text-xs text-gray-500 text-center">
            <TruckIcon className="w-3 h-3 inline mr-1" />
            Estimated delivery: 2-3 business days
          </div>
        )}
      </Card>
    </motion.div>
  );
};

export default CartSummary;