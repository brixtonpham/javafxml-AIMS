import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import ProductCard from './ProductCard';
import { ProductCardSkeleton } from '../LoadingSkeletons';
import type { Product } from '../../types';

interface ProductGridProps {
  products: Product[];
  loading?: boolean;
  onAddToCart?: (productId: string) => void;
  onViewDetails?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  favoriteProducts?: string[];
  loadingProducts?: string[];
  emptyStateMessage?: string;
  className?: string;
}

const ProductGrid: React.FC<ProductGridProps> = ({
  products,
  loading = false,
  onAddToCart,
  onViewDetails,
  onToggleFavorite,
  favoriteProducts = [],
  loadingProducts = [],
  emptyStateMessage = 'No products found',
  className = '',
}) => {
  const [columns, setColumns] = useState(4);
  const [gridItems, setGridItems] = useState<Product[][]>([]);
  const gridRef = useRef<HTMLDivElement>(null);

  // Responsive column calculation
  useEffect(() => {
    const calculateColumns = () => {
      const width = window.innerWidth;
      if (width < 640) return 2; // Mobile: 2 columns
      if (width < 768) return 2; // Small tablet: 2 columns
      if (width < 1024) return 3; // Tablet: 3 columns
      if (width < 1366) return 3; // Small laptop: 3 columns
      return 4; // Desktop: 4 columns
    };

    const handleResize = () => {
      setColumns(calculateColumns());
    };

    handleResize(); // Initial calculation
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Masonry layout calculation
  useEffect(() => {
    if (products.length === 0) {
      setGridItems([]);
      return;
    }

    // Initialize columns array
    const newGridItems: Product[][] = Array.from({ length: columns }, () => []);
    
    // Distribute products across columns for masonry effect
    products.forEach((product, index) => {
      const columnIndex = index % columns;
      newGridItems[columnIndex].push(product);
    });

    setGridItems(newGridItems);
  }, [products, columns]);


  // Empty state component
  const EmptyState = () => (
    <div className="col-span-full flex flex-col items-center justify-center py-16 text-center">
      <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <svg
          className="w-12 h-12 text-gray-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          />
        </svg>
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">No Products Found</h3>
      <p className="text-gray-500 max-w-md">
        {emptyStateMessage}
      </p>
    </div>
  );

  if (loading && products.length === 0) {
    return (
      <div className={`grid gap-6 ${className}`} style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
        {Array.from({ length: 12 }).map((_, index) => (
          <ProductCardSkeleton key={index} />
        ))}
      </div>
    );
  }

  if (!loading && products.length === 0) {
    return (
      <div className={className}>
        <EmptyState />
      </div>
    );
  }

  return (
    <div
      ref={gridRef}
      className={`flex gap-6 ${className}`}
      style={{
        minHeight: loading ? '400px' : 'auto',
      }}
    >
      {gridItems.map((columnProducts, columnIndex) => (
        <div
          key={columnIndex}
          className="flex-1 space-y-6"
          style={{ flex: `1 1 ${100 / columns}%` }}
        >
          <AnimatePresence>
            {columnProducts.map((product, productIndex) => (
              <motion.div
                key={product.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{
                  duration: 0.3,
                  delay: (columnIndex * 0.1) + (productIndex * 0.05),
                }}
              >
                <ProductCard
                  product={product}
                  onAddToCart={onAddToCart}
                  onViewDetails={onViewDetails}
                  onToggleFavorite={onToggleFavorite}
                  isFavorite={favoriteProducts.includes(product.id)}
                  isLoading={loadingProducts.includes(product.id)}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      ))}

      {/* Loading overlay for additional products */}
      {loading && products.length > 0 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center"
        >
          <div className="flex items-center space-x-2 text-gray-600">
            <svg className="animate-spin w-5 h-5" viewBox="0 0 24 24">
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
                fill="none"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <span className="text-sm font-medium">Loading more products...</span>
          </div>
        </motion.div>
      )}
    </div>
  );
};

export default ProductGrid;