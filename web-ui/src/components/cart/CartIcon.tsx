import React from 'react';
import { ShoppingCartIcon } from '@heroicons/react/24/outline';
import { useCart } from '../../hooks/useCart';

interface CartIconProps {
  onClick?: () => void;
}

const CartIcon: React.FC<CartIconProps> = ({ onClick }) => {
  const { totalItems } = useCart();

  return (
    <button
      onClick={onClick}
      className="relative flex items-center justify-center w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 transition"
      aria-label="Cart"
    >
      <ShoppingCartIcon className="w-6 h-6 text-gray-700" />
      {totalItems > 0 && (
        <span className="absolute top-0 right-0 w-5 h-5 bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center">
          {totalItems}
        </span>
      )}
    </button>
  );
};

export default CartIcon;