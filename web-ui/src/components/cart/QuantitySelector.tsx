import React, { useState, useEffect } from 'react';
import { Button } from '../ui';
import { MinusIcon, PlusIcon } from '@heroicons/react/24/outline';
import type { Product } from '../../types';

interface QuantitySelectorProps {
  product: Product;
  currentQuantity: number;
  onQuantityChange: (quantity: number) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  maxQuantity?: number;
  className?: string;
}

const QuantitySelector: React.FC<QuantitySelectorProps> = ({
  product,
  currentQuantity,
  onQuantityChange,
  disabled = false,
  size = 'md',
  showLabel = true,
  maxQuantity,
  className = '',
}) => {
  const [quantity, setQuantity] = useState(currentQuantity);
  const [inputValue, setInputValue] = useState(currentQuantity.toString());

  // Sync with prop changes
  useEffect(() => {
    setQuantity(currentQuantity);
    setInputValue(currentQuantity.toString());
  }, [currentQuantity]);

  const productStock = product.quantityInStock ?? product.quantity ?? 0;
  const maxAllowed = Math.min(
    maxQuantity ?? productStock,
    productStock
  );

  const canDecrease = quantity > 1 && !disabled;
  const canIncrease = quantity < maxAllowed && !disabled;
  const isOutOfStock = productStock === 0;

  const handleDecrease = () => {
    if (canDecrease) {
      const newQuantity = quantity - 1;
      setQuantity(newQuantity);
      setInputValue(newQuantity.toString());
      onQuantityChange(newQuantity);
    }
  };

  const handleIncrease = () => {
    if (canIncrease) {
      const newQuantity = quantity + 1;
      setQuantity(newQuantity);
      setInputValue(newQuantity.toString());
      onQuantityChange(newQuantity);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInputValue(value);

    // Validate and update quantity
    const numValue = parseInt(value, 10);
    if (!isNaN(numValue) && numValue >= 1 && numValue <= maxAllowed) {
      setQuantity(numValue);
      onQuantityChange(numValue);
    }
  };

  const handleInputBlur = () => {
    // Reset to current valid quantity if input is invalid
    const numValue = parseInt(inputValue, 10);
    if (isNaN(numValue) || numValue < 1 || numValue > maxAllowed) {
      setInputValue(quantity.toString());
    }
  };

  const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // Allow only numbers and control keys
    if (!/\d/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
      e.preventDefault();
    }
  };

  // Size-based styling
  const sizeClasses = {
    sm: {
      button: 'w-6 h-6',
      input: 'w-12 h-6 text-xs',
      icon: 'w-3 h-3',
    },
    md: {
      button: 'w-8 h-8',
      input: 'w-16 h-8 text-sm',
      icon: 'w-4 h-4',
    },
    lg: {
      button: 'w-10 h-10',
      input: 'w-20 h-10 text-base',
      icon: 'w-5 h-5',
    },
  };

  const { button: buttonClass, input: inputClass, icon: iconClass } = sizeClasses[size];

  if (isOutOfStock) {
    return (
      <div className={`flex flex-col items-center ${className}`}>
        {showLabel && (
          <span className="text-xs text-gray-500 mb-1">Quantity</span>
        )}
        <div className="text-red-500 text-sm font-medium">
          Out of Stock
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col items-center ${className}`}>
      {showLabel && (
        <span className="text-xs text-gray-500 mb-1">Quantity</span>
      )}
      
      <div className="flex items-center space-x-1">
        <Button
          variant="outline"
          size="sm"
          onClick={handleDecrease}
          disabled={!canDecrease}
          className={`${buttonClass} p-0 flex items-center justify-center`}
          aria-label="Decrease quantity"
        >
          <MinusIcon className={iconClass} />
        </Button>

        <input
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          onBlur={handleInputBlur}
          onKeyDown={handleInputKeyDown}
          disabled={disabled}
          className={`${inputClass} text-center border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:text-gray-500`}
          aria-label="Quantity"
        />

        <Button
          variant="outline"
          size="sm"
          onClick={handleIncrease}
          disabled={!canIncrease}
          className={`${buttonClass} p-0 flex items-center justify-center`}
          aria-label="Increase quantity"
        >
          <PlusIcon className={iconClass} />
        </Button>
      </div>

      {/* Stock warning */}
      {quantity > productStock && (
        <div className="text-xs text-red-500 mt-1">
          Only {productStock} available
        </div>
      )}

      {/* Low stock warning */}
      {productStock <= 5 && productStock > 0 && (
        <div className="text-xs text-orange-500 mt-1">
          {productStock} left in stock
        </div>
      )}
    </div>
  );
};

export default QuantitySelector;