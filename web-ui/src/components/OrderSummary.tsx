import React, { useState, useEffect } from 'react';
import Card from './ui/Card';
import Button from './ui/Button';
import { useCartContext } from '../contexts/CartContext';
import { deliveryService } from '../services/DeliveryService';
import type {
  CheckoutDeliveryInfo,
  DeliveryOptions,
  DeliveryCalculationResult,
  OrderPreview
} from '../types/checkout';
import type { OrderItem } from '../types';
import { DELIVERY_BUSINESS_RULES } from '../types/checkout';

interface OrderSummaryProps {
  deliveryInfo: CheckoutDeliveryInfo;
  deliveryOptions: DeliveryOptions;
  calculationResult: DeliveryCalculationResult;
  onNext: () => void;
  onBack: () => void;
  onEdit?: (section: 'delivery' | 'options') => void;
}

const OrderSummary: React.FC<OrderSummaryProps> = ({
  deliveryInfo,
  deliveryOptions,
  calculationResult,
  onNext,
  onBack,
  onEdit
}) => {
  const { items, totalPrice, totalPriceWithVAT } = useCartContext();
  const [orderPreview, setOrderPreview] = useState<OrderPreview | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [termsAccepted, setTermsAccepted] = useState(false);

  useEffect(() => {
    generateOrderPreview();
  }, [items, deliveryInfo, deliveryOptions, calculationResult]);

  const generateOrderPreview = () => {
    if (!calculationResult || items.length === 0) return;

    // Convert cart items to OrderItem format
    const orderItems: OrderItem[] = items.map(item => ({
      productId: item.productId,
      productTitle: item.product.title,
      productType: item.product.productType,
      quantity: item.quantity,
      unitPrice: item.product.price,
      subtotal: item.subtotal,
      productMetadata: {
        author: item.product.author,
        artists: item.product.artists,
        director: item.product.director,
        category: item.product.category,
      }
    }));

    const subtotal = totalPrice; // Excluding VAT
    const vatAmount = totalPriceWithVAT - totalPrice;
    const shippingFee = Math.max(0, calculationResult.baseShippingFee - calculationResult.freeShippingDiscount);
    const rushDeliveryFee = calculationResult.rushDeliveryFee;
    const totalAmount = subtotal + vatAmount + shippingFee + rushDeliveryFee;

    const preview: OrderPreview = {
      items: orderItems,
      deliveryInfo,
      deliveryOptions,
      subtotal,
      vatAmount,
      shippingFee,
      rushDeliveryFee,
      freeShippingDiscount: calculationResult.freeShippingDiscount,
      totalAmount,
      estimatedDeliveryDays: calculationResult.estimatedDeliveryDays
    };

    setOrderPreview(preview);
  };

  const formatCurrency = (amount: number): string => {
    return amount.toLocaleString('vi-VN') + ' VND';
  };

  const formatDeliveryTime = (days: number): string => {
    if (days === 0.5) return 'Same day delivery';
    if (days === 1) return '1 business day';
    return `${days} business days`;
  };

  if (!orderPreview) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-gray-600">Preparing order summary...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Order Summary</h2>
        <p className="text-gray-600">
          Review your order details before proceeding to payment
        </p>
      </div>

      {/* Order Items */}
      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Items ({orderPreview.items.length})</h3>
          </div>
          <div className="space-y-4">
            {orderPreview.items.map((item, index) => (
              <div key={index} className="flex items-center space-x-4">
                <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center">
                  <span className="text-xs text-gray-500 font-medium">
                    {item.productType}
                  </span>
                </div>
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-medium text-gray-900 truncate">
                    {item.productTitle}
                  </h4>
                  <p className="text-sm text-gray-500">
                    {item.productMetadata?.category}
                    {item.productMetadata?.author && ` • ${item.productMetadata.author}`}
                    {item.productMetadata?.artists && ` • ${item.productMetadata.artists}`}
                    {item.productMetadata?.director && ` • ${item.productMetadata.director}`}
                  </p>
                  <p className="text-sm text-gray-500">
                    {formatCurrency(item.unitPrice)} × {item.quantity}
                  </p>
                </div>
                <div className="text-sm font-medium text-gray-900">
                  {formatCurrency(item.subtotal)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </Card>

      {/* Delivery Information */}
      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Delivery Information</h3>
            {onEdit && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onEdit('delivery')}
              >
                Edit
              </Button>
            )}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-600">Recipient:</p>
              <p className="font-medium text-gray-900">{deliveryInfo.recipientName}</p>
            </div>
            <div>
              <p className="text-gray-600">Phone:</p>
              <p className="font-medium text-gray-900">{deliveryInfo.phone}</p>
            </div>
            <div>
              <p className="text-gray-600">Email:</p>
              <p className="font-medium text-gray-900">{deliveryInfo.email}</p>
            </div>
            <div>
              <p className="text-gray-600">Address:</p>
              <p className="font-medium text-gray-900">
                {deliveryInfo.address}, {deliveryInfo.city}, {deliveryInfo.province}
                {deliveryInfo.postalCode && ` ${deliveryInfo.postalCode}`}
              </p>
            </div>
            {deliveryInfo.deliveryInstructions && (
              <div className="md:col-span-2">
                <p className="text-gray-600">Delivery Instructions:</p>
                <p className="font-medium text-gray-900">{deliveryInfo.deliveryInstructions}</p>
              </div>
            )}
          </div>
        </div>
      </Card>

      {/* Delivery Options */}
      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Delivery Options</h3>
            {onEdit && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onEdit('options')}
              >
                Edit
              </Button>
            )}
          </div>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Delivery type:</span>
              <span className="font-medium text-gray-900">
                {deliveryOptions.isRushOrder ? 'Rush Delivery' : 'Standard Delivery'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Estimated delivery:</span>
              <span className="font-medium text-gray-900">
                {formatDeliveryTime(orderPreview.estimatedDeliveryDays)}
              </span>
            </div>
            {deliveryOptions.preferredDeliveryTime && deliveryOptions.preferredDeliveryTime !== 'any' && (
              <div className="flex justify-between">
                <span className="text-gray-600">Preferred time:</span>
                <span className="font-medium text-gray-900 capitalize">
                  {deliveryOptions.preferredDeliveryTime}
                </span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-gray-600">Delivery zone:</span>
              <span className="font-medium text-gray-900">
                {calculationResult.breakdown.deliveryZone}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Total weight:</span>
              <span className="font-medium text-gray-900">
                {calculationResult.breakdown.totalWeight.toFixed(1)} kg
              </span>
            </div>
          </div>
        </div>
      </Card>

      {/* Detailed Price Breakdown */}
      <Card>
        <div className="p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Price Breakdown</h3>
          <div className="space-y-3">
            {/* Subtotal */}
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Subtotal ({orderPreview.items.length} items):</span>
              <span className="text-gray-900">{formatCurrency(orderPreview.subtotal)}</span>
            </div>

            {/* VAT */}
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">VAT ({(DELIVERY_BUSINESS_RULES.VAT_RATE * 100)}%):</span>
              <span className="text-gray-900">{formatCurrency(orderPreview.vatAmount)}</span>
            </div>

            {/* Shipping Fee Breakdown */}
            <div className="border-t pt-3">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-600">Shipping & Delivery:</span>
                <span className="text-gray-900"></span>
              </div>
              
              {/* Base shipping fee */}
              <div className="flex justify-between text-xs text-gray-500 ml-4">
                <span>Base shipping fee:</span>
                <span>{formatCurrency(calculationResult.baseShippingFee)}</span>
              </div>

              {/* Additional weight fee if applicable */}
              {calculationResult.breakdown.additionalWeightFee > 0 && (
                <div className="flex justify-between text-xs text-gray-500 ml-4">
                  <span>Additional weight fee:</span>
                  <span>{formatCurrency(calculationResult.breakdown.additionalWeightFee)}</span>
                </div>
              )}

              {/* Rush delivery fee */}
              {orderPreview.rushDeliveryFee > 0 && (
                <div className="flex justify-between text-xs text-gray-500 ml-4">
                  <span>Rush delivery fee:</span>
                  <span>{formatCurrency(orderPreview.rushDeliveryFee)}</span>
                </div>
              )}

              {/* Free shipping discount */}
              {orderPreview.freeShippingDiscount > 0 && (
                <div className="flex justify-between text-xs text-green-600 ml-4">
                  <span>Free shipping discount:</span>
                  <span>-{formatCurrency(orderPreview.freeShippingDiscount)}</span>
                </div>
              )}

              {/* Total shipping */}
              <div className="flex justify-between text-sm mt-2">
                <span className="text-gray-600">Total shipping:</span>
                <span className="text-gray-900">
                  {formatCurrency(orderPreview.shippingFee + orderPreview.rushDeliveryFee)}
                </span>
              </div>
            </div>

            {/* Total Amount */}
            <div className="border-t pt-3">
              <div className="flex justify-between text-lg font-semibold text-gray-900">
                <span>Total Amount:</span>
                <span>{formatCurrency(orderPreview.totalAmount)}</span>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Free Shipping Notice */}
      {orderPreview.freeShippingDiscount > 0 && (
        <Card>
          <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              <span className="text-sm font-medium text-green-800">
                Congratulations! You've qualified for free shipping and saved {formatCurrency(orderPreview.freeShippingDiscount)}
              </span>
            </div>
          </div>
        </Card>
      )}

      {/* Order Notes */}
      {deliveryOptions.deliveryInstructions && (
        <Card>
          <div className="p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-2">Delivery Instructions</h3>
            <p className="text-sm text-gray-600">{deliveryOptions.deliveryInstructions}</p>
          </div>
        </Card>
      )}

      {/* Terms and Conditions */}
      <Card>
        <div className="p-6">
          <div className="flex items-start">
            <input
              type="checkbox"
              id="terms"
              checked={termsAccepted}
              onChange={(e) => setTermsAccepted(e.target.checked)}
              className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              required
            />
            <label htmlFor="terms" className="ml-3 text-sm text-gray-600">
              I agree to the{' '}
              <a href="#" className="text-blue-600 hover:text-blue-500 underline">
                Terms and Conditions
              </a>{' '}
              and{' '}
              <a href="#" className="text-blue-600 hover:text-blue-500 underline">
                Privacy Policy
              </a>
            </label>
          </div>
        </div>
      </Card>

      {/* Navigation Buttons */}
      <div className="flex justify-between pt-6">
        <Button
          variant="outline"
          onClick={onBack}
          className="px-6"
        >
          Back to Delivery Options
        </Button>
        <Button
          onClick={onNext}
          disabled={isLoading || !termsAccepted}
          isLoading={isLoading}
          className="px-8"
        >
          Proceed to Payment
        </Button>
      </div>
    </div>
  );
};

export default OrderSummary;