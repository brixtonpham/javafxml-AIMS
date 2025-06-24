import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import Card from './ui/Card';
import Button from './ui/Button';
import { orderService } from '../services/orderService';
import type { Order, OrderItem } from '../types';

const OrderConfirmation: React.FC = () => {
  const navigate = useNavigate();
  const { orderId } = useParams<{ orderId: string }>();
  const [showSuccess, setShowSuccess] = useState(true);

  // Fetch order details
  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => orderService.getOrderById(orderId!),
    enabled: !!orderId,
    retry: 3,
    retryDelay: 1000
  });

  useEffect(() => {
    // Clear any checkout data from session storage
    sessionStorage.removeItem('checkout_current_step');
    sessionStorage.removeItem('checkout_form_data');
    sessionStorage.removeItem('checkout_calculation_result');
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'APPROVED':
        return 'bg-blue-100 text-blue-800';
      case 'SHIPPED':
        return 'bg-indigo-100 text-indigo-800';
      case 'DELIVERED':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'Pending Review';
      case 'APPROVED':
        return 'Approved';
      case 'SHIPPED':
        return 'Shipped';
      case 'DELIVERED':
        return 'Delivered';
      case 'CANCELLED':
        return 'Cancelled';
      case 'REJECTED':
        return 'Rejected';
      default:
        return status;
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading order details...</p>
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card>
          <div className="p-8 text-center">
            <svg className="mx-auto h-12 w-12 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <h3 className="mt-2 text-lg font-medium text-gray-900">Order Not Found</h3>
            <p className="mt-1 text-sm text-gray-500">
              We couldn't find the order you're looking for. It may have been removed or doesn't exist.
            </p>
            <div className="mt-6 space-x-3">
              <Button variant="outline" onClick={() => navigate('/orders')}>
                View My Orders
              </Button>
              <Button onClick={() => navigate('/')}>
                Continue Shopping
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        {/* Success Animation */}
        {showSuccess && (
          <Card className="mb-8">
            <div className="p-8 text-center">
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-4">
                <svg className="h-8 w-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">
                Order Placed Successfully!
              </h1>
              <p className="text-gray-600 mb-4">
                Thank you for your order. We've received your order and will process it shortly.
              </p>
              <Button 
                variant="outline" 
                onClick={() => setShowSuccess(false)}
                className="text-sm"
              >
                View Order Details
              </Button>
            </div>
          </Card>
        )}

        {/* Order Details */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Order Information */}
          <div className="lg:col-span-2 space-y-6">
            {/* Order Header */}
            <Card>
              <div className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h2 className="text-xl font-semibold text-gray-900">
                      Order #{order.id}
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">
                      Placed on {formatDate(order.createdAt)}
                    </p>
                  </div>
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}>
                    {getStatusLabel(order.status)}
                  </span>
                </div>
                
                {order.isRushOrder && (
                  <div className="bg-orange-50 border border-orange-200 rounded-lg p-3 mb-4">
                    <div className="flex items-center">
                      <svg className="h-5 w-5 text-orange-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                      </svg>
                      <span className="text-sm text-orange-800 font-medium">
                        ⚡ Rush Order - Priority Processing
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </Card>

            {/* Order Items */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  Order Items ({order.items.length})
                </h3>
                <div className="space-y-4">
                  {order.items.map((item: any) => (
                    <div key={item.productId} className="flex items-center space-x-4 p-4 bg-gray-50 rounded-lg">
                      <div className="w-16 h-16 bg-gray-200 rounded-md flex items-center justify-center">
                        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                        </svg>
                      </div>
                      <div className="flex-1">
                        <h4 className="font-medium text-gray-900">{item.productTitle}</h4>
                        <p className="text-sm text-gray-500">{item.productMetadata?.category ?? item.productType}</p>
                        <div className="flex items-center justify-between mt-2">
                          <span className="text-sm text-gray-600">
                            Quantity: {item.quantity}
                          </span>
                          <span className="font-medium">
                            {formatCurrency(item.subtotal)}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </Card>

            {/* Delivery Information */}
            {order.deliveryInfo && (
              <Card>
                <div className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">
                    Delivery Information
                  </h3>
                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="font-medium">Recipient:</span> {order.deliveryInfo.recipientName}
                    </div>
                    <div>
                      <span className="font-medium">Phone:</span> {order.deliveryInfo.phone}
                    </div>
                    <div>
                      <span className="font-medium">Email:</span> {order.deliveryInfo.email}
                    </div>
                    <div>
                      <span className="font-medium">Address:</span>{' '}
                      {order.deliveryInfo.address}, {order.deliveryInfo.city}, {order.deliveryInfo.province}
                    </div>
                    {order.deliveryInfo.postalCode && (
                      <div>
                        <span className="font-medium">Postal Code:</span> {order.deliveryInfo.postalCode}
                      </div>
                    )}
                    {order.deliveryInfo.deliveryInstructions && (
                      <div>
                        <span className="font-medium">Instructions:</span> {order.deliveryInfo.deliveryInstructions}
                      </div>
                    )}
                  </div>
                </div>
              </Card>
            )}
          </div>

          {/* Order Summary Sidebar */}
          <div className="space-y-6">
            {/* Payment Summary */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  Order Summary
                </h3>
                <div className="space-y-3">
                  <div className="flex justify-between text-sm">
                    <span>Subtotal</span>
                    <span>{formatCurrency(order.subtotal)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>VAT</span>
                    <span>{formatCurrency(order.vatAmount)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Shipping</span>
                    <span>{formatCurrency(order.shippingFee)}</span>
                  </div>
                  {order.rushOrderFee && order.rushOrderFee > 0 && (
                    <div className="flex justify-between text-sm text-orange-600">
                      <span>Rush Delivery</span>
                      <span>{formatCurrency(order.rushOrderFee)}</span>
                    </div>
                  )}
                  <div className="border-t pt-3">
                    <div className="flex justify-between font-semibold text-lg">
                      <span>Total</span>
                      <span className="text-blue-600">{formatCurrency(order.totalAmount)}</span>
                    </div>
                  </div>
                </div>
              </div>
            </Card>

            {/* Next Steps */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  What's Next?
                </h3>
                <div className="space-y-3 text-sm text-gray-600">
                  <div className="flex items-start space-x-2">
                    <div className="w-2 h-2 bg-blue-600 rounded-full mt-2 flex-shrink-0"></div>
                    <p>Your order is currently pending review by our team.</p>
                  </div>
                  <div className="flex items-start space-x-2">
                    <div className="w-2 h-2 bg-gray-300 rounded-full mt-2 flex-shrink-0"></div>
                    <p>We'll send you an email confirmation once your order is approved.</p>
                  </div>
                  <div className="flex items-start space-x-2">
                    <div className="w-2 h-2 bg-gray-300 rounded-full mt-2 flex-shrink-0"></div>
                    <p>You'll receive tracking information when your order ships.</p>
                  </div>
                </div>
              </div>
            </Card>

            {/* Action Buttons */}
            <div className="space-y-3">
              <Button 
                onClick={() => navigate('/orders')}
                className="w-full"
              >
                View All Orders
              </Button>
              <Button 
                variant="outline"
                onClick={() => navigate('/')}
                className="w-full"
              >
                Continue Shopping
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;
