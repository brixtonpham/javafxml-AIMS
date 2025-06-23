import React from 'react';
import type { PaymentMethod } from '../types';

interface PaymentMethodCardProps {
  paymentMethod: PaymentMethod;
  isSelected: boolean;
  onSelect: (paymentMethodId: string) => void;
}

const PaymentMethodCard: React.FC<PaymentMethodCardProps> = ({ paymentMethod, isSelected, onSelect }) => {
  return (
    <div
      className={`payment-method-card ${isSelected ? 'selected' : ''}`}
      onClick={() => onSelect(paymentMethod.id)}
    >
      <h4>{paymentMethod.name}</h4>
      <p>{paymentMethod.description}</p>
    </div>
  );
};

export default PaymentMethodCard;