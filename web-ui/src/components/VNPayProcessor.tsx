import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePaymentContext } from '../contexts/PaymentContext';
import { vnpayService } from '../services/VNPayService';
import { VNPayValidation } from '../utils/vnpayValidation';
import Card from './ui/Card';
import Button from './ui/Button';
import type { Order } from '../types';

interface VNPayProcessorProps {
  orderId: string;
  paymentMethodId: string;
  orderData?: Order;
  onPaymentSuccess: (transactionId: string) => void;
  onPaymentFailure: (error: string) => void;
  onCancel?: () => void;
}

const VNPayProcessor: React.FC<VNPayProcessorProps> = ({
  orderId,
  paymentMethodId,
  orderData,
  onPaymentSuccess,
  onPaymentFailure,
  onCancel,
}) => {
  const navigate = useNavigate();
  const { initiatePayment, isProcessing, error, currentTransaction } = usePaymentContext();
  const [countdown, setCountdown] = useState(300); // 5 minutes timeout
  const [paymentUrl, setPaymentUrl] = useState<string | null>(null);
  const [isRedirecting, setIsRedirecting] = useState(false);

  // Initialize payment on component mount
  useEffect(() => {
    const initializePayment = async () => {
      try {
        await initiatePayment(orderId, paymentMethodId);
      } catch (error) {
        onPaymentFailure(error instanceof Error ? error.message : 'Failed to initialize payment');
      }
    };

    initializePayment();
  }, [orderId, paymentMethodId, initiatePayment, onPaymentFailure]);

  // Handle countdown timer
  useEffect(() => {
    if (countdown > 0 && !isRedirecting) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else if (countdown === 0) {
      onPaymentFailure('Payment timeout. Please try again.');
    }
  }, [countdown, isRedirecting, onPaymentFailure]);

  // Handle payment URL generation
  useEffect(() => {
    if (currentTransaction && !paymentUrl && !isProcessing) {
      generatePaymentUrl();
    }
  }, [currentTransaction, paymentUrl, isProcessing]);

  const generatePaymentUrl = async () => {
    try {
      if (!orderData) {
        throw new Error('Order data is required for payment processing');
      }

      const paymentRequest = await vnpayService.createPaymentRequest(
        orderId,
        orderData.totalAmount,
        `Payment for Order #${orderId} - ${orderData.items.length} items`,
        await VNPayValidation.getClientIpAddress()
      );

      setPaymentUrl(paymentRequest.paymentUrl);
      
      // Auto-redirect after 3 seconds
      setTimeout(() => {
        handleRedirectToPayment(paymentRequest.paymentUrl);
      }, 3000);

    } catch (error) {
      console.error('Error generating payment URL:', error);
      onPaymentFailure(error instanceof Error ? error.message : 'Failed to generate payment URL');
    }
  };

  const handleRedirectToPayment = (url: string) => {
    setIsRedirecting(true);
    window.location.href = url;
  };

  const handleManualRedirect = () => {
    if (paymentUrl) {
      handleRedirectToPayment(paymentUrl);
    }
  };

  const handleCancelPayment = async () => {
    try {
      if (currentTransaction) {
        await vnpayService.cancelPayment(currentTransaction.id);
      }
      if (onCancel) {
        onCancel();
      } else {
        navigate('/checkout');
      }
    } catch (error) {
      console.error('Error cancelling payment:', error);
      // Still allow navigation even if cancel fails
      if (onCancel) {
        onCancel();
      } else {
        navigate('/checkout');
      }
    }
  };

  const formatTime = (seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  // Show error state
  if (error) {
    return (
      <Card className="max-w-md mx-auto">
        <div className="p-6 text-center">
          <div className="w-16 h-16 mx-auto mb-4 text-red-500">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.962-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Payment Error</h3>
          <p className="text-gray-600 mb-4">{error}</p>
          <div className="flex space-x-3">
            <Button onClick={() => window.location.reload()} className="flex-1">
              Try Again
            </Button>
            <Button variant="outline" onClick={handleCancelPayment} className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Card>
    );
  }

  // Show loading state
  if (isProcessing || !paymentUrl) {
    return (
      <Card className="max-w-md mx-auto">
        <div className="p-6 text-center">
          <div className="w-16 h-16 mx-auto mb-4 text-blue-500">
            <svg className="animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v12m6-6H6" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Preparing Payment</h3>
          <p className="text-gray-600 mb-4">
            We're setting up your secure payment with VNPay. Please wait...
          </p>
          <div className="text-sm text-gray-500">
            Time remaining: {formatTime(countdown)}
          </div>
        </div>
      </Card>
    );
  }

  // Show redirect confirmation
  return (
    <Card className="max-w-md mx-auto">
      <div className="p-6 text-center">
        <div className="w-16 h-16 mx-auto mb-4 text-green-500">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        
        <h3 className="text-lg font-semibold text-gray-900 mb-2">Payment Ready</h3>
        <p className="text-gray-600 mb-4">
          Your payment is ready. You will be redirected to VNPay in a few seconds.
        </p>
        
        {orderData && (
          <div className="bg-gray-50 rounded-lg p-4 mb-4 text-left">
            <div className="text-sm space-y-2">
              <div className="flex justify-between">
                <span className="text-gray-600">Order ID:</span>
                <span className="font-medium">#{orderId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Amount:</span>
                <span className="font-medium">{orderData.totalAmount.toLocaleString('vi-VN')} VND</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Items:</span>
                <span className="font-medium">{orderData.items.length} items</span>
              </div>
            </div>
          </div>
        )}
        
        <div className="text-sm text-gray-500 mb-4">
          {isRedirecting ? (
            <span>Redirecting to VNPay...</span>
          ) : (
            <span>Auto-redirect in 3 seconds or click below</span>
          )}
        </div>
        
        <div className="flex space-x-3">
          <Button
            onClick={handleManualRedirect}
            className="flex-1"
            disabled={isRedirecting}
          >
            {isRedirecting ? 'Redirecting...' : 'Pay Now'}
          </Button>
          <Button
            variant="outline"
            onClick={handleCancelPayment}
            className="flex-1"
            disabled={isRedirecting}
          >
            Cancel
          </Button>
        </div>
        
        <div className="mt-4 text-xs text-gray-500">
          Secure payment powered by VNPay
        </div>
      </div>
    </Card>
  );
};

export default VNPayProcessor;