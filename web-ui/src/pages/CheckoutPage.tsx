import React from 'react';
import { Navigate } from 'react-router-dom';
import CheckoutWizard from '../components/CheckoutWizard';
import { useCartContext } from '../contexts/CartContext';

const CheckoutPage: React.FC = () => {
  const { totalItems, isLoading } = useCartContext();

  // Show loading state while cart is loading
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading checkout...</p>
        </div>
      </div>
    );
  }

  // Redirect to cart if empty
  if (totalItems === 0) {
    return <Navigate to="/cart" replace />;
  }

  // Note: Authentication can be added here if required for checkout

  return (
    <div className="min-h-screen bg-gray-50">
      <CheckoutWizard />
    </div>
  );
};

export default CheckoutPage;