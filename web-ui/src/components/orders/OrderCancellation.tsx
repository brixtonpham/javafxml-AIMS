import React, { useState } from 'react';
import type { Order } from '../../types';
import { canCancelOrder } from '../../types/order';
import { Button } from '../ui';

interface OrderCancellationProps {
  order: Order;
  onCancel: (orderId: string, reason?: string) => Promise<void>;
  onClose: () => void;
  isLoading?: boolean;
}

const CANCELLATION_REASONS = [
  'Changed my mind',
  'Found a better price elsewhere',
  'Ordered by mistake',
  'Delivery time too long',
  'Payment issues',
  'Product no longer needed',
  'Other'
];

export const OrderCancellation: React.FC<OrderCancellationProps> = ({
  order,
  onCancel,
  onClose,
  isLoading = false
}) => {
  const [selectedReason, setSelectedReason] = useState('');
  const [customReason, setCustomReason] = useState('');
  const [showConfirmation, setShowConfirmation] = useState(false);

  const canCancel = canCancelOrder(order);

  if (!canCancel) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
        <div className="bg-white rounded-lg max-w-md w-full p-6">
          <div className="text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 mb-4">
              <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Cannot Cancel Order
            </h3>
            <p className="text-sm text-gray-500 mb-6">
              This order cannot be cancelled because it has already been approved and is being processed.
            </p>
            <Button onClick={onClose} className="w-full">
              Close
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedReason) return;
    
    const reason = selectedReason === 'Other' ? customReason : selectedReason;
    
    if (!showConfirmation) {
      setShowConfirmation(true);
      return;
    }

    try {
      await onCancel(order.id, reason);
      onClose();
    } catch (error) {
      console.error('Failed to cancel order:', error);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  if (showConfirmation) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
        <div className="bg-white rounded-lg max-w-md w-full p-6">
          <div className="text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-yellow-100 mb-4">
              <svg className="h-6 w-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Confirm Order Cancellation
            </h3>
            <div className="text-left bg-gray-50 rounded-lg p-4 mb-6">
              <p className="text-sm text-gray-600 mb-2">
                <strong>Order:</strong> #{order.id.slice(-8).toUpperCase()}
              </p>
              <p className="text-sm text-gray-600 mb-2">
                <strong>Amount:</strong> {formatCurrency(order.totalAmount)}
              </p>
              <p className="text-sm text-gray-600">
                <strong>Reason:</strong> {selectedReason === 'Other' ? customReason : selectedReason}
              </p>
            </div>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
              <p className="text-sm text-blue-800">
                <strong>Refund Information:</strong>
                <br />
                If you have already paid for this order, a full refund will be processed automatically within 3-5 business days.
              </p>
            </div>
            <div className="flex space-x-3">
              <Button
                variant="outline"
                onClick={() => setShowConfirmation(false)}
                disabled={isLoading}
                className="flex-1"
              >
                Back
              </Button>
              <Button
                variant="primary"
                onClick={handleSubmit}
                disabled={isLoading}
                className="flex-1 bg-red-600 hover:bg-red-700"
              >
                {isLoading ? 'Cancelling...' : 'Cancel Order'}
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-medium text-gray-900">
            Cancel Order
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </button>
        </div>

        <div className="mb-6">
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-600 mb-1">
              <strong>Order:</strong> #{order.id.slice(-8).toUpperCase()}
            </p>
            <p className="text-sm text-gray-600 mb-1">
              <strong>Total Amount:</strong> {formatCurrency(order.totalAmount)}
            </p>
            <p className="text-sm text-gray-600">
              <strong>Status:</strong> {order.status}
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Please select a reason for cancellation:
            </label>
            <div className="space-y-2">
              {CANCELLATION_REASONS.map((reason) => (
                <label key={reason} className="flex items-center">
                  <input
                    type="radio"
                    name="reason"
                    value={reason}
                    checked={selectedReason === reason}
                    onChange={(e) => setSelectedReason(e.target.value)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <span className="ml-2 text-sm text-gray-700">{reason}</span>
                </label>
              ))}
            </div>
          </div>

          {selectedReason === 'Other' && (
            <div>
              <label htmlFor="customReason" className="block text-sm font-medium text-gray-700 mb-2">
                Please specify your reason:
              </label>
              <textarea
                id="customReason"
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your reason for cancellation..."
                required
              />
            </div>
          )}

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex">
              <svg className="h-5 w-5 text-yellow-400 mr-2 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              <div>
                <h4 className="text-sm font-medium text-yellow-800">Important Notice</h4>
                <p className="text-sm text-yellow-700 mt-1">
                  Once cancelled, this order cannot be restored. If you have already paid, a refund will be processed automatically.
                </p>
              </div>
            </div>
          </div>

          <div className="flex space-x-3">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="flex-1"
            >
              Keep Order
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={!selectedReason || (selectedReason === 'Other' && !customReason.trim())}
              className="flex-1 bg-red-600 hover:bg-red-700"
            >
              Continue
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};