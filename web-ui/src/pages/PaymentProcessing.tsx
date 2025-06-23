import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { usePaymentContext } from '../contexts/PaymentContext';
import { useOrderContext } from '../contexts/OrderContext';
import VNPayProcessor from '../components/VNPayProcessor';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import AppLayout from '../components/layout/AppLayout';
import type { Order } from '../types';

interface PaymentProcessingLocationState {
  orderId: string;
  paymentMethodId: string;
  orderData?: Order;
  deliveryInfo?: any;
  deliveryOptions?: any;
  calculationResult?: any;
  items?: any[];
}

const PaymentProcessing: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { clearPaymentState, error } = usePaymentContext();
  const { refreshOrder } = useOrderContext();
  
  const [orderData, setOrderData] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [processingError, setProcessingError] = useState<string | null>(null);

  // Get state from navigation
  const state = location.state as PaymentProcessingLocationState | null;

  useEffect(() => {
    // Validate required state
    if (!state || !state.orderId || !state.paymentMethodId) {
      setProcessingError('Missing payment information. Please start checkout again.');
      return;
    }

    // Load order data if not provided
    const loadOrderData = async () => {
      try {
        if (state.orderData) {
          setOrderData(state.orderData);
        } else {
          // Fetch order data from API
          const response = await fetch(`/api/orders/${state.orderId}`);
          if (!response.ok) {
            throw new Error('Failed to load order data');
          }
          const order = await response.json();
          setOrderData(order);
        }
      } catch (error) {
        console.error('Error loading order data:', error);
        setProcessingError('Failed to load order information. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };

    loadOrderData();
  }, [state]);

  // Clear payment state when component unmounts
  useEffect(() => {
    return () => {
      clearPaymentState();
    };
  }, [clearPaymentState]);

  const handlePaymentSuccess = async (transactionId: string) => {
    try {
      // Refresh order data
      if (state?.orderId) {
        await refreshOrder(state.orderId);
      }

      // Navigate to success page
      navigate('/payment/result', {
        replace: true,
        state: {
          success: true,
          transactionId,
          orderId: state?.orderId
        }
      });
    } catch (error) {
      console.error('Error handling payment success:', error);
      // Still navigate to result page, but let it handle the error
      navigate('/payment/result', {
        replace: true,
        state: {
          success: true,
          transactionId,
          orderId: state?.orderId
        }
      });
    }
  };

  const handlePaymentFailure = (errorMessage: string) => {
    console.error('Payment failed:', errorMessage);
    
    // Navigate to result page with error
    navigate('/payment/result', {
      replace: true,
      state: {
        success: false,
        error: errorMessage,
        orderId: state?.orderId
      }
    });
  };

  const handleCancel = () => {
    // Navigate back to checkout
    navigate('/checkout', {
      state: {
        orderId: state?.orderId,
        deliveryInfo: state?.deliveryInfo,
        deliveryOptions: state?.deliveryOptions,
        calculationResult: state?.calculationResult,
        items: state?.items
      }
    });
  };

  const handleRetryPayment = () => {
    // Reload the page to retry payment
    window.location.reload();
  };

  const handleBackToCheckout = () => {
    navigate('/checkout');
  };

  // Show error state if there's a processing error
  if (processingError) {
    return (
      <AppLayout title="Payment Error">
        <div className="min-h-screen flex items-center justify-center">
          <Card className="max-w-md mx-auto">
            <div className="p-8 text-center">
              <div className="w-16 h-16 mx-auto mb-4 text-red-500">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.962-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Payment Setup Error</h3>
              <p className="text-gray-600 mb-4">{processingError}</p>
              <div className="flex space-x-3">
                <Button onClick={handleRetryPayment} className="flex-1">
                  Try Again
                </Button>
                <Button variant="outline" onClick={handleBackToCheckout} className="flex-1">
                  Back to Checkout
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </AppLayout>
    );
  }

  // Show loading state
  if (isLoading || !orderData) {
    return (
      <AppLayout title="Setting Up Payment">
        <div className="min-h-screen flex items-center justify-center">
          <Card className="max-w-md mx-auto">
            <div className="p-8 text-center">
              <div className="w-16 h-16 mx-auto mb-4 text-blue-500">
                <svg className="animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v12m6-6H6" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Setting Up Payment</h3>
              <p className="text-gray-600">
                Please wait while we prepare your payment...
              </p>
            </div>
          </Card>
        </div>
      </AppLayout>
    );
  }

  // Main payment processing view
  return (
    <AppLayout title="Payment Processing">
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Complete Your Payment</h1>
            <p className="text-gray-600">
              Secure payment processing with VNPay
            </p>
          </div>

          {/* Payment Progress */}
          <div className="mb-8">
            <div className="flex items-center justify-center space-x-4">
              <div className="flex items-center">
                <div className="w-8 h-8 bg-green-500 text-white rounded-full flex items-center justify-center">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                </div>
                <span className="ml-2 text-sm font-medium text-green-600">Order Created</span>
              </div>
              
              <div className="w-16 h-0.5 bg-green-500"></div>
              
              <div className="flex items-center">
                <div className="w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center">
                  <span className="text-sm font-medium">2</span>
                </div>
                <span className="ml-2 text-sm font-medium text-blue-600">Payment</span>
              </div>
              
              <div className="w-16 h-0.5 bg-gray-200"></div>
              
              <div className="flex items-center">
                <div className="w-8 h-8 bg-gray-200 text-gray-500 rounded-full flex items-center justify-center">
                  <span className="text-sm font-medium">3</span>
                </div>
                <span className="ml-2 text-sm font-medium text-gray-500">Complete</span>
              </div>
            </div>
          </div>

          {/* Order Summary */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Payment Processor */}
            <div className="lg:col-span-2">
              <VNPayProcessor
                orderId={state!.orderId}
                paymentMethodId={state!.paymentMethodId}
                orderData={orderData}
                onPaymentSuccess={handlePaymentSuccess}
                onPaymentFailure={handlePaymentFailure}
                onCancel={handleCancel}
              />
            </div>

            {/* Order Details Sidebar */}
            <div className="lg:col-span-1">
              <Card>
                <div className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Order Summary</h3>
                  
                  <div className="space-y-3 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Order ID:</span>
                      <span className="font-medium">#{orderData.id}</span>
                    </div>
                    
                    <div className="flex justify-between">
                      <span className="text-gray-600">Items:</span>
                      <span className="font-medium">{orderData.items.length} items</span>
                    </div>
                    
                    <div className="border-t pt-3 space-y-2">
                      <div className="flex justify-between">
                        <span className="text-gray-600">Subtotal:</span>
                        <span>{orderData.subtotal.toLocaleString('vi-VN')} VND</span>
                      </div>
                      
                      {orderData.vatAmount > 0 && (
                        <div className="flex justify-between">
                          <span className="text-gray-600">VAT:</span>
                          <span>{orderData.vatAmount.toLocaleString('vi-VN')} VND</span>
                        </div>
                      )}
                      
                      <div className="flex justify-between">
                        <span className="text-gray-600">Shipping:</span>
                        <span>{orderData.shippingFee.toLocaleString('vi-VN')} VND</span>
                      </div>
                      
                      {orderData.isRushOrder && orderData.rushOrderFee && (
                        <div className="flex justify-between">
                          <span className="text-gray-600">Rush Order:</span>
                          <span>{orderData.rushOrderFee.toLocaleString('vi-VN')} VND</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="border-t pt-3 flex justify-between font-semibold">
                      <span>Total:</span>
                      <span>{orderData.totalAmount.toLocaleString('vi-VN')} VND</span>
                    </div>
                  </div>

                  {/* Security Info */}
                  <div className="mt-6 p-4 bg-blue-50 rounded-lg">
                    <div className="flex items-start">
                      <svg className="w-5 h-5 text-blue-600 mt-0.5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                      </svg>
                      <div>
                        <h4 className="text-sm font-medium text-blue-900">Secure Payment</h4>
                        <p className="text-xs text-blue-700 mt-1">
                          Your payment is protected by VNPay's advanced security measures and SSL encryption.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            </div>
          </div>

          {/* Support Information */}
          <div className="mt-8 text-center">
            <p className="text-sm text-gray-500">
              Having trouble? <a href="/support" className="text-blue-600 hover:text-blue-500 underline">Contact our support team</a>
            </p>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default PaymentProcessing;