import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShoppingCartIcon, EyeIcon, HeartIcon } from '@heroicons/react/24/outline';
import { HeartIcon as HeartSolidIcon } from '@heroicons/react/24/solid';
import { Card, Button } from '../ui';
import AddToCartButton from '../cart/AddToCartButton';
import type { Product } from '../../types';

interface ProductCardProps {
  product: Product;
  onAddToCart?: (productId: string) => void;
  onViewDetails?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  isFavorite?: boolean;
  isLoading?: boolean;
  className?: string;
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
  onViewDetails,
  onToggleFavorite,
  isFavorite = false,
  isLoading = false,
  className = '',
}) => {
  const navigate = useNavigate();
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);

  const handleAddToCart = (e: React.MouseEvent) => {
    e.stopPropagation();
    onAddToCart?.(product.id);
  };

  const handleViewDetails = () => {
    if (onViewDetails) {
      onViewDetails(product.id);
    } else {
      navigate(`/products/${product.id}`);
    }
  };

  const handleToggleFavorite = (e: React.MouseEvent) => {
    e.stopPropagation();
    onToggleFavorite?.(product.id);
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  const getProductTypeIcon = (type: string) => {
    switch (type) {
      case 'BOOK':
        return '📚';
      case 'CD':
        return '💿';
      case 'DVD':
        return '📀';
      case 'LP':
        return '🎵';
      default:
        return '📦';
    }
  };

  const getSecondaryInfo = () => {
    switch (product.productType) {
      case 'BOOK':
        return product.author;
      case 'CD':
        return product.artists;
      case 'DVD':
        return product.director;
      case 'LP':
        return product.artists;
      default:
        return product.category;
    }
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={`group cursor-pointer ${className}`}
      onClick={handleViewDetails}
    >
      <Card
        variant="default"
        padding="none"
        className="h-full overflow-hidden hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1"
      >
        {/* Product Image */}
        <div className="relative aspect-square bg-gray-100 overflow-hidden">
          {!imageError ? (
            <>
              {!imageLoaded && (
                <div className="absolute inset-0 bg-gray-200 animate-pulse" />
              )}
              <img
                src={product.imageUrl || '/api/placeholder/300/300'}
                alt={product.title}
                className={`w-full h-full object-cover transition-opacity duration-300 ${
                  imageLoaded ? 'opacity-100' : 'opacity-0'
                }`}
                onLoad={() => setImageLoaded(true)}
                onError={() => setImageError(true)}
                loading="lazy"
              />
            </>
          ) : (
            <div className="flex items-center justify-center h-full bg-gray-100">
              <span className="text-4xl">{getProductTypeIcon(product.productType)}</span>
            </div>
          )}
          
          {/* Overlay Actions */}
          <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-30 transition-all duration-300 flex items-center justify-center opacity-0 group-hover:opacity-100">
            <div className="flex space-x-2">
              <Button
                size="sm"
                variant="primary"
                onClick={handleAddToCart}
                disabled={isLoading || product.quantity === 0}
                className="transform scale-90 hover:scale-100 transition-transform"
              >
                <ShoppingCartIcon className="w-4 h-4" />
              </Button>
              <Button
                size="sm"
                variant="secondary"
                onClick={handleViewDetails}
                className="transform scale-90 hover:scale-100 transition-transform"
              >
                <EyeIcon className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Favorite Button */}
          <button
            onClick={handleToggleFavorite}
            className="absolute top-2 right-2 p-1.5 rounded-full bg-white bg-opacity-80 backdrop-blur-sm hover:bg-opacity-100 transition-all duration-200"
          >
            {isFavorite ? (
              <HeartSolidIcon className="w-5 h-5 text-red-500" />
            ) : (
              <HeartIcon className="w-5 h-5 text-gray-600 hover:text-red-500" />
            )}
          </button>

          {/* Stock Badge */}
          {product.quantity === 0 && (
            <div className="absolute top-2 left-2 px-2 py-1 bg-red-500 text-white text-xs font-medium rounded">
              Out of Stock
            </div>
          )}
          
          {/* Product Type Badge */}
          <div className="absolute bottom-2 left-2 px-2 py-1 bg-black bg-opacity-60 text-white text-xs font-medium rounded">
            {product.productType}
          </div>
        </div>

        {/* Product Info */}
        <div className="p-4">
          <div className="mb-2">
            <h3 className="font-semibold text-gray-900 line-clamp-2 text-sm leading-tight mb-1">
              {product.title}
            </h3>
            {getSecondaryInfo() && (
              <p className="text-xs text-gray-600 line-clamp-1">
                {getSecondaryInfo()}
              </p>
            )}
          </div>

          <div className="flex items-center justify-between mb-2">
            <span className="text-lg font-bold text-primary-600">
              {formatPrice(product.price)}
            </span>
            <span className="text-xs text-gray-500">
              Stock: {product.quantity}
            </span>
          </div>

          <div className="text-xs text-gray-500 mb-3">
            Category: {product.category}
          </div>

          {/* Quick Add to Cart Button */}
          <AddToCartButton
            product={product}
            size="sm"
            fullWidth
            showQuantity={false}
            className="opacity-0 group-hover:opacity-100 transition-opacity duration-200"
          />
        </div>
      </Card>
    </motion.div>
  );
};

export default ProductCard;