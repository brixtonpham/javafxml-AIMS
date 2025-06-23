// VNPay Payment Integration Types
export interface VNPayConfig {
  vnp_TmnCode: string;
  vnp_HashSecret: string;
  vnp_Url: string;
  vnp_Api: string;
  vnp_ReturnUrl: string;
  vnp_Version: string;
  vnp_Command: string;
  vnp_CurrCode: string;
  vnp_Locale: string;
}

export interface VNPayPaymentParams {
  vnp_Version: string;
  vnp_Command: string;
  vnp_TmnCode: string;
  vnp_Amount: string;
  vnp_CurrCode: string;
  vnp_TxnRef: string;
  vnp_OrderInfo: string;
  vnp_OrderType: string;
  vnp_Locale: string;
  vnp_ReturnUrl: string;
  vnp_IpAddr: string;
  vnp_CreateDate: string;
  vnp_ExpireDate?: string;
  vnp_SecureHash?: string;
}

export interface VNPayCallbackParams {
  vnp_TmnCode: string;
  vnp_Amount: string;
  vnp_BankCode: string;
  vnp_BankTranNo: string;
  vnp_CardType: string;
  vnp_PayDate: string;
  vnp_OrderInfo: string;
  vnp_TransactionNo: string;
  vnp_ResponseCode: string;
  vnp_TransactionStatus: string;
  vnp_TxnRef: string;
  vnp_SecureHashType: string;
  vnp_SecureHash: string;
}

export interface PaymentTransaction {
  id: string;
  orderId: string;
  transactionId: string;
  vnpTransactionNo?: string;
  amount: number;
  currency: string;
  status: PaymentTransactionStatus;
  paymentMethod: string;
  bankCode?: string;
  bankTranNo?: string;
  cardType?: string;
  responseCode: string;
  responseMessage: string;
  payDate?: string;
  createdAt: string;
  updatedAt: string;
  refundAmount?: number;
  refundReason?: string;
  refundDate?: string;
  ipAddress?: string;
  orderInfo: string;
}

export const PaymentTransactionStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED',
  CANCELLED: 'CANCELLED',
  REFUNDED: 'REFUNDED',
  PARTIAL_REFUND: 'PARTIAL_REFUND'
} as const;

export type PaymentTransactionStatus = typeof PaymentTransactionStatus[keyof typeof PaymentTransactionStatus];

export interface PaymentState {
  currentTransaction: PaymentTransaction | null;
  isProcessing: boolean;
  error: string | null;
  paymentUrl: string | null;
  returnData: VNPayCallbackParams | null;
}

export interface RefundRequest {
  orderId: string;
  transactionId: string;
  amount: number;
  reason: string;
  requestedBy: string;
}

export interface RefundResult {
  success: boolean;
  refundId?: string;
  refundAmount?: number;
  message: string;
  errorCode?: string;
  transactionData?: any;
}

export const VNPayResponseCode = {
  SUCCESS: '00',
  SUSPICIOUS_TRANSACTION: '07',
  CARD_BLOCKED: '09',
  EXPIRED_CARD: '10',
  INSUFFICIENT_BALANCE: '11',
  INVALID_CARD: '12',
  INCORRECT_PASSWORD: '13',
  TRANSACTION_LIMIT_EXCEEDED: '24',
  INVALID_AMOUNT: '51',
  ACCOUNT_LOCKED: '65',
  GENERAL_ERROR: '99'
} as const;

export type VNPayResponseCode = typeof VNPayResponseCode[keyof typeof VNPayResponseCode];

export interface PaymentValidationResult {
  isValid: boolean;
  errors: string[];
  secureHashValid?: boolean;
  amountValid?: boolean;
  transactionRefValid?: boolean;
}

export interface EmailNotification {
  type: 'PAYMENT_SUCCESS' | 'PAYMENT_FAILED' | 'REFUND_PROCESSED';
  recipientEmail: string;
  orderId: string;
  transactionId: string;
  amount: number;
  data: Record<string, any>;
}

// Payment Context Types
export interface PaymentContextState extends PaymentState {
  transactions: PaymentTransaction[];
  notifications: EmailNotification[];
}

export interface PaymentContextValue extends PaymentContextState {
  initiatePayment: (orderId: string, paymentMethodId: string) => Promise<void>;
  processPaymentReturn: (returnParams: VNPayCallbackParams) => Promise<PaymentTransaction>;
  processRefund: (refundRequest: RefundRequest) => Promise<RefundResult>;
  validatePaymentData: (params: VNPayCallbackParams) => PaymentValidationResult;
  clearPaymentState: () => void;
  getTransactionHistory: (orderId: string) => Promise<PaymentTransaction[]>;
  sendPaymentNotification: (notification: EmailNotification) => Promise<void>;
}

// Payment Processing Flow Types
export interface PaymentFlow {
  orderId: string;
  totalAmount: number;
  deliveryInfo: any;
  items: any[];
  step: PaymentFlowStep;
  data: Record<string, any>;
}

export const PaymentFlowStep = {
  INITIATED: 'INITIATED',
  REDIRECTED: 'REDIRECTED',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
} as const;

export type PaymentFlowStep = typeof PaymentFlowStep[keyof typeof PaymentFlowStep];