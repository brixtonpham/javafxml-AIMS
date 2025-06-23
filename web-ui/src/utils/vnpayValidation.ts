import crypto from 'crypto';
import type { 
  VNPayCallbackParams, 
  VNPayPaymentParams, 
  PaymentValidationResult,
  VNPayConfig as VNPayConfigType
} from '../types/payment';

// VNPay validation and utility functions
export class VNPayValidation {
  private static hashSecret: string = import.meta.env.VITE_VNPAY_HASH_SECRET || '';
  
  /**
   * Generate HMAC-SHA512 signature for VNPay
   */
  static generateSecureHash(params: Record<string, string>, secretKey: string = this.hashSecret): string {
    // Sort parameters by key
    const sortedKeys = Object.keys(params).sort();
    
    // Build query string
    const queryString = sortedKeys
      .filter(key => key !== 'vnp_SecureHash' && key !== 'vnp_SecureHashType')
      .map(key => `${key}=${encodeURIComponent(params[key])}`)
      .join('&');
    
    // Generate HMAC-SHA512
    return crypto
      .createHmac('sha512', secretKey)
      .update(queryString)
      .digest('hex')
      .toUpperCase();
  }

  /**
   * Validate VNPay callback parameters
   */
  static validateCallbackParams(params: VNPayCallbackParams): PaymentValidationResult {
    const errors: string[] = [];
    let isValid = true;

    // Required fields validation
    const requiredFields = [
      'vnp_TmnCode',
      'vnp_Amount',
      'vnp_ResponseCode',
      'vnp_TransactionStatus',
      'vnp_TxnRef',
      'vnp_SecureHash'
    ];

    for (const field of requiredFields) {
      if (!params[field as keyof VNPayCallbackParams]) {
        errors.push(`Missing required field: ${field}`);
        isValid = false;
      }
    }

    // Validate secure hash
    const providedHash = params.vnp_SecureHash;
    const paramsWithoutHash = { ...params } as any;
    delete paramsWithoutHash.vnp_SecureHash;
    delete paramsWithoutHash.vnp_SecureHashType;

    const calculatedHash = this.generateSecureHash(paramsWithoutHash as Record<string, string>);
    const secureHashValid = providedHash === calculatedHash;

    if (!secureHashValid) {
      errors.push('Invalid secure hash - data may have been tampered with');
      isValid = false;
    }

    // Validate amount format
    const amount = parseInt(params.vnp_Amount);
    const amountValid = !isNaN(amount) && amount > 0;
    
    if (!amountValid) {
      errors.push('Invalid amount format');
      isValid = false;
    }

    // Validate transaction reference format
    const txnRefValid = /^[A-Za-z0-9_-]+$/.test(params.vnp_TxnRef);
    
    if (!txnRefValid) {
      errors.push('Invalid transaction reference format');
      isValid = false;
    }

    return {
      isValid,
      errors,
      secureHashValid,
      amountValid,
      transactionRefValid: txnRefValid
    };
  }

