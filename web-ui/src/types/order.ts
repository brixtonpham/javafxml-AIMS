import type { Order, OrderStatus, PaymentStatus } from './index';

// Enhanced order types for the order management system
export interface OrderTimeline {
  status: OrderStatus;
  timestamp: string;
  description: string;
  updatedBy?: string;
  notes?: string;
}

export interface OrderSearchFilters {
  status?: OrderStatus | 'ALL';
  dateFrom?: string;
  dateTo?: string;
  orderId?: string;
  page?: number;
  pageSize?: number;
}

export interface OrderCancellationRequest {
  orderId: string;
  reason?: string;
}

export interface OrderRefund {
  orderId: string;
  amount: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  transactionId?: string;
  processedAt?: string;
  failureReason?: string;
}

export interface OrderNotification {
  id: string;
  orderId: string;
  type: 'ORDER_CONFIRMATION' | 'STATUS_UPDATE' | 'CANCELLATION' | 'DELIVERY_UPDATE';
  title: string;
  message: string;
  emailSent: boolean;
  createdAt: string;
}

export interface EnhancedOrder extends Order {
  timeline: OrderTimeline[];
  refund?: OrderRefund;
  notifications: OrderNotification[];
  canCancel: boolean;
  cancellationDeadline?: string;
}

export interface OrderStatusStats {
  total: number;
  pending: number;
  approved: number;
  shipped: number;
  delivered: number;
  cancelled: number;
  rejected: number;
}

// Order state machine helpers
export const ORDER_STATUS_FLOW: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['APPROVED', 'CANCELLED', 'REJECTED'],
  APPROVED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
  REJECTED: []
};

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pending Review',
  APPROVED: 'Approved',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
  REJECTED: 'Rejected'
};

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: 'yellow',
  APPROVED: 'blue',
  SHIPPED: 'indigo',
  DELIVERED: 'green',
  CANCELLED: 'red',
  REJECTED: 'red'
};

// Helper functions
export const canCancelOrder = (order: Order): boolean => {
  return order.status === 'PENDING' || order.status === 'APPROVED';
};

export const getOrderStatusProgress = (status: OrderStatus): number => {
  const progressMap: Record<OrderStatus, number> = {
    PENDING: 25,
    APPROVED: 50,
    SHIPPED: 75,
    DELIVERED: 100,
    CANCELLED: 0,
    REJECTED: 0
  };
  return progressMap[status];
};

export const getNextValidStatuses = (currentStatus: OrderStatus): OrderStatus[] => {
  return ORDER_STATUS_FLOW[currentStatus] || [];
};

export const formatOrderTimeline = (order: Order): OrderTimeline[] => {
  const timeline: OrderTimeline[] = [
    {
      status: 'PENDING',
      timestamp: order.createdAt,
      description: 'Order placed and pending review'
    }
  ];

  if (order.approvedAt && order.approvedBy) {
    timeline.push({
      status: 'APPROVED',
      timestamp: order.approvedAt,
      description: 'Order approved by product manager',
      updatedBy: order.approvedBy
    });
  }

  if (order.rejectedAt && order.rejectedBy) {
    timeline.push({
      status: 'REJECTED',
      timestamp: order.rejectedAt,
      description: order.rejectionReason || 'Order rejected',
      updatedBy: order.rejectedBy,
      notes: order.rejectionReason
    });
  }

  if (order.status === 'SHIPPED' && order.trackingNumber) {
    timeline.push({
      status: 'SHIPPED',
      timestamp: order.updatedAt,
      description: `Order shipped with tracking number: ${order.trackingNumber}`
    });
  }

  if (order.status === 'DELIVERED') {
    timeline.push({
      status: 'DELIVERED',
      timestamp: order.updatedAt,
      description: 'Order delivered successfully'
    });
  }

  if (order.status === 'CANCELLED') {
    timeline.push({
      status: 'CANCELLED',
      timestamp: order.updatedAt,
      description: 'Order cancelled'
    });
  }

  return timeline.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
};