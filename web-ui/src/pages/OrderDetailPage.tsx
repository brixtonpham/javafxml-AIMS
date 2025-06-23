import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import { Button } from '../components/ui';
import { OrderStatusTimeline, OrderCancellation } from '../components/orders';
import { useOrderContext } from '../contexts/OrderContext';
import { useAuth } from '../components/auth';
import type { Order } from '../types';
import { canCancelOrder, ORDER_STATUS_LABELS, ORDER_STATUS_COLORS } from '../types/order';

export const OrderDetailPage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const {
    currentOrder,
    isLoading,
    error,
    fetchOrderById,
    cancelOrder,
    orderDetailQuery
  } = useOrderContext();

  const [showCancellation, setShowCancellation] = useState(false);

  useEffect(() => {
    if (orderId && isAuthenticated) {
      fetchOrderById(orderId);
    }
  }, [orderId, isAuthenticated, fetchOrderById]);

  if (!isAuthenticated) {
    return (
      <AppLayout title="Order Details - Please Login">
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Please Log In</h2>
          <p className="text-gray-600 mb-6">You need to be logged in to view order details.</p>
          <Button onClick={() => navigate('/login')}>
            Log In
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (!orderId) {
    return (
      <AppLayout title="Order Not Found">
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Order Not Found</h2>
          <p className="text-gray-600 mb-6">The order ID is missing or invalid.</p>
          <Button onClick={() => navigate('/orders')}>
            Back to Orders
          </Button>
        </div>
      </AppLayout>
    );
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadgeClasses = (status: string) => {
    const color = ORDER_STATUS_COLORS[status as keyof typeof ORDER_STATUS_COLORS];
    const baseClasses = 'inline-flex items-center px-3 py-1 rounded-full text-sm font-medium';
    
    switch (color) {
      case 'yellow':
        return `${baseClasses} bg-yellow-100 text-yellow-800`;
      case 'blue':
        return `${baseClasses} bg-blue-100 text-blue-800`;
      case 'indigo':
        return `${baseClasses} bg-indigo-100 text-indigo-800`;
      case 'green':
        return `${baseClasses} bg-green-100 text-green-800`;
      case 'red':
        return `${baseClasses} bg-red-100 text-red-800`;
      default:
        return `${baseClasses} bg-gray-100 text-gray-800`;
    }
  };

  const handleCancelOrder = async (orderId: string, reason?: string) => {
    try {
      await cancelOrder(orderId, reason);
      setShowCancellation(false);
    } catch (error) {
      console.error('Failed to cancel order:', error);
    }
  };

  const handlePrintInvoice = () => {
    // Create a new window with print-friendly content
    const printWindow = window.open('', '_blank');
    if (!printWindow || !currentOrder) return;

    const invoiceHTML = `
      <!DOCTYPE html>
      <html>
        <head>
          <title>Invoice - Order #${currentOrder.id.slice(-8).toUpperCase()}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .header { text-align: center; margin-bottom: 30px; }
            .order-info { margin-bottom: 20px; }
            .items-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
            .items-table th, .items-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            .items-table th { background-color: #f2f2f2; }
            .totals { margin-left: auto; width: 300px; }
            .total-row { display: flex; justify-content: space-between; margin-bottom: 5px; }
            .grand-total { font-weight: bold; font-size: 18px; border-top: 2px solid #000; padding-top: 10px; }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>AIMS - An Internet Media Store</h1>
            <h2>Invoice</h2>
          </div>
          
          <div class="order-info">
            <p><strong>Order ID:</strong> #${currentOrder.id.slice(-8).toUpperCase()}</p>
            <p><strong>Order Date:</strong> ${formatDate(currentOrder.createdAt)}</p>
            <p><strong>Status:</strong> ${ORDER_STATUS_LABELS[currentOrder.status]}</p>
          </div>
          
          ${currentOrder.deliveryInfo ? `
            <div class="delivery-info">
              <h3>Delivery Information</h3>
              <p><strong>Recipient:</strong> ${currentOrder.deliveryInfo.recipientName}</p>
              <p><strong>Phone:</strong> ${currentOrder.deliveryInfo.phone}</p>
              <p><strong>Email:</strong> ${currentOrder.deliveryInfo.email}</p>
              <p><strong>Address:</strong> ${currentOrder.deliveryInfo.address}, ${currentOrder.deliveryInfo.city}, ${currentOrder.deliveryInfo.province}</p>
            </div>
          ` : ''}
          
          <table class="items-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Type</th>
                <th>Quantity</th>
                <th>Unit Price</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              ${currentOrder.items.map(item => `
                <tr>
                  <td>${item.productTitle}</td>
                  <td>${item.productType}</td>
                  <td>${item.quantity}</td>
                  <td>${formatCurrency(item.unitPrice)}</td>
                  <td>${formatCurrency(item.subtotal)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
          
          <div class="totals">
            <div class="total-row">
              <span>Subtotal:</span>
              <span>${formatCurrency(currentOrder.subtotal)}</span>
            </div>
            <div class="total-row">
              <span>VAT (10%):</span>
              <span>${formatCurrency(currentOrder.vatAmount)}</span>
            </div>
            <div class="total-row">
              <span>Shipping Fee:</span>
              <span>${formatCurrency(currentOrder.shippingFee)}</span>
            </div>
            ${currentOrder.rushOrderFee ? `
              <div class="total-row">
                <span>Rush Order Fee:</span>
                <span>${formatCurrency(currentOrder.rushOrderFee)}</span>
              </div>
            ` : ''}
            <div class="total-row grand-total">
              <span>Total Amount:</span>
              <span>${formatCurrency(currentOrder.totalAmount)}</span>
            </div>
          </div>
          
          <div style="margin-top: 40px;">
            <p><em>Thank you for your business!</em></p>
          </div>
        </body>
      </html>
    `;

    printWindow.document.write(invoiceHTML);
    printWindow.document.close();
    printWindow.print();
    printWindow.close();
  };

  if (isLoading) {
    return (
      <AppLayout title="Loading Order...">
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading order details...</p>
        </div>
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout title="Error Loading Order">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center max-w-2xl mx-auto mt-12">
          <p className="text-red-800 mb-4">{error}</p>
          <div className="space-x-4">
            <Button onClick={() => orderDetailQuery.refetch()}>
              Try Again
            </Button>
            <Button variant="outline" onClick={() => navigate('/orders')}>
              Back to Orders
            </Button>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (!currentOrder) {
    return (
      <AppLayout title="Order Not Found">
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Order Not Found</h2>
          <p className="text-gray-600 mb-6">The requested order could not be found.</p>
          <Button onClick={() => navigate('/orders')}>
            Back to Orders
          </Button>
        </div>
      </AppLayout>
    );
  }

  const canCancel = canCancelOrder(currentOrder);

  return (
    <AppLayout title={`Order #${currentOrder.id.slice(-8).toUpperCase()}`}>
      <div className="max-w-4xl mx-auto px-4 py-6">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <button
              onClick={() => navigate('/orders')}
              className="flex items-center text-blue-600 hover:text-blue-800"
            >
              <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              Back to Orders
            </button>

            <div className="flex space-x-3">
              <Button
                variant="outline"
                onClick={handlePrintInvoice}
              >
                Print Invoice
              </Button>
              
              {canCancel && (
                <Button
                  variant="outline"
                  onClick={() => setShowCancellation(true)}
                  className="text-red-600 border-red-300 hover:bg-red-50"
                >
                  Cancel Order
                </Button>
              )}
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                Order #{currentOrder.id.slice(-8).toUpperCase()}
              </h1>
              <p className="text-gray-600 mt-1">
                Placed on {formatDate(currentOrder.createdAt)}
              </p>
            </div>
            <span className={getStatusBadgeClasses(currentOrder.status)}>
              {ORDER_STATUS_LABELS[currentOrder.status]}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Order Timeline */}
            <OrderStatusTimeline order={currentOrder} />

            {/* Order Items */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Order Items</h3>
              <div className="space-y-4">
                {currentOrder.items.map((item, index) => (
                  <div key={index} className="flex items-center space-x-4 pb-4 border-b border-gray-200 last:border-b-0 last:pb-0">
                    <div className="flex-1">
                      <h4 className="font-medium text-gray-900">{item.productTitle}</h4>
                      <p className="text-sm text-gray-500">{item.productType}</p>
                      {item.productMetadata && (
                        <div className="text-xs text-gray-400 mt-1">
                          {item.productMetadata.author && `Author: ${item.productMetadata.author}`}
                          {item.productMetadata.artists && `Artists: ${item.productMetadata.artists}`}
                          {item.productMetadata.director && `Director: ${item.productMetadata.director}`}
                        </div>
                      )}
                    </div>
                    <div className="text-right">
                      <p className="font-medium text-gray-900">
                        {item.quantity} × {formatCurrency(item.unitPrice)}
                      </p>
                      <p className="text-sm text-gray-500">
                        Total: {formatCurrency(item.subtotal)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Delivery Information */}
            {currentOrder.deliveryInfo && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Delivery Information</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-600">Recipient</p>
                    <p className="font-medium">{currentOrder.deliveryInfo.recipientName}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Phone</p>
                    <p className="font-medium">{currentOrder.deliveryInfo.phone}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Email</p>
                    <p className="font-medium">{currentOrder.deliveryInfo.email}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Address</p>
                    <p className="font-medium">
                      {currentOrder.deliveryInfo.address}<br />
                      {currentOrder.deliveryInfo.city}, {currentOrder.deliveryInfo.province}
                      {currentOrder.deliveryInfo.postalCode && ` ${currentOrder.deliveryInfo.postalCode}`}
                    </p>
                  </div>
                  {currentOrder.deliveryInfo.deliveryInstructions && (
                    <div className="md:col-span-2">
                      <p className="text-sm text-gray-600">Delivery Instructions</p>
                      <p className="font-medium">{currentOrder.deliveryInfo.deliveryInstructions}</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Order Summary */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Order Summary</h3>
              <div className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Subtotal</span>
                  <span>{formatCurrency(currentOrder.subtotal)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">VAT (10%)</span>
                  <span>{formatCurrency(currentOrder.vatAmount)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Shipping Fee</span>
                  <span>{formatCurrency(currentOrder.shippingFee)}</span>
                </div>
                {currentOrder.rushOrderFee && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Rush Order Fee</span>
                    <span>{formatCurrency(currentOrder.rushOrderFee)}</span>
                  </div>
                )}
                <div className="border-t pt-3">
                  <div className="flex justify-between font-semibold text-lg">
                    <span>Total</span>
                    <span>{formatCurrency(currentOrder.totalAmount)}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Payment Information */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Payment Information</h3>
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Payment Method</span>
                  <span>{currentOrder.paymentMethod?.name || 'Not specified'}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Payment Status</span>
                  <span className={`font-medium ${
                    currentOrder.paymentStatus === 'COMPLETED' ? 'text-green-600' :
                    currentOrder.paymentStatus === 'FAILED' ? 'text-red-600' :
                    'text-yellow-600'
                  }`}>
                    {currentOrder.paymentStatus || 'Pending'}
                  </span>
                </div>
              </div>
            </div>

            {/* Contact Support */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-blue-900 mb-2">Need Help?</h3>
              <p className="text-sm text-blue-700 mb-4">
                If you have any questions about your order, our support team is here to help.
              </p>
              <Button variant="outline" className="w-full">
                Contact Support
              </Button>
            </div>
          </div>
        </div>

        {/* Order Cancellation Modal */}
        {showCancellation && (
          <OrderCancellation
            order={currentOrder}
            onCancel={handleCancelOrder}
            onClose={() => setShowCancellation(false)}
            isLoading={false}
          />
        )}
      </div>
    </AppLayout>
  );
};