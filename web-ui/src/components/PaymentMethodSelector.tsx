import React, { useState, useEffect } from 'react';
import PaymentMethodCard from './PaymentMethodCard';
import { paymentService } from '../services/paymentService';
import type { PaymentMethod } from '../types';

interface PaymentMethodSelectorProps {
  onSelect: (paymentMethod: PaymentMethod) => void;
}

const PaymentMethodSelector: React.FC<PaymentMethodSelectorProps> = ({ onSelect }) => {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [selectedMethod, setSelectedMethod] = useState<string | null>(null);

  useEffect(() => {
    const fetchPaymentMethods = async () => {
      try {
        const methods = await paymentService.getPaymentMethods();
        setPaymentMethods(methods);
      } catch (error) {
        console.error('Failed to fetch payment methods:', error);
      }
    };

    fetchPaymentMethods();
  }, []);

  const handleSelect = (methodId: string) => {
    setSelectedMethod(methodId);
    const selected = paymentMethods.find((method) => method.id === methodId);
    if (selected) {
      onSelect(selected);
    }
  };

  return (
    <div className="payment-method-selector">
      <h3>Select a Payment Method</h3>
      <div className="payment-method-cards">
        {paymentMethods.map((method) => (
          <PaymentMethodCard
            key={method.id}
            paymentMethod={method}
            isSelected={selectedMethod === method.id}
            onSelect={handleSelect}
          />
        ))}
      </div>
    </div>
  );
};

export default PaymentMethodSelector;