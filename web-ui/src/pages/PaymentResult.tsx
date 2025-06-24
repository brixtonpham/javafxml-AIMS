import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { usePaymentContext } from '../contexts/PaymentContext';
import { useOrderContext } from '../contexts/OrderContext';
import { VNPayValidation } from '../utils/vnpayValidation';
import { API_BASE_URL } from '../services/api';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import AppLayout from '../components/layout/AppLayout';
import type { VNPayCallbackParams, PaymentTransaction } from '../types/payment';
import type { Order } from '../types';

const PaymentResult: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { processPaymentReturn, isProcessing, error } = usePaymentContext();
  const { refreshOrder } = useOrderContext();
  
  const [transaction, setTransaction] = useState<PaymentTransaction | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [isSuccess, setIsSuccess] = useState<boolean | null>(null);
  const [responseMessage, setResponseMessage] = useState<string>('');
  const [processingComplete, setProcessingComplete] = useState(false);

  useEffect(() => {
    const processPaymentResult = async () => {
      try {
        // Extract VNPay parameters from URL
        const returnParams: Partial<VNPayCallbackParams> = {};
        
        // Get all vnp_ parameters from URL
        for (const [key, value] of searchParams.entries()) {
          if (key.startsWith('vnp_')) {
            (returnParams as any)[key] = value;
          }
        }

        // Validate required parameters
        if (!returnParams.vnp_ResponseCode || !returnParams.vnp_TxnRef || !returnParams.vnp_SecureHash) {
          throw new Error('Missing required payment parameters');
        }

        // Process payment return
        const processedTransaction = await processPaymentReturn(returnParams as VNPayCallbackParams);
        setTransaction(processedTransaction);

        // Determine if payment was successful
        const success = VNPayValidation.isTransactionSuccessful(
          returnParams.vnp_ResponseCode,
          returnParams.vnp_TransactionStatus || '00'
        );
        setIsSuccess(success);

        // Get response message
        const message = VNPayValidation.getResponseMessage(returnParams.vnp_ResponseCode);
        setResponseMessage(message);

        // Get order details if payment was successful
        if (success && processedTransaction.orderId) {
          try {
            const orderResponse = await fetch(`${API_BASE_URL}/orders/${processedTransaction.orderId}`);
            const orderData = await orderResponse.json();
            setOrder(orderData);
            
            // Refresh order in context
            refreshOrder(processedTransaction.orderId);
          } catch (error) {
            console.error('Error fetching order details:', error);
          }
        }

        setProcessingComplete(true);

      } catch (error) {
        console.error('Error processing payment result:', error);
        setIsSuccess(false);
        setResponseMessage(error instanceof Error ? error.message : 'Payment processing failed');
        setProcessingComplete(true);
      }
    };

    if (!processingComplete) {
      processPaymentResult();
    }
  }, [searchParams, processPaymentReturn, refreshOrder, processingComplete]);

  const handleViewOrder = () => {
    if (order) {
      navigate(`/orders/${order.id}`);
    }
  };

  const handleContinueShopping = () => {
    navigate('/');
  };

  const handleRetryPayment = () => {
    if (order) {
      navigate(`/checkout`, { 
        state: { 
          orderId: order.id,
          retry: true 
        } 
      });
    }
  };

  const handleContactSupport = () => {
    // Navigate to support page or open contact modal
    navigate('/support', {
      state: {
        subject: 'Payment Issue',
        orderId: order?.id,
        transactionId: transaction?.id
      }
    });
  };

  // Loading state
  if (isProcessing || !processingComplete) {
    return (
      <AppLayout title="Processing Payment Result">
        <div className="min-h-screen flex items-center justify-center">
          <Card className="max-w-md mx-auto">
            <div className="p-8 text-center">
              <div className="w-16 h-16 mx-auto mb-4 text-blue-500">
                <svg className="animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v12m6-6H6" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Processing Payment Result</h3>
              <p className="text-gray-600">
                Please wait while we confirm your payment status...
              </p>
            </div>
          </Card>
        </div>
      </AppLayout>
    );
  }

  // Error state
  if (error || isSuccess === false) {
    return (
      <AppLayout title="Payment Failed">
        <div className="min-h-screen flex items-center justify-center">
          <Card className="max-w-lg mx-auto">
            <div className="p-8 text-center">
              <div className="w-20 h-20 mx-auto mb-6 text-red-500">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.962-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              
              <h2 className="text-2xl font-bold text-gray-900 mb-4">Payment Failed</h2>
              <p className="text-gray-600 mb-6">
                {error || responseMessage || 'Your payment could not be processed. Please try again.'}
              </p>

              {transaction && (
                <div className="bg-red-50 rounded-lg p-4 mb-6 text-left">
                  <h4 className="font-medium text-red-900 mb-2">Transaction Details</h4>
                  <div className="space-y-1 text-sm text-red-700">
                    <div>Transaction ID: {transaction.transactionId}</div>
                    <div>Amount: {transaction.amount.toLocaleString('vi-VN')} VND</div>
                    <div>Status: {transaction.status}</div>
                    {transaction.responseCode && (
                      <div>Response Code: {transaction.responseCode}</div>
                    )}
                  </div>
                </div>
              )}

              <div className="flex flex-col sm:flex-row gap-3">
                <Button onClick={handleRetryPayment} className="flex-1">
                  Try Again
                </Button>
                <Button variant="outline" onClick={handleContactSupport} className="flex-1">
                  Contact Support
                </Button>
              </div>

              <div className="mt-4">
                <Button variant="outline" onClick={handleContinueShopping} className="text-sm">
                  Continue Shopping
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </AppLayout>
    );
  }

  // Success state
  return (
    <AppLayout title="Payment Successful">
      <div className="min-h-screen flex items-center justify-center">
        <Card className="max-w-2xl mx-auto">
          <div className="p-8">
            {/* Success Header */}
            <div className="text-center mb-8">
              <div className="w-20 h-20 mx-auto mb-6 text-green-500">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h2 className="text-3xl font-bold text-gray-900 mb-2">Payment Successful!</h2>
              <p className="text-gray-600">
                Thank you for your purchase. Your order has been confirmed.
              </p>
            </div>

            {/* Transaction Details */}
            {transaction && (
              <div className="bg-green-50 rounded-lg p-6 mb-6">
                <h3 className="font-semibold text-green-900 mb-4">Transaction Details</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-green-700 font-medium">Transaction ID:</span>
                    <div className="text-green-800">{transaction.transactionId}</div>
                  </div>
                  <div>
                    <span className="text-green-700 font-medium">Amount Paid:</span>
                    <div className="text-green-800 font-semibold">
                      {transaction.amount.toLocaleString('vi-VN')} VND
                    </div>
                  </div>
                  <div>
                    <span className="text-green-700 font-medium">Payment Method:</span>
                    <div className="text-green-800">VNPay</div>
                  </div>
                  <div>
                    <span className="text-green-700 font-medium">Payment Date:</span>
                    <div className="text-green-800">
                      {transaction.payDate || new Date().toLocaleString('vi-VN')}
                    </div>
                  </div>
                  {transaction.bankCode && (
                    <div>
                      <span className="text-green-700 font-medium">Bank:</span>
                      <div className="text-green-800">{transaction.bankCode}</div>
                    </div>
                  )}
                  {transaction.cardType && (
                    <div>
                      <span className="text-green-700 font-medium">Card Type:</span>
                      <div className="text-green-800">{transaction.cardType}</div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Order Summary */}
            {order && (
              <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <h3 className="font-semibold text-gray-900 mb-4">Order Summary</h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Order ID:</span>
                    <span className="font-medium">#{order.id}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Items:</span>
                    <span className="font-medium">{order.items.length} items</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Subtotal:</span>
                    <span>{order.subtotal.toLocaleString('vi-VN')} VND</span>
                  </div>
                  {order.vatAmount > 0 && (
                    <div className="flex justify-between">
                      <span className="text-gray-600">VAT:</span>
                      <span>{order.vatAmount.toLocaleString('vi-VN')} VND</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-gray-600">Shipping:</span>
                    <span>{order.shippingFee.toLocaleString('vi-VN')} VND</span>
                  </div>
                  <div className="border-t pt-3 flex justify-between font-semibold">
                    <span>Total Paid:</span>
                    <span>{order.totalAmount.toLocaleString('vi-VN')} VND</span>
                  </div>
                </div>
              </div>
            )}

            {/* Next Steps */}
            <div className="bg-blue-50 rounded-lg p-6 mb-6">
              <h3 className="font-semibold text-blue-900 mb-3">What's Next?</h3>
              <ul className="space-y-2 text-sm text-blue-800">
                <li className="flex items-start">
                  <svg className="w-4 h-4 mt-0.5 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                  Order confirmation email has been sent to your email address
                </li>
                <li className="flex items-start">
                  <svg className="w-4 h-4 mt-0.5 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                  Your order is now being processed by our team
                </li>
                <li className="flex items-start">
                  <svg className="w-4 h-4 mt-0.5 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                  You'll receive shipping updates via email and SMS
                </li>
                <li className="flex items-start">
                  <svg className="w-4 h-4 mt-0.5 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                  Track your order anytime in your account
                </li>
              </ul>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row gap-3">
              <Button onClick={handleViewOrder} className="flex-1">
                View Order Details
              </Button>
              <Button variant="outline" onClick={handleContinueShopping} className="flex-1">
                Continue Shopping
              </Button>
            </div>

            {/* Support Information */}
            <div className="mt-6 text-center text-sm text-gray-500">
              Need help? <button 
                onClick={handleContactSupport}
                className="text-blue-600 hover:text-blue-500 underline"
              >
                Contact our support team
              </button>
            </div>
          </div>
        </Card>
      </div>
    </AppLayout>
  );
};

export default PaymentResult;