import React from 'react';
import { motion } from 'framer-motion';
import { useCart } from '../../hooks/useCart';
import CartItem from './CartItem';
import CartSummary from './CartSummary';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

const CartDrawer: React.FC<CartDrawerProps> = ({ isOpen, onClose }) => {
  const { items, totalItems, clearCart, cart } = useCart();

  return (
    <motion.div
      initial={{ x: '100%' }}
      animate={{ x: isOpen ? '0%' : '100%' }}
      exit={{ x: '100%' }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      className="fixed top-0 right-0 w-80 h-full bg-white shadow-lg z-50"
    >
      <div className="flex flex-col h-full">
        {/* Header */}
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-lg font-semibold">Your Cart</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 transition"
          >
            Close
          </button>
        </div>

        {/* Cart Items */}
        <div className="flex-1 overflow-y-auto p-4">
          {totalItems > 0 ? (
            items.map((item) => <CartItem key={item.productId} item={item} />)
          ) : (
            <div className="text-center text-gray-500 mt-10">
              Your cart is empty.
            </div>
          )}
        </div>

        {/* Footer */}
        {totalItems > 0 && (
          <div className="p-4 border-t">
            <CartSummary cart={cart || null} />
            <button
              onClick={clearCart}
              className="w-full mt-4 bg-red-500 text-white py-2 rounded hover:bg-red-600 transition"
            >
              Clear Cart
            </button>
          </div>
        )}
      </div>
    </motion.div>
  );
};

export default CartDrawer;