import { vnpayService } from './VNPayService';
import { api } from './api';
import type {
  RefundRequest,
  RefundResult,
  PaymentTransaction,
  EmailNotification
} from '../types/payment';
import type { Order } from '../types';

export class RefundService {
  private static instance: RefundService;

  private constructor() {}

  public static getInstance(): RefundService {
    if (!RefundService.instance) {
      RefundService.instance = new RefundService();
    }
    return RefundService.instance;
  }

  /**
   * Process automatic refund for cancelled orders
   */
  async processAutomaticRefund(
    orderId: string,
    reason: string = 'Order cancelled by customer',
    requestedBy: string = 'SYSTEM'
  ): Promise<RefundResult | null> {
    try {
      console.log(`Processing automatic refund for order ${orderId}`);

      // Get order details
      const order = await this.getOrderDetails(orderId);
      if (!order) {
        throw new Error(`Order ${orderId} not found`);
      }

      // Check if order is eligible for refund
      const eligibility = await this.checkRefundEligibility(order);
      if (!eligibility.eligible) {
        console.log(`Order ${orderId} not eligible for refund: ${eligibility.reason}`);
        return null;
      }

      // Get successful payment transactions for this order
      const transactions = await this.getSuccessfulPayments(orderId);
      if (transactions.length === 0) {
        console.log(`No successful payments found for order ${orderId}`);
        return null;
      }

      // Process refund for the latest successful transaction
      const latestTransaction = transactions[0];
      const refundRequest: RefundRequest = {
        orderId,
        transactionId: latestTransaction.id,
        amount: latestTransaction.amount,
        reason,
        requestedBy
      };

      const refundResult = await vnpayService.processRefund(refundRequest);

      // Log refund attempt
      await this.logRefundAttempt(refundRequest, refundResult);

      // Send notification if successful
      if (refundResult.success) {
        await this.sendRefundNotification(order, refundResult, refundRequest);
      }

      return refundResult;

    } catch (error) {
      console.error(`Error processing automatic refund for order ${orderId}:`, error);
      
      // Log failed refund attempt
      await this.logRefundError(orderId, error instanceof Error ? error.message : 'Unknown error');
      
      throw error;
    }
  }

  /**
   * Process manual refund request
   */
  async processManualRefund(
    orderId: string,
    transactionId: string,
    amount: number,
    reason: string,
    requestedBy: string
  ): Promise<RefundResult> {
    try {
      console.log(`Processing manual refund for order ${orderId}, transaction ${transactionId}`);

      // Validate refund request
      const validation = await this.validateRefundRequest(orderId, transactionId, amount);
      if (!validation.valid) {
        throw new Error(`Invalid refund request: ${validation.errors.join(', ')}`);
      }

      // Create refund request
      const refundRequest: RefundRequest = {
        orderId,
        transactionId,
        amount,
        reason,
        requestedBy
      };

      // Process refund
      const refundResult = await vnpayService.processRefund(refundRequest);

      // Log refund attempt
      await this.logRefundAttempt(refundRequest, refundResult);

      // Send notification
      if (refundResult.success) {
        const order = await this.getOrderDetails(orderId);
        if (order) {
          await this.sendRefundNotification(order, refundResult, refundRequest);
        }
      }

      return refundResult;

    } catch (error) {
      console.error(`Error processing manual refund:`, error);
      throw error;
    }
  }

  /**
   * Check if order is eligible for refund
   */
  async checkRefundEligibility(order: Order): Promise<{
    eligible: boolean;
    reason?: string;
  }> {
    try {
      // Check order status
      if (order.status === 'DELIVERED') {
        return {
          eligible: false,
          reason: 'Cannot refund delivered orders'
        };
      }

      if (order.status === 'SHIPPED') {
        return {
          eligible: false,
          reason: 'Cannot refund shipped orders'
        };
      }

      if (order.status === 'CANCELLED') {
        // Check if already refunded
        const transactions = await this.getRefundedTransactions(order.id);
        if (transactions.length > 0) {
          return {
            eligible: false,
            reason: 'Order already refunded'
          };
        }
      }

      // Check payment status
      if (order.paymentStatus === 'REFUNDED') {
        return {
          eligible: false,
          reason: 'Payment already refunded'
        };
      }

      if (order.paymentStatus !== 'COMPLETED') {
        return {
          eligible: false,
          reason: 'Payment not completed'
        };
      }

      // Check time limit (e.g., 24 hours for automatic refunds)
      const orderDate = new Date(order.createdAt);
      const now = new Date();
      const hoursSinceOrder = (now.getTime() - orderDate.getTime()) / (1000 * 60 * 60);

      if (hoursSinceOrder > 24) {
        return {
          eligible: false,
          reason: 'Refund time limit exceeded (24 hours)'
        };
      }

      return { eligible: true };

    } catch (error) {
      console.error('Error checking refund eligibility:', error);
      return {
        eligible: false,
        reason: 'Error checking eligibility'
      };
    }
  }

