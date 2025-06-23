import React from 'react';
import type { Order } from '../../types';
import type { OrderTimeline } from '../../types/order';
import { formatOrderTimeline, ORDER_STATUS_LABELS, getOrderStatusProgress } from '../../types/order';

interface OrderStatusTimelineProps {
  order: Order;
  timeline?: OrderTimeline[];
}

export const OrderStatusTimeline: React.FC<OrderStatusTimelineProps> = ({
  order,
  timeline
}) => {
  const orderTimeline = timeline || formatOrderTimeline(order);
  const progress = getOrderStatusProgress(order.status);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStepStatus = (stepStatus: string, currentStatus: string) => {
    const statusOrder = ['PENDING', 'APPROVED', 'SHIPPED', 'DELIVERED'];
    const currentIndex = statusOrder.indexOf(currentStatus);
    const stepIndex = statusOrder.indexOf(stepStatus);
    
    if (currentStatus === 'CANCELLED' || currentStatus === 'REJECTED') {
      return stepStatus === 'PENDING' ? 'completed' : 'cancelled';
    }
    
    if (stepIndex <= currentIndex) {
      return 'completed';
    } else {
      return 'pending';
    }
  };

  const getStepIcon = (status: string, stepStatus: string) => {
    switch (stepStatus) {
      case 'completed':
        return (
          <div className="flex items-center justify-center w-8 h-8 bg-green-500 rounded-full">
            <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'cancelled':
        return (
          <div className="flex items-center justify-center w-8 h-8 bg-red-500 rounded-full">
            <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'current':
        return (
          <div className="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-full">
            <div className="w-3 h-3 bg-white rounded-full animate-pulse"></div>
          </div>
        );
      default:
        return (
          <div className="flex items-center justify-center w-8 h-8 bg-gray-300 rounded-full">
            <div className="w-3 h-3 bg-white rounded-full"></div>
          </div>
        );
    }
  };

  const standardSteps = [
    { status: 'PENDING', label: 'Order Placed', description: 'Order submitted and pending review' },
    { status: 'APPROVED', label: 'Order Approved', description: 'Order approved by product manager' },
    { status: 'SHIPPED', label: 'Order Shipped', description: 'Order shipped and in transit' },
    { status: 'DELIVERED', label: 'Order Delivered', description: 'Order delivered successfully' }
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">Order Progress</h3>
        
        {/* Progress Bar */}
        <div className="w-full bg-gray-200 rounded-full h-2 mb-4">
          <div 
            className="bg-blue-500 h-2 rounded-full transition-all duration-300 ease-in-out"
            style={{ width: `${progress}%` }}
          ></div>
        </div>
        
        <div className="flex justify-between text-sm text-gray-600">
          <span>Order Placed</span>
          <span>{progress}% Complete</span>
          <span>Delivered</span>
        </div>
      </div>

      {/* Timeline */}
      <div className="space-y-6">
        {orderTimeline.map((item, index) => {
          const stepStatus = getStepStatus(item.status, order.status);
          const isLast = index === orderTimeline.length - 1;

          return (
            <div key={index} className="relative flex items-start space-x-4">
              {/* Connector line */}
              {!isLast && (
                <div 
                  className={`absolute left-4 top-8 w-0.5 h-6 ${
                    stepStatus === 'completed' ? 'bg-green-500' : 
                    stepStatus === 'cancelled' ? 'bg-red-500' : 'bg-gray-300'
                  }`}
                ></div>
              )}

              {/* Step Icon */}
              {getStepIcon(item.status, stepStatus)}

              {/* Step Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h4 className={`text-sm font-medium ${
                    stepStatus === 'completed' ? 'text-green-900' :
                    stepStatus === 'cancelled' ? 'text-red-900' :
                    'text-gray-900'
                  }`}>
                    {ORDER_STATUS_LABELS[item.status]}
                  </h4>
                  <span className="text-xs text-gray-500">
                    {formatDate(item.timestamp)}
                  </span>
                </div>
                
                <p className="mt-1 text-sm text-gray-600">
                  {item.description}
                </p>
                
                {item.updatedBy && (
                  <p className="mt-1 text-xs text-gray-500">
                    Updated by: {item.updatedBy}
                  </p>
                )}
                
                {item.notes && (
                  <div className="mt-2 p-2 bg-gray-50 rounded text-xs text-gray-600">
                    <strong>Notes:</strong> {item.notes}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Special Status Messages */}
      {order.status === 'CANCELLED' && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-red-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-red-800">Order Cancelled</h4>
              <p className="text-sm text-red-700">
                This order has been cancelled. If you paid for this order, a refund will be processed automatically.
              </p>
            </div>
          </div>
        </div>
      )}

      {order.status === 'REJECTED' && order.rejectionReason && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-red-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-red-800">Order Rejected</h4>
              <p className="text-sm text-red-700">
                <strong>Reason:</strong> {order.rejectionReason}
              </p>
            </div>
          </div>
        </div>
      )}

      {order.trackingNumber && order.status === 'SHIPPED' && (
        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium text-blue-800">Tracking Information</h4>
              <p className="text-sm text-blue-700">
                Tracking Number: <span className="font-mono">{order.trackingNumber}</span>
              </p>
            </div>
            <button className="text-blue-600 hover:text-blue-800 text-sm font-medium">
              Track Package
            </button>
          </div>
        </div>
      )}
    </div>
  );
};