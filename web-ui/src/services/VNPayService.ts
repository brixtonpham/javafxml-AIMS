import { api } from './api';
import { VNPayValidation, VNPayConfigManager } from '../utils/vnpayValidation';
import type {
  VNPayPaymentParams,
  VNPayCallbackParams,
  PaymentTransaction,
  RefundRequest,
  RefundResult,
  PaymentValidationResult,
  VNPayConfig,
  EmailNotification
} from '../types/payment';
import type { Order } from '../types';

export class VNPayService {
  private static instance: VNPayService;
  private config: VNPayConfig;

  private constructor() {
    this.config = VNPayConfigManager.getInstance().getConfig();
  }

  public static getInstance(): VNPayService {
    if (!VNPayService.instance) {
      VNPayService.instance = new VNPayService();
    }
    return VNPayService.instance;
  }

  /**
   * Create VNPay payment request
   */
  async createPaymentRequest(
    orderId: string,
    amount: number,
    orderInfo: string,
    customerIp?: string
  ): Promise<{
    paymentUrl: string;
    transactionRef: string;
    transactionId: string;
  }> {
    try {
      // Generate transaction reference
      const transactionRef = VNPayValidation.generateTransactionRef(orderId);
      
      // Get client IP
      const ipAddress = customerIp || await VNPayValidation.getClientIpAddress();
      
      // Prepare payment parameters
      const paymentParams: VNPayPaymentParams = {
        vnp_Version: this.config.vnp_Version,
        vnp_Command: this.config.vnp_Command,
        vnp_TmnCode: this.config.vnp_TmnCode,
        vnp_Amount: VNPayValidation.formatAmountForVNPay(amount),
        vnp_CurrCode: this.config.vnp_CurrCode,
        vnp_TxnRef: transactionRef,
        vnp_OrderInfo: VNPayValidation.sanitizeOrderInfo(orderInfo),
        vnp_OrderType: 'other',
        vnp_Locale: this.config.vnp_Locale,
        vnp_ReturnUrl: this.config.vnp_ReturnUrl,
        vnp_IpAddr: ipAddress,
        vnp_CreateDate: VNPayValidation.generateCreateDate(),
        vnp_ExpireDate: VNPayValidation.generateExpireDate()
      };

      // Validate payment parameters
      const validation = VNPayValidation.validatePaymentParams(paymentParams);
      if (!validation.isValid) {
        throw new Error(`Invalid payment parameters: ${validation.errors.join(', ')}`);
      }

      // Generate payment URL
      const paymentUrl = VNPayValidation.generatePaymentUrl(paymentParams, this.config);

      // Store transaction in backend
      const transactionResponse = await api.post<PaymentTransaction>('/payment/vnpay/create', {
        orderId,
        transactionRef,
        amount,
        orderInfo,
        ipAddress,
        paymentParams
      });

      return {
        paymentUrl,
        transactionRef,
        transactionId: transactionResponse.data.id
      };
    } catch (error) {
      console.error('Error creating VNPay payment request:', error);
      throw new Error('Failed to create payment request. Please try again.');
    }
  }

  /**
   * Process VNPay payment return/callback
   */
  async processPaymentReturn(returnParams: VNPayCallbackParams): Promise<PaymentTransaction> {
    try {
      // Validate callback parameters
      const validation = VNPayValidation.validateCallbackParams(returnParams);
      if (!validation.isValid) {
        throw new Error(`Invalid callback parameters: ${validation.errors.join(', ')}`);
      }

      // Check transaction success
      const isSuccess = VNPayValidation.isTransactionSuccessful(
        returnParams.vnp_ResponseCode,
        returnParams.vnp_TransactionStatus
      );

      // Process payment result in backend
      const processResponse = await api.post<PaymentTransaction>('/payment/vnpay/process-return', {
        returnParams,
        isSuccess,
        responseMessage: VNPayValidation.getResponseMessage(returnParams.vnp_ResponseCode)
      });

      const transaction = processResponse.data;

      // Send notification email
      if (isSuccess) {
        await this.sendPaymentSuccessNotification(transaction);
      } else {
        await this.sendPaymentFailureNotification(transaction, returnParams.vnp_ResponseCode);
      }

      return transaction;
    } catch (error) {
      console.error('Error processing VNPay payment return:', error);
      throw new Error('Failed to process payment result. Please contact support.');
    }
  }

  /**
   * Query transaction status from VNPay
   */
  async queryTransactionStatus(transactionRef: string, transactionDate: string): Promise<any> {
    try {
      const response = await api.post('/payment/vnpay/query', {
        transactionRef,
        transactionDate,
        config: this.config
      });

      return response.data;
    } catch (error) {
      console.error('Error querying VNPay transaction status:', error);
      throw new Error('Failed to query transaction status.');
    }
  }