  /**
   * Get successful payment transactions for an order
   */
  private async getSuccessfulPayments(orderId: string): Promise<PaymentTransaction[]> {
    try {
      const transactions = await vnpayService.getTransactionHistory(orderId);
      return transactions
        .filter(tx => tx.status === 'SUCCESS')
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } catch (error) {
      console.error('Error getting successful payments:', error);
      return [];
    }
  }

  /**
   * Get refunded transactions for an order
   */
  private async getRefundedTransactions(orderId: string): Promise<PaymentTransaction[]> {
    try {
      const transactions = await vnpayService.getTransactionHistory(orderId);
      return transactions.filter(tx => tx.status === 'REFUNDED' || tx.status === 'PARTIAL_REFUND');
    } catch (error) {
      console.error('Error getting refunded transactions:', error);
      return [];
    }
  }

  /**
   * Validate refund request
   */
  private async validateRefundRequest(
    orderId: string,
    transactionId: string,
    amount: number
  ): Promise<{
    valid: boolean;
    errors: string[];
  }> {
    const errors: string[] = [];

    // Validate amount
    if (amount <= 0) {
      errors.push('Refund amount must be greater than 0');
    }

    // Get transaction details
    try {
      const transactions = await vnpayService.getTransactionHistory(orderId);
      const transaction = transactions.find(tx => tx.id === transactionId);

      if (!transaction) {
        errors.push('Transaction not found');
      } else {
        if (transaction.status !== 'SUCCESS') {
          errors.push('Can only refund successful transactions');
        }

        if (amount > transaction.amount) {
          errors.push('Refund amount cannot exceed transaction amount');
        }

        if (transaction.refundAmount && (transaction.refundAmount + amount) > transaction.amount) {
          errors.push('Total refund amount cannot exceed transaction amount');
        }
      }
    } catch (error) {
      errors.push('Error validating transaction');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Get order details
   */
  private async getOrderDetails(orderId: string): Promise<Order | null> {
    try {
      const response = await api.get<Order>(`/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Error getting order details:', error);
      return null;
    }
  }

  /**
   * Log refund attempt
   */
  private async logRefundAttempt(
    refundRequest: RefundRequest,
    refundResult: RefundResult
  ): Promise<void> {
    try {
      await api.post('/admin/refunds/log', {
        refundRequest,
        refundResult,
        timestamp: new Date().toISOString(),
        success: refundResult.success
      });
    } catch (error) {
      console.error('Error logging refund attempt:', error);
      // Don't throw error as this is non-critical
    }
  }

  /**
   * Log refund error
   */
  private async logRefundError(orderId: string, errorMessage: string): Promise<void> {
    try {
      await api.post('/admin/refunds/log-error', {
        orderId,
        errorMessage,
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      console.error('Error logging refund error:', error);
      // Don't throw error as this is non-critical
    }
  }

  /**
   * Send refund notification
   */
  private async sendRefundNotification(
    order: Order,
    refundResult: RefundResult,
    refundRequest: RefundRequest
  ): Promise<void> {
    try {
      const notification: EmailNotification = {
        type: 'REFUND_PROCESSED',
        recipientEmail: order.deliveryInfo?.email || '',
        orderId: order.id,
        transactionId: refundRequest.transactionId,
        amount: refundRequest.amount,
        data: {
          refundId: refundResult.refundId,
          refundAmount: refundResult.refundAmount,
          reason: refundRequest.reason,
          requestedBy: refundRequest.requestedBy,
          orderNumber: order.id,
          customerName: order.deliveryInfo?.recipientName || 'Customer'
        }
      };

      await api.post('/notifications/refund', notification);
    } catch (error) {
      console.error('Error sending refund notification:', error);
      // Don't throw error as this is non-critical
    }
  }

  /**
   * Get refund status for an order
   */
  async getRefundStatus(orderId: string): Promise<{
    hasRefunds: boolean;
    totalRefunded: number;
    refundHistory: Array<{
      amount: number;
      reason: string;
      date: string;
      status: string;
    }>;
  }> {
    try {
      const transactions = await vnpayService.getTransactionHistory(orderId);
      const refunds = transactions.filter(tx => 
        tx.status === 'REFUNDED' || tx.status === 'PARTIAL_REFUND'
      );

      const totalRefunded = refunds.reduce((sum, tx) => sum + (tx.refundAmount || 0), 0);

      const refundHistory = refunds.map(tx => ({
        amount: tx.refundAmount || 0,
        reason: tx.refundReason || 'No reason provided',
        date: tx.refundDate || tx.updatedAt,
        status: tx.status
      }));

      return {
        hasRefunds: refunds.length > 0,
        totalRefunded,
        refundHistory
      };

    } catch (error) {
      console.error('Error getting refund status:', error);
      return {
        hasRefunds: false,
        totalRefunded: 0,
        refundHistory: []
      };
    }
  }

  /**
   * Cancel pending refund
   */
  async cancelPendingRefund(refundId: string): Promise<boolean> {
    try {
      await api.post(`/admin/refunds/${refundId}/cancel`);
      return true;
    } catch (error) {
      console.error('Error cancelling pending refund:', error);
      return false;
    }
  }
}

// Export singleton instance
export const refundService = RefundService.getInstance();