import React from 'react';
import { Link } from 'react-router-dom';
import type { Order } from '../../types';
import { ORDER_STATUS_LABELS, ORDER_STATUS_COLORS, canCancelOrder } from '../../types/order';
import { Button } from '../ui';

interface OrderCardProps {
  order: Order;
  onCancel?: (orderId: string) => void;
  onViewDetails?: (orderId: string) => void;
}

export const OrderCard: React.FC<OrderCardProps> = ({
  order,
  onCancel,
  onViewDetails
}) => {
  const statusColor = ORDER_STATUS_COLORS[order.status];
  const statusLabel = ORDER_STATUS_LABELS[order.status];
  const canCancel = canCancelOrder(order);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const getStatusBadgeClasses = (color: string) => {
    const baseClasses = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium';
    
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

  const handleCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (onCancel) {
      onCancel(order.id);
    }
  };

  const handleViewDetails = () => {
    if (onViewDetails) {
      onViewDetails(order.id);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 hover:shadow-md transition-shadow">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-3">
          <h3 className="text-lg font-semibold text-gray-900">
            Order #{order.id.slice(-8).toUpperCase()}
          </h3>
          <span className={getStatusBadgeClasses(statusColor)}>
            {statusLabel}
          </span>
        </div>
        <div className="text-sm text-gray-500">
          {formatDate(order.createdAt)}
        </div>
      </div>

      {/* Order Details */}
      <div className="space-y-2 mb-4">
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Items:</span>
          <span className="font-medium">
            {order.items.reduce((total, item) => total + item.quantity, 0)} item(s)
          </span>
        </div>
        
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Total Amount:</span>
          <span className="font-semibold text-lg text-gray-900">
            {formatCurrency(order.totalAmount)}
          </span>
        </div>

        {order.isRushOrder && (
          <div className="flex items-center space-x-1">
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-orange-100 text-orange-800">
              🚀 Rush Order
            </span>
            {order.rushOrderFee && (
              <span className="text-xs text-gray-500">
                (+{formatCurrency(order.rushOrderFee)})
              </span>
            )}
          </div>
        )}

        {order.trackingNumber && (
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Tracking:</span>
            <span className="font-mono text-blue-600">{order.trackingNumber}</span>
          </div>
        )}
      </div>

      {/* Items Preview */}
      <div className="border-t pt-3 mb-4">
        <div className="space-y-1">
          {order.items.slice(0, 2).map((item, index) => (
            <div key={index} className="flex justify-between text-sm">
              <span className="text-gray-600 truncate">
                {item.productTitle} × {item.quantity}
              </span>
              <span className="font-medium ml-2">
                {formatCurrency(item.subtotal)}
              </span>
            </div>
          ))}
          {order.items.length > 2 && (
            <div className="text-xs text-gray-500">
              +{order.items.length - 2} more item(s)
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-between items-center pt-3 border-t">
        <div className="flex space-x-2">
          <Link
            to={`/orders/${order.id}`}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            onClick={handleViewDetails}
          >
            View Details
          </Link>
          
          {canCancel && (
            <button
              onClick={handleCancel}
              className="text-red-600 hover:text-red-800 text-sm font-medium"
            >
              Cancel Order
            </button>
          )}
        </div>

        <div className="flex space-x-2">
          {order.status === 'DELIVERED' && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.print()}
            >
              Download Invoice
            </Button>
          )}
        </div>
      </div>

      {/* Mobile Responsive Actions */}
      <div className="block sm:hidden mt-3 pt-3 border-t">
        <div className="grid grid-cols-2 gap-2">
          <Link
            to={`/orders/${order.id}`}
            className="btn btn-primary text-center"
          >
            View Details
          </Link>
          
          {canCancel && (
            <Button
              variant="outline"
              onClick={handleCancel}
              className="text-red-600 border-red-300 hover:bg-red-50"
            >
              Cancel
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};