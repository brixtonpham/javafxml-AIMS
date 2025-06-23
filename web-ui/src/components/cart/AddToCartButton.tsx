import React, { useState } from 'react';
import { Button } from '../ui';
import { ShoppingCartIcon, CheckIcon, ExclamationTriangleIcon } from '@heroicons/react/24/outline';
import { useCartContext } from '../../contexts/CartContext';
import type { Product } from '../../types';

interface AddToCartButtonProps {
  product?: Product;
  quantity?: number;
  variant?: 'primary' | 'secondary' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  showQuantity?: boolean;
  disabled?: boolean;
  className?: string;
  onSuccess?: () => void;
  onError?: (error: string) => void;
}

const AddToCartButton: React.FC<AddToCartButtonProps> = ({
  product,
  quantity = 1,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  showQuantity = false,
  disabled = false,
  className = '',
  onSuccess,
  onError,
}) => {
  const { 
    addToCart, 
    isAddingToCart, 
    canAddToCart, 
    getItemQuantity,
    isItemInCart 
  } = useCartContext();
  
  const [showSuccess, setShowSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Handle undefined product gracefully
  if (!product) {
    return (
      <Button
        variant="secondary"
        size={size}
        fullWidth={fullWidth}
        disabled={true}
        className={className}
      >
        Product Not Available
      </Button>
    );
  }

  const currentQuantityInCart = getItemQuantity(product.id);
  const isInCart = isItemInCart(product.id);
  const canAdd = canAddToCart(product, quantity);
  const isOutOfStock = product.quantity === 0;
  const isInvalidQuantity = quantity <= 0;
  const isDisabled = disabled || isOutOfStock || !canAdd || isAddingToCart || isInvalidQuantity;

  const handleAddToCart = async () => {
    setError(null);
    
    try {
      await addToCart(product.id, quantity);
      
      // Show success state
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
      
      onSuccess?.();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to add to cart';
      setError(errorMessage);
      onError?.(errorMessage);
      
      // Clear error after 3 seconds
      setTimeout(() => setError(null), 3000);
    }
  };

  const getButtonText = () => {
    if (isAddingToCart) return 'Adding...';
    if (showSuccess) return 'Added!';
    if (isOutOfStock) return 'Out of Stock';
    if (error) return 'Try Again';
    if (isInCart && showQuantity) return `Add More (${currentQuantityInCart} in cart)`;
    return 'Add to Cart';
  };

  const getButtonIcon = () => {
    if (isAddingToCart) return null;
    if (showSuccess) return <CheckIcon className="w-4 h-4" />;
    if (error) return <ExclamationTriangleIcon className="w-4 h-4" />;
    return <ShoppingCartIcon className="w-4 h-4" />;
  };

  const getButtonVariant = () => {
    if (showSuccess) return 'primary';
    if (error) return 'secondary';
    if (isOutOfStock) return 'secondary';
    return variant;
  };

  // Stock validation warning
  const getStockWarning = () => {
    if (isOutOfStock) return null;
    
    const totalRequestedQuantity = currentQuantityInCart + quantity;
    if (totalRequestedQuantity > product.quantity) {
      const available = product.quantity - currentQuantityInCart;
      if (available <= 0) {
        return `Maximum quantity reached`;
      }
      return `Only ${available} more available`;
    }
    
    return null;
  };

  const stockWarning = getStockWarning();

  return (
    <div className={`flex flex-col ${className}`}>
      <Button
        variant={getButtonVariant()}
        size={size}
        fullWidth={fullWidth}
        disabled={isDisabled}
        onClick={handleAddToCart}
        isLoading={isAddingToCart}
        className={`
          ${showSuccess ? 'bg-green-600 hover:bg-green-700 border-green-600' : ''}
          ${error ? 'bg-red-600 hover:bg-red-700 border-red-600' : ''}
          ${isOutOfStock ? 'bg-gray-400 cursor-not-allowed' : ''}
          transition-all duration-200
        `}
      >
        <div className="flex items-center justify-center space-x-2">
          {getButtonIcon()}
          <span>{getButtonText()}</span>
          {showQuantity && quantity > 1 && !isAddingToCart && !showSuccess && (
            <span className="text-xs bg-white bg-opacity-20 px-2 py-0.5 rounded">
              +{quantity}
            </span>
          )}
        </div>
      </Button>

      {/* Stock warning */}
      {stockWarning && (
        <div className="text-xs text-orange-600 mt-1 text-center">
          {stockWarning}
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="text-xs text-red-600 mt-1 text-center">
          {error}
        </div>
      )}

      {/* Low stock indicator */}
      {!isOutOfStock && product.quantity <= 5 && (
        <div className="text-xs text-orange-600 mt-1 text-center">
          Only {product.quantity} left in stock
        </div>
      )}

      {/* Success confirmation */}
      {showSuccess && (
        <div className="text-xs text-green-600 mt-1 text-center">
          Item added to cart successfully!
        </div>
      )}
    </div>
  );
};

export default AddToCartButton;