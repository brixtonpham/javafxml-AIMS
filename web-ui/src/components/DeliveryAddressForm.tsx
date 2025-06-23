import React from 'react';

interface DeliveryAddressFormProps {
  onNext: () => void;
  onBack: () => void;
}

const DeliveryAddressForm: React.FC<DeliveryAddressFormProps> = ({ onNext, onBack }) => {
  return (
    <div>
      <h2>Delivery Address Form</h2>
      <button onClick={onBack}>Back</button>
      <button onClick={onNext}>Next</button>
    </div>
  );
};

export default DeliveryAddressForm;