import React from 'react';

interface PaymentResultProps {
  success: boolean;
  message: string;
  onRetry: () => void;
}

const PaymentResult: React.FC<PaymentResultProps> = ({ success, message, onRetry }) => {
  return (
    <div className={`payment-result ${success ? 'success' : 'failure'}`}>
      <h3>{success ? 'Payment Successful' : 'Payment Failed'}</h3>
      <p>{message}</p>
      {!success && (
        <button onClick={onRetry} className="retry-button">
          Retry Payment
        </button>
      )}
    </div>
  );
};

export default PaymentResult;