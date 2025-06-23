import React from 'react';

interface CustomerInfoFormProps {
  onNext: () => void;
}

const CustomerInfoForm: React.FC<CustomerInfoFormProps> = ({ onNext }) => {
  return (
    <div>
      <h2>Customer Info Form</h2>
      <button onClick={onNext}>Next</button>
    </div>
  );
};

export default CustomerInfoForm;