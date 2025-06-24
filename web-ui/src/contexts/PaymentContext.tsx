import React, { createContext, useContext, useReducer, useCallback } from 'react';
import { vnpayService } from '../services/VNPayService';
import { API_BASE_URL } from '../services/api';
import type {
  PaymentContextState,
  PaymentContextValue,
  PaymentTransaction,
  VNPayCallbackParams,
  RefundRequest,
  RefundResult,
  EmailNotification,
  PaymentValidationResult
} from '../types/payment';
import { VNPayValidation } from '../utils/vnpayValidation';

type PaymentAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_CURRENT_TRANSACTION'; payload: PaymentTransaction | null }
  | { type: 'SET_PAYMENT_URL'; payload: string | null }
  | { type: 'SET_RETURN_DATA'; payload: VNPayCallbackParams | null }
  | { type: 'ADD_TRANSACTION'; payload: PaymentTransaction }
  | { type: 'UPDATE_TRANSACTION'; payload: PaymentTransaction }
  | { type: 'SET_TRANSACTIONS'; payload: PaymentTransaction[] }
  | { type: 'ADD_NOTIFICATION'; payload: EmailNotification }
  | { type: 'CLEAR_PAYMENT_STATE' }
  | { type: 'SET_PROCESSING'; payload: boolean };

const initialState: PaymentContextState = {
  currentTransaction: null,
  isProcessing: false,
  error: null,
  paymentUrl: null,
  returnData: null,
  transactions: [],
  notifications: []
};

function paymentReducer(state: PaymentContextState, action: PaymentAction): PaymentContextState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isProcessing: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    
    case 'SET_CURRENT_TRANSACTION':
      return { ...state, currentTransaction: action.payload };
    
    case 'SET_PAYMENT_URL':
      return { ...state, paymentUrl: action.payload };
    
    case 'SET_RETURN_DATA':
      return { ...state, returnData: action.payload };
    
    case 'ADD_TRANSACTION':
      return {
        ...state,
        transactions: [...state.transactions, action.payload],
        currentTransaction: action.payload
      };
    
    case 'UPDATE_TRANSACTION':
      return {
        ...state,
        transactions: state.transactions.map(tx =>
          tx.id === action.payload.id ? action.payload : tx
        ),
        currentTransaction: state.currentTransaction?.id === action.payload.id
          ? action.payload
          : state.currentTransaction
      };
    
    case 'SET_TRANSACTIONS':
      return { ...state, transactions: action.payload };
    
    case 'ADD_NOTIFICATION':
      return {
        ...state,
        notifications: [...state.notifications, action.payload]
      };
    
    case 'CLEAR_PAYMENT_STATE':
      return {
        ...initialState,
        transactions: state.transactions // Keep transaction history
      };
    
    case 'SET_PROCESSING':
      return { ...state, isProcessing: action.payload };
    
    default:
      return state;
  }
}

const PaymentContext = createContext<PaymentContextValue | undefined>(undefined);

export const usePaymentContext = () => {
  const context = useContext(PaymentContext);
  if (context === undefined) {
    throw new Error('usePaymentContext must be used within a PaymentProvider');
  }
  return context;
};

interface PaymentProviderProps {
  children: React.ReactNode;
}

