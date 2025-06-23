import React from 'react';

const PaymentLoading: React.FC = () => {
  return (
    <div className="payment-loading">
      <div className="spinner"></div>
      <p>Processing your payment, please wait...</p>
    </div>
  );
};

export default PaymentLoading;