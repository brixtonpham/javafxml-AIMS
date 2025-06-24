import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import Card from '../ui/Card';
import Button from '../ui/Button';
import { orderService, type PlaceOrderRequest } from '../../services/orderService';
import { useCartContext } from '../../contexts/CartContext';
import type { DeliveryInfo, PaymentMethod } from '../../types';
import type { DeliveryOptions, DeliveryCalculationResult } from '../../types/checkout';

interface OrderPlacementProps {
  deliveryInfo: DeliveryInfo;
  deliveryOptions: DeliveryOptions;
  calculationResult: DeliveryCalculationResult;
  paymentMethod?: PaymentMethod;
  onBack: () => void;
  onOrderPlaced: (orderId: string) => void;
}

const OrderPlacement: React.FC<OrderPlacementProps> = ({
  deliveryInfo,
  deliveryOptions,
  calculationResult,
  paymentMethod,
  onBack,
  onOrderPlaced
}) => {
  const { cart, clearCart } = useCartContext();
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const [orderNotes, setOrderNotes] = useState('');

  const placeOrderMutation = useMutation({
    mutationFn: async (orderData: PlaceOrderRequest) => {
      const order = await orderService.placeOrder(orderData);
      return order;
    },
    onSuccess: async (order) => {
      // Clear the cart after successful order placement
      try {
        await clearCart();
      } catch (error) {
        console.warn('Failed to clear cart after order placement:', error);
      }
      
      // Clear checkout session storage
      sessionStorage.removeItem('checkout_current_step');
      sessionStorage.removeItem('checkout_form_data');
      sessionStorage.removeItem('checkout_calculation_result');
      
      onOrderPlaced(order.id);
    },
    onError: (error) => {
      console.error('Failed to place order:', error);
    }
  });

  const handlePlaceOrder = () => {
    if (!cart?.sessionId) {
      console.error('No cart session found');
      return;
    }

    if (!agreedToTerms) {
      alert('Please agree to the terms and conditions');
      return;
    }

    const orderData: PlaceOrderRequest = {
      cartSessionId: cart.sessionId,
      deliveryInfo,
      isRushOrder: deliveryOptions.isRushOrder,
      deliveryInstructions: deliveryOptions.deliveryInstructions ?? orderNotes,
      paymentMethodId: paymentMethod?.id
    };

    placeOrderMutation.mutate(orderData);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  // Calculate totals from cart and delivery calculation
  const subtotal = cart?.totalPrice ?? 0;
  const vatAmount = cart?.totalPriceWithVAT ? (cart.totalPriceWithVAT - cart.totalPrice) : 0;
  const shippingFee = calculationResult.totalShippingFee;
  const rushFee = deliveryOptions.isRushOrder ? calculationResult.rushDeliveryFee : 0;
  const totalAmount = subtotal + vatAmount + shippingFee;

  const isLoading = placeOrderMutation.isPending;

  return (
    <div className="space-y-6">
      {/* Order Summary Card */}
      <Card>
        <div className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Final Order Summary
          </h3>
          
          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span>Subtotal ({cart?.totalItems} items)</span>
              <span>{formatCurrency(subtotal)}</span>
            </div>
            
            <div className="flex justify-between text-sm">
              <span>VAT (10%)</span>
              <span>{formatCurrency(vatAmount)}</span>
            </div>
            
            <div className="flex justify-between text-sm">
              <span>Shipping Fee</span>
              <span>{formatCurrency(shippingFee)}</span>
            </div>
            
            {deliveryOptions.isRushOrder && rushFee > 0 && (
              <div className="flex justify-between text-sm text-orange-600">
                <span>Rush Delivery Fee</span>
                <span>{formatCurrency(rushFee)}</span>
              </div>
            )}
            
            <div className="border-t pt-3">
              <div className="flex justify-between font-semibold text-lg">
                <span>Total Amount</span>
                <span className="text-blue-600">{formatCurrency(totalAmount)}</span>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Delivery Information Summary */}
      <Card>
        <div className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Delivery Information
          </h3>
          
          <div className="space-y-2 text-sm">
            <div>
              <span className="font-medium">Recipient:</span> {deliveryInfo.recipientName}
            </div>
            <div>
              <span className="font-medium">Phone:</span> {deliveryInfo.phone}
            </div>
            <div>
              <span className="font-medium">Email:</span> {deliveryInfo.email}
            </div>
            <div>
              <span className="font-medium">Address:</span> {deliveryInfo.address}, {deliveryInfo.city}, {deliveryInfo.province}
            </div>
            {deliveryInfo.postalCode && (
              <div>
                <span className="font-medium">Postal Code:</span> {deliveryInfo.postalCode}
              </div>
            )}
            {deliveryOptions.isRushOrder && (
              <div className="text-orange-600 font-medium">
                ⚡ Rush Delivery Requested
              </div>
            )}
          </div>
        </div>
      </Card>

      {/* Order Notes */}
      <Card>
        <div className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Order Notes (Optional)
          </h3>
          
          <textarea
            value={orderNotes}
            onChange={(e) => setOrderNotes(e.target.value)}
            placeholder="Add any special instructions for your order..."
            className="w-full p-3 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            rows={3}
            maxLength={500}
          />
          <div className="text-xs text-gray-500 mt-1">
            {orderNotes.length}/500 characters
          </div>
        </div>
      </Card>

      {/* Terms and Conditions */}
      <Card>
        <div className="p-6">
          <div className="flex items-start space-x-3">
            <input
              type="checkbox"
              id="terms"
              checked={agreedToTerms}
              onChange={(e) => setAgreedToTerms(e.target.checked)}
              className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="terms" className="text-sm text-gray-700">
              I agree to the{' '}
              <a 
                href="/terms" 
                target="_blank" 
                className="text-blue-600 hover:text-blue-500 underline"
              >
                Terms and Conditions
              </a>
              {' '}and{' '}
              <a 
                href="/privacy" 
                target="_blank" 
                className="text-blue-600 hover:text-blue-500 underline"
              >
                Privacy Policy
              </a>
              . I understand that this order will be processed and charged to my selected payment method.
            </label>
          </div>
        </div>
      </Card>

      {/* Payment Method */}
      {paymentMethod && (
        <Card>
          <div className="p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Payment Method
            </h3>
            <div className="flex items-center space-x-3">
              <div className="w-10 h-6 bg-blue-100 rounded flex items-center justify-center">
                <span className="text-xs font-medium text-blue-600">
                  {paymentMethod.type}
                </span>
              </div>
              <span className="text-sm">{paymentMethod.name}</span>
            </div>
          </div>
        </Card>
      )}

      {/* Error Display */}
      {placeOrderMutation.error && (
        <Card>
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">
                  Failed to place order
                </h3>
                <div className="mt-2 text-sm text-red-700">
                  {placeOrderMutation.error.message || 'An unexpected error occurred. Please try again.'}
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Action Buttons */}
      <div className="flex items-center justify-between pt-6">
        <Button
          variant="outline"
          onClick={onBack}
          disabled={isLoading}
        >
          Back to Review
        </Button>
        
        <Button
          onClick={handlePlaceOrder}
          disabled={!agreedToTerms || isLoading}
          className="bg-green-600 hover:bg-green-700 text-white px-8 py-3 font-semibold"
        >
          {isLoading ? (
            <div className="flex items-center space-x-2">
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              <span>Placing Order...</span>
            </div>
          ) : (
            <>
              Place Order • {formatCurrency(totalAmount)}
            </>
          )}
        </Button>
      </div>
    </div>
  );
};

export default OrderPlacement;