export const PaymentProvider: React.FC<PaymentProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(paymentReducer, initialState);

  // Initiate payment with VNPay
  const initiatePayment = useCallback(async (orderId: string, paymentMethodId: string): Promise<void> => {
    try {
      dispatch({ type: 'SET_PROCESSING', payload: true });
      dispatch({ type: 'SET_ERROR', payload: null });

      // Get order details for payment (this would come from your order service)
      const orderResponse = await fetch(`${API_BASE_URL}/orders/${orderId}`);
      const order = await orderResponse.json();

      if (!order) {
        throw new Error('Order not found');
      }

      // Create payment request
      const paymentRequest = await vnpayService.createPaymentRequest(
        orderId,
        order.totalAmount,
        `Payment for Order #${orderId}`,
        await VNPayValidation.getClientIpAddress()
      );

      // Update state with payment URL and transaction
      dispatch({ type: 'SET_PAYMENT_URL', payload: paymentRequest.paymentUrl });
      
      // Create initial transaction record
      const initialTransaction: PaymentTransaction = {
        id: paymentRequest.transactionId,
        orderId,
        transactionId: paymentRequest.transactionRef,
        amount: order.totalAmount,
        currency: 'VND',
        status: 'PENDING',
        paymentMethod: 'VNPAY',
        responseCode: '',
        responseMessage: 'Payment initiated',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        orderInfo: `Payment for Order #${orderId}`
      };

      dispatch({ type: 'ADD_TRANSACTION', payload: initialTransaction });

      // Redirect to VNPay
      window.location.href = paymentRequest.paymentUrl;

    } catch (error) {
      console.error('Error initiating payment:', error);
      dispatch({ type: 'SET_ERROR', payload: error instanceof Error ? error.message : 'Payment initiation failed' });
    } finally {
      dispatch({ type: 'SET_PROCESSING', payload: false });
    }
  }, []);

  // Process payment return from VNPay
  const processPaymentReturn = useCallback(async (returnParams: VNPayCallbackParams): Promise<PaymentTransaction> => {
    try {
      dispatch({ type: 'SET_PROCESSING', payload: true });
      dispatch({ type: 'SET_ERROR', payload: null });
      dispatch({ type: 'SET_RETURN_DATA', payload: returnParams });

      // Process payment return through service
      const transaction = await vnpayService.processPaymentReturn(returnParams);
      
      // Update transaction in state
      dispatch({ type: 'UPDATE_TRANSACTION', payload: transaction });

      // Clear payment URL since payment is complete
      dispatch({ type: 'SET_PAYMENT_URL', payload: null });

      return transaction;

    } catch (error) {
      console.error('Error processing payment return:', error);
      dispatch({ type: 'SET_ERROR', payload: error instanceof Error ? error.message : 'Payment processing failed' });
      throw error;
    } finally {
      dispatch({ type: 'SET_PROCESSING', payload: false });
    }
  }, []);

  // Process refund request
  const processRefund = useCallback(async (refundRequest: RefundRequest): Promise<RefundResult> => {
    try {
      dispatch({ type: 'SET_PROCESSING', payload: true });
      dispatch({ type: 'SET_ERROR', payload: null });

      // Process refund through service
      const refundResult = await vnpayService.processRefund(refundRequest);

      // Update transaction status if refund was successful
      if (refundResult.success) {
        const updatedTransaction = state.transactions.find(tx => tx.id === refundRequest.transactionId);
        if (updatedTransaction) {
          const refundedTransaction: PaymentTransaction = {
            ...updatedTransaction,
            status: refundRequest.amount === updatedTransaction.amount ? 'REFUNDED' : 'PARTIAL_REFUND',
            refundAmount: refundRequest.amount,
            refundReason: refundRequest.reason,
            refundDate: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          };
          dispatch({ type: 'UPDATE_TRANSACTION', payload: refundedTransaction });
        }
      }

      return refundResult;

    } catch (error) {
      console.error('Error processing refund:', error);
      dispatch({ type: 'SET_ERROR', payload: error instanceof Error ? error.message : 'Refund processing failed' });
      throw error;
    } finally {
      dispatch({ type: 'SET_PROCESSING', payload: false });
    }
  }, [state.transactions]);

  // Validate payment data
  const validatePaymentData = useCallback((params: VNPayCallbackParams): PaymentValidationResult => {
    return VNPayValidation.validateCallbackParams(params);
  }, []);

  // Clear payment state
  const clearPaymentState = useCallback(() => {
    dispatch({ type: 'CLEAR_PAYMENT_STATE' });
  }, []);

  // Get transaction history for an order
  const getTransactionHistory = useCallback(async (orderId: string): Promise<PaymentTransaction[]> => {
    try {
      dispatch({ type: 'SET_PROCESSING', payload: true });
      
      const transactions = await vnpayService.getTransactionHistory(orderId);
      dispatch({ type: 'SET_TRANSACTIONS', payload: transactions });
      
      return transactions;

    } catch (error) {
      console.error('Error fetching transaction history:', error);
      dispatch({ type: 'SET_ERROR', payload: error instanceof Error ? error.message : 'Failed to fetch transaction history' });
      throw error;
    } finally {
      dispatch({ type: 'SET_PROCESSING', payload: false });
    }
  }, []);

  // Send payment notification
  const sendPaymentNotification = useCallback(async (notification: EmailNotification): Promise<void> => {
    try {
      // Add notification to local state
      dispatch({ type: 'ADD_NOTIFICATION', payload: notification });

      // Send notification through API
      await fetch(`${API_BASE_URL}/notifications/payment`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(notification),
      });

    } catch (error) {
      console.error('Error sending payment notification:', error);
      // Don't throw error as this is not critical for payment flow
    }
  }, []);

  // Auto-clear errors after 10 seconds
  React.useEffect(() => {
    if (state.error) {
      const timer = setTimeout(() => {
        dispatch({ type: 'SET_ERROR', payload: null });
      }, 10000);

      return () => clearTimeout(timer);
    }
  }, [state.error]);

  // Handle page visibility change to check for payment completion
  React.useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden && state.paymentUrl && state.currentTransaction) {
        // Check if we're returning from payment gateway
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('vnp_ResponseCode')) {
          // Parse return parameters
          const returnParams: Partial<VNPayCallbackParams> = {};
          for (const [key, value] of urlParams.entries()) {
            if (key.startsWith('vnp_')) {
              (returnParams as any)[key] = value;
            }
          }

          // Process return if we have valid parameters
          if (returnParams.vnp_ResponseCode && returnParams.vnp_TxnRef) {
            processPaymentReturn(returnParams as VNPayCallbackParams);
          }
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [state.paymentUrl, state.currentTransaction, processPaymentReturn]);

  const value: PaymentContextValue = {
    ...state,
    initiatePayment,
    processPaymentReturn,
    processRefund,
    validatePaymentData,
    clearPaymentState,
    getTransactionHistory,
    sendPaymentNotification
  };

  return (
    <PaymentContext.Provider value={value}>
      {children}
    </PaymentContext.Provider>
  );
};