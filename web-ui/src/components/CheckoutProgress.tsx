import React from 'react';

interface CheckoutProgressProps {
  currentStep: number;
}

const CheckoutProgress: React.FC<CheckoutProgressProps> = ({ currentStep }) => {
  const steps = ['Customer Info', 'Delivery Address', 'Delivery Options', 'Order Summary', 'Complete'];

  return (
    <div>
      <h2>Checkout Progress</h2>
      <ul>
        {steps.map((step, index) => (
          <li key={index} style={{ fontWeight: currentStep === index + 1 ? 'bold' : 'normal' }}>
            {step}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default CheckoutProgress;