  /**
   * Process refund request
   */
  async processRefund(refundRequest: RefundRequest): Promise<RefundResult> {
    try {
      // Validate refund request
      if (!refundRequest.orderId || !refundRequest.transactionId || !refundRequest.amount) {
        throw new Error('Invalid refund request parameters');
      }

      if (refundRequest.amount <= 0) {
        throw new Error('Refund amount must be greater than 0');
      }

      // Submit refund request to backend
      const refundResponse = await api.post<RefundResult>('/payment/vnpay/refund', {
        ...refundRequest,
        config: this.config
      });

      const refundResult = refundResponse.data;

      // Send refund notification email
      if (refundResult.success) {
        await this.sendRefundNotification(refundRequest, refundResult);
      }

      return refundResult;
    } catch (error) {
      console.error('Error processing VNPay refund:', error);
      throw new Error('Failed to process refund request. Please contact support.');
    }
  }

  /**
   * Get transaction history for an order
   */
  async getTransactionHistory(orderId: string): Promise<PaymentTransaction[]> {
    try {
      const response = await api.get<PaymentTransaction[]>(`/payment/vnpay/transactions/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching transaction history:', error);
      throw new Error('Failed to fetch transaction history.');
    }
  }

  /**
   * Verify webhook/IPN from VNPay
   */
  async verifyWebhook(webhookData: VNPayCallbackParams): Promise<boolean> {
    try {
      // Validate webhook data
      const validation = VNPayValidation.validateCallbackParams(webhookData);
      if (!validation.isValid) {
        console.error('Invalid webhook data:', validation.errors);
        return false;
      }

      // Process webhook in backend
      await api.post('/payment/vnpay/webhook', {
        webhookData,
        validation
      });

      return true;
    } catch (error) {
      console.error('Error verifying VNPay webhook:', error);
      return false;
    }
  }

  /**
   * Check if payment method is available
   */
  async checkPaymentAvailability(): Promise<boolean> {
    try {
      const response = await api.get<{ available: boolean }>('/payment/vnpay/availability');
      return response.data.available;
    } catch (error) {
      console.error('Error checking VNPay availability:', error);
      return false;
    }
  }

  /**
   * Get payment fees and information
   */
  async getPaymentInfo(amount: number): Promise<{
    amount: number;
    fee: number;
    total: number;
    currency: string;
  }> {
    try {
      const response = await api.post<{
        amount: number;
        fee: number;
        total: number;
        currency: string;
      }>('/payment/vnpay/info', { amount });
      return response.data;
    } catch (error) {
      console.error('Error getting payment info:', error);
      throw new Error('Failed to get payment information.');
    }
  }

  /**
   * Cancel pending payment
   */
  async cancelPayment(transactionId: string): Promise<boolean> {
    try {
      await api.post(`/payment/vnpay/cancel/${transactionId}`);
      return true;
    } catch (error) {
      console.error('Error cancelling payment:', error);
      return false;
    }
  }

  /**
   * Send payment success notification
   */
  private async sendPaymentSuccessNotification(transaction: PaymentTransaction): Promise<void> {
    try {
      const notification: EmailNotification = {
        type: 'PAYMENT_SUCCESS',
        recipientEmail: '', // Will be filled by backend from order data
        orderId: transaction.orderId,
        transactionId: transaction.id,
        amount: transaction.amount,
        data: {
          transactionNo: transaction.vnpTransactionNo,
          bankCode: transaction.bankCode,
          cardType: transaction.cardType,
          payDate: transaction.payDate
        }
      };

      await api.post('/notifications/payment', notification);
    } catch (error) {
      console.error('Error sending payment success notification:', error);
      // Don't throw error as payment was successful
    }
  }

  /**
   * Send payment failure notification
   */
  private async sendPaymentFailureNotification(
    transaction: PaymentTransaction,
    responseCode: string
  ): Promise<void> {
    try {
      const notification: EmailNotification = {
        type: 'PAYMENT_FAILED',
        recipientEmail: '', // Will be filled by backend from order data
        orderId: transaction.orderId,
        transactionId: transaction.id,
        amount: transaction.amount,
        data: {
          responseCode,
          responseMessage: VNPayValidation.getResponseMessage(responseCode),
          failureReason: transaction.responseMessage
        }
      };

      await api.post('/notifications/payment', notification);
    } catch (error) {
      console.error('Error sending payment failure notification:', error);
      // Don't throw error
    }
  }

  /**
   * Send refund notification
   */
  private async sendRefundNotification(
    refundRequest: RefundRequest,
    refundResult: RefundResult
  ): Promise<void> {
    try {
      const notification: EmailNotification = {
        type: 'REFUND_PROCESSED',
        recipientEmail: '', // Will be filled by backend from order data
        orderId: refundRequest.orderId,
        transactionId: refundRequest.transactionId,
        amount: refundRequest.amount,
        data: {
          refundId: refundResult.refundId,
          refundAmount: refundResult.refundAmount,
          reason: refundRequest.reason,
          requestedBy: refundRequest.requestedBy
        }
      };

      await api.post('/notifications/payment', notification);
    } catch (error) {
      console.error('Error sending refund notification:', error);
      // Don't throw error
    }
  }

  /**
   * Update configuration
   */
  updateConfig(newConfig: Partial<VNPayConfig>): void {
    this.config = { ...this.config, ...newConfig };
    VNPayConfigManager.getInstance().updateConfig(newConfig);
  }

  /**
   * Get current configuration
   */
  getConfig(): VNPayConfig {
    return { ...this.config };
  }
}

// Export singleton instance
export const vnpayService = VNPayService.getInstance();