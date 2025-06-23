import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  TrashIcon,
  HeartIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline';
import { HeartIcon as HeartSolidIcon } from '@heroicons/react/24/solid';
import { Button } from '../ui';
import QuantitySelector from './QuantitySelector';
import { useCartContext } from '../../contexts/CartContext';
import type { CartItem as CartItemType } from '../../types';

interface CartItemProps {
  item: CartItemType;
  showDivider?: boolean;
  onRemove?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  isFavorite?: boolean;
}

const CartItem: React.FC<CartItemProps> = ({
  item,
  showDivider = true,
  onRemove,
  onToggleFavorite,
  isFavorite = false
}) => {
  const { updateQuantity, removeFromCart, isUpdatingQuantity } = useCartContext();
  const [isRemoving, setIsRemoving] = useState(false);
  const [showConfirmRemove, setShowConfirmRemove] = useState(false);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  const getProductTypeIcon = (type: string) => {
    switch (type) {
      case 'BOOK': return '📚';
      case 'CD': return '💿';
      case 'DVD': return '📀';
      case 'LP': return '🎵';
      default: return '📦';
    }
  };

  const getSecondaryInfo = () => {
    switch (item.product.productType) {
      case 'BOOK':
        return item.product.author;
      case 'CD':
        return item.product.artists;
      case 'DVD':
        return item.product.director;
      case 'LP':
        return item.product.artists;
      default:
        return item.product.category;
    }
  };

  const handleQuantityChange = async (newQuantity: number) => {
    try {
      await updateQuantity(item.productId, newQuantity);
    } catch (error) {
      console.error('Failed to update quantity:', error);
    }
  };

  const handleRemove = async () => {
    if (!showConfirmRemove) {
      setShowConfirmRemove(true);
      return;
    }

    setIsRemoving(true);
    try {
      await removeFromCart(item.productId);
      onRemove?.(item.productId);
    } catch (error) {
      console.error('Failed to remove item:', error);
    } finally {
      setIsRemoving(false);
      setShowConfirmRemove(false);
    }
  };

  const handleToggleFavorite = () => {
    onToggleFavorite?.(item.productId);
  };

  // Check for stock issues
  const isOutOfStock = item.product.quantity === 0;
  const hasStockIssue = item.quantity > item.product.quantity;
  const stockWarning = hasStockIssue
    ? `Only ${item.product.quantity} available`
    : null;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 20 }}
      className={`${showDivider ? 'border-b border-gray-200' : ''} ${isRemoving ? 'opacity-50' : ''}`}
    >
      <div className="flex items-start space-x-4 py-4">
        {/* Product Image */}
        <div className="flex-shrink-0">
          <div className="relative w-20 h-20 sm:w-24 sm:h-24 bg-gray-100 rounded-lg overflow-hidden">
            <img
              src={item.product.imageUrl || '/api/placeholder/100/100'}
              alt={item.product.title}
              className="w-full h-full object-cover"
              loading="lazy"
            />
            {isOutOfStock && (
              <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                <span className="text-white text-xs font-medium">Out of Stock</span>
              </div>
            )}
            
            {/* Product type badge */}
            <div className="absolute top-1 left-1 text-lg">
              {getProductTypeIcon(item.product.productType)}
            </div>
          </div>
        </div>

        {/* Product Details */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-medium text-gray-900 line-clamp-2">
                {item.product.title}
              </h3>
              
              {getSecondaryInfo() && (
                <p className="text-sm text-gray-600 mt-1">
                  {getSecondaryInfo()}
                </p>
              )}

              <div className="flex items-center space-x-2 mt-1">
                <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                  {item.product.productType}
                </span>
                <span className="text-xs text-gray-500">
                  {item.product.category}
                </span>
              </div>

              {/* Stock warnings */}
              {stockWarning && (
                <div className="flex items-center mt-2 text-xs text-orange-600">
                  <ExclamationTriangleIcon className="w-3 h-3 mr-1" />
                  {stockWarning}
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex items-center space-x-2 ml-4">
              <button
                onClick={handleToggleFavorite}
                className="p-2 text-gray-400 hover:text-red-500 transition-colors"
                title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
              >
                {isFavorite ? (
                  <HeartSolidIcon className="w-4 h-4 text-red-500" />
                ) : (
                  <HeartIcon className="w-4 h-4" />
                )}
              </button>
            </div>
          </div>

          {/* Price and Quantity */}
          <div className="flex items-center justify-between mt-3">
            <div className="flex items-center space-x-4">
              <div className="text-lg font-semibold text-gray-900">
                {formatPrice(item.product.price)}
              </div>
              <div className="text-sm text-gray-500">
                per item
              </div>
            </div>

            <div className="flex items-center space-x-4">
              {/* Quantity Selector */}
              <QuantitySelector
                product={item.product}
                currentQuantity={item.quantity}
                onQuantityChange={handleQuantityChange}
                disabled={isUpdatingQuantity || isOutOfStock}
                size="sm"
                showLabel={false}
                maxQuantity={item.product.quantity}
              />

              {/* Remove Button */}
              <div className="relative">
                <AnimatePresence>
                  {showConfirmRemove ? (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.8 }}
                      className="flex items-center space-x-1"
                    >
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setShowConfirmRemove(false)}
                      >
                        Cancel
                      </Button>
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={handleRemove}
                        isLoading={isRemoving}
                        className="bg-red-600 hover:bg-red-700 border-red-600"
                      >
                        Confirm
                      </Button>
                    </motion.div>
                  ) : (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleRemove}
                      disabled={isRemoving}
                      className="text-red-600 border-red-200 hover:bg-red-50 hover:border-red-300"
                    >
                      <TrashIcon className="w-4 h-4" />
                    </Button>
                  )}
                </AnimatePresence>
              </div>
            </div>
          </div>

          {/* Subtotal */}
          <div className="flex justify-between items-center mt-3 pt-3 border-t border-gray-100">
            <span className="text-sm text-gray-600">Subtotal:</span>
            <span className="text-lg font-semibold text-primary-600">
              {formatPrice(item.subtotal)}
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export default CartItem;