  /**
   * Generate payment URL for VNPay
   */
  static generatePaymentUrl(params: VNPayPaymentParams, config: VNPayConfigType): string {
    // Convert params to Record<string, string>
    const paramRecord: Record<string, string> = {};
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) {
        paramRecord[key] = String(value);
      }
    });

    // Add secure hash
    const secureHash = this.generateSecureHash(paramRecord);
    const paymentParams = {
      ...paramRecord,
      vnp_SecureHash: secureHash
    };

    // Build query string
    const queryString = Object.entries(paymentParams)
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');

    return `${config.vnp_Url}?${queryString}`;
  }

  /**
   * Parse VNPay response code to human-readable message
   */
  static getResponseMessage(responseCode: string): string {
    const messages: Record<string, string> = {
      '00': 'Transaction successful',
      '07': 'Suspicious transaction. Transaction has been declined',
      '09': 'Card/Account is blocked',
      '10': 'Card/Account has expired',
      '11': 'Insufficient balance',
      '12': 'Card/Account is invalid',
      '13': 'Incorrect authentication password',
      '24': 'Transaction limit exceeded',
      '51': 'Insufficient balance in account',
      '65': 'Account has exceeded daily limit',
      '75': 'Bank is under maintenance',
      '79': 'Incorrect password entered too many times',
      '99': 'Other error (general error)'
    };

    return messages[responseCode] || `Unknown error (Code: ${responseCode})`;
  }

  /**
   * Check if transaction was successful
   */
  static isTransactionSuccessful(responseCode: string, transactionStatus: string): boolean {
    return responseCode === '00' && transactionStatus === '00';
  }

  /**
   * Format amount for VNPay (multiply by 100 for VND)
   */
  static formatAmountForVNPay(amount: number): string {
    return (amount * 100).toString();
  }

  /**
   * Parse amount from VNPay (divide by 100 for VND)
   */
  static parseAmountFromVNPay(vnpAmount: string): number {
    return parseInt(vnpAmount) / 100;
  }

  /**
   * Generate transaction reference
   */
  static generateTransactionRef(orderId: string): string {
    const timestamp = Date.now();
    return `AIMS_${orderId}_${timestamp}`;
  }

  /**
   * Parse order ID from transaction reference
   */
  static parseOrderIdFromTxnRef(txnRef: string): string | null {
    const match = txnRef.match(/^AIMS_([^_]+)_\d+$/);
    return match ? match[1] : null;
  }

  /**
   * Generate payment creation date (yyyyMMddHHmmss)
   */
  static generateCreateDate(): string {
    const now = new Date();
    return now.toISOString()
      .replace(/[-T:.Z]/g, '')
      .slice(0, 14);
  }

  /**
   * Generate payment expiration date (30 minutes from now)
   */
  static generateExpireDate(): string {
    const expireTime = new Date();
    expireTime.setMinutes(expireTime.getMinutes() + 30);
    return expireTime.toISOString()
      .replace(/[-T:.Z]/g, '')
      .slice(0, 14);
  }

  /**
   * Validate payment parameters before sending to VNPay
   */
  static validatePaymentParams(params: VNPayPaymentParams): PaymentValidationResult {
    const errors: string[] = [];
    let isValid = true;

    // Validate required fields
    const requiredFields = [
      'vnp_Version',
      'vnp_Command',
      'vnp_TmnCode',
      'vnp_Amount',
      'vnp_CurrCode',
      'vnp_TxnRef',
      'vnp_OrderInfo',
      'vnp_ReturnUrl',
      'vnp_CreateDate'
    ];

    for (const field of requiredFields) {
      if (!params[field as keyof VNPayPaymentParams]) {
        errors.push(`Missing required field: ${field}`);
        isValid = false;
      }
    }

    // Validate amount
    const amount = parseInt(params.vnp_Amount);
    if (isNaN(amount) || amount <= 0) {
      errors.push('Amount must be a positive number');
      isValid = false;
    }

    // Validate transaction reference
    if (!/^[A-Za-z0-9_-]+$/.test(params.vnp_TxnRef)) {
      errors.push('Transaction reference contains invalid characters');
      isValid = false;
    }

    // Validate order info length
    if (params.vnp_OrderInfo.length > 255) {
      errors.push('Order info exceeds maximum length (255 characters)');
      isValid = false;
    }

    // Validate date format
    if (!/^\d{14}$/.test(params.vnp_CreateDate)) {
      errors.push('Invalid create date format (should be yyyyMMddHHmmss)');
      isValid = false;
    }

    return {
      isValid,
      errors
    };
  }

  /**
   * Get client IP address (for browser environment)
   */
  static async getClientIpAddress(): Promise<string> {
    try {
      // In a real application, you would get this from your backend
      // For now, return a default value
      return '127.0.0.1';
    } catch (error) {
      return '127.0.0.1';
    }
  }

  /**
   * Sanitize order info for VNPay
   */
  static sanitizeOrderInfo(orderInfo: string): string {
    // Remove special characters and limit length
    return orderInfo
      .replace(/[^a-zA-Z0-9\s\-_.]/g, '')
      .slice(0, 255)
      .trim();
  }
}

// VNPay configuration manager
export class VNPayConfigManager {
  private static instance: VNPayConfigManager;
  private config: VNPayConfigType;

  private constructor() {
    this.config = {
      vnp_TmnCode: import.meta.env.VITE_VNPAY_TMN_CODE || 'SANDBOX',
      vnp_HashSecret: import.meta.env.VITE_VNPAY_HASH_SECRET || '',
      vnp_Url: import.meta.env.VITE_VNPAY_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html',
      vnp_Api: import.meta.env.VITE_VNPAY_API || 'https://sandbox.vnpayment.vn/merchant_webapi/api/transaction',
      vnp_ReturnUrl: import.meta.env.VITE_VNPAY_RETURN_URL || `${window.location.origin}/payment/result`,
      vnp_Version: '2.1.0',
      vnp_Command: 'pay',
      vnp_CurrCode: 'VND',
      vnp_Locale: 'vn'
    };
  }

  public static getInstance(): VNPayConfigManager {
    if (!VNPayConfigManager.instance) {
      VNPayConfigManager.instance = new VNPayConfigManager();
    }
    return VNPayConfigManager.instance;
  }

  public getConfig(): VNPayConfigType {
    return this.config;
  }

  public updateConfig(newConfig: Partial<VNPayConfigType>): void {
    this.config = { ...this.config, ...newConfig };
  }
}